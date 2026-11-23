import { h } from '../dom';
import { errorMessage } from '../session';
import { t } from '../i18n';

/**
 * A dialog with a form, as the features use them: a title, the fields, and a
 * button which runs `submit`; what goes wrong is told in the dialog, which
 * stays open, and it closes when all went well.
 */
export function formDialog(title: string, fields: (Node | null)[], submitLabel: string, submit: () => Promise<void>,
    extra: (HTMLElement | null)[] = []): HTMLDialogElement {
  const error = h('p', { class: 'error', role: 'alert' });
  const button = h('button', { type: 'submit' }, submitLabel);
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const form = h('form', { class: 'stack dialog-body' }, ...fields, error, h('div', { class: 'actions' }, button, ...extra));
  const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, title), close), form);
  dialog.addEventListener('close', () => dialog.remove());
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    button.disabled = true;
    try {
      await submit();
      dialog.close();
    } catch (e) {
      error.textContent = e instanceof Error && !('response' in e) ? e.message : await errorMessage(e);
      button.disabled = false;
    }
  });
  document.body.append(dialog);
  dialog.showModal();
  return dialog;
}
