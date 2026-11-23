import { TrustLevel, type Circle, type Member, type Trustee } from '../api';
import { ACCESS, addMember, isAdministrator, listMembers, listUsers, removeMember, repairRoom, setAccess } from '../account';
import { fingerprintOf } from '../e2e/identity';
import { acceptKey, checkKey } from '../e2e/device';
import { memberKeyState, roomState } from '../e2e/rooms';
import { h } from '../dom';
import { errorMessage, session } from '../session';
import { t } from '../i18n';
import { avatar } from '../avatars';
import { withLight } from '../presence';
import { callButton } from './call-button';

/** Fired on the document when someone joined or left a room, or their access changed; detail is the circleId. */
export const MEMBERSHIP_CHANGED = 'membership-changed';

export function membershipChanged(circleId: string): void {
  document.dispatchEvent(new CustomEvent(MEMBERSHIP_CHANGED, { detail: circleId }));
}

export interface RoomMembersView {
  element: HTMLElement;
  /** The members as last loaded. */
  members: () => Trustee[];
  reload: () => Promise<void>;
}

export interface RoomMembersOptions {
  /** May change the access and remove members: administrators of the room, and the System Administrator. */
  manage: boolean;
  /**
   * May add existing users: only administrators of the room, as EDS needs
   * their access to the room's key to give it to the user.
   */
  add: boolean;
}

/** The members of a room, with their access. Everyone in the room sees them. */
export function roomMembersView(circle: Circle, options: RoomMembersOptions): RoomMembersView {
  const { manage: administrator, add: mayAdd } = options;
  const circleId = circle.circleId as string;
  const roomName = circle.circleName ?? t('the room');
  const list = h('ul', { class: 'members' });
  const status = h('p', { class: 'error small', role: 'alert' });
  const adder = h('div', { class: 'add-member' });
  const element = h('div', { class: 'room-members stack' }, status, list, adder);
  let members: Trustee[] = [];

  const failed = async (e: unknown) => {
    status.textContent = await errorMessage(e);
  };

  const render = async (): Promise<void> => {
    status.textContent = '';
    try {
      members = await listMembers(circleId);
    } catch (e) {
      await failed(e);
      return;
    }
    // An administrator of a chamber gives its key to those who have keys
    // and not yet the room's (E2E.md, "Repair").
    try {
      if ((await repairRoom(circleId, members)) > 0) {
        members = await listMembers(circleId);
      }
    } catch {
      // The members are shown all the same.
    }
    const administrators = members.filter(isAdministrator).length;
    const chamber = await roomState(circleId).catch(() => null);
    list.replaceChildren(...members.map((member) => item(member, administrators)));
    if (chamber && chamber.kind !== 'room') {
      // Beside each name its fingerprint, to compare, and whether the member has the key of the chamber.
      const items = [...list.querySelectorAll<HTMLLIElement>('li')];
      await Promise.all(members.map(async (member, index) => {
        const print = await fingerprintOf(member.publicKey);
        const state = memberKeyState(member, chamber.current?.version ?? 0);
        // Only administrators see the secrets of others, and so who has the key.
        const told = !administrator || state === 'current' || !chamber.current ? ''
          : state === 'invited' ? t('invited, not joined yet')
            : state === 'none' ? t('no keys yet')
              : t('waits for the key');
        const line = h('span', { class: 'muted small fingerprint key-line', title: t('Fingerprint of the key of {name}. Compare it with them.', { name: member.accountName ?? '' }) },
          print ?? '', told ? ` · ${told}` : '');
        // A key other than the one this device saw before: the stone hands the keys out, so it is told.
        const me = session()?.memberId;
        if (me && member.memberId && member.memberId !== me && member.publicKey && print
            && checkKey(me, member.memberId, member.publicKey) === 'changed') {
          const name = member.accountName ?? '';
          line.classList.add('key-changed');
          line.append(' ', h('strong', { class: 'error' }, t('The key changed.')), ' ',
            h('button', { type: 'button', class: 'link', onclick: () => {
              if (confirm(t('{name} has another key than this device saw before. That is so after {name} made new keys; it could also be a key someone put in between. Compare the fingerprint {fingerprint} with {name}, by voice or in person. Is it theirs?', { name, fingerprint: print }))) {
                acceptKey(me, member.memberId as string, member.publicKey as string);
                void render();
              }
            } }, t('Check…')));
        }
        items[index]?.prepend(line);
      }));
    }
    if (members.length === 0) {
      list.append(h('li', { class: 'muted small' }, t('Nobody is in this room.')));
    }
    if (mayAdd) {
      void renderAdder();
    }
  };

  const item = (member: Trustee, administrators: number): HTMLLIElement => {
    const me = member.memberId === session()?.memberId;
    const memberName = member.accountName ?? '';
    const name = h('span', { class: 'name', title: memberName }, memberName, me ? h('span', { class: 'muted' }, ` ${t('(you)')}`) : null);
    let access: HTMLElement;
    // Nobody changes their own access, and the only administrator keeps theirs: a room is not left without one by a slip.
    const only = isAdministrator(member) && administrators === 1;
    if (administrator && !me && !only) {
      const current = isAdministrator(member) ? TrustLevel.Admin : (member.trustLevel ?? TrustLevel.Read);
      const select = h('select', { class: 'access', 'aria-label': t('Access of {name}', { name: memberName }) },
        ...[TrustLevel.Read, TrustLevel.Write, TrustLevel.Admin].map((level) =>
          h('option', { value: level, selected: level === current }, ACCESS[level])));
      select.addEventListener('change', async () => {
        select.disabled = true;
        try {
          await setAccess(circleId, member.memberId as string, select.value as TrustLevel);
          membershipChanged(circleId);
          await render();
        } catch (e) {
          await failed(e);
          select.value = current;
          select.disabled = false;
        }
      });
      access = select;
    } else {
      const why = !administrator ? undefined
        : me ? t('You cannot change your own access.')
          : t('The only administrator of a room keeps this access.');
      access = h('span', { class: 'access', title: why }, ACCESS[isAdministrator(member) ? TrustLevel.Admin : (member.trustLevel ?? '')] ?? '');
    }
    // Administrators remove others; leaving is done with "Leave room".
    const remove = (administrator && !me)
      ? h('button', { type: 'button', class: 'icon', title: t('Remove {name}', { name: memberName }), 'aria-label': t('Remove {name} from the room', { name: memberName }), onclick: async () => {
        if (!confirm(only
          ? t('{name} is the last administrator of this room. Without one, only the System Administrator can manage the room. Continue?', { name: memberName })
          : t('Remove {name} from {room}? They lose access to its chat and files.', { name: memberName, room: roomName }))) {
          return;
        }
        try {
          await removeMember(circleId, member.memberId as string);
          membershipChanged(circleId);
          await render();
        } catch (e) {
          await failed(e);
        }
      } }, '✕')
      : h('span', {});
    return h('li', {},
      withLight(avatar('user', member.avatar, memberName), member.memberId),
      h('span', { class: 'name-line' }, name, callButton(member.memberId, memberName)), access, remove);
  };

  /** Administrators add users who have an account but aren't in the room. */
  const renderAdder = async (): Promise<void> => {
    let users: Member[];
    try {
      users = await listUsers();
    } catch (e) {
      await failed(e);
      return;
    }
    const inRoom = new Set(members.map((m) => m.memberId));
    const candidates = users.filter((u) => !inRoom.has(u.memberId));
    if (candidates.length === 0) {
      adder.replaceChildren();
      return;
    }
    const user = h('select', { 'aria-label': t('User to add to {room}', { room: roomName }) },
      h('option', { value: '' }, t('Add a user…')),
      ...candidates.map((u) => h('option', { value: u.memberId ?? '' }, u.accountName ?? '')));
    const level = accessSelect(t('Access in {room}', { room: roomName }));
    const add = h('button', { type: 'button' }, t('Add'));
    add.addEventListener('click', async () => {
      add.disabled = true;
      try {
        await addMember(circleId, user.value, level.value as TrustLevel);
        membershipChanged(circleId);
        await render();
      } catch (e) {
        await failed(e);
        add.disabled = false;
      }
    });
    fillAdder(adder, user, level, add);
  };

  return { element, members: () => members, reload: render };
}

/** A choice of access for a new member, "Can read and write" chosen. */
export function accessSelect(label: string): HTMLSelectElement {
  return h('select', { 'aria-label': label },
    ...[TrustLevel.Read, TrustLevel.Write, TrustLevel.Admin].map((level) =>
      h('option', { value: level, selected: level === TrustLevel.Write }, ACCESS[level])));
}

/**
 * Fills an "add" form: the choice of whom or where first; the access and the
 * button show once something is chosen, so the form stays one quiet field.
 */
export function fillAdder(adder: HTMLElement, choice: HTMLSelectElement, level: HTMLSelectElement, add: HTMLButtonElement): void {
  const show = () => {
    level.hidden = add.hidden = !choice.value;
    add.disabled = !choice.value;
  };
  choice.addEventListener('change', show);
  show();
  adder.replaceChildren(choice, level, add);
}
