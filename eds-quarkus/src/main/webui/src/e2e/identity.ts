import { CredentialType } from '../api';
import { friendly } from '../messages';
import { api, session } from '../session';
import { t } from '../i18n';
import { accountKey, keepAccountKey } from './device';
import {
  E2EError, fingerprint, generateKeyPair, importAccountKey, importPrivateKey, openVault, parseDeviceLink, parsePublicKey, parseRecoveryKey,
  publicKeyText, recoveryKey, sealVault, type KeyPair,
} from './convention';
import { random } from './bytes';

/**
 * The logged-in Member's keys for end-to-end encryption (E2E.md): made in
 * this browser, never sent anywhere readable. The private key lies on the
 * stone as the Member's vault, wrapped under the account key, which this
 * device keeps in its key store. A further device gets the account key
 * once, as the recovery key.
 *
 * - `none`: the Member has no keys yet. They are made when a chamber is
 *   created or joined.
 * - `locked`: the Member has keys, but this device not the account key.
 *   The Member types the recovery key.
 * - `ready`: this device has the Member's keys.
 */
export type KeyState = 'none' | 'locked' | 'ready';

let pair: KeyPair | null = null;
let state: KeyState = 'none';
let loadedFor: string | undefined;
let vault: string | undefined;
let publicPoint: Uint8Array | null = null;

/** Fired on the document when the state of the Member's keys changed. */
export const KEYS_CHANGED = 'e2e-keys-changed';

function changed(next: KeyState): KeyState {
  if (state !== next) {
    state = next;
    document.dispatchEvent(new CustomEvent(KEYS_CHANGED, { detail: next }));
  }
  return state;
}

export function keyState(): KeyState {
  return state;
}

/** The Member's key pair, if this device has it. */
export function keyPair(): KeyPair | null {
  return pair;
}

/** Forgets what was loaded, when the Member logs out; the device keeps the account key. */
export function forgetKeys(): void {
  pair = null;
  vault = undefined;
  publicPoint = null;
  loadedFor = undefined;
  state = 'none';
}

/** Loads the Member's keys after a login, as far as this device can. */
export async function loadKeys(): Promise<KeyState> {
  const memberId = session()?.memberId;
  if (!memberId) {
    forgetKeys();
    return state;
  }
  if (loadedFor === memberId && state === 'ready') {
    return state;
  }
  loadedFor = memberId;
  pair = null;
  const own = (await api.members.getMemberById({ memberId })).members?.[0];
  publicPoint = parsePublicKey(own?.publicKey);
  vault = own?.vault ?? undefined;
  if (!publicPoint || !vault) {
    return changed('none');
  }
  const key = await accountKey(memberId);
  if (!key) {
    return changed('locked');
  }
  try {
    const scalar = await openVault(vault, key, memberId);
    pair = { privateKey: await importPrivateKey(scalar, publicPoint), publicPoint };
    scalar.fill(0);
    return changed('ready');
  } catch {
    // The vault was made with another account key: the Member has new keys
    // since this device last saw them.
    return changed('locked');
  }
}

/**
 * Makes the Member's keys: a key pair and an account key. The public key
 * and the vault go to the stone, the account key into this device's key
 * store. Returns the recovery key, to be shown once and written down.
 */
export async function createKeys(): Promise<string> {
  const memberId = session()?.memberId;
  if (!memberId) {
    throw new Error(t('Please sign in again.'));
  }
  const made = await generateKeyPair();
  const account = random(32);
  const sealed = await sealVault(account, memberId, made.scalar);
  made.scalar.fill(0);
  const response = await api.members.updateMember({ processMemberRequest: {
    credentialType: CredentialType.Session,
    credential: session()?.token,
    publicKey: publicKeyText(made.pair.publicPoint),
    vault: sealed,
  } });
  if (response.returnCode !== 200) {
    throw new Error(friendly(response.returnMessage));
  }
  const shown = recoveryKey(account);
  await keepAccountKey(memberId, await importAccountKey(account));
  account.fill(0);
  pair = made.pair;
  publicPoint = made.pair.publicPoint;
  vault = sealed;
  loadedFor = memberId;
  changed('ready');
  return shown;
}

/**
 * Gives this device the Member's keys, with the recovery key as typed, or
 * the link another device showed.
 */
export async function unlockKeys(typed: string): Promise<void> {
  const memberId = session()?.memberId;
  if (!memberId || !vault || !publicPoint) {
    throw new Error(t('There are no keys to unlock.'));
  }
  const account = parseDeviceLink(typed.trim().replace(/^.*(?=#device=)/, '')) ?? parseRecoveryKey(typed);
  if (!account) {
    throw new Error(t('This is not a recovery key: it has 52 letters and digits.'));
  }
  let scalar: Uint8Array;
  try {
    scalar = await openVault(vault, account, memberId);
  } catch (error) {
    throw new Error(error instanceof E2EError ? t('This recovery key does not open your keys.') : String(error));
  }
  pair = { privateKey: await importPrivateKey(scalar, publicPoint), publicPoint };
  scalar.fill(0);
  await keepAccountKey(memberId, await importAccountKey(account));
  account.fill(0);
  changed('ready');
}

/** The fingerprint of the Member's own public key, to compare with what others see. */
export async function ownFingerprint(): Promise<string | null> {
  return publicPoint ? fingerprint(publicPoint) : null;
}

/** The fingerprint of another Member's public key, or null if they have no keys of this convention. */
export async function fingerprintOf(publicKey: string | null | undefined): Promise<string | null> {
  const point = parsePublicKey(publicKey);
  return point ? fingerprint(point) : null;
}
