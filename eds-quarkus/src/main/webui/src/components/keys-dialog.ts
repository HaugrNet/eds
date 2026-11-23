import { h } from '../dom';
import { t } from '../i18n';
import { createKeys, keyState, loadKeys, ownFingerprint, unlockKeys } from '../e2e/identity';
import { forgetRooms } from '../e2e/rooms';

/**
 * Shows the recovery key, once, when a member's keys were made (E2E.md):
 * the member writes it down, as a further device needs it, and nobody can
 * show it again. The dialog only closes when the member says it is kept.
 */
export function showRecoveryKey(recoveryKey: string): Promise<void> {
  return new Promise((resolve) => {
    const copied = h('span', { class: 'muted small', role: 'status' });
    const field = h('textarea', { readonly: true, rows: 3, class: 'recovery-key', 'aria-label': t('Recovery key') }, recoveryKey);
    const kept = h('input', { type: 'checkbox' });
    const done = h('button', { type: 'button', disabled: true, onclick: () => dialog.close() }, t('Continue'));
    kept.addEventListener('change', () => { done.disabled = !kept.checked; });
    const copy = h('button', { type: 'button', class: 'secondary', onclick: async () => {
      try {
        await navigator.clipboard.writeText(recoveryKey);
        copied.textContent = t('Copied.');
      } catch {
        field.select();
        copied.textContent = t('Copy the key from the field.');
      }
    } }, t('Copy'));
    const dialog = h('dialog', { class: 'modal' },
      h('header', {}, h('h2', {}, t('Your recovery key'))),
      h('div', { class: 'stack dialog-body' },
        h('p', {}, t('You now have keys for chambers: rooms whose content only their people can read, not the stone. They were made on this device.')),
        h('p', {}, t('This is your recovery key. Another device, or this one after its data was cleared, needs it once to read your chambers. Write it down and keep it safe: it is shown only now, and nobody can recover it.')),
        field,
        h('div', { class: 'actions' }, copy, copied),
        h('label', { class: 'check' }, kept, t('I have written the recovery key down.')),
        h('div', { class: 'actions' }, done)));
    // Not closed by Escape: the key isn't shown again.
    dialog.addEventListener('cancel', (event) => event.preventDefault());
    dialog.addEventListener('close', () => {
      dialog.remove();
      resolve();
    });
    document.body.append(dialog);
    dialog.showModal();
    field.focus();
    field.select();
  });
}

/**
 * The member's keys for chambers: whether they have any, their fingerprint
 * to compare with what others see, and on a device which hasn't the keys
 * yet, the field for the recovery key. A member without keys can make them
 * here: someone added to a chamber needs them before an administrator can
 * give them its key (E2E.md, "Repair").
 */
export function openKeysDialog(): void {
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const body = h('div', { class: 'stack dialog-body' });
  const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, t('Keys for chambers')), close), body);
  dialog.addEventListener('close', () => dialog.remove());

  const render = async (): Promise<void> => {
    const state = keyState();
    if (state === 'none') {
      const error = h('p', { class: 'error', role: 'alert' });
      const make = h('button', { type: 'button', onclick: async () => {
        error.textContent = '';
        make.disabled = true;
        try {
          // Keys made on another device since this page was loaded stay: new ones would replace them.
          if ((await loadKeys()) === 'none') {
            await showRecoveryKey(await createKeys());
          }
          await render();
        } catch (e) {
          error.textContent = e instanceof Error ? e.message : String(e);
          make.disabled = false;
        }
      } }, t('Make my keys'));
      body.replaceChildren(
        h('p', {}, t('You have no keys for chambers yet. They are made when you create a chamber or join one by a link, or here.')),
        h('p', {}, t('If you were added to a chamber, you need keys before an administrator of it can give you its key.')),
        h('p', { class: 'muted small' }, t('A chamber is a room with end-to-end encryption: the devices of its members encrypt and decrypt, so only they can read the content, with the key of the chamber.')),
        error,
        h('div', { class: 'actions' }, make));
      return;
    }
    if (state === 'ready') {
      body.replaceChildren(
        h('p', {}, t('This device has your keys.')),
        h('p', {}, t('Your fingerprint:'), ' ', h('code', { class: 'fingerprint' }, (await ownFingerprint()) ?? '')),
        h('p', { class: 'muted small' }, t('Others see this fingerprint beside your name in a chamber. If you compare it with them, by voice or in person, you both know that nobody put another key in between.')));
      return;
    }
    const key = h('textarea', { rows: 3, required: true, class: 'recovery-key', autocomplete: 'off', spellcheck: false, placeholder: 'ABCD-EFGH-…' });
    const error = h('p', { class: 'error', role: 'alert' });
    const submit = h('button', { type: 'submit' }, t('Unlock'));
    const form = h('form', { class: 'stack' },
      h('p', {}, t('You have keys for chambers, but this device does not have them yet. Enter your recovery key, which you wrote down when the keys were made.')),
      h('label', {}, t('Recovery key'), key),
      error,
      h('div', { class: 'actions' }, submit));
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      error.textContent = '';
      submit.disabled = true;
      try {
        await unlockKeys(key.value);
        // What was closed is opened anew.
        forgetRooms();
        await render();
      } catch (e) {
        error.textContent = e instanceof Error ? e.message : String(e);
        submit.disabled = false;
      }
    });
    body.replaceChildren(form);
    key.focus();
  };

  document.body.append(dialog);
  dialog.showModal();
  void render();
}
