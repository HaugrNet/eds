// Package features is what Tolk knows of the features of a room (FEATURES.md): the
// declarations Rune and Tolk bring, the entries as the stone takes them, and
// the forms of the entries of the calendar, the plans and the location.
// Nothing here talks to a stone; it says the same as Rune's src/features.
package features

import (
	"context"
	"encoding/base64"
	"encoding/json"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n/rune"
)

// Names are the features Tolk knows, in the order Rune shows them.
var Names = []string{"chat", "calendar", "plans", "location", "calls"}

// Declaration is the declaration of a feature Tolk knows, as it is offered to
// a stone; nil for a feature Tolk doesn't know.
func Declaration(name string) []byte {
	switch name {
	case "chat":
		return rune.ChatFeature
	case "calendar":
		return rune.CalendarFeature
	case "plans":
		return rune.PlansFeature
	case "location":
		return rune.LocationFeature
	case "calls":
		return rune.CallsFeature
	}
	return nil
}

// Version is the version of the declaration Tolk brings of a feature; 0 for a
// feature it doesn't know.
func Version(name string) int {
	var declared struct {
		Version int `json:"version"`
	}
	_ = json.Unmarshal(Declaration(name), &declared)
	return declared.Version
}

// Label is what Rune and Tolk call a feature.
func Label(name string) string {
	switch name {
	case "chat":
		return i18n.Rune("Chat")
	case "calendar":
		return i18n.Rune("Calendar")
	case "plans":
		return i18n.Rune("Plans")
	case "location":
		return i18n.Rune("Location")
	case "calls":
		return i18n.Rune("Calls")
	}
	return name
}

// Sealer is what makes the content of a room ready for the stone, and
// readable again: nothing in a room, encryption in a chamber. A stone of
// package service is one.
type Sealer interface {
	Protect(ctx context.Context, circleID, what string, plain []byte) ([]byte, error)
	Reveal(ctx context.Context, circleID, what string, stored []byte) ([]byte, error)
}

// Encode is the content of an entry as the stone takes it: JSON, encrypted
// in a chamber, as Base64.
func Encode(ctx context.Context, sealer Sealer, circleID, feature, kind string, body any) (string, error) {
	plain, err := json.Marshal(body)
	if err != nil {
		return "", err
	}
	return EncodeRaw(ctx, sealer, circleID, feature, kind, plain)
}

// EncodeRaw is Encode for content which is JSON already.
func EncodeRaw(ctx context.Context, sealer Sealer, circleID, feature, kind string, plain []byte) (string, error) {
	sealed, err := sealer.Protect(ctx, circleID, feature+"|"+kind, plain)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(sealed), nil
}

// Plain is the content of an entry as its people read it: decrypted in a
// chamber. An error says why it can't be read.
func Plain(ctx context.Context, sealer Sealer, feature string, entry *client.Entry) ([]byte, error) {
	stored, err := base64.StdEncoding.DecodeString(entry.Content)
	if err != nil {
		return nil, err
	}
	return sealer.Reveal(ctx, entry.CircleID, feature+"|"+entry.Kind, stored)
}

// Decode reads the content of an entry into a body; false if it can't be
// read (damaged, of another convention, or no key).
func Decode(ctx context.Context, sealer Sealer, feature string, entry *client.Entry, body any) bool {
	plain, err := Plain(ctx, sealer, feature, entry)
	return err == nil && json.Unmarshal(plain, body) == nil
}
