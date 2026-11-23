import type { Circle, SocketFrame, Trustee } from '../api';
import { FeatureState, TrustLevel } from '../api';
import { listMembers } from '../account';
import { h } from '../dom';
import { refusal, roomState } from '../e2e/rooms';
import { errorMessage, session } from '../session';
import { socket } from '../socket';
import { t } from '../i18n';

/**
 * A pane for a feature of a room (FEATURES.md): what the calendar, the plans and
 * the location share. It follows the room shown, joins it on the socket,
 * knows whether the feature is on there and what the person may do, and reads
 * its entries again when the stone tells of a change. What is shown is the
 * pane's own.
 */
export abstract class FeaturePane extends HTMLElement {
  /** The name of the feature, as the stone has it. */
  protected abstract readonly feature: string;
  protected circle: Circle | null = null;
  protected circleId = '';
  /** The state of the feature in the room; undefined if the room hasn't got it. */
  protected state: FeatureState | undefined;
  /** What the person may do in the room. */
  protected level: TrustLevel | undefined;
  /** Why nothing can be read or written: a chamber whose key this device hasn't. */
  protected blocked = '';
  protected members: Trustee[] = [];
  protected status = h('p', { class: 'muted feature-status', role: 'status' });
  protected body = h('div', { class: 'feature-body' });
  private listeners: AbortController | null = null;
  private generation = 0;
  private pending: number | undefined;

  connectedCallback(): void {
    if (!this.firstChild) {
      this.classList.add('feature-pane');
      this.append(this.status, this.body);
      this.status.textContent = t('Choose a room.');
    }
    this.listeners = new AbortController();
    const signal = this.listeners.signal;
    const mine = (event: Event): SocketFrame | null => {
      const frame = (event as CustomEvent<SocketFrame>).detail;
      return frame.circleId === this.circleId && (!frame.feature || frame.feature === this.feature) ? frame : null;
    };
    for (const name of ['added', 'changed', 'removed']) {
      socket.addEventListener(name, (event) => {
        const frame = mine(event);
        if (frame && this.usable()) {
          this.entryChanged(frame);
        }
      }, { signal });
    }
    socket.addEventListener('feature', (event) => {
      if (mine(event)) {
        void this.show(this.circle);
      }
    }, { signal });
    socket.addEventListener('signal', (event) => {
      const frame = mine(event);
      if (frame && this.usable()) {
        this.signalled(frame);
      }
    }, { signal });
    // After the connection came back, what was missed is read.
    socket.addEventListener('state', (event) => {
      if ((event as CustomEvent<boolean>).detail && this.circle) {
        void this.show(this.circle);
      }
    }, { signal });
  }

  disconnectedCallback(): void {
    this.listeners?.abort();
    window.clearTimeout(this.pending);
  }

  /** True if the entries of the feature can be read here. */
  protected usable(): boolean {
    return !this.blocked && (this.state === FeatureState.On || this.state === FeatureState.Readonly);
  }

  /** True if the person may add entries which need WRITE. */
  protected mayWrite(): boolean {
    return !this.blocked && this.state === FeatureState.On && (this.level === TrustLevel.Write || this.level === TrustLevel.Admin);
  }

  /** True if the feature takes new entries from everyone in the room. */
  protected open(): boolean {
    return !this.blocked && this.state === FeatureState.On;
  }

  /** The name of a Member of the room. */
  protected nameOf(memberId: string | undefined): string {
    return this.members.find((member) => member.memberId === memberId)?.accountName ?? t('someone who left');
  }

  protected me(): string {
    return session()?.memberId ?? '';
  }

  /** Shows the feature of a room, or nothing without a room. */
  async show(circle: Circle | null): Promise<void> {
    const generation = ++this.generation;
    this.circle = circle;
    this.circleId = circle?.circleId ?? '';
    this.state = undefined;
    this.blocked = '';
    this.leaving();
    this.body.replaceChildren();
    if (!circle?.circleId) {
      this.status.textContent = t('Choose a room.');
      return;
    }
    this.status.textContent = t('Loading…');
    try {
      const [features, members, room] = await Promise.all([socket.join(circle.circleId), listMembers(circle.circleId), roomState(circle.circleId)]);
      if (generation !== this.generation) {
        return;
      }
      this.members = members;
      this.level = members.find((member) => member.memberId === this.me())?.trustLevel;
      this.state = features.find((feature) => feature.name === this.feature)?.state;
      this.blocked = refusal(room) ?? '';
      if (this.state === undefined || this.state === FeatureState.Off) {
        this.status.textContent = this.state === undefined
          ? t('This room does not have this feature. An administrator of the room can switch it on under "Features".')
          : t('This feature is switched off in this room. What it has is kept.');
        return;
      }
      if (this.blocked) {
        this.status.textContent = this.blocked;
        return;
      }
      this.status.textContent = '';
      await this.load();
    } catch (e) {
      if (generation === this.generation) {
        this.status.textContent = await errorMessage(e);
      }
    }
  }

  /** Reads the entries again, a moment after the last change. */
  protected reloadSoon(): void {
    window.clearTimeout(this.pending);
    this.pending = window.setTimeout(() => {
      if (this.usable()) {
        void this.load().catch(async (e) => { this.status.textContent = await errorMessage(e); });
      }
    }, 150);
  }

  /** Runs what a click asked for, and tells what went wrong. */
  protected async run(work: () => Promise<unknown>): Promise<boolean> {
    try {
      this.status.textContent = '';
      await work();
      return true;
    } catch (e) {
      this.status.textContent = await errorMessage(e);
      return false;
    }
  }

  /** Reads the entries of the feature and shows them. */
  protected abstract load(): Promise<void>;

  /** An entry was added, changed or removed, by anyone. */
  protected entryChanged(_frame: SocketFrame): void {
    this.reloadSoon();
  }

  /** A signal of the feature came from someone else in the room. */
  protected signalled(_frame: SocketFrame): void {
    // Most features have none.
  }

  /** The pane turns to another room, or to none. */
  protected leaving(): void {
    // Nothing to let go of.
  }
}
