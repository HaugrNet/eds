package commands

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"unicode/utf8"

	"github.com/HaugrNet/eds/eds-cli/internal/i18n"
)

// Tolk's help is one set of texts, shown three ways: 'tolk help' is the
// overview with the first steps, 'tolk help <topic>' one topic, and
// 'tolk man' all of it as a manual page.

type helpKind int

const (
	helpText    helpKind = iota // a paragraph
	helpCommand                 // command lines, and what they do
	helpStep                    // a numbered step, with the lines to type
	helpExample                 // lines to type
)

type helpItem struct {
	kind  helpKind
	lines []string // the command lines; they are the same in every language
	text  string
}

type helpSection struct {
	title string
	items []helpItem
}

type helpTopic struct {
	name     string
	summary  string
	words    []string // commands and other words that lead to this topic
	sections []helpSection
}

// short reports whether the item is one short word with its text.
func (item helpItem) short() bool {
	return item.kind == helpCommand && len(item.lines) == 1 && width(item.lines[0]) == 1
}

func text(text string) helpItem { return helpItem{kind: helpText, text: text} }

// cmd is one command; several forms are separated by a line break.
func cmd(lines, text string) helpItem {
	return helpItem{kind: helpCommand, lines: strings.Split(lines, "\n"), text: text}
}

func step(text string, lines ...string) helpItem {
	return helpItem{kind: helpStep, lines: lines, text: text}
}

func example(lines ...string) helpItem { return helpItem{kind: helpExample, lines: lines} }

// helpIntro is what Tolk is, in one line and in a paragraph.
func helpIntro() (line, paragraph string) {
	return i18n.T("the command line of Rune and Haugr EDS"),
		i18n.T("Tolk works with the rooms, users, files and chat of Haugr EDS from the command line. It has a command for each task, made for scripts as much as for people, and an interactive mode, which is Rune in the terminal.")
}

func helpUsage() helpSection {
	return helpSection{i18n.T("Usage"), []helpItem{
		cmd("tolk <command> [<arguments>] [<flags>]", i18n.T("Run one command. Flags may stand anywhere on the line.")),
		cmd("tolk chat", i18n.T("Open the interactive mode: Rune in the terminal.")),
		cmd("tolk help [<topic>|<command>]", i18n.T("Explain a topic, or the topic a command belongs to.")),
	}}
}

// helpTopics are the topics, in the order of the manual page.
func helpTopics() []helpTopic {
	return []helpTopic{
		{
			name:    "start",
			summary: i18n.T("The first steps, from nothing to your first file"),
			words:   []string{"begin", "howto", "how-to", "first"},
			sections: []helpSection{{i18n.T("How to start"), []helpItem{
				text(i18n.T("Tolk calls a server a stone. Your stones, with your account and passphrase on each, are kept in an encrypted container on this computer.")),
				step(i18n.T("Create the container. Tolk first names the file it will write, config.json in its configuration directory, on Linux ~/.config/tolk, and then asks you to choose the container's passphrase."),
					"tolk init"),
				step(i18n.T("Add a stone under a name of your choice, here \"home\". Tolk asks for its URL, your account and your passphrase there, and logs in once to check them. With an invitation link, 'tolk accept <link>' does this step instead."),
					"tolk register stone home"),
				step(i18n.T("Unlock the container, so that Tolk doesn't ask for its passphrase with every command."),
					"tolk login"),
				step(i18n.T("Look around and move a file; \"family\" stands for one of your rooms."),
					"tolk rooms",
					"tolk ls family",
					"tolk send photo.jpg family",
					"tolk get family/photo.jpg"),
				step(i18n.T("Or open the interactive mode, with rooms, chat and files on one screen."),
					"tolk chat"),
				text(i18n.T("When you are done, 'tolk logout' locks the container again.")),
			}}},
		},
		{
			name:    "stones",
			summary: i18n.T("Your servers, and unlocking: init, register, update, remove and list stones, accept, login, logout"),
			words:   []string{"stone", "server", "servers", "haugr", "init", "register", "update", "remove", "list", "accept", "login", "logout", "container", "agent"},
			sections: []helpSection{
				{i18n.T("Stones"), []helpItem{
					text(i18n.T("A stone is a server instance; \"server\" and \"haugr\" mean the same. The first stone you add is the default stone, the one meant when none is named.")),
					cmd("tolk init",
						i18n.T("Create the encrypted container for the stones. It is kept in the file config.json in Tolk's configuration directory, on Linux ~/.config/tolk; 'tolk help scripts' tells more.")),
					cmd("tolk register stone <name> [--url <url>] [--account <name>] [--default] [--no-check]",
						i18n.T("Add a stone: its URL, your account and your passphrase there. Tolk asks for what is not given, and logs in once to check it unless --no-check is given. --default makes it the default stone.")),
					cmd("tolk register stone <name> --url <url> --status-key",
						i18n.T("Add a stone which you only look after: without an account, with a status key, which reads its status and nothing else ('tolk help status').")),
					cmd("tolk update stone <name> [--url <url>] [--account <name>] [--passphrase] [--rename <name>] [--default]",
						i18n.T("Change a stone: its URL, account, passphrase or name, or make it the default stone.")),
					cmd("tolk remove stone <name>",
						i18n.T("Forget a stone.")),
					cmd("tolk list stones",
						i18n.T("List the stones, never their passphrases.")),
					cmd("tolk stone info [<name>]",
						i18n.T("The version and kind of system of a stone.")),
					cmd("tolk accept <invitation link> [--stone <name>] [--default]",
						i18n.T("Join with an invitation: you choose your passphrase, and the stone of the invitation is added.")),
					text(i18n.T("Where Tolk asks for a passphrase on a stone, --passphrase-stdin reads it from standard input instead.")),
				}},
				{i18n.T("Unlocking"), []helpItem{
					cmd("tolk login [--lifetime 8h]",
						i18n.T("Unlock the container and keep it in an agent, as ssh-agent does, until 'tolk logout' or until the lifetime has passed.")),
					cmd("tolk logout",
						i18n.T("End the sessions on the stones and stop the agent.")),
					text(i18n.T("Without an agent, Tolk asks for the container's passphrase with each command.")),
				}},
			},
		},
		{
			name:    "addresses",
			summary: i18n.T("How rooms, files and folders are named"),
			words:   []string{"address", "names", "naming"},
			sections: []helpSection{{i18n.T("Addresses"), []helpItem{
				text(i18n.T("A room is written [stone:]room, a file or folder [stone:]room/path. Without \"stone:\" the default stone is meant.")),
				cmd("family",
					i18n.T("The room \"family\" on the default stone.")),
				cmd("work:plans/2026/budget.ods",
					i18n.T("A file in the folder \"2026\" of the room \"plans\" on the stone \"work\".")),
				cmd("work:plans/2026/",
					i18n.T("That folder; the slash at the end says that a folder is meant.")),
				cmd("@announce",
					i18n.T("Every room of the group \"announce\"; see 'tolk help rooms' for groups.")),
				cmd("'work:*'",
					i18n.T("Every room you are in on the stone \"work\". The quotes keep the shell from the star.")),
				text(i18n.T("Where several addresses may be given, Tolk goes on with the rest when one fails, reports each of them, and ends with 0 only if all succeeded.")),
				text(i18n.T("Write \\: and \\/ for a colon or slash inside a name, or use the ID of the room or file in place of its name.")),
			}}},
		},
		{
			name:    "rooms",
			summary: i18n.T("Rooms, members and users: rooms, room, members, add, access, remove, leave, invite, users, user, avatar, passphrase, group"),
			words:   []string{"room", "members", "member", "users", "user", "add", "access", "leave", "invite", "avatar", "passphrase", "groups", "group"},
			sections: []helpSection{
				{i18n.T("Rooms"), []helpItem{
					cmd("tolk rooms [--stone <name>]",
						i18n.T("The rooms on a stone, and your access to each.")),
					cmd("tolk room new <room> [--chamber] [--features chat,calendar,plans,location,calls]",
						i18n.T("Make a room; you administer it. --chamber makes a chamber, whose content Tolk encrypts before the stone gets it ('tolk help chambers'). --features names the features of the room; without it, the room has the chat.")),
					cmd("tolk room rename <room> <name>",
						i18n.T("Rename a room.")),
					cmd("tolk room avatar <picture>|none --room <room>",
						i18n.T("Give a room a picture, or none.")),
					cmd("tolk leave --room <room> [--yes]",
						i18n.T("Leave a room.")),
				}},
				{i18n.T("Members"), []helpItem{
					cmd("tolk members --room <room>",
						i18n.T("Who is in a room, their access, and who of them is online: with Rune, or Tolk's interactive mode, open and connected.")),
					cmd("tolk add <user> --room <room> [--access read|write|admin]",
						i18n.T("Add a user of the stone to a room.")),
					cmd("tolk access <user> read|write|admin --room <room>",
						i18n.T("Change the access of a member.")),
					cmd("tolk remove <user> --room <room>",
						i18n.T("Take a member out of a room.")),
					cmd("tolk invite <name> --room <room> [--access read|write|admin] [--qr|--no-qr]",
						i18n.T("Invite somebody new into a room. Tolk writes the invitation link, and at a terminal its QR code. The link works once and expires after 7 days.")),
				}},
				{i18n.T("Users"), []helpItem{
					cmd("tolk users [--stone <name>]",
						i18n.T("All users on a stone.")),
					cmd("tolk user <name> [--stone <name>]",
						i18n.T("The rooms of one user.")),
					cmd("tolk user new <name> [--admin] [--stone <name>]\ntolk user rm <name> [--stone <name>] [--yes]",
						i18n.T("Make or delete a user. Only a System Administrator may do this, and only the first one may make an administrator (--admin) or delete one.")),
					cmd("tolk user role <name> admin|user [--stone <name>]",
						i18n.T("Make a user an administrator, or an administrator a user. Only the first System Administrator may.")),
					cmd("tolk avatar [<picture>|none] [--stone <name>]",
						i18n.T("Show your picture, choose one, or have none.")),
					cmd("tolk passphrase [--stone <name>]",
						i18n.T("Change your passphrase on a stone; Tolk keeps the new one in the container.")),
				}},
				{i18n.T("Groups of rooms"), []helpItem{
					text(i18n.T("A group is a list of rooms under one name, written @name wherever rooms are named.")),
					cmd("tolk groups",
						i18n.T("List the groups.")),
					cmd("tolk group set <name> <room> …",
						i18n.T("Make a group, or replace the rooms of one.")),
					cmd("tolk group add <name> <room> …",
						i18n.T("Add rooms to a group.")),
					cmd("tolk group remove <name> [<room> …]",
						i18n.T("Take rooms out of a group; without rooms, remove the group.")),
				}},
			},
		},
		{
			name:    "chambers",
			summary: i18n.T("Chambers, rooms the stone can't read, and your keys for them: key"),
			words:   []string{"chamber", "key", "keys", "recovery", "fingerprint", "e2e"},
			sections: []helpSection{
				{i18n.T("Chambers"), []helpItem{
					text(i18n.T("A chamber is a room whose messages, files and entries Tolk encrypts on your device, with a key only the people of the chamber have: the stone stores them and can't read them. Rune does the same, so both work in the same chambers. What a chamber doesn't hide: who is in it, who wrote when, and the names, types and sizes of its files.")),
					text(i18n.T("A room is a chamber from the start, or never: 'tolk room new <room> --chamber'. Everything else works as in a room: send, get, cp, chat, the features. A copy between a room and a chamber, or between stones, goes through Tolk, which decrypts and encrypts on the way.")),
					text(i18n.T("An invitation into a chamber carries the key of the chamber in its link, so give the link to the invited person directly. Someone who is added and has no keys yet gets the key of the chamber when an administrator next looks at its members. When a member is removed, the chamber gets a new key.")),
				}},
				{i18n.T("Your keys"), []helpItem{
					text(i18n.T("You get keys with your first chamber on a stone. The private key is kept on the stone, locked with a key only you have: the recovery key. Tolk keeps it in the container.")),
					cmd("tolk key [<stone>]",
						i18n.T("Whether you have keys on a stone, whether this device has them, and your fingerprint.")),
					cmd("tolk key create [<stone>]",
						i18n.T("Make your keys without making a chamber, e.g. so that others can add you to theirs.")),
					cmd("tolk key recovery [<stone>]",
						i18n.T("Show the recovery key, to write down. Whoever has it can read your chambers on that stone.")),
					cmd("tolk key link [<stone>] [--qr|--no-qr]",
						i18n.T("Show a link, and at a terminal its QR code, which gives a further device of yours the keys.")),
					cmd("tolk key unlock [<stone>] [--key-stdin]",
						i18n.T("Give this device your keys, with the recovery key or the link another device shows.")),
					cmd("tolk members --room <room>",
						i18n.T("In a chamber, the members are shown with the fingerprints of their keys. Compare a fingerprint with its owner, by voice or in person, to know that nobody put a key in between.")),
					cmd("tolk key accept <user> --room <room>",
						i18n.T("Accept the key of a member which changed since this device last saw it, after you compared its fingerprint. Until then, that member gets no key of a chamber from this device.")),
				}},
			},
		},
		{
			name:    "features",
			summary: i18n.T("What a room has beside its files: features, calendar, plan, location, entry"),
			words:   []string{"feature", "calendar", "cal", "plan", "plans", "location", "entry", "entries", "ics"},
			sections: []helpSection{
				{i18n.T("Features"), []helpItem{
					text(i18n.T("A room can have features beside its files: the chat, a calendar, plans with shared costs, and where its people are. The stone offers features, and the administrators of a room switch them on there. The stone stores what a feature keeps and enforces its rules, without knowing what it is; in a chamber it can't read any of it.")),
					cmd("tolk stone features [--stone <name>]\ntolk stone features offer <feature>|all\ntolk stone features withdraw <feature>",
						i18n.T("The features a stone offers; offering and withdrawing is for its System Administrator.")),
					cmd("tolk features --room <room>\ntolk features on|off|readonly <feature> --room <room>\ntolk features remove <feature> --room <room> [--yes]",
						i18n.T("The features of a room, and switching one. A feature which is off keeps what it has and hides it; read-only shows it and takes nothing new; removing deletes what it has in the room.")),
				}},
				{i18n.T("Calendar"), []helpItem{
					cmd("tolk calendar --room <room> [--from <day>] [--to <day>]",
						i18n.T("What takes place, in the next 30 days unless told otherwise.")),
					cmd("tolk calendar add <title> --room <room> --start <time> [--end <time>] [--all-day] [--place <place>] [--note <text>] [--repeat daily|weekly|monthly|yearly] [--every <n>] [--until <day>] [--zone <time zone>] [--call audio|video]",
						i18n.T("Add an event. A time is written as 2026-12-04, or \"2026-12-04 19:00\"; a day alone makes an event of whole days. --call makes it a call of the room, with voice or with video too; calls are made with Rune.")),
					cmd("tolk calendar change <id> --room <room> [--title <title>] [--start <time>] [--call audio|video|none] …\ntolk calendar remove <id> --room <room>",
						i18n.T("Change or remove an event; everyone who may write in the room may.")),
					cmd("tolk calendar import <file.ics|-> --room <room>\ntolk calendar export --room <room> [-o <file>]",
						i18n.T("Bring events in from an .ics file of another calendar, or take all events of the room out as one.")),
				}},
				{i18n.T("Plans"), []helpItem{
					text(i18n.T("A plan is a trip or an event: what is to do, what was spent, who owes whom. A person is a member of the room by name, or anyone else as a guest.")),
					cmd("tolk plan --room <room>\ntolk plan new <title> --room <room> [--from <day>] [--to <day>] [--place <place>] [--currency EUR] [--people anna,bo,Dana]\ntolk plan show <plan> --room <room>",
						i18n.T("The plans of a room, a new one, and everything of one: its items, its expenses, what everyone owes or is owed, and the payments.")),
					cmd("tolk plan item <plan> <text> --room <room> [--who <name>]\ntolk plan done <plan> <item> --room <room> [--undo]",
						i18n.T("Something to do, to book or to bring, and marking it done.")),
					cmd("tolk plan expense <plan> <amount> <text> --room <room> [--paid-by <name>] [--split anna,bo:2] [--at <day>]\ntolk plan correct <plan> <expense> --room <room> [--amount <amount>] [--text <text>] [--paid-by <name>] [--split …]",
						i18n.T("An expense: who paid, how much, and who shares it; bo:2 gives bo two shares. Without --split everyone of the plan shares evenly. Its author corrects it.")),
					cmd("tolk plan pay <plan> <amount> <to> --room <room> [--paid-by <name>]\ntolk plan confirm <plan> <payment> --room <room>",
						i18n.T("A payment made to settle, and its confirmation by who got the money.")),
					cmd("tolk plan close <plan> --room <room>\ntolk plan open <plan> --room <room>\ntolk plan remove <plan> [<entry>] --room <room>",
						i18n.T("Close a plan which is settled, so its records stay as they are; open it again; remove a plan, or one entry of it.")),
				}},
				{i18n.T("Location"), []helpItem{
					cmd("tolk location --room <room>",
						i18n.T("Where the people of a room were last seen: the place as numbers, the distance from your own position, and an address for a map app.")),
					cmd("tolk location report <latitude> <longitude> --room <room> [--accuracy <metres>] [--note <text>] [--track] [--passing]",
						i18n.T("Report where you are; a script can do this from a device which knows. --track also keeps it as a point of your track of the last day, --passing passes it on to who is there now and keeps nothing.")),
					cmd("tolk location remove --room <room>",
						i18n.T("Remove what is kept of where you were.")),
					text(i18n.T("In a room which isn't a chamber, the stone can read the positions while it handles them. For where people are, a chamber is the better place.")),
				}},
				{i18n.T("Entries of any feature"), []helpItem{
					cmd("tolk entry list <feature> --room <room> [--kind <kind>] [--about <id>] [--name <prefix>]\ntolk entry add <feature> <kind> --room <room> [--content <json>] [--about <id>] [--name <name>]\ntolk entry change <feature> <id> --room <room> [--content <json>] [--name <name>]\ntolk entry remove|close|open <feature> <id> --room <room>",
						i18n.T("The entries of a feature as JSON, also of a feature Tolk doesn't know. The content is read from standard input if --content isn't given. In a chamber Tolk encrypts and decrypts it.")),
				}},
			},
		},
		{
			name:    "status",
			summary: i18n.T("How your stones are: status, and the keys which read it"),
			words:   []string{"watch", "load", "inventory", "monitor"},
			sections: []helpSection{
				{i18n.T("The status of stones"), []helpItem{
					text(i18n.T("A stone tells its System Administrator how it is: whether it is awake, its storage against the ceiling, how many people are active, the requests, its share of the processors, its memory. Only general numbers: no names, and nothing about a single person, room or file.")),
					cmd("tolk status [<stone> …]",
						i18n.T("A table of your stones, the busiest first; with --output json, the latest numbers for scripts.")),
					cmd("tolk status [<stone> …] --watch [--interval <seconds>]",
						i18n.T("The live view, with bars and small curves, until Ctrl-C. In the interactive mode, \"Stones…\" in the menu shows the same.")),
					cmd("tolk stone key [--stone <name>]\ntolk stone key create <label> [--keep]\ntolk stone key withdraw <label>",
						i18n.T("The status keys of a stone. A status key reads the status and nothing else, also while the stone waits for its MasterKey, so who looks after a stone needs no account there. A new key is shown once; --keep also keeps it with the stone's entry, and 'tolk status' then asks with it.")),
					cmd("tolk update stone <name> --status-key\ntolk update stone <name> --no-status-key",
						i18n.T("Keep a status key with a stone's entry, or drop it.")),
					cmd("tolk stone name [<name>] [--stone <name>]",
						i18n.T("How a stone calls itself in its status.")),
				}},
			},
		},
		{
			name:    "files",
			summary: i18n.T("Files and folders: ls, send, get, cp, mv, rename, mkdir, rm"),
			words:   []string{"file", "folder", "folders", "ls", "send", "get", "cp", "mv", "rename", "mkdir", "rm", "upload", "download"},
			sections: []helpSection{
				{i18n.T("Files"), []helpItem{
					cmd("tolk ls <address> …",
						i18n.T("List a room or folder.")),
					cmd("tolk send <file|-> <address> … [--name <name>] [--type <media type>] [--replace] [-p] [--share] [-m <text>]",
						i18n.T("Upload a file, or standard input when \"-\" is given; the input gets its name from --name or from the end of the address. --replace replaces the content of a file that is there already, -p makes the folders that are missing, --share posts the file in the room's chat, and -m adds a text to that.")),
					cmd("tolk get <address> [-o <file|folder|->] [--force]",
						i18n.T("Download a file; \"-o -\" writes it to standard output. A download that was interrupted is continued by running the command again. --force replaces a local file without asking.")),
					cmd("tolk cp <address> <address> … [--replace] [-p]",
						i18n.T("Copy a file to one or more places, also between stones.")),
					cmd("tolk mv <address> <address>",
						i18n.T("Move a file or folder within a stone, or rename it.")),
					cmd("tolk rename <address> <name>",
						i18n.T("Rename a file or folder.")),
					cmd("tolk mkdir <address> [-p]",
						i18n.T("Make a folder; -p also makes the folders above it.")),
					cmd("tolk rm <address> … [--yes]",
						i18n.T("Delete files or folders. Tolk asks first, unless --yes is given.")),
				}},
				{i18n.T("Examples with files"), []helpItem{
					example(
						"tolk send report.pdf work:plans/2026/",
						"tolk send notes.txt family --share -m \"The notes from today\"",
						"pg_dump shop | tolk send - backup:db/shop.sql",
						"tolk get work:plans/2026/report.pdf -o ~/Downloads/",
						"tolk cp home:photos/cat.jpg work:general/"),
				}},
			},
		},
		{
			name:    "chat",
			summary: i18n.T("The interactive mode, and the chat for scripts"),
			words:   []string{"interactive", "tui", "plain"},
			sections: []helpSection{
				{i18n.T("Interactive mode"), []helpItem{
					cmd("tolk chat [--stone <name>]",
						i18n.T("Rune in the terminal: the rooms and users, the chat and the files of one stone. F1 lists the keys.")),
					text(i18n.T("While it is open you are shown as online to the people you share a room with, and a dot marks who of them is online. It says when someone starts a call; calls are made with Rune.")),
					text(i18n.T("'tolk' alone shows the overview of this help.")),
				}},
				{i18n.T("Chat for scripts"), []helpItem{
					cmd("tolk chat --plain --room <room> … [-m <text>] [--history <n>] [--listen]",
						i18n.T("Lines of standard input are sent as messages to the rooms, and what happens in the rooms is written to standard output as JSON lines. -m sends one message, --history first writes the last messages of each room, and --listen goes on after the input has ended.")),
					example(
						"tolk chat --plain --room family -m \"Dinner is ready\"",
						"tolk chat --plain --room work:alerts --history 20 --listen"),
				}},
			},
		},
		{
			name:    "settings",
			summary: i18n.T("Settings and the rest: settings, stone settings, about, version, man, completion"),
			words:   []string{"setting", "about", "version", "help", "man", "completion", "other"},
			sections: []helpSection{{i18n.T("Settings and the rest"), []helpItem{
				cmd("tolk settings [<key> [<value>]]",
					i18n.T("Show or change Tolk's own settings: language, clock, appearance, theme, bell, layout and present. With present false, the interactive mode doesn't show you as online.")),
				cmd("tolk stone settings [<name>]",
					i18n.T("The settings of a stone. Only the System Administrator may see them.")),
				cmd("tolk stone set <key> <value> [--stone <name>]",
					i18n.T("Change one setting of a stone.")),
				cmd("tolk about [--stone <name>]",
					i18n.T("How a stone keeps what is in a room.")),
				cmd("tolk version",
					i18n.T("The version of Tolk.")),
				cmd("tolk help [<topic>|<command>]\ntolk <command> --help",
					i18n.T("This help: the overview, one topic, or with 'tolk help all' everything.")),
				cmd("tolk man",
					i18n.T("The manual page. At a terminal it is opened with man; otherwise it is written to standard output, as with 'tolk man > tolk.1'.")),
				cmd("tolk completion bash",
					i18n.T("The completion of the command line for bash. To use it, add 'source <(tolk completion bash)' to ~/.bashrc.")),
			}}},
		},
		{
			name:    "scripts",
			summary: i18n.T("For scripts: common flags, output, exit codes, environment and files"),
			words:   []string{"script", "flags", "flag", "options", "output", "json", "exit", "codes", "environment", "env"},
			sections: []helpSection{
				{i18n.T("Common flags"), []helpItem{
					cmd("--output json",
						i18n.T("Machine-readable output: the same English keys in every language, times as ISO 8601 in UTC, sizes in bytes. Errors are written as JSON to standard error.")),
					cmd("--no-input",
						i18n.T("Never ask anything; fail instead.")),
					cmd("--lang en|de",
						i18n.T("The language for this call.")),
					cmd("--yes, -y",
						i18n.T("Don't ask before deleting or leaving.")),
					cmd("--help, -h",
						i18n.T("Show the help for the command.")),
				}},
				{i18n.T("Output"), []helpItem{
					text(i18n.T("Data goes to standard output, messages and progress to standard error. Every command works without a terminal; Tolk only asks when a person is at one.")),
				}},
				{i18n.T("Exit codes"), []helpItem{
					cmd("0", i18n.T("All went well.")),
					cmd("1", i18n.T("Another error.")),
					cmd("2", i18n.T("The command line was wrong.")),
					cmd("3", i18n.T("The login failed, or the container stayed locked.")),
					cmd("4", i18n.T("Not found.")),
					cmd("5", i18n.T("No connection to the stone.")),
					cmd("6", i18n.T("Not permitted.")),
				}},
				{i18n.T("Environment"), []helpItem{
					cmd("TOLK_PASSPHRASE_FILE",
						i18n.T("For unattended scripts: a protected file with the container's passphrase. Tolk then needs neither an agent nor a person.")),
					cmd("TOLK_CONFIG_DIR",
						i18n.T("The directory of Tolk's configuration, in place of the usual one.")),
					cmd("TOLK_AGENT_DIR",
						i18n.T("The directory of the agent's socket, in place of the usual one.")),
					cmd("NO_COLOR",
						i18n.T("When set, Tolk writes without colours.")),
					cmd("CI",
						i18n.T("When set, Tolk asks nothing, as with --no-input.")),
				}},
				{i18n.T("Configuration"), []helpItem{
					text(i18n.T("Tolk keeps one file, config.json, in the directory \"tolk\" of your configuration directory: ~/.config/tolk on Linux, ~/Library/Application Support/tolk on macOS, %AppData%\\tolk on Windows, or the directory TOLK_CONFIG_DIR names. It holds the settings, the groups and the encrypted container. Nothing decrypted is written to disk.")),
				}},
			},
		},
	}
}

// topicOf finds the topic a word leads to: its name, or one of its words.
func topicOf(topics []helpTopic, word string) *helpTopic {
	word = strings.ToLower(word)
	for i := range topics {
		if topics[i].name == word {
			return &topics[i]
		}
	}
	for i := range topics {
		for _, w := range topics[i].words {
			if w == word {
				return &topics[i]
			}
		}
	}
	return nil
}

// helpCommand is 'tolk help [<topic>|<command>|all]'.
func (c *command) helpCommand(args []string) error {
	pos, err := parse(c.flags("help"), args)
	if err != nil {
		return err
	}
	if len(pos) == 0 {
		c.help()
		return nil
	}
	topics := helpTopics()
	if strings.ToLower(pos[0]) == "all" {
		c.printHelp(c.helpAll(topics))
		return nil
	}
	topic := topicOf(topics, pos[0])
	if topic == nil {
		names := make([]string, 0, len(topics)+1)
		for _, t := range topics {
			names = append(names, t.name)
		}
		return usage("There is no help on %q. The topics are: %s.", pos[0], strings.Join(append(names, "all"), ", "))
	}
	c.printHelp(c.helpTopic(topic))
	return nil
}

// helpFor shows the help that fits a command line with --help in it: the
// topic of its command, or the overview.
func (c *command) helpFor(args []string) {
	topics := helpTopics()
	for _, arg := range args {
		if strings.HasPrefix(arg, "-") {
			continue
		}
		if topic := topicOf(topics, arg); topic != nil && strings.ToLower(arg) != "help" {
			c.printHelp(c.helpTopic(topic))
			return
		}
	}
	c.help()
}

// help shows the overview: what Tolk is, how to start, and the topics.
func (c *command) help() {
	c.printHelp(c.helpOverview(helpTopics()))
}

func (c *command) printHelp(text string) {
	fmt.Fprint(c.env.Out, text)
}

// The help is laid out for 80 columns: sections start at the margin, what
// is in them is indented, and a blank line stands between any two things.
const (
	helpWidth   = 78
	helpIndent  = "  "
	helpDeeper  = "      "
	helpTopicAt = 24 // the column where the summary of a topic starts
)

func (c *command) helpOverview(topics []helpTopic) string {
	style := c.style()
	var b strings.Builder
	line, _ := helpIntro()
	b.WriteString("Tolk, " + line + ".\n\n")
	c.writeSection(&b, helpUsage())
	c.writeSection(&b, topicOf(topics, "start").sections[0])

	b.WriteString(style.Accent(strings.ToUpper(i18n.T("Help topics"))) + "\n\n")
	row := func(name, summary string) {
		label := helpIndent + "tolk help " + name
		lines := wrapLines(summary, helpWidth-helpTopicAt)
		for i, l := range lines {
			if i == 0 {
				b.WriteString(style.Accent(label) + strings.Repeat(" ", helpTopicAt-width(label)) + l + "\n")
			} else {
				b.WriteString(strings.Repeat(" ", helpTopicAt) + l + "\n")
			}
		}
		b.WriteString("\n")
	}
	for _, topic := range topics {
		row(topic.name, topic.summary)
	}
	row("all", i18n.T("Everything at once"))
	row("<command>", i18n.T("The topic of a command, as in 'tolk help send'"))
	for _, l := range wrapLines(i18n.T("'tolk man' shows all of it as a manual page."), helpWidth-len(helpIndent)) {
		b.WriteString(helpIndent + l + "\n")
	}
	return b.String()
}

func (c *command) helpTopic(topic *helpTopic) string {
	var b strings.Builder
	for _, section := range topic.sections {
		c.writeSection(&b, section)
	}
	return strings.TrimRight(b.String(), "\n") + "\n"
}

func (c *command) helpAll(topics []helpTopic) string {
	var b strings.Builder
	line, paragraph := helpIntro()
	b.WriteString("Tolk, " + line + ".\n\n")
	for _, l := range wrapLines(paragraph, helpWidth) {
		b.WriteString(l + "\n")
	}
	b.WriteString("\n")
	c.writeSection(&b, helpUsage())
	for i := range topics {
		b.WriteString(c.helpTopic(&topics[i]) + "\n")
	}
	return strings.TrimRight(b.String(), "\n") + "\n"
}

// writeSection writes one section for the terminal, ending in a blank line.
func (c *command) writeSection(b *strings.Builder, section helpSection) {
	style := c.style()
	b.WriteString(style.Accent(strings.ToUpper(section.title)) + "\n\n")
	steps := 0
	for i, item := range section.items {
		switch item.kind {
		case helpText:
			for _, l := range wrapLines(item.text, helpWidth-len(helpIndent)) {
				b.WriteString(helpIndent + l + "\n")
			}
		case helpCommand:
			// A list of short words, such as the exit codes, is written as
			// a table: the word and its text on one line, without gaps.
			if item.short() {
				for j, l := range wrapLines(item.text, helpWidth-len(helpDeeper)) {
					if j == 0 {
						b.WriteString(helpIndent + style.Accent(item.lines[0]) + strings.Repeat(" ", len(helpDeeper)-len(helpIndent)-width(item.lines[0])) + l + "\n")
					} else {
						b.WriteString(helpDeeper + l + "\n")
					}
				}
				if i+1 < len(section.items) && section.items[i+1].short() {
					continue
				}
				break
			}
			for _, form := range item.lines {
				for i, l := range wrapCommand(form, helpWidth-len(helpIndent)-4) {
					if i > 0 {
						l = "    " + l
					}
					b.WriteString(helpIndent + style.Accent(l) + "\n")
				}
			}
			for _, l := range wrapLines(item.text, helpWidth-len(helpDeeper)) {
				b.WriteString(helpDeeper + l + "\n")
			}
		case helpStep:
			steps++
			number := fmt.Sprintf("%d. ", steps)
			for i, l := range wrapLines(item.text, helpWidth-len(helpIndent)-len(number)) {
				if i == 0 {
					b.WriteString(helpIndent + number + l + "\n")
				} else {
					b.WriteString(helpIndent + strings.Repeat(" ", len(number)) + l + "\n")
				}
			}
			b.WriteString("\n")
			for _, l := range item.lines {
				b.WriteString(helpDeeper + "   " + style.Accent(l) + "\n")
			}
		case helpExample:
			for _, l := range item.lines {
				b.WriteString(helpDeeper + style.Accent(l) + "\n")
			}
		}
		b.WriteString("\n")
	}
}

// width is the number of columns a text takes; Tolk's texts have no wide
// or combining characters.
func width(s string) int { return utf8.RuneCountInString(s) }

// wrapLines breaks a text into lines of at most max columns, at its spaces.
func wrapLines(s string, max int) []string {
	return fill(strings.Fields(s), max)
}

// wrapCommand breaks a command line like wrapLines, but keeps what stands in
// brackets together, such as [--name <name>] and <invitation link>.
func wrapCommand(s string, max int) []string {
	var parts []string
	depth, start := 0, 0
	for i, r := range s {
		switch r {
		case '[', '<':
			depth++
		case ']', '>':
			if depth > 0 {
				depth--
			}
		case ' ':
			if depth == 0 {
				if i > start {
					parts = append(parts, s[start:i])
				}
				start = i + 1
			}
		}
	}
	if start < len(s) {
		parts = append(parts, s[start:])
	}
	return fill(parts, max)
}

func fill(words []string, max int) []string {
	var lines []string
	line := ""
	for _, word := range words {
		if line != "" && width(line)+1+width(word) > max {
			lines = append(lines, line)
			line = ""
		}
		if line != "" {
			line += " "
		}
		line += word
	}
	if line != "" {
		lines = append(lines, line)
	}
	return lines
}

// man is 'tolk man': the manual page. At a terminal it is shown with man,
// or as 'tolk help all' where there is no man; otherwise the page is
// written to standard output, for 'tolk man > tolk.1'.
func (c *command) man(args []string) error {
	if _, err := parse(c.flags("man"), args); err != nil {
		return err
	}
	page := manPage(helpTopics())
	if !c.env.StdoutTTY {
		c.printHelp(page)
		return nil
	}
	if !showMan(page) {
		c.printHelp(c.helpAll(helpTopics()))
	}
	return nil
}

// showMan opens the page with the system's man, and says whether it did.
func showMan(page string) bool {
	man, err := exec.LookPath("man")
	if err != nil {
		return false
	}
	dir, err := os.MkdirTemp("", "tolk-man-")
	if err != nil {
		return false
	}
	defer os.RemoveAll(dir)
	// A path with a slash in it is taken as a file, not looked up.
	path := filepath.Join(dir, "tolk.1")
	if err := os.WriteFile(path, []byte(page), 0o600); err != nil {
		return false
	}
	show := exec.Command(man, path)
	show.Stdin, show.Stdout, show.Stderr = os.Stdin, os.Stdout, os.Stderr
	return show.Run() == nil
}

// manPage writes the help as a manual page, in the man macros of roff.
func manPage(topics []helpTopic) string {
	var b strings.Builder
	line, paragraph := helpIntro()
	fmt.Fprintf(&b, ".\\\" The manual page of Tolk, written by 'tolk man'. Don't edit it: its texts\n.\\\" are in eds-cli/internal/commands/help.go.\n")
	fmt.Fprintf(&b, ".TH TOLK 1 \"\" \"Tolk %s\" \"Tolk\"\n", roff(Version))
	// Command lines are not to be hyphenated or stretched. The man macros
	// take the adjusting from the string AD at every paragraph.
	b.WriteString(".nh\n.ds AD l\n.ad l\n")
	fmt.Fprintf(&b, ".SH %s\ntolk \\- %s\n", roffTitle(i18n.T("Name")), roff(line))
	usage := helpUsage()
	fmt.Fprintf(&b, ".SH %s\n", roffTitle(i18n.T("Synopsis")))
	for i, item := range usage.items {
		if i > 0 {
			b.WriteString(".br\n")
		}
		b.WriteString(roffBold(item.lines[0]) + "\n")
	}
	fmt.Fprintf(&b, ".SH %s\n%s\n", roffTitle(i18n.T("Description")), roff(paragraph))
	for _, topic := range topics {
		for _, section := range topic.sections {
			manSection(&b, section)
		}
	}
	return b.String()
}

func manSection(b *strings.Builder, section helpSection) {
	fmt.Fprintf(b, ".SH %s\n", roffTitle(section.title))
	steps := 0
	for _, item := range section.items {
		switch item.kind {
		case helpText:
			fmt.Fprintf(b, ".PP\n%s\n", roff(item.text))
		case helpCommand:
			for i, form := range item.lines {
				if i == 0 {
					b.WriteString(".TP\n")
				} else {
					b.WriteString(".TQ\n")
				}
				b.WriteString(roffBold(form) + "\n")
			}
			b.WriteString(roff(item.text) + "\n")
		case helpStep:
			steps++
			fmt.Fprintf(b, ".IP %d. 4\n%s\n", steps, roff(item.text))
			manLines(b, item.lines, 8)
		case helpExample:
			manLines(b, item.lines, 4)
		}
	}
}

// manLines writes lines to type: indented, bold, and not filled.
func manLines(b *strings.Builder, lines []string, indent int) {
	fmt.Fprintf(b, ".IP \"\" %d\n.nf\n", indent)
	for _, l := range lines {
		b.WriteString(roffBold(l) + "\n")
	}
	b.WriteString(".fi\n")
}

// roff escapes a text for roff: the backslash, the dash (which would become
// a hyphen that can't be pasted into a shell), and a dot or quote that
// starts a line and would be read as a request.
func roff(s string) string {
	s = strings.NewReplacer(`\`, `\e`, `-`, `\-`).Replace(s)
	if strings.HasPrefix(s, ".") || strings.HasPrefix(s, "'") {
		s = `\&` + s
	}
	return s
}

func roffBold(s string) string { return `\fB` + roff(s) + `\fR` }

// roffTitle is a section's title: in capitals, and quoted as one argument.
func roffTitle(s string) string {
	return `"` + strings.ReplaceAll(roff(strings.ToUpper(s)), `"`, `""`) + `"`
}
