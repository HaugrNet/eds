import { changePassphrase } from '../account';
import { h } from '../dom';
import { t } from '../i18n';

/** Changes the passphrase of the logged-in member. */
export function openPassphraseDialog(): void {
  const current = h('input', { type: 'password', autocomplete: 'current-password', required: true });
  const next = h('input', { type: 'password', autocomplete: 'new-password', required: true, minlength: 15 });
  const repeat = h('input', { type: 'password', autocomplete: 'new-password', required: true });
  const error = h('p', { class: 'error', role: 'alert' });
  const submit = h('button', { type: 'submit' }, t('Change passphrase'));
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const form = h('form', { class: 'stack dialog-body' },
    h('label', {}, t('Current passphrase'), current),
    h('label', {}, t('New passphrase'), next),
    h('label', {}, t('Repeat the new passphrase'), repeat),
    h('p', { class: 'muted small' }, t('Use at least 20 random characters, or several random words.')),
    error,
    h('div', { class: 'actions' }, submit));
  const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, t('Your passphrase')), close), form);
  dialog.addEventListener('close', () => dialog.remove());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    if (next.value !== repeat.value) {
      error.textContent = t('The new passphrases are not the same.');
      return;
    }
    submit.disabled = true;
    try {
      await changePassphrase(current.value, next.value);
      // A fresh start with the new session.
      location.reload();
    } catch (e) {
      error.textContent = e instanceof Error ? e.message : String(e);
      submit.disabled = false;
    }
  });

  document.body.append(dialog);
  dialog.showModal();
  current.focus();
}
