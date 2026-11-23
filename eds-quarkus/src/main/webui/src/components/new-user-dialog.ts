import { createUser } from '../account';
import { MemberRole } from '../api';
import { h } from '../dom';
import { errorMessage } from '../session';
import { t } from '../i18n';

/**
 * Creates a user with a name and a passphrase, without an invitation; only
 * the System Administrator may. The passphrase is then given to the person
 * by other means, who should change it at the first login. `created` gets
 * the new user's ID. The first System Administrator also chooses whether
 * the account is a user or an administrator; for other administrators it
 * is a user.
 */
export function openNewUserDialog(created: (memberId: string | undefined) => void, chooseRole = false): void {
  const user = h('input', { type: 'radio', name: 'role', value: MemberRole.Standard, checked: true });
  const administrator = h('input', { type: 'radio', name: 'role', value: MemberRole.Admin });
  const roleChoice = chooseRole
    ? h('fieldset', { class: 'stack' },
      h('legend', {}, t('Role')),
      h('label', { class: 'check' }, user, h('span', {}, h('strong', {}, t('User')), ' ', h('span', { class: 'muted' }, t('Works in the rooms they are added to.')))),
      h('label', { class: 'check' }, administrator, h('span', {}, h('strong', {}, t('Administrator')), ' ', h('span', { class: 'muted' }, t('Also manages the users, the rooms and the settings of the stone. Only you make, unmake or delete administrators.')))))
    : null;
  const name = h('input', { required: true, maxlength: 75, autocomplete: 'off', placeholder: t('Account name') });
  const passphrase = h('input', { type: 'password', autocomplete: 'new-password', required: true, minlength: 15 });
  const repeat = h('input', { type: 'password', autocomplete: 'new-password', required: true });
  const error = h('p', { class: 'error', role: 'alert' });
  const submit = h('button', { type: 'submit' }, t('Create user'));
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const form = h('form', { class: 'stack dialog-body' },
    h('label', {}, t('Name'), name),
    h('label', {}, t('Passphrase'), passphrase),
    h('label', {}, t('Repeat the passphrase'), repeat),
    roleChoice,
    h('p', { class: 'muted small' }, t('Use at least 20 random characters, or several random words. Give it to the person in a safe way; they can change it after logging in. Add them to rooms afterwards, under Users.')),
    error,
    h('div', { class: 'actions' }, submit));
  const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, t('New user')), close), form);
  dialog.addEventListener('close', () => dialog.remove());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    if (passphrase.value !== repeat.value) {
      error.textContent = t('The passphrases are not the same.');
      return;
    }
    submit.disabled = true;
    try {
      const memberId = await createUser(name.value.trim(), passphrase.value, administrator.checked ? MemberRole.Admin : MemberRole.Standard);
      dialog.close();
      created(memberId);
    } catch (e) {
      error.textContent = await errorMessage(e);
      submit.disabled = false;
    }
  });

  document.body.append(dialog);
  dialog.showModal();
  name.focus();
}
