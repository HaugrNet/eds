import { h } from '../dom';
import { api, errorMessage } from '../session';
import { t } from '../i18n';

/**
 * The status keys of this stone (STATUS.md), for the System Administrator:
 * a key reads the status of the stone and nothing else, so whoever looks
 * after several stones needs no account on each. A new key is shown once;
 * the stone keeps only its hash.
 */
export function stoneStatusKeys(): { element: HTMLElement; load: () => Promise<void> } {
  const status = h('p', { class: 'error small', role: 'alert' });
  const list = h('div', { class: 'stack' });
  const shown = h('div', { class: 'stack' });
  const label = h('input', { maxlength: 75, required: true, placeholder: t('A label, e.g. who gets the key'), 'aria-label': t('Label of the new status key') });
  const form = h('form', { class: 'feature-add' }, label, h('button', { type: 'submit', class: 'secondary' }, t('Create a status key')));
  const element = h('section', { class: 'setting-group' },
    h('h3', {}, t('Status keys')),
    h('p', { class: 'muted small' }, t('A status key reads the status of this stone (its load and what it holds, in general numbers) and nothing else. Give one to whoever looks after this stone from elsewhere; with its address they add it under "Stones".')),
    status, list, form, shown);

  const load = async (): Promise<void> => {
    try {
      const keys = (await api.status.getStatusKeys()).keys ?? [];
      list.replaceChildren(...(keys.length === 0 ? [h('p', { class: 'muted small' }, t('No status keys.'))] : keys.map((key) =>
        h('div', { class: 'feature-row' }, h('strong', {}, key),
          h('button', { type: 'button', class: 'link danger', onclick: async () => {
            if (confirm(t('Withdraw the status key "{label}"? Whoever uses it is refused from then on.', { label: key }))) {
              try {
                await api.status.deleteStatusKey({ label: key });
              } catch (e) {
                status.textContent = await errorMessage(e);
              }
              await load();
            }
          } }, t('Withdraw'))))));
    } catch (e) {
      status.textContent = await errorMessage(e);
    }
  };
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    status.textContent = '';
    try {
      const made = await api.status.postStatusKey({ statusKeyRequest: { label: label.value.trim() } });
      const field = h('input', { readonly: true, value: made.key ?? '', class: 'recovery-key', 'aria-label': t('The new status key') });
      shown.replaceChildren(
        h('p', {}, t('The status key "{label}". It is shown only now: copy it, and give it to whoever looks after this stone, with the address {address}.', { label: label.value.trim(), address: location.origin })),
        field,
        h('div', { class: 'actions' }, h('button', { type: 'button', class: 'secondary small', onclick: async () => {
          try {
            await navigator.clipboard.writeText(made.key ?? '');
          } catch {
            field.select();
          }
        } }, t('Copy'))));
      label.value = '';
      await load();
    } catch (e) {
      status.textContent = await errorMessage(e);
    }
  });
  return { element, load };
}
