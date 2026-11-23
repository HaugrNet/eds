import { FeatureState, type CircleFeature, type Entry, type SocketFrame } from './api';
import { session } from './session';
import { socket } from './socket';
import { t } from './i18n';
import { addEntry, changeEntry, completeEntry, decodeEntry, listEntries, allEntries, offerFeature, offeredFeatures, switchFeature, usable, type FeatureEntry } from './features/features';
import { noteGiven, type Given } from './reactions';

/** A chat message, as the chat pane shows it. */
export interface ChatMessage {
  messageId?: string;
  circleId?: string;
  memberId?: string;
  accountName?: string;
  time?: string;
  edited?: string;
  replyTo?: string;
  text?: string;
  dataId?: string;
  /** Who gave which reaction: the reaction, and the IDs of the members. */
  reactions?: { [key: string]: string[] };
  /** The message can't be read: damaged, or in a chamber whose key this device hasn't. */
  damaged?: boolean;
  /** Its place in the chat, as the stone counts. */
  position?: number;
}

/** How far a member's client received, and the member read, the chat of a room. */
export interface ChatReceipt {
  memberId?: string;
  accountName?: string;
  readPosition?: number;
  receivedPosition?: number;
}

/** A member is writing. */
export interface ChatTyping {
  circleId?: string;
  memberId?: string;
  accountName?: string;
}

interface MessageBody { v: number; text?: string; reply?: string; file?: string }
interface ReactionBody { v: number; reactions?: string[] }
interface ReceiptBody { v: number; received?: number; read?: number }

const FEATURE = 'chat';

/** What Rune knows of the chat of one room. */
interface Room {
  features: CircleFeature[];
  messages: Map<string, ChatMessage>;
  /** The reactions of each member to each message: message → member → what they gave. */
  reactions: Map<string, Map<string, Given>>;
  reactionsLoaded: boolean;
  /** The member's own marker. */
  received: number;
  read: number;
  newest: number;
}

/**
 * The chat of a room (FEATURES.md). It is a feature: the stone keeps entries of
 * the kinds message, reaction and receipt, and knows nothing of a chat;
 * what a message holds is Rune's, and in a chamber the stone can't read it.
 * This class turns the entries and their announcements into the chat the
 * panes show, over the one socket of the app (socket.ts).
 *
 * Events: `message` (a new message, also the own), `edited` (a message's
 * text or reactions changed), `deleted` (a message was deleted), `receipt`
 * (a member received or read up to a place), `typing` (a member writes),
 * `left` (access to a room ended), `changed` (the files of a joined room
 * changed), `feature` (the features of a room changed), `state` (connected or
 * not).
 */
export class ChatClient extends EventTarget {
  private rooms = new Map<string, Room>();
  /** Rooms with a "received" on its way. */
  private receiving = new Set<string>();
  private listening = false;

  get connected(): boolean {
    return socket.connected;
  }

  connect(): Promise<void> {
    this.listen();
    return socket.connect();
  }

  close(): void {
    this.rooms.clear();
    socket.close();
  }

  /** How the chat stands in a room: on, read-only, off, or not there at all. */
  state(circleId: string): 'on' | 'readonly' | 'off' | 'none' {
    const state = this.rooms.get(circleId)?.features.find((feature) => feature.name === FEATURE)?.state;
    return state === FeatureState.On ? 'on' : state === FeatureState.Readonly ? 'readonly' : state === FeatureState.Off ? 'off' : 'none';
  }

  /** Joins a room; resolves with its unread messages. */
  async join(circleId: string): Promise<number> {
    await this.connect();
    // A room without the chat stays without: its features are chosen when it is made, and under "Features" later.
    const features = await socket.join(circleId);
    const room = this.room(circleId);
    room.features = features;
    if (!usable(features, FEATURE)) {
      return 0;
    }
    const me = session()?.memberId;
    const receipts = await listEntries<ReceiptBody>(circleId, FEATURE, { kind: 'receipt', limit: 200 });
    const own = receipts.find((receipt) => receipt.memberId === me);
    room.read = own?.body?.read ?? 0;
    room.received = own?.body?.received ?? 0;
    const unread = await socket.count(circleId, FEATURE, 'message', room.read);
    if (unread > 0) {
      // Messages sent while away: this client knows of them now.
      this.received(circleId);
    }
    return unread;
  }

  async leave(circleId: string): Promise<void> {
    this.rooms.delete(circleId);
    await socket.leave(circleId);
  }

  async send(circleId: string, text: string, dataId?: string, replyTo?: string): Promise<ChatMessage> {
    await this.connect();
    const body: MessageBody = { v: 1, text };
    if (replyTo) {
      body.reply = replyTo;
    }
    if (dataId) {
      body.file = dataId;
    }
    return this.message(await addEntry<MessageBody>(circleId, FEATURE, 'message', body));
  }

  /** Gives a reaction to a message, or takes it back. */
  async react(circleId: string, messageId: string, reaction: string): Promise<void> {
    await this.connect();
    const me = session()?.memberId ?? '';
    await this.loadReactions(circleId);
    const room = this.room(circleId);
    const mine = room.reactions.get(messageId)?.get(me)?.reactions ?? [];
    const next = mine.includes(reaction) ? mine.filter((given) => given !== reaction) : [...mine, reaction];
    // One entry per member about each message holds all their reactions to it.
    // The answer is noted at once, so the next reaction builds on this one
    // also before the stone has announced it.
    this.noteReaction(room, await addEntry<ReactionBody>(circleId, FEATURE, 'reaction', { v: 1, reactions: next }, messageId), false);
  }

  /** Changes the text of a message; only its author may. */
  async editMessage(circleId: string, messageId: string, text: string): Promise<ChatMessage> {
    await this.connect();
    const known = this.room(circleId).messages.get(messageId);
    const body: MessageBody = { v: 1, text };
    if (known?.replyTo) {
      body.reply = known.replyTo;
    }
    if (known?.dataId) {
      body.file = known.dataId;
    }
    return this.message(await changeEntry<MessageBody>(circleId, FEATURE, 'message', messageId, body));
  }

  /** Deletes a message; only its author may. */
  async deleteMessage(circleId: string, messageId: string): Promise<void> {
    await this.connect();
    await socket.remove(circleId, FEATURE, messageId);
  }

  /** A page of messages before a message, or the newest ones; the oldest first. */
  async history(circleId: string, before?: string, limit = 50): Promise<ChatMessage[]> {
    await this.connect();
    const room = this.room(circleId);
    if (!usable(room.features, FEATURE)) {
      return [];
    }
    await this.loadReactions(circleId);
    const beforePosition = before ? room.messages.get(before)?.position : undefined;
    const entries = await listEntries<MessageBody>(circleId, FEATURE, { kind: 'message', before: beforePosition, limit });
    const messages = entries.reverse().map((entry) => this.message(entry));
    if (!before) {
      // The markers of the others come with the newest page.
      const receipts = await listEntries<ReceiptBody>(circleId, FEATURE, { kind: 'receipt', limit: 200 });
      for (const receipt of receipts) {
        this.tellReceipt(receipt);
      }
    }
    return messages;
  }

  /** Tells the others in the room that the member is writing; not answered. */
  typing(circleId: string): void {
    socket.signal(circleId, FEATURE, 'typing');
  }

  /** Marks the messages of a room read up to the given one. */
  async markRead(circleId: string, messageId: string): Promise<void> {
    await this.connect();
    const room = this.room(circleId);
    const position = room.messages.get(messageId)?.position;
    if (position === undefined || position <= room.read) {
      return;
    }
    room.read = position;
    room.received = Math.max(room.received, position);
    await addEntry<ReceiptBody>(circleId, FEATURE, 'receipt', { v: 1, received: room.received, read: room.read });
  }

  /**
   * Notes that the room's messages reached this client, read or not, so
   * their authors see them as received. Once per room in half a second.
   */
  private received(circleId: string): void {
    if (this.receiving.has(circleId)) {
      return;
    }
    this.receiving.add(circleId);
    setTimeout(() => {
      this.receiving.delete(circleId);
      const room = this.rooms.get(circleId);
      if (!room || !socket.hasJoined(circleId) || !usable(room.features, FEATURE, true)) {
        return;
      }
      void (async () => {
        const newest = room.newest > 0 ? room.newest : (await socket.list(circleId, FEATURE, { kind: 'message', limit: 1, bare: true }))[0]?.position ?? 0;
        if (newest > room.received) {
          room.received = newest;
          await addEntry<ReceiptBody>(circleId, FEATURE, 'receipt', { v: 1, received: room.received, read: room.read });
        }
      })().catch(() => undefined);
    }, 500);
  }

  // --- What the stone holds, as the chat shows it ---------------------------------

  private room(circleId: string): Room {
    let room = this.rooms.get(circleId);
    if (!room) {
      room = { features: [], messages: new Map(), reactions: new Map(), reactionsLoaded: false, received: 0, read: 0, newest: 0 };
      this.rooms.set(circleId, room);
    }
    return room;
  }

  /** The reactions of a room are few and small: they are fetched once, and kept up by the announcements. */
  private async loadReactions(circleId: string): Promise<void> {
    const room = this.room(circleId);
    if (room.reactionsLoaded) {
      return;
    }
    room.reactionsLoaded = true;
    try {
      for (const reaction of await allEntries<ReactionBody>(circleId, FEATURE, 'reaction')) {
        this.noteReaction(room, reaction, false);
      }
    } catch (error) {
      room.reactionsLoaded = false;
      throw error;
    }
  }

  private noteReaction(room: Room, entry: FeatureEntry<ReactionBody>, removed: boolean): void {
    if (!entry.about || !entry.memberId) {
      return;
    }
    let byMember = room.reactions.get(entry.about);
    if (!byMember) {
      byMember = new Map();
      room.reactions.set(entry.about, byMember);
    }
    noteGiven(byMember, entry, removed);
  }

  private reactionsOf(room: Room, messageId: string): { [key: string]: string[] } {
    const reactions: { [key: string]: string[] } = {};
    for (const [memberId, given] of room.reactions.get(messageId) ?? []) {
      for (const reaction of given.reactions) {
        (reactions[reaction] ??= []).push(memberId);
      }
    }
    return reactions;
  }

  private message(entry: FeatureEntry<MessageBody>): ChatMessage {
    const room = this.room(entry.circleId);
    const message: ChatMessage = {
      messageId: entry.entryId,
      circleId: entry.circleId,
      memberId: entry.memberId,
      accountName: entry.accountName,
      time: entry.added,
      edited: entry.changed,
      position: entry.position,
      text: entry.body?.text,
      replyTo: entry.body?.reply,
      dataId: entry.body?.file,
      damaged: entry.body === null,
      reactions: this.reactionsOf(room, entry.entryId),
    };
    room.messages.set(entry.entryId, message);
    room.newest = Math.max(room.newest, entry.position);
    return message;
  }

  private tellReceipt(entry: FeatureEntry<ReceiptBody>): void {
    const receipt: ChatReceipt = {
      memberId: entry.memberId,
      accountName: entry.accountName,
      readPosition: entry.body?.read ?? 0,
      receivedPosition: entry.body?.received ?? 0,
    };
    this.dispatchEvent(new CustomEvent('receipt', { detail: { circleId: entry.circleId, receipt } }));
  }

  // --- What the socket tells ------------------------------------------------------

  private listen(): void {
    if (this.listening) {
      return;
    }
    this.listening = true;
    socket.addEventListener('added', (event) => void this.entryChanged((event as CustomEvent<SocketFrame>).detail, 'added'));
    socket.addEventListener('changed', (event) => void this.entryChanged((event as CustomEvent<SocketFrame>).detail, 'changed'));
    socket.addEventListener('removed', (event) => void this.entryChanged((event as CustomEvent<SocketFrame>).detail, 'removed'));
    socket.addEventListener('signal', (event) => {
      const frame = (event as CustomEvent<SocketFrame>).detail;
      if (frame.feature === FEATURE && frame.signal === 'typing' && frame.memberId) {
        const typing: ChatTyping = { circleId: frame.circleId, memberId: frame.memberId, accountName: frame.accountName };
        this.dispatchEvent(new CustomEvent('typing', { detail: typing }));
      }
    });
    socket.addEventListener('files', (event) => {
      this.dispatchEvent(new CustomEvent('changed', { detail: (event as CustomEvent<SocketFrame>).detail }));
    });
    socket.addEventListener('feature', (event) => {
      const frame = (event as CustomEvent<SocketFrame>).detail;
      if (frame.circleId && this.rooms.has(frame.circleId)) {
        // The features of the room changed: what is known of its chat may be stale.
        void socket.join(frame.circleId).then((features) => {
          this.room(frame.circleId as string).features = features;
          this.dispatchEvent(new CustomEvent('feature', { detail: frame }));
        }).catch(() => undefined);
      }
    });
    socket.addEventListener('left', (event) => {
      const frame = (event as CustomEvent<SocketFrame>).detail;
      this.rooms.delete(frame.circleId ?? '');
      this.dispatchEvent(new CustomEvent('left', { detail: frame }));
    });
    socket.addEventListener('state', (event) => {
      this.dispatchEvent(new CustomEvent('state', { detail: (event as CustomEvent<boolean>).detail }));
    });
  }

  private async entryChanged(frame: SocketFrame, what: 'added' | 'changed' | 'removed'): Promise<void> {
    if (frame.feature !== FEATURE || !frame.entry || !frame.circleId) {
      return;
    }
    const room = this.rooms.get(frame.circleId);
    if (!room) {
      return;
    }
    let entry: Entry;
    try {
      entry = await completeEntry(FEATURE, frame.entry, what === 'removed');
    } catch {
      return;
    }
    switch (entry.kind) {
      case 'message':
        if (what === 'removed') {
          room.messages.delete(entry.entryId ?? '');
          room.reactions.delete(entry.entryId ?? '');
          this.dispatchEvent(new CustomEvent('deleted', { detail: { circleId: frame.circleId, messageId: entry.entryId } }));
        } else {
          const message = this.message(await decodeEntry<MessageBody>(FEATURE, entry));
          if (what === 'added') {
            if (message.memberId !== session()?.memberId) {
              this.received(frame.circleId);
            }
            this.dispatchEvent(new CustomEvent('message', { detail: message }));
          } else {
            this.dispatchEvent(new CustomEvent('edited', { detail: message }));
          }
        }
        break;
      case 'reaction': {
        const reaction = await decodeEntry<ReactionBody>(FEATURE, entry);
        this.noteReaction(room, reaction, what === 'removed');
        const message = reaction.about ? room.messages.get(reaction.about) : undefined;
        if (message) {
          message.reactions = this.reactionsOf(room, reaction.about as string);
          this.dispatchEvent(new CustomEvent('edited', { detail: message }));
        }
        break;
      }
      case 'receipt':
        if (what !== 'removed') {
          this.tellReceipt(await decodeEntry<ReceiptBody>(FEATURE, entry));
        }
        break;
      default:
        break;
    }
  }
}

export const chat = new ChatClient();

/**
 * A stone which doesn't offer the chat yet gets it when its System
 * Administrator uses Rune: rooms always had a chat. The other features are
 * offered in the stone settings.
 */
export async function offerChat(): Promise<void> {
  try {
    if (!(await offeredFeatures()).has(FEATURE)) {
      await offerFeature(FEATURE);
    }
  } catch {
    // Not the System Administrator, or the stone refuses: the rooms tell that they have no chat.
  }
}

/** What the chat pane says when a room has no chat to write to. */
export function chatUnavailable(state: 'on' | 'readonly' | 'off' | 'none'): string {
  switch (state) {
    case 'readonly':
      return t('The chat of this room is closed: it can be read, but nothing new is written.');
    case 'off':
      return t('The chat is switched off in this room.');
    case 'none':
      return t('This room has no chat. An administrator of the room can switch it on.');
    default:
      return '';
  }
}
