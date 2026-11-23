// Package service is the logic shared by Tolk's commands and, later, the
// TUI: getting at the stones and their sessions.
package service

import (
	"context"
	"errors"
	"io"
	"os"
	"sort"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// EnvPassphraseFile names a protected file with the container's passphrase,
// the explicit opt-in for unattended scripts.
const EnvPassphraseFile = "TOLK_PASSPHRASE_FILE"

// Access gives a command the stones and a session on each.
type Access interface {
	// Stones lists the stones, without passphrases, and the groups.
	Stones() ([]agent.StoneInfo, map[string][]string, error)
	// Session returns a stone's API root and a session token there. An
	// empty name means the default stone; renew asks for a fresh login.
	Session(ctx context.Context, stone string, renew bool) (url, token string, err error)
	// Secrets returns what a stone's entry keeps beside the passphrase: its
	// API root, the account key for chambers and the status key, each empty
	// if there is none.
	Secrets(stone string) (url, accountKey, statusKey string, err error)
	// Close ends what the command itself started.
	Close()
}

// Prompt asks a person for a passphrase; nil when nobody is at the terminal.
type Prompt func(label string) (string, error)

// Open finds the unlocked container: a running agent; the passphrase file,
// if the environment names one; a prompt, if a person is at the terminal;
// otherwise it fails with exit code 3.
func Open(prompt Prompt) (Access, error) {
	if agent.Running() {
		return agentAccess{}, nil
	}
	container, err := Unlock(prompt)
	if err != nil {
		return nil, err
	}
	return &directAccess{container: container, tokens: map[string]string{}}, nil
}

// Direct gives access to the stones of a container which is open already,
// without an agent: it logs in to the stones a command uses, and logs out
// at the end.
func Direct(container *config.Container) Access {
	return &directAccess{container: container, tokens: map[string]string{}}
}

// Unlock opens the container from the config with the passphrase file or the
// prompt.
func Unlock(prompt Prompt) (*config.Container, error) {
	container, _, err := UnlockWithPassphrase(prompt)
	return container, err
}

// UnlockWithPassphrase also returns the passphrase, for sealing a change.
func UnlockWithPassphrase(prompt Prompt) (*config.Container, string, error) {
	file, err := config.Load()
	if err != nil {
		return nil, "", exit.New(exit.Other, "%v", err)
	}
	if file.Container == nil {
		return nil, "", exit.New(exit.Auth, "%s", i18n.T("There is no container yet. Run 'tolk init' first."))
	}
	passphrase, err := ContainerPassphrase(prompt)
	if err != nil {
		return nil, "", err
	}
	container, err := config.Open(file.Container, passphrase)
	if errors.Is(err, config.ErrPassphrase) {
		return nil, "", exit.New(exit.Auth, "%s", i18n.T("Wrong passphrase for the container."))
	}
	if err != nil {
		return nil, "", exit.New(exit.Other, "%v", err)
	}
	return container, passphrase, nil
}

// ContainerPassphrase gets the container's passphrase from the file named in
// the environment, or from the prompt.
func ContainerPassphrase(prompt Prompt) (string, error) {
	if path := os.Getenv(EnvPassphraseFile); path != "" {
		data, err := os.ReadFile(path)
		if err != nil {
			return "", exit.New(exit.Auth, "%s", i18n.T("Cannot read the passphrase file %s: %v", path, err))
		}
		return strings.TrimRight(string(data), "\r\n"), nil
	}
	if prompt == nil {
		return "", exit.New(exit.Auth, "%s", i18n.T("The container is locked. Run 'tolk login', or set TOLK_PASSPHRASE_FILE for unattended use."))
	}
	passphrase, err := prompt(i18n.T("Passphrase of the container"))
	if err != nil {
		return "", exit.New(exit.Auth, "%v", err)
	}
	return passphrase, nil
}

// ReadSecret reads one line from a reader, for passphrases given on standard
// input.
func ReadSecret(r io.Reader) (string, error) {
	var b strings.Builder
	buf := make([]byte, 1)
	for {
		n, err := r.Read(buf)
		if n == 1 {
			if buf[0] == '\n' {
				break
			}
			b.WriteByte(buf[0])
		}
		if err != nil {
			if b.Len() == 0 {
				return "", err
			}
			break
		}
	}
	return strings.TrimRight(b.String(), "\r"), nil
}

type agentAccess struct{}

func (agentAccess) Stones() ([]agent.StoneInfo, map[string][]string, error) {
	resp, err := agent.Ask(agent.Request{Op: "stones"})
	if err != nil {
		return nil, nil, err
	}
	return resp.Stones, resp.Groups, nil
}

func (agentAccess) Session(_ context.Context, stone string, renew bool) (string, string, error) {
	resp, err := agent.Ask(agent.Request{Op: "session", Stone: stone, Renew: renew})
	if err != nil {
		return "", "", err
	}
	return resp.URL, resp.Token, nil
}

func (agentAccess) Secrets(stone string) (string, string, string, error) {
	resp, err := agent.Ask(agent.Request{Op: "secrets", Stone: stone})
	if err != nil {
		return "", "", "", err
	}
	return resp.URL, resp.AccountKey, resp.StatusKey, nil
}

func (agentAccess) Close() {}

// directAccess works without an agent: it logs in to the stones a command
// uses and logs out at the end.
type directAccess struct {
	container *config.Container
	tokens    map[string]string
}

func (d *directAccess) Stones() ([]agent.StoneInfo, map[string][]string, error) {
	var stones []agent.StoneInfo
	for name, stone := range d.container.Stones {
		_, in := d.tokens[name]
		stones = append(stones, agent.StoneInfo{Name: name, URL: stone.URL, Account: stone.Account, Default: name == d.container.Default, LoggedIn: in})
	}
	sort.Slice(stones, func(i, j int) bool { return stones[i].Name < stones[j].Name })
	groups := d.container.Groups
	if stones == nil {
		stones = []agent.StoneInfo{}
	}
	if groups == nil {
		groups = map[string][]string{}
	}
	return stones, groups, nil
}

func (d *directAccess) Session(ctx context.Context, name string, renew bool) (string, string, error) {
	if name == "" {
		name = d.container.Default
	}
	stone, ok := d.container.Stones[name]
	if !ok {
		return "", "", exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", name))
	}
	token, have := d.tokens[name]
	if !have || renew {
		var err error
		token, err = client.New(stone.URL).Login(ctx, stone.Account, stone.Passphrase)
		if err != nil {
			return "", "", err
		}
		d.tokens[name] = token
	}
	return stone.URL, token, nil
}

func (d *directAccess) Secrets(name string) (string, string, string, error) {
	if name == "" {
		name = d.container.Default
	}
	stone, ok := d.container.Stones[name]
	if !ok {
		return "", "", "", exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", name))
	}
	return stone.URL, stone.AccountKey, stone.StatusKey, nil
}

func (d *directAccess) Close() {
	for name, token := range d.tokens {
		_ = client.New(d.container.Stones[name].URL).Logout(context.Background(), token)
	}
	d.tokens = map[string]string{}
}
