/**
 * Rune's settings (language, clock, theme, pane layout). They belong to the
 * account and are stored with it in EDS (account-settings.ts); localStorage
 * keeps a copy, for the login page and the start of Rune. Without storage
 * the copy lasts until the page reloads.
 */

export type LanguageChoice = 'auto' | 'en' | 'de';
export type ClockChoice = 'auto' | '24' | '12';

const LANGUAGE_KEY = 'rune.language';
const CLOCK_KEY = 'rune.clock';
const APPEARANCE_KEY = 'rune.appearance';
const PALETTE_KEY = 'rune.palette';
const memory = new Map<string, string>();

/** The settings stored with the account, by their name there. */
export const ACCOUNT_SETTINGS: Record<string, string> = {
  language: LANGUAGE_KEY,
  clock: CLOCK_KEY,
  appearance: APPEARANCE_KEY,
  palette: PALETTE_KEY,
  layout: 'rune.layout',
  present: 'rune.present',
};

let changed: () => void = () => undefined;

/** Registers what runs when a setting of the account was changed here. */
export function onAccountSetting(listener: () => void): void {
  changed = listener;
}

export function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return memory.get(key) ?? null;
  }
}

/** Changes a setting; one of the account is then stored with it. */
export function write(key: string, value: string | null): void {
  writeLocal(key, value);
  if (Object.values(ACCOUNT_SETTINGS).includes(key)) {
    changed();
  }
}

/** Changes the copy in the browser only, e.g. with what the account has. */
export function writeLocal(key: string, value: string | null): void {
  try {
    if (value === null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  } catch {
    if (value === null) {
      memory.delete(key);
    } else {
      memory.set(key, value);
    }
  }
}

export function languageChoice(): LanguageChoice {
  const stored = read(LANGUAGE_KEY);
  return stored === 'en' || stored === 'de' ? stored : 'auto';
}

export function setLanguageChoice(choice: LanguageChoice): void {
  write(LANGUAGE_KEY, choice === 'auto' ? null : choice);
}

/** The clock: 24 hours unless chosen otherwise. */
export function clockChoice(): ClockChoice {
  const stored = read(CLOCK_KEY);
  return stored === 'auto' || stored === '12' ? stored : '24';
}

export function setClockChoice(choice: ClockChoice): void {
  write(CLOCK_KEY, choice === '24' ? null : choice);
}

export type AppearanceChoice = 'auto' | 'light' | 'dark';
export type PaletteChoice = 'rune' | 'fjord' | 'moss' | 'contrast';
export const PALETTES: PaletteChoice[] = ['rune', 'fjord', 'moss', 'contrast'];

export function appearanceChoice(): AppearanceChoice {
  const stored = read(APPEARANCE_KEY);
  return stored === 'light' || stored === 'dark' ? stored : 'auto';
}

export function setAppearanceChoice(choice: AppearanceChoice): void {
  write(APPEARANCE_KEY, choice === 'auto' ? null : choice);
}

export function paletteChoice(): PaletteChoice {
  const stored = read(PALETTE_KEY) as PaletteChoice | null;
  return stored && PALETTES.includes(stored) ? stored : 'rune';
}

export function setPaletteChoice(choice: PaletteChoice): void {
  write(PALETTE_KEY, choice === 'rune' ? null : choice);
}
