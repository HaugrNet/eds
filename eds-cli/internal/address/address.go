// Package address parses Tolk's addresses: stone:room/path.
//
// The stone and its colon may be left out for the default stone. A colon or
// slash inside a name is written with a backslash (\: \/ \\). "@name" names a
// group of addresses from the config, and "stone:*" every room on a stone.
package address

import (
	"fmt"
	"strings"
)

// Address is one parsed target.
type Address struct {
	Stone    string   // empty: the default stone
	Room     string   // a room's name or ID; "*" with AllRooms
	Path     []string // folders and the final name, below the room
	AllRooms bool     // stone:*
	Dir      bool     // the address ended in a slash: a folder, not a file
}

// IsGroup reports whether s names a group (@name), and returns the name.
func IsGroup(s string) (string, bool) {
	if strings.HasPrefix(s, "@") && len(s) > 1 {
		return s[1:], true
	}
	return "", false
}

// Parse parses one address.
func Parse(s string) (Address, error) {
	var a Address
	if s == "" {
		return a, fmt.Errorf("empty address")
	}
	if strings.HasPrefix(s, "@") {
		return a, fmt.Errorf("%q is a group, not an address", s)
	}

	// Split off the stone at the first unescaped colon.
	stone, rest, hasStone := cutUnescaped(s, ':')
	if hasStone {
		if stone == "" {
			return a, fmt.Errorf("address %q has an empty stone name", s)
		}
		a.Stone = unescape(stone)
	} else {
		rest = s
	}
	if _, _, more := cutUnescaped(rest, ':'); more {
		return a, fmt.Errorf("address %q has more than one unescaped colon; write \\: inside a name", s)
	}

	if rest == "" {
		return a, fmt.Errorf("address %q names no room", s)
	}
	parts := splitUnescaped(rest, '/')
	if strings.HasSuffix(rest, "/") && !strings.HasSuffix(rest, `\/`) {
		a.Dir = true
		parts = parts[:len(parts)-1]
	}
	for i, part := range parts {
		if part == "" {
			return a, fmt.Errorf("address %q has an empty name between slashes", s)
		}
		parts[i] = unescape(part)
	}
	a.Room = parts[0]
	if len(parts) > 1 {
		a.Path = parts[1:]
	}
	if a.Room == "*" {
		if len(a.Path) > 0 {
			return a, fmt.Errorf("address %q: nothing may follow *", s)
		}
		a.AllRooms = true
	}
	if len(a.Path) == 0 {
		a.Dir = true
	}
	return a, nil
}

// String writes the address back, escaped.
func (a Address) String() string {
	var b strings.Builder
	if a.Stone != "" {
		b.WriteString(escape(a.Stone))
		b.WriteByte(':')
	}
	b.WriteString(escape(a.Room))
	for _, p := range a.Path {
		b.WriteByte('/')
		b.WriteString(escape(p))
	}
	if a.Dir && len(a.Path) > 0 {
		b.WriteByte('/')
	}
	return b.String()
}

func cutUnescaped(s string, sep byte) (before, after string, found bool) {
	for i := 0; i < len(s); i++ {
		if s[i] == '\\' {
			i++
			continue
		}
		if s[i] == sep {
			return s[:i], s[i+1:], true
		}
	}
	return s, "", false
}

func splitUnescaped(s string, sep byte) []string {
	var parts []string
	for {
		before, after, found := cutUnescaped(s, sep)
		parts = append(parts, before)
		if !found {
			return parts
		}
		s = after
	}
}

func unescape(s string) string {
	if !strings.Contains(s, `\`) {
		return s
	}
	var b strings.Builder
	for i := 0; i < len(s); i++ {
		if s[i] == '\\' && i+1 < len(s) {
			i++
		}
		b.WriteByte(s[i])
	}
	return b.String()
}

func escape(s string) string {
	r := strings.NewReplacer(`\`, `\\`, `:`, `\:`, `/`, `\/`)
	return r.Replace(s)
}
