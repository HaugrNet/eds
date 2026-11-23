package client

// The chat as Tolk works with it. The stone has no chat of its own any more:
// the chat is a feature (FEATURES.md), its messages are entries, and package chat
// turns them into the messages, receipts and events below, which the
// commands and the interactive mode always used.

// The kinds of events of the chat.
const (
	FrameMessage = "MESSAGE" // a message arrived
	FrameSent    = "SENT"    // the own message was stored
	FrameEdited  = "EDITED"  // the text of a message changed
	FrameReacted = "REACTED" // the reactions to a message changed
	FrameDeleted = "DELETED" // a message was deleted
	FrameReceipt = "RECEIPT" // a member received or read up to a place
	FrameTyping  = "TYPING"  // a member is writing
	FrameChanged = "CHANGED" // the files of a room changed
	FrameLeft    = "LEFT"    // access to a room ended
	FrameFeature = "FEATURE" // the features of a room were switched
	FramePresent = "PRESENT" // a member is online in a room now
	FrameAbsent  = "ABSENT"  // a member is no longer online in a room
	FrameCall    = "CALL"    // a member started the call of a room, or calls the account
)

// ChatFrame is one thing that happened in the chat of a room.
type ChatFrame struct {
	Type          string       `json:"type"`
	CircleID      string       `json:"circleId,omitempty"`
	MessageID     string       `json:"messageId,omitempty"`
	Message       *ChatMessage `json:"message,omitempty"`
	Receipt       *ChatReceipt `json:"receipt,omitempty"`
	MemberID      string       `json:"memberId,omitempty"`
	AccountName   string       `json:"accountName,omitempty"`
	ReturnCode    int          `json:"returnCode,omitempty"`
	ReturnMessage string       `json:"returnMessage,omitempty"`
	// Direct: a call for the account alone; else it is the call of the room.
	Direct bool `json:"direct,omitempty"`
}

// ChatMessage is a message in a room.
type ChatMessage struct {
	MessageID   string              `json:"messageId"`
	CircleID    string              `json:"circleId"`
	MemberID    string              `json:"memberId"`
	AccountName string              `json:"accountName"`
	Time        string              `json:"time"`
	Edited      string              `json:"edited,omitempty"`
	ReplyTo     string              `json:"replyTo,omitempty"`
	Text        string              `json:"text"`
	DataID      string              `json:"dataId,omitempty"`
	Reactions   map[string][]string `json:"reactions,omitempty"`
	// Damaged: the message can't be read; it is damaged, or of a chamber
	// whose key this device hasn't.
	Damaged bool `json:"damaged,omitempty"`
	// Position is its place in the chat, as the stone counts.
	Position int64 `json:"position,omitempty"`
}

// ChatReceipt says up to which place a member's client has received, and
// the member has read, the chat of a room.
type ChatReceipt struct {
	MemberID         string `json:"memberId"`
	AccountName      string `json:"accountName"`
	ReadPosition     int64  `json:"readPosition,omitempty"`
	ReceivedPosition int64  `json:"receivedPosition,omitempty"`
}

// Reactions are the reactions a stone accepts, in Rune's order.
var Reactions = []string{"👍", "❤️", "😂", "😮", "😢", "🙏"}

// The frame types of the socket (WebSocket at <API root>/socket), as in the
// contract.
const (
	SocketAuth    = "AUTH"
	SocketReady   = "READY"
	SocketJoin    = "JOIN"
	SocketJoined  = "JOINED"
	SocketLeave   = "LEAVE"
	SocketLeft    = "LEFT"
	SocketAdd     = "ADD"
	SocketChange  = "CHANGE"
	SocketRemove  = "REMOVE"
	SocketClose   = "CLOSE"
	SocketGet     = "GET"
	SocketList    = "LIST"
	SocketCount   = "COUNT"
	SocketSignal  = "SIGNAL"
	SocketDone    = "DONE"
	SocketEntries = "ENTRIES"
	SocketCounted = "COUNTED"
	SocketAdded   = "ADDED"
	SocketChanged = "CHANGED"
	SocketRemoved = "REMOVED"
	SocketFeature = "FEATURE"
	SocketFiles   = "FILES"
	SocketPresent = "PRESENT"
	SocketAbsent  = "ABSENT"
	SocketError   = "ERROR"
)

// SocketFrame is one frame of the socket, in either direction.
type SocketFrame struct {
	Type          string          `json:"type"`
	Ref           string          `json:"ref,omitempty"`
	Token         string          `json:"token,omitempty"`
	MemberID      string          `json:"memberId,omitempty"`
	AccountName   string          `json:"accountName,omitempty"`
	CircleID      string          `json:"circleId,omitempty"`
	Feature       string          `json:"feature,omitempty"`
	Kind          string          `json:"kind,omitempty"`
	EntryID       string          `json:"entryId,omitempty"`
	About         string          `json:"about,omitempty"`
	Name          *string         `json:"name,omitempty"`
	Content       *string         `json:"content,omitempty"`
	Closed        *bool           `json:"closed,omitempty"`
	After         int64           `json:"after,omitempty"`
	Before        int64           `json:"before,omitempty"`
	OldestFirst   bool            `json:"oldestFirst,omitempty"`
	Limit         int             `json:"limit,omitempty"`
	Bare          bool            `json:"bare,omitempty"`
	Signal        string          `json:"signal,omitempty"`
	To            string          `json:"to,omitempty"`      // the one Member a signal is for
	Unseen        bool            `json:"unseen,omitempty"`  // join without being shown as present
	Present       []string        `json:"present,omitempty"` // the Members present, in the answer to joining
	Entry         *Entry          `json:"entry,omitempty"`
	Entries       []Entry         `json:"entries,omitempty"`
	Count         int64           `json:"count,omitempty"`
	Features      []CircleFeature `json:"features,omitempty"`
	DataID        string          `json:"dataId,omitempty"`
	ReturnCode    int             `json:"returnCode,omitempty"`
	ReturnMessage string          `json:"returnMessage,omitempty"`
}

func init() {
	Types["SocketFrame"] = SocketFrame{}
}
