package tui

import (
	"os"
	"strings"

	tea "charm.land/bubbletea/v2"
	"charm.land/lipgloss/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

func fileExists(path string) bool {
	info, err := os.Stat(path)
	return err == nil && info.Mode().IsRegular()
}

// places gives each pane on the screen its rectangle. The last line is the
// status line; a narrow terminal shows one pane under a line of tabs.
func (m *model) places() map[string]rect {
	out := map[string]rect{}
	if m.narrow() {
		out[m.focus] = rect{0, 1, m.w, m.h - 2}
		return out
	}
	m.layout.place(rect{0, 0, m.w, m.h - 1}, out)
	return out
}

func (m *model) View() tea.View {
	content, cursor := m.render()
	view := tea.NewView(content)
	view.AltScreen = true
	view.Cursor = cursor
	view.WindowTitle = m.windowTitle()
	return view
}

// windowTitle is the terminal's title: the unread messages, and where one
// is.
func (m *model) windowTitle() string {
	unread := 0
	for _, n := range m.unread {
		unread += n
	}
	title := "Tolk"
	if m.room != nil {
		title = m.room.Name + " · Tolk"
	}
	if unread > 0 {
		title = "(" + m.locale.Number(float64(unread), 0) + ") " + title
	}
	return title
}

// render draws the screen and says where the cursor is, if anywhere.
func (m *model) render() (string, *tea.Cursor) {
	if m.w < 20 || m.h < 6 {
		return i18n.T("The terminal is too small."), nil
	}
	if m.phase != phaseMain {
		return m.startView(), nil
	}
	var cursor *tea.Cursor
	places := m.places()
	draw := func(pane string) string {
		r := places[pane]
		focused := m.focus == pane && m.over == nil
		var lines []string
		title := paneName(pane)
		switch pane {
		case paneRooms:
			lines = m.roomsView(r.w-2, r.h-2, focused)
		case paneChat:
			if m.room != nil {
				title = m.room.Name
				if m.chamber {
					// A chamber is marked: the stone can't read its content.
					title += " · " + i18n.Rune("Chamber")
				}
			}
			var x, y int
			lines, x, y = m.chatView(r.w-2, r.h-2, focused)
			if x >= 0 && focused {
				cursor = tea.NewCursor(r.x+1+x, r.y+1+y)
			}
		case paneFiles:
			lines = m.filesView(r.w-2, r.h-2, focused)
		}
		return m.st.box(title, lines, r.w, r.h, focused)
	}

	var body string
	if m.narrow() {
		var tabs []string
		for _, pane := range allPanes {
			label := " " + paneName(pane) + " "
			if pane == m.focus {
				tabs = append(tabs, m.st.onAccent.Render(label))
			} else {
				tabs = append(tabs, m.st.muted.Render(label))
			}
		}
		body = fit(strings.Join(tabs, " "), m.w) + "\n" + draw(m.focus)
	} else {
		body = m.compose2(m.layout, rect{0, 0, m.w, m.h - 1}, draw)
	}
	screen := body + "\n" + m.statusLine()
	if m.over != nil {
		window, x, y := m.overlayView()
		ww, wh := lipgloss.Size(window)
		screen = over(screen, window, m.w, m.h)
		if x >= 0 {
			cursor = tea.NewCursor(max((m.w-ww)/2, 0)+x, max((m.h-wh)/2, 0)+y)
		} else {
			cursor = nil
		}
	}
	return screen, cursor
}

// compose2 joins the panes of a layout as its tree says.
func (m *model) compose2(n *node, area rect, draw func(pane string) string) string {
	if n.Pane != "" {
		return draw(n.Pane)
	}
	// Each child's area, as place gives them to the panes below.
	var parts []string
	offset := 0
	total := 0.0
	for _, size := range n.Sizes {
		total += size
	}
	length := area.w
	if n.Dir == "column" {
		length = area.h
	}
	for i, child := range n.Children {
		share := int(float64(length)*n.Sizes[i]/total + 0.5)
		if i == len(n.Children)-1 {
			share = length - offset
		}
		childArea := rect{area.x + offset, area.y, share, area.h}
		if n.Dir == "column" {
			childArea = rect{area.x, area.y + offset, area.w, share}
		}
		parts = append(parts, m.compose2(child, childArea, draw))
		offset += share
	}
	if n.Dir == "column" {
		return strings.Join(parts, "\n")
	}
	return lipgloss.JoinHorizontal(lipgloss.Top, parts...)
}

// statusLine is the last line, as in Rune: the stone's version, "Test
// system" if its data doesn't survive a restart, the live connection, who
// is logged in; or a note, or the transfer in progress.
func (m *model) statusLine() string {
	var left []string
	if m.info != nil {
		left = append(left, "Haugr EDS "+m.info.Version)
		if m.info.Persistent != nil && !*m.info.Persistent {
			left = append(left, m.st.accent.Render(i18n.Rune("Test system")))
		}
	}
	if m.chat != nil {
		left = append(left, m.st.focus.Render("●")+" "+i18n.Rune("Connected"))
	} else {
		left = append(left, m.st.muted.Render("○ "+i18n.Rune("Not connected, trying again…")))
	}
	if m.stone != nil {
		left = append(left, m.stone.Account+"@"+m.stone.Name)
	}
	right := m.st.muted.Render("F1 " + i18n.T("keys") + "  F2 " + i18n.T("menu"))
	switch {
	case m.transfer != nil:
		t := m.transfer
		done := t.done.Load()
		text := "↓ " + t.name
		if t.upload {
			text = "↑ " + t.name
		}
		if t.total > 0 {
			text += "  " + m.locale.Number(float64(done*100/t.total), 0) + " %  " + m.locale.Size(done) + " / " + m.locale.Size(t.total)
		} else {
			text += "  " + m.locale.Size(done)
		}
		right = m.st.focus.Render(text)
	case m.note != "" && m.noteBad:
		right = m.st.danger.Render(m.note)
	case m.note != "":
		right = m.st.text.Render(m.note)
	}
	line := " " + strings.Join(left, m.st.muted.Render(" · "))
	gap := m.w - width(line) - width(right) - 1
	if gap < 1 {
		// The note matters more than the rest.
		return fit(" "+right, m.w)
	}
	return line + strings.Repeat(" ", gap) + right + " "
}

// startView is the start screen: Rune in runes, in the accent colour, and
// what is happening; Rune's footer below. It also carries the notice for a
// stone that doesn't answer.
func (m *model) startView() string {
	name := m.opts.Stone
	if m.stone != nil {
		name = m.stone.Name
	}
	lines := []string{m.st.accent.Render("ᚱ ᚢ ᚾ"), "", m.st.title.Render("Tolk")}
	switch m.phase {
	case phaseConnecting:
		if name == "" {
			lines = append(lines, "", m.st.muted.Render(i18n.T("Reaching the stone…")))
		} else {
			lines = append(lines, "", m.st.muted.Render(i18n.T("Reaching the stone %s…", name)))
		}
	case phaseDown:
		lines = append(lines, "", m.st.title.Render(i18n.Rune("The stone is asleep")), "")
		for _, line := range wrap(i18n.T("The stone doesn't answer at the moment. Tolk asks again every few seconds and goes on by itself."), min(m.w-8, 60)) {
			lines = append(lines, line)
		}
		if m.info != nil && m.info.Persistent != nil {
			if *m.info.Persistent {
				lines = append(lines, "", i18n.Rune("Your rooms and files are safe."))
			} else {
				lines = append(lines, "", m.st.accent.Render(i18n.Rune("Test system"))+" · "+i18n.Rune("The data will not survive a restart of the stone."))
			}
		}
		lines = append(lines, "", m.st.muted.Render("r "+i18n.Rune("Try now")+"   q "+i18n.Rune("Log out")))
	case phaseFailed:
		lines = append(lines, "")
		for _, line := range wrap(m.problem, min(m.w-8, 60)) {
			lines = append(lines, m.st.danger.Render(line))
		}
		lines = append(lines, "", m.st.muted.Render("r "+i18n.Rune("Try now")+"   q "+i18n.Rune("Log out")))
	}
	body := lipgloss.Place(m.w, m.h-3, lipgloss.Center, lipgloss.Center, lipgloss.JoinVertical(lipgloss.Center, lines...))

	// Rune's footer: quiet, in the terminal's own font, never a notice
	// that blocks.
	foot := []string{"", ""}
	if m.info != nil {
		foot[0] = m.st.muted.Render(i18n.Rune("Your Rune is currently running on") + " Haugr EDS " + m.info.Version)
		if m.info.Persistent != nil && !*m.info.Persistent {
			foot[1] = m.st.accent.Render(i18n.Rune("Test system")) + m.st.muted.Render(" · "+i18n.Rune("The data will not survive a restart of the stone."))
		}
	}
	return body + "\n" + lipgloss.PlaceHorizontal(m.w, lipgloss.Center, foot[0]) + "\n" + lipgloss.PlaceHorizontal(m.w, lipgloss.Center, foot[1]) + "\n"
}
