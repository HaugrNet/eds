import { chat } from '../chat';
import { h } from '../dom';
import { session } from '../session';
import { ASYMMETRIC_ALGORITHM, IS_READY, serverInfo, SYMMETRIC_ALGORITHM } from '../server';
import { SERVER_SETTINGS } from './server-settings';
import { t } from '../i18n';

/**
 * The line at the bottom: "Your Rune is running on Haugr EDS" with the
 * version, "Test system" if the data doesn't survive a restart, whether the live
 * connection (chat, changes of files) is up, and who is logged in. The
 * System Administrator also sees the encryption in use, and a warning while
 * EDS isn't ready; these come from the settings of EDS (server-settings.ts).
 */
export class StatusLine {
  // "Your Rune is running on Haugr EDS 2.0"; a phone only has room for "EDS 2.0".
  private version = h('span', {}, 'EDS');
  private test = h('span', { class: 'error', hidden: true }, t('Test system'));
  private connection = h('span', { class: 'connection' });
  private crypto = h('span', { class: 'wide', hidden: true, title: t('Encryption of new data and keys') });
  private ready = h('span', { class: 'error', hidden: true }, t('EDS is not ready'));
  private who = h('span', { class: 'who' }, session()?.accountName ?? '');
  readonly element = h('footer', { class: 'status-line' },
    h('span', { title: t('The version of EDS') }, h('span', { class: 'wide' }, `${t('Your Rune is running on Haugr')} `), this.version),
    this.test,
    this.connection, this.ready, this.crypto, this.who);
  private listeners = new AbortController();

  constructor() {
    const signal = this.listeners.signal;
    this.showConnection(chat.connected ? 'up' : 'starting');
    chat.addEventListener('state', (event) => this.showConnection((event as CustomEvent<boolean>).detail ? 'up' : 'down'), { signal });
    document.addEventListener(SERVER_SETTINGS, (event) => {
      const settings = (event as CustomEvent<Record<string, string>>).detail;
      const algorithms = [settings[SYMMETRIC_ALGORITHM], settings[ASYMMETRIC_ALGORITHM]].filter(Boolean);
      this.crypto.textContent = algorithms.join(' · ');
      this.crypto.hidden = algorithms.length === 0;
      this.ready.hidden = settings[IS_READY] !== 'false';
    }, { signal });
    serverInfo().then((info) => {
      this.version.textContent = `EDS ${info.version}`;
      // A test system says so, as its data doesn't survive a restart.
      if (info.persistent === false) {
        this.test.hidden = false;
        this.test.title = t('In-memory database: all data is lost when the stone stops.');
      }
    }).catch(() => undefined);
  }

  setAdministrator(): void {
    this.who.textContent = `${session()?.accountName ?? ''} · ${t('System Administrator')}`;
  }

  close(): void {
    this.listeners.abort();
  }

  private showConnection(state: 'starting' | 'up' | 'down'): void {
    this.connection.dataset.state = state;
    this.connection.textContent = state === 'up' ? t('Connected') : state === 'down' ? t('Not connected, trying again…') : t('Connecting…');
    this.connection.title = t('The live connection for the chat and changes of files');
  }
}
