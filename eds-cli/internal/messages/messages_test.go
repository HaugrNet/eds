package messages

import (
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

func TestFriendly(t *testing.T) {
	en, de := format.Locale{Language: "en"}, format.Locale{Language: "de"}
	i18n.Set("en")
	defer i18n.Set("en")
	for message, want := range map[string]string{
		"The requesting Account is not permitted to Process Data.":      "You can only read in this room.",
		"Only its author may edit a message.":                           "Only the author can edit a message.",
		"The content is larger than the allowed 2147483648 bytes.":      "The file is larger than the stone allows (2.0 GB).",
		"The Credential is too weak. Use more characters.":              "The passphrase is too weak. Use more characters.",
		"The name of the Data Object is invalid, it must be shorter.":   "The name is empty or too long (at most 75 characters).",
		"No Circle was found, and the Data Objects of the Circle stay.": "No room was found, and the files of the room stay.",
		"Something EDS says that the table doesn't know.":               "Something EDS says that the table doesn't know.",
	} {
		if got := Friendly(message, en); got != want {
			t.Errorf("%q:\n got %q\nwant %q", message, got, want)
		}
	}
	i18n.Set("de")
	if got, want := Friendly("The content is larger than the allowed 1610612736 bytes.", de), "Die Datei ist größer, als der Stein erlaubt (1,5 GB)."; got != want {
		t.Errorf("de: got %q, want %q", got, want)
	}
}

// Every text of the table has a German translation in Rune's catalog.
func TestTableIsTranslated(t *testing.T) {
	de := i18n.RuneCatalog("de")
	if len(Texts()) < 15 {
		t.Fatalf("the table has only %d entries", len(Texts()))
	}
	for _, text := range Texts() {
		if de[text] == "" {
			t.Errorf("Rune's German catalog has no %q", text)
		}
	}
}
