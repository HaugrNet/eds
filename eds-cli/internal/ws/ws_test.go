package ws

import (
	"bytes"
	"context"
	"encoding/binary"
	"errors"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

// The example of RFC 6455, section 1.3.
func TestAcceptKey(t *testing.T) {
	if got := acceptKey("dGhlIHNhbXBsZSBub25jZQ=="); got != "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=" {
		t.Errorf("accept key: %s", got)
	}
}

// pair makes a client and a server connection over a pipe, without the
// handshake.
func pair(t *testing.T) (client, server *Conn, rawServer net.Conn) {
	t.Helper()
	a, b := net.Pipe()
	t.Cleanup(func() { a.Close(); b.Close() })
	client = &Conn{conn: a, reader: newReader(a), client: true, MaxMessage: MaxMessage}
	server = &Conn{conn: b, reader: newReader(b), MaxMessage: MaxMessage}
	return client, server, b
}

// The examples of RFC 6455, section 5.7.
func TestFramesOfTheRFC(t *testing.T) {
	client, _, raw := pair(t)
	// A single-frame unmasked text message, "Hello".
	go raw.Write([]byte{0x81, 0x05, 0x48, 0x65, 0x6c, 0x6c, 0x6f})
	if got, err := client.ReadText(); err != nil || got != "Hello" {
		t.Fatalf("unmasked: %q, %v", got, err)
	}
	// A fragmented unmasked text message, "Hel" and "lo", with a ping
	// between the fragments, which is answered.
	go raw.Write([]byte{0x01, 0x03, 0x48, 0x65, 0x6c, 0x89, 0x02, 'h', 'i', 0x80, 0x02, 0x6c, 0x6f})
	done := make(chan string, 1)
	go func() {
		got, _ := client.ReadText()
		done <- got
	}()
	pong := make([]byte, 8) // header, mask, two bytes
	if _, err := readFull(raw, pong); err != nil {
		t.Fatal(err)
	}
	if pong[0] != 0x8A || pong[1] != 0x82 || string(unmask(pong[2:6], pong[6:])) != "hi" {
		t.Errorf("pong: % x", pong)
	}
	if got := <-done; got != "Hello" {
		t.Errorf("fragmented: %q", got)
	}

	// A client's frame is masked: "Hello" with the key of the RFC's example.
	c2, s2, _ := pair(t)
	go c2.conn.Write([]byte{0x81, 0x85, 0x37, 0xfa, 0x21, 0x3d, 0x7f, 0x9f, 0x4d, 0x51, 0x58})
	if got, err := s2.ReadText(); err != nil || got != "Hello" {
		t.Errorf("masked: %q, %v", got, err)
	}
}

func TestRoundTripOfAllLengths(t *testing.T) {
	client, server, _ := pair(t)
	// The three encodings of the length: up to 125, 16 bits, 64 bits.
	for _, length := range []int{0, 1, 125, 126, 127, 65535, 65536, 200000} {
		message := strings.Repeat("ä", length/2) + strings.Repeat("x", length%2)
		go func() {
			if err := client.WriteText(message); err != nil {
				t.Error(err)
			}
		}()
		got, err := server.ReadText()
		if err != nil || got != message {
			t.Fatalf("to the server, %d bytes: %v", length, err)
		}
		go func() {
			if err := server.WriteText(message); err != nil {
				t.Error(err)
			}
		}()
		got, err = client.ReadText()
		if err != nil || got != message {
			t.Fatalf("to the client, %d bytes: %v", length, err)
		}
	}
}

func TestClientMasksWithFreshKeys(t *testing.T) {
	client, _, raw := pair(t)
	read := func() []byte {
		go client.WriteText("same text")
		frame := make([]byte, 2+4+9)
		if _, err := readFull(raw, frame); err != nil {
			t.Fatal(err)
		}
		if frame[0] != 0x81 || frame[1] != 0x80|9 {
			t.Fatalf("header: % x", frame[:2])
		}
		if string(unmask(frame[2:6], frame[6:])) != "same text" {
			t.Fatalf("payload: % x", frame)
		}
		return frame[2:6]
	}
	if bytes.Equal(read(), read()) {
		t.Error("the same masking key twice")
	}
}

func TestClose(t *testing.T) {
	client, server, _ := pair(t)
	go server.Close(ClosePolicyViolation, "the session has ended")
	_, err := client.ReadText()
	var closed *CloseError
	if !errors.As(err, &closed) || closed.Code != ClosePolicyViolation || closed.Reason != "the session has ended" {
		t.Fatalf("close: %v", err)
	}
	if err := client.WriteText("after the close"); err == nil {
		t.Error("writing after the close worked")
	}
}

func TestProtocolViolations(t *testing.T) {
	big := binary.BigEndian.AppendUint64([]byte{0x81, 127}, uint64(MaxMessage)+1)
	for name, frame := range map[string][]byte{
		"a masked frame from the server":   {0x81, 0x81, 1, 2, 3, 4, 0},
		"reserved bits":                    {0xC1, 0x01, 'x'},
		"a fragmented control frame":       {0x09, 0x00},
		"a control frame over 125 bytes":   append([]byte{0x89, 126, 0, 126}, make([]byte, 126)...),
		"a continuation without a message": {0x80, 0x01, 'x'},
		"a binary message":                 {0x82, 0x01, 'x'},
		"text that isn't UTF-8":            {0x81, 0x02, 0xC3, 0x28},
		"an unknown opcode":                {0x83, 0x00},
		"a message that is too large":      big,
		"a close frame of one byte":        {0x88, 0x01, 0x03},
	} {
		client, _, raw := pair(t)
		go func() {
			raw.Write(frame)
			// What the client answers (a close frame) is read and dropped.
			buffer := make([]byte, 256)
			for {
				if _, err := raw.Read(buffer); err != nil {
					return
				}
			}
		}()
		if got, err := client.ReadText(); err == nil {
			t.Errorf("%s was accepted: %q", name, got)
		}
		raw.Close()
	}
}

func TestDialAndAccept(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("X-Test") != "yes" {
			http.Error(w, "no", http.StatusForbidden)
			return
		}
		conn, err := Accept(w, r)
		if err != nil {
			return
		}
		for {
			message, err := conn.ReadText()
			if err != nil {
				return
			}
			if err := conn.WriteText("echo: " + message); err != nil {
				return
			}
		}
	}))
	defer server.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	conn, err := Dial(ctx, strings.Replace(server.URL, "http://", "ws://", 1)+"/chat", http.Header{"X-Test": {"yes"}})
	if err != nil {
		t.Fatal(err)
	}
	for _, message := range []string{"hei", strings.Repeat("rune ", 30000)} {
		if err := conn.WriteText(message); err != nil {
			t.Fatal(err)
		}
		if got, err := conn.ReadText(); err != nil || got != "echo: "+message {
			t.Fatalf("echo of %d bytes: %v", len(message), err)
		}
	}
	if err := conn.Ping(); err != nil {
		t.Error(err)
	}
	if err := conn.WriteText("after the ping"); err != nil {
		t.Fatal(err)
	}
	if got, _ := conn.ReadText(); got != "echo: after the ping" {
		t.Errorf("after the ping: %q", got)
	}
	conn.Close(CloseNormal, "")

	// Refused by the server: the HTTP status is reported.
	_, err = Dial(ctx, server.URL, nil)
	var refused *HandshakeError
	if !errors.As(err, &refused) || refused.Status != http.StatusForbidden {
		t.Errorf("refused handshake: %v", err)
	}
	// A server that isn't one.
	plain := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.Write([]byte("hello")) }))
	defer plain.Close()
	if _, err := Dial(ctx, plain.URL, nil); err == nil {
		t.Error("a plain HTTP answer was taken as a WebSocket")
	}
}

func unmask(key, payload []byte) []byte {
	out := make([]byte, len(payload))
	for i := range payload {
		out[i] = payload[i] ^ key[i%4]
	}
	return out
}
