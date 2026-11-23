// Package socket is Tolk's client of a stone's socket: JSON frames over a
// WebSocket at <API root>/socket, the same protocol Rune speaks (FEATURES.md).
// Over it the members of a room work with the entries of its features, and are
// told what changes. A request carries a reference and gets one answer with
// it; everything else that arrives is an event.
package socket

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/ws"
)

// Client is a socket connection to one stone.
type Client struct {
	// MemberID and AccountName are who the stone says is connected.
	MemberID    string
	AccountName string

	conn    *ws.Conn
	events  chan client.SocketFrame
	done    chan struct{}
	once    sync.Once
	mu      sync.Mutex
	pending map[string]chan client.SocketFrame
	counter int
	err     error
}

// URL is the socket's address for a stone's API root.
func URL(apiURL string) string {
	apiURL = strings.TrimRight(apiURL, "/")
	switch {
	case strings.HasPrefix(apiURL, "https://"):
		return "wss://" + strings.TrimPrefix(apiURL, "https://") + "/socket"
	case strings.HasPrefix(apiURL, "http://"):
		return "ws://" + strings.TrimPrefix(apiURL, "http://") + "/socket"
	}
	return apiURL + "/socket"
}

// MaxFrame is the largest frame the client reads. The stone sends frames of
// at most 256 KiB; a page of entries may be several of them in one answer
// of older stones, so there is room to spare.
const MaxFrame = 16 << 20

// Dial connects to a stone's socket and authenticates with a session token.
// A token the stone no longer accepts gives an error with return code 403.
func Dial(ctx context.Context, apiURL, token string) (*Client, error) {
	conn, err := ws.Dial(ctx, URL(apiURL), nil)
	if err != nil {
		var refused *ws.HandshakeError
		if errors.As(err, &refused) && refused.Status != 502 && refused.Status != 503 && refused.Status != 504 {
			return nil, exit.New(exit.Other, "the socket of %s refused the connection (HTTP %d)", apiURL, refused.Status)
		}
		return nil, &exit.Error{Code: exit.Network, Message: fmt.Sprintf("cannot reach the socket of %s: %v", apiURL, rootCause(err))}
	}
	conn.MaxMessage = MaxFrame
	c := &Client{conn: conn, events: make(chan client.SocketFrame, 256), done: make(chan struct{}), pending: map[string]chan client.SocketFrame{}}
	go c.read()
	go c.keepAlive()
	ready, err := c.Request(ctx, client.SocketFrame{Type: client.SocketAuth, Token: token})
	if err != nil {
		c.Close()
		return nil, err
	}
	c.MemberID, c.AccountName = ready.MemberID, ready.AccountName
	return c, nil
}

func rootCause(err error) error {
	for {
		next := errors.Unwrap(err)
		if next == nil {
			return err
		}
		err = next
	}
}

// read takes the frames as they arrive: an answer goes to who asked, the
// rest are events. It alone sends events, so it alone closes their channel,
// when it ends.
func (c *Client) read() {
	defer close(c.events)
	for {
		text, err := c.conn.ReadText()
		if err != nil {
			c.fail(err)
			return
		}
		var frame client.SocketFrame
		if json.Unmarshal([]byte(text), &frame) != nil {
			continue
		}
		c.mu.Lock()
		waiting, ok := c.pending[frame.Ref]
		if ok {
			delete(c.pending, frame.Ref)
		}
		c.mu.Unlock()
		if ok {
			waiting <- frame
			continue
		}
		select {
		case c.events <- frame:
		case <-c.done:
			return
		}
	}
}

// keepAlive pings now and then, so proxies don't drop a quiet connection.
func (c *Client) keepAlive() {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			if c.conn.Ping() != nil {
				return
			}
		case <-c.done:
			return
		}
	}
}

// fail ends the client with the reason the connection is gone. It closes
// the connection, which ends the reader, and the reader closes the events.
func (c *Client) fail(err error) {
	c.once.Do(func() {
		var closed *ws.CloseError
		switch {
		case errors.As(err, &closed) && closed.Code == ws.ClosePolicyViolation:
			// The stone ends the socket when the session ends or expires.
			err = &exit.Error{Code: exit.Auth, ReturnCode: 403, Message: "the session has ended"}
		case errors.As(err, &closed) && closed.Code == ws.CloseNormal:
			err = nil
		case errors.Is(err, net.ErrClosed):
			err = nil
		default:
			err = &exit.Error{Code: exit.Network, Message: fmt.Sprintf("the connection to the stone was lost: %v", err)}
		}
		c.mu.Lock()
		c.err = err
		for ref, waiting := range c.pending {
			close(waiting)
			delete(c.pending, ref)
		}
		c.mu.Unlock()
		close(c.done)
		_ = c.conn.Close(ws.CloseNormal, "")
	})
}

// Events are the frames nobody asked for: entries which were added, changed
// or removed, signals, features which were switched, files which changed,
// members who came online or went. The
// channel is closed when the connection ends; Err says why.
func (c *Client) Events() <-chan client.SocketFrame { return c.events }

// Done is closed when the connection ends.
func (c *Client) Done() <-chan struct{} { return c.done }

// Err is why the connection ended; nil while it is open, or after Close.
func (c *Client) Err() error {
	c.mu.Lock()
	defer c.mu.Unlock()
	return c.err
}

// Close ends the connection.
func (c *Client) Close() {
	c.fail(net.ErrClosed)
}

// Request sends a frame and waits for its answer. An ERROR answer is
// returned as an error with the stone's return code and message.
func (c *Client) Request(ctx context.Context, frame client.SocketFrame) (*client.SocketFrame, error) {
	waiting := make(chan client.SocketFrame, 1)
	c.mu.Lock()
	if c.err != nil || c.closed() {
		err := c.err
		c.mu.Unlock()
		if err == nil {
			err = exit.New(exit.Network, "the stone is not connected")
		}
		return nil, err
	}
	c.counter++
	frame.Ref = strconv.Itoa(c.counter)
	c.pending[frame.Ref] = waiting
	c.mu.Unlock()

	data, err := json.Marshal(frame)
	if err != nil {
		return nil, exit.New(exit.Other, "%v", err)
	}
	if err := c.conn.WriteText(string(data)); err != nil {
		c.fail(err)
		return nil, c.lost()
	}
	select {
	case answer, ok := <-waiting:
		if !ok {
			return nil, c.lost()
		}
		if answer.Type == client.SocketError {
			return nil, &exit.Error{Code: exit.FromReturnCode(answer.ReturnCode), Message: answer.ReturnMessage,
				ReturnCode: answer.ReturnCode, ReturnMessage: answer.ReturnMessage}
		}
		return &answer, nil
	case <-ctx.Done():
		c.mu.Lock()
		delete(c.pending, frame.Ref)
		c.mu.Unlock()
		return nil, exit.New(exit.Network, "the stone didn't answer: %v", ctx.Err())
	}
}

func (c *Client) closed() bool {
	select {
	case <-c.done:
		return true
	default:
		return false
	}
}

func (c *Client) lost() error {
	if err := c.Err(); err != nil {
		return err
	}
	return exit.New(exit.Network, "the connection to the stone closed")
}

// Joined is what the stone answers to joining a room.
type Joined struct {
	// Features are the features of the room with their state.
	Features []client.CircleFeature
	// Present are the members who are online in the room, by their IDs.
	Present []string
}

// Join enters a room: what changes in it arrives from now on, also who
// comes and goes (PRESENT, ABSENT). An unseen connection learns who is
// present without its own member being shown as present: for a command,
// and for a person who doesn't want to be seen. Joining again with another
// choice changes it.
func (c *Client) Join(ctx context.Context, circleID string, unseen bool) (*Joined, error) {
	answer, err := c.Request(ctx, client.SocketFrame{Type: client.SocketJoin, CircleID: circleID, Unseen: unseen})
	if err != nil {
		return nil, err
	}
	return &Joined{Features: answer.Features, Present: answer.Present}, nil
}

// Leave stops a room's changes from arriving.
func (c *Client) Leave(ctx context.Context, circleID string) error {
	_, err := c.Request(ctx, client.SocketFrame{Type: client.SocketLeave, CircleID: circleID})
	return err
}

func (c *Client) one(ctx context.Context, frame client.SocketFrame) (*client.Entry, error) {
	answer, err := c.Request(ctx, frame)
	if err != nil {
		return nil, err
	}
	if len(answer.Entries) == 0 {
		return nil, exit.New(exit.Other, "the stone answered without the entry")
	}
	return &answer.Entries[0], nil
}

// Add adds an entry to a feature of a joined room.
func (c *Client) Add(ctx context.Context, circleID, feature, kind, content, about, name string) (*client.Entry, error) {
	frame := client.SocketFrame{Type: client.SocketAdd, CircleID: circleID, Feature: feature, Kind: kind, Content: &content, About: about}
	if name != "" {
		frame.Name = &name
	}
	return c.one(ctx, frame)
}

// Change replaces the content of an entry.
func (c *Client) Change(ctx context.Context, circleID, feature, entryID, content string) (*client.Entry, error) {
	return c.one(ctx, client.SocketFrame{Type: client.SocketChange, CircleID: circleID, Feature: feature, EntryID: entryID, Content: &content})
}

// Remove removes an entry; the answer has what went, the entries about it
// first.
func (c *Client) Remove(ctx context.Context, circleID, feature, entryID string) ([]client.Entry, error) {
	answer, err := c.Request(ctx, client.SocketFrame{Type: client.SocketRemove, CircleID: circleID, Feature: feature, EntryID: entryID})
	if err != nil {
		return nil, err
	}
	return answer.Entries, nil
}

// Get returns one entry with its content.
func (c *Client) Get(ctx context.Context, circleID, feature, entryID string) (*client.Entry, error) {
	return c.one(ctx, client.SocketFrame{Type: client.SocketGet, CircleID: circleID, Feature: feature, EntryID: entryID})
}

// List returns a page of the entries of a feature, the newest first unless
// asked otherwise.
func (c *Client) List(ctx context.Context, circleID, feature string, query client.EntryQuery) ([]client.Entry, error) {
	answer, err := c.Request(ctx, client.SocketFrame{
		Type: client.SocketList, CircleID: circleID, Feature: feature, Kind: query.Kind, About: query.About, After: query.After, Before: query.Before,
		OldestFirst: query.OldestFirst, Limit: query.Limit, Bare: query.Bare, Name: namePrefix(query.Name),
	})
	if err != nil {
		return nil, err
	}
	return answer.Entries, nil
}

func namePrefix(name string) *string {
	if name == "" {
		return nil
	}
	return &name
}

// All returns all entries of a kind, the oldest first, fetched in pages.
func (c *Client) All(ctx context.Context, circleID, feature string, query client.EntryQuery) ([]client.Entry, error) {
	const page = 200
	query.OldestFirst, query.Limit, query.Before = true, page, 0
	var all []client.Entry
	for {
		found, err := c.List(ctx, circleID, feature, query)
		if err != nil {
			return nil, err
		}
		all = append(all, found...)
		if len(found) < page {
			return all, nil
		}
		query.After = found[len(found)-1].Position
	}
}

// Count counts the entries of a feature, of a kind and after a position if
// given.
func (c *Client) Count(ctx context.Context, circleID, feature, kind string, after int64) (int64, error) {
	answer, err := c.Request(ctx, client.SocketFrame{Type: client.SocketCount, CircleID: circleID, Feature: feature, Kind: kind, After: after})
	if err != nil {
		return 0, err
	}
	return answer.Count, nil
}

// Signal sends a signal to the others in the room; it is neither stored nor
// answered.
func (c *Client) Signal(circleID, feature, signal string) {
	c.SignalTo(circleID, feature, signal, "")
}

// SignalTo sends a signal to the connections of one member which joined
// the room, and to nobody else; the own member reaches the own other
// connections. Without a member it goes to everyone else in the room.
func (c *Client) SignalTo(circleID, feature, signal, memberID string) {
	data, _ := json.Marshal(client.SocketFrame{Type: client.SocketSignal, CircleID: circleID, Feature: feature, Signal: signal, To: memberID})
	_ = c.conn.WriteText(string(data))
}
