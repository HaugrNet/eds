import { CredentialType } from './api';
import { friendly } from './messages';
import { api, session } from './session';
import { t } from './i18n';

/**
 * The server: its version (for everyone) and its settings, which only the
 * System Administrator may read and change. Rune knows the standard
 * settings of EDS (StandardSetting), to name and group them; others show as
 * they are.
 */
export type SettingKind = 'number' | 'switch' | 'text' | 'choice' | 'fixed';

export interface SettingInfo {
  key: string;
  label: string;
  help: string;
  kind: SettingKind;
  /** What a number counts, after the field. */
  unit?: string;
  /** A number of bytes: its size is shown beside it. */
  bytes?: boolean;
  /** Asks before it is changed. */
  careful?: boolean;
}

export interface SettingGroup {
  title: string;
  note?: string;
  settings: SettingInfo[];
}

export const SYMMETRIC_ALGORITHM = 'eds.crypto.symmetric.algorithm';
export const ASYMMETRIC_ALGORITHM = 'eds.crypto.asymmetric.algorithm';
export const IS_READY = 'eds.is.ready';

/** Settings EDS keeps for itself; the server pane doesn't show them. */
export const INTERNAL_SETTINGS = [IS_READY];

export interface ServerSettings {
  settings: Record<string, string>;
  /** The values EDS accepts for the settings with a fixed choice: the supported algorithms. */
  allowedValues: Record<string, string[]>;
}

/** The standard settings of EDS, in the groups of the server pane. */
export function settingGroups(): SettingGroup[] {
  return [
    { title: t('Sessions and invitations'), settings: [
      { key: 'eds.session.timeout.minutes', label: t('Session length'), help: t('How long a login lasts.'), kind: 'number', unit: t('minutes') },
      { key: 'eds.invitation.timeout.hours', label: t('Invitation validity'), help: t('How long an invitation can be accepted.'), kind: 'number', unit: t('hours') },
      { key: 'eds.download.token.lifetime.minutes', label: t('Download link lifetime'), help: t('How long a link for pictures, videos and downloads works; at most the rest of the session.'), kind: 'number', unit: t('minutes') },
    ] },
    { title: t('The stone'), settings: [
      { key: 'eds.stone.name', label: t('Name of the stone'), help: t('How this stone calls itself in its status, for those who look after several.'), kind: 'text' },
      { key: 'eds.session.max.per.member', label: t('Sessions per account'), help: t('On how many devices an account can be logged in at once; a further login ends the oldest.'), kind: 'number' },
      { key: 'eds.status.load.interval.seconds', label: t('Load interval'), help: t('How often the stone notes its load for the status.'), kind: 'number', unit: t('seconds') },
      { key: 'eds.status.inventory.interval.minutes', label: t('Inventory interval'), help: t('How often the stone counts what it stores for the status.'), kind: 'number', unit: t('minutes') },
    ] },
    { title: t('Visibility'), settings: [
      { key: 'eds.show.all.circles', label: t('Everyone sees all rooms'), help: t('Off: users see only the rooms they are in.'), kind: 'switch' },
      { key: 'eds.show.trustees', label: t('Users see the rooms of others'), help: t('On: every user can see every room and who is in it, though not what is in it. Off: only the rooms they share with them.'), kind: 'switch', careful: true },
    ] },
    { title: t('Storage and transfers'), settings: [
      { key: 'eds.data.max.total.size', label: t('Storage ceiling'), help: t('Most stored content in total; 0 for the default: 2 GiB on a test system, 250 GiB on a production system.'), kind: 'number', unit: t('bytes'), bytes: true },
      { key: 'eds.stream.max.object.size', label: t('Largest file'), help: t('Largest content of one file.'), kind: 'number', unit: t('bytes'), bytes: true },
      { key: 'eds.data.max.inline.size', label: t('Largest inline content'), help: t('Largest content inside a JSON request; larger content is streamed.'), kind: 'number', unit: t('bytes'), bytes: true },
      { key: 'eds.stream.read.batch.size', label: t('Read batch'), help: t('How much is read from the database at a time while streaming.'), kind: 'number', unit: t('bytes'), bytes: true },
      { key: 'eds.stream.transaction.timeout.seconds', label: t('Transfer timeout'), help: t('How long an upload or download may take.'), kind: 'number', unit: t('seconds') },
    ] },
    { title: t('Integrity checks'), settings: [
      { key: 'eds.sanity.check.startup', label: t('Check at startup'), help: t('Checks the stored data when EDS starts.'), kind: 'switch' },
      { key: 'eds.sanity.check.interval.days', label: t('Check interval'), help: t('How long until a file is checked again.'), kind: 'number', unit: t('days') },
      { key: 'eds.sanity.check.max.objects', label: t('Files per run'), help: t('Most files checked by one hourly run; 0 for no limit.'), kind: 'number' },
    ] },
    { title: t('System'), settings: [
      { key: 'eds.system.locale', label: t('Locale'), help: t('The language EDS uses for names, e.g. EN.'), kind: 'text' },
      { key: 'eds.cors.value', label: t('Allowed origins (CORS)'), help: t('The web addresses other web apps may call EDS from, separated by commas.'), kind: 'text' },
      { key: 'eds.masterkey.url', label: t('MasterKey source'), help: t('Changed with the MasterKey request, not here.'), kind: 'fixed' },
    ] },
    { title: t('Cryptography'), note: t('Used for new keys and new data; what is stored stays as it is. Change these only if you know the consequences.'), settings: [
      { key: SYMMETRIC_ALGORITHM, label: t('Symmetric algorithm'), help: t('Encrypts the data.'), kind: 'choice', careful: true },
      { key: ASYMMETRIC_ALGORITHM, label: t('Asymmetric algorithm'), help: t('The key pairs of users, which protect the room keys.'), kind: 'choice', careful: true },
      { key: 'eds.crypto.pbe.algorithm', label: t('Passphrase algorithm'), help: t('Protects the keys of users with their passphrases.'), kind: 'choice', careful: true },
      { key: 'eds.crypto.hash.algorithm', label: t('Hash algorithm'), help: t('For checksums and fingerprints.'), kind: 'choice', careful: true },
      { key: 'eds.crypto.pbe.iterations', label: t('Passphrase iterations'), help: t('For new passphrases, at least 1000. Each account keeps its own until its passphrase changes.'), kind: 'number' },
    ] },
  ];
}

/** What everyone may know about the running EDS. */
export interface ServerInfo {
  version: string;
  /** The product name of the database, e.g. "H2" or "PostgreSQL". */
  database?: string;
  /** False on a test system: an in-memory database, which loses all data when EDS stops. */
  persistent?: boolean;
}

const PERSISTENT_KEY = 'rune.server.persistent';

/** The version of the running EDS, its database, and whether the data survives a restart. */
export async function serverInfo(): Promise<ServerInfo> {
  const response = await api.version.version();
  const info: ServerInfo = { version: response.version ?? '', database: response.database, persistent: response.persistent };
  // Remembered for the page shown while EDS doesn't answer (unavailable.ts).
  try {
    if (info.persistent === undefined) {
      localStorage.removeItem(PERSISTENT_KEY);
    } else {
      localStorage.setItem(PERSISTENT_KEY, String(info.persistent));
    }
  } catch {
    // Without storage, the page says nothing about the data.
  }
  return info;
}

/**
 * Whether the EDS last reached keeps its data over a restart: false for a
 * test system, undefined if it never said. For when EDS doesn't answer.
 */
export function rememberedPersistent(): boolean | undefined {
  try {
    const value = localStorage.getItem(PERSISTENT_KEY);
    return value === null ? undefined : value === 'true';
  } catch {
    return undefined;
  }
}

/** Reads the settings, after storing the given changes (all or none). Only the System Administrator may. */
export async function serverSettings(changes: Record<string, string> = {}): Promise<ServerSettings> {
  const response = await api.settings.settings({ settingRequest: {
    credentialType: CredentialType.Session,
    credential: session()?.token,
    settings: changes,
  } });
  if (response.returnCode !== 200) {
    throw new Error(friendly(response.returnMessage));
  }
  return { settings: response.settings ?? {}, allowedValues: response.allowedValues ?? {} };
}
