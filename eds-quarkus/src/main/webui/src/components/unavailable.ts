import { h } from '../dom';
import { knotRing } from './knot';
import { retry } from '../backend';
import { rememberedPersistent } from '../server';
import { t } from '../i18n';

/** How often Rune asks whether EDS is back. */
const RETRY_MILLIS = 5000;

/**
 * The picture (design.zip, server-asleep.html): a rune stone and a longhouse
 * asleep under the moon. Rune's own line drawing, in the stage's colours
 * (classes, as the Content Security Policy allows no inline styles).
 */
const PICTURE = `
  <g class="ua-line">
    <path d="M4,94 Q120,86 236,94"/>
    <path d="M32,92 L30,42 Q30,14 52,14 Q74,14 74,42 L72,92"/>
    <path d="M150,62 L166,46 L214,46 L230,62"/>
    <path d="M160,52 L172,40 M172,52 L160,40 M208,52 L220,40 M220,52 L208,40"/>
    <path d="M158,60 L158,90 M222,60 L222,90"/>
    <path d="M184,90 L184,78 Q190,70 196,78 L196,90"/>
  </g>
  <g transform="translate(52,54) scale(0.55) translate(-24,-32)"><path class="ua-mark" d="M14,4 L14,60 M14,4 L34,17 L14,31 L36,60"/></g>
  <circle class="ua-dot" cx="82" cy="89.0" r="2.6"/><circle class="ua-dot" cx="97" cy="88.4" r="2.6"/><circle class="ua-dot" cx="112" cy="87.8" r="2.6"/><circle class="ua-dot" cx="127" cy="87.2" r="2.6"/><circle class="ua-dot" cx="142" cy="86.6" r="2.6"/>
  <circle class="ua-moon" cx="212" cy="18" r="13"/><circle class="ua-moon-cut" cx="219" cy="13" r="11"/>
  <text class="ua-zz" x="230" y="34" font-size="11">z</text><text class="ua-zz" x="236" y="24" font-size="9">z</text>
  <path class="ua-plus" d="M16,12 L24,12 M20,8 L20,16"/><path class="ua-plus" d="M106,18 L114,18 M110,14 L110,22"/><path class="ua-plus" d="M137,6 L143,6 M140,3 L140,9"/><path class="ua-plus" d="M177,24 L183,24 M180,21 L180,27"/>`;

/**
 * Shown instead of the login or the panes while EDS doesn't answer
 * (design.zip, server-asleep.html): what is going on, in plain words, and
 * that nothing needs to be done. Whether the data is safe depends on the
 * system last reached: a test system (in-memory database) loses it. Asks every few seconds, and now with the
 * button; Rune continues where it was when EDS is back (the root listens
 * for it). Which server it is stays in a small line at the foot of the page.
 */
export class EdsUnavailable extends HTMLElement {
  private timer: number | undefined;
  private status = h('p', { class: 'ua-status', role: 'status' });
  private button = h('button', { class: 'rune-btn', type: 'button', onclick: () => void this.check(true) }, t('Try now'));
  private checking = false;

  connectedCallback(): void {
    const picture = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    picture.setAttribute('viewBox', '0 0 244 100');
    picture.setAttribute('class', 'ua-picture');
    picture.setAttribute('role', 'img');
    picture.setAttribute('aria-label', t('A runestone and a sleeping longhouse under the moon'));
    picture.innerHTML = PICTURE;
    const persistent = rememberedPersistent();
    this.replaceChildren(
      h('div', { class: 'rune-stage' },
        knotRing('rune-knot'),
        h('section', { class: 'rune-form rune-status' },
          picture,
          h('h1', {}, t('The stone is asleep')),
          // Only what is known: a test system loses its data with the server.
          persistent === false
            ? h('p', {}, t('This is a test system: its data will not survive a restart.'), h('br'), t('Rune reconnects on its own.'))
            : persistent === true
              ? h('p', {}, t('Your rooms and files are safe.'), h('br'), t('Rune reconnects on its own.'))
              : h('p', {}, t('Rune reconnects on its own.')),
          this.button)),
      h('div', { class: 'rune-foot' }, this.status));
    this.timer = window.setInterval(() => void this.check(), RETRY_MILLIS);
  }

  disconnectedCallback(): void {
    window.clearInterval(this.timer);
  }

  /**
   * Asks whether EDS is back. Only a click shows it on the button: the
   * automatic retries stay silent, as changing the button every few seconds
   * made it flicker.
   *
   * @param manual True for a click on the button
   */
  private async check(manual = false): Promise<void> {
    if (this.checking) {
      return;
    }
    this.checking = true;
    if (manual) {
      this.button.disabled = true;
      this.button.textContent = t('Trying…');
      this.status.textContent = t('Trying…');
    }
    const back = await retry();
    this.checking = false;
    if (manual && this.isConnected && !back) {
      this.button.disabled = false;
      this.button.textContent = t('Try now');
      this.status.textContent = '';
    }
  }
}

customElements.define('eds-unavailable', EdsUnavailable);
