/**
 * What this device keeps for end-to-end encryption (E2E.md): the account
 * key of each Member who used it here, as a key which can derive and can't
 * be read again, in the browser's key store (IndexedDB). With it the device
 * opens the Member's vault after a login; without it, the Member gives the
 * recovery key once.
 *
 * Also remembered: which rooms are chambers. A stone which later presents
 * one of them without its mark doesn't get plain content from this device.
 */
const DATABASE = 'rune-keys';
const STORE = 'accounts';
const CHAMBERS = 'rune.chambers.';

function database(): Promise<IDBDatabase | null> {
  return new Promise((resolve) => {
    try {
      const request = indexedDB.open(DATABASE, 1);
      request.onupgradeneeded = () => request.result.createObjectStore(STORE);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(null);
      request.onblocked = () => resolve(null);
    } catch {
      resolve(null);
    }
  });
}

async function run<T>(mode: IDBTransactionMode, work: (store: IDBObjectStore) => IDBRequest<T>): Promise<T | undefined> {
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
    } finally {
      db.close();
    }
  });
}

/** The account key of a Member on this device, or undefined if the device doesn't have it. */
export async function accountKey(memberId: string): Promise<CryptoKey | undefined> {
  const found = await run('readonly', (store) => store.get(memberId) as IDBRequest<CryptoKey | undefined>);
  return found instanceof CryptoKey ? found : undefined;
}

/** Keeps the account key of a Member on this device; false if the browser has no key store. */
export async function keepAccountKey(memberId: string, key: CryptoKey): Promise<boolean> {
  return (await run('readwrite', (store) => store.put(key, memberId))) !== undefined;
}

export async function forgetAccountKey(memberId: string): Promise<void> {
  await run('readwrite', (store) => store.delete(memberId));
}

const SEEN = 'rune.keys.seen.';

function seenKeys(memberId: string): Record<string, string> {
  try {
    return JSON.parse(localStorage.getItem(SEEN + memberId) ?? '{}') as Record<string, string>;
  } catch {
    return {};
  }
}

/**
 * Compares the public key of another Member with the one this device saw
 * before (E2E.md, "Checking keys"): 'new' the first time, which remembers
 * it, 'same', or 'changed', which keeps the earlier one until the Member
 * accepts the new one (acceptKey).
 */
export function checkKey(memberId: string, otherId: string, publicKey: string): 'new' | 'same' | 'changed' {
  const seen = seenKeys(memberId);
  if (!seen[otherId]) {
    acceptKey(memberId, otherId, publicKey);
    return 'new';
  }
  return seen[otherId] === publicKey ? 'same' : 'changed';
}

/** Remembers the public key of another Member as the one to expect. */
export function acceptKey(memberId: string, otherId: string, publicKey: string): void {
  const seen = seenKeys(memberId);
  seen[otherId] = publicKey;
  try {
    localStorage.setItem(SEEN + memberId, JSON.stringify(seen));
  } catch {
    // Without storage, the device forgets with the page.
  }
}

/** The rooms this device has seen as chambers, for a Member. */
export function knownChambers(memberId: string): Set<string> {
  try {
    return new Set(JSON.parse(localStorage.getItem(CHAMBERS + memberId) ?? '[]') as string[]);
  } catch {
    return new Set();
  }
}

export function rememberChamber(memberId: string, circleId: string): void {
  const known = knownChambers(memberId);
  if (!known.has(circleId)) {
    known.add(circleId);
    try {
      localStorage.setItem(CHAMBERS + memberId, JSON.stringify([...known]));
    } catch {
      // Without storage, the device forgets with the page.
    }
  }
}
