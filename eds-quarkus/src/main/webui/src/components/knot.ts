/**
 * Rune's knot: three strands plaited over and under each other, as a ring
 * around the login and as a frame around a window that floats over the panes. Rune's own drawing, made here rather than kept as
 * a file, so it takes the colours of the theme (classes, as the Content
 * Security Policy allows no inline styles).
 */

/** The side of the ring's drawing; the login's stage has the same size. */
const SIZE = 540;
/** The radius of the circle the ring's plait follows. */
const RADIUS = 238;
/** How far a strand swings to each side of the line it follows. */
const AMPLITUDE = 13;
const STRANDS = 3;
/** The length of one swing of a strand, and the distance between its points. */
const SWING = 60;
const STEP = 1.66;
/** Above these heights a strand is drawn again, over the others: first the gap around it, then the strand. */
const GAP_ABOVE = 0.55;
const STRAND_ABOVE = 0.2;
/** How far the plait reaches from the line it follows: the swing and half the gap around a strand. */
export const KNOT_REACH = AMPLITUDE + 7;

/** A point of a strand, with its height: above zero it goes over, below under. */
type Point = { x: number; y: number; z: number };

/** A point of the closed line a plait follows, with the direction outwards. */
type Along = { x: number; y: number; nx: number; ny: number };

/** One stroke of the drawing: what it is (the CSS class gives colour and width) and its path. */
export type KnotLayer = { part: 'gap' | 'strand' | 'groove'; d: string };

/** A circle, as the line to follow. */
function circle(centre: number, radius: number): Along[] {
  const count = Math.round((2 * Math.PI * radius) / STEP);
  const line: Along[] = [];
  for (let i = 0; i < count; i++) {
    const around = (2 * Math.PI * i) / count - Math.PI / 2;
    const nx = Math.cos(around);
    const ny = Math.sin(around);
    line.push({ x: centre + radius * nx, y: centre + radius * ny, nx, ny });
  }
  return line;
}

/** A rectangle with round corners, as the line to follow: clockwise from the top left. */
function roundedRectangle(left: number, top: number, width: number, height: number, corner: number): Along[] {
  const r = Math.max(1, Math.min(corner, width / 2, height / 2));
  const quarter = (Math.PI * r) / 2;
  // Each side, then the corner after it: where the side starts, its direction and the direction outwards.
  const sides = [
    { x: left + r, y: top, dx: 1, dy: 0, nx: 0, ny: -1, length: width - 2 * r },
    { x: left + width, y: top + r, dx: 0, dy: 1, nx: 1, ny: 0, length: height - 2 * r },
    { x: left + width - r, y: top + height, dx: -1, dy: 0, nx: 0, ny: 1, length: width - 2 * r },
    { x: left, y: top + height - r, dx: 0, dy: -1, nx: -1, ny: 0, length: height - 2 * r },
  ];
  const total = sides.reduce((sum, side) => sum + side.length + quarter, 0);
  const count = Math.round(total / STEP);
  const line: Along[] = [];
  for (let i = 0; i < count; i++) {
    let at = (total * i) / count;
    for (const side of sides) {
      if (at < side.length) {
        line.push({ x: side.x + side.dx * at, y: side.y + side.dy * at, nx: side.nx, ny: side.ny });
        break;
      }
      at -= side.length;
      if (at < quarter) {
        // The corner turns from the side's outwards to its direction, around its centre.
        const turn = at / r;
        const nx = side.nx * Math.cos(turn) + side.dx * Math.sin(turn);
        const ny = side.ny * Math.cos(turn) + side.dy * Math.sin(turn);
        const cx = side.x + side.dx * side.length - side.nx * r;
        const cy = side.y + side.dy * side.length - side.ny * r;
        line.push({ x: cx + nx * r, y: cy + ny * r, nx, ny });
        break;
      }
      at -= quarter;
    }
  }
  return line;
}

/**
 * The strokes of a knot along a closed line, in the order to draw them: all
 * strands whole, then for each strand the stretches where it goes over
 * another, drawn again on top. A strand is a gap in the background colour,
 * the strand on it, and a thin groove along its middle.
 */
export function knotLayers(line: Along[] = circle(SIZE / 2, RADIUS)): KnotLayer[] {
  // A whole number of swings, so the strands meet themselves again.
  const swings = Math.max(STRANDS, Math.round((line.length * STEP) / SWING));
  const strands: Point[][] = [];
  for (let k = 0; k < STRANDS; k++) {
    const phase = (2 * Math.PI * k) / STRANDS;
    strands.push(line.map((at, i) => {
      const swing = (2 * Math.PI * swings * i) / line.length + phase;
      const out = AMPLITUDE * Math.sin(swing);
      return { x: at.x + at.nx * out, y: at.y + at.ny * out, z: -Math.sin(2 * swing) };
    }));
  }
  const layers: KnotLayer[] = [];
  for (const points of strands) {
    const whole = path([...points, points[0]]);
    layers.push({ part: 'gap', d: whole }, { part: 'strand', d: whole }, { part: 'groove', d: whole });
  }
  for (const points of strands) {
    const over = runs(points, STRAND_ABOVE).map(path).join(' ');
    layers.push(
      { part: 'gap', d: runs(points, GAP_ABOVE).map(path).join(' ') },
      { part: 'strand', d: over },
      { part: 'groove', d: over });
  }
  return layers;
}

/** The stretches of a strand that are higher than the given height. */
function runs(points: Point[], height: number): Point[][] {
  const found: Point[][] = [];
  let run: Point[] = [];
  for (let i = 0; i <= points.length; i++) {
    const point = points[i % points.length];
    if (point.z > height) {
      run.push(point);
    } else {
      if (run.length > 2) {
        found.push(run);
      }
      run = [];
    }
  }
  if (run.length > 2) {
    found.push(run);
  }
  return found;
}

function path(points: Point[]): string {
  return 'M' + points.map((point) => `${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' L');
}

const SVG = 'http://www.w3.org/2000/svg';

function draw(svg: SVGSVGElement, width: number, height: number, layers: KnotLayer[]): void {
  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
  svg.replaceChildren(...layers.map((layer) => {
    const stroke = document.createElementNS(SVG, 'path');
    stroke.setAttribute('class', `knot-${layer.part}`);
    stroke.setAttribute('d', layer.d);
    return stroke;
  }));
}

function picture(className: string): SVGSVGElement {
  const svg = document.createElementNS(SVG, 'svg');
  svg.setAttribute('class', className);
  svg.setAttribute('aria-hidden', 'true');
  return svg;
}

/** The knot as a ring; a picture without a meaning of its own: decoration. */
export function knotRing(className: string): SVGSVGElement {
  const svg = picture(className);
  draw(svg, SIZE, SIZE, knotLayers());
  return svg;
}

/**
 * The knot as a frame with round corners, around something of any size:
 * `fit` draws it for a width and height, e.g. when a window changes size.
 */
export function knotFrame(className: string): { element: SVGSVGElement; fit(width: number, height: number): void } {
  const svg = picture(className);
  return {
    element: svg,
    fit(width: number, height: number): void {
      const inset = KNOT_REACH + 2;
      draw(svg, width, height, knotLayers(roundedRectangle(inset, inset, width - 2 * inset, height - 2 * inset, 30)));
    },
  };
}
