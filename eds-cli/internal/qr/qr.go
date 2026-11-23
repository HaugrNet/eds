// Package qr is a QR code encoder (ISO/IEC 18004), a port of Rune's qr.ts,
// so the invitation QR codes need no third-party library. Byte mode (UTF-8),
// error correction level M (about 15 % of the code may be damaged), versions
// 1 to 40, and the mask with the lowest penalty. A test checks that it gives
// the very same codes as Rune's encoder, and decodes all 40 versions.
package qr

import (
	"errors"
	"strings"
)

// Error correction level M, per version (index 0 unused): codewords per
// block and number of blocks (ISO/IEC 18004, table 9).
var eccPerBlock = []int{-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26,
	26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28}
var blocks = []int{-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16,
	17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49}

// levelBits are the format bits of level M.
const levelBits = 0

// ErrTooLong is returned for a text that doesn't fit the largest code.
var ErrTooLong = errors.New("the text is too long for a QR code")

// Code is a QR code: a square of dark and light modules.
type Code struct {
	Version    int
	Size       int
	modules    [][]bool
	isFunction [][]bool
}

// Encode encodes the text as UTF-8 bytes in the smallest version that fits.
func Encode(text string) (*Code, error) {
	bytes := []byte(text)
	for version := 1; version <= 40; version++ {
		capacity := dataCodewords(version) * 8
		countBits := 16
		if version < 10 {
			countBits = 8
		}
		if 4+countBits+len(bytes)*8 > capacity {
			continue
		}
		var bits []byte
		bits = appendBits(bits, 0b0100, 4)
		bits = appendBits(bits, len(bytes), countBits)
		for _, b := range bytes {
			bits = appendBits(bits, int(b), 8)
		}
		bits = appendBits(bits, 0, min(4, capacity-len(bits)))
		bits = appendBits(bits, 0, (8-len(bits)%8)%8)
		for pad := 0xec; len(bits) < capacity; pad ^= 0xec ^ 0x11 {
			bits = appendBits(bits, pad, 8)
		}
		data := make([]byte, 0, len(bits)/8)
		for i := 0; i < len(bits); i += 8 {
			value := 0
			for _, bit := range bits[i : i+8] {
				value = value<<1 | int(bit)
			}
			data = append(data, byte(value))
		}
		return newCode(version, data), nil
	}
	return nil, ErrTooLong
}

func newCode(version int, data []byte) *Code {
	c := &Code{Version: version, Size: version*4 + 17}
	c.modules = make([][]bool, c.Size)
	c.isFunction = make([][]bool, c.Size)
	for i := range c.modules {
		c.modules[i] = make([]bool, c.Size)
		c.isFunction[i] = make([]bool, c.Size)
	}
	c.drawFunctionPatterns()
	c.drawCodewords(c.withErrorCorrection(data))

	best, lowest := 0, -1
	for mask := 0; mask < 8; mask++ {
		c.applyMask(mask)
		c.drawFormatBits(mask)
		if penalty := c.penalty(); lowest < 0 || penalty < lowest {
			best, lowest = mask, penalty
		}
		c.applyMask(mask) // XOR again undoes it
	}
	c.applyMask(best)
	c.drawFormatBits(best)
	return c
}

// Dark reports whether the module at column x, row y is dark.
func (c *Code) Dark(x, y int) bool {
	return x >= 0 && y >= 0 && x < c.Size && y < c.Size && c.modules[y][x]
}

// Text draws the code for a terminal in Unicode half blocks, two rows of
// modules per line, dark on light with the quiet zone of four modules,
// whatever the terminal's colours, so every camera reads it.
func (c *Code) Text() string {
	const quiet = 4
	var b strings.Builder
	for y := -quiet; y < c.Size+quiet; y += 2 {
		// Black on white, set explicitly: a terminal's own colours may be
		// light on dark, which cameras read badly.
		b.WriteString("\x1b[30;107m")
		for x := -quiet; x < c.Size+quiet; x++ {
			top, bottom := c.Dark(x, y), c.Dark(x, y+1)
			switch {
			case top && bottom:
				b.WriteRune('█')
			case top:
				b.WriteRune('▀')
			case bottom:
				b.WriteRune('▄')
			default:
				b.WriteRune(' ')
			}
		}
		b.WriteString("\x1b[0m\n")
	}
	return b.String()
}

// ---------------------------------------------------------------------------
// Function patterns
// ---------------------------------------------------------------------------

func (c *Code) drawFunctionPatterns() {
	for i := 0; i < c.Size; i++ {
		c.setFunction(6, i, i%2 == 0)
		c.setFunction(i, 6, i%2 == 0)
	}
	c.drawFinder(3, 3)
	c.drawFinder(c.Size-4, 3)
	c.drawFinder(3, c.Size-4)

	positions := alignmentPositions(c.Version)
	last := len(positions) - 1
	for i, x := range positions {
		for j, y := range positions {
			// Not over the three finder patterns.
			if !((i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0)) {
				c.drawAlignment(x, y)
			}
		}
	}

	c.drawFormatBits(0) // reserves the areas, drawn again with the mask
	c.drawVersion()
}

func (c *Code) drawFinder(cx, cy int) {
	for dy := -4; dy <= 4; dy++ {
		for dx := -4; dx <= 4; dx++ {
			distance := max(abs(dx), abs(dy))
			x, y := cx+dx, cy+dy
			if x >= 0 && y >= 0 && x < c.Size && y < c.Size {
				c.setFunction(x, y, distance != 2 && distance != 4)
			}
		}
	}
}

func (c *Code) drawAlignment(cx, cy int) {
	for dy := -2; dy <= 2; dy++ {
		for dx := -2; dx <= 2; dx++ {
			c.setFunction(cx+dx, cy+dy, max(abs(dx), abs(dy)) != 1)
		}
	}
}

func (c *Code) drawFormatBits(mask int) {
	data := levelBits<<3 | mask
	remainder := data
	for i := 0; i < 10; i++ {
		remainder = (remainder << 1) ^ ((remainder >> 9) * 0x537)
	}
	bits := (data<<10 | remainder) ^ 0x5412
	bit := func(i int) bool { return (bits>>i)&1 != 0 }

	// Around the top left finder.
	for i := 0; i <= 5; i++ {
		c.setFunction(8, i, bit(i))
	}
	c.setFunction(8, 7, bit(6))
	c.setFunction(8, 8, bit(7))
	c.setFunction(7, 8, bit(8))
	for i := 9; i < 15; i++ {
		c.setFunction(14-i, 8, bit(i))
	}
	// The copy at the other two finders, and the dark module.
	for i := 0; i < 8; i++ {
		c.setFunction(c.Size-1-i, 8, bit(i))
	}
	for i := 8; i < 15; i++ {
		c.setFunction(8, c.Size-15+i, bit(i))
	}
	c.setFunction(8, c.Size-8, true)
}

func (c *Code) drawVersion() {
	if c.Version < 7 {
		return
	}
	remainder := c.Version
	for i := 0; i < 12; i++ {
		remainder = (remainder << 1) ^ ((remainder >> 11) * 0x1f25)
	}
	bits := c.Version<<12 | remainder
	for i := 0; i < 18; i++ {
		dark := (bits>>i)&1 != 0
		a := c.Size - 11 + i%3
		b := i / 3
		c.setFunction(a, b, dark)
		c.setFunction(b, a, dark)
	}
}

func (c *Code) setFunction(x, y int, dark bool) {
	c.modules[y][x] = dark
	c.isFunction[y][x] = true
}

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------

// withErrorCorrection splits the data into blocks, adds Reed-Solomon
// codewords, and interleaves.
func (c *Code) withErrorCorrection(data []byte) []byte {
	count := blocks[c.Version]
	eccLength := eccPerBlock[c.Version]
	raw := rawDataModules(c.Version) / 8
	shortBlocks := count - raw%count
	shortLength := raw / count
	divisor := rsDivisor(eccLength)

	var dataBlocks, eccBlocks [][]byte
	offset := 0
	for i := 0; i < count; i++ {
		length := shortLength - eccLength
		if i >= shortBlocks {
			length++
		}
		block := data[offset : offset+length]
		offset += length
		dataBlocks = append(dataBlocks, block)
		eccBlocks = append(eccBlocks, rsRemainder(block, divisor))
	}

	var result []byte
	for i := 0; i <= shortLength-eccLength; i++ {
		for _, block := range dataBlocks {
			if i < len(block) {
				result = append(result, block[i])
			}
		}
	}
	for i := 0; i < eccLength; i++ {
		for _, block := range eccBlocks {
			result = append(result, block[i])
		}
	}
	return result
}

// drawCodewords places the codewords in the zigzag order, two columns at a
// time.
func (c *Code) drawCodewords(codewords []byte) {
	i := 0
	for right := c.Size - 1; right >= 1; right -= 2 {
		if right == 6 {
			right = 5 // skips the vertical timing pattern
		}
		for vertical := 0; vertical < c.Size; vertical++ {
			for j := 0; j < 2; j++ {
				x := right - j
				upward := (right+1)&2 == 0
				y := vertical
				if upward {
					y = c.Size - 1 - vertical
				}
				if !c.isFunction[y][x] && i < len(codewords)*8 {
					c.modules[y][x] = (codewords[i>>3]>>(7-(i&7)))&1 != 0
					i++
				}
			}
		}
	}
}

func (c *Code) applyMask(mask int) {
	for y := 0; y < c.Size; y++ {
		for x := 0; x < c.Size; x++ {
			if !c.isFunction[y][x] && maskBit(mask, x, y) {
				c.modules[y][x] = !c.modules[y][x]
			}
		}
	}
}

// ---------------------------------------------------------------------------
// Penalty (ISO/IEC 18004, 7.8.3), to choose the mask
// ---------------------------------------------------------------------------

func (c *Code) penalty() int {
	result := 0
	size := c.Size
	at := func(x, y int, horizontal bool) bool {
		if horizontal {
			return c.modules[y][x]
		}
		return c.modules[x][y]
	}

	for _, horizontal := range []bool{true, false} {
		for line := 0; line < size; line++ {
			// Rule 1: runs of five or more modules of one colour.
			run := 1
			for i := 1; i <= size; i++ {
				if i < size && at(i, line, horizontal) == at(i-1, line, horizontal) {
					run++
				} else {
					if run >= 5 {
						result += 3 + (run - 5)
					}
					run = 1
				}
			}
			// Rule 3: 1:1:3:1:1 finder-like patterns with four light modules
			// on one side; outside the symbol counts as light.
			dark := func(k int) bool { return k >= 0 && k < size && at(k, line, horizontal) }
			for i := -4; i < size; i++ {
				core := dark(i) && !dark(i+1) && dark(i+2) && dark(i+3) && dark(i+4) && !dark(i+5) && dark(i+6)
				if core {
					lightBefore := !dark(i-1) && !dark(i-2) && !dark(i-3) && !dark(i-4)
					lightAfter := !dark(i+7) && !dark(i+8) && !dark(i+9) && !dark(i+10)
					if lightBefore || lightAfter {
						result += 40
					}
				}
			}
		}
	}

	// Rule 2: 2x2 blocks of one colour.
	for y := 0; y < size-1; y++ {
		for x := 0; x < size-1; x++ {
			m := c.modules[y][x]
			if m == c.modules[y][x+1] && m == c.modules[y+1][x] && m == c.modules[y+1][x+1] {
				result += 3
			}
		}
	}

	// Rule 4: balance of dark and light modules.
	dark := 0
	for _, row := range c.modules {
		for _, module := range row {
			if module {
				dark++
			}
		}
	}
	total := size * size
	// ceil(|dark*20 - total*10| / total) - 1
	difference := abs(dark*20 - total*10)
	k := (difference+total-1)/total - 1
	return result + max(0, k)*10
}

func appendBits(bits []byte, value, length int) []byte {
	for i := length - 1; i >= 0; i-- {
		bits = append(bits, byte((value>>i)&1))
	}
	return bits
}

func maskBit(mask, x, y int) bool {
	switch mask {
	case 0:
		return (x+y)%2 == 0
	case 1:
		return y%2 == 0
	case 2:
		return x%3 == 0
	case 3:
		return (x+y)%3 == 0
	case 4:
		return (x/3+y/2)%2 == 0
	case 5:
		return (x*y)%2+(x*y)%3 == 0
	case 6:
		return ((x*y)%2+(x*y)%3)%2 == 0
	default:
		return ((x+y)%2+(x*y)%3)%2 == 0
	}
}

// alignmentPositions are the centre positions of the alignment patterns,
// rows and columns alike.
func alignmentPositions(version int) []int {
	if version == 1 {
		return nil
	}
	count := version/7 + 2
	size := version*4 + 17
	step := 26
	if version != 32 {
		// ceil((version*4 + 4) / (count*2 - 2)) * 2
		step = ((version*4 + 4) + (count*2 - 2) - 1) / (count*2 - 2) * 2
	}
	result := make([]int, count)
	result[0] = 6
	for i, position := count-1, size-7; i >= 1; i, position = i-1, position-step {
		result[i] = position
	}
	return result
}

// rawDataModules are the modules available for data and error correction
// codewords.
func rawDataModules(version int) int {
	result := (16*version+128)*version + 64
	if version >= 2 {
		alignments := version/7 + 2
		result -= (25*alignments-10)*alignments - 55
		if version >= 7 {
			result -= 36
		}
	}
	return result
}

func dataCodewords(version int) int {
	return rawDataModules(version)/8 - eccPerBlock[version]*blocks[version]
}

// Reed-Solomon over GF(2^8) with the polynomial x^8 + x^4 + x^3 + x^2 + 1.
func gfMultiply(a, b int) int {
	result := 0
	for i := 7; i >= 0; i-- {
		result = (result << 1) ^ ((result >> 7) * 0x11d)
		result ^= ((b >> i) & 1) * a
	}
	return result & 0xff
}

func rsDivisor(degree int) []byte {
	result := make([]byte, degree)
	result[degree-1] = 1
	root := 1
	for i := 0; i < degree; i++ {
		for j := 0; j < degree; j++ {
			result[j] = byte(gfMultiply(int(result[j]), root))
			if j+1 < degree {
				result[j] ^= result[j+1]
			}
		}
		root = gfMultiply(root, 0x02)
	}
	return result
}

func rsRemainder(data, divisor []byte) []byte {
	result := make([]byte, len(divisor))
	for _, b := range data {
		factor := int(b ^ result[0])
		copy(result, result[1:])
		result[len(result)-1] = 0
		for i, coefficient := range divisor {
			result[i] ^= byte(gfMultiply(int(coefficient), factor))
		}
	}
	return result
}

func abs(v int) int {
	if v < 0 {
		return -v
	}
	return v
}
