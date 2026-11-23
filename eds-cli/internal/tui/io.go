package tui

import (
	"context"
	"io"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/chat"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// Everything that reaches the stone runs as a command, off the screen's own
// loop, and comes back as one of these messages.

type connectedMsg struct {
	stone  *service.Stone
	info   *client.VersionResponse
	chat   *chat.Client
	rooms  []service.Room
	users  []client.Member
	unread map[string]int
	err    error
}

type roomsMsg struct {
	rooms []service.Room
	users []client.Member
	err   error
}

type membersMsg struct {
	roomID  string
	members []client.Trustee
	err     error
}

type userRoomsMsg struct {
	userID string
	rooms  []client.Circle
	err    error
}

type historyMsg struct {
	roomID   string
	messages []client.ChatMessage
	receipts []client.ChatReceipt
	earlier  bool // an earlier page, put before what is shown
	err      error
	// blocked is why nothing can be written to the room's chat: it has
	// none, it is closed or off, or the room is a chamber whose key this
	// device hasn't. chamber says that the room is one.
	blocked string
	chamber bool
}

// frameMsg is something that happened in the chat: an event, or the answer
// to an own request, which the stone sends to the asker only.
type frameMsg struct {
	frame  client.ChatFrame
	answer bool
}

type chatLostMsg struct{ err error }

type filesMsg struct {
	roomID, folderID string
	entries          []service.Entry
	err              error
}

// doneMsg ends an action: a note for the last line, and what to read again.
type doneMsg struct {
	note   string
	err    error
	reload string // rooms, members, files, or nothing
}

type transferMsg struct {
	note string
	err  error
}

type inviteMsg struct {
	name, link string
	added      bool
	err        error
}

const requestTime = 30 * time.Second

// connect reaches the stone: its version first, which needs no login, then
// the session, the rooms and users, and the chat with every room joined.
func (m *model) connect() tea.Cmd {
	svc, name, ctx, present := m.opts.Service, m.opts.Stone, m.ctx, m.ui.Present
	return func() tea.Msg {
		out := connectedMsg{unread: map[string]int{}}
		info, err := svc.Info(name)
		if err != nil {
			return connectedMsg{err: err}
		}
		versionCtx, cancel := context.WithTimeout(ctx, requestTime)
		out.info, err = client.New(info.URL).Version(versionCtx)
		cancel()
		if err != nil {
			return connectedMsg{err: err}
		}
		loginCtx, cancel := context.WithTimeout(ctx, 2*requestTime)
		defer cancel()
		if out.stone, err = svc.Stone(loginCtx, name); err != nil {
			return connectedMsg{info: out.info, err: err}
		}
		out.stone.ForgetRooms()
		if out.rooms, err = out.stone.Rooms(loginCtx); err != nil {
			return connectedMsg{info: out.info, err: err}
		}
		if out.users, err = out.stone.Users(loginCtx); err != nil {
			return connectedMsg{info: out.info, err: err}
		}
		err = out.stone.Do(loginCtx, func(token string) error {
			var err error
			out.chat, err = chat.Dial(loginCtx, out.stone.URL, token, out.stone)
			return err
		})
		if err != nil {
			return connectedMsg{info: out.info, err: err}
		}
		// The interactive mode shows its person as online, unless they
		// chose not to be (the setting "present").
		if present {
			out.chat.Show()
		}
		for _, room := range out.rooms {
			if room.In() {
				if unread, err := out.chat.Join(loginCtx, room.ID); err == nil {
					out.unread[room.ID] = unread
				}
			}
		}
		return out
	}
}

// waitFrame waits for the next thing that happens in the chat.
func (m *model) waitFrame() tea.Cmd {
	conn := m.chat
	if conn == nil {
		return nil
	}
	return func() tea.Msg {
		frame, ok := <-conn.Events()
		if !ok {
			return chatLostMsg{err: conn.Err()}
		}
		return frameMsg{frame: frame}
	}
}

// loadRooms reads the rooms and users again.
func (m *model) loadRooms() tea.Cmd {
	stone, ctx, conn := m.stone, m.ctx, m.chat
	known := map[string]bool{}
	for _, room := range m.rooms {
		known[room.ID] = room.In()
	}
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		stone.ForgetRooms()
		rooms, err := stone.Rooms(ctx)
		if err != nil {
			return roomsMsg{err: err}
		}
		// A room the account is new in is joined, for its messages.
		for _, room := range rooms {
			if room.In() && !known[room.ID] && conn != nil {
				_, _ = conn.Join(ctx, room.ID)
			}
		}
		users, err := stone.FreshUsers(ctx)
		return roomsMsg{rooms: rooms, users: users, err: err}
	}
}

func (m *model) loadMembers(room service.Room) tea.Cmd {
	stone, ctx := m.stone, m.ctx
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		members, err := stone.Members(ctx, &room)
		return membersMsg{roomID: room.ID, members: members, err: err}
	}
}

func (m *model) loadUserRooms(user client.Member) tea.Cmd {
	stone, ctx := m.stone, m.ctx
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		var detail *client.FetchMemberResponse
		err := stone.Do(ctx, func(token string) error {
			var err error
			detail, err = stone.Client.Member(ctx, token, user.MemberID)
			return err
		})
		if err != nil {
			return userRoomsMsg{userID: user.MemberID, err: err}
		}
		return userRoomsMsg{userID: user.MemberID, rooms: detail.Circles}
	}
}

// loadHistory reads a page of a room's messages: the newest, or the one
// before a message.
func (m *model) loadHistory(roomID, before string) tea.Cmd {
	conn, ctx, stone := m.chat, m.ctx, m.stone
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		messages, receipts, err := conn.Page(ctx, roomID, before, 50)
		out := historyMsg{roomID: roomID, messages: messages, receipts: receipts, earlier: before != "", err: err}
		if unavailable := chat.Unavailable(conn.State(roomID)); unavailable != "" {
			out.blocked = unavailable
		}
		if room, roomErr := stone.RoomState(ctx, roomID); roomErr == nil {
			out.chamber = room.Chamber()
			if refusal := room.Refusal(); refusal != "" {
				out.blocked = refusal
			}
		}
		return out
	}
}

func (m *model) loadFiles() tea.Cmd {
	if m.room == nil {
		return nil
	}
	stone, ctx, room := m.stone, m.ctx, *m.room
	folderID := ""
	if len(m.path) > 0 {
		folderID = m.path[len(m.path)-1].id
	}
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		entries, err := stone.List(ctx, &room, folderID)
		// Of a chamber's file the size is that of its content.
		for i := range entries {
			if !entries[i].IsFolder() {
				entries[i].Size = stone.ShownSize(ctx, room.ID, entries[i].Size)
			}
		}
		return filesMsg{roomID: room.ID, folderID: folderID, entries: entries, err: err}
	}
}

// act runs one action against the stone and reports how it went.
func (m *model) act(note, reload string, fn func(ctx context.Context, stone *service.Stone) error) tea.Cmd {
	stone, ctx := m.stone, m.ctx
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		if err := fn(ctx, stone); err != nil {
			return doneMsg{err: err}
		}
		return doneMsg{note: note, reload: reload}
	}
}

// chatDo runs one request on the chat; its answer comes back as an event or
// not at all, so only a failure is reported.
func (m *model) chatDo(fn func(ctx context.Context, conn *chat.Client) error) tea.Cmd {
	conn, ctx := m.chat, m.ctx
	if conn == nil {
		return nil
	}
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		if err := fn(ctx, conn); err != nil {
			return doneMsg{err: err}
		}
		return nil
	}
}

// chatAnswer runs a request whose answer says what changed (an edit, a
// reaction, a deletion), and shows it like an event.
func (m *model) chatAnswer(roomID, messageID string, fn func(ctx context.Context, conn *chat.Client) (*client.ChatFrame, error)) tea.Cmd {
	conn, ctx := m.chat, m.ctx
	if conn == nil {
		return nil
	}
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		answer, err := fn(ctx, conn)
		if err != nil {
			return doneMsg{err: err}
		}
		if answer.CircleID == "" {
			answer.CircleID = roomID
		}
		if answer.MessageID == "" {
			answer.MessageID = messageID
		}
		return frameMsg{frame: *answer, answer: true}
	}
}

// sendMessage posts a message; the stone's answer is shown like a message
// that arrived.
func (m *model) sendMessage(roomID, text, dataID, replyTo string) tea.Cmd {
	conn, ctx := m.chat, m.ctx
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		message, err := conn.Send(ctx, roomID, text, dataID, replyTo)
		if err != nil {
			return doneMsg{err: err}
		}
		return frameMsg{frame: client.ChatFrame{Type: client.FrameSent, CircleID: roomID, Message: message}, answer: true}
	}
}

// countingReader counts what passes, for the progress.
type countingReader struct {
	r io.Reader
	t *transfer
}

func (c countingReader) Read(p []byte) (int, error) {
	n, err := c.r.Read(p)
	c.t.done.Add(int64(n))
	return n, err
}

// upload sends a local file into the folder shown, and shares it in the
// chat if asked.
func (m *model) upload(path string, share bool) tea.Cmd {
	if m.room == nil {
		return nil
	}
	if err := service.CheckWrite(m.room); err != nil {
		m.say(err.Error(), true)
		return nil
	}
	path = expandHome(strings.TrimSpace(path))
	file, err := os.Open(path)
	if err != nil {
		m.say(i18n.T("Cannot read %s: %v", path, pathError(err)), true)
		return nil
	}
	info, err := file.Stat()
	if err != nil || !info.Mode().IsRegular() {
		file.Close()
		m.say(i18n.T("%s is not a file.", path), true)
		return nil
	}
	name := filepath.Base(path)
	if err := service.CheckName(name); err != nil {
		file.Close()
		m.say(err.Error(), true)
		return nil
	}
	t := &transfer{name: name, total: info.Size(), upload: true}
	m.transfer = t
	stone, ctx, room := m.stone, m.ctx, *m.room
	folderID := ""
	if len(m.path) > 0 {
		folderID = m.path[len(m.path)-1].id
	}
	conn := m.chat
	return func() tea.Msg {
		defer file.Close()
		var id string
		err := stone.Do(ctx, func(token string) error {
			var err error
			id, err = stone.Client.AddData(ctx, token, client.ProcessDataRequest{CircleID: room.ID, FolderID: folderID, DataName: name, MediaType: service.MediaTypeOf(name)})
			return err
		})
		if err != nil {
			return transferMsg{err: err}
		}
		// In a chamber the content is encrypted on its way.
		content, length, err := stone.Sealing(ctx, room.ID, countingReader{io.LimitReader(file, t.total), t}, t.total)
		if err == nil {
			_, err = stone.Client.Upload(ctx, stone.Token(), id, content, length)
		}
		if err != nil {
			_ = stone.Client.DeleteData(ctx, stone.Token(), id)
			return transferMsg{err: err}
		}
		if share && conn != nil {
			shareCtx, cancel := context.WithTimeout(ctx, requestTime)
			defer cancel()
			if _, err := conn.Send(shareCtx, room.ID, "", id, ""); err != nil {
				return transferMsg{err: err}
			}
		}
		return transferMsg{note: i18n.T("Uploaded %s.", name)}
	}
}

// download saves a file of the room in the directory Tolk was started in,
// under a name that isn't taken.
func (m *model) download(id, name string, size int64) tea.Cmd {
	t := &transfer{name: name, total: size}
	m.transfer = t
	stone, ctx := m.stone, m.ctx
	roomID := ""
	if m.room != nil {
		roomID = m.room.ID
	}
	return func() tea.Msg {
		var content *client.Content
		err := stone.Do(ctx, func(token string) error {
			var err error
			content, err = stone.Client.Download(ctx, token, id, 0, "")
			return err
		})
		if err != nil {
			return transferMsg{err: err}
		}
		defer content.Body.Close()
		if content.Total > 0 {
			t.total = content.Total
		}
		target := freeName(service.LocalName(name))
		file, err := os.OpenFile(target, os.O_WRONLY|os.O_CREATE|os.O_EXCL, 0o600)
		if err != nil {
			return transferMsg{err: exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", target, pathError(err)))}
		}
		// Of a chamber, what arrives is decrypted on its way.
		readable, protected, err := stone.Opening(ctx, roomID, countingReader{content.Body, t})
		if err != nil {
			file.Close()
			_ = os.Remove(target)
			return transferMsg{err: err}
		}
		_, err = io.Copy(file, readable)
		if closeErr := file.Close(); err == nil {
			err = closeErr
		}
		if err != nil {
			_ = os.Remove(target)
			return transferMsg{err: exit.New(exit.Network, "%s", i18n.T("The download of %s was interrupted: %v", name, err))}
		}
		if !protected {
			return transferMsg{note: i18n.T("Saved %s as %s. It is not protected: it was written by an app which does not know chambers.", name, target)}
		}
		return transferMsg{note: i18n.T("Saved %s as %s.", name, target)}
	}
}

// freeName finds a file name that isn't taken: name, name (1), name (2), …
func freeName(name string) string {
	if _, err := os.Stat(name); err != nil {
		return name
	}
	ext := filepath.Ext(name)
	base := strings.TrimSuffix(name, ext)
	for i := 1; ; i++ {
		candidate := base + " (" + strconv.Itoa(i) + ")" + ext
		if _, err := os.Stat(candidate); err != nil {
			return candidate
		}
	}
}

func expandHome(path string) string {
	if path == "~" || strings.HasPrefix(path, "~/") {
		if home, err := os.UserHomeDir(); err == nil {
			return filepath.Join(home, strings.TrimPrefix(path, "~"))
		}
	}
	// A path dropped onto a terminal often comes quoted.
	if len(path) >= 2 && (path[0] == '\'' || path[0] == '"') && path[len(path)-1] == path[0] {
		return path[1 : len(path)-1]
	}
	return path
}

func pathError(err error) error {
	if pe, ok := err.(*os.PathError); ok {
		return pe.Err
	}
	return err
}
