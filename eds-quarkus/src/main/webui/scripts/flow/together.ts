// Rune and Tolk together (E2E.md phase E6, FEATURES.md phase T6): Rune's own
// modules as one person, the Tolk binary as another, in the same chambers
// of one stone. Each reads what the other wrote: the chat, files, the
// calendar, plans, positions; invitations go both ways; and both show the
// same fingerprints.
//
//   node node_modules/vite/bin/vite.js build --config vite.flow.config.ts
//   (cd ../../../../eds-cli && go build -o /tmp/tolk ./cmd/tolk)
//   node scripts/flow/together.ts http://localhost:8080 /tmp/tolk
//
// The stone must be empty of the accounts used here; a test system (the
// in-memory database) is meant.
import { execFileSync } from 'node:child_process';
import { createHash, randomBytes } from 'node:crypto';
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { base, checks, same, step, User, yes } from './harness.ts';
import { entryName, parseEvent, writeEvent, type CalendarEvent } from '../../src/features/calendar.ts';
import { amountText, balances, settle } from '../../src/features/plans.ts';

const binary = process.argv[3];
if (!binary) {
  console.error('together: name the Tolk binary, e.g. node scripts/flow/together.ts http://localhost:8080 /tmp/tolk');
  process.exit(2);
}
const ADMIN = 'the stone keeper has a long passphrase, 2026';
const PASS = 'a rather long passphrase for the test of both, 2026';

// Tolk with a configuration of its own, and its container's passphrase in a file.
const home = mkdtempSync(join(tmpdir(), 'tolk-together-'));
writeFileSync(join(home, 'pass'), 'the passphrase of the container\n', { mode: 0o600 });
const env = { ...process.env, TOLK_CONFIG_DIR: home, TOLK_AGENT_DIR: join(home, 'agent'), TOLK_PASSPHRASE_FILE: join(home, 'pass'), TOLK_KDF_ITERATIONS: '1000', LC_ALL: 'C', TZ: 'Europe/Berlin' };

/** Runs Tolk; returns what it wrote to standard output. */
function tolk(args: string[], input = ''): string {
  try {
    return execFileSync(binary, args, { env, input, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'], maxBuffer: 64 << 20 });
  } catch (error) {
    const failed = error as { stderr?: string; status?: number };
    throw new Error(`tolk ${args.join(' ')}: exit code ${failed.status}\n${failed.stderr ?? ''}`);
  }
}

/** Runs Tolk, expecting it to fail; returns what it said. */
function tolkRefused(what: string, args: string[]): string {
  try {
    execFileSync(binary, args, { env, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] });
  } catch (error) {
    yes(what, true);
    return (error as { stderr?: string }).stderr ?? '';
  }
  throw new Error(`${what}: tolk ${args.join(' ')} was not refused`);
}

/** The result of a Tolk command for one room, from its JSON. */
function result(out: string): any {
  const first = JSON.parse(out).results[0];
  if (!first.ok) {
    throw new Error(JSON.stringify(first));
  }
  return first.result;
}

/** The texts of the history which 'tolk chat --plain' wrote. */
function history(room: string): string[] {
  return tolk(['chat', '--plain', '--room', room, '--history', '20']).split('\n').filter(Boolean).map((line) => JSON.parse(line))
    .filter((event) => event.history).map((event) => event.text as string);
}

const sha = (data: Uint8Array) => createHash('sha256').update(data).digest('hex');

const admin = new User('admin');
const anna = new User('anna');
const carl = new User('carl');
const users = [admin, anna, carl];

try {
  step('a stone with its features, and anna, who uses Rune');
  await admin.do('login', 'admin', ADMIN);
  for (const feature of ['chat', 'calendar', 'plans', 'location']) {
    await admin.do('offerFeature', feature);
  }
  await admin.do('createUser', 'both-anna', PASS);
  await anna.do('login', 'both-anna', PASS);

  step('anna makes a chamber in Rune, writes, plans, and puts a file there');
  const made = await anna.do('createRoom', 'Both', true, ['chat', 'calendar', 'plans', 'location']);
  const chamber = made.circleId as string;
  await anna.do('join', chamber);
  await anna.do('send', chamber, 'hello from Rune');
  const dinner: CalendarEvent = { uid: 'both-dinner@rune', title: 'Dinner', allDay: false, start: '2026-12-04T19:00', end: '2026-12-04T21:00', zone: 'Europe/Berlin', place: 'Hearth', other: [] };
  await anna.do('addTo', chamber, 'calendar', 'event', { v: 1, ics: writeEvent(dinner) }, undefined, entryName(dinner));
  const runeFile = await anna.do('putFile', chamber, 'rune.bin', 2 * 65536 + 777);

  step('Tolk accepts her invitation: the link carries the key of the chamber');
  const invited = await anna.do('invite', chamber, 'both-tom', 'WRITE');
  tolk(['init']);
  const joined = JSON.parse(tolk(['accept', `${base}/${invited.fragment}`, '--stone', 'both', '--passphrase-stdin', '--output', 'json'], `${PASS}\n`));
  yes('Tolk got keys, and a recovery key', /^([A-Z2-7]{4}-){12}[A-Z2-7]{4}$/.test(joined.recoveryKey));
  same('Tolk has the keys', JSON.parse(tolk(['key', 'show', 'both', '--output', 'json'])).keys, 'ready');

  step('Tolk reads what Rune wrote, and writes');
  same('the chat', history('both:Both'), ['hello from Rune']);
  tolk(['chat', '--plain', '--room', 'both:Both', '-m', 'hello from Tolk']);
  const listed = result(tolk(['calendar', '--room', 'both:Both', '--from', '2026-12-01', '--to', '2026-12-07', '--output', 'json']));
  same('the calendar', listed.events.map((event: any) => `${event.title} ${event.start} ${event.place}`), ['Dinner 2026-12-04T18:00:00Z Hearth']);
  tolk(['calendar', 'add', 'Breakfast', '--room', 'both:Both', '--start', '2026-12-05 09:00', '--end', '2026-12-05 10:00', '--zone', 'Europe/Berlin', '--place', 'Kitchen']);
  const fetched = join(home, 'rune.bin');
  tolk(['get', 'both:Both/rune.bin', '-o', fetched]);
  same('the file Rune encrypted, decrypted by Tolk', sha(readFileSync(fetched)), runeFile.sum);
  const tolkBytes = randomBytes(3 * 65536 + 99);
  writeFileSync(join(home, 'tolk.bin'), tolkBytes);
  const sent = result(tolk(['send', join(home, 'tolk.bin'), 'both:Both/', '--output', 'json']));
  const plan = result(tolk(['plan', 'new', 'Weekend', '--room', 'both:Both', '--people', 'both-anna,both-tom,Dana', '--output', 'json']));
  tolk(['plan', 'expense', 'Weekend', '90', 'Cabin', '--room', 'both:Both']);
  tolk(['location', 'report', '53.5511', '9.9937', '--room', 'both:Both', '--note', 'at the harbour']);

  step('Rune reads what Tolk wrote');
  same('the chat', (await anna.do('history', chamber)).map((m: any) => `${m.by}: ${m.text}`), ['both-anna: hello from Rune', 'both-tom: hello from Tolk']);
  const events = (await anna.do('bodies', chamber, 'calendar', 'event')).map((entry: any) => parseEvent(entry.body.ics) as CalendarEvent);
  same('the calendar', events.map((event: CalendarEvent) => `${event.title} ${event.start} ${event.zone} ${event.place}`).sort(),
    ['Breakfast 2026-12-05T09:00 Europe/Berlin Kitchen', 'Dinner 2026-12-04T19:00 Europe/Berlin Hearth']);
  const tolkFile = await anna.do('getFile', chamber, sent.id);
  same('the file Tolk encrypted, decrypted by Rune', [tolkFile.start, tolkFile.sum, tolkFile.unprotected], ['EDSE', sha(tolkBytes), false]);
  const expenses = (await anna.do('bodies', chamber, 'plans', 'expense', plan.id)).map((entry: any) => entry.body);
  await anna.do('addTo', chamber, 'plans', 'expense', { v: 1, text: 'Food', amount: '30.00', currency: 'EUR', paidBy: expenses[0].split[0].who, split: expenses[0].split.slice(0, 2) }, plan.id);
  const all = (await anna.do('bodies', chamber, 'plans', 'expense', plan.id)).map((entry: any) => entry.body);
  const dues = settle(balances(all, []).get('EUR') ?? new Map()).map((due) => due.amount).sort();
  const shown = result(tolk(['plan', 'show', 'Weekend', '--room', 'both:Both', '--output', 'json']));
  same('who owes whom, worked out by both', shown.settle.EUR.map((due: any) => due.amount).sort(), dues);
  same('and what', dues, ['15.00', '30.00'].map((amount) => amountText(Number(amount) * 100)));
  const seen = await anna.do('bodies', chamber, 'location', 'position');
  same('where Tolk said it is', seen.map((entry: any) => [entry.by, entry.body.lat, entry.body.lon, entry.body.note]), [['both-tom', 53.5511, 9.9937, 'at the harbour']]);
  for (const [feature, kind] of [['chat', 'message'], ['calendar', 'event'], ['plans', 'expense'], ['location', 'position']]) {
    yes(`the stone cannot read the ${kind}s`, (await anna.do('held', chamber, feature, kind)).every((entry: any) => entry.start === 'EDSE'));
  }

  step('both show the same fingerprints');
  const prints = await anna.do('fingerprints', chamber);
  const members = result(tolk(['members', '--room', 'both:Both', '--output', 'json'])).members;
  same('the fingerprints', Object.fromEntries(members.map((member: any) => [member.name, member.fingerprint]).sort()), Object.fromEntries(Object.entries(prints).sort()));
  yes('each has one', members.length === 2 && members.every((member: any) => /^([0-9a-f]{4} ){7}[0-9a-f]{4}$/.test(member.fingerprint)));

  step('the other way: Tolk makes a chamber, and Rune accepts its invitation');
  tolk(['room', 'new', 'both:FromTolk', '--chamber']);
  tolk(['chat', '--plain', '--room', 'both:FromTolk', '-m', 'written before carl came']);
  const link = tolk(['invite', 'both-carl', '--room', 'both:FromTolk', '--no-qr']).trim();
  const accepted = await carl.do('accept', link.slice(link.indexOf('#invite=')), PASS);
  yes('the link carried the invite secret, and carl got a recovery key', accepted.hadSecret && /^([A-Z2-7]{4}-){12}[A-Z2-7]{4}$/.test(accepted.recoveryKey));
  const fromTolk: string = (await carl.do('roomId', 'FromTolk')) ?? '';
  yes('carl is in the chamber', fromTolk !== '');
  same('carl holds its key', (await carl.do('roomKind', fromTolk)).kind, 'ready');
  await carl.do('join', fromTolk);
  same('carl reads what was written before he came', (await carl.do('history', fromTolk)).map((m: any) => m.text), ['written before carl came']);
  await carl.do('send', fromTolk, 'carl here, in Rune');
  same('Tolk reads carl', history('both:FromTolk'), ['written before carl came', 'carl here, in Rune']);

  step('Tolk removes carl: the chamber gets a new key, which Rune follows');
  tolk(['add', 'both-anna', '--room', 'both:FromTolk', '--access', 'write']);
  same('anna, added by Tolk, holds the key', (await anna.do('roomKind', fromTolk)).kind, 'ready');
  tolk(['remove', 'both-carl', '--room', 'both:FromTolk']);
  tolk(['chat', '--plain', '--room', 'both:FromTolk', '-m', 'after carl left']);
  same('Tolk reads old and new', history('both:FromTolk'), ['written before carl came', 'carl here, in Rune', 'after carl left']);
  same('anna has both keys after Tolk changed it', await anna.do('roomKind', fromTolk), { kind: 'ready', versions: [1, 2], current: 2 });
  await anna.do('join', fromTolk);
  same('and reads old and new in Rune', (await anna.do('history', fromTolk)).map((m: any) => m.text), ['written before carl came', 'carl here, in Rune', 'after carl left']);
  await carl.refused('carl gets nothing of it any more', 'join', fromTolk);

  step('Rune removes someone: the chamber gets a new key, which Tolk follows');
  await anna.do('addMember', chamber, accepted.memberId, 'READ');
  await anna.do('removeMember', chamber, accepted.memberId);
  same('the chamber of anna has a second key', (await anna.do('roomKind', chamber)).current, 2);
  await anna.do('send', chamber, 'after the change of key');
  same('Tolk reads old and new', history('both:Both'), ['hello from Rune', 'hello from Tolk', 'after the change of key']);
  tolk(['get', 'both:Both/rune.bin', '-o', join(home, 'again.bin')]);
  same('and a file of the earlier key', sha(readFileSync(join(home, 'again.bin'))), runeFile.sum);
  tolk(['chat', '--plain', '--room', 'both:Both', '-m', 'Tolk under the new key']);
  same('Rune reads what Tolk wrote under it', (await anna.do('history', chamber)).map((m: any) => m.text).slice(-2), ['after the change of key', 'Tolk under the new key']);

  step('a room which is no chamber: the stone reads along, and both say so');
  tolk(['room', 'new', 'both:Yard']);
  tolk(['chat', '--plain', '--room', 'both:Yard', '-m', 'in a room']);
  tolk(['cp', 'both:Both/rune.bin', 'both:Yard/']);
  const copied = join(home, 'copied.bin');
  tolk(['get', 'both:Yard/rune.bin', '-o', copied]);
  same('a copy out of the chamber is the file itself', sha(readFileSync(copied)), runeFile.sum);
  const yard: string = (await anna.do('roomId', 'Yard')) ?? '';
  same('anna is not in that room', yard, '');
  tolkRefused('a feature Tolk does not know is not switched on', ['features', 'on', 'nothing', '--room', 'both:Yard']);

  step('who is online, and calls set for a time: each shows what the other has');
  const online = result(tolk(['members', '--room', 'both:Both', '--output', 'json'])).members;
  same('Tolk shows anna, whose Rune is open, as online, and itself not: a command joins unseen',
    online.map((member: any) => `${member.name}:${member.online}`).sort(), ['both-anna:true', 'both-tom:false']);
  const standup: CalendarEvent = { uid: 'standup@rune', title: 'Stand-up', allDay: false, start: '2026-12-07T09:00', end: '2026-12-07T09:15', zone: 'Europe/Berlin', call: 'video', other: [] };
  await anna.do('addTo', chamber, 'calendar', 'event', { v: 1, ics: writeEvent(standup) }, undefined, entryName(standup));
  tolk(['calendar', 'add', 'Check-in', '--room', 'both:Both', '--start', '2026-12-08 09:00', '--end', '2026-12-08 09:15', '--zone', 'Europe/Berlin', '--call', 'audio']);
  const week = result(tolk(['calendar', '--room', 'both:Both', '--from', '2026-12-07', '--to', '2026-12-09', '--output', 'json']));
  same('Tolk lists the call Rune set, and its own', week.events.map((event: any) => `${event.title}:${event.call}`), ['Stand-up:video', 'Check-in:audio']);
  const calls = (await anna.do('bodies', chamber, 'calendar', 'event')).map((entry: any) => parseEvent(entry.body.ics) as CalendarEvent).filter((event: CalendarEvent) => event.call);
  same('Rune reads both as calls', calls.map((event: CalendarEvent) => `${event.title}:${event.call}`).sort(), ['Check-in:audio', 'Stand-up:video']);

  console.log(`together: ${checks()} checks passed.`);
} catch (error) {
  console.error(`together: FAILED after ${checks()} checks\n${(error as Error).message}`);
  process.exitCode = 1;
} finally {
  await Promise.all(users.map((user) => user.end()));
}
