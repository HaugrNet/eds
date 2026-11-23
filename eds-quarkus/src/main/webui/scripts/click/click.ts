// The click test of Rune: everything Rune offers, used through its pages in
// real browsers, as people would, by several at once. It needs a stone of its
// own, empty, which it claims as its first account (see README.md here):
//
//   node scripts/click/click.ts http://localhost:8090
//
// It fails on the first step which doesn't work, with a screenshot of each
// browser, and afterwards on anything a page showed the developer: an
// uncaught exception, an error in the console, or a request the stone
// refused which the step didn't expect.
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { deflateSync } from 'node:zlib';
import { Browser, sleep, type Problem } from './driver.ts';

const base = (process.argv[2] ?? 'http://localhost:8090').replace(/\/$/, '');
const out = process.env.CLICK_OUT ?? join(process.cwd(), 'target', 'click');
mkdirSync(out, { recursive: true });
const port = Number(process.env.CLICK_PORT ?? 9300);

const problems: Problem[] = [];
const browsers: Browser[] = [];
const started = Date.now();
let steps = 0;
let section = '';

/** The people of the test: the administrator who claims the stone, two users and an invited guest. */
const ADMIN = { name: 'ingrid', passphrase: 'the click test claims this stone, 2026' };
const ANNA = { name: 'anna', passphrase: 'anna walks the fjord in winter light' };
const BJORN = { name: 'bjorn', passphrase: 'bjorn only reads what others write' };
const CARLA = { name: 'carla', passphrase: 'carla came in by an invitation link' };
const ANNA_NEW = 'anna changed her passphrase to this one';

function step(text: string): void {
  steps++;
  console.log(`  - ${text}`);
}

async function part(name: string, run: () => Promise<void>): Promise<void> {
  section = name;
  console.log(`${name}`);
  await run();
}

async function open(name: string, path = '/'): Promise<Browser> {
  const browser = await Browser.start(name, `${base}${path}`, problems, port + browsers.length);
  browsers.push(browser);
  return browser;
}

async function login(b: Browser, who: { name: string; passphrase: string }): Promise<void> {
  await b.fill('Username', who.name);
  await b.fill('Password', who.passphrase);
  await b.click('Sign in');
  await b.see('Rooms', '.circles, eds-circles, body');
  await b.waitFor('document.querySelector("eds-app") !== null', 'the app');
}

async function menu(b: Browser, item: string): Promise<void> {
  await b.clickAt('.account-button');
  await b.click(item, '.account-menu');
}

async function closeDialog(b: Browser): Promise<void> {
  await b.click('Close', 'dialog[open]');
  await b.waitFor('document.querySelectorAll("dialog[open]").length === 0', 'the dialog to close');
}

/** A file to upload, written into the output folder. */
function file(name: string, content: string | Buffer): string {
  const path = join(out, 'files', name);
  mkdirSync(join(out, 'files'), { recursive: true });
  writeFileSync(path, content);
  return path;
}

/** A picture (PNG, 64 x 48, a colour gradient), for avatars, thumbnails and the preview. */
const PNG = (() => {
  const width = 64;
  const height = 48;
  const rows = Buffer.alloc((width * 3 + 1) * height);
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      rows.set([x * 4, y * 5, 140], y * (width * 3 + 1) + 1 + x * 3);
    }
  }
  const crcTable = Array.from({ length: 256 }, (_, n) => {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    return c >>> 0;
  });
  const crc = (data: Buffer) => {
    let c = 0xffffffff;
    for (const byte of data) {
      c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8);
    }
    return (c ^ 0xffffffff) >>> 0;
  };
  const chunk = (type: string, data: Buffer) => {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length);
    const body = Buffer.concat([Buffer.from(type, 'latin1'), data]);
    const sum = Buffer.alloc(4);
    sum.writeUInt32BE(crc(body));
    return Buffer.concat([length, body, sum]);
  };
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header.set([8, 2, 0, 0, 0], 8);
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', header), chunk('IDAT', deflateSync(rows)), chunk('IEND', Buffer.alloc(0))]);
})();

async function main(): Promise<void> {
  console.log(`Click test of Rune at ${base}`);
  const admin = await open('admin');

  await part('The login, and claiming the stone', async () => {
    await admin.see('Sign in');
    await admin.see(/Your Rune is currently running on/);
    step('the login page shows, with the version of the stone');
    await login(admin, ADMIN);
    step(`${ADMIN.name} claims the empty stone as its first account`);
    await admin.see('Connected', 'body');
    await admin.see(/Test system|In-memory/);
    step('the status line: connected, a test system');
    await menu(admin, 'Log out');
    await admin.see('Sign in');
    admin.expect(401, /\/api\/v1\//);
    admin.expect(403, /\/api\/v1\//);
    await admin.fill('Username', ADMIN.name);
    await admin.fill('Password', 'not the passphrase at all');
    await admin.click('Sign in');
    await admin.waitFor('document.querySelector("eds-login .error")?.textContent.trim().length > 0', 'the refused login told');
    step('a wrong passphrase is refused, and told');
    await login(admin, ADMIN);
    step('logged in again');
  });

  await part('The account menu of the System Administrator', async () => {
    await admin.clickAt('.account-button');
    for (const item of ['Change avatar…', 'Change passphrase…', 'Keys for chambers…', 'Language and clock…', 'Theme…',
      'Panes…', 'Notifications…', 'Online status…', 'Stone settings…', 'Stones…', 'About Rune…', 'Log out']) {
      await admin.see(item, '.account-menu');
    }
    await admin.press('Escape');
    step('the menu has every item, also those of the System Administrator');
    await menu(admin, 'About Rune…');
    await admin.see('About Rune');
    await admin.see('The MasterKey');
    await closeDialog(admin);
    step('About Rune tells how the data is protected');
  });

  await part('Stone settings: features and status keys', async () => {
    await menu(admin, 'Stone settings…');
    await admin.see('Stone settings');
    await admin.see('Features');
    const offers = 'return [...document.querySelectorAll(".feature-row button")].filter((b) => b.textContent.trim() === "Offer").length';
    while (await admin.run<number>(`() => { ${offers} }`) > 0) {
      const before = await admin.run<number>(`() => { ${offers} }`);
      await admin.click('Offer', '.feature-row');
      await admin.waitFor(`${offers.replace('return ', 'return (')}) < ${before}`, 'the feature offered');
    }
    await admin.see('offered, version 1', '.feature-row:has(strong)');
    step('every feature is offered to the rooms of the stone');
    admin.answer(true);
    await admin.click('Withdraw', '.feature-row:last-child');
    await admin.waitFor(`${offers.replace('return ', 'return (')}) === 1`, 'the feature withdrawn');
    await admin.click('Offer', '.feature-row');
    await admin.waitFor(`${offers.replace('return ', 'return (')}) === 0`, 'the feature offered again');
    step('a feature no room has is withdrawn, and offered again');
    await admin.fill('Label of the new status key', 'click test monitor');
    await admin.click('Create a status key');
    const statusKey = await admin.waitFor<string>(
      'return document.querySelector("input[aria-label=\'The new status key\']")?.value', 'the status key shown');
    await admin.click('Copy', 'dialog[open]');
    await admin.see('click test monitor');
    step('a status key is created and shown once');
    (globalThis as any).statusKey = statusKey;
    await admin.fill('Allowed origins (CORS)', base, 'dialog[open]');
    await admin.click(/^Save (1 change|changes)/, 'dialog[open]');
    await admin.see('Saved.', 'dialog[open]');
    step('this Rune\'s address is allowed to call the stone from elsewhere (CORS)');
    await admin.click('Close', 'dialog[open]');
    step('the stone settings close');
  });

  await part('Stones: the status of this stone', async () => {
    await menu(admin, 'Stones…');
    await admin.see('Add a stone');
    // The same stone under another address: for the browser another origin.
    await admin.fill('Address of the stone', base.replace('localhost', '127.0.0.1'));
    await admin.fill('Status key', (globalThis as any).statusKey);
    await admin.click('Add a stone');
    await admin.waitFor('document.querySelectorAll("dialog[open] .stone-card").length === 2', 'a second stone listed');
    // A stone just started may not have counted what it holds yet.
    await admin.waitFor('[...document.querySelectorAll("dialog[open] .stone-card")].every((c) => c.textContent.includes("awake") && /Storage|has not counted/.test(c.textContent) && c.textContent.includes("Requests per second"))',
      'both stones awake, with their numbers', 30_000);
    step('the stone is added under another address with its status key, and shows awake with its numbers');
    admin.answer(true);
    await admin.click('Remove from this list', 'dialog[open]');
    await admin.waitFor('document.querySelectorAll("dialog[open] .stone-card").length === 1', 'the further stone removed');
    await closeDialog(admin);
    step('and is removed from the list again');
  });

  await part('Users', async () => {
    for (const [who, role] of [[ANNA, 'User'], [BJORN, 'User'], [{ name: 'dagny', passphrase: 'dagny is deleted at the end of it' }, 'User']] as const) {
      await admin.click('New user');
      await admin.fill('Name', who.name);
      await admin.fill('Passphrase', who.passphrase);
      await admin.fill('Repeat the passphrase', who.passphrase);
      await admin.tick(role);
      await admin.click('Create user');
      await admin.gone('Create user');
      await admin.see(who.name, 'eds-circles');
    }
    step('three users are created by the System Administrator');
    admin.answer(true);
    await admin.click('Make administrator', 'eds-circles');
    await admin.see('admin', 'eds-circles');
    admin.answer(true);
    await admin.click('Make user', 'eds-circles');
    step('a user is made an administrator, and a user again');
  });

  await part('Rooms and a chamber', async () => {
    await admin.click('New room');
    await admin.fill('Name', 'Fjord');
    await admin.tick('Room');
    for (const feature of ['Calendar', 'Plans', 'Location', 'Calls']) {
      await admin.tick(feature);
    }
    await admin.click('Create');
    await admin.see('Fjord', 'eds-circles');
    step('the room Fjord is created, with every feature');
    await admin.click('New room');
    await admin.fill('Name', 'Vault');
    await admin.tick('Chamber');
    await admin.see('The choice is final');
    await admin.click('Create');
    await admin.see('Your recovery key');
    await admin.tick('I have written the recovery key down.');
    await admin.click('Continue');
    await admin.see('Vault', 'eds-circles');
    step('the chamber Vault is created; the administrator gets keys and writes down the recovery key');
  });

  // The rest is in the next parts of the file, added as Rune is walked through.
  await together(admin);
}

/** Opens a room from the rooms pane, and waits until its chat shows. */
async function enter(b: Browser, room: string): Promise<void> {
  await b.click(room, 'eds-circles');
  await b.waitFor(`document.querySelector("eds-app .circle-name")?.textContent.includes(${JSON.stringify(room)})`, `the room ${room} open`);
}

async function withMessage(b: Browser, text: string, action: string): Promise<void> {
  // The actions of a message show on hovering it; the mouse goes there first.
  const point = await b.waitFor<{ x: number; y: number }>(`const li = [...document.querySelectorAll('eds-chat li.message')].find((m) => m.textContent.includes(${JSON.stringify(text)}));
    if (!li) return null; li.scrollIntoView({ block: 'center' }); const r = li.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2 };`,
  `the message "${text}"`);
  await b.send('Input.dispatchMouseEvent', { type: 'mouseMoved', x: point.x, y: point.y });
  await sleep(200);
  const target = await b.waitFor<{ x: number; y: number }>(`const li = [...document.querySelectorAll('eds-chat li.message')].find((m) => m.textContent.includes(${JSON.stringify(text)}));
    const button = li && [...li.querySelectorAll('button')].find((x) => x.getAttribute('aria-label') === ${JSON.stringify(action)} && !x.disabled);
    if (!button) return null; const r = button.getBoundingClientRect(); return r.width > 0 ? { x: r.left + r.width / 2, y: r.top + r.height / 2 } : null;`,
  `"${action}" on the message "${text}"`);
  for (const type of ['mouseMoved', 'mousePressed', 'mouseReleased']) {
    await b.send('Input.dispatchMouseEvent', { type, x: target.x, y: target.y, button: 'left', clickCount: 1 });
  }
  await sleep(150);
}

async function send(b: Browser, text: string): Promise<void> {
  await b.fill('Message', text, 'eds-chat');
  await b.click('Send', 'eds-chat');
  await b.see(text, 'eds-chat');
}

/** Shows the members of a room in the rooms pane, under its name. */
async function members(b: Browser, room: string): Promise<void> {
  // The list is drawn anew when the rooms change (a room just made), which
  // may close it again: then, as a person would, it is opened once more.
  const shown = `rune.visible('select[aria-label="User to add to ${room}"]').length > 0`;
  for (let tries = 0; tries < 5; tries++) {
    if (await b.run<boolean>(`() => ${shown}`)) {
      return;
    }
    await b.click(`Members of ${room}`, 'eds-circles');
    await b.waitFor(shown, `the members of ${room}`, 3000).catch(() => undefined);
  }
  await b.waitFor(shown, `the members of ${room}`);
}

/** Adds a user to a room, with an access, in the open member list of the room. */
async function add(b: Browser, room: string, user: string, access: RegExp, scope = 'eds-circles'): Promise<void> {
  // The member shows in the list of the room with a choice of their access.
  const added = `rune.visible('${scope} select[aria-label="Access of ${user}"]').length > 0`;
  // The rooms pane may be drawn anew meanwhile, which closes the form: then
  // it is opened, and filled, once more.
  for (let tries = 0; ; tries++) {
    try {
      if (scope === 'eds-circles') {
        await members(b, room);
      }
      await b.choose(`User to add to ${room}`, user, scope);
      await b.choose(`Access in ${room}`, access, scope);
      await b.click('Add', scope);
      await b.waitFor(added, `${user} added to ${room}`, 5000);
      return;
    } catch (error) {
      if (tries === 3 || await b.run<boolean>(`() => ${added}`)) {
        if (await b.run<boolean>(`() => ${added}`)) {
          return;
        }
        throw error;
      }
    }
  }
}

/**
 * A scope for the entry of one room (or user) in the rooms pane, found by its
 * name: CSS can't choose by text, so the entry is marked first.
 */
async function entryOf(b: Browser, kind: 'room' | 'user', name: string): Promise<string> {
  const mark = `${kind}-${name.replace(/\W/g, '')}`;
  const items = kind === 'room' ? 'eds-circles li.room' : 'eds-circles li.user-item';
  await b.waitFor(`const li = [...document.querySelectorAll('${items}')].find((e) => e.querySelector(':scope > .row .circle-name-text')?.firstChild?.textContent.trim() === ${JSON.stringify(name)});
    if (!li) return false; li.dataset.click = ${JSON.stringify(mark)}; return true;`, `the ${kind} ${name} in the rooms pane`);
  return `eds-circles li[data-click="${mark}"]`;
}

/** Anna's recovery key, written down when her keys are made. */
let annaRecoveryKey = '';

async function together(admin: Browser): Promise<void> {
  await part('People in rooms', async () => {
    await members(admin, 'Fjord');
    await add(admin, 'Fjord', ANNA.name, /write/i);
    await add(admin, 'Fjord', BJORN.name, /^(Can )?read$/i);
    step('anna (read and write) and bjorn (read only) are added to Fjord');
  });

  const anna = await open('anna');
  await part('A second person: anna', async () => {
    await login(anna, ANNA);
    await anna.see('Fjord', 'eds-circles');
    step('anna logs in and sees Fjord');
    await enter(admin, 'Fjord');
    await enter(anna, 'Fjord');
    await anna.see('No messages yet.', 'eds-chat');
    step('both open Fjord; its chat is empty');
  });

  await part('The chat, between two browsers', async () => {
    await send(admin, 'Welcome to the fjord, anna');
    await anna.see('Welcome to the fjord, anna', 'eds-chat');
    step('a message reaches the other browser');
    await send(anna, 'Thank you, it is beautiful here');
    await admin.see('Thank you, it is beautiful here', 'eds-chat');
    await admin.waitFor(`[...document.querySelectorAll('eds-chat .ticks')].some((t) => /(Read|Received) by anna/.test(t.getAttribute('aria-label') ?? ''))`,
      'the ticks: received or read by anna');
    step('the answer comes back, and the first message shows as received or read');
    await withMessage(anna, 'Welcome to the fjord', 'React');
    await anna.click('React with 👍', 'eds-chat');
    await admin.see('👍', 'eds-chat');
    step('a reaction shows to the other');
    await withMessage(admin, 'Welcome to the fjord', 'Edit message');
    await admin.fill('New text', 'Welcome to the fjord, anna and bjorn', 'eds-chat');
    await admin.click('Save', 'eds-chat');
    await anna.see('Welcome to the fjord, anna and bjorn', 'eds-chat');
    await anna.see('(edited)', 'eds-chat');
    step('an edited message changes for the other, marked edited');
    await withMessage(anna, 'Welcome to the fjord', 'Reply');
    await anna.see(/Replying to/, 'eds-chat');
    await send(anna, 'A reply to the welcome');
    await admin.see('A reply to the welcome', 'eds-chat');
    step('a reply');
    await send(anna, 'This one is a mistake');
    await admin.see('This one is a mistake', 'eds-chat');
    anna.answer(true);
    await withMessage(anna, 'This one is a mistake', 'Delete message');
    await admin.gone('This one is a mistake', 'eds-chat');
    step('a deleted message is gone for the other too');
    // Typing is told now and then, not at every key: anna types for a while.
    await anna.fill('Message', 'typing', 'eds-chat');
    let told = false;
    for (let i = 0; i < 24 && !told; i++) {
      await anna.send('Input.insertText', { text: '.' });
      told = await admin.run<boolean>(`() => rune.visibleText('eds-chat', 'anna is typing…')`);
      await sleep(500);
    }
    await admin.see('anna is typing…', 'eds-chat', 1000);
    await anna.fill('Message', '', 'eds-chat');
    step('the other sees anna typing');
  });

  await part('Files', async () => {
    const notes = file('notes.txt', 'What to bring to the fjord: boots, a map and coffee.\n');
    const picture = file('fjord.png', PNG);
    anna.answer('Trips');
    await anna.click('New folder', 'eds-files');
    await anna.see('Trips', 'eds-files');
    step('a folder is made');
    await anna.files('eds-files input[type=file]', [notes, picture]);
    await anna.see('notes.txt', 'eds-files');
    await anna.see('fjord.png', 'eds-files');
    await admin.see('notes.txt', 'eds-files');
    step('two files are uploaded, and show for the other at once');
    await anna.click('Grid or list', 'eds-files');
    await anna.click('Grid or list', 'eds-files');
    step('the files show as a list and as a grid');
    await admin.waitFor(`[...document.querySelectorAll('eds-files .entry')].some((e) => e.textContent.includes('fjord.png') && e.querySelector('img')?.complete)`,
      'the thumbnail of the picture', 30_000);
    step('the picture gets a thumbnail, also in the other browser');
    await anna.click('fjord.png', 'eds-files');
    await anna.waitFor('[...document.querySelectorAll("dialog[open] img")].some((i) => i.complete && i.naturalWidth === 64)', 'the picture in the preview');
    await closeDialog(anna);
    await anna.click('notes.txt', 'eds-files');
    await anna.see('No preview for this type.', 'dialog[open]');
    await closeDialog(anna);
    step('the preview shows the picture, and tells that a text file has none');
    await anna.click('Actions for notes.txt', 'eds-files');
    anna.answer('packing-list.txt');
    await anna.click('Rename', 'dialog[open]');
    await anna.see('packing-list.txt', 'eds-files');
    await admin.see('packing-list.txt', 'eds-files');
    step('a file is renamed, for both');
    await anna.click('Actions for packing-list.txt', 'eds-files');
    await anna.click('Move', 'dialog[open]');
    await anna.see('Move "packing-list.txt"');
    await anna.waitFor('!document.querySelector("dialog[open] select[aria-label=Destination]")?.disabled', 'the destinations loaded');
    await anna.choose('Destination', /Trips/, 'dialog[open]');
    await anna.click(/^Move$/, 'dialog[open] .actions');
    await anna.gone('packing-list.txt', 'eds-files .files');
    await anna.click('Trips', 'eds-files');
    await anna.see('packing-list.txt', 'eds-files');
    step('a file is moved into the folder, and found there');
    await anna.click('Fjord', 'eds-files .crumbs');
    await anna.click('Actions for fjord.png', 'eds-files');
    await anna.click('Share in the chat', 'dialog[open]');
    await admin.waitFor('document.querySelector("eds-chat .file-card") !== null', 'the shared file in the chat');
    step('a picture is shared in the chat, and shows there for the other');
    await anna.click('Actions for fjord.png', 'eds-files');
    await anna.click(/^Download$/, 'dialog[open]');
    const got = await anna.downloaded('fjord.png');
    if (!got.equals(PNG)) {
      throw new Error(`the downloaded fjord.png has ${got.length} bytes, not the ${PNG.length} uploaded`);
    }
    step('a file is downloaded, the same bytes as uploaded');
  });

  await part('Calendar', async () => {
    await anna.click('Calendar', 'header nav.tabs');
    await anna.click('New event', 'eds-calendar');
    await anna.fill('Title', 'Boat trip');
    const day = new Date(Date.now() + 2 * 86_400_000).toISOString().slice(0, 10);
    await anna.set('Starts', day);
    await anna.set('Ends', day);
    await anna.fill('Place', 'The harbour');
    await anna.click('Add', 'dialog[open]');
    await anna.see(/Boat trip/, 'eds-calendar');
    await admin.click('Calendar', 'header nav.tabs');
    await admin.see(/Boat trip/, 'eds-calendar');
    step('an event is added, and shows in the other calendar');
    for (const view of ['Week', 'List', 'Month']) {
      await anna.choose('View', view, 'eds-calendar');
      await anna.see(/Boat trip/, 'eds-calendar');
    }
    await anna.click('Later', 'eds-calendar');
    await anna.click('Today', 'eds-calendar');
    step('the month, week and list views, and paging through them');
    await anna.click(/Boat trip/, 'eds-calendar');
    await anna.fill('Title', 'Boat trip to the islands');
    await anna.click('Save', 'dialog[open]');
    await admin.see(/Boat trip to the islands/, 'eds-calendar');
    step('the event is changed, for both');
    const ics = file('events.ics', ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//click test//EN', 'BEGIN:VEVENT',
      `UID:click-${Date.now()}@test`, `DTSTART;VALUE=DATE:${day.replaceAll('-', '')}`, `SUMMARY:Imported walk`, 'END:VEVENT', 'END:VCALENDAR', ''].join('\r\n'));
    anna.answer(true);
    await anna.files('eds-calendar input[type=file]', [ics]);
    await anna.see(/Imported walk/, 'eds-calendar');
    step('an .ics file is imported');
    await anna.click('Export', 'eds-calendar');
    step('the events are exported as an .ics file');
    await anna.click(/Imported walk/, 'eds-calendar');
    anna.answer(true);
    await anna.click('Remove', 'dialog[open]');
    await admin.gone(/Imported walk/, 'eds-calendar');
    step('an event is removed, for both');
    await anna.click('Calendar', 'header nav.tabs');
    await admin.click('Calendar', 'header nav.tabs');
  });

  await part('Plans', async () => {
    await anna.click('Plans', 'header nav.tabs');
    await anna.click('New plan', 'eds-plans');
    await anna.fill('Name', 'Islands weekend');
    await anna.fill('Place', 'Lofoten');
    await anna.tick(ADMIN.name, true, 'dialog[open]');
    await anna.tick(ANNA.name, true, 'dialog[open]');
    await anna.fill('Guests without an account', 'Erik');
    await anna.click('Create', 'dialog[open]');
    await anna.see('Islands weekend', 'eds-plans');
    await admin.click('Plans', 'header nav.tabs');
    await admin.see('Islands weekend', 'eds-plans');
    step('a plan is made, and shows for the other');
    await anna.see('‹ All plans', 'eds-plans');
    await anna.fill('Something to do, to book or to bring', 'Book the ferry', 'eds-plans');
    await anna.choose('Who takes it', ANNA.name, 'eds-plans');
    await anna.click('Add', 'eds-plans');
    await anna.see('Book the ferry', 'eds-plans');
    await anna.tick('Book the ferry', true, 'eds-plans');
    step('something to do is added, taken and ticked off');
    await anna.click('Add expense', 'eds-plans');
    await anna.fill('What for', 'Ferry tickets');
    await anna.fill('Amount', '90.00');
    await anna.click('Add', 'dialog[open]');
    await anna.see('Ferry tickets', 'eds-plans');
    await anna.see(/owes|gets/, 'eds-plans');
    step('an expense is added, and the plan says who owes whom');
    await anna.click('Enter a payment', 'eds-plans');
    await anna.fill('Amount', '10.00');
    await anna.click('Enter', 'dialog[open]');
    await anna.see('not confirmed', 'eds-plans');
    step('a payment is entered');
    anna.answer(true);
    await anna.click('Close the plan', 'eds-plans');
    await anna.see('Open again', 'eds-plans');
    await anna.click('Open again', 'eds-plans');
    step('the plan is closed and opened again');
    await anna.click('‹ All plans', 'eds-plans');
    await anna.click('Plans', 'header nav.tabs');
    await admin.click('Plans', 'header nav.tabs');
  });

  await part('Location', async () => {
    await anna.place(68.2, 13.6);
    await admin.place(68.21, 13.62);
    await anna.click('Location', 'header nav.tabs');
    await anna.choose('What this device reports', /latest only/, 'eds-location');
    await admin.click('Location', 'header nav.tabs');
    await admin.see(ANNA.name, 'eds-location');
    step('anna reports where she is, and the other sees it');
    await anna.click('Remove my position', 'eds-location');
    await anna.choose('What this device reports', /^Nothing/, 'eds-location');
    await admin.gone(/from here/, 'eds-location');
    step('and removes it again');
    await anna.click('Location', 'header nav.tabs');
    await admin.click('Location', 'header nav.tabs');
  });

  await part('A call between two browsers', async () => {
    anna.answer(true);
    await anna.click('Voice call', 'eds-chat');
    await admin.see(/In a call|started a call/);
    admin.answer(true);
    await admin.click('Join');
    await admin.see('Hang up', 'eds-call-panel');
    await anna.waitFor('document.querySelectorAll("eds-call-panel video, eds-call-panel audio, eds-call-panel .peer, eds-call-panel li").length > 1',
      'the other in the call');
    step('anna starts a voice call in Fjord, and the administrator joins it');
    await anna.click('Mute', 'eds-call-panel');
    await anna.see('Unmute', 'eds-call-panel');
    step('muting');
    await admin.click('Hang up', 'eds-call-panel');
    await anna.click('Hang up', 'eds-call-panel');
    await anna.gone('Hang up', 'eds-call-panel');
    step('both hang up');
    await admin.click(`Call ${ANNA.name}`, 'eds-circles');
    await admin.see(`Calling ${ANNA.name}…`, 'eds-call-panel');
    await anna.see(new RegExp(`${ADMIN.name} is calling you`));
    await anna.click('Decline');
    await admin.gone('Cancel', 'eds-call-panel');
    step('the administrator calls anna directly; she declines, and his call ends');
    await admin.click(`Call ${ANNA.name}`, 'eds-circles');
    await anna.see(new RegExp(`${ADMIN.name} is calling you`));
    await anna.click('Accept');
    await anna.see('Hang up', 'eds-call-panel');
    await admin.see('Hang up', 'eds-call-panel');
    await anna.click('Hang up', 'eds-call-panel');
    await admin.gone('Hang up', 'eds-call-panel');
    step('he calls again; she accepts, and when she hangs up his call ends too');
  });

  const bjorn = await open('bjorn');
  await part('Reading only', async () => {
    await login(bjorn, BJORN);
    await enter(bjorn, 'Fjord');
    await bjorn.see('Welcome to the fjord, anna and bjorn', 'eds-chat');
    await bjorn.see('You can only read in this room.');
    await bjorn.waitFor('[...document.querySelectorAll("eds-files button")].find((b) => b.textContent === "Upload")?.disabled === true', 'Upload disabled');
    step('bjorn reads the chat and the files, and can\'t write');
  });

  await part('Members and features of a room', async () => {
    await admin.click('Members', 'header');
    await admin.see('Members of Fjord');
    await admin.choose(`Access of ${BJORN.name}`, /read and write/i, 'dialog[open]');
    await admin.settle(500);
    await closeDialog(admin);
    await admin.waitFor(`[...document.querySelectorAll('eds-circles li')].some((li) => li.textContent.includes(${JSON.stringify(BJORN.name)}) && li.textContent.includes('Can read and write') && !li.textContent.includes('ingrid'))`,
      'bjorn can read and write, in the rooms pane');
    // Rune looks at the rooms and the access every 20 seconds.
    await bjorn.gone('You can only read in this room.', 'body', 30_000);
    await bjorn.waitFor('[...document.querySelectorAll("eds-files button")].find((b) => b.textContent === "Upload")?.disabled === false', 'Upload enabled');
    step('the administrator lets bjorn write, which his open Rune follows within 20 seconds');
    await admin.click('Features', 'header');
    await admin.see('Features of Fjord');
    await admin.choose(/Location/, /^Off$/, 'dialog[open]');
    await sleep(500);
    await closeDialog(admin);
    await anna.waitFor('[...document.querySelectorAll("header nav.tabs button")].find((b) => b.textContent === "Location")?.hidden === true',
      'the Location tab gone for anna');
    step('a feature switched off disappears for the members');
  });

  const carla = await open('carla');
  await part('An invitation', async () => {
    await admin.click('Invite someone', 'eds-circles');
    await admin.fill('Name', CARLA.name, 'dialog[open]');
    await admin.click('Invite', 'dialog[open]');
    const link = await admin.waitFor<string>('return document.querySelector("dialog[open] input[aria-label=\'Invitation link\']")?.value', 'the invitation link');
    await closeDialog(admin);
    step('the administrator invites carla to Fjord, with a link');
    await carla.goto(link);
    await carla.see('Welcome to Rune');
    await carla.fill('Passphrase', CARLA.passphrase);
    await carla.fill('Repeat the passphrase', CARLA.passphrase);
    await carla.click('Join');
    await carla.see('Fjord', 'eds-circles');
    await enter(carla, 'Fjord');
    await carla.see('Welcome to the fjord', 'eds-chat');
    step('carla opens the link, chooses her passphrase and is in Fjord');
  });

  await part('A chamber', async () => {
    await enter(admin, 'Vault');
    await admin.click('Members', 'header');
    await add(admin, 'Vault', ANNA.name, /write/i, 'dialog[open]');
    await admin.see(/no keys yet/, 'dialog[open]');
    await closeDialog(admin);
    step('anna is added to the chamber; she has no keys yet');
    // Rune looks for rooms one was added to every 20 seconds.
    await anna.see('Vault', 'eds-circles', 30_000);
    await enter(anna, 'Vault');
    await anna.click('Make my keys', 'eds-chat');
    await anna.click('Make my keys', 'dialog[open]');
    await anna.see('Your recovery key');
    annaRecoveryKey = await anna.waitFor<string>('return document.querySelector("dialog[open] textarea[aria-label=\'Recovery key\']")?.value', 'the recovery key');
    await anna.tick('I have written the recovery key down.');
    await anna.click('Continue');
    await anna.see('This device has your keys.', 'dialog[open]');
    await anna.see(/Your fingerprint/, 'dialog[open]');
    await closeDialog(anna);
    step('anna makes her keys, writes down her recovery key, and sees her fingerprint');
    await anna.waitFor('!document.querySelector("eds-chat")?.textContent.includes("Make my keys")', 'the key of the chamber for anna', 60_000);
    await send(anna, 'Only we can read this');
    await admin.see('Only we can read this', 'eds-chat');
    step('the administrator\'s Rune gives her the key; she writes in the chamber, and the other reads it');
    // Names in a chamber are encrypted too (E2E.md): both clients, each with
    // its own key, show the real name; the stone never sees it.
    const secret = file('quarterly-secret.txt', 'Revenue up 40%. Keep it in the chamber.\n');
    await admin.files('eds-files input[type=file]', [secret]);
    await admin.see('quarterly-secret.txt', 'eds-files');
    await anna.see('quarterly-secret.txt', 'eds-files', 30_000);
    step('a file is uploaded to the chamber; both, with their own keys, see its real name');
    // A raw read of the stone's own API, bypassing Rune, must show no
    // plaintext name — only Base64url ciphertext.
    const stored = await admin.run<{ names: string[] }>(`async () => {
      const token = JSON.parse(sessionStorage.getItem('eds.session')).token;
      const headers = { Authorization: 'Bearer ' + token };
      const circles = await (await fetch('/api/v1/circles', { headers })).json();
      const vault = circles.circles.find((c) => c.circleName === 'Vault');
      const data = await (await fetch('/api/v1/data?circleId=' + vault.circleId, { headers })).json();
      return { names: (data.metadata || []).map((m) => m.dataName) };
    }`);
    if (stored.names.includes('quarterly-secret.txt')) {
      throw new Error(`the stone stored the file name in cleartext: ${JSON.stringify(stored.names)}`);
    }
    if (!stored.names.some((n) => /^[A-Za-z0-9_-]{80,}$/.test(n))) {
      throw new Error(`the stored name is not the encrypted form: ${JSON.stringify(stored.names)}`);
    }
    step('the stone stored the name encrypted: its own API returns only ciphertext');
  });

  await part('The own account', async () => {
    await menu(anna, 'Change avatar…');
    await anna.clickAt('dialog[open] .avatar-choice');
    await anna.waitFor('document.querySelectorAll("dialog[open]").length === 0', 'the avatar saved');
    await anna.waitFor('document.querySelector(".account-button .avatar.picture svg") !== null', 'the picture on the own button');
    await admin.waitFor(`[...document.querySelectorAll('eds-circles li')].some((li) => li.textContent.includes(${JSON.stringify(ANNA.name)}) && li.querySelector('.avatar.picture'))`,
      'anna\'s picture for the others', 30_000);
    step('anna chooses an avatar, which the others see too');
    await menu(anna, 'Change passphrase…');
    await anna.fill('Current passphrase', ANNA.passphrase);
    await anna.fill('New passphrase', ANNA_NEW);
    await anna.fill('Repeat the new passphrase', ANNA_NEW);
    // The new passphrase ends the session; Rune logs in again with it. A
    // request under way in between is refused (403).
    anna.expect(403, /\/api\/v1\//);
    anna.expect(403, /\/api\/v1\//);
    await anna.click('Change passphrase', 'dialog[open] form');
    await anna.waitFor('document.querySelectorAll("dialog[open]").length === 0 && document.querySelector("eds-app") !== null', 'Rune with the new session');
    await anna.see('Fjord', 'eds-circles');
    step('anna changes her passphrase; Rune goes on with a new session');
    await menu(anna, 'Log out');
    await anna.see('Sign in');
    await login(anna, { name: ANNA.name, passphrase: ANNA_NEW });
    step('and she logs in with the new one');
    await menu(anna, 'Theme…');
    await anna.choose('Appearance', 'Dark', 'dialog[open]');
    await anna.waitFor('document.documentElement.dataset.theme === "dark"', 'the dark theme');
    // Rune's own palette is the one without a mark.
    for (const [palette, value] of [[/^Fjord/, 'fjord'], [/^Moss/, 'moss'], ['High contrast', 'contrast'], [/^Rune/, undefined]] as const) {
      await anna.choose('Theme', palette, 'dialog[open]');
      await anna.waitFor(`document.documentElement.dataset.palette === ${JSON.stringify(value)}`, `the palette ${value ?? 'rune'}`);
    }
    await anna.choose('Appearance', 'Light', 'dialog[open]');
    await closeDialog(anna);
    step('the appearance and the palettes change at once');
    await menu(anna, 'Panes…');
    await closeDialog(anna);
    await menu(anna, 'Notifications…');
    await anna.tick(/^On, with the room/);
    await closeDialog(anna);
    await menu(anna, 'Online status…');
    await anna.tick('Show me as online', false);
    await anna.tick('Show me as online', true);
    await closeDialog(anna);
    step('panes, notifications and online status');
    await menu(anna, 'Language and clock…');
    await anna.choose('Language', /Deutsch|German|^de/);
    await anna.see(/Abmelden|Räume/, 'body', 30_000);
    await anna.clickAt('.account-button');
    await anna.click('Sprache und Uhrzeit …', '.account-menu');
    await anna.choose(/^Sprache/, /English|^en/);
    await anna.see('Rooms', 'eds-circles', 30_000);
    step('Rune in German, and back in English');
  });

  await part('On a phone', async () => {
    await bjorn.size(390, 844, true);
    await bjorn.click('Rooms', 'header nav.tabs');
    await bjorn.see('Fjord', 'eds-circles');
    await enter(bjorn, 'Fjord');
    await bjorn.click('Files', 'header nav.tabs');
    await bjorn.see('Trips', 'eds-files');
    await bjorn.click('Chat', 'header nav.tabs');
    await bjorn.see('Welcome to the fjord', 'eds-chat');
    step('on a phone, one pane at a time, switched by the tabs');
  });

  await part('Leaving, removing and deleting', async () => {
    bjorn.answer(true);
    await bjorn.click('Members', 'header');
    await bjorn.click('Leave room', 'dialog[open]');
    await bjorn.gone('Fjord', 'eds-circles');
    step('bjorn leaves Fjord');
    admin.answer('Fjord and islands');
    await members(admin, 'Fjord');
    await admin.click('Rename room…', await entryOf(admin, 'room', 'Fjord'));
    await admin.see('Fjord and islands', 'eds-circles');
    // The others' Rune looks at the rooms every 20 seconds.
    await anna.see('Fjord and islands', 'eds-circles', 30_000);
    step('the room is renamed, for everyone in it');
    const dagny = await entryOf(admin, 'user', 'dagny');
    if (!await admin.run<boolean>(`() => rune.visible('${dagny} button').some((b) => b.textContent === 'Delete user')`)) {
      await admin.click('dagny', dagny);
    }
    admin.answer(true);
    await admin.click('Delete user', dagny);
    await admin.gone('dagny', 'eds-circles');
    step('the administrator deletes a user');
    await admin.click('Room avatar…', await entryOf(admin, 'room', 'Fjord and islands'));
    await admin.clickAt('dialog[open] .avatar-choice');
    await anna.waitFor(`[...document.querySelectorAll('eds-circles li.room')].some((li) => li.textContent.includes('Fjord and islands') && li.querySelector('.avatar.picture'))`,
      'the room\'s picture for anna', 30_000);
    step('the room gets an avatar, which its members see');
    await menu(admin, 'Stone settings…');
    admin.answer(true);
    await admin.click('Withdraw', 'dialog[open] .setting-group:has(input[aria-label="Label of the new status key"])');
    await admin.gone('click test monitor', 'dialog[open]');
    await admin.click('Close', 'dialog[open]');
    step('the status key is withdrawn');
  });

  const phone = await open('anna-phone');
  await part('A chamber on a second device', async () => {
    await login(phone, { name: ANNA.name, passphrase: ANNA_NEW });
    await enter(phone, 'Vault');
    await phone.click('Enter recovery key', 'eds-chat');
    await phone.fill('Recovery key', annaRecoveryKey, 'dialog[open]');
    await phone.click('Unlock', 'dialog[open]');
    await phone.see('This device has your keys.', 'dialog[open]');
    await closeDialog(phone);
    await phone.see('Only we can read this', 'eds-chat', 30_000);
    step('anna, on another device, opens the chamber with her recovery key');
  });
}

/** The browsers' own dialogs, in order: shown when an answer and a dialog didn't match. */
const shownDialogs: string[] = [];

function report(failure?: unknown): never {
  if (problems.some((problem) => problem.kind === 'dialog')) {
    console.log('\nThe dialogs of the browsers, in order:');
    for (const line of shownDialogs) {
      console.log(`  ${line}`);
    }
  }
  const seconds = Math.round((Date.now() - started) / 1000);
  if (problems.length > 0) {
    console.log(`\nWhat the pages showed the developer (${problems.length}):`);
    for (const problem of problems) {
      console.log(`  ${problem.time} ${problem.browser} ${problem.kind}: ${problem.text}`);
    }
  }
  if (failure) {
    console.log(`\nFAILED in "${section}" after ${steps} steps (${seconds} s):\n  ${failure instanceof Error ? failure.message : String(failure)}`);
    console.log(`Screenshots: ${out}`);
  } else if (problems.length > 0) {
    console.log(`\nFAILED: ${steps} steps passed in ${seconds} s, but the pages showed ${problems.length} problems.`);
  } else {
    console.log(`\nPASSED: ${steps} steps in ${seconds} s, nothing else found.`);
  }
  process.exit(failure || problems.length > 0 ? 1 : 0);
}

const remember = (browser: Browser) => shownDialogs.push(...browser.dialogs.map((text) => `${browser.name}: ${text.slice(0, 90)}`));

try {
  await main();
  for (const browser of browsers) {
    remember(browser);
    await browser.close();
  }
  report();
} catch (failure) {
  for (const browser of browsers) {
    await browser.screenshot(join(out, `${browser.name}.png`)).catch(() => undefined);
  }
  for (const browser of browsers) {
    remember(browser);
    await browser.close();
  }
  report(failure);
}
