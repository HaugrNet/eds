import { CredentialType, TrustLevel, type Trustee } from '../api';
import { friendly } from '../messages';
import { api, session } from '../session';
import { t } from '../i18n';
import { checkKey, knownChambers, rememberChamber } from './device';
import { createKeys, keyPair, keyState } from './identity';
import {
  chamberVersion, decryptContent, E2EError, encryptContent, isChamber, isProtected, openSecretWithInvite, openSecretWithKey, openSharedValue,
  parsePublicKey, secretKind, secretVersion, sharedValue, wrapForInvite, wrapToKey, type RoomKey,
} from './convention';
import { fromB64u, random, toB64 } from './bytes';

/**
 * Rooms and chambers (E2E.md). A room is a Circle as it always was: the
 * stone encrypts, and sees the content while it handles it. A chamber is a
 * Circle whose content Rune encrypts before the stone gets it, with a room
 * key which only the people of the chamber have. The stone can't tell the
 * two apart; a chamber is known by its shared value.
 *
 * How this device stands to a room:
 * - `room`: not a chamber.
 * - `ready`: a chamber, and this device has its current key.
 * - `behind`: a chamber whose key was changed; this device has an earlier
 *   one, reads what was written with it, and waits for an administrator.
 * - `waiting`: a chamber, and nobody gave this Member its key yet.
 * - `locked`: a chamber, and this device hasn't the Member's keys.
 * - `stripped`: this device knew the room as a chamber, and the stone now
 *   presents it as a room. Nothing is written to it.
 */
export type RoomKind = 'room' | 'ready' | 'behind' | 'waiting' | 'locked' | 'stripped';

export interface RoomState {
  kind: RoomKind;
  /** The room keys this device has, by their versions. */
  keys: Map<number, Uint8Array>;
  /** The key new content is written with; only when `ready`. */
  current?: RoomKey;
}

const states = new Map<string, Promise<RoomState>>();
const PLAIN: RoomState = { kind: 'room', keys: new Map() };

/** Fired on the document when what this device knows of a room's keys changed, with the Circle's ID. */
export const ROOM_CHANGED = 'e2e-room-changed';

/** Forgets what was opened, e.g. at logout, or for one room when its key may have changed. */
export function forgetRooms(circleId?: string): void {
  if (circleId) {
    states.delete(circleId);
  } else {
    states.clear();
  }
}

/**
 * How this device stands to a room, opened once and kept. A room which
 * waits for its key from an administrator is not kept, but asked for again:
 * the key is there the next time the room is opened, without a new login.
 */
export function roomState(circleId: string): Promise<RoomState> {
  let state = states.get(circleId);
  if (!state) {
    const opened: Promise<RoomState> = open(circleId).then((result) => {
      if ((result.kind === 'waiting' || result.kind === 'behind') && states.get(circleId) === opened) {
        states.delete(circleId);
      }
      return result;
    }, (error) => {
      if (states.get(circleId) === opened) {
        states.delete(circleId);
      }
      throw error;
    });
    state = opened;
    states.set(circleId, state);
  }
  return state;
}

/** True if the room is a chamber, whatever this device can do in it. */
export async function isChamberRoom(circleId: string): Promise<boolean> {
  return (await roomState(circleId)).kind !== 'room';
}

async function open(circleId: string): Promise<RoomState> {
  const memberId = session()?.memberId ?? '';
  const circle = (await api.circles.getCircleById({ circleId })).circles?.[0];
  const shared = circle?.circleKey;
  if (!isChamber(shared)) {
    return knownChambers(memberId).has(circleId) ? { kind: 'stripped', keys: new Map() } : PLAIN;
  }
  rememberChamber(memberId, circleId);
  const pair = keyPair();
  if (!pair) {
    return { kind: keyState() === 'none' ? 'waiting' : 'locked', keys: new Map() };
  }
  const secret = await ownSecret(circleId);
  if (secretKind(secret) !== 'key') {
    return { kind: 'waiting', keys: new Map() };
  }
  let own: RoomKey;
  try {
    own = await openSecretWithKey(secret as string, pair, circleId, memberId);
  } catch {
    // Wrapped to a key this Member no longer has.
    return { kind: 'waiting', keys: new Map() };
  }
  const keys = await openSharedValue(shared as string, circleId, own);
  return own.version === chamberVersion(shared)
    ? { kind: 'ready', keys, current: own }
    : { kind: 'behind', keys };
}

async function ownSecret(circleId: string): Promise<string | undefined> {
  const response = await api.trustees.fetchTrustees({ fetchTrusteeRequest: {
    credentialType: CredentialType.Session,
    credential: session()?.token,
    circleId,
    memberId: session()?.memberId,
  } });
  return response.trustees?.[0]?.secret ?? undefined;
}

async function setSecrets(circleId: string, secrets: { memberId: string; secret: string }[], circleKey?: string): Promise<void> {
  if (secrets.length === 0 && circleKey === undefined) {
    return;
  }
  await api.circles.putCircleSecrets({ circleId, processTrusteeRequest: { secrets, circleKey } });
}

/** The reason why nothing can be written to a room, or null if it can. */
export function refusal(state: RoomState): string | null {
  switch (state.kind) {
    case 'room':
    case 'ready':
      return null;
    case 'behind':
      return t('The key of this chamber was changed. The new one comes from an administrator of it: their app gives it to you as soon as they have the chamber open in Rune, or list its members in Tolk.');
    case 'waiting':
      // Without keys of their own nobody can give them the key of the chamber: that comes first, and is theirs to do.
      return keyState() === 'none'
        ? t('You have no keys for chambers yet. Make them under your name in the menu, in "Keys for chambers". The app of an administrator then gives you the key of this chamber, as soon as they have it open.')
        : t('You have no key to this chamber yet. It comes from an administrator of it: their app gives it to you as soon as they have the chamber open in Rune, or list its members in Tolk.');
    case 'locked':
      return t('This device does not have your keys yet. Enter your recovery key to read chambers here.');
    case 'stripped':
      return t('This room was a chamber, and the stone now presents it as an ordinary room. Rune writes nothing to it.');
  }
}

// --- Content ---------------------------------------------------------------------

/**
 * What goes to the stone for a content of a room: the content itself in a
 * room, encrypted in a chamber. The context says what it is: empty for a
 * file, "thumbnail", or "<feature>|<kind>".
 */
export async function protect(circleId: string, context: string, plain: Uint8Array): Promise<Uint8Array> {
  const state = await roomState(circleId);
  if (state.kind === 'room') {
    return plain;
  }
  if (!state.current) {
    throw new Error(refusal(state) ?? '');
  }
  return encryptContent(state.current, circleId, context, plain);
}

/** A content of a room as it came from the stone, readable: as it is in a room, decrypted in a chamber. */
export async function reveal(circleId: string, context: string, stored: Uint8Array): Promise<Uint8Array> {
  const state = await roomState(circleId);
  if (state.kind === 'room') {
    return stored;
  }
  if (state.keys.size === 0) {
    throw new E2EError(refusal(state) ?? '');
  }
  return decryptContent(state.keys, circleId, context, stored);
}

/** True if a content of a chamber was written by an app which knows chambers. */
export function wasProtected(stored: Uint8Array): boolean {
  return isProtected(stored);
}

// --- Creating, inviting, repairing, changing the key --------------------------------

/** Makes the Member's keys if they have none; returns the recovery key if it made them. */
export async function ensureKeys(): Promise<string | null> {
  if (keyState() === 'none') {
    return createKeys();
  }
  if (keyState() === 'locked') {
    throw new Error(t('This device does not have your keys yet. Enter your recovery key first, under your name in the menu.'));
  }
  return null;
}

/**
 * Turns a Circle which was just created into a chamber: makes its room
 * key, and stores its mark and the creator's secret in one step. The
 * Member's keys must be there (ensureKeys).
 */
export async function makeChamber(circleId: string): Promise<void> {
  const pair = keyPair();
  const memberId = session()?.memberId;
  if (!pair || !memberId) {
    throw new Error(t('This device does not have your keys yet. Enter your recovery key first, under your name in the menu.'));
  }
  const room: RoomKey = { key: random(32), version: 1 };
  await setSecrets(circleId, [{ memberId, secret: await wrapToKey(room, circleId, memberId, pair.publicPoint) }], await sharedValue(circleId, room, []));
  rememberChamber(memberId, circleId);
  forgetRooms(circleId);
}

/**
 * The secret for someone invited into a chamber by a link: the room key
 * under an invite secret, which travels in the link and never reaches the
 * stone. Returns both.
 */
export async function inviteSecret(circleId: string, name: string): Promise<{ secret: string; inviteSecret: Uint8Array }> {
  const state = await roomState(circleId);
  if (!state.current) {
    throw new Error(refusal(state) ?? '');
  }
  const invite = random(32);
  return { secret: await wrapForInvite(state.current, circleId, name, invite), inviteSecret: invite };
}

/** The secret for a Member who has keys: the room key wrapped to their public key. Null if they have none. */
export async function secretFor(circleId: string, memberId: string, publicKey: string | null | undefined): Promise<string | null> {
  const state = await roomState(circleId);
  const point = parsePublicKey(publicKey);
  if (!state.current || !point) {
    return null;
  }
  return wrapToKey(state.current, circleId, memberId, point);
}

/**
 * After accepting an invitation by a link: every secret of this Member
 * which is wrapped under the invite secret is opened with it and wrapped to
 * the Member's own key, so the invite secret can be forgotten.
 */
export async function adoptInvitation(name: string, invite: Uint8Array): Promise<void> {
  const pair = keyPair();
  const memberId = session()?.memberId;
  if (!pair || !memberId) {
    return;
  }
  const response = await api.trustees.fetchTrustees({ fetchTrusteeRequest: { credentialType: CredentialType.Session, credential: session()?.token } });
  for (const trustee of response.trustees ?? []) {
    if (trustee.circleId && secretKind(trustee.secret) === 'invite') {
      const room = await openSecretWithInvite(trustee.secret as string, invite, trustee.circleId, name);
      await api.circles.putCircleTrusteeById({
        circleId: trustee.circleId,
        memberId,
        processTrusteeRequest: { secret: await wrapToKey(room, trustee.circleId, memberId, pair.publicPoint) },
      });
      rememberChamber(memberId, trustee.circleId);
      forgetRooms(trustee.circleId);
    }
  }
}

/**
 * The public key of a member to give a room key to: none if the member has
 * no key, or one other than this device saw before, until the person here
 * has accepted it (E2E.md, "Checking keys"). The stone hands the keys out,
 * so a changed one doesn't get a room key by itself.
 */
function keyToTrust(member: Trustee): Uint8Array | null {
  const me = session()?.memberId;
  const point = parsePublicKey(member.publicKey);
  if (!point || !me || !member.memberId || !member.publicKey) {
    return null;
  }
  return checkKey(me, member.memberId, member.publicKey) === 'changed' ? null : point;
}

/**
 * Repair on sight, by an administrator of a chamber: every member who has
 * a public key and no secret of the current room key gets one. That covers
 * someone added before they had keys, someone who made new keys, and
 * someone who joined with a link from before the key was changed. Returns
 * how many got the key.
 */
export async function repair(circleId: string, members: Trustee[]): Promise<number> {
  const state = await roomState(circleId);
  if (!state.current) {
    return 0;
  }
  const secrets: { memberId: string; secret: string }[] = [];
  for (const member of members) {
    const point = keyToTrust(member);
    const behind = secretKind(member.secret) !== 'key' || secretVersion(member.secret) !== state.current.version;
    if (member.memberId && member.memberId !== session()?.memberId && point && behind && secretKind(member.secret) !== 'invite') {
      secrets.push({ memberId: member.memberId, secret: await wrapToKey(state.current, circleId, member.memberId, point) });
    }
  }
  await setSecrets(circleId, secrets);
  return secrets.length;
}

/**
 * Changes the key of a chamber, after a member was removed: a new room key
 * for everyone who is left and has keys, and the earlier keys under it, so
 * old content stays readable. All in one step on the stone.
 */
export async function changeKey(circleId: string, remaining: Trustee[]): Promise<void> {
  const state = await roomState(circleId);
  const pair = keyPair();
  const memberId = session()?.memberId;
  if (!state.current || !pair || !memberId) {
    throw new Error(refusal(state) ?? t('This device does not have your keys yet. Enter your recovery key first, under your name in the menu.'));
  }
  const next: RoomKey = { key: random(32), version: state.current.version + 1 };
  const earlier: RoomKey[] = [...state.keys.entries()].map(([version, key]) => ({ key, version }));
  const secrets: { memberId: string; secret: string }[] = [{ memberId, secret: await wrapToKey(next, circleId, memberId, pair.publicPoint) }];
  for (const member of remaining) {
    if (member.memberId && member.memberId !== memberId) {
      const point = keyToTrust(member);
      // Someone without keys, with a changed key, or with a link still to accept, waits for a repair.
      secrets.push({ memberId: member.memberId, secret: point ? await wrapToKey(next, circleId, member.memberId, point) : '' });
    }
  }
  await setSecrets(circleId, secrets, await sharedValue(circleId, next, earlier));
  forgetRooms(circleId);
  document.dispatchEvent(new CustomEvent(ROOM_CHANGED, { detail: circleId }));
}

/** What an administrator sees of a member's key in a chamber. */
export function memberKeyState(member: Trustee, currentVersion: number): 'none' | 'invited' | 'behind' | 'current' {
  if (secretKind(member.secret) === 'invite') {
    return 'invited';
  }
  if (!parsePublicKey(member.publicKey)) {
    return 'none';
  }
  return secretKind(member.secret) === 'key' && secretVersion(member.secret) === currentVersion ? 'current' : 'behind';
}

/** Whether the logged-in member may be given access levels in a chamber: everyone may, as in a room. */
export const CHAMBER_LEVELS = [TrustLevel.Read, TrustLevel.Write, TrustLevel.Admin];

/** Base64 of a content as the stone's JSON operations take it. */
export function base64Content(bytes: Uint8Array): string {
  return toB64(bytes);
}

/** Bytes of a Base64 content as the stone's JSON operations give it. */
export function contentBytes(base64: string): Uint8Array {
  return fromB64u(base64);
}

/** A message for a failure of the convention, in Rune's words. */
export function explain(error: unknown): string {
  return error instanceof E2EError ? error.message : friendly(error instanceof Error ? error.message : String(error));
}
