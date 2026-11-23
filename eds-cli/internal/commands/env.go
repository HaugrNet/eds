// Package commands holds Tolk's non-interactive commands, on the standard
// library's flag package.
package commands

import (
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/format"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
	"github.com/HaugrNet/eds/eds-cli/internal/service"
	"github.com/HaugrNet/eds/eds-cli/internal/theme"
)

// Version is Tolk's version, set at build time.
var Version = "dev"

// Env is what a command runs in; tests replace all of it.
type Env struct {
	In           io.Reader
	Out, Err     io.Writer
	StdinTTY     bool
	StdoutTTY    bool
	ErrTTY       bool                               // standard error is a terminal: progress may be shown
	ReadPassword func(label string) (string, error) // asks without echo
	ReadLine     func(label string) (string, error) // asks with echo
}

// options are the flags every command has.
type options struct {
	output  string
	noInput bool
	lang    string
}

type command struct {
	env  *Env
	opts options
	ui   *config.UI
}

// settings are Tolk's own settings, read once.
func (c *command) uiSettings() config.UI {
	if c.ui == nil {
		ui := config.DefaultUI()
		if file, err := config.Load(); err == nil {
			ui = file.UI
		}
		c.ui = &ui
	}
	return *c.ui
}

// locale is how numbers, sizes, dates and times are written for a person.
func (c *command) locale() format.Locale {
	return format.Locale{Language: i18n.Language(), Region: format.Region(), Clock: c.uiSettings().Clock}
}

// style is Rune's palette for standard output; without colours when the
// output isn't a terminal or NO_COLOR is set.
func (c *command) style() theme.Style {
	ui := c.uiSettings()
	return theme.New(ui.Theme, ui.Appearance, theme.Detect(c.env.StdoutTTY && !c.json()))
}

// interactive reports whether a person is at the terminal and may be asked.
func (c *command) interactive() bool {
	return c.env.StdinTTY && c.env.StdoutTTY && os.Getenv("CI") == "" && !c.opts.noInput
}

// prompt is the passphrase prompt, or nil when nobody may be asked.
func (c *command) prompt() service.Prompt {
	if !c.interactive() || c.env.ReadPassword == nil {
		return nil
	}
	return c.env.ReadPassword
}

func (c *command) json() bool { return c.opts.output == "json" }

// printJSON writes a value as indented JSON to standard output.
func (c *command) printJSON(v any) error {
	enc := json.NewEncoder(c.env.Out)
	enc.SetEscapeHTML(false)
	enc.SetIndent("", "  ")
	return enc.Encode(v)
}

func (c *command) say(format string, args ...any) {
	fmt.Fprintf(c.env.Out, format+"\n", args...)
}

// note writes a message for the person, on standard error, so that standard
// output stays data.
func (c *command) note(format string, args ...any) {
	if !c.json() {
		fmt.Fprintf(c.env.Err, format+"\n", args...)
	}
}

// flags makes a flag set with the common flags.
func (c *command) flags(name string) *flag.FlagSet {
	fs := flag.NewFlagSet(name, flag.ContinueOnError)
	fs.SetOutput(io.Discard)
	fs.StringVar(&c.opts.output, "output", c.opts.output, "")
	fs.BoolVar(&c.opts.noInput, "no-input", c.opts.noInput, "")
	fs.StringVar(&c.opts.lang, "lang", c.opts.lang, "")
	return fs
}

// parse reads flags and positional arguments in any order, which the flag
// package alone doesn't do.
func parse(fs *flag.FlagSet, args []string) ([]string, error) {
	var positional []string
	for {
		if err := fs.Parse(args); err != nil {
			if errors.Is(err, flag.ErrHelp) {
				return nil, err
			}
			return nil, exit.New(exit.Usage, "%v", err)
		}
		if fs.NArg() == 0 {
			return positional, nil
		}
		positional = append(positional, fs.Arg(0))
		args = fs.Args()[1:]
	}
}

// noun normalises the word after a verb: a stone is also a server or haugr.
func noun(word string) string {
	switch strings.ToLower(word) {
	case "stone", "stones", "server", "servers", "haugr", "haugrs":
		return "stone"
	case "group", "groups":
		return "group"
	}
	return strings.ToLower(word)
}

// fail reports an error and returns its exit code.
func (c *command) fail(err error) int {
	var done reported
	if errors.As(err, &done) {
		return done.code
	}
	// What a stone said is put in Rune's words; returnMessage keeps EDS's.
	e := c.friendly(err)
	if c.json() {
		enc := json.NewEncoder(c.env.Err)
		enc.SetEscapeHTML(false)
		_ = enc.Encode(map[string]any{"error": map[string]any{
			"code": e.Code, "message": e.Message, "returnCode": e.ReturnCode, "returnMessage": e.ReturnMessage,
		}})
	} else {
		fmt.Fprintf(c.env.Err, "tolk: %s\n", e.Message)
	}
	return e.Code
}

func usage(format string, args ...any) error {
	return exit.New(exit.Usage, "%s", i18n.T(format, args...))
}
