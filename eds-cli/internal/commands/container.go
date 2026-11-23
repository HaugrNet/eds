package commands

import (
	"os"
	"strconv"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// iterations is the PBKDF2 count for a new container; tests lower it.
func iterations() int {
	if n, err := strconv.Atoi(os.Getenv("TOLK_KDF_ITERATIONS")); err == nil && n > 0 {
		return n
	}
	return config.Iterations
}

// init creates the encrypted container.
func (c *command) init(args []string) error {
	if _, err := parse(c.flags("init"), args); err != nil {
		return err
	}
	file, err := config.Load()
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if file.Container != nil {
		return exit.New(exit.Usage, "%s", i18n.T("There is a container already. Use 'tolk register stone' to add stones."))
	}
	// The place is named first, before a passphrase is asked for.
	path, err := config.Path()
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	c.note(i18n.T("The container will be kept in %s.", path))
	var passphrase string
	if os.Getenv(service.EnvPassphraseFile) != "" {
		passphrase, err = service.ContainerPassphrase(nil)
		if err != nil {
			return err
		}
	} else {
		if !c.interactive() || c.env.ReadPassword == nil {
			return exit.New(exit.Auth, "%s", i18n.T("Nobody is at the terminal to choose a passphrase. Set TOLK_PASSPHRASE_FILE."))
		}
		passphrase, err = c.env.ReadPassword(i18n.T("Choose a passphrase for the container"))
		if err != nil {
			return exit.New(exit.Auth, "%v", err)
		}
		again, err := c.env.ReadPassword(i18n.T("Repeat the passphrase"))
		if err != nil {
			return exit.New(exit.Auth, "%v", err)
		}
		if passphrase != again {
			return exit.New(exit.Usage, "%s", i18n.T("The two passphrases differ."))
		}
	}
	if passphrase == "" {
		return exit.New(exit.Usage, "%s", i18n.T("The passphrase must not be empty."))
	}
	file.Container, err = config.Seal(config.NewContainer(), passphrase, iterations())
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if err := config.Save(file); err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	c.note(i18n.T("Created the container. Add a stone with 'tolk register stone <name>'."))
	return nil
}

// save seals the changed container with the same passphrase, writes the
// config, and hands the new container to a running agent.
func (c *command) save(container *config.Container, passphrase string) error {
	file, err := config.Load()
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	n := iterations()
	if file.Container != nil {
		n = file.Container.Iterations
	}
	file.Container, err = config.Seal(container, passphrase, n)
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if err := config.Save(file); err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if agent.Running() {
		if _, err := agent.Ask(agent.Request{Op: "load", Container: container}); err != nil {
			c.note(i18n.T("The agent still has the old container: %v. Run 'tolk logout' and 'tolk login'.", err))
		}
	}
	return nil
}

// login unlocks the container and starts the agent.
func (c *command) login(args []string) error {
	fs := c.flags("login")
	lifetime := fs.Duration("lifetime", 0, "")
	if _, err := parse(fs, args); err != nil {
		return err
	}
	if agent.Running() {
		c.note(i18n.T("The container is already unlocked; the agent is running."))
		return nil
	}
	container, err := service.Unlock(c.prompt())
	if err != nil {
		return err
	}
	if err := agent.Spawn(container, *lifetime); err != nil {
		return exit.New(exit.Other, "%s", i18n.T("Cannot start the agent: %v", err))
	}
	if *lifetime > 0 {
		c.note(i18n.T("Unlocked until %s, or until 'tolk logout'.", time.Now().Add(*lifetime).Format("15:04")))
	} else {
		c.note(i18n.T("Unlocked until 'tolk logout'."))
	}
	return nil
}

// logout ends the sessions on the stones and stops the agent.
func (c *command) logout(args []string) error {
	if _, err := parse(c.flags("logout"), args); err != nil {
		return err
	}
	if !agent.Running() {
		c.note(i18n.T("No agent is running; the container is locked."))
		return nil
	}
	if _, err := agent.Ask(agent.Request{Op: "logout"}); err != nil {
		return err
	}
	c.note(i18n.T("Logged out; the container is locked."))
	return nil
}
