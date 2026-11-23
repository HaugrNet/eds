import { h } from '../dom';
import { clockOptions, language, t } from '../i18n';
import { socket } from '../socket';
import { addEntry, allEntries, changeEntry, type FeatureEntry } from '../features/features';
import {
  addDays, deviceZone, entryName, instant, localWall, monthPrefix, newUid, occurrences, parseCalendar, parseEvent, REPEATING, writeCalendar, writeEvent,
  type CalendarEvent, type EventBody, type Every,
} from '../features/calendar';
import { formDialog } from './form-dialog';
import { FeaturePane } from './feature-pane';
import { calls } from '../calls';
import { startRoomCall } from './call-panel';

const FEATURE = 'calendar';
type View = 'month' | 'week' | 'list';

interface Known { entry: FeatureEntry<EventBody>; event: CalendarEvent }
interface Shown { known: Known; start: Date; end: Date }

function dayOf(date: Date): string {
  return localWall(date).slice(0, 10);
}

function dateOf(wall: string): Date {
  return instant(wall.slice(0, 10));
}

/** The first day of the week where the person's language is spoken: 1 Monday to 7 Sunday. */
function firstWeekday(): number {
  try {
    const locale = new Intl.Locale(language()) as Intl.Locale & { getWeekInfo?: () => { firstDay: number }; weekInfo?: { firstDay: number } };
    return locale.getWeekInfo?.().firstDay ?? locale.weekInfo?.firstDay ?? 1;
  } catch {
    return 1;
  }
}

/** The first day of the week a day is in. */
function weekStart(wall: string): string {
  const weekday = dateOf(wall).getDay() || 7;
  return addDays(wall.slice(0, 10), -((weekday - firstWeekday() + 7) % 7));
}

/**
 * The calendar of a room (FEATURES.md, "Calendar"): its events by month, by
 * week or as a list, with place and time, and `.ics` in and out. An event
 * is one entry holding a VEVENT of iCalendar (features/calendar.ts).
 */
export class EdsCalendar extends FeaturePane {
  protected readonly feature = FEATURE;
  private view: View = 'month';
  /** A day of what is shown. */
  private anchor = dayOf(new Date());
  private known: Known[] = [];

  protected leaving(): void {
    this.known = [];
  }

  /** The days shown: from the first to the day after the last. */
  private span(): [string, string] {
    if (this.view === 'week') {
      const from = weekStart(this.anchor);
      return [from, addDays(from, 7)];
    }
    if (this.view === 'list') {
      return [this.anchor, addDays(this.anchor, 60)];
    }
    const first = `${this.anchor.slice(0, 8)}01`;
    const from = weekStart(first);
    const next = dateOf(first);
    next.setMonth(next.getMonth() + 1);
    const last = weekStart(addDays(dayOf(next), -1));
    return [from, addDays(last, 7)];
  }

  protected async load(): Promise<void> {
    const circleId = this.circleId;
    const [from, to] = this.span();
    // The months shown and the one before, for what began earlier; and all which repeat.
    const prefixes = new Set<string>([REPEATING]);
    const month = dateOf(from);
    month.setDate(1);
    month.setMonth(month.getMonth() - 1);
    while (dayOf(month) < to) {
      prefixes.add(monthPrefix(month.getFullYear(), month.getMonth() + 1));
      month.setMonth(month.getMonth() + 1);
    }
    const pages = await Promise.all([...prefixes].map((prefix) => allEntries<EventBody>(circleId, FEATURE, 'event', undefined, prefix)));
    if (circleId !== this.circleId) {
      return;
    }
    this.known = pages.flat().flatMap((entry) => {
      const event = entry.body && typeof entry.body.ics === 'string' ? parseEvent(entry.body.ics) : null;
      return event ? [{ entry, event }] : [];
    });
    this.render();
  }

  private time(at: Date): string {
    return at.toLocaleTimeString(language(), { hour: '2-digit', minute: '2-digit', ...clockOptions() });
  }

  private render(): void {
    const [from, to] = this.span();
    const shown: Shown[] = this.known.flatMap((known) => occurrences(known.event, dateOf(from), dateOf(to)).map((o) => ({ known, start: o.start, end: o.end })))
      .sort((a, b) => a.start.getTime() - b.start.getTime());
    // What takes place on each day; an event over several days is on each of them.
    const days = new Map<string, Shown[]>();
    for (const one of shown) {
      const last = new Date(Math.max(one.start.getTime(), one.end.getTime() - 1));
      for (let d = dayOf(one.start) < from ? from : dayOf(one.start); d <= dayOf(last) && d < to; d = addDays(d, 1)) {
        days.set(d, [...(days.get(d) ?? []), one]);
      }
    }
    const move = (direction: number) => {
      if (this.view === 'month') {
        const month = dateOf(`${this.anchor.slice(0, 8)}01`);
        month.setMonth(month.getMonth() + direction);
        this.anchor = dayOf(month);
      } else {
        this.anchor = addDays(this.anchor, direction * (this.view === 'week' ? 7 : 60));
      }
      void this.run(() => this.load());
    };
    const title = this.view === 'month'
      ? dateOf(this.anchor).toLocaleDateString(language(), { month: 'long', year: 'numeric' })
      : `${dateOf(from).toLocaleDateString(language(), { dateStyle: 'medium' })} – ${dateOf(addDays(to, -1)).toLocaleDateString(language(), { dateStyle: 'medium' })}`;
    const views = h('select', { 'aria-label': t('View') },
      ...([['month', t('Month')], ['week', t('Week')], ['list', t('List')]] as [View, string][]).map(([value, label]) => h('option', { value, selected: value === this.view }, label)));
    views.addEventListener('change', () => {
      this.view = views.value as View;
      void this.run(() => this.load());
    });
    const file = h('input', { type: 'file', accept: '.ics,text/calendar', hidden: true });
    file.addEventListener('change', () => void this.bringIn(file.files?.[0]));
    const bar = h('div', { class: 'toolbar' },
      h('button', { type: 'button', class: 'icon', 'aria-label': t('Earlier'), onclick: () => move(-1) }, '‹'),
      h('button', { type: 'button', class: 'secondary small', onclick: () => { this.anchor = dayOf(new Date()); void this.run(() => this.load()); } }, t('Today')),
      h('button', { type: 'button', class: 'icon', 'aria-label': t('Later'), onclick: () => move(1) }, '›'),
      h('strong', { class: 'grow calendar-title' }, title), views,
      this.mayWrite() ? h('button', { type: 'button', onclick: () => this.edit(null, this.anchor) }, t('New event')) : null,
      this.mayWrite() ? h('button', { type: 'button', class: 'secondary small', title: t('Bring in events from an .ics file'), onclick: () => file.click() }, t('Import')) : null,
      h('button', { type: 'button', class: 'secondary small', title: t('Take all events of this room out as an .ics file'), onclick: () => void this.run(() => this.takeOut()) }, t('Export')), file);

    const chip = (one: Shown, d: string) => {
      const event = one.known.event;
      const starts = dayOf(one.start) === d;
      return h('button', { type: 'button', class: `event${event.allDay ? ' whole-day' : ''}`, title: [event.title, event.place].filter(Boolean).join(' · '), onclick: (e: Event) => { e.stopPropagation(); this.edit(one.known); } },
        event.allDay || !starts ? null : h('span', { class: 'event-time' }, this.time(one.start)), ' ',
        event.call ? h('span', { class: 'event-call', title: event.call === 'video' ? t('Video call') : t('Voice call') }, '☎ ') : null, event.title || t('(no title)'));
    };
    const today = dayOf(new Date());
    let content: HTMLElement;
    if (this.view === 'list') {
      const listed = [...days].filter(([d]) => d >= this.anchor).sort(([a], [b]) => (a < b ? -1 : 1));
      content = listed.length === 0
        ? h('p', { class: 'muted' }, t('Nothing in these days.'))
        : h('div', { class: 'agenda' }, ...listed.map(([d, events]) => h('section', {},
          h('h4', { class: d === today ? 'today' : '' }, dateOf(d).toLocaleDateString(language(), { weekday: 'long', day: 'numeric', month: 'long' })),
          ...events.map((one) => h('div', { class: 'agenda-row' }, chip(one, d),
            h('span', { class: 'muted small' }, [one.known.event.allDay ? t('all day') : `${this.time(one.start)} – ${this.time(one.end)}`, one.known.event.place].filter(Boolean).join(' · ')))))));
    } else {
      const cells: HTMLElement[] = [];
      for (let d = from; d < to; d = addDays(d, 1)) {
        const events = days.get(d) ?? [];
        const add = this.mayWrite() ? () => this.edit(null, d) : undefined;
        // How many events don't fit the day; shown by fit(), with the month.
        const more = h('button', { type: 'button', class: 'link small more', hidden: true, onclick: (e: Event) => { e.stopPropagation(); this.view = 'list'; this.anchor = d; void this.run(() => this.load()); } });
        cells.push(h('div', { class: `day${d === today ? ' today' : ''}${this.view === 'month' && d.slice(0, 7) !== this.anchor.slice(0, 7) ? ' other-month' : ''}`, onclick: add },
          h('div', { class: 'day-head' },
            h('span', { class: 'day-number' }, this.view === 'week' ? dateOf(d).toLocaleDateString(language(), { weekday: 'short', day: 'numeric', month: 'short' }) : String(Number(d.slice(8)))),
            more),
          ...events.slice(0, 50).map((one) => chip(one, d))));
      }
      const names = this.view === 'month'
        ? Array.from({ length: 7 }, (_, i) => h('div', { class: 'weekday' }, dateOf(addDays(from, i)).toLocaleDateString(language(), { weekday: 'short' })))
        : [];
      content = h('div', { class: `calendar-grid ${this.view}` }, ...names, ...cells);
      content.style.setProperty('--weeks', String(cells.length / 7));
    }
    this.body.replaceChildren(bar, content);
    // The month fills the pane, in width and height; its days show as many events as they have room for.
    this.classList.toggle('fills', this.view === 'month');
    this.sized.disconnect();
    if (this.view === 'month') {
      this.sized.observe(content);
      this.fit(content);
    }
  }

  /** Follows the size of the month shown. */
  private sized = new ResizeObserver((entries) => entries.forEach((entry) => this.fit(entry.target as HTMLElement)));

  /**
   * Shows in each day of the month the events it has room for, below its
   * number, and beside the number how many more there are.
   */
  private fit(grid: HTMLElement): void {
    for (const cell of grid.querySelectorAll<HTMLElement>('.day')) {
      const chips = [...cell.querySelectorAll<HTMLElement>('button.event')];
      const more = cell.querySelector<HTMLElement>('.more');
      const head = cell.querySelector<HTMLElement>('.day-head');
      if (chips.length === 0 || !more || !head) {
        continue;
      }
      chips[0].hidden = false;
      const style = getComputedStyle(cell);
      const gap = parseFloat(style.rowGap) || 0;
      const room = cell.clientHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom) - head.offsetHeight;
      const shown = Math.min(chips.length, Math.max(0, Math.floor(room / (chips[0].offsetHeight + gap))));
      chips.forEach((one, i) => { one.hidden = i >= shown; });
      const hidden = chips.length - shown;
      more.hidden = hidden === 0;
      more.textContent = `+${hidden}`;
      more.title = t('{n} more', { n: hidden });
    }
  }

  /** A new event on a day, or a known one to change. */
  private edit(known: Known | null, onDay?: string): void {
    const old = known?.event;
    const write = this.mayWrite();
    const zone = old?.zone ?? deviceZone();
    const start = old?.start ?? `${onDay ?? dayOf(new Date())}T${localWall(new Date(Date.now() + 3600000)).slice(11, 13)}:00`;
    const end = old?.end ?? `${start.slice(0, 11)}${String(Math.min(23, Number(start.slice(11, 13)) + 1)).padStart(2, '0')}:00`;
    const title = h('input', { required: true, maxlength: 300, value: old?.title ?? '', disabled: !write });
    const allDay = h('input', { type: 'checkbox', checked: old?.allDay ?? false, disabled: !write });
    const startDay = h('input', { type: 'date', required: true, value: start.slice(0, 10), disabled: !write });
    const startTime = h('input', { type: 'time', value: start.slice(11) || '09:00', disabled: !write });
    const endDay = h('input', { type: 'date', required: true, value: old?.allDay ? addDays(end, -1) : end.slice(0, 10), disabled: !write });
    const endTime = h('input', { type: 'time', value: end.slice(11) || '10:00', disabled: !write });
    const place = h('input', { maxlength: 300, value: old?.place ?? '', disabled: !write });
    const note = h('textarea', { rows: 3, maxlength: 4000, disabled: !write }, old?.note ?? '');
    const every = h('select', { disabled: !write },
      h('option', { value: '', selected: !old?.repeat }, old?.rule ? t('By a rule Rune does not show') : t('Does not repeat')),
      ...([['daily', t('Daily')], ['weekly', t('Weekly')], ['monthly', t('Monthly')], ['yearly', t('Yearly')]] as [Every, string][])
        .map(([value, label]) => h('option', { value, selected: old?.repeat?.every === value }, label)));
    const interval = h('input', { type: 'number', min: 1, max: 99, value: String(old?.repeat?.interval ?? 1), class: 'share', disabled: !write });
    const until = h('input', { type: 'date', value: old?.repeat?.until ?? '', disabled: !write });
    const repeating = h('div', { class: 'two-columns' }, h('label', {}, t('Each n-th time'), interval), h('label', {}, t('Until (may be left empty)'), until));
    // The event can be a call of the room (FEATURES.md, "Presence and calls"): everyone whose Rune is open is told when it is due.
    const hasCalls = calls.usable(this.circleId);
    const call = h('select', { disabled: !write },
      ...([['', t('No call')], ['audio', t('Voice call')], ['video', t('Video call')]] as [string, string][])
        .map(([value, label]) => h('option', { value, selected: (old?.call ?? '') === value }, label)));
    const callField = hasCalls || old?.call
      ? h('label', {}, t('Call'), call, h('span', { class: 'muted small' }, hasCalls
        ? t('Everyone in the room whose Rune is open is told ten minutes before and when it starts.')
        : t('This room has no calls. An administrator of the room can switch them on under "Features".')))
      : null;
    const circleId = this.circleId;
    const joinCall = known && old?.call && hasCalls && calls.supported()
      ? h('button', { type: 'button', class: 'secondary', disabled: !!calls.active(), onclick: () => { dialog.close(); void startRoomCall(circleId, old.call ?? 'audio'); } }, t('Join the call'))
      : null;
    const times = () => {
      startTime.hidden = endTime.hidden = allDay.checked;
      repeating.hidden = !every.value;
    };
    allDay.addEventListener('change', times);
    every.addEventListener('change', times);
    startDay.addEventListener('change', () => { if (endDay.value < startDay.value) { endDay.value = startDay.value; } });
    times();
    const remove = known && write
      ? h('button', { type: 'button', class: 'link danger', onclick: () => {
        if (confirm(old?.repeat || old?.rule ? t('Remove "{title}" and all its repetitions, for everyone?', { title: old?.title ?? '' }) : t('Remove "{title}", for everyone?', { title: old?.title ?? '' }))) {
          void this.run(() => socket.remove(this.circleId, FEATURE, known.entry.entryId));
          dialog.close();
        }
      } }, t('Remove'))
      : null;
    const dialog = formDialog(known ? (write ? t('Edit the event') : t('Event')) : t('New event'), [
      h('label', {}, t('Title'), title),
      h('label', { class: 'check' }, allDay, t('All day')),
      h('div', { class: 'two-columns' }, h('label', {}, t('Starts'), startDay, startTime), h('label', {}, t('Ends'), endDay, endTime)),
      old?.zone && old.zone !== deviceZone() ? h('p', { class: 'muted small' }, t('The times are those of {zone}, where the event was made.', { zone: old.zone })) : null,
      h('label', {}, t('Place'), place),
      h('label', {}, t('Note'), note),
      h('label', {}, t('Repeats'), every), repeating,
      callField,
      old?.rule ? h('p', { class: 'muted small' }, t('This event repeats by a rule Rune does not show ({rule}); here it is shown once. The rule is kept unless you choose another.', { rule: old.rule })) : null,
      known ? h('p', { class: 'muted small' }, t('Entered by {name}.', { name: this.nameOf(known.entry.memberId) })) : null,
    ], known ? t('Save') : t('Add'), async () => {
      if (!write) {
        return;
      }
      const event: CalendarEvent = {
        uid: old?.uid ?? newUid(), title: title.value.trim(), allDay: allDay.checked,
        start: allDay.checked ? startDay.value : `${startDay.value}T${startTime.value || '00:00'}`,
        end: allDay.checked ? addDays(endDay.value < startDay.value ? startDay.value : endDay.value, 1) : `${endDay.value}T${endTime.value || '00:00'}`,
        other: old?.other ?? [],
      };
      if (!event.allDay) {
        event.zone = zone;
        if (event.end < event.start) {
          throw new Error(t('The event ends before it starts.'));
        }
      }
      if (place.value.trim()) {
        event.place = place.value.trim();
      }
      if (note.value.trim()) {
        event.note = note.value.trim();
      }
      if (every.value) {
        event.repeat = { every: every.value as Every, interval: Math.max(1, Math.floor(Number(interval.value) || 1)) };
        if (until.value) {
          event.repeat.until = until.value;
        } else if (old?.repeat?.count && old.repeat.every === event.repeat.every) {
          event.repeat.count = old.repeat.count;
        }
      } else if (old?.rule) {
        event.rule = old.rule;
      }
      if (call.value === 'audio' || call.value === 'video') {
        event.call = call.value;
      }
      await this.store(event, known?.entry.entryId);
    }, [joinCall, remove]);
    if (!write) {
      dialog.querySelector<HTMLButtonElement>('button[type=submit]')?.setAttribute('hidden', '');
    }
  }

  private async store(event: CalendarEvent, entryId?: string): Promise<void> {
    const body: EventBody = { v: 1, ics: writeEvent(event) };
    if (entryId) {
      await changeEntry(this.circleId, FEATURE, 'event', entryId, body, entryName(event));
    } else {
      await addEntry(this.circleId, FEATURE, 'event', body, undefined, entryName(event));
    }
  }

  /** Brings the events of an `.ics` file into the room. */
  private async bringIn(file: File | undefined): Promise<void> {
    if (!file) {
      return;
    }
    await this.run(async () => {
      const events = parseCalendar(await file.text());
      if (events.length === 0) {
        throw new Error(t('No events were found in "{name}".', { name: file.name }));
      }
      if (!confirm(t('Bring {n} events from "{name}" into this room?', { n: events.length, name: file.name }))) {
        return;
      }
      // An event which is here already, by its identifier, is replaced.
      const here = await allEntries<EventBody>(this.circleId, FEATURE, 'event');
      const byUid = new Map(here.flatMap((entry) => {
        const event = entry.body ? parseEvent(entry.body.ics) : null;
        return event ? [[event.uid, entry.entryId] as [string, string]] : [];
      }));
      for (const event of events) {
        await this.store(event, byUid.get(event.uid));
      }
      this.status.textContent = t('{n} events were brought in.', { n: events.length });
    });
  }

  /** Hands all events of the room out as an `.ics` file. */
  private async takeOut(): Promise<void> {
    const all = await allEntries<EventBody>(this.circleId, FEATURE, 'event');
    const events = all.flatMap((entry) => {
      const event = entry.body ? parseEvent(entry.body.ics) : null;
      return event ? [event] : [];
    });
    const url = URL.createObjectURL(new Blob([writeCalendar(events)], { type: 'text/calendar' }));
    const link = h('a', { href: url, download: `${(this.circle?.circleName ?? 'calendar').replace(/[^\p{L}\p{N} _.-]/gu, '_')}.ics` });
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  }
}

customElements.define('eds-calendar', EdsCalendar);
