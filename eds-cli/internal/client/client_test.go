package client

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"os"
	"reflect"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

const contractPath = "../../../eds-api/src/main/resources/openapi.json"

// The contract is the source of truth: every operation the client uses must
// exist in it with the same schemas, and every field the Go types read or
// send must be a property of the schema.
func TestClientMatchesContract(t *testing.T) {
	data, err := os.ReadFile(contractPath)
	if err != nil {
		t.Fatalf("the contract is needed for this test: %v", err)
	}
	var doc struct {
		Paths      map[string]map[string]json.RawMessage `json:"paths"`
		Components struct {
			Schemas map[string]json.RawMessage `json:"schemas"`
		} `json:"components"`
	}
	if err := json.Unmarshal(data, &doc); err != nil {
		t.Fatal(err)
	}
	props := func(name string) map[string]bool {
		out := map[string]bool{}
		var walk func(raw json.RawMessage)
		walk = func(raw json.RawMessage) {
			var s struct {
				Ref        string                     `json:"$ref"`
				AllOf      []json.RawMessage          `json:"allOf"`
				Properties map[string]json.RawMessage `json:"properties"`
			}
			if json.Unmarshal(raw, &s) != nil {
				return
			}
			if s.Ref != "" {
				walk(doc.Components.Schemas[s.Ref[strings.LastIndex(s.Ref, "/")+1:]])
			}
			for _, a := range s.AllOf {
				walk(a)
			}
			for p := range s.Properties {
				out[p] = true
			}
		}
		walk(doc.Components.Schemas[name])
		return out
	}
	// Every registered type, nested ones included, only has the contract's
	// fields.
	for schema, goType := range Types {
		known := props(schema)
		if len(known) == 0 {
			t.Errorf("schema %s has no properties in the contract", schema)
		}
		for _, field := range jsonFields(reflect.TypeOf(goType)) {
			if !known[field] {
				t.Errorf("%s: field %q is not in the contract", schema, field)
			}
		}
	}
	if len(Operations) < 25 {
		t.Errorf("only %d operations are checked", len(Operations))
	}
	for _, op := range Operations {
		raw, ok := doc.Paths[op.Path][strings.ToLower(op.Method)]
		if !ok {
			t.Errorf("%s %s is not in the contract", op.Method, op.Path)
			continue
		}
		text := string(raw)
		for _, schema := range []string{op.Request, op.Response} {
			if schema == "" {
				continue
			}
			if !strings.Contains(text, "#/components/schemas/"+schema+`"`) {
				t.Errorf("%s %s doesn't use schema %s in the contract", op.Method, op.Path, schema)
			}
			goType, ok := Types[schema]
			if !ok {
				t.Errorf("no Go type registered for schema %s", schema)
				continue
			}
			known := props(schema)
			if len(known) == 0 {
				t.Errorf("schema %s has no properties in the contract", schema)
			}
			for _, field := range jsonFields(reflect.TypeOf(goType)) {
				if !known[field] {
					t.Errorf("%s: field %q is not in the contract", schema, field)
				}
			}
		}
	}
}

func jsonFields(t reflect.Type) []string {
	var out []string
	for i := 0; i < t.NumField(); i++ {
		f := t.Field(i)
		if f.Anonymous {
			out = append(out, jsonFields(f.Type)...)
			continue
		}
		name := strings.Split(f.Tag.Get("json"), ",")[0]
		if name != "" && name != "-" {
			out = append(out, name)
		}
	}
	return out
}

func TestLoginVersionLogout(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var in ProcessMemberRequest
		_ = json.NewDecoder(r.Body).Decode(&in)
		w.Header().Set("Content-Type", "application/json")
		switch r.URL.Path {
		case "/api/v1/version":
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok","version":"2.0-SNAPSHOT","database":"H2","persistent":false}`))
		case "/api/v1/members/login":
			if in.AccountName != "kim" || in.CredentialType != "PASSPHRASE" || in.Credential != "c2VjcmV0" {
				w.Write([]byte(`{"returnCode":403,"returnMessage":"Cannot authenticate."}`))
				return
			}
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok","memberId":"m1","session":"dG9rZW4="}`))
		case "/api/v1/members/logout":
			if in.CredentialType != "SESSION" || in.Credential != "dG9rZW4=" {
				w.Write([]byte(`{"returnCode":403,"returnMessage":"Cannot authenticate."}`))
				return
			}
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok"}`))
		default:
			w.WriteHeader(404)
			w.Write([]byte(`<html>not found</html>`))
		}
	}))
	defer srv.Close()
	ctx := context.Background()
	c := New(srv.URL + "/api/v1/")

	v, err := c.Version(ctx)
	if err != nil {
		t.Fatal(err)
	}
	if v.Version != "2.0-SNAPSHOT" || v.Database != "H2" || v.Persistent == nil || *v.Persistent {
		t.Errorf("version: %+v", v)
	}
	token, err := c.Login(ctx, "kim", "secret")
	if err != nil || token != "dG9rZW4=" {
		t.Fatalf("login: %q, %v", token, err)
	}
	if err := c.Logout(ctx, token); err != nil {
		t.Errorf("logout: %v", err)
	}

	_, err = c.Login(ctx, "kim", "wrong")
	var e *exit.Error
	if !errors.As(err, &e) || e.Code != exit.Auth || e.ReturnCode != 403 {
		t.Errorf("wrong passphrase: %+v", err)
	}
	_, err = New(srv.URL + "/wrong").Version(ctx)
	if !errors.As(err, &e) || e.Code != exit.NotFound {
		t.Errorf("wrong URL: %+v", err)
	}
	srv.Close()
	_, err = c.Version(ctx)
	if !errors.As(err, &e) || e.Code != exit.Network {
		t.Errorf("stone down: %+v", err)
	}
}
