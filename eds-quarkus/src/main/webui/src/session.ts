import { Configuration, MembersApi, CirclesApi, DataApi, TrusteesApi, SettingsApi, VersionApi, FeaturesApi, StatusApi, ResponseError, CredentialType } from './api';
import { friendly } from './messages';
import { t } from './i18n';
import { BASE_PATH, isGatewayError, suspect } from './backend';

export { BASE_PATH };

const KEY = 'eds.session';

export interface SessionInfo {
  token: string;
  accountName: string;
  memberId?: string;
}

let current: SessionInfo | null = load();

function load(): SessionInfo | null {
  try {
    const stored = sessionStorage.getItem(KEY);
    return stored ? JSON.parse(stored) as SessionInfo : null;
  } catch {
    return null;
  }
}

function store(info: SessionInfo | null): void {
  current = info;
  try {
    if (info) {
      sessionStorage.setItem(KEY, JSON.stringify(info));
    } else {
      sessionStorage.removeItem(KEY);
    }
  } catch {
    // Without storage, the session lasts until the page is reloaded.
  }
}

export function session(): SessionInfo | null {
  return current;
}

const configuration = new Configuration({
  basePath: BASE_PATH,
  accessToken: async () => current?.token ?? '',
  // A request without an answer of EDS: maybe EDS is down (backend.ts).
  middleware: [{
    onError: async () => {
      suspect();
    },
    post: async (context) => {
      if (isGatewayError(context.response.status)) {
        suspect();
      }
    },
  }],
});

export const api = {
  members: new MembersApi(configuration),
  circles: new CirclesApi(configuration),
  data: new DataApi(configuration),
  trustees: new TrusteesApi(configuration),
  settings: new SettingsApi(configuration),
  version: new VersionApi(configuration),
  features: new FeaturesApi(configuration),
  status: new StatusApi(configuration),
};

export function base64(text: string): string {
  const bytes = new TextEncoder().encode(text);
  let binary = '';
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary);
}

/** A failed login, with the returnCode of EDS (0 if EDS wasn't reached). */
export class LoginError extends Error {
  constructor(message: string, readonly returnCode: number) {
    super(message);
  }
}

/** EDS is locked: the MasterKey isn't loaded (CRYPTO_ERROR). */
export const LOCKED = 591;
/** EDS isn't ready: its database is unavailable or invalid (DATABASE_ERROR). */
export const NOT_READY = 594;

export async function login(accountName: string, passphrase: string): Promise<void> {
  let response;
  try {
    response = await api.members.login({
      processMemberRequest: { accountName, credential: base64(passphrase) },
    });
  } catch (e) {
    throw new LoginError(e instanceof Error ? e.message : String(e), 0);
  }
  if (response.returnCode !== 200 || !response.session) {
    throw new LoginError(response.returnMessage ?? t('The login failed.'), response.returnCode ?? 0);
  }
  store({ token: response.session, accountName, memberId: response.memberId });
}

export async function logout(): Promise<void> {
  const token = current?.token;
  const memberId = current?.memberId;
  store(null);
  // Logging out on purpose also removes the member's paused downloads that
  // are kept in the browser (decrypted parts), see downloads.ts.
  if (memberId) {
    const { forgetDownloads } = await import('./downloads');
    await forgetDownloads(memberId);
  }
  if (token) {
    try {
      await api.members.logout({ processMemberRequest: { credentialType: CredentialType.Session, credential: token } });
    } catch {
      // The session is forgotten here anyway.
    }
  }
  window.dispatchEvent(new CustomEvent('eds-logout'));
}

/** Ends the local session when EDS no longer knows it. */
export function sessionEnded(): void {
  store(null);
  window.dispatchEvent(new CustomEvent('eds-logout'));
}

/** The returnMessage of a failed resource call, or a generic text. */
export async function errorMessage(error: unknown): Promise<string> {
  if (error instanceof ResponseError) {
    if (error.response.status === 403) {
      sessionEnded();
    }
    try {
      const body = await error.response.json();
      if (body?.returnMessage) {
        return friendly(body.returnMessage);
      }
    } catch {
      // Not JSON.
    }
    return t('The stone answered {status}.', { status: error.response.status });
  }
  return friendly(error instanceof Error ? error.message : String(error));
}
