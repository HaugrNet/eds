package commands

import (
	"context"
	"encoding/base64"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/address"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/qr"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// Chambers (E2E.md): rooms whose content Tolk encrypts before the stone
// gets it. Here: the account's keys, the recovery key, linking a further
// device, fingerprints, and what the other commands need of them.

// keepAccountKey stores the account key of a stone's account in the
// container; that asks for the container's passphrase.
func (c *command) keepAccountKey(stoneName string, accountKey []byte) error {
	return c.change(func(container *config.Container) error {
		entry, ok := container.Stones[stoneName]
		if !ok {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", stoneName))
		}
		entry.AccountKey = base64.StdEncoding.EncodeToString(accountKey)
		container.Stones[stoneName] = entry
		return nil
	})
}

// showRecoveryKey tells the recovery key, once, with what it is for.
func (c *command) showRecoveryKey(stoneName string, accountKey []byte) {
	c.note("")
	c.note("%s", i18n.T("You now have keys for chambers on %s: rooms whose content only their people can read, not the stone.", stoneName))
	c.note("%s", i18n.T("This is your recovery key. Another device needs it once to read your chambers. Write it down and keep it safe: nobody can recover it."))
	c.note("")
	c.note("    %s", e2e.RecoveryKey(accountKey))
	c.note("")
	c.note("%s", i18n.T("Tolk keeps it in the container, and shows it again with 'tolk key recovery'."))
}

// ensureKeys makes the account's keys if it has none: the keys go to the
// stone, the account key into the container, and the recovery key is shown.
// With keys on the stone which this device hasn't, it fails and says how to
// get them. The account key is returned if it was made here, for output
// as JSON, where nothing is told beside.
func (c *command) ensureKeys(ctx context.Context, stone *service.Stone) ([]byte, error) {
	state, err := stone.LoadKeys(ctx)
	if err != nil {
		return nil, err
	}
	switch state {
	case service.KeysReady:
		return nil, nil
	case service.KeysLocked:
		return nil, exit.New(exit.Auth, "%s", i18n.T("This device does not have your keys yet. Give it your recovery key with 'tolk key unlock'."))
	}
	// The container is opened first: its passphrase is asked for before
	// anything is made, so no key is made which can't be kept.
	var made []byte
	err = c.change(func(container *config.Container) error {
		entry, ok := container.Stones[stone.Name]
		if !ok {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", stone.Name))
		}
		account, err := stone.CreateKeys(ctx)
		if err != nil {
			return err
		}
		made = account
		entry.AccountKey = base64.StdEncoding.EncodeToString(account)
		container.Stones[stone.Name] = entry
		return nil
	})
	if made != nil {
		// Shown also when the container couldn't be saved: the keys are on
		// the stone, and only this key opens them.
		c.showRecoveryKey(stone.Name, made)
	}
	return made, err
}

// chamberWarning is what is said before a chamber is made.
func (c *command) chamberWarning() {
	c.note("%s", i18n.Rune("A room with end-to-end encryption on top: the devices of the members encrypt and decrypt, so the stone never handles readable content."))
	c.note("%s", i18n.Rune("End-to-end encryption needs a key pair for every member: their keys for chambers. The app makes them once per account, with a recovery key for further devices."))
	c.note("%s", i18n.Rune("A member invited by link gets their keys and the key of the chamber on accepting."))
	c.note("%s", i18n.Rune("A member added from an existing account makes their keys first: in Rune under \"Keys for chambers\" in the account menu, in Tolk with tolk key create. The app of an administrator then gives them the key of the chamber, as soon as the administrator has it open in Rune or lists its members in Tolk."))
	c.note("%s", i18n.Rune("Every member needs an app which supports chambers: Rune or Tolk. Other apps show ciphertext, and what they write is not end-to-end encrypted."))
	c.note("%s", i18n.Rune("The choice is final: a room cannot become a chamber, nor a chamber a room."))
}

// keyStone resolves the stone of a 'tolk key' command: the one named, or
// the default.
func (c *command) keyStone(ctx context.Context, svc *service.Service, pos []string) (*service.Stone, error) {
	name := ""
	if len(pos) > 0 {
		name = strings.TrimSuffix(pos[0], ":")
	}
	return svc.Stone(ctx, name)
}

// key is 'tolk key …': the account's keys for chambers.
func (c *command) key(args []string) error {
	verb := "show"
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		switch strings.ToLower(args[0]) {
		case "show", "status", "create", "new", "recovery", "link", "unlock", "accept":
			verb, args = strings.ToLower(args[0]), args[1:]
		}
	}
	fs := c.flags("key " + verb)
	rooms := roomFlag(fs)
	noQR := fs.Bool("no-qr", false, "")
	withQR := fs.Bool("qr", false, "")
	fromStdin := fs.Bool("key-stdin", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()

	if verb == "accept" {
		return c.keyAccept(ctx, svc, pos, *rooms)
	}
	if len(pos) > 1 {
		return usage("Use 'tolk key [show|create|recovery|link|unlock] [stone]', or 'tolk key accept <user> --room <room>'.")
	}
	stone, err := c.keyStone(ctx, svc, pos)
	if err != nil {
		return err
	}
	state, err := stone.LoadKeys(ctx)
	if err != nil {
		return err
	}
	switch verb {
	case "create", "new":
		if state != service.KeysNone {
			return usage("The account on %s has keys already.", stone.Name)
		}
		made, err := c.ensureKeys(ctx, stone)
		if err != nil {
			return err
		}
		if c.json() {
			fingerprint, _ := stone.OwnFingerprint(ctx)
			return c.printJSON(map[string]any{"stone": stone.Name, "keys": "ready", "fingerprint": fingerprint, "recoveryKey": e2e.RecoveryKey(made)})
		}
		return nil
	case "unlock":
		if state == service.KeysNone {
			return usage("The account on %s has no keys yet. They are made with its first chamber, or with 'tolk key create'.", stone.Name)
		}
		if state == service.KeysReady {
			c.note(i18n.T("This device has the keys of %s already.", stone.Name))
			return nil
		}
		var typed string
		if *fromStdin {
			typed, err = service.ReadSecret(c.env.In)
		} else if c.interactive() && c.env.ReadPassword != nil {
			typed, err = c.env.ReadPassword(i18n.T("Recovery key, or the link another device shows"))
		} else {
			return exit.New(exit.Auth, "%s", i18n.T("Nobody is at the terminal to give the recovery key. Use --key-stdin."))
		}
		if err != nil {
			return exit.New(exit.Auth, "%v", err)
		}
		account, err := stone.UnlockKeys(ctx, typed)
		if err != nil {
			return err
		}
		if err := c.keepAccountKey(stone.Name, account); err != nil {
			return err
		}
		c.note(i18n.T("This device has the keys of %s now.", stone.Name))
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "keys": "ready"})
		}
		return nil
	case "recovery", "link":
		account, err := stone.AccountKey(ctx)
		if err != nil {
			return err
		}
		if account == nil {
			if state == service.KeysNone {
				return usage("The account on %s has no keys yet. They are made with its first chamber, or with 'tolk key create'.", stone.Name)
			}
			return exit.New(exit.Auth, "%s", i18n.T("This device does not have your keys yet. Give it your recovery key with 'tolk key unlock'."))
		}
		if verb == "recovery" {
			if c.json() {
				return c.printJSON(map[string]any{"stone": stone.Name, "recoveryKey": e2e.RecoveryKey(account)})
			}
			c.note(i18n.T("The recovery key of your account on %s. Whoever has it can read your chambers there: keep it safe.", stone.Name))
			c.say("%s", e2e.RecoveryKey(account))
			return nil
		}
		// The link gives a further device the keys: it is Rune's address with
		// the account key in the fragment, which never reaches a stone.
		link := service.RuneRoot(stone.URL) + e2e.DeviceLink(account)
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "link": link})
		}
		c.note(i18n.T("This link gives a further device your keys on %s. Whoever has it can read your chambers there: show it only to your own device, and to nobody else.", stone.Name))
		if *withQR || (c.env.ErrTTY && !*noQR) {
			if code, err := qr.Encode(link); err == nil {
				c.note("")
				c.note("%s", strings.TrimRight(code.Text(), "\n"))
				c.note("")
			}
		}
		c.note(i18n.T("On the other device: 'tolk key unlock', or in Rune \"Keys for chambers\", and give it the link."))
		c.say("%s", link)
		return nil
	}
	// show
	fingerprint, err := stone.OwnFingerprint(ctx)
	if err != nil {
		return err
	}
	if c.json() {
		return c.printJSON(map[string]any{"stone": stone.Name, "keys": string(state), "fingerprint": fingerprint})
	}
	switch state {
	case service.KeysNone:
		c.say("%s", i18n.T("%s: no keys for chambers yet. They are made with your first chamber, or with 'tolk key create'.", stone.Name))
	case service.KeysLocked:
		c.say("%s", i18n.T("%s: you have keys for chambers, but this device does not have them yet. Give it your recovery key with 'tolk key unlock'.", stone.Name))
		c.say("%s", i18n.T("Your fingerprint: %s", fingerprint))
	default:
		c.say("%s", i18n.T("%s: this device has your keys for chambers.", stone.Name))
		c.say("%s", i18n.T("Your fingerprint: %s", fingerprint))
		c.note("%s", i18n.Rune("Others see this fingerprint beside your name in a chamber. If you compare it with them, by voice or in person, you both know that nobody put another key in between."))
	}
	return nil
}

// keyAccept accepts the changed key of a member of a room, after the
// person has compared its fingerprint.
func (c *command) keyAccept(ctx context.Context, svc *service.Service, pos, rooms []string) error {
	if len(pos) != 1 || len(rooms) != 1 {
		return usage("Use 'tolk key accept <user> --room <room>'.")
	}
	a, err := address.Parse(rooms[0])
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
	if _, err := stone.LoadKeys(ctx); err != nil {
		return err
	}
	members, err := stone.Members(ctx, room)
	if err != nil {
		return err
	}
	for _, member := range members {
		if member.AccountName != pos[0] && member.MemberID != pos[0] {
			continue
		}
		point := e2e.ParsePublicKey(member.PublicKey)
		if point == nil {
			return usage("%s has no keys for chambers.", member.AccountName)
		}
		stone.AcceptKey(member.MemberID, member.PublicKey)
		given, err := stone.Repair(ctx, room.ID, members)
		if err != nil {
			return err
		}
		c.note(i18n.T("The key of %s with the fingerprint %s is the one this device expects now.", member.AccountName, e2e.Fingerprint(point)))
		if given > 0 {
			c.note(i18n.T("%s got the key of the chamber.", member.AccountName))
		}
		if c.json() {
			return c.printJSON(map[string]any{"user": member.AccountName, "fingerprint": e2e.Fingerprint(point), "repaired": given})
		}
		return nil
	}
	return exit.New(exit.NotFound, "%s", i18n.T("%s is not in the room %q.", pos[0], room.Name))
}

// memberKey is what is told of a member's key in a chamber: the
// fingerprint, whether it changed since this device last saw it, and, to
// administrators, whether the member has the key of the chamber.
type memberKey struct {
	Fingerprint string
	Changed     bool
	State       string // "", "none", "invited", "behind", "current"
}

// chamberMembers looks at the members of a room as a chamber: an
// administrator who sees them repairs (E2E.md, "Repair"), and each member's
// key is compared with what this device saw before. Nil for a room.
func (c *command) chamberMembers(ctx context.Context, stone *service.Stone, room *service.Room, members []client.Trustee) (map[string]memberKey, []client.Trustee, error) {
	state, err := stone.RoomState(ctx, room.ID)
	if err != nil || !state.Chamber() {
		return nil, members, err
	}
	if given, err := stone.Repair(ctx, room.ID, members); err == nil && given > 0 {
		if members, err = stone.Members(ctx, room); err != nil {
			return nil, nil, err
		}
	}
	me, _ := stone.MemberID(ctx)
	keys := map[string]memberKey{}
	for _, member := range members {
		point := e2e.ParsePublicKey(member.PublicKey)
		key := memberKey{}
		if point != nil {
			key.Fingerprint = e2e.Fingerprint(point)
			key.Changed = member.MemberID != me && stone.CheckKey(member.MemberID, member.PublicKey) == service.KeyChanged
		}
		if room.Administers() && state.Current != nil {
			key.State = service.MemberKeyState(member, state.Current.Version)
		}
		keys[member.MemberID] = key
	}
	return keys, members, nil
}

// keyNote is what is said beside a member of a chamber.
func keyNote(key memberKey) string {
	parts := []string{}
	if key.Fingerprint != "" {
		parts = append(parts, key.Fingerprint)
	}
	switch key.State {
	case "invited":
		parts = append(parts, i18n.Rune("invited, not joined yet"))
	case "none":
		parts = append(parts, i18n.Rune("no keys yet"))
	case "behind":
		parts = append(parts, i18n.Rune("waits for the key"))
	}
	if key.Changed {
		parts = append(parts, i18n.T("THE KEY CHANGED: compare the fingerprint, then 'tolk key accept'"))
	}
	return strings.Join(parts, " · ")
}

// switchFeatures switches the features of a new room on; one the stone doesn't
// offer is left out, and told.
func (c *command) switchFeatures(ctx context.Context, stone *service.Stone, circleID string, features []string) []string {
	var on []string
	for _, feature := range features {
		if err := c.switchFeature(ctx, stone, circleID, feature, client.FeatureOn); err != nil {
			c.note(i18n.T("The feature %q wasn't switched on: %v", feature, c.friendly(err).Message))
			continue
		}
		on = append(on, feature)
	}
	return on
}
