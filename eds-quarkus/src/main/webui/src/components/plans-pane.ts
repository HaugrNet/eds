import { TrustLevel } from '../api';
import { h } from '../dom';
import { formatNumber, language, t } from '../i18n';
import { socket } from '../socket';
import { addEntry, allEntries, changeEntry, usable, type FeatureEntry } from '../features/features';
import { addDays, entryName, writeEvent, type CalendarEvent, type EventBody } from '../features/calendar';
import {
  amountText, balances, confirms, guestPerson, memberPerson, minor, personMember, settle,
  type ConfirmationBody, type ExpenseBody, type ItemBody, type PaymentBody, type Person, type PlanBody, type Share,
} from '../features/plans';
import { formDialog } from './form-dialog';
import { FeaturePane } from './feature-pane';

const FEATURE = 'plans';

function today(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

function showDay(day: string | undefined): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(day ?? '');
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])).toLocaleDateString(language(), { dateStyle: 'medium' }) : '';
}

function money(amount: string, currency: string): string {
  return `${formatNumber((minor(amount.replace('-', '')) ?? 0) / 100, 2)} ${currency}`;
}

/**
 * The plans of a room (FEATURES.md, "Plans"): trips and events, each with what
 * is to do, what was spent, what everyone owes or is owed, and the payments
 * which settle it. The working out is Rune's (features/plans.ts); the stone
 * keeps the entries and never sees an amount as such.
 */
export class EdsPlans extends FeaturePane {
  protected readonly feature = FEATURE;
  private plans: FeatureEntry<PlanBody>[] = [];
  private selected = '';

  protected leaving(): void {
    this.selected = '';
  }

  protected async load(): Promise<void> {
    const circleId = this.circleId;
    this.plans = (await allEntries<PlanBody>(circleId, FEATURE, 'plan')).filter((plan) => plan.body);
    if (circleId !== this.circleId) {
      return;
    }
    const plan = this.plans.find((p) => p.entryId === this.selected);
    if (plan) {
      await this.showPlan(plan);
    } else {
      this.selected = '';
      this.showList();
    }
  }

  private person(person: Person): string {
    const memberId = personMember(person);
    return memberId ? this.nameOf(memberId) : t('{name} (guest)', { name: person.slice(2) });
  }

  private showList(): void {
    const add = this.mayWrite() ? h('button', { type: 'button', onclick: () => this.editPlan() }, t('New plan')) : null;
    this.body.replaceChildren(
      h('div', { class: 'toolbar' }, add),
      this.plans.length === 0
        ? h('p', { class: 'muted' }, t('No plans yet. A plan is a trip or an event: what is to do, what it costs, and who owes whom.'))
        : h('ul', { class: 'feature-list' }, ...this.plans.map((plan) => {
          const body = plan.body as PlanBody;
          return h('li', {}, h('button', { type: 'button', class: 'feature-row-open', onclick: () => { this.selected = plan.entryId; void this.run(() => this.showPlan(plan)); } },
            h('strong', {}, body.title),
            h('span', { class: 'muted small' }, [body.from && body.to && body.to !== body.from ? `${showDay(body.from)} – ${showDay(body.to)}` : showDay(body.from), body.place].filter(Boolean).join(' · ')),
            plan.closed ? h('span', { class: 'chamber-mark' }, t('Closed')) : null));
        })));
  }

  private async showPlan(plan: FeatureEntry<PlanBody>): Promise<void> {
    const circleId = this.circleId;
    const body = plan.body as PlanBody;
    const [items, expenses, payments, confirmations] = await Promise.all([
      allEntries<ItemBody>(circleId, FEATURE, 'item', plan.entryId),
      allEntries<ExpenseBody>(circleId, FEATURE, 'expense', plan.entryId),
      allEntries<PaymentBody>(circleId, FEATURE, 'payment', plan.entryId),
      allEntries<ConfirmationBody>(circleId, FEATURE, 'confirmation'),
    ]);
    if (circleId !== this.circleId || this.selected !== plan.entryId) {
      return;
    }
    // A plan's days can go into the calendar of the room, if it has one which takes entries.
    const calendar = (this.level === TrustLevel.Write || this.level === TrustLevel.Admin) && usable(await socket.join(circleId), 'calendar', true);
    const write = this.mayWrite() && !plan.closed;
    const mine = (entry: FeatureEntry<unknown>) => entry.memberId === this.me();
    const remove = (entry: FeatureEntry<unknown>, question: string) => h('button', { type: 'button', class: 'icon', title: t('Remove'), 'aria-label': t('Remove'), onclick: () => {
      if (confirm(question)) {
        void this.run(() => socket.remove(circleId, FEATURE, entry.entryId));
      }
    } }, '✕');

    // The plan itself.
    const administrator = this.level === TrustLevel.Admin;
    const head = h('div', { class: 'feature-head' },
      h('button', { type: 'button', class: 'link', onclick: () => { this.selected = ''; this.showList(); } }, t('‹ All plans')),
      h('h3', {}, body.title, plan.closed ? h('span', { class: 'chamber-mark' }, t('Closed')) : null),
      h('p', { class: 'muted small' }, [body.from && body.to && body.to !== body.from ? `${showDay(body.from)} – ${showDay(body.to)}` : showDay(body.from), body.place,
        body.people.map((p) => this.person(p)).join(', ')].filter(Boolean).join(' · ')),
      h('div', { class: 'toolbar' },
        write ? h('button', { type: 'button', class: 'secondary small', onclick: () => this.editPlan(plan) }, t('Edit')) : null,
        body.from && calendar ? h('button', { type: 'button', class: 'secondary small', onclick: () => void this.run(() => this.intoCalendar(plan)) }, t('Put into the calendar')) : null,
        this.mayWrite() && (mine(plan) || administrator)
          ? h('button', { type: 'button', class: 'secondary small', onclick: () => {
            if (plan.closed || confirm(t('Close "{title}"? Its records stay as they are, and nothing can be added or changed until it is opened again.', { title: body.title }))) {
              void this.run(() => socket.closeEntry(circleId, FEATURE, plan.entryId, !plan.closed));
            }
          } }, plan.closed ? t('Open again') : t('Close the plan'))
          : null,
        this.mayWrite() && mine(plan)
          ? h('button', { type: 'button', class: 'link danger', onclick: () => {
            if (confirm(t('Remove "{title}" with everything in it, for everyone?', { title: body.title }))) {
              this.selected = '';
              void this.run(() => socket.remove(circleId, FEATURE, plan.entryId));
            }
          } }, t('Remove'))
          : null));

    // What is to do.
    const itemList = h('ul', { class: 'feature-list' }, ...items.filter((item) => item.body).map((item) => {
      const it = item.body as ItemBody;
      const done = h('input', { type: 'checkbox', checked: it.done, disabled: !write, 'aria-label': it.text });
      done.addEventListener('change', () => void this.run(() => changeEntry(circleId, FEATURE, 'item', item.entryId, { ...it, done: done.checked })));
      return h('li', { class: it.done ? 'done' : '' }, h('label', { class: 'check' }, done, h('span', {}, it.text)),
        h('span', { class: 'muted small' }, it.who ? this.person(it.who) : ''), write ? remove(item, t('Remove "{text}"?', { text: it.text })) : null);
    }));
    const itemText = h('input', { placeholder: t('Something to do, to book or to bring'), maxlength: 500, 'aria-label': t('Something to do, to book or to bring') });
    const itemWho = h('select', { 'aria-label': t('Who takes it') }, h('option', { value: '' }, t('Nobody yet')), ...body.people.map((p) => h('option', { value: p }, this.person(p))));
    const itemForm = h('form', { class: 'feature-add' }, itemText, itemWho, h('button', { type: 'submit', class: 'secondary' }, t('Add')));
    itemForm.addEventListener('submit', (event) => {
      event.preventDefault();
      const text = itemText.value.trim();
      if (text) {
        const item: ItemBody = { v: 1, text, done: false };
        if (itemWho.value) {
          item.who = itemWho.value;
        }
        void this.run(() => addEntry(circleId, FEATURE, 'item', item, plan.entryId));
      }
    });

    // What was spent.
    const readable = expenses.filter((expense) => expense.body && minor(expense.body.amount) !== null && expense.body.split.length > 0);
    const expenseList = h('ul', { class: 'feature-list' }, ...readable.map((expense) => {
      const e = expense.body as ExpenseBody;
      return h('li', {},
        h('span', { class: 'grow' }, h('strong', {}, e.text), ' ', h('span', { class: 'muted small' },
          t('{who} paid, shared by {people}', { who: this.person(e.paidBy), people: e.split.map((s) => this.person(s.who) + (s.share !== 1 ? ` ×${s.share}` : '')).join(', ') }),
          e.at ? ` · ${showDay(e.at)}` : '', expense.changed && expense.changed !== expense.added ? ` · ${t('corrected')}` : '')),
        h('span', { class: 'amount' }, money(e.amount, e.currency)),
        write && mine(expense) ? h('button', { type: 'button', class: 'icon', title: t('Correct'), 'aria-label': t('Correct'), onclick: () => this.editExpense(plan, expense) }, '✎') : null,
        write && mine(expense) ? remove(expense, t('Remove the expense "{text}"? The shares of everyone change.', { text: e.text })) : null);
    }));

    // Who owes whom.
    const paid = payments.filter((payment) => payment.body && minor(payment.body.amount) !== null);
    const worked = balances(readable.map((e) => e.body as ExpenseBody), paid.map((p) => p.body as PaymentBody));
    const balanceParts: HTMLElement[] = [];
    for (const currency of [...worked.keys()].sort()) {
      const of = worked.get(currency) as Map<Person, number>;
      balanceParts.push(h('ul', { class: 'feature-list' }, ...[...of].filter(([, units]) => units !== 0).map(([person, units]) =>
        h('li', {}, h('span', { class: 'grow' }, this.person(person)),
          h('span', { class: units > 0 ? 'amount owed' : 'amount owes' }, units > 0 ? t('gets {amount}', { amount: money(amountText(units), currency) }) : t('owes {amount}', { amount: money(amountText(-units), currency) }))))));
      const dues = settle(of);
      if (dues.length === 0) {
        balanceParts.push(h('p', { class: 'muted small' }, t('{currency}: settled, nobody owes anything.', { currency })));
      }
      balanceParts.push(...dues.map((due) => {
        const fromMember = personMember(due.from);
        const enter = write && (fromMember === this.me() || fromMember === null)
          ? h('button', { type: 'button', class: 'secondary small', onclick: () => this.editPayment(plan, { v: 1, from: due.from, to: due.to, amount: due.amount, currency, at: today() }) },
            fromMember === this.me() ? t('I paid this') : t('Was paid'))
          : null;
        return h('p', { class: 'due' }, t('{from} pays {to} {amount}', { from: this.person(due.from), to: this.person(due.to), amount: money(due.amount, currency) }), ' ', enter);
      }));
    }

    // The payments made, and whether their receivers confirmed them.
    const paymentList = h('ul', { class: 'feature-list' }, ...paid.map((payment) => {
      const p = payment.body as PaymentBody;
      const confirmed = confirmations.some((c) => c.about === payment.entryId && confirms(payment, c));
      const confirmButton = write && personMember(p.to) === this.me() && !confirmed
        ? h('button', { type: 'button', class: 'secondary small', onclick: () => void this.run(() => addEntry(circleId, FEATURE, 'confirmation', { v: 1 }, payment.entryId)) }, t('I got this'))
        : null;
      return h('li', {},
        h('span', { class: 'grow' }, t('{from} paid {to}', { from: this.person(p.from), to: this.person(p.to) }), p.at ? h('span', { class: 'muted small' }, ` · ${showDay(p.at)}`) : null,
          ' ', h('span', { class: confirmed ? 'muted small' : 'small error' }, personMember(p.to) ? (confirmed ? t('confirmed') : t('not confirmed')) : '')),
        h('span', { class: 'amount' }, money(p.amount, p.currency)), confirmButton,
        write && mine(payment) ? remove(payment, t('Remove this payment?')) : null);
    }));

    const shown: (Node | null)[] = [head,
      h('h4', {}, t('To do')), itemList, write ? itemForm : null,
      h('h4', {}, t('Expenses')), expenseList,
      write ? h('div', { class: 'toolbar' }, h('button', { type: 'button', class: 'secondary', onclick: () => this.editExpense(plan) }, t('Add expense'))) : null,
      h('h4', {}, t('Who owes whom')), ...(balanceParts.length > 0 ? balanceParts : [h('p', { class: 'muted small' }, t('Nothing was spent yet.'))]),
      paid.length > 0 ? h('h4', {}, t('Payments')) : null, paid.length > 0 ? paymentList : null,
      write ? h('div', { class: 'toolbar' }, h('button', { type: 'button', class: 'secondary small', onclick: () => this.editPayment(plan) }, t('Enter a payment'))) : null];
    this.body.replaceChildren(...shown.filter((node): node is Node => node !== null));
  }

  /** Puts the days of a plan into the calendar of the room, as an event of whole days; again, it replaces that event. */
  private async intoCalendar(plan: FeatureEntry<PlanBody>): Promise<void> {
    const body = plan.body as PlanBody;
    if (!body.from) {
      return;
    }
    const event: CalendarEvent = {
      uid: `plan-${plan.entryId}@rune`, title: body.title, allDay: true, start: body.from, end: addDays(body.to && body.to > body.from ? body.to : body.from, 1), other: [],
    };
    if (body.place) {
      event.place = body.place;
    }
    const content: EventBody = { v: 1, ics: writeEvent(event) };
    const before = (await allEntries<EventBody>(this.circleId, 'calendar', 'event')).find((entry) => entry.body?.ics.includes(`UID:${event.uid}`));
    if (before) {
      await changeEntry(this.circleId, 'calendar', 'event', before.entryId, content, entryName(event));
    } else {
      await addEntry(this.circleId, 'calendar', 'event', content, undefined, entryName(event));
    }
    this.status.textContent = t('The days of "{title}" are in the calendar of this room.', { title: body.title });
  }

  /** A new plan, or a changed one. */
  private editPlan(plan?: FeatureEntry<PlanBody>): void {
    const old = plan?.body;
    const title = h('input', { required: true, maxlength: 200, value: old?.title ?? '' });
    const from = h('input', { type: 'date', value: old?.from ?? '' });
    const to = h('input', { type: 'date', value: old?.to ?? '' });
    const place = h('input', { maxlength: 200, value: old?.place ?? '' });
    const currency = h('input', { required: true, maxlength: 3, minlength: 3, pattern: '[A-Za-z]{3}', value: old?.currency ?? 'EUR', size: 4 });
    const chosen = new Set(old?.people ?? this.members.map((m) => memberPerson(m.memberId ?? '')));
    const boxes = this.members.map((member) => {
      const person = memberPerson(member.memberId ?? '');
      const box = h('input', { type: 'checkbox', checked: chosen.has(person), value: person });
      return h('label', { class: 'check' }, box, member.accountName ?? '');
    });
    const guests = h('input', { value: (old?.people ?? []).filter((p) => !personMember(p)).map((p) => p.slice(2)).join(', '), placeholder: t('Names, separated by commas') });
    formDialog(plan ? t('Edit the plan') : t('New plan'), [
      h('label', {}, t('Name'), title),
      h('div', { class: 'two-columns' }, h('label', {}, t('From'), from), h('label', {}, t('To'), to)),
      h('label', {}, t('Place'), place),
      h('label', {}, t('Currency'), currency),
      h('fieldset', { class: 'stack' }, h('legend', {}, t('Who takes part')), ...boxes),
      h('label', {}, t('Guests without an account'), guests),
    ], plan ? t('Save') : t('Create'), async () => {
      const people = [
        ...boxes.map((label) => label.querySelector('input') as HTMLInputElement).filter((box) => box.checked).map((box) => box.value),
        ...[...new Set(guests.value.split(',').map((name) => name.trim()).filter(Boolean))].map(guestPerson),
      ];
      if (people.length === 0) {
        throw new Error(t('Someone has to take part.'));
      }
      const body: PlanBody = { v: 1, title: title.value.trim(), currency: currency.value.toUpperCase(), people };
      if (from.value) {
        body.from = from.value;
      }
      if (to.value || from.value) {
        body.to = to.value && to.value >= from.value ? to.value : from.value;
      }
      if (place.value.trim()) {
        body.place = place.value.trim();
      }
      if (plan) {
        await changeEntry(this.circleId, FEATURE, 'plan', plan.entryId, body);
      } else {
        this.selected = (await addEntry<PlanBody>(this.circleId, FEATURE, 'plan', body)).entryId;
        this.reloadSoon();
      }
    });
  }

  /** A new expense, or the author's correction of one. */
  private editExpense(plan: FeatureEntry<PlanBody>, expense?: FeatureEntry<ExpenseBody>): void {
    const body = plan.body as PlanBody;
    const old = expense?.body;
    const people = [...new Set([...body.people, ...(old ? [old.paidBy, ...old.split.map((s) => s.who)] : [])])];
    const text = h('input', { required: true, maxlength: 300, value: old?.text ?? '' });
    const amount = h('input', { required: true, inputmode: 'decimal', value: old?.amount ?? '', placeholder: '0.00', size: 8 });
    const currency = h('input', { required: true, maxlength: 3, minlength: 3, pattern: '[A-Za-z]{3}', value: old?.currency ?? body.currency, size: 4 });
    const me = memberPerson(this.me());
    const paidBy = h('select', {}, ...people.map((p) => h('option', { value: p, selected: p === (old?.paidBy ?? me) }, this.person(p))));
    const at = h('input', { type: 'date', value: old?.at ?? today() });
    const shares = people.map((person) => {
      const share = old ? old.split.find((s) => s.who === person)?.share ?? 0 : 1;
      const box = h('input', { type: 'checkbox', checked: share > 0, value: person });
      const number = h('input', { type: 'number', min: 1, max: 1000, step: 1, value: String(share || 1), 'aria-label': t('Shares of {name}', { name: this.person(person) }), class: 'share' });
      return { person, box, number, row: h('div', { class: 'share-row' }, h('label', { class: 'check' }, box, this.person(person)), number) };
    });
    formDialog(expense ? t('Correct the expense') : t('Add expense'), [
      h('label', {}, t('What for'), text),
      h('div', { class: 'two-columns' }, h('label', {}, t('Amount'), amount), h('label', {}, t('Currency'), currency)),
      h('div', { class: 'two-columns' }, h('label', {}, t('Paid by'), paidBy), h('label', {}, t('Day'), at)),
      h('fieldset', { class: 'stack' }, h('legend', {}, t('Shared by')), ...shares.map((s) => s.row),
        h('p', { class: 'muted small' }, t('Everyone ticked bears a share; a 2 bears twice as much as a 1.'))),
    ], expense ? t('Save') : t('Add'), async () => {
      const units = minor(amount.value);
      if (units === null || units === 0) {
        throw new Error(t('The amount is a number with at most two decimals, such as 12.50.'));
      }
      const split: Share[] = shares.filter((s) => s.box.checked).map((s) => ({ who: s.person, share: Math.max(1, Math.floor(Number(s.number.value) || 1)) }));
      if (split.length === 0) {
        throw new Error(t('Someone has to share the expense.'));
      }
      const made: ExpenseBody = { v: 1, text: text.value.trim(), amount: amountText(units), currency: currency.value.toUpperCase(), paidBy: paidBy.value, split };
      if (at.value) {
        made.at = at.value;
      }
      if (expense) {
        await changeEntry(this.circleId, FEATURE, 'expense', expense.entryId, made);
      } else {
        await addEntry(this.circleId, FEATURE, 'expense', made, plan.entryId);
      }
    });
  }

  /** A payment someone made to settle. */
  private editPayment(plan: FeatureEntry<PlanBody>, proposed?: PaymentBody): void {
    const body = plan.body as PlanBody;
    const me = memberPerson(this.me());
    const options = (selected: Person | undefined) => body.people.map((p) => h('option', { value: p, selected: p === selected }, this.person(p)));
    const from = h('select', {}, ...options(proposed?.from ?? me));
    const to = h('select', {}, ...options(proposed?.to ?? body.people.find((p) => p !== me)));
    const amount = h('input', { required: true, inputmode: 'decimal', value: proposed?.amount ?? '', placeholder: '0.00', size: 8 });
    const currency = h('input', { required: true, maxlength: 3, minlength: 3, pattern: '[A-Za-z]{3}', value: proposed?.currency ?? body.currency, size: 4 });
    const at = h('input', { type: 'date', value: proposed?.at ?? today() });
    formDialog(t('Enter a payment'), [
      h('div', { class: 'two-columns' }, h('label', {}, t('From'), from), h('label', {}, t('To'), to)),
      h('div', { class: 'two-columns' }, h('label', {}, t('Amount'), amount), h('label', {}, t('Currency'), currency)),
      h('label', {}, t('Day'), at),
      h('p', { class: 'muted small' }, t('The payment counts at once. Who got the money confirms it, and that is shown beside it.')),
    ], t('Enter'), async () => {
      const units = minor(amount.value);
      if (units === null || units === 0) {
        throw new Error(t('The amount is a number with at most two decimals, such as 12.50.'));
      }
      if (from.value === to.value) {
        throw new Error(t('A payment goes from one person to another.'));
      }
      const made: PaymentBody = { v: 1, from: from.value, to: to.value, amount: amountText(units), currency: currency.value.toUpperCase() };
      if (at.value) {
        made.at = at.value;
      }
      await addEntry(this.circleId, FEATURE, 'payment', made, plan.entryId);
    });
  }
}

customElements.define('eds-plans', EdsPlans);
