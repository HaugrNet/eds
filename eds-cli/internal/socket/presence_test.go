package socket

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/fakestone"
)

// hall is a stone with a room of three, with all features switched on.
type hall struct {
	t     *testing.T
	ctx   context.Context
	stone *fakestone.Stone
	room  string
}

func newHall(t *testing.T) *hall {
	t.Helper()
	stone := fakestone.New("admin passphrase")
	t.Cleanup(stone.Close)
	stone.OfferAll()
	stone.AddUser("birgit", "birgit passphrase")
	stone.AddUser("olaf", "olaf passphrase")
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	t.Cleanup(cancel)
	h := &hall{t: t, ctx: ctx, stone: stone, room: stone.AddRoom("Hall", "admin", "ADMIN", "birgit", "WRITE", "olaf", "READ")}
	for _, feature := range []string{"calls", "plans"} {
		stone.SwitchFeature("Hall", feature, client.FeatureOn)
	}
	return h
}

func (h *hall) login(account string) string {
	h.t.Helper()
	token, err := client.New(h.stone.URL()).Login(h.ctx, account, account+" passphrase")
	if err != nil {
		h.t.Fatal(err)
	}
	return token
}

func (h *hall) dial(account string) *Client {
	h.t.Helper()
	c, err := Dial(h.ctx, h.stone.URL(), h.login(account))
	if err != nil {
		h.t.Fatal(err)
	}
	h.t.Cleanup(c.Close)
	return c
}

// join enters the room, and returns who is present by the stone's answer.
func (h *hall) join(c *Client, unseen bool) string {
	h.t.Helper()
	joined, err := c.Join(h.ctx, h.room, unseen)
	if err != nil {
		h.t.Fatal(err)
	}
	return strings.Join(joined.Present, " ")
}

// event waits for the next thing a connection is told.
func (h *hall) event(c *Client) client.SocketFrame {
	h.t.Helper()
	select {
	case frame, ok := <-c.Events():
		if !ok {
			h.t.Fatalf("the connection ended: %v", c.Err())
		}
		return frame
	case <-time.After(10 * time.Second):
		h.t.Fatal("nothing was told")
	}
	return client.SocketFrame{}
}

// told checks the next thing a connection is told: a member who came or
// went.
func (h *hall) told(c *Client, kind string, who *Client) {
	h.t.Helper()
	if frame := h.event(c); frame.Type != kind || frame.CircleID != h.room || frame.MemberID != who.MemberID || frame.AccountName != who.AccountName {
		h.t.Errorf("want %s of %s, got %+v", kind, who.AccountName, frame)
	}
}

// Who is present in a room: the list when joining, and who comes and goes
// afterwards, by the first and the last shown connection of a member.
func TestPresence(t *testing.T) {
	h := newHall(t)
	admin, birgit := h.dial("admin"), h.dial("birgit")
	if present := h.join(admin, false); present != admin.MemberID {
		t.Errorf("alone in the room: %q", present)
	}
	if present := h.join(birgit, false); present != admin.MemberID+" "+birgit.MemberID {
		t.Errorf("two in the room: %q", present)
	}
	h.told(admin, client.SocketPresent, birgit)

	// A second connection of the same member changes nothing, coming or
	// going: olaf, who comes after it, is the next the others are told of.
	second := h.dial("birgit")
	h.join(second, false)
	if err := second.Leave(h.ctx, h.room); err != nil {
		t.Fatal(err)
	}
	olaf := h.dial("olaf")
	if present := h.join(olaf, false); present != strings.Join([]string{admin.MemberID, birgit.MemberID, olaf.MemberID}, " ") {
		t.Errorf("three in the room: %q", present)
	}
	h.told(admin, client.SocketPresent, olaf)
	h.told(birgit, client.SocketPresent, olaf)

	// Leaving, and a connection which closes.
	if err := olaf.Leave(h.ctx, h.room); err != nil {
		t.Fatal(err)
	}
	h.told(admin, client.SocketAbsent, olaf)
	h.told(birgit, client.SocketAbsent, olaf)
	birgit.Close()
	h.told(admin, client.SocketAbsent, birgit)

	// Access taken away: the member is gone for the others, and is told so.
	h.join(olaf, false)
	h.told(admin, client.SocketPresent, olaf)
	if err := client.New(h.stone.URL()).RemoveTrustee(h.ctx, h.login("admin"), h.room, olaf.MemberID); err != nil {
		t.Fatal(err)
	}
	h.told(admin, client.SocketAbsent, olaf)
	if frame := h.event(olaf); frame.Type != client.SocketLeft {
		t.Errorf("olaf was told %+v", frame)
	}
	if present := strings.Join(h.stone.Present("Hall"), " "); present != "admin" {
		t.Errorf("present at the end: %q", present)
	}
}

// An unseen connection learns who is present, and its member isn't shown.
// Joining again with the other choice changes that.
func TestPresenceUnseen(t *testing.T) {
	h := newHall(t)
	admin, script := h.dial("admin"), h.dial("birgit")
	h.join(admin, false)
	if present := h.join(script, true); present != admin.MemberID {
		t.Errorf("an unseen connection is told %q", present)
	}
	if present := strings.Join(h.stone.Present("Hall"), " "); present != "admin" {
		t.Errorf("present with an unseen connection: %q", present)
	}
	// It is told who comes: olaf.
	olaf := h.dial("olaf")
	h.join(olaf, false)
	h.told(script, client.SocketPresent, olaf)
	h.told(admin, client.SocketPresent, olaf)
	// Shown now, and unseen again: the others are told each time, and
	// nothing was told of the unseen join before.
	if present := h.join(script, false); !strings.Contains(present, script.MemberID) {
		t.Errorf("shown now: %q", present)
	}
	h.told(admin, client.SocketPresent, script)
	h.join(script, true)
	h.told(admin, client.SocketAbsent, script)
	// An unseen connection which closes tells nothing: olaf's leaving is next.
	script.Close()
	if err := olaf.Leave(h.ctx, h.room); err != nil {
		t.Fatal(err)
	}
	h.told(admin, client.SocketAbsent, olaf)
}

// A signal to one member reaches their connections which joined, shown or
// unseen, and nobody else; never the connection which sent it.
func TestSignalToOneMember(t *testing.T) {
	h := newHall(t)
	admin, desk, phone, olaf := h.dial("admin"), h.dial("birgit"), h.dial("birgit"), h.dial("olaf")
	h.join(admin, true)
	h.join(desk, true)
	h.join(phone, true)
	h.join(olaf, true)
	signalled := func(c *Client, signal, to string) {
		t.Helper()
		if frame := h.event(c); frame.Type != client.SocketSignal || frame.Feature != "calls" || frame.Signal != signal || frame.To != to || frame.MemberID == "" {
			t.Errorf("want the signal %q to %q, got %+v", signal, to, frame)
		}
	}
	// The calls declare signals with no least time between two: two at once.
	admin.SignalTo(h.room, "calls", "for birgit", desk.MemberID)
	admin.SignalTo(h.room, "calls", "again", desk.MemberID)
	for _, c := range []*Client{desk, phone} {
		signalled(c, "for birgit", desk.MemberID)
		signalled(c, "again", desk.MemberID)
	}
	// To the own member: the other device gets it, the sender doesn't.
	desk.SignalTo(h.room, "calls", "taken", desk.MemberID)
	signalled(phone, "taken", desk.MemberID)
	// To everyone: olaf and the sender's own next frames show that neither
	// got what was for one member. A feature without signals passes none.
	admin.Signal(h.room, "plans", "not declared")
	admin.Signal(h.room, "calls", "for all")
	for _, c := range []*Client{desk, phone, olaf} {
		signalled(c, "for all", "")
	}
	olaf.Signal(h.room, "calls", "from olaf")
	signalled(admin, "from olaf", "")
}
