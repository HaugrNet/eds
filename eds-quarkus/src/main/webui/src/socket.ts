import { suspect } from './backend';
import { SocketFrameType, type CircleFeature, type Entry, type SocketFrame } from './api';
import { friendly } from './messages';
import { BASE_PATH, session, sessionEnded } from './session';
import { t } from './i18n';
import { forgetPresence, memberCame, memberWent, setPresent, showMe } from './presence';

/**
 * The socket of the stone (FEATURES.md): one connection for the app, over
 * which Rune works with the entries of the features of its rooms, and is told
 * what changes in them. It authenticates with the first frame, as browsers
 * can't send an Authorization header on a WebSocket, and reconnects after a
 * network failure. Answers to a frame are matched by `ref`.
 *
 * Events, each with the frame as detail: `added`, `changed` and `removed`
 * (an entry of a feature of a joined room, whoever changed it, also this
 * connection), `feature` (a feature of a room was switched or removed), `signal`
 * (a signal of another connection), `files` (the files of a joined room
 * changed), `left` (access to a room ended), `joined` (a room was joined, or
 * joined again; its features are known anew), and `state` (connected or
 * not, with a boolean as detail).
 *
 * The stone also tells who is present in a joined room (FEATURES.md,
 * "Presence, and signals to one Member"); that is kept in presence.ts.
 */
export class SocketClient extends EventTarget {
  private socket: WebSocket | null = null;
  private ready: Promise<void> | null = null;
  private pending = new Map<string, { resolve: (frame: SocketFrame) => void; reject: (error: Error) => void }>();
  private joined = new Set<string>();
  /** The features of the joined rooms, as the stone last told them. */
  private features = new Map<string, CircleFeature[]>();
  private counter = 0;
  private closedByUs = false;
  private retry = 1000;

  get connected(): boolean {
    return this.socket?.readyState === WebSocket.OPEN;
  }

  connect(): Promise<void> {
    if (this.ready) {
      return this.ready;
    }
    this.closedByUs = false;
    this.ready = new Promise<void>((resolve, reject) => {
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      const socket = new WebSocket(`${protocol}//${location.host}${BASE_PATH}/socket`);
      this.socket = socket;
      socket.onopen = () => {
        this.request({ type: SocketFrameType.Auth, token: session()?.token })
          .then(() => {
            this.retry = 1000;
            this.dispatchEvent(new CustomEvent('state', { detail: true }));
            // Joins again after a reconnect.
            for (const circleId of this.joined) {
              this.enter(circleId).catch(() => this.joined.delete(circleId));
            }
            resolve();
          })
          .catch(reject);
      };
      socket.onmessage = (event) => this.receive(JSON.parse(event.data) as SocketFrame);
      socket.onclose = (event) => {
        this.ready = null;
        this.socket = null;
        for (const waiting of this.pending.values()) {
          waiting.reject(new Error(t('The connection closed.')));
        }
        this.pending.clear();
        // Without the connection, nobody is known to be there.
        forgetPresence();
        this.dispatchEvent(new CustomEvent('state', { detail: false }));
        if (!this.closedByUs) {
          // The connection went: maybe EDS is down.
          suspect();
        }
        reject(new Error(t('The connection closed.')));
        if (event.code === 1008) {
          // The session ended or expired.
          sessionEnded();
        } else if (!this.closedByUs && session()) {
          setTimeout(() => this.connect().catch(() => undefined), this.retry);
          this.retry = Math.min(this.retry * 2, 30000);
        }
      };
    });
    return this.ready;
  }

  close(): void {
    this.closedByUs = true;
    this.joined.clear();
    this.features.clear();
    forgetPresence();
    this.socket?.close(1000);
  }

  /** Joins a room; resolves with its features and their state. */
  async join(circleId: string): Promise<CircleFeature[]> {
    await this.connect();
    const features = await this.enter(circleId);
    this.joined.add(circleId);
    return features;
  }

  /** Joins on the stone, shown as online or not as the person chose, and notes what the answer tells. */
  private async enter(circleId: string): Promise<CircleFeature[]> {
    const answer = await this.request({ type: SocketFrameType.Join, circleId, unseen: showMe() ? undefined : true });
    const features = answer.features ?? [];
    this.features.set(circleId, features);
    setPresent(circleId, answer.present ?? []);
    this.dispatchEvent(new CustomEvent('joined', { detail: { circleId } }));
    return features;
  }

  /** Joins the rooms again, after the person chose to be shown as online or not. */
  async rejoin(): Promise<void> {
    if (this.connected) {
      await Promise.all([...this.joined].map((circleId) => this.enter(circleId).catch(() => undefined)));
    }
  }

  hasJoined(circleId: string): boolean {
    return this.joined.has(circleId);
  }

  /** The rooms joined. */
  rooms(): string[] {
    return [...this.joined];
  }

  /** The features of a joined room as the stone last told them; none for a room which isn't joined. */
  featuresOf(circleId: string): CircleFeature[] {
    return this.joined.has(circleId) ? this.features.get(circleId) ?? [] : [];
  }

  async leave(circleId: string): Promise<void> {
    this.joined.delete(circleId);
    this.features.delete(circleId);
    forgetPresence(circleId);
    if (this.connected) {
      await this.request({ type: SocketFrameType.Leave, circleId }).catch(() => undefined);
    }
  }

  /** Adds an entry; resolves with it. */
  async add(circleId: string, feature: string, kind: string, content: string, about?: string, name?: string): Promise<Entry> {
    return this.one({ type: SocketFrameType.Add, circleId, feature, kind, content, about, name });
  }

  async change(circleId: string, feature: string, entryId: string, content?: string, name?: string): Promise<Entry> {
    return this.one({ type: SocketFrameType.Change, circleId, feature, entryId, content, name });
  }

  /** Removes an entry; resolves with what went, the entries about it first. */
  async remove(circleId: string, feature: string, entryId: string): Promise<Entry[]> {
    await this.connect();
    return (await this.request({ type: SocketFrameType.Remove, circleId, feature, entryId })).entries ?? [];
  }

  async closeEntry(circleId: string, feature: string, entryId: string, closed: boolean): Promise<Entry> {
    return this.one({ type: SocketFrameType.Close, circleId, feature, entryId, closed });
  }

  async get(circleId: string, feature: string, entryId: string): Promise<Entry> {
    return this.one({ type: SocketFrameType.Get, circleId, feature, entryId });
  }

  /** A page of entries, the newest first unless asked otherwise. */
  async list(circleId: string, feature: string, options: Pick<SocketFrame, 'kind' | 'about' | 'name' | 'after' | 'before' | 'oldestFirst' | 'limit' | 'bare'> = {}): Promise<Entry[]> {
    await this.connect();
    return (await this.request({ type: SocketFrameType.List, circleId, feature, ...options })).entries ?? [];
  }

  async count(circleId: string, feature: string, kind?: string, after?: number): Promise<number> {
    await this.connect();
    return (await this.request({ type: SocketFrameType.Count, circleId, feature, kind, after })).count ?? 0;
  }

  /**
   * Sends a signal to the others in the room, or with `to` only to the
   * devices of that member; it is neither stored nor answered. False if it
   * couldn't be sent.
   */
  signal(circleId: string, feature: string, signal: string, to?: string): boolean {
    if (this.socket?.readyState === WebSocket.OPEN && this.joined.has(circleId)) {
      this.socket.send(JSON.stringify({ type: SocketFrameType.Signal, circleId, feature, signal, to }));
      return true;
    }
    return false;
  }

  private async one(frame: SocketFrame): Promise<Entry> {
    await this.connect();
    const entry = (await this.request(frame)).entries?.[0];
    if (!entry) {
      throw new Error(t('The stone answered without the entry.'));
    }
    return entry;
  }

  private request(frame: SocketFrame): Promise<SocketFrame> {
    const ref = String(++this.counter);
    return new Promise<SocketFrame>((resolve, reject) => {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        reject(new Error(t('Rune is not connected to the stone.')));
        return;
      }
      this.pending.set(ref, { resolve, reject });
      this.socket.send(JSON.stringify({ ...frame, ref }));
    });
  }

  private receive(frame: SocketFrame): void {
    const waiting = frame.ref ? this.pending.get(frame.ref) : undefined;
    if (waiting) {
      this.pending.delete(frame.ref as string);
      if (frame.type === SocketFrameType.Error) {
        waiting.reject(new Error(friendly(frame.returnMessage, t('The stone refused the request.'))));
      } else {
        waiting.resolve(frame);
      }
      return;
    }
    switch (frame.type) {
      case SocketFrameType.Added:
        this.tell('added', frame);
        break;
      case SocketFrameType.Changed:
        this.tell('changed', frame);
        break;
      case SocketFrameType.Removed:
        this.tell('removed', frame);
        break;
      case SocketFrameType.Feature:
        this.tell('feature', frame);
        break;
      case SocketFrameType.Signal:
        this.tell('signal', frame);
        break;
      case SocketFrameType.Files:
        this.tell('files', frame);
        break;
      case SocketFrameType.Present:
        if (frame.circleId && frame.memberId) {
          memberCame(frame.circleId, frame.memberId);
        }
        break;
      case SocketFrameType.Absent:
        if (frame.circleId && frame.memberId) {
          memberWent(frame.circleId, frame.memberId);
        }
        break;
      case SocketFrameType.Left:
        if (frame.circleId) {
          this.joined.delete(frame.circleId);
          this.features.delete(frame.circleId);
          forgetPresence(frame.circleId);
          this.tell('left', frame);
        }
        break;
      default:
        break;
    }
  }

  private tell(name: string, frame: SocketFrame): void {
    if (frame.circleId) {
      this.dispatchEvent(new CustomEvent(name, { detail: frame }));
    }
  }
}

export const socket = new SocketClient();
