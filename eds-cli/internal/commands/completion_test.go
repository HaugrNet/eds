package commands

import (
	"slices"
	"strings"
	"testing"

	"github.com/HaugrNet/eds/eds-cli/internal/exit"
)

// The completion, from the command lines of the help.
func TestCompletion(t *testing.T) {
	stones := func() []string { return []string{"home", "work"} }
	for _, tc := range []struct {
		line string   // the words; the last is the one being typed
		want []string // all of these are offered
		not  []string // and none of these
	}{
		{"", []string{"send", "stone", "completion", "chat"}, []string{"__complete", "-p"}},
		{"st", []string{"status", "stone"}, []string{"send"}},
		{"stone|", []string{"info", "key", "features", "set"}, nil},
		{"stones|key|", []string{"create", "withdraw"}, nil},
		{"cal|", []string{"add", "export", "import"}, nil},
		{"send|", []string{filesMarker}, nil},
		{"send|a.txt|family|--", []string{"--name", "--replace", "--share", "--output"}, []string{"-p", "--room"}},
		{"send|a.txt|family|-", []string{"-p", "-m", "--name"}, nil},
		{"get|family/a.txt|-o|", []string{filesMarker}, nil},
		{"features|", []string{"on", "off", "readonly", "remove"}, nil},
		{"features|on|", []string{"chat", "calendar", "plans", "location", "calls"}, nil},
		{"access|bo|", []string{"read", "write", "admin"}, nil},
		{"add|bo|--room|r|--access|", []string{"read", "write", "admin"}, []string{"--room"}},
		{"calendar|add|x|--repeat|", []string{"daily", "weekly", "monthly", "yearly"}, nil},
		{"calendar|add|x|--repeat=w", []string{"--repeat=weekly"}, []string{"--repeat=daily"}},
		{"settings|", []string{"language", "theme", "layout"}, nil},
		{"settings|theme|", []string{"rune", "fjord", "moss", "contrast"}, nil},
		{"help|", []string{"start", "files", "all", "send"}, nil},
		{"--output|", []string{"json", "text"}, nil},
		{"--lang|de|ro", []string{"room", "rooms"}, []string{"send"}},
		{"rooms|--stone|", []string{"home", "work"}, nil},
		{"stone|info|", []string{"home", "work"}, nil},
		{"register|stone|", nil, []string{"home", "work"}},
		{"remove|", []string{"stone"}, nil},
		{"remove|stone|", []string{"home", "work"}, nil},
		{"room|new|x|--features|", []string{"chat", "calls"}, nil},
		{"plan|new|t|--room|r|--cu", []string{"--currency"}, nil},
		{"plan|new|t|--currency|", nil, []string{"EUR"}},
		{"chat|--plain|--room|a|--room|b|--r", []string{"--room"}, nil},
		{"completion|", []string{"bash"}, nil},
		{"version|x|", nil, []string{"send"}},
	} {
		got := completeWords(strings.Split(tc.line, "|"), stones)
		for _, want := range tc.want {
			if !slices.Contains(got, want) {
				t.Errorf("%q: %q is missing in %v", tc.line, want, got)
			}
		}
		for _, not := range tc.not {
			if slices.Contains(got, not) {
				t.Errorf("%q: %q shouldn't be in %v", tc.line, not, got)
			}
		}
	}
}

// Every command line of the help is read, and each names its command; the
// other lines of the help are tables of addresses, flags and exit codes.
func TestCompletionReadsTheHelp(t *testing.T) {
	lines := 0
	for _, topic := range helpTopics() {
		for _, section := range topic.sections {
			for _, item := range section.items {
				if item.kind != helpCommand {
					continue
				}
				for _, line := range item.lines {
					if strings.HasPrefix(line, "tolk ") && !strings.HasPrefix(line, "tolk <") {
						lines++
					}
				}
			}
		}
	}
	patterns := helpPatterns()
	if len(patterns) != lines {
		t.Errorf("%d command lines, but %d were read", lines, len(patterns))
	}
	for _, p := range patterns {
		if len(p.positionals) == 0 || len(p.positionals[0].literals) == 0 {
			t.Errorf("a command line without its command: %+v", p)
		}
	}
}

// The command itself: the script for bash, and the words over the hidden
// command, which never fails, whatever is typed.
func TestCompletionCommand(t *testing.T) {
	setup(t)
	code, out, _ := run("", "completion", "bash")
	if code != exit.OK || !strings.Contains(out, "complete -F _tolk tolk") || !strings.Contains(out, completeVerb) {
		t.Errorf("completion bash: %d %s", code, out)
	}
	if code, out, _ := run("", "completion"); code != exit.OK || !strings.Contains(out, "_tolk()") {
		t.Errorf("completion: %d %s", code, out)
	}
	if code, _, _ := run("", "completion", "zsh"); code != exit.Usage {
		t.Errorf("completion zsh: %d", code)
	}
	if code, out, _ := run("", completeVerb, "stone", ""); code != exit.OK || !strings.Contains(out, "info\n") {
		t.Errorf("__complete stone: %d %s", code, out)
	}
	for _, words := range [][]string{{}, {"--ro"}, {"--output"}, {"-"}, {"--x=y"}, {"\"", "[", ""}} {
		if code, _, errOut := run("", append([]string{completeVerb}, words...)...); code != exit.OK {
			t.Errorf("__complete %q: %d %s", words, code, errOut)
		}
	}
	// No stones without an agent: the container isn't unlocked for this.
	if code, out, _ := run("", completeVerb, "rooms", "--stone", ""); code != exit.OK || out != "" {
		t.Errorf("__complete --stone without an agent: %d %q", code, out)
	}
}
