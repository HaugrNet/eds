import { CredentialType } from './api';
import { myAccount } from './account';
import { api, session } from './session';
import { ACCOUNT_SETTINGS, onAccountSetting, read, writeLocal } from './settings';

/**
 * Rune's settings follow the account: language, clock, theme and the
 * arrangement of the panes are stored with the member in EDS (a JSON
 * object, which only the member gets), read at login and written when one
 * changes. The browser keeps a copy, so the login page looks as it was left.
 * Notifications stay with the device, as the browser's permission does.
 */
let timer: number | undefined;
let pending = false;

/** The settings as the JSON object kept in EDS; the layout as an object, the others as texts. */
function collect(): string {
  const settings: Record<string, unknown> = {};
  for (const [name, key] of Object.entries(ACCOUNT_SETTINGS)) {
    const value = read(key);
    if (value !== null) {
      try {
        settings[name] = name === 'layout' ? JSON.parse(value) : value;
      } catch {
        // A broken layout isn't kept.
      }
    }
  }
  return JSON.stringify(settings);
}

async function push(): Promise<void> {
  window.clearTimeout(timer);
  if (!pending || !session()) {
    return;
  }
  pending = false;
  try {
    await api.members.updateMember({ processMemberRequest: {
      credentialType: CredentialType.Session,
      credential: session()?.token,
      settings: collect(),
    } });
  } catch {
    // The copy in the browser still has them; the next change tries again.
  }
}

// A changed setting is stored shortly after, so dragging a divider doesn't write each step.
onAccountSetting(() => {
  pending = true;
  window.clearTimeout(timer);
  timer = window.setTimeout(() => void push(), 600);
});

/** Stores changed settings now, e.g. before Rune reloads. */
export function saveAccountSettings(): Promise<void> {
  return push();
}

/**
 * Takes the settings of the logged-in account into the browser; an account
 * without settings gets Rune's defaults. Resolves true if the
 * language or clock changed, so Rune must reload to use them.
 */
export async function loadAccountSettings(): Promise<boolean> {
  let stored: string | undefined;
  try {
    stored = (await myAccount())?.settings;
  } catch {
    return false;
  }
  // An account without settings starts with Rune's defaults, not with what
  // the last person left in this browser.
  let settings: Record<string, unknown> = {};
  try {
    settings = stored ? JSON.parse(stored) as Record<string, unknown> : {};
  } catch {
    return false;
  }
  let reload = false;
  for (const [name, key] of Object.entries(ACCOUNT_SETTINGS)) {
    const value = settings[name];
    const wanted = value === undefined || value === null ? null : name === 'layout' ? JSON.stringify(value) : String(value);
    if (read(key) !== wanted) {
      writeLocal(key, wanted);
      reload = reload || name === 'language' || name === 'clock';
    }
  }
  return reload;
}
