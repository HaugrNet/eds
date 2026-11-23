package tui

import (
	"strings"

	tea "charm.land/bubbletea/v2"
	"github.com/charmbracelet/x/ansi"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/qr"
)

// overlay is a window over the panes: a menu to choose from, a line to
// fill in, a question, or a page to read.
type overlay struct {
	kind   int
	title  string
	lines  []string // a page's text, or what stands above a menu or question
	items  []item
	cursor int
	scroll int
	label  string
	in     input
	submit func(m *model, text string) tea.Cmd
	yes    func(m *model) tea.Cmd
	raw    bool // a page whose lines are drawn as they are (a QR code)
	stones bool // the page of the stones' status, which is drawn anew as they answer
}

const (
	overMenu = iota
	overPrompt
	overConfirm
	overPage
)

// item is one line of a menu.
type item struct {
	label string
	hint  string // shown beside it, quietly: the current value
	do    func(m *model) tea.Cmd
}

func stripStyle(text string) string { return ansi.Strip(text) }

func (m *model) menu(title string, items []item, cursor int) {
	m.over = &overlay{kind: overMenu, title: title, items: items, cursor: cursor}
}

func (m *model) prompt(title, label, value string, secret bool, submit func(m *model, text string) tea.Cmd) {
	o := &overlay{kind: overPrompt, title: title, label: label, submit: submit}
	o.in.secret = secret
	o.in.set(value)
	m.over = o
}

func (m *model) confirm(title, question string, yes func(m *model) tea.Cmd) {
	m.over = &overlay{kind: overConfirm, title: title, lines: []string{question}, yes: yes}
}

func (m *model) page(title string, lines []string) {
	m.over = &overlay{kind: overPage, title: title, lines: lines}
}

// showInvitation shows an invitation: its QR code, black on white whatever
// the theme so every camera reads it, and the link.
func (m *model) showInvitation(name, link string) {
	var lines []string
	if code, err := qr.Encode(link); err == nil && code.Size+8 <= m.w-4 && (code.Size+8)/2+8 <= m.h {
		lines = append(lines, strings.Split(strings.TrimRight(code.Text(), "\n"), "\n")...)
		lines = append(lines, "")
	}
	lines = append(lines, i18n.Rune("Let {name} scan this code with their phone, or send them the link. They choose their own passphrase and join the room.", "name", name), "",
		link, "",
		i18n.Rune("The invitation works once and expires after 7 days. Anyone with the link can accept it, so only share it with them."))
	m.over = &overlay{kind: overPage, title: i18n.Rune("Invitation for {name}", "name", name), lines: lines, raw: true}
}

// overlayKey handles a key while a window is open.
func (m *model) overlayKey(msg tea.KeyPressMsg) tea.Cmd {
	o := m.over
	key := msg.String()
	if key == "esc" {
		m.over = nil
		return nil
	}
	switch o.kind {
	case overMenu:
		switch key {
		case "up", "k":
			o.cursor = (o.cursor - 1 + len(o.items)) % len(o.items)
		case "down", "j":
			o.cursor = (o.cursor + 1) % len(o.items)
		case "enter", "space":
			chosen := o.items[o.cursor]
			m.over = nil
			if chosen.do != nil {
				return chosen.do(m)
			}
		case "q":
			m.over = nil
		}
	case overPrompt:
		if key == "enter" {
			text := o.in.text()
			m.over = nil
			return o.submit(m, text)
		}
		o.in.key(msg)
	case overConfirm:
		switch key {
		case "y", "j", "enter":
			m.over = nil
			return o.yes(m)
		case "n", "q":
			m.over = nil
		}
	case overPage:
		switch key {
		case "up", "k":
			o.scroll = max(o.scroll-1, 0)
		case "down", "j":
			o.scroll++
		case "pgup":
			o.scroll = max(o.scroll-10, 0)
		case "pgdown", "space":
			o.scroll += 10
		case "enter", "q":
			m.over = nil
		}
	}
	return nil
}

// overlayView draws the window, and says where its cursor is, if it has
// one: the column and line inside the window, or -1.
func (m *model) overlayView() (string, int, int) {
	o := m.over
	w := min(m.w-4, 72)
	if w < 20 {
		w = max(m.w, 4)
	}
	inner := w - 4
	var lines []string
	cursorX, cursorY := -1, -1
	add := func(text string) {
		for _, line := range wrap(text, inner) {
			lines = append(lines, " "+line)
		}
	}
	switch o.kind {
	case overMenu:
		for _, text := range o.lines {
			add(text)
		}
		for i, it := range o.items {
			line := " " + it.label
			if it.hint != "" {
				line += m.st.muted.Render("  " + it.hint)
			}
			if i == o.cursor {
				line = m.st.selected.Render(fit(stripStyle(line), w-2))
			}
			lines = append(lines, line)
		}
	case overPrompt:
		add(o.label)
		text, column := o.in.view(inner)
		cursorX, cursorY = 2+column, len(lines)+1
		lines = append(lines, " "+m.st.focus.Render(text), "", m.st.muted.Render(" "+i18n.T("Enter to confirm, Esc to cancel")))
	case overConfirm:
		for _, text := range o.lines {
			add(text)
		}
		lines = append(lines, "", m.st.muted.Render(" "+i18n.T("y for yes, n or Esc for no")))
	case overPage:
		for _, text := range o.lines {
			if o.raw && strings.Contains(text, "\x1b") {
				lines = append(lines, " "+text)
			} else {
				add(text)
			}
		}
		room := m.h - 4
		if len(lines) > room && room > 2 {
			o.scroll = min(o.scroll, len(lines)-room+1)
			lines = append(lines[o.scroll:o.scroll+room-1], m.st.muted.Render(" "+i18n.T("↑ ↓ to read on, Esc to close")))
		} else {
			o.scroll = 0
		}
	}
	height := min(len(lines)+2, m.h)
	return m.st.box(o.title, lines, w, height, true), cursorX, cursorY
}
