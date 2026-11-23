package tui

import (
	"context"
	"strconv"
	"strings"

	tea "charm.land/bubbletea/v2"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// row is one line of the rooms pane: a heading, a room, a member of the
// room whose members are shown, a user, or a room of the user whose rooms
// are shown.
type row struct {
	kind   int
	room   *service.Room
	member *client.Trustee
	user   *client.Member
	circle *client.Circle
}

const (
	rowHeading = iota
	rowRoom
	rowMember
	rowUser
	rowUserRoom
	rowHint
)

// rows lists the rooms pane's lines: rooms over users, as in Rune.
func (m *model) rows() []row {
	var rows []row
	for i := range m.rooms {
		room := &m.rooms[i]
		rows = append(rows, row{kind: rowRoom, room: room})
		if room.ID == m.openMembers {
			members := m.members[room.ID]
			for j := range members {
				rows = append(rows, row{kind: rowMember, room: room, member: &members[j]})
			}
		}
	}
	if len(m.rooms) == 0 {
		rows = append(rows, row{kind: rowHint})
	}
	rows = append(rows, row{kind: rowHeading})
	for i := range m.users {
		user := &m.users[i]
		rows = append(rows, row{kind: rowUser, user: user})
		if user.MemberID == m.openUser {
			circles := m.userRooms[user.MemberID]
			for j := range circles {
				rows = append(rows, row{kind: rowUserRoom, user: user, circle: &circles[j]})
			}
		}
	}
	return rows
}

// current is the row under the cursor.
func (m *model) current() row {
	rows := m.rows()
	if m.roomCursor < 0 || m.roomCursor >= len(rows) {
		return row{kind: rowHint}
	}
	return rows[m.roomCursor]
}

// moveRoomCursor moves over the lines that can be chosen.
func (m *model) moveRoomCursor(step int) {
	rows := m.rows()
	at := m.roomCursor
	for {
		at += step
		if at < 0 || at >= len(rows) {
			return
		}
		if rows[at].kind != rowHeading && rows[at].kind != rowHint {
			m.roomCursor = at
			return
		}
	}
}

func accessText(trust string) string {
	switch trust {
	case client.TrustRead:
		return i18n.Rune("Can read")
	case client.TrustWrite:
		return i18n.Rune("Can read and write")
	case client.TrustAdmin, client.TrustSysop:
		return i18n.Rune("Administrator")
	}
	return i18n.Rune("not in it")
}

// roomsView draws the lines of the rooms pane.
func (m *model) roomsView(w, h int, focused bool) []string {
	rows := m.rows()
	var lines []string
	for i, r := range rows {
		line := ""
		switch r.kind {
		case rowHint:
			line = m.st.muted.Render(i18n.T("No rooms yet. Press n for a new one."))
		case rowHeading:
			lines = append(lines, "")
			line = " " + m.st.title.Render(i18n.Rune("Users"))
		case rowRoom:
			mark := "  "
			if m.room != nil && m.room.ID == r.room.ID {
				mark = m.st.accent.Render("▸ ")
			}
			name := r.room.Name
			if r.room.Avatar != "" {
				name += m.st.muted.Render("  " + service.AvatarName(r.room.Avatar))
			}
			if !r.room.In() {
				name = m.st.muted.Render(r.room.Name + "  " + i18n.Rune("not in it"))
			}
			if n := m.unread[r.room.ID]; n > 0 {
				name += " " + m.st.onAccent.Render(" "+strconv.Itoa(n)+" ")
			}
			line = mark + name
		case rowMember:
			line = "    " + m.light(m.online(r.room.ID, r.member.MemberID)) + r.member.AccountName + m.st.muted.Render("  "+accessText(r.member.TrustLevel))
		case rowUser:
			line = m.light(m.chat != nil && m.chat.OnlineAnywhere(r.user.MemberID)) + r.user.AccountName
			if r.user.Avatar != "" {
				line += m.st.muted.Render("  " + service.AvatarName(r.user.Avatar))
			}
			// Short, as Rune's marks: the pane is narrow.
			if r.user.FirstAdministrator {
				line += m.st.muted.Render("  " + i18n.Rune("first admin"))
			} else if r.user.MemberRole == client.RoleAdmin {
				line += m.st.muted.Render("  " + i18n.Rune("admin"))
			}
		case rowUserRoom:
			line = "      " + m.st.muted.Render(r.circle.CircleName)
		}
		if i == m.roomCursor && focused && r.kind != rowHeading {
			line = m.st.selected.Render(fit(stripStyle(line), w))
		}
		lines = append(lines, line)
	}
	// Keep the cursor's line on the screen.
	at := m.roomCursor
	for i, r := range rows {
		if r.kind == rowHeading && i <= m.roomCursor {
			at++ // the empty line before the heading
		}
	}
	if len(lines) > h && at >= h {
		lines = lines[at-h+1:]
	}
	return lines
}

// roomsKey handles a key in the rooms pane.
func (m *model) roomsKey(key string) tea.Cmd {
	r := m.current()
	switch key {
	case "up", "k":
		m.moveRoomCursor(-1)
	case "down", "j":
		m.moveRoomCursor(1)
	case "enter":
		switch r.kind {
		case rowRoom:
			m.openMembers = r.room.ID
			cmd := m.openRoom(*r.room)
			if cmd != nil && !m.narrow() && m.layout.has(paneChat) {
				m.focus = paneChat
			} else if cmd != nil && m.narrow() {
				m.focus = paneChat
			}
			return cmd
		case rowUser:
			return m.toggleUser(r.user)
		}
	case "right", "l":
		switch r.kind {
		case rowRoom:
			m.openMembers = r.room.ID
			return m.loadMembers(*r.room)
		case rowUser:
			if m.openUser != r.user.MemberID {
				return m.toggleUser(r.user)
			}
		}
	case "left", "h":
		switch r.kind {
		case rowRoom, rowMember:
			m.openMembers = ""
			m.roomCursor = min(m.roomCursor, len(m.rows())-1)
			for i, candidate := range m.rows() {
				if candidate.kind == rowRoom && candidate.room.ID == r.room.ID {
					m.roomCursor = i
				}
			}
		case rowUser, rowUserRoom:
			m.openUser = ""
			for i, candidate := range m.rows() {
				if candidate.kind == rowUser && candidate.user.MemberID == r.user.MemberID {
					m.roomCursor = i
				}
			}
		}
	case "n":
		m.prompt(i18n.Rune("New room"), i18n.Rune("Name of the new room"), "", false, func(m *model, name string) tea.Cmd {
			if strings.TrimSpace(name) == "" {
				return nil
			}
			return m.act(i18n.T("Made the room %q on %s. You administer it.", name, m.stone.Name), "rooms", func(ctx context.Context, stone *service.Stone) error {
				return stone.Do(ctx, func(token string) error {
					_, err := stone.Client.CreateCircle(ctx, token, name)
					return err
				})
			})
		})
	case "N":
		if m.admin {
			m.newUser()
		}
	case "e":
		if r.kind == rowRoom && r.room.Administers() {
			room := *r.room
			m.prompt(i18n.Rune("Rename room…"), i18n.Rune("Name"), room.Name, false, func(m *model, name string) tea.Cmd {
				if strings.TrimSpace(name) == "" || name == room.Name {
					return nil
				}
				return m.act(i18n.T("Renamed the room %q to %q.", room.Name, name), "rooms", func(ctx context.Context, stone *service.Stone) error {
					return stone.Do(ctx, func(token string) error {
						return stone.Client.UpdateCircle(ctx, token, room.ID, client.ProcessCircleRequest{CircleName: name})
					})
				})
			})
		}
	case "v":
		if r.kind == rowRoom && r.room.Administers() {
			m.roomAvatar(*r.room)
		}
	case "i":
		if r.room != nil {
			m.invite(*r.room)
		}
	case "a":
		if r.room != nil {
			m.addUser(*r.room)
		}
	case "c":
		if r.kind == rowMember {
			m.changeAccess(*r.room, *r.member)
		}
	case "r":
		// Only the first System Administrator changes roles, and not its own.
		if r.kind == rowUser && m.first && !r.user.FirstAdministrator {
			m.changeRole(*r.user)
		}
	case "x", "delete":
		switch r.kind {
		case rowMember:
			m.removeMember(*r.room, *r.member)
		case rowUser:
			if m.admin {
				m.deleteUser(*r.user)
			}
		}
	case "L":
		if r.room != nil && r.room.In() {
			m.leave(*r.room)
		}
	}
	return nil
}

func (m *model) toggleUser(user *client.Member) tea.Cmd {
	if m.openUser == user.MemberID {
		m.openUser = ""
		return nil
	}
	m.openUser = user.MemberID
	return m.loadUserRooms(*user)
}

// accessItems offers the three access levels, in Rune's words.
func accessItems(choose func(m *model, level string) tea.Cmd) []item {
	return []item{
		{label: i18n.Rune("Can read"), do: func(m *model) tea.Cmd { return choose(m, client.TrustRead) }},
		{label: i18n.Rune("Can read and write"), do: func(m *model) tea.Cmd { return choose(m, client.TrustWrite) }},
		{label: i18n.Rune("Administrator (can invite and manage)"), do: func(m *model) tea.Cmd { return choose(m, client.TrustAdmin) }},
	}
}

func (m *model) invite(room service.Room) {
	m.prompt(i18n.Rune("Invite to {room}", "room", room.Name), i18n.Rune("Name"), "", false, func(m *model, name string) tea.Cmd {
		name = strings.TrimSpace(name)
		if name == "" {
			return nil
		}
		m.menu(i18n.Rune("Access"), accessItems(func(m *model, level string) tea.Cmd {
			stone, ctx := m.stone, m.ctx
			return func() tea.Msg {
				ctx, cancel := context.WithTimeout(ctx, requestTime)
				defer cancel()
				var token string
				err := stone.Do(ctx, func(session string) error {
					var err error
					token, err = stone.Client.Invite(ctx, session, name, room.ID, level)
					return err
				})
				if err != nil {
					return inviteMsg{err: err}
				}
				if token == "" {
					return inviteMsg{name: name, added: true}
				}
				return inviteMsg{name: name, link: service.InvitationLink(stone.URL, name, token)}
			}
		}), 1)
		return nil
	})
}

func (m *model) addUser(room service.Room) {
	in := map[string]bool{}
	for _, member := range m.members[room.ID] {
		in[member.MemberID] = true
	}
	var items []item
	for _, user := range m.users {
		if in[user.MemberID] {
			continue
		}
		user := user
		items = append(items, item{label: user.AccountName, do: func(m *model) tea.Cmd {
			m.menu(i18n.Rune("Access in {room}", "room", room.Name), accessItems(func(m *model, level string) tea.Cmd {
				return m.act(i18n.T("%s: added %s (%s).", room.Name, user.AccountName, accessText(level)), "members", func(ctx context.Context, stone *service.Stone) error {
					return stone.Do(ctx, func(token string) error {
						return stone.Client.AddTrustee(ctx, token, room.ID, user.MemberID, level)
					})
				})
			}), 1)
			return nil
		}})
	}
	if len(items) == 0 {
		m.say(i18n.T("Everyone is in %s already.", room.Name), false)
		return
	}
	m.openMembers = room.ID
	m.menu(i18n.Rune("User to add to {room}", "room", room.Name), items, 0)
}

func (m *model) changeAccess(room service.Room, member client.Trustee) {
	// Nobody changes their own access, and the only administrator keeps theirs.
	if m.chat != nil && member.MemberID == m.chat.MemberID {
		m.say(i18n.Rune("You cannot change your own access."), false)
		return
	}
	if m.lastAdministrator(room, member.MemberID) {
		m.say(i18n.Rune("The only administrator of a room keeps this access."), false)
		return
	}
	m.menu(i18n.Rune("Access of {name}", "name", member.AccountName), accessItems(func(m *model, level string) tea.Cmd {
		return m.act(i18n.T("%s: %s now has the access \"%s\".", room.Name, member.AccountName, accessText(level)), "members", func(ctx context.Context, stone *service.Stone) error {
			return stone.Do(ctx, func(token string) error {
				return stone.Client.AlterTrustee(ctx, token, room.ID, member.MemberID, level)
			})
		})
	}), 0)
}

// lastAdministrator reports whether a member is the room's only one.
func (m *model) lastAdministrator(room service.Room, memberID string) bool {
	admin := func(t client.Trustee) bool {
		return t.TrustLevel == client.TrustAdmin || t.TrustLevel == client.TrustSysop
	}
	is, others := false, 0
	for _, member := range m.members[room.ID] {
		if member.MemberID == memberID {
			is = admin(member)
		} else if admin(member) {
			others++
		}
	}
	return is && others == 0
}

func (m *model) removeMember(room service.Room, member client.Trustee) {
	question := i18n.Rune("Remove {name} from {room}? They lose access to its chat and files.", "name", member.AccountName, "room", room.Name)
	if m.lastAdministrator(room, member.MemberID) {
		question = i18n.Rune("{name} is the last administrator of this room. Without one, only the System Administrator can manage the room. Continue?", "name", member.AccountName)
	}
	m.confirm(i18n.Rune("Remove {name}", "name", member.AccountName), question, func(m *model) tea.Cmd {
		return m.act(i18n.T("%s: removed %s.", room.Name, member.AccountName), "members", func(ctx context.Context, stone *service.Stone) error {
			return stone.Do(ctx, func(token string) error {
				return stone.Client.RemoveTrustee(ctx, token, room.ID, member.MemberID)
			})
		})
	})
}

func (m *model) leave(room service.Room) {
	members := m.members[room.ID]
	myID := ""
	if m.chat != nil {
		myID = m.chat.MemberID
	}
	warning := i18n.Rune("You lose access to its chat and files, until someone invites you again.")
	if len(members) == 1 {
		warning = i18n.Rune("You are the only member. The room and its files stay, but nobody can open them until the System Administrator deletes the room or adds someone.")
	} else if len(members) > 1 && m.lastAdministrator(room, myID) {
		warning = i18n.Rune("You are the last administrator. Without one, only the System Administrator can manage the room.")
	}
	m.confirm(i18n.Rune("Leave room"), i18n.Rune("Leave {room}?", "room", room.Name)+" "+warning, func(m *model) tea.Cmd {
		return m.act(i18n.T("%s: you left the room.", room.Name), "rooms", func(ctx context.Context, stone *service.Stone) error {
			return stone.Do(ctx, func(token string) error {
				return stone.Client.RemoveTrustee(ctx, token, room.ID, myID)
			})
		})
	})
}

func (m *model) newUser() {
	m.prompt(i18n.Rune("New user"), i18n.Rune("Name"), "", false, func(m *model, name string) tea.Cmd {
		name = strings.TrimSpace(name)
		if name == "" {
			return nil
		}
		m.prompt(i18n.Rune("New user"), i18n.T("Passphrase for %s", name), "", true, func(m *model, first string) tea.Cmd {
			m.prompt(i18n.Rune("New user"), i18n.Rune("Repeat the passphrase"), "", true, func(m *model, second string) tea.Cmd {
				if first != second {
					m.say(i18n.Rune("The passphrases are not the same."), true)
					return nil
				}
				create := func(role string) func(m *model) tea.Cmd {
					done := i18n.T("Made the user %q on %s.", name, m.stone.Name)
					if role == client.RoleAdmin {
						done = i18n.T("Made the administrator %q on %s.", name, m.stone.Name)
					}
					return func(m *model) tea.Cmd {
						return m.act(done, "rooms", func(ctx context.Context, stone *service.Stone) error {
							return stone.Do(ctx, func(token string) error {
								_, err := stone.Client.CreateMember(ctx, token, name, first, role)
								return err
							})
						})
					}
				}
				// The first System Administrator chooses the role; for the
				// others it is a user. (An invitation always makes a user.)
				if !m.first {
					return create(client.RoleStandard)(m)
				}
				m.menu(i18n.Rune("Role"), []item{
					{label: i18n.Rune("User"), hint: i18n.Rune("Works in the rooms they are added to."), do: create(client.RoleStandard)},
					{label: i18n.Rune("Administrator"), hint: i18n.Rune("Also manages the users, the rooms and the settings of the stone. Only you make, unmake or delete administrators."), do: create(client.RoleAdmin)},
				}, 0)
				return nil
			})
			return nil
		})
		return nil
	})
}

// changeRole makes a user an administrator, or an administrator a user.
func (m *model) changeRole(user client.Member) {
	role, title := client.RoleAdmin, i18n.Rune("Make administrator")
	question := i18n.Rune("Make {name} an administrator? They can then manage the users, the rooms and the settings of the stone.", "name", user.AccountName)
	done := i18n.T("%s is an administrator on %s.", user.AccountName, m.stone.Name)
	if user.MemberRole == client.RoleAdmin {
		role, title = client.RoleStandard, i18n.Rune("Make user")
		question = i18n.Rune("Make {name} a user again? They keep their rooms, but no longer manage the stone.", "name", user.AccountName)
		done = i18n.T("%s is a user on %s.", user.AccountName, m.stone.Name)
	}
	m.confirm(title, question, func(m *model) tea.Cmd {
		return m.act(done, "rooms", func(ctx context.Context, stone *service.Stone) error {
			return stone.Do(ctx, func(token string) error { return stone.Client.SetRole(ctx, token, user.MemberID, role) })
		})
	})
}

func (m *model) deleteUser(user client.Member) {
	m.confirm(i18n.Rune("Delete user"), i18n.Rune("Delete {name}? Their account and memberships are removed for good; the rooms and their files stay.", "name", user.AccountName), func(m *model) tea.Cmd {
		return m.act(i18n.T("Deleted the user %q on %s.", user.AccountName, m.stone.Name), "rooms", func(ctx context.Context, stone *service.Stone) error {
			return stone.Do(ctx, func(token string) error { return stone.Client.DeleteMember(ctx, token, user.MemberID) })
		})
	})
}

// roomAvatar chooses the room's picture: a place or thing, as in Rune.
func (m *model) roomAvatar(room service.Room) {
	set := func(picture string) func(m *model) tea.Cmd {
		return func(m *model) tea.Cmd {
			return m.act("", "rooms", func(ctx context.Context, stone *service.Stone) error {
				return stone.Do(ctx, func(token string) error {
					return stone.Client.UpdateCircle(ctx, token, room.ID, client.ProcessCircleRequest{Avatar: &picture})
				})
			})
		}
	}
	items := []item{{label: i18n.T("No picture"), do: set("")}}
	cursor := 0
	for i, picture := range service.RoomAvatars {
		if picture == room.Avatar {
			cursor = i + 1
		}
		items = append(items, item{label: service.AvatarName(picture), do: set(picture)})
	}
	m.menu(i18n.Rune("Avatar of {room}", "room", room.Name), items, cursor)
}
