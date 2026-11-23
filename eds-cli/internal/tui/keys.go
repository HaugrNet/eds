package tui

import (
	"context"
	"encoding/json"
	"sort"
	"strings"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// key handles a key press: a window first, then the keys that work
// everywhere, then the focused pane.
func (m *model) key(msg tea.KeyPressMsg) tea.Cmd {
	key := msg.String()
	if key == "ctrl+c" || key == "ctrl+q" {
		return tea.Quit
	}
	if m.phase != phaseMain {
		switch key {
		case "q", "esc":
			return tea.Quit
		case "r", "enter":
			if m.phase != phaseConnecting {
				m.problem = ""
				if m.phase == phaseFailed {
					m.phase = phaseConnecting
				}
				return m.connect()
			}
		}
		return nil
	}
	if m.over != nil {
		return m.overlayKey(msg)
	}
	switch key {
	case "tab":
		m.focusNext(1)
		return nil
	case "shift+tab":
		m.focusNext(-1)
		return nil
	case "f1":
		m.help()
		return nil
	case "f2":
		m.accountMenu()
		return nil
	case "f3":
		m.nextPreset()
		return nil
	case "f4":
		m.swapPane()
		return nil
	case "ctrl+r":
		m.roomPicker()
		return nil
	case "alt+1", "alt+2", "alt+3":
		m.togglePane(allPanes[int(key[len(key)-1]-'1')])
		return nil
	case "ctrl+left", "ctrl+right", "ctrl+up", "ctrl+down":
		m.resize(strings.TrimPrefix(key, "ctrl+"))
		return nil
	}
	switch m.focus {
	case paneRooms:
		if key == "?" {
			m.help()
			return nil
		}
		return m.roomsKey(key)
	case paneChat:
		return m.chatKey(msg)
	case paneFiles:
		if key == "?" {
			m.help()
			return nil
		}
		return m.filesKey(key)
	}
	return nil
}

// paste puts pasted text where text is being written. A path dropped onto
// the terminal arrives as a paste: in the chat it is offered for sending.
func (m *model) paste(text string) tea.Cmd {
	if m.phase != phaseMain {
		return nil
	}
	if m.over != nil {
		if m.over.kind == overPrompt {
			m.over.in.insert(text)
		}
		return nil
	}
	if m.focus == paneChat && m.room != nil && m.room.CanWrite() && m.selected < 0 {
		if m.compose.empty() && looksLikeFile(text) {
			m.compose.set("/send " + strings.TrimSpace(text))
			m.say(i18n.T("Enter sends the file and shares it in the chat; Esc to cancel."), false)
			return nil
		}
		m.compose.insert(text)
	}
	return nil
}

// looksLikeFile reports whether a pasted text is the path of a file here.
func looksLikeFile(text string) bool {
	path := expandHome(strings.TrimSpace(text))
	if path == "" || strings.ContainsAny(path, "\n\r") {
		return false
	}
	return fileExists(path)
}

// togglePane hides a pane that is shown, or shows a hidden one; the last
// pane stays.
func (m *model) togglePane(pane string) {
	if m.narrow() {
		m.focus = pane
		return
	}
	shown := m.layout.has(pane)
	if shown && len(m.layout.panes()) == 1 {
		return
	}
	// A pane put away comes back where it was and as large as it was, as in Rune.
	var before json.RawMessage
	m.layout, before = m.layout.toggle(pane, m.ui.LayoutAway[pane])
	if m.ui.LayoutAway == nil {
		m.ui.LayoutAway = map[string]json.RawMessage{}
	}
	m.ui.LayoutAway[pane] = before
	if !shown {
		m.focus = pane
	}
	m.keepLayout("")
}

// resize moves the focused pane's divider.
func (m *model) resize(direction string) {
	if m.narrow() {
		return
	}
	dir, step := "row", 0.04
	switch direction {
	case "left":
		step = -step
	case "up":
		dir, step = "column", -step
	case "down":
		dir = "column"
	}
	if m.layout.resize(m.focus, dir, step) {
		m.keepLayout("")
	}
}

// nextPreset goes to Rune's next layout.
func (m *model) nextPreset() {
	at := -1
	for i, p := range presets {
		if p.Key == m.ui.Layout {
			at = i
		}
	}
	next := presets[(at+1)%len(presets)]
	m.layout = next.Layout()
	m.keepLayout(next.Key)
	m.say(presetName(next.Key), false)
}

// swapPane exchanges the focused pane's place with the next one's.
func (m *model) swapPane() {
	panes := m.layout.panes()
	if m.narrow() || len(panes) < 2 {
		return
	}
	for i, pane := range panes {
		if pane == m.focus {
			m.layout.swap(pane, panes[(i+1)%len(panes)])
			m.keepLayout("")
			return
		}
	}
}

func presetName(key string) string {
	switch key {
	case "chat-on-top":
		return i18n.Rune("Chat on top")
	case "rooms-beside":
		return i18n.Rune("Rooms beside chat over files")
	case "chat-and-files":
		return i18n.Rune("Chat and files")
	case "chat-only":
		return i18n.Rune("Chat only")
	}
	return i18n.Rune("Three columns")
}

func paneName(pane string) string {
	switch pane {
	case paneRooms:
		return i18n.Rune("Rooms and users")
	case paneChat:
		return i18n.Rune("Chat")
	}
	return i18n.Rune("Files")
}

// roomPicker chooses the room, as Rune's header does when the rooms pane is
// hidden.
func (m *model) roomPicker() {
	var items []item
	cursor := 0
	for _, room := range m.rooms {
		if !room.In() {
			continue
		}
		room := room
		if m.room != nil && m.room.ID == room.ID {
			cursor = len(items)
		}
		hint := ""
		if n := m.unread[room.ID]; n > 0 {
			hint = m.locale.Number(float64(n), 0)
		}
		items = append(items, item{label: room.Name, hint: hint, do: func(m *model) tea.Cmd {
			if m.visible(paneChat) || m.narrow() {
				m.focus = paneChat
			}
			return m.openRoom(room)
		}})
	}
	if len(items) == 0 {
		m.say(i18n.Rune("You haven't been invited to a room yet. Create one with \"New room\"."), false)
		return
	}
	m.menu(i18n.Rune("Choose a room…"), items, cursor)
}

// accountMenu is Rune's account menu: avatar, language and clock, theme,
// panes, notifications, about, and leaving.
func (m *model) accountMenu() {
	account := ""
	if m.stone != nil {
		account = m.stone.Account
	}
	items := []item{
		{label: i18n.Rune("Change avatar…"), do: func(m *model) tea.Cmd { m.avatarMenu(); return nil }},
		{label: i18n.Rune("Language and clock…"), do: func(m *model) tea.Cmd { m.languageMenu(); return nil }},
		{label: i18n.Rune("Theme…"), do: func(m *model) tea.Cmd { m.themeMenu(); return nil }},
		{label: i18n.Rune("Panes…"), do: func(m *model) tea.Cmd { m.panesMenu(); return nil }},
		{label: i18n.Rune("Notifications…"), hint: onOff(m.ui.Bell), do: func(m *model) tea.Cmd {
			m.ui.Bell = !m.ui.Bell
			m.save()
			if m.ui.Bell {
				m.say(i18n.T("The terminal's bell rings for new messages."), false)
			} else {
				m.say(i18n.T("The terminal's bell is off."), false)
			}
			return nil
		}},
		{label: i18n.Rune("Change passphrase"), do: func(m *model) tea.Cmd {
			m.page(i18n.Rune("Change passphrase"), []string{i18n.T("The passphrase is also kept in Tolk's container, which has to be opened to change it. Leave this screen and run 'tolk passphrase'.")})
			return nil
		}},
	}
	if m.admin {
		items = append(items, item{label: i18n.Rune("Stone settings…"), do: func(m *model) tea.Cmd { return m.loadStoneSettings(nil) }})
	}
	// The status of the stones of the container: told to their System
	// Administrators, and to who has a status key.
	items = append(items, item{label: i18n.Rune("Stones…"), do: func(m *model) tea.Cmd { return m.openStones() }})
	items = append(items,
		item{label: i18n.Rune("About Rune…"), do: func(m *model) tea.Cmd { m.about(); return nil }},
		item{label: i18n.T("Keys…"), do: func(m *model) tea.Cmd { m.help(); return nil }},
		item{label: i18n.Rune("Log out"), do: func(m *model) tea.Cmd { return tea.Quit }},
	)
	m.menu(i18n.Rune("{name}: your account and settings", "name", account), items, 0)
}

func onOff(on bool) string {
	if on {
		return i18n.Rune("Yes")
	}
	return i18n.Rune("No")
}

func (m *model) avatarMenu() {
	current := ""
	for _, user := range m.users {
		if m.stone != nil && strings.EqualFold(user.AccountName, m.stone.Account) {
			current = user.Avatar
		}
	}
	set := func(picture string) func(m *model) tea.Cmd {
		return func(m *model) tea.Cmd {
			return m.act("", "rooms", func(ctx context.Context, stone *service.Stone) error {
				return stone.Do(ctx, func(token string) error { return stone.Client.SetAvatar(ctx, token, picture) })
			})
		}
	}
	items := []item{{label: i18n.T("No picture"), do: set("")}}
	cursor := 0
	for i, picture := range service.UserAvatars {
		if picture == current {
			cursor = i + 1
		}
		items = append(items, item{label: service.AvatarName(picture), do: set(picture)})
	}
	m.menu(i18n.Rune("Your avatar"), items, cursor)
}

// choice makes a menu of the values of one of Tolk's settings.
func (m *model) choice(title string, values, labels []string, current string, set func(ui *model, value string)) {
	var items []item
	cursor := 0
	for i, value := range values {
		value := value
		if value == current {
			cursor = i
		}
		items = append(items, item{label: labels[i], do: func(m *model) tea.Cmd {
			set(m, value)
			m.restyle()
			m.save()
			return nil
		}})
	}
	m.menu(title, items, cursor)
}

func (m *model) languageMenu() {
	m.menu(i18n.Rune("Language and clock"), []item{
		{label: i18n.Rune("Language"), hint: m.ui.Language, do: func(m *model) tea.Cmd {
			m.choice(i18n.Rune("Language"), []string{"auto", "en", "de"},
				[]string{i18n.T("As the terminal"), "English", "Deutsch"}, m.ui.Language,
				func(m *model, value string) { m.ui.Language = value })
			return nil
		}},
		{label: i18n.Rune("Clock"), hint: m.ui.Clock, do: func(m *model) tea.Cmd {
			m.choice(i18n.Rune("Clock"), []string{"24", "12", "auto"},
				[]string{i18n.Rune("24 hours ({example})", "example", "14:05"), i18n.Rune("12 hours ({example})", "example", "2:05 PM"), i18n.T("As the terminal's region")}, m.ui.Clock,
				func(m *model, value string) { m.ui.Clock = value })
			return nil
		}},
	}, 0)
}

func (m *model) themeMenu() {
	m.menu(i18n.Rune("Theme"), []item{
		{label: i18n.Rune("Appearance"), hint: m.ui.Appearance, do: func(m *model) tea.Cmd {
			m.choice(i18n.Rune("Appearance"), []string{"auto", "light", "dark"},
				[]string{i18n.T("As the terminal"), i18n.Rune("Light"), i18n.Rune("Dark")}, m.ui.Appearance,
				func(m *model, value string) { m.ui.Appearance = value })
			return nil
		}},
		{label: i18n.Rune("Theme"), hint: m.ui.Theme, do: func(m *model) tea.Cmd {
			m.choice(i18n.Rune("Theme"), []string{"rune", "fjord", "moss", "contrast"},
				[]string{"Rune", i18n.Rune("Fjord (blue)"), i18n.Rune("Moss (green)"), i18n.Rune("High contrast")}, m.ui.Theme,
				func(m *model, value string) { m.ui.Theme = value })
			return nil
		}},
	}, 0)
}

func (m *model) panesMenu() {
	var items []item
	for _, p := range presets {
		p := p
		items = append(items, item{label: presetName(p.Key), do: func(m *model) tea.Cmd {
			m.layout = p.Layout()
			m.keepLayout(p.Key)
			return nil
		}})
	}
	for _, pane := range allPanes {
		pane := pane
		items = append(items, item{label: paneName(pane), hint: onOff(m.layout.has(pane)), do: func(m *model) tea.Cmd {
			m.togglePane(pane)
			return nil
		}})
	}
	m.menu(i18n.Rune("Panes"), items, 0)
}

func (m *model) about() {
	var lines []string
	for i, part := range service.About() {
		if i > 0 {
			lines = append(lines, "")
		}
		lines = append(lines, m.st.accent.Render(part.Title))
		lines = append(lines, part.Text...)
	}
	if m.info != nil && m.info.Persistent != nil {
		lines = append(lines, "", m.st.accent.Render(i18n.Rune("This system")))
		if *m.info.Persistent {
			lines = append(lines, i18n.Rune("This is a production system. Its database ({database}) keeps the data over a restart of the stone.", "database", m.info.Database))
		} else {
			lines = append(lines, i18n.Rune("This is a test system. Its database ({database}) is in memory only: all data is lost when the stone stops.", "database", m.info.Database))
		}
		if m.stone != nil && strings.HasPrefix(m.stone.URL, "https://") {
			lines = append(lines, i18n.Rune("Your connection to this stone is encrypted (HTTPS)."))
		} else {
			lines = append(lines, i18n.Rune("Your connection to this stone is not encrypted (no HTTPS). Others on the network can read along."))
		}
	}
	o := &overlay{kind: overPage, title: i18n.T("About Tolk"), lines: lines, raw: true}
	m.over = o
}

func (m *model) help() {
	m.page(i18n.T("Keys"), strings.Split(i18n.T(`Everywhere
  Tab, Shift+Tab     next and previous pane
  F2                 your account and settings
  Ctrl+R             choose a room
  F3                 next layout; F4 swaps the pane with the next
  Alt+1, 2, 3        show or hide rooms, chat, files
  Ctrl+arrows        make the pane larger or smaller
  Ctrl+Q             leave

Rooms and users
  Enter              open a room; show a user's rooms
  → ←                show or hide a room's members
  n                  new room          e   rename the room
  v                  the room's picture
  i                  invite someone    a   add a user
  c                  change access     x   remove the member
  L                  leave the room
  N, x on a user     new user, delete user (System Administrator)
  r on a user        make administrator or user (first System Administrator)

Chat
  Enter              send
  ↑                  select a message, then:
                     r reply, e edit, d delete, 1–6 react, s save its file
  PgUp, PgDn         earlier and later messages
  /send <file>       upload a file and share it; a dropped file works too
  /room <name>       go to another room

Files
  Enter              open a folder, download a file
  ←, Backspace       the folder above
  u upload   n new folder   r rename   m move   d delete   c share in the chat`), "\n"))
	m.over.raw = true
}

type settingsMsg struct {
	settings *client.SettingResponse
	err      error
}

// loadStoneSettings reads the stone's settings, after storing a change.
func (m *model) loadStoneSettings(change map[string]string) tea.Cmd {
	stone, ctx := m.stone, m.ctx
	return func() tea.Msg {
		ctx, cancel := context.WithTimeout(ctx, requestTime)
		defer cancel()
		var settings *client.SettingResponse
		err := stone.Do(ctx, func(token string) error {
			var err error
			settings, err = stone.Client.Settings(ctx, token, change)
			return err
		})
		return settingsMsg{settings: settings, err: err}
	}
}

// stoneSettings shows the stone's settings as a window over the panes;
// Enter changes the chosen one.
func (m *model) stoneSettings(settings *client.SettingResponse) {
	keys := make([]string, 0, len(settings.Settings))
	for key := range settings.Settings {
		if key != "eds.is.ready" {
			keys = append(keys, key)
		}
	}
	sort.Strings(keys)
	var items []item
	for _, key := range keys {
		key, value := key, settings.Settings[key]
		items = append(items, item{label: key, hint: value, do: func(m *model) tea.Cmd {
			if key == "eds.masterkey.url" {
				m.say(i18n.Rune("Changed with the MasterKey request, not here."), false)
				return nil
			}
			if allowed := settings.AllowedValues[key]; len(allowed) > 0 {
				var choices []item
				cursor := 0
				for i, option := range allowed {
					option := option
					if option == value {
						cursor = i
					}
					choices = append(choices, item{label: option, do: func(m *model) tea.Cmd {
						return m.loadStoneSettings(map[string]string{key: option})
					}})
				}
				m.menu(key, choices, cursor)
				return nil
			}
			m.prompt(i18n.Rune("Stone settings"), key, value, false, func(m *model, text string) tea.Cmd {
				if text == value {
					return m.loadStoneSettings(nil)
				}
				return m.loadStoneSettings(map[string]string{key: text})
			})
			return nil
		}})
	}
	m.menu(i18n.Rune("Stone settings"), items, 0)
}
