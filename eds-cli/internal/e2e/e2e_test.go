package e2e

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n/rune"
)

// The vectors of the convention (E2E.md), which an independent
// implementation made; Rune reproduces the same ones.
type vectors struct {
	CircleID string `json:"circleId"`
	MemberID string `json:"memberId"`
	Keys     struct {
		PrivateKey, PublicPoint, PublicKey, Fingerprint, AccountKey, RecoveryKey, DeviceLink string
	} `json:"keys"`
	Vault struct {
		Salt, Nonce, Vault string
	} `json:"vault"`
	SecretByKey struct {
		RoomKey             string `json:"roomKey"`
		Version             int    `json:"version"`
		EphemeralPrivateKey string `json:"ephemeralPrivateKey"`
		Nonce               string `json:"nonce"`
		Secret              string `json:"secret"`
	} `json:"secretByKey"`
	SecretByInvite struct {
		RoomKey      string `json:"roomKey"`
		Version      int    `json:"version"`
		Name         string `json:"name"`
		InviteSecret string `json:"inviteSecret"`
		Nonce        string `json:"nonce"`
		Secret       string `json:"secret"`
		Token        string `json:"token"`
		Link         string `json:"link"`
	} `json:"secretByInvite"`
	SharedValue struct {
		Version int    `json:"version"`
		RoomKey string `json:"roomKey"`
		Earlier map[string]struct {
			RoomKey string `json:"roomKey"`
			Nonce   string `json:"nonce"`
		} `json:"earlier"`
		Value string `json:"value"`
		First string `json:"first"`
	} `json:"sharedValue"`
	Content []content `json:"content"`
	Large   struct {
		Length      int    `json:"length"`
		Version     int    `json:"version"`
		RoomKey     string `json:"roomKey"`
		ChunkSize   int    `json:"chunkSize"`
		Salt        string `json:"salt"`
		NoncePrefix string `json:"noncePrefix"`
		SHA256      string `json:"sha256"`
	} `json:"large"`
}

type content struct {
	Name        string `json:"name"`
	Context     string `json:"context"`
	Version     int    `json:"version"`
	RoomKey     string `json:"roomKey"`
	ChunkSize   int    `json:"chunkSize"`
	Salt        string `json:"salt"`
	NoncePrefix string `json:"noncePrefix"`
	Plain       string `json:"plain"`
	Encrypted   string `json:"encrypted"`
}

func load(t *testing.T) *vectors {
	t.Helper()
	var v vectors
	if err := json.Unmarshal(rune.Vectors, &v); err != nil {
		t.Fatal(err)
	}
	return &v
}

func b(t *testing.T, text string) []byte {
	t.Helper()
	data, err := FromB64u(text)
	if err != nil {
		t.Fatalf("%q: %v", text, err)
	}
	return data
}

func unhex(t *testing.T, text string) []byte {
	t.Helper()
	data, err := hex.DecodeString(text)
	if err != nil {
		t.Fatal(err)
	}
	return data
}

func same(t *testing.T, what string, got, want any) {
	t.Helper()
	g, _ := json.Marshal(got)
	w, _ := json.Marshal(want)
	if !bytes.Equal(g, w) {
		t.Errorf("%s:\n got %s\nwant %s", what, g, w)
	}
}

func TestKeys(t *testing.T) {
	v := load(t)
	pair, err := KeyPairOf(b(t, v.Keys.PrivateKey))
	if err != nil {
		t.Fatal(err)
	}
	same(t, "the public point", B64u(pair.Point), v.Keys.PublicPoint)
	same(t, "the public key as text", PublicKeyText(pair.Point), v.Keys.PublicKey)
	same(t, "and read again", ParsePublicKey(v.Keys.PublicKey), pair.Point)
	same(t, "the fingerprint", Fingerprint(pair.Point), v.Keys.Fingerprint)
	account := b(t, v.Keys.AccountKey)
	same(t, "the recovery key", RecoveryKey(account), v.Keys.RecoveryKey)
	same(t, "as typed, in lower case with blanks", ParseRecoveryKey(strings.ToLower(strings.ReplaceAll(v.Keys.RecoveryKey, "-", " "))), account)
	same(t, "the device link", DeviceLink(account), v.Keys.DeviceLink)
	same(t, "and read again", ParseDeviceLink(v.Keys.DeviceLink), account)
	for _, wrong := range []string{"", "eds-e2e:1:1:AAAA", "eds-e2e:2:1:" + v.Keys.PublicPoint, "-----BEGIN PUBLIC KEY-----"} {
		if ParsePublicKey(wrong) != nil {
			t.Errorf("%q was read as a public key", wrong)
		}
	}
	if ParseRecoveryKey("ABCD-EFGH") != nil || ParseRecoveryKey("not a key!") != nil || ParseDeviceLink("#invite=abc") != nil {
		t.Error("something which isn't a key was read as one")
	}
}

func TestVault(t *testing.T) {
	v := load(t)
	account := b(t, v.Keys.AccountKey)
	vault, err := SealVault(account, v.MemberID, b(t, v.Keys.PrivateKey), b(t, v.Vault.Salt), b(t, v.Vault.Nonce))
	if err != nil {
		t.Fatal(err)
	}
	same(t, "the vault", vault, v.Vault.Vault)
	pair, err := OpenVault(v.Vault.Vault, account, v.MemberID)
	if err != nil {
		t.Fatal(err)
	}
	same(t, "the key from the vault", B64u(pair.Scalar()), v.Keys.PrivateKey)
	if _, err := OpenVault(v.Vault.Vault, account, "another-member"); err == nil {
		t.Error("the vault opened for another Member")
	}
	if _, err := OpenVault(v.Vault.Vault, make([]byte, 32), v.MemberID); err == nil {
		t.Error("the vault opened with another account key")
	}
	if _, err := OpenVault(`{"hello":1}`, account, v.MemberID); err == nil {
		t.Error("something else opened as a vault")
	}
	fresh, err := SealVault(account, v.MemberID, b(t, v.Keys.PrivateKey), nil, nil)
	if err != nil || fresh == v.Vault.Vault {
		t.Error("a vault with random salt and nonce is the same as the fixed one")
	}
	if _, err := OpenVault(fresh, account, v.MemberID); err != nil {
		t.Error(err)
	}
}

func TestSecrets(t *testing.T) {
	v := load(t)
	pair, _ := KeyPairOf(b(t, v.Keys.PrivateKey))
	room := RoomKey{Key: b(t, v.SecretByKey.RoomKey), Version: v.SecretByKey.Version}
	ephemeral, err := KeyPairOf(b(t, v.SecretByKey.EphemeralPrivateKey))
	if err != nil {
		t.Fatal(err)
	}
	secret, err := WrapToKey(room, v.CircleID, v.MemberID, pair.Point, &Fixed{Ephemeral: ephemeral, Nonce: b(t, v.SecretByKey.Nonce)})
	if err != nil {
		t.Fatal(err)
	}
	same(t, "a secret to a public key", secret, v.SecretByKey.Secret)
	same(t, "its kind and version", []any{SecretKind(secret), SecretVersion(secret)}, []any{"key", room.Version})
	opened, err := OpenSecretWithKey(v.SecretByKey.Secret, pair, v.CircleID, v.MemberID)
	if err != nil {
		t.Fatal(err)
	}
	same(t, "the room key from it", opened, room)
	if _, err := OpenSecretWithKey(v.SecretByKey.Secret, pair, "another-circle", v.MemberID); err == nil {
		t.Error("the secret opened for another Circle")
	}
	random, err := WrapToKey(room, v.CircleID, v.MemberID, pair.Point, nil)
	if err != nil || random == secret {
		t.Error("a secret with a random ephemeral key is the same as the fixed one")
	}
	if again, err := OpenSecretWithKey(random, pair, v.CircleID, v.MemberID); err != nil || !bytes.Equal(again.Key, room.Key) {
		t.Errorf("a fresh secret doesn't open: %v", err)
	}

	invite := v.SecretByInvite
	inviteSecret := b(t, invite.InviteSecret)
	invited, err := WrapForInvite(room, v.CircleID, invite.Name, inviteSecret, b(t, invite.Nonce))
	if err != nil {
		t.Fatal(err)
	}
	same(t, "a secret of an invitation", invited, invite.Secret)
	same(t, "its kind and version", []any{SecretKind(invited), SecretVersion(invited)}, []any{"invite", room.Version})
	fromInvite, err := OpenSecretWithInvite(invite.Secret, inviteSecret, v.CircleID, invite.Name)
	if err != nil {
		t.Fatal(err)
	}
	same(t, "the room key from it", fromInvite, room)
	if _, err := OpenSecretWithInvite(invite.Secret, inviteSecret, v.CircleID, "Someone Else"); err == nil {
		t.Error("the invitation's secret opened under another name")
	}
	if _, err := OpenSecretWithKey(invite.Secret, pair, v.CircleID, v.MemberID); err == nil {
		t.Error("the invitation's secret opened as one to a key")
	}
	link := InvitationFragment(Invitation{Name: invite.Name, Token: invite.Token, Secret: inviteSecret})
	same(t, "the link of the invitation", link, invite.Link)
	same(t, "and read again", ParseInvitationFragment(invite.Link), &Invitation{Name: invite.Name, Token: invite.Token, Secret: inviteSecret})
	plain := ParseInvitationFragment(InvitationFragment(Invitation{Name: "Åsa <&>", Token: "t"}))
	same(t, "a link of a room, with a name of other letters", plain, &Invitation{Name: "Åsa <&>", Token: "t"})
	if ParseInvitationFragment("#device=abc") != nil || ParseInvitationFragment("#invite=!!!") != nil {
		t.Error("something else was read as an invitation")
	}
	same(t, "what isn't of the convention", []any{SecretKind(""), SecretKind("-----BEGIN"), SecretVersion(`{"e2e":2}`)}, []any{"", "", 0})
}

func TestSharedValue(t *testing.T) {
	v := load(t)
	shared := v.SharedValue
	current := RoomKey{Key: b(t, shared.RoomKey), Version: shared.Version}
	earlier := []RoomKey{{Key: b(t, shared.Earlier["2"].RoomKey), Version: 2}, {Key: b(t, shared.Earlier["1"].RoomKey), Version: 1}}
	value, err := SharedValue(v.CircleID, current, earlier, map[int][]byte{1: b(t, shared.Earlier["1"].Nonce), 2: b(t, shared.Earlier["2"].Nonce)})
	if err != nil {
		t.Fatal(err)
	}
	same(t, "the shared value with two earlier keys", value, shared.Value)
	first, _ := SharedValue(v.CircleID, RoomKey{Key: current.Key, Version: 1}, nil, nil)
	same(t, "that of a new chamber", first, shared.First)
	same(t, "a chamber, and its version", []any{IsChamber(value), ChamberVersion(value), IsChamber(""), IsChamber("a key of another kind"), ChamberVersion("x")}, []any{true, 3, false, false, 0})
	keys, err := OpenSharedValue(shared.Value, v.CircleID, current)
	if err != nil {
		t.Fatal(err)
	}
	same(t, "all keys of the chamber", keys, map[int][]byte{1: earlier[1].Key, 2: earlier[0].Key, 3: current.Key})
	behind, err := OpenSharedValue(shared.Value, v.CircleID, RoomKey{Key: earlier[0].Key, Version: 2})
	if err != nil {
		t.Fatal(err)
	}
	same(t, "with a secret of an earlier key, only that one", len(behind), 1)
	if _, err := OpenSharedValue(shared.Value, v.CircleID, RoomKey{Key: make([]byte, 32), Version: 3}); err == nil {
		t.Error("the earlier keys opened with a wrong room key")
	}
}

func TestContent(t *testing.T) {
	v := load(t)
	for _, c := range v.Content {
		room := RoomKey{Key: b(t, c.RoomKey), Version: c.Version}
		options := &ContentOptions{ChunkSize: c.ChunkSize, Salt: b(t, c.Salt), NoncePrefix: unhex(t, c.NoncePrefix)}
		encrypted, err := EncryptContent(room, v.CircleID, c.Context, b(t, c.Plain), options)
		if err != nil {
			t.Fatal(err)
		}
		same(t, c.Name+": encrypted", B64u(encrypted), c.Encrypted)
		keys := map[int][]byte{c.Version: room.Key}
		plain, err := DecryptContent(keys, v.CircleID, c.Context, b(t, c.Encrypted))
		if err != nil {
			t.Fatalf("%s: %v", c.Name, err)
		}
		same(t, c.Name+": decrypted", B64u(plain), c.Plain)
		if _, err := DecryptContent(keys, v.CircleID, c.Context+"x", b(t, c.Encrypted)); err == nil {
			t.Errorf("%s: opened as something else", c.Name)
		}
		if _, err := DecryptContent(keys, "another-circle", c.Context, b(t, c.Encrypted)); err == nil {
			t.Errorf("%s: opened for another Circle", c.Name)
		}
		if _, err := DecryptContent(map[int][]byte{c.Version + 1: room.Key}, v.CircleID, c.Context, b(t, c.Encrypted)); err == nil {
			t.Errorf("%s: opened without its key", c.Name)
		}
		stored := b(t, c.Encrypted)
		if len(stored) > 54+16 {
			// Without its last chunk, or with one byte less, it is cut off.
			if _, err := DecryptContent(keys, v.CircleID, c.Context, stored[:len(stored)-1]); err == nil {
				t.Errorf("%s: opened with a byte missing", c.Name)
			}
			step := c.ChunkSize + 16
			if (len(stored)-54)%step == 0 && len(stored)-54 > step {
				if _, err := DecryptContent(keys, v.CircleID, c.Context, stored[:len(stored)-step]); err == nil {
					t.Errorf("%s: opened without its last chunk", c.Name)
				}
			}
		}
	}
	if _, err := DecryptContent(map[int][]byte{1: make([]byte, 32)}, v.CircleID, "", []byte("plain text of an app which doesn't know chambers")); !errors.Is(err, ErrNotProtected) {
		t.Errorf("content which isn't protected: %v", err)
	}
}

// A file of two full chunks and a rest goes through as a stream, in pieces
// of odd sizes, and comes back the same.
func TestLargeContentAsStream(t *testing.T) {
	v := load(t)
	large := v.Large
	plain := make([]byte, large.Length)
	for i := range plain {
		plain[i] = byte(i*7 + 3)
	}
	room := RoomKey{Key: b(t, large.RoomKey), Version: large.Version}
	var out bytes.Buffer
	encrypter, err := NewEncrypter(&out, room, v.CircleID, "", &ContentOptions{ChunkSize: large.ChunkSize, Salt: b(t, large.Salt), NoncePrefix: unhex(t, large.NoncePrefix)})
	if err != nil {
		t.Fatal(err)
	}
	for rest := plain; len(rest) > 0; {
		n := min(len(rest), 4099)
		if _, err := encrypter.Write(rest[:n]); err != nil {
			t.Fatal(err)
		}
		rest = rest[n:]
	}
	if err := encrypter.Close(); err != nil {
		t.Fatal(err)
	}
	sum := sha256.Sum256(out.Bytes())
	same(t, "the encrypted file, by its hash", hex.EncodeToString(sum[:]), large.SHA256)
	same(t, "its length", int64(out.Len()), EncryptedLength(int64(large.Length)))
	same(t, "and back", PlainLength(int64(out.Len())), int64(large.Length))
	decrypter, err := NewDecrypter(iotest(out.Bytes(), 1500), map[int][]byte{large.Version: room.Key}, v.CircleID, "")
	if err != nil {
		t.Fatal(err)
	}
	back, err := io.ReadAll(decrypter)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(back, plain) {
		t.Error("the file came back changed")
	}
	same(t, "lengths", []int64{EncryptedLength(0), EncryptedLength(1), EncryptedLength(65536), EncryptedLength(65537), PlainLength(70), PlainLength(54 + 65536 + 16), PlainLength(54 + 65537 + 32), PlainLength(10)},
		[]int64{70, 71, 54 + 65536 + 16, 54 + 65537 + 32, 0, 65536, 65537, 0})
}

// iotest reads the data in small pieces, as a network does.
func iotest(data []byte, piece int) io.Reader {
	return &pieces{data: data, piece: piece}
}

type pieces struct {
	data  []byte
	piece int
}

func (p *pieces) Read(out []byte) (int, error) {
	if len(p.data) == 0 {
		return 0, io.EOF
	}
	n := copy(out, p.data[:min(len(p.data), p.piece, len(out))])
	p.data = p.data[n:]
	return n, nil
}
