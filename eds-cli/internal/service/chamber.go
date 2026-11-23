package service

import (
	"bufio"
	"context"
	"encoding/base64"
	"errors"
	"io"
	"strings"
	"sync"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// Rooms and chambers (E2E.md). A room is a Circle as it always was: the
// stone encrypts, and sees the content while it handles it. A chamber is a
// Circle whose content Tolk encrypts before the stone gets it, with a room
// key which only the people of the chamber have. The stone can't tell the
// two apart; a chamber is known by its shared value.
//
// This says the same as Rune's src/e2e (identity.ts, rooms.ts).

// KeyState is how this device stands to the account's keys for chambers.
type KeyState string

const (
	// KeysNone: the account has no keys yet.
	KeysNone KeyState = "none"
	// KeysLocked: the account has keys, and this device hasn't the account key.
	KeysLocked KeyState = "locked"
	// KeysReady: this device has the keys.
	KeysReady KeyState = "ready"
)

// RoomKind is how this device stands to a room.
type RoomKind string

const (
	// KindRoom: not a chamber.
	KindRoom RoomKind = "room"
	// KindReady: a chamber, and this device has its current key.
	KindReady RoomKind = "ready"
	// KindBehind: a chamber whose key was changed; this device has an
	// earlier one, reads what was written with it, and waits for an
	// administrator.
	KindBehind RoomKind = "behind"
	// KindWaiting: a chamber, and nobody gave this account its key yet.
	KindWaiting RoomKind = "waiting"
	// KindLocked: a chamber, and this device hasn't the account's keys.
	KindLocked RoomKind = "locked"
	// KindStripped: this device knew the room as a chamber, and the stone
	// now presents it as a room. Nothing is written to it.
	KindStripped RoomKind = "stripped"
)

// RoomState is what this device has of a room's keys.
type RoomState struct {
	Kind RoomKind
	// Keys are the room keys this device has, by their versions.
	Keys map[int][]byte
	// Current is the key new content is written with; only when ready.
	Current *e2e.RoomKey
}

// Chamber says whether the room is a chamber, whatever this device can do
// in it.
func (r *RoomState) Chamber() bool { return r.Kind != KindRoom }

// Refusal is why nothing can be written to a room, or read of a chamber;
// empty when all is well.
func (r *RoomState) Refusal() string {
	switch r.Kind {
	case KindBehind:
		return i18n.Rune("The key of this chamber was changed. The new one comes from an administrator of it: their app gives it to you as soon as they have the chamber open in Rune, or list its members in Tolk.")
	case KindWaiting:
		return i18n.Rune("You have no key to this chamber yet. It comes from an administrator of it: their app gives it to you as soon as they have the chamber open in Rune, or list its members in Tolk.")
	case KindLocked:
		return i18n.T("This device does not have your keys yet. Give it your recovery key with 'tolk key unlock'.")
	case KindStripped:
		return i18n.T("This room was a chamber, and the stone now presents it as an ordinary room. Tolk writes nothing to it.")
	}
	return ""
}

// chamberData is what a Stone keeps of the account's keys and the rooms it
// opened.
type chamberData struct {
	// The chat reads entries on a goroutine of its own, while a command
	// goes on: what is kept here is guarded.
	mu         sync.Mutex
	loaded     bool
	state      KeyState
	memberID   string
	pair       *e2e.KeyPair
	vault      string
	point      []byte
	accountKey []byte
	rooms      map[string]*RoomState
	deviceMu   sync.Mutex
	device     *deviceState
}

func refused(format string, args ...any) error {
	return exit.New(exit.Auth, "%s", i18n.T(format, args...))
}

// MemberID is the ID of the account the stone is used with.
func (st *Stone) MemberID(ctx context.Context) (string, error) {
	if st.keys.memberID != "" {
		return st.keys.memberID, nil
	}
	me, err := st.Me(ctx)
	if err != nil {
		return "", err
	}
	st.keys.memberID = me.MemberID
	return me.MemberID, nil
}

// LoadKeys loads the account's keys for chambers, as far as this device
// can: the public key and the vault from the stone, the account key from
// the container.
func (st *Stone) LoadKeys(ctx context.Context) (KeyState, error) {
	st.keys.mu.Lock()
	defer st.keys.mu.Unlock()
	return st.loadKeys(ctx)
}

func (st *Stone) loadKeys(ctx context.Context) (KeyState, error) {
	k := &st.keys
	if k.loaded {
		return k.state, nil
	}
	memberID, err := st.MemberID(ctx)
	if err != nil {
		return KeysNone, err
	}
	var own *client.FetchMemberResponse
	if err := st.Do(ctx, func(token string) error {
		var err error
		own, err = st.Client.Member(ctx, token, memberID)
		return err
	}); err != nil {
		return KeysNone, err
	}
	k.loaded, k.state, k.pair = true, KeysNone, nil
	if len(own.Members) == 0 {
		return k.state, nil
	}
	k.point = e2e.ParsePublicKey(own.Members[0].PublicKey)
	k.vault = own.Members[0].Vault
	if k.point == nil || k.vault == "" {
		return k.state, nil
	}
	k.state = KeysLocked
	if k.accountKey == nil {
		_, stored, _, err := st.access.Secrets(st.Name)
		if err != nil {
			return k.state, err
		}
		if key, err := base64.StdEncoding.DecodeString(stored); err == nil && len(key) == 32 {
			k.accountKey = key
		}
	}
	if k.accountKey == nil {
		return k.state, nil
	}
	// A vault which was made with another account key stays closed: the
	// account has new keys since this device last saw them.
	if pair, err := e2e.OpenVault(k.vault, k.accountKey, memberID); err == nil {
		k.pair, k.state = pair, KeysReady
	}
	return k.state, nil
}

// OwnFingerprint is the fingerprint of the account's own public key, to
// compare with what others see; empty without keys.
func (st *Stone) OwnFingerprint(ctx context.Context) (string, error) {
	if _, err := st.LoadKeys(ctx); err != nil {
		return "", err
	}
	if st.keys.point == nil {
		return "", nil
	}
	return e2e.Fingerprint(st.keys.point), nil
}

// AccountKey is the account key this device has for the stone, or nil.
func (st *Stone) AccountKey(ctx context.Context) ([]byte, error) {
	if _, err := st.LoadKeys(ctx); err != nil {
		return nil, err
	}
	if st.keys.state != KeysReady {
		return nil, nil
	}
	return st.keys.accountKey, nil
}

// CreateKeys makes the account's keys: a key pair and an account key. The
// public key and the vault go to the stone. The account key is returned:
// the caller keeps it in the container and shows it, once, as the recovery
// key.
func (st *Stone) CreateKeys(ctx context.Context) ([]byte, error) {
	state, err := st.LoadKeys(ctx)
	if err != nil {
		return nil, err
	}
	if state != KeysNone {
		return nil, exit.New(exit.Usage, "%s", i18n.T("The account has keys already."))
	}
	pair, err := e2e.GenerateKeyPair()
	if err != nil {
		return nil, exit.New(exit.Other, "%v", err)
	}
	account := e2e.Random(32)
	vault, err := e2e.SealVault(account, st.keys.memberID, pair.Scalar(), nil, nil)
	if err != nil {
		return nil, exit.New(exit.Other, "%v", err)
	}
	if err := st.Do(ctx, func(token string) error {
		return st.Client.SetKeys(ctx, token, e2e.PublicKeyText(pair.Point), vault)
	}); err != nil {
		return nil, err
	}
	k := &st.keys
	k.mu.Lock()
	defer k.mu.Unlock()
	k.pair, k.point, k.vault, k.accountKey, k.state = pair, pair.Point, vault, account, KeysReady
	st.forgetDevice()
	return account, nil
}

// UnlockKeys gives this device the account's keys, with the recovery key as
// typed, or the link another device showed. The account key is returned,
// for the caller to keep in the container.
func (st *Stone) UnlockKeys(ctx context.Context, typed string) ([]byte, error) {
	if _, err := st.LoadKeys(ctx); err != nil {
		return nil, err
	}
	k := &st.keys
	if k.point == nil || k.vault == "" {
		return nil, exit.New(exit.Usage, "%s", i18n.T("The account has no keys to unlock."))
	}
	typed = strings.TrimSpace(typed)
	var account []byte
	if i := strings.Index(typed, "#device="); i >= 0 {
		account = e2e.ParseDeviceLink(typed[i:])
	} else {
		account = e2e.ParseRecoveryKey(typed)
	}
	if account == nil {
		return nil, exit.New(exit.Usage, "%s", i18n.Rune("This is not a recovery key: it has 52 letters and digits."))
	}
	pair, err := e2e.OpenVault(k.vault, account, k.memberID)
	if err != nil {
		return nil, exit.New(exit.Auth, "%s", i18n.Rune("This recovery key does not open your keys."))
	}
	k.mu.Lock()
	defer k.mu.Unlock()
	k.pair, k.accountKey, k.state = pair, account, KeysReady
	st.forgetDevice()
	k.rooms = nil
	return account, nil
}

// ForgetRoomState makes the next RoomState ask the stone again, for one room
// or, with an empty ID, for all.
func (st *Stone) ForgetRoomState(circleID string) {
	st.keys.mu.Lock()
	defer st.keys.mu.Unlock()
	if circleID == "" {
		st.keys.rooms = nil
	} else {
		delete(st.keys.rooms, circleID)
	}
}

// RoomState is how this device stands to a room, opened once and kept.
func (st *Stone) RoomState(ctx context.Context, circleID string) (*RoomState, error) {
	st.keys.mu.Lock()
	defer st.keys.mu.Unlock()
	return st.roomState(ctx, circleID)
}

func (st *Stone) roomState(ctx context.Context, circleID string) (*RoomState, error) {
	if state, ok := st.keys.rooms[circleID]; ok {
		return state, nil
	}
	state, err := st.openRoom(ctx, circleID)
	if err != nil {
		return nil, err
	}
	if st.keys.rooms == nil {
		st.keys.rooms = map[string]*RoomState{}
	}
	st.keys.rooms[circleID] = state
	return state, nil
}

func (st *Stone) openRoom(ctx context.Context, circleID string) (*RoomState, error) {
	keyState, err := st.loadKeys(ctx)
	if err != nil {
		return nil, err
	}
	var circle *client.Circle
	var own *client.Trustee
	if err := st.Do(ctx, func(token string) error {
		var err error
		if circle, err = st.Client.Circle(ctx, token, circleID); err != nil {
			// Who isn't in the room can't ask for it; for them it is a room.
			var e *exit.Error
			if errors.As(err, &e) && (e.ReturnCode == 404 || e.ReturnCode == 401) {
				circle, err = &client.Circle{CircleID: circleID}, nil
			}
			return err
		}
		if !e2e.IsChamber(circle.CircleKey) || keyState != KeysReady {
			return nil
		}
		own, err = st.Client.OwnTrustee(ctx, token, circleID, st.keys.memberID)
		return err
	}); err != nil {
		return nil, err
	}
	device := st.device()
	if !e2e.IsChamber(circle.CircleKey) {
		if device.knows(circleID) {
			return &RoomState{Kind: KindStripped}, nil
		}
		return &RoomState{Kind: KindRoom}, nil
	}
	device.remember(circleID)
	if keyState != KeysReady {
		if keyState == KeysNone {
			return &RoomState{Kind: KindWaiting}, nil
		}
		return &RoomState{Kind: KindLocked}, nil
	}
	if own == nil || e2e.SecretKind(own.Secret) != "key" {
		return &RoomState{Kind: KindWaiting}, nil
	}
	room, err := e2e.OpenSecretWithKey(own.Secret, st.keys.pair, circleID, st.keys.memberID)
	if err != nil {
		// Wrapped to a key this account no longer has.
		return &RoomState{Kind: KindWaiting}, nil
	}
	keys, err := e2e.OpenSharedValue(circle.CircleKey, circleID, room)
	if err != nil {
		return nil, exit.New(exit.Other, "%v", err)
	}
	if room.Version == e2e.ChamberVersion(circle.CircleKey) {
		return &RoomState{Kind: KindReady, Keys: keys, Current: &room}, nil
	}
	return &RoomState{Kind: KindBehind, Keys: keys}, nil
}

// Protect is what goes to the stone for a content of a room which is in
// memory: the content itself in a room, encrypted in a chamber. The context
// says what it is: empty for a file, "thumbnail", or "<feature>|<kind>".
func (st *Stone) Protect(ctx context.Context, circleID, what string, plain []byte) ([]byte, error) {
	state, err := st.RoomState(ctx, circleID)
	if err != nil {
		return nil, err
	}
	if state.Kind == KindRoom {
		return plain, nil
	}
	if state.Current == nil {
		return nil, exit.New(exit.Auth, "%s", state.Refusal())
	}
	sealed, err := e2e.EncryptContent(*state.Current, circleID, what, plain, nil)
	if err != nil {
		return nil, exit.New(exit.Other, "%v", err)
	}
	return sealed, nil
}

// Reveal is a content of a room as it came from the stone, readable: as it
// is in a room, decrypted in a chamber.
func (st *Stone) Reveal(ctx context.Context, circleID, what string, stored []byte) ([]byte, error) {
	state, err := st.RoomState(ctx, circleID)
	if err != nil {
		return nil, err
	}
	if state.Kind == KindRoom {
		return stored, nil
	}
	if len(state.Keys) == 0 {
		return nil, exit.New(exit.Auth, "%s", state.Refusal())
	}
	return e2e.DecryptContent(state.Keys, circleID, what, stored)
}

func (st *Stone) setSecrets(ctx context.Context, circleID string, secrets []client.TrusteeSecret, circleKey *string) error {
	return st.Do(ctx, func(token string) error {
		return st.Client.SetSecrets(ctx, token, circleID, secrets, circleKey)
	})
}

// MakeChamber turns a Circle which was just created into a chamber: makes
// its room key, and stores its mark and the creator's secret in one step.
// The account's keys must be there.
func (st *Stone) MakeChamber(ctx context.Context, circleID string) error {
	state, err := st.LoadKeys(ctx)
	if err != nil {
		return err
	}
	if state != KeysReady {
		return refused("This device does not have your keys yet. Give it your recovery key with 'tolk key unlock'.")
	}
	room := e2e.RoomKey{Key: e2e.Random(32), Version: 1}
	secret, err := e2e.WrapToKey(room, circleID, st.keys.memberID, st.keys.pair.Point, nil)
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	shared, err := e2e.SharedValue(circleID, room, nil, nil)
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if err := st.setSecrets(ctx, circleID, []client.TrusteeSecret{{MemberID: st.keys.memberID, Secret: secret}}, &shared); err != nil {
		return err
	}
	st.device().remember(circleID)
	st.ForgetRoomState(circleID)
	return nil
}

func (st *Stone) current(ctx context.Context, circleID string) (*e2e.RoomKey, error) {
	state, err := st.RoomState(ctx, circleID)
	if err != nil {
		return nil, err
	}
	if state.Current == nil {
		return nil, exit.New(exit.Auth, "%s", state.Refusal())
	}
	return state.Current, nil
}

// InviteSecret is the secret for someone invited into a chamber by a link:
// the room key under an invite secret, which travels in the link and never
// reaches the stone. Both are returned.
func (st *Stone) InviteSecret(ctx context.Context, circleID, name string) (secret string, invite []byte, err error) {
	room, err := st.current(ctx, circleID)
	if err != nil {
		return "", nil, err
	}
	invite = e2e.Random(32)
	secret, err = e2e.WrapForInvite(*room, circleID, name, invite, nil)
	if err != nil {
		return "", nil, exit.New(exit.Other, "%v", err)
	}
	return secret, invite, nil
}

// SecretFor is the secret for a user who has keys: the room key wrapped to
// their public key. Empty if they have none; they get the key by a repair.
func (st *Stone) SecretFor(ctx context.Context, circleID, memberID, publicKey string) (string, error) {
	room, err := st.current(ctx, circleID)
	if err != nil {
		return "", err
	}
	point := e2e.ParsePublicKey(publicKey)
	if point == nil {
		return "", nil
	}
	secret, err := e2e.WrapToKey(*room, circleID, memberID, point, nil)
	if err != nil {
		return "", exit.New(exit.Other, "%v", err)
	}
	return secret, nil
}

// AdoptInvitation is run after an invitation by a link was accepted: every
// secret of this account which is wrapped under the invite secret is opened
// with it and wrapped to the account's own key, so the invite secret can be
// forgotten. It returns how many rooms that were.
func (st *Stone) AdoptInvitation(ctx context.Context, name string, invite []byte) (int, error) {
	state, err := st.LoadKeys(ctx)
	if err != nil || state != KeysReady {
		return 0, err
	}
	var trustees []client.Trustee
	if err := st.Do(ctx, func(token string) error {
		var err error
		trustees, err = st.Client.MyTrustees(ctx, token)
		return err
	}); err != nil {
		return 0, err
	}
	adopted := 0
	for _, trustee := range trustees {
		if trustee.CircleID == "" || e2e.SecretKind(trustee.Secret) != "invite" {
			continue
		}
		room, err := e2e.OpenSecretWithInvite(trustee.Secret, invite, trustee.CircleID, name)
		if err != nil {
			return adopted, exit.New(exit.Auth, "%s", i18n.T("The invitation does not open the key of %q: %v", trustee.CircleName, err))
		}
		secret, err := e2e.WrapToKey(room, trustee.CircleID, st.keys.memberID, st.keys.pair.Point, nil)
		if err != nil {
			return adopted, exit.New(exit.Other, "%v", err)
		}
		if err := st.Do(ctx, func(token string) error {
			return st.Client.SetSecret(ctx, token, trustee.CircleID, st.keys.memberID, secret)
		}); err != nil {
			return adopted, err
		}
		st.device().remember(trustee.CircleID)
		st.ForgetRoomState(trustee.CircleID)
		adopted++
	}
	return adopted, nil
}

// keyToTrust is the public key of a member to give a room key to: nil if
// the member has no key, or one other than this device saw before, until
// the person here has accepted it (E2E.md, "Checking keys"). The stone hands
// the keys out, so a changed one doesn't get a room key by itself.
func (st *Stone) keyToTrust(member client.Trustee) []byte {
	point := e2e.ParsePublicKey(member.PublicKey)
	if point == nil || member.MemberID == "" {
		return nil
	}
	if st.CheckKey(member.MemberID, member.PublicKey) == KeyChanged {
		return nil
	}
	return point
}

// Repair is the repair on sight, by an administrator of a chamber: every
// member who has a public key and no secret of the current room key gets
// one. That covers someone added before they had keys, someone who made new
// keys, and someone who joined with a link from before the key was changed.
// It returns how many got the key.
func (st *Stone) Repair(ctx context.Context, circleID string, members []client.Trustee) (int, error) {
	state, err := st.RoomState(ctx, circleID)
	if err != nil || state.Current == nil {
		return 0, err
	}
	admin := false
	for _, member := range members {
		if member.MemberID == st.keys.memberID && (member.TrustLevel == client.TrustAdmin || member.TrustLevel == client.TrustSysop) {
			admin = true
		}
	}
	if !admin {
		return 0, nil
	}
	var secrets []client.TrusteeSecret
	for _, member := range members {
		kind := e2e.SecretKind(member.Secret)
		behind := kind != "key" || e2e.SecretVersion(member.Secret) != state.Current.Version
		point := st.keyToTrust(member)
		if member.MemberID == st.keys.memberID || point == nil || !behind || kind == "invite" {
			continue
		}
		secret, err := e2e.WrapToKey(*state.Current, circleID, member.MemberID, point, nil)
		if err != nil {
			return 0, exit.New(exit.Other, "%v", err)
		}
		secrets = append(secrets, client.TrusteeSecret{MemberID: member.MemberID, Secret: secret})
	}
	if err := st.setSecrets(ctx, circleID, secrets, nil); err != nil {
		return 0, err
	}
	return len(secrets), nil
}

// ChangeKey changes the key of a chamber, after a member was removed: a new
// room key for everyone who is left and has keys, and the earlier keys under
// it, so old content stays readable. All in one step on the stone.
func (st *Stone) ChangeKey(ctx context.Context, circleID string, remaining []client.Trustee) error {
	state, err := st.RoomState(ctx, circleID)
	if err != nil {
		return err
	}
	if state.Current == nil {
		return exit.New(exit.Auth, "%s", state.Refusal())
	}
	next := e2e.RoomKey{Key: e2e.Random(32), Version: state.Current.Version + 1}
	var earlier []e2e.RoomKey
	for version, key := range state.Keys {
		earlier = append(earlier, e2e.RoomKey{Key: key, Version: version})
	}
	own, err := e2e.WrapToKey(next, circleID, st.keys.memberID, st.keys.pair.Point, nil)
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	secrets := []client.TrusteeSecret{{MemberID: st.keys.memberID, Secret: own}}
	for _, member := range remaining {
		if member.MemberID == "" || member.MemberID == st.keys.memberID {
			continue
		}
		// Someone without keys, with a changed key, or with a link still to
		// accept, waits for a repair.
		secret := ""
		if point := st.keyToTrust(member); point != nil {
			if secret, err = e2e.WrapToKey(next, circleID, member.MemberID, point, nil); err != nil {
				return exit.New(exit.Other, "%v", err)
			}
		}
		secrets = append(secrets, client.TrusteeSecret{MemberID: member.MemberID, Secret: secret})
	}
	shared, err := e2e.SharedValue(circleID, next, earlier, nil)
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if err := st.setSecrets(ctx, circleID, secrets, &shared); err != nil {
		return err
	}
	st.ForgetRoomState(circleID)
	return nil
}

// MemberKeyState is what an administrator sees of a member's key in a
// chamber: "none", "invited", "behind" or "current".
func MemberKeyState(member client.Trustee, currentVersion int) string {
	if e2e.SecretKind(member.Secret) == "invite" {
		return "invited"
	}
	if e2e.ParsePublicKey(member.PublicKey) == nil {
		return "none"
	}
	if e2e.SecretKind(member.Secret) == "key" && e2e.SecretVersion(member.Secret) == currentVersion {
		return "current"
	}
	return "behind"
}

// forgetDevice makes the next use read what the device remembers again:
// the account key it is sealed under has changed.
func (st *Stone) forgetDevice() {
	st.keys.deviceMu.Lock()
	st.keys.device = nil
	st.keys.deviceMu.Unlock()
}

// Sealing is what an upload to a room sends: the content itself in a room,
// encrypted as it is read in a chamber, so a file needn't be in memory. The
// length is that of what is sent; -1 if it isn't known.
func (st *Stone) Sealing(ctx context.Context, circleID string, content io.Reader, length int64) (io.Reader, int64, error) {
	state, err := st.RoomState(ctx, circleID)
	if err != nil {
		return nil, 0, err
	}
	if state.Kind == KindRoom {
		return content, length, nil
	}
	if state.Current == nil {
		return nil, 0, exit.New(exit.Auth, "%s", state.Refusal())
	}
	reader, writer := io.Pipe()
	encrypter, err := e2e.NewEncrypter(writer, *state.Current, circleID, "", nil)
	if err != nil {
		return nil, 0, exit.New(exit.Other, "%v", err)
	}
	go func() {
		_, err := io.Copy(encrypter, content)
		if err == nil {
			err = encrypter.Close()
		}
		_ = writer.CloseWithError(err)
	}()
	if length >= 0 {
		length = e2e.EncryptedLength(length)
	}
	return reader, length, nil
}

// Opening is what a download from a room gives: the content itself in a
// room, decrypted as it is read in a chamber. Content of a chamber which no
// app that knows chambers wrote is given as it is, and told: protected is
// false.
func (st *Stone) Opening(ctx context.Context, circleID string, stored io.Reader) (content io.Reader, protected bool, err error) {
	state, err := st.RoomState(ctx, circleID)
	if err != nil {
		return nil, false, err
	}
	if state.Kind == KindRoom {
		return stored, true, nil
	}
	buffered := bufio.NewReaderSize(stored, 64<<10)
	start, _ := buffered.Peek(4)
	if !e2e.IsProtected(start) {
		return buffered, false, nil
	}
	if len(state.Keys) == 0 {
		return nil, false, exit.New(exit.Auth, "%s", state.Refusal())
	}
	decrypter, err := e2e.NewDecrypter(buffered, state.Keys, circleID, "")
	if err != nil {
		return nil, false, exit.New(exit.Other, "%v", err)
	}
	return decrypter, true, nil
}

// ShownSize is the size of a file as its people see it: what the stone
// stores of a chamber's file is a little more.
func (st *Stone) ShownSize(ctx context.Context, circleID string, stored int64) int64 {
	if state, err := st.RoomState(ctx, circleID); err == nil && state.Chamber() {
		return e2e.PlainLength(stored)
	}
	return stored
}
