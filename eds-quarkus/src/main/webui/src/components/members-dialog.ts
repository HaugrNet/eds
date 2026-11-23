import type { Circle } from '../api';
import { isAdministrator, removeMember } from '../account';
import { h } from '../dom';
import { errorMessage, session } from '../session';
import { openInviteDialog } from './invite-dialog';
import { roomMembersView } from './room-members';
import { t } from '../i18n';

/**
 * The members of a room and their access, as in the rooms pane, with
 * inviting and leaving.
 */
export function openMembersDialog(circle: Circle, administrator: boolean): void {
  const circleId = circle.circleId as string;
  const status = h('p', { class: 'error small', role: 'alert' });
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const view = roomMembersView(circle, { manage: administrator, add: administrator });
  const invite = administrator
    ? h('button', { type: 'button', onclick: () => { dialog.close(); openInviteDialog(circle); } }, t('Invite someone'))
    : null;
  const leave = h('button', { type: 'button', class: 'danger' }, t('Leave room'));
  const dialog = h('dialog', { class: 'modal' },
    h('header', {}, h('h2', {}, t('Members of {room}', { room: circle.circleName ?? t('the room') })), close),
    h('div', { class: 'dialog-body stack' }, status, view.element, h('div', { class: 'actions' }, invite, leave),
      administrator ? h('p', { class: 'muted small' }, t('Administrators can invite people, add users and change the access of others.')) : null));
  dialog.addEventListener('close', () => dialog.remove());

  leave.addEventListener('click', async () => {
    const members = view.members();
    const me = members.find((m) => m.memberId === session()?.memberId);
    const others = members.filter((m) => m !== me);
    const lastAdministrator = isAdministrator(me) && others.every((m) => !isAdministrator(m));
    const warning = others.length === 0
      ? t('You are the only member. The room and its files stay, but nobody can open them until the System Administrator deletes the room or adds someone.')
      : lastAdministrator
        ? t('You are the last administrator. Without one, only the System Administrator can manage the room.')
        : t('You lose access to its chat and files, until someone invites you again.');
    if (!confirm(`${t('Leave {room}?', { room: circle.circleName ?? t('the room') })} ${warning}`)) {
      return;
    }
    leave.disabled = true;
    try {
      await removeMember(circleId, session()?.memberId as string);
      // Only that the room was left: the app closes it and reads the rooms
      // anew. Asking for its members or files now would be refused.
      document.dispatchEvent(new CustomEvent('room-left', { detail: circleId }));
      dialog.close();
    } catch (e) {
      status.textContent = await errorMessage(e);
      leave.disabled = false;
    }
  });

  document.body.append(dialog);
  dialog.showModal();
  void view.reload();
}
