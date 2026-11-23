import type { Circle, Metadata } from '../api';
import { inChamber, shownSize } from '../e2e/files';
import { protectName, revealNames } from '../e2e/names';
import { clear, formatSize, formatTime, h } from '../dom';
import { upload } from '../media';
import { chat } from '../chat';
import { api, errorMessage } from '../session';
import { openPreview } from './preview';
import { transferList } from '../downloads';
import { openFileActions } from './file-actions';
import { thumbnail } from './thumbnail';
import { t } from '../i18n';

/** The drag data types of a file dragged from the pane (its ID, its name). */
export const DRAG_TYPE = 'application/x-rune-data';
export const DRAG_NAME = 'application/x-rune-name';

interface Folder {
  id?: string;
  name: string;
}

/**
 * The files of a Circle: a thumbnail grid or a list, folders, uploads with
 * progress, and sharing a file to the chat.
 */
export class EdsFiles extends HTMLElement {
  private circle: Circle | null = null;
  /** True if the room shown is a chamber: its files are a little larger on the stone than for their people. */
  private chamber = false;
  private path: Folder[] = [];
  private view: 'grid' | 'list' = 'grid';
  private crumbs = h('nav', { class: 'crumbs', 'aria-label': t('Folder') });
  private content = h('div', { class: 'files grid' });
  // Uploads and downloads share one list (downloads.ts).
  private transfers = transferList;
  private status = h('p', { class: 'muted' });
  private input = h('input', { type: 'file', multiple: true, hidden: true });
  private toolbar = h('div', { class: 'toolbar' });
  private writable = false;
  private uploadButton = h('button', { onclick: () => this.input.click() }, t('Upload'));
  private folderButton = h('button', { class: 'secondary', onclick: () => void this.createFolder() }, t('New folder'));
  /** Counts the refreshes; only the newest one fills the pane. */
  private generation = 0;
  /** ID of the room's top-level folder, learnt from its listing. */
  private rootId: string | undefined;

  connectedCallback(): void {
    this.input.addEventListener('change', () => {
      const files = Array.from(this.input.files ?? []);
      this.input.value = '';
      void this.uploadAll(files, false);
    });
    const toggle = h('button', { class: 'icon', title: t('Grid or list'), 'aria-label': t('Grid or list'), onclick: () => {
      this.view = this.view === 'grid' ? 'list' : 'grid';
      this.content.className = `files ${this.view}`;
    } }, '▦');
    this.toolbar.replaceChildren(this.uploadButton, this.folderButton, toggle);
    this.showWritable();
    this.replaceChildren(this.toolbar, this.crumbs, this.transfers, this.status, this.content, this.input);
    // Members who can only read can't drop files here.
    this.addEventListener('dragover', (event) => {
      if (this.writable) {
        event.preventDefault();
        this.dataset.dropHint = t('Drop to upload');
        this.classList.add('dropping');
      }
    });
    this.addEventListener('dragleave', () => this.classList.remove('dropping'));
    this.addEventListener('drop', (event) => {
      event.preventDefault();
      this.classList.remove('dropping');
      if (this.writable) {
        void this.uploadAll(Array.from(event.dataTransfer?.files ?? []), false);
      }
    });
    this.show(null);
  }

  show(circle: Circle | null): void {
    this.circle = circle;
    this.path = circle ? [{ name: circle.circleName ?? '' }] : [];
    this.toolbar.hidden = !circle;
    void this.refresh();
  }

  /** The open room got a new name: the first crumb follows, the open folder stays. */
  renamed(circle: Circle): void {
    if (this.circle && this.path.length > 0 && this.path[0].name !== (circle.circleName ?? '')) {
      this.circle = { ...this.circle, circleName: circle.circleName };
      this.path[0].name = circle.circleName ?? '';
      this.renderCrumbs();
    }
  }

  /** The folder open in the pane, or undefined for the top level. */
  currentFolderId(): string | undefined {
    return this.path[this.path.length - 1]?.id;
  }

  /**
   * Uploading, new folders, dropping, and renaming, moving and deleting are
   * for members who may write in the room; who can only read sees them
   * disabled, or not at all. EDS refuses them anyway.
   */
  setWritable(writable: boolean): void {
    if (this.writable !== writable) {
      this.writable = writable;
      this.showWritable();
      void this.refresh();
    }
  }

  private showWritable(): void {
    const why = this.writable ? undefined : t('You can only read in this room.');
    for (const button of [this.uploadButton, this.folderButton]) {
      button.disabled = !this.writable;
      if (why) {
        button.title = why;
      } else {
        button.removeAttribute('title');
      }
    }
  }

  /** Reloads the current folder, e.g. when a file was shared in the chat. */
  /**
   * Reloads the current folder. Several refreshes may run at once (an own
   * upload, CHANGED, the access check), so only the newest one fills the
   * pane, and only once its answer is there; an earlier one, answered later,
   * would otherwise add its entries a second time.
   */
  async refresh(): Promise<void> {
    const generation = ++this.generation;
    this.renderCrumbs();
    if (!this.circle) {
      clear(this.content);
      this.status.textContent = t('Choose a room.');
      return;
    }
    if (this.content.childElementCount === 0) {
      this.status.textContent = t('Loading…');
    }
    try {
      const folder = this.path[this.path.length - 1];
      const response = folder.id
        ? await api.data.getDataById({ dataId: folder.id, pageSize: 100 })
        : await api.data.getData({ circleId: this.circle.circleId as string, pageSize: 100 });
      const chamber = await inChamber(this.circle.circleId as string).catch(() => false);
      if (!folder.id) {
        this.rootId = response.metadata?.[0]?.folderId ?? this.rootId;
      }
      if (generation !== this.generation) {
        return; // A newer refresh is under way.
      }
      this.chamber = chamber;
      // In a chamber the stone stored the names encrypted (E2E.md); reveal
      // them here, once, so every view below works with the real names.
      await revealNames(this.circle.circleId as string, response.metadata ?? []);
      if (generation !== this.generation) {
        return; // A newer refresh is under way.
      }
      const entries = (response.metadata ?? []).sort((a, b) =>
        Number(b.typeName === 'folder') - Number(a.typeName === 'folder') || (a.dataName ?? '').localeCompare(b.dataName ?? ''));
      this.status.textContent = entries.length === 0 ? (this.writable ? t('No files yet. Upload, or drop files here.') : t('No files yet.')) : '';
      this.content.replaceChildren(...entries.map((entry) => this.entry(entry)));
    } catch (e) {
      if (generation === this.generation) {
        this.status.textContent = await errorMessage(e);
      }
    }
  }

  /** Uploads files to the current folder; with share, also to the chat. */
  async uploadAll(files: File[], share: boolean, target?: string): Promise<string[]> {
    const circle = this.circle;
    if (!circle || files.length === 0) {
      return [];
    }
    const folderId = target ?? this.path[this.path.length - 1]?.id;
    const ids: string[] = [];
    for (const file of files) {
      const bar = h('progress', { max: 100, value: 0 });
      const label = h('span', {}, file.name);
      const row = h('li', {}, label, bar);
      this.transfers.append(row);
      try {
        const dataId = await upload(file, circle.circleId as string, folderId, (sent, total) => {
          bar.value = total > 0 ? Math.round((sent / total) * 100) : 0;
        });
        ids.push(dataId);
        if (share) {
          await chat.send(circle.circleId as string, file.name, dataId);
        }
        row.remove();
      } catch (e) {
        row.className = 'error';
        bar.remove();
        label.textContent = `${file.name}: ${await errorMessage(e)}`;
        setTimeout(() => row.remove(), 8000);
      }
    }
    await this.refresh();
    return ids;
  }

  private entry(metadata: Metadata): HTMLElement {
    const isFolder = metadata.typeName === 'folder';
    const open = h('button', {
      class: 'entry-open',
      onclick: () => {
        if (isFolder) {
          this.path.push({ id: metadata.dataId, name: metadata.dataName ?? '' });
          void this.refresh();
        } else {
          void openPreview(metadata);
        }
      },
    }, thumbnail(metadata),
      h('span', { class: 'name' }, metadata.dataName ?? ''),
      h('span', { class: 'meta muted' }, isFolder ? t('Folder') : `${formatSize(shownSize(metadata.size, this.chamber))} · ${formatTime(metadata.added)}`));
    // Share, rename, move, delete: one menu, so the tiles stay calm.
    const more = h('button', {
      class: 'icon more', title: t('More'), 'aria-label': t('Actions for {name}', { name: metadata.dataName ?? '' }),
      onclick: () => {
        if (this.circle) {
          openFileActions(metadata, {
            circle: this.circle,
            writable: this.writable,
            report: (message) => { this.status.textContent = message; },
            refresh: () => this.refresh(),
          });
        }
      },
    }, '⋯');
    const entry = h('div', { class: 'entry' }, open, h('span', { class: 'entry-actions' }, more));
    if (isFolder) {
      this.dropTarget(entry, () => metadata.dataId);
    } else if (this.writable) {
      entry.draggable = true;
      entry.addEventListener('dragstart', (event) => {
        event.dataTransfer?.setData(DRAG_TYPE, metadata.dataId as string);
        event.dataTransfer?.setData(DRAG_NAME, metadata.dataName ?? '');
        event.dataTransfer!.effectAllowed = 'move';
        entry.classList.add('dragging');
      });
      entry.addEventListener('dragend', () => entry.classList.remove('dragging'));
    }
    return entry;
  }

  /**
   * Makes an element a drop target for a folder: files from the computer
   * are uploaded into it, a file dragged from the pane is moved into it.
   */
  private dropTarget(element: HTMLElement, folderId: () => string | undefined): void {
    element.addEventListener('dragover', (event) => {
      const types = Array.from(event.dataTransfer?.types ?? []);
      if (this.writable && (types.includes('Files') || types.includes(DRAG_TYPE))) {
        event.preventDefault();
        event.stopPropagation();
        element.classList.add('drop-here');
      }
    });
    element.addEventListener('dragleave', () => element.classList.remove('drop-here'));
    element.addEventListener('drop', (event) => {
      element.classList.remove('drop-here');
      const target = folderId();
      if (!target || !this.writable) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      this.classList.remove('dropping');
      const moved = event.dataTransfer?.getData(DRAG_TYPE);
      if (moved) {
        void this.moveInto(moved, target);
      } else {
        void this.uploadAll(Array.from(event.dataTransfer?.files ?? []), false, target);
      }
    });
  }

  private async moveInto(dataId: string, folderId: string): Promise<void> {
    if (dataId === folderId) {
      return;
    }
    try {
      await api.data.putDataById({ dataId, processDataRequest: { folderId } });
      await this.refresh();
    } catch (e) {
      this.status.textContent = await errorMessage(e);
    }
  }

  private renderCrumbs(): void {
    clear(this.crumbs);
    this.path.forEach((folder, index) => {
      if (index > 0) {
        this.crumbs.append(' / ');
      }
      const crumb = index === this.path.length - 1
        ? h('span', {}, folder.name)
        : h('button', { class: 'link', onclick: () => { this.path = this.path.slice(0, index + 1); void this.refresh(); } }, folder.name);
      if (index < this.path.length - 1) {
        this.dropTarget(crumb, () => folder.id ?? this.rootId);
      }
      this.crumbs.append(crumb);
    });
  }

  private async createFolder(): Promise<void> {
    const name = prompt(t('Name of the new folder'));
    if (!name || !this.circle) {
      return;
    }
    try {
      await api.data.postData({ processDataRequest: {
        circleId: this.circle.circleId, folderId: this.path[this.path.length - 1]?.id,
        dataName: await protectName(this.circle.circleId as string, name.trim()), typeName: 'folder',
      } });
      await this.refresh();
    } catch (e) {
      this.status.textContent = await errorMessage(e);
    }
  }
}

customElements.define('eds-files', EdsFiles);
