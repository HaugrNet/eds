/**
 * A QR code encoder (ISO/IEC 18004), written for Rune so the invitation QR
 * codes need no third-party library. Byte mode (UTF-8), error correction
 * level M (about 15 % of the code may be damaged), versions 1 to 40, and the
 * mask with the lowest penalty. The result is the matrix of dark modules.
 */

// Error correction level M, per version (index 0 unused): codewords per
// block and number of blocks (ISO/IEC 18004, table 9).
const ECC_PER_BLOCK = [-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26,
  26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28];
const BLOCKS = [-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16,
  17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49];
/** Format bits of level M. */
const LEVEL_BITS = 0;

export class QrCode {
  readonly size: number;
  private readonly modules: boolean[][];
  private readonly isFunction: boolean[][];

  private constructor(readonly version: number, data: number[]) {
    this.size = version * 4 + 17;
    this.modules = Array.from({ length: this.size }, () => new Array<boolean>(this.size).fill(false));
    this.isFunction = Array.from({ length: this.size }, () => new Array<boolean>(this.size).fill(false));
    this.drawFunctionPatterns();
    this.drawCodewords(this.withErrorCorrection(data));

    let best = 0;
    let lowest = Infinity;
    for (let mask = 0; mask < 8; mask++) {
      this.applyMask(mask);
      this.drawFormatBits(mask);
      const penalty = this.penalty();
      if (penalty < lowest) {
        best = mask;
        lowest = penalty;
      }
      this.applyMask(mask); // XOR again undoes it
    }
    this.applyMask(best);
    this.drawFormatBits(best);
  }

  /** Encodes the text as UTF-8 bytes in the smallest version that fits. */
  static encode(text: string): QrCode {
    const bytes = Array.from(new TextEncoder().encode(text));
    for (let version = 1; version <= 40; version++) {
      const capacity = dataCodewords(version) * 8;
      const needed = 4 + (version < 10 ? 8 : 16) + bytes.length * 8;
      if (needed <= capacity) {
        const bits: number[] = [];
        append(bits, 0b0100, 4);
        append(bits, bytes.length, version < 10 ? 8 : 16);
        for (const b of bytes) {
          append(bits, b, 8);
        }
        append(bits, 0, Math.min(4, capacity - bits.length));
        append(bits, 0, (8 - (bits.length % 8)) % 8);
        for (let pad = 0xec; bits.length < capacity; pad ^= 0xec ^ 0x11) {
          append(bits, pad, 8);
        }
        const data: number[] = [];
        for (let i = 0; i < bits.length; i += 8) {
          data.push(bits.slice(i, i + 8).reduce((value, bit) => (value << 1) | bit, 0));
        }
        return new QrCode(version, data);
      }
    }
    throw new RangeError('The text is too long for a QR code.');
  }

  /** True if the module at column x, row y is dark. */
  dark(x: number, y: number): boolean {
    return x >= 0 && y >= 0 && x < this.size && y < this.size && this.modules[y][x];
  }

  // ---------------------------------------------------------------------------
  // Function patterns
  // ---------------------------------------------------------------------------

  private drawFunctionPatterns(): void {
    for (let i = 0; i < this.size; i++) {
      this.setFunction(6, i, i % 2 === 0);
      this.setFunction(i, 6, i % 2 === 0);
    }
    this.drawFinder(3, 3);
    this.drawFinder(this.size - 4, 3);
    this.drawFinder(3, this.size - 4);

    const positions = alignmentPositions(this.version);
    const last = positions.length - 1;
    positions.forEach((x, i) => positions.forEach((y, j) => {
      // Not over the three finder patterns.
      if (!((i === 0 && j === 0) || (i === 0 && j === last) || (i === last && j === 0))) {
        this.drawAlignment(x, y);
      }
    }));

    this.drawFormatBits(0); // reserves the areas, drawn again with the mask
    this.drawVersion();
  }

  private drawFinder(cx: number, cy: number): void {
    for (let dy = -4; dy <= 4; dy++) {
      for (let dx = -4; dx <= 4; dx++) {
        const distance = Math.max(Math.abs(dx), Math.abs(dy));
        const x = cx + dx;
        const y = cy + dy;
        if (x >= 0 && y >= 0 && x < this.size && y < this.size) {
          this.setFunction(x, y, distance !== 2 && distance !== 4);
        }
      }
    }
  }

  private drawAlignment(cx: number, cy: number): void {
    for (let dy = -2; dy <= 2; dy++) {
      for (let dx = -2; dx <= 2; dx++) {
        this.setFunction(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) !== 1);
      }
    }
  }

  private drawFormatBits(mask: number): void {
    const data = (LEVEL_BITS << 3) | mask;
    let remainder = data;
    for (let i = 0; i < 10; i++) {
      remainder = (remainder << 1) ^ ((remainder >>> 9) * 0x537);
    }
    const bits = ((data << 10) | remainder) ^ 0x5412;
    const bit = (i: number) => ((bits >>> i) & 1) !== 0;

    // Around the top left finder.
    for (let i = 0; i <= 5; i++) {
      this.setFunction(8, i, bit(i));
    }
    this.setFunction(8, 7, bit(6));
    this.setFunction(8, 8, bit(7));
    this.setFunction(7, 8, bit(8));
    for (let i = 9; i < 15; i++) {
      this.setFunction(14 - i, 8, bit(i));
    }
    // The copy at the other two finders, and the dark module.
    for (let i = 0; i < 8; i++) {
      this.setFunction(this.size - 1 - i, 8, bit(i));
    }
    for (let i = 8; i < 15; i++) {
      this.setFunction(8, this.size - 15 + i, bit(i));
    }
    this.setFunction(8, this.size - 8, true);
  }

  private drawVersion(): void {
    if (this.version < 7) {
      return;
    }
    let remainder = this.version;
    for (let i = 0; i < 12; i++) {
      remainder = (remainder << 1) ^ ((remainder >>> 11) * 0x1f25);
    }
    const bits = (this.version << 12) | remainder;
    for (let i = 0; i < 18; i++) {
      const dark = ((bits >>> i) & 1) !== 0;
      const a = this.size - 11 + (i % 3);
      const b = Math.floor(i / 3);
      this.setFunction(a, b, dark);
      this.setFunction(b, a, dark);
    }
  }

  private setFunction(x: number, y: number, dark: boolean): void {
    this.modules[y][x] = dark;
    this.isFunction[y][x] = true;
  }

  // ---------------------------------------------------------------------------
  // Data
  // ---------------------------------------------------------------------------

  /** Splits the data into blocks, adds Reed-Solomon codewords, interleaves. */
  private withErrorCorrection(data: number[]): number[] {
    const blocks = BLOCKS[this.version];
    const eccLength = ECC_PER_BLOCK[this.version];
    const raw = Math.floor(rawDataModules(this.version) / 8);
    const shortBlocks = blocks - (raw % blocks);
    const shortLength = Math.floor(raw / blocks);
    const divisor = rsDivisor(eccLength);

    const dataBlocks: number[][] = [];
    const eccBlocks: number[][] = [];
    for (let i = 0, offset = 0; i < blocks; i++) {
      const length = shortLength - eccLength + (i < shortBlocks ? 0 : 1);
      const block = data.slice(offset, offset + length);
      offset += length;
      dataBlocks.push(block);
      eccBlocks.push(rsRemainder(block, divisor));
    }

    const result: number[] = [];
    for (let i = 0; i <= shortLength - eccLength; i++) {
      for (const block of dataBlocks) {
        if (i < block.length) {
          result.push(block[i]);
        }
      }
    }
    for (let i = 0; i < eccLength; i++) {
      for (const block of eccBlocks) {
        result.push(block[i]);
      }
    }
    return result;
  }

  /** Places the codewords in the zigzag order, two columns at a time. */
  private drawCodewords(codewords: number[]): void {
    let i = 0;
    for (let right = this.size - 1; right >= 1; right -= 2) {
      if (right === 6) {
        right = 5; // skips the vertical timing pattern
      }
      for (let vertical = 0; vertical < this.size; vertical++) {
        for (let j = 0; j < 2; j++) {
          const x = right - j;
          const upward = ((right + 1) & 2) === 0;
          const y = upward ? this.size - 1 - vertical : vertical;
          if (!this.isFunction[y][x] && i < codewords.length * 8) {
            this.modules[y][x] = ((codewords[i >>> 3] >>> (7 - (i & 7))) & 1) !== 0;
            i++;
          }
        }
      }
    }
  }

  private applyMask(mask: number): void {
    for (let y = 0; y < this.size; y++) {
      for (let x = 0; x < this.size; x++) {
        if (!this.isFunction[y][x] && maskBit(mask, x, y)) {
          this.modules[y][x] = !this.modules[y][x];
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Penalty (ISO/IEC 18004, 7.8.3), to choose the mask
  // ---------------------------------------------------------------------------

  private penalty(): number {
    let result = 0;
    const size = this.size;
    const at = (x: number, y: number, horizontal: boolean) => (horizontal ? this.modules[y][x] : this.modules[x][y]);

    for (const horizontal of [true, false]) {
      for (let line = 0; line < size; line++) {
        // Rule 1: runs of five or more modules of one colour.
        let run = 1;
        for (let i = 1; i <= size; i++) {
          if (i < size && at(i, line, horizontal) === at(i - 1, line, horizontal)) {
            run++;
          } else {
            if (run >= 5) {
              result += 3 + (run - 5);
            }
            run = 1;
          }
        }
        // Rule 3: 1:1:3:1:1 finder-like patterns with four light modules on
        // one side; outside the symbol counts as light.
        for (let i = -4; i < size; i++) {
          const dark = (k: number) => k >= 0 && k < size && at(k, line, horizontal);
          const core = dark(i) && !dark(i + 1) && dark(i + 2) && dark(i + 3) && dark(i + 4) && !dark(i + 5) && dark(i + 6);
          if (core) {
            const lightBefore = [1, 2, 3, 4].every((k) => !dark(i - k));
            const lightAfter = [7, 8, 9, 10].every((k) => !dark(i + k));
            if (lightBefore || lightAfter) {
              result += 40;
            }
          }
        }
      }
    }

    // Rule 2: 2x2 blocks of one colour.
    for (let y = 0; y < size - 1; y++) {
      for (let x = 0; x < size - 1; x++) {
        const c = this.modules[y][x];
        if (c === this.modules[y][x + 1] && c === this.modules[y + 1][x] && c === this.modules[y + 1][x + 1]) {
          result += 3;
        }
      }
    }

    // Rule 4: balance of dark and light modules.
    let dark = 0;
    for (const row of this.modules) {
      for (const module of row) {
        dark += module ? 1 : 0;
      }
    }
    const total = size * size;
    const k = Math.ceil(Math.abs(dark * 20 - total * 10) / total) - 1;
    return result + Math.max(0, k) * 10;
  }
}

function append(bits: number[], value: number, length: number): void {
  for (let i = length - 1; i >= 0; i--) {
    bits.push((value >>> i) & 1);
  }
}

function maskBit(mask: number, x: number, y: number): boolean {
  switch (mask) {
    case 0: return (x + y) % 2 === 0;
    case 1: return y % 2 === 0;
    case 2: return x % 3 === 0;
    case 3: return (x + y) % 3 === 0;
    case 4: return (Math.floor(x / 3) + Math.floor(y / 2)) % 2 === 0;
    case 5: return ((x * y) % 2) + ((x * y) % 3) === 0;
    case 6: return (((x * y) % 2) + ((x * y) % 3)) % 2 === 0;
    default: return (((x + y) % 2) + ((x * y) % 3)) % 2 === 0;
  }
}

/** Centre positions of the alignment patterns, rows and columns alike. */
function alignmentPositions(version: number): number[] {
  if (version === 1) {
    return [];
  }
  const count = Math.floor(version / 7) + 2;
  const size = version * 4 + 17;
  const step = version === 32 ? 26 : Math.ceil((version * 4 + 4) / (count * 2 - 2)) * 2;
  const result = [6];
  for (let position = size - 7; result.length < count; position -= step) {
    result.splice(1, 0, position);
  }
  return result;
}

/** Modules available for data and error correction codewords. */
function rawDataModules(version: number): number {
  let result = (16 * version + 128) * version + 64;
  if (version >= 2) {
    const alignments = Math.floor(version / 7) + 2;
    result -= (25 * alignments - 10) * alignments - 55;
    if (version >= 7) {
      result -= 36;
    }
  }
  return result;
}

function dataCodewords(version: number): number {
  return Math.floor(rawDataModules(version) / 8) - ECC_PER_BLOCK[version] * BLOCKS[version];
}

// Reed-Solomon over GF(2^8) with the polynomial x^8 + x^4 + x^3 + x^2 + 1.
function gfMultiply(a: number, b: number): number {
  let result = 0;
  for (let i = 7; i >= 0; i--) {
    result = (result << 1) ^ ((result >>> 7) * 0x11d);
    result ^= ((b >>> i) & 1) * a;
  }
  return result & 0xff;
}

function rsDivisor(degree: number): number[] {
  const result = new Array<number>(degree).fill(0);
  result[degree - 1] = 1;
  let root = 1;
  for (let i = 0; i < degree; i++) {
    for (let j = 0; j < degree; j++) {
      result[j] = gfMultiply(result[j], root);
      if (j + 1 < degree) {
        result[j] ^= result[j + 1];
      }
    }
    root = gfMultiply(root, 0x02);
  }
  return result;
}

function rsRemainder(data: number[], divisor: number[]): number[] {
  const result = new Array<number>(divisor.length).fill(0);
  for (const b of data) {
    const factor = b ^ (result.shift() as number);
    result.push(0);
    divisor.forEach((coefficient, i) => {
      result[i] ^= gfMultiply(coefficient, factor);
    });
  }
  return result;
}

/**
 * The QR code as an SVG element: black on white with the quiet zone of four
 * modules, whatever the theme, so every camera reads it.
 */
export function qrSvg(text: string, label: string): SVGSVGElement {
  const qr = QrCode.encode(text);
  const quiet = 4;
  const size = qr.size + quiet * 2;
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', `0 0 ${size} ${size}`);
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', label);
  svg.setAttribute('shape-rendering', 'crispEdges');
  const background = document.createElementNS(ns, 'rect');
  background.setAttribute('width', String(size));
  background.setAttribute('height', String(size));
  background.setAttribute('fill', '#ffffff');
  let path = '';
  for (let y = 0; y < qr.size; y++) {
    for (let x = 0; x < qr.size; x++) {
      if (qr.dark(x, y)) {
        // One rectangle per run of dark modules in the row.
        let run = 1;
        while (qr.dark(x + run, y)) {
          run++;
        }
        path += `M${x + quiet} ${y + quiet}h${run}v1h-${run}z`;
        x += run - 1;
      }
    }
  }
  const modules = document.createElementNS(ns, 'path');
  modules.setAttribute('d', path);
  modules.setAttribute('fill', '#000000');
  svg.append(background, modules);
  return svg;
}
