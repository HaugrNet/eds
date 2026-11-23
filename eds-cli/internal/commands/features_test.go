package commands

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/e2e"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// result decodes the result of a command for one room.
func result(t *testing.T, out string) map[string]any {
	t.Helper()
	list := results(t, out)
	if len(list) != 1 || !list[0].OK {
		t.Fatalf("the result: %s", out)
	}
	value, _ := list[0].Result.(map[string]any)
	return value
}

// The features of a stone and of a room, with Rune's words.
func TestFeaturesOfStoneAndRoom(t *testing.T) {
	work, _ := world(t)
	out := must(t, "", "stone", "features")
	for _, word := range []string{"Chat", "Calendar", "Plans", "Location", "offered, version 1"} {
		if !strings.Contains(out, word) {
			t.Errorf("the features of the stone lack %q:\n%s", word, out)
		}
	}
	must(t, "", "stone", "features", "withdraw", "location")
	if out := must(t, "", "stone", "features"); !strings.Contains(out, "not offered") {
		t.Errorf("after withdrawing: %s", out)
	}
	// A feature the stone doesn't offer is offered by its System Administrator
	// when it is first switched on.
	must(t, "", "features", "on", "location", "--room", "work:Projects")
	if out := must(t, "", "features", "--room", "work:Projects"); !strings.Contains(out, "Location") || !strings.Contains(out, "Chat") {
		t.Errorf("the features of the room: %s", out)
	}
	must(t, "", "features", "readonly", "chat", "--room", "work:Projects")
	if code, _, errOut := run("hello\n", "chat", "--plain", "--room", "work:Projects"); code == exit.OK || !strings.Contains(errOut, "closed") {
		t.Errorf("a closed chat: code %d, %s", code, errOut)
	}
	must(t, "", "features", "on", "chat", "--room", "work:Projects")
	must(t, "", "chat", "--plain", "--room", "work:Projects", "-m", "kept?")
	must(t, "", "features", "remove", "chat", "--room", "work:Projects", "--yes")
	if len(work.Messages("Projects")) != 0 {
		t.Error("removing a feature kept its entries")
	}
	if code, _, _ := run("", "features", "on", "nothing", "--room", "work:Projects"); code == exit.OK {
		t.Error("a feature nobody knows was switched on")
	}
	// Who isn't the System Administrator can't offer, and is told who can.
	must(t, "birgit secret\n", "register", "stone", "birgit", "--url", work.URL(), "--account", "birgit", "--passphrase-stdin")
	if code, _, _ := run("", "stone", "features", "offer", "chat", "--stone", "birgit"); code == exit.OK {
		t.Error("someone else offered a feature")
	}
}

// The calendar, the plans and the location from the command line, by two
// people in a chamber: what Rune shows in its panes.
func TestFeaturesInAChamber(t *testing.T) {
	work, _ := world(t)
	dir := t.TempDir()
	must(t, "", "room", "new", "work:Vault", "--chamber", "--features", "chat,calendar,plans,location")
	must(t, "birgit secret\n", "register", "stone", "birgit", "--url", work.URL(), "--account", "birgit", "--passphrase-stdin")
	must(t, "", "key", "create", "birgit")
	must(t, "", "add", "birgit", "--room", "work:Vault", "--access", "write")

	// The calendar: an event, a repeating one, seen by the other.
	added := result(t, must(t, "", "calendar", "add", "Dinner", "--room", "work:Vault", "--start", "2026-12-04 19:00", "--end", "2026-12-04 21:00", "--place", "Hearth", "--zone", "Europe/Berlin", "--output", "json"))
	dinner, _ := added["id"].(string)
	must(t, "", "calendar", "add", "Choir", "--room", "birgit:Vault", "--start", "2026-11-02 18:00", "--repeat", "weekly", "--zone", "Europe/Berlin")
	listed := result(t, must(t, "", "calendar", "--room", "birgit:Vault", "--from", "2026-12-01", "--to", "2026-12-07", "--output", "json"))
	var seen []string
	for _, e := range listed["events"].([]any) {
		event := e.(map[string]any)
		seen = append(seen, event["title"].(string)+" "+event["start"].(string))
	}
	if strings.Join(seen, "; ") != "Dinner 2026-12-04T18:00:00Z; Choir 2026-12-07T17:00:00Z" {
		t.Errorf("birgit sees %v", seen)
	}
	for _, stored := range work.Stored("Vault", "calendar", "event") {
		if !e2e.IsProtected(stored) {
			t.Errorf("the stone can read an event: %q", stored)
		}
	}
	// birgit moves the dinner; in admin's list it is on the next day.
	must(t, "", "calendar", "change", dinner, "--room", "birgit:Vault", "--start", "2026-12-05 19:00")
	if out := must(t, "", "calendar", "--room", "work:Vault", "--from", "2026-12-05", "--to", "2026-12-05"); !strings.Contains(out, "Dinner") || !strings.Contains(out, "Hearth") {
		t.Errorf("after the change: %s", out)
	}
	// Out as .ics and in again: nothing doubles.
	file := filepath.Join(dir, "vault.ics")
	must(t, "", "calendar", "export", "--room", "work:Vault", "-o", file)
	if ics, _ := os.ReadFile(file); !strings.Contains(string(ics), "SUMMARY:Dinner") || !strings.Contains(string(ics), "RRULE:FREQ=WEEKLY") {
		t.Errorf("the .ics: %s", ics)
	}
	must(t, "", "calendar", "import", file, "--room", "work:Vault")
	if again := result(t, must(t, "", "entry", "list", "calendar", "--room", "work:Vault", "--output", "json")); len(again["entries"].([]any)) != 2 {
		t.Errorf("after bringing the same events in: %v", again)
	}
	must(t, "", "calendar", "remove", dinner, "--room", "work:Vault")

	// A plan: costs shared by three, a correction, payments, a confirmation.
	plan := result(t, must(t, "", "plan", "new", "Weekend", "--room", "work:Vault", "--from", "2026-12-04", "--to", "2026-12-06", "--people", "admin,birgit,Dana", "--output", "json"))
	planID, _ := plan["id"].(string)
	must(t, "", "plan", "item", "Weekend", "Book the cabin", "--room", "work:Vault", "--who", "birgit")
	cabin := result(t, must(t, "", "plan", "expense", "Weekend", "90", "Cabin", "--room", "birgit:Vault", "--output", "json"))
	must(t, "", "plan", "expense", "Weekend", "30.00", "Food", "--room", "work:Vault", "--split", "admin,birgit")
	dues := func(stone string) string {
		shown := result(t, must(t, "", "plan", "show", planID, "--room", stone+":Vault", "--output", "json"))
		var list []string
		for _, due := range shown["settle"].(map[string]any)["EUR"].([]any) {
			d := due.(map[string]any)
			list = append(list, d["from"].(string)+" pays "+d["to"].(string)+" "+d["amount"].(string))
		}
		return strings.Join(list, "; ")
	}
	if got := dues("work"); got != "Dana (guest) pays birgit 30.00; admin pays birgit 15.00" {
		t.Errorf("who owes whom: %s", got)
	}
	if dues("birgit") != dues("work") {
		t.Error("the two work out different things")
	}
	if code, _, _ := run("", "plan", "correct", "Weekend", cabin["id"].(string), "--amount", "1", "--room", "work:Vault"); code == exit.OK {
		t.Error("an expense was corrected by someone else")
	}
	must(t, "", "plan", "correct", "Weekend", cabin["id"].(string), "--amount", "120", "--room", "birgit:Vault")
	if got := dues("work"); got != "Dana (guest) pays birgit 40.00; admin pays birgit 25.00" {
		t.Errorf("after the correction: %s", got)
	}
	paid := result(t, must(t, "", "plan", "pay", "Weekend", "25", "birgit", "--room", "work:Vault", "--output", "json"))
	must(t, "", "plan", "pay", "Weekend", "40", "birgit", "--paid-by", "Dana", "--room", "birgit:Vault")
	must(t, "", "plan", "confirm", "Weekend", paid["id"].(string), "--room", "birgit:Vault")
	shown := must(t, "", "plan", "show", "Weekend", "--room", "work:Vault")
	for _, word := range []string{"Book the cabin", "Cabin", "settled, nobody owes anything", "admin paid birgit", "confirmed", "Dana (guest) paid birgit"} {
		if !strings.Contains(shown, word) {
			t.Errorf("the plan lacks %q:\n%s", word, shown)
		}
	}
	must(t, "", "plan", "close", "Weekend", "--room", "work:Vault")
	if code, _, _ := run("", "plan", "item", "Weekend", "too late", "--room", "birgit:Vault"); code == exit.OK {
		t.Error("something was added to a closed plan")
	}
	for _, stored := range work.Stored("Vault", "plans", "expense") {
		if !e2e.IsProtected(stored) {
			t.Errorf("the stone can read an expense: %q", stored)
		}
	}

	// Where people are: reported by one, shown to the other; one position
	// per member is kept.
	must(t, "", "location", "report", "52.52", "13.405", "--room", "birgit:Vault", "--accuracy", "12")
	must(t, "", "location", "report", "53.5511", "9.9937", "--room", "birgit:Vault", "--track", "--note", "at the harbour")
	must(t, "", "location", "report", "52.52", "13.405", "--room", "work:Vault")
	where := result(t, must(t, "", "location", "--room", "work:Vault", "--output", "json"))
	persons := where["people"].([]any)
	if len(persons) != 2 {
		t.Fatalf("who was seen: %v", where)
	}
	for _, p := range persons {
		person := p.(map[string]any)
		if person["name"] == "birgit" {
			position := person["position"].(map[string]any)
			if position["lat"] != 53.5511 || person["geo"] != "geo:53.5511,9.9937" || person["distance"].(float64) < 250000 || len(person["track"].([]any)) != 1 {
				t.Errorf("birgit is seen as %v", person)
			}
		}
	}
	if out := must(t, "", "location", "--room", "work:Vault"); !strings.Contains(out, "km from you") || !strings.Contains(out, "at the harbour") {
		t.Errorf("as a person reads it: %s", out)
	}
	must(t, "", "location", "report", "1", "2", "--room", "birgit:Vault", "--passing")
	must(t, "", "location", "remove", "--room", "birgit:Vault")
	if left := result(t, must(t, "", "location", "--room", "work:Vault", "--output", "json")); len(left["people"].([]any)) != 1 {
		t.Errorf("after birgit removed hers: %v", left)
	}
	if code, _, _ := run("", "location", "report", "95", "0", "--room", "work:Vault"); code != exit.Usage {
		t.Error("a place which isn't one was reported")
	}

	// The general command, for a feature Tolk needn't know: the content is
	// JSON as its people read it, and encrypted on the stone.
	entry := result(t, must(t, "", "entry", "add", "plans", "plan", "--room", "work:Vault", "--content", `{"v":1,"title":"By a script","currency":"EUR","people":[]}`, "--output", "json"))
	if content, _ := json.Marshal(entry["content"]); !strings.Contains(string(content), "By a script") {
		t.Errorf("the entry: %v", entry)
	}
	list := result(t, must(t, "", "entry", "list", "plans", "--kind", "plan", "--room", "birgit:Vault", "--output", "json"))
	if len(list["entries"].([]any)) != 2 {
		t.Errorf("the plans as entries: %v", list)
	}
	must(t, `{"v":1,"title":"Changed by a script","currency":"EUR","people":[]}`, "entry", "change", "plans", entry["id"].(string), "--room", "work:Vault")
	if out := must(t, "", "plan", "--room", "birgit:Vault"); !strings.Contains(out, "Changed by a script") || !strings.Contains(out, "Closed") {
		t.Errorf("the plans: %s", out)
	}
	must(t, "", "entry", "remove", "plans", entry["id"].(string), "--room", "work:Vault")
}
