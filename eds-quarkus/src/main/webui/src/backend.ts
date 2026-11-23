/**
 * Whether EDS answers at all. Rune is a PWA: its shell comes from the
 * service worker's cache, also while the server is down. Then, instead of
 * panes full of failed requests, Rune shows one page (eds-unavailable) and
 * returns by itself when EDS is back.
 *
 * A failed request only raises a suspicion; EDS counts as down when two
 * calls of its version operation fail in a row, so one lost request or a
 * short hiccup doesn't take the panes away.
 */

// The REST paths are below the app: the app is at <root>/, EDS at <root>/api/v1.
// The root is that of the page, so a path prefix of a reverse proxy is kept.
export const BASE_PATH = `${location.pathname.replace(/[^/]*$/, '')}api/v1`;

/** Fired on the window when EDS stopped answering, and when it answers again. */
export const UNREACHABLE = 'eds-unreachable';
export const REACHABLE = 'eds-reachable';

/** What a proxy in front of EDS answers while EDS is down. */
const GATEWAY_ERRORS = [502, 503, 504];

let down = false;
let checking = false;

export function isDown(): boolean {
  return down;
}

/** Was the answer given for EDS by a proxy which can't reach it? */
export function isGatewayError(status: number): boolean {
  return GATEWAY_ERRORS.includes(status);
}

/** Asks EDS for its version, which needs no login; any answer of EDS itself counts. */
export async function reachable(): Promise<boolean> {
  try {
    const response = await fetch(`${BASE_PATH}/version`, { method: 'POST', cache: 'no-store' });
    return !isGatewayError(response.status);
  } catch {
    return false;
  }
}

/** A request failed without an answer of EDS: checks whether EDS is down. */
export function suspect(): void {
  if (down || checking) {
    return;
  }
  checking = true;
  void (async () => {
    let up = await reachable();
    if (!up) {
      await new Promise((resolve) => setTimeout(resolve, 1500));
      up = await reachable();
    }
    checking = false;
    if (!up && !down) {
      down = true;
      window.dispatchEvent(new CustomEvent(UNREACHABLE));
    }
  })();
}

/** Tries again while EDS is down; resolves true, and tells the window, when it is back. */
export async function retry(): Promise<boolean> {
  if (!down) {
    return true;
  }
  if (await reachable()) {
    down = false;
    window.dispatchEvent(new CustomEvent(REACHABLE));
    return true;
  }
  return false;
}
