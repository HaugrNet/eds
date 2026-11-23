// Package agent keeps the unlocked container in memory between commands,
// like ssh-agent. `tolk login` starts it and hands it the container; it logs
// in to a stone when a command first needs it, keeps the session tokens, and
// logs in again when one has expired. `tolk logout` ends the sessions and
// stops it. Nothing decrypted is written to disk.
//
// Commands talk to it over a socket in a directory only the user can open:
// one JSON request, one JSON answer per connection.
package agent

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"os"
	"sort"
	"sync"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// Request is what a command asks the agent.
type Request struct {
	Op        string            `json:"op"` // ping, stones, session, secrets, load, logout
	Stone     string            `json:"stone,omitempty"`
	Renew     bool              `json:"renew,omitempty"`
	Container *config.Container `json:"container,omitempty"`
}

// StoneInfo is a stone without its passphrase.
type StoneInfo struct {
	Name     string `json:"name"`
	URL      string `json:"url"`
	Account  string `json:"account"`
	Default  bool   `json:"default"`
	LoggedIn bool   `json:"loggedIn"`
}

// Response is the agent's answer.
type Response struct {
	OK     bool                `json:"ok"`
	Error  *exit.Error         `json:"error,omitempty"`
	Stones []StoneInfo         `json:"stones"`
	Groups map[string][]string `json:"groups"`
	URL    string              `json:"url,omitempty"`
	Token  string              `json:"token,omitempty"`
	// What a stone's entry keeps beside the passphrase: the account key for
	// chambers and the status key.
	AccountKey string `json:"accountKey,omitempty"`
	StatusKey  string `json:"statusKey,omitempty"`
	Expires    string `json:"expires,omitempty"` // when the agent stops by itself, RFC 3339
}

// Server is the agent's state.
type Server struct {
	mu        sync.Mutex
	container *config.Container
	tokens    map[string]string
	expires   time.Time
	done      chan struct{}
	once      sync.Once
}

// NewServer makes an agent holding the container. A lifetime above zero
// makes it stop by itself after that time.
func NewServer(container *config.Container, lifetime time.Duration) *Server {
	s := &Server{container: container, tokens: map[string]string{}, done: make(chan struct{})}
	if lifetime > 0 {
		s.expires = time.Now().Add(lifetime)
		time.AfterFunc(lifetime, s.Stop)
	}
	return s
}

// Stop ends the sessions on the stones and stops the agent.
func (s *Server) Stop() {
	s.once.Do(func() {
		s.mu.Lock()
		defer s.mu.Unlock()
		ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		defer cancel()
		for name, token := range s.tokens {
			if stone, ok := s.container.Stones[name]; ok {
				_ = client.New(stone.URL).Logout(ctx, token) // best effort
			}
		}
		s.tokens = map[string]string{}
		s.container = config.NewContainer()
		close(s.done)
	})
}

// Serve answers requests on the listener until the agent is stopped.
func (s *Server) Serve(listener net.Listener) error {
	go func() {
		<-s.done
		listener.Close()
	}()
	for {
		conn, err := listener.Accept()
		if err != nil {
			select {
			case <-s.done:
				return nil
			default:
				return err
			}
		}
		go s.handle(conn)
	}
}

func (s *Server) handle(conn net.Conn) {
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(90 * time.Second))
	var req Request
	if err := json.NewDecoder(conn).Decode(&req); err != nil {
		return
	}
	resp := s.answer(req)
	_ = json.NewEncoder(conn).Encode(resp)
	if req.Op == "logout" {
		s.Stop()
	}
}

func (s *Server) answer(req Request) Response {
	s.mu.Lock()
	defer s.mu.Unlock()
	resp := Response{OK: true}
	if !s.expires.IsZero() {
		resp.Expires = s.expires.Format(time.RFC3339)
	}
	switch req.Op {
	case "ping", "logout":
	case "stones":
		for name, stone := range s.container.Stones {
			_, in := s.tokens[name]
			resp.Stones = append(resp.Stones, StoneInfo{Name: name, URL: stone.URL, Account: stone.Account, Default: name == s.container.Default, LoggedIn: in})
		}
		sort.Slice(resp.Stones, func(i, j int) bool { return resp.Stones[i].Name < resp.Stones[j].Name })
		resp.Groups = s.container.Groups
		if resp.Stones == nil {
			resp.Stones = []StoneInfo{}
		}
		if resp.Groups == nil {
			resp.Groups = map[string][]string{}
		}
	case "session":
		name := req.Stone
		if name == "" {
			name = s.container.Default
		}
		stone, ok := s.container.Stones[name]
		if !ok {
			return fail(exit.New(exit.NotFound, "no stone named %q", name))
		}
		token, have := s.tokens[name]
		if !have || req.Renew {
			ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
			defer cancel()
			var err error
			token, err = client.New(stone.URL).Login(ctx, stone.Account, stone.Passphrase)
			if err != nil {
				delete(s.tokens, name)
				var e *exit.Error
				if errors.As(err, &e) {
					return fail(e)
				}
				return fail(exit.New(exit.Other, "%v", err))
			}
			s.tokens[name] = token
		}
		resp.URL, resp.Token = stone.URL, token
	case "secrets":
		name := req.Stone
		if name == "" {
			name = s.container.Default
		}
		stone, ok := s.container.Stones[name]
		if !ok {
			return fail(exit.New(exit.NotFound, "no stone named %q", name))
		}
		resp.URL, resp.AccountKey, resp.StatusKey = stone.URL, stone.AccountKey, stone.StatusKey
	case "load":
		if req.Container == nil {
			return fail(exit.New(exit.Usage, "load without a container"))
		}
		// Sessions of stones that changed or went away are dropped.
		for name := range s.tokens {
			old, now := s.container.Stones[name], req.Container.Stones[name]
			if old.URL != now.URL || old.Account != now.Account || old.Passphrase != now.Passphrase {
				delete(s.tokens, name)
			}
		}
		s.container = req.Container
	default:
		return fail(exit.New(exit.Usage, "unknown request %q", req.Op))
	}
	return resp
}

func fail(e *exit.Error) Response { return Response{Error: e} }

// Listen opens the agent's socket. It fails if an agent is already running;
// a socket left behind by a dead agent is replaced.
func Listen() (net.Listener, error) {
	path, err := SocketPath()
	if err != nil {
		return nil, err
	}
	if _, err := Ask(Request{Op: "ping"}); err == nil {
		return nil, fmt.Errorf("an agent is already running")
	}
	_ = os.Remove(path)
	listener, err := net.Listen("unix", path)
	if err != nil {
		return nil, err
	}
	_ = os.Chmod(path, 0o600)
	return listener, nil
}

// ErrNotRunning is returned when no agent answers.
var ErrNotRunning = errors.New("no agent is running")

// Ask sends one request to the agent.
func Ask(req Request) (*Response, error) {
	path, err := SocketPath()
	if err != nil {
		return nil, err
	}
	conn, err := net.DialTimeout("unix", path, 2*time.Second)
	if err != nil {
		return nil, ErrNotRunning
	}
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(90 * time.Second))
	if err := json.NewEncoder(conn).Encode(req); err != nil {
		return nil, ErrNotRunning
	}
	var resp Response
	if err := json.NewDecoder(conn).Decode(&resp); err != nil {
		return nil, ErrNotRunning
	}
	if resp.Error != nil {
		return nil, resp.Error
	}
	return &resp, nil
}

// Running reports whether an agent answers.
func Running() bool {
	_, err := Ask(Request{Op: "ping"})
	return err == nil
}
