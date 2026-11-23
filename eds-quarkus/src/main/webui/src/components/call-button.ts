import { calls } from '../calls';
import { h } from '../dom';
import { t } from '../i18n';
import { presence } from '../presence';
import { startDirectCall } from './call-panel';

/** A telephone, as a line drawing in the colour of the text. */
const PHONE = "<svg viewBox='0 0 24 24' width='16' height='16' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true'><path d='M6.5 3.5h3l1.5 4-2 1.5a12 12 0 0 0 6 6l1.5-2 4 1.5v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.500 5.700 2 2 0 0 1 6.500 3.500z'/></svg>";

function show(button: HTMLElement): void {
  button.hidden = !calls.canCall(button.dataset.member);
  (button as HTMLButtonElement).disabled = !!calls.active();
}

let following = false;

/** Every such button on the page follows who can be called. */
function follow(): void {
  if (following) {
    return;
  }
  following = true;
  const all = () => document.querySelectorAll<HTMLElement>('button.call-person').forEach(show);
  presence.addEventListener('change', all);
  calls.addEventListener('change', all);
}

/**
 * The telephone beside a person (RUNE.md, "Presence and calls"): it shows
 * while they can be called, which is while they are online in a room with
 * calls shared with them, and calls them directly, with the voice; the
 * camera is switched on in the call.
 */
export function callButton(memberId: string | undefined, accountName: string): HTMLButtonElement {
  const label = t('Call {name}', { name: accountName });
  const button = h('button', { type: 'button', class: 'icon call-person', title: label, 'aria-label': label, hidden: true, onclick: (event: Event) => {
    event.stopPropagation();
    void startDirectCall(memberId ?? '', accountName);
  } });
  // The drawing is a constant of this file, never data.
  button.innerHTML = PHONE;
  button.dataset.member = memberId ?? '';
  follow();
  show(button);
  return button;
}
