package tui

import (
	"context"
	"errors"
	"sync/atomic"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/chat"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/messages"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
	"github.com/HaugrNet/eds/eds-cli/internal/status"
)

// Options are what the interactive mode starts with.
type Options struct {
	Context context.Context
	Service *service.Service
	Stone   string                // the stone to open; empty for the default
	UI      config.UI             // Tolk's settings
	SaveUI  func(config.UI) error // keeps changed settings; may be nil
	Version string                // Tolk's version
}

// Run shows the interactive mode until the person leaves it.
func Run(opts Options) error {
	if opts.Context == nil {
		opts.Context = context.Background()
	}
	m := newModel(opts)
	_, err := tea.NewProgram(m, tea.WithContext(opts.Context)).Run()
	if m.chat != nil {
		m.chat.Close()
	}
	if errors.Is(err, tea.ErrProgramKilled) || errors.Is(err, context.Canceled) {
		return nil
	}
	return err
}

// The phases of the interactive mode.
const (
	phaseConnecting = iota // the start screen, while the stone is reached
	phaseMain              // the panes
	phaseDown              // the stone doesn't answer: Rune's "asleep" notice
	phaseFailed            // the stone refused: nothing to retry by itself
)

// narrowBelow is the width under which one pane is shown at a time, as
// Rune does on phones.
const narrowBelow = 100

type crumb struct{ id, name string }

type typist struct {
	name  string
	until time.Time
}

// transfer is an upload or download in progress.
type transfer struct {
	name   string
	total  int64
	done   atomic.Int64
	upload bool
}

type model struct {
	opts   Options
	ctx    context.Context
	w, h   int
	ui     config.UI
	st     styles
	dark   bool
	locale format.Locale

	phase     int
	problem   string
	downTicks int
	info      *client.VersionResponse
	stone     *service.Stone
	chat      *chat.Client
	// What the stones of the container told of their status, while the
	// window "Stones" is open.
	stones      []status.View
	stonesBusy  bool
	stonesAsked time.Time
	// blocked is why nothing can be written to the open room's chat, and
	// chamber whether the open room is one.
	blocked string
	chamber bool
	admin   bool // the account is a System Administrator
	first   bool // the account is the first System Administrator, who rules the others

	layout *node
	focus  string

	// Rooms and users.
	rooms       []service.Room
	users       []client.Member
	unread      map[string]int
	roomCursor  int
	openMembers string // the room whose members are shown
	members     map[string][]client.Trustee
	openUser    string // the user whose rooms are shown
	userRooms   map[string][]client.Circle

	// Chat.
	room      *service.Room
	msgs      []client.ChatMessage
	receipts  map[string]client.ChatReceipt
	selected  int // the selected message, or -1 while writing
	scroll    int // lines scrolled up from the newest
	compose   input
	replyTo   string
	editing   string
	typists   map[string]typist
	typedAt   time.Time
	earlier   bool // there may be earlier messages
	loading   bool
	chatError string

	// Files.
	path       []crumb
	entries    []service.Entry
	fileCursor int
	fileNames  map[string]string // names of files seen, for shared files in the chat
	transfer   *transfer

	over *overlay

	note    string
	noteBad bool
	noteEnd time.Time
}

func newModel(opts Options) *model {
	m := &model{
		opts: opts, ctx: opts.Context, ui: opts.UI, dark: true,
		unread: map[string]int{}, members: map[string][]client.Trustee{}, userRooms: map[string][]client.Circle{},
		receipts: map[string]client.ChatReceipt{}, typists: map[string]typist{}, fileNames: map[string]string{},
		selected: -1, focus: paneRooms,
	}
	m.layout = layoutFor(m.ui.Layout, m.ui.LayoutTree)
	m.restyle()
	return m
}

// restyle applies the settings that change how things look and read.
func (m *model) restyle() {
	dark := m.dark
	switch m.ui.Appearance {
	case "light":
		dark = false
	case "dark":
		dark = true
	}
	m.st = newStyles(m.ui.Theme, dark)
	i18n.Set(m.ui.Language)
	m.locale = format.Locale{Language: i18n.Language(), Region: format.Region(), Clock: m.ui.Clock}
}

// save keeps the settings, when the caller gave a way to.
func (m *model) save() {
	if m.opts.SaveUI != nil {
		if err := m.opts.SaveUI(m.ui); err != nil {
			m.say(err.Error(), true)
		}
	}
}

// say shows a note in the last line for a while.
func (m *model) say(text string, bad bool) {
	m.note, m.noteBad, m.noteEnd = text, bad, time.Now().Add(6*time.Second)
}

// friendly puts an error in Rune's words.
func (m *model) friendly(err error) string {
	var e *exit.Error
	if errors.As(err, &e) {
		if e.ReturnCode != 0 && e.ReturnMessage != "" && e.Message == e.ReturnMessage {
			return messages.Friendly(e.ReturnMessage, m.locale)
		}
		return e.Message
	}
	return err.Error()
}

// unreachable reports whether an error means the stone doesn't answer.
func unreachable(err error) bool {
	var e *exit.Error
	return errors.As(err, &e) && e.Code == exit.Network
}

func (m *model) narrow() bool { return m.w < narrowBelow }

// shown lists the panes on the screen, in reading order.
func (m *model) shown() []string {
	if m.narrow() {
		return allPanes
	}
	return m.layout.panes()
}

func (m *model) Init() tea.Cmd {
	return tea.Batch(m.connect(), tick(), tea.RequestBackgroundColor)
}

func (m *model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.w, m.h = msg.Width, msg.Height
		return m, nil
	case tea.BackgroundColorMsg:
		m.dark = msg.IsDark()
		m.restyle()
		return m, nil
	case tea.KeyPressMsg:
		return m, m.key(msg)
	case tea.PasteMsg:
		return m, m.paste(msg.Content)
	case tickMsg:
		return m, m.tick(time.Time(msg))
	}
	return m, m.arrived(msg)
}

// tick comes twice a second: notes and typing lines end, progress moves,
// and a stone that is down is asked again.
func (m *model) tick(now time.Time) tea.Cmd {
	if m.note != "" && now.After(m.noteEnd) {
		m.note = ""
	}
	for id, t := range m.typists {
		if now.After(t.until) {
			delete(m.typists, id)
		}
	}
	cmds := []tea.Cmd{tick()}
	if again := m.stonesTick(now); again != nil {
		cmds = append(cmds, again)
	}
	// A stone that doesn't answer is asked again every 5 s, as Rune does.
	if m.phase == phaseDown {
		m.downTicks++
		if m.downTicks%10 == 0 {
			cmds = append(cmds, m.connect())
		}
	}
	return tea.Batch(cmds...)
}

type tickMsg time.Time

func tick() tea.Cmd {
	return tea.Tick(500*time.Millisecond, func(t time.Time) tea.Msg { return tickMsg(t) })
}

// focusNext moves the focus to the next (or previous) pane shown.
func (m *model) focusNext(step int) {
	shown := m.shown()
	if len(shown) == 0 {
		return
	}
	at := 0
	for i, pane := range shown {
		if pane == m.focus {
			at = i
		}
	}
	m.focus = shown[(at+step+len(shown))%len(shown)]
}

// ensureFocus keeps the focus on a pane that is on the screen.
func (m *model) ensureFocus() {
	shown := m.shown()
	for _, pane := range shown {
		if pane == m.focus {
			return
		}
	}
	if len(shown) > 0 {
		m.focus = shown[0]
	}
}

// keepLayout stores the layout as the person arranged it.
func (m *model) keepLayout(preset string) {
	if preset != "" {
		m.ui.Layout, m.ui.LayoutTree = preset, nil
	} else {
		m.ui.Layout, m.ui.LayoutTree = "custom", m.layout.encode()
	}
	m.ensureFocus()
	m.save()
}
