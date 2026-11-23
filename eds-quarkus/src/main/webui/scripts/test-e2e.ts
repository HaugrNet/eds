// Checks Rune's implementation of the convention for end-to-end encryption
// (src/e2e/convention.ts) against the vectors of an independent
// implementation (src/e2e/vectors.json, made by vectors/e2e-v1.py), and that
// what it writes it reads again, and refuses what was tampered with.
// Run by `npm run build`: node scripts/test-e2e.ts
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import * as e2e from '../src/e2e/convention.ts';
import { fromB64u, fromHex, text, toB64u, utf8 } from '../src/e2e/bytes.ts';

const vectors = JSON.parse(readFileSync(new URL('../src/e2e/vectors.json', import.meta.url), 'utf8'));
let checks = 0;
const failures: string[] = [];

function same(name: string, actual: unknown, expected: unknown): void {
  checks++;
  if (actual !== expected) {
    failures.push(`${name}\n    expected: ${String(expected).substring(0, 200)}\n    actual:   ${String(actual).substring(0, 200)}`);
  }
}

async function refused(name: string, action: () => Promise<unknown>): Promise<void> {
  checks++;
  try {
    await action();
    failures.push(`${name}: was accepted`);
  } catch (error) {
    if (!(error instanceof e2e.E2EError)) {
      failures.push(`${name}: failed with ${String(error)}, not as a refusal`);
    }
  }
}

const circleId: string = vectors.circleId;
const memberId: string = vectors.memberId;
const scalar = fromB64u(vectors.keys.privateKey);
const point = fromB64u(vectors.keys.publicPoint);
const accountKey = fromB64u(vectors.keys.accountKey);
const pair: e2e.KeyPair = { privateKey: await e2e.importPrivateKey(scalar, point), publicPoint: point };

// --- The Member's keys
same('public key text', e2e.publicKeyText(point), vectors.keys.publicKey);
same('public key parsed', toB64u(e2e.parsePublicKey(vectors.keys.publicKey)!), vectors.keys.publicPoint);
same('a key of another convention', e2e.parsePublicKey('-----BEGIN PUBLIC KEY-----'), null);
same('no key', e2e.parsePublicKey(null), null);
same('fingerprint', await e2e.fingerprint(point), vectors.keys.fingerprint);
same('recovery key', e2e.recoveryKey(accountKey), vectors.keys.recoveryKey);
same('recovery key typed', toB64u(e2e.parseRecoveryKey(vectors.keys.recoveryKey.toLowerCase().replace(/-/g, ' '))!), vectors.keys.accountKey);
same('not a recovery key', e2e.parseRecoveryKey('ABCD-EFGH'), null);
same('not a recovery key at all', e2e.parseRecoveryKey('this is no key!'), null);
same('device link', e2e.deviceLink(accountKey), vectors.keys.deviceLink);
same('device link parsed', toB64u(e2e.parseDeviceLink(vectors.keys.deviceLink)!), vectors.keys.accountKey);
same('not a device link', e2e.parseDeviceLink('#invite=abc'), null);

// --- The vault
same('vault', await e2e.sealVault(accountKey, memberId, scalar, fromB64u(vectors.vault.salt), fromB64u(vectors.vault.nonce)), vectors.vault.vault);
same('vault opened', toB64u(await e2e.openVault(vectors.vault.vault, accountKey, memberId)), vectors.keys.privateKey);
same('vault opened with a key which cannot be exported', toB64u(await e2e.openVault(vectors.vault.vault, await e2e.importAccountKey(accountKey), memberId)), vectors.keys.privateKey);
same('vault of random salt opened', toB64u(await e2e.openVault(await e2e.sealVault(accountKey, memberId, scalar), accountKey, memberId)), vectors.keys.privateKey);
await refused('vault with another account key', () => e2e.openVault(vectors.vault.vault, new Uint8Array(32), memberId));
await refused('vault of another Member', () => e2e.openVault(vectors.vault.vault, accountKey, circleId));
await refused('vault which is not one', () => e2e.openVault('{"settings":true}', accountKey, memberId));

// --- A secret to a public key
const byKey = vectors.secretByKey;
const room: e2e.RoomKey = { key: fromB64u(byKey.roomKey), version: byKey.version };
const epk = fromB64u(JSON.parse(byKey.secret).epk);
same('secret to a key', await e2e.wrapToKey(room, circleId, memberId, point,
  { ephemeral: { scalar: fromB64u(byKey.ephemeralPrivateKey), point: epk }, nonce: fromB64u(byKey.nonce) }), byKey.secret);
const opened = await e2e.openSecretWithKey(byKey.secret, pair, circleId, memberId);
same('secret to a key opened', toB64u(opened.key), byKey.roomKey);
same('secret to a key, version', opened.version, 3);
same('secret kind', e2e.secretKind(byKey.secret), 'key');
same('secret version', e2e.secretVersion(byKey.secret), 3);
const fresh = await e2e.wrapToKey(room, circleId, memberId, point);
same('secret with a random ephemeral key opened', toB64u((await e2e.openSecretWithKey(fresh, pair, circleId, memberId)).key), byKey.roomKey);
same('two wraps differ', fresh === await e2e.wrapToKey(room, circleId, memberId, point), false);
await refused('secret of another Circle', () => e2e.openSecretWithKey(byKey.secret, pair, memberId, memberId));
await refused('secret of another Member', () => e2e.openSecretWithKey(byKey.secret, pair, circleId, circleId));
const other = await e2e.generateKeyPair();
await refused('secret for another key', () => e2e.openSecretWithKey(byKey.secret, other.pair, circleId, memberId));
await refused('secret with a changed version', () => e2e.openSecretWithKey(byKey.secret.replace('"version":3', '"version":4'), pair, circleId, memberId));

// --- A secret of an invitation
const byInvite = vectors.secretByInvite;
const inviteSecret = fromB64u(byInvite.inviteSecret);
same('secret of an invitation', await e2e.wrapForInvite(room, circleId, byInvite.name, inviteSecret, { nonce: fromB64u(byInvite.nonce) }), byInvite.secret);
same('secret of an invitation opened', toB64u((await e2e.openSecretWithInvite(byInvite.secret, inviteSecret, circleId, byInvite.name)).key), byInvite.roomKey);
same('secret kind of an invitation', e2e.secretKind(byInvite.secret), 'invite');
same('no secret kind', e2e.secretKind('plain text'), null);
same('no secret version', e2e.secretVersion(null), 0);
await refused('secret of an invitation for another name', () => e2e.openSecretWithInvite(byInvite.secret, inviteSecret, circleId, 'Someone Else'));
await refused('secret of an invitation with another invite secret', () => e2e.openSecretWithInvite(byInvite.secret, new Uint8Array(32), circleId, byInvite.name));
await refused('a secret to a key opened as an invitation', () => e2e.openSecretWithInvite(byKey.secret, inviteSecret, circleId, byInvite.name));
same('invitation link', e2e.invitationFragment({ name: byInvite.name, token: byInvite.token, secret: inviteSecret }), byInvite.link);
const link = e2e.parseInvitationFragment(byInvite.link)!;
same('invitation link, name', link.name, byInvite.name);
same('invitation link, token', link.token, byInvite.token);
same('invitation link, invite secret', toB64u(link.secret!), byInvite.inviteSecret);
const plain = e2e.parseInvitationFragment(e2e.invitationFragment({ name: 'Room Guest', token: 'abc' }))!;
same('invitation link of a room has no secret', plain.secret, undefined);
same('invitation link of a room, name', plain.name, 'Room Guest');
same('not an invitation link', e2e.parseInvitationFragment('#device=abc'), null);

// --- The shared value of a chamber
const shared = vectors.sharedValue;
const earlier: e2e.RoomKey[] = Object.entries(shared.earlier).map(([v, old]: [string, any]) => ({ key: fromB64u(old.roomKey), version: Number(v) }));
const nonces: Record<number, Uint8Array> = {};
for (const [v, old] of Object.entries(shared.earlier) as [string, any][]) {
  nonces[Number(v)] = fromB64u(old.nonce);
}
same('shared value', await e2e.sharedValue(circleId, room, earlier, nonces), shared.value);
same('shared value of a new chamber', await e2e.sharedValue(circleId, { key: fromB64u(shared.earlier['1'].roomKey), version: 1 }, []), shared.first);
same('a chamber', e2e.isChamber(shared.value), true);
same('a room', e2e.isChamber(null), false);
same('a room with a key of another convention', e2e.isChamber('a shared key'), false);
same('chamber version', e2e.chamberVersion(shared.value), 3);
const keys = await e2e.openSharedValue(shared.value, circleId, room);
same('room keys', [...keys.keys()].sort().join(','), '1,2,3');
same('earlier room key 1', toB64u(keys.get(1)!), shared.earlier['1'].roomKey);
same('earlier room key 2', toB64u(keys.get(2)!), shared.earlier['2'].roomKey);
const behind = await e2e.openSharedValue(shared.value, circleId, { key: fromB64u(shared.earlier['2'].roomKey), version: 2 });
same('a Member whose secret is behind has only their key', [...behind.keys()].join(','), '2');
await refused('shared value with a wrong key', () => e2e.openSharedValue(shared.value, circleId, { key: new Uint8Array(32), version: 3 }));

// --- Content
for (const vector of vectors.content) {
  const key: e2e.RoomKey = { key: fromB64u(vector.roomKey), version: vector.version };
  const options = { chunkSize: vector.chunkSize, salt: fromB64u(vector.salt), noncePrefix: fromHex(vector.noncePrefix) };
  const encrypted = await e2e.encryptContent(key, circleId, vector.context, fromB64u(vector.plain), options);
  same(`content: ${vector.name}`, toB64u(encrypted), vector.encrypted);
  same(`content length: ${vector.name}`, e2e.encryptedLength(fromB64u(vector.plain).length, vector.chunkSize), encrypted.length);
  same(`content read: ${vector.name}`, toB64u(await e2e.decryptContent(keys, circleId, vector.context, fromB64u(vector.encrypted))), vector.plain);
}
const entry = vectors.content.find((v: any) => v.name === 'an entry of a feature');
same('an entry is JSON', JSON.parse(text(await e2e.decryptContent(keys, circleId, entry.context, fromB64u(entry.encrypted)))).text, 'Hello, chamber');
const fileName = vectors.content.find((v: any) => v.name === 'a file name');
same('a file name is its text', text(await e2e.decryptContent(keys, circleId, fileName.context, fromB64u(fileName.encrypted))), 'Quarterly report (Q4).pdf');
await refused('a file name presented as a file', () => e2e.decryptContent(keys, circleId, '', fromB64u(fileName.encrypted)));
const several = fromB64u(vectors.content.find((v: any) => v.name === 'several chunks').encrypted);
await refused('content of another Circle', () => e2e.decryptContent(keys, memberId, '', several));
await refused('content presented as something else', () => e2e.decryptContent(keys, circleId, 'thumbnail', several));
await refused('a thumbnail presented as a file', () => e2e.decryptContent(keys, circleId, '', fromB64u(vectors.content.find((v: any) => v.name === 'a thumbnail').encrypted)));
await refused('content without its last chunk', () => e2e.decryptContent(keys, circleId, '', several.subarray(0, several.length - 21)));
await refused('content cut within a chunk', () => e2e.decryptContent(keys, circleId, '', several.subarray(0, several.length - 3)));
await refused('content with a chunk too many', () => e2e.decryptContent(keys, circleId, '', new Uint8Array([...several, ...several.subarray(54, 86)])));
const swapped = several.slice();
swapped.set(several.subarray(54 + 32, 54 + 64), 54);
swapped.set(several.subarray(54, 54 + 32), 54 + 32);
await refused('content with two chunks swapped', () => e2e.decryptContent(keys, circleId, '', swapped));
const flipped = several.slice();
flipped[60] ^= 1;
await refused('content with a changed byte', () => e2e.decryptContent(keys, circleId, '', flipped));
const header = several.slice();
header[9] = 9;
await refused('content of a key this device does not have', () => e2e.decryptContent(keys, circleId, '', header));
await refused('content which is not protected', () => e2e.decryptContent(keys, circleId, '', utf8('just a text file, written by an app which knows no chambers')));
same('protected', e2e.isProtected(several), true);
same('not protected', e2e.isProtected(utf8('plain')), false);

// --- A file of the usual chunk size, by its hash
const large = vectors.large;
const bytes = new Uint8Array(large.length);
for (let i = 0; i < bytes.length; i++) {
  bytes[i] = (i * 7 + 3) % 256;
}
const largeKey: e2e.RoomKey = { key: fromB64u(large.roomKey), version: large.version };
const largeEncrypted = await e2e.encryptContent(largeKey, circleId, '', bytes, { salt: fromB64u(large.salt), noncePrefix: fromHex(large.noncePrefix) });
same('a file of 64 KiB chunks', createHash('sha256').update(largeEncrypted).digest('hex'), large.sha256);
same('a file of 64 KiB chunks read', createHash('sha256').update(await e2e.decryptContent(keys, circleId, '', largeEncrypted)).digest('hex'),
  createHash('sha256').update(bytes).digest('hex'));

// --- New keys, as a Member makes them
const made = await e2e.generateKeyPair();
same('a new public point', made.pair.publicPoint.length, 97);
same('a new private key', made.scalar.length, 48);
const madeSecret = await e2e.wrapToKey(room, circleId, memberId, made.pair.publicPoint);
same('a new key opens what is wrapped to it', toB64u((await e2e.openSecretWithKey(madeSecret, made.pair, circleId, memberId)).key), byKey.roomKey);
const again = await e2e.importPrivateKey(await e2e.openVault(await e2e.sealVault(accountKey, memberId, made.scalar), accountKey, memberId), made.pair.publicPoint);
same('a key from its vault opens it too', toB64u((await e2e.openSecretWithKey(madeSecret, { privateKey: again, publicPoint: made.pair.publicPoint }, circleId, memberId)).key), byKey.roomKey);

if (failures.length > 0) {
  console.error(`e2e: ${failures.length} of ${checks} checks failed:\n  ` + failures.join('\n  '));
  process.exit(1);
}
console.log(`e2e: ${checks} checks against the vectors passed.`);
