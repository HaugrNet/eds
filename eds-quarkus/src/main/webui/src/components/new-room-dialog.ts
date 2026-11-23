import { createRoom } from '../account';
import { h } from '../dom';
import { errorMessage } from '../session';
import { t } from '../i18n';
import { keyState } from '../e2e/identity';
import { showRecoveryKey } from './keys-dialog';
import { offeredFeatures, FEATURES } from '../features/features';
import { featureLabel } from './features-dialog';

/**
 * Creates a room, or a chamber: a room whose content Rune encrypts before
 * the stone gets it (E2E.md). The choice is made here and stays. `created`
 * gets the ID of the new room.
 */
export function openNewRoomDialog(created: (circleId: string) => void): void {
  const name = h('input', { required: true, maxlength: 75, autocomplete: 'off', placeholder: t('Name of the new room') });
  const room = h('input', { type: 'radio', name: 'kind', value: 'room', checked: true });
  const chamber = h('input', { type: 'radio', name: 'kind', value: 'chamber' });
  const warning = h('div', { class: 'stack notice', hidden: true },
    h('p', {}, t('End-to-end encryption needs a key pair for every member: their keys for chambers. The app makes them once per account, with a recovery key for further devices.')),
    h('p', {}, t('A member invited by link gets their keys and the key of the chamber on accepting.')),
    h('p', {}, t('A member added from an existing account makes their keys first: in Rune under "Keys for chambers" in the account menu, in Tolk with tolk key create. The app of an administrator then gives them the key of the chamber, as soon as the administrator has it open in Rune or lists its members in Tolk.')),
    h('p', {}, t('Every member needs an app which supports chambers: Rune or Tolk. Other apps show ciphertext, and what they write is not end-to-end encrypted.')),
    h('p', {}, t('The choice is final: a room cannot become a chamber, nor a chamber a room.')),
    keyState() === 'none'
      ? h('p', {}, t('You have no keys yet. They are made with this chamber, and you get the recovery key to write down.'))
      : keyState() === 'locked'
        ? h('p', { class: 'error' }, t('This device does not have your keys yet. Enter your recovery key first, under your name in the menu.'))
        : null);
  const choose = () => { warning.hidden = !chamber.checked; };
  room.addEventListener('change', choose);
  chamber.addEventListener('change', choose);
  // The features of the new room, of those the stone offers; the chat is ticked.
  const features: { name: string; box: HTMLInputElement }[] = [];
  const featureChoice = h('fieldset', { class: 'stack', hidden: true }, h('legend', {}, t('Features')));
  const offered = offeredFeatures().then((offered) => {
    for (const feature of FEATURES.filter((known) => offered.has(known))) {
      const box = h('input', { type: 'checkbox', checked: feature === 'chat' });
      features.push({ name: feature, box });
      featureChoice.append(h('label', { class: 'check' }, box, featureLabel(feature)));
    }
    featureChoice.append(h('p', { class: 'muted small' }, t('They can be switched on and off later, under "Features".')));
    featureChoice.hidden = features.length === 0;
  }).catch(() => undefined);
  const error = h('p', { class: 'error', role: 'alert' });
  const submit = h('button', { type: 'submit' }, t('Create'));
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const form = h('form', { class: 'stack dialog-body' },
    h('label', {}, t('Name'), name),
    h('fieldset', { class: 'stack' },
      h('legend', {}, t('Kind')),
      h('label', { class: 'check' }, room, h('span', {}, h('strong', {}, t('Room')), ' ', h('span', { class: 'muted' }, t('Encrypted in transit and in storage. The stone encrypts and decrypts for the members of the room, and only they can read the content.')))),
      h('label', { class: 'check' }, chamber, h('span', {}, h('strong', {}, t('Chamber')), ' ', h('span', { class: 'muted' }, t('A room with end-to-end encryption on top: the devices of the members encrypt and decrypt, so the stone never handles readable content.'))))),
    warning,
    featureChoice,
    error,
    h('div', { class: 'actions' }, submit));
  const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, t('New room')), close), form);
  dialog.addEventListener('close', () => dialog.remove());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    submit.disabled = true;
    try {
      // What the stone offers may still be on its way; if it can't be read, the room gets the chat, as rooms always had one.
      await offered;
      const chosen = featureChoice.hidden && features.length === 0 ? undefined : features.filter((feature) => feature.box.checked).map((feature) => feature.name);
      const made = await createRoom(name.value.trim(), chamber.checked, chosen);
      dialog.close();
      if (made.recoveryKey) {
        await showRecoveryKey(made.recoveryKey);
      }
      created(made.circleId);
    } catch (e) {
      error.textContent = e instanceof Error && !('response' in e) ? e.message : await errorMessage(e);
      submit.disabled = false;
    }
  });

  document.body.append(dialog);
  dialog.showModal();
  name.focus();
}
