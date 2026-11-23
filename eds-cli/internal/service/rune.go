package service

import (
	"encoding/base64"
	"encoding/json"
	"mime"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// What Tolk's commands and its interactive mode share of Rune's ways.

// Rune's avatars: users choose a creature of the sagas, rooms a place or
// thing. EDS stores only the ID; a terminal shows the picture's name.
var (
	UserAvatars = []string{"raven", "wolf", "bear", "eagle", "serpent", "horse", "boar", "stag", "goat", "cat", "squirrel", "salmon"}
	RoomAvatars = []string{"tree", "longship", "longhouse", "mountain", "fjord", "bridge", "hearth", "shield", "horn", "helmet", "sun", "moon"}
)

// AvatarName is the name of a picture in Rune's words.
func AvatarName(id string) string {
	switch id {
	case "raven":
		return i18n.Rune("Raven")
	case "wolf":
		return i18n.Rune("Wolf")
	case "bear":
		return i18n.Rune("Bear")
	case "eagle":
		return i18n.Rune("Eagle")
	case "serpent":
		return i18n.Rune("Serpent")
	case "horse":
		return i18n.Rune("Horse")
	case "boar":
		return i18n.Rune("Boar")
	case "stag":
		return i18n.Rune("Stag")
	case "goat":
		return i18n.Rune("Goat")
	case "cat":
		return i18n.Rune("Cat")
	case "squirrel":
		return i18n.Rune("Squirrel")
	case "salmon":
		return i18n.Rune("Salmon")
	case "tree":
		return i18n.Rune("World tree")
	case "longship":
		return i18n.Rune("Longship")
	case "longhouse":
		return i18n.Rune("Longhouse")
	case "mountain":
		return i18n.Rune("Mountain")
	case "fjord":
		return i18n.Rune("Fjord")
	case "bridge":
		return i18n.Rune("Rainbow bridge")
	case "hearth":
		return i18n.Rune("Hearth")
	case "shield":
		return i18n.Rune("Shield")
	case "horn":
		return i18n.Rune("Drinking horn")
	case "helmet":
		return i18n.Rune("Helmet")
	case "sun":
		return i18n.Rune("Sun")
	case "moon":
		return i18n.Rune("Moon")
	}
	return id
}

// InvitePrefix starts the fragment of an invitation link.
const InvitePrefix = "#invite="

// RuneRoot is the address Rune is served at, from a stone's API root.
func RuneRoot(apiURL string) string {
	return strings.TrimSuffix(strings.TrimRight(apiURL, "/"), "/api/v1") + "/"
}

// InvitationLink is Rune's invitation link: Rune's address with the invited
// name and the one-time token in the fragment, as base64url of
// {"n": …, "t": …}.
func InvitationLink(apiURL, name, token string) string {
	payload, _ := json.Marshal(struct {
		Name  string `json:"n"`
		Token string `json:"t"`
	}{name, token})
	return RuneRoot(apiURL) + InvitePrefix + base64.RawURLEncoding.EncodeToString(payload)
}

// MediaTypePattern is what EDS accepts as a media type, as Rune checks it.
var MediaTypePattern = regexp.MustCompile(`^[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]{0,126}/[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]{0,126}$`)

// MediaTypeOf guesses the media type from a file's name; empty if unknown.
func MediaTypeOf(name string) string {
	mediaType, _, _ := strings.Cut(mime.TypeByExtension(strings.ToLower(filepath.Ext(name))), ";")
	mediaType = strings.TrimSpace(mediaType)
	if !MediaTypePattern.MatchString(mediaType) {
		return ""
	}
	return mediaType
}

// LocalName makes a file's name in a room safe as a local file name: no
// directories, and not special.
func LocalName(name string) string {
	name = strings.NewReplacer("/", "_", `\`, "_", "\x00", "_").Replace(name)
	if name == "" || name == "." || name == ".." {
		return "download"
	}
	return name
}

// AboutPart is a section of the About text.
type AboutPart struct {
	Title string   `json:"title"`
	Text  []string `json:"text"`
}

// About says how a stone keeps what is in a room, in Rune's words, with
// Tolk's own about the container and the agent.
func About() []AboutPart {
	return []AboutPart{
		{i18n.T("About Tolk"), []string{
			i18n.Rune("Rune is a place for rooms with a chat and files, kept by a stone: a Haugr EDS instance, an encrypted data store. Every message, file and thumbnail in a room is encrypted before it is stored. This is how."),
			i18n.T("Tolk is the command line of Rune: the same rooms, users and files, for the terminal and for scripts."),
		}},
		{i18n.Rune("Your passphrase"), []string{
			i18n.Rune("Your account has two pairs of keys: ML-KEM-1024, which is made to also withstand quantum computers, and the elliptic curve P-384. Both are used together, so the protection holds as long as one of them does."),
			i18n.Rune("The private keys are locked with a key made from your passphrase (PBKDF2-HMAC-SHA512). Without the passphrase they can't be opened, which is also why nobody can recover a forgotten one."),
		}},
		{i18n.Rune("Rooms"), []string{
			i18n.Rune("Each room has a random secret of 256 bits. Every member of the room has a copy of it, locked with that member's keys. Who isn't a member has no copy and can't read the room; that goes for the System Administrator too."),
		}},
		{i18n.Rune("Messages and files"), []string{
			i18n.Rune("Each message and each file gets a key of its own, made from the room's secret (HKDF-SHA512), and is encrypted in pieces with AES-256-GCM. Each piece is sealed, so a changed, missing or swapped piece is noticed when it is read."),
		}},
		{i18n.Rune("The MasterKey"), []string{
			i18n.Rune("All keys the stone keeps are locked a second time with the MasterKey. The stone gets it from elsewhere when it starts and only has it in memory. A stolen copy of the database can't be read without it, and gives nothing to try passphrases against."),
		}},
		{i18n.Rune("Chambers"), []string{
			i18n.T("A chamber is a room whose messages, files and entries of features Tolk encrypts on this computer, before the stone gets them (P-384, HKDF-SHA512, AES-256-GCM). The key of a chamber is given from member to member, locked to each one's own key for chambers; the stone stores these locked copies and can't open them. Your own key is kept on the stone locked with your recovery key, which Tolk keeps in its container."),
			i18n.T("What a chamber does not hide: who is in it, who wrote when, and the names, types and sizes of its files. Rune comes from the stone, so a stone which turns against its people could hand out a changed Rune; Tolk doesn't come from the stone. Compare fingerprints with the others ('tolk members') to know that nobody put a key in between."),
		}},
		{i18n.Rune("What you should know"), []string{
			i18n.T("In a room, the stone does the encryption, not Tolk. Your passphrase and what you read and write travel to the stone, and it works with them while you are logged in. So the connection must be protected, and the stone trusted while it runs."),
			i18n.Rune("Names and avatars of accounts and rooms, names of files and folders, and their sizes and times are stored readable."),
			i18n.T("Tolk keeps the address, account and passphrase of each stone in a container on this computer, encrypted with AES-256-GCM under a key made from the container's passphrase (PBKDF2-HMAC-SHA256). While you are logged in with 'tolk login', an agent holds the opened container in memory; nothing decrypted is written to disk."),
		}},
	}
}
