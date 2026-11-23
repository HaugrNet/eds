package features

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"
	_ "time/tzdata" // the time zones, also where the system has none
)

// The feature "calendar" (FEATURES.md, "Calendar"): the events of a room. An
// entry holds one event of iCalendar (RFC 5545), so events can be brought
// in and taken out as `.ics`. Here: the event as Tolk works with it, its
// VEVENT, the name of its entry, and when it takes place, also when it
// repeats. It says the same as Rune's src/features/calendar.ts.
//
// First version: single events, and events repeating daily, weekly, monthly
// or yearly, each n-th time, until a day or a number of times. A rule Tolk
// doesn't know is kept as it is and the event shown once.

// EventBody is the content of an entry of the kind "event".
type EventBody struct {
	V   int    `json:"v"`
	ICS string `json:"ics"`
}

// Repeat says how an event repeats.
type Repeat struct {
	Every    string `json:"every"` // daily, weekly, monthly, yearly
	Interval int    `json:"interval"`
	Until    string `json:"until,omitempty"` // the last day it may start on, YYYY-MM-DD
	Count    int    `json:"count,omitempty"`
}

// Event is an event of a room's calendar.
type Event struct {
	UID    string `json:"uid"`
	Title  string `json:"title"`
	AllDay bool   `json:"allDay"`
	// Start and End are times as the clock on the wall says there:
	// "YYYY-MM-DD" for a whole day, else "YYYY-MM-DDTHH:MM". Of whole days
	// End is the day after the last, as iCalendar has it.
	Start string `json:"start"`
	End   string `json:"end"`
	// Zone is the time zone of start and end (IANA); none for whole days.
	Zone   string  `json:"zone,omitempty"`
	Place  string  `json:"place,omitempty"`
	Note   string  `json:"note,omitempty"`
	Repeat *Repeat `json:"repeat,omitempty"`
	// Rule is a rule of repeating which Tolk doesn't know, kept as it came.
	Rule string `json:"rule,omitempty"`
	// Call makes the event a call of the room, "audio" for the voice or
	// "video" for video too (FEATURES.md, "Presence and calls").
	Call string `json:"call,omitempty"`
	// Other are the lines of the event Tolk doesn't know, kept so they
	// survive a change.
	Other []string `json:"other,omitempty"`
}

var frequencies = map[string]string{"DAILY": "daily", "WEEKLY": "weekly", "MONTHLY": "monthly", "YEARLY": "yearly"}

// DeviceZone is the time zone of this device by its name in the common
// database, such as "Europe/Berlin"; "UTC" if it can't be told.
func DeviceZone() string {
	if name := os.Getenv("TZ"); name != "" {
		if _, err := time.LoadLocation(name); err == nil && name != "Local" {
			return name
		}
	}
	if name := time.Local.String(); name != "Local" && name != "" {
		if _, err := time.LoadLocation(name); err == nil {
			return name
		}
	}
	// Where the system keeps its zone as a link into the database.
	if target, err := filepath.EvalSymlinks("/etc/localtime"); err == nil {
		if i := strings.Index(target, "zoneinfo/"); i >= 0 {
			name := target[i+len("zoneinfo/"):]
			if _, err := time.LoadLocation(name); err == nil {
				return name
			}
		}
	}
	return "UTC"
}

func location(zone string) *time.Location {
	if zone == "" {
		return time.Local
	}
	if loc, err := time.LoadLocation(zone); err == nil {
		return loc
	}
	return time.Local
}

// --- Days and wall times -----------------------------------------------------------

var wallPattern = regexp.MustCompile(`^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?`)

func numbers(wall string) (y, m, d, h, min int, ok bool) {
	match := wallPattern.FindStringSubmatch(wall)
	if match == nil {
		return 0, 0, 0, 0, 0, false
	}
	y, _ = strconv.Atoi(match[1])
	m, _ = strconv.Atoi(match[2])
	d, _ = strconv.Atoi(match[3])
	h, _ = strconv.Atoi(match[4])
	min, _ = strconv.Atoi(match[5])
	return y, m, d, h, min, true
}

// Day is "YYYY-MM-DD" of a year, a month and a day; empty if there is no
// such day.
func Day(year, month, date int) string {
	made := time.Date(year, time.Month(month), date, 0, 0, 0, 0, time.UTC)
	if made.Year() != year || int(made.Month()) != month || made.Day() != date {
		return ""
	}
	return fmt.Sprintf("%04d-%02d-%02d", year, month, date)
}

// AddDays is the day a number of days later (or earlier); a time of day
// stays.
func AddDays(wall string, days int) string {
	y, m, d, _, _, ok := numbers(wall)
	if !ok {
		return wall
	}
	moved := time.Date(y, time.Month(m), d+days, 0, 0, 0, 0, time.UTC)
	rest := ""
	if len(wall) > 10 {
		rest = wall[10:]
	}
	return moved.Format("2006-01-02") + rest
}

func daysBetween(from, to string) int {
	y1, m1, d1, _, _, _ := numbers(from)
	y2, m2, d2, _, _, _ := numbers(to)
	a := time.Date(y1, time.Month(m1), d1, 0, 0, 0, 0, time.UTC)
	b := time.Date(y2, time.Month(m2), d2, 0, 0, 0, 0, time.UTC)
	return int(b.Sub(a).Hours()/24 + 0.5*sign(b.Sub(a).Hours()))
}

func sign(value float64) float64 {
	if value < 0 {
		return -1
	}
	return 1
}

// Instant is the instant of a time on the wall in a zone. A whole day
// starts at midnight of this device, as a day is the same day for
// everyone.
func Instant(wall, zone string) time.Time {
	y, m, d, h, min, _ := numbers(wall)
	if zone == "" || len(wall) == 10 {
		return time.Date(y, time.Month(m), d, h, min, 0, 0, time.Local)
	}
	return time.Date(y, time.Month(m), d, h, min, 0, 0, location(zone))
}

// WallIn is the time on the wall of a zone at an instant:
// "YYYY-MM-DDTHH:MM".
func WallIn(at time.Time, zone string) string {
	return at.In(location(zone)).Format("2006-01-02T15:04")
}

// --- iCalendar ---------------------------------------------------------------------

var escaper = strings.NewReplacer(`\`, `\\`, ";", `\;`, ",", `\,`, "\r\n", `\n`, "\n", `\n`)

func escapeText(value string) string { return escaper.Replace(value) }

func unescapeText(value string) string {
	var out strings.Builder
	for i := 0; i < len(value); i++ {
		if value[i] == '\\' && i+1 < len(value) {
			switch value[i+1] {
			case 'n', 'N':
				out.WriteByte('\n')
				i++
				continue
			case '\\', ';', ',':
				out.WriteByte(value[i+1])
				i++
				continue
			}
		}
		out.WriteByte(value[i])
	}
	return out.String()
}

// fold folds a line as iCalendar wants it: at most 75 octets, continued
// with a space.
func fold(line string) string {
	var lines []string
	current, length := "", 0
	for _, character := range line {
		size := len(string(character))
		limit := 74
		if len(lines) == 0 {
			limit = 75
		}
		if length+size > limit {
			lines = append(lines, current)
			current, length = "", 0
		}
		current += string(character)
		length += size
	}
	lines = append(lines, current)
	return strings.Join(lines, "\r\n ")
}

var folding = regexp.MustCompile(`\r?\n[ \t]`)

// unfold gives the lines of an iCalendar text, unfolded.
func unfold(text string) []string {
	var lines []string
	for _, line := range strings.Split(folding.ReplaceAllString(text, ""), "\n") {
		if line = strings.TrimRight(line, "\r"); line != "" {
			lines = append(lines, line)
		}
	}
	return lines
}

type property struct {
	name       string
	parameters map[string]string
	value      string
}

func parseProperty(line string) *property {
	// The name and its parameters end at the first colon outside quotes.
	quoted, colon := false, -1
	for i := 0; i < len(line); i++ {
		if line[i] == '"' {
			quoted = !quoted
		} else if line[i] == ':' && !quoted {
			colon = i
			break
		}
	}
	if colon < 1 {
		return nil
	}
	parts := strings.Split(line[:colon], ";")
	found := &property{name: strings.ToUpper(parts[0]), parameters: map[string]string{}, value: line[colon+1:]}
	for _, parameter := range parts[1:] {
		if name, value, ok := strings.Cut(parameter, "="); ok && name != "" {
			found.parameters[strings.ToUpper(name)] = strings.Trim(value, `"`)
		}
	}
	return found
}

func compact(wall string) string {
	if len(wall) == 10 {
		return strings.ReplaceAll(wall, "-", "")
	}
	return strings.NewReplacer("-", "", ":", "").Replace(wall) + "00"
}

var icsTime = regexp.MustCompile(`^(\d{4})(\d{2})(\d{2})(?:T(\d{2})(\d{2})(\d{2})?(Z)?)?$`)

// readTime is a time of iCalendar as a time on the wall, with its zone.
func readTime(found *property) (wall, zone string, allDay, ok bool) {
	match := icsTime.FindStringSubmatch(strings.TrimSpace(found.value))
	if match == nil {
		return "", "", false, false
	}
	date := match[1] + "-" + match[2] + "-" + match[3]
	if match[4] == "" {
		return date, "", true, true
	}
	wall = date + "T" + match[4] + ":" + match[5]
	if match[7] != "" {
		return wall, "UTC", false, true
	}
	// A zone which isn't one of the common database (some programs name
	// their own) is read as this device's.
	zone = found.parameters["TZID"]
	if _, err := time.LoadLocation(zone); zone == "" || zone == "Local" || err != nil {
		zone = DeviceZone()
	}
	return wall, zone, false, true
}

var icsDuration = regexp.MustCompile(`^P(?:(\d+)W)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$`)

// readDuration is a duration of iCalendar ("P1D", "PT1H30M", "P1W") in
// minutes; -1 if it isn't one.
func readDuration(value string) int {
	match := icsDuration.FindStringSubmatch(strings.TrimSpace(value))
	if match == nil {
		return -1
	}
	n := func(i int) int { v, _ := strconv.Atoi(match[i]); return v }
	return n(1)*10080 + n(2)*1440 + n(3)*60 + n(4)
}

func readRule(value string) *Repeat {
	parts := map[string]string{}
	for _, part := range strings.Split(value, ";") {
		name, v, _ := strings.Cut(part, "=")
		parts[strings.ToUpper(name)] = v
	}
	every := frequencies[strings.ToUpper(parts["FREQ"])]
	if every == "" {
		return nil
	}
	for name := range parts {
		switch name {
		case "FREQ", "INTERVAL", "UNTIL", "COUNT", "WKST":
		default:
			return nil
		}
	}
	repeat := &Repeat{Every: every, Interval: 1}
	if n, err := strconv.Atoi(parts["INTERVAL"]); err == nil && n > 1 {
		repeat.Interval = n
	}
	if until := parts["UNTIL"]; len(until) >= 8 && icsTime.MatchString(until) {
		repeat.Until = until[:4] + "-" + until[4:6] + "-" + until[6:8]
	}
	if n, err := strconv.Atoi(parts["COUNT"]); err == nil && n > 0 {
		repeat.Count = n
	}
	return repeat
}

func writeRule(repeat *Repeat) string {
	rule := "FREQ=" + strings.ToUpper(repeat.Every)
	if repeat.Interval > 1 {
		rule += ";INTERVAL=" + strconv.Itoa(repeat.Interval)
	}
	if repeat.Count > 0 {
		rule += ";COUNT=" + strconv.Itoa(repeat.Count)
	}
	if repeat.Until != "" {
		rule += ";UNTIL=" + strings.ReplaceAll(repeat.Until, "-", "")
		if repeat.Count == 0 {
			rule += "T235959Z"
		}
	}
	return rule
}

var known = map[string]bool{"UID": true, "DTSTAMP": true, "DTSTART": true, "DTEND": true, "DURATION": true, "SUMMARY": true, "LOCATION": true, "DESCRIPTION": true, "RRULE": true}

// callLine is the line which makes an event a call: `X-EDS-CALL:audio` or
// `X-EDS-CALL:video`.
const callLine = "X-EDS-CALL"

// callOf is the kind of call a line names; empty if it isn't that line, or
// names a kind Tolk doesn't know. Such a line is kept among the others.
func callOf(read *property) string {
	if read == nil || read.name != callLine {
		return ""
	}
	if call := strings.ToLower(strings.TrimSpace(read.value)); call == "audio" || call == "video" {
		return call
	}
	return ""
}

// NewUID makes the identifier of a new event.
func NewUID() string {
	random := make([]byte, 16)
	_, _ = rand.Read(random)
	text := hex.EncodeToString(random)
	return text[:8] + "-" + text[8:12] + "-" + text[12:16] + "-" + text[16:20] + "-" + text[20:] + "@tolk"
}

// ParseEvent reads one event; nil if the text isn't one, or has no start.
func ParseEvent(ics string) *Event {
	lines := unfold(ics)
	begin, end := -1, -1
	for i, line := range lines {
		upper := strings.ToUpper(line)
		if begin < 0 && upper == "BEGIN:VEVENT" {
			begin = i
		} else if begin >= 0 && upper == "END:VEVENT" {
			end = i
			break
		}
	}
	if begin < 0 || end < 0 {
		return nil
	}
	found := map[string]*property{}
	var other []string
	call := ""
	nested := 0
	for _, line := range lines[begin+1 : end] {
		upper := strings.ToUpper(line)
		if strings.HasPrefix(upper, "BEGIN:") {
			nested++
		}
		var read *property
		if nested == 0 {
			read = parseProperty(line)
		}
		if read != nil && known[read.name] {
			found[read.name] = read
		} else if kind := callOf(read); kind != "" {
			call = kind
		} else {
			other = append(other, line)
		}
		if strings.HasPrefix(upper, "END:") {
			nested--
		}
	}
	if found["DTSTART"] == nil {
		return nil
	}
	start, zone, allDay, ok := readTime(found["DTSTART"])
	if !ok {
		return nil
	}
	endWall := ""
	if found["DTEND"] != nil {
		if wall, endZone, endAllDay, ok := readTime(found["DTEND"]); ok && endAllDay == allDay {
			// Start and end are kept in the zone of the start.
			if allDay || endZone == zone {
				endWall = wall
			} else {
				endWall = WallIn(Instant(wall, endZone), zone)
			}
		}
	}
	if endWall == "" && found["DURATION"] != nil {
		if minutes := readDuration(found["DURATION"].value); minutes >= 0 {
			if allDay {
				endWall = AddDays(start, max(1, (minutes+720)/1440))
			} else {
				endWall = WallIn(Instant(start, zone).Add(time.Duration(minutes)*time.Minute), zone)
			}
		}
	}
	if endWall == "" || endWall < start || (allDay && endWall == start) {
		if allDay {
			endWall = AddDays(start, 1)
		} else {
			endWall = start
		}
	}
	event := &Event{Title: unescapeText(text(found["SUMMARY"])), AllDay: allDay, Start: start, End: endWall, Other: other}
	if event.UID = strings.TrimSpace(text(found["UID"])); event.UID == "" {
		event.UID = NewUID()
	}
	if !allDay {
		event.Zone = zone
	}
	event.Place = unescapeText(text(found["LOCATION"]))
	event.Note = unescapeText(text(found["DESCRIPTION"]))
	if rule := strings.TrimSpace(text(found["RRULE"])); rule != "" {
		if event.Repeat = readRule(rule); event.Repeat == nil {
			event.Rule = rule
		}
	}
	event.Call = call
	return event
}

func text(found *property) string {
	if found == nil {
		return ""
	}
	return found.value
}

func writeTime(name, wall string, event *Event) string {
	if event.AllDay {
		return name + ";VALUE=DATE:" + compact(wall)
	}
	if event.Zone == "UTC" {
		return name + ":" + compact(wall) + "Z"
	}
	zone := event.Zone
	if zone == "" {
		zone = DeviceZone()
	}
	return name + ";TZID=" + zone + ":" + compact(wall)
}

// WriteEvent is the event as a VEVENT of iCalendar; stamp is when it was
// written.
func WriteEvent(event *Event, stamp time.Time) string {
	lines := []string{
		"BEGIN:VEVENT",
		"UID:" + event.UID,
		"DTSTAMP:" + stamp.UTC().Format("20060102T150405Z"),
		writeTime("DTSTART", event.Start, event),
		writeTime("DTEND", event.End, event),
		"SUMMARY:" + escapeText(event.Title),
	}
	if event.Place != "" {
		lines = append(lines, "LOCATION:"+escapeText(event.Place))
	}
	if event.Note != "" {
		lines = append(lines, "DESCRIPTION:"+escapeText(event.Note))
	}
	if event.Repeat != nil {
		lines = append(lines, "RRULE:"+writeRule(event.Repeat))
	} else if event.Rule != "" {
		lines = append(lines, "RRULE:"+event.Rule)
	}
	if event.Call != "" {
		lines = append(lines, callLine+":"+event.Call)
	}
	lines = append(lines, event.Other...)
	lines = append(lines, "END:VEVENT")
	for i := range lines {
		lines[i] = fold(lines[i])
	}
	return strings.Join(lines, "\r\n")
}

// ParseCalendar reads all events of an `.ics` file.
func ParseCalendar(text string) []*Event {
	lines := unfold(text)
	var events []*Event
	from := -1
	for i, line := range lines {
		upper := strings.ToUpper(line)
		if upper == "BEGIN:VEVENT" {
			from = i
		} else if upper == "END:VEVENT" && from >= 0 {
			if event := ParseEvent(strings.Join(lines[from:i+1], "\r\n")); event != nil {
				events = append(events, event)
			}
			from = -1
		}
	}
	return events
}

// WriteCalendar is events as an `.ics` file.
func WriteCalendar(events []*Event, stamp time.Time) string {
	lines := []string{"BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Haugr//Tolk//EN", "CALSCALE:GREGORIAN"}
	for _, event := range events {
		lines = append(lines, WriteEvent(event, stamp))
	}
	return strings.Join(append(lines, "END:VCALENDAR", ""), "\r\n")
}

// --- Entries -----------------------------------------------------------------------

// MonthPrefix is the prefix of the names of the entries of a month,
// "YYYY-MM/".
func MonthPrefix(year, month int) string { return fmt.Sprintf("%04d-%02d/", year, month) }

// Repeating is the prefix of the names of the entries of repeating events.
const Repeating = "repeat/"

var notInName = regexp.MustCompile(`[^A-Za-z0-9.@_-]`)

// EntryName is the name of the entry of an event: by the month it starts
// in, so a client fetches the months it shows; repeating events are named
// apart, and always fetched.
func EntryName(event *Event) string {
	id := notInName.ReplaceAllString(event.UID, "")
	if len(id) > 40 {
		id = id[:40]
	}
	if id == "" {
		id = "event"
	}
	if event.Repeat != nil || event.Rule != "" {
		return Repeating + id
	}
	return event.Start[:7] + "/" + id
}

// --- When it takes place -------------------------------------------------------------

// Occurrence is one time an event takes place.
type Occurrence struct {
	Start, End time.Time
}

// moved is the start of the k-th time an event takes place, as a time on
// the wall; empty if that time doesn't exist (a month without the day).
func moved(event *Event, k int) string {
	y, m, d, _, _, _ := numbers(event.Start)
	interval := 1
	if event.Repeat != nil && event.Repeat.Interval > 1 {
		interval = event.Repeat.Interval
	}
	step := k * interval
	clock := ""
	if len(event.Start) > 10 {
		clock = event.Start[10:]
	}
	every := ""
	if event.Repeat != nil {
		every = event.Repeat.Every
	}
	switch every {
	case "daily":
		return AddDays(event.Start, step)
	case "weekly":
		return AddDays(event.Start, 7*step)
	case "monthly":
		months := (m - 1) + step
		if made := Day(y+months/12, months%12+1, d); made != "" {
			return made + clock
		}
		return "" // A month without that day is passed over.
	case "yearly":
		if made := Day(y+step, m, d); made != "" {
			return made + clock
		}
		return ""
	}
	if k == 0 {
		return event.Start
	}
	return ""
}

// Occurrences is when an event takes place between two instants: once, or
// each time it repeats. What overlaps the span counts.
func Occurrences(event *Event, from, to time.Time) []Occurrence {
	var length time.Duration
	days := 0
	if event.AllDay {
		days = daysBetween(event.Start, event.End)
	} else {
		length = Instant(event.End, event.Zone).Sub(Instant(event.Start, event.Zone))
	}
	at := func(startWall string) Occurrence {
		start := Instant(startWall, event.Zone)
		if event.AllDay {
			return Occurrence{Start: start, End: Instant(AddDays(startWall, days), "")}
		}
		return Occurrence{Start: start, End: start.Add(length)}
	}
	overlaps := func(o Occurrence) bool {
		return o.End.After(from) || (o.End.Equal(o.Start) && !o.Start.Before(from))
	}
	if event.Repeat == nil {
		if once := at(event.Start); overlaps(once) && once.Start.Before(to) {
			return []Occurrence{once}
		}
		return nil
	}
	var found []Occurrence
	k := 0
	// Days and weeks pass evenly, so what lies long before the span is skipped.
	if event.Repeat.Count == 0 && (event.Repeat.Every == "daily" || event.Repeat.Every == "weekly") {
		step := max(1, event.Repeat.Interval)
		if event.Repeat.Every == "weekly" {
			step *= 7
		}
		long := days
		if !event.AllDay {
			long = int(length.Hours()/24) + 1
		}
		before := daysBetween(event.Start, from.In(time.Local).Format("2006-01-02")) - long - 2
		k = max(0, before/step)
	}
	taken := 0
	for tries := 0; tries < 6000; tries, k = tries+1, k+1 {
		if event.Repeat.Count > 0 && k >= event.Repeat.Count*50 {
			break
		}
		startWall := moved(event, k)
		if startWall == "" {
			continue
		}
		if event.Repeat.Until != "" && startWall[:10] > event.Repeat.Until {
			break
		}
		taken++
		if event.Repeat.Count > 0 && taken > event.Repeat.Count {
			break
		}
		occurrence := at(startWall)
		if !occurrence.Start.Before(to) {
			break
		}
		if overlaps(occurrence) {
			found = append(found, occurrence)
		}
	}
	sort.Slice(found, func(i, j int) bool { return found[i].Start.Before(found[j].Start) })
	return found
}
