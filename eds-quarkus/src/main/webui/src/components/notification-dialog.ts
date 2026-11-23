import { h } from '../dom';
import { level, setLevel, type NotificationLevel } from '../notifications';
import { t } from '../i18n';

/** Chooses whether, and how much, new messages are notified on this device. */
export function openNotificationDialog(onChange: () => void): void {
  const choices: [NotificationLevel, string, string][] = [
    ['off', t('Off'), t('Only the counts in the rooms list and the tab title.')],
    ['quiet', t('On, without details'), t('"New message in Rune": nothing about room, sender or text.')],
    ['room', t('On, with the room'), t('The room\'s name, not the sender or the text.')],
    ['full', t('On, with sender and text'), t('Visible to anyone who sees the screen, also when it is locked.')],
  ];
  const current = level();
  const error = h('p', { class: 'error small', role: 'alert' });
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const options = choices.map(([value, label, hint]) => {
    const radio = h('input', { type: 'radio', name: 'level', value, checked: value === current });
    radio.addEventListener('change', async () => {
      error.textContent = '';
      if (!await setLevel(value)) {
        error.textContent = t('The browser doesn\'t allow notifications for Rune. Allow them in its site settings, then try again.');
        (options[0].querySelector('input') as HTMLInputElement).checked = level() === 'off';
      }
      onChange();
    });
    return h('label', { class: 'choice' }, radio, h('span', {}, h('strong', {}, label), h('br', {}), h('span', { class: 'muted small' }, hint)));
  });
  const dialog = h('dialog', { class: 'modal' },
    h('header', {}, h('h2', {}, t('Notifications')), close),
    h('div', { class: 'dialog-body stack' }, ...options, error,
      h('p', { class: 'muted small' }, t('Notifications show while Rune is open in a tab. This choice is kept on this device only.'))));
  dialog.addEventListener('close', () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();
}
