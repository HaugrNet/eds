package status

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// The status of one stone as people watch it: what the stone last told,
// the samples gathered while watching, and the lines which show it, in the
// table of 'tolk status', its live view, and the interactive mode.

// View is what is known of one stone while its status is watched.
type View struct {
	Name      string // the stone's name in the container
	URL       string
	Status    *client.StatusResponse
	Load      []client.StatusSample
	Inventory []client.StatusSample
	Problem   string // why the stone gave no status
	Answered  time.Time
}

// Asker asks a stone, by its name in the container, for its status.
type Asker func(ctx context.Context, name string, since int64, history bool) (*client.StatusResponse, error)

// Ask asks the stone again, and adds what is new to what is known.
func (w *View) Ask(ctx context.Context, ask Asker, history bool) {
	since := int64(0)
	if len(w.Load) > 0 {
		since = w.Load[len(w.Load)-1].Sequence
	}
	asked, cancel := context.WithTimeout(ctx, 15*time.Second)
	defer cancel()
	got, err := ask(asked, w.Name, since, history)
	if err != nil {
		var e *exit.Error
		switch {
		case errors.As(err, &e) && (e.ReturnCode == 403 || e.ReturnCode == 401):
			w.Problem = i18n.T("refuses the key or the account")
		case errors.As(err, &e) && e.Code == exit.Network:
			w.Problem = i18n.Rune("does not answer")
		default:
			w.Problem = err.Error()
		}
		return
	}
	w.Status, w.Problem, w.Answered = got, "", time.Now()
	if got.Load != nil {
		w.Load = Merge(w.Load, got.Load.Samples)
	}
	if got.Inventory != nil {
		if history {
			w.Inventory = got.Inventory.Samples
		} else {
			w.Inventory = Merge(w.Inventory, got.Inventory.Samples)
		}
	}
}

// Title is the stone's own name, if it has one, and its name in the container.
func (w *View) Title() string {
	if w.Status != nil && w.Status.Name != "" {
		return w.Status.Name + " (" + w.Name + ")"
	}
	return w.Name
}

// Interval is the seconds between two samples of the load.
func (w *View) Interval() float64 {
	if w.Status != nil && w.Status.Load != nil && w.Status.Load.Interval > 0 {
		return float64(w.Status.Load.Interval)
	}
	return 5
}

// JSON is the status of a stone for scripts: the latest numbers by their
// names.
func (w *View) JSON() map[string]any {
	out := map[string]any{"stone": w.Name, "url": w.URL}
	if w.Problem != "" {
		out["problem"] = w.Problem
	}
	if w.Status == nil {
		return out
	}
	s := w.Status
	out["name"], out["version"], out["awake"], out["started"], out["database"] = s.Name, s.Version, s.Awake, s.Started, s.Database
	latest := func(samples []client.StatusSample) map[string]float64 {
		if len(samples) == 0 {
			return map[string]float64{}
		}
		return samples[len(samples)-1].Values
	}
	out["load"], out["inventory"] = latest(w.Load), latest(w.Inventory)
	if full, ok := FullAt(w.Inventory); ok {
		out["fullAt"] = full.UTC().Format(time.RFC3339)
	}
	return out
}

// mark puts a sign before what is near a limit.
func mark(level string) string {
	switch level {
	case Near:
		return "!!"
	case Watch:
		return "! "
	}
	return "  "
}

// Lines are the lines of one stone in the live view: bars and small curves.
func (w *View) Lines(locale format.Locale) []string {
	head := "● " + w.Title()
	switch {
	case w.Problem != "" && w.Status == nil:
		return []string{head + "  " + w.Problem, ""}
	case w.Problem != "":
		head += "  " + w.Problem + " (" + i18n.T("last answer %s", locale.Time(w.Answered)) + ")"
	case w.Status.Awake:
		head += "  " + i18n.Rune("awake")
	default:
		head += "  " + i18n.Rune("asleep: waits for its MasterKey")
	}
	s := w.Status
	about := []string{"EDS " + s.Version}
	if s.Database != "" {
		about = append(about, s.Database)
	}
	if started, ok := Time(s.Started); ok {
		about = append(about, i18n.T("running since %s", locale.DateTime(started.Local())))
	}
	lines := []string{head, "  " + strings.Join(about, " · ")}
	if used, ok := Latest(w.Inventory, "storageUsed"); ok {
		ceiling, _ := Latest(w.Inventory, "storageCeiling")
		share := 0.0
		if ceiling > 0 {
			share = used / ceiling
		}
		line := fmt.Sprintf("%s%-9s %s %5.1f %%  %s", mark(Level(share, ceiling > 0, 0.8, 0.95)), i18n.Rune("Storage"), Bar(share, 24), share*100,
			i18n.Rune("{used} of {max}", "used", locale.Size(int64(used)), "max", locale.Size(int64(ceiling))))
		if full, ok := FullAt(w.Inventory); ok {
			line += "  " + i18n.T("full around %s", locale.DateTime(full.Local()))
		}
		lines = append(lines, line)
	}
	number := func(name string) (float64, bool) { return Latest(w.Load, name) }
	row := func(level, label, shown string, values []float64, top float64) string {
		return fmt.Sprintf("%s%-22s %-22s %s", mark(level), label, shown, Spark(values, 40, top))
	}
	accounts, _ := number("accounts")
	lines = append(lines, row(Fine, i18n.Rune("People active"), locale.Number(accounts, 0), Values(w.Load, "accounts"), 0))
	requests, _ := number("requests")
	perSecond := Values(w.Load, "requests")
	for i := range perSecond {
		perSecond[i] /= w.Interval()
	}
	lines = append(lines, row(Fine, i18n.Rune("Requests per second"), locale.Number(requests/w.Interval(), 1), perSecond, 0))
	if share, ok := number("processShare"); ok {
		lines = append(lines, row(Level(share, true, 0.7, 0.9), i18n.T("Stone's processors"), locale.Number(share*100, 0)+" %", Values(w.Load, "processShare"), 1))
	}
	if system, ok := number("systemLoad"); ok && system >= 0 {
		processors, _ := number("processors")
		lines = append(lines, row(Level(system/max(1, processors), true, 1, 2), i18n.Rune("Load of the host"),
			i18n.Rune("{load} on {n} processors", "load", locale.Number(system, 2), "n", locale.Number(processors, 0)), Values(w.Load, "systemLoad"), 0))
	}
	if used, ok := number("memoryUsed"); ok {
		most, _ := number("memoryMax")
		shown := locale.Size(int64(used))
		if most > 0 {
			shown = i18n.Rune("{used} of {max}", "used", locale.Size(int64(used)), "max", locale.Size(int64(most)))
		}
		lines = append(lines, row(Level(used/max(1, most), most > 0, 0.8, 0.95), i18n.Rune("Memory"), shown, Values(w.Load, "memoryUsed"), most))
	}
	if millis, ok := number("requestMillis"); ok {
		lines = append(lines, row(Level(millis, true, 500, 2000), i18n.Rune("Time a request takes"), locale.Number(millis, 1)+" ms", Values(w.Load, "requestMillis"), 0))
	}
	failed, _ := number("failed")
	database, _ := number("databaseMillis")
	connections, _ := number("connections")
	count := func(name string) string {
		value, ok := Latest(w.Inventory, name)
		if !ok {
			return "–"
		}
		return locale.Number(value, 0)
	}
	lines = append(lines, "  "+strings.Join([]string{
		i18n.Rune("Refused or failed") + " " + locale.Number(failed, 0),
		i18n.Rune("Answer of the database") + " " + locale.Number(database, 1) + " ms",
		i18n.Rune("Connections") + " " + locale.Number(connections, 0),
	}, " · "), "  "+strings.Join([]string{
		i18n.Rune("Accounts") + " " + count("accounts"), i18n.Rune("Rooms") + " " + count("circles"), i18n.Rune("Files") + " " + count("files"),
		i18n.Rune("Entries of features") + " " + count("entries"), i18n.Rune("Sessions") + " " + count("sessions"),
	}, " · "), "")
	return lines
}
