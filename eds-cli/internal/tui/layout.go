package tui

import (
	"encoding/json"
)

// The panes, with Rune's names.
const (
	paneRooms = "rooms"
	paneChat  = "chat"
	paneFiles = "files"
)

var allPanes = []string{paneRooms, paneChat, paneFiles}

// node is Rune's layout tree (layout.ts), so a layout can be copied between
// Rune and Tolk: a pane, or a row or column of nodes with their shares.
type node struct {
	Pane     string    `json:"pane,omitempty"`
	Dir      string    `json:"dir,omitempty"` // row, column
	Children []*node   `json:"children,omitempty"`
	Sizes    []float64 `json:"sizes,omitempty"`
}

func leaf(pane string) *node { return &node{Pane: pane} }

// preset is a named layout, as in Rune.
type preset struct {
	Key    string // the value of the setting
	Name   string // Rune's English name
	Layout func() *node
}

var presets = []preset{
	{"three-columns", "Three columns", func() *node {
		return &node{Dir: "row", Children: []*node{leaf(paneRooms), leaf(paneChat), leaf(paneFiles)}, Sizes: []float64{0.22, 0.43, 0.35}}
	}},
	{"chat-on-top", "Chat on top", func() *node {
		return &node{Dir: "column", Children: []*node{leaf(paneChat),
			{Dir: "row", Children: []*node{leaf(paneRooms), leaf(paneFiles)}, Sizes: []float64{0.4, 0.6}}}, Sizes: []float64{0.6, 0.4}}
	}},
	{"rooms-beside", "Rooms beside chat over files", func() *node {
		return &node{Dir: "row", Children: []*node{leaf(paneRooms),
			{Dir: "column", Children: []*node{leaf(paneChat), leaf(paneFiles)}, Sizes: []float64{0.6, 0.4}}}, Sizes: []float64{0.25, 0.75}}
	}},
	{"chat-and-files", "Chat and files", func() *node {
		return &node{Dir: "row", Children: []*node{leaf(paneChat), leaf(paneFiles)}, Sizes: []float64{0.55, 0.45}}
	}},
	{"chat-only", "Chat only", func() *node { return leaf(paneChat) }},
}

// layoutFor returns the layout of the settings: a preset by its name, or the
// stored tree if it is a valid one; otherwise Rune's default.
func layoutFor(name string, tree json.RawMessage) *node {
	if name == "custom" && len(tree) > 0 {
		n := &node{}
		if json.Unmarshal(tree, n) == nil && n.valid(map[string]bool{}) {
			return n
		}
	}
	for _, p := range presets {
		if p.Key == name {
			return p.Layout()
		}
	}
	return presets[0].Layout()
}

// valid checks a tree as Rune does: known panes, each once, and splits of
// two or more with a positive share each.
func (n *node) valid(seen map[string]bool) bool {
	if n == nil {
		return false
	}
	if n.Pane != "" {
		if seen[n.Pane] || (n.Pane != paneRooms && n.Pane != paneChat && n.Pane != paneFiles) || len(n.Children) > 0 {
			return false
		}
		seen[n.Pane] = true
		return true
	}
	if (n.Dir != "row" && n.Dir != "column") || len(n.Children) < 2 || len(n.Children) != len(n.Sizes) {
		return false
	}
	for i, child := range n.Children {
		if n.Sizes[i] <= 0 || !child.valid(seen) {
			return false
		}
	}
	return true
}

// panes lists the panes of a layout, in reading order.
func (n *node) panes() []string {
	if n == nil {
		return nil
	}
	if n.Pane != "" {
		return []string{n.Pane}
	}
	var out []string
	for _, child := range n.Children {
		out = append(out, child.panes()...)
	}
	return out
}

func (n *node) has(pane string) bool {
	for _, p := range n.panes() {
		if p == pane {
			return true
		}
	}
	return false
}

// rect is a pane's place on the screen, in cells.
type rect struct{ x, y, w, h int }

// place gives every pane of a layout its rectangle inside the area: each
// split shares its length by the sizes, and the last child takes the rest.
func (n *node) place(area rect, out map[string]rect) {
	if n == nil {
		return
	}
	if n.Pane != "" {
		out[n.Pane] = area
		return
	}
	total := 0.0
	for _, size := range n.Sizes {
		total += size
	}
	length := area.w
	if n.Dir == "column" {
		length = area.h
	}
	offset := 0
	for i, child := range n.Children {
		share := int(float64(length)*n.Sizes[i]/total + 0.5)
		if i == len(n.Children)-1 {
			share = length - offset
		}
		share = max(share, 0)
		if n.Dir == "column" {
			child.place(rect{area.x, area.y + offset, area.w, share}, out)
		} else {
			child.place(rect{area.x + offset, area.y, share, area.h}, out)
		}
		offset += share
	}
}

// isRooms tells the rooms pane by itself: where one chooses, which keeps its
// size when other panes come and go (Rune's layout.ts).
func (n *node) isRooms() bool { return n != nil && n.Pane == paneRooms }

// remove takes a pane out of a layout; a split left with one child becomes
// that child. The place which gets free goes to the panes with content, in
// the proportion of their sizes: the rooms pane keeps its size, as in Rune.
// It returns nil when the pane was the whole layout.
func (n *node) remove(pane string) *node {
	if n == nil {
		return nil
	}
	if n.Pane != "" {
		if n.Pane == pane {
			return nil
		}
		return n
	}
	var children []*node
	var sizes []float64
	freed := 0.0
	for i, child := range n.Children {
		if kept := child.remove(pane); kept != nil {
			children, sizes = append(children, kept), append(sizes, n.Sizes[i])
		} else {
			freed += n.Sizes[i]
		}
	}
	share := 0.0
	for i, child := range children {
		if !child.isRooms() {
			share += sizes[i]
		}
	}
	if freed > 0 && share > 0 {
		for i, child := range children {
			if !child.isRooms() {
				sizes[i] += freed * sizes[i] / share
			}
		}
	}
	switch len(children) {
	case 0:
		return nil
	case 1:
		return children[0]
	}
	return &node{Dir: n.Dir, Children: children, Sizes: sizes}
}

// add shows a pane again: the rooms at the left, the files at the right,
// the chat between them, as Rune's first layout has them. As in Rune, a
// pane with content takes its place from the other panes with content, an
// equal share of theirs, and the rooms pane keeps its size; the rooms pane
// itself comes back with 22 % of the width.
func (n *node) add(pane string) *node {
	if n == nil {
		return leaf(pane)
	}
	if n.has(pane) {
		return n
	}
	fresh := leaf(pane)
	if n.Pane == "" && n.Dir == "row" {
		children, sizes := append([]*node{}, n.Children...), append([]float64{}, n.Sizes...)
		total, theirs, givers := 0.0, 0.0, 0
		for i, size := range sizes {
			total += size
			if !children[i].isRooms() {
				theirs += size
				givers++
			}
		}
		if pane == paneRooms {
			return &node{Dir: "row", Children: append([]*node{fresh}, children...), Sizes: append([]float64{total * 0.22 / 0.78}, sizes...)}
		}
		share := total / float64(len(sizes)+1)
		if givers > 0 && theirs > 0 {
			share = theirs / float64(givers+1)
			for i := range sizes {
				if !children[i].isRooms() {
					sizes[i] *= (theirs - share) / theirs
				}
			}
		} else {
			for i := range sizes {
				sizes[i] *= float64(len(sizes)) / float64(len(sizes)+1)
			}
		}
		at := len(children)
		if pane != paneFiles {
			at = 0
			if len(children) > 0 && children[0].isRooms() {
				at = 1
			}
		}
		children = append(children[:at], append([]*node{fresh}, children[at:]...)...)
		sizes = append(sizes[:at], append([]float64{share}, sizes[at:]...)...)
		return &node{Dir: "row", Children: children, Sizes: sizes}
	}
	if pane == paneRooms {
		return &node{Dir: "row", Children: []*node{fresh, n}, Sizes: []float64{0.22, 0.78}}
	}
	if n.isRooms() {
		return &node{Dir: "row", Children: []*node{n, fresh}, Sizes: []float64{0.22, 0.78}}
	}
	return &node{Dir: "row", Children: []*node{n, fresh}, Sizes: []float64{0.6, 0.4}}
}

// toggle puts a pane away, or brings it back to where it was and as large
// as it was (Rune's togglePane): before is the layout from before the pane
// was put away. When the other panes were arranged anew meanwhile, the pane
// comes back as add places it. The only pane shown stays. It returns the
// new layout, and the layout to keep for bringing the pane back.
func (n *node) toggle(pane string, before json.RawMessage) (*node, json.RawMessage) {
	if n.has(pane) {
		rest := n.remove(pane)
		if rest == nil {
			return n, before
		}
		return rest, n.encode()
	}
	if len(before) > 0 {
		kept := &node{}
		if json.Unmarshal(before, kept) == nil && kept.valid(map[string]bool{}) && kept.has(pane) {
			if rest := kept.remove(pane); rest != nil && string(rest.encode()) == string(n.encode()) {
				// Read again: remove may have changed the sizes of what it was given.
				again := &node{}
				if json.Unmarshal(before, again) == nil {
					return again, before
				}
			}
		}
	}
	return n.add(pane), before
}

// resize moves the divider beside a pane: the pane grows (or shrinks, for a
// negative step) along the direction, and its neighbour gives (or takes)
// the same. It reports whether anything moved.
func (n *node) resize(pane, dir string, step float64) bool {
	if n == nil || n.Pane != "" {
		return false
	}
	for i, child := range n.Children {
		if !child.has(pane) {
			continue
		}
		// The innermost split in that direction is the one to change.
		if child.resize(pane, dir, step) {
			return true
		}
		if n.Dir != dir {
			return false
		}
		other := i + 1
		if other >= len(n.Children) {
			other = i - 1
		}
		total := 0.0
		for _, size := range n.Sizes {
			total += size
		}
		delta := step * total
		const least = 0.1
		if n.Sizes[i]+delta < least*total || n.Sizes[other]-delta < least*total {
			return false
		}
		n.Sizes[i] += delta
		n.Sizes[other] -= delta
		return true
	}
	return false
}

// swap exchanges two panes' places.
func (n *node) swap(a, b string) {
	if n == nil {
		return
	}
	switch n.Pane {
	case a:
		n.Pane = b
	case b:
		n.Pane = a
	}
	for _, child := range n.Children {
		child.swap(a, b)
	}
}

// encode writes the tree for the settings.
func (n *node) encode() json.RawMessage {
	data, _ := json.Marshal(n)
	return data
}
