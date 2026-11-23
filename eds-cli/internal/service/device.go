package service

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/hkdf"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"sync"

	"github.com/HaugrNet/eds/eds-cli/internal/config"
)

// What this device remembers of a stone, for chambers (E2E.md): which rooms
// it has seen as chambers, so a stone which later presents one as a room is
// noticed, and the public keys of others it has seen, so one which changes
// is told ("Checking keys").
//
// It is kept beside the container, in a file of its own for each account
// with keys, sealed under a key made from the account key: so it can be
// written without the container's passphrase, and a copy of the file tells
// nothing, not even which stone it belongs to. Without keys there is
// nothing to remember: a room is then a room, or a chamber which waits.
type deviceState struct {
	Chambers []string          `json:"chambers"`
	Seen     map[string]string `json:"seen"`
	path     string
	aead     cipher.AEAD
	mu       sync.Mutex
}

// The outcome of comparing a member's public key with the one seen before.
const (
	KeyNew     = "new"
	KeySame    = "same"
	KeyChanged = "changed"
)

const deviceAAD = "tolk-device-v1"

// device loads what this device remembers for the stone's account, once.
func (st *Stone) device() *deviceState {
	st.keys.deviceMu.Lock()
	defer st.keys.deviceMu.Unlock()
	if st.keys.device != nil {
		return st.keys.device
	}
	state := &deviceState{Seen: map[string]string{}}
	st.keys.device = state
	account := st.keys.accountKey
	if len(account) != 32 {
		return state
	}
	key, err := hkdf.Key(sha256.New, account, nil, "tolk/device/v1/key", 32)
	if err != nil {
		return state
	}
	name, err := hkdf.Key(sha256.New, account, nil, "tolk/device/v1/name", 12)
	if err != nil {
		return state
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return state
	}
	if state.aead, err = cipher.NewGCM(block); err != nil {
		return state
	}
	dir, err := config.Dir()
	if err != nil {
		return state
	}
	state.path = filepath.Join(dir, "device-"+hex.EncodeToString(name)+".bin")
	sealed, err := os.ReadFile(state.path)
	if err != nil || len(sealed) < 12 {
		return state
	}
	plain, err := state.aead.Open(nil, sealed[:12], sealed[12:], []byte(deviceAAD))
	if err != nil {
		return state // Not this account's, or damaged: it starts anew.
	}
	_ = json.Unmarshal(plain, state)
	if state.Seen == nil {
		state.Seen = map[string]string{}
	}
	return state
}

func (d *deviceState) save() {
	if d.aead == nil || d.path == "" {
		return // Without keys, the device remembers while the command runs.
	}
	plain, err := json.Marshal(d)
	if err != nil {
		return
	}
	nonce := make([]byte, 12)
	if _, err := rand.Read(nonce); err != nil {
		return
	}
	temporary := d.path + ".tmp"
	if os.WriteFile(temporary, d.aead.Seal(nonce, nonce, plain, []byte(deviceAAD)), 0o600) == nil {
		_ = os.Rename(temporary, d.path)
	}
}

func (d *deviceState) knows(circleID string) bool {
	d.mu.Lock()
	defer d.mu.Unlock()
	return d.has(circleID)
}

func (d *deviceState) has(circleID string) bool {
	for _, known := range d.Chambers {
		if known == circleID {
			return true
		}
	}
	return false
}

func (d *deviceState) remember(circleID string) {
	d.mu.Lock()
	defer d.mu.Unlock()
	if !d.has(circleID) {
		d.Chambers = append(d.Chambers, circleID)
		d.save()
	}
}

// CheckKey compares the public key of another member with the one this
// device saw before: KeyNew the first time, which remembers it, KeySame, or
// KeyChanged, which keeps the earlier one until the person accepts the new
// one (AcceptKey).
func (st *Stone) CheckKey(memberID, publicKey string) string {
	device := st.device()
	device.mu.Lock()
	defer device.mu.Unlock()
	seen, ok := device.Seen[memberID]
	if !ok {
		device.Seen[memberID] = publicKey
		device.save()
		return KeyNew
	}
	if seen == publicKey {
		return KeySame
	}
	return KeyChanged
}

// AcceptKey remembers the public key of another member as the one to
// expect.
func (st *Stone) AcceptKey(memberID, publicKey string) {
	device := st.device()
	device.mu.Lock()
	defer device.mu.Unlock()
	device.Seen[memberID] = publicKey
	device.save()
}
