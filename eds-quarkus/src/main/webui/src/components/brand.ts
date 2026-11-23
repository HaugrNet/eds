import { h } from '../dom';

/** The Rune mark, in its light or dark version after the OS setting. */
export function brandMark(className: string): HTMLPictureElement {
  return h('picture', {},
    h('source', { srcset: 'rune-mark-dark.svg', media: '(prefers-color-scheme: dark)' }),
    h('img', { src: 'rune-mark-light.svg', alt: '', class: className }));
}
