package commands

import (
	"context"
	"encoding/base64"
	"errors"
	"net/url"
	"regexp"
	"sort"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/address"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/qr"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// The invitation link is Rune's: Rune's address with the invited name and
// the one-time token in the fragment, as base64url of {"n": …, "t": …}.
const invitePrefix = service.InvitePrefix

// For a chamber the link also carries the invite secret, under which the
// room key waits; it never reaches the stone (E2E.md).
type invitation struct {
	Name   string
	Token  string
	Secret []byte
}

func invitationLink(apiURL string, inv invitation) string {
	return service.RuneRoot(apiURL) + e2e.InvitationFragment(e2e.Invitation{Name: inv.Name, Token: inv.Token, Secret: inv.Secret})
}

// parseInvitation takes an invitation link apart: the stone's API root, and
// the invitation.
func parseInvitation(link string) (string, *invitation, error) {
	root, payload, found := strings.Cut(strings.TrimSpace(link), invitePrefix)
	if !found {
		return "", nil, usage("That is not an invitation link: it has no %s.", invitePrefix)
	}
	read := e2e.ParseInvitationFragment(invitePrefix + payload)
	if read == nil || read.Name == "" || read.Token == "" {
		return "", nil, usage("The invitation link is damaged; ask for it again.")
	}
	inv := &invitation{Name: read.Name, Token: read.Token, Secret: read.Secret}
	u, err := url.Parse(root)
	if err != nil || u.Host == "" || (u.Scheme != "https" && u.Scheme != "http") {
		return "", nil, usage("The invitation link has no address of a stone.")
	}
	u.RawQuery, u.Fragment = "", ""
	// Rune may be served as …/ or …/index.html.
	if i := strings.LastIndex(u.Path, "/"); i >= 0 {
		u.Path = u.Path[:i]
	}
	u.Path += apiRoot
	return u.String(), inv, nil
}

// invite invites a new user into a room: the link, and its QR code.
func (c *command) invite(args []string) error {
	fs := c.flags("invite")
	rooms := roomFlag(fs)
	access := fs.String("access", "write", "")
	noQR := fs.Bool("no-qr", false, "")
	withQR := fs.Bool("qr", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 || len(*rooms) != 1 || strings.HasPrefix((*rooms)[0], "@") {
		return usage("Use 'tolk invite <name> --room <room> [--access read|write|admin]'.")
	}
	level, err := trustLevel(*access)
	if err != nil {
		return err
	}
	name := pos[0]
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	a, err := address.Parse((*rooms)[0])
	if err != nil || a.AllRooms {
		return usage("Name one room, as [stone:]room.")
	}
	stone, err := svc.Stone(ctx, a.Stone)
	if err != nil {
		return err
	}
	room, err := stone.Room(ctx, a.Room)
	if err != nil {
		return err
	}
	// Into a chamber the room key goes along: for a new person under an
	// invite secret, which only the link carries; for someone who has keys
	// already, wrapped to their public key.
	state, err := stone.RoomState(ctx, room.ID)
	if err != nil {
		return err
	}
	secret, inviteSecret := "", []byte(nil)
	if state.Chamber() {
		if state.Current == nil {
			return exit.New(exit.Auth, "%s", state.Refusal())
		}
		if existing, err := stone.User(ctx, name); err == nil {
			// Without keys they get the room key later, by a repair.
			if secret, err = stone.SecretFor(ctx, room.ID, existing.MemberID, existing.PublicKey); err != nil {
				return err
			}
		} else if secret, inviteSecret, err = stone.InviteSecret(ctx, room.ID, name); err != nil {
			return err
		}
	}
	var token, memberID string
	err = stone.Do(ctx, func(session string) error {
		var err error
		token, memberID, err = stone.Client.InviteWithSecret(ctx, session, name, room.ID, level, secret)
		return err
	})
	if err != nil {
		return err
	}
	if token == "" && inviteSecret != nil && memberID != "" {
		// The name had an account after all, which the list didn't show: the
		// secret under the invite secret is of no use to it. An
		// administrator's repair gives the key.
		_ = stone.Do(ctx, func(session string) error {
			return stone.Client.SetSecret(ctx, session, room.ID, memberID, "")
		})
	}
	if token == "" {
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "room": room.Name, "name": name, "invited": false, "added": true})
		}
		c.note("%s", i18n.Rune("{name} already uses Rune, and was added to the room.", "name", name))
		return nil
	}
	link := invitationLink(stone.URL, invitation{Name: name, Token: token, Secret: inviteSecret})
	if c.json() {
		return c.printJSON(map[string]any{"stone": stone.Name, "room": room.Name, "name": name, "invited": true, "link": link, "access": accessWord(level), "chamber": state.Chamber()})
	}
	c.note("%s", i18n.Rune("Invitation for {name} created.", "name", name))
	if *withQR || (c.env.ErrTTY && !*noQR) {
		if code, err := qr.Encode(link); err == nil {
			c.note("")
			c.note("%s", strings.TrimRight(code.Text(), "\n"))
			c.note("")
			c.note("%s", i18n.Rune("Let {name} scan this code with their phone, or send them the link. They choose their own passphrase and join the room.", "name", name))
		}
	}
	c.note("%s", i18n.Rune("The invitation works once and expires after 7 days. Anyone with the link can accept it, so only share it with them."))
	if inviteSecret != nil {
		c.note("%s", i18n.Rune("This is a chamber: the link also carries its key for {name}. Give it to them directly, not through someone who should not read the chamber.", "name", name))
	}
	c.say("%s", link)
	return nil
}

// accept takes an invitation: it sets the passphrase of the invited account
// and registers the stone in the container.
func (c *command) accept(args []string) error {
	fs := c.flags("accept")
	stoneName := fs.String("stone", "", "")
	fromStdin := fs.Bool("passphrase-stdin", false, "")
	makeDefault := fs.Bool("default", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 {
		return usage("Use 'tolk accept <invitation link> [--stone <name>]'.")
	}
	apiURL, inv, err := parseInvitation(pos[0])
	if err != nil {
		return err
	}
	name := *stoneName
	if name == "" {
		u, _ := url.Parse(apiURL)
		suggestion := strings.Split(u.Hostname(), ".")[0]
		if c.interactive() && c.env.ReadLine != nil {
			answer, err := c.env.ReadLine(i18n.T("Your name for this stone [%s]", suggestion))
			if err != nil {
				return exit.New(exit.Usage, "%v", err)
			}
			name = strings.TrimSpace(answer)
		}
		if name == "" {
			name = suggestion
		}
	}
	if !validStoneName(name) {
		return usage("%q can't be a stone's name. Use letters, digits, dot, dash and underscore.", name)
	}
	var accountKey []byte
	err = c.change(func(container *config.Container) error {
		if _, exists := container.Stones[name]; exists {
			return exit.New(exit.Usage, "%s", i18n.T("There is a stone named %q already. Choose another name with --stone.", name))
		}
		c.note("%s", i18n.Rune("You were invited as {name}. Choose a passphrase to join.", "name", inv.Name))
		c.note("%s", i18n.Rune("Use at least 20 random characters, or several random words. Keep it safe: nobody can recover it."))
		passphrase, err := c.newPassphrase(*fromStdin, i18n.Rune("Passphrase"))
		if err != nil {
			return err
		}
		ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
		defer cancel()
		if err := client.New(apiURL).AcceptInvitation(ctx, inv.Name, inv.Token, passphrase); err != nil {
			var e *exit.Error
			if errors.As(err, &e) && e.ReturnCode != 0 && invalidInvitation.MatchString(e.ReturnMessage) {
				return &exit.Error{Code: e.Code, ReturnCode: e.ReturnCode, ReturnMessage: e.ReturnMessage,
					Message: i18n.Rune("This invitation is no longer valid. Please ask for a new one.")}
			}
			return err
		}
		stone := config.Stone{URL: apiURL, Account: inv.Name, Passphrase: passphrase}
		if err := c.check(stone); err != nil {
			return err
		}
		if inv.Secret != nil {
			// An invitation into a chamber: the new account gets keys, and
			// the room key is taken over with the invite secret and wrapped
			// to them. Nobody else has to be online for it.
			only := config.NewContainer()
			only.Stones[name], only.Default = stone, name
			svc := service.NewService(service.Direct(only))
			defer svc.Close()
			joined, err := svc.Stone(ctx, name)
			if err != nil {
				return err
			}
			if accountKey, err = joined.CreateKeys(ctx); err != nil {
				return err
			}
			stone.AccountKey = base64.StdEncoding.EncodeToString(accountKey)
			if _, err := joined.AdoptInvitation(ctx, inv.Name, inv.Secret); err != nil {
				return err
			}
		}
		container.Stones[name] = stone
		if container.Default == "" || *makeDefault {
			container.Default = name
		}
		return nil
	})
	if accountKey != nil && !c.json() {
		// Shown also when the container couldn't be saved: the keys are on
		// the stone, and only this key opens them.
		c.showRecoveryKey(name, accountKey)
	}
	if err != nil {
		return err
	}
	if c.json() {
		out := map[string]any{"stone": name, "url": apiURL, "account": inv.Name}
		if accountKey != nil {
			out["recoveryKey"] = e2e.RecoveryKey(accountKey)
		}
		return c.printJSON(out)
	}
	c.note(i18n.T("Welcome. The stone is registered as %q (%s as %s).", name, apiURL, inv.Name))
	return nil
}

var invalidInvitation = regexp.MustCompile(`(?i)invitation|Account does not`)

func validStoneName(name string) bool { return stoneName.MatchString(name) }

// passphrase changes the passphrase of the account on a stone, and in the
// container.
func (c *command) passphrase(args []string) error {
	fs := c.flags("passphrase")
	stoneName := fs.String("stone", "", "")
	fromStdin := fs.Bool("passphrase-stdin", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) > 0 {
		return usage("Use 'tolk passphrase [--stone <name>]'.")
	}
	changed := ""
	err = c.change(func(container *config.Container) error {
		name := firstNonEmpty(*stoneName, container.Default)
		stone, ok := container.Stones[name]
		if !ok {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", name))
		}
		next, err := c.newPassphrase(*fromStdin, i18n.Rune("New passphrase"))
		if err != nil {
			return err
		}
		ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
		defer cancel()
		if err := client.New(stone.URL).ChangePassphrase(ctx, stone.Account, stone.Passphrase, next); err != nil {
			var e *exit.Error
			if errors.As(err, &e) && e.ReturnCode == 403 {
				return &exit.Error{Code: exit.Auth, ReturnCode: 403, ReturnMessage: e.ReturnMessage,
					Message: i18n.T("The stone refused the passphrase Tolk has for %s. Correct it with 'tolk update stone %s --passphrase'.", stone.Account, name)}
			}
			return err
		}
		stone.Passphrase = next
		container.Stones[name] = stone
		changed = name
		return nil
	})
	if err != nil {
		return err
	}
	if c.json() {
		return c.printJSON(map[string]any{"stone": changed, "changed": true})
	}
	c.note(i18n.T("The passphrase on %q is changed, and Tolk has the new one.", changed))
	return nil
}

// about says how a stone keeps what is in a room, in Rune's words.
func (c *command) about(args []string) error {
	fs := c.flags("about")
	stoneName := fs.String("stone", "", "")
	if _, err := parse(fs, args); err != nil {
		return err
	}
	type part = service.AboutPart
	parts := service.About()

	// This system: needs a stone, but no login; left out when there is none.
	system := map[string]any{}
	var systemLines []string
	if svc, err := c.service(); err == nil {
		defer svc.Close()
		if info, err := svc.Info(*stoneName); err == nil {
			ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
			defer cancel()
			system["stone"], system["url"] = info.Name, info.URL
			system["https"] = strings.HasPrefix(info.URL, "https://")
			if strings.HasPrefix(info.URL, "https://") {
				systemLines = append(systemLines, i18n.Rune("Your connection to this stone is encrypted (HTTPS)."))
			} else {
				systemLines = append(systemLines, i18n.Rune("Your connection to this stone is not encrypted (no HTTPS). Others on the network can read along."))
			}
			if version, err := client.New(info.URL).Version(ctx); err == nil && version.Persistent != nil {
				system["version"], system["database"], system["persistent"] = version.Version, version.Database, *version.Persistent
				if *version.Persistent {
					systemLines = append(systemLines, i18n.Rune("This is a production system. Its database ({database}) keeps the data over a restart of the stone.", "database", version.Database))
				} else {
					systemLines = append(systemLines, i18n.Rune("This is a test system. Its database ({database}) is in memory only: all data is lost when the stone stops.", "database", version.Database))
				}
			}
			parts = append(parts, part{Title: i18n.Rune("This system") + " (" + info.Name + ")", Text: systemLines})
		}
	}
	if c.json() {
		return c.printJSON(map[string]any{"tolk": Version, "about": parts, "system": system})
	}
	style := c.style()
	for i, p := range parts {
		if i > 0 {
			c.say("")
		}
		c.say("%s", style.Accent(p.Title))
		for _, text := range p.Text {
			c.say("%s", wrap(text, 78))
		}
	}
	return nil
}

// wrap breaks a text into lines of at most the given width, at spaces.
func wrap(text string, width int) string {
	var b strings.Builder
	line := 0
	for i, word := range strings.Fields(text) {
		n := len([]rune(word))
		if i > 0 {
			if line+1+n > width {
				b.WriteByte('\n')
				line = 0
			} else {
				b.WriteByte(' ')
				line++
			}
		}
		b.WriteString(word)
		line += n
	}
	return b.String()
}

// Settings of a stone that are EDS's own business, and not shown.
var internalSettings = map[string]bool{"eds.is.ready": true}

// stoneSettings lists the settings of a stone; only the System
// Administrator may.
func (c *command) stoneSettings(args []string) error {
	pos, err := parse(c.flags("stone settings"), args)
	if err != nil {
		return err
	}
	if len(pos) > 1 {
		return usage("Use 'tolk stone settings [<name>]'.")
	}
	return c.settingsOf(append(pos, "")[0], nil)
}

// stoneSet changes one setting of a stone.
func (c *command) stoneSet(args []string) error {
	fs := c.flags("stone set")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 2 {
		return usage("Use 'tolk stone set <key> <value> [--stone <name>]'.")
	}
	return c.settingsOf(*stoneName, map[string]string{pos[0]: pos[1]})
}

func (c *command) settingsOf(stoneName string, changes map[string]string) error {
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, stoneName)
	if err != nil {
		return err
	}
	var settings *client.SettingResponse
	read := func(change map[string]string) error {
		return stone.Do(ctx, func(token string) error {
			var err error
			settings, err = stone.Client.Settings(ctx, token, change)
			return err
		})
	}
	if err := read(nil); err != nil {
		return err
	}
	for key, value := range changes {
		if internalSettings[key] {
			return usage("%q is EDS's own setting and can't be changed.", key)
		}
		if _, known := settings.Settings[key]; !known {
			return exit.New(exit.NotFound, "%s", i18n.T("The stone has no setting %q. See 'tolk stone settings'.", key))
		}
		if allowed := settings.AllowedValues[key]; len(allowed) > 0 && !contains(allowed, value) {
			return usage("%q takes one of: %s.", key, strings.Join(allowed, ", "))
		}
	}
	if len(changes) > 0 {
		if err := read(changes); err != nil {
			return err
		}
	}
	shown := map[string]string{}
	for key, value := range settings.Settings {
		if !internalSettings[key] && (len(changes) == 0 || hasKey(changes, key)) {
			shown[key] = value
		}
	}
	if c.json() {
		allowed := map[string][]string{}
		for key := range shown {
			if values := settings.AllowedValues[key]; len(values) > 0 {
				allowed[key] = values
			}
		}
		return c.printJSON(map[string]any{"stone": stone.Name, "settings": shown, "allowedValues": allowed})
	}
	keys := make([]string, 0, len(shown))
	width := 0
	for key := range shown {
		keys = append(keys, key)
		width = max(width, len(key))
	}
	sort.Strings(keys)
	for _, key := range keys {
		c.say("%s  %s", pad(key, width), shown[key])
	}
	return nil
}

func hasKey(m map[string]string, key string) bool {
	_, ok := m[key]
	return ok
}

// groups lists the groups of the container.
func (c *command) groups(args []string) error {
	if _, err := parse(c.flags("groups"), args); err != nil {
		return err
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	groups, err := svc.Groups()
	if err != nil {
		return err
	}
	if c.json() {
		return c.printJSON(map[string]any{"groups": groups})
	}
	if len(groups) == 0 {
		c.note(i18n.T("No groups yet. Make one with 'tolk group set <name> <room> …'."))
		return nil
	}
	names := make([]string, 0, len(groups))
	for name := range groups {
		names = append(names, name)
	}
	sort.Strings(names)
	for _, name := range names {
		c.say("@%s", name)
		for _, member := range groups[name] {
			c.say("  %s", member)
		}
	}
	return nil
}

// group handles 'tolk group set|add|remove'. A group is a named list of
// addresses, written @name where an address is expected.
func (c *command) group(args []string) error {
	pos, err := parse(c.flags("group"), args)
	if err != nil {
		return err
	}
	if len(pos) == 0 || strings.ToLower(pos[0]) == "list" {
		return c.groups(nil)
	}
	if len(pos) < 2 {
		return usage("Use 'tolk group set <name> <room> …', 'tolk group add <name> <room> …' or 'tolk group remove <name> [<room> …]'.")
	}
	verb, name, members := strings.ToLower(pos[0]), strings.TrimPrefix(pos[1], "@"), pos[2:]
	if !validStoneName(name) {
		return usage("%q can't be a group's name. Use letters, digits, dot, dash and underscore.", name)
	}
	if (verb == "set" || verb == "add") && len(members) == 0 {
		return usage("Name at least one room, as [stone:]room.")
	}
	if verb != "set" && verb != "add" && verb != "remove" && verb != "rm" {
		return usage("Unknown group command %q. Try 'tolk group set <name> <room> …'.", pos[0])
	}
	count := 0
	err = c.change(func(container *config.Container) error {
		// Addresses are stored with their stone, so a later change of the
		// default stone doesn't change what a group means.
		var full []string
		for _, member := range members {
			a, err := address.Parse(member)
			if err != nil {
				return exit.New(exit.Usage, "%v", err)
			}
			if a.Stone == "" {
				a.Stone = container.Default
			}
			if _, ok := container.Stones[a.Stone]; !ok {
				return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", a.Stone))
			}
			full = append(full, a.String())
		}
		existing, exists := container.Groups[name]
		switch verb {
		case "set":
			container.Groups[name] = full
		case "add":
			for _, member := range full {
				if !contains(existing, member) {
					existing = append(existing, member)
				}
			}
			container.Groups[name] = existing
		default:
			if !exists {
				return exit.New(exit.NotFound, "%s", i18n.T("No group named %q.", name))
			}
			if len(full) == 0 {
				delete(container.Groups, name)
				return nil
			}
			kept := []string{}
			for _, member := range existing {
				if !contains(full, member) {
					kept = append(kept, member)
				}
			}
			container.Groups[name] = kept
		}
		count = len(container.Groups[name])
		return nil
	})
	if err != nil {
		return err
	}
	if c.json() {
		return c.printJSON(map[string]any{"group": name, "members": count})
	}
	if count == 0 {
		c.note(i18n.T("The group @%s is removed.", name))
	} else {
		c.note(i18n.T("The group @%s has %d rooms.", name, count))
	}
	return nil
}
