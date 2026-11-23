// Package config holds Tolk's config file: the UI settings in the clear, and
// the encrypted container with the stones.
//
// The container is the whole entry of each stone (name, URL, account,
// passphrase) and the groups, so the file doesn't reveal which stones are
// used. It is sealed with AES-256-GCM under a key derived from the
// container's passphrase with PBKDF2-HMAC-SHA256, both from Go's standard
// library.
package config

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sort"
)

const (
	// Iterations is the PBKDF2 iteration count for a new container.
	Iterations = 600_000
	kdfName    = "pbkdf2-hmac-sha256"
	aad        = "tolk-container-v1"
	// EnvDir overrides the config directory.
	EnvDir = "TOLK_CONFIG_DIR"
)

// ErrPassphrase is returned when the container doesn't open.
var ErrPassphrase = errors.New("wrong passphrase for the container")

// ErrNoContainer is returned when the config has no container yet.
var ErrNoContainer = errors.New("no container; run 'tolk init'")

// File is config.json.
type File struct {
	UI        UI      `json:"ui"`
	Container *Sealed `json:"container,omitempty"`
}

// UI are the settings of Tolk itself, per machine.
type UI struct {
	Language   string `json:"language"`   // auto, en, de
	Clock      string `json:"clock"`      // 24, 12, auto
	Appearance string `json:"appearance"` // auto, light, dark
	Theme      string `json:"theme"`      // rune, fjord, moss, contrast
	Bell       bool   `json:"bell"`       // terminal bell for new messages
	Present    bool   `json:"present"`    // the interactive mode shows the person as online
	// Layout is the arrangement of the panes in the interactive mode: the
	// name of a preset, or "custom" with the tree in LayoutTree (Rune's
	// layout tree, so a layout can be copied between the two).
	Layout     string          `json:"layout"`
	LayoutTree json.RawMessage `json:"layoutTree,omitempty"`
	// LayoutAway holds, by pane, the layout from before the pane was hidden,
	// so it comes back where it was (Rune keeps the same per device).
	LayoutAway map[string]json.RawMessage `json:"layoutAway,omitempty"`
}

// DefaultUI are the settings of a new config.
func DefaultUI() UI {
	return UI{Language: "auto", Clock: "24", Appearance: "auto", Theme: "rune", Bell: false, Present: true, Layout: "three-columns"}
}

// Sealed is the encrypted container as it is stored.
type Sealed struct {
	Version    int    `json:"version"`
	KDF        string `json:"kdf"`
	Iterations int    `json:"iterations"`
	Salt       []byte `json:"salt"`
	Nonce      []byte `json:"nonce"`
	Data       []byte `json:"data"`
}

// Container is the decrypted content.
type Container struct {
	Default string              `json:"default"`
	Stones  map[string]Stone    `json:"stones"`
	Groups  map[string][]string `json:"groups"`
}

// Stone is one server instance and the account used there.
type Stone struct {
	URL        string `json:"url"` // the API root, e.g. https://eds.example.com/api/v1
	Account    string `json:"account"`
	Passphrase string `json:"passphrase"`
	// AccountKey is the account's key for chambers (E2E.md), Base64: it
	// opens the vault with the private key, which the stone keeps. It is the
	// recovery key in another form.
	AccountKey string `json:"accountKey,omitempty"`
	// StatusKey reads the status of the stone and nothing else (STATUS.md).
	// A stone may be kept with a status key only, without an account.
	StatusKey string `json:"statusKey,omitempty"`
}

// NewContainer returns an empty container.
func NewContainer() *Container {
	return &Container{Stones: map[string]Stone{}, Groups: map[string][]string{}}
}

// StoneNames returns the names of the stones, sorted.
func (c *Container) StoneNames() []string {
	names := make([]string, 0, len(c.Stones))
	for name := range c.Stones {
		names = append(names, name)
	}
	sort.Strings(names)
	return names
}

// Dir returns the config directory, and makes it if needed.
func Dir() (string, error) {
	dir := os.Getenv(EnvDir)
	if dir == "" {
		base, err := os.UserConfigDir()
		if err != nil {
			return "", err
		}
		dir = filepath.Join(base, "tolk")
	}
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return "", err
	}
	return dir, nil
}

// Path returns the path of config.json.
func Path() (string, error) {
	dir, err := Dir()
	if err != nil {
		return "", err
	}
	return filepath.Join(dir, "config.json"), nil
}

// Load reads config.json; a missing file gives the defaults.
func Load() (*File, error) {
	path, err := Path()
	if err != nil {
		return nil, err
	}
	f := &File{UI: DefaultUI()}
	data, err := os.ReadFile(path)
	if errors.Is(err, os.ErrNotExist) {
		return f, nil
	}
	if err != nil {
		return nil, err
	}
	if err := json.Unmarshal(data, f); err != nil {
		return nil, fmt.Errorf("%s: %w", path, err)
	}
	return f, nil
}

// Save writes config.json, readable by the user only, replacing it at once.
func Save(f *File) error {
	path, err := Path()
	if err != nil {
		return err
	}
	data, err := json.MarshalIndent(f, "", "  ")
	if err != nil {
		return err
	}
	tmp, err := os.CreateTemp(filepath.Dir(path), "config-*.json")
	if err != nil {
		return err
	}
	defer os.Remove(tmp.Name())
	if err := tmp.Chmod(0o600); err != nil {
		tmp.Close()
		return err
	}
	if _, err := tmp.Write(append(data, '\n')); err != nil {
		tmp.Close()
		return err
	}
	if err := tmp.Close(); err != nil {
		return err
	}
	return os.Rename(tmp.Name(), path)
}

// Seal encrypts the container under the passphrase, with a fresh salt and
// nonce.
func Seal(c *Container, passphrase string, iterations int) (*Sealed, error) {
	plain, err := json.Marshal(c)
	if err != nil {
		return nil, err
	}
	s := &Sealed{Version: 1, KDF: kdfName, Iterations: iterations, Salt: make([]byte, 16), Nonce: make([]byte, 12)}
	if _, err := rand.Read(s.Salt); err != nil {
		return nil, err
	}
	if _, err := rand.Read(s.Nonce); err != nil {
		return nil, err
	}
	gcm, err := newGCM(passphrase, s.Salt, iterations)
	if err != nil {
		return nil, err
	}
	s.Data = gcm.Seal(nil, s.Nonce, plain, []byte(aad))
	return s, nil
}

// Open decrypts the container. A wrong passphrase gives ErrPassphrase.
func Open(s *Sealed, passphrase string) (*Container, error) {
	if s == nil {
		return nil, ErrNoContainer
	}
	if s.Version != 1 || s.KDF != kdfName {
		return nil, fmt.Errorf("container version %d (%s) is not supported", s.Version, s.KDF)
	}
	gcm, err := newGCM(passphrase, s.Salt, s.Iterations)
	if err != nil {
		return nil, err
	}
	plain, err := gcm.Open(nil, s.Nonce, s.Data, []byte(aad))
	if err != nil {
		return nil, ErrPassphrase
	}
	c := NewContainer()
	if err := json.Unmarshal(plain, c); err != nil {
		return nil, err
	}
	if c.Stones == nil {
		c.Stones = map[string]Stone{}
	}
	if c.Groups == nil {
		c.Groups = map[string][]string{}
	}
	return c, nil
}

func newGCM(passphrase string, salt []byte, iterations int) (cipher.AEAD, error) {
	key, err := pbkdf2.Key(sha256.New, passphrase, salt, iterations, 32)
	if err != nil {
		return nil, err
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	return cipher.NewGCM(block)
}
