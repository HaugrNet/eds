package commands

import (
	"sort"
	"strconv"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// The UI settings and the values each takes.
var uiValues = map[string][]string{
	"language":   {"auto", "en", "de"},
	"clock":      {"24", "12", "auto"},
	"appearance": {"auto", "light", "dark"},
	"theme":      {"rune", "fjord", "moss", "contrast"},
	"bell":       {"false", "true"},
	"present":    {"true", "false"},
	"layout":     {"three-columns", "chat-on-top", "rooms-beside", "chat-and-files", "chat-only"},
}

func uiGet(ui config.UI, key string) string {
	switch key {
	case "language":
		return ui.Language
	case "clock":
		return ui.Clock
	case "appearance":
		return ui.Appearance
	case "theme":
		return ui.Theme
	case "bell":
		return strconv.FormatBool(ui.Bell)
	case "present":
		return strconv.FormatBool(ui.Present)
	case "layout":
		return firstNonEmpty(ui.Layout, "three-columns")
	}
	return ""
}

func uiSet(ui *config.UI, key, value string) {
	switch key {
	case "language":
		ui.Language = value
	case "clock":
		ui.Clock = value
	case "appearance":
		ui.Appearance = value
	case "theme":
		ui.Theme = value
	case "bell":
		ui.Bell = value == "true"
	case "present":
		ui.Present = value == "true"
	case "layout":
		ui.Layout, ui.LayoutTree = value, nil
	}
}

// settings shows or changes Tolk's own settings.
func (c *command) settings(args []string) error {
	pos, err := parse(c.flags("settings"), args)
	if err != nil {
		return err
	}
	file, err := config.Load()
	if err != nil {
		return exit.New(exit.Other, "%v", err)
	}
	keys := make([]string, 0, len(uiValues))
	for key := range uiValues {
		keys = append(keys, key)
	}
	sort.Strings(keys)

	switch len(pos) {
	case 0:
		if c.json() {
			return c.printJSON(file.UI)
		}
		for _, key := range keys {
			c.say("%-10s  %s", key, uiGet(file.UI, key))
		}
		return nil
	case 1, 2:
		key := strings.ToLower(pos[0])
		values, ok := uiValues[key]
		if !ok {
			return usage("Unknown setting %q. The settings are: %s.", pos[0], strings.Join(keys, ", "))
		}
		if len(pos) == 1 {
			if c.json() {
				return c.printJSON(map[string]string{key: uiGet(file.UI, key)})
			}
			c.say("%s", uiGet(file.UI, key))
			return nil
		}
		value := strings.ToLower(pos[1])
		valid := false
		for _, v := range values {
			valid = valid || v == value
		}
		if !valid {
			return usage("%q is not a value for %s. Use one of: %s.", pos[1], key, strings.Join(values, ", "))
		}
		uiSet(&file.UI, key, value)
		if err := config.Save(file); err != nil {
			return exit.New(exit.Other, "%v", err)
		}
		return nil
	}
	return usage("Too many arguments. Use 'tolk settings [<key> [<value>]]'.")
}
