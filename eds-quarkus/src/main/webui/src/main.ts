// Rune's fonts, self-hosted (OFL): no requests to font services, and they
// work offline. Each file covers its scripts; browsers load only those used.
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@fontsource/space-grotesk/600.css';
import './rune-theme.css';
import './themes.css';
import './styles.css';
import './components/login';
import './components/app';
import './components/unavailable';
import { EdsAccept } from './components/accept';
import { invitationFromLocation } from './invitation';
import { session } from './session';
import { applyTheme, pinThemeColor } from './theme';
import { setScreenTitle } from './notifications';
import { t } from './i18n';
import { loadAccountSettings } from './account-settings';
import { isDown, REACHABLE, suspect, UNREACHABLE } from './backend';

// Before anything is shown, so the page doesn't flash in another theme.
applyTheme();

/**
 * Shows the login, or the app while a session is known; while EDS doesn't
 * answer, a page saying so instead of either. The login and that page are
 * dark in every theme, with their own tab title and browser bar colour.
 */
class EdsRoot extends HTMLElement {
  connectedCallback(): void {
    this.addEventListener('login', () => this.render());
    window.addEventListener('eds-logout', () => this.render());
    // EDS stopped answering, or is back: Rune continues where it was, as
    // the session is kept.
    window.addEventListener(UNREACHABLE, () => this.render());
    window.addEventListener(REACHABLE, () => this.render());
    // An invitation link opened in a tab which shows Rune already changes
    // only the fragment of the address, which doesn't load the page anew.
    window.addEventListener('hashchange', () => this.render());
    this.render();
    // The shell may come from the service worker's cache while EDS is down.
    suspect();
  }

  private render(): void {
    // Opened from an invitation QR code or link: choose a passphrase first.
    const invitation = invitationFromLocation();
    if (invitation) {
      const accept = new EdsAccept();
      accept.invitation = invitation;
      pinThemeColor(null);
      setScreenTitle(null);
      this.replaceChildren(accept);
      return;
    }
    if (isDown()) {
      pinThemeColor('--rune-bg');
      setScreenTitle(t('Stone asleep'));
      this.replaceChildren(document.createElement('eds-unavailable'));
      return;
    }
    if (!session()) {
      pinThemeColor('--rune-bg');
      setScreenTitle(t('Sign in'));
      this.replaceChildren(document.createElement('eds-login'));
      return;
    }
    // The settings follow the account: taken from it before Rune shows.
    const token = session()?.token;
    pinThemeColor(null);
    setScreenTitle(null);
    this.replaceChildren();
    void loadAccountSettings().then((reload) => {
      if (session()?.token !== token || isDown()) {
        return;
      }
      if (reload) {
        // Another language or clock: every text and time follows.
        location.reload();
        return;
      }
      applyTheme();
      this.replaceChildren(document.createElement('eds-app'));
    });
  }
}

customElements.define('eds-root', EdsRoot);

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('sw.js').catch(() => undefined);
}
