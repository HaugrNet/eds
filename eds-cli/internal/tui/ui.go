// Package tui is Tolk's interactive mode: Rune in a terminal. The panes are
// Rune's (rooms and users, chat, files), with its words, rules and palettes.
// It talks to one stone at a time, through the same service and chat client
// as the commands.
//
// It is built on Bubble Tea and Lip Gloss (decided 2026-11-11, for the eye;
// a later goal is to replace them with code on the standard library). Only
// this package uses them.
package tui

import (
	"strings"

	"charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"

	"github.com/HaugrNet/eds/eds-cli/internal/theme"
)

// styles are the colours of a palette as Lip Gloss styles.
type styles struct {
	text, muted, accent, border, focus, selected, onAccent, title, danger lipgloss.Style
	contrast                                                              bool
}

func newStyles(palette string, dark bool) styles {
	p, ok := theme.Palettes[palette]
	if !ok {
		palette, p = "rune", theme.Palettes["rune"]
	}
	c := p.Light
	if dark {
		c = p.Dark
	}
	s := styles{
		text:     lipgloss.NewStyle().Foreground(lipgloss.Color(c.Text)),
		muted:    lipgloss.NewStyle().Foreground(lipgloss.Color(c.Muted)),
		accent:   lipgloss.NewStyle().Foreground(lipgloss.Color(c.Accent)).Bold(true),
		border:   lipgloss.NewStyle().Foreground(lipgloss.Color(c.Border)),
		focus:    lipgloss.NewStyle().Foreground(lipgloss.Color(c.Accent)),
		selected: lipgloss.NewStyle().Foreground(lipgloss.Color(c.OnAccent)).Background(lipgloss.Color(c.Accent)),
		onAccent: lipgloss.NewStyle().Foreground(lipgloss.Color(c.OnAccent)).Background(lipgloss.Color(c.Accent)).Bold(true),
		title:    lipgloss.NewStyle().Foreground(lipgloss.Color(c.Text)).Bold(true),
		danger:   lipgloss.NewStyle().Foreground(lipgloss.Color(c.Accent)).Bold(true),
		contrast: palette == "contrast",
	}
	if s.contrast {
		// "High contrast" uses bold and underline as well as colour.
		s.accent = s.accent.Underline(true)
		s.focus = s.focus.Bold(true)
	}
	return s
}

// width is the number of terminal cells a text takes.
func width(text string) int { return ansi.StringWidth(text) }

// fit makes a line exactly w cells wide: cut with an ellipsis, or filled
// with spaces.
func fit(line string, w int) string {
	if w <= 0 {
		return ""
	}
	if n := width(line); n > w {
		line = ansi.Truncate(line, w, "…")
	}
	if n := width(line); n < w {
		line += strings.Repeat(" ", w-n)
	}
	return line
}

// wrap breaks a text into lines of at most w cells, at spaces where it can.
func wrap(text string, w int) []string {
	if w <= 0 {
		return nil
	}
	return strings.Split(ansi.Wrap(text, w, ""), "\n")
}

// box draws a pane: a rounded border with the title in it, around exactly
// h-2 lines of w-2 cells. A focused pane has its border in the accent
// colour.
func (s styles) box(title string, lines []string, w, h int, focused bool) string {
	if w < 4 || h < 3 {
		return strings.Repeat(strings.Repeat(" ", max(w, 0))+"\n", max(h-1, 0)) + strings.Repeat(" ", max(w, 0))
	}
	edge := s.border
	if focused {
		edge = s.focus
	}
	inner := w - 2
	head := ""
	if title != "" {
		label := " " + ansi.Truncate(title, inner-2, "…") + " "
		if focused {
			head = s.accent.Render(label)
		} else {
			head = s.title.Render(label)
		}
	}
	var b strings.Builder
	b.WriteString(edge.Render("╭─") + head + edge.Render(strings.Repeat("─", max(inner-1-width(head), 0))+"╮"))
	for i := 0; i < h-2; i++ {
		line := ""
		if i < len(lines) {
			line = lines[i]
		}
		b.WriteString("\n" + edge.Render("│") + fit(line, inner) + edge.Render("│"))
	}
	b.WriteString("\n" + edge.Render("╰"+strings.Repeat("─", inner)+"╯"))
	return b.String()
}

// over puts a window in the middle of the screen, on top of what is there.
func over(base, window string, w, h int) string {
	ww, wh := lipgloss.Size(window)
	x, y := max((w-ww)/2, 0), max((h-wh)/2, 0)
	return lipgloss.NewCompositor(lipgloss.NewLayer(base), lipgloss.NewLayer(window).X(x).Y(y).Z(1)).Render()
}
