import type { Metadata } from './api';
import { formatSize, h } from './dom';
import { contentUrl } from './media';
import { friendly } from './messages';
import { session } from './session';
import * as staging from './staging';
import * as storage from './storage';
import { formatNumber, t as tr } from './i18n';
import { revealBlob, type Passed } from './e2e/files';

/**
 * Downloads with progress, pause and resume, also across reloads (Rune).
 *
 * The browser's own download shows no progress inside the app, so Rune reads
 * the content itself, into the browser's private file storage (OPFS, see
 * staging.ts), where every piece is on disk as soon as it arrives. A paused
 * download, one whose connection was lost, and one still running when the
 * page reloads, all continue from the size of that file, with a range
 * request; If-Range with the ETag makes EDS send the whole content when it
 * changed meanwhile, so old and new are never mixed. Running downloads
 * continue on their own after a reload, paused ones on Resume.
 *
 * When complete, the file goes to its place: in Chromium browsers into the
 * file chosen in the save dialog at the start (after a reload, that needs a
 * click on Save, for the browser's permission); elsewhere to the browser's
 * own download, read from the disk, so even large files don't fill the
 * memory. The staged file is decrypted content: it is deleted once the file
 * is saved, on Discard, and at logout. Of a chamber, what arrives is what
 * Rune encrypted before the upload: it is staged as it comes, so pausing
 * and resuming work the same, and decrypted when it is complete, into a
 * second staged file, which goes the same way. The details of each download (not
 * the content) are kept per member in IndexedDB (storage.ts).
 *
 * Without the private file storage (some private windows), downloads run in
 * memory and don't survive a reload.
 */
export const transferList = h('ul', { class: 'transfers' });

export interface DownloadProgress {
  (received: number, total: number): void;
}

interface FileHandle {
  createWritable: () => Promise<WritableStream<Uint8Array>>;
  queryPermission?: (options: { mode: 'readwrite' }) => Promise<PermissionState>;
  requestPermission?: (options: { mode: 'readwrite' }) => Promise<PermissionState>;
}

interface SaveFilePickerWindow {
  showSaveFilePicker?: (options: { suggestedName?: string }) => Promise<FileHandle>;
}

type State = 'running' | 'paused' | 'save' | 'delivered';

interface Transfer {
  key: string;
  metadata: Metadata;
  name: string;
  memberId: string;
  /** The file chosen in the save dialog, or none: the browser's download. */
  handle: FileHandle | null;
  /** True if the content goes to the private file storage, not memory. */
  staged: boolean;
  chunks: BlobPart[];
  received: number;
  total: number;
  etag: string | null;
  running: boolean;
  /** Complete, waiting for a click on Save (the file's permission). */
  waitingToSave: boolean;
  /** Set when a running download is cancelled, not paused. */
  discarded: boolean;
  /** The content as its people read it, once complete: decrypted, for a chamber. */
  plain: Passed | null;
  controller: AbortController | null;
  progress?: DownloadProgress;
  done: Promise<void>;
  finish: () => void;
  row: HTMLLIElement;
  label: HTMLSpanElement;
  bar: HTMLProgressElement;
  amount: HTMLSpanElement;
  buttons: HTMLSpanElement;
}

interface Stored {
  key: string;
  memberId: string;
  metadata: Metadata;
  name: string;
  total: number;
  etag: string | null;
  handle: FileHandle | null;
  state: State;
}

const transfers = new Map<string, Transfer>();

class Fatal extends Error {}

/**
 * Downloads a file, or continues its paused download, or saves a complete
 * one; resolves when it is saved, paused, or discarded.
 */
export async function download(metadata: Metadata, progress?: DownloadProgress): Promise<void> {
  const existing = transfers.get(metadata.dataId as string);
  if (existing) {
    existing.progress = progress ?? existing.progress;
    if (existing.waitingToSave) {
      void save(existing);
    } else if (!existing.running) {
      void run(existing);
    }
    return existing.done;
  }

  const name = metadata.dataName ?? 'download';
  // The save dialog first, while the click still counts as the person's own
  // action; the browser refuses it later.
  let handle: FileHandle | null = null;
  const picker = (window as unknown as SaveFilePickerWindow).showSaveFilePicker;
  if (picker) {
    try {
      handle = await picker({ suggestedName: name });
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        return; // The person closed the save dialog.
      }
    }
  }

  const t = create(metadata, name, session()?.memberId ?? 'member', handle, staging.available());
  t.progress = progress;
  show(t);
  void run(t);
  return t.done;
}

function create(metadata: Metadata, name: string, memberId: string, handle: FileHandle | null, staged: boolean): Transfer {
  let finish = () => undefined as void;
  const done = new Promise<void>((resolve) => { finish = resolve; });
  return {
    key: `${memberId}.${metadata.dataId as string}`, metadata, name, memberId, handle, staged,
    chunks: [], received: 0, total: metadata.size ?? 0, etag: null,
    running: false, waitingToSave: false, discarded: false, plain: null, controller: null, done, finish,
    row: h('li', { class: 'download' }),
    label: h('span', {}, `↓ ${name}`),
    bar: h('progress', { max: 100, value: 0 }),
    amount: h('span', { class: 'muted small' }),
    buttons: h('span', { class: 'transfer-buttons' }),
  };
}

function show(t: Transfer): void {
  t.row.append(t.label, t.bar, t.amount, t.buttons);
  transferList.append(t.row);
  transfers.set(t.metadata.dataId as string, t);
}

async function run(t: Transfer): Promise<void> {
  t.running = true;
  t.controller = new AbortController();
  t.row.classList.remove('paused');
  showButtons(t);

  try {
    if (t.staged) {
      // What is on disk already, also from before a reload.
      t.received = await staging.open(t.key);
    }
    const headers: Record<string, string> = {};
    if (t.received > 0) {
      headers.Range = `bytes=${t.received}-`;
      if (t.etag) {
        headers['If-Range'] = t.etag;
      }
    }
    const response = await fetch(await contentUrl(t.metadata.dataId as string), { signal: t.controller.signal, headers });
    if (response.status === 200 && t.received > 0) {
      // The content changed since (or the range wasn't served): start over,
      // the whole content follows.
      t.received = t.staged ? await staging.truncate(t.key) : 0;
      t.chunks = [];
    } else if (response.status === 416 && t.received > 0 && t.received === t.total) {
      // Everything had arrived before the reload.
      await complete(t);
      return;
    } else if (response.status !== 200 && response.status !== 206) {
      let message = tr('The download failed ({status}).', { status: response.status });
      try {
        message = friendly((await response.json()).returnMessage, message);
      } catch {
        // Not JSON.
      }
      throw new Fatal(message);
    }
    t.etag = response.headers.get('ETag') ?? t.etag;
    const range = response.headers.get('Content-Range')?.match(/\/(\d+)$/);
    t.total = range ? Number(range[1]) : (Number(response.headers.get('Content-Length')) || t.total);
    await persist(t, 'running');

    const reader = (response.body as ReadableStream<Uint8Array>).getReader();
    for (;;) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      if (t.staged) {
        t.received = await staging.append(t.key, value);
      } else {
        t.chunks.push(value as Uint8Array<ArrayBuffer>);
        t.received += value.length;
      }
      showProgress(t);
    }
    await complete(t);
  } catch (e) {
    t.running = false;
    if (t.staged) {
      await staging.close(t.key).catch(() => undefined);
    }
    if (t.discarded) {
      await cleanUp(t);
    } else if (e instanceof Fatal) {
      await fail(t, e.message);
    } else {
      // Paused by the person, or the connection was lost: both resume.
      pause(t, t.controller?.signal.aborted ? null : tr('The connection was lost.'));
    }
  }
}

/**
 * The complete content, readable: of a chamber it is decrypted here, once.
 * Content of a chamber which isn't protected is handed out only if the
 * person says so.
 */
async function readable(t: Transfer): Promise<Blob> {
  if (!t.plain) {
    const stored = t.staged ? await staging.file(t.key) : new Blob(t.chunks, { type: 'application/octet-stream' });
    if (!stored) {
      throw new Fatal(tr('The downloaded file is gone.'));
    }
    const circleId = t.metadata.circleId;
    if (!circleId) {
      return stored;
    }
    try {
      t.amount.textContent = tr('Decrypting…');
      t.plain = await revealBlob(circleId, '', stored, `${t.key}.plain`);
    } catch (e) {
      throw new Fatal(e instanceof Error ? e.message : String(e));
    }
    if (t.plain.unprotected && !confirm(tr('"{name}" is not protected: it was written by an app which does not know chambers. Save it all the same?', { name: t.name }))) {
      throw new Fatal(tr('Not saved.'));
    }
  }
  return t.plain.blob;
}

/** The content is all there: off to its place. */
async function complete(t: Transfer): Promise<void> {
  t.running = false;
  if (t.staged) {
    await staging.close(t.key);
  }
  if (t.handle) {
    // After a reload, writing to the chosen file needs the person's click.
    const permission = await t.handle.queryPermission?.({ mode: 'readwrite' }) ?? 'granted';
    if (permission !== 'granted') {
      t.waitingToSave = true;
      t.amount.textContent = tr('Complete. Click Save to write the file.');
      showButtons(t);
      await persist(t, 'save');
      return;
    }
    await save(t);
    return;
  }
  // The browser's own download, of the file on disk (or the parts in memory).
  const content = await readable(t);
  const url = URL.createObjectURL(content);
  const link = h('a', { href: url, download: t.name });
  document.body.append(link);
  link.click();
  link.remove();
  // The browser reads the file after the click; it goes a little later.
  await persist(t, 'delivered');
  setTimeout(() => {
    URL.revokeObjectURL(url);
    void cleanUp(t);
  }, 120000);
  end(t);
}

/** Writes the complete download into the file chosen in the save dialog. */
async function save(t: Transfer): Promise<void> {
  const handle = t.handle as FileHandle;
  try {
    if (await handle.requestPermission?.({ mode: 'readwrite' }) === 'denied') {
      throw new Fatal(tr('Rune may no longer write to the chosen file. Download it again.'));
    }
    const content = await readable(t);
    await content.stream().pipeTo(await handle.createWritable());
    await cleanUp(t);
    end(t);
  } catch (e) {
    await fail(t, e instanceof Error ? e.message : String(e));
  }
}

function showProgress(t: Transfer): void {
  const percent = t.total > 0 ? Math.min(100, Math.round((t.received / t.total) * 100)) : 0;
  t.bar.value = percent;
  t.amount.textContent = t.total > 0 ? tr('{percent} % of {size}', { percent: formatNumber(percent), size: formatSize(t.total) }) : formatSize(t.received);
  t.progress?.(t.received, t.total);
}

function showButtons(t: Transfer): void {
  const button = (label: string, text: string, action: () => void) =>
    h('button', { type: 'button', class: 'icon', title: label, 'aria-label': `${label} ${t.name}`, onclick: action }, text);
  if (t.waitingToSave) {
    t.buttons.replaceChildren(
      h('button', { type: 'button', class: 'secondary', onclick: () => void save(t) }, tr('Save')),
      button(tr('Discard'), '✕', () => discard(t)));
  } else if (t.running) {
    t.buttons.replaceChildren(button(tr('Pause'), '⏸', () => t.controller?.abort()), button(tr('Cancel'), '✕', () => discard(t)));
  } else {
    t.buttons.replaceChildren(button(tr('Resume'), '▶', () => void run(t)), button(tr('Discard'), '✕', () => discard(t)));
  }
}

function pause(t: Transfer, reason: string | null): void {
  t.row.classList.add('paused');
  showProgress(t);
  const percent = t.total > 0 ? Math.round((t.received / t.total) * 100) : 0;
  t.amount.textContent = `${reason ? `${reason} ` : ''}${tr('Paused at {percent} %', { percent: formatNumber(percent) })}`;
  showButtons(t);
  void persist(t, 'paused');
  t.finish();
}

function discard(t: Transfer): void {
  if (t.running) {
    // The running transfer closes its file first, then cleans up (run).
    t.discarded = true;
    t.controller?.abort();
  } else {
    void cleanUp(t);
    end(t);
  }
}

/** Removes the staged file and the stored details. */
async function cleanUp(t: Transfer): Promise<void> {
  await t.plain?.release().catch(() => undefined);
  t.plain = null;
  if (t.staged) {
    await staging.remove(t.key);
    await staging.remove(`${t.key}.plain`);
  }
  await storage.remove(t.key);
}

function end(t: Transfer): void {
  t.running = false;
  t.waitingToSave = false;
  if (transfers.get(t.metadata.dataId as string) === t) {
    transfers.delete(t.metadata.dataId as string);
  }
  t.row.remove();
  t.finish();
}

async function fail(t: Transfer, message: string): Promise<void> {
  await cleanUp(t);
  transfers.delete(t.metadata.dataId as string);
  t.row.className = 'download error';
  t.row.replaceChildren(h('span', {}, `${t.name}: ${message}`));
  setTimeout(() => t.row.remove(), 8000);
  t.finish();
}

/** Keeps the details of a download for after a reload (not the content). */
async function persist(t: Transfer, state: State): Promise<void> {
  if (!t.staged) {
    return; // In memory, nothing to continue after a reload.
  }
  const stored: Stored = {
    key: t.key, memberId: t.memberId, metadata: t.metadata, name: t.name,
    total: t.total, etag: t.etag, handle: t.handle, state,
  };
  await storage.put(stored);
}

/** True if the file has a paused download, for the button's text. */
export function isPaused(dataId: string): boolean {
  const transfer = transfers.get(dataId);
  return !!transfer && !transfer.running;
}

/**
 * After a reload: the downloads of the logged-in member come back; running
 * ones continue on their own, paused ones wait for Resume, complete ones for
 * Save. Delivered ones are cleaned up.
 */
export async function restoreDownloads(): Promise<void> {
  const memberId = session()?.memberId;
  if (!memberId || !staging.available()) {
    return;
  }
  for (const stored of await storage.all<Stored>()) {
    if (stored.memberId !== memberId || transfers.has(stored.metadata.dataId as string)) {
      continue;
    }
    const t = create(stored.metadata, stored.name, memberId, stored.handle, true);
    if (stored.state === 'delivered') {
      await cleanUp(t);
      continue;
    }
    t.total = stored.total;
    t.etag = stored.etag;
    t.received = (await staging.file(t.key))?.size ?? 0;
    show(t);
    if (stored.state === 'running') {
      void run(t);
    } else if (stored.state === 'save') {
      t.waitingToSave = true;
      showProgress(t);
      t.amount.textContent = tr('Complete. Click Save to write the file.');
      showButtons(t);
    } else {
      pause(t, null);
    }
  }
}

/**
 * At logout: the member's downloads stop, and their staged files (decrypted
 * content) and details are deleted.
 */
export async function forgetDownloads(memberId: string): Promise<void> {
  for (const t of transfers.values()) {
    t.discarded = true;
    t.controller?.abort();
  }
  transfers.clear();
  transferList.replaceChildren();
  // What uploads and previews of chambers staged, and a reload left behind.
  await staging.sweep(['protect.', 'reveal.']);
  for (const stored of await storage.all<Stored>()) {
    if (stored.memberId === memberId) {
      if (staging.available()) {
        await staging.remove(stored.key);
        await staging.remove(`${stored.key}.plain`);
      }
      await storage.remove(stored.key);
    }
  }
}
