import { formatSize, h } from '../dom';
import { formatNumber, language, t, tn } from '../i18n';
import {
  apiAddress, askStatus, busy, curve, fullAt, keepStones, latest, level, merge, StatusError, stones,
  type Level, type Sample, type Status, type Stone,
} from '../stones';
import { knotFrame } from './knot';

/** What is known of one stone while the screen is open. */
interface Watched {
  /** The further stone, or null for the one Rune is served by. */
  stone: Stone | null;
  status?: Status;
  load: Sample[];
  inventory: Sample[];
  day: Sample[];
  /** Seconds between two samples of the load. */
  interval: number;
  problem?: StatusError['kind'];
  /** When the stone last answered, on this device's clock. */
  answered?: number;
  /** When the day and the inventories were last asked for. */
  history: number;
  showDay: boolean;
  card: HTMLElement;
}

function utc(time: string | undefined): Date | null {
  if (!time) {
    return null;
  }
  const at = new Date(/Z|[+-]\d\d:?\d\d$/.test(time) ? time : `${time}Z`);
  return Number.isNaN(at.getTime()) ? null : at;
}

function when(at: Date | number | null): string {
  return at === null ? '' : new Date(at).toLocaleString(language(), { dateStyle: 'medium', timeStyle: 'short' });
}

/** A small curve of numbers, drawn by Rune itself. */
function spark(label: string, values: number[], shown: string, mark: Level = 'fine', top?: number): HTMLElement {
  const NS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(NS, 'svg');
  svg.setAttribute('viewBox', '0 0 120 28');
  svg.setAttribute('preserveAspectRatio', 'none');
  svg.setAttribute('class', 'spark');
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', `${label}: ${shown}`);
  const line = document.createElementNS(NS, 'polyline');
  line.setAttribute('points', curve(values, 120, 26, top).split(' ').map((point) => { const [x, y] = point.split(','); return `${x},${(Number(y) + 1).toFixed(1)}`; }).join(' '));
  svg.append(line);
  return h('div', { class: `measure ${mark}` }, h('span', { class: 'measure-name' }, label), h('strong', {}, shown), values.length > 1 ? svg : null);
}

function number(label: string, shown: string, mark: Level = 'fine'): HTMLElement {
  return h('div', { class: `measure ${mark}` }, h('span', { class: 'measure-name' }, label), h('strong', {}, shown));
}

/**
 * The stones a System Administrator looks after (STATUS.md): the one Rune
 * is served by, read with the session, and further ones by address and
 * status key. For each, what it tells: whether it is awake, its storage
 * against the ceiling, who is active, the requests, the processors, the
 * memory, each with a small curve of the recent load, and the last day on
 * request. The busiest come first; what is near a limit is coloured. The
 * limits are Rune's.
 */
export function openStonesWindow(): void {
  const frame = knotFrame('window-knot');
  const watched: Watched[] = [];
  const cards = h('div', { class: 'stones' });
  const error = h('p', { class: 'error small', role: 'alert' });
  let period = 5;
  let timer: number | undefined;
  let closed = false;

  const watch = (stone: Stone | null): Watched => ({ stone, load: [], inventory: [], day: [], interval: 5, history: 0, showDay: false, card: h('article', { class: 'stone-card' }) });
  watched.push(watch(null), ...stones().map(watch));

  const title = (w: Watched): string => w.status?.name || (w.stone ? new URL(w.stone.address).host : location.host);

  const draw = (w: Watched): void => {
    const status = w.status;
    const samples = w.showDay ? w.day : w.load;
    const step = w.showDay ? 300 : Math.max(1, w.interval);
    const values = (name: string) => samples.map((sample) => sample.values[name] ?? 0);
    const used = latest(w.inventory, 'storageUsed');
    const ceiling = latest(w.inventory, 'storageCeiling');
    const filled = used !== undefined && ceiling ? used / ceiling : undefined;
    const full = fullAt(w.inventory);
    const counted = utc(w.inventory[w.inventory.length - 1]?.time);
    const share = latest(w.load, 'processShare');
    const system = latest(w.load, 'systemLoad');
    const processors = latest(w.load, 'processors') ?? 1;
    const memoryUsed = latest(w.load, 'memoryUsed');
    const memoryMax = latest(w.load, 'memoryMax');
    const requests = latest(w.load, 'requests') ?? 0;
    const failed = latest(w.load, 'failed') ?? 0;
    const millis = latest(w.load, 'requestMillis');
    const database = latest(w.load, 'databaseMillis');
    const remove = w.stone
      ? h('button', { type: 'button', class: 'icon', title: t('Remove from this list'), 'aria-label': t('Remove from this list'), onclick: () => {
        if (confirm(t('Remove {stone} from this list? Its status key stays valid on the stone.', { stone: title(w) }))) {
          watched.splice(watched.indexOf(w), 1);
          keepStones(watched.flatMap((x) => (x.stone ? [x.stone] : [])));
          order();
        }
      } }, '✕')
      : null;
    const state = w.problem === 'refused' ? h('span', { class: 'stone-state near' }, w.stone ? t('refuses the key') : t('refuses the session'))
      : w.problem ? h('span', { class: 'stone-state near' }, t('does not answer'))
        : !status ? h('span', { class: 'stone-state' }, t('asking…'))
          : status.awake ? h('span', { class: 'stone-state fine' }, t('awake'))
            : h('span', { class: 'stone-state watch' }, t('asleep: waits for its MasterKey'));
    const head = h('header', {}, h('h3', {}, title(w)), w.stone ? null : h('span', { class: 'chamber-mark' }, t('this stone')), state, remove);
    if (!status) {
      w.card.replaceChildren(head, w.problem
        ? h('p', { class: 'muted small' }, w.problem === 'refused'
          ? t('The stone refuses the key: it was withdrawn, or it is not a status key of this stone.')
          : t('The stone gave no answer. Its address may be wrong, it may be stopped, or it does not allow this Rune to ask (its setting for allowed origins).'))
        : h('p', { class: 'muted small' }, w.stone?.address ?? ''));
      return;
    }
    const started = utc(status.started);
    const about = [
      status.version ? t('EDS {version}', { version: status.version }) : '',
      status.database ? (status.persistent === false ? t('{database}, a test system', { database: status.database }) : status.database) : '',
      started ? t('running since {time}', { time: when(started) }) : '',
      w.stone ? w.stone.address : '',
    ].filter(Boolean).join(' · ');
    const storage = filled !== undefined
      ? h('div', { class: `storage ${level(filled, 0.8, 0.95)}` },
        h('div', { class: 'storage-text' }, h('span', {}, t('Storage')), h('strong', {}, t('{used} of {ceiling} ({percent} %)', { used: formatSize(used), ceiling: formatSize(ceiling), percent: formatNumber(filled * 100, 1) }))),
        h('progress', { max: 1, value: Math.min(1, filled) }),
        h('span', { class: 'muted small' }, t('Counted {time}; the stone counts what it holds every {n} minutes.', { time: when(counted), n: formatNumber(Math.round((status.inventory?.interval ?? 3600) / 60)) }),
          full !== null ? ` ${t('Full around {time}, if it fills as it did.', { time: when(full) })}` : ''))
      : h('p', { class: 'muted small' }, t('The stone has not counted what it stores yet.'));
    const count = (name: string) => { const value = latest(w.inventory, name); return value === undefined ? '–' : formatNumber(value); };
    const measures = h('div', { class: 'measures' },
      spark(t('People active'), values('accounts'), formatNumber(latest(w.load, 'accounts') ?? 0)),
      spark(t('Requests per second'), values('requests').map((value) => value / step), formatNumber(requests / Math.max(1, w.interval), 1)),
      spark(t('Stone\'s share of the processors'), values('processShare'), share === undefined ? '–' : `${formatNumber(share * 100, 0)} %`, level(share, 0.7, 0.9), 1),
      spark(t('Load of the host'), values('systemLoad'), system === undefined || system < 0 ? '–' : t('{load} on {n} processors', { load: formatNumber(system, 2), n: formatNumber(processors) }),
        level(system === undefined || system < 0 ? undefined : system / Math.max(1, processors), 1, 2)),
      spark(t('Memory'), values('memoryUsed'), memoryUsed === undefined ? '–' : memoryMax ? t('{used} of {max}', { used: formatSize(memoryUsed), max: formatSize(memoryMax) }) : formatSize(memoryUsed),
        level(memoryUsed !== undefined && memoryMax ? memoryUsed / memoryMax : undefined, 0.8, 0.95), memoryMax),
      spark(t('Time a request takes'), values('requestMillis'), millis === undefined ? '–' : t('{n} ms', { n: formatNumber(millis, millis < 10 ? 1 : 0) }), level(millis, 500, 2000)),
      number(t('Refused or failed'), formatNumber(failed), level(requests > 0 ? failed / requests : undefined, 0.05, 0.2)),
      number(t('Answer of the database'), database === undefined ? '–' : t('{n} ms', { n: formatNumber(database, database < 10 ? 1 : 0) }), level(database, 50, 250)),
      number(t('Connections'), formatNumber(latest(w.load, 'connections') ?? 0)),
      number(t('Accounts'), count('accounts')), number(t('Rooms'), count('circles')), number(t('Files'), count('files')),
      number(t('Entries of features'), count('entries')), number(t('Sessions'), count('sessions')));
    const day = h('button', { type: 'button', class: 'link small', onclick: () => {
      w.showDay = !w.showDay;
      if (w.showDay) {
        w.history = 0; // The day is asked for at once.
        void ask(w).then(() => draw(w));
      }
      draw(w);
    } }, w.showDay ? t('Show the recent load') : t('Show the last 24 hours'));
    const trouble = w.problem
      ? h('p', { class: 'error small' }, w.problem === 'refused' ? t('The stone now refuses the key.') : t('The stone does not answer. It last did {time}.', { time: when(w.answered ?? null) }))
      : null;
    w.card.replaceChildren(head, h('p', { class: 'muted small' }, about), ...(trouble ? [trouble] : []), storage, measures,
      h('p', { class: 'muted small' }, w.interval === 0 && !w.showDay ? t('This stone does not gather its load (the interval is 0).')
        : w.showDay ? t('The curves show the last 24 hours, in steps of five minutes.')
          : tn(w.interval, 'The curves show the recent load, a sample every second.', 'The curves show the recent load, a sample every {n} seconds.'), ' ', day));
  };

  /** The busiest first; a stone which doesn't answer is told at the top. */
  const order = (): void => {
    const rank = (w: Watched) => (w.problem ? 2 : 0) + (w.status ? busy(w.load) : 0);
    cards.replaceChildren(...[...watched].sort((a, b) => rank(b) - rank(a)).map((w) => w.card));
  };

  const ask = async (w: Watched): Promise<void> => {
    const history = Date.now() - w.history > 300000;
    try {
      const status = await askStatus(w.stone, w.load.length > 0 ? w.load[w.load.length - 1].sequence : undefined, history, Math.max(4000, period * 1500));
      w.status = status;
      w.problem = undefined;
      w.answered = Date.now();
      w.interval = status.load?.interval ?? w.interval;
      w.load = merge(w.load, status.load?.samples);
      w.inventory = history ? status.inventory?.samples ?? w.inventory : merge(w.inventory, status.inventory?.samples);
      if (history) {
        w.day = status.day?.samples ?? [];
        w.history = Date.now();
      }
    } catch (e) {
      w.problem = e instanceof StatusError ? e.kind : 'failed';
    }
  };

  const round = async (): Promise<void> => {
    await Promise.all(watched.map((w) => ask(w)));
    if (closed) {
      return;
    }
    watched.forEach(draw);
    order();
    timer = window.setTimeout(() => void round(), period * 1000);
  };

  const pace = h('select', { 'aria-label': t('How often the stones are asked') },
    ...[1, 5, 30].map((seconds) => h('option', { value: String(seconds), selected: seconds === period }, tn(seconds, 'every second', 'every {n} seconds'))));
  pace.addEventListener('change', () => {
    period = Number(pace.value);
    window.clearTimeout(timer);
    void round();
  });
  const address = h('input', { required: true, placeholder: 'https://stone.example', 'aria-label': t('Address of the stone'), autocomplete: 'off' });
  const key = h('input', { required: true, placeholder: t('Status key'), 'aria-label': t('Status key'), autocomplete: 'off', type: 'password' });
  const add = h('form', { class: 'feature-add' }, address, key, h('button', { type: 'submit', class: 'secondary' }, t('Add a stone')));
  add.addEventListener('submit', (event) => {
    event.preventDefault();
    error.textContent = '';
    const api = apiAddress(address.value);
    if (!api) {
      error.textContent = t('This is not the address of a stone.');
      return;
    }
    if (watched.some((w) => w.stone?.address === api)) {
      error.textContent = t('This stone is in the list already.');
      return;
    }
    const w = watch({ address: api, key: key.value.trim() });
    watched.push(w);
    keepStones(watched.flatMap((x) => (x.stone ? [x.stone] : [])));
    address.value = '';
    key.value = '';
    draw(w);
    order();
    void ask(w).then(() => { draw(w); order(); });
  });

  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const dialog = h('dialog', { class: 'knot-window', 'aria-label': t('Stones') },
    frame.element,
    h('div', { class: 'window-body' },
      h('header', {}, h('h2', {}, t('Stones')), h('label', { class: 'pace small' }, t('Asked'), pace), close),
      h('div', { class: 'stones-body' },
        cards,
        h('section', { class: 'setting-group' },
          h('h3', {}, t('A further stone')),
          h('p', { class: 'muted small' }, t('Its System Administrator creates a status key in its stone settings, and names the address of this Rune ({origin}) among the allowed origins there. The key reads the status and nothing else; it is kept in this browser.', { origin: location.origin })),
          error, add))));
  const sizes = new ResizeObserver(() => frame.fit(dialog.offsetWidth, dialog.offsetHeight));
  sizes.observe(dialog);
  dialog.addEventListener('close', () => {
    closed = true;
    window.clearTimeout(timer);
    sizes.disconnect();
    dialog.remove();
  });
  document.body.append(dialog);
  dialog.showModal();
  watched.forEach(draw);
  order();
  void round();
}
