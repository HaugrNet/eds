// One user of Rune in the flow test (scripts/flow/flow.ts): a worker which
// runs Rune's own modules, the ones without a page, and does what the main
// thread asks. The modules are singletons, as in a browser tab, so every
// user is a worker of its own.
import './shims.ts';
import { parentPort } from 'node:worker_threads';
import { CredentialType, FeatureState, MemberRole, TrustLevel } from '../../src/api';
import { api, BASE_PATH, login, session, sessionEnded } from '../../src/session';
import { protectBlob, revealBlob } from '../../src/e2e/files';
import {
  acceptInvitation, addMember, createRoom, createUser, deleteUser, inviteToRoom, listMembers, listUsers, myRooms, removeMember, repairRoom, setAccess, setRole,
} from '../../src/account';
import { chat, offerChat, type ChatMessage, type ChatReceipt } from '../../src/chat';
import { invitationFragment, parseInvitationFragment } from '../../src/e2e/convention';
import { createKeys, fingerprintOf, forgetKeys, keyState, loadKeys, ownFingerprint, unlockKeys } from '../../src/e2e/identity';
import { forgetRooms, memberKeyState, roomState } from '../../src/e2e/rooms';
import { fromB64u } from '../../src/e2e/bytes';
import { addEntry, allEntries, changeEntry, offeredFeatures, offerFeature, roomFeatures, switchFeature } from '../../src/features/features';
import { socket } from '../../src/socket';
import type { SocketFrame } from '../../src/api';
import { calls, decodeSignal, encodeSignal } from '../../src/calls';
import type { CallMessage } from '../../src/features/calls';
import { isOnline, presentIn, setShowMe } from '../../src/presence';

const messages: ChatMessage[] = [];
const edited: ChatMessage[] = [];
const deleted: string[] = [];
const receipts = new Map<string, ChatReceipt>();
const typing: string[] = [];
const left: string[] = [];
chat.addEventListener('message', (event) => messages.push((event as CustomEvent<ChatMessage>).detail));
chat.addEventListener('edited', (event) => edited.push((event as CustomEvent<ChatMessage>).detail));
chat.addEventListener('deleted', (event) => deleted.push((event as CustomEvent<{ messageId: string }>).detail.messageId));
chat.addEventListener('receipt', (event) => {
  const { receipt } = (event as CustomEvent<{ receipt: ChatReceipt }>).detail;
  receipts.set(receipt.accountName ?? '', receipt);
});
chat.addEventListener('typing', (event) => typing.push((event as CustomEvent<{ accountName: string }>).detail.accountName));
chat.addEventListener('left', (event) => left.push((event as CustomEvent<{ circleId: string }>).detail.circleId));

// What reaches this user of the calls of others: who sent it, for whom, the message, and how the signal starts as the stone passes it on.
const heard: { from: string; to?: string; circleId: string; message: CallMessage; start: string }[] = [];
socket.addEventListener('signal', (event) => {
  const frame = (event as CustomEvent<SocketFrame>).detail;
  if (frame.feature === 'calls' && frame.circleId && frame.signal) {
    const circleId = frame.circleId;
    const start = new TextDecoder('latin1').decode(fromB64u(frame.signal)).slice(0, 4);
    void decodeSignal(circleId, frame.signal).then((message) => {
      if (message) {
        heard.push({ from: frame.accountName ?? '', to: frame.to ?? undefined, circleId, message, start });
      }
    }).catch(() => undefined);
  }
});
// The calls follow the socket, as in the app; a worker has no microphone, so it enters none.
calls.start();

async function until<T>(what: string, look: () => T | undefined | false, millis = 10000): Promise<T> {
  const end = Date.now() + millis;
  for (;;) {
    const found = look();
    if (found) {
      return found;
    }
    if (Date.now() > end) {
      throw new Error(`waited in vain for ${what}`);
    }
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
}

function shown(message: ChatMessage): { text?: string; by?: string; damaged: boolean; reactions: string; id?: string; edited: boolean } {
  return {
    id: message.messageId,
    text: message.text,
    by: message.accountName,
    damaged: message.damaged === true,
    edited: !!message.edited,
    reactions: Object.entries(message.reactions ?? {}).map(([reaction, by]) => `${reaction}x${by.length}`).sort().join(' '),
  };
}

async function sum(bytes: Uint8Array): Promise<string> {
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes as Uint8Array<ArrayBuffer>))].map((b) => b.toString(16).padStart(2, '0')).join('');
}

const ops: Record<string, (...args: any[]) => Promise<unknown>> = {
  login: async (name: string, passphrase: string) => {
    await login(name, passphrase);
    await chat.connect();
    return { memberId: session()?.memberId, keys: await loadKeys() };
  },
  logout: async () => {
    // As the app does when it goes: the socket closes, and what was opened
    // of keys and rooms is forgotten. (Rune's own logout also clears the
    // downloads of a page, which a worker hasn't.)
    chat.close();
    forgetRooms();
    forgetKeys();
    const token = session()?.token;
    if (token) {
      await api.members.logout({ processMemberRequest: { credentialType: CredentialType.Session, credential: token } });
    }
    sessionEnded();
  },
  offerChat: () => offerChat(),
  // Who is online, as the lights of the app show it.
  online: async (memberId: string) => isOnline(memberId),
  present: async (circleId: string) => presentIn(circleId),
  waitOnline: (memberId: string, online: boolean) => until(`${memberId} ${online ? 'online' : 'gone'}`, () => isOnline(memberId) === online),
  showMe: async (shown: boolean) => {
    setShowMe(shown);
    await socket.rejoin();
  },
  // A message of a call, as the app sends it; and what came of the calls of others.
  callSignal: async (circleId: string, message: Omit<CallMessage, 'v'>, to?: string) => socket.signal(circleId, 'calls', await encodeSignal(circleId, message), to),
  waitCall: async (type: string, call: string) => {
    const found = await until(`a ${type} of the call ${call}`, () => heard.find((one) => one.message.t === type && one.message.call === call));
    return { from: found.from, to: found.to, start: found.start, media: found.message.media, peer: found.message.peer };
  },
  // The newest of a type, once it was heard a second time: the same call rang in a room and then in a chamber.
  until: async (_what: string) => until('a second ring', () => heard.filter((one) => one.message.t === 'ring').length >= 2),
  lastCall: async (type: string, call: string) => {
    const found = heard.filter((one) => one.message.t === type && one.message.call === call).pop();
    return found ? { from: found.from, to: found.to, start: found.start, media: found.message.media, peer: found.message.peer } : null;
  },
  heardCalls: async (call: string) => heard.filter((one) => one.message.call === call).map((one) => one.message.t),
  canCall: async (memberId: string) => calls.canCall(memberId),
  callsUsable: async (circleId: string) => calls.usable(circleId),
  waitInRoomCall: (circleId: string, members: string[]) =>
    until(`the call of ${circleId} with ${members.join(', ') || 'nobody'}`, () => JSON.stringify(calls.inRoomCall(circleId).sort()) === JSON.stringify([...members].sort())),
  offerFeature: async (name: 'chat' | 'calendar' | 'location' | 'plans' | 'calls') => {
    await offerFeature(name);
    return [...(await offeredFeatures()).keys()].sort();
  },
  createUser: (name: string, passphrase: string, role?: 'ADMIN' | 'STANDARD') =>
    createUser(name, passphrase, role === 'ADMIN' ? MemberRole.Admin : MemberRole.Standard),
  // The role of a user as the list of users tells it, and whether it is the first administrator.
  userRole: async (name: string) => {
    const user = (await listUsers()).find((one) => one.accountName === name);
    return user ? `${user.memberRole}${user.firstAdministrator ? ', first' : ''}` : null;
  },
  setRole: async (name: string, role: 'ADMIN' | 'STANDARD') => {
    const user = (await listUsers()).find((one) => one.accountName === name);
    await setRole(user?.memberId as string, role === 'ADMIN' ? MemberRole.Admin : MemberRole.Standard);
  },
  deleteUser: async (name: string) => deleteUser((await listUsers()).find((one) => one.accountName === name)?.memberId as string),
  userId: async (name: string) => (await listUsers()).find((user) => user.accountName === name)?.memberId,
  createRoom: (name: string, chamber: boolean, features?: string[]) => createRoom(name, chamber, features),
  // The fingerprints of the members of a room, by their names.
  fingerprints: async (circleId: string) => {
    const prints: Record<string, string | null> = {};
    for (const member of await listMembers(circleId)) {
      prints[member.accountName ?? ''] = await fingerprintOf(member.publicKey);
    }
    return prints;
  },
  rooms: async () => (await myRooms()).map((room) => room.circleName).sort(),
  roomId: async (name: string) => (await myRooms()).find((room) => room.circleName === name)?.circleId,
  roomKind: async (circleId: string) => {
    forgetRooms(circleId);
    const state = await roomState(circleId);
    return { kind: state.kind, versions: [...state.keys.keys()].sort(), current: state.current?.version };
  },
  sharedValue: async (circleId: string) => (await api.circles.getCircleById({ circleId })).circles?.[0]?.circleKey,
  features: async (circleId: string) => (await roomFeatures(circleId)).map((feature) => `${feature.name}:${feature.state}`).sort(),
  switchFeature: (circleId: string, name: string, state: FeatureState) => switchFeature(circleId, name, state),
  setAccess: (circleId: string, memberId: string, level: TrustLevel) => setAccess(circleId, memberId, level),
  // The entries of any feature, as Rune's panes write and read them.
  addTo: async (circleId: string, feature: string, kind: string, body: object, about?: string, name?: string) => {
    await socket.join(circleId);
    return (await addEntry(circleId, feature, kind, body, about, name)).entryId;
  },
  changeIn: async (circleId: string, feature: string, kind: string, entryId: string, body: object, name?: string) =>
    (await changeEntry(circleId, feature, kind, entryId, body, name)).entryId,
  removeFrom: async (circleId: string, feature: string, entryId: string) => (await socket.remove(circleId, feature, entryId)).length,
  closeIn: async (circleId: string, feature: string, entryId: string, closed: boolean) => (await socket.closeEntry(circleId, feature, entryId, closed)).closed,
  bodies: async (circleId: string, feature: string, kind: string, about?: string, name?: string) => {
    await socket.join(circleId);
    return (await allEntries<object>(circleId, feature, kind, about, name)).map((entry) => ({
      id: entry.entryId, by: entry.accountName, memberId: entry.memberId, name: entry.name, about: entry.about, closed: entry.closed,
      added: entry.added, changed: entry.changed, body: entry.body, problem: entry.problem,
    }));
  },
  // What the stone holds of a kind: the names of the entries, and how their contents start.
  held: async (circleId: string, feature: string, kind: string) => {
    const response = await api.features.getEntries({ circleId, feature, kind, oldestFirst: true });
    return (response.entries ?? []).map((entry) => ({ name: entry.name, start: new TextDecoder('latin1').decode(fromB64u(entry.content ?? '')).slice(0, 4) }));
  },
  invite: async (circleId: string, name: string, level: TrustLevel) => {
    const result = await inviteToRoom(circleId, name, level);
    return { message: result.message, fragment: result.token ? invitationFragment({ name, token: result.token, secret: result.secret }) : null };
  },
  accept: async (fragment: string, passphrase: string) => {
    const invitation = parseInvitationFragment(fragment);
    if (!invitation) {
      throw new Error('not an invitation');
    }
    const recoveryKey = await acceptInvitation(invitation, passphrase);
    await chat.connect();
    return { recoveryKey, hadSecret: !!invitation.secret, memberId: session()?.memberId };
  },
  addMember: (circleId: string, memberId: string, level: TrustLevel) => addMember(circleId, memberId, level),
  removeMember: (circleId: string, memberId: string) => removeMember(circleId, memberId),
  members: async (circleId: string) => {
    const state = await roomState(circleId);
    return (await listMembers(circleId)).map((member) => `${member.accountName}:${member.trustLevel}:${memberKeyState(member, state.current?.version ?? 0)}`).sort();
  },
  repair: async (circleId: string) => repairRoom(circleId, await listMembers(circleId)),
  keyState: async () => keyState(),
  createKeys: () => createKeys(),
  unlock: (recoveryKey: string) => unlockKeys(recoveryKey),
  fingerprint: () => ownFingerprint(),
  join: (circleId: string) => chat.join(circleId),
  chatState: async (circleId: string) => chat.state(circleId),
  send: async (circleId: string, text: string, replyTo?: string) => shown(await chat.send(circleId, text, undefined, replyTo)),
  history: async (circleId: string) => (await chat.history(circleId)).map(shown),
  waitMessage: async (text: string) => shown(await until(`the message "${text}"`, () => messages.find((message) => message.text === text))),
  waitEdited: async (messageId: string, test: string) =>
    shown(await until(`a change of ${messageId}: ${test}`, () => edited.find((message) => message.messageId === messageId && JSON.stringify(shown(message)).includes(test)))),
  waitDeleted: (messageId: string) => until(`the deletion of ${messageId}`, () => deleted.includes(messageId)),
  waitReceipt: async (name: string, read: number) =>
    until(`a marker of ${name} at ${read}`, () => ((receipts.get(name)?.readPosition ?? 0) >= read ? receipts.get(name) : undefined)),
  waitTyping: (name: string) => until(`${name} typing`, () => typing.includes(name)),
  waitLeft: (circleId: string) => until(`leaving ${circleId}`, () => left.includes(circleId)),
  react: (circleId: string, messageId: string, reaction: string) => chat.react(circleId, messageId, reaction),
  edit: async (circleId: string, messageId: string, text: string) => shown(await chat.editMessage(circleId, messageId, text)),
  remove: (circleId: string, messageId: string) => chat.deleteMessage(circleId, messageId),
  markRead: (circleId: string, messageId: string) => chat.markRead(circleId, messageId),
  typing: async (circleId: string) => chat.typing(circleId),
  // What the stone holds of the chat of a room, as it hands it out: the start of each content, as text.
  // A file of the given length goes into a room as Rune's upload sends it: protected first, in a chamber.
  putFile: async (circleId: string, name: string, length: number) => {
    const plain = new Uint8Array(length);
    for (let i = 0; i < length; i++) {
      plain[i] = (i * 31 + length) & 0xff;
    }
    const registered = await api.data.postData({ processDataRequest: { circleId, dataName: name, mediaType: 'application/octet-stream' } });
    const body = await protectBlob(circleId, '', new Blob([plain]));
    const response = await fetch(`${BASE_PATH}/data/${registered.dataId}/content`, {
      method: 'PUT', headers: { Authorization: `Bearer ${session()?.token ?? ''}`, 'Content-Type': 'application/octet-stream' }, body: body.blob,
    });
    await body.release();
    return { dataId: registered.dataId, status: response.status, sum: await sum(plain), sent: body.blob.size };
  },
  // And comes out as Rune's download and preview read it.
  getFile: async (circleId: string, dataId: string) => {
    const response = await fetch(`${BASE_PATH}/data/${dataId}/content`, { headers: { Authorization: `Bearer ${session()?.token ?? ''}` } });
    if (!response.ok) {
      throw new Error(`the stone answered ${response.status}`);
    }
    const stored = await response.blob();
    const start = new TextDecoder('latin1').decode(await stored.slice(0, 4).arrayBuffer());
    const passed = await revealBlob(circleId, '', stored);
    const plain = new Uint8Array(await passed.blob.arrayBuffer());
    return { start, stored: stored.size, length: plain.length, sum: await sum(plain), unprotected: passed.unprotected === true };
  },
  stored: async (circleId: string) => {
    const response = await api.features.getEntries({ circleId, feature: 'chat', kind: 'message', oldestFirst: true });
    return (response.entries ?? []).map((entry) => new TextDecoder('latin1').decode(fromB64u(entry.content ?? '')));
  },
};

parentPort?.on('message', (request: { id: number; op: string; args: unknown[] }) => {
  const op = ops[request.op];
  void (async () => {
    try {
      if (!op) {
        throw new Error(`unknown operation ${request.op}`);
      }
      parentPort?.postMessage({ id: request.id, result: await op(...request.args) });
    } catch (error) {
      parentPort?.postMessage({ id: request.id, error: error instanceof Error ? error.message : String(error) });
    }
  })();
});
