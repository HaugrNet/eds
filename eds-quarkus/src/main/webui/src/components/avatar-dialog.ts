import { avatar, describe, pictures, type AvatarKind } from '../avatars';
import { h } from '../dom';
import { t } from '../i18n';
import { errorMessage } from '../session';

/**
 * Chooses an avatar: a user's own (creatures of the sagas) or a room's
 * (places and things). `save` stores the choice ('' for none, then the
 * initial is shown); the dialog closes when it succeeded.
 */
export function openAvatarDialog(kind: AvatarKind, title: string, name: string, current: string | undefined, save: (avatar: string) => Promise<void>): void {
  const error = h('p', { class: 'error small', role: 'alert' });
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const choose = async (id: string, button: HTMLButtonElement) => {
    error.textContent = '';
    grid.querySelectorAll('button').forEach((b) => { b.disabled = true; });
    try {
      await save(id);
      dialog.close();
    } catch (e) {
      error.textContent = await errorMessage(e);
      grid.querySelectorAll('button').forEach((b) => { b.disabled = false; });
      button.focus();
    }
  };
  const option = (id: string) => {
    const [label, hint] = id ? describe(id) : [t('No picture'), t('The initial of the name')];
    const button = h('button', {
      type: 'button', class: 'avatar-choice', 'aria-pressed': String((current ?? '') === id), title: hint,
    }, avatar(kind, id || undefined, name, 'large'), h('span', { class: 'avatar-name' }, label), h('span', { class: 'muted small' }, hint));
    button.addEventListener('click', () => void choose(id, button));
    return button;
  };
  const grid = h('div', { class: 'avatar-grid' }, ...pictures(kind).map(option), option(''));
  const dialog = h('dialog', { class: 'modal wide' },
    h('header', {}, h('h2', {}, title), close),
    h('div', { class: 'dialog-body stack' }, grid, error));
  dialog.addEventListener('close', () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();
  grid.querySelector<HTMLButtonElement>('[aria-pressed="true"]')?.focus();
}
