import type { SocketFrame } from './api';
import { calls } from './calls';
import { startRoomCall } from './components/call-panel';
import { notice } from './components/notices';
import { callsDue, monthPrefix, parseEvent, REPEATING, type CalendarEvent, type DueCall, type EventBody } from './features/calendar';
import { allEntries, usable } from './features/features';
import { t } from './i18n';
import { announce, roomName } from './notifications';
import { socket } from './socket';

/**
 * Calls set for a time (FEATURES.md, "Presence and calls"): an event of a
 * room's calendar which is a call. Rune looks at the calendars of all rooms
 * of the person, and tells ten minutes before such a call and when it
 * starts, with a button to join; a notification too, if the person chose
 * notifications. It only tells while it is open: who opens Rune during the
 * call is told then, and sees it in the calendar otherwise.
 */
const SOON = 10 * 60 * 1000;
/** How far ahead Rune looks; it looks again long before that is over. */
const AHEAD = 24 * 60 * 60 * 1000;
const AGAIN = 30 * 60 * 1000;

/** What was told already, so nothing is told twice while Rune is open. */
const told = new Set<string>();
let timers: number[] = [];
let again: number | undefined;
let soon: number | undefined;
let looking = 0;
let listeners: AbortController | null = null;

function tell(circleId: string, call: DueCall, when: 'soon' | 'now' | 'on'): void {
  const key = `${circleId}|${call.uid}|${call.start.toISOString()}|${when === 'soon' ? 'soon' : 'now'}`;
  if (told.has(key) || calls.inCallOf(circleId)) {
    return;
  }
  told.add(key);
  const room = roomName(circleId);
  const title = call.title || t('(no title)');
  const minutes = Math.max(1, Math.round((call.start.getTime() - Date.now()) / 60000));
  const text = when === 'soon' ? t('The call "{title}" in {room} starts in {n} minutes.', { title, room, n: minutes })
    : when === 'now' ? t('The call "{title}" in {room} starts now.', { title, room })
      : t('The call "{title}" in {room} is on.', { title, room });
  const join = calls.supported() && calls.usable(circleId)
    ? [{ label: t('Join'), run: () => void startRoomCall(circleId, call.media) }]
    : [];
  // The note stays until the call is over, or until it is closed.
  notice(text, join, Math.max(60, Math.round((call.end.getTime() - Date.now()) / 1000)), `due-${circleId}-${call.uid}`);
  void announce(circleId, { quiet: t('A call in Rune'), room: t('A call is due'), full: text }, undefined, 'call-');
}

async function eventsOf(circleId: string, now: Date): Promise<CalendarEvent[]> {
  const next = new Date(now.getFullYear(), now.getMonth() + 1, 1);
  const prefixes = [REPEATING, monthPrefix(now.getFullYear(), now.getMonth() + 1), monthPrefix(next.getFullYear(), next.getMonth() + 1)];
  const pages = await Promise.all(prefixes.map((prefix) => allEntries<EventBody>(circleId, 'calendar', 'event', undefined, prefix)));
  return pages.flat().flatMap((entry) => {
    const event = entry.body && typeof entry.body.ics === 'string' ? parseEvent(entry.body.ics) : null;
    return event ? [event] : [];
  });
}

/** Looks at the calendars of the rooms, tells what is due, and sets the clock for what comes. */
async function look(): Promise<void> {
  const run = ++looking;
  const now = new Date();
  const due: [string, DueCall][] = [];
  for (const circleId of socket.rooms()) {
    if (!usable(socket.featuresOf(circleId), 'calendar')) {
      continue;
    }
    try {
      for (const call of callsDue(await eventsOf(circleId, now), now, new Date(now.getTime() + AHEAD))) {
        due.push([circleId, call]);
      }
    } catch {
      // A calendar which can't be read now is looked at the next time.
    }
  }
  if (run !== looking) {
    return;
  }
  timers.forEach((timer) => window.clearTimeout(timer));
  timers = [];
  for (const [circleId, call] of due) {
    const start = call.start.getTime();
    const at = Date.now();
    if (start <= at) {
      tell(circleId, call, 'on');
      continue;
    }
    if (start - SOON <= at) {
      tell(circleId, call, 'soon');
    } else {
      timers.push(window.setTimeout(() => tell(circleId, call, 'soon'), start - SOON - at));
    }
    timers.push(window.setTimeout(() => tell(circleId, call, 'now'), start - at));
  }
}

/** Looks again a moment after the last thing which may have changed a calendar. */
function lookSoon(): void {
  window.clearTimeout(soon);
  soon = window.setTimeout(() => void look(), 2000);
}

/** Starts telling about calls set for a time; once per login. */
export function startReminders(): void {
  if (listeners) {
    return;
  }
  listeners = new AbortController();
  const signal = listeners.signal;
  for (const name of ['added', 'changed', 'removed', 'feature']) {
    socket.addEventListener(name, (event) => {
      const feature = (event as CustomEvent<SocketFrame>).detail.feature;
      if (feature === 'calendar' || feature === 'calls') {
        lookSoon();
      }
    }, { signal });
  }
  // A room joined, or joined again after the connection came back.
  socket.addEventListener('joined', () => lookSoon(), { signal });
  again = window.setInterval(() => void look(), AGAIN);
  lookSoon();
}

/** Stops, at logout. */
export function stopReminders(): void {
  listeners?.abort();
  listeners = null;
  looking++;
  window.clearInterval(again);
  window.clearTimeout(soon);
  timers.forEach((timer) => window.clearTimeout(timer));
  timers = [];
  told.clear();
}
