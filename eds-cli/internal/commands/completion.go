package commands

import (
	"fmt"
	"slices"
	"sort"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/features"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
)

// The completion of the command line. 'tolk completion bash' writes a small
// bash function, which asks Tolk itself for the words: 'tolk __complete
// <words>' gets the words of the line up to the one being completed, and
// writes one candidate per line. The words come from the command lines of
// the help, so a command in the help is completed without a list of its
// own. A line ":files" asks bash to complete file names as well.

// filesMarker is the line which asks the shell for file names.
const filesMarker = ":files"

// completeVerb is the hidden command the bash function calls.
const completeVerb = "__complete"

// The flags which every command takes (flags in env.go).
var commonFlags = map[string][]string{
	"--output":   {"text", "json"},
	"--lang":     nil, // set in commonFlagValues, from i18n
	"--no-input": nil,
	"--help":     nil,
}

// Short forms of the commands, as dispatch reads them.
var verbAliases = map[string]string{
	"cal": "calendar", "keys": "key", "feature": "features", "entries": "entry",
	"plans": "plan", "stones": "stone", "server": "stone", "haugr": "stone",
}

const bashCompletion = `# The bash completion of Tolk: source <(tolk completion bash)
_tolk() {
    local line="${COMP_LINE:0:COMP_POINT}" words=() cur prefix candidate files=false
    read -ra words <<< "${line}"
    [[ "${line}" =~ [[:space:]]$ ]] && words+=("")
    cur="${words[-1]}"
    COMPREPLY=()
    while IFS= read -r candidate; do
        if [[ "${candidate}" == "` + filesMarker + `" ]]; then
            files=true
        elif [[ -n "${candidate}" ]]; then
            COMPREPLY+=("${candidate}")
        fi
    done < <("${words[0]}" ` + completeVerb + ` "${words[@]:1}" 2>/dev/null)
    if [[ "${files}" == true ]]; then
        compopt -o filenames 2>/dev/null
        mapfile -t -O "${#COMPREPLY[@]}" COMPREPLY < <(compgen -f -- "${cur}")
    fi
    # bash completes the part after the last = or :, so that is cut off
    prefix="${cur%"${cur##*[=:]}"}"
    [[ -n "${prefix}" ]] && COMPREPLY=("${COMPREPLY[@]#"${prefix}"}")
}
complete -F _tolk tolk
`

func (c *command) completion(args []string) error {
	rest, err := parse(c.flags("completion"), args)
	if err != nil {
		return err
	}
	if len(rest) > 1 || (len(rest) == 1 && rest[0] != "bash") {
		return usage("Only bash is supported: 'tolk completion bash'.")
	}
	_, err = c.env.Out.Write([]byte(bashCompletion))
	return err
}

// complete writes the candidates for the last of the words; it never fails
// and never asks for anything, as it runs while the user types.
func (c *command) complete(words []string) error {
	if len(words) == 0 {
		words = []string{""}
	}
	for _, candidate := range completeWords(words, completeStones) {
		if _, err := fmt.Fprintln(c.env.Out, candidate); err != nil {
			return err
		}
	}
	return nil
}

// completeStones names the stones, when the agent has the container open;
// the container itself is never unlocked for a completion.
func completeStones() []string {
	if !agent.Running() {
		return nil
	}
	access, err := service.Open(nil)
	if err != nil {
		return nil
	}
	defer access.Close()
	stones, _, err := access.Stones()
	if err != nil {
		return nil
	}
	names := make([]string, 0, len(stones))
	for _, stone := range stones {
		names = append(names, stone.Name)
	}
	return names
}

// slot is one place on a command line: literal words, a placeholder, or both.
type slot struct {
	literals    []string
	placeholder string
	repeat      bool
}

// pattern is one command line of the help.
type pattern struct {
	positionals []slot
	flags       map[string]*slot // nil: a flag without a value
}

// completeWords finds the candidates for the last word, which is the one
// being typed; the others are the words before it.
func completeWords(words []string, stones func() []string) []string {
	cur := words[len(words)-1]
	patterns := helpPatterns()
	positionals, lastFlag, given := typedWords(words[:len(words)-1], patterns)
	matching := matchingPatterns(patterns, positionals)

	var out []string
	// --flag=value
	if strings.HasPrefix(cur, "-") && strings.Contains(cur, "=") {
		name, value, _ := strings.Cut(cur, "=")
		for _, candidate := range flagValues(name, matching, stones) {
			if candidate != filesMarker && strings.HasPrefix(candidate, value) {
				out = append(out, name+"="+candidate)
			}
		}
		return finish(out)
	}
	if lastFlag != "" {
		return finish(filter(flagValues(lastFlag, matching, stones), cur))
	}
	if !strings.HasPrefix(cur, "-") {
		out = filter(positionalCandidates(matching, positionals, stones), cur)
		if len(out) > 0 || strings.Contains(cur, "/") || strings.Contains(cur, ".") {
			return finish(out)
		}
	}
	for _, flag := range flagNames(matching) {
		if strings.HasPrefix(flag, cur) && (!given[flag] || flag == "--room") {
			out = append(out, flag)
		}
	}
	return finish(out)
}

func finish(candidates []string) []string {
	sort.Strings(candidates)
	return slices.Compact(candidates)
}

func filter(candidates []string, prefix string) []string {
	var out []string
	for _, candidate := range candidates {
		if candidate == filesMarker || strings.HasPrefix(strings.ToLower(candidate), strings.ToLower(prefix)) {
			out = append(out, candidate)
		}
	}
	return out
}

// typedWords splits the words given into the positional words and the
// flags, and tells whether the last word is a flag which wants a value.
func typedWords(words []string, patterns []pattern) (positionals []string, lastFlag string, given map[string]bool) {
	takesValue := map[string]bool{"--output": true, "--lang": true}
	for _, p := range patterns {
		for name, value := range p.flags {
			if value != nil {
				takesValue[name] = true
			}
		}
	}
	given = map[string]bool{}
	for i := 0; i < len(words); i++ {
		word := words[i]
		if !strings.HasPrefix(word, "-") || word == "-" {
			positionals = append(positionals, strings.ToLower(word))
			continue
		}
		// Go's flags take -room as --room; -o is a short one.
		name, _, inline := strings.Cut(word, "=")
		if !strings.HasPrefix(name, "--") && len(name) > 2 {
			name = "-" + name
		}
		given[name] = true
		if !inline && takesValue[name] {
			if i == len(words)-1 {
				lastFlag = name
			}
			i++
		}
	}
	if len(positionals) > 0 {
		if verb, ok := verbAliases[positionals[0]]; ok {
			positionals[0] = verb
		}
	}
	return positionals, lastFlag, given
}

// matchingPatterns are the command lines whose places fit the positional
// words typed so far.
func matchingPatterns(patterns []pattern, positionals []string) []pattern {
	var out []pattern
	for _, p := range patterns {
		if fits(p, positionals) {
			out = append(out, p)
		}
	}
	return out
}

func fits(p pattern, positionals []string) bool {
	for i, word := range positionals {
		s, ok := slotAt(p, i)
		if !ok {
			return false
		}
		if s.placeholder == "" && !slices.Contains(s.literals, word) {
			return false
		}
	}
	return true
}

func slotAt(p pattern, i int) (slot, bool) {
	if i < len(p.positionals) {
		return p.positionals[i], true
	}
	if n := len(p.positionals); n > 0 && p.positionals[n-1].repeat {
		return p.positionals[n-1], true
	}
	return slot{}, false
}

func positionalCandidates(matching []pattern, positionals []string, stones func() []string) []string {
	var out []string
	at := len(positionals)
	for _, p := range matching {
		s, ok := slotAt(p, at)
		if !ok {
			continue
		}
		out = append(out, s.literals...)
		out = append(out, placeholderValues(p, at, s.placeholder, positionals, stones)...)
	}
	return out
}

// placeholderValues are the words a placeholder stands for, where Tolk knows
// them without asking a stone.
func placeholderValues(p pattern, at int, placeholder string, positionals []string, stones func() []string) []string {
	verb := ""
	if len(positionals) > 0 {
		verb = positionals[0]
	}
	previous := ""
	if at > 0 && at-1 < len(p.positionals) && len(p.positionals[at-1].literals) == 1 {
		previous = p.positionals[at-1].literals[0]
	}
	switch {
	case placeholder == "":
		return nil
	case strings.Contains(placeholder, "file") || strings.Contains(placeholder, "picture") || strings.Contains(placeholder, "folder"):
		return []string{filesMarker}
	case placeholder == "stone":
		return stones()
	case placeholder == "name" && verb != "register" && (previous == "stone" || previous == "info" || previous == "settings"):
		return stones()
	case placeholder == "topic":
		var out []string
		for _, topic := range helpTopics() {
			out = append(out, topic.name)
		}
		out = append(out, "all")
		for _, p := range helpPatterns() {
			if len(p.positionals) > 0 {
				out = append(out, p.positionals[0].literals...)
			}
		}
		return out
	case placeholder == "feature":
		return features.Names
	case placeholder == "key" && verb == "settings":
		return mapKeys(uiValues)
	case placeholder == "value" && verb == "settings" && at == 2:
		return uiValues[positionals[1]]
	}
	return nil
}

func mapKeys(m map[string][]string) []string {
	keys := make([]string, 0, len(m))
	for key := range m {
		keys = append(keys, key)
	}
	return keys
}

func flagValues(name string, matching []pattern, stones func() []string) []string {
	switch name {
	case "--lang":
		return i18n.Languages()
	case "--output":
		return commonFlags["--output"]
	case "--stone":
		return stones()
	case "--features":
		return features.Names
	}
	var out []string
	for _, p := range matching {
		if s := p.flags[name]; s != nil {
			out = append(out, s.literals...)
			if strings.Contains(s.placeholder, "file") || strings.Contains(s.placeholder, "picture") {
				out = append(out, filesMarker)
			}
		}
	}
	return out
}

func flagNames(matching []pattern) []string {
	var out []string
	for name := range commonFlags {
		out = append(out, name)
	}
	for _, p := range matching {
		for name := range p.flags {
			out = append(out, name)
		}
	}
	return out
}

// helpPatterns reads the command lines of the help.
func helpPatterns() []pattern {
	var out []pattern
	for _, topic := range helpTopics() {
		for _, section := range topic.sections {
			for _, item := range section.items {
				if item.kind != helpCommand {
					continue
				}
				for _, line := range item.lines {
					if p, ok := parsePattern(line); ok {
						out = append(out, p)
					}
				}
			}
		}
	}
	return out
}

// parsePattern reads one command line, such as
// "tolk send <file|-> <address> … [--name <name>] [-p]".
func parsePattern(line string) (pattern, bool) {
	tokens := tokenize(line)
	if len(tokens) < 2 || tokens[0] != "tolk" || strings.HasPrefix(tokens[1], "<") || strings.HasPrefix(tokens[1], "-") {
		return pattern{}, false
	}
	p := pattern{flags: map[string]*slot{}}
	p.read(tokens[1:])
	return p, true
}

func (p *pattern) read(tokens []string) {
	for i := 0; i < len(tokens); i++ {
		token := tokens[i]
		switch {
		case token == "…":
			if n := len(p.positionals); n > 0 && (i == 0 || !strings.HasPrefix(tokens[i-1], "-")) {
				p.positionals[n-1].repeat = true
			}
		case strings.HasPrefix(token, "[") && strings.HasSuffix(token, "]"):
			p.read(tokenize(token[1 : len(token)-1]))
		case strings.HasPrefix(token, "-") && token != "-" && !strings.HasPrefix(token, "-|"):
			var value *slot
			if i+1 < len(tokens) && tokens[i+1] == "…" {
				value = &slot{placeholder: "value"}
				i++
			} else if i+1 < len(tokens) && !strings.HasPrefix(tokens[i+1], "-") && !strings.HasPrefix(tokens[i+1], "[") {
				s := slotOf(tokens[i+1])
				value = &s
				i++
			}
			for _, name := range strings.Split(token, "|") {
				p.flags[name] = value
			}
		default:
			p.positionals = append(p.positionals, slotOf(token))
		}
	}
}

// slotOf reads "a|b|c", "<feature>|all" or "<file|->".
func slotOf(token string) slot {
	var s slot
	if strings.HasPrefix(token, "<") && strings.HasSuffix(token, ">") && !strings.Contains(token, ">|") {
		s.placeholder = strings.SplitN(strings.Trim(token, "<>"), "|", 2)[0]
		return s
	}
	for _, part := range strings.Split(token, "|") {
		if strings.HasPrefix(part, "<") {
			if s.placeholder == "" {
				s.placeholder = strings.Trim(part, "<>")
			}
		} else if !strings.Contains(part, ",") && !strings.Contains(part, ":") {
			s.literals = append(s.literals, part)
		}
	}
	if len(s.literals) == 1 && s.placeholder == "" && !strings.Contains(token, "|") {
		// One word on its own is a literal only when it is lower case,
		// not an example value such as EUR or 8h.
		if strings.ToLower(token) != token || strings.ContainsAny(token, "0123456789") {
			s.literals = nil
			s.placeholder = "value"
		}
	}
	return s
}

// tokenize splits at spaces, but not inside <…> or […].
func tokenize(line string) []string {
	var tokens []string
	var b strings.Builder
	depth := 0
	for _, r := range line {
		switch {
		case r == '<' || r == '[':
			depth++
		case r == '>' || r == ']':
			depth--
		}
		if r == ' ' && depth == 0 {
			if b.Len() > 0 {
				tokens = append(tokens, b.String())
				b.Reset()
			}
			continue
		}
		b.WriteRune(r)
	}
	if b.Len() > 0 {
		tokens = append(tokens, b.String())
	}
	return tokens
}
