import type { Member } from '../api';
import { openStonesWindow } from './stones-window';
import { openKeysDialog } from './keys-dialog';
import { keyState } from '../e2e/identity';
import { isSystemAdministrator, myAccount, setOwnAvatar } from '../account';
import { avatar } from '../avatars';
import { h } from '../dom';
import { t } from '../i18n';
import { showMe } from '../presence';
import { level, type NotificationLevel } from '../notifications';
import { logout, session } from '../session';
import { openAboutDialog } from './about-dialog';
import { openAvatarDialog } from './avatar-dialog';
import { openNotificationDialog } from './notification-dialog';
import { openPassphraseDialog } from './passphrase-dialog';
import { openSettingsDialog } from './settings-dialog';
import type { Workspace } from './workspace';

function notificationState(value: NotificationLevel): string {
  return { off: t('Off'), quiet: t('On, without details'), room: t('On, with the room'), full: t('On, with sender and text') }[value];
}

/**
 * The own avatar in the header, and its menu: avatar, passphrase, language
 * and clock, theme, panes, notifications (for all rooms, on this device),
 * the settings of EDS for the System Administrator, about Rune and logging
 * out. A menu as WAI-ARIA describes it: arrow keys move, Escape
 * closes, a click outside closes.
 */
export class AccountMenu {
  readonly element: HTMLElement;
  private button = h('button', { type: 'button', class: 'account-button', 'aria-haspopup': 'menu', 'aria-expanded': 'false' });
  private menu = h('div', { class: 'account-menu', role: 'menu', hidden: true });
  private me: Member | undefined;
  private outside = new AbortController();

  constructor(private readonly workspace: Workspace) {
    this.element = h('div', { class: 'account' }, this.button, this.menu);
    this.button.addEventListener('click', () => (this.menu.hidden ? void this.open() : this.close(true)));
    this.menu.addEventListener('keydown', (event) => this.keys(event));
    document.addEventListener('avatars-changed', () => void this.load());
    this.showButton();
    void this.load();
  }

  private async load(): Promise<void> {
    try {
      this.me = await myAccount();
    } catch {
      // The initial stays.
    }
    this.showButton();
  }

  private name(): string {
    return this.me?.accountName ?? session()?.accountName ?? '';
  }

  private showButton(): void {
    this.button.replaceChildren(avatar('user', this.me?.avatar, this.name()));
    this.button.title = t('{name}: your account and settings', { name: this.name() });
    this.button.setAttribute('aria-label', this.button.title);
  }

  private async open(): Promise<void> {
    // Opened right after the login, the account may still be on its way: the
    // items of a System Administrator depend on it.
    if (!this.me) {
      await this.load();
    }
    const item = (label: string, run: () => void, extra?: string) => {
      const button = h('button', { type: 'button', role: 'menuitem', tabindex: -1, class: 'menu-item' },
        h('span', {}, label), extra ? h('span', { class: 'muted small' }, extra) : null);
      button.addEventListener('click', () => {
        this.close(false);
        run();
      });
      return button;
    };
    const name = this.name();
    this.menu.replaceChildren(
      h('div', { class: 'menu-head' }, avatar('user', this.me?.avatar, name, 'large'),
        h('span', {}, h('strong', {}, name),
          isSystemAdministrator(this.me) ? h('span', { class: 'muted small' }, t('System Administrator')) : null)),
      item(t('Change avatar…'), () => openAvatarDialog('user', t('Your avatar'), name, this.me?.avatar, setOwnAvatar)),
      item(t('Change passphrase…'), () => openPassphraseDialog()),
      item(t('Keys for chambers…'), () => openKeysDialog(), keyState() === 'locked' ? t('Recovery key needed') : undefined),
      h('hr', { role: 'separator' }),
      item(t('Language and clock…'), () => openSettingsDialog(this.workspace, 'language')),
      item(t('Theme…'), () => openSettingsDialog(this.workspace, 'theme')),
      item(t('Panes…'), () => openSettingsDialog(this.workspace, 'panes')),
      item(t('Notifications…'), () => openNotificationDialog(() => undefined), notificationState(level())),
      item(t('Online status…'), () => openSettingsDialog(this.workspace, 'presence'), showMe() ? t('shown') : t('not shown')),
      ...(isSystemAdministrator(this.me) ? [
        h('hr', { role: 'separator' }),
        item(t('Stone settings…'), () => this.element.dispatchEvent(new CustomEvent('open-server', { bubbles: true }))),
        item(t('Stones…'), () => openStonesWindow()),
      ] : []),
      h('hr', { role: 'separator' }),
      item(t('About Rune…'), () => openAboutDialog()),
      item(t('Log out'), () => void logout()));
    this.menu.hidden = false;
    this.button.setAttribute('aria-expanded', 'true');
    this.items()[0]?.focus();
    // A click anywhere else closes the menu.
    this.outside = new AbortController();
    document.addEventListener('pointerdown', (event) => {
      if (!this.element.contains(event.target as Node)) {
        this.close(false);
      }
    }, { signal: this.outside.signal, capture: true });
  }

  private close(focusButton: boolean): void {
    this.outside.abort();
    this.menu.hidden = true;
    this.button.setAttribute('aria-expanded', 'false');
    if (focusButton) {
      this.button.focus();
    }
  }

  private items(): HTMLButtonElement[] {
    return [...this.menu.querySelectorAll<HTMLButtonElement>('[role="menuitem"]')];
  }

  private keys(event: KeyboardEvent): void {
    const items = this.items();
    const at = items.indexOf(document.activeElement as HTMLButtonElement);
    const move = (to: number) => {
      event.preventDefault();
      items[(to + items.length) % items.length]?.focus();
    };
    switch (event.key) {
      case 'ArrowDown': move(at + 1); break;
      case 'ArrowUp': move(at - 1); break;
      case 'Home': move(0); break;
      case 'End': move(items.length - 1); break;
      case 'Escape': event.preventDefault(); this.close(true); break;
      case 'Tab': this.close(false); break;
      default: break;
    }
  }
}
