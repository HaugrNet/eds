package tui

import (
	"context"
	"strings"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/chat"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// own reports whether a message is the account's.
func (m *model) own(msg client.ChatMessage) bool {
	return m.chat != nil && msg.MemberID == m.chat.MemberID
}

// messageLines draws one message for a width: who and when, what it
// answers, its text, its file, its reactions.
func (m *model) messageLines(index, w int) []string {
	msg := m.msgs[index]
	who := m.st.title.Render(msg.AccountName)
	if m.own(msg) {
		who = m.st.accent.Render(msg.AccountName)
	}
	head := who
	if t, ok := format.ParseTime(msg.Time); ok {
		local := t.Local()
		stamp := m.locale.Time(local)
		if now := time.Now(); local.Year() != now.Year() || local.YearDay() != now.YearDay() {
			stamp = m.locale.DateTime(local)
		}
		head += m.st.muted.Render("  " + stamp)
	}
	if msg.Edited != "" {
		head += m.st.muted.Render("  " + i18n.Rune("(edited)"))
	}
	if m.own(msg) {
		switch m.ticks(index) {
		case 1:
			head += m.st.muted.Render("  ✓")
		case 2:
			head += m.st.muted.Render("  ✓✓")
		default:
			head += "  " + m.st.accent.Render("✓✓")
		}
	}
	lines := []string{head}
	if msg.ReplyTo != "" {
		quote := i18n.Rune("Reply")
		for _, other := range m.msgs {
			if other.MessageID == msg.ReplyTo {
				quote = other.AccountName + ": " + other.Text
			}
		}
		lines = append(lines, m.st.muted.Render(fit("↪ "+strings.ReplaceAll(quote, "\n", " "), min(w, width("↪ "+quote)))))
	}
	if msg.Damaged {
		lines = append(lines, m.st.muted.Render(i18n.Rune("This message can't be read.")))
	} else if msg.Text != "" {
		for _, paragraph := range strings.Split(msg.Text, "\n") {
			lines = append(lines, wrap(paragraph, w)...)
		}
	}
	if msg.DataID != "" {
		name := m.fileNames[msg.DataID]
		if name == "" {
			name = i18n.Rune("Shared file…")
		}
		lines = append(lines, m.st.focus.Render("▤ "+name))
	}
	if len(msg.Reactions) > 0 {
		var parts []string
		for _, reaction := range client.Reactions {
			if who := msg.Reactions[reaction]; len(who) > 0 {
				parts = append(parts, reaction+" "+m.locale.Number(float64(len(who)), 0))
			}
		}
		lines = append(lines, m.st.muted.Render(strings.Join(parts, "   ")))
	}
	return lines
}

// messageHeight is the number of lines a message takes, with the empty
// line after it.
func (m *model) messageHeight(index int) int {
	return len(m.messageLines(index, max(m.chatWidth()-2, 10))) + 1
}

// chatWidth is the width of the chat pane's inside.
func (m *model) chatWidth() int {
	if r, ok := m.places()[paneChat]; ok {
		return r.w - 2
	}
	return max(m.w-2, 10)
}

// chatView draws the chat pane's inside: the messages, who is typing, what
// is answered or edited, and the line to write in. It returns the cursor's
// place in it, or -1.
func (m *model) chatView(w, h int, focused bool) ([]string, int, int) {
	if m.room == nil {
		return []string{"", " " + m.st.muted.Render(i18n.Rune("Choose a room."))}, -1, -1
	}
	// The foot: typing, the reply or edit bar, the input.
	var foot []string
	if names := m.typing(); names != "" {
		foot = append(foot, m.st.muted.Render(" "+names))
	}
	if m.editing != "" {
		foot = append(foot, m.st.focus.Render(" "+i18n.Rune("Edit message"))+m.st.muted.Render("  Esc"))
	} else if m.replyTo != "" {
		quote := ""
		for _, msg := range m.msgs {
			if msg.MessageID == m.replyTo {
				quote = msg.AccountName + ": " + strings.ReplaceAll(msg.Text, "\n", " ")
			}
		}
		foot = append(foot, fit(m.st.focus.Render(" "+i18n.Rune("Replying to"))+" "+m.st.muted.Render(quote), w))
	}
	cursorX, cursorY := -1, -1
	switch {
	case m.blocked != "":
		// A chat which is closed or off, or a chamber whose key this device
		// hasn't: nothing is written, and it is said why.
		for _, line := range wrap(m.blocked, w-2) {
			foot = append(foot, m.st.muted.Render(" "+line))
		}
	case !m.room.CanWrite():
		foot = append(foot, m.st.muted.Render(" "+i18n.Rune("You can only read in this room.")))
	case m.selected >= 0:
		foot = append(foot, m.st.muted.Render(" "+m.selectionHint()))
	default:
		text, column := m.compose.view(w - 3)
		if m.compose.empty() {
			text = m.st.muted.Render(fit(i18n.Rune("Message"), w-3))
		}
		foot = append(foot, m.st.focus.Render(" › ")+text)
		if focused {
			cursorX, cursorY = 3+column, h-1
		}
	}
	room := h - len(foot)
	if room < 1 {
		return foot[max(len(foot)-h, 0):], cursorX, cursorY
	}

	// The messages, from the newest up, as many as fit above the foot.
	var body []string
	switch {
	case m.loading && len(m.msgs) == 0:
		body = []string{" " + m.st.muted.Render("…")}
	case len(m.msgs) == 0:
		body = []string{" " + m.st.muted.Render(i18n.Rune("No messages yet."))}
	default:
		var all []string
		starts := make([]int, len(m.msgs))
		if m.earlier {
			all = append(all, " "+m.st.muted.Render("↑ "+i18n.Rune("Earlier messages")+"  PgUp"), "")
		}
		for i := range m.msgs {
			starts[i] = len(all)
			mark := " "
			if i == m.selected {
				mark = m.st.accent.Render("▌")
			}
			for _, line := range m.messageLines(i, w-2) {
				all = append(all, mark+line)
			}
			all = append(all, "")
		}
		if len(all) > 0 {
			all = all[:len(all)-1]
		}
		// Keep the selected message on the screen.
		if m.selected >= 0 && m.selected < len(starts) {
			top := starts[m.selected]
			bottom := len(all)
			if m.selected+1 < len(starts) {
				bottom = starts[m.selected+1] - 1
			}
			if fromEnd := len(all) - bottom; m.scroll > fromEnd {
				m.scroll = fromEnd
			}
			if fromEnd := len(all) - top - room; m.scroll < fromEnd {
				m.scroll = fromEnd
			}
		}
		m.scroll = max(min(m.scroll, len(all)-room), 0)
		end := len(all) - m.scroll
		body = all[max(end-room, 0):end]
	}
	lines := make([]string, 0, h)
	for i := len(body); i < room; i++ {
		lines = append(lines, "")
	}
	lines = append(lines, body...)
	return append(lines, foot...), cursorX, cursorY
}

// typing says who is writing, in Rune's words.
func (m *model) typing() string {
	var names []string
	for _, t := range m.typists {
		names = append(names, t.name)
	}
	switch len(names) {
	case 0:
		return ""
	case 1:
		return i18n.Rune("{name} is typing…", "name", names[0])
	case 2:
		return i18n.Rune("{name} and {other} are typing…", "name", names[0], "other", names[1])
	}
	return i18n.Rune("{n} people are typing…", "n", m.locale.Number(float64(len(names)), 0))
}

// selectionHint lists what can be done with the selected message.
func (m *model) selectionHint() string {
	if m.selected < 0 || m.selected >= len(m.msgs) {
		return ""
	}
	msg := m.msgs[m.selected]
	var parts []string
	if m.room.CanWrite() {
		parts = append(parts, "r "+i18n.Rune("Reply"), "1–6 "+strings.Join(client.Reactions, " "))
		if m.own(msg) {
			parts = append(parts, "e "+i18n.Rune("Edit message"))
		}
	}
	if m.own(msg) {
		parts = append(parts, "d "+i18n.Rune("Delete message"))
	}
	if msg.DataID != "" {
		parts = append(parts, "s "+i18n.Rune("Download"))
	}
	return strings.Join(parts, " · ")
}

// chatKey handles a key in the chat pane.
func (m *model) chatKey(msg tea.KeyPressMsg) tea.Cmd {
	if m.room == nil {
		return nil
	}
	key := msg.String()
	page := max(m.h/2, 3)
	switch key {
	case "pgup":
		m.scroll += page
		if m.earlier && !m.loading && len(m.msgs) > 0 {
			m.loading = true
			return m.loadHistory(m.room.ID, m.msgs[0].MessageID)
		}
		return nil
	case "pgdown":
		m.scroll = max(m.scroll-page, 0)
		if m.scroll == 0 && m.unread[m.room.ID] > 0 {
			return m.markRead()
		}
		return nil
	}
	if m.selected >= 0 {
		return m.selectionKey(key)
	}
	switch key {
	case "up":
		if len(m.msgs) > 0 {
			m.selected = len(m.msgs) - 1
		}
		return nil
	case "esc":
		if m.editing != "" {
			m.compose.clear()
		}
		m.replyTo, m.editing = "", ""
		return nil
	case "enter":
		return m.submit()
	}
	if !m.room.CanWrite() {
		return nil
	}
	if m.compose.key(msg) {
		return m.typed()
	}
	return nil
}

// typed tells the room that the account is writing, at most every 3 s.
func (m *model) typed() tea.Cmd {
	if m.compose.empty() || time.Since(m.typedAt) < 3*time.Second || m.chat == nil {
		return nil
	}
	m.typedAt = time.Now()
	conn, roomID := m.chat, m.room.ID
	return func() tea.Msg {
		conn.Typing(roomID)
		return nil
	}
}

// submit sends what was written: a message, an edit, or a command
// (/send <file>, /room <name>).
func (m *model) submit() tea.Cmd {
	text := strings.TrimSpace(m.compose.text())
	if text == "" {
		return nil
	}
	if err := service.CheckWrite(m.room); err != nil && !strings.HasPrefix(text, "/room ") {
		m.say(err.Error(), true)
		return nil
	}
	m.compose.clear()
	m.scroll = 0
	roomID := m.room.ID
	switch {
	case strings.HasPrefix(text, "/send "):
		return m.upload(strings.TrimPrefix(text, "/send "), true)
	case strings.HasPrefix(text, "/room "):
		name := strings.TrimSpace(strings.TrimPrefix(text, "/room "))
		for _, room := range m.rooms {
			if strings.EqualFold(room.Name, name) {
				return m.openRoom(room)
			}
		}
		m.say(i18n.T("No room named %q on the stone %q.", name, m.stone.Name), true)
		return nil
	case m.editing != "":
		id := m.editing
		m.editing = ""
		return m.chatAnswer(roomID, id, func(ctx context.Context, conn *chat.Client) (*client.ChatFrame, error) {
			return conn.Edit(ctx, roomID, id, text)
		})
	}
	replyTo := m.replyTo
	m.replyTo = ""
	return m.sendMessage(roomID, text, "", replyTo)
}

// selectionKey handles a key while a message is selected.
func (m *model) selectionKey(key string) tea.Cmd {
	msg := m.msgs[m.selected]
	roomID := m.room.ID
	switch key {
	case "up", "k":
		if m.selected > 0 {
			m.selected--
		} else if m.earlier && !m.loading {
			m.loading = true
			return m.loadHistory(roomID, m.msgs[0].MessageID)
		}
	case "down", "j":
		if m.selected < len(m.msgs)-1 {
			m.selected++
		} else {
			m.selected, m.scroll = -1, 0
		}
	case "esc", "enter", "q":
		m.selected, m.scroll = -1, 0
	case "r":
		if m.room.CanWrite() {
			m.replyTo, m.editing, m.selected, m.scroll = msg.MessageID, "", -1, 0
		}
	case "e":
		if m.own(msg) && m.room.CanWrite() {
			m.editing, m.replyTo, m.selected, m.scroll = msg.MessageID, "", -1, 0
			m.compose.set(msg.Text)
		} else if !m.own(msg) {
			m.say(i18n.Rune("Only the author can edit a message."), true)
		}
	case "d":
		if !m.own(msg) {
			m.say(i18n.Rune("Only the author can delete a message."), true)
			return nil
		}
		question := i18n.Rune("Delete this message for everyone in the room?")
		if msg.DataID != "" {
			question += " " + i18n.Rune("The shared file stays in the room.")
		}
		m.confirm(i18n.Rune("Delete message"), question, func(m *model) tea.Cmd {
			return m.chatAnswer(roomID, msg.MessageID, func(ctx context.Context, conn *chat.Client) (*client.ChatFrame, error) {
				return conn.Delete(ctx, roomID, msg.MessageID)
			})
		})
	case "s":
		if msg.DataID != "" {
			name := m.fileNames[msg.DataID]
			if name == "" {
				name = "download"
			}
			return m.download(msg.DataID, name, 0)
		}
	case "1", "2", "3", "4", "5", "6":
		if m.room.CanWrite() {
			reaction := client.Reactions[int(key[0]-'1')]
			return m.chatAnswer(roomID, msg.MessageID, func(ctx context.Context, conn *chat.Client) (*client.ChatFrame, error) {
				return conn.React(ctx, roomID, msg.MessageID, reaction)
			})
		}
	}
	return nil
}
