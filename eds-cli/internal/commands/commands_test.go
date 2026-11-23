package commands

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// A fake stone: a test system whose account "kim" has the passphrase
// "secret".
func fakeStone(t *testing.T) *httptest.Server {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var in struct{ AccountName, Credential string }
		_ = json.NewDecoder(r.Body).Decode(&in)
		switch r.URL.Path {
		case "/api/v1/version":
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok","version":"2.0-SNAPSHOT","database":"H2","persistent":false}`))
		case "/api/v1/members/login":
			if in.AccountName != "kim" || in.Credential != "c2VjcmV0" {
				w.Write([]byte(`{"returnCode":403,"returnMessage":"Cannot authenticate."}`))
				return
			}
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok","session":"dG9rZW4="}`))
		case "/api/v1/members/logout":
			w.Write([]byte(`{"returnCode":200,"returnMessage":"Ok"}`))
		default:
			w.WriteHeader(404)
		}
	}))
	t.Cleanup(srv.Close)
	return srv
}

// run runs one command line non-interactively and returns the exit code and
// what it wrote.
func run(stdin string, args ...string) (int, string, string) {
	var out, errOut bytes.Buffer
	code := Run(args, &Env{In: strings.NewReader(stdin), Out: &out, Err: &errOut})
	return code, out.String(), errOut.String()
}

func setup(t *testing.T) string {
	dir := t.TempDir()
	t.Setenv(config.EnvDir, dir)
	t.Setenv(agent.EnvDir, filepath.Join(dir, "agent"))
	t.Setenv("TOLK_KDF_ITERATIONS", "1000")
	t.Setenv("LC_ALL", "C")
	t.Setenv("LANG", "")
	passFile := filepath.Join(dir, "pass")
	if err := os.WriteFile(passFile, []byte("container passphrase\n"), 0o600); err != nil {
		t.Fatal(err)
	}
	t.Setenv(service.EnvPassphraseFile, passFile)
	return dir
}

func TestStoneCommands(t *testing.T) {
	dir := setup(t)
	srv := fakeStone(t)

	if code, _, errOut := run("", "list", "stones"); code != exit.Auth {
		t.Fatalf("before init: code %d, %s", code, errOut)
	}
	if code, _, errOut := run("", "init"); code != exit.OK {
		t.Fatalf("init: code %d, %s", code, errOut)
	}
	if code, _, _ := run("", "init"); code != exit.Usage {
		t.Errorf("second init: code %d, want usage", code)
	}

	// A bare host gets the API root appended.
	host := strings.TrimPrefix(srv.URL, "http://")
	code, _, errOut := run("secret\n", "register", "stone", "work", "--url", "http://"+host, "--account", "kim", "--passphrase-stdin")
	if code != exit.OK {
		t.Fatalf("register: code %d, %s", code, errOut)
	}
	if code, _, _ := run("secret\n", "register", "stone", "work", "--url", srv.URL, "--account", "kim", "--passphrase-stdin"); code != exit.Usage {
		t.Errorf("register twice: code %d, want usage", code)
	}
	if code, _, _ := run("wrong\n", "register", "server", "other", "--url", srv.URL, "--account", "kim", "--passphrase-stdin"); code != exit.Auth {
		t.Errorf("wrong passphrase: code %d, want auth", code)
	}
	if code, _, _ := run("", "register", "stone", "other", "--url", srv.URL, "--account", "kim"); code != exit.Usage {
		t.Errorf("no passphrase and nobody to ask: code %d, want usage", code)
	}
	if code, _, _ := run("secret\n", "register", "stone", "a:b", "--url", srv.URL, "--account", "kim", "--passphrase-stdin"); code != exit.Usage {
		t.Errorf("bad name: code %d, want usage", code)
	}
	// Flags may come before the name, and "haugr" means stone.
	if code, _, errOut := run("secret\n", "register", "haugr", "--url", srv.URL+"/api/v1", "--account", "kim", "--passphrase-stdin", "home"); code != exit.OK {
		t.Fatalf("register home: code %d, %s", code, errOut)
	}

	code, out, errOut := run("", "list", "stones", "--output", "json")
	if code != exit.OK {
		t.Fatalf("list: code %d, %s", code, errOut)
	}
	var listed struct {
		Stones []agent.StoneInfo
	}
	if err := json.Unmarshal([]byte(out), &listed); err != nil {
		t.Fatalf("list output: %v\n%s", err, out)
	}
	if len(listed.Stones) != 2 || listed.Stones[1].Name != "work" || !listed.Stones[1].Default ||
		listed.Stones[1].URL != srv.URL+"/api/v1" || listed.Stones[1].Account != "kim" {
		t.Errorf("listed: %+v", listed.Stones)
	}
	if strings.Contains(out, "secret") {
		t.Errorf("the list shows a passphrase: %s", out)
	}
	raw, _ := os.ReadFile(filepath.Join(dir, "config.json"))
	for _, leak := range []string{host, "kim", "secret", "work"} {
		if strings.Contains(string(raw), leak) {
			t.Errorf("config.json reveals %q", leak)
		}
	}

	code, out, errOut = run("", "stone", "info", "--output=json")
	if code != exit.OK {
		t.Fatalf("stone info: code %d, %s", code, errOut)
	}
	if !strings.Contains(out, `"version": "2.0-SNAPSHOT"`) || !strings.Contains(out, `"kind": "test"`) || !strings.Contains(out, `"stone": "work"`) {
		t.Errorf("stone info: %s", out)
	}
	if _, out, _ = run("", "stone", "info", "home"); !strings.Contains(out, "Test system") {
		t.Errorf("stone info text: %s", out)
	}
	if code, _, _ := run("", "stone", "info", "nowhere"); code != exit.NotFound {
		t.Errorf("unknown stone: code %d", code)
	}

	if code, _, errOut := run("", "update", "stone", "home", "--rename", "cabin", "--default"); code != exit.OK {
		t.Fatalf("update: code %d, %s", code, errOut)
	}
	if code, _, _ := run("", "update", "stone", "cabin"); code != exit.Usage {
		t.Errorf("update without a change: code %d", code)
	}
	if code, _, errOut := run("", "remove", "stone", "work"); code != exit.OK {
		t.Fatalf("remove: code %d, %s", code, errOut)
	}
	_, out, _ = run("", "list", "stones")
	if !strings.Contains(out, "* cabin") || strings.Contains(out, "work") {
		t.Errorf("after update and remove: %s", out)
	}

	// Without the passphrase file and nobody at the terminal: exit code 3.
	t.Setenv(service.EnvPassphraseFile, "")
	if code, _, _ := run("", "list", "stones"); code != exit.Auth {
		t.Errorf("locked: code %d, want auth", code)
	}
}

func TestSettingsAndHelp(t *testing.T) {
	setup(t)
	if code, out, _ := run("", "settings", "language"); code != exit.OK || strings.TrimSpace(out) != "auto" {
		t.Errorf("settings language: %d %q", code, out)
	}
	if code, _, _ := run("", "settings", "language", "de"); code != exit.OK {
		t.Errorf("set language: %d", code)
	}
	if _, out, _ := run("", "settings", "--output", "json"); !strings.Contains(out, `"language": "de"`) {
		t.Errorf("settings json: %s", out)
	}
	// Shown as online in the interactive mode, unless switched off.
	if code, out, _ := run("", "settings", "present"); code != exit.OK || strings.TrimSpace(out) != "true" {
		t.Errorf("settings present: %d %q", code, out)
	}
	if code, _, _ := run("", "settings", "present", "false"); code != exit.OK {
		t.Errorf("set present: %d", code)
	}
	if _, out, _ := run("", "settings", "--output", "json"); !strings.Contains(out, `"present": false`) {
		t.Errorf("settings json: %s", out)
	}
	if code, _, _ := run("", "settings", "language", "xx"); code != exit.Usage {
		t.Errorf("bad value: %d", code)
	}
	if code, _, _ := run("", "settings", "nothing"); code != exit.Usage {
		t.Errorf("bad key: %d", code)
	}
	if code, out, _ := run("", "--lang", "en", "help"); code != exit.OK || !strings.Contains(out, "tolk register stone") || !strings.Contains(out, "HOW TO START") {
		t.Errorf("help: %d %s", code, out)
	}
	if code, _, errOut := run("", "frobnicate", "--output", "json"); code != exit.Usage || !strings.Contains(errOut, `"code":2`) {
		t.Errorf("unknown command: %d %s", code, errOut)
	}
	if code, out, _ := run("", "version"); code != exit.OK || !strings.HasPrefix(out, "tolk ") {
		t.Errorf("version: %d %s", code, out)
	}
}

func TestNormaliseURL(t *testing.T) {
	for in, want := range map[string]string{
		"eds.example.com":                 "https://eds.example.com/api/v1",
		"https://eds.example.com/":        "https://eds.example.com/api/v1",
		"http://localhost:8080/api/v1/":   "http://localhost:8080/api/v1",
		"https://example.com/rune/api/v1": "https://example.com/rune/api/v1",
	} {
		if got, err := normaliseURL(in); err != nil || got != want {
			t.Errorf("normaliseURL(%q) = %q, %v; want %q", in, got, err, want)
		}
	}
	for _, bad := range []string{"", "ftp://x", "https://"} {
		if _, err := normaliseURL(bad); err == nil {
			t.Errorf("normaliseURL(%q) should fail", bad)
		}
	}
}

// The help: an overview, topics, the topic of a command, and all of it; in
// both languages, and never wider than a terminal.
func TestHelpTopics(t *testing.T) {
	setup(t)
	_, files, _ := run("", "--lang", "en", "help", "files")
	if !strings.Contains(files, "tolk send <file|->") || strings.Contains(files, "tolk register stone") {
		t.Errorf("help files: %s", files)
	}
	// A command leads to its topic, with 'help' before it or --help after.
	for _, args := range [][]string{{"help", "send"}, {"send", "--help"}, {"get", "-h"}, {"help", "FILES"}} {
		if code, out, _ := run("", append([]string{"--lang", "en"}, args...)...); code != exit.OK || out != files {
			t.Errorf("%v: %d, not the help on files: %s", args, code, out)
		}
	}
	if code, _, errOut := run("", "--lang", "en", "help", "frobnicate"); code != exit.Usage || !strings.Contains(errOut, "stones, addresses") {
		t.Errorf("help on nothing: %d %s", code, errOut)
	}
	// Every command has a topic.
	for _, verb := range []string{"init", "login", "logout", "settings", "rooms", "room", "members", "users", "user", "add",
		"access", "leave", "invite", "accept", "passphrase", "avatar", "about", "groups", "group", "chat", "ls", "mkdir",
		"mv", "rename", "rm", "send", "get", "cp", "register", "update", "remove", "list", "stone", "stones", "server",
		"haugr", "version", "help", "man"} {
		if topicOf(helpTopics(), verb) == nil {
			t.Errorf("no help topic for %q", verb)
		}
	}
	for _, lang := range []string{"en", "de"} {
		_, all, _ := run("", "--lang", lang, "help", "all")
		_, overview, _ := run("", "--lang", lang, "help")
		for _, topic := range helpTopics() {
			if !strings.Contains(overview, "tolk help "+topic.name) {
				t.Errorf("%s: the overview doesn't name the topic %s", lang, topic.name)
			}
			if _, out, _ := run("", "--lang", lang, "help", topic.name); !strings.Contains(all, out) {
				t.Errorf("%s: 'help all' lacks the topic %s", lang, topic.name)
			}
		}
		for _, line := range strings.Split(all+overview, "\n") {
			if n := len([]rune(line)); n > 80 {
				t.Errorf("%s: a line of the help has %d columns: %s", lang, n, line)
			}
		}
	}
}

func TestManPage(t *testing.T) {
	setup(t)
	code, page, _ := run("", "--lang", "en", "man")
	if code != exit.OK || !strings.Contains(page, ".TH TOLK 1") || !strings.Contains(page, ".SH \"HOW TO START\"") {
		t.Fatalf("man: %d %s", code, page)
	}
	// Dashes are escaped, so that a flag can be copied from the page, and
	// no line is taken for a request by mistake.
	if !strings.Contains(page, `[\-\-lifetime 8h]`) {
		t.Errorf("man: the dashes are not escaped")
	}
	for _, line := range strings.Split(page, "\n") {
		if strings.HasPrefix(line, "'") || (strings.HasPrefix(line, ".") && !strings.HasPrefix(line, ".\\\"") &&
			!regexp.MustCompile(`^\.(TH|SH|PP|TP|TQ|IP|nf|fi|br|nh|ad|ds)\b`).MatchString(line)) {
			t.Errorf("man: this line would be read as a request: %s", line)
		}
	}
	if _, german, _ := run("", "--lang", "de", "man"); !strings.Contains(german, ".SH \"ERSTE SCHRITTE\"") {
		t.Errorf("man in German: %s", german)
	}
}
