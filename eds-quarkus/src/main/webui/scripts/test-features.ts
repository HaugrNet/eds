// Checks the logic of Rune's features (FEATURES.md): plans against the shared
// examples (src/features/plans-examples.json, made by vectors/plans-v1.py),
// the calendar's iCalendar and repeating, and the location's distances.
//   node scripts/test-features.ts
import { readFileSync } from 'node:fs';
import { amountText, balances, confirms, minor, parts, settle } from '../src/features/plans.ts';
import { addDays, callsDue, entryName, instant, occurrences, parseCalendar, parseEvent, wallIn, writeCalendar, writeEvent, type CalendarEvent } from '../src/features/calendar.ts';
import { coordinates, distance, geoLink, isPosition, positionBody } from '../src/features/location.ts';
import { MAX_PEOPLE, ROOM_CALL, RoomCalls, offers, parseServers, readMessage, relayServers, writeMessage, writeServers } from '../src/features/calls.ts';
import { apiAddress, busy, curve, fullAt, latest, level, merge } from '../src/status.ts';
import { noteGiven, type Given } from '../src/reactions.ts';

let checks = 0;
const failures: string[] = [];
/** The same value with the keys of its objects in order, so two objects with the same content compare equal. */
function ordered(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(ordered);
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, ordered((value as Record<string, unknown>)[key])]));
  }
  return value;
}

function same(what: string, actual: unknown, expected: unknown): void {
  checks++;
  if (JSON.stringify(ordered(actual)) !== JSON.stringify(ordered(expected))) {
    failures.push(`${what}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}

// --- Plans -------------------------------------------------------------------------
const examples = JSON.parse(readFileSync(new URL('../src/features/plans-examples.json', import.meta.url), 'utf8')).examples;
for (const example of examples) {
  const worked = balances(example.expenses, example.payments);
  const shown: Record<string, Record<string, string>> = {};
  const dues: Record<string, unknown[]> = {};
  for (const currency of [...worked.keys()].sort()) {
    shown[currency] = Object.fromEntries([...worked.get(currency)!].sort(([a], [b]) => (a < b ? -1 : 1)).map(([person, units]) => [person, amountText(units)]));
    dues[currency] = settle(worked.get(currency)!);
  }
  const sorted = (value: Record<string, unknown>) => Object.fromEntries(Object.keys(value).sort().map((key) => [key, value[key]]));
  same(`plans, "${example.name}": balances`, shown, Object.fromEntries(Object.keys(example.balances).sort().map((c) => [c, sorted(example.balances[c])])));
  same(`plans, "${example.name}": settling`, dues, sorted(example.settle));
}
same('an amount in hundredths', [minor('12.5'), minor('12,50'), minor('0.07'), minor('7'), minor('1.234'), minor('-1'), minor('abc')], [1250, 1250, 7, 700, null, null, null]);
same('hundredths as text', [amountText(1250), amountText(-7), amountText(0)], ['12.50', '-0.07', '0.00']);
same('a hundredth left over goes to the first', parts({ amount: '10.00', split: [{ who: 'a', share: 1 }, { who: 'b', share: 1 }, { who: 'c', share: 1 }] }), [334, 333, 333]);
const payment = { body: { v: 1 as const, from: 'm:a', to: 'm:b', amount: '5.00', currency: 'EUR' }, added: '2026-11-15T10:00:00', changed: '2026-11-15T12:00:00' };
same('a confirmation by the receiver, after the last change', confirms(payment, { memberId: 'b', added: '2026-11-15T12:00:01' }), true);
same('one from before the change does not count', confirms(payment, { memberId: 'b', added: '2026-11-15T11:00:00' }), false);
same('nor one by someone else', confirms(payment, { memberId: 'a', added: '2026-11-15T13:00:00' }), false);

// --- Calendar ----------------------------------------------------------------------
const stamp = new Date('2026-11-15T09:00:00Z');
const dinner: CalendarEvent = {
  uid: 'dinner-1@rune', title: 'Dinner; with Åsa, at "Hearth"', allDay: false, start: '2026-11-20T19:00', end: '2026-11-20T21:30',
  zone: 'Europe/Berlin', place: 'Hearth, Main St 1', note: 'Bring wine\nand bread', other: [],
};
const written = writeEvent(dinner, stamp);
same('an event as iCalendar', written.split('\r\n'), [
  'BEGIN:VEVENT', 'UID:dinner-1@rune', 'DTSTAMP:20261115T090000Z', 'DTSTART;TZID=Europe/Berlin:20261120T190000', 'DTEND;TZID=Europe/Berlin:20261120T213000',
  'SUMMARY:Dinner\\; with Åsa\\, at "Hearth"', 'LOCATION:Hearth\\, Main St 1', 'DESCRIPTION:Bring wine\\nand bread', 'END:VEVENT']);
same('and read again', parseEvent(written), dinner);
same('its entry is named by its month', entryName(dinner), '2026-11/dinner-1@rune');
same('19:00 in Berlin in winter is 18:00 UTC', instant(dinner.start, dinner.zone).toISOString(), '2026-11-20T18:00:00.000Z');
same('and in summer 17:00 UTC', instant('2026-07-20T19:00', 'Europe/Berlin').toISOString(), '2026-07-20T17:00:00.000Z');
same('an instant as the wall of a zone', wallIn(new Date('2026-11-20T18:00:00Z'), 'America/New_York'), '2026-11-20T13:00');
same('it takes place once', occurrences(dinner, new Date('2026-11-01T00:00:00Z'), new Date('2026-12-01T00:00:00Z')).map((o) => [o.start.toISOString(), o.end.toISOString()]),
  [['2026-11-20T18:00:00.000Z', '2026-11-20T20:30:00.000Z']]);
same('and not in another month', occurrences(dinner, new Date('2026-12-01T00:00:00Z'), new Date('2027-01-01T00:00:00Z')).length, 0);

const long = { ...dinner, title: 'A title which is long enough that its line must be folded, with ümläüts and more text to be sure of it' };
const folded = writeEvent(long, stamp).split('\r\n');
same('no line is longer than 75 octets', folded.every((line) => new TextEncoder().encode(line).length <= 75), true);
same('a folded line is read whole', parseEvent(folded.join('\r\n'))?.title, long.title);

const weekly: CalendarEvent = { uid: 'w@rune', title: 'Choir', allDay: false, start: '2026-03-23T18:00', end: '2026-03-23T19:00', zone: 'Europe/Berlin',
  repeat: { every: 'weekly', interval: 1, until: '2026-04-06' }, other: [] };
same('a weekly event keeps its time on the wall over the change of clocks',
  occurrences(weekly, new Date('2026-03-01T00:00:00Z'), new Date('2026-05-01T00:00:00Z')).map((o) => o.start.toISOString()),
  ['2026-03-23T17:00:00.000Z', '2026-03-30T16:00:00.000Z', '2026-04-06T16:00:00.000Z']);
same('its rule', writeEvent(weekly, stamp).split('\r\n').find((line) => line.startsWith('RRULE')), 'RRULE:FREQ=WEEKLY;UNTIL=20260406T235959Z');
same('read again', parseEvent(writeEvent(weekly, stamp)), weekly);
same('a repeating event is named apart', entryName(weekly), 'repeat/w@rune');

const monthly: CalendarEvent = { uid: 'm@rune', title: 'Rent', allDay: true, start: '2026-01-31', end: '2026-02-01', repeat: { every: 'monthly', interval: 1, count: 4 }, other: [] };
const days = (event: CalendarEvent, from: string, to: string) => occurrences(event, instant(from), instant(to)).map((o) => [o.start.getFullYear(), o.start.getMonth() + 1, o.start.getDate()].join('-'));
same('monthly on the 31st passes over the shorter months, four times in all', days(monthly, '2026-01-01', '2027-01-01'), ['2026-1-31', '2026-3-31', '2026-5-31', '2026-7-31']);
const yearly: CalendarEvent = { uid: 'y@rune', title: 'Birthday', allDay: true, start: '2024-02-29', end: '2024-03-01', repeat: { every: 'yearly', interval: 1 }, other: [] };
same('yearly on the 29th of February only in leap years', days(yearly, '2024-01-01', '2033-01-01'), ['2024-2-29', '2028-2-29', '2032-2-29']);
const daily: CalendarEvent = { uid: 'd@rune', title: 'Pills', allDay: true, start: '2020-01-01', end: '2020-01-02', repeat: { every: 'daily', interval: 3 }, other: [] };
same('every third day, far from its start', days(daily, '2026-11-01', '2026-11-11'), ['2026-11-1', '2026-11-4', '2026-11-7', '2026-11-10']);
const trip: CalendarEvent = { uid: 't@rune', title: 'Trip', allDay: true, start: '2026-11-28', end: '2026-12-03', other: [] };
same('whole days over the end of a month show in both', [days(trip, '2026-11-01', '2026-12-01').length, days(trip, '2026-12-01', '2027-01-01').length], [1, 1]);
same('the day after', [addDays('2026-12-31', 1), addDays('2026-03-01', -1)], ['2027-01-01', '2026-02-28']);

const foreign = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'BEGIN:VEVENT', 'UID:abc', 'DTSTART:20261224T170000Z', 'DURATION:PT1H30M', 'SUMMARY:Carols',
  'RRULE:FREQ=MONTHLY;BYDAY=2TU', 'X-CUSTOM;X=1:kept', 'BEGIN:VALARM', 'ACTION:DISPLAY', 'TRIGGER:-PT15M', 'END:VALARM', 'END:VEVENT',
  'BEGIN:VEVENT', 'UID:def', 'DTSTART;VALUE=DATE:20261225', 'SUMMARY:Holiday', 'END:VEVENT', 'END:VCALENDAR'].join('\r\n');
const read = parseCalendar(foreign);
same('a file of another program: two events', read.map((event) => [event.uid, event.start, event.end, event.zone, event.allDay]),
  [['abc', '2026-12-24T17:00', '2026-12-24T18:30', 'UTC', false], ['def', '2026-12-25', '2026-12-26', undefined, true]]);
same('a rule Rune does not know is kept, and the event shown once', [read[0].rule, read[0].repeat, entryName(read[0])], ['FREQ=MONTHLY;BYDAY=2TU', undefined, 'repeat/abc']);
same('what Rune does not know of an event survives', read[0].other, ['X-CUSTOM;X=1:kept', 'BEGIN:VALARM', 'ACTION:DISPLAY', 'TRIGGER:-PT15M', 'END:VALARM']);
same('and goes out again', parseCalendar(writeCalendar(read, stamp)), read);
same('not an event', [parseEvent('hello'), parseEvent('BEGIN:VEVENT\r\nSUMMARY:no start\r\nEND:VEVENT')], [null, null]);

// A call set for a time is an event with a line of its own (FEATURES.md, "Presence and calls").
const standup: CalendarEvent = { ...dinner, uid: 'standup@rune', title: 'Stand-up', call: 'video', other: ['X-CUSTOM:kept'] };
same('an event which is a call has its line, before what Rune does not know', writeEvent(standup, stamp).split('\r\n').slice(-3), ['X-EDS-CALL:video', 'X-CUSTOM:kept', 'END:VEVENT']);
same('and is read again', parseEvent(writeEvent(standup, stamp)), standup);
same('a call with the voice', parseEvent(writeEvent({ ...standup, call: 'audio' }, stamp))?.call, 'audio');
const odd = parseEvent(writeEvent({ ...dinner, other: ['X-EDS-CALL:hologram'] }, stamp));
same('a kind of call Rune does not know makes no call, and the line is kept', [odd?.call, odd?.other], [undefined, ['X-EDS-CALL:hologram']]);
same('an event which is no call has no such line', written.includes('X-EDS-CALL'), false);
const weeklyCall: CalendarEvent = { ...weekly, uid: 'wc@rune', title: 'Weekly', call: 'audio' };
const wholeDayCall: CalendarEvent = { ...trip, uid: 'tc@rune', call: 'audio' };
same('the calls which are due: those which begin in the time looked at, or go on at its start, the earliest first; no other events, and no whole days',
  callsDue([dinner, weeklyCall, standup, wholeDayCall], new Date('2026-11-20T19:00:00Z'), new Date('2026-11-21T19:00:00Z')).map((due) => [due.uid, due.media, due.start.toISOString()]),
  [['standup@rune', 'video', '2026-11-20T18:00:00.000Z']]);
same('a call which repeats is due each time', callsDue([weeklyCall], new Date('2026-03-29T00:00:00Z'), new Date('2026-04-07T00:00:00Z')).map((due) => due.start.toISOString()),
  ['2026-03-30T16:00:00.000Z', '2026-04-06T16:00:00.000Z']);
same('nothing is due long before', callsDue([standup], new Date('2026-11-01T00:00:00Z'), new Date('2026-11-02T00:00:00Z')), []);

// --- Calls -------------------------------------------------------------------------
const alice = '11111111-1111-4111-8111-111111111111';
const bert = '22222222-2222-4222-8222-222222222222';
const offer = { t: 'offer' as const, call: ROOM_CALL, peer: alice, target: bert, sdp: 'v=0\r\n' };
same('a message there and back', readMessage(writeMessage(offer)), { v: 1, ...offer });
same('what a version does not know is passed over', readMessage(JSON.stringify({ v: 1, t: 'join', call: ROOM_CALL, peer: alice, media: 'video', later: true })),
  { v: 1, t: 'join', call: ROOM_CALL, peer: alice, media: 'video' });
same('a candidate as WebRTC gives it', readMessage(writeMessage({ t: 'ice', call: alice, peer: alice, target: bert, candidate: { candidate: 'candidate:1 1 udp 1 192.0.2.1 9 typ host', sdpMid: '0', sdpMLineIndex: 0 } }))?.candidate,
  { candidate: 'candidate:1 1 udp 1 192.0.2.1 9 typ host', sdpMid: '0', sdpMLineIndex: 0, usernameFragment: null });
same('not a message: no JSON, another version, an unknown type, no call, an offer without its description or for nobody, a join of nobody',
  ['hello', '{"v":2,"t":"join","call":"room","peer":"' + alice + '"}', '{"v":1,"t":"dance","call":"room"}', '{"v":1,"t":"probe"}', '{"v":1,"t":"probe","call":"all rooms"}',
    writeMessage({ t: 'offer', call: ROOM_CALL, peer: alice, target: bert }), writeMessage({ t: 'offer', call: ROOM_CALL, peer: alice, sdp: 'v=0' }), writeMessage({ t: 'join', call: ROOM_CALL })].map(readMessage),
  [null, null, null, null, null, null, null, null]);
same('a probe and a ring need little', [readMessage(writeMessage({ t: 'probe', call: ROOM_CALL }))?.t, readMessage(writeMessage({ t: 'ring', call: alice, media: 'audio' }))?.media], ['probe', 'audio']);
same('of two devices the one with the smaller text offers, and only one of them', [offers(alice, bert), offers(bert, alice), offers(alice, alice)], [true, false, false]);
same('a call has four people at most', MAX_PEOPLE, 4);

const servers = parseServers('stun:stun.example.org:3478\n\n  turn:turn.example.org:3478?transport=tcp  anna  s3cret with spaces \nturns:[2001:db8::1]:5349 bo pw');
same('the relay as an administrator writes it', servers, [
  { urls: ['stun:stun.example.org:3478'] },
  { urls: ['turn:turn.example.org:3478?transport=tcp'], username: 'anna', credential: 's3cret with spaces' },
  { urls: ['turns:[2001:db8::1]:5349'], username: 'bo', credential: 'pw' }]);
same('and written again', parseServers(writeServers(servers)), servers);
let refused = '';
try {
  parseServers('stun:ok.example.org\nhttps://example.org/turn');
} catch (error) {
  refused = (error as Error).message;
}
same('a line which is no server is named', refused, 'https://example.org/turn');
same('the servers of an entry, without what is not one', relayServers({ v: 1, servers: [{ urls: ['turn:t.example.org', 'javascript:alert(1)'], username: 'u', credential: 'c' }, { urls: 'stun:s.example.org' }, { urls: [] }, null, { urls: ['ftp://x'] }] }),
  [{ urls: ['turn:t.example.org'], username: 'u', credential: 'c' }, { urls: ['stun:s.example.org'] }]);
same('an entry which is none has no servers', [relayServers(null), relayServers({ v: 1 }), relayServers({ servers: 'x' })], [[], [], []]);

const going = new RoomCalls();
const join = (peer: string) => readMessage(writeMessage({ t: 'join', call: ROOM_CALL, peer, media: 'audio' }))!;
same('a join begins the call of a room', [going.note('r1', 'm-alice', join(alice)), going.members('r1')], [true, ['m-alice']]);
same('told twice, it changes nothing', going.note('r1', 'm-alice', join(alice)), false);
going.note('r1', 'm-alice', join(bert));
going.note('r1', 'm-bert', readMessage(writeMessage({ t: 'ongoing', call: ROOM_CALL, peer: '33333333-3333-4333-8333-333333333333', media: 'video', peers: 3 }))!);
same('two devices of one member are one person', [going.peers('r1').length, going.members('r1')], [3, ['m-alice', 'm-bert']]);
same('a direct call is not the call of the room', going.note('r1', 'm-carl', readMessage(writeMessage({ t: 'join', call: alice, peer: alice }))!), false);
going.note('r1', 'm-alice', readMessage(writeMessage({ t: 'leave', call: ROOM_CALL, peer: alice }))!);
same('a device left, its member has another', going.members('r1'), ['m-alice', 'm-bert']);
same('a member who is no longer present left with all devices', [going.memberWent('r1', 'm-alice'), going.members('r1'), going.memberWent('r1', 'm-alice')], [true, ['m-bert'], false]);
same('another room has no call', [going.members('r2'), going.peers('r2')], [[], []]);
going.forget('r1');
same('and a room which is forgotten has none', going.members('r1'), []);

// --- Location ----------------------------------------------------------------------
const berlin = { lat: 52.52, lon: 13.405 };
const hamburg = { lat: 53.5511, lon: 9.9937 };
same('Berlin to Hamburg is about 255 km', Math.round(distance(berlin, hamburg) / 1000), 255);
same('no distance to itself', distance(berlin, berlin), 0);
same('a position as an entry has it', positionBody(52.5200066, 13.4049541, 12.6, new Date('2026-11-15T10:00:00Z')), { v: 1, lat: 52.520007, lon: 13.404954, at: '2026-11-15T10:00:00.000Z', acc: 13 });
same('what is a position', [isPosition({ v: 1, lat: 1, lon: 2, at: '2026-11-15T10:00:00Z' }), isPosition({ lat: 91, lon: 2, at: '2026-11-15T10:00:00Z' }), isPosition({ lat: 1, lon: 2 }), isPosition(null)], [true, false, false, false]);
same('the address for a map app', geoLink({ lat: 52.52, lon: 13.405, acc: 13 }), 'geo:52.52,13.405;u=13');
same('a place as numbers', coordinates({ lat: -33.8688, lon: 151.2093 }), '33.86880° S, 151.20930° E');

// --- Stones (STATUS.md) ---------------------------------------------------------------
same('the address of a stone', [apiAddress('stone.example'), apiAddress('https://stone.example/'), apiAddress('http://localhost:8080/api/v1/'), apiAddress('https://host/eds'), apiAddress('  '), apiAddress('http://')],
  ['https://stone.example/api/v1', 'https://stone.example/api/v1', 'http://localhost:8080/api/v1', 'https://host/eds/api/v1', null, null]);
const sample = (sequence: number, values: Record<string, number> = {}, time = '2026-11-15T10:00:00') => ({ sequence, time, values });
same('new samples are added without doubles', merge([sample(1), sample(2)], [sample(2), sample(3)]).map((s) => s.sequence), [1, 2, 3]);
same('a stone which started anew begins again', merge([sample(100), sample(101)], [sample(1), sample(2)]).map((s) => s.sequence), [1, 2]);
same('nothing new', merge([sample(1)], undefined).map((s) => s.sequence), [1]);
same('the latest value told', [latest([sample(1, { a: 1 }), sample(2, { b: 2 })], 'a'), latest([sample(1)], 'a')], [1, undefined]);
const filling = [sample(1, { storageUsed: 100, storageCeiling: 1000 }, '2026-11-15T10:00:00'), sample(2, { storageUsed: 400, storageCeiling: 1000 }, '2026-11-15T13:00:00')];
same('full in six hours if it fills as it did', new Date(fullAt(filling) as number).toISOString(), '2026-11-15T19:00:00.000Z');
same('not filling, or no ceiling: no estimate', [fullAt([filling[1], filling[0]].map((s, i) => ({ ...s, sequence: i }))), fullAt([filling[0]]), fullAt(filling.map((s) => ({ ...s, values: { storageUsed: s.values.storageUsed, storageCeiling: 0 } })))], [null, null, null]);
same('how near a limit', [level(0.5, 0.7, 0.9), level(0.8, 0.7, 0.9), level(0.95, 0.7, 0.9), level(undefined, 0.7, 0.9)], ['fine', 'watch', 'near', 'fine']);
same('how busy, by the share of the processors or the host', [busy([sample(1, { processShare: 0.25, systemLoad: 8, processors: 4 })]), busy([sample(1, { systemLoad: 2, processors: 4 })]), busy([])], [0.25, 0.5, 0]);
same('a curve', curve([0, 5, 10], 100, 20), '0.0,20.0 50.0,10.0 100.0,0.0');

// Reactions (src/reactions.ts): the stone replaces a member's entry with every
// change, and what it announces of the old one may arrive late.
const reaction = (entryId: string, position: number, ...reactions: string[]) => ({ entryId, position, memberId: 'birgit', body: { reactions } });
const gave = new Map<string, Given>();
const birgit = () => gave.get('birgit')?.reactions ?? null;
noteGiven(gave, reaction('first', 1, '👍'), false);
noteGiven(gave, reaction('second', 2, '👍', '❤️'), false);
noteGiven(gave, reaction('first', 1, '👍'), true);
same('the end of an earlier entry, told late, takes nothing', birgit(), ['👍', '❤️']);
noteGiven(gave, reaction('first', 1, '👍'), false);
same('an earlier entry, told late, changes nothing', birgit(), ['👍', '❤️']);
noteGiven(gave, reaction('third', 3), false);
noteGiven(gave, reaction('second', 2, '👍', '❤️'), false);
same('an entry without reactions is the newest too', birgit(), []);
noteGiven(gave, reaction('third', 3), true);
same('the end of the known entry ends the reactions', birgit(), null);
noteGiven(gave, { entryId: 'fourth', position: 4, body: { reactions: ['👍'] } }, false);
same('an entry of nobody is passed over', gave.size, 0);

if (failures.length > 0) {
  console.error(failures.join('\n'));
  console.error(`features: ${failures.length} of ${checks} checks failed.`);
  process.exit(1);
}
console.log(`features: ${checks} checks passed.`);
