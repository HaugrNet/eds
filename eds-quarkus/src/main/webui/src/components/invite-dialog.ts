import { TrustLevel, type Circle } from '../api';
import { inviteToRoom } from '../account';
import { h } from '../dom';
import { invitationLink } from '../invitation';
import { qrSvg } from '../qr';
import { t } from '../i18n';

/**
 * Invites someone into a room. For a new person, the dialog shows a QR code
 * and the link: scanning it opens Rune, where they choose their passphrase
 * and are in the room at once. A name that already uses Rune is added
 * directly.
 */
export function openInviteDialog(circle: Circle): void {
  const name = h('input', { name: 'name', required: true, maxlength: 75, autocomplete: 'off', placeholder: t('Name of the person') });
  const level = h('select', { name: 'level' },
    h('option', { value: TrustLevel.Read }, t('Can read')),
    h('option', { value: TrustLevel.Write, selected: true }, t('Can read and write')),
    h('option', { value: TrustLevel.Admin }, t('Administrator (can invite and manage)')));
  const error = h('p', { class: 'error', role: 'alert' });
  const submit = h('button', { type: 'submit' }, t('Invite'));
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const body = h('div', { class: 'dialog-body' });
  const form = h('form', { class: 'stack' },
    h('label', {}, t('Name'), name),
    h('label', {}, t('Access'), level),
    h('p', { class: 'muted small' }, t('The name becomes their account name in Rune.')),
    error,
    h('div', { class: 'actions' }, submit));
  body.append(form);
  const dialog = h('dialog', { class: 'modal' },
    h('header', {}, h('h2', {}, t('Invite to {room}', { room: circle.circleName ?? t('the room') })), close),
    body);
  dialog.addEventListener('close', () => dialog.remove());

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    submit.disabled = true;
    const invited = name.value.trim();
    try {
      const result = await inviteToRoom(circle.circleId as string, invited, level.value as TrustLevel);
      body.replaceChildren(result.token ? showInvitation(invited, result.token, result.secret) : h('p', {}, result.message));
    } catch (e) {
      error.textContent = e instanceof Error ? e.message : String(e);
    } finally {
      submit.disabled = false;
    }
  });

  document.body.append(dialog);
  dialog.showModal();
  name.focus();
}

function showInvitation(name: string, token: string, secret?: Uint8Array): HTMLElement {
  // For a chamber the link also carries the invite secret, under which the
  // room key waits; it never reaches the stone.
  const link = invitationLink({ name, token, secret });
  const copied = h('span', { class: 'muted small', role: 'status' });
  const copy = h('button', { type: 'button', class: 'secondary', onclick: async () => {
    try {
      await navigator.clipboard.writeText(link);
      copied.textContent = t('Copied.');
    } catch {
      copied.textContent = t('Copy the link from the field.');
    }
  } }, t('Copy link'));
  const share = ('share' in navigator)
    ? h('button', { type: 'button', class: 'secondary', onclick: () => {
      navigator.share({ title: 'Rune invitation', text: t('{name}, you are invited to a room in Rune.', { name }), url: link }).catch(() => undefined);
    } }, t('Share'))
    : null;
  return h('div', { class: 'stack invitation' },
    h('p', {}, t('Let {name} scan this code with their phone, or send them the link. They choose their own passphrase and join the room.', { name })),
    h('div', { class: 'qr' }, qrSvg(link, t('Invitation for {name}', { name }))),
    h('input', { value: link, readonly: true, 'aria-label': t('Invitation link'), onfocus: (event: Event) => (event.target as HTMLInputElement).select() }),
    h('div', { class: 'actions' }, copy, share, copied),
    h('p', { class: 'muted small' }, t('The invitation works once and expires after 7 days. Anyone with the link can accept it, so only share it with them.')),
    secret ? h('p', { class: 'muted small' }, t('This is a chamber: the link also carries its key for {name}. Give it to them directly, not through someone who should not read the chamber.', { name })) : null);
}
