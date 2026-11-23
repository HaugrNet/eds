import { formatSize } from './dom';
import { t } from './i18n';
import table from './messages.json';

/**
 * Turns EDS's error messages into Rune's words: EDS says Circle and Data
 * Object, Rune says room and file. The common messages get their own text;
 * anything else has those words replaced, so no EDS term leaks through.
 *
 * The table is messages.json, which Tolk (eds-cli) uses too, so the two say
 * the same; scripts/check-i18n.mjs checks that its texts are translated.
 */
interface Value { group: number; format?: string }
interface Entry { pattern: string; text: string; values?: Record<string, Value> }

const KNOWN: [RegExp, Entry][] = (table.messages as Entry[]).map((entry) => [new RegExp(entry.pattern), entry]);
const WORDS: [RegExp, string][] = (table.words as string[][]).map(([from, to]) => [new RegExp(`\\b${from}\\b`, 'g'), to]);

export function friendly(message: string | undefined, fallback = t('Something went wrong.')): string {
  const text = (message ?? '').trim();
  if (!text) {
    return fallback;
  }
  for (const [pattern, entry] of KNOWN) {
    const match = text.match(pattern);
    if (match) {
      const params: Record<string, string> = {};
      for (const [name, value] of Object.entries(entry.values ?? {})) {
        const raw = match[value.group] ?? '';
        params[name] = value.format === 'size' ? formatSize(Number(raw)) : raw;
      }
      // The text comes from the table, not from a literal: tr() keeps the
      // build check from looking for one here.
      return translate(entry.text, params);
    }
  }
  // Other messages stay in English, in Rune's words.
  return WORDS.reduce((result, [from, to]) => result.replace(from, to), text);
}

const translate = t;
