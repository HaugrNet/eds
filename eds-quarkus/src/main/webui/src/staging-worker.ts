/// <reference lib="webworker" />
/**
 * Writes downloads into the browser's private file storage (OPFS), in a
 * worker, as only workers may use synchronous access handles: they write in
 * place and keep what was written, also when the page reloads. A file for
 * each download, by its key; the main thread reads the files.
 */
interface Request {
  id: number;
  op: 'open' | 'write' | 'truncate' | 'close';
  name: string;
  data?: Uint8Array;
}

interface SyncAccessHandle {
  write: (data: Uint8Array, options: { at: number }) => number;
  truncate: (size: number) => void;
  getSize: () => number;
  flush: () => void;
  close: () => void;
}

const handles = new Map<string, SyncAccessHandle>();

async function directory(): Promise<FileSystemDirectoryHandle> {
  return (await navigator.storage.getDirectory()).getDirectoryHandle('rune-downloads', { create: true });
}

async function handle(name: string): Promise<SyncAccessHandle> {
  let found = handles.get(name);
  if (!found) {
    const file = await (await directory()).getFileHandle(name, { create: true });
    found = await (file as unknown as { createSyncAccessHandle: () => Promise<SyncAccessHandle> }).createSyncAccessHandle();
    handles.set(name, found);
  }
  return found;
}

self.onmessage = async (event: MessageEvent<Request>) => {
  const { id, op, name, data } = event.data;
  try {
    let size = 0;
    if (op === 'close') {
      const open = handles.get(name);
      if (open) {
        open.flush();
        open.close();
        handles.delete(name);
      }
    } else {
      const access = await handle(name);
      if (op === 'write' && data) {
        access.write(data, { at: access.getSize() });
      } else if (op === 'truncate') {
        access.truncate(0);
        access.flush();
      }
      size = access.getSize();
    }
    (self as unknown as Worker).postMessage({ id, size });
  } catch (e) {
    (self as unknown as Worker).postMessage({ id, error: e instanceof Error ? e.message : String(e) });
  }
};
