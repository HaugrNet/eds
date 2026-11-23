/**
 * The stones a System Administrator looks after (STATUS.md): the own one,
 * read with the session, and further ones by their address and a status
 * key, which reads the status and nothing else. The keys are kept in the
 * browser of this device. Here: the list, asking a stone, and what is
 * worked out of its answer; the screen is components/stones-window.ts.
 */
import { BASE_PATH, session } from './session';
import type { Status } from './status';

export * from './status';

/** A further stone: where it is, and the key which reads its status. */
export interface Stone { address: string; key: string }

const KEY = 'rune.stones.';

/** The further stones kept on this device for the logged-in member. */
export function stones(): Stone[] {
  try {
    const kept = JSON.parse(localStorage.getItem(KEY + (session()?.memberId ?? '')) ?? '[]') as Stone[];
    return kept.filter((stone) => typeof stone.address === 'string' && typeof stone.key === 'string');
  } catch {
    return [];
  }
}

export function keepStones(list: Stone[]): void {
  try {
    localStorage.setItem(KEY + (session()?.memberId ?? ''), JSON.stringify(list));
  } catch {
    // Without storage, the list holds while the page is open.
  }
}

/** Why a stone gave no status. */
export class StatusError extends Error {
  /** 'refused': the key or session isn't accepted; 'silent': the stone doesn't answer; 'failed': it answered something else. */
  readonly kind: 'refused' | 'silent' | 'failed';
  constructor(kind: 'refused' | 'silent' | 'failed', message: string) {
    super(message);
    this.kind = kind;
  }
}

/**
 * Asks a stone for its status: the own one (null) with the session, a
 * further one with its status key. `since` is the last sample of the load
 * which is here already; `history` asks for the day and all inventories.
 */
export async function askStatus(stone: Stone | null, since?: number, history = false, timeout = 8000): Promise<Status> {
  const query = new URLSearchParams();
  if (since !== undefined) {
    query.set('since', String(since));
  }
  if (history) {
    query.set('history', 'true');
  }
  const base = stone ? stone.address : BASE_PATH;
  const stop = new AbortController();
  const timer = setTimeout(() => stop.abort(), timeout);
  let response: Response;
  try {
    response = await fetch(`${base}/status${query.size > 0 ? `?${query}` : ''}`, {
      headers: { Authorization: `Bearer ${stone ? stone.key : session()?.token ?? ''}` },
      signal: stop.signal, cache: 'no-store', credentials: 'omit',
    });
  } catch (e) {
    throw new StatusError('silent', e instanceof Error ? e.message : String(e));
  } finally {
    clearTimeout(timer);
  }
  if (response.status === 401 || response.status === 403) {
    throw new StatusError('refused', String(response.status));
  }
  if (!response.ok) {
    throw new StatusError('failed', String(response.status));
  }
  return await response.json() as Status;
}

