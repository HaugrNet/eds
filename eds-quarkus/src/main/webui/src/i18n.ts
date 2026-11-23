import de from './i18n/de.json';
import { clockChoice, languageChoice } from './settings';

/**
 * Rune's languages. The texts are written in English in the code, wrapped
 * in t(); a catalog (i18n/*.json, which Tolk
 * embeds too) maps them to another language (gettext style). Values
 * are put in with `{name}`. `npm run build` checks that every text has a
 * translation in each catalog (scripts/check-i18n.mjs).
 */
export type Language = 'en' | 'de';

/** Each language in its own name, for the settings. */
export const LANGUAGES: Record<Language, string> = { en: 'English', de: 'Deutsch' };

const CATALOGS: Record<Language, Record<string, string>> = { en: {}, de };

function browserLanguages(): readonly string[] {
  return navigator.languages?.length ? navigator.languages : [navigator.language || 'en'];
}

function primary(tag: string): string {
  return tag.toLowerCase().split('-')[0];
}

/** The browser's first language that Rune has, or English. */
export function browserLanguage(): Language {
  for (const tag of browserLanguages()) {
    const code = primary(tag);
    if (code in CATALOGS) {
      return code as Language;
    }
  }
  return 'en';
}

const current: Language = languageChoice() === 'auto' ? browserLanguage() : languageChoice() as Language;

export function language(): Language {
  return current;
}

/**
 * The locale for dates and numbers: the browser's own variant of the
 * language (en-GB, de-AT, …), or the language itself.
 */
export const locale: string = browserLanguages().find((tag) => primary(tag) === current) ?? current;

/** The clock: 24 hours, or as chosen 12 hours or what the locale uses. */
export function hourCycle(): 'h23' | 'h12' | undefined {
  const choice = clockChoice();
  return choice === '24' ? 'h23' : choice === '12' ? 'h12' : undefined;
}

/**
 * Hours and minutes in a clock: 14:05 with 24 hours, 2:05 PM with 12 (no
 * leading zero, as usual there).
 */
export function clockOptions(cycle = hourCycle()): Intl.DateTimeFormatOptions {
  const resolved = new Intl.DateTimeFormat(locale, { hour: 'numeric', hourCycle: cycle }).resolvedOptions().hourCycle;
  const twelve = resolved === 'h12' || resolved === 'h11';
  return { hour: twelve ? 'numeric' : '2-digit', minute: '2-digit', hourCycle: cycle };
}

type Params = Record<string, string | number>;

/** The text in the chosen language, with the `{name}` values put in. */
export function t(text: string, params?: Params): string {
  const translated = CATALOGS[current][text] ?? text;
  return params ? translated.replace(/\{(\w+)\}/g, (all, name: string) => (name in params ? String(params[name]) : all)) : translated;
}

const plurals = new Intl.PluralRules(locale);

/** One of two texts by the count (`{n}` is the count). */
export function tn(n: number, one: string, other: string, params?: Params): string {
  return t(plurals.select(n) === 'one' ? one : other, { n: formatNumber(n), ...params });
}

export function formatNumber(n: number, digits = 0): string {
  return new Intl.NumberFormat(locale, { maximumFractionDigits: digits, minimumFractionDigits: digits }).format(n);
}

document.documentElement.lang = current;
