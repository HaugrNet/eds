package tui

import (
	"encoding/json"
	"math"
	"testing"
)

// share gives the part of a row which a pane has, of 1.
func share(t *testing.T, n *node, pane string) float64 {
	t.Helper()
	total := 0.0
	for _, size := range n.Sizes {
		total += size
	}
	for i, child := range n.Children {
		if child.Pane == pane {
			return n.Sizes[i] / total
		}
	}
	t.Fatalf("no pane %s in %s", pane, n.encode())
	return 0
}

func near(a, b float64) bool { return math.Abs(a-b) < 1e-9 }

// The rooms pane keeps its size when panes with content come and go, as in
// Rune's layout.ts (removePane, addPane).
func TestRoomsPaneKeepsItsSize(t *testing.T) {
	three := presets[0].Layout()
	rooms := share(t, three, paneRooms)

	without := three.remove(paneChat)
	if got := share(t, without, paneRooms); !near(got, rooms) {
		t.Errorf("without the chat the rooms have %v, not %v", got, rooms)
	}
	if got := share(t, without, paneFiles); !near(got, 1-rooms) {
		t.Errorf("the files have %v, not the rest %v", got, 1-rooms)
	}

	again := without.add(paneChat)
	if got := share(t, again, paneRooms); !near(got, rooms) {
		t.Errorf("with the chat again the rooms have %v, not %v", got, rooms)
	}
	if chat, files := share(t, again, paneChat), share(t, again, paneFiles); !near(chat, files) || !near(chat, (1-rooms)/2) {
		t.Errorf("chat %v and files %v don't share the rest equally", chat, files)
	}
	if order := again.panes(); order[0] != paneRooms || order[1] != paneChat || order[2] != paneFiles {
		t.Errorf("order %v", order)
	}

	// The rooms pane itself comes back at the left, with 22 % of the width.
	back := three.remove(paneRooms).add(paneRooms)
	if got := share(t, back, paneRooms); !near(got, 0.22) || back.panes()[0] != paneRooms {
		t.Errorf("the rooms come back with %v in %v", got, back.panes())
	}
}

// A pane put away comes back where it was and as large as it was, as with
// Rune's togglePane; after another change it comes back as add places it.
func TestPaneComesBackWhereItWas(t *testing.T) {
	// The files left of the chat, and of a size of the person's own.
	layout := &node{Dir: "row", Children: []*node{leaf(paneRooms), leaf(paneFiles), leaf(paneChat)}, Sizes: []float64{0.2, 0.5, 0.3}}
	was := string(layout.encode())

	away, before := layout.toggle(paneFiles, nil)
	if away.has(paneFiles) || len(before) == 0 {
		t.Fatalf("the files are not away: %s", away.encode())
	}
	back, _ := away.toggle(paneFiles, before)
	if got := string(back.encode()); got != was {
		t.Errorf("back as %s, was %s", got, was)
	}

	// Stored and read again, as the settings do.
	var stored json.RawMessage = append(json.RawMessage{}, away.encode()...)
	read := layoutFor("custom", stored)
	if back, _ := read.toggle(paneFiles, before); string(back.encode()) != was {
		t.Errorf("after reading the settings back as %s, was %s", back.encode(), was)
	}

	// Arranged anew meanwhile: at the right, as add places the files.
	away.Sizes[0] = 0.4
	other, _ := away.toggle(paneFiles, before)
	if order := other.panes(); order[len(order)-1] != paneFiles {
		t.Errorf("after a change the files are at %v", order)
	}

	// The only pane stays.
	only := leaf(paneChat)
	if same, _ := only.toggle(paneChat, nil); !same.has(paneChat) {
		t.Error("the only pane went away")
	}
}
