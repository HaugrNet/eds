package commands

import (
	"context"
	"sort"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/address"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
	"github.com/HaugrNet/eds/eds-cli/internal/socket"
)

// Rune's avatars, see the service.
var (
	userAvatars = service.UserAvatars
	roomAvatars = service.RoomAvatars
)

func avatarName(id string) string { return service.AvatarName(id) }

func contains(values []string, value string) bool {
	for _, v := range values {
		if v == value {
			return true
		}
	}
	return false
}

// when writes a stone's time for a person, in the local time zone.
func (c *command) when(stamp string) string {
	t, ok := format.ParseTime(stamp)
	if !ok {
		return ""
	}
	return c.locale().DateTime(t.Local())
}

// iso writes a stone's time for machine-readable output.
func iso(stamp string) string {
	t, ok := format.ParseTime(stamp)
	if !ok {
		return ""
	}
	return format.ISO(t)
}

// rooms lists the rooms on a stone.
func (c *command) rooms(args []string) error {
	fs := c.flags("rooms")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) > 0 {
		return usage("Use 'tolk rooms [--stone <name>]'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, *stoneName)
	if err != nil {
		return err
	}
	rooms, err := stone.Rooms(ctx)
	if err != nil {
		return err
	}
	if c.json() {
		out := []map[string]any{}
		for _, room := range rooms {
			out = append(out, map[string]any{"stone": stone.Name, "id": room.ID, "name": room.Name, "avatar": room.Avatar, "access": accessWord(room.Trust), "member": room.In()})
		}
		return c.printJSON(map[string]any{"rooms": out})
	}
	if len(rooms) == 0 {
		c.note(i18n.T("No rooms on the stone %q yet. Make one with 'tolk room new <name>'.", stone.Name))
		return nil
	}
	width := 4
	for _, room := range rooms {
		width = max(width, len([]rune(room.Name)))
	}
	for _, room := range rooms {
		access := accessText(room.Trust)
		if !room.In() {
			access = c.style().Muted(access)
		}
		c.say("%s  %s", pad(room.Name, width), access)
	}
	return nil
}

// pad fills a text with spaces to a width, counted in characters.
func pad(text string, width int) string {
	if n := len([]rune(text)); n < width {
		return text + strings.Repeat(" ", width-n)
	}
	return text
}

// room handles 'tolk room new|rename|avatar'.
func (c *command) roomCommand(args []string) error {
	if len(args) == 0 {
		return usage("Use 'tolk room new <name>', 'tolk room rename <room> <new name>' or 'tolk room avatar <picture> --room <room>'.")
	}
	switch strings.ToLower(args[0]) {
	case "new", "create":
		return c.roomNew(args[1:])
	case "rename":
		return c.roomRename(args[1:])
	case "avatar":
		return c.roomAvatar(args[1:])
	}
	return usage("Unknown room command %q. Try 'tolk room new <name>'.", args[0])
}

func (c *command) roomNew(args []string) error {
	fs := c.flags("room new")
	chamber := fs.Bool("chamber", false, "")
	featureNames := fs.String("features", "chat", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 {
		return usage("Use 'tolk room new <[stone:]name> [--chamber] [--features chat,calendar,…]'.")
	}
	a, err := address.Parse(pos[0])
	if err != nil || len(a.Path) > 0 || a.AllRooms {
		return usage("%q is not a room's name; write it as [stone:]name.", pos[0])
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, a.Stone)
	if err != nil {
		return err
	}
	var made []byte
	if *chamber {
		// The keys first: without them there is no chamber, and no room is
		// left behind.
		c.chamberWarning()
		if made, err = c.ensureKeys(ctx, stone); err != nil {
			return err
		}
	}
	var id string
	err = stone.Do(ctx, func(token string) error {
		var err error
		id, err = stone.Client.CreateCircle(ctx, token, a.Room)
		return err
	})
	if err != nil {
		return err
	}
	stone.ForgetRooms()
	if *chamber {
		if err := stone.MakeChamber(ctx, id); err != nil {
			return err
		}
	}
	// The features chosen for it: the chat, unless told otherwise.
	var features []string
	for _, feature := range strings.Split(*featureNames, ",") {
		if feature = strings.TrimSpace(feature); feature != "" && feature != "none" {
			features = append(features, feature)
		}
	}
	features = c.switchFeatures(ctx, stone, id, features)
	if c.json() {
		out := map[string]any{"stone": stone.Name, "id": id, "name": a.Room, "chamber": *chamber, "features": features}
		if made != nil {
			// The account got its keys with this chamber: the key to write down.
			out["recoveryKey"] = e2e.RecoveryKey(made)
		}
		return c.printJSON(out)
	}
	if *chamber {
		c.note(i18n.T("Made the chamber %q on %s. You administer it.", a.Room, stone.Name))
	} else {
		c.note(i18n.T("Made the room %q on %s. You administer it.", a.Room, stone.Name))
	}
	return nil
}

func (c *command) roomRename(args []string) error {
	pos, err := parse(c.flags("room rename"), args)
	if err != nil {
		return err
	}
	if len(pos) != 2 || strings.TrimSpace(pos[1]) == "" {
		return usage("Use 'tolk room rename <[stone:]room> <new name>'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, pos[:1], func(t target) (any, error) {
		err := t.stone.Do(ctx, func(token string) error {
			return t.stone.Client.UpdateCircle(ctx, token, t.room.ID, client.ProcessCircleRequest{CircleName: pos[1]})
		})
		if err != nil {
			return nil, err
		}
		c.note(i18n.T("Renamed the room %q to %q.", t.room.Name, pos[1]))
		return map[string]any{"id": t.room.ID, "name": pos[1]}, nil
	})
}

func (c *command) roomAvatar(args []string) error {
	fs := c.flags("room avatar")
	rooms := roomFlag(fs)
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 || len(*rooms) == 0 {
		return usage("Use 'tolk room avatar <picture|none> --room <room>'. The pictures: %s.", strings.Join(roomAvatars, ", "))
	}
	picture := strings.ToLower(pos[0])
	if picture == "none" {
		picture = ""
	} else if !contains(roomAvatars, picture) {
		return usage("%q is not a picture for a room. The pictures: %s.", pos[0], strings.Join(roomAvatars, ", "))
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, *rooms, func(t target) (any, error) {
		err := t.stone.Do(ctx, func(token string) error {
			return t.stone.Client.UpdateCircle(ctx, token, t.room.ID, client.ProcessCircleRequest{Avatar: &picture})
		})
		if err != nil {
			return nil, err
		}
		if picture == "" {
			c.note(i18n.T("%s: the room has no picture now.", t.label))
		} else {
			c.note(i18n.T("%s: the room's picture is now %s.", t.label, avatarName(picture)))
		}
		return map[string]any{"avatar": picture}, nil
	})
}

// roomArgs takes the rooms of a command from --room and from the remaining
// arguments, so both 'tolk members --room x' and 'tolk members x' work.
func roomArgs(rooms *list, positional []string) []string {
	return append(append([]string{}, *rooms...), positional...)
}

// online asks the stone who is online in a room, by member ID: it joins the
// room on the socket unseen, so the command doesn't show its person as
// online, reads who is present and leaves. False if it can't be told,
// as for a room the account isn't in.
func (c *command) online(ctx context.Context, t target) (map[string]bool, bool) {
	var conn *socket.Client
	err := t.stone.Do(ctx, func(token string) error {
		var err error
		conn, err = socket.Dial(ctx, t.stone.URL, token)
		return err
	})
	if err != nil {
		return nil, false
	}
	defer conn.Close()
	joined, err := conn.Join(ctx, t.room.ID, true)
	if err != nil {
		return nil, false
	}
	_ = conn.Leave(ctx, t.room.ID)
	present := map[string]bool{}
	for _, memberID := range joined.Present {
		present[memberID] = true
	}
	return present, true
}

// members lists the members of rooms, with their access and who is online.
func (c *command) members(args []string) error {
	fs := c.flags("members")
	rooms := roomFlag(fs)
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	names := roomArgs(rooms, pos)
	if len(names) == 0 {
		return usage("Use 'tolk members --room <room>'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	several := len(names) > 1 || strings.HasPrefix(names[0], "@") || strings.HasSuffix(names[0], "*")
	return c.each(ctx, svc, names, func(t target) (any, error) {
		trustees, err := t.stone.Members(ctx, t.room)
		if err != nil {
			return nil, err
		}
		// In a chamber: an administrator who looks repairs, and each key is
		// shown by its fingerprint.
		keys, trustees, err := c.chamberMembers(ctx, t.stone, t.room, trustees)
		if err != nil {
			return nil, err
		}
		present, told := c.online(ctx, t)
		// The column of who is online: the word, or as much room.
		light := func(memberID string) string {
			if !told {
				return ""
			}
			word := i18n.Rune("online")
			if !present[memberID] {
				word = ""
			}
			return "  " + pad(word, len([]rune(i18n.Rune("online"))))
		}
		out := []map[string]any{}
		width, accessWidth := 4, 4
		for _, m := range trustees {
			entry := map[string]any{"id": m.MemberID, "name": m.AccountName, "avatar": m.Avatar, "access": accessWord(m.TrustLevel), "added": iso(m.Added)}
			if told {
				entry["online"] = present[m.MemberID]
			}
			if key, ok := keys[m.MemberID]; ok {
				entry["fingerprint"], entry["keyChanged"] = key.Fingerprint, key.Changed
				if key.State != "" {
					entry["key"] = key.State
				}
			}
			out = append(out, entry)
			width = max(width, len([]rune(m.AccountName)))
			accessWidth = max(accessWidth, len([]rune(accessText(m.TrustLevel))))
		}
		if !c.json() {
			if several {
				c.say("%s:", t.label)
			}
			for _, m := range trustees {
				if key, ok := keys[m.MemberID]; ok {
					c.say("%s  %s%s  %s", pad(m.AccountName, width), pad(accessText(m.TrustLevel), accessWidth), light(m.MemberID), keyNote(key))
				} else {
					c.say("%s", strings.TrimRight(pad(m.AccountName, width)+"  "+pad(accessText(m.TrustLevel), accessWidth)+light(m.MemberID), " "))
				}
			}
		}
		result := map[string]any{"members": out}
		if keys != nil {
			result["chamber"] = true
		}
		return result, nil
	})
}

// adminText names an administrator's role, and says if it is the first.
func adminText(user client.Member) string {
	if user.FirstAdministrator {
		return i18n.Rune("System Administrator") + " (" + i18n.Rune("first admin") + ")"
	}
	return i18n.Rune("System Administrator")
}

// users lists all users of a stone.
func (c *command) users(args []string) error {
	fs := c.flags("users")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) > 0 {
		return usage("Use 'tolk users [--stone <name>]'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, *stoneName)
	if err != nil {
		return err
	}
	users, err := stone.Users(ctx)
	if err != nil {
		return err
	}
	if c.json() {
		out := []map[string]any{}
		for _, u := range users {
			out = append(out, map[string]any{"stone": stone.Name, "id": u.MemberID, "name": u.AccountName, "avatar": u.Avatar, "systemAdministrator": u.MemberRole == client.RoleAdmin, "firstAdministrator": u.FirstAdministrator, "added": iso(u.Added)})
		}
		return c.printJSON(map[string]any{"users": out})
	}
	width := 4
	for _, u := range users {
		width = max(width, len([]rune(u.AccountName)))
	}
	for _, u := range users {
		role := ""
		if u.MemberRole == client.RoleAdmin {
			role = adminText(u)
		}
		c.say("%s", strings.TrimRight(pad(u.AccountName, width)+"  "+role, " "))
	}
	return nil
}

// user handles 'tolk user <name>', 'tolk user new <name> [--admin]',
// 'tolk user role <name> admin|user' and 'tolk user rm <name>'.
func (c *command) user(args []string) error {
	fs := c.flags("user")
	stoneName := fs.String("stone", "", "")
	fromStdin := fs.Bool("passphrase-stdin", false, "")
	asAdmin := fs.Bool("admin", false, "")
	yes := fs.Bool("yes", false, "")
	fs.BoolVar(yes, "y", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	action, name, role := "show", "", client.RoleStandard
	switch {
	case len(pos) == 1:
		name = pos[0]
	case len(pos) == 2 && (pos[0] == "new" || pos[0] == "rm" || pos[0] == "delete"):
		action, name = pos[0], pos[1]
	case len(pos) == 3 && pos[0] == "role" && (pos[2] == "admin" || pos[2] == "user"):
		action, name = pos[0], pos[1]
		if pos[2] == "admin" {
			role = client.RoleAdmin
		}
	default:
		return usage("Use 'tolk user <name>', 'tolk user new <name> [--admin]', 'tolk user role <name> admin|user' or 'tolk user rm <name>'.")
	}
	if *asAdmin {
		if action != "new" {
			return usage("--admin is for 'tolk user new'.")
		}
		role = client.RoleAdmin
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, *stoneName)
	if err != nil {
		return err
	}
	switch action {
	case "new":
		passphrase, err := c.newPassphrase(*fromStdin, i18n.T("Passphrase for %s", name))
		if err != nil {
			return err
		}
		var id string
		err = stone.Do(ctx, func(token string) error {
			var err error
			id, err = stone.Client.CreateMember(ctx, token, name, passphrase, role)
			return err
		})
		if err != nil {
			return err
		}
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "id": id, "name": name, "systemAdministrator": role == client.RoleAdmin})
		}
		if role == client.RoleAdmin {
			c.note(i18n.T("Made the administrator %q on %s.", name, stone.Name))
		} else {
			c.note(i18n.T("Made the user %q on %s.", name, stone.Name))
		}
		return nil
	case "role":
		user, err := stone.User(ctx, name)
		if err != nil {
			return err
		}
		err = stone.Do(ctx, func(token string) error { return stone.Client.SetRole(ctx, token, user.MemberID, role) })
		if err != nil {
			return err
		}
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "id": user.MemberID, "name": user.AccountName, "systemAdministrator": role == client.RoleAdmin})
		}
		if role == client.RoleAdmin {
			c.note(i18n.T("%s is an administrator on %s.", user.AccountName, stone.Name))
		} else {
			c.note(i18n.T("%s is a user on %s.", user.AccountName, stone.Name))
		}
		return nil
	case "rm", "delete":
		user, err := stone.User(ctx, name)
		if err != nil {
			return err
		}
		ok, err := c.confirm(*yes, i18n.Rune("Delete {name}? Their account and memberships are removed for good; the rooms and their files stay.", "name", user.AccountName))
		if err != nil {
			return err
		}
		if !ok {
			return exit.New(exit.Other, "%s", i18n.T("Nothing was deleted."))
		}
		err = stone.Do(ctx, func(token string) error { return stone.Client.DeleteMember(ctx, token, user.MemberID) })
		if err != nil {
			return err
		}
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "id": user.MemberID, "name": user.AccountName, "deleted": true})
		}
		c.note(i18n.T("Deleted the user %q on %s.", user.AccountName, stone.Name))
		return nil
	}
	// The rooms of a user.
	user, err := stone.User(ctx, name)
	if err != nil {
		return err
	}
	var detail *client.FetchMemberResponse
	err = stone.Do(ctx, func(token string) error {
		var err error
		detail, err = stone.Client.Member(ctx, token, user.MemberID)
		return err
	})
	if err != nil {
		return err
	}
	circles := detail.Circles
	sort.Slice(circles, func(i, j int) bool {
		return strings.ToLower(circles[i].CircleName) < strings.ToLower(circles[j].CircleName)
	})
	if c.json() {
		rooms := []map[string]any{}
		for _, circle := range circles {
			rooms = append(rooms, map[string]any{"id": circle.CircleID, "name": circle.CircleName, "avatar": circle.Avatar})
		}
		return c.printJSON(map[string]any{"stone": stone.Name, "id": user.MemberID, "name": user.AccountName, "avatar": user.Avatar,
			"systemAdministrator": user.MemberRole == client.RoleAdmin, "firstAdministrator": user.FirstAdministrator, "added": iso(user.Added), "rooms": rooms})
	}
	c.say("%s", user.AccountName)
	if user.MemberRole == client.RoleAdmin {
		c.say("  %s", adminText(*user))
	}
	if user.Avatar != "" {
		c.say("  %s", avatarName(user.Avatar))
	}
	c.say("%s:", i18n.Rune("Rooms"))
	for _, circle := range circles {
		c.say("  %s", circle.CircleName)
	}
	return nil
}

// membership handles 'tolk add', 'tolk remove' and 'tolk access'.
func (c *command) membership(verb string, args []string) error {
	fs := c.flags(verb)
	rooms := roomFlag(fs)
	access := fs.String("access", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	level := ""
	switch verb {
	case "add":
		if len(pos) != 1 || len(*rooms) == 0 {
			return usage("Use 'tolk add <user> --room <room> [--access read|write|admin]'.")
		}
		if level, err = trustLevel(firstNonEmpty(*access, "write")); err != nil {
			return err
		}
	case "remove":
		if len(pos) != 1 || len(*rooms) == 0 {
			return usage("Use 'tolk remove <user> --room <room>', or 'tolk remove stone <name>'.")
		}
	case "access":
		if len(pos) != 2 || len(*rooms) == 0 {
			return usage("Use 'tolk access <user> read|write|admin --room <room>'.")
		}
		if level, err = trustLevel(pos[1]); err != nil {
			return err
		}
	}
	name := pos[0]
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, *rooms, func(t target) (any, error) {
		user, err := t.stone.User(ctx, name)
		if err != nil {
			return nil, err
		}
		// In a chamber, who is added gets its key, if they have keys; and
		// when someone is removed, the chamber gets a new one.
		state, err := t.stone.RoomState(ctx, t.room.ID)
		if err != nil {
			return nil, err
		}
		secret := ""
		if verb == "add" && state.Chamber() {
			if state.Current == nil {
				return nil, exit.New(exit.Auth, "%s", state.Refusal())
			}
			if secret, err = t.stone.SecretFor(ctx, t.room.ID, user.MemberID, user.PublicKey); err != nil {
				return nil, err
			}
		}
		err = t.stone.Do(ctx, func(token string) error {
			switch verb {
			case "add":
				return t.stone.Client.AddTrusteeWithSecret(ctx, token, t.room.ID, user.MemberID, level, secret)
			case "remove":
				return t.stone.Client.RemoveTrustee(ctx, token, t.room.ID, user.MemberID)
			default:
				return t.stone.Client.AlterTrustee(ctx, token, t.room.ID, user.MemberID, level)
			}
		})
		if err != nil {
			return nil, err
		}
		if verb == "remove" && state.Kind == service.KindReady {
			// The one who was removed had the key of the chamber.
			remaining, err := t.stone.Members(ctx, t.room)
			if err != nil {
				return nil, err
			}
			if err := t.stone.ChangeKey(ctx, t.room.ID, remaining); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: the chamber has a new key, for those who are left.", t.label))
		}
		if verb == "add" && state.Chamber() && secret == "" {
			c.note(i18n.T("%s: %s has no keys for chambers yet, and gets the key of this one when an administrator next looks at its members.", t.label, user.AccountName))
		}
		switch verb {
		case "add":
			c.note(i18n.T("%s: added %s (%s).", t.label, user.AccountName, accessText(level)))
		case "remove":
			c.note(i18n.T("%s: removed %s.", t.label, user.AccountName))
		default:
			c.note(i18n.T("%s: %s now has the access \"%s\".", t.label, user.AccountName, accessText(level)))
		}
		return map[string]any{"user": user.AccountName, "access": accessWord(level)}, nil
	})
}

// leave takes the account out of rooms, with Rune's warnings.
func (c *command) leave(args []string) error {
	fs := c.flags("leave")
	rooms := roomFlag(fs)
	yes := fs.Bool("yes", false, "")
	fs.BoolVar(yes, "y", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	names := roomArgs(rooms, pos)
	if len(names) == 0 {
		return usage("Use 'tolk leave --room <room>'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, names, func(t target) (any, error) {
		members, err := t.stone.Members(ctx, t.room)
		if err != nil {
			return nil, err
		}
		var me *client.Trustee
		otherAdmins, others := 0, 0
		for i := range members {
			if strings.EqualFold(members[i].AccountName, t.stone.Account) {
				me = &members[i]
				continue
			}
			others++
			if members[i].TrustLevel == client.TrustAdmin || members[i].TrustLevel == client.TrustSysop {
				otherAdmins++
			}
		}
		if me == nil {
			return nil, exit.New(exit.NotFound, "%s", i18n.Rune("You are no longer in this room."))
		}
		warning := i18n.Rune("You lose access to its chat and files, until someone invites you again.")
		if others == 0 {
			warning = i18n.Rune("You are the only member. The room and its files stay, but nobody can open them until the System Administrator deletes the room or adds someone.")
		} else if t.room.Administers() && otherAdmins == 0 {
			warning = i18n.Rune("You are the last administrator. Without one, only the System Administrator can manage the room.")
		}
		ok, err := c.confirm(*yes, i18n.Rune("Leave {room}?", "room", t.room.Name)+" "+warning)
		if err != nil {
			return nil, err
		}
		if !ok {
			return nil, exit.New(exit.Other, "%s", i18n.T("Still in the room."))
		}
		err = t.stone.Do(ctx, func(token string) error {
			return t.stone.Client.RemoveTrustee(ctx, token, t.room.ID, me.MemberID)
		})
		if err != nil {
			return nil, err
		}
		c.note(i18n.T("%s: you left the room.", t.label))
		return map[string]any{"left": true}, nil
	})
}

// avatar shows or sets the own picture on a stone.
func (c *command) avatar(args []string) error {
	fs := c.flags("avatar")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) > 1 {
		return usage("Use 'tolk avatar [<picture>|none]'. The pictures: %s.", strings.Join(userAvatars, ", "))
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, *stoneName)
	if err != nil {
		return err
	}
	if len(pos) == 0 {
		me, err := stone.Me(ctx)
		if err != nil {
			return err
		}
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "avatar": me.Avatar, "choices": userAvatars})
		}
		if me.Avatar == "" {
			c.say("%s", i18n.T("No picture. The pictures: %s.", strings.Join(userAvatars, ", ")))
		} else {
			c.say("%s (%s)", me.Avatar, avatarName(me.Avatar))
		}
		return nil
	}
	picture := strings.ToLower(pos[0])
	if picture == "none" {
		picture = ""
	} else if !contains(userAvatars, picture) {
		return usage("%q is not a picture for a user. The pictures: %s.", pos[0], strings.Join(userAvatars, ", "))
	}
	err = stone.Do(ctx, func(token string) error { return stone.Client.SetAvatar(ctx, token, picture) })
	if err != nil {
		return err
	}
	if c.json() {
		return c.printJSON(map[string]any{"stone": stone.Name, "avatar": picture})
	}
	if picture == "" {
		c.note(i18n.T("You have no picture on %s now.", stone.Name))
	} else {
		c.note(i18n.T("Your picture on %s is now %s.", stone.Name, avatarName(picture)))
	}
	return nil
}
