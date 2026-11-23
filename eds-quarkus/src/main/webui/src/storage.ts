/**
 * A small key-value store on IndexedDB, for what must survive a reload but
 * can't go into localStorage: file handles and binary data. Every call fails
 * softly: without IndexedDB (a private window, blocked storage), Rune works
 * on, only without the persistence.
 */
const DATABASE = 'rune';
const STORE = 'downloads';

let opened: Promise<IDBDatabase | null> | null = null;

function database(): Promise<IDBDatabase | null> {
  if (!opened) {
    opened = new Promise((resolve) => {
      try {
        const request = indexedDB.open(DATABASE, 1);
        request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: 'key' });
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => resolve(null);
        request.onblocked = () => resolve(null);
      } catch {
        resolve(null);
      }
    });
  }
  return opened;
}

async function transaction<T>(mode: IDBTransactionMode, work: (store: IDBObjectStore) => IDBRequest<T>): Promise<T | undefined> {
  const db = await database();
  if (!db) {
    return undefined;
  }
  return new Promise((resolve) => {
    try {
      const request = work(db.transaction(STORE, mode).objectStore(STORE));
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(undefined);
    } catch {
      resolve(undefined);
    }
  });
}

export async function put(value: { key: string }): Promise<void> {
  await transaction('readwrite', (store) => store.put(value));
}

export async function remove(key: string): Promise<void> {
  await transaction('readwrite', (store) => store.delete(key));
}

export async function all<T>(): Promise<T[]> {
  return (await transaction('readonly', (store) => store.getAll() as IDBRequest<T[]>)) ?? [];
}
