package commands

import (
	"context"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// The plans of a room (FEATURES.md, "Plans"): trips and events, each with
// what is to do, what was spent, what everyone owes or is owed, and the
// payments which settle it. The working out is the clients'; the stone
// keeps the entries and never sees an amount as such.

// planView is a plan with everything about it.
type planView struct {
	entry    client.Entry
	plan     features.Plan
	items    []client.Entry
	expenses []client.Entry
	payments []client.Entry
	confirms []client.Entry
}

// people turns names into persons of a plan and back: a member of the room
// by name, anyone else as a guest.
type people struct {
	byName map[string]string // account name to member ID
	names  map[string]string // member ID to account name
}

func (c *command) people(ctx context.Context, t target) (*people, error) {
	names, err := c.memberNames(ctx, t)
	if err != nil {
		return nil, err
	}
	p := &people{byName: map[string]string{}, names: names}
	for id, name := range names {
		p.byName[strings.ToLower(name)] = id
	}
	return p, nil
}

func (p *people) person(name string) string {
	name = strings.TrimSpace(name)
	if id, ok := p.byName[strings.ToLower(name)]; ok {
		return features.MemberPerson(id)
	}
	if strings.HasPrefix(name, "m:") || strings.HasPrefix(name, "g:") {
		return name
	}
	return features.GuestPerson(name)
}

func (p *people) name(person string) string {
	if id := features.PersonMember(person); id != "" {
		if name, ok := p.names[id]; ok {
			return name
		}
		return i18n.Rune("someone who left")
	}
	return i18n.Rune("{name} (guest)", "name", strings.TrimPrefix(person, "g:"))
}

// findPlan finds a plan of a room by its ID or its title.
func (c *command) findPlan(ctx context.Context, t target, which string) (*planView, error) {
	entries, err := c.listEntries(ctx, t, "plans", client.EntryQuery{Kind: "plan"})
	if err != nil {
		return nil, err
	}
	var found *planView
	for i := range entries {
		var plan features.Plan
		if !features.Decode(ctx, t.stone, "plans", &entries[i], &plan) {
			continue
		}
		if entries[i].EntryID == which || strings.EqualFold(plan.Title, which) {
			if found != nil {
				return nil, exit.New(exit.Usage, "%s", i18n.T("Several plans in %s are named %q. Use the plan's ID.", t.label, which))
			}
			found = &planView{entry: entries[i], plan: plan}
		}
	}
	if found == nil {
		return nil, exit.New(exit.NotFound, "%s", i18n.T("No plan %q in %s.", which, t.label))
	}
	return found, nil
}

func (c *command) loadPlan(ctx context.Context, t target, view *planView) error {
	var err error
	if view.items, err = c.listEntries(ctx, t, "plans", client.EntryQuery{Kind: "item", About: view.entry.EntryID}); err != nil {
		return err
	}
	if view.expenses, err = c.listEntries(ctx, t, "plans", client.EntryQuery{Kind: "expense", About: view.entry.EntryID}); err != nil {
		return err
	}
	if view.payments, err = c.listEntries(ctx, t, "plans", client.EntryQuery{Kind: "payment", About: view.entry.EntryID}); err != nil {
		return err
	}
	view.confirms, err = c.listEntries(ctx, t, "plans", client.EntryQuery{Kind: "confirmation"})
	return err
}

// split reads who shares an expense: "anna,bo:2,Dana" gives bo two shares.
func split(text string, who *people) ([]features.Share, error) {
	var shares []features.Share
	for _, part := range strings.Split(text, ",") {
		if part = strings.TrimSpace(part); part == "" {
			continue
		}
		name, count := part, 1
		if i := strings.LastIndex(part, ":"); i > 0 && !strings.HasPrefix(part, "m:") && !strings.HasPrefix(part, "g:") {
			n, err := strconv.Atoi(part[i+1:])
			if err != nil || n < 1 {
				return nil, usage("%q is not a share. Write it as name, or name:2 for two shares.", part)
			}
			name, count = part[:i], n
		}
		shares = append(shares, features.Share{Who: who.person(name), Share: count})
	}
	return shares, nil
}

// plan is 'tolk plan …': the plans of a room.
func (c *command) plan(args []string) error {
	verb := "list"
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		switch strings.ToLower(args[0]) {
		case "list", "new", "show", "item", "done", "expense", "correct", "pay", "confirm", "close", "open", "remove":
			verb, args = strings.ToLower(args[0]), args[1:]
		}
	}
	fs := c.flags("plan " + verb)
	rooms := roomFlag(fs)
	from := fs.String("from", "", "")
	to := fs.String("to", "", "")
	place := fs.String("place", "", "")
	currency := fs.String("currency", "", "")
	with := fs.String("people", "", "")
	who := fs.String("who", "", "")
	paidBy := fs.String("paid-by", "", "")
	shared := fs.String("split", "", "")
	at := fs.String("at", "", "")
	text := fs.String("text", "", "")
	amountFlag := fs.String("amount", "", "")
	undone := fs.Bool("undo", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	misuse := usage("Use 'tolk plan [list] --room <room>', 'tolk plan new <title> --room <room> [--from <day>] [--to <day>] [--place <place>] [--currency EUR] [--people anna,bo,Dana]', 'tolk plan show <plan> --room <room>', 'tolk plan item <plan> <text> [--who <name>]', 'tolk plan done <plan> <item> [--undo]', 'tolk plan expense <plan> <amount> <text> [--paid-by <name>] [--split anna,bo:2] [--at <day>]', 'tolk plan correct <plan> <expense> [--amount …] [--text …] [--paid-by …] [--split …]', 'tolk plan pay <plan> <amount> <to> [--paid-by <name>]', 'tolk plan confirm <plan> <payment>', 'tolk plan close|open <plan>' or 'tolk plan remove <plan> [<entry>]'.")
	want := map[string][2]int{"list": {0, 0}, "new": {1, 1}, "show": {1, 1}, "item": {2, 2}, "done": {2, 2}, "expense": {3, 3}, "correct": {2, 2},
		"pay": {3, 3}, "confirm": {2, 2}, "close": {1, 1}, "open": {1, 1}, "remove": {1, 2}}[verb]
	if len(pos) < want[0] || len(pos) > want[1] {
		return misuse
	}
	day := func(value string) (string, error) {
		if value == "" {
			return "", nil
		}
		read, ok := wall(value)
		if !ok {
			return "", usage("%q is not a day. Write it as 2026-12-04.", value)
		}
		return read[:10], nil
	}
	amount := func(value string) (string, error) {
		units, ok := features.Minor(value)
		if !ok || units == 0 {
			return "", usage("The amount is a number with at most two decimals, such as 12.50.")
		}
		return features.AmountText(units), nil
	}
	code := func(value, fallback string) (string, error) {
		if value == "" {
			value = fallback
		}
		if len(value) != 3 {
			return "", usage("%q is not a currency. Use its three letters, such as EUR.", value)
		}
		return strings.ToUpper(value), nil
	}
	return c.oneRoom(*rooms, misuse, func(ctx context.Context, t target) (any, error) {
		persons, err := c.people(ctx, t)
		if err != nil {
			return nil, err
		}
		me, err := t.stone.MemberID(ctx)
		if err != nil {
			return nil, err
		}
		switch verb {
		case "list":
			entries, err := c.listEntries(ctx, t, "plans", client.EntryQuery{Kind: "plan"})
			if err != nil {
				return nil, err
			}
			out := []map[string]any{}
			for i := range entries {
				var plan features.Plan
				if !features.Decode(ctx, t.stone, "plans", &entries[i], &plan) {
					continue
				}
				out = append(out, map[string]any{"id": entries[i].EntryID, "closed": entries[i].Closed, "plan": plan})
				if !c.json() {
					line := plan.Title
					if plan.From != "" {
						line += "  " + plan.From
						if plan.To != "" && plan.To != plan.From {
							line += " – " + plan.To
						}
					}
					if plan.Place != "" {
						line += "  " + plan.Place
					}
					if entries[i].Closed {
						line += "  [" + i18n.Rune("Closed") + "]"
					}
					c.say("%s  %s", line, entries[i].EntryID)
				}
			}
			if !c.json() && len(out) == 0 {
				c.say("%s", i18n.T("No plans yet. Make one with 'tolk plan new <title> --room <room>'."))
			}
			return map[string]any{"plans": out}, nil
		case "new":
			plan := features.Plan{V: 1, Title: pos[0], Place: *place}
			if plan.From, err = day(*from); err != nil {
				return nil, err
			}
			if plan.To, err = day(*to); err != nil {
				return nil, err
			}
			if plan.To == "" || plan.To < plan.From {
				plan.To = plan.From
			}
			if plan.Currency, err = code(*currency, "EUR"); err != nil {
				return nil, err
			}
			if *with == "" {
				// Everyone in the room takes part, unless told otherwise.
				ids := make([]string, 0, len(persons.names))
				for id := range persons.names {
					ids = append(ids, id)
				}
				sort.Slice(ids, func(i, j int) bool { return persons.names[ids[i]] < persons.names[ids[j]] })
				for _, id := range ids {
					plan.People = append(plan.People, features.MemberPerson(id))
				}
			}
			for _, name := range strings.Split(*with, ",") {
				if strings.TrimSpace(name) != "" {
					plan.People = append(plan.People, persons.person(name))
				}
			}
			entry, err := c.addEntry(ctx, t, "plans", "plan", plan, "", "")
			if err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: made the plan %q (%s).", t.label, plan.Title, entry.EntryID))
			return map[string]any{"id": entry.EntryID, "plan": plan}, nil
		}
		view, err := c.findPlan(ctx, t, pos[0])
		if err != nil {
			return nil, err
		}
		planID := view.entry.EntryID
		switch verb {
		case "item":
			item := features.Item{V: 1, Text: pos[1]}
			if *who != "" {
				item.Who = persons.person(*who)
			}
			entry, err := c.addEntry(ctx, t, "plans", "item", item, planID, "")
			if err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: added %q to %q.", t.label, item.Text, view.plan.Title))
			return map[string]any{"id": entry.EntryID, "item": item}, nil
		case "expense":
			expense := features.Expense{V: 1, Text: pos[2], PaidBy: features.MemberPerson(me)}
			if expense.Amount, err = amount(pos[1]); err != nil {
				return nil, err
			}
			if expense.Currency, err = code(*currency, view.plan.Currency); err != nil {
				return nil, err
			}
			if *paidBy != "" {
				expense.PaidBy = persons.person(*paidBy)
			}
			if expense.At, err = day(*at); err != nil {
				return nil, err
			}
			if expense.At == "" {
				expense.At = time.Now().Format("2006-01-02")
			}
			if *shared == "" {
				// Shared evenly by everyone of the plan, unless told otherwise.
				for _, person := range view.plan.People {
					expense.Split = append(expense.Split, features.Share{Who: person, Share: 1})
				}
			} else if expense.Split, err = split(*shared, persons); err != nil {
				return nil, err
			}
			if len(expense.Split) == 0 {
				return nil, usage("Someone has to share the expense: --split anna,bo.")
			}
			entry, err := c.addEntry(ctx, t, "plans", "expense", expense, planID, "")
			if err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: added the expense %q, %s %s.", t.label, expense.Text, expense.Amount, expense.Currency))
			return map[string]any{"id": entry.EntryID, "expense": expense}, nil
		case "pay":
			payment := features.Payment{V: 1, From: features.MemberPerson(me), To: persons.person(pos[2]), At: time.Now().Format("2006-01-02")}
			if payment.Amount, err = amount(pos[1]); err != nil {
				return nil, err
			}
			if payment.Currency, err = code(*currency, view.plan.Currency); err != nil {
				return nil, err
			}
			if *paidBy != "" {
				payment.From = persons.person(*paidBy)
			}
			if payment.From == payment.To {
				return nil, usage("A payment goes from one person to another.")
			}
			entry, err := c.addEntry(ctx, t, "plans", "payment", payment, planID, "")
			if err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: entered that %s paid %s %s %s.", t.label, persons.name(payment.From), persons.name(payment.To), payment.Amount, payment.Currency))
			return map[string]any{"id": entry.EntryID, "payment": payment}, nil
		case "close", "open":
			err := t.stone.Do(ctx, func(token string) error {
				_, err := t.stone.Client.CloseEntry(ctx, token, t.room.ID, "plans", planID, verb == "close")
				return err
			})
			if err != nil {
				return nil, err
			}
			if verb == "close" {
				c.note(i18n.T("%s: closed the plan %q. Its records stay as they are.", t.label, view.plan.Title))
			} else {
				c.note(i18n.T("%s: opened the plan %q again.", t.label, view.plan.Title))
			}
			return map[string]any{"id": planID, "closed": verb == "close"}, nil
		case "remove":
			which := planID
			if len(pos) == 2 {
				which = pos[1]
			}
			if err := c.removeEntry(ctx, t, "plans", which); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: removed %s.", t.label, which))
			return map[string]any{"removed": which}, nil
		}
		if err := c.loadPlan(ctx, t, view); err != nil {
			return nil, err
		}
		find := func(list []client.Entry, id string) *client.Entry {
			for i := range list {
				if list[i].EntryID == id {
					return &list[i]
				}
			}
			return nil
		}
		switch verb {
		case "done":
			entry := find(view.items, pos[1])
			var item features.Item
			if entry == nil || !features.Decode(ctx, t.stone, "plans", entry, &item) {
				return nil, exit.New(exit.NotFound, "%s", i18n.T("No item %q in the plan %q.", pos[1], view.plan.Title))
			}
			item.Done = !*undone
			if _, err := c.changeEntry(ctx, t, "plans", "item", entry.EntryID, item, ""); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: %q is marked.", t.label, item.Text))
			return map[string]any{"id": entry.EntryID, "item": item}, nil
		case "correct":
			entry := find(view.expenses, pos[1])
			var expense features.Expense
			if entry == nil || !features.Decode(ctx, t.stone, "plans", entry, &expense) {
				return nil, exit.New(exit.NotFound, "%s", i18n.T("No expense %q in the plan %q.", pos[1], view.plan.Title))
			}
			if *amountFlag != "" {
				if expense.Amount, err = amount(*amountFlag); err != nil {
					return nil, err
				}
			}
			if *text != "" {
				expense.Text = *text
			}
			if *currency != "" {
				if expense.Currency, err = code(*currency, ""); err != nil {
					return nil, err
				}
			}
			if *paidBy != "" {
				expense.PaidBy = persons.person(*paidBy)
			}
			if *shared != "" {
				if expense.Split, err = split(*shared, persons); err != nil {
					return nil, err
				}
			}
			if *at != "" {
				if expense.At, err = day(*at); err != nil {
					return nil, err
				}
			}
			if _, err := c.changeEntry(ctx, t, "plans", "expense", entry.EntryID, expense, ""); err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: corrected the expense %q, now %s %s.", t.label, expense.Text, expense.Amount, expense.Currency))
			return map[string]any{"id": entry.EntryID, "expense": expense}, nil
		case "confirm":
			entry := find(view.payments, pos[1])
			if entry == nil {
				return nil, exit.New(exit.NotFound, "%s", i18n.T("No payment %q in the plan %q.", pos[1], view.plan.Title))
			}
			confirmation, err := c.addEntry(ctx, t, "plans", "confirmation", map[string]int{"v": 1}, entry.EntryID, "")
			if err != nil {
				return nil, err
			}
			c.note(i18n.T("%s: confirmed that you got the payment.", t.label))
			return map[string]any{"id": confirmation.EntryID, "payment": entry.EntryID}, nil
		}

		// show: everything of the plan, and who owes whom.
		locale := c.locale()
		money := func(text, currency string) string {
			units, _ := features.Minor(strings.TrimPrefix(text, "-"))
			return locale.Number(float64(units)/100, 2) + " " + currency
		}
		out := map[string]any{"id": planID, "closed": view.entry.Closed, "plan": view.plan}
		var lines []string
		head := view.plan.Title
		if view.entry.Closed {
			head += "  [" + i18n.Rune("Closed") + "]"
		}
		lines = append(lines, head)
		var names []string
		for _, person := range view.plan.People {
			names = append(names, persons.name(person))
		}
		lines = append(lines, strings.Join(names, ", "), "")

		items := []map[string]any{}
		lines = append(lines, i18n.Rune("To do"))
		for i := range view.items {
			var item features.Item
			if !features.Decode(ctx, t.stone, "plans", &view.items[i], &item) {
				continue
			}
			items = append(items, map[string]any{"id": view.items[i].EntryID, "item": item})
			mark := "[ ]"
			if item.Done {
				mark = "[x]"
			}
			line := "  " + mark + " " + item.Text
			if item.Who != "" {
				line += "  (" + persons.name(item.Who) + ")"
			}
			lines = append(lines, line+"  "+view.items[i].EntryID)
		}
		out["items"] = items

		var expenses []features.Expense
		expenseList := []map[string]any{}
		lines = append(lines, "", i18n.Rune("Expenses"))
		for i := range view.expenses {
			var expense features.Expense
			if !features.Decode(ctx, t.stone, "plans", &view.expenses[i], &expense) {
				continue
			}
			if _, ok := features.Minor(expense.Amount); !ok || len(expense.Split) == 0 {
				continue
			}
			expenses = append(expenses, expense)
			expenseList = append(expenseList, map[string]any{"id": view.expenses[i].EntryID, "by": view.expenses[i].AccountName, "expense": expense,
				"corrected": view.expenses[i].Changed != "" && view.expenses[i].Changed != view.expenses[i].Added})
			var sharing []string
			for _, share := range expense.Split {
				name := persons.name(share.Who)
				if share.Share != 1 {
					name += " ×" + strconv.Itoa(share.Share)
				}
				sharing = append(sharing, name)
			}
			lines = append(lines, "  "+pad(money(expense.Amount, expense.Currency), 14)+" "+expense.Text+"  "+
				i18n.Rune("{who} paid, shared by {people}", "who", persons.name(expense.PaidBy), "people", strings.Join(sharing, ", "))+"  "+view.expenses[i].EntryID)
		}
		out["expenses"] = expenseList

		var payments []features.Payment
		paymentList := []map[string]any{}
		var paymentLines []string
		for i := range view.payments {
			var payment features.Payment
			if !features.Decode(ctx, t.stone, "plans", &view.payments[i], &payment) {
				continue
			}
			if _, ok := features.Minor(payment.Amount); !ok {
				continue
			}
			payments = append(payments, payment)
			confirmed := false
			for _, confirmation := range view.confirms {
				if confirmation.About == view.payments[i].EntryID &&
					features.Confirms(payment, view.payments[i].Added, view.payments[i].Changed, confirmation.MemberID, confirmation.Added) {
					confirmed = true
				}
			}
			paymentList = append(paymentList, map[string]any{"id": view.payments[i].EntryID, "by": view.payments[i].AccountName, "payment": payment, "confirmed": confirmed})
			state := i18n.Rune("not confirmed")
			if confirmed {
				state = i18n.Rune("confirmed")
			} else if features.PersonMember(payment.To) == "" {
				state = ""
			}
			paymentLines = append(paymentLines, "  "+pad(money(payment.Amount, payment.Currency), 14)+" "+
				i18n.Rune("{from} paid {to}", "from", persons.name(payment.From), "to", persons.name(payment.To))+"  "+state+"  "+view.payments[i].EntryID)
		}
		out["payments"] = paymentList

		worked := features.Balances(expenses, payments)
		currencies := make([]string, 0, len(worked))
		for currency := range worked {
			currencies = append(currencies, currency)
		}
		sort.Strings(currencies)
		balances, dues := map[string]map[string]string{}, map[string][]map[string]string{}
		lines = append(lines, "", i18n.Rune("Who owes whom"))
		if len(currencies) == 0 {
			lines = append(lines, "  "+i18n.Rune("Nothing was spent yet."))
		}
		for _, currency := range currencies {
			balances[currency] = map[string]string{}
			who := make([]string, 0, len(worked[currency]))
			for person := range worked[currency] {
				who = append(who, person)
			}
			sort.Strings(who)
			for _, person := range who {
				units := worked[currency][person]
				balances[currency][persons.name(person)] = features.AmountText(units)
				switch {
				case units > 0:
					lines = append(lines, "  "+pad(persons.name(person), 16)+" "+i18n.Rune("gets {amount}", "amount", money(features.AmountText(units), currency)))
				case units < 0:
					lines = append(lines, "  "+pad(persons.name(person), 16)+" "+i18n.Rune("owes {amount}", "amount", money(features.AmountText(-units), currency)))
				}
			}
			settle := features.Settle(worked[currency])
			dues[currency] = []map[string]string{}
			if len(settle) == 0 {
				lines = append(lines, "  "+i18n.Rune("{currency}: settled, nobody owes anything.", "currency", currency))
			}
			for _, due := range settle {
				dues[currency] = append(dues[currency], map[string]string{"from": persons.name(due.From), "to": persons.name(due.To), "amount": due.Amount})
				lines = append(lines, "  → "+i18n.Rune("{from} pays {to} {amount}", "from", persons.name(due.From), "to", persons.name(due.To), "amount", money(due.Amount, currency)))
			}
		}
		out["balances"], out["settle"] = balances, dues
		if len(paymentLines) > 0 {
			lines = append(lines, "", i18n.Rune("Payments"))
			lines = append(lines, paymentLines...)
		}
		if !c.json() {
			for _, line := range lines {
				c.say("%s", line)
			}
		}
		return out, nil
	})
}
