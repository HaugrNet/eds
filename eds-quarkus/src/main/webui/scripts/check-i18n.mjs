// Checks that every text of Rune, written as t('…') or tn(n, '…', '…') or
// named in messages.json, has a translation in each catalog of src/i18n
// (JSON, shared with Tolk), and that the catalogs have no texts the code no
// longer uses. Run by `npm run build`; with
// --missing it prints the missing entries, ready to translate.
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('../src/', import.meta.url).pathname;
const STRING = String.raw`'((?:[^'\\]|\\.)*)'`;
const unescape = (s) => s.replace(/\\(.)/g, (_, c) => (c === 'n' ? '\n' : c));

function sources(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      return entry.name === 'api' || entry.name === 'i18n' ? [] : sources(path);
    }
    return entry.name.endsWith('.ts') ? [path] : [];
  });
}

const used = new Map();
const problems = [];
for (const file of sources(root)) {
  const text = readFileSync(file, 'utf8');
  const relative = file.substring(root.length);
  for (const m of text.matchAll(new RegExp(String.raw`\b(?:t|tr)\(\s*` + STRING, 'g'))) {
    used.set(unescape(m[1]), relative);
  }
  for (const m of text.matchAll(new RegExp(String.raw`\btn\([^,]+,\s*` + STRING + String.raw`\s*,\s*` + STRING, 'g'))) {
    used.set(unescape(m[1]), relative);
    used.set(unescape(m[2]), relative);
  }
  for (const m of text.matchAll(/\b(?:t|tr)\(\s*[`"]/g)) {
    problems.push(`${relative}: t() takes a '…' literal, not a template or "…" string (offset ${m.index}).`);
  }
}

for (const entry of JSON.parse(readFileSync(join(root, 'messages.json'), 'utf8')).messages) {
  used.set(entry.text, 'messages.json');
  new RegExp(entry.pattern);
}

const missingOnly = process.argv.includes('--missing');
for (const catalog of readdirSync(join(root, 'i18n')).filter((f) => f.endsWith('.json'))) {
  const entries = JSON.parse(readFileSync(join(root, 'i18n', catalog), 'utf8'));
  const keys = new Set(Object.keys(entries));
  const missing = [...used.keys()].filter((k) => !keys.has(k));
  const unused = [...keys].filter((k) => !used.has(k));
  if (missingOnly) {
    for (const k of missing) {
      console.log(`  ${JSON.stringify(k)}: "",  (${used.get(k)})`);
    }
    continue;
  }
  for (const k of missing) {
    problems.push(`${catalog}: no translation of '${k}' (${used.get(k)}).`);
  }
  for (const k of unused) {
    problems.push(`${catalog}: '${k}' is no longer used.`);
  }
  // Every {value} of the text must be in the translation too.
  const names = (s) => [...s.matchAll(/\{(\w+)\}/g)].map((x) => x[1]).sort().join(',');
  for (const [key, value] of Object.entries(entries)) {
    if (names(key) !== names(value)) {
      problems.push(`${catalog}: the values of '${key}' differ in the translation.`);
    }
  }
}
if (!missingOnly) {
  if (problems.length > 0) {
    console.error(problems.join('\n'));
    process.exit(1);
  }
  console.log(`i18n: ${used.size} texts, all translated.`);
}
