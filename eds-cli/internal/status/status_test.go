package status

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
)

func sample(sequence int64, at string, values map[string]float64) client.StatusSample {
	if at == "" {
		at = "2026-11-15T10:00:00"
	}
	return client.StatusSample{Sequence: sequence, Time: at, Values: values}
}

func sequences(samples []client.StatusSample) []int64 {
	out := []int64{}
	for _, s := range samples {
		out = append(out, s.Sequence)
	}
	return out
}

func same(t *testing.T, what string, got, want any) {
	t.Helper()
	g, _ := json.Marshal(got)
	w, _ := json.Marshal(want)
	if string(g) != string(w) {
		t.Errorf("%s: got %s, want %s", what, g, w)
	}
}

// The same cases as Rune's test of src/status.ts.
func TestStatus(t *testing.T) {
	one, two, three := sample(1, "", nil), sample(2, "", nil), sample(3, "", nil)
	same(t, "new samples are added without doubles", sequences(Merge([]client.StatusSample{one, two}, []client.StatusSample{two, three})), []int64{1, 2, 3})
	same(t, "a stone which started anew begins again", sequences(Merge([]client.StatusSample{sample(100, "", nil), sample(101, "", nil)}, []client.StatusSample{one, two})), []int64{1, 2})
	same(t, "nothing new", sequences(Merge([]client.StatusSample{one}, nil)), []int64{1})
	if value, ok := Latest([]client.StatusSample{sample(1, "", map[string]float64{"a": 1}), sample(2, "", map[string]float64{"b": 2})}, "a"); !ok || value != 1 {
		t.Errorf("the latest value told: %v, %v", value, ok)
	}
	if _, ok := Latest([]client.StatusSample{one}, "a"); ok {
		t.Error("a value the stone doesn't tell")
	}
	filling := []client.StatusSample{
		sample(1, "2026-11-15T10:00:00", map[string]float64{"storageUsed": 100, "storageCeiling": 1000}),
		sample(2, "2026-11-15T13:00:00", map[string]float64{"storageUsed": 400, "storageCeiling": 1000}),
	}
	if full, ok := FullAt(filling); !ok || full.UTC().Format(time.RFC3339) != "2026-11-15T19:00:00Z" {
		t.Errorf("full in six hours if it fills as it did: %v, %v", full, ok)
	}
	if _, ok := FullAt([]client.StatusSample{filling[1], filling[0]}); ok {
		t.Error("an estimate for a stone which isn't filling")
	}
	if _, ok := FullAt(filling[:1]); ok {
		t.Error("an estimate from one inventory")
	}
	same(t, "how near a limit", []string{Level(0.5, true, 0.7, 0.9), Level(0.8, true, 0.7, 0.9), Level(0.95, true, 0.7, 0.9), Level(0.95, false, 0.7, 0.9)}, []string{Fine, Watch, Near, Fine})
	same(t, "how busy", []float64{
		Busy([]client.StatusSample{sample(1, "", map[string]float64{"processShare": 0.25, "systemLoad": 8, "processors": 4})}),
		Busy([]client.StatusSample{sample(1, "", map[string]float64{"systemLoad": 2, "processors": 4})}), Busy(nil)}, []float64{0.25, 0.5, 0})
	same(t, "a small curve", []string{Spark([]float64{0, 5, 10}, 10, 0), Spark([]float64{1, 2, 3, 4}, 2, 4), Spark(nil, 5, 0), Spark([]float64{0, 0}, 5, 0)}, []string{"▁▅█", "▆█", "", "▁▁"})
	same(t, "a bar", []string{Bar(0.5, 8), Bar(0, 4), Bar(2, 4)}, []string{"████░░░░", "░░░░", "████"})
	if at, ok := Time("2026-11-15T15:33:12.579426614"); !ok || at.UTC().Hour() != 15 {
		t.Errorf("a time of the stone: %v, %v", at, ok)
	}
}
