import { h } from './dom';
import { t } from './i18n';
import { read, write } from './settings';

/**
 * Who is online (FEATURES.md, "Presence and calls"). The stone tells, for
 * every room Rune joined on the socket, which members are present in it:
 * those with an app open and connected which is shown. Rune joins all rooms
 * of the person, so someone is online here if they are present in a room
 * shared with them. Of people one shares no room with, nothing is known.
 *
 * `presence` fires `change` when someone came or went; its detail has the
 * room, and the member who went, if it was one going.
 */
export const presence = new EventTarget();

/** Room → the members present in it. */
const rooms = new Map<string, Set<string>>();

// Also named in settings.ts (ACCOUNT_SETTINGS): the choice is kept with the account.
const KEY = 'rune.present';

/** Whether the person is shown as online; they are, unless they chose not to be. */
export function showMe(): boolean {
  return read(KEY) !== 'off';
}

export function setShowMe(shown: boolean): void {
  write(KEY, shown ? null : 'off');
}

/** True if the member is present in a room shared with them. */
export function isOnline(memberId: string | undefined): boolean {
  if (!memberId) {
    return false;
  }
  for (const members of rooms.values()) {
    if (members.has(memberId)) {
      return true;
    }
  }
  return false;
}

/** True if the member is present in the room. */
export function isPresentIn(circleId: string, memberId: string): boolean {
  return rooms.get(circleId)?.has(memberId) ?? false;
}

/** The members present in a room, in order. */
export function presentIn(circleId: string): string[] {
  return [...(rooms.get(circleId) ?? [])].sort();
}

/** The rooms in which the member is present. */
export function roomsWith(memberId: string): string[] {
  return [...rooms].filter(([, members]) => members.has(memberId)).map(([circleId]) => circleId);
}

function changed(circleId?: string, went?: string): void {
  presence.dispatchEvent(new CustomEvent('change', { detail: { circleId, went } }));
  showLights();
}

/** The members present in a room, as the stone told on joining it. */
export function setPresent(circleId: string, memberIds: string[]): void {
  rooms.set(circleId, new Set(memberIds));
  changed(circleId);
}

export function memberCame(circleId: string, memberId: string): void {
  let members = rooms.get(circleId);
  if (!members) {
    members = new Set();
    rooms.set(circleId, members);
  }
  members.add(memberId);
  changed(circleId);
}

export function memberWent(circleId: string, memberId: string): void {
  rooms.get(circleId)?.delete(memberId);
  changed(circleId, memberId);
}

/** A room was left, or with no room the connection went: nobody is known to be there. */
export function forgetPresence(circleId?: string): void {
  if (circleId) {
    rooms.delete(circleId);
  } else {
    rooms.clear();
  }
  changed(circleId);
}

/** Every light on the page follows who is online. */
function showLights(): void {
  if (typeof document === 'undefined' || typeof document.querySelectorAll !== 'function') {
    return;
  }
  for (const light of document.querySelectorAll<HTMLElement>('.presence-light')) {
    setLight(light);
  }
}

function setLight(light: HTMLElement): void {
  const online = isOnline(light.dataset.member);
  light.hidden = !online;
  // The avatar is hidden from screen readers; the light says it in words.
  light.setAttribute('aria-label', online ? t('online') : '');
  light.title = online ? t('online') : '';
}

/**
 * An avatar with the light of its member: a small dot which shows while
 * the member is online, and follows by itself.
 */
export function withLight(avatar: HTMLElement, memberId: string | undefined): HTMLElement {
  const light = h('span', { class: 'presence-light', role: 'img', hidden: true });
  light.dataset.member = memberId ?? '';
  setLight(light);
  return h('span', { class: 'with-light' }, avatar, light);
}
