/**
 * What a stone tells about itself (STATUS.md), and what Rune works out of
 * it: the address of a stone, the samples which are new, the latest
 * numbers, when the storage is full, how near a limit a number is, and the
 * points of a small curve. Nothing here touches the page, the session or
 * the stone.
 */
export interface Sample { sequence: number; time: string; values: Record<string, number> }
export interface Series { interval: number; samples: Sample[] }

/** What a stone tells about itself. */
export interface Status {
  name?: string;
  version?: string;
  awake?: boolean;
  time?: string;
  started?: string;
  database?: string;
  persistent?: boolean;
  databaseMark?: string;
  load?: Series;
  day?: Series;
  inventory?: Series;
}

/** As many samples as a stone keeps. */
const KEPT = 720;

/** The address of a stone's API, from what a person typed: "stone.example", "https://stone.example/", ".../api/v1". */
export function apiAddress(typed: string): string | null {
  let address = typed.trim().replace(/\/+$/, '');
  if (!address || /^https?:$/i.test(address)) {
    return null;
  }
  if (!/^https?:\/\//i.test(address)) {
    address = `https://${address}`;
  }
  try {
    const url = new URL(address);
    if (!/\/api\/v\d+$/.test(url.pathname)) {
      url.pathname = `${url.pathname.replace(/\/+$/, '')}/api/v1`;
    }
    url.search = '';
    url.hash = '';
    return url.toString().replace(/\/+$/, '');
  } catch {
    return null;
  }
}

/** Adds the new samples of a series to those which are here, the oldest first, without doubles. */
export function merge(have: Sample[], more: Sample[] | undefined): Sample[] {
  const last = have.length > 0 ? have[have.length - 1].sequence : -Infinity;
  const fresh = (more ?? []).filter((sample) => sample.sequence > last);
  // A stone which started anew counts from the beginning: what was here is of its earlier life.
  if (more && more.length > 0 && more[more.length - 1].sequence < last) {
    return more.slice(-KEPT);
  }
  return [...have, ...fresh].slice(-KEPT);
}

/** The latest value of a name in a series; undefined if the stone doesn't tell it. */
export function latest(samples: Sample[] | undefined, name: string): number | undefined {
  for (let i = (samples?.length ?? 0) - 1; i >= 0; i--) {
    const value = (samples as Sample[])[i].values[name];
    if (typeof value === 'number') {
      return value;
    }
  }
  return undefined;
}

function instant(time: string): number {
  return Date.parse(/Z|[+-]\d\d:?\d\d$/.test(time) ? time : `${time}Z`);
}

/**
 * When the storage is full, if it goes on filling as it did over the kept
 * inventories: the time in milliseconds, or null if it isn't filling, has
 * no ceiling, or too little is known.
 */
export function fullAt(inventory: Sample[] | undefined): number | null {
  const known = (inventory ?? []).filter((sample) => typeof sample.values.storageUsed === 'number');
  if (known.length < 2) {
    return null;
  }
  const first = known[0];
  const last = known[known.length - 1];
  const ceiling = last.values.storageCeiling;
  const grown = last.values.storageUsed - first.values.storageUsed;
  const passed = instant(last.time) - instant(first.time);
  if (!ceiling || grown <= 0 || passed <= 0) {
    return null;
  }
  return instant(last.time) + ((ceiling - last.values.storageUsed) / grown) * passed;
}

/** How near a limit a number is: fine, to be watched, or near. The limits are Rune's own. */
export type Level = 'fine' | 'watch' | 'near';

export function level(value: number | undefined, watch: number, near: number): Level {
  return value === undefined || value < watch ? 'fine' : value < near ? 'watch' : 'near';
}

/** How busy a stone is, for sorting: its share of the processors, else the host's load per processor. */
export function busy(load: Sample[] | undefined): number {
  const share = latest(load, 'processShare');
  if (share !== undefined) {
    return share;
  }
  const system = latest(load, 'systemLoad');
  const processors = latest(load, 'processors') ?? 1;
  return system !== undefined && system >= 0 ? system / Math.max(1, processors) : 0;
}

/** The points of a small curve over a series of numbers, for an SVG polyline in a box of the given size. */
export function curve(values: number[], width: number, height: number, top?: number): string {
  if (values.length === 0) {
    return '';
  }
  const most = Math.max(top ?? 0, ...values, 1e-9);
  const step = values.length > 1 ? width / (values.length - 1) : 0;
  return values.map((value, index) => `${(index * step).toFixed(1)},${(height - (Math.max(0, value) / most) * height).toFixed(1)}`).join(' ');
}
