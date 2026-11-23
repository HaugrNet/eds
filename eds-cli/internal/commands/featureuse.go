package commands

import (
	"context"
	"encoding/json"
	"io"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// The calendar, the location and the plans of a room (FEATURES.md), for
// scripts as for people: what Rune shows in its panes, as commands.

// --- Calendar ----------------------------------------------------------------------

// wall reads a time as people write it: "2026-12-04", "2026-12-04 19:00"
// or "2026-12-04T19:00".
func wall(text string) (string, bool) {
	text = strings.Replace(strings.TrimSpace(text), " ", "T", 1)
	for _, layout := range []string{"2006-01-02T15:04", "2006-01-02"} {
		if at, err := time.Parse(layout, text); err == nil {
			return at.Format(layout), true
		}
	}
	return "", false
}

type knownEvent struct {
	entry client.Entry
	event *features.Event
}

func (c *command) events(ctx context.Context, t target, prefixes ...string) ([]knownEvent, error) {
	var found []knownEvent
	if len(prefixes) == 0 {
		prefixes = []string{""}
	}
	for _, prefix := range prefixes {
		entries, err := c.listEntries(ctx, t, "calendar", client.EntryQuery{Kind: "event", Name: prefix})
		if err != nil {
			return nil, err
		}
		for i := range entries {
			var body features.EventBody
			if !features.Decode(ctx, t.stone, "calendar", &entries[i], &body) {
				continue
			}
			if event := features.ParseEvent(body.ICS); event != nil {
				found = append(found, knownEvent{entries[i], event})
			}
		}
	}
	return found, nil
}

func (c *command) storeEvent(ctx context.Context, t target, event *features.Event, entryID string) (*client.Entry, error) {
	body := features.EventBody{V: 1, ICS: features.WriteEvent(event, time.Now())}
	if entryID != "" {
		return c.changeEntry(ctx, t, "calendar", "event", entryID, body, features.EntryName(event))
	}
	return c.addEntry(ctx, t, "calendar", "event", body, "", features.EntryName(event))
}

func (c *command) eventJSON(known knownEvent) map[string]any {
	out := map[string]any{"id": known.entry.EntryID, "by": known.entry.AccountName, "event": known.event}
	return out
}

// calendar is 'tolk calendar [list|add|change|remove|import|export] …
// --room <room>'.
func (c *command) calendar(args []string) error {
	verb := "list"
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		switch strings.ToLower(args[0]) {
		case "list", "add", "change", "remove", "import", "export":
			verb, args = strings.ToLower(args[0]), args[1:]
		}
	}
	fs := c.flags("calendar " + verb)
	rooms := roomFlag(fs)
	from := fs.String("from", "", "")
	to := fs.String("to", "", "")
	start := fs.String("start", "", "")
	end := fs.String("end", "", "")
	allDay := fs.Bool("all-day", false, "")
	place := fs.String("place", "", "")
	note := fs.String("note", "", "")
	title := fs.String("title", "", "")
	repeat := fs.String("repeat", "", "")
	every := fs.Int("every", 1, "")
	until := fs.String("until", "", "")
	zone := fs.String("zone", "", "")
	call := fs.String("call", "", "")
	output := fs.String("o", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	misuse := usage("Use 'tolk calendar [list] --room <room> [--from <day>] [--to <day>]', 'tolk calendar add <title> --room <room> --start <time> [--end <time>] [--all-day] [--place <place>] [--note <note>] [--repeat daily|weekly|monthly|yearly] [--every <n>] [--until <day>] [--call audio|video]', 'tolk calendar change <id> --room <room> [--title …] [--start …] …', 'tolk calendar remove <id> --room <room>', 'tolk calendar import <file.ics|-> --room <room>' or 'tolk calendar export --room <room> [-o <file>]'.")
	want := map[string]int{"list": 0, "add": 1, "change": 1, "remove": 1, "import": 1, "export": 0}[verb]
	if len(pos) != want {
		return misuse
	}
	if *repeat != "" && !contains([]string{"daily", "weekly", "monthly", "yearly", "none"}, *repeat) {
		return usage("%q is not a way to repeat. Use daily, weekly, monthly or yearly.", *repeat)
	}
	if *call != "" && !contains([]string{"audio", "video", "none"}, *call) {
		return usage("%q is not a kind of call. Use audio, video or none.", *call)
	}
	// apply puts what the flags say into an event.
	apply := func(event *features.Event) error {
		if *title != "" {
			event.Title = *title
		}
		if *start != "" {
			at, ok := wall(*start)
			if !ok {
				return usage("%q is not a time. Write it as 2026-12-04, or \"2026-12-04 19:00\".", *start)
			}
			// The length of the event stays, unless an end is given.
			length := time.Duration(0)
			if event.Start != "" && !event.AllDay && len(at) > 10 {
				length = features.Instant(event.End, event.Zone).Sub(features.Instant(event.Start, event.Zone))
			}
			days := 1
			if event.AllDay && event.Start != "" {
				days = max(1, int(features.Instant(event.End, "").Sub(features.Instant(event.Start, "")).Hours()/24+0.5))
			}
			event.AllDay = len(at) == 10 || *allDay
			if event.AllDay {
				event.Start, event.Zone = at[:10], ""
				event.End = features.AddDays(event.Start, days)
			} else {
				if event.Zone == "" {
					event.Zone = features.DeviceZone()
				}
				event.Start = at
				if length == 0 {
					length = time.Hour
				}
				event.End = features.WallIn(features.Instant(at, event.Zone).Add(length), event.Zone)
			}
		}
		if *zone != "" && !event.AllDay {
			if _, err := time.LoadLocation(*zone); err != nil {
				return usage("%q is not a time zone, like Europe/Berlin.", *zone)
			}
			event.Zone = *zone
		}
		if *end != "" {
			at, ok := wall(*end)
			if !ok {
				return usage("%q is not a time. Write it as 2026-12-04, or \"2026-12-04 19:00\".", *end)
			}
			if event.AllDay {
				// The last day, as people say it; iCalendar keeps the day after.
				event.End = features.AddDays(at[:10], 1)
			} else if len(at) == 10 {
				event.End = at + "T" + event.Start[11:]
			} else {
				event.End = at
			}
		}
		if event.Start == "" {
			return usage("An event needs a start: --start 2026-12-04, or --start \"2026-12-04 19:00\".")
		}
		if event.End < event.Start {
			return usage("The event ends before it starts.")
		}
		if *place != "" {
			event.Place = *place
		}
		if *note != "" {
			event.Note = *note
		}
		switch {
		case *repeat == "none":
			event.Repeat, event.Rule = nil, ""
		case *repeat != "":
			event.Repeat, event.Rule = &features.Repeat{Every: *repeat, Interval: max(1, *every)}, ""
		}
		// A call of the room, set for the time of the event.
		switch *call {
		case "none":
			event.Call = ""
		case "audio", "video":
			event.Call = *call
		}
		if *until != "" && event.Repeat != nil {
			day, ok := wall(*until)
			if !ok {
				return usage("%q is not a day. Write it as 2026-12-04.", *until)
			}
			event.Repeat.Until, event.Repeat.Count = day[:10], 0
		}
		return nil
	}
	return c.oneRoom(*rooms, misuse, func(ctx context.Context, t target) (any, error) {
		switch verb {
		case "add":
			event := &features.Event{UID: features.NewUID(), Title: pos[0]}
			if err := apply(event); err != nil {
				return nil, err
			}
			entry, err := c.storeEvent(ctx, t, event, "")
			if err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: added %q (%s).", t.label, event.Title, entry.EntryID))
			return c.eventJSON(knownEvent{*entry, event}), nil
		case "change", "remove":
			known, err := c.events(ctx, t)
			if err != nil {
				return nil, err
			}
			for _, k := range known {
				if k.entry.EntryID != pos[0] && k.event.UID != pos[0] {
					continue
				}
				if verb == "remove" {
					if err := c.removeEntry(ctx, t, "calendar", k.entry.EntryID); err != nil {
						return nil, err
					}
					c.note(i18n.T("%s: removed %q.", t.label, k.event.Title))
					return map[string]any{"removed": k.entry.EntryID}, nil
				}
				if err := apply(k.event); err != nil {
					return nil, err
				}
				entry, err := c.storeEvent(ctx, t, k.event, k.entry.EntryID)
				if err != nil {
					return nil, err
				}
				c.note(i18n.T("%s: changed %q.", t.label, k.event.Title))
				return c.eventJSON(knownEvent{*entry, k.event}), nil
			}
			return nil, exit.New(exit.NotFound, "%s", i18n.T("No event %q in the calendar of %s.", pos[0], t.label))
		case "import":
			var data []byte
			if pos[0] == "-" {
				data, err = io.ReadAll(io.LimitReader(c.env.In, 16<<20))
			} else {
				data, err = os.ReadFile(pos[0])
			}
			if err != nil {
				return nil, exit.New(exit.NotFound, "%s", i18n.T("Cannot read %s: %v", pos[0], rootError(err)))
			}
			brought := features.ParseCalendar(string(data))
			if len(brought) == 0 {
				return nil, exit.New(exit.Usage, "%s", i18n.T("No events were found in %s.", pos[0]))
			}
			// An event which is here already, by its identifier, is replaced.
			known, err := c.events(ctx, t)
			if err != nil {
				return nil, err
			}
			here := map[string]string{}
			for _, k := range known {
				here[k.event.UID] = k.entry.EntryID
			}
			for _, event := range brought {
				if _, err := c.storeEvent(ctx, t, event, here[event.UID]); err != nil {
					return nil, err
				}
			}
			c.note(i18n.T("%s: %d events were brought in.", t.label, len(brought)))
			return map[string]any{"imported": len(brought)}, nil
		case "export":
			known, err := c.events(ctx, t)
			if err != nil {
				return nil, err
			}
			events := []*features.Event{}
			for _, k := range known {
				events = append(events, k.event)
			}
			ics := features.WriteCalendar(events, time.Now())
			if *output == "" || *output == "-" {
				if c.json() {
					return map[string]any{"events": len(events), "ics": ics}, nil
				}
				c.say("%s", strings.TrimRight(ics, "\r\n"))
				return map[string]any{"events": len(events)}, nil
			}
			if err := os.WriteFile(*output, []byte(ics), 0o600); err != nil {
				return nil, exit.New(exit.Other, "%s", i18n.T("Cannot write %s: %v", *output, rootError(err)))
			}
			c.note(i18n.T("%s: %d events were written to %s.", t.label, len(events), *output))
			return map[string]any{"events": len(events), "file": *output}, nil
		}
		// list: what takes place in a span, 30 days from today unless told.
		first := time.Now().Format("2006-01-02")
		if *from != "" {
			day, ok := wall(*from)
			if !ok {
				return nil, usage("%q is not a day. Write it as 2026-12-04.", *from)
			}
			first = day[:10]
		}
		last := features.AddDays(first, 30)
		if *to != "" {
			day, ok := wall(*to)
			if !ok {
				return nil, usage("%q is not a day. Write it as 2026-12-04.", *to)
			}
			last = features.AddDays(day[:10], 1)
		}
		// The months of the span and the one before, for what began earlier;
		// and all which repeat.
		prefixes := []string{features.Repeating}
		month := features.Instant(first, "").AddDate(0, -1, 0)
		month = time.Date(month.Year(), month.Month(), 1, 0, 0, 0, 0, time.Local)
		for ; month.Format("2006-01-02") < last; month = month.AddDate(0, 1, 0) {
			prefixes = append(prefixes, features.MonthPrefix(month.Year(), int(month.Month())))
		}
		known, err := c.events(ctx, t, prefixes...)
		if err != nil {
			return nil, err
		}
		type shown struct {
			known knownEvent
			at    features.Occurrence
		}
		var list []shown
		for _, k := range known {
			for _, at := range features.Occurrences(k.event, features.Instant(first, ""), features.Instant(last, "")) {
				list = append(list, shown{k, at})
			}
		}
		sort.SliceStable(list, func(i, j int) bool { return list[i].at.Start.Before(list[j].at.Start) })
		out := []map[string]any{}
		locale := c.locale()
		for _, s := range list {
			event := s.known.event
			out = append(out, map[string]any{"id": s.known.entry.EntryID, "title": event.Title, "start": s.at.Start.UTC().Format(time.RFC3339),
				"end": s.at.End.UTC().Format(time.RFC3339), "allDay": event.AllDay, "place": event.Place, "repeats": event.Repeat != nil || event.Rule != "", "call": event.Call})
			if c.json() {
				continue
			}
			when := locale.DateTime(s.at.Start) + " – " + locale.Time(s.at.End)
			if event.AllDay {
				when = locale.Date(s.at.Start)
				if lastDay := s.at.End.AddDate(0, 0, -1); lastDay.After(s.at.Start) {
					when += " – " + locale.Date(lastDay)
				}
			}
			line := when + "  " + event.Title
			if event.Place != "" {
				line += "  (" + event.Place + ")"
			}
			if event.Repeat != nil || event.Rule != "" {
				line += "  ↻"
			}
			switch event.Call {
			case "audio":
				line += "  [" + i18n.Rune("Voice call") + "]"
			case "video":
				line += "  [" + i18n.Rune("Video call") + "]"
			}
			c.say("%s  %s", line, s.known.entry.EntryID)
		}
		if !c.json() && len(list) == 0 {
			c.say("%s", i18n.Rune("Nothing in these days."))
		}
		return map[string]any{"events": out}, nil
	})
}

// --- Location ----------------------------------------------------------------------

// location is 'tolk location [show|report|remove] --room <room>'.
func (c *command) location(args []string) error {
	verb := "show"
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		switch strings.ToLower(args[0]) {
		case "show", "report", "remove":
			verb, args = strings.ToLower(args[0]), args[1:]
		}
	}
	fs := c.flags("location " + verb)
	rooms := roomFlag(fs)
	accuracy := fs.Float64("accuracy", 0, "")
	note := fs.String("note", "", "")
	track := fs.Bool("track", false, "")
	passing := fs.Bool("passing", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	misuse := usage("Use 'tolk location [show] --room <room>', 'tolk location report <latitude> <longitude> --room <room> [--accuracy <metres>] [--note <text>] [--track] [--passing]' or 'tolk location remove --room <room>'.")
	if (verb == "report" && len(pos) != 2) || (verb != "report" && len(pos) != 0) {
		return misuse
	}
	var lat, lon float64
	if verb == "report" {
		var errLat, errLon error
		lat, errLat = strconv.ParseFloat(pos[0], 64)
		lon, errLon = strconv.ParseFloat(pos[1], 64)
		if errLat != nil || errLon != nil || lat < -90 || lat > 90 || lon < -180 || lon > 180 {
			return usage("%s %s is not a place. Give the latitude (-90 to 90) and the longitude (-180 to 180) as numbers.", pos[0], pos[1])
		}
	}
	return c.oneRoom(*rooms, misuse, func(ctx context.Context, t target) (any, error) {
		me, err := t.stone.MemberID(ctx)
		if err != nil {
			return nil, err
		}
		switch verb {
		case "report":
			position := features.NewPosition(lat, lon, *accuracy, time.Now(), *note)
			if *passing {
				// Passed on to who is there now, and not kept: a signal.
				conn, err := c.socketOf(ctx, t)
				if err != nil {
					return nil, err
				}
				defer conn.Close()
				data, _ := json.Marshal(position)
				conn.Signal(t.room.ID, "location", string(data))
				// The signal has no answer; a request after it tells that it went.
				if _, err := conn.Count(ctx, t.room.ID, "location", "position", 0); err != nil {
					return nil, err
				}
				c.note(i18n.T("%s: passed on where you are; it is not kept.", t.label))
				return map[string]any{"passed": position}, nil
			}
			if _, err := c.addEntry(ctx, t, "location", "position", position, "", ""); err != nil {
				return nil, err
			}
			if *track {
				if _, err := c.addEntry(ctx, t, "location", "track", position, "", ""); err != nil {
					return nil, err
				}
			}
			c.note(i18n.T("%s: reported %s.", t.label, features.Coordinates(position)))
			return map[string]any{"reported": position, "track": *track}, nil
		case "remove":
			removed := 0
			for _, kind := range []string{"position", "track"} {
				entries, err := c.listEntries(ctx, t, "location", client.EntryQuery{Kind: kind, Bare: true})
				if err != nil {
					return nil, err
				}
				for _, entry := range entries {
					if entry.MemberID == me {
						if err := c.removeEntry(ctx, t, "location", entry.EntryID); err != nil {
							return nil, err
						}
						removed++
					}
				}
			}
			c.note(i18n.T("%s: removed what was kept of where you were.", t.label))
			return map[string]any{"removed": removed}, nil
		}
		// show: of each person the newest position, and their track.
		positions, err := c.listEntries(ctx, t, "location", client.EntryQuery{Kind: "position"})
		if err != nil {
			return nil, err
		}
		points, err := c.listEntries(ctx, t, "location", client.EntryQuery{Kind: "track"})
		if err != nil {
			return nil, err
		}
		type seen struct {
			entry    client.Entry
			position features.Position
			track    []features.Position
		}
		people := map[string]*seen{}
		for i := range positions {
			var position features.Position
			if features.Decode(ctx, t.stone, "location", &positions[i], &position) && position.Valid() {
				if before := people[positions[i].MemberID]; before == nil || before.position.At < position.At {
					people[positions[i].MemberID] = &seen{entry: positions[i], position: position}
				}
			}
		}
		for i := range points {
			var position features.Position
			if person := people[points[i].MemberID]; person != nil && features.Decode(ctx, t.stone, "location", &points[i], &position) && position.Valid() {
				person.track = append(person.track, position)
			}
		}
		var list []*seen
		for _, person := range people {
			list = append(list, person)
		}
		sort.Slice(list, func(i, j int) bool { return list[i].position.At > list[j].position.At })
		out := []map[string]any{}
		locale := c.locale()
		var own *features.Position
		if mine := people[me]; mine != nil {
			own = &mine.position
		}
		for _, person := range list {
			entry := map[string]any{"name": person.entry.AccountName, "id": person.entry.MemberID, "position": person.position, "geo": features.GeoLink(person.position), "track": person.track}
			line := pad(person.entry.AccountName, 12) + "  " + c.when(person.position.At) + "  " + features.Coordinates(person.position)
			if person.position.Acc > 0 {
				line += " ±" + locale.Number(float64(person.position.Acc), 0) + " m"
			}
			if own != nil && person.entry.MemberID != me {
				metres := features.Distance(own.Lat, own.Lon, person.position.Lat, person.position.Lon)
				entry["distance"] = int(metres + 0.5)
				if metres < 1000 {
					line += "  " + i18n.T("%s m from you", locale.Number(metres, 0))
				} else {
					line += "  " + i18n.T("%s km from you", locale.Number(metres/1000, 1))
				}
			}
			if person.position.Note != "" {
				line += "  " + person.position.Note
			}
			out = append(out, entry)
			if !c.json() {
				c.say("%s", line)
				c.say("%s  %s", strings.Repeat(" ", 12), features.GeoLink(person.position))
				if len(person.track) > 0 {
					c.say("%s  %s", strings.Repeat(" ", 12), i18n.T("track of the last day: %d places", len(person.track)))
				}
			}
		}
		if !c.json() && len(list) == 0 {
			c.say("%s", i18n.Rune("Nobody has reported where they are."))
		}
		return map[string]any{"people": out}, nil
	})
}
