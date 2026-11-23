/**
 * The feature "plans" (FEATURES.md, "Plans"): trips and events with what is to
 * do, what was spent and who owes whom. The forms of its entries, and the
 * working out of balances and of the payments which settle them, as the
 * shared examples (plans-examples.json) have them. Nothing here touches the
 * page or the stone.
 */

/** A person of a plan: "m:<memberId>" for a Member, "g:<name>" for a guest without an account. */
export type Person = string;

export interface PlanBody { v: 1; title: string; from?: string; to?: string; place?: string; currency: string; people: Person[] }
export interface ItemBody { v: 1; text: string; who?: Person; done: boolean }
export interface Share { who: Person; share: number }
export interface ExpenseBody { v: 1; text: string; amount: string; currency: string; paidBy: Person; split: Share[]; at?: string }
export interface PaymentBody { v: 1; from: Person; to: Person; amount: string; currency: string; at?: string }
export interface ConfirmationBody { v: 1 }

export function memberPerson(memberId: string): Person {
  return `m:${memberId}`;
}

export function guestPerson(name: string): Person {
  return `g:${name}`;
}

/** The Member's ID of a person, or null for a guest. */
export function personMember(person: Person): string | null {
  return person.startsWith('m:') ? person.slice(2) : null;
}

/** An amount, a decimal text with at most two decimals, in hundredths; null if it isn't one. */
export function minor(amount: string): number | null {
  const match = /^(\d{1,12})(?:[.,](\d{1,2}))?$/.exec(amount.trim());
  if (!match) {
    return null;
  }
  return Number(match[1]) * 100 + Number((match[2] ?? '').padEnd(2, '0'));
}

/** Hundredths as the text of an amount. */
export function amountText(units: number): string {
  const whole = Math.floor(Math.abs(units) / 100);
  return `${units < 0 ? '-' : ''}${whole}.${String(Math.abs(units) % 100).padStart(2, '0')}`;
}

/** What each of the split bears of an expense, in hundredths, in the order of the split. */
export function parts(expense: Pick<ExpenseBody, 'amount' | 'split'>): number[] {
  const amount = minor(expense.amount) ?? 0;
  const total = expense.split.reduce((sum, part) => sum + part.share, 0);
  if (total <= 0) {
    return expense.split.map(() => 0);
  }
  const borne = expense.split.map((part) => Math.floor((amount * part.share) / total));
  // What rounding left over goes, a hundredth each, to the first of the split.
  const left = amount - borne.reduce((sum, units) => sum + units, 0);
  for (let i = 0; i < left; i++) {
    borne[i % borne.length] += 1;
  }
  return borne;
}

/** What each person is owed (positive) or owes (negative), in hundredths, per currency. */
export function balances(expenses: Pick<ExpenseBody, 'amount' | 'currency' | 'paidBy' | 'split'>[],
    payments: Pick<PaymentBody, 'amount' | 'currency' | 'from' | 'to'>[]): Map<string, Map<Person, number>> {
  const result = new Map<string, Map<Person, number>>();
  const add = (currency: string, person: Person, units: number) => {
    const of = result.get(currency) ?? new Map<Person, number>();
    of.set(person, (of.get(person) ?? 0) + units);
    result.set(currency, of);
  };
  for (const expense of expenses) {
    const amount = minor(expense.amount);
    if (amount === null || expense.split.length === 0) {
      continue;
    }
    add(expense.currency, expense.paidBy, amount);
    parts(expense).forEach((units, index) => add(expense.currency, expense.split[index].who, -units));
  }
  for (const payment of payments) {
    const amount = minor(payment.amount);
    if (amount === null) {
      continue;
    }
    add(payment.currency, payment.from, amount);
    add(payment.currency, payment.to, -amount);
  }
  return result;
}

/** Orders as the examples do: by the number, then by the text of the person, code point by code point. */
function first(entries: [number, Person][]): [number, Person] | undefined {
  return entries.sort((a, b) => a[0] - b[0] || (a[1] < b[1] ? -1 : a[1] > b[1] ? 1 : 0))[0];
}

export interface Due { from: Person; to: Person; amount: string }

/**
 * The payments which settle one currency: the one who owes most pays the
 * one who is owed most, as much as the smaller of the two, until nothing
 * is left.
 */
export function settle(balance: Map<Person, number>): Due[] {
  const left = new Map(balance);
  const dues: Due[] = [];
  for (;;) {
    const debtor = first([...left].filter(([, units]) => units < 0).map(([person, units]) => [units, person]));
    const creditor = first([...left].filter(([, units]) => units > 0).map(([person, units]) => [-units, person]));
    if (!debtor || !creditor) {
      return dues;
    }
    const amount = Math.min(-debtor[0], -creditor[0]);
    dues.push({ from: debtor[1], to: creditor[1], amount: amountText(amount) });
    left.set(debtor[1], (left.get(debtor[1]) ?? 0) + amount);
    left.set(creditor[1], (left.get(creditor[1]) ?? 0) - amount);
  }
}

/**
 * Whether a confirmation counts: its author is the receiver of the payment,
 * and it was added after the payment was last changed (the stone notes both
 * times).
 */
export function confirms(payment: { body: PaymentBody | null; added?: string; changed?: string },
    confirmation: { memberId?: string; added?: string }): boolean {
  const receiver = payment.body ? personMember(payment.body.to) : null;
  const since = payment.changed ?? payment.added ?? '';
  return !!receiver && confirmation.memberId === receiver && (confirmation.added ?? '') >= since;
}
