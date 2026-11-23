// Package theme has Rune's palettes for the terminal: the same hex values
// as Rune's CSS (a test reads rune-theme.css and themes.css, so they can't
// drift), written in true colour, or the nearest of 256 or 16 colours where
// the terminal has no more. NO_COLOR turns colour off.
package theme

import (
	"fmt"
	"os"
	"strings"
)

// Colours are the tokens of a palette, as #RRGGBB.
type Colours struct {
	Bg, Surface, Border, Text, Muted, Accent, OnAccent string
}

// Palette is a theme of Rune, light and dark.
type Palette struct {
	Light, Dark Colours
}

// Names are the palettes in Rune's order.
var Names = []string{"rune", "fjord", "moss", "contrast"}

// Palettes are Rune's palettes.
var Palettes = map[string]Palette{
	"rune": {
		Light: Colours{"#F3F0E8", "#FBFAF6", "#D9D4C8", "#1E2124", "#5C6166", "#A63F24", "#F3F0E8"},
		Dark:  Colours{"#1E2124", "#3A3F44", "#4A5056", "#F3F0E8", "#B9BDC1", "#EC9473", "#1E2124"},
	},
	"fjord": {
		Light: Colours{"#EEF2F5", "#F9FBFC", "#CBD5DD", "#1B2733", "#4E5D6B", "#1F5F8B", "#FFFFFF"},
		Dark:  Colours{"#141B22", "#1E2731", "#33414E", "#E6EDF3", "#A9B6C2", "#6FB3E0", "#0E1620"},
	},
	"moss": {
		Light: Colours{"#F1F3EC", "#FAFBF7", "#D0D6C4", "#1F261C", "#545E4E", "#3F6B2E", "#FFFFFF"},
		Dark:  Colours{"#171C15", "#222A1F", "#39432F", "#E8EDE2", "#B0BAA6", "#8DBF6A", "#142010"},
	},
	"contrast": {
		Light: Colours{"#FFFFFF", "#FFFFFF", "#000000", "#000000", "#333333", "#0B3D91", "#FFFFFF"},
		Dark:  Colours{"#000000", "#0A0A0A", "#FFFFFF", "#FFFFFF", "#DDDDDD", "#FFD400", "#000000"},
	},
}

// Depth is how many colours the terminal has.
type Depth int

// The depths, from none to true colour.
const (
	None Depth = iota
	Basic
	Indexed
	True
)

// Detect reads the terminal's colours from the environment. Without a
// terminal, or with NO_COLOR set, there are none.
func Detect(isTerminal bool) Depth {
	if !isTerminal || os.Getenv("NO_COLOR") != "" || os.Getenv("TERM") == "dumb" {
		return None
	}
	if ct := strings.ToLower(os.Getenv("COLORTERM")); ct == "truecolor" || ct == "24bit" {
		return True
	}
	if strings.Contains(os.Getenv("TERM"), "256color") {
		return Indexed
	}
	return Basic
}

// Style writes text in a palette's colours.
type Style struct {
	Colours  Colours
	Depth    Depth
	Contrast bool // "High contrast" uses bold and underline as well as colour
}

// New makes the style for a palette; an unknown name gives Rune's own. On a
// terminal without a known background the dark side is used with "auto",
// as most terminals are dark.
func New(name, appearance string, depth Depth) Style {
	palette, ok := Palettes[name]
	if !ok {
		name, palette = "rune", Palettes["rune"]
	}
	colours := palette.Dark
	if appearance == "light" {
		colours = palette.Light
	}
	return Style{Colours: colours, Depth: depth, Contrast: name == "contrast"}
}

// Accent writes text in the accent colour, bold: for what Rune emphasises.
func (s Style) Accent(text string) string {
	codes := "1"
	if s.Contrast {
		codes = "1;4"
	}
	return s.paint(text, s.Colours.Accent, codes)
}

// Muted writes text in the muted colour: for what Rune shows quietly.
func (s Style) Muted(text string) string {
	return s.paint(text, s.Colours.Muted, "")
}

func (s Style) paint(text, hex, extra string) string {
	if s.Depth == None || text == "" {
		return text
	}
	r, g, b := RGB(hex)
	var colour string
	switch s.Depth {
	case True:
		colour = fmt.Sprintf("38;2;%d;%d;%d", r, g, b)
	case Indexed:
		colour = fmt.Sprintf("38;5;%d", Index256(r, g, b))
	default:
		colour = fmt.Sprintf("%d", Index16(r, g, b))
	}
	if extra != "" {
		colour = extra + ";" + colour
	}
	return "\x1b[" + colour + "m" + text + "\x1b[0m"
}

// RGB reads #RRGGBB.
func RGB(hex string) (r, g, b int) {
	_, _ = fmt.Sscanf(strings.TrimPrefix(hex, "#"), "%02x%02x%02x", &r, &g, &b)
	return
}

var cube = []int{0, 95, 135, 175, 215, 255}

// Index256 finds the nearest of the 256 colours: the 6×6×6 cube or the
// grey ramp, whichever is closer.
func Index256(r, g, b int) int {
	nearest := func(v int) int {
		best := 0
		for i, level := range cube {
			if abs(level-v) < abs(cube[best]-v) {
				best = i
			}
		}
		return best
	}
	ri, gi, bi := nearest(r), nearest(g), nearest(b)
	cubeDist := distance(r, g, b, cube[ri], cube[gi], cube[bi])
	grey := (r + g + b) / 3
	gi2 := (grey - 8 + 5) / 10
	if gi2 < 0 {
		gi2 = 0
	}
	if gi2 > 23 {
		gi2 = 23
	}
	level := 8 + 10*gi2
	if distance(r, g, b, level, level, level) < cubeDist {
		return 232 + gi2
	}
	return 16 + 36*ri + 6*gi + bi
}

// The 16 colours as most terminals show them.
var basic = [16][3]int{
	{0, 0, 0}, {170, 0, 0}, {0, 170, 0}, {170, 85, 0}, {0, 0, 170}, {170, 0, 170}, {0, 170, 170}, {170, 170, 170},
	{85, 85, 85}, {255, 85, 85}, {85, 255, 85}, {255, 255, 85}, {85, 85, 255}, {255, 85, 255}, {85, 255, 255}, {255, 255, 255},
}

// Index16 finds the nearest of the 16 colours, as its foreground code
// (30–37, 90–97).
func Index16(r, g, b int) int {
	best := 0
	for i, c := range basic {
		if distance(r, g, b, c[0], c[1], c[2]) < distance(r, g, b, basic[best][0], basic[best][1], basic[best][2]) {
			best = i
		}
	}
	if best < 8 {
		return 30 + best
	}
	return 90 + best - 8
}

func distance(r1, g1, b1, r2, g2, b2 int) int {
	return (r1-r2)*(r1-r2) + (g1-g2)*(g1-g2) + (b1-b2)*(b1-b2)
}

func abs(v int) int {
	if v < 0 {
		return -v
	}
	return v
}
