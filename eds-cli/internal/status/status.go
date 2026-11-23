// Package status is what Tolk works out of what a stone tells about itself
// (STATUS.md): the samples which are new, the latest numbers, when the
// storage is full, how near a limit a number is, and bars and small curves
// for a terminal. It says the same as Rune's src/status.ts.
package status

import (
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
)

// kept is as many samples as a stone keeps.
const kept = 720

// Merge adds the new samples of a series to those which are here, the
// oldest first, without doubles. A stone which started anew counts from the
// beginning: what was here is of its earlier life.
func Merge(have, more []client.StatusSample) []client.StatusSample {
	if len(more) == 0 {
		return have
	}
	if len(have) > 0 && more[len(more)-1].Sequence < have[len(have)-1].Sequence {
		have = nil
	}
	last := int64(-1)
	if len(have) > 0 {
		last = have[len(have)-1].Sequence
	}
	out := append([]client.StatusSample{}, have...)
	for _, sample := range more {
		if sample.Sequence > last {
			out = append(out, sample)
		}
	}
	if len(out) > kept {
		out = out[len(out)-kept:]
	}
	return out
}

// Latest is the latest value of a name in a series; false if the stone
// doesn't tell it.
func Latest(samples []client.StatusSample, name string) (float64, bool) {
	for i := len(samples) - 1; i >= 0; i-- {
		if value, ok := samples[i].Values[name]; ok {
			return value, true
		}
	}
	return 0, false
}

// Values are the values of a name over a series; what a sample doesn't
// tell counts as nothing.
func Values(samples []client.StatusSample, name string) []float64 {
	out := make([]float64, len(samples))
	for i, sample := range samples {
		out[i] = sample.Values[name]
	}
	return out
}

// Time reads a time of the stone: UTC, without a zone.
func Time(text string) (time.Time, bool) {
	for _, layout := range []string{"2006-01-02T15:04:05.999999999", time.RFC3339Nano} {
		if at, err := time.Parse(layout, text); err == nil {
			return at, true
		}
	}
	return time.Time{}, false
}

// FullAt is when the storage is full, if it goes on filling as it did over
// the kept inventories; false if it isn't filling, has no ceiling, or too
// little is known.
func FullAt(inventory []client.StatusSample) (time.Time, bool) {
	var known []client.StatusSample
	for _, sample := range inventory {
		if _, ok := sample.Values["storageUsed"]; ok {
			known = append(known, sample)
		}
	}
	if len(known) < 2 {
		return time.Time{}, false
	}
	first, last := known[0], known[len(known)-1]
	from, okFrom := Time(first.Time)
	to, okTo := Time(last.Time)
	ceiling := last.Values["storageCeiling"]
	grown := last.Values["storageUsed"] - first.Values["storageUsed"]
	passed := to.Sub(from)
	if !okFrom || !okTo || ceiling <= 0 || grown <= 0 || passed <= 0 {
		return time.Time{}, false
	}
	left := (ceiling - last.Values["storageUsed"]) / grown
	return to.Add(time.Duration(left * float64(passed))), true
}

// How near a limit a number is. The limits are Tolk's own.
const (
	Fine  = "fine"
	Watch = "watch"
	Near  = "near"
)

// Level says how near a limit a value is.
func Level(value float64, known bool, watch, near float64) string {
	switch {
	case !known || value < watch:
		return Fine
	case value < near:
		return Watch
	}
	return Near
}

// Busy is how busy a stone is, for sorting: its share of the processors,
// else the host's load per processor.
func Busy(load []client.StatusSample) float64 {
	if share, ok := Latest(load, "processShare"); ok {
		return share
	}
	system, ok := Latest(load, "systemLoad")
	if !ok || system < 0 {
		return 0
	}
	processors, _ := Latest(load, "processors")
	return system / max(1, processors)
}

var blocks = []rune("▁▂▃▄▅▆▇█")

// Spark is a small curve over the last values of a series, as wide as
// asked: one character for each value. top is the value of a full bar; 0
// takes the largest of the values.
func Spark(values []float64, width int, top float64) string {
	if len(values) > width {
		values = values[len(values)-width:]
	}
	most := top
	for _, value := range values {
		most = max(most, value)
	}
	var out strings.Builder
	for _, value := range values {
		index := 0
		if most > 0 && value > 0 {
			index = min(len(blocks)-1, int(value/most*float64(len(blocks)-1)+0.5))
		}
		out.WriteRune(blocks[index])
	}
	return out.String()
}

// Bar is a bar of a width which is filled as far as a share from 0 to 1.
func Bar(share float64, width int) string {
	filled := int(min(1, max(0, share))*float64(width) + 0.5)
	return strings.Repeat("█", filled) + strings.Repeat("░", width-filled)
}
