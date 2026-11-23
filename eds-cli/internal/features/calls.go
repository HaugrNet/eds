package features

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"regexp"
)

// The feature "calls" (FEATURES.md, "Presence and calls"): voice and video
// between the people of a room, or between two of them. Tolk makes no
// calls, as a terminal has no WebRTC; it reads the messages which set a
// call up, to say that someone calls. Here: those messages, as the signals
// of the feature carry them. It says the same as Rune's
// src/features/calls.ts.

// RoomCall is the `call` of the call of a room; a room has one at a time.
// A direct call has a UUID.
const RoomCall = "room"

// callKind makes, with the feature, the context of a signal of a call as
// content of a chamber (E2E.md): "calls|signal".
const callKind = "signal"

// CallMessage is a message of a call, version 1. Fields a type doesn't use
// are left out.
type CallMessage struct {
	V int `json:"v"`
	// T is the type: ring, join, here, full, offer, answer, ice, leave,
	// decline, taken, probe, ongoing or state.
	T string `json:"t"`
	// Call is "room", or the UUID of a direct call.
	Call string `json:"call"`
	// Peer is the device which sends, and Target the one the message is for.
	Peer   string `json:"peer,omitempty"`
	Target string `json:"target,omitempty"`
	// Media is "audio" or "video".
	Media string `json:"media,omitempty"`
	// SDP is the session description of WebRTC, of an offer or an answer.
	SDP string `json:"sdp,omitempty"`
	// Candidate is a candidate of ICE, as WebRTC gives it.
	Candidate map[string]any `json:"candidate,omitempty"`
	// Peers is how many devices are in the call, of "ongoing".
	Peers *int `json:"peers,omitempty"`
	// Mic and Camera say whether the device sends sound and picture now, of
	// "state".
	Mic    *bool `json:"mic,omitempty"`
	Camera *bool `json:"camera,omitempty"`
}

var (
	callTypes = map[string]bool{"ring": true, "join": true, "here": true, "full": true, "offer": true, "answer": true, "ice": true,
		"leave": true, "decline": true, "taken": true, "probe": true, "ongoing": true, "state": true}
	// The types which come from a device in a call, and so name it.
	fromAPeer = map[string]bool{"join": true, "here": true, "full": true, "offer": true, "answer": true, "ice": true, "ongoing": true, "state": true}
	// The types which are for one device.
	forAPeer = map[string]bool{"here": true, "full": true, "offer": true, "answer": true, "ice": true}
	callID   = regexp.MustCompile(`^[0-9a-fA-F-]{8,64}$`)
)

// WriteCallMessage is the text of a message, as a signal carries it before
// it is encrypted or encoded.
func WriteCallMessage(message CallMessage) string {
	message.V = 1
	data, _ := json.Marshal(message)
	return string(data)
}

// ReadCallMessage reads a message; nil if the text isn't one this version
// knows, or lacks what its type needs. Fields it doesn't know are passed
// over.
func ReadCallMessage(text string) *CallMessage {
	var raw map[string]any
	if json.Unmarshal([]byte(text), &raw) != nil || raw == nil {
		return nil
	}
	str := func(key string) string { value, _ := raw[key].(string); return value }
	id := func(key string) string {
		if value := str(key); callID.MatchString(value) {
			return value
		}
		return ""
	}
	version, _ := raw["v"].(float64)
	call := str("call")
	if version != 1 || !callTypes[str("t")] || !(call == RoomCall || callID.MatchString(call)) {
		return nil
	}
	message := &CallMessage{V: 1, T: str("t"), Call: call, Peer: id("peer"), Target: id("target"), SDP: str("sdp")}
	if media := str("media"); media == "audio" || media == "video" {
		message.Media = media
	}
	if candidate, ok := raw["candidate"].(map[string]any); ok {
		message.Candidate = candidate
	}
	if peers, ok := raw["peers"].(float64); ok && peers >= 0 && peers == float64(int(peers)) {
		count := int(peers)
		message.Peers = &count
	}
	if mic, ok := raw["mic"].(bool); ok {
		message.Mic = &mic
	}
	if camera, ok := raw["camera"].(bool); ok {
		message.Camera = &camera
	}
	if (fromAPeer[message.T] && message.Peer == "") || (forAPeer[message.T] && message.Target == "") ||
		((message.T == "offer" || message.T == "answer") && message.SDP == "") || (message.T == "ice" && message.Candidate == nil) {
		return nil
	}
	return message
}

// CallSignal is a message as the signal of the feature carries it: Base64,
// in a chamber encrypted with the room key.
func CallSignal(ctx context.Context, sealer Sealer, circleID string, message CallMessage) (string, error) {
	return EncodeRaw(ctx, sealer, circleID, "calls", callKind, []byte(WriteCallMessage(message)))
}

// ReadCallSignal reads the signal of a call; nil if it can't be read
// (damaged, of a chamber whose key this device hasn't) or isn't a message.
func ReadCallSignal(ctx context.Context, sealer Sealer, circleID, signal string) *CallMessage {
	stored, err := base64.StdEncoding.DecodeString(signal)
	if err != nil || signal == "" {
		return nil
	}
	plain, err := sealer.Reveal(ctx, circleID, "calls|"+callKind, stored)
	if err != nil {
		return nil
	}
	return ReadCallMessage(string(plain))
}
