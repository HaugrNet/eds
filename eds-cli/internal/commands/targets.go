package commands

import (
	"context"
	"errors"
	"flag"
	"os"
	"os/signal"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/address"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/messages"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// list is a flag that may be given several times.
type list []string

func (l *list) String() string     { return strings.Join(*l, ",") }
func (l *list) Set(v string) error { *l = append(*l, v); return nil }

// roomFlag adds --room (and its alias --circle), which may be repeated.
func roomFlag(fs *flag.FlagSet) *list {
	rooms := &list{}
	fs.Var(rooms, "room", "")
	fs.Var(rooms, "circle", "")
	return rooms
}

// context returns a context that ends when the person presses Ctrl+C.
func (c *command) context() (context.Context, context.CancelFunc) {
	return signal.NotifyContext(context.Background(), os.Interrupt)
}

// service opens the container and returns the service for this command.
func (c *command) service() (*service.Service, error) {
	access, err := service.Open(c.prompt())
	if err != nil {
		return nil, err
	}
	return service.NewService(access), nil
}

// target is one resolved address: a room on a stone, and a path below it.
type target struct {
	stone *service.Stone
	room  *service.Room
	path  []string
	dir   bool   // the address ended in a slash, or names only the room
	label string // the address, for messages
}

// outcome is what happened for one target.
type outcome struct {
	Target string     `json:"target"`
	OK     bool       `json:"ok"`
	Error  *errorJSON `json:"error,omitempty"`
	Result any        `json:"result,omitempty"`
}

type errorJSON struct {
	Code          int    `json:"code"`
	Message       string `json:"message"`
	ReturnCode    int    `json:"returnCode,omitempty"`
	ReturnMessage string `json:"returnMessage,omitempty"`
}

// reported is returned when the failures were already shown, target by
// target; only the exit code is left to give.
type reported struct{ code int }

func (r reported) Error() string { return "some targets failed" }

// friendly puts an error in Tolk's words: what a stone said goes through
// Rune's table.
func (c *command) friendly(err error) *exit.Error {
	var e *exit.Error
	if !errors.As(err, &e) {
		return exit.New(exit.Other, "%v", err)
	}
	if e.ReturnCode != 0 && e.ReturnMessage != "" && e.Message == e.ReturnMessage {
		return &exit.Error{Code: e.Code, ReturnCode: e.ReturnCode, ReturnMessage: e.ReturnMessage,
			Message: messages.Friendly(e.ReturnMessage, c.locale())}
	}
	return e
}

// targets resolves an address to its room, or with stone:* to every room
// the account is in on that stone.
func (c *command) targets(ctx context.Context, svc *service.Service, a address.Address) ([]target, error) {
	stone, err := svc.Stone(ctx, a.Stone)
	if err != nil {
		return nil, err
	}
	if a.AllRooms {
		rooms, err := stone.Rooms(ctx)
		if err != nil {
			return nil, err
		}
		var out []target
		for i := range rooms {
			if rooms[i].In() {
				one := address.Address{Stone: stone.Name, Room: rooms[i].Name, Dir: true}
				out = append(out, target{stone: stone, room: &rooms[i], dir: true, label: one.String()})
			}
		}
		return out, nil
	}
	room, err := stone.Room(ctx, a.Room)
	if err != nil {
		return nil, err
	}
	a.Stone = stone.Name
	return []target{{stone: stone, room: room, path: a.Path, dir: a.Dir, label: a.String()}}, nil
}

// failure records that a target failed, and says so right away.
func (c *command) failure(label string, err error) outcome {
	e := c.friendly(err)
	if !c.json() {
		c.note("tolk: %s: %s", label, e.Message)
	}
	return outcome{Target: label, Error: &errorJSON{e.Code, e.Message, e.ReturnCode, e.ReturnMessage}}
}

// planned is one target of a command in the order it was given: resolved,
// or already failed.
type planned struct {
	target target
	failed *outcome
}

// resolveAll turns the arguments (addresses and @groups) into targets, in
// the order given. What can't be resolved is kept as a failed outcome in
// its place, so the rest still runs.
func (c *command) resolveAll(ctx context.Context, svc *service.Service, args []string) ([]planned, error) {
	addresses, err := svc.Expand(args)
	if err != nil {
		return nil, err
	}
	if len(addresses) == 0 {
		return nil, usage("Name at least one room, as [stone:]room.")
	}
	var plan []planned
	for _, a := range addresses {
		resolved, err := c.targets(ctx, svc, a)
		if err != nil {
			failed := c.failure(a.String(), err)
			plan = append(plan, planned{failed: &failed})
			continue
		}
		for _, t := range resolved {
			plan = append(plan, planned{target: t})
		}
	}
	return plan, nil
}

// finish reports the outcomes: as {"results": […]} with --output json, and
// as an error when any target failed, with the exit code they share, or 1.
func (c *command) finish(outcomes []outcome) error {
	if c.json() {
		if outcomes == nil {
			outcomes = []outcome{}
		}
		if err := c.printJSON(map[string]any{"results": outcomes}); err != nil {
			return err
		}
	}
	code := exit.OK
	for _, o := range outcomes {
		if o.Error == nil {
			continue
		}
		if code == exit.OK {
			code = o.Error.Code
		} else if code != o.Error.Code {
			code = exit.Other
		}
	}
	if code != exit.OK {
		return reported{code}
	}
	return nil
}

// each runs fn for every target the arguments name. It continues with the
// rest when one fails, reports each target, and succeeds only if all did.
func (c *command) each(ctx context.Context, svc *service.Service, args []string, fn func(t target) (any, error)) error {
	plan, err := c.resolveAll(ctx, svc, args)
	if err != nil {
		return err
	}
	var outcomes []outcome
	for _, p := range plan {
		switch {
		case p.failed != nil:
			outcomes = append(outcomes, *p.failed)
		case ctx.Err() != nil:
			outcomes = append(outcomes, c.failure(p.target.label, exit.New(exit.Other, "%s", i18n.T("Interrupted."))))
		default:
			result, err := fn(p.target)
			if err != nil {
				outcomes = append(outcomes, c.failure(p.target.label, err))
			} else {
				outcomes = append(outcomes, outcome{Target: p.target.label, OK: true, Result: result})
			}
		}
	}
	return c.finish(outcomes)
}

// confirm asks a yes-or-no question when a person is at the terminal;
// otherwise, and with --yes, the answer is yes, as scripts are never asked.
func (c *command) confirm(yes bool, question string) (bool, error) {
	if yes || !c.interactive() || c.env.ReadLine == nil {
		return true, nil
	}
	answer, err := c.env.ReadLine(question + " " + i18n.T("[y/N]"))
	if err != nil {
		return false, exit.New(exit.Other, "%v", err)
	}
	switch strings.ToLower(strings.TrimSpace(answer)) {
	case "y", "yes", "j", "ja":
		return true, nil
	}
	return false, nil
}

// newPassphrase gets a new passphrase: from standard input when asked so,
// otherwise from the person, twice.
func (c *command) newPassphrase(fromStdin bool, label string) (string, error) {
	if fromStdin {
		passphrase, err := service.ReadSecret(c.env.In)
		if err != nil || passphrase == "" {
			return "", usage("No passphrase on standard input.")
		}
		return passphrase, nil
	}
	if !c.interactive() || c.env.ReadPassword == nil {
		return "", usage("Give the passphrase on standard input with --passphrase-stdin; nobody is at the terminal to ask.")
	}
	first, err := c.env.ReadPassword(label)
	if err != nil {
		return "", exit.New(exit.Usage, "%v", err)
	}
	if first == "" {
		return "", usage("The passphrase must not be empty.")
	}
	second, err := c.env.ReadPassword(i18n.Rune("Repeat the passphrase"))
	if err != nil {
		return "", exit.New(exit.Usage, "%v", err)
	}
	if first != second {
		return "", exit.New(exit.Usage, "%s", i18n.Rune("The passphrases are not the same."))
	}
	return first, nil
}

// The access levels, as written on the command line and as Rune names them.
var accessLevels = map[string]string{"read": client.TrustRead, "write": client.TrustWrite, "admin": client.TrustAdmin}

func trustLevel(word string) (string, error) {
	level, ok := accessLevels[strings.ToLower(word)]
	if !ok {
		return "", usage("The access is read, write or admin, not %q.", word)
	}
	return level, nil
}

// accessWord is the access level for machine-readable output.
func accessWord(trust string) string {
	switch trust {
	case client.TrustRead:
		return "read"
	case client.TrustWrite:
		return "write"
	case client.TrustAdmin, client.TrustSysop:
		return "admin"
	}
	return "none"
}

// accessText is the access level in Rune's words.
func accessText(trust string) string {
	switch trust {
	case client.TrustRead:
		return i18n.Rune("Can read")
	case client.TrustWrite:
		return i18n.Rune("Can read and write")
	case client.TrustAdmin, client.TrustSysop:
		return i18n.Rune("Administrator")
	}
	return i18n.Rune("not in it")
}
