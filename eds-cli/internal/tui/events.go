package tui

import (
	"context"
	"sort"
	"strings"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/chat"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// arrived takes what a command brought back.
func (m *model) arrived(msg tea.Msg) tea.Cmd {
	switch msg := msg.(type) {
	case connectedMsg:
		return m.connected(msg)
	case roomsMsg:
		if msg.err != nil {
			return m.failed(msg.err)
		}
		m.setRooms(msg.rooms, msg.users)
	case membersMsg:
		if msg.err != nil {
			return m.failed(msg.err)
		}
		m.members[msg.roomID] = msg.members
	case userRoomsMsg:
		if msg.err != nil {
			return m.failed(msg.err)
		}
		sort.Slice(msg.rooms, func(i, j int) bool {
			return strings.ToLower(msg.rooms[i].CircleName) < strings.ToLower(msg.rooms[j].CircleName)
		})
		m.userRooms[msg.userID] = msg.rooms
	case historyMsg:
		return m.history(msg)
	case stonesMsg:
		m.showStones(msg)
	case frameMsg:
		return tea.Batch(m.frame(msg.frame), m.waitFrameIfEvent(msg))
	case chatLostMsg:
		// The chat is gone: the stone is down, or it ended the session.
		m.chat = nil
		if msg.err == nil || m.phase != phaseMain {
			return nil
		}
		if unreachable(msg.err) {
			return m.failed(msg.err)
		}
		// A stone keeps one session per account. Logging in again by
		// itself would end the session of whoever took it, and so on, so
		// the person decides.
		m.phase = phaseFailed
		m.problem = i18n.T("The stone ended the session. A stone keeps one session per account: a login or logout with the same account elsewhere ends this one.")
	case filesMsg:
		if m.room == nil || msg.roomID != m.room.ID {
			return nil
		}
		if msg.err != nil {
			return m.failed(msg.err)
		}
		m.entries = msg.entries
		m.fileCursor = min(m.fileCursor, max(len(m.entries)-1, 0))
		for _, e := range msg.entries {
			if !e.IsFolder() {
				m.fileNames[e.DataID] = e.DataName
			}
		}
	case doneMsg:
		if msg.err != nil {
			return m.failed(msg.err)
		}
		if msg.note != "" {
			m.say(msg.note, false)
		}
		return m.reload(msg.reload)
	case transferMsg:
		m.transfer = nil
		if msg.err != nil {
			return m.failed(msg.err)
		}
		m.say(msg.note, false)
		return m.loadFiles()
	case settingsMsg:
		if msg.err != nil {
			return m.failed(msg.err)
		}
		m.stoneSettings(msg.settings)
	case inviteMsg:
		if msg.err != nil {
			return m.failed(msg.err)
		}
		if msg.added {
			m.say(i18n.Rune("{name} already uses Rune, and was added to the room.", "name", msg.name), false)
		} else {
			m.showInvitation(msg.name, msg.link)
		}
		return m.reload("members")
	}
	return nil
}

// waitFrameIfEvent waits for the next chat event, after one that came from
// the chat itself; the answer to an own request doesn't start another wait.
func (m *model) waitFrameIfEvent(msg frameMsg) tea.Cmd {
	if msg.answer {
		return nil
	}
	return m.waitFrame()
}

// reload reads again what an action changed.
func (m *model) reload(what string) tea.Cmd {
	switch what {
	case "rooms":
		return m.loadRooms()
	case "members":
		cmds := []tea.Cmd{m.loadRooms()}
		for _, room := range m.rooms {
			if room.ID == m.openMembers || (m.room != nil && room.ID == m.room.ID) {
				cmds = append(cmds, m.loadMembers(room))
			}
		}
		return tea.Batch(cmds...)
	case "files":
		return m.loadFiles()
	}
	return nil
}

// failed shows what went wrong: a stone that doesn't answer gets Rune's
// notice, with the panes kept behind it; anything else a note.
func (m *model) failed(err error) tea.Cmd {
	if err == nil {
		return nil
	}
	if unreachable(err) {
		if m.phase == phaseMain || m.phase == phaseConnecting {
			m.phase = phaseDown
			m.problem = m.friendly(err)
			if m.chat != nil {
				m.chat.Close()
				m.chat = nil
			}
		}
		return nil
	}
	m.say(m.friendly(err), true)
	return nil
}

func (m *model) connected(msg connectedMsg) tea.Cmd {
	if msg.info != nil {
		m.info = msg.info
	}
	if msg.err != nil {
		if unreachable(msg.err) {
			m.phase, m.problem = phaseDown, m.friendly(msg.err)
		} else if m.phase != phaseMain {
			m.phase, m.problem = phaseFailed, m.friendly(msg.err)
		}
		return nil
	}
	if m.chat != nil {
		m.chat.Close()
	}
	m.stone, m.chat, m.unread = msg.stone, msg.chat, msg.unread
	m.phase, m.problem = phaseMain, ""
	m.setRooms(msg.rooms, msg.users)
	m.ensureFocus()
	cmds := []tea.Cmd{m.waitFrame()}
	// After the stone was away, the open room is read again.
	if m.room != nil {
		for i := range m.rooms {
			if m.rooms[i].ID == m.room.ID {
				cmds = append(cmds, m.openRoom(m.rooms[i]))
			}
		}
	}
	return tea.Batch(cmds...)
}

func (m *model) setRooms(rooms []service.Room, users []client.Member) {
	m.rooms, m.users = rooms, users
	m.admin, m.first = false, false
	for _, user := range users {
		if m.stone != nil && strings.EqualFold(user.AccountName, m.stone.Account) {
			m.admin = user.MemberRole == client.RoleAdmin
			m.first = user.FirstAdministrator
		}
	}
	m.roomCursor = min(m.roomCursor, max(len(m.rows())-1, 0))
	// The open room may have changed (renamed, another access), or be gone.
	if m.room != nil {
		found := false
		for i := range rooms {
			if rooms[i].ID == m.room.ID && rooms[i].In() {
				m.room, found = &rooms[i], true
			}
		}
		if !found {
			m.closeRoom()
		}
	}
}

func (m *model) closeRoom() {
	m.room, m.msgs, m.entries, m.path = nil, nil, nil, nil
	m.blocked, m.chamber = "", false
	m.selected, m.scroll, m.replyTo, m.editing = -1, 0, "", ""
	m.compose.clear()
}

// openRoom shows a room in the chat and the files: its newest messages,
// its members (for names and checkmarks) and its top folder.
func (m *model) openRoom(room service.Room) tea.Cmd {
	if !room.In() {
		m.say(i18n.T("You are not in %s, so it can't be opened.", room.Name), true)
		return nil
	}
	if m.room == nil || m.room.ID != room.ID {
		m.closeRoom()
		m.receipts = map[string]client.ChatReceipt{}
		m.typists = map[string]typist{}
	}
	m.room = &room
	m.loading, m.earlier = true, false
	return tea.Batch(m.loadHistory(room.ID, ""), m.loadMembers(room), m.loadFiles())
}

func (m *model) history(msg historyMsg) tea.Cmd {
	if m.room == nil || msg.roomID != m.room.ID {
		return nil
	}
	m.loading = false
	if msg.err != nil {
		return m.failed(msg.err)
	}
	m.blocked, m.chamber = msg.blocked, msg.chamber
	if msg.earlier {
		m.msgs = append(append([]client.ChatMessage{}, msg.messages...), m.msgs...)
		if m.selected >= 0 {
			m.selected += len(msg.messages)
		}
		m.earlier = len(msg.messages) >= 50
		return nil
	}
	m.msgs = msg.messages
	m.earlier = len(msg.messages) >= 50
	m.scroll, m.selected = 0, -1
	for _, receipt := range msg.receipts {
		m.receipts[receipt.MemberID] = receipt
	}
	return m.markRead()
}

// markRead tells the stone that the open room is read up to its newest
// message, when there was something unread.
func (m *model) markRead() tea.Cmd {
	if m.room == nil || len(m.msgs) == 0 {
		return nil
	}
	roomID, newest := m.room.ID, m.msgs[len(m.msgs)-1].MessageID
	m.unread[roomID] = 0
	return m.chatDo(func(ctx context.Context, conn *chat.Client) error {
		return conn.MarkRead(ctx, roomID, newest)
	})
}

// frame takes what happened in the chat.
func (m *model) frame(f client.ChatFrame) tea.Cmd {
	roomID := f.CircleID
	if f.Message != nil && f.Message.CircleID != "" {
		roomID = f.Message.CircleID
	}
	open := m.room != nil && m.room.ID == roomID
	switch f.Type {
	case client.FrameMessage, client.FrameSent:
		if f.Message == nil {
			return nil
		}
		own := f.Type == client.FrameSent
		if !own {
			delete(m.typists, f.Message.MemberID)
		}
		if open {
			m.msgs = append(m.msgs, *f.Message)
			if m.scroll > 0 {
				// Reading earlier messages: the view stays where it is.
				m.scroll += m.messageHeight(len(m.msgs) - 1)
			}
			if own || (m.scroll == 0 && m.visible(paneChat)) {
				return m.markRead()
			}
		}
		if own {
			return nil
		}
		m.unread[roomID]++
		cmds := []tea.Cmd{m.chatDo(func(ctx context.Context, conn *chat.Client) error { return conn.Received(ctx, roomID) })}
		if m.ui.Bell {
			cmds = append(cmds, tea.Raw("\a"))
		}
		return tea.Batch(cmds...)
	case client.FrameEdited, client.FrameReacted:
		if open && f.Message != nil {
			for i := range m.msgs {
				if m.msgs[i].MessageID == f.Message.MessageID {
					m.msgs[i] = *f.Message
				}
			}
		}
	case client.FrameDeleted:
		if open {
			for i := range m.msgs {
				if m.msgs[i].MessageID == f.MessageID {
					m.msgs = append(m.msgs[:i], m.msgs[i+1:]...)
					if m.selected >= len(m.msgs) {
						m.selected = len(m.msgs) - 1
					}
					break
				}
			}
		}
	case client.FrameReceipt:
		if open && f.Receipt != nil && f.Receipt.MemberID != m.chat.MemberID {
			m.receipts[f.Receipt.MemberID] = *f.Receipt
		}
	case client.FrameTyping:
		if open && f.MemberID != "" && m.chat != nil && f.MemberID != m.chat.MemberID {
			m.typists[f.MemberID] = typist{name: f.AccountName, until: time.Now().Add(5 * time.Second)}
		}
	case client.FrameChanged:
		// The room's files, or who is in it, changed.
		if open {
			return tea.Batch(m.loadFiles(), m.reload("members"))
		}
		return m.loadRooms()
	case client.FrameLeft:
		return m.loadRooms()
	case client.FramePresent, client.FrameAbsent:
		// The chat knows who is online; the rooms pane asks it when drawn.
	case client.FrameCall:
		// Tolk makes no calls: it says that someone calls, in Rune's words,
		// and where calls are made.
		told := i18n.Rune("{name} started a call in {room}.", "name", f.AccountName, "room", m.roomName(roomID))
		if f.Direct {
			told = i18n.Rune("{name} is calling you.", "name", f.AccountName)
		}
		m.say(told+" "+i18n.T("Calls are made with Rune."), false)
		if m.ui.Bell {
			return tea.Raw("\a")
		}
	}
	return nil
}

// roomName is the name of a room of the stone; its ID if it isn't known.
func (m *model) roomName(roomID string) string {
	for i := range m.rooms {
		if m.rooms[i].ID == roomID {
			return m.rooms[i].Name
		}
	}
	return roomID
}

// online reports whether a member is online in a room, as the stone told;
// nobody is while the stone isn't connected.
func (m *model) online(roomID, memberID string) bool {
	return m.chat != nil && m.chat.Online(roomID, memberID)
}

// light is the mark of a person who is online, or as much room.
func (m *model) light(online bool) string {
	if online {
		return m.st.accent.Render("● ")
	}
	return "  "
}

// visible reports whether a pane is on the screen.
func (m *model) visible(pane string) bool {
	if m.narrow() {
		return m.focus == pane
	}
	return m.layout.has(pane)
}

// name returns the name of a member of the open room.
func (m *model) name(memberID string) string {
	if m.room != nil {
		for _, member := range m.members[m.room.ID] {
			if member.MemberID == memberID {
				return member.AccountName
			}
		}
	}
	for _, user := range m.users {
		if user.MemberID == memberID {
			return user.AccountName
		}
	}
	return ""
}

// ticks are the checkmarks of an own message, as Rune shows them: 1 when
// the stone has it, 2 when it reached a client of everyone else in the
// room, 3 when everyone has read it. A marker is "up to a message": by its
// place among the messages shown, or, when that message isn't shown
// (earlier, or deleted), by its time.
func (m *model) ticks(index int) int {
	if m.room == nil || m.chat == nil {
		return 1
	}
	// A message is reached by a marker which stands at its place or after.
	position := m.msgs[index].Position
	reaches := func(marker int64) bool { return marker > 0 && position <= marker }
	others, read, received := 0, 0, 0
	for _, member := range m.members[m.room.ID] {
		if member.MemberID == m.chat.MemberID {
			continue
		}
		others++
		receipt := m.receipts[member.MemberID]
		if reaches(receipt.ReadPosition) {
			read++
		} else if reaches(receipt.ReceivedPosition) {
			received++
		}
	}
	switch {
	case others == 0:
		return 1
	case read == others:
		return 3
	case read+received == others:
		return 2
	}
	return 1
}
