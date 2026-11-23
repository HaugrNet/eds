import { h } from '../dom';
import { knotRing } from './knot';
import { LOCKED, login, LoginError, NOT_READY } from '../session';
import { serverInfo } from '../server';
import { t } from '../i18n';

/**
 * The login (design.zip, login.html): Rune in runes, the username and the
 * password, inside a plaited knot. Fires `login` when a session is started.
 * A narrow window has no room for the knot, and shows the form alone. What
 * went wrong is told below the knot, as it has no room for more than the form.
 */
export class EdsLogin extends HTMLElement {
  connectedCallback(): void {
    const account = h('input', { id: 'rune-user', name: 'username', type: 'text', autocomplete: 'username', required: true, maxlength: 75 });
    const passphrase = h('input', { id: 'rune-pass', name: 'password', type: 'password', autocomplete: 'current-password', required: true });
    const error = h('div', { class: 'error', role: 'alert' });
    const button = h('button', { class: 'rune-btn', type: 'submit' }, t('Sign in'));
    // Where Rune runs, and whether that is a test system: a quiet footer, as a
    // block above the fields pushed the button onto the ring.
    const running = h('span', {});
    const test = h('p', { hidden: true },
      h('strong', {}, t('Test system')), ' · ', t('The data will not survive a restart of the stone.'));
    const foot = h('div', { class: 'rune-foot' },
      h('p', {}, t('Your Rune is currently running on'), ' ', running),
      test);
    const form = h('form', { class: 'rune-form', autocomplete: 'on' },
      runeMark(),
      h('div', { class: 'rune-fields' },
        h('div', { class: 'rune-field' }, h('label', { for: 'rune-user' }, t('Username')), account),
        h('div', { class: 'rune-field' }, h('label', { for: 'rune-pass' }, t('Password')), passphrase)),
      button);
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      error.textContent = '';
      button.disabled = true;
      try {
        await login(account.value.trim(), passphrase.value);
        passphrase.value = '';
        this.dispatchEvent(new CustomEvent('login', { bubbles: true }));
      } catch (e) {
        error.replaceChildren(...explain(e));
      } finally {
        button.disabled = false;
      }
    });
    running.textContent = 'Haugr EDS';
    this.replaceChildren(h('div', { class: 'rune-stage' }, knotRing('rune-knot'), form), error, foot);
    account.focus();
    // The login needs the server, so it is reachable here.
    serverInfo().then((info) => {
      running.textContent = `Haugr EDS ${info.version}`;
      test.hidden = info.persistent !== false;
    }).catch(() => undefined);
  }
}

/** The runes of the mark: ᚱ, ᚢ and ᚾ, each 48 wide. */
const RUNES = [
  'M14,4 L14,60 M14,4 L34,17 L14,31 L36,60',
  'M60,60 L60,4 L84,22 L84,60',
  'M120,4 L120,60 M110,22 L130,38',
];

/** Rune written in runes (ᚱᚢᚾ, rún), in the accent colour of the theme. */
function runeMark(): SVGSVGElement {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 144 64');
  svg.setAttribute('class', 'rune-logo');
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', 'Rune');
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', RUNES.join(' '));
  svg.appendChild(path);
  return svg;
}

/**
 * Explains a failed login. A locked or not ready EDS isn't the user's fault,
 * and only the System Administrator can fix it, so it gets a notice instead
 * of the raw message, without details of the setup.
 */
function explain(error: unknown): Node[] {
  const code = error instanceof LoginError ? error.returnCode : 0;
  const message = error instanceof Error ? error.message : t('The login failed.');
  switch (code) {
    case LOCKED:
      return notice(t('Rune is locked'),
        t('The MasterKey is not loaded, so the stone can\'t open any data yet. Please ask your administrator to unlock it.'),
        t('Administrators: see RUNNING.md.'));
    case NOT_READY:
      return notice(t('Rune is not ready'),
        t('The stone can\'t use its database at the moment. Please try again later, or ask your administrator.'),
        t('Administrators: see RUNNING.md.'));
    case 0:
      return notice(t('Rune can\'t be reached'), t('Check your connection and try again.'), message);
    default:
      return [h('p', {}, message)];
  }
}

function notice(title: string, text: string, detail: string): Node[] {
  return [h('div', { class: 'notice' }, h('strong', {}, title), h('p', {}, text), h('p', { class: 'muted small' }, detail))];
}

customElements.define('eds-login', EdsLogin);
