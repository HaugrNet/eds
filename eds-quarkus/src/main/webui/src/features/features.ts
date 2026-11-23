import { FeatureState, type CircleFeature, type Entry } from '../api';
import { api } from '../session';
import { socket } from '../socket';
import { text, utf8 } from '../e2e/bytes';
import { base64Content, contentBytes, protect, reveal, roomState } from '../e2e/rooms';
import chatDeclaration from './declarations/chat.json';
import calendarDeclaration from './declarations/calendar.json';
import locationDeclaration from './declarations/location.json';
import plansDeclaration from './declarations/plans.json';
import callsDeclaration from './declarations/calls.json';

/**
 * Features (FEATURES.md): what a room or chamber can have beside its files. The
 * stone knows a feature only as a declaration, which Rune brings along: the
 * kinds of its entries and the rules for them. What an entry holds is
 * Rune's business; in a chamber Rune encrypts it before the stone gets it.
 */
export const DECLARATIONS: Record<string, { feature: string; version: number }> = {
  chat: chatDeclaration,
  calendar: calendarDeclaration,
  location: locationDeclaration,
  plans: plansDeclaration,
  calls: callsDeclaration,
};

/** The features Rune knows, in the order it shows them. */
export const FEATURES = ['chat', 'calendar', 'plans', 'location', 'calls'] as const;
export type FeatureName = typeof FEATURES[number];

/** The names of the features the stone offers, with their versions. */
export async function offeredFeatures(): Promise<Map<string, number>> {
  const response = await api.features.getFeatures();
  return new Map((response.features ?? []).map((feature) => [feature.name ?? '', feature.version ?? 0]));
}

/** Lets the stone offer a feature Rune knows, or brings its declaration up to Rune's; System Administrator only. */
export async function offerFeature(name: FeatureName): Promise<void> {
  await api.features.putFeature({ feature: name, featureRequest: { declaration: JSON.stringify(DECLARATIONS[name]) } });
}

/** Stops the stone offering a feature; it must be removed from every room first. */
export async function withdrawFeature(name: string): Promise<void> {
  await api.features.deleteFeature({ feature: name });
}

/** The features of a room with their state. */
export async function roomFeatures(circleId: string): Promise<CircleFeature[]> {
  return (await api.features.getCircleFeatures({ circleId })).features ?? [];
}

/** Switches a feature of a room on, off or to read-only; administrators of the room may. */
export async function switchFeature(circleId: string, name: string, state: FeatureState): Promise<void> {
  await api.features.putCircleFeature({ circleId, feature: name, circleFeatureRequest: { state } });
}

/** Removes a feature from a room, with all its entries. */
export async function removeFeature(circleId: string, name: string): Promise<void> {
  await api.features.deleteCircleFeature({ circleId, feature: name });
}

/** True if the feature is switched on, or at least readable, in the room. */
export function usable(features: CircleFeature[], name: string, write = false): boolean {
  const state = features.find((feature) => feature.name === name)?.state;
  return state === FeatureState.On || (!write && state === FeatureState.Readonly);
}

// --- Entries ---------------------------------------------------------------------

/** An entry as Rune works with it: what the stone notes, and the content as the object it is. */
export interface FeatureEntry<T> {
  entryId: string;
  circleId: string;
  kind: string;
  position: number;
  about?: string;
  name?: string;
  memberId?: string;
  accountName?: string;
  added?: string;
  changed?: string;
  closed: boolean;
  /** The content; null if it can't be read (damaged, of another convention, or no key). */
  body: T | null;
  /** Why the content can't be read. */
  problem?: string;
}

/** The content of an entry as the stone takes it: JSON, encrypted in a chamber, as Base64. */
export async function encodeEntry(circleId: string, feature: string, kind: string, body: object): Promise<string> {
  return base64Content(await protect(circleId, `${feature}|${kind}`, utf8(JSON.stringify(body))));
}

/** An entry of the stone as Rune works with it. An entry which can't be read keeps its place, without content. */
export async function decodeEntry<T>(feature: string, entry: Entry): Promise<FeatureEntry<T>> {
  const decoded: FeatureEntry<T> = {
    entryId: entry.entryId ?? '',
    circleId: entry.circleId ?? '',
    kind: entry.kind ?? '',
    position: entry.position ?? 0,
    about: entry.about ?? undefined,
    name: entry.name ?? undefined,
    memberId: entry.memberId ?? undefined,
    accountName: entry.accountName ?? undefined,
    added: entry.added ?? undefined,
    changed: entry.changed ?? undefined,
    closed: entry.closed === true,
    body: null,
  };
  if (entry.content === undefined || entry.content === null) {
    return decoded;
  }
  try {
    const stored = contentBytes(entry.content);
    const plain = await reveal(decoded.circleId, `${feature}|${decoded.kind}`, stored);
    const value = JSON.parse(text(plain)) as unknown;
    if (typeof value === 'object' && value !== null) {
      decoded.body = value as T;
    }
  } catch (error) {
    decoded.problem = error instanceof Error ? error.message : String(error);
  }
  return decoded;
}

/** An announced entry may come without its content, when that is long: then it is fetched. */
export async function completeEntry(feature: string, entry: Entry, removed = false): Promise<Entry> {
  if (removed || (entry.content !== undefined && entry.content !== null) || !entry.circleId || !entry.entryId) {
    return entry;
  }
  return socket.get(entry.circleId, feature, entry.entryId);
}

/** Adds an entry to a feature of a room. */
export async function addEntry<T>(circleId: string, feature: string, kind: string, body: object, about?: string, name?: string): Promise<FeatureEntry<T>> {
  return decodeEntry<T>(feature, await socket.add(circleId, feature, kind, await encodeEntry(circleId, feature, kind, body), about, name));
}

/** Changes the content of an entry, and its name if given. */
export async function changeEntry<T>(circleId: string, feature: string, kind: string, entryId: string, body: object, name?: string): Promise<FeatureEntry<T>> {
  return decodeEntry<T>(feature, await socket.change(circleId, feature, entryId, await encodeEntry(circleId, feature, kind, body), name));
}

/** A page of entries of a feature of a room, decoded. */
export async function listEntries<T>(circleId: string, feature: string, options: Parameters<typeof socket.list>[2] = {}): Promise<FeatureEntry<T>[]> {
  const entries = await socket.list(circleId, feature, options);
  return Promise.all(entries.map((entry) => decodeEntry<T>(feature, entry)));
}

/** All entries of a kind, the oldest first, fetched in pages; only those about an entry, or whose names start with `name`, if given. */
export async function allEntries<T>(circleId: string, feature: string, kind: string, about?: string, name?: string): Promise<FeatureEntry<T>[]> {
  const all: FeatureEntry<T>[] = [];
  let after: number | undefined;
  for (;;) {
    const page = await listEntries<T>(circleId, feature, { kind, about, name, after, oldestFirst: true, limit: 200 });
    all.push(...page);
    if (page.length < 200) {
      return all;
    }
    after = page[page.length - 1].position;
  }
}

/** True if the room is a chamber in which this device can't write right now; the reason is in e2e/rooms. */
export async function canWrite(circleId: string): Promise<boolean> {
  const state = await roomState(circleId);
  return state.kind === 'room' || state.kind === 'ready';
}
