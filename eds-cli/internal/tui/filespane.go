package tui

import (
	"context"
	"strings"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// filesView draws the files pane's inside: where one is, and the folders
// and files there.
func (m *model) filesView(w, h int, focused bool) []string {
	if m.room == nil {
		return []string{"", " " + m.st.muted.Render(i18n.Rune("Choose a room."))}
	}
	where := m.room.Name
	for _, c := range m.path {
		where += " / " + c.name
	}
	lines := []string{" " + m.st.muted.Render(where)}
	if len(m.entries) == 0 {
		return append(lines, "", " "+m.st.muted.Render(i18n.Rune("No files yet.")))
	}
	sizeWidth := 1
	for _, e := range m.entries {
		if !e.IsFolder() {
			sizeWidth = max(sizeWidth, width(m.locale.Size(e.Size)))
		}
	}
	var rows []string
	for i, e := range m.entries {
		size, name := "", e.DataName
		if e.IsFolder() {
			name = "▸ " + name + "/"
		} else {
			size = m.locale.Size(e.Size)
			name = "  " + name
		}
		when := ""
		if t, ok := format.ParseTime(e.Added); ok {
			when = m.locale.Date(t.Local())
		}
		right := strings.Repeat(" ", sizeWidth-width(size)) + size + "  " + when
		left := fit(name, max(w-width(right)-3, 8))
		line := " " + left + " " + m.st.muted.Render(right)
		if i == m.fileCursor && focused {
			line = m.st.selected.Render(fit(" "+left+" "+right, w))
		}
		rows = append(rows, line)
	}
	room := h - 1
	if len(rows) > room && m.fileCursor >= room {
		rows = rows[m.fileCursor-room+1:]
	}
	return append(lines, rows...)
}

// folderID is the folder shown; empty for the room's top level.
func (m *model) folderID() string {
	if len(m.path) == 0 {
		return ""
	}
	return m.path[len(m.path)-1].id
}

// canWrite says, with a note if not, whether the open room may be written.
func (m *model) canWrite() bool {
	if m.room == nil {
		return false
	}
	if err := service.CheckWrite(m.room); err != nil {
		m.say(err.Error(), true)
		return false
	}
	return true
}

// filesKey handles a key in the files pane.
func (m *model) filesKey(key string) tea.Cmd {
	if m.room == nil {
		return nil
	}
	var entry *service.Entry
	if m.fileCursor >= 0 && m.fileCursor < len(m.entries) {
		entry = &m.entries[m.fileCursor]
	}
	room := *m.room
	switch key {
	case "up", "k":
		m.fileCursor = max(m.fileCursor-1, 0)
	case "down", "j":
		m.fileCursor = min(m.fileCursor+1, max(len(m.entries)-1, 0))
	case "home":
		m.fileCursor = 0
	case "end":
		m.fileCursor = max(len(m.entries)-1, 0)
	case "enter", "right", "l":
		if entry == nil {
			return nil
		}
		if entry.IsFolder() {
			m.path = append(m.path, crumb{entry.DataID, entry.DataName})
			m.fileCursor, m.entries = 0, nil
			return m.loadFiles()
		}
		if key == "enter" {
			return m.download(entry.DataID, entry.DataName, entry.Size)
		}
	case "s":
		if entry != nil && !entry.IsFolder() {
			return m.download(entry.DataID, entry.DataName, entry.Size)
		}
	case "backspace", "left", "h":
		if len(m.path) > 0 {
			m.path = m.path[:len(m.path)-1]
			m.fileCursor, m.entries = 0, nil
			return m.loadFiles()
		}
	case "u":
		if m.canWrite() {
			m.prompt(i18n.Rune("Upload"), i18n.T("File on this computer"), "", false, func(m *model, path string) tea.Cmd {
				if strings.TrimSpace(path) == "" {
					return nil
				}
				return m.upload(path, false)
			})
		}
	case "n":
		if m.canWrite() {
			folderID := m.folderID()
			m.prompt(i18n.Rune("New folder"), i18n.Rune("Name of the new folder"), "", false, func(m *model, name string) tea.Cmd {
				name = strings.TrimSpace(name)
				if err := service.CheckName(name); err != nil {
					m.say(err.Error(), true)
					return nil
				}
				return m.act(i18n.T("Made the folder %s.", name), "files", func(ctx context.Context, stone *service.Stone) error {
					return stone.Do(ctx, func(token string) error {
						_, err := stone.Client.AddData(ctx, token, client.ProcessDataRequest{CircleID: room.ID, FolderID: folderID, DataName: name, TypeName: client.TypeFolder})
						return err
					})
				})
			})
		}
	case "r":
		if entry != nil && m.canWrite() {
			e := *entry
			m.prompt(i18n.Rune("Rename"), i18n.Rune("Name"), e.DataName, false, func(m *model, name string) tea.Cmd {
				name = strings.TrimSpace(name)
				if name == e.DataName {
					return nil
				}
				if err := service.CheckName(name); err != nil {
					m.say(err.Error(), true)
					return nil
				}
				return m.act(i18n.T("Renamed %s to %q.", e.DataName, name), "files", func(ctx context.Context, stone *service.Stone) error {
					return stone.Do(ctx, func(token string) error {
						return stone.Client.UpdateData(ctx, token, e.DataID, client.ProcessDataRequest{DataName: name})
					})
				})
			})
		}
	case "d", "delete":
		if entry != nil && m.canWrite() {
			e := *entry
			question := i18n.Rune("Delete \"{name}\"? It is deleted for everyone in the room, and can't be restored.", "name", e.DataName)
			title := i18n.T("Delete file")
			if e.IsFolder() {
				question = i18n.Rune("Delete the folder \"{name}\"? It is deleted for everyone in the room, and can't be restored.", "name", e.DataName)
				title = i18n.Rune("Delete folder")
			}
			m.confirm(title, question, func(m *model) tea.Cmd {
				return m.act(i18n.T("Deleted %s.", e.DataName), "files", func(ctx context.Context, stone *service.Stone) error {
					return stone.Do(ctx, func(token string) error { return stone.Client.DeleteData(ctx, token, e.DataID) })
				})
			})
		}
	case "m":
		if entry != nil && m.canWrite() {
			if entry.IsFolder() {
				m.say(i18n.Rune("Folders can't be moved, only files."), true)
				return nil
			}
			m.move(room, *entry)
		}
	case "c":
		if entry != nil && !entry.IsFolder() && m.canWrite() {
			id, name := entry.DataID, entry.DataName
			m.say(i18n.T("Shared %s in the chat.", name), false)
			return m.sendMessage(room.ID, "", id, "")
		}
	}
	return nil
}

// move offers where a file can go: the folders here and the folder above,
// and the other rooms the account can write in, as Rune does.
func (m *model) move(room service.Room, e service.Entry) {
	var items []item
	target := func(label string, change func(ctx context.Context, stone *service.Stone, token string) error) {
		items = append(items, item{label: label, do: func(m *model) tea.Cmd {
			return m.act(i18n.T("Moved %s to %s.", e.DataName, label), "files", func(ctx context.Context, stone *service.Stone) error {
				return stone.Do(ctx, func(token string) error { return change(ctx, stone, token) })
			})
		}})
	}
	if len(m.path) > 0 {
		// The folder above: its ID is the folder of the folder shown.
		label := i18n.Rune("Top level")
		if len(m.path) > 1 {
			label = m.path[len(m.path)-2].name + "/"
		}
		here := m.folderID()
		target(label, func(ctx context.Context, stone *service.Stone, token string) error {
			parent := ""
			above := ""
			if len(m.path) > 1 {
				above = m.path[len(m.path)-2].id
			}
			entries, err := stone.Client.List(ctx, token, room.ID, above)
			if err != nil {
				return err
			}
			for _, candidate := range entries {
				if candidate.DataID == here {
					parent = candidate.FolderID
				}
			}
			return stone.Client.UpdateData(ctx, token, e.DataID, client.ProcessDataRequest{FolderID: parent})
		})
	}
	for _, other := range m.entries {
		if other.IsFolder() {
			folder := other
			target(folder.DataName+"/", func(ctx context.Context, stone *service.Stone, token string) error {
				return stone.Client.UpdateData(ctx, token, e.DataID, client.ProcessDataRequest{FolderID: folder.DataID})
			})
		}
	}
	for _, other := range m.rooms {
		if other.ID != room.ID && other.CanWrite() {
			to := other
			target(to.Name, func(ctx context.Context, stone *service.Stone, token string) error {
				_, err := stone.Client.MoveData(ctx, token, e.DataID, client.ProcessDataRequest{TargetCircleID: to.ID})
				return err
			})
		}
	}
	if len(items) == 0 {
		m.say(i18n.Rune("No other place to move it to"), false)
		return
	}
	o := &overlay{kind: overMenu, title: i18n.Rune("Move \"{name}\"", "name", e.DataName), items: items}
	o.lines = []string{i18n.Rune("Moving to another room encrypts the file with that room's key. Its thumbnail is made again on the next upload."), ""}
	m.over = o
}
