package service

import (
	"context"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// fakeAccess counts logins; each renewal gives a new token.
type fakeAccess struct {
	logins int
	fail   error
}

func (f *fakeAccess) Stones() ([]agent.StoneInfo, map[string][]string, error) { return nil, nil, nil }
func (f *fakeAccess) Secrets(string) (string, string, string, error)          { return "", "", "", nil }
func (f *fakeAccess) Close()                                                  {}
func (f *fakeAccess) Session(_ context.Context, _ string, renew bool) (string, string, error) {
	if f.logins == 0 || renew {
		if f.fail != nil && f.logins > 0 {
			return "", "", f.fail
		}
		f.logins++
	}
	return "http://stone/api/v1", []string{"", "first", "second"}[f.logins], nil
}

func expired() error {
	return &exit.Error{Code: exit.Auth, ReturnCode: 403, Message: "The Session has expired.", ReturnMessage: "The Session has expired."}
}

// stoneWith makes a stone that has logged in once.
func stoneWith(access *fakeAccess) *Stone {
	_, token, _ := access.Session(context.Background(), "", false)
	return &Stone{Name: "work", access: access, token: token}
}

func TestDoRenewsAnExpiredSession(t *testing.T) {
	access := &fakeAccess{}
	var tokens []string
	err := stoneWith(access).Do(context.Background(), func(token string) error {
		tokens = append(tokens, token)
		if token == "first" {
			return expired()
		}
		return nil
	})
	if err != nil || len(tokens) != 2 || tokens[1] != "second" {
		t.Fatalf("err %v, tokens %v", err, tokens)
	}
}

func TestDoRenewsOnlyOnce(t *testing.T) {
	access := &fakeAccess{}
	calls := 0
	err := stoneWith(access).Do(context.Background(), func(string) error {
		calls++
		return expired()
	})
	if err == nil || calls != 2 {
		t.Fatalf("err %v after %d calls", err, calls)
	}
}

func TestDoKeepsOtherErrors(t *testing.T) {
	access := &fakeAccess{}
	calls := 0
	denied := &exit.Error{Code: exit.NotPermitted, ReturnCode: 401, Message: "no"}
	err := stoneWith(access).Do(context.Background(), func(string) error {
		calls++
		return denied
	})
	if err != denied || calls != 1 || access.logins != 1 {
		t.Fatalf("err %v after %d calls, %d logins", err, calls, access.logins)
	}
}

func TestDoReportsAFailedRenewal(t *testing.T) {
	refused := &exit.Error{Code: exit.Auth, ReturnCode: 403, Message: "refused"}
	access := &fakeAccess{fail: refused}
	err := stoneWith(access).Do(context.Background(), func(string) error { return expired() })
	if err != refused {
		t.Fatalf("err %v", err)
	}
}
