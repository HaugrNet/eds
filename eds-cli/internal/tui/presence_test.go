package tui

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/fakestone"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/socket"
)

// asItIs is the sealer of a room: the content goes as it is.
type asItIs struct{}

func (asItIs) Protect(_ context.Context, _, _ string, data []byte) ([]byte, error) { return data, nil }
func (asItIs) Reveal(_ context.Context, _, _ string, data []byte) ([]byte, error)  { return data, nil }

// birgitsRune opens a connection as birgit's Rune would: shown in Projects.
func birgitsRune(t *testing.T, stone *fakestone.Stone) *socket.Client {
	t.Helper()
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	token, err := newClient(stone.URL()).Login(ctx, "birgit", "birgit secret")
	if err != nil {
		t.Fatal(err)
	}
	conn, err := socket.Dial(ctx, stone.URL(), token)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(conn.Close)
	if _, err := conn.Join(ctx, stone.RoomID("Projects"), false); err != nil {
		t.Fatal(err)
	}
	return conn
}

// membersOfProjects opens the members of Projects in the rooms pane.
func membersOfProjects(d *driver) {
	d.t.Helper()
	for d.m.current().room == nil || d.m.current().room.Name != "Projects" {
		d.keys("down")
	}
	d.keys("right")
	d.until("the members", func() bool { return len(d.m.members[d.m.current().room.ID]) == 2 })
}

// Who is online has a dot in the rooms pane, which follows who comes and
// goes; the interactive mode is shown as online itself; and when someone
// calls, the notes say so.
func TestOnlineAndCalls(t *testing.T) {
	d, stone := world(t, "admin", "admin secret", 120, 34)
	t.Cleanup(func() { i18n.Set("en") })
	stone.OfferAll()
	stone.SwitchFeature("Projects", "calls", client.FeatureOn)
	d.start(d.m.connect())
	d.until("the panes", func() bool { return d.m.phase == phaseMain })
	if present := strings.Join(stone.Present("Projects"), " "); present != "admin" {
		t.Errorf("the interactive mode is open, and online in Projects are: %q", present)
	}
	membersOfProjects(d)
	d.shows("│    ● admin  ", "│      birgit  ", "│● admin  ", "│  birgit ")

	// birgit opens Rune, and leaves it again.
	browser := birgitsRune(t, stone)
	d.until("birgit online", func() bool { return strings.Contains(d.screen(), "│    ● birgit  ") })
	d.shows("│● birgit ", "│  olaf ")

	// She starts the call of the room, and then calls admin alone.
	room, ctx := stone.RoomID("Projects"), context.Background()
	ring := func(message features.CallMessage, to string) {
		t.Helper()
		signal, err := features.CallSignal(ctx, asItIs{}, room, message)
		if err != nil {
			t.Fatal(err)
		}
		browser.SignalTo(room, "calls", signal, to)
	}
	ring(features.CallMessage{T: "ring", Call: features.RoomCall, Media: "video"}, "")
	d.until("the call of the room", func() bool {
		return strings.Contains(d.screen(), "birgit started a call in Projects. Calls are made with Rune.")
	})
	ring(features.CallMessage{T: "ring", Call: "11111111-1111-4111-8111-111111111111", Media: "audio"}, d.m.chat.MemberID)
	d.until("the direct call", func() bool { return strings.Contains(d.screen(), "birgit is calling you. Calls are made with Rune.") })
	d.m.ui.Language = "de"
	d.m.restyle()
	ring(features.CallMessage{T: "ring", Call: features.RoomCall, Media: "audio"}, "")
	d.until("the call of the room, in German", func() bool {
		return strings.Contains(d.screen(), "birgit hat in Projects einen Anruf begonnen. Anrufe führt man mit Rune.")
	})
	d.m.ui.Language = "en"
	d.m.restyle()

	browser.Close()
	d.until("birgit gone", func() bool { return !strings.Contains(d.screen(), "● birgit") })
	d.shows("│    ● admin  ", "│      birgit  ", "│  birgit ")

	// Without the stone nobody is known to be online.
	me := d.m.chat.MemberID
	stone.EndChats(1008)
	d.until("the notice", func() bool { return d.m.phase == phaseFailed })
	if d.m.online(room, me) || strings.Contains(d.screen(), "● admin") {
		t.Errorf("the connection is gone, and someone is marked as online:\n%s", d.screen())
	}
}

// With the setting "present" off, the interactive mode isn't shown as
// online, and still marks who is.
func TestNotShownAsOnline(t *testing.T) {
	d, stone := world(t, "admin", "admin secret", 120, 34)
	d.m.ui.Present = false
	d.start(d.m.connect())
	d.until("the panes", func() bool { return d.m.phase == phaseMain })
	if present, listeners := stone.Present("Projects"), stone.Listeners("Projects"); len(present) != 0 || listeners != 1 {
		t.Errorf("unseen: online are %q, and %d connections joined", present, listeners)
	}
	membersOfProjects(d)
	birgitsRune(t, stone)
	d.until("birgit online", func() bool { return strings.Contains(d.screen(), "│    ● birgit  ") })
	d.shows("│● birgit ")
	d.hides("● admin")
}
