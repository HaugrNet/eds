package commands

import (
	"errors"
	"flag"
	"strings"

	"github.com/HaugrNet/eds/eds-cli/internal/agent"
	"github.com/HaugrNet/eds/eds-cli/internal/config"
	"github.com/HaugrNet/eds/eds-cli/internal/exit"
	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// Run runs one command line and returns the exit code.
func Run(args []string, env *Env) int {
	c := &command{env: env, opts: options{output: "text"}}

	// The language is needed before anything is said.
	lang := ""
	if file, err := config.Load(); err == nil {
		lang = file.UI.Language
	}
	for i, arg := range args {
		if arg == "--lang" && i+1 < len(args) {
			lang = args[i+1]
		} else if strings.HasPrefix(arg, "--lang=") {
			lang = strings.TrimPrefix(arg, "--lang=")
		}
		if arg == "--output=json" || (arg == "--output" && i+1 < len(args) && args[i+1] == "json") {
			c.opts.output = "json"
		}
	}
	i18n.Set(lang)

	if len(args) == 1 && args[0] == agent.ChildArg {
		if err := agent.RunChild(); err != nil {
			return c.fail(err)
		}
		return exit.OK
	}

	// The completion runs while the user types: the words aren't a whole
	// command line yet, so they aren't parsed as one.
	if len(args) > 0 && args[0] == completeVerb {
		if err := c.complete(args[1:]); err != nil {
			return c.fail(err)
		}
		return exit.OK
	}

	err := c.dispatch(args)
	if errors.Is(err, flag.ErrHelp) {
		// 'tolk send --help' shows the topic that send belongs to.
		c.helpFor(args)
		return exit.OK
	}
	if err != nil {
		return c.fail(err)
	}
	return exit.OK
}

func (c *command) dispatch(args []string) error {
	// Common flags may come before the command.
	fs := c.flags("tolk")
	if err := fs.Parse(args); err != nil {
		if errors.Is(err, flag.ErrHelp) {
			return err
		}
		return exit.New(exit.Usage, "%v", err)
	}
	args = fs.Args()
	if len(args) == 0 {
		// The overview of the help, which also tells how to start. The
		// interactive mode is 'tolk chat'.
		c.help()
		return nil
	}
	verb, rest := strings.ToLower(args[0]), args[1:]
	switch verb {
	case "help":
		return c.helpCommand(rest)
	case "man":
		return c.man(rest)
	case "completion":
		return c.completion(rest)
	case "version":
		return c.version(rest)
	case "init":
		return c.init(rest)
	case "login":
		return c.login(rest)
	case "logout":
		return c.logout(rest)
	case "settings":
		return c.settings(rest)
	case "rooms":
		return c.rooms(rest)
	case "room":
		return c.roomCommand(rest)
	case "members":
		return c.members(rest)
	case "users":
		return c.users(rest)
	case "user":
		return c.user(rest)
	case "add", "access":
		return c.membership(verb, rest)
	case "leave":
		return c.leave(rest)
	case "invite":
		return c.invite(rest)
	case "accept":
		return c.accept(rest)
	case "passphrase":
		return c.passphrase(rest)
	case "key", "keys":
		return c.key(rest)
	case "features", "feature":
		return c.roomFeatures(rest)
	case "entry", "entries":
		return c.entry(rest)
	case "status":
		return c.statusCommand(rest)
	case "calendar", "cal":
		return c.calendar(rest)
	case "location":
		return c.location(rest)
	case "plan", "plans":
		return c.plan(rest)
	case "avatar":
		return c.avatar(rest)
	case "about":
		return c.about(rest)
	case "groups":
		return c.groups(rest)
	case "group":
		return c.group(rest)
	case "chat":
		return c.chat(rest)
	case "ls":
		return c.ls(rest)
	case "mkdir":
		return c.mkdir(rest)
	case "mv":
		return c.mv(rest)
	case "rename":
		return c.rename(rest)
	case "rm":
		return c.rm(rest)
	case "send":
		return c.send(rest)
	case "get":
		return c.get(rest)
	case "cp":
		return c.cp(rest)
	case "register", "update", "remove", "list":
		if len(rest) == 0 || strings.HasPrefix(rest[0], "-") {
			return usage("Use 'tolk %s stone …'.", verb)
		}
		// 'tolk remove <user> --room <room>' takes a member out of a room;
		// 'tolk remove stone <name>' forgets a stone.
		if verb == "remove" && hasFlag(rest, "room", "circle") {
			return c.membership(verb, rest)
		}
		if verb == "list" {
			switch noun(rest[0]) {
			case "group":
				return c.groups(rest[1:])
			case "rooms", "room":
				return c.rooms(rest[1:])
			case "users", "user":
				return c.users(rest[1:])
			}
		}
		if noun(rest[0]) != "stone" {
			return usage("Unknown: 'tolk %s %s'. Try 'tolk %s stone'.", verb, rest[0], verb)
		}
		switch verb {
		case "register":
			return c.registerStone(rest[1:])
		case "update":
			return c.updateStone(rest[1:])
		case "remove":
			return c.removeStone(rest[1:])
		default:
			return c.listStones(rest[1:])
		}
	case "stone", "stones", "server", "haugr":
		if len(rest) > 0 {
			switch strings.ToLower(rest[0]) {
			case "info":
				return c.stoneInfo(rest[1:])
			case "settings":
				return c.stoneSettings(rest[1:])
			case "set":
				return c.stoneSet(rest[1:])
			case "features":
				return c.stoneFeatures(rest[1:])
			case "key", "keys":
				return c.stoneKey(rest[1:])
			case "name":
				return c.stoneNameCommand(rest[1:])
			}
		}
		if len(rest) == 0 || strings.ToLower(rest[0]) == "list" {
			if len(rest) > 0 {
				rest = rest[1:]
			}
			return c.listStones(rest)
		}
		return usage("Unknown stone command %q. Try 'tolk stone info'.", rest[0])
	}
	return usage("Unknown command %q. Try 'tolk help'.", verb)
}

func (c *command) version(args []string) error {
	if _, err := parse(c.flags("version"), args); err != nil {
		return err
	}
	if c.json() {
		return c.printJSON(map[string]string{"tolk": Version})
	}
	c.say("tolk %s", Version)
	return nil
}

// hasFlag reports whether one of the flags is among the arguments.
func hasFlag(args []string, names ...string) bool {
	for _, arg := range args {
		for _, name := range names {
			if arg == "--"+name || arg == "-"+name || strings.HasPrefix(arg, "--"+name+"=") || strings.HasPrefix(arg, "-"+name+"=") {
				return true
			}
		}
	}
	return false
}
