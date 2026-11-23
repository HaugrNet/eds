// The convention of Rune and Tolk for end-to-end encryption, version 1,
// suite 1 (E2E.md): P-384, HKDF-SHA-512 and AES-256-GCM, all of the
// browser's WebCrypto, so no library is needed. The stone takes no part in
// any of this: it stores what these functions make, without looking inside.
//
// Nothing here touches the page or the stone, so it runs in a browser, a
// worker and Node alike, and is tested against vectors.json, which an
// independent implementation made (vectors/e2e-v1.py).
import { concat, equal, fromB64u, fromBase32, random, text, toB64u, toBase32, toHex, utf8 } from './bytes.ts';

const PREFIX = 'eds-e2e/v1/';
const PUBLIC_KEY_PREFIX = 'eds-e2e:1:1:';
const MAGIC = utf8('EDSE');
const HEADER_LENGTH = 54;
const TAG_LENGTH = 16;
/** The chunk size writers use: 64 KiB. */
export const CHUNK_SIZE = 65536;
const MIN_CHUNK = 16;
const MAX_CHUNK = 16 * 1024 * 1024;

/** A failure of the convention: what was read isn't what it should be. */
export class E2EError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'E2EError';
  }
}

// --- Primitives --------------------------------------------------------------

/** WebCrypto wants a plain ArrayBuffer view. */
function buffer(bytes: Uint8Array): Uint8Array<ArrayBuffer> {
  return bytes.buffer instanceof ArrayBuffer && bytes.byteOffset === 0 && bytes.byteLength === bytes.buffer.byteLength
    ? (bytes as Uint8Array<ArrayBuffer>)
    : new Uint8Array(bytes);
}

/** HKDF-SHA-512 to an AES-256-GCM key. Without a salt, HKDF's default. */
async function hkdf(ikm: Uint8Array | CryptoKey, salt: Uint8Array | null, info: string): Promise<CryptoKey> {
  const base = ikm instanceof Uint8Array
    ? await crypto.subtle.importKey('raw', buffer(ikm), 'HKDF', false, ['deriveKey'])
    : ikm;
  return crypto.subtle.deriveKey(
    { name: 'HKDF', hash: 'SHA-512', salt: buffer(salt ?? new Uint8Array(0)), info: buffer(utf8(info)) },
    base,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt'],
  );
}

/** AES-256-GCM: nonce (12) | ciphertext | tag (16). */
async function seal(key: CryptoKey, aad: string, plain: Uint8Array, nonce: Uint8Array = random(12)): Promise<Uint8Array> {
  const sealed = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: buffer(nonce), additionalData: buffer(utf8(aad)) }, key, buffer(plain));
  return concat(nonce, new Uint8Array(sealed));
}

async function open(key: CryptoKey, aad: string, sealed: Uint8Array, what: string): Promise<Uint8Array> {
  if (sealed.length < 12 + TAG_LENGTH) {
    throw new E2EError(`${what} is too short.`);
  }
  try {
    return new Uint8Array(await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: buffer(sealed.subarray(0, 12)), additionalData: buffer(utf8(aad)) },
      key,
      buffer(sealed.subarray(12)),
    ));
  } catch {
    throw new E2EError(`${what} can't be opened: it is damaged, or not meant for this key.`);
  }
}

function parse(json: string, what: string): Record<string, unknown> {
  let value: unknown;
  try {
    value = JSON.parse(json);
  } catch {
    throw new E2EError(`${what} isn't of this convention.`);
  }
  if (typeof value !== 'object' || value === null || (value as Record<string, unknown>).e2e !== 1) {
    throw new E2EError(`${what} isn't of this convention.`);
  }
  if ((value as Record<string, unknown>).suite !== 1) {
    throw new E2EError(`${what} uses a suite this app doesn't know.`);
  }
  return value as Record<string, unknown>;
}

function field(value: Record<string, unknown>, name: string, what: string): string {
  const found = value[name];
  if (typeof found !== 'string') {
    throw new E2EError(`${what} is incomplete.`);
  }
  return found;
}

function version(value: Record<string, unknown>, what: string): number {
  const found = value.version;
  if (typeof found !== 'number' || !Number.isInteger(found) || found < 1) {
    throw new E2EError(`${what} is incomplete.`);
  }
  return found;
}

// --- The Member's keys --------------------------------------------------------

/** A Member's key pair: the private key for use, and the public point. */
export interface KeyPair {
  privateKey: CryptoKey;
  publicPoint: Uint8Array;
}

function jwk(scalar: Uint8Array | null, point: Uint8Array): JsonWebKey {
  if (point.length !== 97 || point[0] !== 4) {
    throw new E2EError('The public key is not a point of P-384.');
  }
  const key: JsonWebKey = { kty: 'EC', crv: 'P-384', x: toB64u(point.subarray(1, 49)), y: toB64u(point.subarray(49, 97)), ext: true };
  if (scalar) {
    key.d = toB64u(scalar);
  }
  return key;
}

/** The private key, from its scalar and its public point, for use only: it can't be exported. */
export async function importPrivateKey(scalar: Uint8Array, point: Uint8Array): Promise<CryptoKey> {
  try {
    return await crypto.subtle.importKey('jwk', jwk(scalar, point), { name: 'ECDH', namedCurve: 'P-384' }, false, ['deriveBits']);
  } catch {
    throw new E2EError('The private key does not fit its public key.');
  }
}

async function importPublicKey(point: Uint8Array): Promise<CryptoKey> {
  try {
    return await crypto.subtle.importKey('raw', buffer(point), { name: 'ECDH', namedCurve: 'P-384' }, false, []);
  } catch {
    throw new E2EError('The public key is not a point of P-384.');
  }
}

/** A new key pair, with the scalar of the private key, which goes into the vault and nowhere else. */
export async function generateKeyPair(): Promise<{ pair: KeyPair; scalar: Uint8Array }> {
  const generated = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-384' }, true, ['deriveBits']);
  const exported = await crypto.subtle.exportKey('jwk', generated.privateKey);
  const scalar = fromB64u(exported.d as string);
  const publicPoint = concat(Uint8Array.of(4), fromB64u(exported.x as string), fromB64u(exported.y as string));
  return { pair: { privateKey: await importPrivateKey(scalar, publicPoint), publicPoint }, scalar };
}

/** Member.publicKey: the convention, its version, the suite, and the point. */
export function publicKeyText(point: Uint8Array): string {
  return PUBLIC_KEY_PREFIX + toB64u(point);
}

/** The point of a Member.publicKey, or null if the key isn't of this convention. */
export function parsePublicKey(value: string | null | undefined): Uint8Array | null {
  if (!value || !value.startsWith(PUBLIC_KEY_PREFIX)) {
    return null;
  }
  try {
    const point = fromB64u(value.substring(PUBLIC_KEY_PREFIX.length));
    return point.length === 97 && point[0] === 4 ? point : null;
  } catch {
    return null;
  }
}

/** What two people compare: eight groups of four hex digits. */
export async function fingerprint(point: Uint8Array): Promise<string> {
  const digest = toHex(new Uint8Array(await crypto.subtle.digest('SHA-256', buffer(point)))).substring(0, 32);
  return digest.match(/.{4}/g)!.join(' ');
}

/** The account key as people write it down. */
export function recoveryKey(accountKey: Uint8Array): string {
  return toBase32(accountKey).match(/.{1,4}/g)!.join('-');
}

/** The account key of a recovery key as typed, or null if it isn't one. */
export function parseRecoveryKey(typed: string): Uint8Array | null {
  const bytes = fromBase32(typed);
  return bytes && bytes.length === 32 ? bytes : null;
}

/** The fragment which gives a further device the account key. */
export function deviceLink(accountKey: Uint8Array): string {
  return '#device=' + toB64u(accountKey);
}

export function parseDeviceLink(hash: string): Uint8Array | null {
  if (!hash.startsWith('#device=')) {
    return null;
  }
  try {
    const bytes = fromB64u(hash.substring(8));
    return bytes.length === 32 ? bytes : null;
  } catch {
    return null;
  }
}

/** The account key as a key which can only derive, and can't be read again. */
export function importAccountKey(accountKey: Uint8Array): Promise<CryptoKey> {
  return crypto.subtle.importKey('raw', buffer(accountKey), 'HKDF', false, ['deriveKey']);
}

/** Member.vault: the private key under the account key. */
export async function sealVault(accountKey: Uint8Array | CryptoKey, memberId: string, scalar: Uint8Array,
                                salt: Uint8Array = random(32), nonce?: Uint8Array): Promise<string> {
  const label = `${PREFIX}vault|${memberId}`;
  const key = await hkdf(accountKey, salt, label);
  return JSON.stringify({ e2e: 1, suite: 1, salt: toB64u(salt), key: toB64u(await seal(key, label, scalar, nonce)) });
}

/** The scalar of the private key, from the vault. */
export async function openVault(vault: string, accountKey: Uint8Array | CryptoKey, memberId: string): Promise<Uint8Array> {
  const value = parse(vault, 'The vault');
  const label = `${PREFIX}vault|${memberId}`;
  const key = await hkdf(accountKey, fromB64u(field(value, 'salt', 'The vault')), label);
  const scalar = await open(key, label, fromB64u(field(value, 'key', 'The vault')), 'The vault');
  if (scalar.length !== 48) {
    throw new E2EError('The vault does not hold a private key.');
  }
  return scalar;
}

// --- The room key --------------------------------------------------------------

/** A room key with its version. */
export interface RoomKey {
  key: Uint8Array;
  version: number;
}

/** For the tests: the ephemeral key and the nonce which are random otherwise. */
export interface Fixed {
  ephemeral?: { scalar: Uint8Array; point: Uint8Array };
  nonce?: Uint8Array;
}

/** Trustee.secret: the room key wrapped to a Member's public key. */
export async function wrapToKey(room: RoomKey, circleId: string, memberId: string, memberPoint: Uint8Array, fixed: Fixed = {}): Promise<string> {
  let ephemeralKey: CryptoKey;
  let epk: Uint8Array;
  if (fixed.ephemeral) {
    ephemeralKey = await importPrivateKey(fixed.ephemeral.scalar, fixed.ephemeral.point);
    epk = fixed.ephemeral.point;
  } else {
    const generated = await generateKeyPair();
    ephemeralKey = generated.pair.privateKey;
    epk = generated.pair.publicPoint;
    generated.scalar.fill(0);
  }
  const shared = new Uint8Array(await crypto.subtle.deriveBits({ name: 'ECDH', public: await importPublicKey(memberPoint) }, ephemeralKey, 384));
  const label = `${PREFIX}secret|${circleId}|${memberId}|${room.version}`;
  const key = await hkdf(shared, concat(epk, memberPoint), label);
  shared.fill(0);
  return JSON.stringify({ e2e: 1, suite: 1, by: 'key', version: room.version, epk: toB64u(epk), key: toB64u(await seal(key, label, room.key, fixed.nonce)) });
}

/** Trustee.secret of an invitation: the room key under the invite secret of the link. */
export async function wrapForInvite(room: RoomKey, circleId: string, name: string, inviteSecret: Uint8Array, fixed: Fixed = {}): Promise<string> {
  const label = `${PREFIX}invite|${circleId}|${name}|${room.version}`;
  const key = await hkdf(inviteSecret, null, label);
  return JSON.stringify({ e2e: 1, suite: 1, by: 'invite', version: room.version, key: toB64u(await seal(key, label, room.key, fixed.nonce)) });
}

/** How a secret is wrapped: to the Member's key, or under an invite secret. Null if it isn't of this convention. */
export function secretKind(secret: string | null | undefined): 'key' | 'invite' | null {
  if (!secret) {
    return null;
  }
  try {
    const by = parse(secret, 'The secret').by;
    return by === 'key' || by === 'invite' ? by : null;
  } catch {
    return null;
  }
}

/** The version of the room key in a secret, without opening it; 0 if it isn't of this convention. */
export function secretVersion(secret: string | null | undefined): number {
  if (!secret) {
    return 0;
  }
  try {
    return version(parse(secret, 'The secret'), 'The secret');
  } catch {
    return 0;
  }
}

/** The room key of a secret which was wrapped to the Member's public key. */
export async function openSecretWithKey(secret: string, pair: KeyPair, circleId: string, memberId: string): Promise<RoomKey> {
  const value = parse(secret, 'The secret');
  if (value.by !== 'key') {
    throw new E2EError('The secret is not wrapped to a key.');
  }
  const roomVersion = version(value, 'The secret');
  const epk = fromB64u(field(value, 'epk', 'The secret'));
  const shared = new Uint8Array(await crypto.subtle.deriveBits({ name: 'ECDH', public: await importPublicKey(epk) }, pair.privateKey, 384));
  const label = `${PREFIX}secret|${circleId}|${memberId}|${roomVersion}`;
  const key = await hkdf(shared, concat(epk, pair.publicPoint), label);
  shared.fill(0);
  return { key: roomKey(await open(key, label, fromB64u(field(value, 'key', 'The secret')), 'The secret')), version: roomVersion };
}

/** The room key of the secret of an invitation. */
export async function openSecretWithInvite(secret: string, inviteSecret: Uint8Array, circleId: string, name: string): Promise<RoomKey> {
  const value = parse(secret, 'The secret');
  if (value.by !== 'invite') {
    throw new E2EError('The secret is not of an invitation.');
  }
  const roomVersion = version(value, 'The secret');
  const label = `${PREFIX}invite|${circleId}|${name}|${roomVersion}`;
  const key = await hkdf(inviteSecret, null, label);
  return { key: roomKey(await open(key, label, fromB64u(field(value, 'key', 'The secret')), 'The secret')), version: roomVersion };
}

function roomKey(key: Uint8Array): Uint8Array {
  if (key.length !== 32) {
    throw new E2EError('The secret does not hold a room key.');
  }
  return key;
}

/** Circle.circleKey: marks the chamber, and holds the earlier room keys under the current one. */
export async function sharedValue(circleId: string, current: RoomKey, earlier: RoomKey[], nonces: Record<number, Uint8Array> = {}): Promise<string> {
  const base = `${PREFIX}earlier|${circleId}|${current.version}`;
  const key = await hkdf(current.key, null, base);
  const sealed: Record<string, string> = {};
  for (const old of [...earlier].sort((a, b) => a.version - b.version)) {
    sealed[String(old.version)] = toB64u(await seal(key, `${base}|${old.version}`, old.key, nonces[old.version]));
  }
  return JSON.stringify({ e2e: 1, suite: 1, version: current.version, earlier: sealed });
}

/** True if a Circle's shared value says that it is a chamber of this convention. */
export function isChamber(value: string | null | undefined): boolean {
  if (!value) {
    return false;
  }
  try {
    return (JSON.parse(value) as Record<string, unknown>).e2e === 1;
  } catch {
    return false;
  }
}

/** The version of the room key which a chamber's shared value names; 0 if it isn't a chamber. */
export function chamberVersion(value: string | null | undefined): number {
  if (!value) {
    return 0;
  }
  try {
    return version(parse(value, 'The room'), 'The room');
  } catch {
    return 0;
  }
}

/** All room keys of a chamber, by their versions: the current one, and the earlier ones from the shared value. */
export async function openSharedValue(value: string, circleId: string, current: RoomKey): Promise<Map<number, Uint8Array>> {
  const parsed = parse(value, 'The room');
  const keys = new Map<number, Uint8Array>([[current.version, current.key]]);
  if (version(parsed, 'The room') !== current.version) {
    // The room's key was changed, and this Member's secret isn't of the new one yet.
    return keys;
  }
  const base = `${PREFIX}earlier|${circleId}|${current.version}`;
  const key = await hkdf(current.key, null, base);
  const earlier = (parsed.earlier ?? {}) as Record<string, unknown>;
  for (const [oldVersion, sealed] of Object.entries(earlier)) {
    if (typeof sealed === 'string' && /^[1-9][0-9]*$/.test(oldVersion)) {
      keys.set(Number(oldVersion), roomKey(await open(key, `${base}|${oldVersion}`, fromB64u(sealed), 'An earlier key of the room')));
    }
  }
  return keys;
}

// --- The invitation link --------------------------------------------------------

/** What an invitation link carries: the name, the stone's token, and for a chamber the invite secret. */
export interface Invitation {
  name: string;
  token: string;
  secret?: Uint8Array;
}

export function invitationFragment(invitation: Invitation): string {
  const payload: Record<string, string> = { n: invitation.name, t: invitation.token };
  if (invitation.secret) {
    payload.k = toB64u(invitation.secret);
  }
  return '#invite=' + toB64u(utf8(JSON.stringify(payload)));
}

export function parseInvitationFragment(hash: string): Invitation | null {
  if (!hash.startsWith('#invite=')) {
    return null;
  }
  try {
    const value = JSON.parse(text(fromB64u(hash.substring(8)))) as Record<string, unknown>;
    if (typeof value.n !== 'string' || typeof value.t !== 'string') {
      return null;
    }
    const invitation: Invitation = { name: value.n, token: value.t };
    if (typeof value.k === 'string') {
      const secret = fromB64u(value.k);
      if (secret.length !== 32) {
        return null;
      }
      invitation.secret = secret;
    }
    return invitation;
  } catch {
    return null;
  }
}

// --- Content ---------------------------------------------------------------------

/** For the tests: what is random otherwise, and a chunk size other than the usual. */
export interface ContentOptions {
  chunkSize?: number;
  salt?: Uint8Array;
  noncePrefix?: Uint8Array;
}

function uint32(value: number): Uint8Array {
  const bytes = new Uint8Array(4);
  new DataView(bytes.buffer).setUint32(0, value);
  return bytes;
}

function contentHeader(roomVersion: number, chunkSize: number, salt: Uint8Array, prefix: Uint8Array): Uint8Array {
  return concat(MAGIC, Uint8Array.of(1, 1), uint32(roomVersion), Uint8Array.of(1), uint32(chunkSize), salt, prefix);
}

function contentKey(room: Uint8Array, salt: Uint8Array, circleId: string, roomVersion: number): Promise<CryptoKey> {
  return hkdf(room, salt, `${PREFIX}content|${circleId}|${roomVersion}`);
}

function chunkNonce(prefix: Uint8Array, index: number, last: boolean): Uint8Array {
  return concat(prefix, uint32(index), Uint8Array.of(last ? 1 : 0));
}

/** The length of a content once it is encrypted. */
export function encryptedLength(plainLength: number, chunkSize: number = CHUNK_SIZE): number {
  const chunks = Math.max(1, Math.ceil(plainLength / chunkSize));
  return HEADER_LENGTH + plainLength + chunks * TAG_LENGTH;
}

/** True if the bytes start as content of this convention does. */
export function isProtected(start: Uint8Array): boolean {
  return start.length >= MAGIC.length && equal(start.subarray(0, MAGIC.length), MAGIC);
}

/** Seals the chunks of one content, in their order; the last one is told as such. */
export interface ContentWriter {
  /** What the content starts with. */
  header: Uint8Array;
  chunkSize: number;
  seal(index: number, last: boolean, chunk: Uint8Array): Promise<Uint8Array>;
}

/**
 * Starts the encryption of a file, a thumbnail or an entry of a feature, chunk
 * by chunk, so a large file needn't be in memory. The context says what it
 * is: empty for a file, "thumbnail", or "<feature>|<kind>".
 */
export async function contentWriter(room: RoomKey, circleId: string, context: string, options: ContentOptions = {}): Promise<ContentWriter> {
  const chunkSize = options.chunkSize ?? CHUNK_SIZE;
  const salt = options.salt ?? random(32);
  const prefix = options.noncePrefix ?? random(7);
  const header = contentHeader(room.version, chunkSize, salt, prefix);
  const key = await contentKey(room.key, salt, circleId, room.version);
  const aad = buffer(concat(header, utf8(`|${circleId}|${context}`)));
  return {
    header,
    chunkSize,
    seal: async (index, last, chunk) => new Uint8Array(await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv: buffer(chunkNonce(prefix, index, last)), additionalData: aad }, key, buffer(chunk))),
  };
}

/** How many chunks a content of this length has: at least one, which may be empty. */
export function chunkCount(plainLength: number, chunkSize: number = CHUNK_SIZE): number {
  return Math.max(1, Math.ceil(plainLength / chunkSize));
}

/** Encrypts a content which is in memory. */
export async function encryptContent(room: RoomKey, circleId: string, context: string, plain: Uint8Array, options: ContentOptions = {}): Promise<Uint8Array> {
  const writer = await contentWriter(room, circleId, context, options);
  const count = chunkCount(plain.length, writer.chunkSize);
  const parts: Uint8Array[] = [writer.header];
  for (let i = 0; i < count; i++) {
    parts.push(await writer.seal(i, i === count - 1, plain.subarray(i * writer.chunkSize, (i + 1) * writer.chunkSize)));
  }
  return concat(...parts);
}

/** What the header of an encrypted content says. */
export interface ContentHeader {
  roomVersion: number;
  chunkSize: number;
  salt: Uint8Array;
  noncePrefix: Uint8Array;
  header: Uint8Array;
}

/** Reads the header of an encrypted content. */
export function readContentHeader(data: Uint8Array): ContentHeader {
  if (!isProtected(data)) {
    throw new E2EError('The content is not protected: it was not written by an app which knows chambers.');
  }
  if (data.length < HEADER_LENGTH + TAG_LENGTH) {
    throw new E2EError('The content is too short.');
  }
  if (data[4] !== 1 || data[10] !== 1) {
    throw new E2EError('The content is of a format this app does not know.');
  }
  if (data[5] !== 1) {
    throw new E2EError('The content uses a suite this app does not know.');
  }
  const view = new DataView(data.buffer, data.byteOffset, data.byteLength);
  const chunkSize = view.getUint32(11);
  if (chunkSize < MIN_CHUNK || chunkSize > MAX_CHUNK) {
    throw new E2EError('The content is of a format this app does not know.');
  }
  return {
    roomVersion: view.getUint32(6),
    chunkSize,
    salt: data.subarray(15, 47),
    noncePrefix: data.subarray(47, 54),
    header: data.subarray(0, HEADER_LENGTH),
  };
}

/** Opens the chunks of one content, in their order. */
export interface ContentReader {
  /** The length of the header, after which the chunks follow. */
  offset: number;
  /** The length of a sealed chunk which isn't the last. */
  step: number;
  /** How many chunks a content of this stored length has. */
  count(storedLength: number): number;
  open(index: number, last: boolean, chunk: Uint8Array): Promise<Uint8Array>;
}

/**
 * Starts the decryption of a file, a thumbnail or an entry of a feature, chunk
 * by chunk. The keys are the room keys of the chamber by their versions;
 * the context is what the reader expects the content to be; `start` is the
 * beginning of the content, with at least its header and one tag.
 */
export async function contentReader(keys: Map<number, Uint8Array>, circleId: string, context: string, start: Uint8Array): Promise<ContentReader> {
  const header = readContentHeader(start);
  const room = keys.get(header.roomVersion);
  if (!room) {
    throw new E2EError(`The content was written under key ${header.roomVersion} of the room, which this device does not have.`);
  }
  const key = await contentKey(room, header.salt, circleId, header.roomVersion);
  const aad = buffer(concat(header.header.slice(), utf8(`|${circleId}|${context}`)));
  const prefix = header.noncePrefix.slice();
  const step = header.chunkSize + TAG_LENGTH;
  return {
    offset: HEADER_LENGTH,
    step,
    count: (storedLength) => Math.max(1, Math.ceil((storedLength - HEADER_LENGTH) / step)),
    open: async (index, last, chunk) => {
      // A chunk which isn't the last is full; the last one may be shorter.
      if (chunk.length < TAG_LENGTH || (!last && chunk.length !== step)) {
        throw new E2EError('The content is cut off.');
      }
      try {
        return new Uint8Array(await crypto.subtle.decrypt(
          { name: 'AES-GCM', iv: buffer(chunkNonce(prefix, index, last)), additionalData: aad }, key, buffer(chunk)));
      } catch {
        throw new E2EError("The content can't be opened: it is damaged, cut off, or not what it is presented as.");
      }
    },
  };
}

/** The length of a content once it is decrypted, from its stored length; for the usual chunk size. */
export function plainLength(storedLength: number, chunkSize: number = CHUNK_SIZE): number {
  const body = storedLength - HEADER_LENGTH;
  if (body < TAG_LENGTH) {
    return 0;
  }
  return body - Math.max(1, Math.ceil(body / (chunkSize + TAG_LENGTH))) * TAG_LENGTH;
}

/** Decrypts a content which is in memory. */
export async function decryptContent(keys: Map<number, Uint8Array>, circleId: string, context: string, data: Uint8Array): Promise<Uint8Array> {
  const reader = await contentReader(keys, circleId, context, data);
  const count = reader.count(data.length);
  const parts: Uint8Array[] = [];
  for (let i = 0; i < count; i++) {
    parts.push(await reader.open(i, i === count - 1, data.subarray(reader.offset + i * reader.step, reader.offset + (i + 1) * reader.step)));
  }
  return concat(...parts);
}
