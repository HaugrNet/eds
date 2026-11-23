package commands

import (
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// The status of stones (STATUS.md): by the System Administrator's session,
// and by a status key for a stone which is only looked after.
func TestStatusOfStones(t *testing.T) {
	work, _ := world(t)
	must(t, "", "stone", "name", "Work stone")
	if out := strings.TrimSpace(must(t, "", "stone", "name")); out != "Work stone" {
		t.Errorf("the name of the stone: %q", out)
	}
	created := object(t, must(t, "", "stone", "key", "create", "ops", "--output", "json"))
	key, _ := created["key"].(string)
	if key == "" || created["url"] != work.URL() {
		t.Fatalf("the status key: %v", created)
	}
	if out := must(t, "", "stone", "key"); strings.TrimSpace(out) != "ops" {
		t.Errorf("the status keys: %q", out)
	}
	// A stone which is only looked after: no account, only the key.
	must(t, key+"\n", "register", "stone", "watch", "--url", work.URL(), "--status-key-stdin")
	if code, _, errOut := run("", "rooms", "--stone", "watch"); code == exit.OK || !strings.Contains(errOut, "status key only") {
		t.Errorf("a stone with a status key only: code %d, %s", code, errOut)
	}
	if code, _, _ := run("wrong key\n", "register", "stone", "wrong", "--url", work.URL(), "--status-key-stdin"); code == exit.OK {
		t.Error("a stone was registered with a key it refuses")
	}

	work.Load(50, 0.42)
	code, out, _ := run("", "status", "--output", "json")
	stones := map[string]map[string]any{}
	for _, s := range object(t, out)["stones"].([]any) {
		stone := s.(map[string]any)
		stones[stone["stone"].(string)] = stone
	}
	// home is used by birgit, who isn't its System Administrator.
	if code != exit.Network || len(stones) != 3 || stones["home"]["problem"] == nil {
		t.Errorf("the status of all: code %d, %s", code, out)
	}
	for _, name := range []string{"work", "watch"} {
		stone := stones[name]
		load, _ := stone["load"].(map[string]any)
		inventory, _ := stone["inventory"].(map[string]any)
		if stone["name"] != "Work stone" || stone["awake"] != true || load["processShare"] != 0.42 || load["requests"] != 50.0 || inventory["circles"] != 2.0 {
			t.Errorf("%s: %v", name, stone)
		}
	}
	table := must(t, "", "status", "work", "watch")
	if !strings.Contains(table, "Work stone (watch)") || !strings.Contains(table, "42 %") || !strings.Contains(table, "10.0") {
		t.Errorf("the table:\n%s", table)
	}
	// The live view, for as many rounds as told.
	live := must(t, "", "status", "watch", "--watch", "--count", "2", "--interval", "1")
	for _, word := range []string{"Work stone (watch)", "awake", "Storage", "Requests per second", "Stone's processors", "42 %", "▁", "Memory"} {
		if !strings.Contains(live, word) {
			t.Errorf("the live view lacks %q:\n%s", word, live)
		}
	}
	if strings.Count(live, "The stones, asked every 1 s") != 2 {
		t.Errorf("the live view was drawn %d times", strings.Count(live, "The stones, asked every"))
	}

	// A stone which sleeps, a key which was withdrawn, a stone which is gone.
	work.Asleep(true)
	if out := must(t, "", "status", "watch"); !strings.Contains(out, "asleep") {
		t.Errorf("a stone which sleeps: %s", out)
	}
	must(t, "", "stone", "key", "withdraw", "ops")
	if code, out, _ := run("", "status", "watch"); code != exit.Network || !strings.Contains(out, "refuses") {
		t.Errorf("a withdrawn key: code %d, %s", code, out)
	}
	work.Close()
	if code, out, _ := run("", "status", "watch"); code != exit.Network || !strings.Contains(out, "does not answer") {
		t.Errorf("a stone which is gone: code %d, %s", code, out)
	}
}
