import type { Circle, Metadata } from '../api';
import { inChamber, shownSize } from '../e2e/files';
import { protectName, revealName } from '../e2e/names';
import { keyState } from '../e2e/identity';
import { openKeysDialog } from './keys-dialog';
import { chat, chatUnavailable, type ChatMessage, type ChatReceipt, type ChatTyping } from '../chat';
import { refusal, roomState } from '../e2e/rooms';
import { clear, formatSize, formatTime, h } from '../dom';
import { upload } from '../media';
import { api, errorMessage, session } from '../session';
import { foldersOf } from './file-actions';
import { DRAG_NAME, DRAG_TYPE } from './files';
import { listMembers } from '../account';
import { setCount, setOpenRoom } from '../notifications';
import { openPreview } from './preview';
import { calls } from '../calls';
import { startRoomCall } from './call-panel';

/** The reactions EDS accepts (Constants.CHAT_REACTIONS). */
const REACTIONS = ['👍', '❤️', '😂', '😮', '😢', '🙏'];
import { thumbnail } from './thumbnail';
import { t, tn } from '../i18n';
import { avatar } from '../avatars';

/** The checkmarks of an own message: sent, and received or read by everyone. */
const ONE_TICK = '<svg viewBox="0 0 16 12" width="16" height="12" aria-hidden="true"><path d="M3.5 6.5l3 3 6-7"/></svg>';
const TWO_TICKS = '<svg viewBox="0 0 21 12" width="21" height="12" aria-hidden="true"><path d="M1.5 6.5l3 3 6-7"/><path d="M9.5 8.5l1 1 6-7"/></svg>';

/** A time of EDS (UTC, without a zone) in milliseconds; NaN without one. */
function stamp(time: string | undefined): number {
  return time ? Date.parse(/Z|[+-]\d\d:\d\d$/.test(time) ? time : `${time}Z`) : NaN;
}

/**
 * The chat of a Circle. Shared files appear in the timeline as cards with
 * their thumbnail; a click opens the preview.
 */
export class EdsChat extends HTMLElement {
  private circle: Circle | null = null;
  private timeline = h('ol', { class: 'timeline', 'aria-live': 'polite' });
  private earlier = h('button', { class: 'link earlier', hidden: true, onclick: () => void this.loadEarlier() }, t('Earlier messages'));
  private text = h('textarea', { rows: 1, placeholder: t('Message'), 'aria-label': t('Message'), maxlength: 16384 });
  private status = h('p', { class: 'muted status' });
  private composer = h('form', { class: 'composer' });
  /** May the member write in the room: unknown (null) until the access is checked. */
  private writable: boolean | null = null;
  /** Why nothing is written to the room, although the member may write: its chat is closed, or its chamber waits for a key. */
  private blocked = '';
  private readOnly = h('p', { class: 'muted small read-only-note', hidden: true }, t('You can only read in this room.'));
  private oldest: string | undefined;
  /** Counts the rooms shown, so an answer for an earlier one is told apart. */
  private shown = 0;
  /** The chat connection was lost since the room was loaded. */
  private lost = false;
  private seen = new Set<string>();
  private files = new Map<string, Promise<Metadata | null>>();
  private listeners = new AbortController();
  /** The messages shown, for the quotes of replies. */
  private messages = new Map<string, ChatMessage>();
  /** The message the next one replies to. */
  private replyingTo: ChatMessage | null = null;
  private replyBar = h('div', { class: 'reply-bar', hidden: true });
  /** Up to which message each member has read (memberId → receipt). */
  private receipts = new Map<string, ChatReceipt>();
  /** The newest message marked read, and the timer of the next mark. */
  private markedRead: string | undefined;
  private readTimer: number | undefined;
  /** Who is writing now (memberId → name and timer), and when we last told. */
  private typists = new Map<string, { name: string; timer: number }>();
  private typingLine = h('div', { class: 'typing muted', 'aria-live': 'polite' });
  /** The call of the room: the buttons which begin one, or who is in it and "Join". */
  private callBar = h('div', { class: 'call-bar', hidden: true });
  private toldTyping = 0;
  /** Names of the room's members, for who reacted. */
  private names = new Map<string, string>();
  /** The avatars of the room's members (memberId → avatar ID). */
  private avatars = new Map<string, string>();
  /** The folder open in the files pane, the default target of uploads. */
  openFolder: () => string | undefined = () => undefined;

  connectedCallback(): void {
    const attach = h('input', { type: 'file', multiple: true, hidden: true });
    attach.addEventListener('change', () => {
      const chosen = Array.from(attach.files ?? []);
      attach.value = '';
      void this.chooseFolder(chosen);
    });
    this.composer.replaceChildren(
      h('button', { type: 'button', class: 'icon', title: t('Share a file'), 'aria-label': t('Share a file'), onclick: () => attach.click() }, '📎'),
      this.text,
      h('button', { type: 'submit' }, t('Send')),
      attach);
    this.composer.addEventListener('submit', (event) => { event.preventDefault(); void this.send(); });
    this.text.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        void this.send();
      }
    });
    this.listeners = new AbortController();
    const signal = this.listeners.signal;
    chat.addEventListener('message', (event) => this.received((event as CustomEvent<ChatMessage>).detail), { signal });
    // Who is writing: shown for 5 s after their last TYPING.
    chat.addEventListener('typing', (event) => {
      const frame = (event as CustomEvent<ChatTyping>).detail;
      if (frame.circleId !== this.circle?.circleId || !frame.memberId) {
        return;
      }
      const id = frame.memberId;
      window.clearTimeout(this.typists.get(id)?.timer);
      this.typists.set(id, { name: frame.accountName ?? 'someone', timer: window.setTimeout(() => this.stopTyping(id), 5000) });
      this.showTyping();
    }, { signal });
    // Receipts: the checkmarks of the own messages.
    chat.addEventListener('receipt', (event) => {
      const { circleId, receipt } = (event as CustomEvent<{ circleId: string; receipt: ChatReceipt }>).detail;
      if (circleId === this.circle?.circleId && receipt.memberId && receipt.memberId !== session()?.memberId) {
        this.receipts.set(receipt.memberId, receipt);
        this.showReceipts();
      }
    }, { signal });
    // Marks read when the tab comes back.
    document.addEventListener('visibilitychange', () => this.scheduleRead(), { signal });
    (this.timeline.parentElement ?? this).addEventListener('scroll', () => this.scheduleRead(), { signal, passive: true } as AddEventListenerOptions);
    // An edited message changes for everyone in the room, in its place.
    chat.addEventListener('edited', (event) => {
      const message = (event as CustomEvent<ChatMessage>).detail;
      if (message.circleId === this.circle?.circleId) {
        this.timeline.querySelector(`li[data-message-id="${CSS.escape(message.messageId ?? '')}"]`)?.replaceWith(this.item(message));
        this.showReceipts();
      }
    }, { signal });
    // A deleted message goes for everyone in the room.
    chat.addEventListener('deleted', (event) => {
      const frame = (event as CustomEvent<{ circleId?: string; messageId?: string }>).detail;
      if (frame.circleId === this.circle?.circleId) {
        this.messages.delete(frame.messageId ?? '');
        this.timeline.querySelector(`li[data-message-id="${CSS.escape(frame.messageId ?? '')}"]`)?.remove();
        this.showReceipts();
      }
    }, { signal });
    chat.addEventListener('left', (event) => {
      if ((event as CustomEvent).detail.circleId === this.circle?.circleId) {
        this.status.textContent = t('You are no longer in this room.');
        this.composer.hidden = true;
      }
    }, { signal });
    // The chat of the room was switched on, off or to read-only.
    chat.addEventListener('feature', (event) => {
      const frame = (event as CustomEvent<{ circleId?: string; feature?: string }>).detail;
      if (this.circle && frame.circleId === this.circle.circleId && (!frame.feature || frame.feature === 'chat')) {
        void this.show(this.circle);
      }
    }, { signal });
    chat.addEventListener('state', (event) => {
      const connected = (event as CustomEvent<boolean>).detail;
      if (!this.circle) {
        return;
      }
      if (!connected) {
        this.lost = true;
        this.status.textContent = t('Reconnecting…');
      } else if (this.lost) {
        // What was sent meanwhile never arrived: the room is read again.
        void this.show(this.circle);
      } else {
        this.status.textContent = '';
      }
    }, { signal });
    calls.addEventListener('change', () => this.showCallBar(), { signal });
    this.replaceChildren(this.callBar, this.status, h('div', { class: 'scroller' }, this.earlier, this.timeline), this.typingLine, this.replyBar, this.composer, this.readOnly);
    // Dropped files are shared like chosen ones; a file dragged from the
    // files pane is in the room already, and is shared as it is.
    const accepts = (event: DragEvent) => {
      const types = Array.from(event.dataTransfer?.types ?? []);
      return !!this.circle?.circleId && this.writable === true && (types.includes('Files') || types.includes(DRAG_TYPE));
    };
    this.addEventListener('dragover', (event) => {
      if (accepts(event)) {
        event.preventDefault();
        this.dataset.dropHint = t('Drop to share');
        this.classList.add('dropping');
      }
    }, { signal: this.listeners.signal });
    this.addEventListener('dragleave', (event) => {
      if (!this.contains(event.relatedTarget as Node | null)) {
        this.classList.remove('dropping');
      }
    }, { signal: this.listeners.signal });
    this.addEventListener('drop', (event) => {
      this.classList.remove('dropping');
      if (!accepts(event)) {
        return;
      }
      event.preventDefault();
      const dataId = event.dataTransfer?.getData(DRAG_TYPE);
      if (dataId) {
        void this.shareExisting(dataId, event.dataTransfer?.getData(DRAG_NAME) || t('A file'));
      } else {
        const files = Array.from(event.dataTransfer?.files ?? []);
        if (files.length > 0) {
          void this.chooseFolder(files);
        }
      }
    }, { signal: this.listeners.signal });
    // Pasted images are shared like chosen files; text pastes as text.
    this.text.addEventListener('paste', (event) => {
      const images = Array.from(event.clipboardData?.files ?? []).filter((file) => file.type.startsWith('image/'));
      if (images.length > 0 && this.circle?.circleId && this.writable === true) {
        event.preventDefault();
        void this.chooseFolder(images.map((image, index) => pastedName(image, index)));
      }
    }, { signal: this.listeners.signal });
    // Typing: told at most every 3 s while writing.
    this.text.addEventListener('input', () => {
      const circleId = this.circle?.circleId;
      if (circleId && this.writable === true && this.text.value.trim() && Date.now() - this.toldTyping > 3000) {
        this.toldTyping = Date.now();
        chat.typing(circleId);
      }
    }, { signal: this.listeners.signal });
    this.show(null);
  }

  disconnectedCallback(): void {
    this.listeners.abort();
  }



  async show(circle: Circle | null): Promise<void> {
    // The previous room stays joined: its new messages count as unread.
    this.circle = circle;
    const shown = ++this.shown;
    setOpenRoom(circle?.circleId ?? null, () => this.isAtEnd());
    this.oldest = undefined;
    this.seen.clear();
    this.receipts.clear();
    this.markedRead = undefined;
    for (const id of [...this.typists.keys()]) {
      this.stopTyping(id);
    }
    clear(this.timeline);
    this.earlier.hidden = true;
    this.blocked = '';
    this.composer.hidden = !circle;
    this.showWritable();
    this.showCallBar();
    if (!circle?.circleId) {
      this.status.textContent = t('Choose a room.');
      return;
    }
    this.status.textContent = t('Loading…');
    void listMembers(circle.circleId).then((members) => {
      this.names = new Map(members.map((m) => [m.memberId ?? '', m.accountName ?? '']));
      this.avatars = new Map(members.map((m) => [m.memberId ?? '', m.avatar ?? '']));
      this.showAvatars();
      this.showReceipts();
      this.showCallBar();
    }).catch(() => undefined);
    try {
      await chat.join(circle.circleId);
      // Whether a call goes on in the room is asked anew when it is looked at.
      calls.look(circle.circleId);
      this.showCallBar();
      // A chamber whose key this device hasn't, or a chat which is closed
      // or not there: what can be read is shown, and nothing is written.
      const room = await roomState(circle.circleId);
      const blocked = refusal(room) ?? chatUnavailable(chat.state(circle.circleId));
      const messages = await chat.history(circle.circleId);
      if (this.shown !== shown) {
        return;
      }
      this.lost = false;
      this.blocked = blocked;
      this.showWritable();
      this.status.textContent = blocked || (messages.length === 0 ? t('No messages yet.') : '');
      if (keyState() === 'locked' && blocked) {
        this.status.append(' ', h('button', { type: 'button', class: 'link', onclick: () => openKeysDialog() }, t('Enter recovery key')));
      } else if (keyState() === 'none' && room.kind === 'waiting') {
        this.status.append(' ', h('button', { type: 'button', class: 'link', onclick: () => openKeysDialog() }, t('Make my keys')));
      }
      this.prependMessages(messages);
      this.scrollToEnd();
      this.showReceipts();
      this.scheduleRead();
    } catch (e) {
      const message = await errorMessage(e);
      if (this.shown === shown) {
        // Tried again when the chat is connected again.
        this.lost = true;
        this.status.textContent = message;
      }
    }
  }

  /**
   * The call of the room, where the room has calls: two buttons which begin
   * one, or while one goes on who is in it, and "Join".
   */
  private showCallBar(): void {
    const circleId = this.circle?.circleId;
    if (!circleId || !calls.supported() || !calls.usable(circleId)) {
      this.callBar.hidden = true;
      this.callBar.replaceChildren();
      return;
    }
    this.callBar.hidden = false;
    const busy = !!calls.active();
    const members = calls.inRoomCall(circleId);
    if (members.length === 0) {
      this.callBar.replaceChildren(
        h('button', { type: 'button', class: 'secondary small', disabled: busy, onclick: () => void startRoomCall(circleId, 'audio') }, t('Voice call')),
        h('button', { type: 'button', class: 'secondary small', disabled: busy, onclick: () => void startRoomCall(circleId, 'video') }, t('Video call')));
      return;
    }
    const me = session()?.memberId;
    const names = members.map((memberId) => (memberId === me ? t('You') : this.names.get(memberId) ?? t('someone'))).join(', ');
    this.callBar.replaceChildren(h('span', { class: 'call-going', role: 'status' }, t('In a call: {names}', { names })));
    if (!calls.inCallOf(circleId)) {
      this.callBar.append(h('button', { type: 'button', class: 'small', disabled: busy, onclick: () => void startRoomCall(circleId, 'audio') }, t('Join')));
    }
  }

  private async loadEarlier(): Promise<void> {
    if (!this.circle?.circleId || !this.oldest) {
      return;
    }
    const scroller = this.timeline.parentElement as HTMLElement;
    const height = scroller.scrollHeight;
    const shown = this.shown;
    const messages = await chat.history(this.circle.circleId, this.oldest);
    if (this.shown !== shown) {
      // A room was opened meanwhile, maybe this one again: the answer
      // belongs to a timeline which is gone.
      return;
    }
    this.prependMessages(messages);
    this.showReceipts();
    scroller.scrollTop += scroller.scrollHeight - height;
  }

  private prependMessages(messages: ChatMessage[]): void {
    const items = messages.filter((m) => !this.seen.has(m.messageId ?? '')).map((m) => this.item(m));
    this.timeline.prepend(...items);
    if (messages.length > 0) {
      this.oldest = messages[0].messageId;
    }
    this.earlier.hidden = messages.length < 50;
  }

  private received(message: ChatMessage): void {
    if (message.circleId !== this.circle?.circleId || this.seen.has(message.messageId ?? '')) {
      return;
    }
    // Their message is there: they stopped writing.
    this.stopTyping(message.memberId ?? '');
    const atEnd = this.isAtEnd();
    this.status.textContent = '';
    this.timeline.append(this.item(message));
    this.showReceipts();
    if (atEnd || message.memberId === session()?.memberId) {
      this.scrollToEnd();
    }
    this.scheduleRead();
    if (message.dataId) {
      this.dispatchEvent(new CustomEvent('shared', { detail: message, bubbles: true }));
    }
  }

  /**
   * Members who can only read the room see the messages and may mark them
   * read and delete their own, but not write, reply, react, edit or share:
   * the composer gives way to a note. Until the access is known (null), the
   * composer shows, disabled. EDS refuses the frames anyway.
   */
  setWritable(writable: boolean | null): void {
    this.writable = writable;
    this.showWritable();
  }

  private showWritable(): void {
    const readOnly = !!this.circle && this.writable === false;
    this.classList.toggle('read-only', readOnly);
    this.readOnly.hidden = !readOnly;
    if (readOnly) {
      this.composer.hidden = true;
      this.replyTo(null);
      this.timeline.querySelectorAll('.reaction-picker').forEach((picker) => picker.remove());
    } else if (this.circle) {
      this.composer.hidden = this.blocked !== '';
    }
    for (const control of this.composer.querySelectorAll<HTMLButtonElement | HTMLTextAreaElement>('button, textarea')) {
      control.disabled = this.writable !== true || this.blocked !== '';
    }
  }

  private async send(): Promise<void> {
    const text = this.text.value.trim();
    if (!text || !this.circle?.circleId || this.writable !== true) {
      return;
    }
    this.text.value = '';
    const replyTo = this.replyingTo?.messageId;
    this.replyTo(null);
    try {
      await chat.send(this.circle.circleId, text, undefined, replyTo);
    } catch (e) {
      this.text.value = text;
      this.status.textContent = await errorMessage(e);
    }
  }

  /** Shares a file of the room in the chat, with the typed text or its name. */
  private async shareExisting(dataId: string, name: string): Promise<void> {
    const circleId = this.circle?.circleId;
    if (!circleId || this.writable !== true) {
      return;
    }
    const text = this.text.value.trim() || name;
    const replyTo = this.replyingTo?.messageId;
    try {
      await chat.send(circleId, text, dataId, replyTo);
      this.text.value = '';
      this.replyTo(null);
    } catch (e) {
      this.status.textContent = await errorMessage(e);
    }
  }

  /**
   * Asks where the files go: the top level, or a folder of the room; the
   * folder open in the files pane is preselected.
   */
  private async chooseFolder(files: File[]): Promise<void> {
    const circle = this.circle;
    if (!circle?.circleId || files.length === 0 || this.writable !== true) {
      return;
    }
    const select = h('select', { 'aria-label': t('Folder'), disabled: true }, h('option', { value: '' }, t('Top level')));
    const submit = h('button', { type: 'submit' }, tn(files.length, 'Upload and share', 'Upload and share {n} files'));
    const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
    const names = files.length === 1 ? files[0].name : tn(files.length, '{n} file', '{n} files');
    const error = h('p', { class: 'error small', role: 'alert' });
    // A new folder goes inside the folder selected now.
    const create = h('button', { type: 'button', class: 'secondary', onclick: async () => {
      const name = prompt(t('Name of the new folder'))?.trim();
      if (!name) {
        return;
      }
      if (name.startsWith('.')) {
        error.textContent = t('Names starting with a dot are hidden in Rune, so they aren\'t allowed here.');
        return;
      }
      error.textContent = '';
      try {
        const parent = select.value || undefined;
        const created = await api.data.postData({ processDataRequest: { circleId: circle.circleId, folderId: parent, dataName: await protectName(circle.circleId as string, name), typeName: 'folder' } });
        const parentLabel = parent ? select.selectedOptions[0]?.textContent ?? '' : '';
        select.append(h('option', { value: created.dataId as string }, parentLabel ? `${parentLabel} / ${name}` : name));
        select.value = created.dataId as string;
      } catch (e) {
        error.textContent = await errorMessage(e);
      }
    } }, t('New folder'));
    const form = h('form', { class: 'dialog-body stack' },
      h('p', {}, names),
      previews(files),
      h('label', {}, t('Save in'), select),
      h('div', { class: 'actions' }, create),
      error,
      h('div', { class: 'actions' }, submit));
    const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, t('Share in the chat')), close), form);
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog);
    dialog.showModal();

    try {
      const { folders } = await foldersOf(circle);
      const open = this.openFolder();
      for (const folder of folders) {
        select.append(h('option', { value: folder.id, selected: folder.id === open }, folder.path));
      }
    } catch {
      // Without the folder list, the top level is still possible.
    }
    select.disabled = false;

    form.addEventListener('submit', (event) => {
      event.preventDefault();
      dialog.close();
      void this.share(files, select.value || undefined);
    });
  }

  /** Uploads files to a folder of the room (the top level by default) and shares them. */
  private async share(files: File[], folderId?: string): Promise<void> {
    const circleId = this.circle?.circleId;
    if (!circleId) {
      return;
    }
    for (const file of files) {
      const progress = h('progress', { max: 100, value: 0 });
      this.status.replaceChildren(`${t('Uploading {name}', { name: file.name })} `, progress);
      try {
        const dataId = await upload(file, circleId, folderId, (sent, total) => {
          progress.value = total > 0 ? Math.round((sent / total) * 100) : 0;
        });
        await chat.send(circleId, this.text.value.trim() || file.name, dataId);
        this.text.value = '';
        this.status.textContent = '';
      } catch (e) {
        this.status.textContent = `${file.name}: ${await errorMessage(e)}`;
      }
    }
  }

  private item(message: ChatMessage): HTMLLIElement {
    this.seen.add(message.messageId ?? '');
    if (message.messageId && !message.damaged) {
      this.messages.set(message.messageId, message);
    }
    const reply = h('button', {
      type: 'button', class: 'icon delete write-only', title: t('Reply'), 'aria-label': t('Reply'), onclick: () => this.replyTo(message),
    }, '↩');
    const own = message.memberId !== undefined && message.memberId === session()?.memberId;
    // Only the author may edit or delete a message.
    const remove = !own ? null : h('button', {
      type: 'button', class: 'icon delete', title: t('Delete message'), 'aria-label': t('Delete message'),
      onclick: () => void this.deleteMessage(message),
    }, '🗑');
    const change = !own ? null : h('button', {
      type: 'button', class: 'icon delete write-only', title: t('Edit message'), 'aria-label': t('Edit message'),
      onclick: (event: Event) => this.editMessage(message, (event.target as HTMLElement).closest('li') as HTMLLIElement),
    }, '✎');
    if (message.damaged) {
      return h('li', { class: 'message damaged', 'data-message-id': message.messageId ?? '' },
        h('p', { class: 'muted' }, t('This message can\'t be read.')));
    }
    const item = h('li', { class: own ? 'message own' : 'message', 'data-message-id': message.messageId ?? '' },
      h('div', { class: 'meta muted' }, own ? null : this.authorAvatar(message.memberId, message.accountName), own ? t('You') : (message.accountName ?? ''), ' · ', h('time', { datetime: message.time ?? '' }, formatTime(message.time)),
        message.edited ? h('span', { title: t('Edited {time}', { time: formatTime(message.edited) }) }, ` ${t('(edited)')}`) : null,
        own ? h('span', { class: 'ticks', role: 'img' }) : null, reply, this.reactButton(message), change, remove),
      message.replyTo ? this.quote(message.replyTo) : null,
      h('p', { class: 'text' }, message.text ?? ''),
      message.dataId ? this.fileCard(message.dataId) : null,
      this.reactions(message));
    return item;
  }

  /** The author's avatar beside a message of someone else; filled in once the members are known. */
  private authorAvatar(memberId: string | undefined, name: string | undefined): HTMLElement {
    const element = avatar('user', this.avatars.get(memberId ?? '') || undefined, name ?? '', 'small');
    element.classList.add('author');
    element.dataset.memberId = memberId ?? '';
    return element;
  }

  /** Puts the members' avatars on the messages shown before they were known. */
  private showAvatars(): void {
    this.timeline.querySelectorAll<HTMLElement>('.avatar.author').forEach((old) => {
      const memberId = old.dataset.memberId ?? '';
      old.replaceWith(this.authorAvatar(memberId, this.names.get(memberId) ?? old.textContent ?? ''));
    });
  }

  /** The reactions under a message; a click gives or takes back the own one. */
  private reactions(message: ChatMessage): HTMLElement | null {
    const given = Object.entries(message.reactions ?? {}).filter(([, members]) => members.length > 0);
    if (given.length === 0) {
      return null;
    }
    const me = session()?.memberId;
    return h('div', { class: 'reactions' }, ...given.map(([reaction, members]) => h('button', {
      type: 'button', class: members.includes(me ?? '') ? 'reaction mine' : 'reaction',
      title: members.map((id) => (id === me ? t('You') : this.names.get(id) ?? t('someone'))).join(', '),
      onclick: () => void this.react(message, reaction),
    }, `${reaction} ${members.length}`)));
  }

  /** Opens the six reactions to choose from. */
  private reactButton(message: ChatMessage): HTMLElement {
    const button = h('button', { type: 'button', class: 'icon delete write-only', title: t('React'), 'aria-label': t('React') }, '☺');
    button.addEventListener('click', () => {
      const open = button.parentElement?.querySelector('.reaction-picker');
      if (open) {
        open.remove();
        return;
      }
      button.after(h('span', { class: 'reaction-picker' }, ...REACTIONS.map((reaction) =>
        h('button', { type: 'button', class: 'icon', 'aria-label': t('React with {reaction}', { reaction }), onclick: () => void this.react(message, reaction) }, reaction))));
    });
    return button;
  }

  private async react(message: ChatMessage, reaction: string): Promise<void> {
    if (!this.circle?.circleId || !message.messageId || this.writable !== true) {
      return;
    }
    try {
      // REACTED replaces the message with its new reactions, for everyone.
      await chat.react(this.circle.circleId, message.messageId, reaction);
    } catch (e) {
      this.status.textContent = await errorMessage(e);
    }
  }

  /**
   * Marks the newest message read, once a second at most, and only while the
   * room is open, the tab visible and the newest messages in view.
   */
  private scheduleRead(): void {
    window.clearTimeout(this.readTimer);
    this.readTimer = window.setTimeout(() => {
      const circleId = this.circle?.circleId;
      const newest = this.timeline.lastElementChild?.getAttribute('data-message-id');
      if (!circleId || !newest || newest === this.markedRead || document.visibilityState !== 'visible' || !this.isAtEnd() || !this.isConnected) {
        return;
      }
      this.markedRead = newest;
      chat.markRead(circleId, newest)
        .then(() => setCount(circleId, 0))
        .catch(() => { this.markedRead = undefined; });
    }, 1000);
  }

  private stopTyping(memberId: string): void {
    const typist = this.typists.get(memberId);
    if (typist) {
      window.clearTimeout(typist.timer);
      this.typists.delete(memberId);
      this.showTyping();
    }
  }

  private showTyping(): void {
    const names = [...this.typists.values()].map((typist) => typist.name);
    this.typingLine.textContent = names.length === 0 ? ''
      : names.length === 1 ? t('{name} is typing…', { name: names[0] })
        : names.length === 2 ? t('{name} and {other} are typing…', { name: names[0], other: names[1] })
          : t('{n} people are typing…', { n: names.length });
  }

  /**
   * The checkmarks of the own messages: one when EDS has the message, two
   * when it reached a client of everyone else in the room, two blue ones
   * when everyone has read it. Who received and who read is in the tooltip.
   * A marker is "up to a message": by its place in the timeline, or, when
   * that message isn't shown (earlier, or deleted), by its time.
   */
  private showReceipts(): void {
    const me = session()?.memberId;
    const items = [...this.timeline.querySelectorAll<HTMLElement>('li[data-message-id]')];
    const others = [...this.names.keys()].filter((id) => id && id !== me);
    // A marker is "up to a place": the stone counts the places of a chat.
    const reaches = (position: number | undefined, marker: number | undefined): boolean =>
      position !== undefined && marker !== undefined && marker > 0 && position <= marker;
    items.forEach((item) => {
      const ticks = item.querySelector<HTMLElement>('.ticks');
      if (!ticks) {
        return;
      }
      const position = this.messages.get(item.dataset.messageId ?? '')?.position;
      const read: string[] = [];
      const received: string[] = [];
      for (const id of others) {
        const receipt = this.receipts.get(id);
        if (reaches(position, receipt?.readPosition)) {
          read.push(this.names.get(id) ?? '');
        } else if (reaches(position, receipt?.receivedPosition)) {
          received.push(this.names.get(id) ?? '');
        }
      }
      const state = others.length === 0 ? 'sent'
        : read.length === others.length ? 'read'
          : read.length + received.length === others.length ? 'received' : 'sent';
      const told = [
        read.length > 0 ? t('Read by {names}', { names: read.join(', ') }) : '',
        received.length > 0 ? t('Received by {names}', { names: received.join(', ') }) : '',
      ].filter(Boolean).join(' · ') || t('Sent');
      if (ticks.dataset.state !== state) {
        ticks.dataset.state = state;
        ticks.innerHTML = state === 'sent' ? ONE_TICK : TWO_TICKS;
      }
      ticks.title = told;
      ticks.setAttribute('aria-label', told);
    });
  }

  /** Sets (or clears) the message the next one replies to. */
  private replyTo(message: ChatMessage | null): void {
    if (message && this.writable !== true) {
      return;
    }
    this.replyingTo = message;
    this.replyBar.hidden = !message;
    if (message) {
      this.replyBar.replaceChildren(
        h('span', {}, `↩ ${t('Replying to')} `, h('strong', {}, this.author(message)), ': ', snippet(message.text)),
        h('button', { type: 'button', class: 'icon', 'aria-label': t('Don\'t reply'), onclick: () => this.replyTo(null) }, '✕'));
      this.text.focus();
    }
  }

  private author(message: ChatMessage): string {
    return message.memberId === session()?.memberId ? t('you') : (message.accountName ?? '');
  }

  /** A small quote of the message replied to; a click shows it. */
  private quote(replyTo: string): HTMLElement {
    const original = this.messages.get(replyTo);
    const target = () => this.timeline.querySelector<HTMLElement>(`li[data-message-id="${CSS.escape(replyTo)}"]`);
    return h('button', {
      type: 'button', class: 'quote',
      onclick: () => {
        const element = target();
        element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        element?.classList.add('highlight');
        setTimeout(() => element?.classList.remove('highlight'), 1500);
      },
    }, ...(original
      ? [h('strong', {}, this.author(original)), ' ', snippet(original.text)]
      : [!this.earlier.hidden ? 'an earlier message' : 'a deleted message']));
  }

  /** Turns the text into an edit box; Enter saves, Escape cancels. */
  private editMessage(message: ChatMessage, item: HTMLLIElement): void {
    const text = item.querySelector('p.text');
    if (!text || !this.circle?.circleId || !message.messageId || this.writable !== true) {
      return;
    }
    const circleId = this.circle.circleId;
    const box = h('textarea', { class: 'edit', rows: 2, maxlength: 16384, 'aria-label': t('New text') });
    box.value = message.text ?? '';
    const error = h('span', { class: 'error small' });
    const restore = () => editor.replaceWith(text);
    const save = async (): Promise<void> => {
      const value = box.value.trim();
      if (!value || value === message.text) {
        restore();
        return;
      }
      try {
        // The EDITED frame replaces the whole item, here and for the others.
        await chat.editMessage(circleId, message.messageId as string, value);
      } catch (e) {
        error.textContent = await errorMessage(e);
      }
    };
    box.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        void save();
      } else if (event.key === 'Escape') {
        restore();
      }
    });
    const editor = h('div', { class: 'editor' }, box,
      h('div', { class: 'actions' },
        h('button', { type: 'button', onclick: () => void save() }, t('Save')),
        h('button', { type: 'button', class: 'secondary', onclick: restore }, t('Cancel')),
        error));
    text.replaceWith(editor);
    box.focus();
  }

  private async deleteMessage(message: ChatMessage): Promise<void> {
    const shared = message.dataId ? ` ${t('The shared file stays in the room.')}` : '';
    if (!this.circle?.circleId || !message.messageId || !confirm(`${t('Delete this message for everyone in the room?')}${shared}`)) {
      return;
    }
    try {
      await chat.deleteMessage(this.circle.circleId, message.messageId);
    } catch (e) {
      this.status.textContent = await errorMessage(e);
    }
  }

  private fileCard(dataId: string): HTMLElement {
    const card = h('button', { class: 'file-card', disabled: true }, h('span', { class: 'muted' }, t('Shared file…')));
    if (!this.files.has(dataId)) {
      this.files.set(dataId, api.data.getDataById({ dataId })
        .then((response) => response.metadata?.[0] ?? null)
        .catch(() => null));
    }
    void this.files.get(dataId)?.then(async (metadata) => {
      if (!metadata) {
        card.replaceChildren(h('span', { class: 'muted' }, t('The shared file is no longer available.')));
        return;
      }
      card.disabled = false;
      const realName = await revealName(metadata.circleId ?? '', metadata.dataName).catch(() => metadata.dataName ?? '');
      metadata.dataName = realName;
      card.replaceChildren(thumbnail(metadata), h('span', { class: 'name' }, realName), h('span', { class: 'muted' },
        formatSize(shownSize(metadata.size, metadata.circleId ? await inChamber(metadata.circleId).catch(() => false) : false))));
      card.addEventListener('click', () => void openPreview(metadata));
    });
    return card;
  }

  private isAtEnd(): boolean {
    const scroller = this.timeline.parentElement as HTMLElement;
    return scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight < 80;
  }

  private scrollToEnd(): void {
    const scroller = this.timeline.parentElement as HTMLElement;
    requestAnimationFrame(() => { scroller.scrollTop = scroller.scrollHeight; });
  }
}

/**
 * A pasted image gets a name with the time: screenshots arrive as
 * "image.png", which would clash in a folder. Several pasted together are a
 * second apart.
 */
function pastedName(image: File, index: number): File {
  const time = new Date(Date.now() + index * 1000);
  const pad = (n: number) => String(n).padStart(2, '0');
  const stamp = `${time.getFullYear()}-${pad(time.getMonth() + 1)}-${pad(time.getDate())} ${pad(time.getHours())}.${pad(time.getMinutes())}.${pad(time.getSeconds())}`;
  const extension = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp' }[image.type] ?? 'png';
  return new File([image], `${t('Pasted image')} ${stamp}.${extension}`, { type: image.type, lastModified: time.getTime() });
}

/** Small previews of the images about to be shared (local, blob: URLs). */
function previews(files: File[]): HTMLElement | null {
  const images = files.filter((file) => file.type.startsWith('image/')).slice(0, 4);
  if (images.length === 0) {
    return null;
  }
  return h('div', { class: 'paste-previews' }, ...images.map((file) => {
    const url = URL.createObjectURL(file);
    const image = h('img', { src: url, alt: file.name });
    image.addEventListener('load', () => URL.revokeObjectURL(url), { once: true });
    return image;
  }));
}

/** The start of a text, for quotes. */
function snippet(text: string | undefined): string {
  const flat = (text ?? '').replace(/\s+/g, ' ').trim();
  return flat.length > 80 ? `${flat.substring(0, 79)}…` : flat;
}

customElements.define('eds-chat', EdsChat);
