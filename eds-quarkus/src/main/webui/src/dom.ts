// A small DOM helper. Text is always set as text, never parsed as HTML, so
// names and messages from other Members can't inject markup.
import { clockOptions, formatNumber, locale } from './i18n';

type Child = Node | string | null | undefined | false;
type Props = { [key: string]: string | boolean | number | EventListener | undefined };

export function h<K extends keyof HTMLElementTagNameMap>(tag: K, props: Props = {}, ...children: Child[]): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (value === undefined || value === false) {
      continue;
    }
    if (key.startsWith('on') && typeof value === 'function') {
      element.addEventListener(key.substring(2), value);
    } else if (key === 'class') {
      element.className = String(value);
    } else if (value === true) {
      element.setAttribute(key, '');
    } else {
      element.setAttribute(key, String(value));
    }
  }
  append(element, ...children);
  return element;
}

export function append(parent: Node, ...children: Child[]): void {
  for (const child of children) {
    if (child) {
      parent.appendChild(typeof child === 'string' ? document.createTextNode(child) : child);
    }
  }
}

export function clear(element: Element): void {
  element.replaceChildren();
}

export function formatSize(bytes: number | undefined): string {
  if (bytes === undefined || bytes === null) {
    return '';
  }
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${formatNumber(value, unit === 0 || value >= 10 ? 0 : 1)} ${units[unit]}`;
}

/** EDS sends UTC times without a zone suffix. */
export function parseTime(time: string | undefined): Date | null {
  return time ? new Date(time.endsWith('Z') ? time : `${time}Z`) : null;
}

/**
 * A time in the language's format, with the chosen clock (24 or 12 hours):
 * only the time for today, else the date with it.
 */
export function formatTime(time: string | undefined): string {
  const date = parseTime(time);
  if (!date) {
    return '';
  }
  const today = new Date().toDateString() === date.toDateString();
  const clock = clockOptions();
  return today
    ? date.toLocaleTimeString(locale, clock)
    : date.toLocaleString(locale, { year: 'numeric', month: '2-digit', day: '2-digit', ...clock });
}
