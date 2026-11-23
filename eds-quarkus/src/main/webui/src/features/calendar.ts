/**
 * The feature "calendar" (FEATURES.md, "Calendar"): the events of a room. An
 * entry holds one event of iCalendar (RFC 5545), so events can be brought
 * in and taken out as `.ics`. Here: the event as Rune works with it, its
 * VEVENT, the name of its entry, and when it takes place, also when it
 * repeats. Nothing here touches the page or the stone.
 *
 * First version: single events, and events repeating daily, weekly, monthly
 * or yearly, each n-th time, until a day or a number of times. A rule Rune
 * doesn't know (such as "every second Tuesday of the month") is kept as it
 * is and the event shown once, with a note.
 */
export interface EventBody { v: 1; ics: string }

export type Every = 'daily' | 'weekly' | 'monthly' | 'yearly';

export interface Repeat {
  every: Every;
  /** Each n-th time; 1 for every. */
  interval: number;
  /** The last day it may start on, "YYYY-MM-DD". */
  until?: string;
  /** How many times it takes place in all. */
  count?: number;
}

export interface CalendarEvent {
  uid: string;
  title: string;
  allDay: boolean;
  /** When it starts, as the clock on the wall says there: "YYYY-MM-DD" for a whole day, else "YYYY-MM-DDTHH:MM". */
  start: string;
  /** When it ends, in the same form; of whole days the day after the last, as iCalendar has it. */
  end: string;
  /** The time zone of start and end (IANA, such as "Europe/Berlin"); none for whole days. */
  zone?: string;
  place?: string;
  note?: string;
  repeat?: Repeat;
  /** A rule of repeating which Rune doesn't know, kept as it came. */
  rule?: string;
  /** The event is a call of the room, with the voice or with video too (FEATURES.md, "Presence and calls"). */
  call?: 'audio' | 'video';
  /** The lines of the event Rune doesn't know, kept so they survive a change. */
  other: string[];
}

const FREQUENCIES: Record<string, Every> = { DAILY: 'daily', WEEKLY: 'weekly', MONTHLY: 'monthly', YEARLY: 'yearly' };

/** The time zone of this device. */
export function deviceZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
}

function validZone(zone: string): boolean {
  try {
    new Intl.DateTimeFormat('en', { timeZone: zone });
    return true;
  } catch {
    return false;
  }
}

// --- Days and wall times -----------------------------------------------------------

const pad = (value: number, length = 2) => String(value).padStart(length, '0');

/** "YYYY-MM-DD" of a year, a month (1 to 12) and a day; null if there is no such day. */
export function day(year: number, month: number, date: number): string | null {
  const made = new Date(Date.UTC(year, month - 1, date));
  return made.getUTCFullYear() === year && made.getUTCMonth() === month - 1 && made.getUTCDate() === date
    ? `${pad(year, 4)}-${pad(month)}-${pad(date)}` : null;
}

function numbers(wall: string): [number, number, number, number, number] {
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2}))?/.exec(wall);
  if (!match) {
    throw new Error(`not a time: ${wall}`);
  }
  return [Number(match[1]), Number(match[2]), Number(match[3]), Number(match[4] ?? 0), Number(match[5] ?? 0)];
}

/** The day a number of days later (or earlier). */
export function addDays(wall: string, days: number): string {
  const [y, m, d] = numbers(wall);
  const moved = new Date(Date.UTC(y, m - 1, d + days));
  return `${pad(moved.getUTCFullYear(), 4)}-${pad(moved.getUTCMonth() + 1)}-${pad(moved.getUTCDate())}${wall.slice(10)}`;
}

function daysBetween(from: string, to: string): number {
  const [y1, m1, d1] = numbers(from);
  const [y2, m2, d2] = numbers(to);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

/** How far the clocks of a zone are ahead of UTC at an instant, in milliseconds. */
function offset(zone: string, instant: number): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: zone, hourCycle: 'h23', year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric',
  }).formatToParts(new Date(instant));
  const of = (type: string) => Number(parts.find((part) => part.type === type)?.value ?? 0);
  return Date.UTC(of('year'), of('month') - 1, of('day'), of('hour'), of('minute'), of('second')) - Math.floor(instant / 1000) * 1000;
}

/**
 * The instant of a time on the wall in a zone. A whole day starts at
 * midnight of this device, as a day is the same day for everyone.
 */
export function instant(wall: string, zone?: string): Date {
  const [y, m, d, h, min] = numbers(wall);
  if (!zone || wall.length === 10) {
    return new Date(y, m - 1, d, h, min);
  }
  const guess = Date.UTC(y, m - 1, d, h, min);
  const first = offset(zone, guess);
  const second = offset(zone, guess - first);
  return new Date(guess - (second === first ? first : second));
}

/** The time on the wall of this device at an instant: "YYYY-MM-DDTHH:MM". */
export function localWall(at: Date): string {
  return `${pad(at.getFullYear(), 4)}-${pad(at.getMonth() + 1)}-${pad(at.getDate())}T${pad(at.getHours())}:${pad(at.getMinutes())}`;
}

// --- iCalendar ---------------------------------------------------------------------

function escapeText(value: string): string {
  return value.replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n');
}

function unescapeText(value: string): string {
  return value.replace(/\\([\\;,nN])/g, (_, c: string) => (c === 'n' || c === 'N' ? '\n' : c));
}

/** Folds a line as iCalendar wants it: at most 75 octets, continued with a space. */
function fold(line: string): string {
  const encoder = new TextEncoder();
  const lines: string[] = [];
  let current = '';
  let length = 0;
  for (const character of line) {
    const size = encoder.encode(character).length;
    if (length + size > (lines.length === 0 ? 75 : 74)) {
      lines.push(current);
      current = '';
      length = 0;
    }
    current += character;
    length += size;
  }
  lines.push(current);
  return lines.join('\r\n ');
}

/** The lines of an iCalendar text, unfolded. */
function unfold(text: string): string[] {
  return text.replace(/\r?\n[ \t]/g, '').split(/\r?\n/).filter((line) => line.length > 0);
}

interface Property { name: string; parameters: Record<string, string>; value: string }

function property(line: string): Property | null {
  // The name and its parameters end at the first colon outside quotes.
  let quoted = false;
  let colon = -1;
  for (let i = 0; i < line.length; i++) {
    if (line[i] === '"') {
      quoted = !quoted;
    } else if (line[i] === ':' && !quoted) {
      colon = i;
      break;
    }
  }
  if (colon < 1) {
    return null;
  }
  const [name, ...rest] = line.slice(0, colon).split(';');
  const parameters: Record<string, string> = {};
  for (const parameter of rest) {
    const equals = parameter.indexOf('=');
    if (equals > 0) {
      parameters[parameter.slice(0, equals).toUpperCase()] = parameter.slice(equals + 1).replace(/^"|"$/g, '');
    }
  }
  return { name: name.toUpperCase(), parameters, value: line.slice(colon + 1) };
}

function compact(wall: string): string {
  return wall.length === 10 ? wall.replace(/-/g, '') : `${wall.replace(/[-:]/g, '')}00`;
}

/** A time of iCalendar as a time on the wall, with its zone. */
function readTime(found: Property): { wall: string; zone?: string; allDay: boolean } | null {
  const match = /^(\d{4})(\d{2})(\d{2})(?:T(\d{2})(\d{2})(\d{2})?(Z)?)?$/.exec(found.value.trim());
  if (!match) {
    return null;
  }
  const date = `${match[1]}-${match[2]}-${match[3]}`;
  if (match[4] === undefined) {
    return { wall: date, allDay: true };
  }
  const wall = `${date}T${match[4]}:${match[5]}`;
  if (match[7]) {
    return { wall, zone: 'UTC', allDay: false };
  }
  const zone = found.parameters.TZID;
  // A zone which isn't one of the common database (some programs name their own) is read as this device's.
  return { wall, zone: zone && validZone(zone) ? zone : deviceZone(), allDay: false };
}

/** A duration of iCalendar ("P1D", "PT1H30M", "P1W") in minutes. */
function readDuration(value: string): number | null {
  const match = /^P(?:(\d+)W)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$/.exec(value.trim());
  if (!match) {
    return null;
  }
  return Number(match[1] ?? 0) * 10080 + Number(match[2] ?? 0) * 1440 + Number(match[3] ?? 0) * 60 + Number(match[4] ?? 0);
}

function readRule(value: string): Repeat | null {
  const parts = new Map(value.split(';').map((part) => part.split('=') as [string, string]).map(([key, v]) => [key.toUpperCase(), v ?? '']));
  const every = FREQUENCIES[(parts.get('FREQ') ?? '').toUpperCase()];
  if (!every || [...parts.keys()].some((key) => !['FREQ', 'INTERVAL', 'UNTIL', 'COUNT', 'WKST'].includes(key))) {
    return null;
  }
  const repeat: Repeat = { every, interval: Math.max(1, Number(parts.get('INTERVAL') ?? 1) || 1) };
  const until = /^(\d{4})(\d{2})(\d{2})/.exec(parts.get('UNTIL') ?? '');
  if (until) {
    repeat.until = `${until[1]}-${until[2]}-${until[3]}`;
  }
  const count = Number(parts.get('COUNT') ?? 0);
  if (count > 0) {
    repeat.count = count;
  }
  return repeat;
}

function writeRule(repeat: Repeat): string {
  return `FREQ=${repeat.every.toUpperCase()}${repeat.interval > 1 ? `;INTERVAL=${repeat.interval}` : ''}`
    + `${repeat.count ? `;COUNT=${repeat.count}` : ''}${repeat.until ? `;UNTIL=${repeat.until.replace(/-/g, '')}${repeat.count ? '' : 'T235959Z'}` : ''}`;
}

const KNOWN = ['UID', 'DTSTAMP', 'DTSTART', 'DTEND', 'DURATION', 'SUMMARY', 'LOCATION', 'DESCRIPTION', 'RRULE'];
/** The line which makes an event a call: `X-EDS-CALL:audio` or `X-EDS-CALL:video`. */
const CALL = 'X-EDS-CALL';

/** Reads one event; null if the text isn't one, or has no start. */
export function parseEvent(ics: string): CalendarEvent | null {
  const lines = unfold(ics);
  const begin = lines.findIndex((line) => line.toUpperCase() === 'BEGIN:VEVENT');
  const end = lines.findIndex((line, index) => index > begin && line.toUpperCase() === 'END:VEVENT');
  if (begin < 0 || end < 0) {
    return null;
  }
  const found = new Map<string, Property>();
  const other: string[] = [];
  let nested = 0;
  for (const line of lines.slice(begin + 1, end)) {
    const upper = line.toUpperCase();
    if (upper.startsWith('BEGIN:')) {
      nested++;
    }
    const read = nested === 0 ? property(line) : null;
    const call = read?.name === CALL ? read.value.trim().toLowerCase() : '';
    if (read && KNOWN.includes(read.name)) {
      found.set(read.name, read);
    } else if (read && (call === 'audio' || call === 'video')) {
      found.set(CALL, read);
    } else {
      other.push(line);
    }
    if (upper.startsWith('END:')) {
      nested--;
    }
  }
  const startProperty = found.get('DTSTART');
  const start = startProperty ? readTime(startProperty) : null;
  if (!start) {
    return null;
  }
  const endProperty = found.get('DTEND');
  const ended = endProperty ? readTime(endProperty) : null;
  const duration = found.has('DURATION') ? readDuration(found.get('DURATION')!.value) : null;
  let endWall: string;
  if (ended && ended.allDay === start.allDay) {
    // Start and end are kept in the zone of the start.
    endWall = start.allDay || ended.zone === start.zone ? ended.wall : wallIn(instant(ended.wall, ended.zone), start.zone as string);
  } else if (duration !== null) {
    endWall = start.allDay ? addDays(start.wall, Math.max(1, Math.round(duration / 1440))) : wallIn(new Date(instant(start.wall, start.zone).getTime() + duration * 60000), start.zone as string);
  } else {
    endWall = start.allDay ? addDays(start.wall, 1) : start.wall;
  }
  if (endWall < start.wall || (start.allDay && endWall === start.wall)) {
    endWall = start.allDay ? addDays(start.wall, 1) : start.wall;
  }
  const event: CalendarEvent = {
    uid: found.get('UID')?.value.trim() || newUid(),
    title: unescapeText(found.get('SUMMARY')?.value ?? ''),
    allDay: start.allDay,
    start: start.wall,
    end: endWall,
    other,
  };
  if (!start.allDay) {
    event.zone = start.zone;
  }
  const place = unescapeText(found.get('LOCATION')?.value ?? '');
  if (place) {
    event.place = place;
  }
  const note = unescapeText(found.get('DESCRIPTION')?.value ?? '');
  if (note) {
    event.note = note;
  }
  const rule = found.get('RRULE')?.value.trim();
  if (rule) {
    const repeat = readRule(rule);
    if (repeat) {
      event.repeat = repeat;
    } else {
      event.rule = rule;
    }
  }
  const call = found.get(CALL)?.value.trim().toLowerCase();
  if (call === 'audio' || call === 'video') {
    event.call = call;
  }
  return event;
}

/** The time on the wall of a zone at an instant: "YYYY-MM-DDTHH:MM". */
export function wallIn(at: Date, zone: string): string {
  const shifted = new Date(at.getTime() + offset(zone, at.getTime()));
  return `${pad(shifted.getUTCFullYear(), 4)}-${pad(shifted.getUTCMonth() + 1)}-${pad(shifted.getUTCDate())}T${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}`;
}

function writeTime(name: string, wall: string, event: CalendarEvent): string {
  if (event.allDay) {
    return `${name};VALUE=DATE:${compact(wall)}`;
  }
  return event.zone === 'UTC' ? `${name}:${compact(wall)}Z` : `${name};TZID=${event.zone ?? deviceZone()}:${compact(wall)}`;
}

/** The event as a VEVENT of iCalendar; `stamp` is when it was written. */
export function writeEvent(event: CalendarEvent, stamp: Date = new Date()): string {
  const lines = [
    'BEGIN:VEVENT',
    `UID:${event.uid}`,
    `DTSTAMP:${stamp.toISOString().replace(/[-:]/g, '').replace(/\.\d+/, '')}`,
    writeTime('DTSTART', event.start, event),
    writeTime('DTEND', event.end, event),
    `SUMMARY:${escapeText(event.title)}`,
  ];
  if (event.place) {
    lines.push(`LOCATION:${escapeText(event.place)}`);
  }
  if (event.note) {
    lines.push(`DESCRIPTION:${escapeText(event.note)}`);
  }
  if (event.repeat) {
    lines.push(`RRULE:${writeRule(event.repeat)}`);
  } else if (event.rule) {
    lines.push(`RRULE:${event.rule}`);
  }
  if (event.call) {
    lines.push(`${CALL}:${event.call}`);
  }
  lines.push(...event.other, 'END:VEVENT');
  return lines.map(fold).join('\r\n');
}

/** All events of an `.ics` file. */
export function parseCalendar(text: string): CalendarEvent[] {
  const lines = unfold(text);
  const events: CalendarEvent[] = [];
  let from = -1;
  lines.forEach((line, index) => {
    const upper = line.toUpperCase();
    if (upper === 'BEGIN:VEVENT') {
      from = index;
    } else if (upper === 'END:VEVENT' && from >= 0) {
      const event = parseEvent(lines.slice(from, index + 1).join('\r\n'));
      if (event) {
        events.push(event);
      }
      from = -1;
    }
  });
  return events;
}

/** Events as an `.ics` file. */
export function writeCalendar(events: CalendarEvent[], stamp: Date = new Date()): string {
  return ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Haugr//Rune//EN', 'CALSCALE:GREGORIAN',
    ...events.map((event) => writeEvent(event, stamp)), 'END:VCALENDAR', ''].join('\r\n');
}

export function newUid(): string {
  return `${crypto.randomUUID()}@rune`;
}

// --- Entries -----------------------------------------------------------------------

/** The prefix of the names of the entries of a month, "YYYY-MM/". */
export function monthPrefix(year: number, month: number): string {
  return `${pad(year, 4)}-${pad(month)}/`;
}

export const REPEATING = 'repeat/';

/**
 * The name of the entry of an event: by the month it starts in, so a client
 * fetches the months it shows; repeating events are named apart, and always
 * fetched. The rest of the name tells entries of a month apart.
 */
export function entryName(event: CalendarEvent): string {
  const id = event.uid.replace(/[^A-Za-z0-9.@_-]/g, '').slice(0, 40) || 'event';
  return `${event.repeat || event.rule ? REPEATING : `${event.start.slice(0, 7)}/`}${id}`;
}

// --- When it takes place -------------------------------------------------------------

export interface Occurrence { start: Date; end: Date }

function moved(event: CalendarEvent, k: number): string | null {
  const [y, m, d] = numbers(event.start);
  const step = k * (event.repeat?.interval ?? 1);
  const time = event.start.slice(10);
  switch (event.repeat?.every) {
    case 'daily':
      return addDays(event.start, step);
    case 'weekly':
      return addDays(event.start, 7 * step);
    case 'monthly': {
      const months = (m - 1) + step;
      const made = day(y + Math.floor(months / 12), (months % 12) + 1, d);
      return made ? made + time : null; // A month without that day is passed over.
    }
    case 'yearly': {
      const made = day(y + step, m, d);
      return made ? made + time : null;
    }
    default:
      return k === 0 ? event.start : null;
  }
}

/**
 * When an event takes place between two instants: once, or each time it
 * repeats. What overlaps the span counts.
 */
export function occurrences(event: CalendarEvent, from: Date, to: Date): Occurrence[] {
  const length = event.allDay
    ? daysBetween(event.start, event.end)
    : instant(event.end, event.zone).getTime() - instant(event.start, event.zone).getTime();
  const found: Occurrence[] = [];
  const at = (startWall: string): Occurrence => {
    const start = instant(startWall, event.zone);
    return { start, end: event.allDay ? instant(addDays(startWall, length)) : new Date(start.getTime() + length) };
  };
  if (!event.repeat) {
    const once = at(event.start);
    return once.end > from && once.start < to || (once.end.getTime() === once.start.getTime() && once.start >= from && once.start < to) ? [once] : [];
  }
  let k = 0;
  // Days and weeks pass evenly, so what lies long before the span is skipped.
  if (!event.repeat.count && (event.repeat.every === 'daily' || event.repeat.every === 'weekly')) {
    const step = (event.repeat.every === 'weekly' ? 7 : 1) * event.repeat.interval;
    const before = daysBetween(event.start, localWall(from).slice(0, 10)) - Math.ceil(length / (event.allDay ? 1 : 86400000)) - 2;
    k = Math.max(0, Math.floor(before / step));
  }
  let taken = 0;
  for (let tries = 0; tries < 6000; tries++, k++) {
    const startWall = moved(event, k);
    if (event.repeat.count && k >= event.repeat.count * 50) {
      break;
    }
    if (!startWall) {
      continue;
    }
    if (event.repeat.until && startWall.slice(0, 10) > event.repeat.until) {
      break;
    }
    taken++;
    if (event.repeat.count && taken > event.repeat.count) {
      break;
    }
    const occurrence = at(startWall);
    if (occurrence.start >= to) {
      break;
    }
    if (occurrence.end > from || occurrence.end.getTime() === occurrence.start.getTime() && occurrence.start >= from) {
      found.push(occurrence);
    }
  }
  return found;
}

// --- Calls set for a time ------------------------------------------------------------

/** A call set for a time which is due: an event which is a call, at one of the times it takes place. */
export interface DueCall { uid: string; title: string; media: 'audio' | 'video'; start: Date; end: Date }

/**
 * The calls among events which begin between two instants, or go on at the
 * first of them; the earliest first (FEATURES.md, "Presence and calls").
 */
export function callsDue(events: CalendarEvent[], from: Date, to: Date): DueCall[] {
  return events.filter((event) => event.call && !event.allDay)
    .flatMap((event) => occurrences(event, from, to).map((occurrence) => ({ uid: event.uid, title: event.title, media: event.call as 'audio' | 'video', start: occurrence.start, end: occurrence.end })))
    .sort((a, b) => a.start.getTime() - b.start.getTime());
}
