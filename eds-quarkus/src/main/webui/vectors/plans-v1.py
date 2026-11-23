# Independent implementation of how the feature "plans" works out who owes whom
# (FEATURES.md, "Plans"), written from the specification only. It makes the
# examples which Rune and Tolk must reproduce: python3 -I vectors/plans-v1.py
# (in eds-quarkus/src/main/webui). Needs nothing but Python.
import json, os
from decimal import Decimal


def minor(amount):
    """An amount, a decimal text with at most two decimals, in hundredths."""
    value = Decimal(amount) * 100
    assert value == value.to_integral_value(), amount
    return int(value)


def text(units):
    sign = "-" if units < 0 else ""
    return "%s%d.%02d" % (sign, abs(units) // 100, abs(units) % 100)


def balances(expenses, payments):
    """What each person is owed (positive) or owes (negative), per currency."""
    result = {}

    def add(currency, person, units):
        result.setdefault(currency, {})
        result[currency][person] = result[currency].get(person, 0) + units

    for expense in expenses:
        amount = minor(expense["amount"])
        total = sum(part["share"] for part in expense["split"])
        parts = [amount * part["share"] // total for part in expense["split"]]
        # What rounding left over goes, a hundredth each, to the first of the split.
        for i in range(amount - sum(parts)):
            parts[i] += 1
        add(expense["currency"], expense["paidBy"], amount)
        for part, units in zip(expense["split"], parts):
            add(expense["currency"], part["who"], -units)
    for payment in payments:
        amount = minor(payment["amount"])
        add(payment["currency"], payment["from"], amount)
        add(payment["currency"], payment["to"], -amount)
    return result


def settle(balance):
    """The fewest-looking payments which settle a currency: the one who owes most pays the one who is owed most."""
    left = dict(balance)
    payments = []
    while True:
        debtors = sorted((units, person) for person, units in left.items() if units < 0)
        creditors = sorted((-units, person) for person, units in left.items() if units > 0)
        if not debtors or not creditors:
            break
        owed, debtor = debtors[0]
        due, creditor = creditors[0]
        amount = min(-owed, -due)
        payments.append({"from": debtor, "to": creditor, "amount": text(amount)})
        left[debtor] += amount
        left[creditor] -= amount
    return payments


def example(name, expenses, payments):
    result = balances(expenses, payments)
    return {
        "name": name,
        "expenses": expenses,
        "payments": payments,
        "balances": {currency: {person: text(units) for person, units in sorted(people.items())} for currency, people in sorted(result.items())},
        "settle": {currency: settle(people) for currency, people in sorted(result.items())},
    }


A, B, C, G = "m:alice", "m:bob", "m:carol", "g:Dana (guest)"
examples = {
    "about": "Examples of the feature 'plans' (FEATURES.md): expenses and payments, the balances which follow, and the payments which settle them. "
             "Made by vectors/plans-v1.py, an independent implementation. A person is 'm:<memberId>' or 'g:<name>'; here names stand for IDs.",
    "examples": [
        example("three share a dinner evenly", [
            {"amount": "90.00", "currency": "EUR", "paidBy": A, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}, {"who": C, "share": 1}]},
        ], []),
        example("a hundredth is left over", [
            {"amount": "100.00", "currency": "EUR", "paidBy": B, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}, {"who": C, "share": 1}]},
        ], []),
        example("unequal shares, and a guest without an account", [
            {"amount": "120.00", "currency": "EUR", "paidBy": A, "split": [{"who": A, "share": 2}, {"who": B, "share": 1}, {"who": G, "share": 1}]},
            {"amount": "30.50", "currency": "EUR", "paidBy": G, "split": [{"who": B, "share": 1}, {"who": G, "share": 1}]},
        ], []),
        example("several expenses and a payment already made", [
            {"amount": "200.00", "currency": "EUR", "paidBy": A, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}, {"who": C, "share": 1}, {"who": G, "share": 1}]},
            {"amount": "60.00", "currency": "EUR", "paidBy": B, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}, {"who": C, "share": 1}]},
            {"amount": "45.99", "currency": "EUR", "paidBy": C, "split": [{"who": A, "share": 1}, {"who": C, "share": 2}]},
        ], [
            {"amount": "50.00", "currency": "EUR", "from": G, "to": A},
        ]),
        example("two currencies are kept apart", [
            {"amount": "80.00", "currency": "EUR", "paidBy": A, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}]},
            {"amount": "900.00", "currency": "DKK", "paidBy": B, "split": [{"who": A, "share": 1}, {"who": B, "share": 2}]},
        ], []),
        example("settled: nothing is left", [
            {"amount": "40.00", "currency": "EUR", "paidBy": A, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}]},
        ], [
            {"amount": "20.00", "currency": "EUR", "from": B, "to": A},
        ]),
        example("who paid isn't among those who share", [
            {"amount": "33.33", "currency": "EUR", "paidBy": C, "split": [{"who": A, "share": 1}, {"who": B, "share": 1}]},
        ], []),
    ],
}

target = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "features", "plans-examples.json")
with open(target, "w", encoding="utf-8") as out:
    json.dump(examples, out, indent=2, ensure_ascii=False)
    out.write("\n")
print("wrote", os.path.normpath(target))
