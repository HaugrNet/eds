import { appearanceChoice, paletteChoice } from './settings';

/** The colour variable the browser's bar shows instead of the theme's background, if any. */
let pinned: string | null = null;

/**
 * Pins the browser's bar to a colour variable, or with null lets it follow
 * the theme again. The login and the page for an EDS that doesn't answer are
 * dark in every theme, so their bar is `--rune-bg`.
 */
export function pinThemeColor(variable: string | null): void {
  pinned = variable;
  applyTheme();
}

/**
 * Applies the chosen theme: light, dark or as the system (`data-theme`),
 * and the palette (`data-palette`, none for Rune's own). The colours are
 * the tokens of rune-theme.css and themes.css. The browser's bar takes the
 * background colour.
 */
export function applyTheme(): void {
  const root = document.documentElement;
  const appearance = appearanceChoice();
  const palette = paletteChoice();
  if (appearance === 'auto') {
    root.removeAttribute('data-theme');
  } else {
    root.dataset.theme = appearance;
  }
  if (palette === 'rune') {
    root.removeAttribute('data-palette');
  } else {
    root.dataset.palette = palette;
  }
  const background = getComputedStyle(root).getPropertyValue(pinned ?? '--color-bg').trim();
  document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]').forEach((meta) => {
    meta.content = background;
  });
}

// With "as the system", the bar follows when the system changes.
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', applyTheme);
