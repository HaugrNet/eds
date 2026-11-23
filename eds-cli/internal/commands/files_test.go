package commands

import (
	"bytes"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/fakestone"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// world sets up a container with two stones: "work" as admin (the default)
// and "home" as birgit, who can only read in the room Family there.
func world(t *testing.T) (work, home *fakestone.Stone) {
	t.Helper()
	setup(t)
	work = fakestone.New("admin secret")
	home = fakestone.New("other secret")
	t.Cleanup(work.Close)
	t.Cleanup(home.Close)
	work.AddUser("birgit", "birgit secret")
	work.AddRoom("Projects", "admin", "ADMIN", "birgit", "WRITE")
	work.AddRoom("Archive", "admin", "ADMIN")
	home.AddUser("birgit", "birgit secret")
	home.AddRoom("Family", "admin", "ADMIN", "birgit", "READ")
	home.AddRoom("Birgit", "birgit", "ADMIN")
	must(t, "", "init")
	must(t, "admin secret\n", "register", "stone", "work", "--url", work.URL(), "--account", "admin", "--passphrase-stdin")
	must(t, "birgit secret\n", "register", "stone", "home", "--url", home.URL(), "--account", "birgit", "--passphrase-stdin")
	return work, home
}

// must runs a command that has to succeed, and returns its output.
func must(t *testing.T, stdin string, args ...string) string {
	t.Helper()
	code, out, errOut := run(stdin, args...)
	if code != exit.OK {
		t.Fatalf("tolk %s: exit code %d\n%s", strings.Join(args, " "), code, errOut)
	}
	return out
}

// results decodes the {"results": […]} of a command.
func results(t *testing.T, out string) []outcome {
	t.Helper()
	var doc struct {
		Results []outcome `json:"results"`
	}
	if err := json.Unmarshal([]byte(out), &doc); err != nil {
		t.Fatalf("%v in %s", err, out)
	}
	return doc.Results
}

func TestRoomsAndMembers(t *testing.T) {
	world(t)
	out := must(t, "", "rooms")
	if !strings.Contains(out, "Archive") || !strings.Contains(out, "Projects") || !strings.Contains(out, "Administrator") {
		t.Errorf("rooms:\n%s", out)
	}
	out = must(t, "", "rooms", "--stone", "home", "--output", "json")
	if !strings.Contains(out, `"access": "read"`) || !strings.Contains(out, `"name": "Family"`) {
		t.Errorf("rooms of home:\n%s", out)
	}
	must(t, "", "room", "new", "work:Plans")
	must(t, "", "add", "birgit", "--room", "Plans", "--access", "read")
	must(t, "", "access", "birgit", "admin", "--circle", "work:Plans")
	out = must(t, "", "members", "--room", "Plans")
	if !strings.Contains(out, "birgit") || strings.Count(out, "Administrator") != 2 {
		t.Errorf("members:\n%s", out)
	}
	must(t, "", "remove", "birgit", "--room", "Plans")
	if out = must(t, "", "members", "Plans"); strings.Contains(out, "birgit") {
		t.Errorf("after remove:\n%s", out)
	}
	// Several rooms: the one that fails is reported, the other still done.
	code, out, _ := run("", "add", "birgit", "--room", "Archive", "--room", "Nowhere", "--output", "json")
	got := results(t, out)
	if code != exit.NotFound || len(got) != 2 || !got[0].OK || got[1].OK || got[1].Error.Code != exit.NotFound {
		t.Errorf("add to two rooms: code %d, %+v", code, got)
	}
	// Only the System Administrator makes users.
	if code, _, errOut := run("some passphrase\n", "user", "new", "olaf", "--stone", "home", "--passphrase-stdin"); code != exit.NotPermitted {
		t.Errorf("user new as birgit: code %d, %s", code, errOut)
	}
	must(t, "some passphrase\n", "user", "new", "olaf", "--passphrase-stdin")
	if out = must(t, "", "users"); !strings.Contains(out, "olaf") {
		t.Errorf("users:\n%s", out)
	}
	must(t, "", "user", "rm", "olaf")
	// The first System Administrator makes an administrator, and a user of it again.
	must(t, "some passphrase\n", "user", "new", "olga", "--admin", "--passphrase-stdin")
	if out = must(t, "", "users"); !strings.Contains(out, "olga") || !strings.Contains(out, "(first admin)") {
		t.Errorf("users with two administrators:\n%s", out)
	}
	if out = must(t, "", "user", "olga", "--output", "json"); !strings.Contains(out, `"systemAdministrator": true`) || !strings.Contains(out, `"firstAdministrator": false`) {
		t.Errorf("olga as an administrator:\n%s", out)
	}
	must(t, "", "user", "role", "olga", "user")
	if out = must(t, "", "user", "olga", "--output", "json"); !strings.Contains(out, `"systemAdministrator": false`) {
		t.Errorf("olga as a user again:\n%s", out)
	}
	if code, _, _ := run("", "user", "role", "olga", "boss"); code != exit.Usage {
		t.Errorf("an unknown role: code %d", code)
	}
	if code, _, _ := run("", "user", "rm", "olga", "--admin"); code != exit.Usage {
		t.Errorf("--admin beside rm: code %d", code)
	}
	must(t, "", "user", "rm", "olga", "--yes")
	must(t, "", "leave", "--room", "home:Birgit")
	if out = must(t, "", "rooms", "--stone", "home"); strings.Contains(out, "Birgit") {
		t.Errorf("after leaving:\n%s", out)
	}
}

func TestSendListGet(t *testing.T) {
	work, _ := world(t)
	dir := t.TempDir()
	content := bytes.Repeat([]byte("Rune and Tolk. "), 20000)
	local := filepath.Join(dir, "report.txt")
	if err := os.WriteFile(local, content, 0o600); err != nil {
		t.Fatal(err)
	}

	must(t, "", "send", local, "Projects")
	if got, ok := work.File("Projects", "report.txt"); !ok || !bytes.Equal(got, content) {
		t.Fatal("the file didn't arrive")
	}
	// The same name again is refused, unless it is to be replaced.
	if code, _, errOut := run("", "send", local, "Projects"); code != exit.Usage || !strings.Contains(errOut, "--replace") {
		t.Errorf("same name: code %d, %s", code, errOut)
	}
	must(t, "", "send", local, "Projects", "--replace")

	// A folder that isn't there: refused, or made with -p.
	if code, _, _ := run("", "send", local, "Projects/2026/q4/"); code != exit.NotFound {
		t.Errorf("missing folder: code %d", code)
	}
	must(t, "", "send", local, "Projects/2026/q4/final.txt", "-p")
	if _, ok := work.File("Projects", "2026", "q4", "final.txt"); !ok {
		t.Error("the folders weren't made")
	}

	// A pipe has no length: sent without one, to two rooms at once.
	before := work.Chunked
	out := must(t, string(content), "send", "-", "Projects/piped.txt", "Archive/piped.txt", "--output", "json")
	if got := results(t, out); len(got) != 2 || !got[0].OK || !got[1].OK {
		t.Errorf("piped: %s", out)
	}
	if work.Chunked != before+2 {
		t.Errorf("%d uploads without a length, want 2 more than %d", work.Chunked, before)
	}
	for _, room := range []string{"Projects", "Archive"} {
		if got, ok := work.File(room, "piped.txt"); !ok || !bytes.Equal(got, content) {
			t.Errorf("%s: the piped content didn't arrive", room)
		}
	}
	// From a pipe, a name is needed.
	if code, _, _ := run("x", "send", "-", "Projects"); code != exit.Usage {
		t.Errorf("pipe without a name: code %d", code)
	}
	must(t, "x", "send", "-", "Projects", "--name", "x.txt")

	out = must(t, "", "ls", "Projects", "--output", "json")
	for _, name := range []string{"report.txt", "piped.txt", "x.txt", `"type": "folder"`, `"size": 300000`, `"added": "2026-11-10T14:05:00Z"`} {
		if !strings.Contains(out, name) {
			t.Errorf("ls has no %s:\n%s", name, out)
		}
	}
	out = must(t, "", "ls", "Projects/2026/q4")
	if !strings.Contains(out, "final.txt") || !strings.Contains(out, "293 KB") || !strings.Contains(out, "10/11/2026") {
		t.Errorf("ls of a folder:\n%s", out)
	}

	// To standard output, and to a file.
	if out = must(t, "", "get", "Projects/2026/q4/final.txt", "-o", "-"); out != string(content) {
		t.Error("get to standard output differs")
	}
	target := filepath.Join(dir, "down.txt")
	must(t, "", "get", "Projects/report.txt", "-o", target)
	if got, _ := os.ReadFile(target); !bytes.Equal(got, content) {
		t.Error("get to a file differs")
	}
	if code, _, _ := run("", "get", "Projects/report.txt", "-o", target); code != exit.Usage {
		t.Errorf("existing file: code %d", code)
	}
	must(t, "", "get", "Projects/report.txt", "-o", target, "--force")
	if code, _, _ := run("", "get", "Projects/nothing.txt", "-o", target, "--force"); code != exit.NotFound {
		t.Errorf("missing file: code %d", code)
	}
}

func TestGetContinuesAnInterruptedDownload(t *testing.T) {
	work, _ := world(t)
	dir := t.TempDir()
	content := bytes.Repeat([]byte("0123456789"), 5000)
	out := must(t, string(content), "send", "-", "Projects/big.bin", "--output", "json")
	etag := results(t, out)[0].Result.(map[string]any)["etag"].(string)

	// What an interrupted download leaves: the first part, and its ETag.
	target := filepath.Join(dir, "big.bin")
	os.WriteFile(target+".eds-part", content[:20000], 0o600)
	os.WriteFile(target+".eds-part.etag", []byte(etag+"\n"), 0o600)
	out = must(t, "", "get", "Projects/big.bin", "-o", target, "--output", "json")
	if !strings.Contains(out, `"resumed": true`) {
		t.Errorf("not resumed: %s", out)
	}
	if got, _ := os.ReadFile(target); !bytes.Equal(got, content) {
		t.Error("the continued download differs")
	}
	if _, err := os.Stat(target + ".eds-part.etag"); err == nil {
		t.Error("the ETag file is left behind")
	}

	// The content changed meanwhile: the part is of the old one, so the
	// download starts again.
	changed := bytes.Repeat([]byte("abcdefghij"), 5000)
	must(t, string(changed), "send", "-", "Projects/big.bin", "--replace")
	os.WriteFile(target+".eds-part", content[:20000], 0o600)
	os.WriteFile(target+".eds-part.etag", []byte(etag+"\n"), 0o600)
	out = must(t, "", "get", "Projects/big.bin", "-o", target, "--force", "--output", "json")
	if !strings.Contains(out, `"resumed": false`) {
		t.Errorf("resumed with other content: %s", out)
	}
	if got, _ := os.ReadFile(target); !bytes.Equal(got, changed) {
		t.Error("the restarted download differs")
	}
	_ = work
}

func TestMoveRenameDelete(t *testing.T) {
	work, _ := world(t)
	must(t, "one", "send", "-", "Projects/a.txt")
	must(t, "", "mkdir", "Projects/docs")
	if code, _, _ := run("", "mkdir", "Projects/docs"); code != exit.Usage {
		t.Errorf("mkdir twice: code %d", code)
	}
	if code, _, errOut := run("", "mkdir", "Projects/.secret"); code != exit.Usage || !strings.Contains(errOut, "dot") {
		t.Errorf("hidden name: code %d, %s", code, errOut)
	}
	must(t, "", "mv", "Projects/a.txt", "Projects/docs/")
	if _, ok := work.File("Projects", "docs", "a.txt"); !ok {
		t.Error("not moved into the folder")
	}
	must(t, "", "mv", "Projects/docs/a.txt", "Archive/b.txt")
	if got, ok := work.File("Archive", "b.txt"); !ok || string(got) != "one" {
		t.Error("not moved to the other room under the new name")
	}
	must(t, "", "rename", "Archive/b.txt", "c.txt")
	if code, _, errOut := run("", "mv", "Archive/c.txt", "home:Birgit"); code != exit.Usage || !strings.Contains(errOut, "tolk cp") {
		t.Errorf("mv to another stone: code %d, %s", code, errOut)
	}
	must(t, "two", "send", "-", "Projects/docs/keep.txt")
	if code, _, errOut := run("", "rm", "Projects/docs"); code == exit.OK || !strings.Contains(errOut, "Only empty folders") {
		t.Errorf("rm of a folder with files: code %d, %s", code, errOut)
	}
	must(t, "", "rm", "Projects/docs/keep.txt", "Projects/docs", "Archive/c.txt")
	if _, ok := work.File("Archive", "c.txt"); ok {
		t.Error("not deleted")
	}
}

func TestCopyBetweenStonesAndReadOnly(t *testing.T) {
	work, home := world(t)
	must(t, "saga", "send", "-", "work:Projects/saga.txt", "--type", "text/plain")
	must(t, "", "cp", "work:Projects/saga.txt", "home:Birgit", "home:Birgit/copy.txt", "work:Archive")
	for _, where := range []struct {
		stone *fakestone.Stone
		room  string
		name  string
	}{{home, "Birgit", "saga.txt"}, {home, "Birgit", "copy.txt"}, {work, "Archive", "saga.txt"}} {
		if got, ok := where.stone.File(where.room, where.name); !ok || string(got) != "saga" {
			t.Errorf("%s/%s wasn't copied", where.room, where.name)
		}
	}
	// Where birgit can only read, Tolk refuses early, in Rune's words.
	for _, args := range [][]string{
		{"send", "-", "home:Family/x.txt"},
		{"mkdir", "home:Family/new"},
		{"cp", "work:Projects/saga.txt", "home:Family"},
	} {
		uploads := home.Uploads
		code, _, errOut := run("x", args...)
		if code != exit.NotPermitted || !strings.Contains(errOut, "You can only read in this room.") {
			t.Errorf("tolk %s: code %d, %s", strings.Join(args, " "), code, errOut)
		}
		if home.Uploads != uploads {
			t.Errorf("tolk %s uploaded although it may not", strings.Join(args, " "))
		}
	}
	// One of several targets fails: the others are done, the exit code says so.
	code, out, _ := run("x", "send", "-", "home:Birgit/y.txt", "home:Family/y.txt", "--output", "json")
	got := results(t, out)
	if code != exit.NotPermitted || len(got) != 2 || !got[0].OK || got[1].OK {
		t.Errorf("two targets: code %d, %s", code, out)
	}
	if _, ok := home.File("Birgit", "y.txt"); !ok {
		t.Error("the target that could be written wasn't")
	}
}

func TestGroupsAndAllRooms(t *testing.T) {
	work, home := world(t)
	must(t, "", "group", "set", "announce", "Projects", "home:Birgit")
	if out := must(t, "", "groups"); !strings.Contains(out, "@announce") || !strings.Contains(out, "work:Projects") {
		t.Errorf("groups:\n%s", out)
	}
	must(t, "news", "send", "-", "@announce", "--name", "news.txt")
	if _, ok := work.File("Projects", "news.txt"); !ok {
		t.Error("the group's first room got nothing")
	}
	if _, ok := home.File("Birgit", "news.txt"); !ok {
		t.Error("the group's second room got nothing")
	}
	out := must(t, "", "ls", "work:*")
	if !strings.Contains(out, "work:Projects:") || !strings.Contains(out, "work:Archive:") {
		t.Errorf("every room:\n%s", out)
	}
	must(t, "", "group", "remove", "announce")
	if code, _, _ := run("", "ls", "@announce"); code != exit.NotFound {
		t.Errorf("removed group: code %d", code)
	}
}

func TestInviteAcceptPassphrase(t *testing.T) {
	work, _ := world(t)
	link := strings.TrimSpace(must(t, "", "invite", "jørgen", "--room", "Projects", "--access", "read"))
	if !strings.HasPrefix(link, work.Server.URL+"/#invite=") {
		t.Fatalf("link: %s", link)
	}
	// With --qr the code is drawn on standard error; the link stays alone
	// on standard output.
	code, out, errOut := run("", "invite", "åse", "--room", "Projects", "--qr")
	if code != exit.OK || !strings.Contains(errOut, "▀") || strings.Contains(out, "▀") || strings.Count(strings.TrimSpace(out), "\n") != 0 {
		t.Errorf("qr: code %d\n%s", code, out)
	}
	must(t, "a new passphrase\n", "accept", link, "--stone", "j", "--passphrase-stdin")
	if out := must(t, "", "rooms", "--stone", "j"); !strings.Contains(out, "Projects") || !strings.Contains(out, "Can read") {
		t.Errorf("rooms after accepting:\n%s", out)
	}
	if code, _, errOut := run("again\n", "accept", link, "--stone", "j2", "--passphrase-stdin"); code == exit.OK || !strings.Contains(errOut, "no longer valid") {
		t.Errorf("accepting twice: code %d, %s", code, errOut)
	}
	// Someone with an account is added instead.
	if _, _, errOut := run("", "invite", "birgit", "--room", "Archive"); !strings.Contains(errOut, "already uses Rune") {
		t.Errorf("inviting a user: %s", errOut)
	}
	must(t, "the next passphrase\n", "passphrase", "--stone", "j", "--passphrase-stdin")
	must(t, "", "rooms", "--stone", "j")
}

func TestStoneSettings(t *testing.T) {
	world(t)
	if out := must(t, "", "stone", "settings"); !strings.Contains(out, "eds.session.timeout.minutes") || strings.Contains(out, "eds.is.ready") {
		t.Errorf("settings:\n%s", out)
	}
	if out := must(t, "", "stone", "set", "eds.session.timeout.minutes", "60"); !strings.Contains(out, "60") {
		t.Errorf("set:\n%s", out)
	}
	if code, _, _ := run("", "stone", "set", "eds.crypto.hash.algorithm", "MD5"); code != exit.Usage {
		t.Errorf("a value that isn't allowed: code %d", code)
	}
	if code, _, _ := run("", "stone", "settings", "home"); code != exit.NotPermitted {
		t.Errorf("settings as birgit: code %d", code)
	}
}

// A session that is no longer valid is renewed, and the command goes on.
func TestExpiredSessionIsRenewed(t *testing.T) {
	work, _ := world(t)
	// The agent, in this process: it keeps the session between commands.
	container, err := service.Unlock(nil)
	if err != nil {
		t.Fatal(err)
	}
	listener, err := agent.Listen()
	if err != nil {
		t.Fatal(err)
	}
	server := agent.NewServer(container, 0)
	go server.Serve(listener)
	t.Cleanup(server.Stop)
	must(t, "one", "send", "-", "Projects/one.txt")
	work.ExpireSessions()
	must(t, "two", "send", "-", "Projects/two.txt")
	if got, ok := work.File("Projects", "two.txt"); !ok || string(got) != "two" {
		t.Error("nothing was sent after the session expired")
	}
	if out := must(t, "", "ls", "Projects"); !strings.Contains(out, "two.txt") {
		t.Errorf("ls:\n%s", out)
	}
	if work.Logins() != 1 {
		t.Errorf("%d sessions open, want the one the agent renewed", work.Logins())
	}
}
