package commands

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"io"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
	"github.com/HaugrNet/eds/eds-cli/internal/socket"
)

// Features (FEATURES.md): what a room can have beside its files. Here: the features
// a stone offers, the features of a room, and a general command for the
// entries of any feature, so a script can use one Tolk doesn't know.

// stoneFeatures is 'tolk stone features [offer|withdraw <feature>]'.
func (c *command) stoneFeatures(args []string) error {
	verb := "list"
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		switch strings.ToLower(args[0]) {
		case "list", "offer", "withdraw":
			verb, args = strings.ToLower(args[0]), args[1:]
		}
	}
	fs := c.flags("stone features")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if (verb == "list" && len(pos) != 0) || (verb != "list" && len(pos) != 1) {
		return usage("Use 'tolk stone features [--stone <name>]', 'tolk stone features offer <feature|all>' or 'tolk stone features withdraw <feature>'.")
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
	switch verb {
	case "offer":
		names := []string{pos[0]}
		if pos[0] == "all" {
			names = features.Names
		}
		for _, name := range names {
			declaration := features.Declaration(name)
			if declaration == nil {
				return usage("Tolk doesn't know the feature %q. It knows: %s.", name, strings.Join(features.Names, ", "))
			}
			if err := stone.Do(ctx, func(token string) error { return stone.Client.OfferFeature(ctx, token, name, string(declaration)) }); err != nil {
				return err
			}
			c.note(i18n.T("%s now offers %s (version %d).", stone.Name, features.Label(name), features.Version(name)))
		}
	case "withdraw":
		if err := stone.Do(ctx, func(token string) error { return stone.Client.WithdrawFeature(ctx, token, pos[0]) }); err != nil {
			return err
		}
		c.note(i18n.T("%s no longer offers %s.", stone.Name, features.Label(pos[0])))
	}
	var offered []client.Feature
	if err := stone.Do(ctx, func(token string) error {
		var err error
		offered, err = stone.Client.Features(ctx, token)
		return err
	}); err != nil {
		return err
	}
	versions := map[string]int{}
	for _, feature := range offered {
		versions[feature.Name] = feature.Version
	}
	names := append([]string{}, features.Names...)
	for _, feature := range offered {
		if !contains(names, feature.Name) {
			names = append(names, feature.Name)
		}
	}
	if c.json() {
		list := []map[string]any{}
		for _, name := range names {
			version, offered := versions[name]
			list = append(list, map[string]any{"feature": name, "offered": offered, "version": version, "tolk": features.Version(name)})
		}
		return c.printJSON(map[string]any{"stone": stone.Name, "features": list})
	}
	if verb != "list" {
		return nil
	}
	for _, name := range names {
		version, ok := versions[name]
		told := i18n.Rune("not offered")
		if ok && features.Version(name) > version {
			told = i18n.T("offered, version %d; Tolk has version %d", version, features.Version(name))
		} else if ok {
			told = i18n.T("offered, version %d", version)
		}
		c.say("%s  %s", pad(features.Label(name), 10), told)
	}
	return nil
}

var featureStates = map[string]string{"on": client.FeatureOn, "off": client.FeatureOff, "readonly": client.FeatureReadonly, "read-only": client.FeatureReadonly}

func stateWord(state string) string {
	switch state {
	case client.FeatureOn:
		return i18n.Rune("On")
	case client.FeatureReadonly:
		return i18n.Rune("Read-only")
	case client.FeatureOff:
		return i18n.Rune("Off")
	}
	return i18n.Rune("Not here")
}

// roomFeatures is 'tolk features [on|off|readonly|remove <feature>] --room <room>'.
func (c *command) roomFeatures(args []string) error {
	verb, feature := "list", ""
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		word := strings.ToLower(args[0])
		if _, ok := featureStates[word]; ok || word == "remove" || word == "list" {
			verb, args = word, args[1:]
		}
	}
	fs := c.flags("features")
	rooms := roomFlag(fs)
	yes := fs.Bool("yes", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if verb != "list" {
		if len(pos) == 0 {
			return usage("Use 'tolk features --room <room>', or 'tolk features on|off|readonly|remove <feature> --room <room>'.")
		}
		feature, pos = pos[0], pos[1:]
	}
	names := roomArgs(rooms, pos)
	if len(names) == 0 {
		return usage("Use 'tolk features --room <room>', or 'tolk features on|off|readonly|remove <feature> --room <room>'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	if verb == "remove" {
		ok, err := c.confirm(*yes, i18n.T("Remove %s from %s? Everything it has there is deleted, for everyone.", features.Label(feature), strings.Join(names, ", ")))
		if err != nil {
			return err
		}
		if !ok {
			return exit.New(exit.Other, "%s", i18n.T("Nothing was removed."))
		}
	}
	several := len(names) > 1 || strings.HasPrefix(names[0], "@") || strings.HasSuffix(names[0], "*")
	return c.each(ctx, svc, names, func(t target) (any, error) {
		switch verb {
		case "list":
		case "remove":
			if err := t.stone.Do(ctx, func(token string) error { return t.stone.Client.RemoveFeature(ctx, token, t.room.ID, feature) }); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: removed %s.", t.label, features.Label(feature)))
		default:
			if err := c.switchFeature(ctx, t.stone, t.room.ID, feature, featureStates[verb]); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: %s is now %s.", t.label, features.Label(feature), stateWord(featureStates[verb])))
		}
		var have []client.CircleFeature
		if err := t.stone.Do(ctx, func(token string) error {
			var err error
			have, err = t.stone.Client.CircleFeatures(ctx, token, t.room.ID)
			return err
		}); err != nil {
			return nil, err
		}
		list := []map[string]any{}
		for _, feature := range have {
			list = append(list, map[string]any{"feature": feature.Name, "state": strings.ToLower(feature.State), "version": feature.Version})
		}
		if !c.json() && verb == "list" {
			if several {
				c.say("%s:", t.label)
			}
			if len(have) == 0 {
				c.say("%s", i18n.T("No features. Switch one on with 'tolk features on <feature> --room <room>'."))
			}
			for _, feature := range have {
				note := ""
				if features.Declaration(feature.Name) == nil {
					note = "  " + i18n.T("(Tolk can't show this feature; 'tolk entry' reads its entries)")
				}
				c.say("%s  %s%s", pad(features.Label(feature.Name), 10), stateWord(feature.State), note)
			}
		}
		return map[string]any{"features": list}, nil
	})
}

// switchFeature sets the state of a feature in a room. A feature Tolk knows which
// the stone doesn't offer yet is offered first, if the account is the
// System Administrator: as Rune offers the chat at first use.
func (c *command) switchFeature(ctx context.Context, stone *service.Stone, circleID, feature, state string) error {
	set := func() error {
		return stone.Do(ctx, func(token string) error { return stone.Client.SwitchFeature(ctx, token, circleID, feature, state) })
	}
	err := set()
	var e *exit.Error
	if err == nil || !errors.As(err, &e) || e.ReturnCode != 404 || features.Declaration(feature) == nil {
		return err
	}
	if offer := stone.Do(ctx, func(token string) error {
		return stone.Client.OfferFeature(ctx, token, feature, string(features.Declaration(feature)))
	}); offer != nil {
		return exit.New(exit.NotFound, "%s", i18n.T("The stone %s doesn't offer %s. Its System Administrator can, with 'tolk stone features offer %s'.", stone.Name, features.Label(feature), feature))
	}
	return set()
}

// --- Entries, for the commands of the features ------------------------------------------

func (c *command) listEntries(ctx context.Context, t target, feature string, query client.EntryQuery) ([]client.Entry, error) {
	var found []client.Entry
	err := t.stone.Do(ctx, func(token string) error {
		var err error
		found, err = t.stone.Client.AllEntries(ctx, token, t.room.ID, feature, query)
		return err
	})
	for i := range found {
		found[i].CircleID = t.room.ID
	}
	return found, err
}

func (c *command) addEntry(ctx context.Context, t target, feature, kind string, body any, about, name string) (*client.Entry, error) {
	content, err := features.Encode(ctx, t.stone, t.room.ID, feature, kind, body)
	if err != nil {
		return nil, err
	}
	in := client.EntryRequest{Kind: kind, About: about, Content: &content}
	if name != "" {
		in.Name = &name
	}
	var entry *client.Entry
	err = t.stone.Do(ctx, func(token string) error {
		var err error
		entry, err = t.stone.Client.AddEntry(ctx, token, t.room.ID, feature, in)
		return err
	})
	return entry, err
}

func (c *command) changeEntry(ctx context.Context, t target, feature, kind, entryID string, body any, name string) (*client.Entry, error) {
	content, err := features.Encode(ctx, t.stone, t.room.ID, feature, kind, body)
	if err != nil {
		return nil, err
	}
	in := client.EntryRequest{Content: &content}
	if name != "" {
		in.Name = &name
	}
	var entry *client.Entry
	err = t.stone.Do(ctx, func(token string) error {
		var err error
		entry, err = t.stone.Client.ChangeEntry(ctx, token, t.room.ID, feature, entryID, in)
		return err
	})
	return entry, err
}

func (c *command) removeEntry(ctx context.Context, t target, feature, entryID string) error {
	return t.stone.Do(ctx, func(token string) error {
		_, err := t.stone.Client.RemoveEntry(ctx, token, t.room.ID, feature, entryID)
		return err
	})
}

// names maps the IDs of the members of a room to their names.
func (c *command) memberNames(ctx context.Context, t target) (map[string]string, error) {
	members, err := t.stone.Members(ctx, t.room)
	if err != nil {
		return nil, err
	}
	names := map[string]string{}
	for _, member := range members {
		names[member.MemberID] = member.AccountName
	}
	return names, nil
}

// oneRoom runs a command of a feature for the one room it names.
func (c *command) oneRoom(rooms []string, misuse error, fn func(ctx context.Context, t target) (any, error)) error {
	if len(rooms) != 1 || strings.HasPrefix(rooms[0], "@") || strings.HasSuffix(rooms[0], "*") {
		return misuse
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	return c.each(ctx, svc, rooms, func(t target) (any, error) { return fn(ctx, t) })
}

// --- The general command ----------------------------------------------------------------

// entryJSON is an entry as 'tolk entry' prints it: what the stone notes,
// and the content as its people read it.
func (c *command) entryJSON(ctx context.Context, t target, feature string, entry *client.Entry) map[string]any {
	out := map[string]any{"id": entry.EntryID, "kind": entry.Kind, "position": entry.Position, "by": entry.AccountName, "byId": entry.MemberID,
		"added": iso(entry.Added), "closed": entry.Closed}
	if entry.About != "" {
		out["about"] = entry.About
	}
	if entry.Name != "" {
		out["name"] = entry.Name
	}
	if entry.Changed != "" {
		out["changed"] = iso(entry.Changed)
	}
	entry.CircleID = t.room.ID
	plain, err := features.Plain(ctx, t.stone, feature, entry)
	switch {
	case err != nil:
		// Damaged, of another convention, or of a key this device hasn't.
		out["unreadable"] = err.Error()
	case json.Valid(plain) && len(plain) > 0:
		out["content"] = json.RawMessage(plain)
	default:
		out["contentBase64"] = base64.StdEncoding.EncodeToString(plain)
	}
	return out
}

// entry is 'tolk entry list|add|change|remove|close|open <feature> … --room
// <room>': the entries of any feature, as JSON.
func (c *command) entry(args []string) error {
	misuse := usage("Use 'tolk entry list <feature> --room <room> [--kind <kind>] [--about <id>] [--name <prefix>]', 'tolk entry add <feature> <kind> --room <room> [--content <json>] [--about <id>] [--name <name>]', 'tolk entry change <feature> <id> --room <room> [--content <json>] [--name <name>]', or 'tolk entry remove|close|open <feature> <id> --room <room>'.")
	if len(args) == 0 {
		return misuse
	}
	verb, args := strings.ToLower(args[0]), args[1:]
	fs := c.flags("entry " + verb)
	rooms := roomFlag(fs)
	kind := fs.String("kind", "", "")
	about := fs.String("about", "", "")
	name := fs.String("name", "", "")
	content := fs.String("content", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	want := map[string]int{"list": 1, "add": 2, "change": 2, "remove": 2, "close": 2, "open": 2}[verb]
	if want == 0 || len(pos) != want {
		return misuse
	}
	feature := pos[0]
	// The content of a new or changed entry: the flag, or standard input.
	body := func() ([]byte, error) {
		if *content != "" {
			return []byte(*content), nil
		}
		if verb == "change" && *name != "" {
			return nil, nil // only the name changes
		}
		data, err := io.ReadAll(io.LimitReader(c.env.In, 1<<20))
		if err != nil {
			return nil, exit.New(exit.Other, "%v", err)
		}
		return []byte(strings.TrimSpace(string(data))), nil
	}
	return c.oneRoom(*rooms, misuse, func(ctx context.Context, t target) (any, error) {
		var entry *client.Entry
		switch verb {
		case "list":
			found, err := c.listEntries(ctx, t, feature, client.EntryQuery{Kind: *kind, About: *about, Name: *name})
			if err != nil {
				return nil, err
			}
			list := []map[string]any{}
			for i := range found {
				list = append(list, c.entryJSON(ctx, t, feature, &found[i]))
			}
			if !c.json() {
				for _, item := range list {
					line, _ := json.Marshal(item)
					c.say("%s", line)
				}
			}
			return map[string]any{"entries": list}, nil
		case "add", "change":
			plain, err := body()
			if err != nil {
				return nil, err
			}
			in := client.EntryRequest{About: *about}
			if *name != "" {
				in.Name = name
			}
			if plain != nil {
				entryKind := pos[1]
				if verb == "change" {
					// The kind of the entry says what its content is, in a chamber.
					var before *client.Entry
					if err := t.stone.Do(ctx, func(token string) error {
						var err error
						before, err = t.stone.Client.GetEntry(ctx, token, t.room.ID, feature, pos[1])
						return err
					}); err != nil {
						return nil, err
					}
					entryKind = before.Kind
				}
				sealed, err := features.EncodeRaw(ctx, t.stone, t.room.ID, feature, entryKind, plain)
				if err != nil {
					return nil, err
				}
				in.Content = &sealed
			}
			err = t.stone.Do(ctx, func(token string) error {
				var err error
				if verb == "add" {
					in.Kind = pos[1]
					entry, err = t.stone.Client.AddEntry(ctx, token, t.room.ID, feature, in)
				} else {
					entry, err = t.stone.Client.ChangeEntry(ctx, token, t.room.ID, feature, pos[1], in)
				}
				return err
			})
			if err != nil {
				return nil, err
			}
		case "remove":
			if err := c.removeEntry(ctx, t, feature, pos[1]); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: removed the entry %s.", t.label, pos[1]))
			return map[string]any{"removed": pos[1]}, nil
		default:
			err := t.stone.Do(ctx, func(token string) error {
				var err error
				entry, err = t.stone.Client.CloseEntry(ctx, token, t.room.ID, feature, pos[1], verb == "close")
				return err
			})
			if err != nil {
				return nil, err
			}
		}
		out := c.entryJSON(ctx, t, feature, entry)
		if !c.json() {
			line, _ := json.Marshal(out)
			c.say("%s", line)
		}
		return out, nil
	})
}

// socketOf opens the stone's socket and joins the room: for what has no
// operation of the API, such as a signal.
func (c *command) socketOf(ctx context.Context, t target) (*socket.Client, error) {
	var conn *socket.Client
	err := t.stone.Do(ctx, func(token string) error {
		var err error
		conn, err = socket.Dial(ctx, t.stone.URL, token)
		return err
	})
	if err != nil {
		return nil, err
	}
	// Unseen: a command doesn't show its person as online.
	if _, err := conn.Join(ctx, t.room.ID, true); err != nil {
		conn.Close()
		return nil, err
	}
	return conn, nil
}
