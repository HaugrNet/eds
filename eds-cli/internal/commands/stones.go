package commands

import (
	"context"
	"errors"
	"net/url"
	"regexp"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// A stone's name is the person's own short word for it.
var stoneName = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$`)

const apiRoot = "/api/v1"

// normaliseURL turns what a person types into a stone's API root: a bare
// host gets https://, and a URL without a path gets /api/v1.
func normaliseURL(raw string) (string, error) {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return "", usage("The URL must not be empty.")
	}
	if !strings.Contains(raw, "://") {
		raw = "https://" + raw
	}
	u, err := url.Parse(raw)
	if err != nil || u.Host == "" || (u.Scheme != "https" && u.Scheme != "http") {
		return "", usage("%q is not a URL of a stone.", raw)
	}
	u.Path = strings.TrimRight(u.Path, "/")
	if u.Path == "" {
		u.Path = apiRoot
	}
	u.RawQuery, u.Fragment = "", ""
	return u.String(), nil
}

// ask gets a value from a flag, or from the person.
func (c *command) ask(value, label, flagName string) (string, error) {
	if value != "" {
		return value, nil
	}
	if !c.interactive() || c.env.ReadLine == nil {
		return "", usage("Give --%s; nobody is at the terminal to ask.", flagName)
	}
	answer, err := c.env.ReadLine(label)
	if err != nil {
		return "", exit.New(exit.Usage, "%v", err)
	}
	return strings.TrimSpace(answer), nil
}

// stonePassphrase gets the passphrase of the account on a stone: from
// standard input when asked so, otherwise from the person. Never from a flag
// or an environment variable.
func (c *command) stonePassphrase(fromStdin bool, label string) (string, error) {
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
	passphrase, err := c.env.ReadPassword(label)
	if err != nil {
		return "", exit.New(exit.Usage, "%v", err)
	}
	if passphrase == "" {
		return "", usage("The passphrase must not be empty.")
	}
	return passphrase, nil
}

// check logs in to the stone once and out again, to be sure of the entry.
func (c *command) check(stone config.Stone) error {
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()
	cl := client.New(stone.URL)
	if _, err := cl.Version(ctx); err != nil {
		var e *exit.Error
		if errors.As(err, &e) && e.Code == exit.NotFound && !strings.HasSuffix(stone.URL, apiRoot) {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone answers at %s. A stone's URL usually ends in %s.", stone.URL, apiRoot))
		}
		return err
	}
	token, err := cl.Login(ctx, stone.Account, stone.Passphrase)
	if err != nil {
		var e *exit.Error
		if errors.As(err, &e) && e.Code == exit.Auth {
			return exit.New(exit.Auth, "%s", i18n.T("The stone refused the account or the passphrase."))
		}
		return err
	}
	_ = cl.Logout(ctx, token)
	return nil
}

// change opens the container, applies a change, seals and saves it, and
// tells a running agent.
func (c *command) change(apply func(*config.Container) error) error {
	container, passphrase, err := service.UnlockWithPassphrase(c.prompt())
	if err != nil {
		return err
	}
	if err := apply(container); err != nil {
		return err
	}
	file, err := config.Load()
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	file.Container, err = config.Seal(container, passphrase, file.Container.Iterations)
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if err := config.Save(file); err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if agent.Running() {
		if _, err := agent.Ask(agent.Request{Op: "load", Container: container}); err != nil {
			c.note(i18n.T("Saved, but the agent still has the old stones: %v. Run 'tolk logout' and 'tolk login'.", err))
		}
	}
	return nil
}

func (c *command) registerStone(args []string) error {
	fs := c.flags("register stone")
	rawURL := fs.String("url", "", "")
	account := fs.String("account", "", "")
	fromStdin := fs.Bool("passphrase-stdin", false, "")
	makeDefault := fs.Bool("default", false, "")
	noCheck := fs.Bool("no-check", false, "")
	statusKey := fs.Bool("status-key", false, "")
	statusKeyStdin := fs.Bool("status-key-stdin", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 {
		return usage("Use 'tolk register stone <name>'.")
	}
	name := pos[0]
	if !stoneName.MatchString(name) {
		return usage("%q can't be a stone's name. Use letters, digits, dot, dash and underscore.", name)
	}
	var stone config.Stone
	if stone.URL, err = c.ask(*rawURL, i18n.T("URL of the stone"), "url"); err != nil {
		return err
	}
	if stone.URL, err = normaliseURL(stone.URL); err != nil {
		return err
	}
	if *statusKey || *statusKeyStdin {
		// A stone which is only looked after: kept with a status key, which
		// reads its status and nothing else, and without an account.
		if stone.StatusKey, err = c.stonePassphrase(*statusKeyStdin, i18n.T("Status key of %s", name)); err != nil {
			return err
		}
		if !*noCheck {
			ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
			defer cancel()
			if _, err := client.New(stone.URL).Status(ctx, stone.StatusKey, 0, false); err != nil {
				return err
			}
		}
		err = c.change(func(container *config.Container) error {
			if _, exists := container.Stones[name]; exists {
				return exit.New(exit.Usage, "%s", i18n.T("There is a stone named %q already. Use 'tolk update stone %s'.", name, name))
			}
			container.Stones[name] = stone
			return nil
		})
		if err != nil {
			return err
		}
		c.note(i18n.T("Registered the stone %q (%s), to look after: 'tolk status' tells how it is.", name, stone.URL))
		return nil
	}
	if stone.Account, err = c.ask(*account, i18n.T("Account name"), "account"); err != nil {
		return err
	}
	if stone.Account == "" {
		return usage("The account name must not be empty.")
	}
	if stone.Passphrase, err = c.stonePassphrase(*fromStdin, i18n.T("Passphrase of %s on %s", stone.Account, name)); err != nil {
		return err
	}
	if !*noCheck {
		if err := c.check(stone); err != nil {
			return err
		}
	}
	err = c.change(func(container *config.Container) error {
		if _, exists := container.Stones[name]; exists {
			return exit.New(exit.Usage, "%s", i18n.T("There is a stone named %q already. Use 'tolk update stone %s'.", name, name))
		}
		container.Stones[name] = stone
		if container.Default == "" || *makeDefault {
			container.Default = name
		}
		return nil
	})
	if err != nil {
		return err
	}
	c.note(i18n.T("Registered the stone %q (%s as %s).", name, stone.URL, stone.Account))
	return nil
}

func (c *command) updateStone(args []string) error {
	fs := c.flags("update stone")
	rawURL := fs.String("url", "", "")
	account := fs.String("account", "", "")
	newPass := fs.Bool("passphrase", false, "")
	fromStdin := fs.Bool("passphrase-stdin", false, "")
	rename := fs.String("rename", "", "")
	makeDefault := fs.Bool("default", false, "")
	noCheck := fs.Bool("no-check", false, "")
	statusKey := fs.Bool("status-key", false, "")
	statusKeyStdin := fs.Bool("status-key-stdin", false, "")
	noStatusKey := fs.Bool("no-status-key", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) != 1 {
		return usage("Use 'tolk update stone <name>'.")
	}
	name := pos[0]
	if *rename != "" && !stoneName.MatchString(*rename) {
		return usage("%q can't be a stone's name. Use letters, digits, dot, dash and underscore.", *rename)
	}
	if *rawURL == "" && *account == "" && !*newPass && !*fromStdin && *rename == "" && !*makeDefault && !*statusKey && !*statusKeyStdin && !*noStatusKey {
		return usage("Say what to change: --url, --account, --passphrase, --rename, --default, --status-key or --no-status-key.")
	}
	// The stone is checked with the changed entry before anything is saved,
	// so the passphrase is asked for first and the check needs the old entry.
	var changed config.Stone
	err = c.change(func(container *config.Container) error {
		stone, ok := container.Stones[name]
		if !ok {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", name))
		}
		if *rawURL != "" {
			u, err := normaliseURL(*rawURL)
			if err != nil {
				return err
			}
			stone.URL = u
		}
		if *account != "" {
			stone.Account = *account
		}
		if *newPass || *fromStdin {
			if stone.Passphrase, err = c.stonePassphrase(*fromStdin, i18n.T("Passphrase of %s on %s", stone.Account, name)); err != nil {
				return err
			}
		}
		if !*noCheck && stone.Account != "" && (*rawURL != "" || *account != "" || *newPass || *fromStdin) {
			if err := c.check(stone); err != nil {
				return err
			}
		}
		if *statusKey || *statusKeyStdin {
			// With a status key, 'tolk status' asks with it, not with the account.
			if stone.StatusKey, err = c.stonePassphrase(*statusKeyStdin, i18n.T("Status key of %s", name)); err != nil {
				return err
			}
			if !*noCheck {
				ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
				defer cancel()
				if _, err := client.New(stone.URL).Status(ctx, stone.StatusKey, 0, false); err != nil {
					return err
				}
			}
		}
		if *noStatusKey {
			stone.StatusKey = ""
		}
		newName := name
		if *rename != "" {
			if _, exists := container.Stones[*rename]; exists {
				return exit.New(exit.Usage, "%s", i18n.T("There is a stone named %q already.", *rename))
			}
			newName = *rename
			delete(container.Stones, name)
			for group, targets := range container.Groups {
				for i, target := range targets {
					if strings.HasPrefix(target, name+":") {
						container.Groups[group][i] = newName + target[len(name):]
					}
				}
			}
		}
		container.Stones[newName] = stone
		if container.Default == name || *makeDefault {
			container.Default = newName
		}
		changed = stone
		return nil
	})
	if err != nil {
		return err
	}
	c.note(i18n.T("Updated the stone %q (%s as %s).", firstNonEmpty(*rename, name), changed.URL, changed.Account))
	return nil
}

func firstNonEmpty(a, b string) string {
	if a != "" {
		return a
	}
	return b
}

func (c *command) removeStone(args []string) error {
	pos, err := parse(c.flags("remove stone"), args)
	if err != nil {
		return err
	}
	if len(pos) != 1 {
		return usage("Use 'tolk remove stone <name>'.")
	}
	name := pos[0]
	err = c.change(func(container *config.Container) error {
		if _, ok := container.Stones[name]; !ok {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", name))
		}
		delete(container.Stones, name)
		if container.Default == name {
			container.Default = ""
			for other := range container.Stones {
				if container.Default == "" || other < container.Default {
					container.Default = other
				}
			}
		}
		for group, targets := range container.Groups {
			kept := targets[:0]
			for _, target := range targets {
				if !strings.HasPrefix(target, name+":") {
					kept = append(kept, target)
				}
			}
			container.Groups[group] = kept
		}
		return nil
	})
	if err != nil {
		return err
	}
	c.note(i18n.T("Removed the stone %q.", name))
	return nil
}

func (c *command) listStones(args []string) error {
	if _, err := parse(c.flags("list stones"), args); err != nil {
		return err
	}
	access, err := service.Open(c.prompt())
	if err != nil {
		return err
	}
	defer access.Close()
	stones, groups, err := access.Stones()
	if err != nil {
		return err
	}
	if c.json() {
		if stones == nil {
			stones = []agent.StoneInfo{}
		}
		return c.printJSON(map[string]any{"stones": stones, "groups": groups})
	}
	if len(stones) == 0 {
		c.note(i18n.T("No stones yet. Add one with 'tolk register stone <name>'."))
		return nil
	}
	for _, stone := range stones {
		mark := " "
		if stone.Default {
			mark = "*"
		}
		c.say("%s %-16s %-20s %s", mark, stone.Name, stone.Account, stone.URL)
	}
	return nil
}

// stoneInfo asks a stone for its version and kind of system; it needs the
// container for the URL, but no login.
func (c *command) stoneInfo(args []string) error {
	pos, err := parse(c.flags("stone info"), args)
	if err != nil {
		return err
	}
	if len(pos) > 1 {
		return usage("Use 'tolk stone info [<name>]'.")
	}
	access, err := service.Open(c.prompt())
	if err != nil {
		return err
	}
	defer access.Close()
	stones, _, err := access.Stones()
	if err != nil {
		return err
	}
	var target *agent.StoneInfo
	for i := range stones {
		if (len(pos) == 1 && stones[i].Name == pos[0]) || (len(pos) == 0 && stones[i].Default) {
			target = &stones[i]
		}
	}
	if target == nil {
		if len(pos) == 1 {
			return exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", pos[0]))
		}
		return exit.New(exit.NotFound, "%s", i18n.T("No stones yet. Add one with 'tolk register stone <name>'."))
	}
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()
	info, err := client.New(target.URL).Version(ctx)
	if err != nil {
		return err
	}
	kind := "unknown"
	if info.Persistent != nil {
		kind = "production"
		if !*info.Persistent {
			kind = "test"
		}
	}
	if c.json() {
		return c.printJSON(map[string]any{
			"stone": target.Name, "url": target.URL, "version": info.Version,
			"database": info.Database, "persistent": info.Persistent, "kind": kind,
		})
	}
	c.say("%-10s %s", i18n.T("Stone"), target.Name)
	c.say("%-10s %s", "URL", target.URL)
	c.say("%-10s Haugr EDS %s", i18n.T("Version"), info.Version)
	if info.Database != "" {
		c.say("%-10s %s", i18n.T("Database"), info.Database)
	}
	switch kind {
	case "test":
		c.say("%-10s %s", i18n.T("System"), c.style().Accent(i18n.Rune("Test system"))+": "+i18n.Rune("The data will not survive a restart of the stone."))
	case "production":
		c.say("%-10s %s", i18n.T("System"), i18n.T("Production system"))
	}
	return nil
}
