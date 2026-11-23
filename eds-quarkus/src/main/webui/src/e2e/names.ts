import { fromB64u, text, toB64u, utf8 } from './bytes.ts';
import { isProtected } from './convention.ts';
import { isChamberRoom, protect, reveal } from './rooms.ts';

/**
 * The names of files and folders of a chamber (E2E.md). The stone stores a
 * name as it is given; in a chamber the client encrypts it first, so the
 * stone, a database dump and a curious operator see only ciphertext, never
 * "tax return.pdf". The crypto is the room's, bound to the Circle and the
 * key version, under a context of its own ("name") so a name is never taken
 * for a file's content or a thumbnail.
 *
 * In a room nothing happens: the name passes as it is. The root folder keeps
 * its "/", and a name written by a client that doesn't know chambers is
 * shown as it is, as its unreadable content would be.
 */
const NAME_CONTEXT = 'name';

/** The stored name for a file or folder: encrypted in a chamber, as given in a room. */
export async function protectName(circleId: string, name: string): Promise<string> {
  if (name === '/' || !(await isChamberRoom(circleId))) {
    return name;
  }
  return toB64u(await protect(circleId, NAME_CONTEXT, utf8(name)));
}

/** The real name from what the stone stored: decrypted in a chamber, as it is otherwise. */
export async function revealName(circleId: string, stored: string | null | undefined): Promise<string> {
  if (!stored || stored === '/' || !(await isChamberRoom(circleId))) {
    return stored ?? '';
  }
  let bytes: Uint8Array;
  try {
    bytes = fromB64u(stored);
  } catch {
    return stored;
  }
  if (!isProtected(bytes)) {
    return stored;
  }
  try {
    return text(await reveal(circleId, NAME_CONTEXT, bytes));
  } catch {
    return stored;
  }
}

/**
 * Reveals the names of a listing in place, so every view downstream (the
 * tiles, the preview, downloads, the actions, a share to the chat) works
 * with the real names and nothing else has to know. A no-op for a room.
 */
export async function revealNames(circleId: string, list: Array<{ dataName?: string | null }>): Promise<void> {
  if (!(await isChamberRoom(circleId))) {
    return;
  }
  await Promise.all(list.map(async (item) => {
    item.dataName = await revealName(circleId, item.dataName);
  }));
}
