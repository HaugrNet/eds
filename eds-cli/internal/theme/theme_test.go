package theme

import (
	"errors"
	"os"
	"regexp"
	"strings"
	"testing"
)

const webui = "../../../eds-quarkus/src/main/webui/src/"

var (
	rule     = regexp.MustCompile(`(?s)([^{}]+)\{([^{}]*)\}`)
	property = regexp.MustCompile(`(--[a-z0-9-]+)\s*:\s*([^;]+);`)
	comment  = regexp.MustCompile(`(?s)/\*.*?\*/`)
)

// block returns the properties of the rule with exactly this selector.
func block(t *testing.T, css, selector string) map[string]string {
	t.Helper()
	for _, m := range rule.FindAllStringSubmatch(css, -1) {
		if strings.TrimSpace(m[1]) != selector {
			continue
		}
		found := map[string]string{}
		for _, p := range property.FindAllStringSubmatch(m[2], -1) {
			found[p[1]] = strings.TrimSpace(p[2])
		}
		return found
	}
	t.Fatalf("no rule %q in Rune's CSS", selector)
	return nil
}

func read(t *testing.T, name string) string {
	t.Helper()
	data, err := os.ReadFile(webui + name)
	if errors.Is(err, os.ErrNotExist) {
		t.Skipf("Rune's sources aren't here (%s)", name)
	}
	if err != nil {
		t.Fatal(err)
	}
	return comment.ReplaceAllString(string(data), "")
}

// The palettes are Rune's: every colour is read from its CSS and compared.
func TestPalettesMatchRune(t *testing.T) {
	base := read(t, "rune-theme.css")
	themes := read(t, "themes.css")
	root := block(t, base, ":root")
	resolve := func(value string) string {
		if strings.HasPrefix(value, "var(") {
			value = root[strings.TrimSuffix(strings.TrimPrefix(value, "var("), ")")]
		}
		return strings.ToUpper(value)
	}
	check := func(name string, got Colours, css map[string]string) {
		t.Helper()
		for token, have := range map[string]string{
			"--color-bg": got.Bg, "--color-surface": got.Surface, "--color-border": got.Border,
			"--color-text": got.Text, "--color-text-muted": got.Muted, "--color-accent": got.Accent,
			"--color-on-accent": got.OnAccent,
		} {
			if want := resolve(css[token]); want != strings.ToUpper(have) {
				t.Errorf("%s %s: Tolk has %s, Rune has %s", name, token, have, want)
			}
		}
	}
	check("rune light", Palettes["rune"].Light, root)
	check("rune dark", Palettes["rune"].Dark, block(t, base, `:root[data-theme="dark"]`))
	for _, name := range Names[1:] {
		check(name+" light", Palettes[name].Light, block(t, themes, `:root[data-palette="`+name+`"]`))
		check(name+" dark", Palettes[name].Dark, block(t, themes, `:root[data-palette="`+name+`"][data-theme="dark"]`))
	}
	if len(Palettes) != len(Names) {
		t.Errorf("%d palettes, %d names", len(Palettes), len(Names))
	}
}

func TestPaint(t *testing.T) {
	style := New("rune", "dark", True)
	if got, want := style.Accent("Test system"), "\x1b[1;38;2;236;148;115mTest system\x1b[0m"; got != want {
		t.Errorf("true colour: %q, want %q", got, want)
	}
	if got := New("rune", "dark", None).Accent("x"); got != "x" {
		t.Errorf("no colour: %q", got)
	}
	if got := New("contrast", "dark", Basic).Accent("x"); got != "\x1b[1;4;93mx\x1b[0m" {
		t.Errorf("contrast, 16 colours: %q", got)
	}
	if got := New("nonsense", "light", Indexed).Muted("x"); !strings.HasPrefix(got, "\x1b[38;5;") {
		t.Errorf("256 colours: %q", got)
	}
}

func TestNearestColours(t *testing.T) {
	for _, c := range []struct{ r, g, b, want int }{
		{0, 0, 0, 16}, {255, 255, 255, 231}, {255, 0, 0, 196}, {128, 128, 128, 244}, {95, 135, 175, 67},
	} {
		if got := Index256(c.r, c.g, c.b); got != c.want {
			t.Errorf("256: %d,%d,%d = %d, want %d", c.r, c.g, c.b, got, c.want)
		}
	}
	if got := Index16(255, 212, 0); got != 93 {
		t.Errorf("16: yellow = %d", got)
	}
}

func TestDetect(t *testing.T) {
	t.Setenv("NO_COLOR", "")
	t.Setenv("COLORTERM", "truecolor")
	t.Setenv("TERM", "xterm-256color")
	if Detect(true) != True || Detect(false) != None {
		t.Error("truecolor")
	}
	t.Setenv("COLORTERM", "")
	if Detect(true) != Indexed {
		t.Error("256")
	}
	t.Setenv("TERM", "xterm")
	if Detect(true) != Basic {
		t.Error("16")
	}
	t.Setenv("NO_COLOR", "1")
	if Detect(true) != None {
		t.Error("NO_COLOR")
	}
}
