// The flow test of Rune: Rune's own modules, the ones without a page, as
// several users against a running stone. It goes through what people do in
// rooms and chambers, and checks what each of them gets, and what the stone
// holds. The pages themselves aren't part of it.
//
//   node node_modules/vite/bin/vite.js build --config vite.flow.config.ts
//   node scripts/flow/flow.ts http://localhost:8080
//
// The stone must be empty of the accounts used here; a test system (the
// in-memory database) is meant. Its first System Administrator, here
// "admin", is made with the first login.
import { checks, same, step, User, yes } from './harness.ts';
import { entryName, monthPrefix, occurrences, parseEvent, REPEATING, writeEvent, type CalendarEvent } from '../../src/features/calendar.ts';
import { amountText, balances, confirms, settle } from '../../src/features/plans.ts';
import { positionBody } from '../../src/features/location.ts';

const ADMIN = 'the stone keeper has a long passphrase, 2026';
const PASS = 'a rather long passphrase for the flow test, 2026';

const admin = new User('admin');
const alice = new User('alice');
const bob = new User('bob');
const carol = new User('carol');
const bob2 = new User('bob, second device');
const users = [admin, alice, bob, carol, bob2];

try {
  step('the System Administrator makes two users, and the stone offers the chat');
  await admin.do('login', 'admin', ADMIN);
  await admin.do('offerChat');
  await admin.do('createUser', 'flow-alice', PASS);
  await admin.do('createUser', 'flow-bob', PASS);
  same('alice has no keys yet', (await alice.do('login', 'flow-alice', PASS)).keys, 'none');

  step('several administrators: only the first makes, unmakes or deletes them');
  same('the one who came first is the first administrator', await admin.do('userRole', 'admin'), 'ADMIN, first');
  await admin.do('createUser', 'flow-second-admin', PASS, 'ADMIN');
  same('a second administrator', await admin.do('userRole', 'flow-second-admin'), 'ADMIN');
  const second = new User('second admin');
  users.push(second);
  await second.do('login', 'flow-second-admin', PASS);
  await second.do('createUser', 'flow-made-by-second', PASS);
  same('the second makes users', await second.do('userRole', 'flow-made-by-second'), 'STANDARD');
  await second.refused('the second makes no administrator', 'createUser', 'flow-no-admin', PASS, 'ADMIN');
  await second.refused('the second changes no role', 'setRole', 'flow-made-by-second', 'ADMIN');
  await second.refused('the second takes no role from the first', 'setRole', 'admin', 'STANDARD');
  await admin.do('setRole', 'flow-made-by-second', 'ADMIN');
  same('the first makes an administrator', await alice.do('userRole', 'flow-made-by-second'), 'ADMIN');
  await second.refused('the second deletes no administrator', 'deleteUser', 'flow-made-by-second');
  await admin.do('setRole', 'flow-made-by-second', 'STANDARD');
  same('and makes a user of them again', await alice.do('userRole', 'flow-made-by-second'), 'STANDARD');
  await second.do('deleteUser', 'flow-made-by-second');
  same('the second deletes users', await admin.do('userRole', 'flow-made-by-second'), null);
  await admin.do('deleteUser', 'flow-second-admin');
  same('the first deletes an administrator', await admin.do('userRole', 'flow-second-admin'), null);
  await bob.do('login', 'flow-bob', PASS);
  const bobId: string = await alice.do('userId', 'flow-bob');

  step('a room: the stone encrypts, and the chat is a feature');
  const room = (await alice.do('createRoom', 'Flow room', false)).circleId as string;
  same('a room', (await alice.do('roomKind', room)).kind, 'room');
  same('the chat is on in a new room', await alice.do('features', room), ['chat:ON']);
  same('nothing unread', await alice.do('join', room), 0);
  const hello = await alice.do('send', room, 'hello room');
  same('the author', hello.by, 'flow-alice');
  same('alice hears her own message', (await alice.do('waitMessage', 'hello room')).id, hello.id);
  same('added to the room at once', (await alice.do('invite', room, 'flow-bob', 'WRITE')).fragment, null);
  same('one unread for bob', await bob.do('join', room), 1);
  same('bob reads the history', (await bob.do('history', room)).map((m: any) => m.text), ['hello room']);
  const hi = await bob.do('send', room, 'hi alice', hello.id);
  same('alice gets it', (await alice.do('waitMessage', 'hi alice')).by, 'flow-bob');
  yes('a room is readable on the stone', (await alice.do('stored', room)).some((content: string) => content.includes('hello room')));

  step('reactions, markers, typing, editing and deleting, as the chat always did');
  await alice.do('react', room, hi.id, '👍');
  same('bob sees the reaction', (await bob.do('waitEdited', hi.id, '👍x1')).reactions, '👍x1');
  await bob.do('react', room, hi.id, '👍');
  same('two gave it', (await alice.do('waitEdited', hi.id, '👍x2')).reactions, '👍x2');
  await alice.do('react', room, hi.id, '👍');
  same('alice takes hers back', (await bob.do('waitEdited', hi.id, '"reactions":"👍x1"')).reactions, '👍x1');
  // One after the other, without waiting for what the stone announces of the first.
  await bob.do('react', room, hi.id, '❤️');
  await bob.do('react', room, hi.id, '🎉');
  const three = ['👍x1', '❤️x1', '🎉x1'].sort().join(' ');
  same('two reactions in a row are both kept', (await alice.do('waitEdited', hi.id, three)).reactions, three);
  await bob.do('markRead', room, hello.id);
  yes('alice sees how far bob read', (await alice.do('waitReceipt', 'flow-bob', 1)).readPosition >= 1);
  await bob.do('typing', room);
  yes('alice sees bob typing', await alice.do('waitTyping', 'flow-bob'));
  same('bob edits his message', (await bob.do('edit', room, hi.id, 'hi alice!')).text, 'hi alice!');
  yes('alice sees the edit', (await alice.do('waitEdited', hi.id, 'hi alice!')).edited);
  await alice.refused('editing the message of another', 'edit', room, hi.id, 'not mine');
  await alice.refused('deleting the message of another', 'remove', room, hi.id);
  await bob.do('remove', room, hi.id);
  yes('alice sees it go', await alice.do('waitDeleted', hi.id));
  same('one message is left', (await alice.do('history', room)).map((m: any) => m.text), ['hello room']);

  step('a chamber: Rune encrypts, and the stone holds what it cannot read');
  const made = await alice.do('createRoom', 'Flow chamber', true);
  const chamber = made.circleId as string;
  yes('alice got keys, and a recovery key to write down', /^([A-Z2-7]{4}-){12}[A-Z2-7]{4}$/.test(made.recoveryKey));
  same('alice has keys now', await alice.do('keyState'), 'ready');
  same('a chamber with its first key', await alice.do('roomKind', chamber), { kind: 'ready', versions: [1], current: 1 });
  await alice.do('join', chamber);
  const secretWords = await alice.do('send', chamber, 'secret words');
  await alice.do('waitMessage', 'secret words');
  const stored: string[] = await alice.do('stored', chamber);
  same('one message on the stone', stored.length, 1);
  yes('it starts as content of the convention', stored[0].startsWith('EDSE'));
  yes('the stone cannot read it', !stored[0].includes('secret words') && !stored[0].includes('text'));
  yes('a second chamber needs no new keys', (await alice.do('createRoom', 'Flow chamber two', true)).recoveryKey === null);

  step('an invitation by link into the chamber: nobody has to be online');
  const invited = await alice.do('invite', chamber, 'flow-carol', 'WRITE');
  yes('a link with the invite secret', typeof invited.fragment === 'string' && invited.fragment.startsWith('#invite='));
  same('until she accepts, carol is no member', (await alice.do('members', chamber)), ['flow-alice:ADMIN:current']);
  await alice.do('logout');
  const accepted = await carol.do('accept', invited.fragment, PASS);
  yes('the link carried the invite secret', accepted.hadSecret);
  yes('carol got keys, and a recovery key', /^([A-Z2-7]{4}-){12}[A-Z2-7]{4}$/.test(accepted.recoveryKey));
  same('carol is in the chamber with its key', await carol.do('roomKind', chamber), { kind: 'ready', versions: [1], current: 1 });
  same('one unread for carol', await carol.do('join', chamber), 1);
  same('carol reads what was written before she came', (await carol.do('history', chamber)).map((m: any) => m.text), ['secret words']);
  await carol.do('send', chamber, 'carol here');
  await alice.do('login', 'flow-alice', PASS);
  // alice's worker has no key store, like a browser without IndexedDB: she gives her recovery key again.
  same('alice on a device without her account key', await alice.do('keyState'), 'locked');
  same('the chamber is locked for her there', (await alice.do('roomKind', chamber)).kind, 'locked');
  await alice.refused('a wrong recovery key', 'unlock', 'AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA-AAAA');
  await alice.refused('something which is no recovery key', 'unlock', 'not a key');
  await alice.do('unlock', made.recoveryKey);
  same('with the recovery key she is back', (await alice.do('roomKind', chamber)).kind, 'ready');
  await alice.do('join', chamber);
  same('alice reads carol', (await alice.do('history', chamber)).map((m: any) => m.text), ['secret words', 'carol here']);
  same('carol\'s secret is of her own key now', await alice.do('members', chamber), ['flow-alice:ADMIN:current', 'flow-carol:WRITE:current']);

  step('someone without keys is added, gets keys, and an administrator repairs');
  same('added at once', (await alice.do('invite', chamber, 'flow-bob', 'READ')).fragment, null);
  same('bob has no key to the chamber', (await bob.do('roomKind', chamber)).kind, 'waiting');
  await bob.do('join', chamber);
  yes('bob sees that there are messages, and cannot read them', (await bob.do('history', chamber)).every((m: any) => m.damaged && m.text === undefined));
  const bobKey: string = await bob.do('createKeys');
  same('nobody to repair yet for alice? one: bob', await alice.do('repair', chamber), 1);
  same('repaired once is enough', await alice.do('repair', chamber), 0);
  same('bob has the key', (await bob.do('roomKind', chamber)).kind, 'ready');
  same('bob reads the chamber', (await bob.do('history', chamber)).map((m: any) => m.text), ['secret words', 'carol here']);
  await bob.refused('bob may only read', 'send', chamber, 'not allowed');

  step('a member is removed: the chamber gets a new key, and old content stays readable');
  const carolId: string = await alice.do('userId', 'flow-carol');
  await alice.do('removeMember', chamber, carolId);
  yes('carol is told that she left', await carol.do('waitLeft', chamber));
  same('the second key, and the first one kept', await alice.do('roomKind', chamber), { kind: 'ready', versions: [1, 2], current: 2 });
  yes('the stone says version 2', JSON.parse(await alice.do('sharedValue', chamber)).version === 2);
  await alice.do('join', chamber);
  await alice.do('send', chamber, 'after carol left');
  same('bob has both keys', await bob.do('roomKind', chamber), { kind: 'ready', versions: [1, 2], current: 2 });
  await bob.do('join', chamber);
  same('bob reads old and new', (await bob.do('history', chamber)).map((m: any) => m.text), ['secret words', 'carol here', 'after carol left']);
  same('carol is shown nothing of it any more', (await carol.do('history', chamber)).length, 0);
  await carol.refused('and cannot join it again', 'join', chamber);
  same('members and their keys', await alice.do('members', chamber), ['flow-alice:ADMIN:current', 'flow-bob:READ:current']);

  step('files of a chamber: encrypted before the stone gets them, and unchanged for its people');
  const length = 3 * 65536 + 4321;
  const put = await alice.do('putFile', chamber, 'flow.bin', length);
  same('the stone took it', put.status, 200);
  same('what went there is the file, a header and a tag for each chunk', put.sent, length + 54 + 4 * 16);
  const got = await bob.do('getFile', chamber, put.dataId);
  same('the stone holds content of the convention', [got.start, got.stored], ['EDSE', put.sent]);
  same('bob reads the file unchanged', [got.length, got.sum, got.unprotected], [length, put.sum, false]);
  const empty = await alice.do('putFile', chamber, 'empty.bin', 0);
  same('an empty file is a header and a tag', empty.sent, 70);
  same('and comes back empty', (await bob.do('getFile', chamber, empty.dataId)).length, 0);
  await carol.refused('carol, removed, gets nothing of it', 'getFile', chamber, put.dataId);
  const plainFile = await alice.do('putFile', room, 'flow.bin', 1000);
  same('in a room the file goes as it is', plainFile.sent, 1000);
  same('and is read as it is', (await alice.do('getFile', room, plainFile.dataId)).sum, plainFile.sum);

  step('a further device gets the keys with the recovery key');
  same('bob on a new device', (await bob2.do('login', 'flow-bob', PASS)).keys, 'locked');
  same('the chamber is locked there', (await bob2.do('roomKind', chamber)).kind, 'locked');
  await bob2.do('unlock', bobKey.toLowerCase().replace(/-/g, ' '));
  same('and open with the recovery key', (await bob2.do('roomKind', chamber)).kind, 'ready');
  await bob2.do('join', chamber);
  same('the second device reads the chamber', (await bob2.do('history', chamber)).length, 3);
  same('both devices show the same fingerprint', await bob2.do('fingerprint'), await bob.do('fingerprint'));
  // Two devices, one account: neither ended the other's session.
  same('the first device still works', (await bob.do('history', room)).length, 1);

  step('the chat of a room can be closed and switched off');
  await alice.do('switchFeature', room, 'chat', 'READONLY');
  await alice.do('join', room);
  same('closed', await alice.do('chatState', room), 'readonly');
  await alice.refused('nothing new in a closed chat', 'send', room, 'too late');
  same('but it is read', (await alice.do('history', room)).length, 1);
  await alice.do('switchFeature', room, 'chat', 'OFF');
  await alice.do('join', room);
  same('switched off', await alice.do('chatState', room), 'off');
  same('nothing is shown', (await alice.do('history', room)).length, 0);
  await alice.do('switchFeature', room, 'chat', 'ON');
  await alice.do('join', room);
  same('and everything is there again', (await alice.do('history', room)).map((m: any) => m.text), ['hello room']);

  step('the other features, in the chamber: the stone offers them, the room switches them on');
  for (const feature of ['calendar', 'plans', 'location']) {
    await admin.do('offerFeature', feature);
    await alice.do('switchFeature', chamber, feature, 'ON');
  }
  same('the features of the chamber', await alice.do('features', chamber), ['calendar:ON', 'chat:ON', 'location:ON', 'plans:ON']);
  const bobsId = await admin.do('userId', 'flow-bob');
  const alicesId = await admin.do('userId', 'flow-alice');
  await alice.do('setAccess', chamber, bobsId, 'WRITE');

  step('a calendar shared by two');
  const dinner: CalendarEvent = { uid: 'flow-dinner@rune', title: 'Dinner', allDay: false, start: '2026-12-04T19:00', end: '2026-12-04T21:00', zone: 'Europe/Berlin', place: 'Hearth', other: [] };
  const dinnerId = await alice.do('addTo', chamber, 'calendar', 'event', { v: 1, ics: writeEvent(dinner) }, undefined, entryName(dinner));
  const choir: CalendarEvent = { uid: 'flow-choir@rune', title: 'Choir', allDay: false, start: '2026-11-02T18:00', end: '2026-11-02T19:00', zone: 'Europe/Berlin', repeat: { every: 'weekly', interval: 1 }, other: [] };
  await bob.do('addTo', chamber, 'calendar', 'event', { v: 1, ics: writeEvent(choir) }, undefined, entryName(choir));
  const december = async (user: User) => {
    const found = [...await user.do('bodies', chamber, 'calendar', 'event', undefined, monthPrefix(2026, 12)), ...await user.do('bodies', chamber, 'calendar', 'event', undefined, REPEATING)];
    return found.map((entry: any) => parseEvent(entry.body.ics) as CalendarEvent)
      .flatMap((event) => occurrences(event, new Date('2026-12-01T00:00:00Z'), new Date('2026-12-08T00:00:00Z')).map((o) => `${event.title} ${o.start.toISOString().slice(0, 16)}`)).sort();
  };
  same('bob sees the first week of December', await december(bob), ['Choir 2026-12-07T17:00', 'Dinner 2026-12-04T18:00']);
  same('the stone holds names by month and content it cannot read', await alice.do('held', chamber, 'calendar', 'event'),
    [{ name: '2026-12/flow-dinner@rune', start: 'EDSE' }, { name: 'repeat/flow-choir@rune', start: 'EDSE' }]);
  const later = { ...dinner, start: '2026-12-05T19:00', end: '2026-12-05T21:00' };
  await bob.do('changeIn', chamber, 'calendar', 'event', dinnerId, { v: 1, ics: writeEvent(later) }, entryName(later));
  same('bob moved the dinner, and alice sees it', await december(alice), ['Choir 2026-12-07T17:00', 'Dinner 2026-12-05T18:00']);

  step('a plan: costs shared by three, settled and closed');
  const A = `m:${alicesId}`;
  const B = `m:${bobsId}`;
  const D = 'g:Dana';
  const planId = await alice.do('addTo', chamber, 'plans', 'plan', { v: 1, title: 'Weekend', from: '2026-12-04', to: '2026-12-06', currency: 'EUR', people: [A, B, D] });
  const cabin = await bob.do('addTo', chamber, 'plans', 'expense', { v: 1, text: 'Cabin', amount: '90.00', currency: 'EUR', paidBy: B, split: [{ who: A, share: 1 }, { who: B, share: 1 }, { who: D, share: 1 }] }, planId);
  await alice.do('addTo', chamber, 'plans', 'expense', { v: 1, text: 'Food', amount: '30.00', currency: 'EUR', paidBy: A, split: [{ who: A, share: 1 }, { who: B, share: 1 }] }, planId);
  await alice.do('addTo', chamber, 'plans', 'item', { v: 1, text: 'Book the cabin', who: B, done: false }, planId);
  const owed = async (user: User) => {
    const expenses = (await user.do('bodies', chamber, 'plans', 'expense', planId)).map((entry: any) => entry.body);
    const payments = (await user.do('bodies', chamber, 'plans', 'payment', planId)).map((entry: any) => entry.body);
    const of = balances(expenses, payments).get('EUR') ?? new Map<string, number>();
    const names: Record<string, string> = { [A]: 'alice', [B]: 'bob', [D]: 'Dana' };
    return {
      balances: Object.fromEntries([...of].map(([person, units]) => [names[person], amountText(units)]).sort()),
      dues: settle(of).map((due) => `${names[due.from]} pays ${names[due.to]} ${due.amount}`),
    };
  };
  same('who owes whom, as alice works it out', await owed(alice), { balances: { Dana: '-30.00', alice: '-15.00', bob: '45.00' }, dues: ['Dana pays bob 30.00', 'alice pays bob 15.00'] });
  same('bob works out the same', await owed(bob), await owed(alice));
  await alice.refused('an expense is corrected by its author only', 'changeIn', chamber, 'plans', 'expense', cabin, { v: 1, text: 'Cabin', amount: '1.00', currency: 'EUR', paidBy: B, split: [{ who: A, share: 1 }] });
  await bob.do('changeIn', chamber, 'plans', 'expense', cabin, { v: 1, text: 'Cabin', amount: '120.00', currency: 'EUR', paidBy: B, split: [{ who: A, share: 1 }, { who: B, share: 1 }, { who: D, share: 1 }] });
  same('bob corrected his expense, and the shares follow', (await owed(alice)).dues, ['Dana pays bob 40.00', 'alice pays bob 25.00']);
  const paymentId = await alice.do('addTo', chamber, 'plans', 'payment', { v: 1, from: A, to: B, amount: '25.00', currency: 'EUR' }, planId);
  await bob.do('addTo', chamber, 'plans', 'payment', { v: 1, from: D, to: B, amount: '40.00', currency: 'EUR' }, planId);
  same('with the payments nothing is left', await owed(bob), { balances: { Dana: '0.00', alice: '0.00', bob: '0.00' }, dues: [] });
  await bob.do('addTo', chamber, 'plans', 'confirmation', { v: 1 }, paymentId);
  const payment = (await alice.do('bodies', chamber, 'plans', 'payment', planId)).find((entry: any) => entry.id === paymentId);
  const confirmations = (await alice.do('bodies', chamber, 'plans', 'confirmation')).filter((entry: any) => entry.about === paymentId);
  yes('bob, who got the money, confirmed it', confirmations.some((c: any) => confirms(payment, c)));
  yes('the stone cannot read an amount', (await alice.do('held', chamber, 'plans', 'expense')).every((entry: any) => entry.start === 'EDSE'));
  same('alice closes the plan', await alice.do('closeIn', chamber, 'plans', planId, true), true);
  await bob.refused('nothing is added to a closed plan', 'addTo', chamber, 'plans', 'item', { v: 1, text: 'too late', done: false }, planId);
  same('its records stay', (await bob.do('bodies', chamber, 'plans', 'expense', planId)).length, 2);

  step('where people are: a position reported on one device shows on another');
  await bob.do('addTo', chamber, 'location', 'position', positionBody(52.52, 13.405, 12, new Date('2026-11-15T10:00:00Z')));
  await bob.do('addTo', chamber, 'location', 'position', positionBody(53.5511, 9.9937, 20, new Date('2026-11-15T12:00:00Z')));
  const seen = await alice.do('bodies', chamber, 'location', 'position');
  same('alice sees bob, his latest position only', seen.map((entry: any) => [entry.by, entry.body.lat, entry.body.lon]), [['flow-bob', 53.5511, 9.9937]]);
  yes('the stone cannot read where he is', (await alice.do('held', chamber, 'location', 'position')).every((entry: any) => entry.start === 'EDSE'));
  await alice.refused('nobody changes the position of another', 'changeIn', chamber, 'location', 'position', seen[0].id, positionBody(0, 0, 1, new Date()));

  step('a feature switched off is hidden for everyone, and kept');
  await alice.do('switchFeature', chamber, 'calendar', 'OFF');
  await bob.refused('bob gets nothing of the calendar', 'bodies', chamber, 'calendar', 'event');
  await alice.do('switchFeature', chamber, 'calendar', 'ON');
  same('switched on again, everything is there', (await bob.do('bodies', chamber, 'calendar', 'event')).length, 2);

  step('who is online: the stone tells the people of a room, and nobody else');
  same('alice sees herself and bob in the chamber', await alice.do('present', chamber), [alicesId, bobsId].sort());
  yes('bob is online for alice', await alice.do('online', bobsId));
  same('carol shares no room with them, and knows of nobody', [await carol.do('online', alicesId), await carol.do('online', bobsId)], [false, false]);
  await bob.do('showMe', false);
  yes('bob hid on one device and is still shown by the other', await alice.do('online', bobsId));
  await bob2.do('showMe', false);
  yes('hidden on both, his light goes out for alice', await alice.do('waitOnline', bobsId, false));
  same('alice is alone in the chamber, as the stone tells it', await alice.do('present', chamber), [alicesId]);
  yes('bob still sees alice', await bob.do('online', alicesId));
  await bob.do('showMe', true);
  yes('shown again, it comes on without alice asking', await alice.do('waitOnline', bobsId, true));
  await bob2.do('logout');
  yes('the second device logged out: bob is still online', await alice.do('online', bobsId));
  await alice.do('addMember', room, carolId, 'READ');
  await carol.do('join', room);
  yes('carol was added to the room and joined it: alice sees her come', await alice.do('waitOnline', carolId, true));
  same('and carol sees who is there', await carol.do('present', room), [alicesId, bobsId, carolId].sort());

  step('calls: what sets one up reaches whom it is for, and in a chamber the stone cannot read it');
  await admin.do('offerFeature', 'calls');
  same('no calls where the feature is not switched on', await alice.do('callsUsable', room), false);
  await alice.do('switchFeature', room, 'calls', 'ON');
  await alice.do('switchFeature', chamber, 'calls', 'ON');
  for (const user of [alice, bob, carol]) {
    await user.do('join', room);
  }
  await alice.do('join', chamber);
  await bob.do('join', chamber);
  same('calls in the room and the chamber', [await bob.do('callsUsable', room), await bob.do('callsUsable', chamber)], [true, true]);
  const direct = '0f1e2d3c-4b5a-4978-8695-a4b3c2d1e0f9';
  const device = '11111111-2222-4333-8444-555555555555';
  await alice.do('callSignal', room, { t: 'ring', call: direct, peer: device, media: 'video' }, bobsId);
  same('a direct call rings at bob, and says that it is for him', await bob.do('waitCall', 'ring', direct), { from: 'flow-alice', to: bobsId, start: '{"v"', media: 'video', peer: device });
  await alice.do('callSignal', room, { t: 'join', call: 'room', peer: device, media: 'audio' });
  same('the call of the room is told to everyone in it', [(await bob.do('waitCall', 'join', 'room')).from, (await carol.do('waitCall', 'join', 'room')).to], ['flow-alice', undefined]);
  same('carol, in the same room, heard nothing of the direct call', await carol.do('heardCalls', direct), []);
  yes('bob and carol know that alice is in the call of the room', await bob.do('waitInRoomCall', room, [alicesId]) && await carol.do('waitInRoomCall', room, [alicesId]));
  await alice.do('callSignal', room, { t: 'leave', call: 'room', peer: device });
  yes('and that it is over when she left', await bob.do('waitInRoomCall', room, []));
  await alice.do('callSignal', chamber, { t: 'ring', call: direct, peer: device, media: 'audio' }, bobsId);
  await bob.do('until', 'heardTwice');
  same('in a chamber the stone passes on what it cannot read, and bob reads it', (await bob.do('lastCall', 'ring', direct)), { from: 'flow-alice', to: bobsId, start: 'EDSE', media: 'audio', peer: device });

  step('the relay of a room: its administrators name it, everyone in the room reads it');
  const relay = { v: 1, servers: [{ urls: ['turn:turn.example.org:3478'], username: 'flow', credential: 'not a real one' }] };
  await alice.do('addTo', chamber, 'calls', 'relay', { v: 1, servers: [{ urls: ['stun:old.example.org:3478'] }] });
  await alice.do('addTo', chamber, 'calls', 'relay', relay);
  same('bob reads the relay, the newest one only', (await bob.do('bodies', chamber, 'calls', 'relay')).map((entry: any) => entry.body), [relay]);
  yes('the stone cannot read it, the credential least of all', (await alice.do('held', chamber, 'calls', 'relay')).every((entry: any) => entry.start === 'EDSE'));
  await bob.refused('bob, no administrator, names no relay', 'addTo', chamber, 'calls', 'relay', relay);

  console.log(`flow: ${checks()} checks passed.`);
} catch (error) {
  console.error(`flow: FAILED after ${checks()} checks\n${(error as Error).message}`);
  process.exitCode = 1;
} finally {
  await Promise.all(users.map((user) => user.end()));
}
