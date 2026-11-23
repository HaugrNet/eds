// Tolk is the command line of Rune and Haugr EDS.
package main

import (
	"bufio"
	"fmt"
	"os"
	"strings"

	"golang.org/x/term"

	"github.com/HaugrNet/eds/eds-cli/internal/commands"
)

// version is set at build time with -ldflags "-X main.version=…".
var version = "dev"

func main() {
	commands.Version = version
	env := &commands.Env{
		In:        os.Stdin,
		Out:       os.Stdout,
		Err:       os.Stderr,
		StdinTTY:  term.IsTerminal(int(os.Stdin.Fd())),
		StdoutTTY: term.IsTerminal(int(os.Stdout.Fd())),
		ErrTTY:    term.IsTerminal(int(os.Stderr.Fd())),
		ReadPassword: func(label string) (string, error) {
			fmt.Fprintf(os.Stderr, "%s: ", label)
			data, err := term.ReadPassword(int(os.Stdin.Fd()))
			fmt.Fprintln(os.Stderr)
			return string(data), err
		},
		ReadLine: func(label string) (string, error) {
			fmt.Fprintf(os.Stderr, "%s: ", label)
			line, err := bufio.NewReader(os.Stdin).ReadString('\n')
			return strings.TrimRight(line, "\r\n"), err
		},
	}
	os.Exit(commands.Run(os.Args[1:], env))
}
