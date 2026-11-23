//go:build ignore

// Copies the files shared with Rune from Rune's sources (go generate).
package main

import (
	"fmt"
	"os"
)

func main() {
	const rune = "../../../../eds-quarkus/src/main/webui/src/"
	for name, source := range map[string]string{
		"de.json":             rune + "i18n/de.json",
		"messages.json":       rune + "messages.json",
		"vectors.json":        rune + "e2e/vectors.json",
		"plans-examples.json": rune + "features/plans-examples.json",
		"chat.json":           rune + "features/declarations/chat.json",
		"calendar.json":       rune + "features/declarations/calendar.json",
		"location.json":       rune + "features/declarations/location.json",
		"plans.json":          rune + "features/declarations/plans.json",
		"calls.json":          rune + "features/declarations/calls.json",
	} {
		data, err := os.ReadFile(source)
		if err != nil {
			fmt.Fprintln(os.Stderr, err)
			os.Exit(1)
		}
		if err := os.WriteFile(name, data, 0o644); err != nil {
			fmt.Fprintln(os.Stderr, err)
			os.Exit(1)
		}
	}
}
