import { h } from '../dom';
import { t } from '../i18n';

/**
 * Notes of Rune which don't belong to a pane: that a call began, that a
 * call set for a time is due, why a call ended. They show in a corner over
 * the panes, each with what can be done about it, until they are closed or
 * their time is up.
 */
export interface NoticeAction { label: string; run: () => void }

let region: HTMLElement | null = null;

function place(): HTMLElement {
  if (!region || !region.isConnected) {
    region = h('div', { class: 'notices', role: 'status', 'aria-live': 'polite' });
    document.body.append(region);
  }
  return region;
}

/** Shows a note; one with the same `key` replaces the earlier one. It goes after `seconds`, or stays with 0. Returns what closes it. */
export function notice(text: string, actions: NoticeAction[] = [], seconds = 30, key?: string): () => void {
  const element = h('div', { class: 'notice-card' });
  const close = () => element.remove();
  if (key) {
    element.dataset.key = key;
    place().querySelectorAll<HTMLElement>('.notice-card').forEach((old) => {
      if (old.dataset.key === key) {
        old.remove();
      }
    });
  }
  element.append(
    h('p', {}, text),
    h('div', { class: 'actions' },
      ...actions.map((action) => h('button', { type: 'button', class: 'small', onclick: () => { close(); action.run(); } }, action.label)),
      h('button', { type: 'button', class: 'secondary small', onclick: close }, t('Close'))));
  place().append(element);
  if (seconds > 0) {
    window.setTimeout(close, seconds * 1000);
  }
  return close;
}

/** Takes all notes away, at logout. */
export function clearNotices(): void {
  region?.remove();
  region = null;
}
