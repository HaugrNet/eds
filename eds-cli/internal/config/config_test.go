package config

import (
	"errors"
	"os"
	"reflect"
	"testing"
)

func TestSealOpen(t *testing.T) {
	c := NewContainer()
	c.Default = "work"
	c.Stones["work"] = Stone{URL: "https://eds.example.com/api/v1", Account: "kim", Passphrase: "correct horse battery staple"}
	c.Groups["announce"] = []string{"work:general"}

	sealed, err := Seal(c, "container passphrase", 1000)
	if err != nil {
		t.Fatal(err)
	}
	got, err := Open(sealed, "container passphrase")
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(got, c) {
		t.Errorf("got %+v, want %+v", got, c)
	}
	if _, err := Open(sealed, "wrong"); !errors.Is(err, ErrPassphrase) {
		t.Errorf("wrong passphrase: got %v, want ErrPassphrase", err)
	}
	sealed.Data[0] ^= 1
	if _, err := Open(sealed, "container passphrase"); !errors.Is(err, ErrPassphrase) {
		t.Errorf("tampered data: got %v, want ErrPassphrase", err)
	}
	if _, err := Open(nil, "x"); !errors.Is(err, ErrNoContainer) {
		t.Errorf("no container: got %v", err)
	}
}

func TestLoadSave(t *testing.T) {
	t.Setenv(EnvDir, t.TempDir())
	f, err := Load()
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(f.UI, DefaultUI()) || f.Container != nil {
		t.Errorf("defaults: %+v", f)
	}
	f.UI.Language = "de"
	f.Container, _ = Seal(NewContainer(), "p", 1000)
	if err := Save(f); err != nil {
		t.Fatal(err)
	}
	path, _ := Path()
	info, err := os.Stat(path)
	if err != nil {
		t.Fatal(err)
	}
	if info.Mode().Perm() != 0o600 {
		t.Errorf("mode %v, want 0600", info.Mode().Perm())
	}
	g, err := Load()
	if err != nil {
		t.Fatal(err)
	}
	if g.UI.Language != "de" || g.Container == nil {
		t.Errorf("loaded %+v", g)
	}
	// A config from before a setting existed keeps the setting's default:
	// the person is shown as online, unless they chose not to be.
	if err := os.WriteFile(path, []byte(`{"ui": {"language": "de"}}`), 0o600); err != nil {
		t.Fatal(err)
	}
	if old, err := Load(); err != nil || !old.UI.Present || old.UI.Language != "de" {
		t.Errorf("a config without \"present\": %+v, %v", old, err)
	}
	g.UI.Present = false
	if err := Save(g); err != nil {
		t.Fatal(err)
	}
	if chosen, err := Load(); err != nil || chosen.UI.Present {
		t.Errorf("\"present\" switched off: %+v, %v", chosen, err)
	}
	data, _ := os.ReadFile(path)
	if string(data) == "" || containsAny(string(data), "example.com", "passphrase\"") {
		t.Errorf("config leaks: %s", data)
	}
}

func containsAny(s string, subs ...string) bool {
	for _, sub := range subs {
		for i := 0; i+len(sub) <= len(s); i++ {
			if s[i:i+len(sub)] == sub {
				return true
			}
		}
	}
	return false
}
