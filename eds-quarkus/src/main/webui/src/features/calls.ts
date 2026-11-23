/**
 * The feature "calls" (FEATURES.md, "Presence and calls"): voice and video
 * between the people of a room, or between two of them. Here: the messages
 * which set a call up, as the signals of the feature carry them, the rule of
 * who offers, and the relay of a room. Nothing here touches the page, the
 * stone or WebRTC; that is calls.ts.
 */

/** A call has at most this many people: the devices connect to each other, everyone to everyone. */
export const MAX_PEOPLE = 4;

/** How long a direct call rings before the caller gives up. */
export const RING_SECONDS = 45;

/** The `call` of the call of a room; a room has one at a time. A direct call has a UUID. */
export const ROOM_CALL = 'room';

export type Media = 'audio' | 'video';

export type MessageType = 'ring' | 'join' | 'here' | 'full' | 'offer' | 'answer' | 'ice' | 'leave' | 'decline' | 'taken' | 'probe' | 'ongoing' | 'state';

/** A message of a call, version 1. Fields a type doesn't use are left out. */
export interface CallMessage {
  v: 1;
  t: MessageType;
  /** `room`, or the UUID of a direct call. */
  call: string;
  /** The device which sends: a UUID it made for itself when it entered the call. */
  peer?: string;
  /** The device the message is for. */
  target?: string;
  media?: Media;
  /** The session description of WebRTC, of an offer or an answer. */
  sdp?: string;
  /** A candidate of ICE, as WebRTC gives it. */
  candidate?: { candidate?: string; sdpMid?: string | null; sdpMLineIndex?: number | null; usernameFragment?: string | null };
  /** `ongoing`: how many devices are in the call. */
  peers?: number;
  /** `state`: whether the device sends sound and picture now. */
  mic?: boolean;
  camera?: boolean;
}

const TYPES: MessageType[] = ['ring', 'join', 'here', 'full', 'offer', 'answer', 'ice', 'leave', 'decline', 'taken', 'probe', 'ongoing', 'state'];
/** The types which come from a device in a call, and so name it. */
const FROM_A_PEER: MessageType[] = ['join', 'here', 'full', 'offer', 'answer', 'ice', 'ongoing', 'state'];
/** The types which are for one device. */
const FOR_A_PEER: MessageType[] = ['here', 'full', 'offer', 'answer', 'ice'];

const ID = /^[0-9a-fA-F-]{8,64}$/;

/** The text of a message, as a signal carries it before it is encrypted or encoded. */
export function writeMessage(message: Omit<CallMessage, 'v'>): string {
  return JSON.stringify({ v: 1, ...message });
}

/**
 * Reads a message; null if the text isn't one this version knows, or lacks
 * what its type needs. Fields it doesn't know are passed over.
 */
export function readMessage(text: string): CallMessage | null {
  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch {
    return null;
  }
  if (!value || typeof value !== 'object') {
    return null;
  }
  const raw = value as Record<string, unknown>;
  const type = raw.t as MessageType;
  if (raw.v !== 1 || !TYPES.includes(type) || typeof raw.call !== 'string' || !(raw.call === ROOM_CALL || ID.test(raw.call))) {
    return null;
  }
  const id = (field: unknown): string | undefined => (typeof field === 'string' && ID.test(field) ? field : undefined);
  const message: CallMessage = { v: 1, t: type, call: raw.call };
  message.peer = id(raw.peer);
  message.target = id(raw.target);
  if (raw.media === 'audio' || raw.media === 'video') {
    message.media = raw.media;
  }
  if (typeof raw.sdp === 'string') {
    message.sdp = raw.sdp;
  }
  if (raw.candidate && typeof raw.candidate === 'object') {
    const candidate = raw.candidate as Record<string, unknown>;
    message.candidate = {
      candidate: typeof candidate.candidate === 'string' ? candidate.candidate : undefined,
      sdpMid: typeof candidate.sdpMid === 'string' ? candidate.sdpMid : null,
      sdpMLineIndex: typeof candidate.sdpMLineIndex === 'number' ? candidate.sdpMLineIndex : null,
      usernameFragment: typeof candidate.usernameFragment === 'string' ? candidate.usernameFragment : null,
    };
  }
  if (typeof raw.peers === 'number' && Number.isInteger(raw.peers) && raw.peers >= 0) {
    message.peers = raw.peers;
  }
  if (typeof raw.mic === 'boolean') {
    message.mic = raw.mic;
  }
  if (typeof raw.camera === 'boolean') {
    message.camera = raw.camera;
  }
  if ((FROM_A_PEER.includes(type) && !message.peer) || (FOR_A_PEER.includes(type) && !message.target)
      || ((type === 'offer' || type === 'answer') && !message.sdp) || (type === 'ice' && !message.candidate)) {
    return null;
  }
  return message;
}

/** Of two devices in a call, the one whose `peer` is the smaller text sends the offer. */
export function offers(mine: string, theirs: string): boolean {
  return mine < theirs;
}

// --- The relay of a room ---------------------------------------------------------------

/** A STUN or TURN server, as WebRTC takes it. */
export interface RelayServer { urls: string[]; username?: string; credential?: string }

/** The entry of the kind `relay`: the servers of a room. */
export interface RelayBody { v: 1; servers: RelayServer[] }

const SERVER_URL = /^(stun|stuns|turn|turns):[A-Za-z0-9.\-[\]:]+(\?transport=(udp|tcp))?$/;

/**
 * Reads the servers as an administrator writes them, one per line: the
 * address (`stun:host:3478`, `turn:host:3478`, `turns:host:5349`), and for a
 * TURN server its user name and its credential after it. Throws with the
 * line which isn't one.
 */
export function parseServers(text: string): RelayServer[] {
  const servers: RelayServer[] = [];
  for (const line of text.split(/\r?\n/).map((one) => one.trim()).filter((one) => one.length > 0)) {
    const [url, username, ...credential] = line.split(/\s+/);
    if (!SERVER_URL.test(url)) {
      throw new Error(line);
    }
    const server: RelayServer = { urls: [url] };
    if (username) {
      server.username = username;
      server.credential = credential.join(' ');
    }
    servers.push(server);
  }
  return servers;
}

/** The servers as lines, as `parseServers` reads them. */
export function writeServers(servers: RelayServer[]): string {
  return servers.flatMap((server) => server.urls.map((url) => [url, server.username, server.credential].filter((part) => part !== undefined && part !== '').join(' '))).join('\n');
}

/** The servers of an entry, as far as they are well-formed; what isn't is left out. */
export function relayServers(body: unknown): RelayServer[] {
  const servers = (body as RelayBody | null)?.servers;
  if (!Array.isArray(servers)) {
    return [];
  }
  return servers.flatMap((server) => {
    const urls = (Array.isArray(server?.urls) ? server.urls : [server?.urls]).filter((url: unknown): url is string => typeof url === 'string' && SERVER_URL.test(url));
    if (urls.length === 0) {
      return [];
    }
    const one: RelayServer = { urls };
    if (typeof server.username === 'string' && typeof server.credential === 'string') {
      one.username = server.username;
      one.credential = server.credential;
    }
    return [one];
  });
}

// --- Who is in the call of a room, as someone outside it knows ---------------------------

/** A device in a call. */
export interface CallPeer { peer: string; memberId: string; media: Media }

/**
 * The devices in the calls of the rooms, as their messages tell: a `join`
 * and an `ongoing` add one, a `leave` takes one away. Rune keeps it for
 * every room, to say that a call goes on and who is in it.
 */
export class RoomCalls {
  private rooms = new Map<string, Map<string, CallPeer>>();

  /** Notes a message of the call of a room; true if it changed who is in it. */
  note(circleId: string, memberId: string, message: CallMessage): boolean {
    if (message.call !== ROOM_CALL) {
      return false;
    }
    if ((message.t === 'join' || message.t === 'ongoing' || message.t === 'here') && message.peer) {
      return this.add(circleId, { peer: message.peer, memberId, media: message.media ?? 'audio' });
    }
    if (message.t === 'leave' && message.peer) {
      return this.remove(circleId, message.peer);
    }
    return false;
  }

  add(circleId: string, peer: CallPeer): boolean {
    let peers = this.rooms.get(circleId);
    if (!peers) {
      peers = new Map();
      this.rooms.set(circleId, peers);
    }
    const known = peers.has(peer.peer);
    peers.set(peer.peer, peer);
    return !known;
  }

  remove(circleId: string, peer: string): boolean {
    return this.rooms.get(circleId)?.delete(peer) ?? false;
  }

  /** A member is no longer present in the room: their devices left the call. */
  memberWent(circleId: string, memberId: string): boolean {
    const peers = this.rooms.get(circleId);
    let changed = false;
    for (const [id, peer] of peers ?? []) {
      if (peer.memberId === memberId) {
        peers?.delete(id);
        changed = true;
      }
    }
    return changed;
  }

  /** Forgets what is known of a room, or of all. */
  forget(circleId?: string): void {
    if (circleId) {
      this.rooms.delete(circleId);
    } else {
      this.rooms.clear();
    }
  }

  peers(circleId: string): CallPeer[] {
    return [...(this.rooms.get(circleId)?.values() ?? [])];
  }

  /** The members in the call of a room, each once. */
  members(circleId: string): string[] {
    return [...new Set(this.peers(circleId).map((peer) => peer.memberId))];
  }
}
