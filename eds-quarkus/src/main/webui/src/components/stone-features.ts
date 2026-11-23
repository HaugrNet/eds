import { h } from '../dom';
import { errorMessage } from '../session';
import { t } from '../i18n';
import { DECLARATIONS, offeredFeatures, offerFeature, FEATURES, withdrawFeature } from '../features/features';
import { featureLabel } from './features-dialog';

/**
 * The features this stone offers to its rooms (FEATURES.md), for the System
 * Administrator: each feature Rune knows can be offered, brought up to Rune's
 * version, or withdrawn once no room has it.
 */
export function stoneFeatures(): { element: HTMLElement; load: () => Promise<void> } {
  const status = h('p', { class: 'error small', role: 'alert' });
  const list = h('div', { class: 'stack' });
  const element = h('section', { class: 'setting-group' },
    h('h3', {}, t('Features')),
    h('p', { class: 'muted small' }, t('What the rooms of this stone may use beside their files. The administrators of a room switch them on there. The stone stores what a feature keeps and enforces its rules, without knowing what it is.')),
    status, list);

  const load = async (): Promise<void> => {
    try {
      const offered = await offeredFeatures();
      const names = [...FEATURES, ...[...offered.keys()].filter((name) => !(FEATURES as readonly string[]).includes(name))];
      list.replaceChildren(...names.map((name) => {
        const version = offered.get(name);
        const mine = DECLARATIONS[name]?.version;
        const run = async (work: () => Promise<void>) => {
          status.textContent = '';
          try {
            await work();
          } catch (e) {
            status.textContent = await errorMessage(e);
          }
          await load();
        };
        const told = version === undefined ? t('not offered')
          : mine !== undefined && mine > version ? t('offered, version {version}; Rune has version {mine}', { version, mine })
            : t('offered, version {version}', { version });
        const offer = mine !== undefined && (version === undefined || mine > version)
          ? h('button', { type: 'button', class: 'secondary small', onclick: () => void run(() => offerFeature(name as typeof FEATURES[number])) }, version === undefined ? t('Offer') : t('Update'))
          : null;
        const withdraw = version !== undefined
          ? h('button', { type: 'button', class: 'link danger', onclick: () => {
            if (confirm(t('Stop offering {feature}? This works only when no room has it.', { feature: featureLabel(name) }))) {
              void run(() => withdrawFeature(name));
            }
          } }, t('Withdraw'))
          : null;
        return h('div', { class: 'feature-row' }, h('strong', {}, featureLabel(name)), h('span', { class: 'muted small' }, told), offer, withdraw);
      }));
    } catch (e) {
      status.textContent = await errorMessage(e);
    }
  };
  return { element, load };
}
