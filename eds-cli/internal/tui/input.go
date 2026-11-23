package tui

import (
	"strings"

	tea "charm.land/bubbletea/v2"
	"github.com/charmbracelet/x/ansi"
)

// input is a line of text being written: the text, and where the cursor is.
type input struct {
	runes  []rune
	cursor int
	secret bool // shown as dots: a passphrase
}

func (in *input) text() string { return string(in.runes) }

func (in *input) set(text string) {
	in.runes = []rune(text)
	in.cursor = len(in.runes)
}

func (in *input) clear() { in.runes, in.cursor = nil, 0 }

func (in *input) empty() bool { return len(in.runes) == 0 }

func (in *input) insert(text string) {
	// A pasted text keeps its words, but not its line breaks.
	text = strings.NewReplacer("\r\n", " ", "\n", " ", "\r", " ", "\t", " ").Replace(text)
	added := []rune(text)
	in.runes = append(in.runes[:in.cursor], append(added, in.runes[in.cursor:]...)...)
	in.cursor += len(added)
}

// key handles a key press; it reports whether the key was the input's.
func (in *input) key(msg tea.KeyPressMsg) bool {
	switch msg.String() {
	case "left", "ctrl+b":
		in.cursor = max(in.cursor-1, 0)
	case "right", "ctrl+f":
		in.cursor = min(in.cursor+1, len(in.runes))
	case "home", "ctrl+a":
		in.cursor = 0
	case "end", "ctrl+e":
		in.cursor = len(in.runes)
	case "backspace", "ctrl+h":
		if in.cursor > 0 {
			in.runes = append(in.runes[:in.cursor-1], in.runes[in.cursor:]...)
			in.cursor--
		}
	case "delete", "ctrl+d":
		if in.cursor < len(in.runes) {
			in.runes = append(in.runes[:in.cursor], in.runes[in.cursor+1:]...)
		}
	case "ctrl+u":
		in.runes, in.cursor = in.runes[in.cursor:], 0
	case "ctrl+k":
		in.runes = in.runes[:in.cursor]
	case "ctrl+w":
		at := in.cursor
		for at > 0 && in.runes[at-1] == ' ' {
			at--
		}
		for at > 0 && in.runes[at-1] != ' ' {
			at--
		}
		in.runes = append(in.runes[:at], in.runes[in.cursor:]...)
		in.cursor = at
	default:
		if text := msg.Key().Text; text != "" {
			in.insert(text)
			return true
		}
		return false
	}
	return true
}

// view shows the line in w cells, scrolled so the cursor is visible, and
// returns the cursor's column in it.
func (in *input) view(w int) (string, int) {
	shown := in.runes
	if in.secret {
		shown = []rune(strings.Repeat("•", len(in.runes)))
	}
	before := string(shown[:in.cursor])
	text := string(shown)
	column := ansi.StringWidth(before)
	if w <= 1 {
		return "", 0
	}
	// Scroll: keep the cursor inside the last cell.
	offset := 0
	if column >= w {
		offset = column - w + 1
	}
	visible := ansi.Cut(text, offset, offset+w)
	return fit(visible, w), column - offset
}
