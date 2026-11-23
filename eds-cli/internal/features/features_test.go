package features

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"math"
	"sort"
	"strings"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n/rune"
)

func same(t *testing.T, what string, got, want any) {
	t.Helper()
	g, _ := json.Marshal(got)
	w, _ := json.Marshal(want)
	if !bytes.Equal(g, w) {
		t.Errorf("%s:\n got %s\nwant %s", what, g, w)
	}
}

// The examples of who owes whom, which Rune reproduces too
// (plans-examples.json, made by an independent implementation).
func TestPlansExamples(t *testing.T) {
	var doc struct {
		Examples []struct {
			Name     string                       `json:"name"`
			Expenses []Expense                    `json:"expenses"`
			Payments []Payment                    `json:"payments"`
			Balances map[string]map[string]string `json:"balances"`
			Settle   map[string][]Due             `json:"settle"`
		} `json:"examples"`
	}
	if err := json.Unmarshal(rune.PlansExamples, &doc); err != nil {
		t.Fatal(err)
	}
	if len(doc.Examples) < 7 {
		t.Fatalf("only %d examples", len(doc.Examples))
	}
	for _, example := range doc.Examples {
		worked := Balances(example.Expenses, example.Payments)
		shown := map[string]map[string]string{}
		dues := map[string][]Due{}
		for currency, of := range worked {
			shown[currency] = map[string]string{}
			for person, units := range of {
				shown[currency][person] = AmountText(units)
			}
			dues[currency] = Settle(of)
		}
		same(t, example.Name+": balances", shown, example.Balances)
		same(t, example.Name+": settling", dues, example.Settle)
	}
}

func TestPlans(t *testing.T) {
	amounts := []string{"12.5", "12,50", "0.07", "7", "1.234", "-1", "abc"}
	var read []any
	for _, amount := range amounts {
		if units, ok := Minor(amount); ok {
			read = append(read, units)
		} else {
			read = append(read, nil)
		}
	}
	same(t, "an amount in hundredths", read, []any{1250, 1250, 7, 700, nil, nil, nil})
	same(t, "hundredths as text", []string{AmountText(1250), AmountText(-7), AmountText(0)}, []string{"12.50", "-0.07", "0.00"})
	same(t, "a hundredth left over goes to the first", Parts(Expense{Amount: "10.00", Split: []Share{{"a", 1}, {"b", 1}, {"c", 1}}}), []int64{334, 333, 333})
	payment := Payment{V: 1, From: "m:a", To: "m:b", Amount: "5.00", Currency: "EUR"}
	same(t, "a confirmation by the receiver, after the last change", Confirms(payment, "2026-11-15T10:00:00", "2026-11-15T12:00:00", "b", "2026-11-15T12:00:01"), true)
	same(t, "one from before the change does not count", Confirms(payment, "2026-11-15T10:00:00", "2026-11-15T12:00:00", "b", "2026-11-15T11:00:00"), false)
	same(t, "nor one by someone else", Confirms(payment, "2026-11-15T10:00:00", "", "a", "2026-11-15T13:00:00"), false)
	same(t, "people", []string{MemberPerson("x"), GuestPerson("Dana"), PersonMember("m:x"), PersonMember("g:Dana")}, []string{"m:x", "g:Dana", "x", ""})
}

func starts(event *Event, from, to string) []string {
	var out []string
	for _, o := range Occurrences(event, Instant(from, ""), Instant(to, "")) {
		out = append(out, o.Start.In(time.Local).Format("2006-1-2"))
	}
	return out
}

func TestCalendar(t *testing.T) {
	stamp := time.Date(2026, 11, 15, 9, 0, 0, 0, time.UTC)
	dinner := &Event{UID: "dinner-1@rune", Title: `Dinner; with Åsa, at "Hearth"`, Start: "2026-11-20T19:00", End: "2026-11-20T21:30",
		Zone: "Europe/Berlin", Place: "Hearth, Main St 1", Note: "Bring wine\nand bread"}
	written := WriteEvent(dinner, stamp)
	same(t, "an event as iCalendar", strings.Split(written, "\r\n"), []string{
		"BEGIN:VEVENT", "UID:dinner-1@rune", "DTSTAMP:20261115T090000Z", "DTSTART;TZID=Europe/Berlin:20261120T190000", "DTEND;TZID=Europe/Berlin:20261120T213000",
		`SUMMARY:Dinner\; with Åsa\, at "Hearth"`, `LOCATION:Hearth\, Main St 1`, `DESCRIPTION:Bring wine\nand bread`, "END:VEVENT"})
	same(t, "and read again", ParseEvent(written), dinner)
	same(t, "its entry is named by its month", EntryName(dinner), "2026-11/dinner-1@rune")
	same(t, "19:00 in Berlin in winter is 18:00 UTC", Instant(dinner.Start, dinner.Zone).UTC().Format(time.RFC3339), "2026-11-20T18:00:00Z")
	same(t, "and in summer 17:00 UTC", Instant("2026-07-20T19:00", "Europe/Berlin").UTC().Format(time.RFC3339), "2026-07-20T17:00:00Z")
	same(t, "an instant as the wall of a zone", WallIn(time.Date(2026, 11, 20, 18, 0, 0, 0, time.UTC), "America/New_York"), "2026-11-20T13:00")
	once := Occurrences(dinner, time.Date(2026, 11, 1, 0, 0, 0, 0, time.UTC), time.Date(2026, 12, 1, 0, 0, 0, 0, time.UTC))
	if len(once) != 1 || once[0].Start.UTC().Format(time.RFC3339) != "2026-11-20T18:00:00Z" || once[0].End.UTC().Format(time.RFC3339) != "2026-11-20T20:30:00Z" {
		t.Errorf("it takes place once: %v", once)
	}
	same(t, "and not in another month", len(Occurrences(dinner, time.Date(2026, 12, 1, 0, 0, 0, 0, time.UTC), time.Date(2027, 1, 1, 0, 0, 0, 0, time.UTC))), 0)

	long := *dinner
	long.Title = "A title which is long enough that its line must be folded, with ümläüts and more text to be sure of it"
	folded := WriteEvent(&long, stamp)
	for _, line := range strings.Split(folded, "\r\n") {
		if len(line) > 75 {
			t.Errorf("a line of %d octets: %q", len(line), line)
		}
	}
	same(t, "a folded line is read whole", ParseEvent(folded).Title, long.Title)

	weekly := &Event{UID: "w@rune", Title: "Choir", Start: "2026-03-23T18:00", End: "2026-03-23T19:00", Zone: "Europe/Berlin", Repeat: &Repeat{Every: "weekly", Interval: 1, Until: "2026-04-06"}}
	var instants []string
	for _, o := range Occurrences(weekly, time.Date(2026, 3, 1, 0, 0, 0, 0, time.UTC), time.Date(2026, 5, 1, 0, 0, 0, 0, time.UTC)) {
		instants = append(instants, o.Start.UTC().Format(time.RFC3339))
	}
	same(t, "a weekly event keeps its time on the wall over the change of clocks", instants, []string{"2026-03-23T17:00:00Z", "2026-03-30T16:00:00Z", "2026-04-06T16:00:00Z"})
	if !strings.Contains(WriteEvent(weekly, stamp), "\r\nRRULE:FREQ=WEEKLY;UNTIL=20260406T235959Z\r\n") {
		t.Errorf("its rule: %s", WriteEvent(weekly, stamp))
	}
	same(t, "read again", ParseEvent(WriteEvent(weekly, stamp)), weekly)
	same(t, "a repeating event is named apart", EntryName(weekly), "repeat/w@rune")

	monthly := &Event{UID: "m@rune", Title: "Rent", AllDay: true, Start: "2026-01-31", End: "2026-02-01", Repeat: &Repeat{Every: "monthly", Interval: 1, Count: 4}}
	same(t, "monthly on the 31st passes over the shorter months, four times in all", starts(monthly, "2026-01-01", "2027-01-01"), []string{"2026-1-31", "2026-3-31", "2026-5-31", "2026-7-31"})
	yearly := &Event{UID: "y@rune", Title: "Birthday", AllDay: true, Start: "2024-02-29", End: "2024-03-01", Repeat: &Repeat{Every: "yearly", Interval: 1}}
	same(t, "yearly on the 29th of February only in leap years", starts(yearly, "2024-01-01", "2033-01-01"), []string{"2024-2-29", "2028-2-29", "2032-2-29"})
	daily := &Event{UID: "d@rune", Title: "Pills", AllDay: true, Start: "2020-01-01", End: "2020-01-02", Repeat: &Repeat{Every: "daily", Interval: 3}}
	same(t, "every third day, far from its start", starts(daily, "2026-11-01", "2026-11-11"), []string{"2026-11-1", "2026-11-4", "2026-11-7", "2026-11-10"})
	trip := &Event{UID: "t@rune", Title: "Trip", AllDay: true, Start: "2026-11-28", End: "2026-12-03"}
	same(t, "whole days over the end of a month show in both", []int{len(starts(trip, "2026-11-01", "2026-12-01")), len(starts(trip, "2026-12-01", "2027-01-01"))}, []int{1, 1})
	same(t, "the day after", []string{AddDays("2026-12-31", 1), AddDays("2026-03-01", -1), AddDays("2026-03-28T18:00", 7)}, []string{"2027-01-01", "2026-02-28", "2026-04-04T18:00"})
	same(t, "days which aren't", []string{Day(2026, 2, 30), Day(2028, 2, 29)}, []string{"", "2028-02-29"})

	foreign := strings.Join([]string{"BEGIN:VCALENDAR", "VERSION:2.0", "BEGIN:VEVENT", "UID:abc", "DTSTART:20261224T170000Z", "DURATION:PT1H30M", "SUMMARY:Carols",
		"RRULE:FREQ=MONTHLY;BYDAY=2TU", "X-CUSTOM;X=1:kept", "BEGIN:VALARM", "ACTION:DISPLAY", "TRIGGER:-PT15M", "END:VALARM", "END:VEVENT",
		"BEGIN:VEVENT", "UID:def", "DTSTART;VALUE=DATE:20261225", "SUMMARY:Holiday", "END:VEVENT", "END:VCALENDAR"}, "\r\n")
	read := ParseCalendar(foreign)
	if len(read) != 2 {
		t.Fatalf("a file of another program: %d events", len(read))
	}
	same(t, "the first", []any{read[0].UID, read[0].Start, read[0].End, read[0].Zone, read[0].AllDay}, []any{"abc", "2026-12-24T17:00", "2026-12-24T18:30", "UTC", false})
	same(t, "the second", []any{read[1].UID, read[1].Start, read[1].End, read[1].Zone, read[1].AllDay}, []any{"def", "2026-12-25", "2026-12-26", "", true})
	same(t, "a rule Tolk does not know is kept, and the event shown once", []any{read[0].Rule, read[0].Repeat, EntryName(read[0])}, []any{"FREQ=MONTHLY;BYDAY=2TU", nil, "repeat/abc"})
	same(t, "what Tolk does not know of an event survives", read[0].Other, []string{"X-CUSTOM;X=1:kept", "BEGIN:VALARM", "ACTION:DISPLAY", "TRIGGER:-PT15M", "END:VALARM"})
	same(t, "and goes out again", ParseCalendar(WriteCalendar(read, stamp)), read)
	if ParseEvent("hello") != nil || ParseEvent("BEGIN:VEVENT\r\nSUMMARY:no start\r\nEND:VEVENT") != nil {
		t.Error("something else was read as an event")
	}
	same(t, "the months", []string{MonthPrefix(2026, 3), Repeating}, []string{"2026-03/", "repeat/"})
}

// A call set for a time is an event with a line of its own (FEATURES.md,
// "Presence and calls"); the cases of Rune's test-features.ts.
func TestCalendarCalls(t *testing.T) {
	stamp := time.Date(2026, 11, 15, 12, 0, 0, 0, time.UTC)
	standup := &Event{UID: "standup@rune", Title: "Stand-up", Start: "2026-11-20T19:00", End: "2026-11-20T21:30", Zone: "Europe/Berlin", Call: "video", Other: []string{"X-CUSTOM:kept"}}
	lines := strings.Split(WriteEvent(standup, stamp), "\r\n")
	same(t, "an event which is a call has its line, before what Tolk does not know", lines[len(lines)-3:], []string{"X-EDS-CALL:video", "X-CUSTOM:kept", "END:VEVENT"})
	same(t, "and is read again", ParseEvent(WriteEvent(standup, stamp)), standup)
	voice := *standup
	voice.Call = "audio"
	same(t, "a call with the voice", ParseEvent(WriteEvent(&voice, stamp)).Call, "audio")
	same(t, "as another program writes it", ParseEvent("BEGIN:VEVENT\r\nUID:a\r\nDTSTART:20261224T170000Z\r\nx-eds-call: Video \r\nEND:VEVENT").Call, "video")
	odd := *standup
	odd.Call, odd.Other = "", []string{"X-EDS-CALL:hologram"}
	read := ParseEvent(WriteEvent(&odd, stamp))
	same(t, "a kind of call Tolk does not know makes no call, and the line is kept", []any{read.Call, read.Other}, []any{"", []string{"X-EDS-CALL:hologram"}})
	plain := *standup
	plain.Call = ""
	same(t, "an event which is no call has no such line", strings.Contains(WriteEvent(&plain, stamp), "X-EDS-CALL"), false)
	same(t, "through an .ics file", ParseCalendar(WriteCalendar([]*Event{standup, &plain}, stamp)), []*Event{standup, &plain})
}

// plainSealer is the sealer of a room: the content goes as it is.
type plainSealer struct{}

func (plainSealer) Protect(_ context.Context, _, _ string, data []byte) ([]byte, error) {
	return data, nil
}
func (plainSealer) Reveal(_ context.Context, _, _ string, data []byte) ([]byte, error) {
	return data, nil
}

// The messages of a call; the cases of Rune's test-features.ts.
func TestCallMessages(t *testing.T) {
	const alice, bert = "11111111-1111-4111-8111-111111111111", "22222222-2222-4222-8222-222222222222"
	offer := CallMessage{V: 1, T: "offer", Call: RoomCall, Peer: alice, Target: bert, SDP: "v=0\r\n"}
	same(t, "a message there and back", ReadCallMessage(WriteCallMessage(offer)), offer)
	same(t, "what a version does not know is passed over", ReadCallMessage(`{"v":1,"t":"join","call":"room","peer":"`+alice+`","media":"video","later":true}`),
		CallMessage{V: 1, T: "join", Call: RoomCall, Peer: alice, Media: "video"})
	var read []bool
	for _, text := range []string{"hello", "null", `{"v":2,"t":"join","call":"room","peer":"` + alice + `"}`, `{"v":1,"t":"dance","call":"room"}`, `{"v":1,"t":"probe"}`,
		`{"v":1,"t":"probe","call":"all rooms"}`, WriteCallMessage(CallMessage{T: "offer", Call: RoomCall, Peer: alice, Target: bert}),
		WriteCallMessage(CallMessage{T: "offer", Call: RoomCall, Peer: alice, SDP: "v=0"}), WriteCallMessage(CallMessage{T: "join", Call: RoomCall}),
		WriteCallMessage(CallMessage{T: "ice", Call: alice, Peer: alice, Target: bert})} {
		read = append(read, ReadCallMessage(text) != nil)
	}
	same(t, "not a message: no JSON, another version, an unknown type, no call, an offer without its description or for nobody, a join of nobody, a candidate without one",
		read, make([]bool, 10))
	same(t, "a probe and a ring need little", []string{ReadCallMessage(WriteCallMessage(CallMessage{T: "probe", Call: RoomCall})).T,
		ReadCallMessage(WriteCallMessage(CallMessage{T: "ring", Call: alice, Media: "audio"})).Media}, []string{"probe", "audio"})
	ice := ReadCallMessage(`{"v":1,"t":"ice","call":"` + alice + `","peer":"` + alice + `","target":"` + bert + `","candidate":{"candidate":"candidate:1 1 udp 1 192.0.2.1 9 typ host","sdpMid":"0"}}`)
	same(t, "a candidate as WebRTC gives it", ice.Candidate, map[string]any{"candidate": "candidate:1 1 udp 1 192.0.2.1 9 typ host", "sdpMid": "0"})

	// As a signal carries it: Base64, as the content of an entry.
	ctx := context.Background()
	signal, err := CallSignal(ctx, plainSealer{}, "c-1", CallMessage{T: "ring", Call: RoomCall, Media: "video"})
	raw, _ := base64.StdEncoding.DecodeString(signal)
	same(t, "the signal of a ring", []any{err, string(raw)}, []any{nil, `{"v":1,"t":"ring","call":"room","media":"video"}`})
	same(t, "and read again", ReadCallSignal(ctx, plainSealer{}, "c-1", signal), CallMessage{V: 1, T: "ring", Call: RoomCall, Media: "video"})
	same(t, "what is no signal of a call", []bool{ReadCallSignal(ctx, plainSealer{}, "c-1", "") == nil, ReadCallSignal(ctx, plainSealer{}, "c-1", "typing") == nil,
		ReadCallSignal(ctx, plainSealer{}, "c-1", "!") == nil}, []bool{true, true, true})
}

func TestLocation(t *testing.T) {
	same(t, "Berlin to Hamburg is about 255 km", math.Round(Distance(52.52, 13.405, 53.5511, 9.9937)/1000), 255)
	same(t, "no distance to itself", Distance(52.52, 13.405, 52.52, 13.405), 0)
	position := NewPosition(52.5200066, 13.4049541, 12.6, time.Date(2026, 11, 15, 10, 0, 0, 0, time.UTC), "")
	encoded, _ := json.Marshal(position)
	same(t, "a position as an entry has it", string(encoded), `{"v":1,"lat":52.520007,"lon":13.404954,"acc":13,"at":"2026-11-15T10:00:00.000Z"}`)
	same(t, "what is a position", []bool{position.Valid(), Position{Lat: 91, Lon: 2, At: "2026-11-15T10:00:00Z"}.Valid(), Position{Lat: 1, Lon: 2}.Valid()}, []bool{true, false, false})
	same(t, "the address for a map app", GeoLink(Position{Lat: 52.52, Lon: 13.405, Acc: 13}), "geo:52.52,13.405;u=13")
	same(t, "a place as numbers", Coordinates(Position{Lat: -33.8688, Lon: 151.2093}), "33.86880° S, 151.20930° E")
}

func TestDeclarations(t *testing.T) {
	names := append([]string{}, Names...)
	sort.Strings(names)
	same(t, "the features Tolk knows", names, []string{"calendar", "calls", "chat", "location", "plans"})
	for _, name := range Names {
		if Version(name) < 1 || Declaration(name) == nil || Label(name) == name {
			t.Errorf("%s: version %d", name, Version(name))
		}
	}
	if Version("unknown") != 0 || Declaration("unknown") != nil || Label("unknown") != "unknown" {
		t.Error("a feature Tolk doesn't know")
	}
}
