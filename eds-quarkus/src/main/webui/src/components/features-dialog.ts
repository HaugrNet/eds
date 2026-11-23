import type { Circle } from '../api';
import { FeatureState } from '../api';
import { h } from '../dom';
import { errorMessage } from '../session';
import { t } from '../i18n';
import { addEntry, listEntries, offeredFeatures, removeFeature, roomFeatures, switchFeature, FEATURES, type FeatureName } from '../features/features';
import { parseServers, relayServers, writeServers, type RelayBody } from '../features/calls';
import { socket } from '../socket';
import { formDialog } from './form-dialog';

/** Fired on the document with the ID of a room, when its features were switched here. */
export const FEATURES_CHANGED = 'room-features-changed';

/** What Rune calls its features. */
export function featureLabel(name: string): string {
  const labels: Record<FeatureName, string> = { chat: t('Chat'), calendar: t('Calendar'), plans: t('Plans'), location: t('Location'), calls: t('Calls') };
  return labels[name as FeatureName] ?? name;
}

/**
 * The features of a room (FEATURES.md): which of those the stone offers are on,
 * read-only or off here. Administrators of the room switch them; a feature
 * switched off keeps its entries and hides them.
 */
export function openFeaturesDialog(circle: Circle, administrator: boolean): void {
  const circleId = circle.circleId as string;
  const status = h('p', { class: 'error small', role: 'alert' });
  const list = h('div', { class: 'stack' }, h('p', { class: 'muted' }, t('Loading…')));
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const dialog = h('dialog', { class: 'modal' },
    h('header', {}, h('h2', {}, t('Features of {room}', { room: circle.circleName ?? t('the room') })), close),
    h('div', { class: 'dialog-body stack' }, status, list,
      h('p', { class: 'muted small' }, administrator
        ? t('A feature which is off keeps what it has and hides it; read-only shows it and takes nothing new. Removing a feature deletes what it has in this room.')
        : t('Administrators of the room switch its features.'))));
  dialog.addEventListener('close', () => dialog.remove());

  const render = async (): Promise<void> => {
    try {
      const [offered, features] = await Promise.all([offeredFeatures(), roomFeatures(circleId)]);
      const names = [...FEATURES.filter((name) => offered.has(name)), ...features.map((feature) => feature.name ?? '').filter((name) => !(FEATURES as readonly string[]).includes(name))];
      if (names.length === 0) {
        list.replaceChildren(h('p', { class: 'muted' }, t('This stone offers no features. Its System Administrator can offer them in the stone settings.')));
        return;
      }
      list.replaceChildren(...names.map((name) => {
        const state = features.find((feature) => feature.name === name)?.state;
        const run = async (work: () => Promise<void>) => {
          status.textContent = '';
          try {
            await work();
            document.dispatchEvent(new CustomEvent(FEATURES_CHANGED, { detail: circleId }));
          } catch (e) {
            status.textContent = await errorMessage(e);
          }
          await render();
        };
        const choice = h('select', { 'aria-label': featureLabel(name), disabled: !administrator },
          state ? null : h('option', { value: '', selected: true }, t('Not here')),
          h('option', { value: FeatureState.On, selected: state === FeatureState.On }, t('On')),
          h('option', { value: FeatureState.Readonly, selected: state === FeatureState.Readonly }, t('Read-only')),
          h('option', { value: FeatureState.Off, selected: state === FeatureState.Off }, t('Off')));
        choice.addEventListener('change', () => {
          if (choice.value) {
            void run(() => switchFeature(circleId, name, choice.value as FeatureState));
          }
        });
        const remove = state && administrator
          ? h('button', { type: 'button', class: 'link danger', onclick: () => {
            if (confirm(t('Remove {feature} from this room? Everything it has here is deleted, for everyone.', { feature: featureLabel(name) }))) {
              void run(() => removeFeature(circleId, name));
            }
          } }, t('Remove'))
          : null;
        const unknown = !(FEATURES as readonly string[]).includes(name);
        // The relay of the room's calls is the administrators' to name.
        const relay = name === 'calls' && state === FeatureState.On && administrator
          ? h('button', { type: 'button', class: 'link', onclick: () => void openRelayDialog(circleId).catch(async (e) => { status.textContent = await errorMessage(e); }) }, t('Relay…'))
          : null;
        return h('div', { class: 'feature-row' }, h('strong', {}, featureLabel(name)), choice, relay, remove,
          unknown ? h('span', { class: 'muted small' }, t('This app can\'t show this feature; another app of the room may.')) : null);
      }));
    } catch (e) {
      list.replaceChildren();
      status.textContent = await errorMessage(e);
    }
  };

  document.body.append(dialog);
  dialog.showModal();
  void render();
}

/**
 * The relay of the calls of a room (FEATURES.md, "Presence and calls"): the
 * STUN and TURN servers its administrators name, one per line. They are kept
 * as an entry of the room, in a chamber unreadable for the stone.
 */
async function openRelayDialog(circleId: string): Promise<void> {
  await socket.join(circleId);
  const servers = relayServers((await listEntries<RelayBody>(circleId, 'calls', { kind: 'relay', limit: 1 }))[0]?.body);
  const lines = h('textarea', { rows: 4, spellcheck: false, placeholder: 'stun:stun.example.org:3478\nturn:turn.example.org:3478 name credential' }, writeServers(servers));
  formDialog(t('Relay for calls'), [
    h('p', { class: 'muted small' }, t('A call connects the devices directly. Where two devices cannot reach each other, a TURN server passes the call on, which it cannot read; a STUN server tells a device its address as others see it. Rune names none by itself.')),
    h('label', {}, t('Servers, one per line: the address, and for a TURN server its user name and credential'), lines),
    h('p', { class: 'muted small' }, t('Everyone in the room can read them. They count for calls which begin afterwards.')),
  ], t('Save'), async () => {
    let parsed;
    try {
      parsed = parseServers(lines.value);
    } catch (e) {
      throw new Error(t('"{line}" is not a server. Write stun:host:port, or turn:host:port with a user name and a credential.', { line: e instanceof Error ? e.message : '' }));
    }
    const body: RelayBody = { v: 1, servers: parsed };
    await addEntry(circleId, 'calls', 'relay', body);
  });
}
