package socket

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/ws"
)

// flood is a stone which answers the first frame with READY and then sends
// events for as long as it can.
func flood(t *testing.T) *httptest.Server {
	t.Helper()
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		conn, err := ws.Accept(w, r)
		if err != nil {
			return
		}
		defer func() { _ = conn.Close(ws.CloseNormal, "") }()
		text, err := conn.ReadText()
		if err != nil {
			return
		}
		var auth client.SocketFrame
		if json.Unmarshal([]byte(text), &auth) != nil {
			return
		}
		ready, _ := json.Marshal(client.SocketFrame{Type: client.SocketReady, Ref: auth.Ref})
		if conn.WriteText(string(ready)) != nil {
			return
		}
		event, _ := json.Marshal(client.SocketFrame{Type: client.SocketSignal})
		for conn.WriteText(string(event)) == nil {
		}
	}))
	t.Cleanup(server.Close)
	return server
}

// Closing while events arrive: the reader has one in hand then, and must not
// meet a closed channel when it passes it on.
func TestCloseWhileEventsArrive(t *testing.T) {
	server := flood(t)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	for range 100 {
		c, err := Dial(ctx, server.URL, "token")
		if err != nil {
			t.Fatal(err)
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
		if err := c.Err(); err != nil {
			t.Fatalf("closed, but with the error %v", err)
		}
	}
}

// A connection the stone ends closes the events too, and says why.
func TestEventsCloseWhenTheStoneLeaves(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		conn, err := ws.Accept(w, r)
		if err != nil {
			return
		}
		text, _ := conn.ReadText()
		var auth client.SocketFrame
		_ = json.Unmarshal([]byte(text), &auth)
		ready, _ := json.Marshal(client.SocketFrame{Type: client.SocketReady, Ref: auth.Ref})
		_ = conn.WriteText(string(ready))
		_ = conn.Close(ws.ClosePolicyViolation, "")
	}))
	defer server.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	c, err := Dial(ctx, server.URL, "token")
	if err != nil {
		t.Fatal(err)
	}
	select {
	case _, open := <-c.Events():
		if open {
			t.Fatal("an event, where the end was expected")
		}
	case <-time.After(10 * time.Second):
		t.Fatal("the events weren't closed")
	}
	if c.Err() == nil {
		t.Fatal("the session ended, and Err doesn't say so")
	}
}
