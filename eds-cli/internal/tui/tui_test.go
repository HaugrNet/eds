package tui

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/fakestone"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// access gives the service one stone, without a container or an agent.
type access struct {
	url, account, passphrase string
	tokens                   int
}

func (a *access) Stones() ([]agent.StoneInfo, map[string][]string, error) {
	return []agent.StoneInfo{{Name: "work", URL: a.url, Account: a.account, Default: true}}, map[string][]string{}, nil
}

func (a *access) Secrets(string) (string, string, string, error) { return "", "", "", nil }
func (a *access) Session(ctx context.Context, _ string, _ bool) (string, string, error) {
	a.tokens++
	token, err := newClient(a.url).Login(ctx, a.account, a.passphrase)
	return a.url, token, err
}

func (a *access) Close() {}

// driver runs the model as the program would: commands in the background,
// their messages one after the other. Ticks are left out, so nothing
// happens by itself.
type driver struct {
	t    *testing.T
	m    *model
	msgs chan tea.Msg
	quit bool
}

func (d *driver) start(cmd tea.Cmd) {
	if cmd == nil {
		return
	}
	go func() { d.msgs <- cmd() }()
}

func (d *driver) take(msg tea.Msg) {
	switch msg := msg.(type) {
	case nil, tickMsg:
	case tea.BatchMsg:
		for _, cmd := range msg {
			d.start(cmd)
		}
	case tea.QuitMsg:
		d.quit = true
	default:
		_, cmd := d.m.Update(msg)
		d.start(cmd)
	}
}

// until lets the model work until something is true.
func (d *driver) until(what string, done func() bool) {
	d.t.Helper()
	deadline := time.After(10 * time.Second)
	for !done() {
		select {
		case msg := <-d.msgs:
			d.take(msg)
		case <-deadline:
			d.t.Fatalf("waited in vain for: %s\n%s", what, d.screen())
		}
	}
}

// settle lets what is under way finish.
func (d *driver) settle() {
	for {
		select {
		case msg := <-d.msgs:
			d.take(msg)
		case <-time.After(150 * time.Millisecond):
			return
		}
	}
}

// keys presses keys: a name like "enter" or "ctrl+r", or a text to type.
func (d *driver) keys(keys ...string) {
	d.t.Helper()
	for _, key := range keys {
		msg, ok := named[key]
		if ok {
			d.take(msg)
			continue
		}
		for _, r := range key {
			d.take(tea.KeyPressMsg{Code: r, Text: string(r)})
		}
	}
	d.settle()
}

var named = map[string]tea.KeyPressMsg{
	"enter": {Code: tea.KeyEnter}, "tab": {Code: tea.KeyTab}, "esc": {Code: tea.KeyEscape},
	"up": {Code: tea.KeyUp}, "down": {Code: tea.KeyDown}, "left": {Code: tea.KeyLeft}, "right": {Code: tea.KeyRight},
	"backspace": {Code: tea.KeyBackspace}, "pgup": {Code: tea.KeyPgUp}, "f1": {Code: tea.KeyF1}, "f2": {Code: tea.KeyF2},
	"f3": {Code: tea.KeyF3}, "f4": {Code: tea.KeyF4}, "ctrl+r": {Code: 'r', Mod: tea.ModCtrl}, "ctrl+q": {Code: 'q', Mod: tea.ModCtrl},
	"ctrl+right": {Code: tea.KeyRight, Mod: tea.ModCtrl}, "alt+3": {Code: '3', Mod: tea.ModAlt},
}

// screen is what is shown, without colours.
func (d *driver) screen() string {
	content, _ := d.m.render()
	return stripStyle(content)
}

func (d *driver) shows(texts ...string) {
	d.t.Helper()
	screen := d.screen()
	for _, text := range texts {
		if !strings.Contains(screen, text) {
			d.t.Errorf("the screen doesn't show %q:\n%s", text, screen)
		}
	}
}

func (d *driver) hides(texts ...string) {
	d.t.Helper()
	screen := d.screen()
	for _, text := range texts {
		if strings.Contains(screen, text) {
			d.t.Errorf("the screen shows %q:\n%s", text, screen)
		}
	}
}

// world is a stone with the System Administrator, birgit and olaf, and
// three rooms.
func world(t *testing.T, account, passphrase string, width, height int) (*driver, *fakestone.Stone) {
	t.Helper()
	i18n.Set("en")
	t.Setenv("LC_ALL", "C")
	t.Setenv("LANG", "")
	stone := fakestone.New("admin secret")
	t.Cleanup(stone.Close)
	stone.AddUser("birgit", "birgit secret")
	stone.AddUser("olaf", "olaf secret")
	stone.AddRoom("Projects", "admin", "ADMIN", "birgit", "WRITE")
	stone.AddRoom("Archive", "admin", "ADMIN", "birgit", "READ")
	stone.AddRoom("Hall", "birgit", "ADMIN", "olaf", "WRITE")
	svc := service.NewService(&access{url: stone.URL(), account: account, passphrase: passphrase})
	var saved config.UI
	m := newModel(Options{Context: context.Background(), Service: svc, UI: config.DefaultUI(),
		SaveUI: func(ui config.UI) error { saved = ui; return nil }})
	_ = saved
	d := &driver{t: t, m: m, msgs: make(chan tea.Msg, 64)}
	t.Cleanup(func() {
		if m.chat != nil {
			m.chat.Close()
		}
	})
	d.take(tea.WindowSizeMsg{Width: width, Height: height})
	return d, stone
}

func connect(t *testing.T, account, passphrase string, width, height int) (*driver, *fakestone.Stone) {
	t.Helper()
	d, stone := world(t, account, passphrase, width, height)
	d.start(d.m.connect())
	d.until("the panes", func() bool { return d.m.phase == phaseMain })
	return d, stone
}

func TestStartScreenAndPanes(t *testing.T) {
	d, _ := world(t, "admin", "admin secret", 120, 30)
	d.shows("ᚱ ᚢ ᚾ", "Tolk", "Reaching the stone")
	d.start(d.m.connect())
	d.until("the panes", func() bool { return d.m.phase == phaseMain })
	// The three panes, rooms over users, and Rune's status line with the
	// test system marked.
	d.shows("Rooms and users", "Chat", "Files", "Archive", "Hall", "Projects", "Users", "birgit", "first admin",
		"Haugr EDS 2.0-TEST", "Test system", "Connected", "admin@work", "Choose a room.")
	// The System Administrator sees every room, and which they aren't in.
	d.shows("Hall  not in it")
	for _, line := range strings.Split(d.screen(), "\n") {
		if w := width(line); w != 120 {
			t.Fatalf("a line is %d cells wide, not 120: %q", w, line)
		}
	}
	if lines := strings.Count(d.screen(), "\n") + 1; lines != 30 {
		t.Errorf("%d lines, not 30", lines)
	}
}

func TestWrongPassphraseAndStoneDown(t *testing.T) {
	d, _ := world(t, "admin", "wrong", 100, 24)
	d.start(d.m.connect())
	d.until("the refusal", func() bool { return d.m.phase == phaseFailed })
	d.shows("refused the account or the passphrase", "Try now", "Test system")

	d, stone := connect(t, "admin", "admin secret", 100, 24)
	stone.Close()
	d.keys("enter") // opening a room finds the stone gone
	d.until("the notice", func() bool { return d.m.phase == phaseDown })
	d.shows("The stone is asleep", "Try now", "The data will not survive a restart of the stone.")
}

func TestChat(t *testing.T) {
	d, stone := connect(t, "birgit", "birgit secret", 120, 30)
	stone.Say("Projects", "admin", "Welcome to the hall")
	d.until("the unread count", func() bool { return strings.Contains(d.screen(), "Projects  1 ") })

	// Opening the room reads it: the count goes, the message shows.
	d.keys("down", "down", "enter")
	d.until("the room", func() bool { return d.m.room != nil && len(d.m.msgs) == 1 })
	d.shows("Welcome to the hall", "admin", "Message")
	d.hides("Projects  1 ")
	if d.m.focus != paneChat {
		t.Errorf("the focus is on %s", d.m.focus)
	}

	// Writing and sending: one checkmark, two once admin has it, and in
	// the accent colour once read (the same two marks without colours).
	d.keys("Hei alle", "enter")
	d.until("the own message", func() bool { return len(d.m.msgs) == 2 })
	if stone.Messages("Projects")[1] != "Hei alle" {
		t.Errorf("the stone has %v", stone.Messages("Projects"))
	}
	if ticks := d.m.ticks(1); ticks != 1 {
		t.Errorf("%d checkmarks before anyone has it", ticks)
	}
	stone.Read("Projects", "admin")
	d.until("the read receipt", func() bool { return d.m.ticks(1) == 3 })
	d.shows("✓✓")

	// Someone is writing, then their message arrives.
	stone.Typing("Projects", "admin")
	d.until("the typing line", func() bool { return strings.Contains(d.screen(), "admin is typing…") })
	id := stone.Say("Projects", "admin", "Hei birgit")
	d.until("the answer", func() bool { return len(d.m.msgs) == 3 })
	d.hides("is typing")

	// Select the last message: reply to it, and react to it.
	d.keys("up")
	d.shows("r Reply", "1–6")
	d.keys("r")
	d.shows("Replying to admin: Hei birgit")
	d.keys("Takk", "enter")
	d.until("the reply", func() bool { return len(d.m.msgs) == 4 && d.m.msgs[3].ReplyTo == id })
	d.shows("↪ admin: Hei birgit")
	d.keys("up", "up", "1")
	d.until("the reaction", func() bool { return len(d.m.msgs[2].Reactions["👍"]) == 1 })
	d.shows("👍 1")

	// Edit and delete the own message; not someone else's.
	d.keys("down", "e")
	d.shows("Edit message")
	d.keys(" for now", "enter")
	d.until("the edit", func() bool { return d.m.msgs[3].Text == "Takk for now" })
	d.shows("Takk for now", "(edited)")
	d.keys("up", "up", "d")
	d.shows("Only the author can delete a message.")
	d.keys("down", "d")
	d.shows("Delete this message for everyone in the room?")
	d.keys("y")
	d.until("the deletion", func() bool { return len(d.m.msgs) == 3 })
	d.hides("Takk for now")
}

func TestReadOnlyRoom(t *testing.T) {
	d, _ := connect(t, "birgit", "birgit secret", 120, 30)
	d.keys("enter") // Archive, where birgit can only read
	d.until("the room", func() bool { return d.m.room != nil && !d.m.loading })
	d.shows("You can only read in this room.")
	d.hides("Message  ")
	d.keys("hello", "enter")
	if len(d.m.msgs) != 0 {
		t.Error("something was sent")
	}
	// No upload, new folder, rename or delete in the files either.
	d.keys("tab", "n")
	d.shows("You can only read in this room.")
	if d.m.over != nil {
		t.Error("a window opened for a new folder")
	}
}

func TestFiles(t *testing.T) {
	d, stone := connect(t, "admin", "admin secret", 120, 30)
	dir := t.TempDir()
	t.Chdir(dir)
	local := filepath.Join(dir, "saga.txt")
	os.WriteFile(local, []byte("the saga"), 0o600)

	d.keys("down", "down", "enter", "tab") // Projects, then the files pane
	d.until("the room", func() bool { return d.m.room != nil && d.m.focus == paneFiles })
	d.shows("No files yet.")
	// A new folder, into it, and an upload there.
	d.keys("n", "sagas", "enter")
	d.until("the folder", func() bool { return len(d.m.entries) == 1 })
	d.shows("▸ sagas/")
	d.keys("enter")
	d.until("inside the folder", func() bool { return len(d.m.path) == 1 && d.m.entries == nil || len(d.m.entries) == 0 })
	d.keys("u", local, "enter")
	d.until("the upload", func() bool { return len(d.m.entries) == 1 && d.m.transfer == nil })
	if got, ok := stone.File("Projects", "sagas", "saga.txt"); !ok || string(got) != "the saga" {
		t.Fatal("the file didn't arrive")
	}
	d.shows("Projects / sagas", "saga.txt", "8 B", "Uploaded saga.txt.")

	// Rename, share in the chat, download (beside the local file of the
	// same name, so under a free name), move up, delete.
	d.keys("r", "backspace", "backspace", "backspace", "md", "enter")
	d.until("the new name", func() bool { return len(d.m.entries) == 1 && d.m.entries[0].DataName == "saga.md" })
	d.keys("c")
	d.until("the shared file", func() bool { return len(d.m.msgs) == 1 })
	d.shows("▤ saga.md")
	os.WriteFile(filepath.Join(dir, "saga.md"), []byte("taken"), 0o600)
	d.keys("enter")
	d.until("the download", func() bool {
		_, err := os.Stat(filepath.Join(dir, "saga (1).md"))
		return err == nil && d.m.transfer == nil
	})
	if got, _ := os.ReadFile(filepath.Join(dir, "saga (1).md")); string(got) != "the saga" {
		t.Errorf("downloaded %q", got)
	}
	d.keys("m")
	d.shows("Move \"saga.md\"", "Top level", "Archive")
	d.keys("enter")
	d.until("the move", func() bool { _, ok := stone.File("Projects", "saga.md"); return ok })
	d.keys("backspace")
	d.until("the top level", func() bool { return len(d.m.path) == 0 && len(d.m.entries) == 2 })
	d.keys("down", "d")
	d.shows("Delete \"saga.md\"?")
	d.keys("y")
	d.until("the deletion", func() bool { return len(d.m.entries) == 1 })
}

func TestRoomsAndMembers(t *testing.T) {
	d, stone := connect(t, "admin", "admin secret", 120, 34)
	// A new room; the creator administers it.
	d.keys("n", "Plans", "enter")
	d.until("the room", func() bool { return strings.Contains(d.screen(), "Plans") && d.m.over == nil })
	// Its members, then add olaf with read access.
	for d.m.current().room == nil || d.m.current().room.Name != "Plans" {
		d.keys("down")
	}
	d.keys("right")
	d.until("the members", func() bool { return len(d.m.members[d.m.current().room.ID]) == 1 })
	d.keys("a")
	d.shows("User to add to Plans", "birgit", "olaf")
	if len(d.m.over.items) != 2 {
		t.Errorf("%d users offered, want the two who aren't in the room", len(d.m.over.items))
	}
	d.keys("down", "enter")
	d.shows("Access in Plans", "Can read and write")
	d.keys("up", "enter")
	d.until("olaf in the room", func() bool { return strings.Contains(d.screen(), "olaf  Can read") })
	// Invite someone new: the link, with a QR code.
	d.keys("i", "åse", "enter", "enter")
	d.until("the invitation", func() bool { return d.m.over != nil && d.m.over.kind == overPage })
	d.shows("Invitation for åse", stone.Server.URL+"/#invite=", "▀")
	d.keys("esc")
	// Remove olaf again, with Rune's question.
	d.keys("down", "down")
	if r := d.m.current(); r.kind != rowMember {
		t.Fatalf("not on a member: %+v", r)
	}
	name := d.m.current().member.AccountName
	d.keys("x")
	d.shows("Remove " + name + " from Plans? They lose access to its chat and files.")
	d.keys("y")
	d.until("the member gone", func() bool { return !strings.Contains(d.screen(), name+"  Can read") })
}

// The first System Administrator chooses the role of a new account, and
// changes it later with r; it is marked as the first.
func TestUsersAndRoles(t *testing.T) {
	d, _ := connect(t, "admin", "admin secret", 120, 34)
	d.until("the users", func() bool { return len(d.m.users) > 0 })
	d.shows("admin  first admin")
	d.keys("N", "olga", "enter", "a long passphrase for olga", "enter", "a long passphrase for olga", "enter")
	d.shows("Role", "User", "Administrator")
	d.keys("down", "enter")
	d.until("olga as an administrator", func() bool {
		for _, user := range d.m.users {
			if user.AccountName == "olga" {
				return user.MemberRole == "ADMIN" && !user.FirstAdministrator
			}
		}
		return false
	})
	at := func(name string) {
		t.Helper()
		for i := 0; i < 40; i++ {
			if r := d.m.current(); r.kind == rowUser && r.user.AccountName == name {
				return
			}
			d.keys("down")
		}
		t.Fatalf("no row for the user %s", name)
	}
	at("olga")
	d.shows("olga  admin")
	d.keys("r")
	if d.m.over == nil || d.m.over.title != "Make user" || len(d.m.over.lines) != 1 || d.m.over.lines[0] != "Make olga a user again? They keep their rooms, but no longer manage the stone." {
		t.Fatalf("r on olga: %+v", d.m.over)
	}
	d.keys("y")
	d.until("olga as a user", func() bool {
		for _, user := range d.m.users {
			if user.AccountName == "olga" {
				return user.MemberRole == "STANDARD"
			}
		}
		return false
	})
	// Not for the first itself.
	for d.m.current().kind != rowUser || d.m.current().user.AccountName != "admin" {
		d.keys("up")
	}
	d.keys("r")
	if d.m.over != nil {
		t.Errorf("r on the first administrator opened %q", d.m.over.title)
	}
}

func TestLayoutsAndNarrow(t *testing.T) {
	d, _ := connect(t, "admin", "admin secret", 120, 30)
	d.keys("f3")
	if d.m.ui.Layout != "chat-on-top" {
		t.Errorf("layout %s", d.m.ui.Layout)
	}
	d.shows("Chat on top")
	// Hide the files, make the pane wider: the layout is the person's own.
	d.keys("alt+3")
	d.hides("Files")
	d.keys("ctrl+right")
	if d.m.ui.Layout != "custom" || len(d.m.ui.LayoutTree) == 0 {
		t.Errorf("layout %s %s", d.m.ui.Layout, d.m.ui.LayoutTree)
	}
	if again := layoutFor("custom", d.m.ui.LayoutTree); len(again.panes()) != 2 {
		t.Errorf("the stored layout has %v", again.panes())
	}
	for _, line := range strings.Split(d.screen(), "\n") {
		if w := width(line); w != 120 {
			t.Fatalf("a line is %d cells wide: %q", w, line)
		}
	}
	// A narrow terminal: one pane at a time, with tabs, as Rune on phones.
	d.take(tea.WindowSizeMsg{Width: 60, Height: 20})
	d.shows(" Rooms and users ", " Chat ", " Files ")
	d.hides("Choose a room.")
	d.keys("tab")
	d.shows("Choose a room.")
	for _, line := range strings.Split(d.screen(), "\n") {
		if w := width(line); w != 60 {
			t.Fatalf("a narrow line is %d cells wide: %q", w, line)
		}
	}
}

func TestMenusLanguageAndTheme(t *testing.T) {
	d, _ := connect(t, "admin", "admin secret", 120, 34)
	d.keys("f2")
	d.shows("admin: your account and settings", "Change avatar…", "Language and clock…", "Theme…", "Panes…", "Stone settings…", "Log out")
	d.keys("down", "enter", "enter", "down", "down", "enter") // language: Deutsch
	d.shows("Räume und Benutzer", "Dateien", "Testsystem", "Verbunden")
	if d.m.ui.Language != "de" {
		t.Errorf("language %s", d.m.ui.Language)
	}
	d.keys("f1")
	d.shows("Tasten")
	d.keys("esc")
	d.m.ui.Language = "en"
	d.m.restyle()
	d.keys("f2")
	for i := 0; i < 6; i++ {
		d.keys("down")
	}
	d.keys("enter")
	d.until("the settings", func() bool { return d.m.over != nil && strings.Contains(d.screen(), "eds.session.timeout.minutes") })
	d.hides("eds.is.ready")
	d.keys("esc", "ctrl+q")
	if !d.quit {
		t.Error("Ctrl+Q didn't leave")
	}
}

// Another login with the same account ends the session: Tolk says so and
// waits for the person, instead of taking the session back by itself.
func TestSessionEndedElsewhere(t *testing.T) {
	d, stone := connect(t, "admin", "admin secret", 100, 24)
	stone.EndChats(1008)
	d.until("the notice", func() bool { return d.m.phase == phaseFailed })
	d.shows("The stone ended the session", "one session per", "Try now")
	d.keys("r")
	d.until("the panes again", func() bool { return d.m.phase == phaseMain && d.m.chat != nil })
	d.shows("Connected")
}

// The window "Stones": the status of the stones of the container, with bars
// and small curves, asked again while it is open.
func TestStonesScreen(t *testing.T) {
	d, stone := connect(t, "admin", "admin secret", 120, 40)
	stone.Load(50, 0.42)
	d.keys("f2")
	d.shows("Stones…")
	for i := 0; i < 7; i++ {
		d.keys("down")
	}
	d.keys("enter")
	d.until("the status", func() bool { return strings.Contains(d.screen(), "awake") })
	d.shows("Stones", "work", "Storage", "People active", "Requests per second", "10.0", "Stone's processors", "42 %", "Memory")
	// While the window is open the stones are asked again.
	stone.Asleep(true)
	d.m.stonesAsked = d.m.stonesAsked.Add(-time.Minute)
	d.start(d.m.tick(time.Now()))
	d.until("the stone to fall asleep", func() bool { return strings.Contains(d.screen(), "asleep") })
	d.keys("esc")
	d.hides("People active")
}
