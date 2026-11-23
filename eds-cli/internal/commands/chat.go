package commands

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"sync"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/chat"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
	"github.com/HaugrNet/eds/eds-cli/internal/tui"
)

// chats holds one chat connection per stone for a command.
type chats struct {
	mu      sync.Mutex
	clients map[*service.Stone]*chat.Client
}

func newChats() *chats { return &chats{clients: map[*service.Stone]*chat.Client{}} }

// of returns the chat connection of a stone, opening it when first asked.
func (cs *chats) of(ctx context.Context, stone *service.Stone) (*chat.Client, error) {
	cs.mu.Lock()
	defer cs.mu.Unlock()
	if c, ok := cs.clients[stone]; ok {
		return c, nil
	}
	var c *chat.Client
	dialCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()
	// A session the stone no longer accepts is renewed, once.
	err := stone.Do(dialCtx, func(token string) error {
		var err error
		c, err = chat.Dial(dialCtx, stone.URL, token, stone)
		return err
	})
	if err != nil {
		return nil, err
	}
	cs.clients[stone] = c
	return c, nil
}

func (cs *chats) close() {
	cs.mu.Lock()
	defer cs.mu.Unlock()
	for _, c := range cs.clients {
		c.Close()
	}
}

// share posts a file of a room in its chat, as Rune's "share in the chat".
func (cs *chats) share(ctx context.Context, t target, dataID, text string) error {
	c, err := cs.of(ctx, t.stone)
	if err != nil {
		return err
	}
	requestCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()
	if _, err := c.Join(requestCtx, t.room.ID); err != nil {
		return err
	}
	_, err = c.Send(requestCtx, t.room.ID, text, dataID, "")
	return err
}

// ended puts the end of a chat connection in words. A stone keeps one
// session per account, so a login or logout with the same account
// elsewhere (Rune, another Tolk without the agent) ends this one.
func ended(err error) error {
	var e *exit.Error
	if errors.As(err, &e) && e.Code == exit.Auth {
		return &exit.Error{Code: exit.Auth, ReturnCode: e.ReturnCode,
			Message: i18n.T("The stone ended the session. A stone keeps one session per account: a login or logout with the same account elsewhere ends this one.")}
	}
	if errors.As(err, &e) && e.Code == exit.Network {
		return &exit.Error{Code: exit.Network, Message: i18n.T("The connection to the chat was lost.")}
	}
	return err
}

// chatEvent is one line of 'tolk chat --plain': stable English keys, times
// as ISO-8601 UTC, whatever the language.
type chatEvent struct {
	Event     string              `json:"event"` // message, sent, edited, reaction, deleted, left
	Stone     string              `json:"stone"`
	Room      string              `json:"room"`
	RoomID    string              `json:"roomId"`
	ID        string              `json:"id,omitempty"`
	From      string              `json:"from,omitempty"`
	FromID    string              `json:"fromId,omitempty"`
	Time      string              `json:"time,omitempty"`
	Edited    string              `json:"edited,omitempty"`
	Text      *string             `json:"text,omitempty"`
	ReplyTo   string              `json:"replyTo,omitempty"`
	FileID    string              `json:"fileId,omitempty"`
	Reactions map[string][]string `json:"reactions,omitempty"`
	History   bool                `json:"history,omitempty"`
	// Damaged: the message can't be read; it is damaged, or was written
	// under a key of the chamber which this device hasn't.
	Damaged bool `json:"damaged,omitempty"`
}

// chatReadable says why the chat of a room can't be used at all: the room
// has none, it is switched off, or the room is a chamber whose key this
// device hasn't. What only keeps from writing is told, and the chat is
// read.
func (c *command) chatReadable(ctx context.Context, t target, conn *chat.Client) error {
	switch state := conn.State(t.room.ID); state {
	case "none", "off":
		return exit.New(exit.NotFound, "%s", chat.Unavailable(state))
	case "readonly":
		c.note("%s: %s", t.label, chat.Unavailable(state))
	}
	room, err := t.stone.RoomState(ctx, t.room.ID)
	if err != nil {
		return err
	}
	if refusal := room.Refusal(); refusal != "" {
		if len(room.Keys) == 0 {
			return exit.New(exit.Auth, "%s", refusal)
		}
		c.note("%s: %s", t.label, refusal)
	}
	return nil
}

func messageEvent(kind string, t target, m *client.ChatMessage) chatEvent {
	text := m.Text
	return chatEvent{Event: kind, Stone: t.stone.Name, Room: t.room.Name, RoomID: t.room.ID, ID: m.MessageID, From: m.AccountName, FromID: m.MemberID,
		Time: iso(m.Time), Edited: iso(m.Edited), Text: &text, ReplyTo: m.ReplyTo, FileID: m.DataID, Reactions: m.Reactions, Damaged: m.Damaged}
}

// chat is the scriptable chat: lines of standard input are sent as
// messages to the rooms, and what happens in the rooms is written to
// standard output as JSON lines.
func (c *command) chat(args []string) error {
	fs := c.flags("chat")
	rooms := roomFlag(fs)
	plain := fs.Bool("plain", false, "")
	listen := fs.Bool("listen", false, "")
	text := fs.String("message", "", "")
	fs.StringVar(text, "m", "", "")
	history := fs.Int("history", 0, "")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if !*plain {
		// Without --plain: the chat on the screen, Tolk's interactive mode.
		stone := *stoneName
		if names := roomArgs(rooms, pos); len(names) > 0 {
			return usage("The chat on the screen opens a stone, not a room: 'tolk chat [--stone <name>]'. For scripts, use 'tolk chat --plain --room <room>'.")
		}
		return c.interactiveMode(stone)
	}
	names := roomArgs(rooms, pos)
	if len(names) == 0 {
		return usage("Use 'tolk chat --plain --room <room> … [-m <text>] [--listen] [--history <n>]'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	plan, err := c.resolveAll(ctx, svc, names)
	if err != nil {
		return err
	}
	connections := newChats()
	defer connections.close()

	// Lines are written whole, also when several stones write at once.
	var outMu sync.Mutex
	encoder := json.NewEncoder(c.env.Out)
	encoder.SetEscapeHTML(false)
	emit := func(event chatEvent) {
		outMu.Lock()
		defer outMu.Unlock()
		_ = encoder.Encode(event)
	}

	// Join every room; one that fails is reported, the others go on.
	code := exit.OK
	failed := func(label string, err error) {
		e := c.friendly(err)
		fmt.Fprintf(c.env.Err, "tolk: %s: %s\n", label, e.Message)
		if code == exit.OK {
			code = e.Code
		} else if code != e.Code {
			code = exit.Other
		}
	}
	var joined []target
	byRoom := map[*chat.Client]map[string]target{}
	for _, p := range plan {
		if p.failed != nil {
			if code == exit.OK {
				code = p.failed.Error.Code
			} else if code != p.failed.Error.Code {
				code = exit.Other
			}
			continue
		}
		t := p.target
		conn, err := connections.of(ctx, t.stone)
		if err == nil {
			requestCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
			_, err = conn.Join(requestCtx, t.room.ID)
			if err == nil {
				err = c.chatReadable(requestCtx, t, conn)
			}
			if err == nil && *history > 0 {
				var messages []client.ChatMessage
				if messages, err = conn.History(requestCtx, t.room.ID, "", *history); err == nil {
					for i := range messages {
						event := messageEvent("message", t, &messages[i])
						event.History = true
						emit(event)
					}
				}
			}
			cancel()
		}
		if err != nil {
			failed(t.label, err)
			continue
		}
		joined = append(joined, t)
		if byRoom[conn] == nil {
			byRoom[conn] = map[string]target{}
		}
		byRoom[conn][t.room.ID] = t
	}
	if len(joined) == 0 {
		return reported{code}
	}

	// What happens in the rooms, as events.
	var pumps sync.WaitGroup
	lost := make(chan error, len(byRoom))
	for conn, rooms := range byRoom {
		pumps.Add(1)
		go func() {
			defer pumps.Done()
			for frame := range conn.Events() {
				roomID := frame.CircleID
				if frame.Message != nil && frame.Message.CircleID != "" {
					roomID = frame.Message.CircleID
				}
				t, ok := rooms[roomID]
				if !ok {
					continue
				}
				switch frame.Type {
				case client.FrameMessage:
					if frame.Message != nil {
						emit(messageEvent("message", t, frame.Message))
					}
				case client.FrameEdited:
					if frame.Message != nil {
						emit(messageEvent("edited", t, frame.Message))
					}
				case client.FrameReacted:
					if frame.Message != nil {
						emit(messageEvent("reaction", t, frame.Message))
					}
				case client.FrameDeleted:
					emit(chatEvent{Event: "deleted", Stone: t.stone.Name, Room: t.room.Name, RoomID: t.room.ID, ID: frame.MessageID})
				case client.FrameLeft:
					emit(chatEvent{Event: "left", Stone: t.stone.Name, Room: t.room.Name, RoomID: t.room.ID})
				}
			}
			lost <- conn.Err()
		}()
	}

	// What is said: one text, or the lines of standard input. A room where
	// the account can only read is refused once, in Rune's words.
	refused := map[string]bool{}
	say := func(line string) {
		for _, t := range joined {
			if !t.room.CanWrite() {
				if !refused[t.label] {
					refused[t.label] = true
					failed(t.label, service.CheckWrite(t.room))
				}
				continue
			}
			conn, _ := connections.of(ctx, t.stone)
			requestCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
			message, err := conn.Send(requestCtx, t.room.ID, line, "", "")
			cancel()
			if err != nil {
				failed(t.label, err)
				continue
			}
			emit(messageEvent("sent", t, message))
		}
	}
	if *text != "" {
		say(*text)
	} else {
		lines := make(chan string)
		go func() {
			defer close(lines)
			scanner := bufio.NewScanner(c.env.In)
			scanner.Buffer(make([]byte, 64<<10), 100<<10)
			for scanner.Scan() {
				lines <- scanner.Text()
			}
		}()
	reading:
		for {
			select {
			case line, ok := <-lines:
				if !ok {
					break reading
				}
				if strings.TrimSpace(line) != "" {
					say(line)
				}
			case <-ctx.Done():
				break reading
			case err := <-lost:
				if err != nil {
					failed(i18n.T("chat"), ended(err))
				}
				if code == exit.OK {
					return nil
				}
				return reported{code}
			}
		}
	}
	if *listen {
		// Until interrupted, or the connection ends.
		select {
		case <-ctx.Done():
		case err := <-lost:
			if err != nil {
				failed(i18n.T("chat"), ended(err))
			}
		}
	}
	connections.close()
	pumps.Wait()
	if code != exit.OK {
		return reported{code}
	}
	return nil
}

// interactiveMode opens Tolk's interactive mode on a stone: Rune in the
// terminal. It needs a terminal; a script gets a clear message instead of a
// program that waits for keys.
func (c *command) interactiveMode(stoneName string) error {
	if !c.env.StdinTTY || !c.env.StdoutTTY {
		return usage("Tolk's interactive mode needs a terminal. For scripts, see 'tolk help': every command works without one, and 'tolk chat --plain' is the chat.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	if _, err := svc.Info(stoneName); err != nil {
		return err
	}
	ctx, cancel := c.context()
	defer cancel()
	file, err := config.Load()
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	if c.opts.lang != "" {
		file.UI.Language = c.opts.lang
	}
	err = tui.Run(tui.Options{
		Context: ctx, Service: svc, Stone: stoneName, UI: file.UI, Version: Version,
		SaveUI: func(ui config.UI) error {
			// Only the settings are written; the container stays as it is.
			current, err := config.Load()
			if err != nil {
				return err
			}
			current.UI = ui
			return config.Save(current)
		},
	})
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	return nil
}
