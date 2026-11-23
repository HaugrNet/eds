// Package messages turns EDS's messages into Rune's words, with the same
// table as Rune (messages.json, shared): EDS says Circle and Data Object,
// Rune and Tolk say room and file. The common messages get their own text,
// translated with Rune's catalog; anything else stays in English with those
// words replaced.
package messages

import (
	"encoding/json"
	"regexp"
	"strconv"

	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	shared "github.com/HaugrNet/eds/eds-cli/internal/i18n/rune"
)

type value struct {
	Group  int    `json:"group"`
	Format string `json:"format"`
}

type entry struct {
	Pattern string           `json:"pattern"`
	Text    string           `json:"text"`
	Values  map[string]value `json:"values"`
	re      *regexp.Regexp
}

type word struct {
	re *regexp.Regexp
	to string
}

var (
	known []entry
	words []word
)

func init() {
	var table struct {
		Messages []entry    `json:"messages"`
		Words    [][]string `json:"words"`
	}
	if err := json.Unmarshal(shared.Messages, &table); err != nil {
		panic("messages: messages.json: " + err.Error())
	}
	for _, e := range table.Messages {
		e.re = regexp.MustCompile(e.Pattern)
		known = append(known, e)
	}
	for _, pair := range table.Words {
		words = append(words, word{regexp.MustCompile(`\b` + regexp.QuoteMeta(pair[0]) + `\b`), pair[1]})
	}
}

// Texts returns the English texts of the table, for the tests.
func Texts() []string {
	var texts []string
	for _, e := range known {
		texts = append(texts, e.Text)
	}
	return texts
}

// Friendly returns a stone's message in Rune's words and Tolk's language.
func Friendly(message string, locale format.Locale) string {
	for _, e := range known {
		match := e.re.FindStringSubmatch(message)
		if match == nil {
			continue
		}
		var pairs []string
		for name, v := range e.Values {
			raw := ""
			if v.Group < len(match) {
				raw = match[v.Group]
			}
			if v.Format == "size" {
				if n, err := strconv.ParseInt(raw, 10, 64); err == nil {
					raw = locale.Size(n)
				}
			}
			pairs = append(pairs, name, raw)
		}
		text := e.Text
		return i18n.Rune(text, pairs...)
	}
	for _, w := range words {
		message = w.re.ReplaceAllString(message, w.to)
	}
	return message
}
