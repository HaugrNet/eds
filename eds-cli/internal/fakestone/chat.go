package fakestone

import (
	"encoding/base64"
	"encoding/json"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n/rune"
)

// The chat is a feature (FEATURES.md): its messages, reactions and receipts are
// entries. These helpers act as another member of a room would, so the
// tests can tell what Tolk shows of it. They write as an app does in a
// room which isn't a chamber: the content as it is.

// OfferChat lets the stone offer the chat, with the declaration Rune and
// Tolk bring.
func (s *Stone) OfferChat() { s.OfferFeature(rune.ChatFeature) }

// OfferAll lets the stone offer the five features Rune and Tolk know.
func (s *Stone) OfferAll() {
	for _, text := range [][]byte{rune.ChatFeature, rune.CalendarFeature, rune.LocationFeature, rune.PlansFeature, rune.CallsFeature} {
		s.OfferFeature(text)
	}
}

func encode(body map[string]any) string {
	data, _ := json.Marshal(body)
	return base64.StdEncoding.EncodeToString(data)
}

func decode(content string) map[string]any {
	var body map[string]any
	data, _ := base64.StdEncoding.DecodeString(content)
	_ = json.Unmarshal(data, &body)
	return body
}

// Messages returns the texts of a room's chat, oldest first. What isn't
// readable JSON (the content of a chamber) is returned as it is stored.
func (s *Stone) Messages(roomName string) []string {
	s.mu.Lock()
	defer s.mu.Unlock()
	var out []string
	r := s.roomByName(roomName)
	for _, e := range s.featureEntries {
		if r != nil && e.roomID == r.id && e.feature == "chat" && e.kind == "message" {
			body := decode(e.content)
			if text, ok := body["text"].(string); ok {
				// A shared file is told by its name.
				if file, ok := body["file"].(string); ok {
					if shared := s.entries[file]; shared != nil {
						text += " [file " + shared.Name + "]"
					}
				}
				out = append(out, text)
			} else {
				raw, _ := base64.StdEncoding.DecodeString(e.content)
				out = append(out, string(raw))
			}
		}
	}
	return out
}

// Stored returns the contents of the entries of a feature's kind in a room as
// the stone holds them, oldest first.
func (s *Stone) Stored(roomName, feature, kind string) [][]byte {
	s.mu.Lock()
	defer s.mu.Unlock()
	var out [][]byte
	r := s.roomByName(roomName)
	for _, e := range s.featureEntries {
		if r != nil && e.roomID == r.id && e.feature == feature && e.kind == kind {
			raw, _ := base64.StdEncoding.DecodeString(e.content)
			out = append(out, raw)
		}
	}
	return out
}

func (s *Stone) act(roomName, from string, do func(r *room, a *account) string) string {
	s.mu.Lock()
	defer s.mu.Unlock()
	r, a := s.roomByName(roomName), s.accounts[from]
	if r == nil || a == nil {
		return ""
	}
	return do(r, a)
}

// Say puts a message into a room's chat, as a member writing from another
// client; it returns the message's ID.
func (s *Stone) Say(roomName, from, text string) string {
	return s.act(roomName, from, func(r *room, a *account) string {
		e, _, _ := s.addEntry(a, r.id, "chat", "message", encode(map[string]any{"v": 1, "text": text}), "", "")
		if e == nil {
			return ""
		}
		return e.id
	})
}

// Edit replaces the text of a message, as its author would.
func (s *Stone) Edit(roomName, messageID, text string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if e := s.entryByID(messageID); e != nil {
		body := decode(e.content)
		body["text"] = text
		content := encode(body)
		_, _, _ = s.changeEntry(s.accounts[e.author], e.roomID, "chat", e.id, &content, nil)
	}
}

// React gives a member's reactions to a message: all of them, as the entry
// holds them.
func (s *Stone) React(roomName, messageID, from string, reactions ...string) {
	s.act(roomName, from, func(r *room, a *account) string {
		_, _, _ = s.addEntry(a, r.id, "chat", "reaction", encode(map[string]any{"v": 1, "reactions": reactions}), messageID, "")
		return ""
	})
}

// Delete removes a message, as its author would.
func (s *Stone) Delete(roomName, messageID string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if e := s.entryByID(messageID); e != nil {
		_, _, _ = s.removeEntry(s.accounts[e.author], e.roomID, "chat", e.id)
	}
}

// Read marks a room as read by a member up to its newest message.
func (s *Stone) Read(roomName, user string) {
	s.act(roomName, user, func(r *room, a *account) string {
		var last int64
		for _, e := range s.featureEntries {
			if e.roomID == r.id && e.feature == "chat" && e.kind == "message" {
				last = e.position
			}
		}
		_, _, _ = s.addEntry(a, r.id, "chat", "receipt", encode(map[string]any{"v": 1, "received": last, "read": last}), "", "")
		return ""
	})
}

// Typing tells a room that a member is writing.
func (s *Stone) Typing(roomName, user string) {
	s.act(roomName, user, func(r *room, a *account) string {
		s.announce(r.id, nil, response{"type": "SIGNAL", "circleId": r.id, "feature": "chat", "signal": "typing", "memberId": a.id, "accountName": a.name})
		return ""
	})
}
