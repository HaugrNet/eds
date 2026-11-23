// Package fakestone is a small stone in memory, for Tolk's tests: accounts,
// sessions, rooms with members, folders and files with content, behind the
// same operations of the API that Tolk's client uses. It keeps nothing
// encrypted and checks only what the tests need; the real EDS is the judge
// of the API, and Tolk is tried against it too.
package fakestone

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"regexp"
	"strconv"
	"strings"
	"sync"
)

// Stone is the fake stone.
type Stone struct {
	Server *httptest.Server

	mu       sync.Mutex
	next     int
	accounts map[string]*account // by name
	sessions map[string]string   // token to account name
	rooms    map[string]*room    // by ID
	entries  map[string]*Entry   // by ID
	invites  map[string]string   // token to account name
	settings map[string]string
	// Features: the declarations the stone offers, the entries of the rooms,
	// the last position of each room's feature, and the open sockets.
	features       map[string]*declaration
	featureEntries []*featureEntry
	positions      map[string]int64
	sockets        map[*socketConn]bool
	// The status: its keys by label, and what the stone tells of its load.
	statusKeys              map[string]string
	started                 string
	sequence                int64
	loadRequests, loadShare float64
	asleep                  bool
	// MaxSize refuses larger content with 413, when above zero.
	MaxSize int64
	// Uploads counts the content uploads, and Chunked those without a length.
	Uploads, Chunked int
}

type account struct {
	id, name, passphrase, avatar string
	admin                        bool
	first                        bool // the first System Administrator, who claimed the stone
	// For chambers: the public key, and the vault with the private one.
	publicKey, vault string
}

type room struct {
	id, name, avatar string
	members          map[string]string // account name to trust level
	// For chambers: the shared value, and the secrets by account name.
	circleKey string
	secrets   map[string]string
	features  map[string]string // name to state
}

func newRoom(id, name string) *room {
	return &room{id: id, name: name, members: map[string]string{}, secrets: map[string]string{}, features: map[string]string{}}
}

// Entry is a file or folder.
type Entry struct {
	ID, RoomID, FolderID, Name, MediaType string
	Folder                                bool
	Content                               []byte
	ETag                                  string
}

// New starts a fake stone with the System Administrator "admin".
func New(adminPassphrase string) *Stone {
	s := &Stone{
		accounts: map[string]*account{}, sessions: map[string]string{}, rooms: map[string]*room{},
		entries: map[string]*Entry{}, invites: map[string]string{},
		features: map[string]*declaration{}, positions: map[string]int64{}, sockets: map[*socketConn]bool{}, statusKeys: map[string]string{}, started: now(),
		settings: map[string]string{"eds.session.timeout.minutes": "480", "eds.crypto.hash.algorithm": "SHA_512", "eds.is.ready": "true", "eds.stone.name": ""},
	}
	s.accounts["admin"] = &account{id: s.id("m"), name: "admin", passphrase: adminPassphrase, admin: true, first: true}
	s.Server = httptest.NewServer(http.HandlerFunc(s.serve))
	// As a stone which Rune's System Administrator set up: it offers the
	// features Rune and Tolk know.
	s.OfferAll()
	return s
}

// URL is the stone's API root.
func (s *Stone) URL() string { return s.Server.URL + "/api/v1" }

// Close stops the stone.
func (s *Stone) Close() { s.Server.Close() }

func (s *Stone) id(prefix string) string {
	s.next++
	return fmt.Sprintf("%s-%04d", prefix, s.next)
}

// AddUser makes an account.
func (s *Stone) AddUser(name, passphrase string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.accounts[name] = &account{id: s.id("m"), name: name, passphrase: passphrase}
}

// AddRoom makes a room with members: name, trust level, name, trust level.
func (s *Stone) AddRoom(name string, members ...string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	r := newRoom(s.id("c"), name)
	for i := 0; i+1 < len(members); i += 2 {
		r.members[members[i]] = members[i+1]
	}
	// As a room made by Rune or Tolk: with the chat, if the stone offers it.
	if s.features["chat"] != nil {
		r.features["chat"] = "ON"
	}
	s.rooms[r.id] = r
	return r.id
}

// ExpireSessions makes every session invalid, as a restart of the stone or
// the end of their lifetime does.
func (s *Stone) ExpireSessions() {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.sessions = map[string]string{}
}

// Logins returns how many sessions are open.
func (s *Stone) Logins() int {
	s.mu.Lock()
	defer s.mu.Unlock()
	return len(s.sessions)
}

// File returns the content of the file at a path below a room, by names.
func (s *Stone) File(roomName string, path ...string) ([]byte, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, r := range s.rooms {
		if r.name != roomName {
			continue
		}
		folder := r.id
		for i, name := range path {
			var found *Entry
			for _, e := range s.entries {
				if e.RoomID == r.id && e.FolderID == folder && e.Name == name {
					found = e
				}
			}
			if found == nil {
				return nil, false
			}
			if i == len(path)-1 {
				return found.Content, !found.Folder
			}
			folder = found.ID
		}
	}
	return nil, false
}

type response map[string]any

func (s *Stone) serve(w http.ResponseWriter, r *http.Request) {
	path := strings.TrimPrefix(r.URL.Path, "/api/v1")
	if path == r.URL.Path {
		w.WriteHeader(404)
		return
	}
	parts := strings.Split(strings.Trim(path, "/"), "/")
	if path == "/socket" {
		s.socket(w, r)
		return
	}
	if len(parts) == 3 && parts[0] == "data" && parts[2] == "content" {
		// The body is read before the lock is taken, so several uploads
		// can arrive at the same time.
		var body []byte
		var err error
		if r.Method == http.MethodPut {
			body, err = io.ReadAll(r.Body)
		}
		s.mu.Lock()
		defer s.mu.Unlock()
		s.content(w, r, parts[1], body, err)
		return
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	var in map[string]any
	_ = json.NewDecoder(r.Body).Decode(&in)
	str := func(key string) string { v, _ := in[key].(string); return v }

	// The JSON operations answer 200 with the outcome in returnCode; the
	// ID-based ones answer with the return code as HTTP status.
	jsonStyle := false
	code, message, out := 200, "Ok", response{}
	fail := func(c int, m string) { code, message = c, m }
	me := s.accounts[s.sessions[strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")]]
	if me == nil && str("credentialType") == "SESSION" {
		me = s.accounts[s.sessions[str("credential")]]
	}
	needSession := func() bool {
		if me == nil {
			fail(403, "The Session has expired.")
			return false
		}
		return true
	}
	route := r.Method + " " + strings.Join(mask(parts), "/")
	switch route {
	case "POST version":
		jsonStyle = true
		out["version"], out["database"], out["persistent"] = "2.0-TEST", "H2", false
	case "POST members/login":
		jsonStyle = true
		a := s.accounts[str("accountName")]
		if a == nil || b64(a.passphrase) != str("credential") {
			fail(403, "Cannot authenticate.")
			break
		}
		token := b64(s.id("session"))
		s.sessions[token] = a.name
		out["session"], out["memberId"] = token, a.id
	case "POST members/logout":
		jsonStyle = true
		delete(s.sessions, str("credential"))
	case "POST trustees/fetchTrustees":
		jsonStyle = true
		if !needSession() {
			break
		}
		list := []response{}
		for _, room := range s.rooms {
			if only := str("circleId"); only != "" && only != room.id {
				continue
			}
			if level, ok := room.members[me.name]; ok {
				list = append(list, response{"memberId": me.id, "accountName": me.name, "publicKey": me.publicKey, "circleId": room.id, "circleName": room.name,
					"circleAvatar": room.avatar, "trustLevel": level, "secret": room.secrets[me.name]})
			}
		}
		out["trustees"] = list
	case "GET circles":
		if !needSession() {
			break
		}
		list := []response{}
		for _, room := range s.rooms {
			if _, in := room.members[me.name]; in || me.admin {
				list = append(list, response{"circleId": room.id, "circleName": room.name, "avatar": room.avatar, "circleKey": room.circleKey})
			}
		}
		out["circles"] = list
	case "POST circles":
		if !needSession() {
			break
		}
		room := newRoom(s.id("c"), str("circleName"))
		room.members[me.name] = "ADMIN"
		s.rooms[room.id] = room
		out["circleId"] = room.id
	case "PUT circles/*":
		if room := s.room(parts[1], me, "ADMIN", fail); room != nil {
			if name := str("circleName"); name != "" {
				room.name = name
			}
			if avatar, ok := in["avatar"].(string); ok {
				room.avatar = avatar
			}
		}
	case "GET circles/*/trustees":
		if room := s.room(parts[1], me, "READ", fail); room != nil {
			list := []response{}
			for name, level := range room.members {
				entry := response{"memberId": s.accounts[name].id, "accountName": name, "avatar": s.accounts[name].avatar, "publicKey": s.accounts[name].publicKey,
					"circleId": room.id, "circleName": room.name, "trustLevel": level}
				// Administrators see the secrets of all, others their own.
				if room.members[me.name] == "ADMIN" || name == me.name {
					entry["secret"] = room.secrets[name]
				}
				list = append(list, entry)
			}
			out["trustees"] = list
		}
	case "POST circles/*/trustees":
		if room := s.room(parts[1], me, "ADMIN", fail); room != nil {
			a := s.byID(str("memberId"))
			if a == nil {
				fail(404, "The Member could not be found.")
			} else if _, in := room.members[a.name]; in {
				fail(404, "The Member is already a trustee of the requested Circle.")
			} else {
				room.members[a.name] = str("trustLevel")
				room.secrets[a.name] = str("secret")
			}
		}
	case "PUT circles/*/trustees/*", "DELETE circles/*/trustees/*":
		a := s.byID(parts[3])
		need := "ADMIN"
		_, secretOnly := in["secret"].(string)
		secretOnly = secretOnly && str("trustLevel") == ""
		if a != nil && me != nil && a.name == me.name && (r.Method == "DELETE" || secretOnly) {
			need = "READ" // anyone may leave, and set their own secret
		}
		if room := s.room(parts[1], me, need, fail); room != nil {
			if a == nil || room.members[a.name] == "" {
				fail(404, "The Trustee could not be found.")
			} else if r.Method == "DELETE" {
				delete(room.members, a.name)
				delete(room.secrets, a.name)
				for c := range s.sockets {
					if c.me == a && c.joined[room.id] {
						s.show(c, room.id, false)
						delete(c.joined, room.id)
						data, _ := json.Marshal(response{"type": "LEFT", "circleId": room.id, "returnCode": 401})
						_ = c.conn.WriteText(string(data))
					}
				}
			} else {
				if secret, ok := in["secret"].(string); ok {
					room.secrets[a.name] = secret
				}
				if level := str("trustLevel"); level != "" {
					room.members[a.name] = level
				}
			}
		}
	case "GET members":
		if !needSession() {
			break
		}
		list := []response{}
		for _, a := range s.accounts {
			list = append(list, a.json())
		}
		out["members"] = list
	case "GET members/*":
		if !needSession() {
			break
		}
		a := s.byID(parts[1])
		if a == nil {
			fail(404, "The Member could not be found.")
			break
		}
		circles := []response{}
		for _, room := range s.rooms {
			if _, in := room.members[a.name]; in {
				circles = append(circles, response{"circleId": room.id, "circleName": room.name})
			}
		}
		member := a.json()
		if a == me {
			member["vault"] = a.vault // only the account itself gets its vault
		}
		out["members"], out["circles"] = []response{member}, circles
	case "POST members":
		if !needSession() {
			break
		}
		if !me.admin {
			fail(401, "Cannot complete this request, as it is only allowed for the System Administrator.")
			break
		}
		if str("memberRole") == "ADMIN" && !me.first {
			fail(401, "Only the first System Administrator may make, unmake or delete administrators.")
			break
		}
		a := &account{id: s.id("m"), name: str("newAccountName"), passphrase: unb64(str("newCredential")), admin: str("memberRole") == "ADMIN"}
		s.accounts[a.name] = a
		out["memberId"] = a.id
	case "PUT members/*":
		if !needSession() {
			break
		}
		a := s.byID(parts[1])
		switch {
		case !me.admin:
			fail(401, "Only Administrators may update the Role of a member.")
		case !me.first:
			fail(401, "Only the first System Administrator may make, unmake or delete administrators.")
		case a == nil:
			fail(404, "The requested Member cannot be found.")
		case a == me:
			fail(405, "It is not permitted to alter own account.")
		default:
			a.admin = str("memberRole") == "ADMIN"
		}
	case "DELETE members/*":
		if !needSession() {
			break
		}
		a := s.byID(parts[1])
		if !me.admin || a == nil {
			fail(401, "The current Account is not allowed to perform the given action.")
			break
		}
		if a.first {
			fail(405, "The System Administrator Account may not be deleted.")
			break
		}
		if a.admin && !me.first {
			fail(401, "Only the first System Administrator may make, unmake or delete administrators.")
			break
		}
		delete(s.accounts, a.name)
		for _, room := range s.rooms {
			delete(room.members, a.name)
		}
	case "POST members/inviteMember":
		jsonStyle = true
		if !needSession() {
			break
		}
		if str("memberRole") == "ADMIN" {
			fail(405, "An invitation makes a user; an administrator is made by creating the account, or by changing the role afterwards.")
			break
		}
		room := s.rooms[str("circleId")]
		if room == nil || room.members[me.name] != "ADMIN" {
			fail(401, "The current Account is not allowed to perform the given action.")
			break
		}
		name := str("newAccountName")
		if existing, exists := s.accounts[name]; exists {
			room.members[name] = str("trustLevel")
			room.secrets[name] = str("secret")
			out["memberId"] = existing.id
			break
		}
		token := b64(s.id("invitation"))
		s.invites[token] = name
		s.accounts[name] = &account{id: s.id("m"), name: name}
		room.members[name] = str("trustLevel")
		room.secrets[name] = str("secret")
		out["invitation"] = token
	case "POST members/updateMember":
		jsonStyle = true
		switch str("credentialType") {
		case "INVITATION":
			name := s.invites[str("credential")]
			if name == "" || name != str("accountName") {
				fail(403, "The invitation is no longer valid.")
				break
			}
			delete(s.invites, str("credential"))
			s.accounts[name].passphrase = unb64(str("newCredential"))
		case "PASSPHRASE":
			a := s.accounts[str("accountName")]
			if a == nil || b64(a.passphrase) != str("credential") {
				fail(403, "Cannot authenticate.")
				break
			}
			a.passphrase = unb64(str("newCredential"))
		default:
			if needSession() {
				if avatar, ok := in["avatar"].(string); ok {
					me.avatar = avatar
				}
				if key, ok := in["publicKey"].(string); ok {
					me.publicKey = key
				}
				if vault, ok := in["vault"].(string); ok {
					me.vault = vault
				}
			}
		}
	case "POST settings":
		jsonStyle = true
		if !needSession() {
			break
		}
		if !me.admin {
			fail(401, "Cannot complete this request, as it is only allowed for the System Administrator.")
			break
		}
		if changes, ok := in["settings"].(map[string]any); ok {
			for key, value := range changes {
				s.settings[key], _ = value.(string)
			}
		}
		out["settings"] = s.settings
		out["allowedValues"] = map[string][]string{"eds.crypto.hash.algorithm": {"SHA_384", "SHA_512"}}
	case "GET data":
		if room := s.room(r.URL.Query().Get("circleId"), me, "READ", fail); room != nil {
			s.list(room.id, room.id, r, out)
		}
	case "GET data/*":
		if e := s.entry(parts[1], me, "READ", fail); e != nil {
			s.list(e.RoomID, e.ID, r, out)
		}
	case "POST data":
		room := s.room(str("circleId"), me, "WRITE", fail)
		if room == nil {
			break
		}
		folder := str("folderId")
		if folder == "" {
			folder = room.id
		}
		if s.named(room.id, folder, str("dataName")) != nil {
			fail(409, "Another record with the same name already exists.")
			break
		}
		e := &Entry{ID: s.id("d"), RoomID: room.id, FolderID: folder, Name: str("dataName"), MediaType: str("mediaType"), Folder: str("typeName") == "folder"}
		s.entries[e.ID] = e
		out["dataId"] = e.ID
	case "PUT data/*":
		if e := s.entry(parts[1], me, "WRITE", fail); e != nil {
			name, folder := e.Name, e.FolderID
			if n := str("dataName"); n != "" {
				name = n
			}
			if f := str("folderId"); f != "" {
				if e.Folder {
					fail(405, "It is not permitted to move Folders.")
					break
				}
				folder = f
			}
			if other := s.named(e.RoomID, folder, name); other != nil && other != e {
				fail(409, "The name '"+name+"' provided is already being used in the given folder.")
				break
			}
			e.Name, e.FolderID = name, folder
		}
	case "DELETE data/*":
		if e := s.entry(parts[1], me, "WRITE", fail); e != nil {
			for _, other := range s.entries {
				if other.FolderID == e.ID {
					fail(409, "The Folder cannot be removed as it is not empty.")
				}
			}
			if code == 200 {
				delete(s.entries, e.ID)
			}
		}
	case "POST data/*/move":
		if e := s.entry(parts[1], me, "WRITE", fail); e != nil {
			if room := s.room(str("targetCircleId"), me, "WRITE", fail); room != nil {
				e.RoomID, e.FolderID = room.id, room.id
				if f := str("targetFolderId"); f != "" {
					e.FolderID = f
				}
				out["dataId"] = e.ID
			}
		}
	default:
		if !s.extra(route, parts, r, in, me, out, fail) {
			w.WriteHeader(404)
			return
		}
	}
	out["returnCode"], out["returnMessage"] = code, message
	w.Header().Set("Content-Type", "application/json")
	if !jsonStyle && code != 200 {
		w.WriteHeader(code)
	}
	_ = json.NewEncoder(w).Encode(out)
}

var generatedID = regexp.MustCompile(`^[a-z]+-\d{4}$`)

// mask replaces IDs in a path by *, to match routes.
func mask(parts []string) []string {
	out := append([]string{}, parts...)
	for i := range out {
		if generatedID.MatchString(out[i]) {
			out[i] = "*"
		}
	}
	// The names of features and the labels of status keys stand where IDs do.
	if len(out) >= 2 && out[0] == "features" {
		out[1] = "*"
	}
	if len(out) >= 4 && out[0] == "circles" && out[2] == "features" {
		out[3] = "*"
	}
	if len(out) == 3 && out[0] == "status" && out[1] == "keys" {
		out[2] = "*"
	}
	return out
}

var levels = map[string]int{"READ": 1, "WRITE": 2, "ADMIN": 3}

// room returns a room the account has at least the given access to.
func (s *Stone) room(id string, me *account, need string, fail func(int, string)) *room {
	if me == nil {
		fail(403, "The Session has expired.")
		return nil
	}
	room := s.rooms[id]
	if room == nil {
		fail(404, "The Circle could not be found.")
		return nil
	}
	if levels[room.members[me.name]] < levels[need] {
		message := "The current Account is not allowed to perform the given action."
		if need == "WRITE" {
			message = "The requesting Account is not permitted to Process Data."
		}
		fail(401, message)
		return nil
	}
	return room
}

func (s *Stone) entry(id string, me *account, need string, fail func(int, string)) *Entry {
	if me == nil {
		fail(403, "The Session has expired.")
		return nil
	}
	e := s.entries[id]
	if e == nil {
		fail(404, "The Data Object could not be found.")
		return nil
	}
	if s.room(e.RoomID, me, need, fail) == nil {
		return nil
	}
	return e
}

func (s *Stone) named(roomID, folderID, name string) *Entry {
	for _, e := range s.entries {
		if e.RoomID == roomID && e.FolderID == folderID && e.Name == name {
			return e
		}
	}
	return nil
}

func (s *Stone) byID(id string) *account {
	for _, a := range s.accounts {
		if a.id == id {
			return a
		}
	}
	return nil
}

func (a *account) json() response {
	role := "STANDARD"
	if a.admin {
		role = "ADMIN"
	}
	return response{"memberId": a.id, "accountName": a.name, "memberRole": role, "firstAdministrator": a.first, "avatar": a.avatar, "publicKey": a.publicKey}
}

// list writes a page of a folder's entries; the top level's folder ID is
// the room's own, as in EDS, where it is the root folder's.
func (s *Stone) list(roomID, folderID string, r *http.Request, out response) {
	var all []response
	for _, e := range s.entries {
		if e.RoomID == roomID && e.FolderID == folderID && !strings.HasPrefix(e.Name, ".") {
			item := response{"dataId": e.ID, "circleId": e.RoomID, "folderId": e.FolderID, "dataName": e.Name, "added": "2026-11-10T14:05:00"}
			if e.Folder {
				item["typeName"] = "folder"
			} else {
				item["typeName"], item["size"], item["mediaType"] = "data", len(e.Content), e.MediaType
			}
			all = append(all, item)
		}
	}
	size, _ := strconv.Atoi(r.URL.Query().Get("pageSize"))
	page, _ := strconv.Atoi(r.URL.Query().Get("pageNumber"))
	if size <= 0 || size > 100 {
		size = 100
	}
	if page <= 0 {
		page = 1
	}
	// A stable order, so pages don't overlap.
	for i := range all {
		for j := i + 1; j < len(all); j++ {
			if all[j]["dataId"].(string) < all[i]["dataId"].(string) {
				all[i], all[j] = all[j], all[i]
			}
		}
	}
	from, to := min((page-1)*size, len(all)), min(page*size, len(all))
	items := all[from:to]
	if items == nil {
		items = []response{}
	}
	out["metadata"], out["records"] = items, len(all)
}

// content serves uploads and downloads of a file's content.
func (s *Stone) content(w http.ResponseWriter, r *http.Request, id string, data []byte, readErr error) {
	me := s.accounts[s.sessions[strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")]]
	code, message := 200, "Ok"
	fail := func(c int, m string) { code, message = c, m }
	need := "READ"
	if r.Method == http.MethodPut {
		need = "WRITE"
	}
	e := s.entry(id, me, need, fail)
	answer := func(extra response) {
		if extra == nil {
			extra = response{}
		}
		extra["returnCode"], extra["returnMessage"] = code, message
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(code)
		if r.Method != http.MethodHead {
			_ = json.NewEncoder(w).Encode(extra)
		}
	}
	if e == nil {
		answer(nil)
		return
	}
	if e.Folder {
		fail(405, "Folders have no content.")
		answer(nil)
		return
	}
	if r.Method == http.MethodPut {
		s.Uploads++
		if r.ContentLength < 0 {
			s.Chunked++
		}
		if s.MaxSize > 0 && r.ContentLength > s.MaxSize {
			fail(413, fmt.Sprintf("The content is larger than the allowed %d bytes.", s.MaxSize))
			answer(nil)
			return
		}
		if readErr != nil {
			fail(406, "The upload failed and was rolled back: "+readErr.Error())
			answer(nil)
			return
		}
		if s.MaxSize > 0 && int64(len(data)) > s.MaxSize {
			fail(413, fmt.Sprintf("The content is larger than the allowed %d bytes.", s.MaxSize))
			answer(nil)
			return
		}
		e.Content = data
		e.ETag = `"` + s.id("etag") + `"`
		w.Header().Set("ETag", e.ETag)
		answer(response{"dataId": e.ID, "etag": e.ETag})
		return
	}
	content, status := e.Content, 200
	if spec := r.Header.Get("Range"); strings.HasPrefix(spec, "bytes=") && (r.Header.Get("If-Range") == "" || r.Header.Get("If-Range") == e.ETag) {
		from, _ := strconv.Atoi(strings.TrimSuffix(strings.TrimPrefix(spec, "bytes="), "-"))
		if from >= len(content) {
			w.Header().Set("Content-Range", fmt.Sprintf("bytes */%d", len(content)))
			fail(416, "The requested range is outside the content.")
			answer(nil)
			return
		}
		w.Header().Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", from, len(content)-1, len(content)))
		content, status = content[from:], 206
	}
	w.Header().Set("ETag", e.ETag)
	w.Header().Set("Content-Type", "application/octet-stream")
	w.Header().Set("Content-Length", strconv.Itoa(len(content)))
	w.WriteHeader(status)
	if r.Method != http.MethodHead {
		_, _ = w.Write(content)
	}
}

func b64(text string) string { return base64.StdEncoding.EncodeToString([]byte(text)) }

func unb64(text string) string {
	data, _ := base64.StdEncoding.DecodeString(text)
	return string(data)
}
