// Package rune holds the files Tolk shares with Rune, so the two say the
// same: Rune's German catalog, the table that turns EDS's messages into
// Rune's words, the test vectors of the convention for end-to-end
// encryption, the examples of the plans, and the declarations of the features. They are copies of the files in Rune's sources, as Go only
// embeds files of its own module; `go generate ./...` renews them, and a
// test fails when a copy differs from its source.
package rune

import _ "embed"

//go:generate go run copy.go

const sources = "../../../../eds-quarkus/src/main/webui/src/"

// Sources maps each copy to its source, relative to this directory.
var Sources = map[string]string{
	"de.json":             sources + "i18n/de.json",
	"messages.json":       sources + "messages.json",
	"vectors.json":        sources + "e2e/vectors.json",
	"plans-examples.json": sources + "features/plans-examples.json",
	"chat.json":           sources + "features/declarations/chat.json",
	"calendar.json":       sources + "features/declarations/calendar.json",
	"location.json":       sources + "features/declarations/location.json",
	"plans.json":          sources + "features/declarations/plans.json",
	"calls.json":          sources + "features/declarations/calls.json",
}

// German is Rune's German catalog: English text to German text, with
// {name} for values.
//
//go:embed de.json
var German []byte

// Messages is the table of EDS's messages (see its "about").
//
//go:embed messages.json
var Messages []byte

// Vectors are the test vectors of the convention for end-to-end encryption
// (E2E.md), made by an independent implementation.
//
//go:embed vectors.json
var Vectors []byte

// PlansExamples are the examples of who owes whom (FEATURES.md, "Plans").
//
//go:embed plans-examples.json
var PlansExamples []byte

// The declarations of the features Rune and Tolk know (FEATURES.md).
//
//go:embed chat.json
var ChatFeature []byte

//go:embed calendar.json
var CalendarFeature []byte

//go:embed location.json
var LocationFeature []byte

//go:embed plans.json
var PlansFeature []byte

//go:embed calls.json
var CallsFeature []byte

// Embedded maps each copy to what is embedded of it.
var Embedded = map[string]*[]byte{
	"de.json": &German, "messages.json": &Messages, "vectors.json": &Vectors, "plans-examples.json": &PlansExamples,
	"chat.json": &ChatFeature, "calendar.json": &CalendarFeature, "location.json": &LocationFeature, "plans.json": &PlansFeature, "calls.json": &CallsFeature,
}
