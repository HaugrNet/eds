import { h } from '../dom';
import { saveAccountSettings } from '../account-settings';
import { browserLanguage, clockOptions, LANGUAGES, locale, t, type Language } from '../i18n';
import { addPane, PANES, paneName, panesIn, presets, removePane, resetLayout } from '../layout';
import { appearanceChoice, clockChoice, languageChoice, paletteChoice, setAppearanceChoice, setClockChoice, setLanguageChoice, setPaletteChoice, type AppearanceChoice, type ClockChoice, type LanguageChoice, type PaletteChoice } from '../settings';
import { applyTheme } from '../theme';
import { setShowMe, showMe } from '../presence';
import { socket } from '../socket';
import type { Workspace } from './workspace';

/** An example time in a clock, e.g. 14:05 or 2:05 PM. */
function sample(hourCycle?: 'h23' | 'h12'): string {
  return new Date(2026, 0, 1, 14, 5).toLocaleTimeString(locale, clockOptions(hourCycle));
}

export type SettingsSection = 'language' | 'theme' | 'panes' | 'presence';

/**
 * A part of the settings of this device, opened from the account menu:
 * language and clock (they reload Rune, so every text and time follows),
 * appearance and theme (at once), the arrangement of the panes, or whether
 * the person is shown as online.
 */
export function openSettingsDialog(workspace: Workspace, section: SettingsSection): void {
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');

  const language = h('select', {},
    h('option', { value: 'auto', selected: languageChoice() === 'auto' }, t('Automatic ({language})', { language: LANGUAGES[browserLanguage()] })),
    ...(Object.keys(LANGUAGES) as Language[]).map((code) =>
      h('option', { value: code, selected: languageChoice() === code, lang: code }, LANGUAGES[code])));
  language.addEventListener('change', () => {
    setLanguageChoice(language.value as LanguageChoice);
    void saveAccountSettings().then(() => location.reload());
  });

  const clock = h('select', {},
    h('option', { value: '24', selected: clockChoice() === '24' }, t('24 hours ({example})', { example: sample('h23') })),
    h('option', { value: '12', selected: clockChoice() === '12' }, t('12 hours ({example})', { example: sample('h12') })),
    h('option', { value: 'auto', selected: clockChoice() === 'auto' }, t('As the language ({example})', { example: sample() })));
  clock.addEventListener('change', () => {
    setClockChoice(clock.value as ClockChoice);
    void saveAccountSettings().then(() => location.reload());
  });

  // Appearance and theme apply at once.
  const appearance = h('select', {},
    h('option', { value: 'auto', selected: appearanceChoice() === 'auto' }, t('As the system')),
    h('option', { value: 'light', selected: appearanceChoice() === 'light' }, t('Light')),
    h('option', { value: 'dark', selected: appearanceChoice() === 'dark' }, t('Dark')));
  appearance.addEventListener('change', () => {
    setAppearanceChoice(appearance.value as AppearanceChoice);
    applyTheme();
  });
  const palettes: [PaletteChoice, string][] = [
    ['rune', t('Rune (stone and ochre)')],
    ['fjord', t('Fjord (blue)')],
    ['moss', t('Moss (green)')],
    ['contrast', t('High contrast')],
  ];
  const palette = h('select', {}, ...palettes.map(([value, label]) =>
    h('option', { value, selected: paletteChoice() === value }, label)));
  palette.addEventListener('change', () => {
    setPaletteChoice(palette.value as PaletteChoice);
    applyTheme();
  });

  const shown = h('div', { class: 'pane-choices' });
  const showChoices = () => {
    const visible = panesIn(workspace.current());
    shown.replaceChildren(...PANES.map((pane) => {
      const box = h('input', { type: 'checkbox', checked: visible.includes(pane), disabled: visible.length === 1 && visible.includes(pane) });
      box.addEventListener('change', () => {
        const layout = workspace.current();
        workspace.set(box.checked ? addPane(layout, pane) : (removePane(layout, pane) ?? layout));
        showChoices();
      });
      return h('label', { class: 'choice inline' }, box, paneName(pane));
    }));
  };
  showChoices();
  const presetButtons = presets().map((preset) =>
    h('button', { type: 'button', class: 'secondary', onclick: () => { workspace.set(preset.layout); showChoices(); } }, preset.name));
  const reset = h('button', { type: 'button', class: 'link', onclick: () => { workspace.set(resetLayout()); showChoices(); } }, t('Reset'));

  // Shown as online or not: the rooms are joined anew, so the others see it at once.
  const shownBox = h('input', { type: 'checkbox', checked: showMe() });
  shownBox.addEventListener('change', () => {
    setShowMe(shownBox.checked);
    void socket.rejoin();
  });

  const sections: Record<SettingsSection, [string, HTMLElement[]]> = {
    language: [t('Language and clock'), [
      h('label', {}, t('Language'), language),
      h('label', {}, t('Clock'), clock),
      h('p', { class: 'muted small' }, t('Rune reloads to use them.')),
    ]],
    theme: [t('Theme'), [
      h('label', {}, t('Appearance'), appearance),
      h('label', {}, t('Theme'), palette),
    ]],
    panes: [t('Panes'), [
      h('p', { class: 'muted small' }, t('Drag a pane by its bar onto another pane: near an edge to put it there, in the middle to swap the two. At the edge of the window, it goes along that whole edge. Drag the dividers to resize.')),
      h('div', { class: 'actions' }, ...presetButtons),
      h('div', { class: 'actions' }, t('Show:'), shown),
      h('div', { class: 'actions' }, reset),
      h('p', { class: 'muted small' }, t('On a phone, Rune shows one pane at a time.')),
    ]],
    presence: [t('Online status'), [
      h('label', { class: 'check' }, shownBox, t('Show me as online')),
      h('p', { class: 'muted small' }, t('While Rune is open, the people you share a room with see a light beside your name. Switched off, they see none, and cannot call you directly; you still see theirs.')),
    ]],
  };
  const [title, body] = sections[section];
  const dialog = h('dialog', { class: 'modal' },
    h('header', {}, h('h2', {}, title), close),
    h('div', { class: 'dialog-body stack' }, ...body,
      h('p', { class: 'muted small' }, t('Kept with your account.'))));
  dialog.addEventListener('close', () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();
}
