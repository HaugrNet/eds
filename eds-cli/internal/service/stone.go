package service

import (
	"context"
	"errors"
	"sort"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/address"
	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// Service is what one command works with: the stones of the container, and
// a session on each stone it uses.
type Service struct {
	access Access
	infos  []agent.StoneInfo
	groups map[string][]string
	loaded bool
	stones map[string]*Stone
}

// NewService makes the service for a command.
func NewService(access Access) *Service {
	return &Service{access: access, stones: map[string]*Stone{}}
}

// Close ends what the command itself started.
func (s *Service) Close() { s.access.Close() }

func (s *Service) load() error {
	if s.loaded {
		return nil
	}
	infos, groups, err := s.access.Stones()
	if err != nil {
		return err
	}
	s.infos, s.groups, s.loaded = infos, groups, true
	return nil
}

// Info returns a stone's entry, without logging in; an empty name means the
// default stone.
func (s *Service) Info(name string) (*agent.StoneInfo, error) {
	if err := s.load(); err != nil {
		return nil, err
	}
	for i := range s.infos {
		if (name != "" && s.infos[i].Name == name) || (name == "" && s.infos[i].Default) {
			return &s.infos[i], nil
		}
	}
	if name == "" {
		return nil, exit.New(exit.NotFound, "%s", i18n.T("No stones yet. Add one with 'tolk register stone <name>'."))
	}
	return nil, exit.New(exit.NotFound, "%s", i18n.T("No stone named %q.", name))
}

// Groups returns the groups of the container.
func (s *Service) Groups() (map[string][]string, error) {
	if err := s.load(); err != nil {
		return nil, err
	}
	return s.groups, nil
}

// Expand turns what a person wrote into addresses: an address stays one, and
// "@name" becomes the addresses of that group.
func (s *Service) Expand(targets []string) ([]address.Address, error) {
	var out []address.Address
	for _, target := range targets {
		if name, ok := address.IsGroup(target); ok {
			groups, err := s.Groups()
			if err != nil {
				return nil, err
			}
			members, ok := groups[name]
			if !ok {
				return nil, exit.New(exit.NotFound, "%s", i18n.T("No group named %q.", name))
			}
			for _, member := range members {
				a, err := address.Parse(member)
				if err != nil {
					return nil, exit.New(exit.Usage, "%s", i18n.T("The group %q has an invalid address: %v", name, err))
				}
				out = append(out, a)
			}
			continue
		}
		a, err := address.Parse(target)
		if err != nil {
			return nil, exit.New(exit.Usage, "%v", err)
		}
		out = append(out, a)
	}
	return out, nil
}

// Stone returns a stone with a session; an empty name means the default.
func (s *Service) Stone(ctx context.Context, name string) (*Stone, error) {
	info, err := s.Info(name)
	if err != nil {
		return nil, err
	}
	if stone, ok := s.stones[info.Name]; ok {
		return stone, nil
	}
	if info.Account == "" {
		return nil, exit.New(exit.Usage, "%s", i18n.T("The stone %q is kept with a status key only: 'tolk status' tells how it is, and nothing else can be done there.", info.Name))
	}
	url, token, err := s.access.Session(ctx, info.Name, false)
	if err != nil {
		var e *exit.Error
		if errors.As(err, &e) && e.ReturnCode == 403 {
			return nil, &exit.Error{Code: exit.Auth, ReturnCode: 403, ReturnMessage: e.ReturnMessage,
				Message: i18n.T("The stone %q refused the account or the passphrase. Change them with 'tolk update stone %s'.", info.Name, info.Name)}
		}
		return nil, err
	}
	stone := &Stone{Name: info.Name, URL: url, Account: info.Account, Client: client.New(url), access: s.access, token: token}
	s.stones[info.Name] = stone
	return stone, nil
}

// Stone is one stone with a session on it.
type Stone struct {
	Name    string
	URL     string
	Account string
	Client  *client.Client

	access Access
	token  string
	rooms  []Room
	users  []client.Member
	keys   chamberData
}

// Token returns the session token in use.
func (st *Stone) Token() string { return st.token }

// Do runs one operation with the session. When the stone answers that the
// session is no longer valid (expired, or the stone restarted), Do logs in
// again and runs it once more, so fn must be safe to repeat.
func (st *Stone) Do(ctx context.Context, fn func(token string) error) error {
	err := fn(st.token)
	var e *exit.Error
	if !errors.As(err, &e) || e.ReturnCode != 403 {
		return err
	}
	_, token, renewErr := st.access.Session(ctx, st.Name, true)
	if renewErr != nil {
		return renewErr
	}
	st.token = token
	return fn(token)
}

// Room is a room as the account sees it.
type Room struct {
	ID     string
	Name   string
	Avatar string
	Trust  string // READ, WRITE, ADMIN, SYSOP; empty when the account isn't in it
}

// In reports whether the account is a member of the room.
func (r Room) In() bool { return r.Trust != "" }

// CanWrite reports whether the account may write in the room.
func (r Room) CanWrite() bool { return r.Trust != "" && r.Trust != client.TrustRead }

// Administers reports whether the account administers the room.
func (r Room) Administers() bool { return r.Trust == client.TrustAdmin || r.Trust == client.TrustSysop }

// Rooms lists the rooms the account is in, and those it may see without
// being in them (the System Administrator sees all), sorted by name.
func (st *Stone) Rooms(ctx context.Context) ([]Room, error) {
	if st.rooms != nil {
		return st.rooms, nil
	}
	var trustees []client.Trustee
	var circles []client.Circle
	err := st.Do(ctx, func(token string) error {
		var err error
		if trustees, err = st.Client.MyTrustees(ctx, token); err != nil {
			return err
		}
		// Someone in no room may get "not found" here; that's no rooms.
		circles, err = st.Client.Circles(ctx, token)
		var e *exit.Error
		if errors.As(err, &e) && (e.ReturnCode == 404 || e.ReturnCode == 401) {
			circles, err = nil, nil
		}
		return err
	})
	if err != nil {
		return nil, err
	}
	rooms := []Room{}
	seen := map[string]bool{}
	for _, t := range trustees {
		rooms = append(rooms, Room{ID: t.CircleID, Name: t.CircleName, Avatar: t.CircleAvatar, Trust: t.TrustLevel})
		seen[t.CircleID] = true
	}
	for _, c := range circles {
		if !seen[c.CircleID] {
			rooms = append(rooms, Room{ID: c.CircleID, Name: c.CircleName, Avatar: c.Avatar})
		}
	}
	sort.Slice(rooms, func(i, j int) bool { return strings.ToLower(rooms[i].Name) < strings.ToLower(rooms[j].Name) })
	st.rooms = rooms
	return rooms, nil
}

// ForgetRooms makes the next Rooms ask the stone again.
func (st *Stone) ForgetRooms() { st.rooms = nil }

// Room finds a room by its name, or by its ID.
func (st *Stone) Room(ctx context.Context, nameOrID string) (*Room, error) {
	rooms, err := st.Rooms(ctx)
	if err != nil {
		return nil, err
	}
	var found *Room
	for i := range rooms {
		if rooms[i].Name == nameOrID {
			if found != nil {
				return nil, exit.New(exit.Usage, "%s", i18n.T("Several rooms on %q are named %q. Use the room's ID.", st.Name, nameOrID))
			}
			found = &rooms[i]
		}
	}
	if found != nil {
		return found, nil
	}
	for i := range rooms {
		if rooms[i].ID == nameOrID {
			return &rooms[i], nil
		}
	}
	return nil, exit.New(exit.NotFound, "%s", i18n.T("No room named %q on the stone %q.", nameOrID, st.Name))
}

// Users lists all users of the stone, sorted by name.
func (st *Stone) Users(ctx context.Context) ([]client.Member, error) {
	if st.users != nil {
		return st.users, nil
	}
	var users []client.Member
	err := st.Do(ctx, func(token string) error {
		var err error
		users, err = st.Client.Members(ctx, token)
		return err
	})
	if err != nil {
		return nil, err
	}
	sort.Slice(users, func(i, j int) bool {
		return strings.ToLower(users[i].AccountName) < strings.ToLower(users[j].AccountName)
	})
	st.users = users
	return users, nil
}

// FreshUsers asks the stone for the users again.
func (st *Stone) FreshUsers(ctx context.Context) ([]client.Member, error) {
	st.users = nil
	return st.Users(ctx)
}

// User finds a user by name, or by ID.
func (st *Stone) User(ctx context.Context, nameOrID string) (*client.Member, error) {
	users, err := st.Users(ctx)
	if err != nil {
		return nil, err
	}
	for i := range users {
		if users[i].AccountName == nameOrID {
			return &users[i], nil
		}
	}
	for i := range users {
		if users[i].MemberID == nameOrID || strings.EqualFold(users[i].AccountName, nameOrID) {
			return &users[i], nil
		}
	}
	return nil, exit.New(exit.NotFound, "%s", i18n.T("No user named %q on the stone %q.", nameOrID, st.Name))
}

// Me returns the account the stone is used with.
func (st *Stone) Me(ctx context.Context) (*client.Member, error) {
	return st.User(ctx, st.Account)
}

// Members lists the members of a room, administrators first, then by name.
func (st *Stone) Members(ctx context.Context, room *Room) ([]client.Trustee, error) {
	var trustees []client.Trustee
	err := st.Do(ctx, func(token string) error {
		var err error
		trustees, err = st.Client.Trustees(ctx, token, room.ID)
		return err
	})
	if err != nil {
		return nil, err
	}
	admin := func(t client.Trustee) bool {
		return t.TrustLevel == client.TrustAdmin || t.TrustLevel == client.TrustSysop
	}
	sort.SliceStable(trustees, func(i, j int) bool {
		if admin(trustees[i]) != admin(trustees[j]) {
			return admin(trustees[i])
		}
		return strings.ToLower(trustees[i].AccountName) < strings.ToLower(trustees[j].AccountName)
	})
	return trustees, nil
}
