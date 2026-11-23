import { MemberRole, TrustLevel, type Circle, type Member } from '../api';
import { openNewRoomDialog } from './new-room-dialog';
import { isChamberRoom } from '../e2e/rooms';
import { clear, h } from '../dom';
import { addMember, allRooms, renameRoom, setRoomAvatar, deleteUser, isFirstAdministrator, isSystemAdministrator, listUsers, myTrustees, removeMember, roomsOf, setRole } from '../account';
import { count, unread, watchRooms } from '../notifications';
import { chat } from '../chat';
import { api, errorMessage, session } from '../session';
import { openNewUserDialog } from './new-user-dialog';
import { accessSelect, fillAdder, MEMBERSHIP_CHANGED, membershipChanged, roomMembersView } from './room-members';
import { t } from '../i18n';
import { avatar } from '../avatars';
import { openAvatarDialog } from './avatar-dialog';
import { openInviteDialog } from './invite-dialog';
import { withLight } from '../presence';
import { callButton } from './call-button';

/**
 * Rooms on top, users below. Clicking a room opens it (fires `select` with
 * the Circle) and shows its members; the arrow only shows them. Clicking a
 * user shows their rooms. Room administrators add and remove members there;
 * the System Administrator also creates and deletes users, and sees every
 * room. Rune calls a Circle a room; the code keeps the EDS name.
 */
export class EdsCircles extends HTMLElement {
  private list = h('ul', { class: 'circles' });
  private users = h('ul', { class: 'users' });
  private selected: string | null = null;
  private generation = 0;
  private status = h('p', { class: 'error small', role: 'alert' });
  private usersStatus = h('p', { class: 'error small', role: 'alert' });
  private newUser = h('button', { type: 'button', hidden: true, onclick: () => this.createUser() }, t('New user'));

  /** Open rooms and users, kept over reloads. */
  private expandedRooms = new Set<string>();
  private expandedUsers = new Set<string>();
  /** Reloads the open details, after a membership changed. */
  private openDetails = new Map<string, () => Promise<void>>();
  /** The own trust level per room. */
  private levels = new Map<string, TrustLevel>();
  private systemAdministrator = false;
  /** The first System Administrator, who alone makes, unmakes or deletes administrators. */
  private firstAdministrator = false;
  /** The rooms the member is in, as last loaded. */
  private own: Circle[] = [];
  /** Ends the listeners of the last rendered list. */
  private rendered = new AbortController();
  private listeners = new AbortController();
  /** What the lists were made of when last shown, to tell a change. */
  private seen = '';
  private watch: number | undefined;

  connectedCallback(): void {
    const create = h('button', { type: 'button', onclick: () => this.create() }, t('New room'));
    this.replaceChildren(
      h('section', { class: 'side-section', 'aria-label': t('Rooms') },
        h('div', { class: 'pane-head' }, h('h2', { class: 'pane-title' }, t('Rooms')), create), this.status, this.list),
      h('section', { class: 'side-section', 'aria-label': t('Users') },
        h('div', { class: 'pane-head' }, h('h2', { class: 'pane-title' }, t('Users')), this.newUser), this.usersStatus, this.users));
    const signal = this.listeners.signal;
    document.addEventListener(MEMBERSHIP_CHANGED, () => void this.refreshDetails(), { signal });
    document.addEventListener('avatars-changed', () => void this.load(), { signal });
    document.addEventListener('room-renamed', () => void this.load(), { signal });
    // Rooms one was added to or removed from, and new rooms and users: looked
    // for every 20 seconds, when coming back to Rune, and when the stone says
    // that a room was left or the connection is back.
    this.watch = window.setInterval(() => void this.check(), 20_000);
    document.addEventListener('visibilitychange', () => void this.check(), { signal });
    window.addEventListener('focus', () => void this.check(), { signal });
    chat.addEventListener('left', () => void this.check(), { signal });
    chat.addEventListener('state', () => void this.check(), { signal });
    void this.load();
  }

  disconnectedCallback(): void {
    window.clearInterval(this.watch);
    this.listeners.abort();
    this.rendered.abort();
  }

  /** Creates a room or a chamber; the creator administers it. */
  private create(): void {
    this.status.textContent = '';
    openNewRoomDialog((circleId) => {
      this.selected = circleId;
      void this.load().then(() => this.list.querySelector<HTMLButtonElement>('button[aria-current="true"]')?.click());
    });
  }

  /** Renames a room; the list, the header and the files follow. */
  private async rename(circle: Circle): Promise<void> {
    const name = prompt(t('New name of {room}', { room: circle.circleName ?? '' }), circle.circleName ?? '')?.trim();
    if (!name || name === circle.circleName) {
      return;
    }
    this.status.textContent = '';
    try {
      await renameRoom(circle.circleId ?? '', name);
    } catch (e) {
      this.status.textContent = await errorMessage(e);
    }
  }

  private createUser(): void {
    openNewUserDialog((memberId) => {
      if (memberId) {
        this.expandedUsers.add(memberId);
      }
      void this.load();
    }, this.firstAdministrator);
  }

  /** Loads rooms and users; only the newest of loads running at once fills the pane. */
  async load(): Promise<void> {
    await this.fill(false);
  }

  /**
   * Looks whether the rooms or the users changed, and shows them anew if so:
   * the stone doesn't tell a member that they were added to a room, or that
   * a room or a user was made. What is open or being typed stays as long as
   * nothing changed.
   */
  private async check(): Promise<void> {
    if (document.visibilityState === 'visible' && session()) {
      await this.fill(true).catch(() => undefined);
    }
  }

  private async fill(onlyChanged: boolean): Promise<void> {
    const generation = ++this.generation;
    const [trustees, users] = await Promise.allSettled([myTrustees(), listUsers()]);
    const me = users.status === 'fulfilled' ? users.value.find((u) => u.memberId === session()?.memberId) : undefined;
    const systemAdministrator = isSystemAdministrator(me);
    const firstAdministrator = isFirstAdministrator(me);
    let everyRoom: Circle[] = [];
    if (systemAdministrator) {
      try {
        everyRoom = await allRooms();
      } catch {
        // Then only the own rooms.
      }
    }
    if (generation !== this.generation) {
      return;
    }
    // What the lists are made of; a failed look changes nothing by itself.
    const seen = trustees.status === 'fulfilled' && users.status === 'fulfilled'
      ? JSON.stringify([
        trustees.value.map((trustee) => [trustee.circleId, trustee.circleName, trustee.circleAvatar, trustee.trustLevel]),
        everyRoom.map((circle) => [circle.circleId, circle.circleName, circle.avatar]),
        users.value.map((user) => [user.memberId, user.accountName, user.avatar, user.memberRole, user.firstAdministrator]),
      ])
      : '';
    if (onlyChanged && (seen === '' || seen === this.seen)) {
      return;
    }
    this.seen = seen;
    this.rendered.abort();
    this.rendered = new AbortController();
    this.openDetails.clear();
    this.systemAdministrator = systemAdministrator;
    this.firstAdministrator = firstAdministrator;
    this.newUser.hidden = !systemAdministrator;
    this.status.textContent = '';
    this.usersStatus.textContent = '';

    if (trustees.status === 'fulfilled') {
      const before = this.levels;
      this.levels = new Map(trustees.value.map((trustee) => [trustee.circleId ?? '', trustee.trustLevel ?? TrustLevel.Read]));
      // Someone changed the own access to a room: the panes of the room,
      // if it is open, follow (what may be written, what is shown).
      for (const [circleId, level] of this.levels) {
        if (before.has(circleId) && before.get(circleId) !== level) {
          membershipChanged(circleId);
        }
      }
      const own: Circle[] = trustees.value.map((trustee) => ({ circleId: trustee.circleId, circleName: trustee.circleName, avatar: trustee.circleAvatar }));
      this.own = [...own].sort((a, b) => (a.circleName ?? '').localeCompare(b.circleName ?? ''));
      // All rooms on the chat line: their unread counts and new messages.
      void watchRooms(own);
      this.dispatchEvent(new CustomEvent('rooms', { bubbles: true }));
      const rooms = [...own, ...everyRoom.filter((c) => !this.levels.has(c.circleId ?? ''))]
        .sort((a, b) => (a.circleName ?? '').localeCompare(b.circleName ?? ''));
      this.list.replaceChildren(...rooms.map((circle) => this.roomItem(circle)));
      if (rooms.length === 0) {
        this.list.append(h('li', { class: 'muted' }, t('You haven\'t been invited to a room yet. Create one with "New room".')));
      }
    } else {
      this.list.replaceChildren(h('li', { class: 'error' }, await errorMessage(trustees.reason)));
    }

    if (users.status === 'fulfilled') {
      this.users.replaceChildren(...users.value.map((user) => this.userItem(user)));
    } else {
      this.users.replaceChildren(h('li', { class: 'error' }, await errorMessage(users.reason)));
    }
  }

  private async refreshDetails(): Promise<void> {
    await Promise.all([...this.openDetails.values()].map((reload) => reload()));
  }

  /** May the logged-in member add users to the room? */
  private administers(circleId: string): boolean {
    return this.levels.get(circleId) === TrustLevel.Admin || this.levels.get(circleId) === TrustLevel.Sysop;
  }

  // ---- Rooms

  private roomItem(circle: Circle): HTMLLIElement {
    const circleId = circle.circleId ?? '';
    const member = this.levels.has(circleId);
    const details = h('div', { class: 'details' });
    const li = h('li', { class: 'room' });
    const toggle = h('button', { class: 'icon disclose', 'aria-label': t('Members of {room}', { room: circle.circleName ?? '' }) }, '▸');
    // A chamber is marked; whether a room is one is only known to its members.
    const mark = h('span', { class: 'chamber-mark', hidden: true, title: t('A chamber: end-to-end encrypted. Only its members can read the content, with the key of the chamber on their devices.') }, t('Chamber'));
    if (member) {
      void isChamberRoom(circleId).then((chamber) => { mark.hidden = !chamber; }).catch(() => undefined);
    }
    const button = h('button', { class: 'circle', 'aria-current': this.selected === circleId ? 'true' : undefined },
      avatar('room', circle.avatar, circle.circleName ?? ''),
      h('span', { class: 'circle-name-text' }, circle.circleName ?? ''),
      mark,
      member ? this.badge(circleId) : h('span', { class: 'muted small', title: t('You are not in this room') }, t('not in it')));
    button.dataset.circleId = circleId;

    const expand = (open: boolean) => {
      if (open) {
        this.expandedRooms.add(circleId);
        const view = roomMembersView(circle, {
          manage: this.administers(circleId) || this.systemAdministrator,
          add: this.administers(circleId),
        });
        // Administrators of the room choose its avatar.
        const picture = this.administers(circleId)
          ? h('button', { type: 'button', class: 'secondary small', onclick: () => openAvatarDialog('room', t('Avatar of {room}', { room: circle.circleName ?? '' }), circle.circleName ?? '', circle.avatar,
            (id) => setRoomAvatar(circleId, id)) }, t('Room avatar…'))
          : null;
        // They also invite people who have no account yet, with a link or QR code.
        const invite = this.administers(circleId)
          ? h('button', { type: 'button', class: 'secondary small', onclick: () => openInviteDialog(circle) }, t('Invite someone'))
          : null;
        // And they rename the room.
        const rename = this.administers(circleId)
          ? h('button', { type: 'button', class: 'secondary small', onclick: () => void this.rename(circle) }, t('Rename room…'))
          : null;
        details.replaceChildren(view.element, ...(picture && invite && rename ? [h('div', { class: 'actions' }, invite, picture, rename)] : []));
        this.openDetails.set(`room:${circleId}`, view.reload);
        void view.reload();
      } else {
        this.expandedRooms.delete(circleId);
        this.openDetails.delete(`room:${circleId}`);
        clear(details);
      }
      details.hidden = !open;
      toggle.textContent = open ? '▾' : '▸';
      toggle.setAttribute('aria-expanded', String(open));
      button.setAttribute('aria-expanded', String(open));
    };

    toggle.addEventListener('click', () => expand(!this.expandedRooms.has(circleId)));
    button.addEventListener('click', () => {
      if (!member) {
        // The System Administrator sees the room, but can't open its chat and files.
        expand(!this.expandedRooms.has(circleId));
        return;
      }
      if (this.selected === circleId) {
        expand(!this.expandedRooms.has(circleId));
      } else if (!this.expandedRooms.has(circleId)) {
        expand(true);
      }
      this.selected = circleId;
      this.list.querySelectorAll('button.circle').forEach((b) => b.removeAttribute('aria-current'));
      button.setAttribute('aria-current', 'true');
      this.dispatchEvent(new CustomEvent('room-select', { detail: circle, bubbles: true }));
    });
    expand(this.expandedRooms.has(circleId));
    li.append(h('div', { class: 'row' }, button, toggle), details);
    return li;
  }

  /** The unread count of a room; kept up to date. */
  private badge(circleId: string): HTMLElement {
    const badge = h('span', { class: 'badge', 'aria-label': 'unread messages' });
    const show = () => {
      const n = count(circleId);
      badge.textContent = n > 99 ? '99+' : String(n);
      badge.hidden = n === 0;
    };
    unread.addEventListener('change', (event) => {
      if ((event as CustomEvent<string>).detail === circleId) {
        show();
      }
    }, { signal: this.rendered.signal });
    show();
    return badge;
  }

  /** The rooms the member is in, by name. */
  rooms(): Circle[] {
    return this.own;
  }

  /** Opens a room by its ID, e.g. from a notification. */
  open(circleId: string): void {
    const button = this.list.querySelector<HTMLButtonElement>(`button.circle[data-circle-id="${CSS.escape(circleId)}"]`);
    if (!button || !this.levels.has(circleId)) {
      return;
    }
    if (this.selected === circleId) {
      // Already open: show it again, without folding its members away.
      const circle: Circle = { circleId, circleName: button.querySelector('.circle-name-text')?.textContent ?? '' };
      this.dispatchEvent(new CustomEvent('room-select', { detail: circle, bubbles: true }));
    } else {
      button.click();
    }
  }

  // ---- Users

  private userItem(user: Member): HTMLLIElement {
    const memberId = user.memberId ?? '';
    const userName = user.accountName ?? '';
    const me = memberId === session()?.memberId;
    const details = h('div', { class: 'details' });
    const button = h('button', { class: 'user' },
      withLight(avatar('user', user.avatar, userName), memberId),
      h('span', { class: 'circle-name-text', title: userName }, userName, me ? h('span', { class: 'muted' }, ` ${t('(you)')}`) : null),
      isFirstAdministrator(user)
        ? h('span', { class: 'muted small', title: t('The first System Administrator, who claimed the stone') }, t('first admin'))
        : isSystemAdministrator(user) ? h('span', { class: 'muted small', title: t('System Administrator') }, t('admin')) : null);

    const expand = (open: boolean) => {
      if (open) {
        this.expandedUsers.add(memberId);
        const reload = this.userDetails(user, details);
        this.openDetails.set(`user:${memberId}`, reload);
        void reload();
      } else {
        this.expandedUsers.delete(memberId);
        this.openDetails.delete(`user:${memberId}`);
        clear(details);
      }
      details.hidden = !open;
      button.setAttribute('aria-expanded', String(open));
    };
    button.addEventListener('click', () => expand(!this.expandedUsers.has(memberId)));
    expand(this.expandedUsers.has(memberId));
    return h('li', { class: 'user-item' }, h('div', { class: 'row' }, button, callButton(memberId, userName)), details);
  }

  /** The rooms of a user, adding to and removing from rooms, and deleting the user. */
  private userDetails(user: Member, details: HTMLElement): () => Promise<void> {
    const memberId = user.memberId ?? '';
    const userName = user.accountName ?? '';
    const me = memberId === session()?.memberId;
    const status = h('p', { class: 'error small', role: 'alert' });
    const rooms = h('ul', { class: 'members' });
    const adder = h('div', { class: 'add-member' });
    const removable = !me && this.systemAdministrator && !isSystemAdministrator(user);
    const remove = removable
      ? h('button', { type: 'button', class: 'danger small', onclick: () => void this.deleteUser(user, status) }, t('Delete user'))
      : null;
    // Only the first System Administrator changes roles, and not its own.
    const roleChange = (!me && this.firstAdministrator && !isFirstAdministrator(user))
      ? h('button', { type: 'button', class: 'small', onclick: () => void this.changeRole(user, status) },
        isSystemAdministrator(user) ? t('Make user') : t('Make administrator'))
      : null;
    const actions = [roleChange, remove].filter((button) => button !== null);
    details.replaceChildren(h('div', { class: 'stack' }, status, rooms, adder, actions.length > 0 ? h('div', { class: 'actions' }, ...actions) : null));

    const reload = async (): Promise<void> => {
      status.textContent = '';
      let theirs: Circle[];
      try {
        theirs = await roomsOf(memberId);
      } catch (e) {
        status.textContent = await errorMessage(e);
        return;
      }
      rooms.replaceChildren(...theirs.map((circle) => this.userRoom(user, circle, status, reload)));
      if (theirs.length === 0) {
        rooms.append(h('li', { class: 'muted small' }, me ? t('You are in no room.') : this.systemAdministrator ? t('{name} is in no room.', { name: userName }) : t('{name} is in no room you can see.', { name: userName })));
      }
      // Rooms the logged-in member administers, without the user.
      const inRoom = new Set(theirs.map((c) => c.circleId));
      const candidates = [...this.list.querySelectorAll<HTMLButtonElement>('button.circle')]
        .map((b) => ({ circleId: b.dataset.circleId ?? '', circleName: b.querySelector('.circle-name-text')?.textContent ?? '' }))
        .filter((c) => this.administers(c.circleId) && !inRoom.has(c.circleId));
      if (me || candidates.length === 0) {
        clear(adder);
        return;
      }
      const room = h('select', { 'aria-label': t('Room to add {name} to', { name: userName }) },
        h('option', { value: '' }, t('Add to a room…')),
        ...candidates.map((c) => h('option', { value: c.circleId }, c.circleName)));
      const level = accessSelect(t('Access of {name}', { name: userName }));
      const add = h('button', { type: 'button' }, t('Add'));
      add.addEventListener('click', async () => {
        add.disabled = true;
        try {
          await addMember(room.value, memberId, level.value as TrustLevel);
          membershipChanged(room.value);
        } catch (e) {
          status.textContent = await errorMessage(e);
          add.disabled = false;
        }
      });
      fillAdder(adder, room, level, add);
    };
    return reload;
  }

  /** Makes a user an administrator, or an administrator a user. */
  private async changeRole(user: Member, status: HTMLElement): Promise<void> {
    const name = user.accountName ?? '';
    const toAdministrator = !isSystemAdministrator(user);
    const question = toAdministrator
      ? t('Make {name} an administrator? They can then manage the users, the rooms and the settings of the stone.', { name })
      : t('Make {name} a user again? They keep their rooms, but no longer manage the stone.', { name });
    if (!confirm(question)) {
      return;
    }
    try {
      await setRole(user.memberId as string, toAdministrator ? MemberRole.Admin : MemberRole.Standard);
      await this.load();
    } catch (e) {
      status.textContent = await errorMessage(e);
    }
  }

  private userRoom(user: Member, circle: Circle, status: HTMLElement, reload: () => Promise<void>): HTMLLIElement {
    const circleId = circle.circleId ?? '';
    const userName = user.accountName ?? '';
    const roomName = circle.circleName ?? '';
    const me = user.memberId === session()?.memberId;
    const name = this.levels.has(circleId)
      ? h('button', { type: 'button', class: 'link name', title: t('Open {room}', { room: roomName }), onclick: () => this.open(circleId) }, roomName)
      : h('span', { class: 'name', title: roomName }, roomName);
    const remove = (!me && (this.administers(circleId) || this.systemAdministrator))
      ? h('button', { type: 'button', class: 'icon', title: t('Remove {name} from {room}', { name: userName, room: roomName }), 'aria-label': t('Remove {name} from {room}', { name: userName, room: roomName }), onclick: async () => {
        if (!confirm(t('Remove {name} from {room}? They lose access to its chat and files.', { name: userName, room: roomName }))) {
          return;
        }
        try {
          await removeMember(circleId, user.memberId as string);
          membershipChanged(circleId);
          await reload();
        } catch (e) {
          status.textContent = await errorMessage(e);
        }
      } }, '✕')
      : h('span', {});
    return h('li', { class: 'user-room' },
      avatar('room', circle.avatar, roomName, 'small'),
      name, remove);
  }

  private async deleteUser(user: Member, status: HTMLElement): Promise<void> {
    const userName = user.accountName ?? '';
    if (!confirm(t('Delete {name}? Their account and memberships are removed for good; the rooms and their files stay.', { name: userName }))) {
      return;
    }
    try {
      await deleteUser(user.memberId as string);
      this.expandedUsers.delete(user.memberId ?? '');
      await this.load();
    } catch (e) {
      status.textContent = await errorMessage(e);
    }
  }
}

customElements.define('eds-circles', EdsCircles);
