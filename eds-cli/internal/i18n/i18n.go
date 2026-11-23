// Package i18n holds Tolk's texts: written in English as T("…"), with a
// German catalog (de.json, embedded). A test fails when a text has no
// translation or a translation is unused.
package i18n

import (
	_ "embed"
	"encoding/json"
	"fmt"
	"os"
	"strings"

	shared "github.com/HaugrNet/eds/eds-cli/internal/i18n/rune"
)

//go:embed de.json
var deJSON []byte

var catalogs = map[string]map[string]string{"en": nil}

// runeCatalogs are Rune's catalogs, for the texts Tolk shares with it.
var runeCatalogs = map[string]map[string]string{"en": nil}

var current = "en"

func init() {
	de := map[string]string{}
	if err := json.Unmarshal(deJSON, &de); err != nil {
		panic("i18n: de.json: " + err.Error())
	}
	catalogs["de"] = de
	runeDe := map[string]string{}
	if err := json.Unmarshal(shared.German, &runeDe); err != nil {
		panic("i18n: rune/de.json: " + err.Error())
	}
	runeCatalogs["de"] = runeDe
}

// Languages are the languages Tolk has.
func Languages() []string { return []string{"en", "de"} }

// Detect chooses the language from the environment: LC_ALL, LC_MESSAGES,
// then LANG; English if none matches.
func Detect() string {
	for _, name := range []string{"LC_ALL", "LC_MESSAGES", "LANG"} {
		value := strings.ToLower(os.Getenv(name))
		if value == "" {
			continue
		}
		for lang := range catalogs {
			if strings.HasPrefix(value, lang) {
				return lang
			}
		}
		return "en"
	}
	return "en"
}

// Set chooses the language: "auto" (or "") detects it; an unknown one gives
// English.
func Set(lang string) {
	if lang == "" || lang == "auto" {
		lang = Detect()
	}
	if _, ok := catalogs[lang]; !ok {
		lang = "en"
	}
	current = lang
}

// Language returns the language in use.
func Language() string { return current }

// T translates an English text and formats it with the arguments.
func T(text string, args ...any) string {
	if translated, ok := catalogs[current][text]; ok && translated != "" {
		text = translated
	}
	if len(args) == 0 {
		return text
	}
	return fmt.Sprintf(text, args...)
}

// Rune translates a text Tolk shares with Rune, from Rune's catalog, and
// puts the values in for {name}; pairs are name, value, name, value. A test
// checks that Rune has every such text.
func Rune(text string, pairs ...string) string {
	if translated, ok := runeCatalogs[current][text]; ok && translated != "" {
		text = translated
	}
	if len(pairs) < 2 {
		return text
	}
	replace := make([]string, 0, len(pairs))
	for i := 0; i+1 < len(pairs); i += 2 {
		replace = append(replace, "{"+pairs[i]+"}", pairs[i+1])
	}
	return strings.NewReplacer(replace...).Replace(text)
}

// RuneCatalog returns Rune's catalog of a language, for the tests.
func RuneCatalog(lang string) map[string]string { return runeCatalogs[lang] }

// Catalog returns the catalog of a language, for the completeness test.
func Catalog(lang string) map[string]string { return catalogs[lang] }
