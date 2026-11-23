import { calls, type ActiveCall, type IncomingCall, type Participant } from '../calls';
import { MAX_PEOPLE, type Media } from '../features/calls';
import { avatar } from '../avatars';
import { h } from '../dom';
import { t } from '../i18n';
import { announce, roomName } from '../notifications';
import { read, writeLocal } from '../settings';
import { session } from '../session';
import { notice } from './notices';

// On this device: the person read what a call tells the others.
const TOLD = 'rune.calls.told';

/**
 * Before the first call on a device, Rune says what a call tells the others
 * and the stone (FEATURES.md, "What protects a call"). False if the person
 * doesn't go on.
 */
function told(): boolean {
  if (read(TOLD) === 'yes') {
    return true;
  }
  const go = confirm(t('A call connects your device directly to the devices of the others, so they learn its network address. Sound and picture are encrypted between the devices and never pass the stone. The stone passes on the call setup: encrypted in transit in a room, end-to-end encrypted in a chamber. Continue?'));
  if (go) {
    writeLocal(TOLD, 'yes');
  }
  return go;
}

async function run(work: () => Promise<void>): Promise<void> {
  try {
    await work();
  } catch (error) {
    notice(error instanceof Error ? error.message : String(error), [], 20, 'call-problem');
  }
}

/** Enters the call of a room, or begins it. */
export async function startRoomCall(circleId: string, media: Media): Promise<void> {
  if (told()) {
    await run(() => calls.joinRoom(circleId, media));
  }
}

/** Calls a person directly. */
export async function startDirectCall(memberId: string, accountName: string, media: Media = 'audio'): Promise<void> {
  if (told()) {
    await run(() => calls.callPerson(memberId, accountName, media));
  }
}

/** A quiet ringing while a direct call waits to be taken; silent where the browser doesn't let a page make sound by itself. */
class RingTone {
  private context: AudioContext | null = null;
  private timer: number | undefined;

  start(): void {
    if (this.timer !== undefined) {
      return;
    }
    const beep = () => {
      try {
        this.context ??= new AudioContext();
        const oscillator = this.context.createOscillator();
        const gain = this.context.createGain();
        oscillator.frequency.value = 440;
        gain.gain.value = 0.08;
        oscillator.connect(gain).connect(this.context.destination);
        oscillator.start();
        oscillator.stop(this.context.currentTime + 0.4);
      } catch {
        // Then it rings without a sound.
      }
    };
    beep();
    this.timer = window.setInterval(beep, 2000);
  }

  stop(): void {
    window.clearInterval(this.timer);
    this.timer = undefined;
    void this.context?.close().catch(() => undefined);
    this.context = null;
  }
}

/**
 * The call this device is in, and the calls ringing here, over the panes
 * (RUNE.md, "Presence and calls"): a tile per person with their picture, or
 * their name while they send none, and buttons for the microphone, the
 * camera and hanging up. On a phone it fills the screen.
 */
export class EdsCallPanel extends HTMLElement {
  private listeners = new AbortController();
  private ringTone = new RingTone();
  private ringing = h('div', { class: 'call-ringing' });
  private heading = h('strong', { class: 'call-title' });
  private line = h('span', { class: 'muted small call-line' });
  private tiles = h('div', { class: 'call-tiles' });
  private mic = h('button', { type: 'button', class: 'secondary', onclick: () => calls.setMic(!(calls.active()?.mic ?? false)) });
  private camera = h('button', { type: 'button', class: 'secondary', onclick: () => void run(() => calls.setCamera(!(calls.active()?.camera ?? false))) });
  private hangUp = h('button', { type: 'button', class: 'danger', onclick: () => calls.hangUp() }, t('Hang up'));
  private call = h('section', { class: 'call-box', hidden: true, 'aria-label': t('Call') },
    h('header', {}, this.heading, this.line), this.tiles, h('div', { class: 'actions call-controls' }, this.mic, this.camera, this.hangUp));

  connectedCallback(): void {
    this.replaceChildren(this.ringing, this.call);
    const signal = this.listeners.signal;
    calls.addEventListener('change', () => this.render(), { signal });
    calls.addEventListener('incoming', (event) => {
      const incoming = (event as CustomEvent<IncomingCall>).detail;
      void announce(incoming.circleId, {
        quiet: t('A call in Rune'), room: t('A call'), full: t('{name} is calling you.', { name: incoming.accountName }),
      }, undefined, 'call-');
    }, { signal });
    calls.addEventListener('missed', (event) => {
      const missed = (event as CustomEvent<IncomingCall>).detail;
      notice(t('{name} called you.', { name: missed.accountName }), [], 0, `missed-${missed.id}`);
    }, { signal });
    calls.addEventListener('started', (event) => {
      const { circleId, accountName, media } = (event as CustomEvent<{ circleId: string; accountName: string; media: Media }>).detail;
      const room = roomName(circleId);
      notice(t('{name} started a call in {room}.', { name: accountName, room }),
        [{ label: t('Join'), run: () => void startRoomCall(circleId, media) }], 60, `call-${circleId}`);
      void announce(circleId, {
        quiet: t('A call in Rune'), room: t('A call began'), full: t('{name} started a call in {room}.', { name: accountName, room }),
      }, undefined, 'call-');
    }, { signal });
    calls.addEventListener('ended', (event) => {
      const note = (event as CustomEvent<{ note?: string }>).detail.note;
      if (note) {
        notice(note, [], 20, 'call-ended');
      }
    }, { signal });
    this.render();
  }

  disconnectedCallback(): void {
    this.listeners.abort();
    this.ringTone.stop();
  }

  private render(): void {
    const incoming = calls.incoming();
    this.ringing.replaceChildren(...incoming.map((one) => h('div', { class: 'notice-card call-incoming', role: 'alertdialog', 'aria-label': t('{name} is calling you.', { name: one.accountName }) },
      h('p', {}, avatar('user', undefined, one.accountName, 'small'), ' ',
        one.media === 'video' ? t('{name} is calling you, with video.', { name: one.accountName }) : t('{name} is calling you.', { name: one.accountName })),
      h('div', { class: 'actions' },
        h('button', { type: 'button', onclick: () => { if (told()) { void run(() => calls.accept(one.id)); } } }, t('Accept')),
        h('button', { type: 'button', class: 'danger', onclick: () => calls.decline(one.id) }, t('Decline'))))));
    if (incoming.length > 0) {
      this.ringTone.start();
    } else {
      this.ringTone.stop();
    }

    const call = calls.active();
    this.call.hidden = !call;
    this.classList.toggle('in-call', !!call);
    if (!call) {
      this.tiles.replaceChildren();
      return;
    }
    this.heading.textContent = call.direct ? call.otherName ?? '' : roomName(call.circleId) || t('Call');
    this.line.textContent = call.phase === 'ringing' ? t('Calling {name}…', { name: call.otherName ?? '' })
      : call.participants.length === 0 ? (call.direct ? t('Connecting…') : t('Nobody else is in the call yet.'))
        : call.participants.length + 1 >= MAX_PEOPLE ? t('The call is full: {n} people.', { n: MAX_PEOPLE }) : '';
    this.mic.textContent = call.mic ? t('Mute') : t('Unmute');
    this.mic.setAttribute('aria-pressed', String(!call.mic));
    this.camera.textContent = call.camera ? t('Camera off') : t('Camera on');
    this.camera.setAttribute('aria-pressed', String(call.camera));
    this.hangUp.textContent = call.phase === 'ringing' ? t('Cancel') : t('Hang up');
    this.showTiles(call);
  }

  /** The tiles follow who is in the call; a tile which stays keeps playing. */
  private showTiles(call: ActiveCall): void {
    const wanted = new Map<string, { name: string; stream: MediaStream; camera: boolean; mic: boolean; own: boolean; state: Participant['state'] }>();
    wanted.set('own', { name: t('You'), stream: call.local, camera: call.camera, mic: call.mic, own: true, state: 'connected' });
    for (const one of call.participants) {
      wanted.set(one.peer, { name: one.accountName, stream: one.stream, camera: one.camera && one.stream.getVideoTracks().length > 0, mic: one.mic, own: false, state: one.state });
    }
    for (const tile of [...this.tiles.children] as HTMLElement[]) {
      if (!wanted.has(tile.dataset.peer ?? '')) {
        tile.remove();
      }
    }
    for (const [peer, one] of wanted) {
      let tile = [...this.tiles.children].find((child) => (child as HTMLElement).dataset.peer === peer) as HTMLElement | undefined;
      if (!tile) {
        const video = h('video', { autoplay: true, playsinline: true });
        // The own picture is shown, not heard.
        video.muted = one.own;
        tile = h('div', { class: 'call-tile' }, video, h('div', { class: 'call-face' }), h('div', { class: 'call-name' }));
        tile.dataset.peer = peer;
        this.tiles.append(tile);
      }
      const video = tile.querySelector('video') as HTMLVideoElement;
      if (video.srcObject !== one.stream) {
        video.srcObject = one.stream;
        void video.play().catch(() => undefined);
      }
      tile.classList.toggle('no-picture', !one.camera);
      tile.classList.toggle('own', one.own);
      const face = tile.querySelector('.call-face') as HTMLElement;
      if (face.dataset.name !== one.name) {
        face.dataset.name = one.name;
        face.replaceChildren(avatar('user', undefined, one.own ? session()?.accountName ?? '' : one.name, 'large'));
      }
      const state = one.state === 'failed' ? t('No connection. Without a relay, the two devices must reach each other directly.')
        : one.state === 'connecting' ? t('Connecting…') : one.mic ? '' : t('muted');
      (tile.querySelector('.call-name') as HTMLElement).textContent = state ? `${one.name} · ${state}` : one.name;
    }
    this.tiles.dataset.count = String(wanted.size);
  }
}

customElements.define('eds-call-panel', EdsCallPanel);
