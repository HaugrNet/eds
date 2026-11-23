import type { SocketFrame } from '../api';
import { h } from '../dom';
import { formatNumber, language, t } from '../i18n';
import { inChamber } from '../e2e/files';
import { socket } from '../socket';
import { allEntries, type FeatureEntry } from '../features/features';
import { coordinates, distance, geoLink, isPosition, type PositionBody, type Reporting } from '../features/location';
import { canReport, onReport, ownPosition, reporting, reportProblem, setReporting } from '../features/reporter';
import { FeaturePane } from './feature-pane';

const FEATURE = 'location';
/** A position which was passed on and not kept is shown this long. */
const PASSING_SHOWN = 300000;

function ago(at: string): string {
  const seconds = Math.round((Date.parse(at) - Date.now()) / 1000);
  const format = new Intl.RelativeTimeFormat(language(), { numeric: 'auto' });
  const steps: [number, Intl.RelativeTimeFormatUnit][] = [[60, 'second'], [3600, 'minute'], [86400, 'hour'], [Infinity, 'day']];
  let unit = 1;
  for (const [limit, name] of steps) {
    if (Math.abs(seconds) < limit) {
      return format.format(Math.round(seconds / unit), name);
    }
    unit = limit;
  }
  return '';
}

function far(metres: number): string {
  return metres < 1000 ? t('{n} m', { n: formatNumber(Math.round(metres)) }) : t('{n} km', { n: formatNumber(metres / 1000, metres < 10000 ? 1 : 0) });
}

/**
 * Where the people of a room were last seen (FEATURES.md, "Location"): a list,
 * with the place as numbers, the distance from this device, and a link
 * which opens the device's own map app; and the switch for what this device
 * reports. No map: Rune loads nothing from elsewhere.
 */
export class EdsLocation extends FeaturePane {
  protected readonly feature = FEATURE;
  private positions: FeatureEntry<PositionBody>[] = [];
  private tracks: FeatureEntry<PositionBody>[] = [];
  private passing = new Map<string, PositionBody>();
  private tracksOf = new Set<string>();
  private chamber = false;
  private stop: (() => void) | null = null;
  private tick: number | undefined;

  connectedCallback(): void {
    super.connectedCallback();
    // The distances follow this device; the times pass.
    this.stop = onReport(() => this.render());
    this.tick = window.setInterval(() => this.render(), 30000);
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.stop?.();
    window.clearInterval(this.tick);
  }

  protected leaving(): void {
    this.positions = [];
    this.tracks = [];
    this.passing.clear();
    this.tracksOf.clear();
  }

  protected async load(): Promise<void> {
    const circleId = this.circleId;
    const [positions, tracks, chamber] = await Promise.all([
      allEntries<PositionBody>(circleId, FEATURE, 'position'),
      allEntries<PositionBody>(circleId, FEATURE, 'track'),
      inChamber(circleId),
    ]);
    if (circleId !== this.circleId) {
      return;
    }
    this.positions = positions.filter((entry) => isPosition(entry.body));
    this.tracks = tracks.filter((entry) => isPosition(entry.body));
    this.chamber = chamber;
    this.render();
  }

  protected signalled(frame: SocketFrame): void {
    try {
      const body = JSON.parse(frame.signal ?? '') as unknown;
      if (frame.memberId && isPosition(body)) {
        this.passing.set(frame.memberId, body);
        this.render();
      }
    } catch {
      // Not a position.
    }
  }

  private render(): void {
    if (!this.circleId || !this.usable()) {
      return;
    }
    const circleId = this.circleId;
    const own = ownPosition();
    const now = Date.now();
    // Of each person the newest: what is kept, or what was passed on a moment ago.
    const seen = new Map<string, { body: PositionBody; kept: boolean; entry?: FeatureEntry<PositionBody> }>();
    for (const entry of this.positions) {
      const body = entry.body as PositionBody;
      const before = seen.get(entry.memberId ?? '');
      if (entry.memberId && (!before || before.body.at < body.at)) {
        seen.set(entry.memberId, { body, kept: true, entry });
      }
    }
    for (const [memberId, body] of this.passing) {
      const before = seen.get(memberId);
      if (now - Date.parse(body.at) < PASSING_SHOWN && (!before || before.body.at < body.at)) {
        seen.set(memberId, { body, kept: false });
      }
    }
    const people = [...seen].sort((a, b) => (a[1].body.at < b[1].body.at ? 1 : -1));
    const list = h('ul', { class: 'feature-list places' }, ...people.map(([memberId, { body, kept, entry }]) => {
      const mine = memberId === this.me();
      const track = this.tracks.filter((point) => point.memberId === memberId).sort((a, b) => ((a.body as PositionBody).at < (b.body as PositionBody).at ? 1 : -1));
      const open = this.tracksOf.has(memberId);
      return h('li', { class: 'place' },
        h('div', { class: 'grow' },
          h('strong', {}, this.nameOf(memberId), mine ? ` ${t('(you)')}` : ''), ' ',
          h('span', { class: 'muted small' }, ago(body.at), kept ? '' : ` · ${t('passing, not kept')}`),
          h('div', { class: 'small' }, h('a', { href: geoLink(body), title: t('Open in the map app of this device') }, coordinates(body)),
            body.acc ? h('span', { class: 'muted' }, ` ± ${far(body.acc)}`) : null,
            own && !mine ? h('span', { class: 'muted' }, ` · ${t('{distance} from here', { distance: far(distance(own, body)) })}`) : null),
          body.note ? h('div', { class: 'small' }, body.note) : null,
          track.length > 0
            ? h('button', { type: 'button', class: 'link small', onclick: () => { if (open) { this.tracksOf.delete(memberId); } else { this.tracksOf.add(memberId); } this.render(); } },
              open ? t('Hide the track') : t('Track of the last day ({n})', { n: track.length }))
            : null,
          open ? h('ol', { class: 'track small' }, ...track.map((point) => {
            const at = point.body as PositionBody;
            return h('li', {}, new Date(at.at).toLocaleTimeString(language(), { hour: '2-digit', minute: '2-digit' }), ' ', h('a', { href: geoLink(at) }, coordinates(at)));
          })) : null),
        mine && kept && entry
          ? h('button', { type: 'button', class: 'icon', title: t('Remove my position'), 'aria-label': t('Remove my position'), onclick: () => {
            void this.run(async () => {
              await socket.remove(circleId, FEATURE, entry.entryId);
              for (const point of this.tracks.filter((p) => p.memberId === this.me())) {
                await socket.remove(circleId, FEATURE, point.entryId);
              }
            });
          } }, '✕')
          : null);
    }));

    const mode = reporting(circleId);
    const choice = h('select', { 'aria-label': t('What this device reports'), disabled: !this.open() || !canReport() },
      ...([['off', t('Nothing')], ['latest', t('Where I am, the latest only')], ['track', t('Where I am, and my track of the last day')], ['passing', t('Where I am, passed on and not kept')]] as [Reporting, string][])
        .map(([value, label]) => h('option', { value, selected: value === mode }, label)));
    choice.addEventListener('change', () => {
      setReporting(circleId, choice.value as Reporting);
      this.render();
    });
    const problem = reportProblem();
    this.body.replaceChildren(
      h('div', { class: 'stack report' },
        h('label', {}, t('This device reports to this room'), choice),
        !canReport() ? h('p', { class: 'muted small' }, t('This device can\'t tell where it is.')) : null,
        mode !== 'off' && problem ? h('p', { class: 'error small' }, t('The device does not tell where it is: {problem}', { problem })) : null,
        h('p', { class: 'muted small' }, t('Rune reports only while it is open on this device, and only what you chose here. Everyone in the room sees it; you can remove what is kept of you.')),
        this.chamber ? null : h('p', { class: 'notice small' }, t('This is a room, not a chamber: the stone can read the positions while it handles them. For where people are, a chamber is the better place.'))),
      people.length === 0 ? h('p', { class: 'muted' }, t('Nobody has reported where they are.')) : list);
  }
}

customElements.define('eds-location', EdsLocation);
