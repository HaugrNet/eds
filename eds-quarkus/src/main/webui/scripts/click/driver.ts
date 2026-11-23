// Drives Rune's real pages in headless Chromium, as a person would: real
// mouse clicks at the place of an element, real typing, and the browser's own
// dialogs answered. Only Node and Chromium are needed: the DevTools protocol
// (CDP) is spoken over Node's own WebSocket.
//
// Every error a page shows the developer is collected: uncaught exceptions,
// console errors and requests EDS answered with an error which the test did
// not expect. The click test fails on any of them.
import { spawn, type ChildProcess } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

const CHROMIUM = process.env.CHROMIUM ?? '/snap/bin/chromium';
/** Chromium as a snap can't read /tmp or hidden folders, so its profiles are in a plain folder of the home. */
const PROFILES = join(homedir(), 'eds-click-profiles');
const WAIT = Number(process.env.CLICK_WAIT ?? 15_000);

type Pending = { resolve: (value: any) => void; reject: (error: Error) => void; method: string };
type Listener = (params: any, sessionId?: string) => void;

/** One connection to a browser, with its pages as sessions (flat mode). */
class Connection {
  private socket: WebSocket;
  private counter = 0;
  private pending = new Map<number, Pending>();
  private listeners = new Map<string, Listener[]>();

  constructor(socket: WebSocket) {
    this.socket = socket;
    socket.addEventListener('message', (event) => {
      const message = JSON.parse(String(event.data));
      if (message.id !== undefined) {
        const waiting = this.pending.get(message.id);
        this.pending.delete(message.id);
        if (message.error) {
          waiting?.reject(new Error(`${waiting.method}: ${message.error.message}`));
        } else {
          waiting?.resolve(message.result);
        }
      } else {
        for (const listener of this.listeners.get(message.method) ?? []) {
          listener(message.params, message.sessionId);
        }
      }
    });
  }

  static open(url: string): Promise<Connection> {
    return new Promise((resolve, reject) => {
      const socket = new WebSocket(url);
      socket.addEventListener('open', () => resolve(new Connection(socket)));
      socket.addEventListener('error', () => reject(new Error(`No connection to ${url}`)));
    });
  }

  send(method: string, params: object = {}, sessionId?: string): Promise<any> {
    const id = ++this.counter;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject, method });
      this.socket.send(JSON.stringify({ id, method, params, sessionId }));
    });
  }

  on(method: string, listener: Listener): void {
    this.listeners.set(method, [...(this.listeners.get(method) ?? []), listener]);
  }

  close(): void {
    this.socket.close();
  }
}

/** What went wrong on a page, beside the steps of the test. */
export type Problem = { browser: string; kind: string; text: string; time: string };

/** A request answered with an error which a step expects, e.g. a refused login. */
export type Expected = { status: number; path: RegExp };

/**
 * A browser with one page: one person using Rune. Each has a profile of its
 * own, so they share nothing but the stone.
 */
export class Browser {
  readonly name: string;
  readonly problems: Problem[];
  private process: ChildProcess;
  private connection!: Connection;
  private session!: string;
  private profile: string;
  private answers: (string | boolean)[] = [];
  private expected: Expected[] = [];
  private requests = new Map<string, { url: string; method: string }>();
  /** The texts of the browser's own dialogs (confirm, prompt, alert) in order. */
  readonly dialogs: string[] = [];
  /** Downloads by their file name: where the browser put the file, once it is complete. */
  private downloads = new Map<string, { guid: string; done: boolean }>();

  private constructor(name: string, problems: Problem[], process: ChildProcess, profile: string) {
    this.name = name;
    this.problems = problems;
    this.process = process;
    this.profile = profile;
  }

  /** Starts a browser with a fresh profile, and opens `url`. */
  static async start(name: string, url: string, problems: Problem[], port: number, extra: string[] = []): Promise<Browser> {
    const profile = join(PROFILES, `${name}-${process.pid}`);
    rmSync(profile, { recursive: true, force: true });
    mkdirSync(profile, { recursive: true });
    const child = spawn(CHROMIUM, [
      '--headless=new', '--no-sandbox', '--no-proxy-server', '--no-first-run', '--no-default-browser-check',
      '--disable-background-timer-throttling', '--disable-renderer-backgrounding', '--disable-backgrounding-occluded-windows',
      '--lang=en-US', '--window-size=1400,900',
      '--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream', '--autoplay-policy=no-user-gesture-required',
      `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`, ...extra, 'about:blank',
    ], { stdio: 'ignore' });
    const browser = new Browser(name, problems, child, profile);
    await browser.connect(port);
    await browser.goto(url);
    return browser;
  }

  private async connect(port: number): Promise<void> {
    let version: any;
    for (let tries = 0; ; tries++) {
      try {
        version = await (await fetch(`http://127.0.0.1:${port}/json/version`)).json();
        break;
      } catch (error) {
        if (tries > 100) {
          throw new Error(`${this.name}: Chromium didn't start (${String(error)})`);
        }
        await sleep(100);
      }
    }
    this.connection = await Connection.open(version.webSocketDebuggerUrl);
    const { targetInfos } = await this.connection.send('Target.getTargets');
    const page = targetInfos.find((target: any) => target.type === 'page');
    const { sessionId } = await this.connection.send('Target.attachToTarget', { targetId: page.targetId, flatten: true });
    this.session = sessionId;
    const c = this.connection;
    const mine = (listener: Listener): Listener => (params, session) => {
      if (session === this.session) {
        listener(params);
      }
    };
    c.on('Runtime.exceptionThrown', mine((params) => {
      const details = params.exceptionDetails;
      this.problem('exception', details.exception?.description ?? details.text);
    }));
    c.on('Runtime.consoleAPICalled', mine((params) => {
      if (params.type === 'error' || params.type === 'assert') {
        this.problem(`console.${params.type}`, params.args.map((arg: any) => arg.value ?? arg.description ?? '').join(' '));
      }
    }));
    c.on('Log.entryAdded', mine((params) => {
      // Failed requests are judged by their answer below; the rest is the browser's own complaint.
      if (params.entry.level === 'error' && params.entry.source !== 'network') {
        this.problem(`log.${params.entry.source}`, params.entry.text);
      }
    }));
    c.on('Network.requestWillBeSent', mine((params) => {
      this.requests.set(params.requestId, { url: params.request.url, method: params.request.method });
    }));
    c.on('Network.responseReceived', mine((params) => {
      const status = params.response.status;
      const request = this.requests.get(params.requestId);
      if (status >= 400 && request) {
        const path = new URL(request.url).pathname;
        const at = this.expected.findIndex((expected) => expected.status === status && expected.path.test(path));
        if (at >= 0) {
          this.expected.splice(at, 1);
        } else {
          this.problem('http', `${request.method} ${path} answered ${status}`);
        }
      }
    }));
    c.on('Network.loadingFailed', mine((params) => {
      const request = this.requests.get(params.requestId);
      // A request the page itself gave up (a closed socket, an aborted fetch) isn't an error of the stone.
      if (request && !params.canceled && params.type !== 'WebSocket') {
        this.problem('network', `${request.method} ${new URL(request.url).pathname}: ${params.errorText}`);
      }
    }));
    c.on('Page.javascriptDialogOpening', mine((params) => {
      this.dialogs.push(params.message);
      // A prompt takes the next text, a confirm the next yes or no.
      const wanted = params.type === 'prompt' ? 'string' : 'boolean';
      const at = this.answers.findIndex((answer) => typeof answer === wanted);
      const answer = at >= 0 && params.type !== 'alert' ? this.answers.splice(at, 1)[0] : undefined;
      if (answer === undefined && params.type !== 'alert') {
        this.problem('dialog', `unexpected ${params.type}: ${params.message}`);
      }
      void c.send('Page.handleJavaScriptDialog', {
        accept: answer !== false,
        ...(typeof answer === 'string' ? { promptText: answer } : {}),
      }, this.session);
    }));
    // Downloads go into the profile, named by their ID, and are told when complete.
    c.on('Browser.downloadWillBegin', (params) => {
      this.downloads.set(params.suggestedFilename, { guid: params.guid, done: false });
    });
    c.on('Browser.downloadProgress', (params) => {
      for (const download of this.downloads.values()) {
        if (download.guid === params.guid && params.state === 'completed') {
          download.done = true;
        }
      }
    });
    await c.send('Browser.setDownloadBehavior', { behavior: 'allowAndName', downloadPath: join(this.profile, 'downloads'), eventsEnabled: true });
    // The save dialog of the File System Access API can't be answered in a
    // headless browser: Rune downloads the way it does in browsers without it.
    await this.send('Page.addScriptToEvaluateOnNewDocument', { source: 'window.showSaveFilePicker = undefined;' });
    // When the page last changed: a person clicks once it holds still.
    await this.send('Page.addScriptToEvaluateOnNewDocument', { source: `window.__changed = Date.now();
      new MutationObserver(() => { window.__changed = Date.now(); }).observe(document, { subtree: true, childList: true, attributes: true, characterData: true });` });
    for (const domain of ['Page', 'Runtime', 'Log', 'Network', 'DOM']) {
      await this.send(`${domain}.enable`);
    }
    await this.send('Emulation.setDeviceMetricsOverride', { width: 1400, height: 900, deviceScaleFactor: 1, mobile: false });
    await this.send('Browser.grantPermissions', { permissions: ['notifications', 'audioCapture', 'videoCapture', 'geolocation', 'clipboardReadWrite'] }).catch(() => undefined);
  }

  private problem(kind: string, text: string): void {
    this.problems.push({ browser: this.name, kind, text, time: new Date().toISOString().slice(11, 23) });
  }

  send(method: string, params: object = {}): Promise<any> {
    return this.connection.send(method, params, method.startsWith('Browser.') ? undefined : this.session);
  }

  /** The next confirm() is accepted (true) or declined (false), the next prompt() answered with the text. */
  answer(...answers: (string | boolean)[]): void {
    this.answers.push(...answers);
  }

  /** The next answer with `status` to a request whose path matches is expected, not a problem. */
  expect(status: number, path: RegExp): void {
    this.expected.push({ status, path });
  }

  async goto(url: string): Promise<void> {
    await this.send('Page.navigate', { url });
    await this.waitFor('document.readyState === "complete"');
  }

  async reload(): Promise<void> {
    await this.send('Page.reload', { ignoreCache: false });
    await sleep(300);
    await this.waitFor('document.readyState === "complete"');
  }

  /** Runs a function in the page, with the helpers of PAGE_HELPERS as `rune`. */
  async run<T = any>(fn: string, ...args: unknown[]): Promise<T> {
    const expression = `(async () => { ${PAGE_HELPERS}; return await (${fn})(...${JSON.stringify(args)}); })()`;
    const result = await this.send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true, userGesture: true });
    if (result.exceptionDetails) {
      throw new Error(`${this.name}: ${result.exceptionDetails.exception?.description ?? result.exceptionDetails.text}`);
    }
    return result.result.value as T;
  }

  /** Waits until a page expression is true(ish), and gives its value. */
  async waitFor<T = any>(expression: string, what = expression, timeout = WAIT): Promise<T> {
    const end = Date.now() + timeout;
    let last: unknown;
    while (Date.now() < end) {
      try {
        const value = await this.run(`async () => { ${expression.includes('return') ? expression : `return (${expression});`} }`);
        if (value) {
          return value as T;
        }
        last = value;
      } catch (error) {
        last = error;
      }
      await sleep(150);
    }
    throw new Error(`${this.name}: waited ${timeout / 1000} s for ${what} (last: ${String(last)})`);
  }

  /** Waits until `text` is shown, within the element `scope` (a CSS selector), and gives what it is in. */
  async see(text: string | RegExp, scope = 'body', timeout = WAIT): Promise<void> {
    await this.waitFor(`rune.visibleText(${JSON.stringify(scope)}, ${toSource(text)})`, `"${String(text)}" in ${scope}`, timeout);
  }

  /** Waits until `text` is gone from `scope`. */
  async gone(text: string | RegExp, scope = 'body', timeout = WAIT): Promise<void> {
    await this.waitFor(`!rune.visibleText(${JSON.stringify(scope)}, ${toSource(text)})`, `"${String(text)}" gone from ${scope}`, timeout);
  }

  /**
   * Clicks the visible button, link or menu item showing `text` (or with it
   * as its label or title), within `scope`: with the mouse, at its middle.
   * Waits until there is one, and it is enabled.
   */
  async click(text: string | RegExp, scope = 'body'): Promise<void> {
    await this.settle();
    const point = await this.waitFor<{ x: number; y: number }>(
      `return rune.pointOf(rune.clickable(${JSON.stringify(scope)}, ${toSource(text)}))`, `a button "${String(text)}" in ${scope}`);
    await this.mouse(point);
  }

  /** Clicks the element a selector finds first among the visible ones. */
  async clickAt(selector: string): Promise<void> {
    await this.settle();
    const point = await this.waitFor<{ x: number; y: number }>(
      `return rune.pointOf(rune.visible(${JSON.stringify(selector)})[0])`, `an element ${selector}`);
    await this.mouse(point);
  }

  /**
   * Waits until the page held still for a moment (at most a few seconds): a
   * pane which is drawn anew would otherwise take the click meant for what
   * it showed before.
   */
  async settle(quiet = 250, longest = 3000): Promise<void> {
    await this.waitFor(`Date.now() - (window.__changed ?? 0) >= ${quiet}`, 'the page to hold still', longest).catch(() => undefined);
  }

  private async mouse(point: { x: number; y: number }): Promise<void> {
    for (const type of ['mouseMoved', 'mousePressed', 'mouseReleased']) {
      await this.send('Input.dispatchMouseEvent', { type, x: point.x, y: point.y, button: 'left', clickCount: 1, pointerType: 'mouse' });
    }
    await sleep(120);
  }

  /** Types into the field labelled `label` (its label, aria-label or placeholder) in `scope`, replacing what was there. */
  async fill(label: string | RegExp, value: string, scope = 'body'): Promise<void> {
    const field = `rune.field(${JSON.stringify(scope)}, ${toSource(label)})`;
    // The page may take the focus meanwhile (e.g. a room which opens): then
    // the field doesn't hold the text, and it is typed again.
    for (let tries = 0; ; tries++) {
      await this.settle();
      const point = await this.waitFor<{ x: number; y: number }>(`return rune.pointOf(${field})`, `a field "${String(label)}" in ${scope}`);
      await this.mouse(point);
      await this.run(`() => { const e = document.activeElement; if (e && 'select' in e) e.select(); }`);
      await this.send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
      await this.send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Backspace', code: 'Backspace', windowsVirtualKeyCode: 8 });
      if (value) {
        await this.send('Input.insertText', { text: value });
      }
      await sleep(100);
      const held = await this.run<string | null>(`() => ${field}?.value ?? null`);
      if (held === value) {
        return;
      }
      if (tries === 3) {
        throw new Error(`${this.name}: the field "${String(label)}" holds "${held}" instead of "${value}"`);
      }
    }
  }

  /**
   * Sets a date, time or other picker field labelled `label`, which can't be
   * typed into reliably, with the events a person's input gives.
   */
  async set(label: string | RegExp, value: string, scope = 'body'): Promise<void> {
    await this.waitFor<boolean>(`const f = rune.field(${JSON.stringify(scope)}, ${toSource(label)}); if (!f) return false;
      f.focus(); f.value = ${JSON.stringify(value)};
      f.dispatchEvent(new Event('input', { bubbles: true })); f.dispatchEvent(new Event('change', { bubbles: true }));
      return f.value === ${JSON.stringify(value)};`, `the field "${String(label)}" set to ${value}`);
  }

  /** Where the device says it is, for the location feature. */
  async place(latitude: number, longitude: number): Promise<void> {
    await this.send('Emulation.setGeolocationOverride', { latitude, longitude, accuracy: 10 });
  }

  /** Chooses the option showing `option` in the select labelled `label`. */
  async choose(label: string | RegExp, option: string | RegExp, scope = 'body'): Promise<void> {
    const done = await this.waitFor<boolean>(`return rune.choose(${JSON.stringify(scope)}, ${toSource(label)}, ${toSource(option)})`,
      `"${String(option)}" in the choice "${String(label)}"`);
    if (!done) {
      throw new Error(`${this.name}: can't choose ${String(option)}`);
    }
  }

  /** Ticks (or unticks) the checkbox or radio button labelled `label`. */
  async tick(label: string | RegExp, on = true, scope = 'body'): Promise<void> {
    const point = await this.waitFor<{ x: number; y: number; already?: boolean }>(
      `return rune.pointOf(rune.box(${JSON.stringify(scope)}, ${toSource(label)}, ${on}))`, `a box "${String(label)}" in ${scope}`);
    if (!point.already) {
      await this.mouse(point);
    }
  }

  /** Presses a key, e.g. Enter or Escape, where the focus is. */
  async press(key: string): Promise<void> {
    const codes: Record<string, number> = { Enter: 13, Escape: 27, Tab: 9, ArrowDown: 40, ArrowUp: 38 };
    for (const type of ['keyDown', 'keyUp']) {
      await this.send('Input.dispatchKeyEvent', {
        type, key, code: key, windowsVirtualKeyCode: codes[key] ?? 0,
        ...(key === 'Enter' && type === 'keyDown' ? { text: '\r' } : {}),
      });
    }
    await sleep(100);
  }

  /** Gives files to the file input a selector finds (also a hidden one, as Rune opens it with a button). */
  async files(selector: string, paths: string[]): Promise<void> {
    const { root } = await this.send('DOM.getDocument', { depth: 0 });
    const { nodeIds } = await this.send('DOM.querySelectorAll', { nodeId: root.nodeId, selector });
    if (nodeIds.length === 0) {
      throw new Error(`${this.name}: no file input ${selector}`);
    }
    await this.send('DOM.setFileInputFiles', { nodeId: nodeIds[nodeIds.length - 1], files: paths });
  }

  /** Waits until the browser downloaded a file of this name, and gives its content. */
  async downloaded(name: string, timeout = 30_000): Promise<Buffer> {
    const end = Date.now() + timeout;
    while (Date.now() < end) {
      const download = this.downloads.get(name);
      const path = download ? join(this.profile, 'downloads', download.guid) : '';
      if (download?.done && existsSync(path)) {
        return readFileSync(path);
      }
      await sleep(150);
    }
    throw new Error(`${this.name}: no download of ${name} within ${timeout / 1000} s`);
  }

  async screenshot(path: string): Promise<void> {
    const { data } = await this.send('Page.captureScreenshot', { format: 'png' });
    writeFileSync(path, Buffer.from(data, 'base64'));
  }

  /** Sets the size of the window, e.g. that of a phone. */
  async size(width: number, height: number, mobile = false): Promise<void> {
    await this.send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile });
    await sleep(300);
  }

  async close(): Promise<void> {
    for (const answer of this.answers) {
      this.problem('dialog', `an answer for a dialog which never came: ${JSON.stringify(answer)}`);
    }
    this.answers = [];
    try {
      await this.connection.send('Browser.close');
    } catch {
      // Gone already.
    }
    this.connection?.close();
    await new Promise<void>((resolve) => {
      if (this.process.exitCode !== null) {
        resolve();
        return;
      }
      const timer = setTimeout(() => { this.process.kill('SIGKILL'); resolve(); }, 3000);
      this.process.once('exit', () => { clearTimeout(timer); resolve(); });
    });
    rmSync(this.profile, { recursive: true, force: true });
  }
}

export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function toSource(value: string | RegExp): string {
  return value instanceof RegExp ? `{ re: ${JSON.stringify(value.source)}, flags: ${JSON.stringify(value.flags)} }` : JSON.stringify(value);
}

/**
 * Helpers in the page: what a person sees. An element counts when it is
 * rendered, not hidden and inside an open dialog if one is modal (the rest
 * of the page is inert then).
 */
const PAGE_HELPERS = `
const rune = (() => {
  const matches = (value, text) => {
    const v = String(value ?? '').replace(/\\s+/g, ' ').trim();
    if (typeof text === 'string') return v === text || v.startsWith(text);
    return new RegExp(text.re, text.flags).test(v);
  };
  const shown = (e) => {
    if (!e || !e.isConnected) return false;
    if (e.closest('[hidden]') || e.closest('[inert]')) return false;
    const style = getComputedStyle(e);
    if (style.visibility === 'hidden' || style.display === 'none') return false;
    const r = e.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  };
  // With a modal dialog open, the rest of the page is inert: only the
  // dialog counts, also within a scope which holds it.
  const roots = (scope) => {
    const modal = [...document.querySelectorAll('dialog[open]')].filter((d) => d.matches(':modal'));
    const top = modal[modal.length - 1];
    const all = [...document.querySelectorAll(scope)];
    if (!top) return all;
    return all.filter((s) => top.contains(s) || s.contains(top)).map((s) => (s.contains(top) ? top : s));
  };
  const within = (scope, selector) => roots(scope).flatMap((r) => [...r.querySelectorAll(selector)]).filter(shown);
  // What a button is called: its label or title, its text, or the text of
  // one of its parts (a room's button also holds its avatar's initial).
  const label = (e) => [e.getAttribute('aria-label'), e.getAttribute('title'), e.textContent, e.value,
    ...[...e.querySelectorAll('span, strong')].map((c) => c.textContent)].filter((v) => v);
  return {
    visible: (selector) => [...document.querySelectorAll(selector)].filter(shown),
    visibleText: (scope, text) => roots(scope).some((r) => {
      const walker = document.createTreeWalker(r, NodeFilter.SHOW_ELEMENT);
      for (let e = walker.currentNode; e; e = walker.nextNode()) {
        if (e.children.length === 0 || [...e.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim())) {
          if (shown(e) && (matches(e.textContent, text) || matches(e.getAttribute('aria-label'), text) || matches(e.value, text))) return true;
        }
      }
      return false;
    }),
    clickable: (scope, text) => within(scope, 'button, a, [role=menuitem], [role=tab], summary, [role=button], li[tabindex], .file-card')
      .find((e) => !e.disabled && label(e).some((v) => matches(v, text))),
    field: (scope, text) => {
      const fields = within(scope, 'input:not([type=checkbox]):not([type=radio]):not([type=file]), textarea, select');
      return fields.find((f) => matches(f.getAttribute('aria-label'), text) || matches(f.placeholder, text)
        || (f.labels && [...f.labels].some((l) => matches(l.textContent, text) || matches(l.firstChild?.textContent, text))));
    },
    box: (scope, text, on) => {
      const boxes = within(scope, 'input[type=checkbox], input[type=radio]');
      const box = boxes.find((b) => (b.labels && [...b.labels].some((l) => matches(l.textContent, text))) || matches(b.getAttribute('aria-label'), text));
      if (!box) return null;
      if (box.checked === on) return { already: true };
      return box.closest('label') ?? box;
    },
    choose: (scope, text, option) => {
      const f = within(scope, 'select').find((s) => matches(s.getAttribute('aria-label'), text)
        || (s.labels && [...s.labels].some((l) => matches(l.firstChild?.textContent, text) || matches(l.textContent, text))));
      if (!f) return false;
      const o = [...f.options].find((o) => matches(o.textContent, option) || matches(o.value, option));
      if (!o) return false;
      f.value = o.value;
      f.dispatchEvent(new Event('input', { bubbles: true }));
      f.dispatchEvent(new Event('change', { bubbles: true }));
      return true;
    },
    pointOf: (e) => {
      if (!e) return null;
      if (e.already) return { x: -1, y: -1, already: true };
      e.scrollIntoView({ block: 'center', inline: 'center' });
      const r = e.getBoundingClientRect();
      const x = r.left + r.width / 2, y = r.top + r.height / 2;
      // What a person would hit there: the element, or something in it.
      const hit = document.elementFromPoint(x, y);
      if (!hit || !(hit === e || e.contains(hit) || hit.contains(e) || (e.labels && [...e.labels].some((l) => l.contains(hit))))) return null;
      return { x, y };
    },
  };
})();
`;
