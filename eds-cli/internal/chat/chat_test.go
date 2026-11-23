package chat

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/fakestone"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
)

// plain is the sealer of a room: the content goes as it is.
type plain struct{}

func (plain) Protect(_ context.Context, _, _ string, data []byte) ([]byte, error) { return data, nil }
func (plain) Reveal(_ context.Context, _, _ string, data []byte) ([]byte, error)  { return data, nil }

// chamber is the sealer of a chamber with one room key.
type chamber struct{ key e2e.RoomKey }

func (c chamber) Protect(_ context.Context, circleID, what string, data []byte) ([]byte, error) {
	return e2e.EncryptContent(c.key, circleID, what, data, nil)
}

func (c chamber) Reveal(_ context.Context, circleID, what string, data []byte) ([]byte, error) {
	return e2e.DecryptContent(map[int][]byte{c.key.Version: c.key.Key}, circleID, what, data)
}

type world struct {
	t     *testing.T
	ctx   context.Context
	stone *fakestone.Stone
	room  string
}

func newWorld(t *testing.T) *world {
	t.Helper()
	stone := fakestone.New("admin passphrase")
	t.Cleanup(stone.Close)
	stone.OfferAll()
	stone.AddUser("birgit", "birgit passphrase")
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	t.Cleanup(cancel)
	return &world{t: t, ctx: ctx, stone: stone, room: stone.AddRoom("Projects", "admin", "ADMIN", "birgit", "WRITE")}
}

func (w *world) dial(account string, sealer Sealer) *Client {
	w.t.Helper()
	token, err := client.New(w.stone.URL()).Login(w.ctx, account, account+" passphrase")
	if err != nil {
		w.t.Fatal(err)
	}
	c, err := Dial(w.ctx, w.stone.URL(), token, sealer)
	if err != nil {
		w.t.Fatal(err)
	}
	w.t.Cleanup(c.Close)
	return c
}

// next waits for the next event of a kind, and passes over the others; of
// any kind, if none is named.
func next(t *testing.T, c *Client, kind string) client.ChatFrame {
	t.Helper()
	timeout := time.After(10 * time.Second)
	for {
		select {
		case frame, ok := <-c.Events():
			if !ok {
				t.Fatalf("the connection ended while waiting for %s: %v", kind, c.Err())
			}
			if frame.Type == kind || kind == "" {
				return frame
			}
		case <-timeout:
			t.Fatalf("no %s came", kind)
		}
	}
}

func TestChatOnEntries(t *testing.T) {
	w := newWorld(t)
	admin, birgit := w.dial("admin", plain{}), w.dial("birgit", plain{})
	if unread, err := admin.Join(w.ctx, w.room); err != nil || unread != 0 {
		t.Fatalf("join: %d, %v", unread, err)
	}
	if admin.State(w.room) != "on" {
		t.Fatalf("the chat is %s", admin.State(w.room))
	}
	if _, err := birgit.Join(w.ctx, w.room); err != nil {
		t.Fatal(err)
	}

	// A message: its author gets it as the answer, the other as an event.
	sent, err := admin.Send(w.ctx, w.room, "Hei birgit", "", "")
	if err != nil {
		t.Fatal(err)
	}
	if sent.Text != "Hei birgit" || sent.AccountName != "admin" || sent.Position != 1 || sent.Damaged {
		t.Errorf("sent: %+v", sent)
	}
	got := next(t, birgit, client.FrameMessage)
	if got.Message.MessageID != sent.MessageID || got.Message.Text != "Hei birgit" {
		t.Errorf("birgit got %+v", got.Message)
	}
	// The author isn't told of the own message a second time: the next
	// thing which reaches admin is birgit's answer.
	answer, err := birgit.Send(w.ctx, w.room, "Hei admin", "", sent.MessageID)
	if err != nil {
		t.Fatal(err)
	}
	if got := next(t, admin, client.FrameMessage); got.Message.MessageID != answer.MessageID || got.Message.ReplyTo != sent.MessageID {
		t.Errorf("admin got %+v", got.Message)
	}

	// Reactions: one entry per member about a message holds all of theirs.
	if _, err := birgit.React(w.ctx, w.room, sent.MessageID, "👍"); err != nil {
		t.Fatal(err)
	}
	// admin has read the room before, so the reactions are known to it.
	if _, err := admin.History(w.ctx, w.room, "", 50); err != nil {
		t.Fatal(err)
	}
	reacted, err := birgit.React(w.ctx, w.room, sent.MessageID, "❤️")
	if err != nil {
		t.Fatal(err)
	}
	if len(reacted.Message.Reactions) != 2 || reacted.Message.Reactions["👍"][0] != birgit.MemberID {
		t.Errorf("birgit's reactions: %+v", reacted.Message.Reactions)
	}
	// What the stone announced of the first reaction may come first.
	for got := next(t, admin, client.FrameReacted); len(got.Message.Reactions) != 2; got = next(t, admin, client.FrameReacted) {
	}
	if taken, err := birgit.React(w.ctx, w.room, sent.MessageID, "👍"); err != nil || len(taken.Message.Reactions) != 1 {
		t.Errorf("taking a reaction back: %+v, %v", taken, err)
	}

	// Editing and deleting, by the author only.
	edited, err := admin.Edit(w.ctx, w.room, sent.MessageID, "Hei birgit!")
	if err != nil || edited.Message.Text != "Hei birgit!" || edited.Message.Edited == "" {
		t.Fatalf("edit: %+v, %v", edited, err)
	}
	if got := next(t, birgit, client.FrameEdited); got.Message.Text != "Hei birgit!" {
		t.Errorf("birgit sees %+v", got.Message)
	}
	if _, err := birgit.Edit(w.ctx, w.room, sent.MessageID, "not mine"); err == nil {
		t.Error("birgit edited admin's message")
	}

	// The history, in pages, and the receipts with the newest page.
	if err := birgit.MarkRead(w.ctx, w.room, answer.MessageID); err != nil {
		t.Fatal(err)
	}
	if got := next(t, admin, client.FrameReceipt); got.Receipt.MemberID != birgit.MemberID || got.Receipt.ReadPosition != answer.Position {
		t.Errorf("receipt: %+v", got.Receipt)
	}
	messages, receipts, err := admin.Page(w.ctx, w.room, "", 50)
	if err != nil || len(messages) != 2 || messages[0].Text != "Hei birgit!" || messages[1].Text != "Hei admin" {
		t.Fatalf("history: %+v, %v", messages, err)
	}
	if len(messages[0].Reactions["❤️"]) != 1 {
		t.Errorf("the history has the reactions %+v", messages[0].Reactions)
	}
	if len(receipts) != 1 || receipts[0].AccountName != "birgit" {
		t.Errorf("receipts: %+v", receipts)
	}
	if older, err := admin.History(w.ctx, w.room, answer.MessageID, 50); err != nil || len(older) != 1 || older[0].MessageID != sent.MessageID {
		t.Errorf("the page before: %+v, %v", older, err)
	}

	// Typing is a signal: not stored, and not told to who types.
	birgit.Typing(w.room)
	if got := next(t, admin, client.FrameTyping); got.AccountName != "birgit" {
		t.Errorf("typing: %+v", got)
	}

	if _, err := admin.Delete(w.ctx, w.room, sent.MessageID); err != nil {
		t.Fatal(err)
	}
	if got := next(t, birgit, client.FrameDeleted); got.MessageID != sent.MessageID {
		t.Errorf("deleted: %+v", got)
	}
	if texts := w.stone.Messages("Projects"); len(texts) != 1 || texts[0] != "Hei admin" {
		t.Errorf("the stone has %v", texts)
	}

	// Someone who comes back later is told how much is unread.
	later := w.dial("admin", plain{})
	w.stone.Say("Projects", "birgit", "anyone?")
	if unread, err := later.Join(w.ctx, w.room); err != nil || unread != 2 {
		t.Errorf("unread: %d, %v", unread, err)
	}
}

// In a chamber the stone holds what it can't read, and a member without
// the key sees that there is a message which can't be read.
func TestChatInAChamber(t *testing.T) {
	w := newWorld(t)
	key := chamber{key: e2e.RoomKey{Key: bytes.Repeat([]byte{7}, 32), Version: 1}}
	admin, birgit, outsider := w.dial("admin", key), w.dial("birgit", key), w.dial("birgit", chamber{key: e2e.RoomKey{Key: bytes.Repeat([]byte{9}, 32), Version: 1}})
	for _, c := range []*Client{admin, birgit, outsider} {
		if _, err := c.Join(w.ctx, w.room); err != nil {
			t.Fatal(err)
		}
	}
	sent, err := admin.Send(w.ctx, w.room, "secret words", "", "")
	if err != nil || sent.Text != "secret words" {
		t.Fatalf("send: %+v, %v", sent, err)
	}
	if got := next(t, birgit, client.FrameMessage); got.Message.Text != "secret words" || got.Message.Damaged {
		t.Errorf("birgit got %+v", got.Message)
	}
	if got := next(t, outsider, client.FrameMessage); got.Message.Text != "" || !got.Message.Damaged {
		t.Errorf("without the key: %+v", got.Message)
	}
	stored := w.stone.Stored("Projects", "chat", "message")
	if len(stored) != 1 || !e2e.IsProtected(stored[0]) || strings.Contains(string(stored[0]), "secret") {
		t.Errorf("the stone holds %q", stored)
	}
}

// A chat which is closed can be read and takes nothing new; one which is
// switched off shows nothing.
func TestChatClosedAndOff(t *testing.T) {
	w := newWorld(t)
	admin := w.dial("admin", plain{})
	if _, err := admin.Join(w.ctx, w.room); err != nil {
		t.Fatal(err)
	}
	if _, err := admin.Send(w.ctx, w.room, "before", "", ""); err != nil {
		t.Fatal(err)
	}
	w.stone.SwitchFeature("Projects", "chat", "READONLY")
	next(t, admin, client.FrameFeature)
	if admin.State(w.room) != "readonly" {
		t.Errorf("the chat is %s", admin.State(w.room))
	}
	if _, err := admin.Send(w.ctx, w.room, "too late", "", ""); err == nil {
		t.Error("a closed chat took a message")
	}
	if messages, err := admin.History(w.ctx, w.room, "", 50); err != nil || len(messages) != 1 {
		t.Errorf("a closed chat is read: %+v, %v", messages, err)
	}
	w.stone.SwitchFeature("Projects", "chat", "OFF")
	next(t, admin, client.FrameFeature)
	if messages, err := admin.History(w.ctx, w.room, "", 50); err != nil || len(messages) != 0 || admin.State(w.room) != "off" {
		t.Errorf("switched off: %+v, %v, %s", messages, err, admin.State(w.room))
	}
	if Unavailable("off") == "" || Unavailable("on") != "" {
		t.Error("what is said of a chat which isn't on")
	}
}

// Send tells of frames beside the listener, which closes the events when
// the connection ends: telling while it ends, and after, must not meet a
// closed channel.
func TestTellingWhileTheConnectionEnds(t *testing.T) {
	w := newWorld(t)
	for range 50 {
		c := w.dial("admin", plain{})
		stop := make(chan struct{})
		var tellers sync.WaitGroup
		for range 4 {
			tellers.Go(func() {
				for {
					select {
					case <-stop:
						return
					default:
						c.tell(client.ChatFrame{Type: client.FrameChanged, CircleID: w.room})
					}
				}
			})
		}
		drained := make(chan struct{})
		go func() {
			defer close(drained)
			for range c.Events() {
			}
		}()
		time.Sleep(time.Millisecond)
		c.Close()
		select {
		case <-drained:
		case <-time.After(10 * time.Second):
			t.Fatal("the events weren't closed")
		}
		// The tellers go on for a moment after the end.
		time.Sleep(time.Millisecond)
		close(stop)
		tellers.Wait()
	}
}

// The stone replaces a member's reactions to a message with a new entry,
// and tells of the end of the old one. What it tells may arrive after the
// answer which made the new one known, and must not undo it.
func TestReactionsAnnouncedLate(t *testing.T) {
	w := newWorld(t)
	c := w.dial("admin", plain{})
	reaction := func(id string, position int64, reactions ...string) *client.Entry {
		content, _ := json.Marshal(reactionBody{V: 1, Reactions: reactions})
		return &client.Entry{EntryID: id, CircleID: w.room, Kind: "reaction", Position: position, About: "message", MemberID: "birgit",
			Content: base64.StdEncoding.EncodeToString(content)}
	}
	gave := func() string {
		c.mu.Lock()
		defer c.mu.Unlock()
		return strings.Join(c.room(w.room).reactions["message"]["birgit"].reactions, " ")
	}
	first, second, third := reaction("first", 1, "👍"), reaction("second", 2, "👍", "❤️"), reaction("third", 3)

	c.noteReaction(w.ctx, first, false)
	c.noteReaction(w.ctx, second, false)
	// The end of the first entry, and the first entry itself, arrive late.
	c.noteReaction(w.ctx, first, true)
	if gave() != "👍 ❤️" {
		t.Errorf("after the end of an earlier entry: %q", gave())
	}
	c.noteReaction(w.ctx, first, false)
	if gave() != "👍 ❤️" {
		t.Errorf("after an earlier entry: %q", gave())
	}
	// An entry without reactions is the newest too.
	c.noteReaction(w.ctx, third, false)
	c.noteReaction(w.ctx, second, false)
	c.mu.Lock()
	shown := c.room(w.room).reactionsOf("message")
	c.mu.Unlock()
	if gave() != "" || shown != nil {
		t.Errorf("after all were taken back: %q, %+v", gave(), shown)
	}
	// The end of the entry which is known ends the reactions.
	c.noteReaction(w.ctx, second, false)
	c.noteReaction(w.ctx, third, true)
	c.noteReaction(w.ctx, second, false)
	if gave() != "👍 ❤️" {
		t.Errorf("after the end of the known entry: %q", gave())
	}
}

// Who is online in a room: a client which is shown is, one which isn't
// still learns of the others, and follows who comes and goes.
func TestOnline(t *testing.T) {
	w := newWorld(t)
	admin, birgit, script := w.dial("admin", plain{}), w.dial("birgit", plain{}), w.dial("birgit", plain{})
	admin.Show()
	birgit.Show()
	if _, err := admin.Join(w.ctx, w.room); err != nil {
		t.Fatal(err)
	}
	// A command joins unseen: nobody is told, and it knows who is there.
	if _, err := script.Join(w.ctx, w.room); err != nil {
		t.Fatal(err)
	}
	if !script.Online(w.room, admin.MemberID) || script.Online(w.room, script.MemberID) || admin.Online(w.room, birgit.MemberID) {
		t.Error("an unseen client: it is shown, or doesn't know who is")
	}
	if !admin.Online(w.room, admin.MemberID) || !admin.OnlineAnywhere(admin.MemberID) || admin.Online("another room", admin.MemberID) {
		t.Error("a shown client doesn't know itself as online in the room")
	}
	if _, err := birgit.Join(w.ctx, w.room); err != nil {
		t.Fatal(err)
	}
	if got := next(t, admin, client.FramePresent); got.MemberID != birgit.MemberID || got.AccountName != "birgit" || got.CircleID != w.room {
		t.Errorf("came: %+v", got)
	}
	if !admin.Online(w.room, birgit.MemberID) || !birgit.Online(w.room, admin.MemberID) {
		t.Error("the two don't know each other as online")
	}
	next(t, script, client.FramePresent)
	if !script.Online(w.room, birgit.MemberID) {
		t.Error("the unseen client wasn't told who came")
	}
	// The features of the room change: the room is joined again as before,
	// which tells nobody anything of who is online.
	w.stone.SwitchFeature("Projects", "plans", client.FeatureOn)
	next(t, admin, client.FrameFeature)
	next(t, script, client.FrameFeature)
	if present := strings.Join(w.stone.Present("Projects"), " "); present != "admin birgit" {
		t.Errorf("present after the features changed: %q", present)
	}
	birgit.Close()
	if got := next(t, admin, client.FrameAbsent); got.MemberID != birgit.MemberID {
		t.Errorf("went: %+v", got)
	}
	if admin.Online(w.room, birgit.MemberID) || admin.OnlineAnywhere(birgit.MemberID) {
		t.Error("birgit is still online after her last shown connection went")
	}
}

// Someone starts the call of a room, or calls directly: the chat tells, in
// a room and, with the room key, in a chamber. Who hasn't the key, and who
// isn't called, is told nothing.
func TestCalls(t *testing.T) {
	const call = "11111111-1111-4111-8111-111111111111"
	key := chamber{key: e2e.RoomKey{Key: bytes.Repeat([]byte{7}, 32), Version: 1}}
	other := chamber{key: e2e.RoomKey{Key: bytes.Repeat([]byte{9}, 32), Version: 1}}
	for name, sealers := range map[string][2]Sealer{"room": {plain{}, plain{}}, "chamber": {key, other}} {
		w := newWorld(t)
		w.stone.SwitchFeature("Projects", "calls", client.FeatureOn)
		admin, birgit, outsider := w.dial("admin", sealers[0]), w.dial("birgit", sealers[0]), w.dial("birgit", sealers[1])
		for _, c := range []*Client{admin, birgit, outsider} {
			if _, err := c.Join(w.ctx, w.room); err != nil {
				t.Fatal(err)
			}
		}
		send := func(message features.CallMessage, to string) {
			t.Helper()
			signal, err := features.CallSignal(w.ctx, sealers[0], w.room, message)
			if err != nil {
				t.Fatal(err)
			}
			if name == "chamber" {
				if raw, _ := base64.StdEncoding.DecodeString(signal); !e2e.IsProtected(raw) {
					t.Errorf("the stone can read the signal of a call: %q", raw)
				}
			}
			admin.Socket().SignalTo(w.room, "calls", signal, to)
		}
		// What sets a call up beside the ring is passed over.
		send(features.CallMessage{T: "join", Call: features.RoomCall, Peer: call, Media: "audio"}, "")
		send(features.CallMessage{T: "ring", Call: features.RoomCall, Media: "audio"}, "")
		if got := next(t, birgit, client.FrameCall); got.Direct || got.MemberID != admin.MemberID || got.AccountName != "admin" || got.CircleID != w.room {
			t.Errorf("%s: the call of the room: %+v", name, got)
		}
		send(features.CallMessage{T: "ring", Call: call, Media: "video"}, birgit.MemberID)
		if got := next(t, birgit, client.FrameCall); !got.Direct || got.AccountName != "admin" {
			t.Errorf("%s: a direct call: %+v", name, got)
		}
		// The connection without the key got both signals, and can read
		// them only in a room which is no chamber.
		birgit.Typing(w.room)
		told := 0
		for frame := next(t, outsider, ""); frame.Type != client.FrameTyping; frame = next(t, outsider, "") {
			if frame.Type == client.FrameCall {
				told++
			}
		}
		if want := map[string]int{"room": 2, "chamber": 0}[name]; told != want {
			t.Errorf("%s: without the key %d calls were told, want %d", name, told, want)
		}
	}
}
