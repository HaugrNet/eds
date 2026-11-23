package fakestone

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/ws"
)

// Features, their entries, the socket and the status, as far as Tolk's tests
// need them: the fake stone stores what it is given without looking inside,
// and enforces the rules of a feature's declaration, as a stone does.

type kindRule struct {
	Add    string   `json:"add"`
	Change []string `json:"change"`
	Remove []string `json:"remove"`
	Close  []string `json:"close"`
	Keep   string   `json:"keep"`
	About  string   `json:"about"`
	Size   int      `json:"size"`
}

// declaration is the declaration of a feature. Signals is the least time
// between two signals: 0 for none, and nil for a feature without signals.
type declaration struct {
	Feature string              `json:"feature"`
	Version int                 `json:"version"`
	Signals *int                `json:"signals"`
	Kinds   map[string]kindRule `json:"kinds"`
	text    string
}

type featureEntry struct {
	id, roomID, feature, kind, about, name, author string
	position                                       int64
	added, changed                                 string
	closed                                         bool
	content                                        string
}

type socketConn struct {
	conn   *ws.Conn
	me     *account
	joined map[string]bool
	// shown are the rooms in which the connection shows its member as
	// present: joined, and not unseen.
	shown map[string]bool
}

func now() string { return time.Now().UTC().Format("2006-01-02T15:04:05.000000") }

func (e *featureEntry) json(s *Stone, withContent bool) response {
	out := response{
		"entryId": e.id, "circleId": e.roomID, "feature": e.feature, "kind": e.kind, "position": e.position,
		"added": e.added, "closed": e.closed, "accountName": e.author,
	}
	if a := s.accounts[e.author]; a != nil {
		out["memberId"] = a.id
	}
	if e.about != "" {
		out["about"] = e.about
	}
	if e.name != "" {
		out["name"] = e.name
	}
	if e.changed != "" {
		out["changed"] = e.changed
	}
	if withContent {
		out["content"] = e.content
	}
	return out
}

// OfferFeature lets the stone offer a feature, by its declaration, as its System
// Administrator would.
func (s *Stone) OfferFeature(text []byte) {
	s.mu.Lock()
	defer s.mu.Unlock()
	var d declaration
	if json.Unmarshal(text, &d) == nil && d.Feature != "" {
		d.text = string(text)
		s.features[d.Feature] = &d
	}
}

// SwitchFeature sets the state of a feature in a room: ON, READONLY, OFF, or ""
// to remove it.
func (s *Stone) SwitchFeature(roomName, feature, state string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, r := range s.rooms {
		if r.name == roomName {
			s.switchFeature(r, feature, state)
		}
	}
}

func (s *Stone) switchFeature(r *room, feature, state string) {
	if state == "" {
		delete(r.features, feature)
		kept := s.featureEntries[:0]
		for _, e := range s.featureEntries {
			if e.roomID != r.id || e.feature != feature {
				kept = append(kept, e)
			}
		}
		s.featureEntries = kept
	} else {
		r.features[feature] = state
	}
	s.announce(r.id, nil, response{"type": "FEATURE", "circleId": r.id, "feature": feature})
}

func (s *Stone) roomFeatures(r *room) []response {
	names := make([]string, 0, len(r.features))
	for name := range r.features {
		names = append(names, name)
	}
	sort.Strings(names)
	list := []response{}
	for _, name := range names {
		version := 0
		if d := s.features[name]; d != nil {
			version = d.Version
		}
		list = append(list, response{"name": name, "version": version, "state": r.features[name]})
	}
	return list
}

func (s *Stone) roomByName(name string) *room {
	for _, r := range s.rooms {
		if r.name == name {
			return r
		}
	}
	return nil
}

func (s *Stone) entryByID(id string) *featureEntry {
	for _, e := range s.featureEntries {
		if e.id == id {
			return e
		}
	}
	return nil
}

// may says whether an account may do what a rule names: "author", "adders"
// (everyone who may add the kind) or "administrators".
func may(who []string, rule kindRule, level string, author bool) bool {
	for _, w := range who {
		switch w {
		case "author":
			if author {
				return true
			}
		case "adders":
			if levels[level] >= levels[rule.Add] {
				return true
			}
		case "administrators":
			if level == "ADMIN" {
				return true
			}
		}
	}
	return false
}

// feature finds the feature of a room for an account, and what it may do there.
func (s *Stone) feature(me *account, roomID, feature string, write bool) (*room, *declaration, string, int, string) {
	if me == nil {
		return nil, nil, "", 403, "The Session has expired."
	}
	r := s.rooms[roomID]
	if r == nil || r.members[me.name] == "" {
		return nil, nil, "", 401, "The current Account is not allowed to perform the given action."
	}
	d := s.features[feature]
	state := r.features[feature]
	if d == nil || state == "" || state == "OFF" {
		return nil, nil, "", 404, "The Circle doesn't have the feature '" + feature + "'."
	}
	if write && state != "ON" {
		return nil, nil, "", 405, "The feature '" + feature + "' is read-only in this Circle."
	}
	return r, d, r.members[me.name], 200, ""
}

func (s *Stone) addEntry(me *account, roomID, feature, kind, content, about, name string) (*featureEntry, int, string) {
	r, d, level, code, message := s.feature(me, roomID, feature, true)
	if code != 200 {
		return nil, code, message
	}
	rule, ok := d.Kinds[kind]
	if !ok {
		return nil, 406, "The kind '" + kind + "' isn't declared."
	}
	if levels[level] < levels[rule.Add] {
		return nil, 401, "The current Account is not allowed to perform the given action."
	}
	raw, err := base64.StdEncoding.DecodeString(content)
	if err != nil {
		return nil, 406, "The content is not Base64."
	}
	if rule.Size > 0 && len(raw) > rule.Size {
		return nil, 413, "The content is larger than the kind allows."
	}
	if rule.About != "" {
		target := s.entryByID(about)
		if target == nil || target.roomID != r.id || target.feature != feature || target.kind != rule.About {
			return nil, 406, "The entry is about one which isn't there."
		}
		if target.closed {
			return nil, 405, "The entry it is about is closed."
		}
	} else if about != "" {
		return nil, 406, "The kind '" + kind + "' is about nothing."
	}
	// One per Member, or one per Member about each entry: the earlier one goes.
	if rule.Keep == "member" || rule.Keep == "member-about" {
		kept := s.featureEntries[:0]
		for _, e := range s.featureEntries {
			same := e.roomID == r.id && e.feature == feature && e.kind == kind && e.author == me.name && (rule.Keep == "member" || e.about == about)
			if same {
				s.announce(r.id, nil, response{"type": "REMOVED", "circleId": r.id, "feature": feature, "entry": e.json(s, false)})
			} else {
				kept = append(kept, e)
			}
		}
		s.featureEntries = kept
	}
	s.positions[r.id+"|"+feature]++
	e := &featureEntry{
		id: s.id("e"), roomID: r.id, feature: feature, kind: kind, about: about, name: name, author: me.name,
		position: s.positions[r.id+"|"+feature], added: now(), content: content,
	}
	s.featureEntries = append(s.featureEntries, e)
	s.announce(r.id, nil, response{"type": "ADDED", "circleId": r.id, "feature": feature, "entry": e.json(s, true)})
	return e, 200, ""
}

// own finds an entry for a change, and says whether the account may make
// it: which names the rule's list ("change", "remove", "close").
func (s *Stone) own(me *account, roomID, feature, entryID, which string) (*featureEntry, int, string) {
	_, d, level, code, message := s.feature(me, roomID, feature, true)
	if code != 200 {
		return nil, code, message
	}
	e := s.entryByID(entryID)
	if e == nil || e.roomID != roomID || e.feature != feature {
		return nil, 404, "The entry could not be found."
	}
	rule := d.Kinds[e.kind]
	who := map[string][]string{"change": rule.Change, "remove": rule.Remove, "close": rule.Close}[which]
	if !may(who, rule, level, e.author == me.name) {
		return nil, 401, "The current Account is not allowed to perform the given action."
	}
	if which != "close" {
		if e.closed {
			return nil, 405, "The entry is closed."
		}
		if target := s.entryByID(e.about); target != nil && target.closed {
			return nil, 405, "The entry it is about is closed."
		}
	}
	return e, 200, ""
}

func (s *Stone) changeEntry(me *account, roomID, feature, entryID string, content, name *string) (*featureEntry, int, string) {
	e, code, message := s.own(me, roomID, feature, entryID, "change")
	if code != 200 {
		return nil, code, message
	}
	if content != nil {
		if _, err := base64.StdEncoding.DecodeString(*content); err != nil {
			return nil, 406, "The content is not Base64."
		}
		e.content = *content
	}
	if name != nil {
		e.name = *name
	}
	e.changed = now()
	s.announce(roomID, nil, response{"type": "CHANGED", "circleId": roomID, "feature": feature, "entry": e.json(s, true)})
	return e, 200, ""
}

func (s *Stone) removeEntry(me *account, roomID, feature, entryID string) ([]*featureEntry, int, string) {
	e, code, message := s.own(me, roomID, feature, entryID, "remove")
	if code != 200 {
		return nil, code, message
	}
	// What is about the entry goes with it, and what is about that.
	gone := map[string]bool{e.id: true}
	for grew := true; grew; {
		grew = false
		for _, other := range s.featureEntries {
			if gone[other.about] && !gone[other.id] {
				gone[other.id], grew = true, true
			}
		}
	}
	var removed []*featureEntry
	kept := s.featureEntries[:0]
	for _, other := range s.featureEntries {
		if gone[other.id] {
			if other != e {
				removed = append(removed, other)
			}
		} else {
			kept = append(kept, other)
		}
	}
	s.featureEntries = kept
	removed = append(removed, e)
	for _, r := range removed {
		s.announce(roomID, nil, response{"type": "REMOVED", "circleId": roomID, "feature": feature, "entry": r.json(s, false)})
	}
	return removed, 200, ""
}

func (s *Stone) closeEntry(me *account, roomID, feature, entryID string, closed bool) (*featureEntry, int, string) {
	e, code, message := s.own(me, roomID, feature, entryID, "close")
	if code != 200 {
		return nil, code, message
	}
	e.closed = closed
	s.announce(roomID, nil, response{"type": "CHANGED", "circleId": roomID, "feature": feature, "entry": e.json(s, true)})
	return e, 200, ""
}

type entryQuery struct {
	kind, about, name string
	after, before     int64
	oldestFirst, bare bool
	limit             int
}

func (s *Stone) listEntries(me *account, roomID, feature string, q entryQuery) ([]response, int, string) {
	_, _, _, code, message := s.feature(me, roomID, feature, false)
	if code != 200 {
		return nil, code, message
	}
	var found []*featureEntry
	for _, e := range s.featureEntries {
		if e.roomID != roomID || e.feature != feature || (q.kind != "" && e.kind != q.kind) || (q.about != "" && e.about != q.about) ||
			(q.name != "" && !strings.HasPrefix(e.name, q.name)) || (q.after > 0 && e.position <= q.after) || (q.before > 0 && e.position >= q.before) {
			continue
		}
		found = append(found, e)
	}
	sort.Slice(found, func(i, j int) bool {
		if q.oldestFirst {
			return found[i].position < found[j].position
		}
		return found[i].position > found[j].position
	})
	limit := q.limit
	if limit <= 0 || limit > 200 {
		limit = 50
	}
	if len(found) > limit {
		found = found[:limit]
	}
	list := []response{}
	for _, e := range found {
		list = append(list, e.json(s, !q.bare))
	}
	return list, 200, ""
}

func (s *Stone) countEntries(me *account, roomID, feature, kind string, after int64) (int, int, string) {
	_, _, _, code, message := s.feature(me, roomID, feature, false)
	if code != 200 {
		return 0, code, message
	}
	count := 0
	for _, e := range s.featureEntries {
		if e.roomID == roomID && e.feature == feature && (kind == "" || e.kind == kind) && e.position > after {
			count++
		}
	}
	return count, 200, ""
}

// extra answers the operations for chambers, features and the status; false if
// the route isn't one of them.
func (s *Stone) extra(route string, parts []string, r *http.Request, in map[string]any, me *account, out response, fail func(int, string)) bool {
	str := func(key string) string { v, _ := in[key].(string); return v }
	optional := func(key string) *string {
		if v, ok := in[key].(string); ok {
			return &v
		}
		return nil
	}
	query := r.URL.Query()
	number := func(key string) int64 { n, _ := strconv.ParseInt(query.Get(key), 10, 64); return n }
	switch route {
	case "GET circles/*":
		if room := s.room(parts[1], me, "READ", fail); room != nil {
			out["circles"] = []response{{"circleId": room.id, "circleName": room.name, "avatar": room.avatar, "circleKey": room.circleKey}}
		}
	case "PUT circles/*/secrets":
		if room := s.room(parts[1], me, "ADMIN", fail); room != nil {
			list, _ := in["secrets"].([]any)
			// All or none: every Member named must be in the room.
			for _, item := range list {
				entry, _ := item.(map[string]any)
				id, _ := entry["memberId"].(string)
				if a := s.byID(id); a == nil || room.members[a.name] == "" {
					fail(404, "The Trustee could not be found.")
					return true
				}
			}
			for _, item := range list {
				entry, _ := item.(map[string]any)
				id, _ := entry["memberId"].(string)
				secret, _ := entry["secret"].(string)
				room.secrets[s.byID(id).name] = secret
			}
			if key := optional("circleKey"); key != nil {
				room.circleKey = *key
			}
		}
	case "GET features":
		if me == nil {
			fail(403, "The Session has expired.")
			break
		}
		names := make([]string, 0, len(s.features))
		for name := range s.features {
			names = append(names, name)
		}
		sort.Strings(names)
		list := []response{}
		for _, name := range names {
			list = append(list, response{"name": name, "version": s.features[name].Version, "declaration": s.features[name].text})
		}
		out["features"] = list
	case "PUT features/*", "DELETE features/*":
		if me == nil || !me.admin {
			fail(401, "Cannot complete this request, as it is only allowed for the System Administrator.")
			break
		}
		if r.Method == http.MethodDelete {
			for _, room := range s.rooms {
				if room.features[parts[1]] != "" {
					fail(409, "The feature is still used by a Circle.")
					return true
				}
			}
			delete(s.features, parts[1])
			break
		}
		var d declaration
		if json.Unmarshal([]byte(str("declaration")), &d) != nil || d.Feature != parts[1] || len(d.Kinds) == 0 {
			fail(406, "The declaration is invalid.")
			break
		}
		d.text = str("declaration")
		s.features[d.Feature] = &d
	case "GET circles/*/features":
		if room := s.room(parts[1], me, "READ", fail); room != nil {
			out["features"] = s.roomFeatures(room)
		}
	case "PUT circles/*/features/*", "DELETE circles/*/features/*":
		if room := s.room(parts[1], me, "ADMIN", fail); room != nil {
			if r.Method == http.MethodDelete {
				s.switchFeature(room, parts[3], "")
			} else if s.features[parts[3]] == nil {
				fail(404, "The stone doesn't offer the feature '"+parts[3]+"'.")
			} else if state := str("state"); state != "ON" && state != "READONLY" && state != "OFF" {
				fail(406, "The state is invalid.")
			} else {
				s.switchFeature(room, parts[3], state)
			}
			out["features"] = s.roomFeatures(room)
		}
	case "POST circles/*/features/*/entries":
		e, code, message := s.addEntry(me, parts[1], parts[3], str("kind"), str("content"), str("about"), str("name"))
		if code != 200 {
			fail(code, message)
			break
		}
		out["entries"] = []response{e.json(s, true)}
	case "GET circles/*/features/*/entries":
		limit, _ := strconv.Atoi(query.Get("limit"))
		list, code, message := s.listEntries(me, parts[1], parts[3], entryQuery{
			kind: query.Get("kind"), about: query.Get("about"), name: query.Get("name"), after: number("after"), before: number("before"),
			oldestFirst: query.Get("oldestFirst") == "true", bare: query.Get("content") == "false", limit: limit,
		})
		if code != 200 {
			fail(code, message)
			break
		}
		out["entries"] = list
	case "GET circles/*/features/*/count":
		count, code, message := s.countEntries(me, parts[1], parts[3], query.Get("kind"), number("after"))
		if code != 200 {
			fail(code, message)
			break
		}
		out["count"] = count
	case "GET circles/*/features/*/entries/*":
		if _, _, _, code, message := s.feature(me, parts[1], parts[3], false); code != 200 {
			fail(code, message)
		} else if e := s.entryByID(parts[5]); e == nil || e.roomID != parts[1] || e.feature != parts[3] {
			fail(404, "The entry could not be found.")
		} else {
			out["entries"] = []response{e.json(s, true)}
		}
	case "PUT circles/*/features/*/entries/*":
		e, code, message := s.changeEntry(me, parts[1], parts[3], parts[5], optional("content"), optional("name"))
		if code != 200 {
			fail(code, message)
			break
		}
		out["entries"] = []response{e.json(s, true)}
	case "DELETE circles/*/features/*/entries/*":
		removed, code, message := s.removeEntry(me, parts[1], parts[3], parts[5])
		if code != 200 {
			fail(code, message)
			break
		}
		list := []response{}
		for _, e := range removed {
			list = append(list, e.json(s, false))
		}
		out["entries"] = list
	case "PUT circles/*/features/*/entries/*/closed":
		closed, _ := in["closed"].(bool)
		e, code, message := s.closeEntry(me, parts[1], parts[3], parts[5], closed)
		if code != 200 {
			fail(code, message)
			break
		}
		out["entries"] = []response{e.json(s, true)}
	case "GET status":
		bearer := strings.TrimPrefix(r.Header.Get("Authorization"), "Bearer ")
		byKey := false
		for _, key := range s.statusKeys {
			byKey = byKey || (key != "" && key == bearer)
		}
		if !byKey && me == nil {
			fail(403, "The status key or the session isn't accepted.")
			break
		}
		if !byKey && !me.admin {
			fail(401, "Cannot complete this request, as it is only allowed for the System Administrator.")
			break
		}
		s.status(out, number("since"), query.Get("history") == "true")
	case "GET status/keys", "POST status/keys", "DELETE status/keys/*":
		if me == nil || !me.admin {
			fail(401, "Cannot complete this request, as it is only allowed for the System Administrator.")
			break
		}
		switch r.Method {
		case http.MethodPost:
			label := str("label")
			if label == "" || s.statusKeys[label] != "" {
				fail(406, "The label is missing, or used already.")
				return true
			}
			s.statusKeys[label] = base64.RawURLEncoding.EncodeToString([]byte(fmt.Sprintf("status-key-%s-%04d-padding-to-32b", label, len(s.statusKeys))))
			out["key"] = s.statusKeys[label]
		case http.MethodDelete:
			if s.statusKeys[parts[2]] == "" {
				fail(404, "There is no status key with the label.")
				return true
			}
			delete(s.statusKeys, parts[2])
		}
		labels := make([]string, 0, len(s.statusKeys))
		for label := range s.statusKeys {
			labels = append(labels, label)
		}
		sort.Strings(labels)
		out["keys"] = labels
	default:
		return false
	}
	return true
}

// StatusKey returns the status key with a label, as its System
// Administrator got it.
func (s *Stone) StatusKey(label string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.statusKeys[label]
}

// AddStatusKey makes a status key with a label and returns it.
func (s *Stone) AddStatusKey(label string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.statusKeys[label] = "status-key-of-" + label
	return s.statusKeys[label]
}

// Load sets what the stone tells of its load in its next samples: the
// requests since the last sample and its share of the processors.
func (s *Stone) Load(requests, share float64) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.loadRequests, s.loadShare = requests, share
	s.sequence++
}

// Asleep makes the stone tell that it is locked and waits for its
// MasterKey.
func (s *Stone) Asleep(asleep bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.asleep = asleep
}

func (s *Stone) status(out response, since int64, history bool) {
	if s.sequence == 0 {
		s.sequence = 3
	}
	out["name"], out["version"], out["awake"] = s.settings["eds.stone.name"], "2.0-TEST", !s.asleep
	out["time"], out["started"] = now(), s.started
	out["database"], out["persistent"] = "H2", false
	samples := []response{}
	for sequence := max(1, s.sequence-2); sequence <= s.sequence; sequence++ {
		if sequence <= since {
			continue
		}
		samples = append(samples, response{"sequence": sequence, "time": now(), "values": response{
			"requests": s.loadRequests, "failed": 0, "requestMillis": 4.5, "requestMillisMax": 12, "databaseMillis": 0.8,
			"accounts": len(s.sessions), "connections": len(s.sockets), "processShare": s.loadShare, "systemLoad": 1.5, "processors": 4,
			"memoryUsed": 64 << 20, "memoryMax": 1 << 30, "threads": 30,
		}})
	}
	out["load"] = response{"interval": 5, "samples": samples}
	stored := 0
	for _, e := range s.entries {
		stored += len(e.Content)
	}
	inventory := response{"sequence": 1, "time": s.started, "values": response{
		"storageUsed": stored, "storageCeiling": 2 << 30, "accounts": len(s.accounts), "circles": len(s.rooms),
		"files": len(s.entries), "entries": len(s.featureEntries), "sessions": len(s.sessions),
	}}
	out["inventory"] = response{"interval": 3600, "samples": []response{inventory}}
	if history {
		out["day"] = response{"interval": 300, "samples": []response{{"sequence": 1, "time": s.started, "values": response{
			"requests": s.loadRequests * 60, "processShare": s.loadShare, "processShareMax": s.loadShare, "samples": 60,
		}}}}
	}
}

// --- The socket --------------------------------------------------------------------

// announce tells the connections which joined a room; except leaves one out
// (nil for none).
func (s *Stone) announce(roomID string, except *socketConn, frame response) {
	data, _ := json.Marshal(frame)
	for c := range s.sockets {
		if c != except && c.joined[roomID] {
			_ = c.conn.WriteText(string(data))
		}
	}
}

// show sets whether a connection is shown in a room, and tells the others
// who joined when the member came or went by that: with their first shown
// connection, or their last.
func (s *Stone) show(c *socketConn, roomID string, shown bool) {
	if c.me == nil || c.shown[roomID] == shown {
		return
	}
	if shown {
		c.shown[roomID] = true
	} else {
		delete(c.shown, roomID)
	}
	for other := range s.sockets {
		if other != c && other.me == c.me && other.shown[roomID] {
			return
		}
	}
	kind := "ABSENT"
	if shown {
		kind = "PRESENT"
	}
	s.announce(roomID, c, response{"type": kind, "circleId": roomID, "memberId": c.me.id, "accountName": c.me.name})
}

// present lists the members present in a room, by their IDs, in order.
func (s *Stone) present(roomID string) []string {
	members := map[string]bool{}
	for c := range s.sockets {
		if c.me != nil && c.shown[roomID] {
			members[c.me.id] = true
		}
	}
	list := make([]string, 0, len(members))
	for id := range members {
		list = append(list, id)
	}
	sort.Strings(list)
	return list
}

// RoomID returns the ID of a room; empty if there is none of the name.
func (s *Stone) RoomID(roomName string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r := s.roomByName(roomName); r != nil {
		return r.id
	}
	return ""
}

// Present returns the names of the accounts which are present in a room,
// sorted.
func (s *Stone) Present(roomName string) []string {
	s.mu.Lock()
	defer s.mu.Unlock()
	names := []string{}
	if r := s.roomByName(roomName); r != nil {
		for _, id := range s.present(r.id) {
			if a := s.byID(id); a != nil {
				names = append(names, a.name)
			}
		}
	}
	sort.Strings(names)
	return names
}

// Push sends a frame to everyone who joined a room, as the stone would.
func (s *Stone) Push(roomName string, frame map[string]any) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r := s.roomByName(roomName); r != nil {
		frame["circleId"] = r.id
		s.announce(r.id, nil, frame)
	}
}

// EndChats closes every socket connection with a close code, as the stone
// does when a session ends (1008) or it restarts.
func (s *Stone) EndChats(code int) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for c := range s.sockets {
		_ = c.conn.Close(code, "the session ended")
	}
}

// Listeners returns how many connections have joined a room.
func (s *Stone) Listeners(roomName string) int {
	s.mu.Lock()
	defer s.mu.Unlock()
	count := 0
	if r := s.roomByName(roomName); r != nil {
		for c := range s.sockets {
			if c.joined[r.id] {
				count++
			}
		}
	}
	return count
}

func (s *Stone) socket(w http.ResponseWriter, r *http.Request) {
	conn, err := ws.Accept(w, r)
	if err != nil {
		return
	}
	c := &socketConn{conn: conn, joined: map[string]bool{}, shown: map[string]bool{}}
	defer func() {
		s.mu.Lock()
		// The connection went: its member is no longer present where this
		// was their last shown connection.
		for roomID := range c.shown {
			s.show(c, roomID, false)
		}
		delete(s.sockets, c)
		s.mu.Unlock()
		_ = conn.Close(1000, "")
	}()
	for {
		text, err := conn.ReadText()
		if err != nil {
			return
		}
		var in map[string]any
		if json.Unmarshal([]byte(text), &in) != nil {
			return
		}
		s.mu.Lock()
		out := s.socketFrame(c, in)
		if out != nil {
			if ref, ok := in["ref"].(string); ok {
				out["ref"] = ref
			}
			data, _ := json.Marshal(out)
			_ = conn.WriteText(string(data))
		}
		s.mu.Unlock()
	}
}

func (s *Stone) socketFrame(c *socketConn, in map[string]any) response {
	str := func(key string) string { v, _ := in[key].(string); return v }
	optional := func(key string) *string {
		if v, ok := in[key].(string); ok {
			return &v
		}
		return nil
	}
	number := func(key string) int64 { v, _ := in[key].(float64); return int64(v) }
	fail := func(code int, message string) response {
		return response{"type": "ERROR", "returnCode": code, "returnMessage": message}
	}
	kind := str("type")
	if kind == "AUTH" {
		c.me = s.accounts[s.sessions[str("token")]]
		if c.me == nil {
			return fail(403, "The Session has expired.")
		}
		s.sockets[c] = true
		return response{"type": "READY", "memberId": c.me.id, "accountName": c.me.name}
	}
	if c.me == nil || s.accounts[c.me.name] == nil {
		return fail(403, "The Session has expired.")
	}
	roomID, feature := str("circleId"), str("feature")
	room := s.rooms[roomID]
	if room == nil || room.members[c.me.name] == "" {
		return fail(401, "The current Account is not allowed to perform the given action.")
	}
	switch kind {
	case "JOIN":
		c.joined[roomID] = true
		unseen, _ := in["unseen"].(bool)
		s.show(c, roomID, !unseen)
		return response{"type": "JOINED", "circleId": roomID, "features": s.roomFeatures(room), "present": s.present(roomID)}
	case "LEAVE":
		s.show(c, roomID, false)
		delete(c.joined, roomID)
		return response{"type": "LEFT", "circleId": roomID}
	}
	if !c.joined[roomID] {
		return fail(405, "The Circle wasn't joined.")
	}
	done := func(entries ...*featureEntry) response {
		list := []response{}
		for _, e := range entries {
			list = append(list, e.json(s, true))
		}
		return response{"type": "DONE", "circleId": roomID, "feature": feature, "entries": list}
	}
	switch kind {
	case "ADD":
		e, code, message := s.addEntry(c.me, roomID, feature, str("kind"), str("content"), str("about"), str("name"))
		if code != 200 {
			return fail(code, message)
		}
		return done(e)
	case "CHANGE":
		e, code, message := s.changeEntry(c.me, roomID, feature, str("entryId"), optional("content"), optional("name"))
		if code != 200 {
			return fail(code, message)
		}
		return done(e)
	case "REMOVE":
		removed, code, message := s.removeEntry(c.me, roomID, feature, str("entryId"))
		if code != 200 {
			return fail(code, message)
		}
		out := done()
		list := []response{}
		for _, e := range removed {
			list = append(list, e.json(s, false))
		}
		out["entries"] = list
		return out
	case "CLOSE":
		closed, _ := in["closed"].(bool)
		e, code, message := s.closeEntry(c.me, roomID, feature, str("entryId"), closed)
		if code != 200 {
			return fail(code, message)
		}
		return done(e)
	case "GET":
		if _, _, _, code, message := s.feature(c.me, roomID, feature, false); code != 200 {
			return fail(code, message)
		}
		e := s.entryByID(str("entryId"))
		if e == nil || e.roomID != roomID || e.feature != feature {
			return fail(404, "The entry could not be found.")
		}
		return done(e)
	case "LIST":
		oldest, _ := in["oldestFirst"].(bool)
		bare, _ := in["bare"].(bool)
		list, code, message := s.listEntries(c.me, roomID, feature, entryQuery{
			kind: str("kind"), about: str("about"), name: str("name"), after: number("after"), before: number("before"),
			oldestFirst: oldest, bare: bare, limit: int(number("limit")),
		})
		if code != 200 {
			return fail(code, message)
		}
		return response{"type": "ENTRIES", "circleId": roomID, "feature": feature, "entries": list}
	case "COUNT":
		count, code, message := s.countEntries(c.me, roomID, feature, str("kind"), number("after"))
		if code != 200 {
			return fail(code, message)
		}
		return response{"type": "COUNTED", "circleId": roomID, "feature": feature, "count": count}
	case "SIGNAL":
		if d := s.features[feature]; d != nil && d.Signals != nil && room.features[feature] == "ON" {
			frame := response{"type": "SIGNAL", "circleId": roomID, "feature": feature, "signal": str("signal"), "memberId": c.me.id, "accountName": c.me.name}
			to := str("to")
			if to == "" {
				s.announce(roomID, c, frame)
				return nil
			}
			// For one member: their connections which joined, shown or
			// unseen, and nobody else.
			frame["to"] = to
			data, _ := json.Marshal(frame)
			for other := range s.sockets {
				if other != c && other.me != nil && other.me.id == to && other.joined[roomID] {
					_ = other.conn.WriteText(string(data))
				}
			}
		}
		return nil
	}
	return fail(406, "unknown frame "+kind)
}

// Strip presents a chamber as an ordinary room, as a stone would which
// turned against its people: the shared value is gone.
func (s *Stone) Strip(roomName string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r := s.roomByName(roomName); r != nil {
		r.circleKey = ""
	}
}

// Secret returns the secret of a member of a room, as the stone holds it.
func (s *Stone) Secret(roomName, user string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r := s.roomByName(roomName); r != nil {
		return r.secrets[user]
	}
	return ""
}

// PutFile stores a file at the top of a room as an app would which sends
// the content as it is.
func (s *Stone) PutFile(roomName, name string, content []byte) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if r := s.roomByName(roomName); r != nil {
		e := &Entry{ID: s.id("d"), RoomID: r.id, FolderID: r.id, Name: name, Content: content, ETag: "\"plain\""}
		s.entries[e.ID] = e
	}
}
