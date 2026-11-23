package qr

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"os"
	"strings"
	"testing"
)

// text makes the test text of a length, as the generator of testdata did.
func text(length int) string {
	var b strings.Builder
	for i := 0; i < length; i++ {
		b.WriteByte(byte(33 + (i*7+length)%90))
	}
	return b.String()
}

func matrix(c *Code) string {
	var b strings.Builder
	for y := 0; y < c.Size; y++ {
		for x := 0; x < c.Size; x++ {
			if c.Dark(x, y) {
				b.WriteByte('1')
			} else {
				b.WriteByte('0')
			}
		}
		b.WriteByte('\n')
	}
	return b.String()
}

// testdata/rune.json has the SHA-256 of the codes Rune's encoder (qr.ts)
// makes, one text per version 1 to 40 and a few more: Tolk's must be the
// very same, module for module.
func TestSameCodesAsRune(t *testing.T) {
	data, err := os.ReadFile("testdata/rune.json")
	if err != nil {
		t.Fatal(err)
	}
	var cases []struct {
		Length  int     `json:"length"`
		Text    *string `json:"text"`
		Version int     `json:"version"`
		SHA256  string  `json:"sha256"`
	}
	if err := json.Unmarshal(data, &cases); err != nil {
		t.Fatal(err)
	}
	versions := map[int]bool{}
	for _, c := range cases {
		input := text(c.Length)
		if c.Text != nil {
			input = *c.Text
		}
		code, err := Encode(input)
		if err != nil {
			t.Fatalf("%q: %v", input, err)
		}
		versions[code.Version] = true
		sum := sha256.Sum256([]byte(matrix(code)))
		if code.Version != c.Version || hex.EncodeToString(sum[:]) != c.SHA256 {
			t.Errorf("version %d (%d bytes): Tolk's code differs from Rune's (Tolk made version %d)", c.Version, len(input), code.Version)
		}
	}
	if len(versions) != 40 {
		t.Errorf("%d versions compared, want all 40", len(versions))
	}
}

// decode reads a code back, as a scanner would: the format bits give the
// mask, the modules are unmasked and read in the zigzag order, the blocks
// are taken apart, each block's error correction is checked, and the bytes
// are read from the data.
func decode(t *testing.T, c *Code) string {
	t.Helper()
	version := (c.Size - 17) / 4

	// Format bits, around the top left finder.
	bits := 0
	set := func(i int, dark bool) {
		if dark {
			bits |= 1 << i
		}
	}
	for i := 0; i <= 5; i++ {
		set(i, c.Dark(8, i))
	}
	set(6, c.Dark(8, 7))
	set(7, c.Dark(8, 8))
	set(8, c.Dark(7, 8))
	for i := 9; i < 15; i++ {
		set(i, c.Dark(14-i, 8))
	}
	bits ^= 0x5412
	format := bits >> 10
	remainder := format
	for i := 0; i < 10; i++ {
		remainder = (remainder << 1) ^ ((remainder >> 9) * 0x537)
	}
	if remainder != bits&0x3ff {
		t.Fatalf("version %d: the format bits don't check", version)
	}
	if format>>3 != levelBits {
		t.Fatalf("version %d: level bits %d", version, format>>3)
	}
	mask := format & 7

	// Which modules carry data: all but the function patterns.
	blank := &Code{Version: version, Size: c.Size}
	blank.modules = make([][]bool, c.Size)
	blank.isFunction = make([][]bool, c.Size)
	for i := range blank.modules {
		blank.modules[i] = make([]bool, c.Size)
		blank.isFunction[i] = make([]bool, c.Size)
	}
	blank.drawFunctionPatterns()

	var stream []byte
	for right := c.Size - 1; right >= 1; right -= 2 {
		if right == 6 {
			right = 5
		}
		for vertical := 0; vertical < c.Size; vertical++ {
			for j := 0; j < 2; j++ {
				x := right - j
				y := vertical
				if (right+1)&2 == 0 {
					y = c.Size - 1 - vertical
				}
				if !blank.isFunction[y][x] {
					bit := c.Dark(x, y) != maskBit(mask, x, y)
					if bit {
						stream = append(stream, 1)
					} else {
						stream = append(stream, 0)
					}
				}
			}
		}
	}
	raw := rawDataModules(version) / 8
	codewords := make([]byte, raw)
	for i := range codewords {
		for _, bit := range stream[i*8 : i*8+8] {
			codewords[i] = codewords[i]<<1 | bit
		}
	}

	// Take the interleaved blocks apart.
	count, eccLength := blocks[version], eccPerBlock[version]
	shortBlocks, shortLength := count-raw%count, raw/count
	dataBlocks := make([][]byte, count)
	eccBlocks := make([][]byte, count)
	next := 0
	for i := 0; i <= shortLength-eccLength; i++ {
		for b := 0; b < count; b++ {
			length := shortLength - eccLength
			if b >= shortBlocks {
				length++
			}
			if i < length {
				dataBlocks[b] = append(dataBlocks[b], codewords[next])
				next++
			}
		}
	}
	for i := 0; i < eccLength; i++ {
		for b := 0; b < count; b++ {
			eccBlocks[b] = append(eccBlocks[b], codewords[next])
			next++
		}
	}
	divisor := rsDivisor(eccLength)
	var data []byte
	for b := range dataBlocks {
		if string(rsRemainder(dataBlocks[b], divisor)) != string(eccBlocks[b]) {
			t.Fatalf("version %d: block %d fails its error correction", version, b)
		}
		data = append(data, dataBlocks[b]...)
	}

	// Byte mode: 0100, the count, the bytes.
	position := 0
	read := func(n int) int {
		value := 0
		for i := 0; i < n; i++ {
			value = value<<1 | int(data[position/8]>>(7-position%8))&1
			position++
		}
		return value
	}
	if mode := read(4); mode != 0b0100 {
		t.Fatalf("version %d: mode %04b", version, mode)
	}
	countBits := 16
	if version < 10 {
		countBits = 8
	}
	length := read(countBits)
	out := make([]byte, length)
	for i := range out {
		out[i] = byte(read(8))
	}
	return string(out)
}

// Every version is decoded again, as a scanner would.
func TestDecodeAllVersions(t *testing.T) {
	// The shortest and the longest text of every version.
	longest := func(version int) int {
		countBits := 16
		if version < 10 {
			countBits = 8
		}
		return (dataCodewords(version)*8 - 4 - countBits) / 8
	}
	for version := 1; version <= 40; version++ {
		shortest := 0
		if version > 1 {
			shortest = longest(version-1) + 1
		}
		for _, length := range []int{shortest, longest(version)} {
			code := mustEncode(t, text(length))
			if code.Version != version {
				t.Errorf("%d bytes: version %d, want %d", length, code.Version, version)
			}
			if got := decode(t, code); got != text(length) {
				t.Errorf("version %d, %d bytes: decoded %q", code.Version, length, got)
			}
		}
	}
	if got := decode(t, mustEncode(t, "Grüße, Jörmungandr ᚱᚢᚾ")); got != "Grüße, Jörmungandr ᚱᚢᚾ" {
		t.Errorf("UTF-8: %q", got)
	}
}

func mustEncode(t *testing.T, text string) *Code {
	t.Helper()
	code, err := Encode(text)
	if err != nil {
		t.Fatal(err)
	}
	return code
}

func TestTooLong(t *testing.T) {
	if _, err := Encode(strings.Repeat("x", 2332)); err != ErrTooLong {
		t.Errorf("2332 bytes: %v", err)
	}
	if code, err := Encode(strings.Repeat("x", 2331)); err != nil || code.Version != 40 {
		t.Errorf("2331 bytes: %v", err)
	}
}

func TestText(t *testing.T) {
	code := mustEncode(t, "tolk")
	lines := strings.Split(strings.TrimRight(code.Text(), "\n"), "\n")
	// 21 modules and a quiet zone of 4 on each side, two rows per line.
	if len(lines) != (21+8+1)/2 {
		t.Errorf("%d lines", len(lines))
	}
	for _, line := range lines {
		plain := strings.TrimSuffix(strings.TrimPrefix(line, "\x1b[30;107m"), "\x1b[0m")
		if n := len([]rune(plain)); n != 21+8 {
			t.Errorf("line has %d modules: %q", n, plain)
		}
	}
}
