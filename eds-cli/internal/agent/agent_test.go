package agent

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"path/filepath"
	"sync/atomic"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// A fake stone that counts logins and logouts.
func fakeStone(t *testing.T) (*httptest.Server, *int32, *int32) {
	var logins, logouts int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var in struct{ AccountName, Credential string }
		_ = json.NewDecoder(r.Body).Decode(&in)
		switch r.URL.Path {
		case "/api/v1/members/login":
			if in.Credential != "c2VjcmV0" { // "secret"
				w.Write([]byte(`{"returnCode":403,"returnMessage":"Cannot authenticate."}`))
				return
			}
			n := atomic.AddInt32(&logins, 1)
			json.NewEncoder(w).Encode(map[string]any{"returnCode": 200, "returnMessage": "Ok", "session": "token-" + string(rune('0'+n))})
		case "/api/v1/members/logout":
			atomic.AddInt32(&logouts, 1)
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok"}`))
		}
	}))
	t.Cleanup(srv.Close)
	return srv, &logins, &logouts
}

func TestAgent(t *testing.T) {
	t.Setenv(EnvDir, filepath.Join(t.TempDir(), "agent"))
	srv, logins, logouts := fakeStone(t)
	container := config.NewContainer()
	container.Default = "work"
	container.Stones["work"] = config.Stone{URL: srv.URL + "/api/v1", Account: "kim", Passphrase: "secret"}
	container.Stones["bad"] = config.Stone{URL: srv.URL + "/api/v1", Account: "kim", Passphrase: "wrong"}
	container.Groups["announce"] = []string{"work:general"}

	if Running() {
		t.Fatal("no agent should be running yet")
	}
	if _, err := Ask(Request{Op: "ping"}); !errors.Is(err, ErrNotRunning) {
		t.Fatalf("got %v, want ErrNotRunning", err)
	}
	listener, err := Listen()
	if err != nil {
		t.Fatal(err)
	}
	server := NewServer(container, 0)
	served := make(chan error, 1)
	go func() { served <- server.Serve(listener) }()

	if _, err := Listen(); err == nil {
		t.Error("a second agent must be refused")
	}
	resp, err := Ask(Request{Op: "stones"})
	if err != nil {
		t.Fatal(err)
	}
	if len(resp.Stones) != 2 || resp.Stones[1].Name != "work" || !resp.Stones[1].Default || resp.Stones[1].LoggedIn {
		t.Errorf("stones: %+v", resp.Stones)
	}
	if len(resp.Groups["announce"]) != 1 {
		t.Errorf("groups: %+v", resp.Groups)
	}
	raw, _ := json.Marshal(resp)
	if string(raw) == "" || contains(string(raw), "secret") {
		t.Errorf("the stones answer leaks the passphrase: %s", raw)
	}

	// The default stone: one login, reused by the next command.
	first, err := Ask(Request{Op: "session"})
	if err != nil || first.Token != "token-1" || first.URL != srv.URL+"/api/v1" {
		t.Fatalf("session: %+v, %v", first, err)
	}
	again, _ := Ask(Request{Op: "session", Stone: "work"})
	if again.Token != "token-1" || atomic.LoadInt32(logins) != 1 {
		t.Errorf("the session should be reused: %+v, %d logins", again, *logins)
	}
	renewed, _ := Ask(Request{Op: "session", Stone: "work", Renew: true})
	if renewed.Token != "token-2" {
		t.Errorf("renew: %+v", renewed)
	}

	var e *exit.Error
	if _, err := Ask(Request{Op: "session", Stone: "bad"}); !errors.As(err, &e) || e.Code != exit.Auth {
		t.Errorf("wrong passphrase: %v", err)
	}
	if _, err := Ask(Request{Op: "session", Stone: "nowhere"}); !errors.As(err, &e) || e.Code != exit.NotFound {
		t.Errorf("unknown stone: %v", err)
	}

	if _, err := Ask(Request{Op: "logout"}); err != nil {
		t.Fatal(err)
	}
	if err := <-served; err != nil {
		t.Errorf("serve: %v", err)
	}
	if atomic.LoadInt32(logouts) != 1 {
		t.Errorf("%d logouts, want 1", *logouts)
	}
	if Running() {
		t.Error("the agent should have stopped")
	}
}

func contains(s, sub string) bool {
	for i := 0; i+len(sub) <= len(s); i++ {
		if s[i:i+len(sub)] == sub {
			return true
		}
	}
	return false
}
