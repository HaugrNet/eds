// Package chat is the chat of a room as Tolk works with it. The chat is a
// feature (FEATURES.md): the stone keeps entries of the kinds message, reaction
// and receipt, and knows nothing of a chat; what a message holds is the
// clients', and in a chamber the stone can't read it. This package turns
// the entries and their announcements on the socket into the messages,
// receipts and events which the commands and the interactive mode show, as
// Rune's src/chat.ts does.
package chat

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"sync"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/socket"
)

const feature = "chat"

// Sealer is what makes the content of a room ready for the stone, and
// readable again: nothing in a room, encryption in a chamber. A stone of
// package service is one.
type Sealer interface {
	Protect(ctx context.Context, circleID, what string, plain []byte) ([]byte, error)
	Reveal(ctx context.Context, circleID, what string, stored []byte) ([]byte, error)
}

type messageBody struct {
	V     int    `json:"v"`
	Text  string `json:"text"`
	Reply string `json:"reply,omitempty"`
	File  string `json:"file,omitempty"`
}

type reactionBody struct {
	V         int      `json:"v"`
	Reactions []string `json:"reactions"`
}

type receiptBody struct {
	V        int   `json:"v"`
	Received int64 `json:"received"`
	Read     int64 `json:"read"`
}

// given is what a member gave a message, and the entry which says so. The
// stone replaces that entry with every change, and what it announces of the
// old one may arrive after the new one is known: the entry tells them apart.
type given struct {
	entryID   string
	position  int64
	reactions []string
}

// room is what the client knows of the chat of one room.
type room struct {
	features []client.CircleFeature
	messages map[string]*client.ChatMessage
	// The reactions of each member to each message: message, member, reactions.
	reactions       map[string]map[string]given
	reactionsLoaded bool
	received, read  int64
	newest          int64
	// The members who are online in the room, as the stone tells.
	present map[string]bool
}

// Client is the chat on one stone.
type Client struct {
	// MemberID and AccountName are who the stone says is connected.
	MemberID    string
	AccountName string

	socket *socket.Client
	sealer Sealer
	events chan client.ChatFrame
	// Guards the end of events: Send tells of frames too, beside listen,
	// which closes the channel.
	telling sync.RWMutex
	ended   bool
	mu      sync.Mutex
	rooms   map[string]*room
	// Own messages on their way: what the stone announces of them is held
	// back, as the answer to the request tells of them.
	sending int
	held    []client.ChatFrame
	// Own messages whose announcement is still to come: it is passed over.
	own map[string]bool
	// shown: the rooms are joined so that the account is shown as online.
	shown bool
	// Held while a room is joined, and while a member who came or went is
	// noted: what the stone tells after its answer to joining is noted
	// after the answer.
	joining sync.Mutex
}

// Dial connects to a stone's socket for the chat, and authenticates with a
// session token. A token the stone no longer accepts gives an error with
// return code 403.
func Dial(ctx context.Context, apiURL, token string, sealer Sealer) (*Client, error) {
	conn, err := socket.Dial(ctx, apiURL, token)
	if err != nil {
		return nil, err
	}
	c := &Client{MemberID: conn.MemberID, AccountName: conn.AccountName, socket: conn, sealer: sealer,
		events: make(chan client.ChatFrame, 256), rooms: map[string]*room{}, own: map[string]bool{}}
	go c.listen()
	return c, nil
}

// Show makes the account shown as online in the rooms joined from now on,
// as the interactive mode is. Without it a connection joins unseen, so a
// command doesn't make its person flicker.
func (c *Client) Show() {
	c.mu.Lock()
	c.shown = true
	c.mu.Unlock()
}

// Online reports whether a member is online in a joined room.
func (c *Client) Online(circleID, memberID string) bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	r := c.rooms[circleID]
	return r != nil && r.present[memberID]
}

// OnlineAnywhere reports whether a member is online in one of the joined
// rooms.
func (c *Client) OnlineAnywhere(memberID string) bool {
	c.mu.Lock()
	defer c.mu.Unlock()
	for _, r := range c.rooms {
		if r.present[memberID] {
			return true
		}
	}
	return false
}

// join enters a room on the socket, shown or unseen as the client is, and
// notes its features and who is online in it.
func (c *Client) join(ctx context.Context, circleID string) ([]client.CircleFeature, error) {
	c.joining.Lock()
	defer c.joining.Unlock()
	c.mu.Lock()
	shown := c.shown
	c.mu.Unlock()
	joined, err := c.socket.Join(ctx, circleID, !shown)
	if err != nil {
		return nil, err
	}
	present := map[string]bool{}
	for _, memberID := range joined.Present {
		present[memberID] = true
	}
	if shown {
		present[c.MemberID] = true
	}
	c.mu.Lock()
	r := c.room(circleID)
	r.features, r.present = joined.Features, present
	c.mu.Unlock()
	return joined.Features, nil
}

// Socket is the connection the chat runs on, for the other features of a room.
func (c *Client) Socket() *socket.Client { return c.socket }

// Events are what happens in the chats of the joined rooms: messages of
// others, edits, reactions, deletions, receipts, typing. The channel is
// closed when the connection ends; Err says why.
func (c *Client) Events() <-chan client.ChatFrame { return c.events }

// Err is why the connection ended; nil while it is open, or after Close.
func (c *Client) Err() error { return c.socket.Err() }

// Close ends the connection.
func (c *Client) Close() { c.socket.Close() }

func (c *Client) room(circleID string) *room {
	r := c.rooms[circleID]
	if r == nil {
		r = &room{messages: map[string]*client.ChatMessage{}, reactions: map[string]map[string]given{}, present: map[string]bool{}}
		c.rooms[circleID] = r
	}
	return r
}

// State is how the chat stands in a joined room: "on", "readonly", "off",
// or "none" if the room hasn't got it.
func (c *Client) State(circleID string) string {
	c.mu.Lock()
	defer c.mu.Unlock()
	return state(c.room(circleID).features)
}

func state(features []client.CircleFeature) string {
	for _, t := range features {
		if t.Name == feature {
			switch t.State {
			case client.FeatureOn:
				return "on"
			case client.FeatureReadonly:
				return "readonly"
			default:
				return "off"
			}
		}
	}
	return "none"
}

func readable(features []client.CircleFeature) bool {
	s := state(features)
	return s == "on" || s == "readonly"
}

// Unavailable is why nothing can be written to the chat of a room in a
// state, in Rune's words; empty when it is on.
func Unavailable(state string) string {
	switch state {
	case "readonly":
		return i18n.Rune("The chat of this room is closed: it can be read, but nothing new is written.")
	case "off":
		return i18n.Rune("The chat is switched off in this room.")
	case "none":
		return i18n.Rune("This room has no chat. An administrator of the room can switch it on.")
	}
	return ""
}

func (c *Client) encode(ctx context.Context, circleID, kind string, body any) (string, error) {
	plain, err := json.Marshal(body)
	if err != nil {
		return "", exit.New(exit.Other, "%v", err)
	}
	sealed, err := c.sealer.Protect(ctx, circleID, feature+"|"+kind, plain)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(sealed), nil
}

// decode reads the content of an entry into a body; false if it can't be
// read (damaged, of another convention, or no key).
func (c *Client) decode(ctx context.Context, entry *client.Entry, body any) bool {
	stored, err := base64.StdEncoding.DecodeString(entry.Content)
	if err != nil || entry.Content == "" {
		return false
	}
	plain, err := c.sealer.Reveal(ctx, entry.CircleID, feature+"|"+entry.Kind, stored)
	if err != nil {
		return false
	}
	return json.Unmarshal(plain, body) == nil
}

func (c *Client) add(ctx context.Context, circleID, kind string, body any, about string) (*client.Entry, error) {
	content, err := c.encode(ctx, circleID, kind, body)
	if err != nil {
		return nil, err
	}
	return c.socket.Add(ctx, circleID, feature, kind, content, about, "")
}

// Join enters a room: its messages arrive from now on, and it can be sent
// to. It returns the number of unread messages.
func (c *Client) Join(ctx context.Context, circleID string) (int, error) {
	has, err := c.join(ctx, circleID)
	if err != nil {
		return 0, err
	}
	c.mu.Lock()
	r := c.room(circleID)
	c.mu.Unlock()
	if !readable(has) {
		return 0, nil
	}
	// The own marker: one receipt per member is kept.
	receipts, err := c.socket.List(ctx, circleID, feature, client.EntryQuery{Kind: "receipt", Limit: 200})
	if err != nil {
		return 0, err
	}
	var read, received int64
	for i := range receipts {
		if receipts[i].MemberID == c.MemberID {
			var body receiptBody
			if c.decode(ctx, &receipts[i], &body) {
				read, received = body.Read, body.Received
			}
		}
	}
	c.mu.Lock()
	r.read, r.received = read, received
	c.mu.Unlock()
	unread, err := c.socket.Count(ctx, circleID, feature, "message", read)
	if err != nil {
		return 0, err
	}
	return int(unread), nil
}

// Leave stops a room's messages from arriving.
func (c *Client) Leave(ctx context.Context, circleID string) error {
	c.mu.Lock()
	delete(c.rooms, circleID)
	c.mu.Unlock()
	return c.socket.Leave(ctx, circleID)
}

// Send posts a message to a joined room; dataID shares a file of the room,
// replyTo answers a message.
func (c *Client) Send(ctx context.Context, circleID, text, dataID, replyTo string) (*client.ChatMessage, error) {
	c.mu.Lock()
	c.sending++
	c.mu.Unlock()
	entry, err := c.add(ctx, circleID, "message", messageBody{V: 1, Text: text, Reply: replyTo, File: dataID}, "")
	var message *client.ChatMessage
	if err == nil {
		message = c.message(ctx, entry)
	}
	// What was announced meanwhile is told now, without the own message.
	c.mu.Lock()
	c.sending--
	var release []client.ChatFrame
	if c.sending == 0 {
		release, c.held = c.held, nil
	}
	announced := false
	for _, frame := range c.held {
		announced = announced || (message != nil && frame.Message != nil && frame.Message.MessageID == message.MessageID)
	}
	if message != nil && !announced {
		// The stone's word of the message is still on its way.
		for _, frame := range release {
			announced = announced || (frame.Message != nil && frame.Message.MessageID == message.MessageID)
		}
		if !announced {
			c.own[message.MessageID] = true
		}
	}
	c.mu.Unlock()
	for _, frame := range release {
		if message == nil || frame.Message == nil || frame.Message.MessageID != message.MessageID {
			c.tell(frame)
		}
	}
	if err != nil {
		return nil, err
	}
	return message, nil
}

func (c *Client) loadReactions(ctx context.Context, circleID string) error {
	c.mu.Lock()
	r := c.room(circleID)
	loaded := r.reactionsLoaded
	r.reactionsLoaded = true
	c.mu.Unlock()
	if loaded {
		return nil
	}
	// The reactions of a room are few and small: they are fetched once, and
	// kept up by what the stone announces.
	entries, err := c.socket.All(ctx, circleID, feature, client.EntryQuery{Kind: "reaction"})
	if err != nil {
		c.mu.Lock()
		r.reactionsLoaded = false
		c.mu.Unlock()
		return err
	}
	for i := range entries {
		c.noteReaction(ctx, &entries[i], false)
	}
	return nil
}

func (c *Client) noteReaction(ctx context.Context, entry *client.Entry, removed bool) {
	if entry.About == "" || entry.MemberID == "" {
		return
	}
	var body reactionBody
	if !removed {
		c.decode(ctx, entry, &body)
	}
	c.mu.Lock()
	defer c.mu.Unlock()
	r := c.room(entry.CircleID)
	byMember := r.reactions[entry.About]
	if byMember == nil {
		byMember = map[string]given{}
		r.reactions[entry.About] = byMember
	}
	known, ok := byMember[entry.MemberID]
	switch {
	case removed:
		// Only the entry which is known takes the reactions with it: an
		// earlier one was replaced, and its end says nothing of them.
		if ok && known.entryID == entry.EntryID {
			delete(byMember, entry.MemberID)
		}
	case !ok || entry.Position >= known.position:
		// Kept also when it has no reactions, so an earlier entry which
		// arrives late doesn't bring its own back.
		byMember[entry.MemberID] = given{entryID: entry.EntryID, position: entry.Position, reactions: body.Reactions}
	}
}

// reactionsOf is who gave which reaction to a message; the lock is held.
func (r *room) reactionsOf(messageID string) map[string][]string {
	out := map[string][]string{}
	for memberID, gave := range r.reactions[messageID] {
		for _, reaction := range gave.reactions {
			out[reaction] = append(out[reaction], memberID)
		}
	}
	if len(out) == 0 {
		return nil
	}
	return out
}

// message is an entry as the chat shows it, and remembers it.
func (c *Client) message(ctx context.Context, entry *client.Entry) *client.ChatMessage {
	var body messageBody
	ok := c.decode(ctx, entry, &body)
	message := &client.ChatMessage{
		MessageID: entry.EntryID, CircleID: entry.CircleID, MemberID: entry.MemberID, AccountName: entry.AccountName,
		Time: entry.Added, Edited: entry.Changed, Position: entry.Position,
		Text: body.Text, ReplyTo: body.Reply, DataID: body.File, Damaged: !ok,
	}
	c.mu.Lock()
	r := c.room(entry.CircleID)
	message.Reactions = r.reactionsOf(entry.EntryID)
	r.messages[entry.EntryID] = message
	r.newest = max(r.newest, entry.Position)
	c.mu.Unlock()
	copied := *message
	return &copied
}

// Page is a page of a room's history, oldest first; before is a message ID
// to page back from. With the newest page come the receipts: up to where
// each member has received and read.
func (c *Client) Page(ctx context.Context, circleID, before string, limit int) ([]client.ChatMessage, []client.ChatReceipt, error) {
	c.mu.Lock()
	r := c.room(circleID)
	features := r.features
	var beforePosition int64
	if known := r.messages[before]; known != nil {
		beforePosition = known.Position
	}
	c.mu.Unlock()
	if !readable(features) {
		return nil, nil, nil
	}
	if err := c.loadReactions(ctx, circleID); err != nil {
		return nil, nil, err
	}
	if before != "" && beforePosition == 0 {
		// A message this client hasn't seen: the stone knows its place.
		entry, err := c.socket.Get(ctx, circleID, feature, before)
		if err != nil {
			return nil, nil, err
		}
		beforePosition = entry.Position
	}
	entries, err := c.socket.List(ctx, circleID, feature, client.EntryQuery{Kind: "message", Before: beforePosition, Limit: limit})
	if err != nil {
		return nil, nil, err
	}
	messages := make([]client.ChatMessage, 0, len(entries))
	for i := len(entries) - 1; i >= 0; i-- {
		messages = append(messages, *c.message(ctx, &entries[i]))
	}
	var receipts []client.ChatReceipt
	if before == "" {
		found, err := c.socket.List(ctx, circleID, feature, client.EntryQuery{Kind: "receipt", Limit: 200})
		if err != nil {
			return nil, nil, err
		}
		for i := range found {
			receipts = append(receipts, c.receipt(ctx, &found[i]))
		}
	}
	return messages, receipts, nil
}

// History returns the newest messages of a room, oldest first; before is a
// message ID to page back from.
func (c *Client) History(ctx context.Context, circleID, before string, limit int) ([]client.ChatMessage, error) {
	messages, _, err := c.Page(ctx, circleID, before, limit)
	return messages, err
}

func (c *Client) receipt(ctx context.Context, entry *client.Entry) client.ChatReceipt {
	var body receiptBody
	c.decode(ctx, entry, &body)
	return client.ChatReceipt{MemberID: entry.MemberID, AccountName: entry.AccountName, ReadPosition: body.Read, ReceivedPosition: body.Received}
}

// Edit replaces the text of an own message; the answer has the message as
// it is now.
func (c *Client) Edit(ctx context.Context, circleID, messageID, text string) (*client.ChatFrame, error) {
	body := messageBody{V: 1, Text: text}
	c.mu.Lock()
	if known := c.room(circleID).messages[messageID]; known != nil {
		body.Reply, body.File = known.ReplyTo, known.DataID
	}
	c.mu.Unlock()
	content, err := c.encode(ctx, circleID, "message", body)
	if err != nil {
		return nil, err
	}
	entry, err := c.socket.Change(ctx, circleID, feature, messageID, content)
	if err != nil {
		return nil, err
	}
	return &client.ChatFrame{Type: client.FrameEdited, CircleID: circleID, MessageID: messageID, Message: c.message(ctx, entry)}, nil
}

// Delete removes an own message, for everyone.
func (c *Client) Delete(ctx context.Context, circleID, messageID string) (*client.ChatFrame, error) {
	if _, err := c.socket.Remove(ctx, circleID, feature, messageID); err != nil {
		return nil, err
	}
	c.mu.Lock()
	r := c.room(circleID)
	delete(r.messages, messageID)
	delete(r.reactions, messageID)
	c.mu.Unlock()
	return &client.ChatFrame{Type: client.FrameDeleted, CircleID: circleID, MessageID: messageID}, nil
}

// React gives a reaction to a message, or takes it back if it was given;
// the answer has the message as it is now.
func (c *Client) React(ctx context.Context, circleID, messageID, reaction string) (*client.ChatFrame, error) {
	if err := c.loadReactions(ctx, circleID); err != nil {
		return nil, err
	}
	c.mu.Lock()
	mine := c.room(circleID).reactions[messageID][c.MemberID].reactions
	c.mu.Unlock()
	next := []string{}
	given := false
	for _, r := range mine {
		if r == reaction {
			given = true
		} else {
			next = append(next, r)
		}
	}
	if !given {
		next = append(next, reaction)
	}
	// One entry per member about each message holds all their reactions to it.
	entry, err := c.add(ctx, circleID, "reaction", reactionBody{V: 1, Reactions: next}, messageID)
	if err != nil {
		return nil, err
	}
	c.noteReaction(ctx, entry, false)
	c.mu.Lock()
	r := c.room(circleID)
	var message *client.ChatMessage
	if known := r.messages[messageID]; known != nil {
		known.Reactions = r.reactionsOf(messageID)
		copied := *known
		message = &copied
	}
	c.mu.Unlock()
	return &client.ChatFrame{Type: client.FrameReacted, CircleID: circleID, MessageID: messageID, Message: message}, nil
}

func (c *Client) mark(ctx context.Context, circleID string, read, received int64) error {
	c.mu.Lock()
	r := c.room(circleID)
	if read <= r.read && received <= r.received {
		c.mu.Unlock()
		return nil
	}
	r.read, r.received = max(r.read, read), max(r.received, received, read)
	body := receiptBody{V: 1, Received: r.received, Read: r.read}
	writable := state(r.features) == "on"
	c.mu.Unlock()
	if !writable {
		return nil
	}
	_, err := c.add(ctx, circleID, "receipt", body, "")
	return err
}

// MarkRead says that the room was read up to a message.
func (c *Client) MarkRead(ctx context.Context, circleID, messageID string) error {
	c.mu.Lock()
	known := c.room(circleID).messages[messageID]
	c.mu.Unlock()
	if known == nil {
		return nil
	}
	return c.mark(ctx, circleID, known.Position, known.Position)
}

// Received says that this client has the room's messages, without having
// read them: the second checkmark of their authors.
func (c *Client) Received(ctx context.Context, circleID string) error {
	c.mu.Lock()
	newest := c.room(circleID).newest
	c.mu.Unlock()
	if newest == 0 {
		entries, err := c.socket.List(ctx, circleID, feature, client.EntryQuery{Kind: "message", Limit: 1, Bare: true})
		if err != nil || len(entries) == 0 {
			return err
		}
		newest = entries[0].Position
	}
	return c.mark(ctx, circleID, 0, newest)
}

// Typing tells the room that the account is writing; nothing is answered.
func (c *Client) Typing(circleID string) {
	c.socket.Signal(circleID, feature, "typing")
}

func (c *Client) tell(frame client.ChatFrame) {
	c.telling.RLock()
	defer c.telling.RUnlock()
	if c.ended {
		return
	}
	select {
	case c.events <- frame:
	case <-c.socket.Done():
	}
}

// listen turns what the socket tells into what happens in the chat.
func (c *Client) listen() {
	defer func() {
		// The socket is done by now, so nobody waits in tell.
		c.telling.Lock()
		c.ended = true
		close(c.events)
		c.telling.Unlock()
	}()
	for frame := range c.socket.Events() {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		c.take(ctx, frame)
		cancel()
	}
}

// called takes a signal of the feature "calls": Tolk makes no calls, and
// tells when one begins. A ring is sent once to the room when its call
// starts, and to one member for a direct call; in a chamber it is
// encrypted with the room key.
func (c *Client) called(ctx context.Context, frame client.SocketFrame) {
	if frame.CircleID == "" || frame.MemberID == "" || frame.MemberID == c.MemberID {
		return
	}
	message := features.ReadCallSignal(ctx, c.sealer, frame.CircleID, frame.Signal)
	if message == nil || message.T != "ring" {
		return
	}
	direct := message.Call != features.RoomCall
	if direct && frame.To != c.MemberID {
		return
	}
	c.tell(client.ChatFrame{Type: client.FrameCall, CircleID: frame.CircleID, MemberID: frame.MemberID, AccountName: frame.AccountName, Direct: direct})
}

func (c *Client) take(ctx context.Context, frame client.SocketFrame) {
	switch frame.Type {
	case client.SocketFiles:
		c.tell(client.ChatFrame{Type: client.FrameChanged, CircleID: frame.CircleID})
		return
	case client.SocketLeft:
		c.mu.Lock()
		delete(c.rooms, frame.CircleID)
		c.mu.Unlock()
		c.tell(client.ChatFrame{Type: client.FrameLeft, CircleID: frame.CircleID, ReturnCode: frame.ReturnCode, ReturnMessage: frame.ReturnMessage})
		return
	case client.SocketFeature:
		// The features of the room changed: what is known of its chat may be stale.
		_, _ = c.join(ctx, frame.CircleID)
		c.tell(client.ChatFrame{Type: client.FrameFeature, CircleID: frame.CircleID})
		return
	case client.SocketSignal:
		if frame.Feature == feature && frame.Signal == "typing" && frame.MemberID != "" {
			c.tell(client.ChatFrame{Type: client.FrameTyping, CircleID: frame.CircleID, MemberID: frame.MemberID, AccountName: frame.AccountName})
		}
		if frame.Feature == "calls" {
			c.called(ctx, frame)
		}
		return
	case client.SocketPresent, client.SocketAbsent:
		// A member's first connection came to the room, or the last one went.
		c.joining.Lock()
		c.mu.Lock()
		r := c.rooms[frame.CircleID]
		if r != nil && frame.Type == client.SocketPresent {
			r.present[frame.MemberID] = true
		} else if r != nil {
			delete(r.present, frame.MemberID)
		}
		c.mu.Unlock()
		c.joining.Unlock()
		if r != nil && frame.MemberID != "" {
			c.tell(client.ChatFrame{Type: frame.Type, CircleID: frame.CircleID, MemberID: frame.MemberID, AccountName: frame.AccountName})
		}
		return
	}
	if frame.Feature != feature || frame.Entry == nil || frame.CircleID == "" {
		return
	}
	entry := *frame.Entry
	entry.CircleID = frame.CircleID
	removed := frame.Type == client.SocketRemoved
	// A long content is left out of what is announced, and fetched.
	if !removed && entry.Content == "" && entry.EntryID != "" {
		if whole, err := c.socket.Get(ctx, frame.CircleID, feature, entry.EntryID); err == nil {
			entry = *whole
			entry.CircleID = frame.CircleID
		}
	}
	switch entry.Kind {
	case "message":
		if removed {
			c.mu.Lock()
			r := c.room(frame.CircleID)
			_, known := r.messages[entry.EntryID]
			delete(r.messages, entry.EntryID)
			delete(r.reactions, entry.EntryID)
			c.mu.Unlock()
			// What this client deleted itself was told by the answer.
			if known {
				c.tell(client.ChatFrame{Type: client.FrameDeleted, CircleID: frame.CircleID, MessageID: entry.EntryID})
			}
			return
		}
		c.mu.Lock()
		before := c.room(frame.CircleID).messages[entry.EntryID]
		var earlier client.ChatMessage
		if before != nil {
			earlier = *before
		}
		c.mu.Unlock()
		message := c.message(ctx, &entry)
		if frame.Type == client.SocketAdded {
			out := client.ChatFrame{Type: client.FrameMessage, CircleID: frame.CircleID, Message: message}
			c.mu.Lock()
			if c.own[message.MessageID] {
				// The answer to the request told of it already.
				delete(c.own, message.MessageID)
				c.mu.Unlock()
				return
			}
			if c.sending > 0 && message.MemberID == c.MemberID {
				// Perhaps the own message on its way: the answer decides.
				c.held = append(c.held, out)
				c.mu.Unlock()
				return
			}
			c.mu.Unlock()
			c.tell(out)
		} else if before == nil || earlier.Text != message.Text || earlier.Edited != message.Edited {
			// An edit this client made itself was told by the answer.
			c.tell(client.ChatFrame{Type: client.FrameEdited, CircleID: frame.CircleID, Message: message})
		}
	case "reaction":
		c.mu.Lock()
		loaded := c.room(frame.CircleID).reactionsLoaded
		c.mu.Unlock()
		if loaded {
			c.noteReaction(ctx, &entry, removed)
		} else if c.loadReactions(ctx, frame.CircleID) != nil {
			return // They are fetched whole; this one is among them.
		}
		c.mu.Lock()
		r := c.room(frame.CircleID)
		known := r.messages[entry.About]
		c.mu.Unlock()
		if known == nil {
			// A message this client hasn't seen yet: the stone has it.
			whole, err := c.socket.Get(ctx, frame.CircleID, feature, entry.About)
			if err != nil {
				return
			}
			whole.CircleID = frame.CircleID
			c.message(ctx, whole)
		}
		c.mu.Lock()
		var message *client.ChatMessage
		if known := r.messages[entry.About]; known != nil {
			known.Reactions = r.reactionsOf(entry.About)
			copied := *known
			message = &copied
		}
		c.mu.Unlock()
		if message != nil {
			c.tell(client.ChatFrame{Type: client.FrameReacted, CircleID: frame.CircleID, Message: message})
		}
	case "receipt":
		if !removed {
			receipt := c.receipt(ctx, &entry)
			c.tell(client.ChatFrame{Type: client.FrameReceipt, CircleID: frame.CircleID, Receipt: &receipt})
		}
	}
}
