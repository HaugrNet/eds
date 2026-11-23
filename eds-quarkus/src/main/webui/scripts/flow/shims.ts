// What Rune's modules expect of a browser, as far as the flow test needs it
// (scripts/flow): an address, storage which lasts as long as the worker, and
// places to send events to. There is no key store, so a worker forgets the
// member's account key as a device without IndexedDB would.
import { workerData } from 'node:worker_threads';

class Storage {
  private values = new Map<string, string>();
  getItem(key: string): string | null {
    return this.values.get(key) ?? null;
  }
  setItem(key: string, value: string): void {
    this.values.set(key, String(value));
  }
  removeItem(key: string): void {
    this.values.delete(key);
  }
}

const base = new URL((workerData as { base: string }).base);
const globals = globalThis as Record<string, unknown>;
globals.location = { protocol: base.protocol, host: base.host, origin: base.origin, pathname: '/', search: '', hash: '' };
globals.history = { replaceState: () => undefined };
globals.sessionStorage = new Storage();
globals.localStorage = new Storage();
const page = new EventTarget() as EventTarget & Record<string, unknown>;
page.documentElement = { lang: 'en', dataset: {}, style: { setProperty: () => undefined } };
page.visibilityState = 'visible';
globals.document = page;
const view = new EventTarget() as EventTarget & Record<string, unknown>;
view.setTimeout = setTimeout;
view.clearTimeout = clearTimeout;
view.matchMedia = () => ({ matches: false, addEventListener: () => undefined });
globals.window = view;
globals.matchMedia = view.matchMedia;
if (!('navigator' in globalThis) || !(globalThis.navigator as { languages?: unknown }).languages) {
  Object.defineProperty(globalThis, 'navigator', { value: { languages: ['en'], language: 'en' }, configurable: true });
}

// Rune asks the stone it was served from, with paths; here the stone is elsewhere.
const ask = globalThis.fetch;
globals.fetch = (input: unknown, init?: RequestInit) =>
  ask(typeof input === 'string' && input.startsWith('/') ? base.origin + input : (input as RequestInfo), init);
