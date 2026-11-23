package commands

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/fakestone"
	"github.com/HaugrNet/eds/eds-cli/internal/socket"
)

// present waits until the accounts shown as online in a room are these.
func present(t *testing.T, stone *fakestone.Stone, room, want string) {
	t.Helper()
	for deadline := time.Now().Add(10 * time.Second); ; time.Sleep(10 * time.Millisecond) {
		got := strings.Join(stone.Present(room), " ")
		if got == want {
			return
		}
		if time.Now().After(deadline) {
			t.Fatalf("online in %s: %q, want %q", room, got, want)
		}
	}
}

// 'tolk members' says who is online, and isn't shown as online itself.
func TestMembersOnline(t *testing.T) {
	work, _ := world(t)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	// birgit has Rune open: a connection which is shown in the room.
	token, err := client.New(work.URL()).Login(ctx, "birgit", "birgit secret")
	if err != nil {
		t.Fatal(err)
	}
	browser, err := socket.Dial(ctx, work.URL(), token)
	if err != nil {
		t.Fatal(err)
	}
	defer browser.Close()
	if _, err := browser.Join(ctx, work.RoomID("Projects"), false); err != nil {
		t.Fatal(err)
	}
	lines := map[string]string{}
	for _, line := range strings.Split(must(t, "", "members", "--room", "Projects"), "\n") {
		if name, _, ok := strings.Cut(line, " "); ok {
			lines[name] = line
		}
	}
	if !strings.HasSuffix(lines["birgit"], "  online") || strings.Contains(lines["admin"], "online") || lines["admin"] != strings.TrimRight(lines["admin"], " ") {
		t.Errorf("members: %q", lines)
	}
	if out := must(t, "", "--lang", "de", "members", "--room", "Projects"); !strings.Contains(out, "online") {
		t.Errorf("members in German:\n%s", out)
	}
	online := map[string]any{}
	for _, m := range result(t, must(t, "", "members", "--room", "Projects", "--output", "json"))["members"].([]any) {
		member := m.(map[string]any)
		online[member["name"].(string)] = member["online"]
	}
	if online["birgit"] != true || online["admin"] != false {
		t.Errorf("members as JSON: %v", online)
	}
	// The command joined unseen and left: nobody saw admin come or go.
	present(t, work, "Projects", "birgit")
	select {
	case frame := <-browser.Events():
		t.Errorf("birgit was told of the command: %+v", frame)
	default:
	}
	// Other commands on the socket aren't shown either.
	must(t, "", "chat", "--plain", "--room", "Projects", "-m", "from a script")
	must(t, "", "features", "on", "location", "--room", "Projects")
	must(t, "", "location", "report", "52.52", "13.405", "--room", "Projects", "--passing")
	present(t, work, "Projects", "birgit")
	for len(browser.Events()) > 0 {
		if frame := <-browser.Events(); frame.Type == client.SocketPresent || frame.Type == client.SocketAbsent {
			t.Errorf("birgit was told that a command came or went: %+v", frame)
		}
	}
	browser.Close()
	present(t, work, "Projects", "")
	if out := must(t, "", "members", "--room", "Projects"); strings.Contains(out, "online") {
		t.Errorf("nobody is online:\n%s", out)
	}
}

// The feature "calls" by its name, and events of the calendar which are
// calls.
func TestCallsFeatureAndCalendar(t *testing.T) {
	world(t)
	must(t, "", "stone", "features", "offer", "all")
	if out := must(t, "", "stone", "features"); !strings.Contains(out, "Calls") {
		t.Errorf("the features of the stone:\n%s", out)
	}
	must(t, "", "features", "on", "calls", "--room", "Projects")
	if out := must(t, "", "--lang", "de", "features", "--room", "Projects"); !strings.Contains(out, "Anrufe") {
		t.Errorf("the features of the room, in German:\n%s", out)
	}

	must(t, "", "features", "on", "calendar", "--room", "Projects")
	added := result(t, must(t, "", "calendar", "add", "Stand-up", "--room", "Projects", "--start", "2026-12-04 09:00", "--zone", "Europe/Berlin", "--call", "video", "--output", "json"))
	standup, _ := added["id"].(string)
	if event, _ := added["event"].(map[string]any); event["call"] != "video" {
		t.Errorf("added: %v", added)
	}
	must(t, "", "calendar", "add", "Dinner", "--room", "Projects", "--start", "2026-12-04 19:00", "--zone", "Europe/Berlin")
	list := func(args ...string) string {
		return must(t, "", append([]string{"calendar", "--room", "Projects", "--from", "2026-12-04", "--to", "2026-12-04"}, args...)...)
	}
	marked := func(out, title string) string {
		for _, line := range strings.Split(out, "\n") {
			if strings.Contains(line, title) {
				return line
			}
		}
		return ""
	}
	if out := list(); !strings.Contains(marked(out, "Stand-up"), "[Video call]") || strings.Contains(marked(out, "Dinner"), "call") {
		t.Errorf("the list:\n%s", out)
	}
	if out := list("--lang", "de"); !strings.Contains(marked(out, "Stand-up"), "[Videoanruf]") {
		t.Errorf("the list in German:\n%s", out)
	}
	calls := map[string]any{}
	for _, e := range result(t, list("--output", "json"))["events"].([]any) {
		event := e.(map[string]any)
		calls[event["title"].(string)] = event["call"]
	}
	if calls["Stand-up"] != "video" || calls["Dinner"] != "" {
		t.Errorf("the list as JSON: %v", calls)
	}
	// Out as .ics with its line, changed, and in again as it was.
	file := filepath.Join(t.TempDir(), "projects.ics")
	must(t, "", "calendar", "export", "--room", "Projects", "-o", file)
	if ics, _ := os.ReadFile(file); strings.Count(string(ics), "X-EDS-CALL:video\r\n") != 1 {
		t.Errorf("the .ics: %s", ics)
	}
	must(t, "", "calendar", "change", standup, "--room", "Projects", "--call", "audio")
	if out := list(); !strings.Contains(marked(out, "Stand-up"), "[Voice call]") {
		t.Errorf("after the change to voice:\n%s", out)
	}
	must(t, "", "calendar", "change", standup, "--room", "Projects", "--call", "none")
	if out := list(); strings.Contains(out, "call") {
		t.Errorf("after the change to no call:\n%s", out)
	}
	must(t, "", "calendar", "import", file, "--room", "Projects")
	if out := list(); !strings.Contains(marked(out, "Stand-up"), "[Video call]") {
		t.Errorf("after bringing the .ics in:\n%s", out)
	}
	if code, _, errOut := run("", "calendar", "change", standup, "--room", "Projects", "--call", "hologram"); code == 0 || !strings.Contains(errOut, "audio, video or none") {
		t.Errorf("a kind of call nobody knows: code %d, %s", code, errOut)
	}
}
