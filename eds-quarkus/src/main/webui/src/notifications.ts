import type { Circle } from './api';
import type { ChatMessage } from './chat';
import { chat } from './chat';
import { session } from './session';
import { t } from './i18n';

/**
 * Unread counts and browser notifications for new chat messages (Rune).
 *
 * Rune joins all rooms of the member on the chat connection, so new
 * messages arrive from every room. A room's count comes from EDS when
 * joining (messages after the read marker), grows with each new message
 * from someone else while the member doesn't look at that room, and drops
 * to 0 when the room is marked read. The tab title shows the total.
 *
 * Notifications are opt-in per device, and say as little as chosen, as they
 * may show on a locked screen: by default "New message in Rune", or also the
 * room, or also sender and text. They only work while Rune is open in a tab;
 * notifications for a closed app would need Web Push via the browser makers'
 * push services, which Rune doesn't use.
 */
export type NotificationLevel = 'off' | 'quiet' | 'room' | 'full';

const KEY = 'rune.notifications';
const counts = new Map<string, number>();
const names = new Map<string, string>();
export const unread = new EventTarget();

/** The room open in the app, and whether its newest messages are in view. */
let openRoom: string | null = null;
let looking = () => false;

export function setOpenRoom(circleId: string | null, isLooking: () => boolean): void {
  openRoom = circleId;
  looking = isLooking;
}

/** The screen named in the tab title instead of the unread count; the login and the page for an EDS that doesn't answer. */
let screen: string | null = null;

/** Names a screen in the tab title ("Sign in · Rune"), or with null gives the title back to the unread count. */
export function setScreenTitle(name: string | null): void {
  screen = name;
  showTitle();
}

function showTitle(): void {
  if (screen) {
    document.title = `${screen} · Rune`;
    return;
  }
  const total = [...counts.values()].reduce((sum, n) => sum + n, 0);
  document.title = total > 0 ? `(${total > 99 ? '99+' : total}) Rune` : 'Rune';
}

export function count(circleId: string): number {
  return counts.get(circleId) ?? 0;
}

export function setCount(circleId: string, value: number): void {
  counts.set(circleId, value);
  showTitle();
  unread.dispatchEvent(new CustomEvent('change', { detail: circleId }));
}

export function level(): NotificationLevel {
  try {
    return (localStorage.getItem(KEY) as NotificationLevel | null) ?? 'off';
  } catch {
    return 'off';
  }
}

/** Sets the level; turning notifications on asks the browser first. */
export async function setLevel(value: NotificationLevel): Promise<boolean> {
  if (value !== 'off') {
    if (!('Notification' in window)) {
      return false;
    }
    if (Notification.permission !== 'granted' && await Notification.requestPermission() !== 'granted') {
      return false;
    }
  }
  try {
    localStorage.setItem(KEY, value);
  } catch {
    // Without storage, the choice lasts until the page reloads.
  }
  return true;
}

/** Joins all rooms of the member, for their unread counts and new messages. */
export async function watchRooms(rooms: Circle[]): Promise<void> {
  for (const room of rooms) {
    if (room.circleId) {
      names.set(room.circleId, room.circleName ?? '');
      chat.join(room.circleId).then((n) => setCount(room.circleId as string, n)).catch(() => undefined);
    }
  }
}

export function forgetRoom(circleId: string): void {
  counts.delete(circleId);
  setCount(circleId, 0);
  counts.delete(circleId);
}

let started = false;

/** Listens for new messages; once per page. */
export function startNotifications(openRoomById: (circleId: string) => void): void {
  if (started) {
    return;
  }
  started = true;
  opener = openRoomById;
  chat.addEventListener('message', (event) => {
    const message = (event as CustomEvent<ChatMessage>).detail;
    const circleId = message.circleId;
    if (!circleId || message.memberId === session()?.memberId) {
      return;
    }
    const seen = circleId === openRoom && document.visibilityState === 'visible' && looking();
    if (!seen) {
      setCount(circleId, count(circleId) + 1);
      void notify(message, circleId, openRoomById);
    }
  });
  // A click on a notification shown by the service worker opens its room.
  navigator.serviceWorker?.addEventListener('message', (event) => {
    if (event.data?.type === 'open-room' && typeof event.data.circleId === 'string') {
      openRoomById(event.data.circleId);
    }
  });
}

async function notify(message: ChatMessage, circleId: string, openRoomById: (circleId: string) => void): Promise<void> {
  await announce(circleId, {
    quiet: t('New message in Rune'), room: t('New message'),
    full: `${message.accountName ?? ''}: ${(message.text ?? '').substring(0, 140)}`,
  }, openRoomById);
}

/** The name of a room of the member, as last seen; empty if it isn't known. */
export function roomName(circleId: string): string {
  return names.get(circleId) ?? '';
}

let opener: (circleId: string) => void = () => undefined;

/**
 * A browser notification about a room, if the person chose notifications,
 * saying as little as they chose: `quiet` names nothing, `room` goes under
 * the room's name, `full` says who and what. A click opens the room.
 */
export async function announce(circleId: string, texts: { quiet: string; room: string; full: string }, openRoomById: (circleId: string) => void = opener, kind = ''): Promise<void> {
  const chosen = level();
  if (chosen === 'off' || !('Notification' in window) || Notification.permission !== 'granted') {
    return;
  }
  const room = names.get(circleId) ?? '';
  const title = chosen === 'quiet' || !room ? 'Rune' : `Rune · ${room}`;
  const body = chosen === 'full' ? texts.full : chosen === 'room' ? texts.room : texts.quiet;
  const options = { body, tag: `rune-${kind}${chosen === 'quiet' ? 'all' : circleId}`, icon: 'icons/icon-192.png', data: { circleId } };
  try {
    // Through the service worker where there is one: phones need that.
    const registration = await navigator.serviceWorker?.getRegistration();
    if (registration) {
      await registration.showNotification(title, options);
      return;
    }
    const shown = new Notification(title, options);
    shown.onclick = () => {
      window.focus();
      openRoomById(circleId);
      shown.close();
    };
  } catch {
    // Notifications are a nicety; nothing else depends on them.
  }
}
