import { h } from './dom';
import { t } from './i18n';

/**
 * Rune's avatars: creatures of the sagas for users, places and things for
 * rooms. EDS stores only the ID (e.g. "raven"); the pictures are Rune's own
 * line drawings (24×24, stroke in the text colour, cut-outs in the avatar's
 * background), so they follow the theme. Without an avatar, or with an ID
 * Rune doesn't know, the initial of the name is shown, as before. Runes and
 * symbols used by extremists (valknut, Othala, …) are left out on purpose.
 */
export type AvatarKind = 'user' | 'room';

/** The SVG's inner markup of each picture: constants of this file, never data. */
const USERS: Record<string, string> = {
  raven: `<path d='M2.5 17.5 7 15.6c-.6-3.4 1.1-6.6 4-8.1 1-2 2.8-3.1 4.8-3.1 1.4 0 2.6.6 3.3 1.5l2.4.6-2.2 1.2c.2 2.7-.9 5.1-3 6.8l-1.9 4.6h-1.7l1-3.6c-1.8.6-3.7.6-5.3 0z' fill='currentColor'/><circle cx='16.6' cy='6.6' r='.85' class='cut' stroke='none'/>`,
  wolf: `<path d='M6 3.5l2.6 4.2h6.8L18 3.5l1 6.5-2 3.2-2.4 1.2L12 20l-2.6-5.6L7 13.2 5 10z'/><path d='M9.4 11h.01M14.6 11h.01' stroke-width='2.2'/><path d='M10.8 16.3h2.4'/>`,
  bear: `<circle cx='6.6' cy='6.6' r='2.2'/><circle cx='17.4' cy='6.6' r='2.2'/><path d='M5.5 11.5C5.5 7.9 8.4 6 12 6s6.5 1.9 6.5 5.5c0 4.2-2.9 7.5-6.5 7.5s-6.5-3.3-6.5-7.5z'/><path d='M9.6 11h.01M14.4 11h.01' stroke-width='2.2'/><path d='M10.6 14.3c.4-.6 2.4-.6 2.8 0L12 15.6z' fill='currentColor'/>`,
  eagle: `<path d='M4.5 21c.3-4.2 1.6-7.8 3.6-10.4C9.7 8.4 12 7 14.6 7c1.8 0 3.4.6 4.6 1.6 1.4.2 2.4 1.2 2.6 2.6.2 1.3-.2 2.7-1.1 3.6.2-1.2-.2-2.2-1.1-2.6l-2.8.4c-1.5 1.8-2.4 4.6-2.4 8.4z' fill='currentColor'/><path d='M8 13.8l2.6-1.2M6.8 17l3-1.1' class='cut-line' stroke-width='1.2'/><circle cx='16.3' cy='9.9' r='.85' class='cut' stroke='none'/>`,
  serpent: `<path d='M17.6 5.4A8.2 8.2 0 1 0 20.2 12'/><path d='M20.2 12c0-1.2-.3-2.3-.7-3.3l-2.3.4 1.2-2.4-1.6-1.8 2.6-.2'/><path d='M18.8 9.9h.01' stroke-width='2.2'/><path d='M8 9.5c.8-.9 1.9-1.5 3.1-1.6M7.2 13.5c.2 1.3 1 2.4 2 3.1M13.5 16.3c1-.4 1.9-1.1 2.4-2' />`,
  horse: `<path d='M7 20.5h10.5l-.4-3.2c1.6-2.4 2-5.6.7-8.6-1.3-3-4.2-4.9-7.3-4.6l-.9-1.6-1 2.3C6.1 6 4.6 8.4 4.5 11l2.6 1.1 2.7-1.3c.1 1.8-.8 3.5-2 4.9z'/><path d='M12.7 4.8c2.2 1.6 3.4 4.6 2.6 8.2'/><path d='M8.3 7.9h.01' stroke-width='2.4'/>`,
  boar: `<path d='M3.5 13.5c0-3.6 3.6-6.5 8-6.5 2.2 0 4.2.7 5.6 1.9l1.4-2.4.5 3.4c1.3 1 2.5 1.6 3 2.6v2.6c-.8.6-2 .9-3 .9l-1.5 3.5h-2.2l.3-2.6c-1.7.5-3.6.5-5.3 0L9.6 20H7.4l.4-2.9c-2.6-.8-4.3-2-4.3-3.6z'/><path d='M19.6 14.8c.5.9.4 1.8-.2 2.5'/><path d='M16.8 11h.01' stroke-width='2.2'/>`,
  stag: `<path d='M9 21v-3.2c-1.6-1-2.4-2.5-2.4-4.3V11l2.4-.6h6l2.4.6v2.5c0 1.8-.8 3.3-2.4 4.3V21'/><path d='M9 10.4L7 7.5 4.5 7M7 7.5 6.5 4M7.6 8.6 4 9.5M15 10.4l2-2.9 2.5-.5M17 7.5l.5-3.5M16.4 8.6l3.6.9'/><path d='M10.3 14h.01M13.7 14h.01' stroke-width='2.2'/>`,
  goat: `<path d='M8.5 9C6 8 4.5 5.8 5 3.5c1.4 1.6 3.4 2.5 5.3 2.7h3.4c1.9-.2 3.9-1.1 5.3-2.7.5 2.3-1 4.5-3.5 5.5l-.6 5.5c-.3 2-1.6 3.4-2.9 3.4s-2.6-1.4-2.9-3.4z'/><path d='M10.8 17.9l1.2 3.1 1.2-3.1'/><path d='M10.3 11.6h.01M13.7 11.6h.01' stroke-width='2.2'/>`,
  cat: `<path d='M5 4l3.8 3.4h6.4L19 4l.6 7.5c0 4.3-3.4 8-7.6 8s-7.6-3.7-7.6-8z'/><path d='M9.6 12.2h.01M14.4 12.2h.01' stroke-width='2.2'/><path d='M11.2 15l.8.7.8-.7M4 14.5l4 .5M4.5 17l3.8-1M20 14.5l-4 .5M19.5 17l-3.8-1'/>`,
  squirrel: `<path d='M14 20.5H7.8c-1.5 0-2.3-1-2.3-2.2 0-1.4 1.2-2.3 2.5-2.3h1.5c-.5-2.6.5-5.3 3-6.6l-.6-3 1.9 1.3 1.6-1.6.6 2.6c1.5 1 2.3 2.6 2.1 4.2l-2.6.6'/><path d='M14 20.5c1.8-1.8 2.2-4.6 1-6.8M14 20.5c4.5.3 7-3 6.5-7.2-.4-3.2-2.6-4.7-4.6-4.8'/><path d='M14.8 10.2h.01' stroke-width='2.2'/>`,
  salmon: `<path d='M2.5 12c2.6-3.4 6.4-5 10.5-5 3.6 0 6.2 1.6 7.8 3.3L22 8v8l-1.2-2.3C19.2 15.4 16.6 17 13 17c-4.1 0-7.9-1.6-10.5-5z'/><path d='M11 7.5c-.8 1.4-.8 7.6 0 9M7 11.9h.01' /><path d='M6.4 11.2h.01' stroke-width='2.2'/>`,
};

const ROOMS: Record<string, string> = {
  tree: `<path d='M12 20.5v-8.5M12 14.5l-3-2.4M12 13.4l3.2-2.8'/><path d='M12 20.5c-1.5 0-3.7.3-5 1.2M12 20.5c1.5 0 3.7.3 5 1.2M12 20.5v1.4'/><path d='M12 2.5c3.6 0 6.5 2.4 6.5 5.5 0 3-2.6 5-6.5 5S5.5 11 5.5 8c0-3.1 2.9-5.5 6.5-5.5z'/>`,
  longship: `<path d='M2.5 14.5h19c-1.2 3-4 4.5-7.5 4.5h-4c-3.5 0-6.3-1.5-7.5-4.5zM2.5 14.5 1.8 11.5l1.8 1M21.5 14.5l.7-3-1.8 1'/><path d='M12 14.5V3M12 4.5l6 1.6v5.4L12 13M12 4.5 6 6.1v5.4L12 13'/><circle cx='7' cy='16.4' r='.7'/><circle cx='10.3' cy='16.6' r='.7'/><circle cx='13.7' cy='16.6' r='.7'/><circle cx='17' cy='16.4' r='.7'/>`,
  longhouse: `<path d='M2.5 12.5 12 5l9.5 7.5M4.5 11v8.5h15V11'/><path d='M10 19.5v-5h4v5'/><path d='M10 6.6 8.3 3.5M14 6.6l1.7-3.1'/>`,
  mountain: `<path d='M2 19.5 9 7l3.6 6.2L15 9.5l7 10z'/><path d='M7.2 10.2 9 12l1.6-1.6L12 12'/><circle cx='18' cy='5' r='1.6'/>`,
  fjord: `<path d='M2 15V8.5L5 5l2.5 2.5L9.5 6l1 2.2V15M22 15V9l-3-3.5-2 2-1.5-1.3-1 2.3V15'/><path d='M2 15c1.6-.9 3.2-.9 4.8 0s3.2.9 4.8 0 3.2-.9 4.8 0 3.2.9 4.8 0M2 19c1.6-.9 3.2-.9 4.8 0s3.2.9 4.8 0 3.2-.9 4.8 0 3.2.9 4.8 0'/><path d='M12 9.5l1.4 2.5h-2.8z' fill='currentColor' stroke='none'/>`,
  bridge: `<path d='M2 18.5a10 10 0 0 1 20 0'/><path d='M5 18.5a7 7 0 0 1 14 0'/><path d='M8 18.5a4 4 0 0 1 8 0'/><path d='M1.5 18.5h4M18.5 18.5h4'/>`,
  hearth: `<path d='M12 3c.5 3 4.5 4.8 4.5 9a4.5 4.5 0 0 1-9 0c0-2.2 1.1-3.4 2-4.3.2 1.5.9 2.4 1.8 2.6C10.5 7.6 11 5 12 3z'/><path d='M4 20.5l16-3M4 17.5l16 3'/>`,
  shield: `<circle cx='12' cy='12' r='9'/><circle cx='12' cy='12' r='2.3'/><path d='M12 3v6.7M12 14.3V21M3 12h6.7M14.3 12H21'/>`,
  horn: `<path d='M3.5 6.5c4.2 0 8.4 2 11.7 5.3 2.2 2.2 3.8 4.8 4.3 7.7l2 .8-2.6 1.2c-2-2.6-4.4-4.8-7-6.3-2.7-1.6-5.6-2.5-8.4-2.7z'/><path d='M3.5 6.5v6M7 7l-.4 5.9M10.4 8.3l-1.3 5.6'/>`,
  helmet: `<path d='M4.5 12.5a7.5 7.5 0 0 1 15 0v3.5M4.5 12.5V16'/><path d='M4.5 12.5h15M12 5v7.5'/><circle cx='9.2' cy='15.2' r='1.9'/><circle cx='14.8' cy='15.2' r='1.9'/><path d='M12 12.5v7'/>`,
  sun: `<circle cx='12' cy='12' r='4.2'/><path d='M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1'/>`,
  moon: `<path d='M15.5 3.5a8.6 8.6 0 1 0 5 13.6A7.2 7.2 0 0 1 15.5 3.5z'/><path d='M18.5 6.5l.6 1.4 1.4.6-1.4.6-.6 1.4-.6-1.4-1.4-.6 1.4-.6z' fill='currentColor' stroke='none'/>`,
};

/** The name of each picture and who or what it is, in the chosen language. */
const NAMES: Record<string, () => [string, string]> = {
  raven: () => [t('Raven'), t('Huginn and Muninn, Odin\'s ravens')],
  wolf: () => [t('Wolf'), t('Geri and Freki')],
  bear: () => [t('Bear'), t('The berserkers\' bear')],
  eagle: () => [t('Eagle'), t('In the crown of Yggdrasil')],
  serpent: () => [t('Serpent'), t('Jörmungandr, around the world')],
  horse: () => [t('Horse'), t('Sleipnir, with eight legs')],
  boar: () => [t('Boar'), t('Gullinbursti, Freyr\'s golden boar')],
  stag: () => [t('Stag'), t('Eikthyrnir, on Valhalla\'s roof')],
  goat: () => [t('Goat'), t('Tanngrisnir, Thor\'s goat')],
  cat: () => [t('Cat'), t('Freyja\'s cats')],
  squirrel: () => [t('Squirrel'), t('Ratatoskr, on the world tree')],
  salmon: () => [t('Salmon'), t('Loki as a salmon')],
  tree: () => [t('World tree'), t('Yggdrasil')],
  longship: () => [t('Longship'), t('Over the sea')],
  longhouse: () => [t('Longhouse'), t('The hall')],
  mountain: () => [t('Mountain'), t('Jotunheim\'s peaks')],
  fjord: () => [t('Fjord'), t('Between the cliffs')],
  bridge: () => [t('Rainbow bridge'), t('Bifröst')],
  hearth: () => [t('Hearth'), t('The fire in the hall')],
  shield: () => [t('Shield'), t('The round shield')],
  horn: () => [t('Drinking horn'), t('For the feast')],
  helmet: () => [t('Helmet'), t('With eye guards, no horns')],
  sun: () => [t('Sun'), t('Sól')],
  moon: () => [t('Moon'), t('Máni')],
};

export function pictures(kind: AvatarKind): string[] {
  return Object.keys(kind === 'user' ? USERS : ROOMS);
}

export function describe(id: string): [string, string] {
  return NAMES[id]?.() ?? [id, ''];
}

/** The SVG of a picture; its markup is a constant above, never text from EDS. */
export function pictureSvg(kind: AvatarKind, id: string | undefined): SVGSVGElement | null {
  const markup = id ? (kind === 'user' ? USERS : ROOMS)[id] : undefined;
  if (!markup) {
    return null;
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  for (const [name, value] of Object.entries({
    viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.6',
    'stroke-linecap': 'round', 'stroke-linejoin': 'round', 'aria-hidden': 'true', focusable: 'false',
  })) {
    svg.setAttribute(name, value);
  }
  svg.innerHTML = markup;
  return svg;
}

/**
 * An avatar: the picture, or the initial of the name. The size is a class:
 * '' (30 px), 'small' (24 px) or 'large' (44 px).
 */
export function avatar(kind: AvatarKind, id: string | undefined, name: string, size: '' | 'small' | 'large' = ''): HTMLElement {
  const svg = pictureSvg(kind, id);
  const element = h('span', { class: ['avatar', size, svg ? 'picture' : ''].filter(Boolean).join(' '), 'aria-hidden': 'true' });
  if (svg) {
    element.title = describe(id as string)[0];
    element.append(svg);
  } else {
    element.textContent = (name || '?').substring(0, 1).toUpperCase();
  }
  return element;
}
