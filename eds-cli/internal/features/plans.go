package features

import (
	"fmt"
	"regexp"
	"sort"
	"strconv"
	"strings"
)

// The feature "plans" (FEATURES.md, "Plans"): trips and events with what is to
// do, what was spent and who owes whom. The forms of its entries, and the
// working out of balances and of the payments which settle them, as the
// shared examples (plans-examples.json) have them.

// A person of a plan is "m:<memberId>" for a Member, "g:<name>" for a guest
// without an account.

// Plan is the content of an entry of the kind "plan".
type Plan struct {
	V        int      `json:"v"`
	Title    string   `json:"title"`
	From     string   `json:"from,omitempty"`
	To       string   `json:"to,omitempty"`
	Place    string   `json:"place,omitempty"`
	Currency string   `json:"currency"`
	People   []string `json:"people"`
}

// Item is something to do, to book or to bring.
type Item struct {
	V    int    `json:"v"`
	Text string `json:"text"`
	Who  string `json:"who,omitempty"`
	Done bool   `json:"done"`
}

// Share is the part of an expense one person bears.
type Share struct {
	Who   string `json:"who"`
	Share int    `json:"share"`
}

// Expense says who paid, how much, and among whom it is shared.
type Expense struct {
	V        int     `json:"v"`
	Text     string  `json:"text,omitempty"`
	Amount   string  `json:"amount"`
	Currency string  `json:"currency"`
	PaidBy   string  `json:"paidBy"`
	Split    []Share `json:"split"`
	At       string  `json:"at,omitempty"`
}

// Payment says that one person gave another money to settle.
type Payment struct {
	V        int    `json:"v"`
	From     string `json:"from"`
	To       string `json:"to"`
	Amount   string `json:"amount"`
	Currency string `json:"currency"`
	At       string `json:"at,omitempty"`
}

// MemberPerson is the person of a Member.
func MemberPerson(memberID string) string { return "m:" + memberID }

// GuestPerson is the person of a guest without an account.
func GuestPerson(name string) string { return "g:" + name }

// PersonMember is the Member's ID of a person; empty for a guest.
func PersonMember(person string) string {
	if strings.HasPrefix(person, "m:") {
		return person[2:]
	}
	return ""
}

var amountPattern = regexp.MustCompile(`^(\d{1,12})(?:[.,](\d{1,2}))?$`)

// Minor is an amount, a decimal text with at most two decimals, in
// hundredths; false if it isn't one.
func Minor(amount string) (int64, bool) {
	match := amountPattern.FindStringSubmatch(strings.TrimSpace(amount))
	if match == nil {
		return 0, false
	}
	whole, _ := strconv.ParseInt(match[1], 10, 64)
	cents, _ := strconv.ParseInt((match[2] + "00")[:2], 10, 64)
	return whole*100 + cents, true
}

// AmountText is hundredths as the text of an amount.
func AmountText(units int64) string {
	sign := ""
	if units < 0 {
		sign, units = "-", -units
	}
	return fmt.Sprintf("%s%d.%02d", sign, units/100, units%100)
}

// Parts is what each of the split bears of an expense, in hundredths, in
// the order of the split.
func Parts(expense Expense) []int64 {
	amount, _ := Minor(expense.Amount)
	var total int64
	for _, part := range expense.Split {
		total += int64(part.Share)
	}
	borne := make([]int64, len(expense.Split))
	if total <= 0 {
		return borne
	}
	var sum int64
	for i, part := range expense.Split {
		borne[i] = amount * int64(part.Share) / total
		sum += borne[i]
	}
	// What rounding left over goes, a hundredth each, to the first of the split.
	for i := int64(0); i < amount-sum; i++ {
		borne[int(i)%len(borne)]++
	}
	return borne
}

// Balances is what each person is owed (positive) or owes (negative), in
// hundredths, per currency.
func Balances(expenses []Expense, payments []Payment) map[string]map[string]int64 {
	result := map[string]map[string]int64{}
	add := func(currency, person string, units int64) {
		if result[currency] == nil {
			result[currency] = map[string]int64{}
		}
		result[currency][person] += units
	}
	for _, expense := range expenses {
		amount, ok := Minor(expense.Amount)
		if !ok || len(expense.Split) == 0 {
			continue
		}
		add(expense.Currency, expense.PaidBy, amount)
		for i, units := range Parts(expense) {
			add(expense.Currency, expense.Split[i].Who, -units)
		}
	}
	for _, payment := range payments {
		amount, ok := Minor(payment.Amount)
		if !ok {
			continue
		}
		add(payment.Currency, payment.From, amount)
		add(payment.Currency, payment.To, -amount)
	}
	return result
}

// Due is a payment which helps to settle.
type Due struct {
	From   string `json:"from"`
	To     string `json:"to"`
	Amount string `json:"amount"`
}

// Settle is the payments which settle one currency: the one who owes most
// pays the one who is owed most, as much as the smaller of the two, until
// nothing is left; of equals the first by the person's text.
func Settle(balance map[string]int64) []Due {
	left := map[string]int64{}
	for person, units := range balance {
		left[person] = units
	}
	dues := []Due{}
	type entry struct {
		units  int64
		person string
	}
	first := func(want func(int64) bool, key func(int64) int64) *entry {
		var list []entry
		for person, units := range left {
			if want(units) {
				list = append(list, entry{key(units), person})
			}
		}
		if len(list) == 0 {
			return nil
		}
		sort.Slice(list, func(i, j int) bool {
			if list[i].units != list[j].units {
				return list[i].units < list[j].units
			}
			return list[i].person < list[j].person
		})
		return &list[0]
	}
	for {
		debtor := first(func(u int64) bool { return u < 0 }, func(u int64) int64 { return u })
		creditor := first(func(u int64) bool { return u > 0 }, func(u int64) int64 { return -u })
		if debtor == nil || creditor == nil {
			return dues
		}
		amount := min(-debtor.units, -creditor.units)
		dues = append(dues, Due{From: debtor.person, To: creditor.person, Amount: AmountText(amount)})
		left[debtor.person] += amount
		left[creditor.person] -= amount
	}
}

// Confirms says whether a confirmation counts: its author is the receiver
// of the payment, and it was added after the payment was last changed (the
// stone notes both times).
func Confirms(payment Payment, paymentAdded, paymentChanged, confirmedBy, confirmationAdded string) bool {
	receiver := PersonMember(payment.To)
	since := paymentChanged
	if since == "" {
		since = paymentAdded
	}
	return receiver != "" && confirmedBy == receiver && confirmationAdded >= since
}
