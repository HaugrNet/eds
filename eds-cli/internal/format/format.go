// Package format writes numbers, sizes, dates and times for Tolk's two
// languages. Go has no locale formatting, so Tolk does it itself: English by
// region (11/10/2026 in the United States, 10/11/2026 elsewhere), German
// 10.11.2026 with a decimal comma. Machine-readable output doesn't use this:
// it has bytes and ISO-8601 UTC, whatever the language.
package format

import (
	"os"
	"strconv"
	"strings"
	"time"
)

// Locale is what the formatting depends on.
type Locale struct {
	Language string // en, de
	Region   string // US, GB, DE, …; may be empty
	Clock    string // 24, 12, auto
}

// twelveHours are the regions that usually write the time with AM and PM.
var twelveHours = map[string]bool{"US": true, "CA": true, "AU": true, "NZ": true, "IN": true, "PH": true}

// Region reads the region from the environment (LC_ALL, LC_TIME, then LANG),
// e.g. "US" from en_US.UTF-8.
func Region() string {
	for _, name := range []string{"LC_ALL", "LC_TIME", "LANG"} {
		value := os.Getenv(name)
		if value == "" {
			continue
		}
		if i := strings.IndexAny(value, ".@"); i >= 0 {
			value = value[:i]
		}
		if i := strings.IndexAny(value, "_-"); i >= 0 && len(value) >= i+3 {
			return strings.ToUpper(value[i+1 : i+3])
		}
		return ""
	}
	return ""
}

// Number writes a number with the given decimals and the language's
// separators: 1,234.5 in English, 1.234,5 in German.
func (l Locale) Number(value float64, decimals int) string {
	text := strconv.FormatFloat(value, 'f', decimals, 64)
	whole, fraction, _ := strings.Cut(text, ".")
	negative := strings.HasPrefix(whole, "-")
	whole = strings.TrimPrefix(whole, "-")
	group, point := ",", "."
	if l.Language == "de" {
		group, point = ".", ","
	}
	var b strings.Builder
	if negative {
		b.WriteByte('-')
	}
	for i, digit := range whole {
		if i > 0 && (len(whole)-i)%3 == 0 {
			b.WriteString(group)
		}
		b.WriteRune(digit)
	}
	if fraction != "" {
		b.WriteString(point + fraction)
	}
	return b.String()
}

// Size writes a number of bytes as Rune does: in steps of 1024 up to GB,
// with one decimal below 10 (1.5 MB, 12 MB, 512 B).
func (l Locale) Size(bytes int64) string {
	units := []string{"B", "KB", "MB", "GB"}
	value, unit := float64(bytes), 0
	for value >= 1024 && unit < len(units)-1 {
		value /= 1024
		unit++
	}
	decimals := 1
	if unit == 0 || value >= 10 {
		decimals = 0
	}
	return l.Number(value, decimals) + " " + units[unit]
}

// twelve reports whether the clock has 12 hours.
func (l Locale) twelve() bool {
	switch l.Clock {
	case "12":
		return true
	case "auto":
		return l.Language == "en" && twelveHours[l.Region]
	}
	return false
}

// Date writes a date: 10.11.2026 in German, 11/10/2026 in the United States,
// 10/11/2026 in other English.
func (l Locale) Date(t time.Time) string {
	switch {
	case l.Language == "de":
		return t.Format("02.01.2006")
	case l.Region == "US":
		return t.Format("01/02/2006")
	}
	return t.Format("02/01/2006")
}

// Clock writes hours and minutes: 14:05, or 2:05 PM with 12 hours.
func (l Locale) Time(t time.Time) string {
	if l.twelve() {
		return t.Format("3:04 PM")
	}
	return t.Format("15:04")
}

// DateTime writes date and time, in the time's own zone.
func (l Locale) DateTime(t time.Time) string {
	return l.Date(t) + ", " + l.Time(t)
}

// ParseTime reads a time as a stone sends it: ISO-8601, in UTC when it has
// no zone.
func ParseTime(text string) (time.Time, bool) {
	if text == "" {
		return time.Time{}, false
	}
	for _, layout := range []string{time.RFC3339Nano, "2006-01-02T15:04:05.999999999", "2006-01-02T15:04:05", "2006-01-02T15:04"} {
		if t, err := time.Parse(layout, text); err == nil {
			return t, true
		}
	}
	return time.Time{}, false
}

// ISO writes a time for machine-readable output: ISO-8601 in UTC.
func ISO(t time.Time) string {
	return t.UTC().Format("2006-01-02T15:04:05Z")
}
