import { socket } from '../socket';
import { addEntry } from './features';
import { distance, positionBody, type PositionBody, type Reporting } from './location';

/**
 * Reports where this device is to rooms with the location feature (FEATURES.md,
 * "Location"). Each Member decides on each device, for each room, whether
 * it reports: not at all, the latest position only, a track too, or
 * positions which are passed on and not kept. The choice stays on the
 * device. Rune reports only while it is open; nobody reports for someone
 * else.
 */
const KEY = 'rune.location.';
const FEATURE = 'location';
/** A kept position at most this often, and a point of the track when the device moved. */
const LATEST_EVERY = 60000;
const TRACK_EVERY = 120000;
const TRACK_MOVED = 30;
const PASSING_EVERY = 5000;

interface Room { mode: Reporting; latest: number; track: number; passed: number; trackAt?: PositionBody }

const rooms = new Map<string, Room>();
let watch: number | null = null;
let memberId = '';
let last: PositionBody | null = null;
let problem = '';
const listeners = new Set<() => void>();

function stored(): Record<string, Reporting> {
  try {
    return JSON.parse(localStorage.getItem(KEY + memberId) ?? '{}') as Record<string, Reporting>;
  } catch {
    return {};
  }
}

/** True if this device can tell where it is. */
export function canReport(): boolean {
  return typeof navigator !== 'undefined' && 'geolocation' in navigator;
}

/** What this device reports to a room. */
export function reporting(circleId: string): Reporting {
  return rooms.get(circleId)?.mode ?? 'off';
}

/** Where this device last was, if it knows. */
export function ownPosition(): PositionBody | null {
  return last;
}

/** Why the device can't tell where it is, or nothing. */
export function reportProblem(): string {
  return problem;
}

/** Tells when the device's position or its problem changed. */
export function onReport(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function told(): void {
  listeners.forEach((listener) => listener());
}

async function send(circleId: string, room: Room, body: PositionBody): Promise<void> {
  const now = Date.now();
  if (room.mode === 'passing') {
    if (now - room.passed >= PASSING_EVERY) {
      room.passed = now;
      socket.signal(circleId, FEATURE, JSON.stringify(body));
    }
    return;
  }
  if (now - room.latest >= LATEST_EVERY) {
    room.latest = now;
    await addEntry(circleId, FEATURE, 'position', body);
  }
  if (room.mode === 'track' && now - room.track >= TRACK_EVERY && (!room.trackAt || distance(room.trackAt, body) >= TRACK_MOVED)) {
    room.track = now;
    room.trackAt = body;
    await addEntry(circleId, FEATURE, 'track', body);
  }
}

function measured(position: GeolocationPosition): void {
  problem = '';
  last = positionBody(position.coords.latitude, position.coords.longitude, position.coords.accuracy, new Date(position.timestamp));
  for (const [circleId, room] of rooms) {
    if (room.mode !== 'off') {
      void socket.join(circleId).then(() => send(circleId, room, last as PositionBody)).catch(() => undefined);
    }
  }
  told();
}

function follow(): void {
  const wanted = [...rooms.values()].some((room) => room.mode !== 'off');
  if (wanted && watch === null && canReport()) {
    watch = navigator.geolocation.watchPosition(measured, (error) => {
      problem = error.message || String(error.code);
      told();
    }, { enableHighAccuracy: true, maximumAge: 30000, timeout: 60000 });
  } else if (!wanted && watch !== null) {
    navigator.geolocation.clearWatch(watch);
    watch = null;
  }
}

/** Sets what this device reports to a room, and keeps the choice on the device. */
export function setReporting(circleId: string, mode: Reporting): void {
  rooms.set(circleId, { mode, latest: 0, track: 0, passed: 0 });
  const kept = stored();
  if (mode === 'off') {
    delete kept[circleId];
  } else {
    kept[circleId] = mode;
  }
  try {
    localStorage.setItem(KEY + memberId, JSON.stringify(kept));
  } catch {
    // Without storage, the choice holds while the page is open.
  }
  follow();
  // What is known already goes at once.
  const room = rooms.get(circleId);
  if (last && room && mode !== 'off') {
    void send(circleId, room, last).catch(() => undefined);
  }
}

/** After login: the device reports again to the rooms it was told to. */
export function startReporting(member: string): void {
  memberId = member;
  rooms.clear();
  for (const [circleId, mode] of Object.entries(stored())) {
    rooms.set(circleId, { mode, latest: 0, track: 0, passed: 0 });
  }
  follow();
}

/** At logout, and when a room is left: the device stops reporting. */
export function stopReporting(circleId?: string): void {
  if (circleId) {
    rooms.delete(circleId);
  } else {
    rooms.clear();
    last = null;
    problem = '';
  }
  follow();
}
