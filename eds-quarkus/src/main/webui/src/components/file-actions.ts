import type { Circle, Metadata } from '../api';
import { moveToRoom } from '../media';
import { myRooms } from '../account';
import { chat } from '../chat';
import { download, isPaused } from '../downloads';
import { h } from '../dom';
import { api, errorMessage } from '../session';
import { protectName, revealNames } from '../e2e/names';
import { t } from '../i18n';

/** What the files pane gives the action menu. */
export interface FileContext {
  circle: Circle;
  writable: boolean;
  /** Reports a problem in the files pane. */
  report: (message: string) => void;
  /** Reloads the files pane. */
  refresh: () => Promise<void>;
}

/**
 * The actions for one file or folder, as a small sheet: share in the chat,
 * rename, move (files only; EDS doesn't move folders), delete. Only what the
 * member may do is offered.
 */
export function openFileActions(metadata: Metadata, context: FileContext): void {
  const folder = metadata.typeName === 'folder';
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const action = (label: string, run: () => void, className = 'secondary') =>
    h('button', { type: 'button', class: className, onclick: () => { dialog.close(); run(); } }, label);
  const dialog = h('dialog', { class: 'modal sheet' },
    h('header', {}, h('h2', {}, metadata.dataName ?? ''), close),
    h('div', { class: 'dialog-body stack' },
      folder ? null : action(isPaused(metadata.dataId as string) ? t('Resume download') : t('Download'), () => void download(metadata)),
      folder || !context.writable ? null : action(t('Share in the chat'), () => void share(metadata, context)),
      context.writable ? action(t('Rename'), () => void rename(metadata, context)) : null,
      context.writable && !folder ? action(t('Move'), () => openMoveDialog(metadata, context)) : null,
      context.writable ? action(folder ? t('Delete folder') : t('Delete'), () => void remove(metadata, context), 'danger') : null));
  dialog.addEventListener('close', () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();
}

async function share(metadata: Metadata, context: FileContext): Promise<void> {
  try {
    await chat.send(context.circle.circleId as string, metadata.dataName ?? t('A file'), metadata.dataId);
  } catch (e) {
    context.report(await errorMessage(e));
  }
}

async function rename(metadata: Metadata, context: FileContext): Promise<void> {
  const name = prompt(t('New name'), metadata.dataName ?? '')?.trim();
  if (!name || name === metadata.dataName) {
    return;
  }
  if (name.startsWith('.')) {
    context.report(t('Names starting with a dot are hidden in Rune, so they aren\'t allowed here.'));
    return;
  }
  try {
    const stored = await protectName(context.circle.circleId as string, name);
    await api.data.putDataById({ dataId: metadata.dataId as string, processDataRequest: { dataName: stored } });
    await context.refresh();
  } catch (e) {
    context.report(await errorMessage(e));
  }
}

async function remove(metadata: Metadata, context: FileContext): Promise<void> {
  const folder = metadata.typeName === 'folder';
  if (!confirm(folder
    ? t('Delete the folder "{name}"? It is deleted for everyone in the room, and can\'t be restored.', { name: metadata.dataName ?? '' })
    : t('Delete "{name}"? It is deleted for everyone in the room, and can\'t be restored.', { name: metadata.dataName ?? '' }))) {
    return;
  }
  try {
    await api.data.deleteDataById({ dataId: metadata.dataId as string });
    await context.refresh();
  } catch (e) {
    context.report(await errorMessage(e));
  }
}

interface Destination {
  label: string;
  /** A folder of this room, or null for another room. */
  folderId?: string;
  /** Another room. */
  circleId?: string;
}

/** The folders of a room, with their path, a few levels deep. */
export async function foldersOf(circle: Circle): Promise<{ rootId?: string; folders: { id: string; path: string }[] }> {
  const folders: { id: string; path: string }[] = [];
  const circleId = circle.circleId as string;
  const root = await api.data.getData({ circleId, pageSize: 100 });
  const rootId = root.metadata?.[0]?.folderId;
  await revealNames(circleId, root.metadata ?? []);
  const walk = async (entries: Metadata[], prefix: string, depth: number): Promise<void> => {
    for (const entry of entries.filter((e) => e.typeName === 'folder')) {
      const path = prefix ? `${prefix} / ${entry.dataName ?? ''}` : entry.dataName ?? '';
      folders.push({ id: entry.dataId as string, path });
      if (depth < 4) {
        const inside = await api.data.getDataById({ dataId: entry.dataId as string, pageSize: 100 });
        await revealNames(circleId, inside.metadata ?? []);
        await walk(inside.metadata ?? [], path, depth + 1);
      }
    }
  };
  await walk(root.metadata ?? [], '', 1);
  return { rootId, folders };
}

/** Moves a file to another folder of the room, or to another room. */
function openMoveDialog(metadata: Metadata, context: FileContext): void {
  const select = h('select', { 'aria-label': t('Destination'), disabled: true }, h('option', {}, t('Loading…')));
  const error = h('p', { class: 'error', role: 'alert' });
  const submit = h('button', { type: 'submit', disabled: true }, t('Move'));
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const form = h('form', { class: 'dialog-body stack' },
    h('label', {}, t('Move to'), select),
    h('p', { class: 'muted small' }, t('Moving to another room encrypts the file with that room\'s key. Its thumbnail is made again on the next upload.')),
    error,
    h('div', { class: 'actions' }, submit));
  const dialog = h('dialog', { class: 'modal' }, h('header', {}, h('h2', {}, t('Move "{name}"', { name: metadata.dataName ?? '' })), close), form);
  dialog.addEventListener('close', () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();

  const destinations: Destination[] = [];
  void (async () => {
    try {
      const [{ rootId, folders }, rooms] = await Promise.all([foldersOf(context.circle), myRooms()]);
      const here = h('optgroup', { label: t('In {room}', { room: context.circle.circleName ?? '' }) });
      const add = (group: HTMLOptGroupElement, destination: Destination) => {
        group.append(h('option', { value: String(destinations.length) }, destination.label));
        destinations.push(destination);
      };
      if (rootId && rootId !== metadata.folderId) {
        add(here, { label: t('Top level'), folderId: rootId });
      }
      for (const folder of folders.filter((f) => f.id !== metadata.folderId)) {
        add(here, { label: folder.path, folderId: folder.id });
      }
      const others = h('optgroup', { label: t('Other rooms') });
      for (const room of rooms.filter((r) => r.circleId !== context.circle.circleId)) {
        add(others, { label: room.circleName ?? '', circleId: room.circleId });
      }
      select.replaceChildren(...[here, others].filter((group) => group.children.length > 0));
      if (destinations.length === 0) {
        select.replaceChildren(h('option', {}, t('No other place to move it to')));
        return;
      }
      select.disabled = false;
      submit.disabled = false;
    } catch (e) {
      error.textContent = await errorMessage(e);
    }
  })();

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const destination = destinations[Number(select.value)];
    if (!destination) {
      return;
    }
    submit.disabled = true;
    error.textContent = '';
    try {
      if (destination.circleId) {
        await moveToRoom(metadata, context.circle.circleId as string, destination.circleId);
      } else {
        await api.data.putDataById({ dataId: metadata.dataId as string, processDataRequest: { folderId: destination.folderId } });
      }
      dialog.close();
      await context.refresh();
    } catch (e) {
      error.textContent = await errorMessage(e);
      submit.disabled = false;
    }
  });
}
