// Package ws is a WebSocket implementation (RFC 6455) on Go's standard
// library, written for Tolk so the chat needs no third-party library: the
// opening handshake, masked frames, fragmented messages, ping and pong, and
// the closing handshake. It has no extensions (no compression) and no
// subprotocols, which the chat doesn't use.
//
// Dial is the client. Accept is the server side of the handshake, which
// Tolk's tests and its fake stone use.
package ws

import (
	"bufio"
	"context"
	"crypto/rand"
	"crypto/sha1"
	"crypto/tls"
	"encoding/base64"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"
	"unicode/utf8"
)

// The GUID of RFC 6455, section 1.3.
const guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

// Opcodes (RFC 6455, section 5.2).
const (
	opContinuation = 0x0
	opText         = 0x1
	opBinary       = 0x2
	opClose        = 0x8
	opPing         = 0x9
	opPong         = 0xA
)

// Close codes (RFC 6455, section 7.4.1).
const (
	CloseNormal          = 1000
	CloseGoingAway       = 1001
	CloseProtocolError   = 1002
	CloseNoStatus        = 1005
	CloseAbnormal        = 1006
	CloseInvalidPayload  = 1007
	ClosePolicyViolation = 1008
	CloseTooBig          = 1009
)

// MaxMessage is the largest message a connection reads, unless changed.
const MaxMessage = 1 << 20

// CloseError is returned by ReadText when the other side closed the
// connection.
type CloseError struct {
	Code   int
	Reason string
}

func (e *CloseError) Error() string {
	if e.Reason != "" {
		return fmt.Sprintf("the connection was closed (%d: %s)", e.Code, e.Reason)
	}
	return fmt.Sprintf("the connection was closed (%d)", e.Code)
}

// Conn is a WebSocket connection.
type Conn struct {
	conn   net.Conn
	reader *bufio.Reader
	client bool // a client masks what it sends, and expects unmasked frames
	// MaxMessage is the largest message accepted; larger ones close the
	// connection with 1009.
	MaxMessage int

	writeMu sync.Mutex
	closed  bool
}

// acceptKey computes Sec-WebSocket-Accept for a Sec-WebSocket-Key.
func acceptKey(key string) string {
	sum := sha1.Sum([]byte(key + guid))
	return base64.StdEncoding.EncodeToString(sum[:])
}

// Dial opens a WebSocket connection to a ws:// or wss:// URL (http:// and
// https:// are taken as those). The header is sent with the handshake. A
// proxy from the environment (HTTPS_PROXY, HTTP_PROXY, NO_PROXY) is used
// with CONNECT.
func Dial(ctx context.Context, rawURL string, header http.Header) (*Conn, error) {
	u, err := url.Parse(rawURL)
	if err != nil {
		return nil, err
	}
	secure := false
	switch u.Scheme {
	case "ws", "http":
		u.Scheme = "http"
	case "wss", "https":
		u.Scheme, secure = "https", true
	default:
		return nil, fmt.Errorf("%q is not a WebSocket URL", rawURL)
	}
	address := u.Host
	if u.Port() == "" {
		if secure {
			address = net.JoinHostPort(u.Hostname(), "443")
		} else {
			address = net.JoinHostPort(u.Hostname(), "80")
		}
	}
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
	if err != nil {
		return nil, err
	}

	conn, err := dial(ctx, request, address)
	if err != nil {
		return nil, err
	}
	if secure {
		tlsConn := tls.Client(conn, &tls.Config{ServerName: u.Hostname(), NextProtos: []string{"http/1.1"}, MinVersion: tls.VersionTLS12})
		if err := tlsConn.HandshakeContext(ctx); err != nil {
			conn.Close()
			return nil, err
		}
		conn = tlsConn
	}
	// The handshake ends with the context; afterwards the connection lives
	// on, and deadlines are set by its user.
	if deadline, ok := ctx.Deadline(); ok {
		_ = conn.SetDeadline(deadline)
	}
	stop := context.AfterFunc(ctx, func() { conn.Close() })

	nonce := make([]byte, 16)
	if _, err := rand.Read(nonce); err != nil {
		conn.Close()
		return nil, err
	}
	key := base64.StdEncoding.EncodeToString(nonce)
	for name, values := range header {
		request.Header[name] = values
	}
	request.Header.Set("Upgrade", "websocket")
	request.Header.Set("Connection", "Upgrade")
	request.Header.Set("Sec-WebSocket-Key", key)
	request.Header.Set("Sec-WebSocket-Version", "13")
	if err := request.Write(conn); err != nil {
		conn.Close()
		return nil, err
	}
	reader := bufio.NewReader(conn)
	response, err := http.ReadResponse(reader, request)
	if err != nil {
		conn.Close()
		return nil, err
	}
	if response.StatusCode != http.StatusSwitchingProtocols {
		response.Body.Close()
		conn.Close()
		return nil, &HandshakeError{Status: response.StatusCode}
	}
	if !strings.EqualFold(response.Header.Get("Upgrade"), "websocket") ||
		!headerHas(response.Header, "Connection", "upgrade") ||
		response.Header.Get("Sec-WebSocket-Accept") != acceptKey(key) {
		conn.Close()
		return nil, errors.New("the server's WebSocket handshake is not valid")
	}
	if response.Header.Get("Sec-WebSocket-Extensions") != "" {
		conn.Close()
		return nil, errors.New("the server chose a WebSocket extension that wasn't offered")
	}
	if !stop() {
		conn.Close()
		return nil, ctx.Err()
	}
	_ = conn.SetDeadline(time.Time{})
	return &Conn{conn: conn, reader: reader, client: true, MaxMessage: MaxMessage}, nil
}

// HandshakeError is returned when the server answers the handshake with an
// HTTP status other than 101.
type HandshakeError struct{ Status int }

func (e *HandshakeError) Error() string {
	return fmt.Sprintf("the server refused the WebSocket connection (HTTP %d)", e.Status)
}

// dial connects to the server, directly or through the environment's proxy.
func dial(ctx context.Context, request *http.Request, address string) (net.Conn, error) {
	dialer := &net.Dialer{Timeout: 30 * time.Second}
	proxy, err := http.ProxyFromEnvironment(request)
	if err != nil || proxy == nil {
		return dialer.DialContext(ctx, "tcp", address)
	}
	proxyAddress := proxy.Host
	if proxy.Port() == "" {
		port := "80"
		if proxy.Scheme == "https" {
			port = "443"
		}
		proxyAddress = net.JoinHostPort(proxy.Hostname(), port)
	}
	conn, err := dialer.DialContext(ctx, "tcp", proxyAddress)
	if err != nil {
		return nil, err
	}
	if proxy.Scheme == "https" {
		tlsConn := tls.Client(conn, &tls.Config{ServerName: proxy.Hostname(), MinVersion: tls.VersionTLS12})
		if err := tlsConn.HandshakeContext(ctx); err != nil {
			conn.Close()
			return nil, err
		}
		conn = tlsConn
	}
	connect := &http.Request{Method: http.MethodConnect, URL: &url.URL{Opaque: address}, Host: address, Header: http.Header{}}
	if proxy.User != nil {
		password, _ := proxy.User.Password()
		connect.Header.Set("Proxy-Authorization", "Basic "+base64.StdEncoding.EncodeToString([]byte(proxy.User.Username()+":"+password)))
	}
	if err := connect.Write(conn); err != nil {
		conn.Close()
		return nil, err
	}
	// Read byte by byte up to the end of the header, so nothing of what
	// follows the proxy's answer is taken.
	response, err := http.ReadResponse(bufio.NewReaderSize(&oneByte{conn}, 1), connect)
	if err != nil {
		conn.Close()
		return nil, err
	}
	if response.StatusCode != http.StatusOK {
		conn.Close()
		return nil, fmt.Errorf("the proxy %s refused the connection (HTTP %d)", proxy.Host, response.StatusCode)
	}
	return conn, nil
}

// oneByte reads one byte at a time, so a buffered reader on top of it
// doesn't read ahead.
type oneByte struct{ r io.Reader }

func (o *oneByte) Read(p []byte) (int, error) {
	if len(p) == 0 {
		return 0, nil
	}
	return o.r.Read(p[:1])
}

func headerHas(header http.Header, name, token string) bool {
	for _, value := range header.Values(name) {
		for _, part := range strings.Split(value, ",") {
			if strings.EqualFold(strings.TrimSpace(part), token) {
				return true
			}
		}
	}
	return false
}

// Accept answers a client's handshake and takes over the connection: the
// server side, for tests.
func Accept(w http.ResponseWriter, r *http.Request) (*Conn, error) {
	key := r.Header.Get("Sec-WebSocket-Key")
	if r.Method != http.MethodGet || !strings.EqualFold(r.Header.Get("Upgrade"), "websocket") ||
		!headerHas(r.Header, "Connection", "upgrade") || r.Header.Get("Sec-WebSocket-Version") != "13" || key == "" {
		http.Error(w, "not a WebSocket handshake", http.StatusBadRequest)
		return nil, errors.New("not a WebSocket handshake")
	}
	hijacker, ok := w.(http.Hijacker)
	if !ok {
		http.Error(w, "the connection can't be taken over", http.StatusInternalServerError)
		return nil, errors.New("the connection can't be taken over")
	}
	conn, buffered, err := hijacker.Hijack()
	if err != nil {
		return nil, err
	}
	answer := "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: " + acceptKey(key) + "\r\n\r\n"
	if _, err := conn.Write([]byte(answer)); err != nil {
		conn.Close()
		return nil, err
	}
	return &Conn{conn: conn, reader: buffered.Reader, MaxMessage: MaxMessage}, nil
}

// frame is one frame as read.
type frame struct {
	fin     bool
	opcode  byte
	payload []byte
}

// readFrame reads one frame, and checks what RFC 6455 requires of it.
func (c *Conn) readFrame(limit int) (*frame, error) {
	var head [2]byte
	if _, err := io.ReadFull(c.reader, head[:]); err != nil {
		return nil, err
	}
	f := &frame{fin: head[0]&0x80 != 0, opcode: head[0] & 0x0F}
	if head[0]&0x70 != 0 {
		return nil, c.fail(CloseProtocolError, "reserved bits are set, but no extension was agreed")
	}
	masked := head[1]&0x80 != 0
	// A server must not mask, a client must (section 5.1).
	if masked == c.client {
		return nil, c.fail(CloseProtocolError, "wrong masking of a frame")
	}
	length := uint64(head[1] & 0x7F)
	control := f.opcode >= 0x8
	if control && (length > 125 || !f.fin) {
		return nil, c.fail(CloseProtocolError, "a control frame is too long or fragmented")
	}
	switch length {
	case 126:
		var ext [2]byte
		if _, err := io.ReadFull(c.reader, ext[:]); err != nil {
			return nil, err
		}
		length = uint64(binary.BigEndian.Uint16(ext[:]))
	case 127:
		var ext [8]byte
		if _, err := io.ReadFull(c.reader, ext[:]); err != nil {
			return nil, err
		}
		length = binary.BigEndian.Uint64(ext[:])
		if length>>63 != 0 {
			return nil, c.fail(CloseProtocolError, "invalid frame length")
		}
	}
	if length > uint64(limit) {
		return nil, c.fail(CloseTooBig, "the message is too large")
	}
	var mask [4]byte
	if masked {
		if _, err := io.ReadFull(c.reader, mask[:]); err != nil {
			return nil, err
		}
	}
	f.payload = make([]byte, length)
	if _, err := io.ReadFull(c.reader, f.payload); err != nil {
		return nil, err
	}
	if masked {
		for i := range f.payload {
			f.payload[i] ^= mask[i%4]
		}
	}
	return f, nil
}

// writeFrame writes one frame, masked if this is the client.
func (c *Conn) writeFrame(opcode byte, payload []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	if c.closed && opcode != opClose {
		return net.ErrClosed
	}
	head := make([]byte, 0, 14+len(payload))
	head = append(head, 0x80|opcode)
	maskBit := byte(0)
	if c.client {
		maskBit = 0x80
	}
	switch {
	case len(payload) <= 125:
		head = append(head, maskBit|byte(len(payload)))
	case len(payload) <= 0xFFFF:
		head = append(head, maskBit|126)
		head = binary.BigEndian.AppendUint16(head, uint16(len(payload)))
	default:
		head = append(head, maskBit|127)
		head = binary.BigEndian.AppendUint64(head, uint64(len(payload)))
	}
	if c.client {
		var mask [4]byte
		if _, err := rand.Read(mask[:]); err != nil {
			return err
		}
		head = append(head, mask[:]...)
		start := len(head)
		head = append(head, payload...)
		for i := range payload {
			head[start+i] ^= mask[i%4]
		}
	} else {
		head = append(head, payload...)
	}
	_, err := c.conn.Write(head)
	return err
}

// fail closes the connection for a protocol violation and returns the
// error.
func (c *Conn) fail(code int, reason string) error {
	_ = c.writeClose(code, reason)
	c.conn.Close()
	return fmt.Errorf("WebSocket protocol error: %s", reason)
}

func (c *Conn) writeClose(code int, reason string) error {
	payload := binary.BigEndian.AppendUint16(nil, uint16(code))
	if len(reason) > 123 {
		reason = reason[:123]
	}
	payload = append(payload, reason...)
	err := c.writeFrame(opClose, payload)
	c.writeMu.Lock()
	c.closed = true
	c.writeMu.Unlock()
	return err
}

// ReadText reads the next text message, putting a fragmented one together.
// Pings are answered and pongs passed over on the way. When the other side
// closes, the close is answered and a *CloseError returned.
func (c *Conn) ReadText() (string, error) {
	var message []byte
	started := false
	for {
		f, err := c.readFrame(c.MaxMessage - len(message))
		if err != nil {
			return "", err
		}
		switch f.opcode {
		case opPing:
			if err := c.writeFrame(opPong, f.payload); err != nil {
				return "", err
			}
		case opPong:
		case opClose:
			closeErr := &CloseError{Code: CloseNoStatus}
			if len(f.payload) == 1 {
				return "", c.fail(CloseProtocolError, "invalid close frame")
			}
			if len(f.payload) >= 2 {
				closeErr.Code = int(binary.BigEndian.Uint16(f.payload))
				closeErr.Reason = string(f.payload[2:])
				if !utf8.ValidString(closeErr.Reason) {
					return "", c.fail(CloseInvalidPayload, "the close reason is not UTF-8")
				}
			}
			code := closeErr.Code
			if code == CloseNoStatus {
				code = CloseNormal
			}
			_ = c.writeClose(code, "")
			c.conn.Close()
			return "", closeErr
		case opText, opBinary:
			if started {
				return "", c.fail(CloseProtocolError, "a new message began inside a fragmented one")
			}
			if f.opcode == opBinary {
				return "", c.fail(1003, "binary messages are not accepted")
			}
			started = true
			message = f.payload
			if f.fin {
				return c.text(message)
			}
		case opContinuation:
			if !started {
				return "", c.fail(CloseProtocolError, "a continuation without a message")
			}
			message = append(message, f.payload...)
			if f.fin {
				return c.text(message)
			}
		default:
			return "", c.fail(CloseProtocolError, "unknown opcode")
		}
	}
}

func (c *Conn) text(message []byte) (string, error) {
	if !utf8.Valid(message) {
		return "", c.fail(CloseInvalidPayload, "a text message is not UTF-8")
	}
	return string(message), nil
}

// WriteText sends a text message, in one frame.
func (c *Conn) WriteText(message string) error {
	return c.writeFrame(opText, []byte(message))
}

// Ping sends a ping; the pong is passed over by ReadText.
func (c *Conn) Ping() error {
	return c.writeFrame(opPing, nil)
}

// SetReadDeadline sets when a pending or future read fails; zero for never.
func (c *Conn) SetReadDeadline(t time.Time) error { return c.conn.SetReadDeadline(t) }

// Close sends the close frame with a code and reason, and closes the
// connection. A reader blocked in ReadText returns with an error.
func (c *Conn) Close(code int, reason string) error {
	err := c.writeClose(code, reason)
	closeErr := c.conn.Close()
	if err != nil {
		return err
	}
	return closeErr
}
