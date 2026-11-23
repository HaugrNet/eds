import { formatSize, h } from '../dom';
import { errorMessage } from '../session';
import { INTERNAL_SETTINGS, serverSettings, settingGroups, type ServerSettings, type SettingGroup, type SettingInfo } from '../server';
import { t } from '../i18n';

/** Fired on the document with the settings, when they were read or changed. */
export const SERVER_SETTINGS = 'server-settings';

/**
 * The settings of EDS, for the System Administrator: reviewed in groups,
 * changed in place and saved together; EDS takes all changes or none.
 * Shown in a window over the panes (server-window.ts).
 * Settings Rune doesn't know show as they are, at the end.
 */
export class EdsServer extends HTMLElement {
  private status = h('p', { class: 'small', role: 'status' });
  private body = h('div', { class: 'server-settings' });
  private save = h('button', { type: 'button', disabled: true, onclick: () => void this.store() }, t('Save changes'));
  private discard = h('button', { type: 'button', class: 'secondary', disabled: true, onclick: () => this.render() }, t('Discard'));
  private reload = h('button', { type: 'button', class: 'icon', title: t('Read the settings again'), 'aria-label': t('Read the settings again'), onclick: () => void this.load() }, '⟳');
  /** The settings as EDS has them, and as edited here. */
  private stored: Record<string, string> = {};
  private allowed: Record<string, string[]> = {};
  private edited: Record<string, string> = {};
  private careful = new Set<string>();
  /** Further sections shown after the settings: the features and the status keys of the stone. */
  private sections: HTMLElement[] = [];

  setSections(sections: HTMLElement[]): void {
    this.sections = sections;
  }

  connectedCallback(): void {
    if (!this.firstChild) {
      this.replaceChildren(
        h('div', { class: 'toolbar server-bar' }, this.save, this.discard, this.reload, this.status),
        this.body);
    }
  }

  async load(): Promise<void> {
    this.tell(t('Loading…'));
    try {
      this.use(await serverSettings());
      this.tell('');
    } catch (e) {
      this.tell(await errorMessage(e), true);
    }
  }

  private use(server: ServerSettings): void {
    this.stored = server.settings;
    this.allowed = server.allowedValues;
    this.render();
    document.dispatchEvent(new CustomEvent(SERVER_SETTINGS, { detail: server.settings }));
  }

  private tell(text: string, error = false): void {
    this.status.textContent = text;
    this.status.classList.toggle('error', error);
    this.status.classList.toggle('muted', !error);
  }

  /** Whether a setting was edited and not saved. */
  dirty(): boolean {
    return Object.keys(this.changes()).length > 0;
  }

  /** Drops the edits that weren't saved. */
  discardChanges(): void {
    this.render();
  }

  private changes(): Record<string, string> {
    return Object.fromEntries(Object.entries(this.edited).filter(([key, value]) => value.trim() !== (this.stored[key] ?? '')).map(([key, value]) => [key, value.trim()]));
  }

  private showDirty(): void {
    const changed = Object.keys(this.changes());
    this.save.disabled = this.discard.disabled = changed.length === 0;
    this.save.textContent = changed.length > 1 ? t('Save {n} changes', { n: changed.length }) : t('Save changes');
    this.body.querySelectorAll<HTMLElement>('[data-key]').forEach((row) => row.classList.toggle('changed', changed.includes(row.dataset.key ?? '')));
  }

  private async store(): Promise<void> {
    const changes = this.changes();
    const risky = Object.keys(changes).filter((key) => this.careful.has(key));
    if (risky.length > 0 && !confirm(t('Change {keys}? A wrong value can lock everyone out or make new data unreadable.', { keys: risky.join(', ') }))) {
      return;
    }
    this.save.disabled = true;
    this.tell(t('Saving…'));
    try {
      this.use(await serverSettings(changes));
      this.tell(t('Saved.'));
    } catch (e) {
      // EDS took none of the changes; they stay here to be corrected.
      this.tell(await errorMessage(e), true);
      this.showDirty();
    }
  }

  private render(): void {
    this.edited = {};
    this.careful.clear();
    const known = new Set<string>();
    const groups: SettingGroup[] = settingGroups();
    for (const group of groups) {
      group.settings = group.settings.filter((setting) => setting.key in this.stored);
      group.settings.forEach((setting) => known.add(setting.key));
    }
    const others = Object.keys(this.stored).filter((key) => !known.has(key) && !INTERNAL_SETTINGS.includes(key)).sort();
    if (others.length > 0) {
      groups.push({ title: t('Other settings'), note: t('Settings Rune has no name for. An empty value removes one.'),
        settings: others.map((key) => ({ key, label: key, help: '', kind: 'text' })) });
    }
    this.body.replaceChildren(...groups.filter((group) => group.settings.length > 0).map((group) =>
      h('section', { class: 'setting-group' },
        h('h3', {}, group.title),
        group.note ? h('p', { class: 'muted small' }, group.note) : null,
        ...group.settings.map((setting) => this.row(setting)))), ...this.sections);
    this.showDirty();
  }

  private row(setting: SettingInfo): HTMLElement {
    const value = this.stored[setting.key] ?? '';
    const id = `setting-${setting.key.replace(/[^a-z0-9]+/gi, '-')}`;
    if (setting.careful) {
      this.careful.add(setting.key);
    }
    const change = (to: string) => {
      this.edited[setting.key] = to;
      this.showDirty();
    };
    let field: HTMLElement;
    if (setting.kind === 'fixed') {
      const shown = value === 'true' ? t('Yes') : value === 'false' ? t('No') : value || t('not set');
      field = h('span', { class: 'setting-fixed', id }, shown);
    } else if (setting.kind === 'switch') {
      const box = h('input', { type: 'checkbox', id, checked: value === 'true' });
      box.addEventListener('change', () => change(String(box.checked)));
      field = h('span', { class: 'setting-field' }, box);
    } else if (setting.kind === 'choice' && (this.allowed[setting.key] ?? []).length > 0) {
      // What EDS supports; a stored value it no longer lists stays choosable.
      const choices = this.allowed[setting.key];
      const select = h('select', { id }, ...(choices.includes(value) ? choices : [value, ...choices]).map((choice) =>
        h('option', { value: choice, selected: choice === value }, choice)));
      select.addEventListener('change', () => change(select.value));
      field = h('span', { class: 'setting-field' }, select);
    } else {
      const number = setting.kind === 'number';
      const input = h('input', { type: 'text', id, value, spellcheck: false, autocomplete: 'off', ...(number ? { inputmode: 'numeric', pattern: '\\d+', class: 'number' } : {}) });
      const size = setting.bytes ? h('span', { class: 'muted small setting-size' }) : null;
      const showSize = () => {
        if (size) {
          const bytes = Number(input.value);
          size.textContent = /^\d+$/.test(input.value.trim()) && bytes > 0 ? `≈ ${formatSize(bytes)}` : '';
        }
      };
      input.addEventListener('input', () => { change(input.value); showSize(); });
      showSize();
      field = h('span', { class: 'setting-field' }, input, setting.unit ? h('span', { class: 'muted small' }, setting.unit) : null, size);
    }
    return h('div', { class: 'setting', 'data-key': setting.key },
      h('div', { class: 'setting-name' },
        h('label', { for: id }, setting.label),
        setting.help ? h('span', { class: 'muted small' }, setting.help) : null,
        setting.label === setting.key ? null : h('code', { class: 'muted' }, setting.key)),
      field);
  }
}

customElements.define('eds-server', EdsServer);
