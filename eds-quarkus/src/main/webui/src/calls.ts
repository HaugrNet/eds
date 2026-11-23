import type { SocketFrame } from './api';
import { session } from './session';
import { socket } from './socket';
import { t } from './i18n';
import { text, utf8 } from './e2e/bytes';
import { base64Content, contentBytes, isChamberRoom, protect, reveal } from './e2e/rooms';
import { listEntries, usable } from './features/features';
import { presence, roomsWith } from './presence';
import {
  MAX_PEOPLE, RING_SECONDS, ROOM_CALL, RoomCalls, offers, readMessage, relayServers, writeMessage,
  type CallMessage, type Media, type RelayBody, type RelayServer,
} from './features/calls';

const FEATURE = 'calls';
/** The context of a signal of a call, as content of a chamber (E2E.md). */
const CONTEXT = 'calls|signal';

/** A message of a call as the signal which carries it: Base64, and in a chamber encrypted with its key before. */
export async function encodeSignal(circleId: string, message: Omit<CallMessage, 'v'>): Promise<string> {
  return base64Content(await protect(circleId, CONTEXT, utf8(writeMessage(message))));
}

/** The message a signal carries; null if it is none. Throws if it can't be read here: damaged, or of a chamber whose key this device hasn't. */
export async function decodeSignal(circleId: string, signal: string): Promise<CallMessage | null> {
  return readMessage(text(await reveal(circleId, CONTEXT, contentBytes(signal))));
}

/** Someone else in the call this device is in. */
export interface Participant {
  peer: string;
  memberId: string;
  accountName: string;
  /** What comes from them. */
  stream: MediaStream;
  state: 'connecting' | 'connected' | 'failed';
  mic: boolean;
  camera: boolean;
}

/** The call this device is in. */
export interface ActiveCall {
  /** `room`, or the UUID of a direct call. */
  id: string;
  circleId: string;
  direct: boolean;
  /** The other member of a direct call, and their name. */
  other?: string;
  otherName?: string;
  /** A direct call which rings at the other's; active once they took it. */
  phase: 'ringing' | 'active';
  /** What this device sends. */
  local: MediaStream;
  mic: boolean;
  camera: boolean;
  participants: Participant[];
}

/** A direct call which rings here. */
export interface IncomingCall {
  id: string;
  circleId: string;
  memberId: string;
  accountName: string;
  media: Media;
}

interface Link extends Participant {
  pc: RTCPeerConnection;
  /** Candidates which came before the description of the other side. */
  waiting: RTCIceCandidateInit[];
  described: boolean;
}

interface Session {
  id: string;
  circleId: string;
  direct: boolean;
  other?: string;
  otherName?: string;
  phase: 'ringing' | 'active';
  /** This device in the call. */
  peer: string;
  media: Media;
  local: MediaStream;
  servers: RelayServer[];
  links: Map<string, Link>;
  timer?: ReturnType<typeof setTimeout>;
}

/**
 * The calls of Rune (FEATURES.md, "Presence and calls"). A call is made by
 * the browser (WebRTC): sound and picture go from device to device, and
 * the stone only passes on what sets the call up, as signals of the feature
 * `calls`; in a chamber Rune encrypts them with the key of the chamber. At
 * most four people are in a call, each connected to each.
 *
 * Events: `change` (anything of the call this device is in, of the calls
 * ringing here, or of who is in the call of a room), `incoming` (a direct
 * call rings; detail is the IncomingCall), `missed` (it stopped before it
 * was taken), `started` (someone began the call of a room; detail has the
 * circleId, memberId, accountName and media), `ended` (the call this device
 * was in is over; detail has a `note` saying why, if there is one).
 */
export class Calls extends EventTarget {
  private current: Session | null = null;
  private ringing = new Map<string, IncomingCall & { timer: ReturnType<typeof setTimeout> }>();
  private roomCalls = new RoomCalls();
  /** The rooms asked whether a call goes on, since the connection came. */
  private probed = new Set<string>();
  /** Signals go out, and are read, in the order they came: encrypting and decrypting take a moment. */
  private outbox: Promise<unknown> = Promise.resolve();
  private inbox: Promise<unknown> = Promise.resolve();
  private listening = false;

  /** Listens to the socket; once per page. */
  start(): void {
    if (this.listening) {
      return;
    }
    this.listening = true;
    socket.addEventListener('signal', (event) => {
      const frame = (event as CustomEvent<SocketFrame>).detail;
      if (frame.feature === FEATURE) {
        this.inbox = this.inbox.then(() => this.signalled(frame)).catch(() => undefined);
      }
    });
    // A room joined on the socket is asked once whether a call goes on in it.
    socket.addEventListener('joined', (event) => {
      const circleId = (event as CustomEvent<{ circleId: string }>).detail.circleId;
      if (this.usable(circleId) && !this.probed.has(circleId)) {
        this.probed.add(circleId);
        void this.send(circleId, { t: 'probe', call: ROOM_CALL });
      }
      this.changed();
    });
    socket.addEventListener('left', (event) => {
      const circleId = (event as CustomEvent<SocketFrame>).detail.circleId ?? '';
      this.probed.delete(circleId);
      this.roomCalls.forget(circleId);
      if (this.current?.circleId === circleId) {
        this.finish(t('Your access to the room ended.'));
      }
      this.changed();
    });
    socket.addEventListener('state', (event) => {
      if (!(event as CustomEvent<boolean>).detail) {
        // Without the stone nothing can be set up, and the others see this device go.
        this.probed.clear();
        this.roomCalls.forget();
        for (const id of [...this.ringing.keys()]) {
          this.stopRinging(id);
        }
        this.finish(t('The connection to the stone was lost.'));
        this.changed();
      }
    });
    // Someone who is no longer present in a room has left its call.
    presence.addEventListener('change', (event) => {
      const { circleId, went } = (event as CustomEvent<{ circleId?: string; went?: string }>).detail;
      if (!circleId || !went) {
        this.changed();
        return;
      }
      this.roomCalls.memberWent(circleId, went);
      const call = this.current;
      if (call && call.circleId === circleId) {
        if (call.direct && call.other === went) {
          this.finish(t('{name} is no longer online.', { name: call.otherName ?? '' }));
        } else {
          for (const link of [...call.links.values()].filter((one) => one.memberId === went)) {
            this.drop(call, link.peer);
          }
        }
      }
      this.changed();
    });
    window.addEventListener('pagehide', () => this.hangUp());
  }

  /** Stops everything, at logout. */
  stop(): void {
    this.hangUp();
    for (const id of [...this.ringing.keys()]) {
      this.stopRinging(id);
    }
    this.roomCalls.forget();
    this.probed.clear();
  }

  // --- What is known -------------------------------------------------------------------

  /** True if this browser can make calls at all. */
  supported(): boolean {
    return typeof RTCPeerConnection !== 'undefined' && !!navigator.mediaDevices?.getUserMedia;
  }

  /** True if the room has calls, switched on. */
  usable(circleId: string): boolean {
    return usable(socket.featuresOf(circleId), FEATURE, true);
  }

  /** The call this device is in, if any. */
  active(): ActiveCall | null {
    const call = this.current;
    if (!call) {
      return null;
    }
    const video = call.local.getVideoTracks()[0];
    return {
      id: call.id, circleId: call.circleId, direct: call.direct, other: call.other, otherName: call.otherName, phase: call.phase,
      local: call.local, mic: call.local.getAudioTracks()[0]?.enabled ?? false, camera: !!video && video.readyState === 'live',
      participants: [...call.links.values()],
    };
  }

  /** The direct calls ringing here. */
  incoming(): IncomingCall[] {
    return [...this.ringing.values()];
  }

  /** The members in the call of a room; empty if none goes on. */
  inRoomCall(circleId: string): string[] {
    return this.roomCalls.members(circleId);
  }

  /** True if this device is in the call of the room. */
  inCallOf(circleId: string): boolean {
    return !!this.current && !this.current.direct && this.current.circleId === circleId;
  }

  /** The rooms shared with a member in which they can be called now: they are present there, and the room has calls. */
  private roomsToCall(memberId: string): string[] {
    return roomsWith(memberId).filter((circleId) => this.usable(circleId));
  }

  /** True if the member can be called now. */
  canCall(memberId: string | undefined): boolean {
    return !!memberId && memberId !== session()?.memberId && this.supported() && this.roomsToCall(memberId).length > 0;
  }

  // --- Entering and leaving ------------------------------------------------------------

  /** Asks anew whether a call goes on in a room, when the room is looked at. */
  look(circleId: string): void {
    if (this.usable(circleId) && socket.hasJoined(circleId) && !this.inCallOf(circleId)) {
      this.roomCalls.forget(circleId);
      this.probed.add(circleId);
      void this.send(circleId, { t: 'probe', call: ROOM_CALL });
      this.changed();
    }
  }

  /** Enters the call of a room, which begins with the first who does. */
  async joinRoom(circleId: string, media: Media): Promise<void> {
    if (this.inCallOf(circleId)) {
      return;
    }
    const call = await this.enter(circleId, ROOM_CALL, media);
    this.roomCalls.add(circleId, { peer: call.peer, memberId: session()?.memberId ?? '', media });
    await this.send(circleId, { t: 'join', call: ROOM_CALL, peer: call.peer, media });
    // Nobody answered: this device is the first, and the others are told that a call began.
    call.timer = setTimeout(() => {
      if (this.current === call && call.links.size === 0) {
        void this.send(circleId, { t: 'ring', call: ROOM_CALL, peer: call.peer, media });
      }
    }, 1500);
    this.changed();
  }

  /** Calls a member directly; it rings at their devices. */
  async callPerson(memberId: string, accountName: string, media: Media): Promise<void> {
    const rooms = this.roomsToCall(memberId);
    if (rooms.length === 0) {
      throw new Error(t('{name} cannot be called now: they are not online in a room with calls which you share.', { name: accountName }));
    }
    // A chamber before a room: there the stone can't read what sets the call up.
    const chambers = await Promise.all(rooms.map((circleId) => isChamberRoom(circleId).catch(() => false)));
    const circleId = rooms[chambers.indexOf(true)] ?? rooms[0];
    const call = await this.enter(circleId, crypto.randomUUID(), media, memberId, accountName);
    await this.send(circleId, { t: 'ring', call: call.id, peer: call.peer, media }, memberId);
    call.timer = setTimeout(() => {
      if (this.current === call && call.phase === 'ringing') {
        void this.send(circleId, { t: 'leave', call: call.id, peer: call.peer }, memberId);
        this.finish(t('{name} did not answer.', { name: accountName }));
      }
    }, RING_SECONDS * 1000);
    this.changed();
  }

  /** Takes a direct call which rings here. */
  async accept(id: string): Promise<void> {
    const ringing = this.ringing.get(id);
    if (!ringing) {
      return;
    }
    const call = await this.enter(ringing.circleId, id, ringing.media, ringing.memberId, ringing.accountName, 'active');
    this.stopRinging(id);
    await this.send(call.circleId, { t: 'join', call: id, peer: call.peer, media: ringing.media }, ringing.memberId);
    // The person's other devices stop ringing.
    void this.send(call.circleId, { t: 'taken', call: id }, session()?.memberId);
    this.changed();
  }

  /** Refuses a direct call which rings here. */
  decline(id: string): void {
    const ringing = this.ringing.get(id);
    if (ringing) {
      this.stopRinging(id);
      void this.send(ringing.circleId, { t: 'decline', call: id }, ringing.memberId);
      void this.send(ringing.circleId, { t: 'taken', call: id }, session()?.memberId);
      this.changed();
    }
  }

  /** Leaves the call; a direct call ends by that. */
  hangUp(): void {
    const call = this.current;
    if (call) {
      void this.send(call.circleId, { t: 'leave', call: call.id, peer: call.peer }, call.direct ? call.other : undefined);
      this.finish();
    }
  }

  /** Switches the microphone on or off. */
  setMic(on: boolean): void {
    const call = this.current;
    const track = call?.local.getAudioTracks()[0];
    if (call && track) {
      track.enabled = on;
      this.tellState(call);
      this.changed();
    }
  }

  /** Switches the camera on or off; switching it on asks the browser for it the first time. */
  async setCamera(on: boolean): Promise<void> {
    const call = this.current;
    if (!call) {
      return;
    }
    const old = call.local.getVideoTracks()[0];
    let track: MediaStreamTrack | null = null;
    if (on) {
      try {
        track = (await navigator.mediaDevices.getUserMedia({ video: true })).getVideoTracks()[0] ?? null;
      } catch {
        throw new Error(t('The browser did not allow the camera, or there is none.'));
      }
      if (this.current !== call) {
        track?.stop();
        return;
      }
    }
    if (old) {
      old.stop();
      call.local.removeTrack(old);
    }
    if (track) {
      call.local.addTrack(track);
    }
    // Every connection is set up for a picture; it gets one now, or none, without a new setup.
    await Promise.all([...call.links.values()].map((link) => sender(link.pc, 'video')?.replaceTrack(track).catch(() => undefined)));
    this.tellState(call);
    this.changed();
  }

  /** Enters a call: asks for the microphone, and the camera for a video call, and reads the relay of the room. */
  private async enter(circleId: string, id: string, media: Media, other?: string, otherName?: string, phase: 'ringing' | 'active' = other ? 'ringing' : 'active'): Promise<Session> {
    if (this.current) {
      throw new Error(t('You are in a call already.'));
    }
    if (!this.supported()) {
      throw new Error(t('This browser cannot make calls.'));
    }
    if (!this.usable(circleId)) {
      throw new Error(t('This room has no calls. An administrator of the room can switch them on under "Features".'));
    }
    let local: MediaStream;
    try {
      local = await navigator.mediaDevices.getUserMedia({ audio: true, video: media === 'video' });
    } catch {
      try {
        // Without a camera, or without leave to use it: with the voice alone.
        local = await navigator.mediaDevices.getUserMedia({ audio: true });
      } catch {
        throw new Error(t('A call needs the microphone, and the browser did not allow it.'));
      }
    }
    if (this.current) {
      local.getTracks().forEach((track) => track.stop());
      throw new Error(t('You are in a call already.'));
    }
    let servers: RelayServer[] = [];
    try {
      servers = relayServers((await listEntries<RelayBody>(circleId, FEATURE, { kind: 'relay', limit: 1 }))[0]?.body);
    } catch {
      // Without the relay of the room, the devices try each other directly.
    }
    const call: Session = { id, circleId, direct: !!other, other, otherName, phase, peer: crypto.randomUUID(), media, local, servers, links: new Map() };
    this.current = call;
    return call;
  }

  /** The call this device is in is over. */
  private finish(note?: string): void {
    const call = this.current;
    if (!call) {
      return;
    }
    this.current = null;
    clearTimeout(call.timer);
    for (const link of call.links.values()) {
      link.pc.close();
    }
    call.links.clear();
    call.local.getTracks().forEach((track) => track.stop());
    if (!call.direct) {
      this.roomCalls.remove(call.circleId, call.peer);
    }
    this.dispatchEvent(new CustomEvent('ended', { detail: { note } }));
    this.changed();
  }

  private stopRinging(id: string): void {
    const ringing = this.ringing.get(id);
    if (ringing) {
      clearTimeout(ringing.timer);
      this.ringing.delete(id);
    }
  }

  private changed(): void {
    this.dispatchEvent(new CustomEvent('change'));
  }

  // --- Signals -------------------------------------------------------------------------

  /** Sends a message of a call to the room, or to the devices of one member. */
  private send(circleId: string, message: Omit<CallMessage, 'v'>, to?: string): Promise<void> {
    const sent = this.outbox.then(async () => {
      socket.signal(circleId, FEATURE, await encodeSignal(circleId, message), to);
    });
    this.outbox = sent.catch(() => undefined);
    return sent;
  }

  private tellState(call: Session): void {
    const video = call.local.getVideoTracks()[0];
    void this.send(call.circleId, {
      t: 'state', call: call.id, peer: call.peer,
      mic: call.local.getAudioTracks()[0]?.enabled ?? false, camera: !!video && video.readyState === 'live',
    }, call.direct ? call.other : undefined);
  }

  private async signalled(frame: SocketFrame): Promise<void> {
    const circleId = frame.circleId;
    const from = frame.memberId;
    if (!circleId || !from || !frame.signal) {
      return;
    }
    let message: CallMessage | null;
    try {
      message = await decodeSignal(circleId, frame.signal);
    } catch {
      return;
    }
    if (message) {
      await this.handle(circleId, from, frame.accountName ?? '', message, frame.to ?? undefined);
      this.changed();
    }
  }

  private async handle(circleId: string, from: string, name: string, message: CallMessage, to: string | undefined): Promise<void> {
    const me = session()?.memberId ?? '';
    const call = this.current && this.current.id === message.call && this.current.circleId === circleId ? this.current : null;
    // The other of a direct call is the only one whose messages count in it.
    const party = !!call && (!call.direct || from === call.other);
    this.roomCalls.note(circleId, from, message);

    switch (message.t) {
      case 'ring':
        if (message.call === ROOM_CALL) {
          if (from !== me && !this.inCallOf(circleId)) {
            this.dispatchEvent(new CustomEvent('started', { detail: { circleId, memberId: from, accountName: name, media: message.media ?? 'audio' } }));
          }
        } else if (to === me && from !== me && !this.ringing.has(message.call)) {
          if (this.current) {
            // In a call already: the caller is told at once.
            void this.send(circleId, { t: 'decline', call: message.call }, from);
            return;
          }
          const id = message.call;
          const ringing = { id, circleId, memberId: from, accountName: name, media: message.media ?? 'audio', timer: setTimeout(() => this.missed(id), (RING_SECONDS + 5) * 1000) };
          this.ringing.set(id, ringing);
          this.dispatchEvent(new CustomEvent('incoming', { detail: ringing }));
        }
        break;
      case 'probe':
        if (call && !call.direct && message.call === ROOM_CALL) {
          void this.send(circleId, { t: 'ongoing', call: ROOM_CALL, peer: call.peer, media: call.media, peers: call.links.size + 1 }, from);
        }
        break;
      case 'taken':
        if (from === me && to === me) {
          this.stopRinging(message.call);
        }
        break;
      case 'decline':
        if (call?.direct && party && call.phase === 'ringing') {
          this.finish(t('{name} declined the call.', { name: call.otherName ?? name }));
        }
        break;
      case 'join':
        if (call && party && message.peer && message.peer !== call.peer) {
          if (call.links.size >= MAX_PEOPLE - 1 && !call.links.has(message.peer)) {
            void this.send(circleId, { t: 'full', call: call.id, peer: call.peer, target: message.peer }, from);
            return;
          }
          if (call.direct) {
            call.phase = 'active';
            clearTimeout(call.timer);
          }
          void this.send(circleId, { t: 'here', call: call.id, peer: call.peer, target: message.peer, media: call.media }, from);
          await this.meet(call, message.peer, from, name, message.media ?? 'audio');
        }
        break;
      case 'here':
        if (call && party && message.peer && message.target === call.peer) {
          await this.meet(call, message.peer, from, name, message.media ?? 'audio');
        }
        break;
      case 'full':
        if (call && party && message.target === call.peer && call.links.size === 0) {
          void this.send(circleId, { t: 'leave', call: call.id, peer: call.peer }, call.direct ? call.other : undefined);
          this.finish(t('The call is full: it has {n} people, which is the most.', { n: MAX_PEOPLE }));
        }
        break;
      case 'offer':
      case 'answer':
      case 'ice':
        if (call && party && message.peer && message.target === call.peer) {
          await this.negotiate(call, from, name, message);
        }
        break;
      case 'state': {
        const link = call && message.peer ? call.links.get(message.peer) : undefined;
        if (link && link.memberId === from) {
          link.mic = message.mic ?? link.mic;
          link.camera = message.camera ?? link.camera;
        }
        break;
      }
      case 'leave':
        if (this.ringing.get(message.call)?.memberId === from) {
          this.missed(message.call);
        } else if (call?.direct && party) {
          this.finish(t('{name} hung up.', { name: call.otherName ?? name }));
        } else if (call && message.peer && call.links.get(message.peer)?.memberId === from) {
          this.drop(call, message.peer);
        }
        break;
      default:
        break;
    }
  }

  /** A direct call stopped ringing before it was taken. */
  private missed(id: string): void {
    const ringing = this.ringing.get(id);
    if (ringing) {
      this.stopRinging(id);
      this.dispatchEvent(new CustomEvent('missed', { detail: ringing }));
      this.changed();
    }
  }

  // --- The connections, device to device -----------------------------------------------

  /** Another device is in the call: a connection to it is made, and offered if that falls to this device. */
  private async meet(call: Session, peer: string, memberId: string, accountName: string, media: Media): Promise<Link | null> {
    const known = call.links.get(peer);
    if (known) {
      return known.memberId === memberId ? known : null;
    }
    if (call.links.size >= MAX_PEOPLE - 1 || peer === call.peer) {
      return null;
    }
    const pc = new RTCPeerConnection({ iceServers: call.servers });
    const link: Link = { peer, memberId, accountName, pc, stream: new MediaStream(), state: 'connecting', mic: true, camera: media === 'video', waiting: [], described: false };
    call.links.set(peer, link);
    const to = memberId;
    pc.onicecandidate = (event) => {
      if (event.candidate && this.current === call) {
        void this.send(call.circleId, { t: 'ice', call: call.id, peer: call.peer, target: peer, candidate: event.candidate.toJSON() }, to);
      }
    };
    pc.ontrack = (event) => {
      if (!link.stream.getTracks().includes(event.track)) {
        link.stream.addTrack(event.track);
      }
      this.changed();
    };
    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'connected') {
        link.state = 'connected';
      } else if (pc.connectionState === 'failed') {
        link.state = 'failed';
      } else if (pc.connectionState === 'closed') {
        return;
      }
      this.changed();
    };
    if (offers(call.peer, peer)) {
      // Sound and picture both ways, whether or not a picture is sent yet.
      for (const kind of ['audio', 'video'] as const) {
        const track = call.local.getTracks().find((one) => one.kind === kind);
        pc.addTransceiver(track ?? kind, { direction: 'sendrecv', streams: [call.local] });
      }
      await pc.setLocalDescription(await pc.createOffer());
      void this.send(call.circleId, { t: 'offer', call: call.id, peer: call.peer, target: peer, sdp: pc.localDescription?.sdp ?? '' }, to);
    }
    // The other learns what this device sends.
    this.tellState(call);
    return link;
  }

  private async negotiate(call: Session, from: string, name: string, message: CallMessage): Promise<void> {
    const link = await this.meet(call, message.peer as string, from, name, 'audio');
    if (!link) {
      return;
    }
    const pc = link.pc;
    try {
      if (message.t === 'ice') {
        if (link.described) {
          await pc.addIceCandidate(message.candidate as RTCIceCandidateInit);
        } else {
          link.waiting.push(message.candidate as RTCIceCandidateInit);
        }
        return;
      }
      if (message.t === 'offer') {
        if (offers(call.peer, link.peer)) {
          return; // The offer is this device's to make.
        }
        await pc.setRemoteDescription({ type: 'offer', sdp: message.sdp });
        for (const transceiver of pc.getTransceivers()) {
          transceiver.direction = 'sendrecv';
          const track = call.local.getTracks().find((one) => one.kind === transceiver.receiver.track.kind);
          if (track) {
            await transceiver.sender.replaceTrack(track);
          }
        }
        await pc.setLocalDescription(await pc.createAnswer());
        void this.send(call.circleId, { t: 'answer', call: call.id, peer: call.peer, target: link.peer, sdp: pc.localDescription?.sdp ?? '' }, from);
      } else {
        await pc.setRemoteDescription({ type: 'answer', sdp: message.sdp });
      }
      link.described = true;
      for (const candidate of link.waiting.splice(0)) {
        await pc.addIceCandidate(candidate);
      }
    } catch {
      link.state = 'failed';
    }
  }

  /** A device left the call. */
  private drop(call: Session, peer: string): void {
    const link = call.links.get(peer);
    if (link) {
      link.pc.close();
      call.links.delete(peer);
    }
    if (!call.direct) {
      this.roomCalls.remove(call.circleId, peer);
    }
    this.changed();
  }
}

/** The sender of a kind of track of a connection. */
function sender(pc: RTCPeerConnection, kind: 'audio' | 'video'): RTCRtpSender | undefined {
  return pc.getTransceivers().find((transceiver) => transceiver.receiver.track.kind === kind)?.sender;
}

export const calls = new Calls();
