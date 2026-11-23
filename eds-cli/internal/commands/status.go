package commands

import (
	"fmt"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/HaugrNet/eds/eds-cli/internal/client"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/status"
)

// The status of stones (STATUS.md), for a System Administrator who looks
// after several: status keys, the name of a stone, a table of the stones,
// and the live view.

// stoneKey is 'tolk stone key [list|create <label>|withdraw <label>]'.
func (c *command) stoneKey(args []string) error {
	verb := "list"
	if len(args) > 0 && !strings.HasPrefix(args[0], "-") {
		switch strings.ToLower(args[0]) {
		case "list", "create", "withdraw":
			verb, args = strings.ToLower(args[0]), args[1:]
		}
	}
	fs := c.flags("stone key")
	stoneName := fs.String("stone", "", "")
	keep := fs.Bool("keep", false, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if (verb == "list" && len(pos) != 0) || (verb != "list" && len(pos) != 1) {
		return usage("Use 'tolk stone key [--stone <name>]', 'tolk stone key create <label> [--keep]' or 'tolk stone key withdraw <label>'.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, *stoneName)
	if err != nil {
		return err
	}
	switch verb {
	case "create":
		var key string
		if err := stone.Do(ctx, func(token string) error {
			var err error
			key, err = stone.Client.CreateStatusKey(ctx, token, pos[0])
			return err
		}); err != nil {
			return err
		}
		if *keep {
			// Kept with the stone's entry: 'tolk status' then asks with it.
			if err := c.change(func(container *config.Container) error {
				entry := container.Stones[stone.Name]
				entry.StatusKey = key
				container.Stones[stone.Name] = entry
				return nil
			}); err != nil {
				c.say("%s", key)
				return err
			}
		}
		if c.json() {
			return c.printJSON(map[string]any{"stone": stone.Name, "label": pos[0], "key": key, "url": stone.URL})
		}
		c.note(i18n.T("The status key %q of %s. It is shown only now. It reads the status of the stone and nothing else; give it to whoever looks after the stone, with the address %s.", pos[0], stone.Name, stone.URL))
		c.say("%s", key)
		return nil
	case "withdraw":
		if err := stone.Do(ctx, func(token string) error { return stone.Client.WithdrawStatusKey(ctx, token, pos[0]) }); err != nil {
			return err
		}
		c.note(i18n.T("Withdrew the status key %q of %s. Whoever uses it is refused from now on.", pos[0], stone.Name))
	}
	var labels []string
	if err := stone.Do(ctx, func(token string) error {
		var err error
		labels, err = stone.Client.StatusKeys(ctx, token)
		return err
	}); err != nil {
		return err
	}
	if c.json() {
		if labels == nil {
			labels = []string{}
		}
		return c.printJSON(map[string]any{"stone": stone.Name, "keys": labels})
	}
	if verb == "list" {
		if len(labels) == 0 {
			c.say("%s", i18n.Rune("No status keys."))
		}
		for _, label := range labels {
			c.say("%s", label)
		}
	}
	return nil
}

// stoneNameCommand is 'tolk stone name [<name>]': how a stone calls itself
// in its status.
func (c *command) stoneNameCommand(args []string) error {
	fs := c.flags("stone name")
	stoneName := fs.String("stone", "", "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if len(pos) > 1 {
		return usage("Use 'tolk stone name [<name>] [--stone <name>]'.")
	}
	if len(pos) == 1 {
		return c.settingsOf(*stoneName, map[string]string{"eds.stone.name": pos[0]})
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	stone, err := svc.Stone(ctx, *stoneName)
	if err != nil {
		return err
	}
	var settings *client.SettingResponse
	if err := stone.Do(ctx, func(token string) error {
		var err error
		settings, err = stone.Client.Settings(ctx, token, nil)
		return err
	}); err != nil {
		return err
	}
	name := settings.Settings["eds.stone.name"]
	if c.json() {
		return c.printJSON(map[string]any{"stone": stone.Name, "name": name})
	}
	if name == "" {
		c.say("%s", i18n.T("%s has no name of its own yet. Give it one with 'tolk stone name <name>'.", stone.Name))
	} else {
		c.say("%s", name)
	}
	return nil
}

// statusCommand is 'tolk status [stone …] [--watch]': a table of the
// stones, JSON for scripts, or the live view.
func (c *command) statusCommand(args []string) error {
	fs := c.flags("status")
	watch := fs.Bool("watch", false, "")
	interval := fs.Float64("interval", 5, "")
	rounds := fs.Int("count", 0, "")
	pos, err := parse(fs, args)
	if err != nil {
		return err
	}
	if *interval < 1 {
		return usage("The stones are asked every second at most: --interval 1.")
	}
	svc, err := c.service()
	if err != nil {
		return err
	}
	defer svc.Close()
	ctx, cancel := c.context()
	defer cancel()
	infos, err := svc.Infos()
	if err != nil {
		return err
	}
	var stones []*status.View
	for _, info := range infos {
		if len(pos) == 0 || contains(pos, info.Name) {
			stones = append(stones, &status.View{Name: info.Name, URL: info.URL})
		}
	}
	for _, name := range pos {
		if _, err := svc.Info(name); err != nil {
			return err
		}
	}
	if len(stones) == 0 {
		return exit.New(exit.NotFound, "%s", i18n.T("No stones yet. Add one with 'tolk register stone <name>'."))
	}
	ask := func(history bool) {
		var all sync.WaitGroup
		for _, stone := range stones {
			all.Add(1)
			go func() {
				defer all.Done()
				stone.Ask(ctx, svc.Status, history)
			}()
		}
		all.Wait()
		// The busiest first; one which doesn't answer before all.
		sort.SliceStable(stones, func(i, j int) bool {
			rank := func(w *status.View) float64 {
				if w.Problem != "" {
					return 2
				}
				return status.Busy(w.Load)
			}
			return rank(stones[i]) > rank(stones[j])
		})
	}
	failed := func() error {
		for _, stone := range stones {
			if stone.Problem != "" {
				return reported{exit.Network}
			}
		}
		return nil
	}

	if !*watch {
		ask(true)
		if c.json() {
			list := []map[string]any{}
			for _, stone := range stones {
				list = append(list, stone.JSON())
			}
			if err := c.printJSON(map[string]any{"stones": list}); err != nil {
				return err
			}
			return failed()
		}
		locale := c.locale()
		width := len(i18n.T("Stone"))
		for _, stone := range stones {
			width = max(width, len([]rune(stone.Title())))
		}
		c.say("%s  %-8s %-9s %7s %7s %6s %10s", pad(i18n.T("Stone"), width), i18n.T("State"), i18n.Rune("Storage"), i18n.T("People"), i18n.T("Req/s"), i18n.T("CPU"), i18n.Rune("Memory"))
		for _, stone := range stones {
			if stone.Status == nil {
				c.say("%s  %s", pad(stone.Title(), width), stone.Problem)
				continue
			}
			state := i18n.Rune("awake")
			if !stone.Status.Awake {
				state = i18n.T("asleep")
			}
			storage := "–"
			if used, ok := status.Latest(stone.Inventory, "storageUsed"); ok {
				if ceiling, _ := status.Latest(stone.Inventory, "storageCeiling"); ceiling > 0 {
					storage = locale.Number(used/ceiling*100, 1) + " %"
				}
			}
			accounts, _ := status.Latest(stone.Load, "accounts")
			requests, _ := status.Latest(stone.Load, "requests")
			cpu := "–"
			if share, ok := status.Latest(stone.Load, "processShare"); ok {
				cpu = locale.Number(share*100, 0) + " %"
			}
			memory, _ := status.Latest(stone.Load, "memoryUsed")
			c.say("%s  %-8s %-9s %7s %7s %6s %10s", pad(stone.Title(), width), state, storage, locale.Number(accounts, 0),
				locale.Number(requests/stone.Interval(), 1), cpu, locale.Size(int64(memory)))
		}
		return failed()
	}

	// The live view: the stones are asked again and again, and drawn with
	// bars and small curves; until it is interrupted, or as often as told.
	if c.json() {
		return usage("The live view is for people. For scripts, ask 'tolk status --output json' as often as needed.")
	}
	lastHistory := time.Time{}
	for round := 1; ; round++ {
		history := time.Since(lastHistory) > 5*time.Minute
		ask(history)
		if history {
			lastHistory = time.Now()
		}
		var out strings.Builder
		if c.env.ErrTTY {
			out.WriteString("\x1b[H\x1b[2J") // the screen is drawn anew
		}
		out.WriteString(i18n.T("The stones, asked every %s s · %s · Ctrl-C ends", c.locale().Number(*interval, 0), c.locale().Time(time.Now())) + "\n\n")
		for _, stone := range stones {
			for _, line := range stone.Lines(c.locale()) {
				out.WriteString(line + "\n")
			}
		}
		fmt.Fprint(c.env.Out, out.String())
		if *rounds > 0 && round >= *rounds {
			return failed()
		}
		select {
		case <-ctx.Done():
			return nil
		case <-time.After(time.Duration(*interval * float64(time.Second))):
		}
	}
}
