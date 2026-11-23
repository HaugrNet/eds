// Package e2e is the convention of Rune and Tolk for end-to-end encryption,
// version 1, suite 1 (E2E.md): P-384, HKDF-SHA-512 and AES-256-GCM, all of
// Go's standard library. The stone takes no part in any of this: it stores
// what these functions make, without looking inside.
//
// It says the same as Rune's src/e2e/convention.ts, and is tested against
// the same vectors, which an independent implementation made.
package e2e

import (
	"bufio"
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/ecdh"
	"crypto/hkdf"
	"crypto/rand"
	"crypto/sha256"
	"crypto/sha512"
	"encoding/base32"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"regexp"
	"sort"
	"strconv"
	"strings"
)

const (
	prefix          = "eds-e2e/v1/"
	publicKeyPrefix = "eds-e2e:1:1:"
	magic           = "EDSE"
	headerLength    = 54
	tagLength       = 16
	// ChunkSize is the chunk size writers use: 64 KiB.
	ChunkSize = 65536
	minChunk  = 16
	maxChunk  = 16 * 1024 * 1024
)

// Error is a failure of the convention: what was read isn't what it should
// be.
type Error struct{ Message string }

func (e *Error) Error() string { return e.Message }

func failure(format string, args ...any) error {
	return &Error{Message: fmt.Sprintf(format, args...)}
}

// --- Bytes ---------------------------------------------------------------------

// B64u is Base64, URL-safe, without padding.
func B64u(data []byte) string { return base64.RawURLEncoding.EncodeToString(data) }

// FromB64u reads Base64, URL-safe or standard, with or without padding.
func FromB64u(text string) ([]byte, error) {
	clean := strings.TrimRight(strings.NewReplacer("+", "-", "/", "_").Replace(strings.TrimSpace(text)), "=")
	return base64.RawURLEncoding.DecodeString(clean)
}

func random(length int) []byte {
	data := make([]byte, length)
	if _, err := rand.Read(data); err != nil {
		panic(err) // The system has no randomness: nothing can be done safely.
	}
	return data
}

// Random returns random bytes, such as an account key or an invite secret.
func Random(length int) []byte { return random(length) }

// --- Primitives ----------------------------------------------------------------

func derive(secret, salt []byte, info string) (cipher.AEAD, error) {
	key, err := hkdf.Key(sha512.New, secret, salt, info, 32)
	if err != nil {
		return nil, err
	}
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}
	return cipher.NewGCM(block)
}

// seal is AES-256-GCM: nonce (12) | ciphertext | tag (16).
func seal(aead cipher.AEAD, aad string, plain, nonce []byte) []byte {
	if nonce == nil {
		nonce = random(12)
	}
	return aead.Seal(append([]byte{}, nonce...), nonce, plain, []byte(aad))
}

func open(aead cipher.AEAD, aad string, sealed []byte, what string) ([]byte, error) {
	if len(sealed) < 12+tagLength {
		return nil, failure("%s is too short.", what)
	}
	plain, err := aead.Open(nil, sealed[:12], sealed[12:], []byte(aad))
	if err != nil {
		return nil, failure("%s can't be opened: it is damaged, or not meant for this key.", what)
	}
	return plain, nil
}

func parse(text, what string) (map[string]any, error) {
	var value map[string]any
	if err := json.Unmarshal([]byte(text), &value); err != nil || value == nil {
		return nil, failure("%s isn't of this convention.", what)
	}
	if number, ok := value["e2e"].(float64); !ok || number != 1 {
		return nil, failure("%s isn't of this convention.", what)
	}
	if number, ok := value["suite"].(float64); !ok || number != 1 {
		return nil, failure("%s uses a suite this app doesn't know.", what)
	}
	return value, nil
}

func field(value map[string]any, name, what string) ([]byte, error) {
	text, ok := value[name].(string)
	if !ok {
		return nil, failure("%s is incomplete.", what)
	}
	data, err := FromB64u(text)
	if err != nil {
		return nil, failure("%s is incomplete.", what)
	}
	return data, nil
}

func version(value map[string]any, what string) (int, error) {
	number, ok := value["version"].(float64)
	if !ok || number < 1 || number != float64(int(number)) {
		return 0, failure("%s is incomplete.", what)
	}
	return int(number), nil
}

// --- The Member's keys -----------------------------------------------------------

// KeyPair is a Member's key pair: the private key, and the public point (97
// bytes, uncompressed).
type KeyPair struct {
	Private *ecdh.PrivateKey
	Point   []byte
}

// Scalar is the private key as it goes into the vault: 48 bytes.
func (k *KeyPair) Scalar() []byte { return k.Private.Bytes() }

// GenerateKeyPair makes a new key pair.
func GenerateKeyPair() (*KeyPair, error) {
	private, err := ecdh.P384().GenerateKey(rand.Reader)
	if err != nil {
		return nil, err
	}
	return &KeyPair{Private: private, Point: private.PublicKey().Bytes()}, nil
}

// KeyPairOf is the key pair of a private key's scalar.
func KeyPairOf(scalar []byte) (*KeyPair, error) {
	private, err := ecdh.P384().NewPrivateKey(scalar)
	if err != nil {
		return nil, failure("The vault does not hold a private key.")
	}
	return &KeyPair{Private: private, Point: private.PublicKey().Bytes()}, nil
}

// PublicKeyText is Member.publicKey: the convention, its version, the suite,
// and the point.
func PublicKeyText(point []byte) string { return publicKeyPrefix + B64u(point) }

// ParsePublicKey is the point of a Member.publicKey, or nil if the key isn't
// of this convention.
func ParsePublicKey(value string) []byte {
	if !strings.HasPrefix(value, publicKeyPrefix) {
		return nil
	}
	point, err := FromB64u(value[len(publicKeyPrefix):])
	if err != nil || len(point) != 97 || point[0] != 4 {
		return nil
	}
	if _, err := ecdh.P384().NewPublicKey(point); err != nil {
		return nil
	}
	return point
}

// Fingerprint is what two people compare: eight groups of four hex digits.
func Fingerprint(point []byte) string {
	sum := sha256.Sum256(point)
	digits := hex.EncodeToString(sum[:])[:32]
	groups := make([]string, 0, 8)
	for i := 0; i < len(digits); i += 4 {
		groups = append(groups, digits[i:i+4])
	}
	return strings.Join(groups, " ")
}

var base32Plain = base32.StdEncoding.WithPadding(base32.NoPadding)

// RecoveryKey is the account key as people write it down.
func RecoveryKey(accountKey []byte) string {
	text := base32Plain.EncodeToString(accountKey)
	groups := []string{}
	for i := 0; i < len(text); i += 4 {
		groups = append(groups, text[i:min(i+4, len(text))])
	}
	return strings.Join(groups, "-")
}

// ParseRecoveryKey is the account key of a recovery key as typed (blanks,
// hyphens and case don't matter), or nil if it isn't one.
func ParseRecoveryKey(typed string) []byte {
	clean := strings.TrimRight(strings.Map(func(r rune) rune {
		if r == '-' || r == ' ' || r == '\t' || r == '\n' || r == '\r' {
			return -1
		}
		return r
	}, strings.ToUpper(typed)), "=")
	data, err := base32Plain.DecodeString(clean)
	if err != nil || len(data) != 32 {
		return nil
	}
	return data
}

// DeviceLink is the fragment which gives a further device the account key.
func DeviceLink(accountKey []byte) string { return "#device=" + B64u(accountKey) }

// ParseDeviceLink is the account key of a device link, or nil.
func ParseDeviceLink(fragment string) []byte {
	if !strings.HasPrefix(fragment, "#device=") {
		return nil
	}
	data, err := FromB64u(fragment[8:])
	if err != nil || len(data) != 32 {
		return nil
	}
	return data
}

// SealVault is Member.vault: the private key under the account key. Salt and
// nonce are random unless given (the tests give them).
func SealVault(accountKey []byte, memberID string, scalar, salt, nonce []byte) (string, error) {
	if salt == nil {
		salt = random(32)
	}
	label := prefix + "vault|" + memberID
	aead, err := derive(accountKey, salt, label)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf(`{"e2e":1,"suite":1,"salt":"%s","key":"%s"}`, B64u(salt), B64u(seal(aead, label, scalar, nonce))), nil
}

// OpenVault is the key pair from the vault.
func OpenVault(vault string, accountKey []byte, memberID string) (*KeyPair, error) {
	value, err := parse(vault, "The vault")
	if err != nil {
		return nil, err
	}
	salt, err := field(value, "salt", "The vault")
	if err != nil {
		return nil, err
	}
	sealed, err := field(value, "key", "The vault")
	if err != nil {
		return nil, err
	}
	label := prefix + "vault|" + memberID
	aead, err := derive(accountKey, salt, label)
	if err != nil {
		return nil, err
	}
	scalar, err := open(aead, label, sealed, "The vault")
	if err != nil {
		return nil, err
	}
	if len(scalar) != 48 {
		return nil, failure("The vault does not hold a private key.")
	}
	return KeyPairOf(scalar)
}

// --- The room key ----------------------------------------------------------------

// RoomKey is a room key with its version.
type RoomKey struct {
	Key     []byte
	Version int
}

// Fixed is, for the tests, the ephemeral key and the nonce which are random
// otherwise.
type Fixed struct {
	Ephemeral *KeyPair
	Nonce     []byte
}

func shared(private *ecdh.PrivateKey, point []byte) ([]byte, error) {
	public, err := ecdh.P384().NewPublicKey(point)
	if err != nil {
		return nil, failure("The public key is not a point of P-384.")
	}
	return private.ECDH(public)
}

// WrapToKey is Trustee.secret: the room key wrapped to a Member's public
// key.
func WrapToKey(room RoomKey, circleID, memberID string, memberPoint []byte, fixed *Fixed) (string, error) {
	var ephemeral *KeyPair
	var nonce []byte
	if fixed != nil {
		ephemeral, nonce = fixed.Ephemeral, fixed.Nonce
	}
	if ephemeral == nil {
		generated, err := GenerateKeyPair()
		if err != nil {
			return "", err
		}
		ephemeral = generated
	}
	secret, err := shared(ephemeral.Private, memberPoint)
	if err != nil {
		return "", err
	}
	label := fmt.Sprintf("%ssecret|%s|%s|%d", prefix, circleID, memberID, room.Version)
	aead, err := derive(secret, append(append([]byte{}, ephemeral.Point...), memberPoint...), label)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf(`{"e2e":1,"suite":1,"by":"key","version":%d,"epk":"%s","key":"%s"}`,
		room.Version, B64u(ephemeral.Point), B64u(seal(aead, label, room.Key, nonce))), nil
}

// WrapForInvite is Trustee.secret of an invitation: the room key under the
// invite secret of the link.
func WrapForInvite(room RoomKey, circleID, name string, inviteSecret, nonce []byte) (string, error) {
	label := fmt.Sprintf("%sinvite|%s|%s|%d", prefix, circleID, name, room.Version)
	aead, err := derive(inviteSecret, nil, label)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf(`{"e2e":1,"suite":1,"by":"invite","version":%d,"key":"%s"}`, room.Version, B64u(seal(aead, label, room.Key, nonce))), nil
}

// SecretKind is how a secret is wrapped: "key" to the Member's key, "invite"
// under an invite secret, "" if it isn't of this convention.
func SecretKind(secret string) string {
	if secret == "" {
		return ""
	}
	value, err := parse(secret, "The secret")
	if err != nil {
		return ""
	}
	if by, _ := value["by"].(string); by == "key" || by == "invite" {
		return by
	}
	return ""
}

// SecretVersion is the version of the room key in a secret, without opening
// it; 0 if it isn't of this convention.
func SecretVersion(secret string) int {
	if secret == "" {
		return 0
	}
	value, err := parse(secret, "The secret")
	if err != nil {
		return 0
	}
	found, err := version(value, "The secret")
	if err != nil {
		return 0
	}
	return found
}

func roomKey(key []byte) ([]byte, error) {
	if len(key) != 32 {
		return nil, failure("The secret does not hold a room key.")
	}
	return key, nil
}

// OpenSecretWithKey is the room key of a secret which was wrapped to the
// Member's public key.
func OpenSecretWithKey(secret string, pair *KeyPair, circleID, memberID string) (RoomKey, error) {
	value, err := parse(secret, "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	if by, _ := value["by"].(string); by != "key" {
		return RoomKey{}, failure("The secret is not wrapped to a key.")
	}
	roomVersion, err := version(value, "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	epk, err := field(value, "epk", "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	sealed, err := field(value, "key", "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	secretBytes, err := shared(pair.Private, epk)
	if err != nil {
		return RoomKey{}, err
	}
	label := fmt.Sprintf("%ssecret|%s|%s|%d", prefix, circleID, memberID, roomVersion)
	aead, err := derive(secretBytes, append(append([]byte{}, epk...), pair.Point...), label)
	if err != nil {
		return RoomKey{}, err
	}
	plain, err := open(aead, label, sealed, "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	key, err := roomKey(plain)
	return RoomKey{Key: key, Version: roomVersion}, err
}

// OpenSecretWithInvite is the room key of the secret of an invitation.
func OpenSecretWithInvite(secret string, inviteSecret []byte, circleID, name string) (RoomKey, error) {
	value, err := parse(secret, "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	if by, _ := value["by"].(string); by != "invite" {
		return RoomKey{}, failure("The secret is not of an invitation.")
	}
	roomVersion, err := version(value, "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	sealed, err := field(value, "key", "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	label := fmt.Sprintf("%sinvite|%s|%s|%d", prefix, circleID, name, roomVersion)
	aead, err := derive(inviteSecret, nil, label)
	if err != nil {
		return RoomKey{}, err
	}
	plain, err := open(aead, label, sealed, "The secret")
	if err != nil {
		return RoomKey{}, err
	}
	key, err := roomKey(plain)
	return RoomKey{Key: key, Version: roomVersion}, err
}

// SharedValue is the Circle's shared value: it marks the chamber, and holds
// the earlier room keys under the current one. The nonces are random unless
// given by version (the tests give them).
func SharedValue(circleID string, current RoomKey, earlier []RoomKey, nonces map[int][]byte) (string, error) {
	base := fmt.Sprintf("%searlier|%s|%d", prefix, circleID, current.Version)
	aead, err := derive(current.Key, nil, base)
	if err != nil {
		return "", err
	}
	sorted := append([]RoomKey{}, earlier...)
	sort.Slice(sorted, func(i, j int) bool { return sorted[i].Version < sorted[j].Version })
	parts := make([]string, 0, len(sorted))
	for _, old := range sorted {
		parts = append(parts, fmt.Sprintf(`"%d":"%s"`, old.Version, B64u(seal(aead, fmt.Sprintf("%s|%d", base, old.Version), old.Key, nonces[old.Version]))))
	}
	return fmt.Sprintf(`{"e2e":1,"suite":1,"version":%d,"earlier":{%s}}`, current.Version, strings.Join(parts, ",")), nil
}

// IsChamber says whether a Circle's shared value marks a chamber of this
// convention.
func IsChamber(value string) bool {
	if value == "" {
		return false
	}
	var parsed map[string]any
	if err := json.Unmarshal([]byte(value), &parsed); err != nil {
		return false
	}
	number, ok := parsed["e2e"].(float64)
	return ok && number == 1
}

// ChamberVersion is the version of the room key which a chamber's shared
// value names; 0 if it isn't a chamber.
func ChamberVersion(value string) int {
	if value == "" {
		return 0
	}
	parsed, err := parse(value, "The room")
	if err != nil {
		return 0
	}
	found, err := version(parsed, "The room")
	if err != nil {
		return 0
	}
	return found
}

var positive = regexp.MustCompile(`^[1-9][0-9]*$`)

// OpenSharedValue returns all room keys of a chamber by their versions: the
// current one, and the earlier ones from the shared value.
func OpenSharedValue(value, circleID string, current RoomKey) (map[int][]byte, error) {
	parsed, err := parse(value, "The room")
	if err != nil {
		return nil, err
	}
	keys := map[int][]byte{current.Version: current.Key}
	named, err := version(parsed, "The room")
	if err != nil {
		return nil, err
	}
	if named != current.Version {
		// The room's key was changed, and this Member's secret isn't of the new one yet.
		return keys, nil
	}
	base := fmt.Sprintf("%searlier|%s|%d", prefix, circleID, current.Version)
	aead, err := derive(current.Key, nil, base)
	if err != nil {
		return nil, err
	}
	earlier, _ := parsed["earlier"].(map[string]any)
	for oldVersion, sealed := range earlier {
		text, ok := sealed.(string)
		if !ok || !positive.MatchString(oldVersion) {
			continue
		}
		data, err := FromB64u(text)
		if err != nil {
			return nil, failure("An earlier key of the room is incomplete.")
		}
		plain, err := open(aead, base+"|"+oldVersion, data, "An earlier key of the room")
		if err != nil {
			return nil, err
		}
		key, err := roomKey(plain)
		if err != nil {
			return nil, err
		}
		number, _ := strconv.Atoi(oldVersion)
		keys[number] = key
	}
	return keys, nil
}

// --- The invitation link ----------------------------------------------------------

// Invitation is what an invitation link carries: the name, the stone's
// token, and for a chamber the invite secret.
type Invitation struct {
	Name   string
	Token  string
	Secret []byte
}

func jsonText(text string) string {
	var buffer bytes.Buffer
	encoder := json.NewEncoder(&buffer)
	encoder.SetEscapeHTML(false)
	_ = encoder.Encode(text)
	return strings.TrimRight(buffer.String(), "\n")
}

// InvitationFragment is the fragment of an invitation link.
func InvitationFragment(invitation Invitation) string {
	payload := fmt.Sprintf(`{"n":%s,"t":%s`, jsonText(invitation.Name), jsonText(invitation.Token))
	if invitation.Secret != nil {
		payload += fmt.Sprintf(`,"k":"%s"`, B64u(invitation.Secret))
	}
	return "#invite=" + B64u([]byte(payload+"}"))
}

// ParseInvitationFragment reads the fragment of an invitation link; nil if
// it isn't one.
func ParseInvitationFragment(fragment string) *Invitation {
	if !strings.HasPrefix(fragment, "#invite=") {
		return nil
	}
	data, err := FromB64u(fragment[8:])
	if err != nil {
		return nil
	}
	var value struct {
		N *string `json:"n"`
		T *string `json:"t"`
		K *string `json:"k"`
	}
	if err := json.Unmarshal(data, &value); err != nil || value.N == nil || value.T == nil {
		return nil
	}
	invitation := &Invitation{Name: *value.N, Token: *value.T}
	if value.K != nil {
		secret, err := FromB64u(*value.K)
		if err != nil || len(secret) != 32 {
			return nil
		}
		invitation.Secret = secret
	}
	return invitation
}

// --- Content ---------------------------------------------------------------------

// ContentOptions are, for the tests, what is random otherwise, and a chunk
// size other than the usual.
type ContentOptions struct {
	ChunkSize   int
	Salt        []byte
	NoncePrefix []byte
}

func contentHeader(roomVersion, chunkSize int, salt, noncePrefix []byte) []byte {
	header := make([]byte, 0, headerLength)
	header = append(header, magic...)
	header = append(header, 1, 1)
	header = binary.BigEndian.AppendUint32(header, uint32(roomVersion))
	header = append(header, 1)
	header = binary.BigEndian.AppendUint32(header, uint32(chunkSize))
	header = append(header, salt...)
	return append(header, noncePrefix...)
}

func contentKey(room, salt []byte, circleID string, roomVersion int) (cipher.AEAD, error) {
	return derive(room, salt, fmt.Sprintf("%scontent|%s|%d", prefix, circleID, roomVersion))
}

func chunkNonce(noncePrefix []byte, index uint32, last bool) []byte {
	nonce := append([]byte{}, noncePrefix...)
	nonce = binary.BigEndian.AppendUint32(nonce, index)
	if last {
		return append(nonce, 1)
	}
	return append(nonce, 0)
}

// EncryptedLength is the length of a content once it is encrypted.
func EncryptedLength(plainLength int64) int64 {
	chunks := max(1, (plainLength+ChunkSize-1)/ChunkSize)
	return headerLength + plainLength + chunks*tagLength
}

// PlainLength is the length of a content once it is decrypted, from its
// stored length; for the usual chunk size.
func PlainLength(storedLength int64) int64 {
	body := storedLength - headerLength
	if body < tagLength {
		return 0
	}
	chunks := max(1, (body+ChunkSize+tagLength-1)/(ChunkSize+tagLength))
	return body - chunks*tagLength
}

// IsProtected says whether the bytes start as content of this convention
// does.
func IsProtected(start []byte) bool {
	return len(start) >= len(magic) && string(start[:len(magic)]) == magic
}

// Encrypter encrypts a file, a thumbnail or an entry of a feature as it is
// written, chunk by chunk, so a file needn't be in memory. Close seals the
// last chunk.
type Encrypter struct {
	out      io.Writer
	aead     cipher.AEAD
	aad      []byte
	prefix   []byte
	size     int
	pending  []byte
	index    uint32
	started  bool
	header   []byte
	finished bool
}

// NewEncrypter starts the encryption of a content to a writer. The context
// says what it is: empty for a file, "thumbnail", or "<feature>|<kind>".
func NewEncrypter(out io.Writer, room RoomKey, circleID, context string, options *ContentOptions) (*Encrypter, error) {
	size := ChunkSize
	var salt, noncePrefix []byte
	if options != nil {
		if options.ChunkSize > 0 {
			size = options.ChunkSize
		}
		salt, noncePrefix = options.Salt, options.NoncePrefix
	}
	if salt == nil {
		salt = random(32)
	}
	if noncePrefix == nil {
		noncePrefix = random(7)
	}
	header := contentHeader(room.Version, size, salt, noncePrefix)
	aead, err := contentKey(room.Key, salt, circleID, room.Version)
	if err != nil {
		return nil, err
	}
	return &Encrypter{
		out: out, aead: aead, prefix: noncePrefix, size: size, header: header,
		aad: append(append([]byte{}, header...), []byte("|"+circleID+"|"+context)...),
	}, nil
}

func (e *Encrypter) start() error {
	if e.started {
		return nil
	}
	e.started = true
	_, err := e.out.Write(e.header)
	return err
}

func (e *Encrypter) sealChunk(chunk []byte, last bool) error {
	if err := e.start(); err != nil {
		return err
	}
	_, err := e.out.Write(e.aead.Seal(nil, chunkNonce(e.prefix, e.index, last), chunk, e.aad))
	e.index++
	return err
}

// Write takes more of the content.
func (e *Encrypter) Write(data []byte) (int, error) {
	written := len(data)
	for len(data) > 0 {
		// A full chunk waits until more comes: it may be the last.
		if len(e.pending) == e.size {
			if err := e.sealChunk(e.pending, false); err != nil {
				return 0, err
			}
			e.pending = e.pending[:0]
		}
		take := min(e.size-len(e.pending), len(data))
		e.pending = append(e.pending, data[:take]...)
		data = data[take:]
	}
	return written, nil
}

// Close seals what is left as the last chunk; a content without bytes is
// one empty chunk.
func (e *Encrypter) Close() error {
	if e.finished {
		return nil
	}
	e.finished = true
	return e.sealChunk(e.pending, true)
}

// EncryptContent encrypts a content which is in memory.
func EncryptContent(room RoomKey, circleID, context string, plain []byte, options *ContentOptions) ([]byte, error) {
	var out bytes.Buffer
	encrypter, err := NewEncrypter(&out, room, circleID, context, options)
	if err != nil {
		return nil, err
	}
	if _, err := encrypter.Write(plain); err != nil {
		return nil, err
	}
	if err := encrypter.Close(); err != nil {
		return nil, err
	}
	return out.Bytes(), nil
}

// Decrypter decrypts a content as it is read, chunk by chunk.
type Decrypter struct {
	in     *bufio.Reader
	aead   cipher.AEAD
	aad    []byte
	prefix []byte
	step   int
	index  uint32
	plain  []byte
	done   bool
}

// ErrNotProtected is returned for content of a chamber which doesn't start
// as the convention's does: an app which doesn't know chambers wrote it.
var ErrNotProtected = &Error{Message: "The content is not protected: it was not written by an app which knows chambers."}

// NewDecrypter starts the decryption of a content from a reader. The keys
// are the room keys of the chamber by their versions; the context is what
// the reader expects the content to be.
func NewDecrypter(in io.Reader, keys map[int][]byte, circleID, context string) (*Decrypter, error) {
	reader := bufio.NewReaderSize(in, ChunkSize+tagLength+headerLength)
	header := make([]byte, headerLength)
	n, err := io.ReadFull(reader, header)
	if !IsProtected(header[:n]) {
		return nil, ErrNotProtected
	}
	if err != nil {
		return nil, failure("The content is too short.")
	}
	if header[4] != 1 || header[10] != 1 {
		return nil, failure("The content is of a format this app does not know.")
	}
	if header[5] != 1 {
		return nil, failure("The content uses a suite this app does not know.")
	}
	roomVersion := int(binary.BigEndian.Uint32(header[6:10]))
	size := int(binary.BigEndian.Uint32(header[11:15]))
	if size < minChunk || size > maxChunk {
		return nil, failure("The content is of a format this app does not know.")
	}
	room, ok := keys[roomVersion]
	if !ok {
		return nil, failure("The content was written under key %d of the room, which this device does not have.", roomVersion)
	}
	aead, err := contentKey(room, header[15:47], circleID, roomVersion)
	if err != nil {
		return nil, err
	}
	if size+tagLength > reader.Size() {
		reader = bufio.NewReaderSize(reader, size+tagLength+1)
	}
	return &Decrypter{
		in: reader, aead: aead, prefix: append([]byte{}, header[47:54]...), step: size + tagLength,
		aad: append(append([]byte{}, header...), []byte("|"+circleID+"|"+context)...),
	}, nil
}

// Read gives the decrypted content.
func (d *Decrypter) Read(out []byte) (int, error) {
	for len(d.plain) == 0 {
		if d.done {
			return 0, io.EOF
		}
		chunk := make([]byte, d.step)
		n, err := io.ReadFull(d.in, chunk)
		if err != nil && !errors.Is(err, io.ErrUnexpectedEOF) && !errors.Is(err, io.EOF) {
			return 0, err
		}
		chunk = chunk[:n]
		// The last chunk is the one after which nothing follows.
		_, peek := d.in.Peek(1)
		last := peek != nil
		if last && !errors.Is(peek, io.EOF) {
			return 0, peek
		}
		// A chunk which isn't the last is full; the last one may be shorter.
		if len(chunk) < tagLength || (!last && len(chunk) != d.step) {
			return 0, failure("The content is cut off.")
		}
		plain, err := d.aead.Open(nil, chunkNonce(d.prefix, d.index, last), chunk, d.aad)
		if err != nil {
			return 0, failure("The content can't be opened: it is damaged, cut off, or not what it is presented as.")
		}
		d.index++
		d.plain = plain
		d.done = last
	}
	n := copy(out, d.plain)
	d.plain = d.plain[n:]
	return n, nil
}

// DecryptContent decrypts a content which is in memory.
func DecryptContent(keys map[int][]byte, circleID, context string, data []byte) ([]byte, error) {
	decrypter, err := NewDecrypter(bytes.NewReader(data), keys, circleID, context)
	if err != nil {
		return nil, err
	}
	return io.ReadAll(decrypter)
}
