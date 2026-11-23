import { h } from '../dom';
import { t } from '../i18n';
import { knotFrame } from './knot';
import { stoneFeatures } from './stone-features';
import { stoneStatusKeys } from './stone-status-keys';
import type { EdsServer } from './server-settings';

/**
 * The settings of EDS in a window that floats over the panes, for the
 * System Administrator: opened from the account menu. Rune's knot goes
 * around it, as a frame with round corners, to show that it lies on top.
 * Edits that weren't saved are asked about before it closes.
 */
export function openServerWindow(settings: EdsServer): void {
  const frame = knotFrame('window-knot');
  const features = stoneFeatures();
  const keys = stoneStatusKeys();
  settings.setSections([features.element, keys.element]);
  const leave = () => {
    if (!settings.dirty() || confirm(t('Close without saving the changes?'))) {
      dialog.close();
    }
  };
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: leave }, '✕');
  const dialog = h('dialog', { class: 'knot-window', 'aria-label': t('Stone settings') },
    frame.element,
    h('div', { class: 'window-body' },
      h('header', {}, h('h2', {}, t('Stone settings')), close),
      settings));
  // Escape closes as the button does.
  dialog.addEventListener('cancel', (event) => {
    event.preventDefault();
    leave();
  });
  // The knot is drawn for the size of the window, and again when that changes.
  const sizes = new ResizeObserver(() => frame.fit(dialog.offsetWidth, dialog.offsetHeight));
  sizes.observe(dialog);
  dialog.addEventListener('close', () => {
    sizes.disconnect();
    settings.discardChanges();
    dialog.remove();
  });
  document.body.append(dialog);
  dialog.showModal();
  void settings.load();
  void features.load();
  void keys.load();
}
