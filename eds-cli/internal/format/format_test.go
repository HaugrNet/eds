package format

import (
	"testing"
	"time"
)

func TestNumberAndSize(t *testing.T) {
	en, de := Locale{Language: "en"}, Locale{Language: "de"}
	for _, c := range []struct {
		bytes  int64
		en, de string
	}{
		{0, "0 B", "0 B"},
		{512, "512 B", "512 B"},
		{1023, "1,023 B", "1.023 B"},
		{1536, "1.5 KB", "1,5 KB"},
		{12 << 20, "12 MB", "12 MB"},
		{5 << 29, "2.5 GB", "2,5 GB"},
		{2048 << 30, "2,048 GB", "2.048 GB"},
	} {
		if got := en.Size(c.bytes); got != c.en {
			t.Errorf("en %d: %q, want %q", c.bytes, got, c.en)
		}
		if got := de.Size(c.bytes); got != c.de {
			t.Errorf("de %d: %q, want %q", c.bytes, got, c.de)
		}
	}
	if got := de.Number(-1234567.891, 2); got != "-1.234.567,89" {
		t.Errorf("number: %q", got)
	}
}

func TestDatesAndClocks(t *testing.T) {
	when := time.Date(2026, 11, 10, 14, 5, 0, 0, time.UTC)
	for _, c := range []struct {
		locale Locale
		want   string
	}{
		{Locale{"de", "DE", "24"}, "10.11.2026, 14:05"},
		{Locale{"en", "US", "24"}, "11/10/2026, 14:05"},
		{Locale{"en", "GB", "24"}, "10/11/2026, 14:05"},
		{Locale{"en", "US", "auto"}, "11/10/2026, 2:05 PM"},
		{Locale{"en", "GB", "auto"}, "10/11/2026, 14:05"},
		{Locale{"de", "DE", "12"}, "10.11.2026, 2:05 PM"},
		{Locale{"en", "", "auto"}, "10/11/2026, 14:05"},
	} {
		if got := c.locale.DateTime(when); got != c.want {
			t.Errorf("%+v: %q, want %q", c.locale, got, c.want)
		}
	}
}

func TestRegion(t *testing.T) {
	t.Setenv("LC_ALL", "")
	t.Setenv("LC_TIME", "en_US.UTF-8")
	t.Setenv("LANG", "de_DE.UTF-8")
	if got := Region(); got != "US" {
		t.Errorf("LC_TIME: %q", got)
	}
	t.Setenv("LC_TIME", "")
	if got := Region(); got != "DE" {
		t.Errorf("LANG: %q", got)
	}
	t.Setenv("LANG", "C")
	if got := Region(); got != "" {
		t.Errorf("C: %q", got)
	}
}

func TestParseTime(t *testing.T) {
	// A stone sends UTC without a zone.
	got, ok := ParseTime("2026-11-10T14:05:06.123")
	if !ok || ISO(got) != "2026-11-10T14:05:06Z" {
		t.Errorf("no zone: %v %v", got, ok)
	}
	got, ok = ParseTime("2026-11-10T15:05:06+01:00")
	if !ok || ISO(got) != "2026-11-10T14:05:06Z" {
		t.Errorf("zone: %v %v", got, ok)
	}
	if _, ok := ParseTime("yesterday"); ok {
		t.Error("nonsense parsed")
	}
}
