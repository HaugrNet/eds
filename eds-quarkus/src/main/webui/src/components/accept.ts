import { acceptInvitation } from '../account';
import { h } from '../dom';
import { forgetInvitationLocation, type Invitation } from '../invitation';
import { brandMark } from './brand';
import { showRecoveryKey } from './keys-dialog';
import { t } from '../i18n';

/**
 * Opened from an invitation QR code or link: the invited person chooses a
 * passphrase, and is in the room. Fires `login` when done.
 */
export class EdsAccept extends HTMLElement {
  invitation: Invitation | null = null;

  connectedCallback(): void {
    const invitation = this.invitation;
    if (!invitation) {
      return;
    }
    const next = h('input', { type: 'password', autocomplete: 'new-password', required: true, minlength: 15 });
    const repeat = h('input', { type: 'password', autocomplete: 'new-password', required: true });
    const error = h('p', { class: 'error', role: 'alert' });
    const submit = h('button', { type: 'submit' }, t('Join'));
    const form = h('form', { class: 'login card' },
      brandMark('logo'),
      h('h1', {}, t('Welcome to Rune')),
      h('p', {}, t('You were invited as {name}. Choose a passphrase to join.', { name: invitation.name })),
      // The account name, for password managers.
      h('input', { type: 'text', autocomplete: 'username', value: invitation.name, hidden: true, readonly: true }),
      h('label', {}, t('Passphrase'), next),
      h('label', {}, t('Repeat the passphrase'), repeat),
      h('p', { class: 'muted small' }, t('Use at least 20 random characters, or several random words. Keep it safe: nobody can recover it.')),
      error,
      submit,
      h('button', { type: 'button', class: 'link', onclick: () => {
        forgetInvitationLocation();
        this.dispatchEvent(new CustomEvent('login', { bubbles: true }));
      } }, t('I already have an account')));
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      error.textContent = '';
      if (next.value !== repeat.value) {
        error.textContent = t('The passphrases are not the same.');
        return;
      }
      submit.disabled = true;
      try {
        const recoveryKey = await acceptInvitation(invitation, next.value);
        forgetInvitationLocation();
        if (recoveryKey) {
          await showRecoveryKey(recoveryKey);
        }
        this.dispatchEvent(new CustomEvent('login', { bubbles: true }));
      } catch (e) {
        error.textContent = e instanceof Error ? e.message : String(e);
        submit.disabled = false;
      }
    });
    this.replaceChildren(form);
    next.focus();
  }
}

customElements.define('eds-accept', EdsAccept);
