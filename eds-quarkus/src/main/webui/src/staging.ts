/**
 * The private file storage (OPFS) for downloads in progress, see
 * staging-worker.ts. Files are written by the worker and read here.
 */
const DIRECTORY = 'rune-downloads';

let worker: Worker | null = null;
let counter = 0;
const pending = new Map<number, { resolve: (size: number) => void; reject: (error: Error) => void }>();

/** True if the browser has the private file storage and workers for it. */
export function available(): boolean {
  return typeof Worker !== 'undefined' && !!navigator.storage?.getDirectory;
}

function call(op: string, name: string, data?: Uint8Array): Promise<number> {
  if (!worker) {
    worker = new Worker(new URL('./staging-worker.ts', import.meta.url), { type: 'module' });
    worker.onmessage = (event: MessageEvent<{ id: number; size?: number; error?: string }>) => {
      const waiting = pending.get(event.data.id);
      pending.delete(event.data.id);
      if (event.data.error) {
        waiting?.reject(new Error(event.data.error));
      } else {
        waiting?.resolve(event.data.size ?? 0);
      }
    };
  }
  const id = ++counter;
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    worker!.postMessage({ id, op, name, data });
  });
}

/** Opens (creates) the staged file; resolves with its size. */
export function open(name: string): Promise<number> {
  return call('open', name);
}

/** Appends to the staged file; resolves with its new size. */
export function append(name: string, data: Uint8Array): Promise<number> {
  return call('write', name, data);
}

export function truncate(name: string): Promise<number> {
  return call('truncate', name);
}

/** Closes the staged file, so it can be read. */
export async function close(name: string): Promise<void> {
  await call('close', name);
}

async function directory(): Promise<FileSystemDirectoryHandle> {
  return (await navigator.storage.getDirectory()).getDirectoryHandle(DIRECTORY, { create: true });
}

/** The staged file, to read; null if there is none. */
export async function file(name: string): Promise<File | null> {
  try {
    return await (await (await directory()).getFileHandle(name)).getFile();
  } catch {
    return null;
  }
}

/**
 * Removes the staged files whose names start with one of the prefixes: what
 * an upload or a preview of a chamber left behind when the page went away.
 */
export async function sweep(prefixes: string[]): Promise<void> {
  if (!available()) {
    return;
  }
  try {
    const entries = (await directory()) as unknown as { keys: () => AsyncIterable<string> };
    const names: string[] = [];
    for await (const name of entries.keys()) {
      if (prefixes.some((prefix) => name.startsWith(prefix))) {
        names.push(name);
      }
    }
    for (const name of names) {
      await remove(name);
    }
  } catch {
    // Nothing to sweep.
  }
}

export async function remove(name: string): Promise<void> {
  try {
    await close(name);
    await (await directory()).removeEntry(name);
  } catch {
    // Already gone.
  }
}
