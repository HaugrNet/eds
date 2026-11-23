package commands

import (
	"bufio"
	"bytes"
	"encoding/json"
	"io"
	"strings"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// events decodes the JSON lines of 'tolk chat --plain'.
func events(t *testing.T, out string) []chatEvent {
	t.Helper()
	var list []chatEvent
	for _, line := range strings.Split(strings.TrimSpace(out), "\n") {
		if line == "" {
			continue
		}
		var event chatEvent
		if err := json.Unmarshal([]byte(line), &event); err != nil {
			t.Fatalf("%v in %q", err, line)
		}
		list = append(list, event)
	}
	return list
}

func TestChatSendsLinesToSeveralRooms(t *testing.T) {
	work, home := world(t)
	out := must(t, "Deploy done\n\nSecond line\n", "chat", "--plain", "--room", "work:Projects", "--room", "home:Birgit")
	got := events(t, out)
	if len(got) != 4 {
		t.Fatalf("%d events: %s", len(got), out)
	}
	for _, event := range got {
		if event.Event != "sent" || event.Text == nil || event.ID == "" || event.Time == "" {
			t.Errorf("event: %+v", event)
		}
	}
	if got[0].Stone != "work" || got[0].Room != "Projects" || got[0].From != "admin" || got[1].Stone != "home" || got[1].From != "birgit" {
		t.Errorf("who and where: %+v %+v", got[0], got[1])
	}
	if texts := work.Messages("Projects"); len(texts) != 2 || texts[0] != "Deploy done" || texts[1] != "Second line" {
		t.Errorf("work: %v", texts)
	}
	if texts := home.Messages("Birgit"); len(texts) != 2 {
		t.Errorf("home: %v", texts)
	}

	// One text with -m, and the last messages with --history.
	out = must(t, "", "chat", "--plain", "--room", "Projects", "-m", "Third", "--history", "2")
	got = events(t, out)
	if len(got) != 3 || !got[0].History || *got[0].Text != "Deploy done" || *got[1].Text != "Second line" || got[2].Event != "sent" {
		t.Errorf("history and -m: %s", out)
	}
}

func TestChatReadOnlyAndUnknownRooms(t *testing.T) {
	_, home := world(t)
	// Where the account can only read, nothing is sent: Rune's words, and
	// exit code 6. The other room still gets the line.
	code, out, errOut := run("hello\n", "chat", "--plain", "--room", "home:Family", "--room", "home:Birgit")
	if code != exit.NotPermitted || !strings.Contains(errOut, "You can only read in this room.") {
		t.Errorf("read-only: code %d, %s", code, errOut)
	}
	if got := events(t, out); len(got) != 1 || got[0].Room != "Birgit" {
		t.Errorf("events: %s", out)
	}
	if texts := home.Messages("Family"); len(texts) != 0 {
		t.Errorf("sent to the read-only room: %v", texts)
	}
	if code, _, _ := run("x\n", "chat", "--plain", "--room", "home:Nowhere"); code != exit.NotFound {
		t.Errorf("unknown room: code %d", code)
	}
	if code, _, errOut := run("", "chat", "--room", "home:Birgit"); code != exit.Usage || !strings.Contains(errOut, "--plain") {
		t.Errorf("without --plain: code %d, %s", code, errOut)
	}
}

// With --listen, what others do in the room arrives as events: messages,
// replies, edits, reactions, deletions.
func TestChatListens(t *testing.T) {
	work, _ := world(t)
	input, feed := io.Pipe()
	output, sink := io.Pipe()
	var errOut bytes.Buffer
	done := make(chan int, 1)
	go func() {
		done <- Run([]string{"chat", "--plain", "--listen", "--room", "Projects"}, &Env{In: input, Out: sink, Err: &errOut})
		sink.Close()
	}()
	lines := bufio.NewScanner(output)
	next := func() chatEvent {
		t.Helper()
		got := make(chan chatEvent, 1)
		go func() {
			if lines.Scan() {
				var event chatEvent
				_ = json.Unmarshal(lines.Bytes(), &event)
				got <- event
			} else {
				got <- chatEvent{Event: "end of output"}
			}
		}()
		select {
		case event := <-got:
			return event
		case <-time.After(10 * time.Second):
			t.Fatalf("no event; standard error: %s", errOut.String())
			return chatEvent{}
		}
	}
	// The command has joined once its own line comes back.
	feed.Write([]byte("here\n"))
	if event := next(); event.Event != "sent" || *event.Text != "here" {
		t.Fatalf("own line: %+v", event)
	}

	id := work.Say("Projects", "birgit", "hei")
	if event := next(); event.Event != "message" || event.From != "birgit" || *event.Text != "hei" || event.ID != id {
		t.Errorf("message: %+v", event)
	}
	work.Edit("Projects", id, "hei!")
	if event := next(); event.Event != "edited" || *event.Text != "hei!" || event.Edited == "" {
		t.Errorf("edited: %+v", event)
	}
	work.React("Projects", id, "admin", "👍")
	if event := next(); event.Event != "reaction" || len(event.Reactions["👍"]) != 1 {
		t.Errorf("reaction: %+v", event)
	}
	// What is about a message goes with it, and first: its reactions.
	work.Delete("Projects", id)
	if event := next(); event.Event != "reaction" || len(event.Reactions) != 0 {
		t.Errorf("the reactions of a deleted message: %+v", event)
	}
	if event := next(); event.Event != "deleted" || event.ID != id {
		t.Errorf("deleted: %+v", event)
	}

	// The input ends, but --listen goes on, until the stone ends the chat.
	feed.Close()
	work.Say("Projects", "birgit", "still there?")
	if event := next(); event.Event != "message" || *event.Text != "still there?" {
		t.Errorf("after the input ended: %+v", event)
	}
	work.EndChats(1008)
	select {
	case code := <-done:
		if code != exit.Auth {
			t.Errorf("after the session ended: code %d, %s", code, errOut.String())
		}
	case <-time.After(10 * time.Second):
		t.Fatal("the command didn't end with the connection")
	}
}

func TestSendShare(t *testing.T) {
	work, _ := world(t)
	out := must(t, "the minutes", "send", "-", "Projects/minutes.txt", "--share", "-m", "From today", "--output", "json")
	if !strings.Contains(out, `"shared": true`) {
		t.Errorf("result: %s", out)
	}
	if texts := work.Messages("Projects"); len(texts) != 1 || texts[0] != "From today [file minutes.txt]" {
		t.Errorf("chat: %v", texts)
	}
}
