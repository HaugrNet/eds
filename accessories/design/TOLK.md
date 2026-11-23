# Tolk plan: the CLI of EDS

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: done (Phase L6, 2026-11-11: L6a–L6e, the non-interactive CLI and the TUI),
with what followed Rune until 2026-11-17 (see "After L6"). The TUI hasn't been
reviewed on a real screen by its author's user yet; see "Open decisions".

One of the three plans of EDS 2.0: `PLAN.md` (the backend), `RUNE.md` (Rune, the
PWA), `TOLK.md` (this file). Tolk is Phase L6 of the work on the clients. It builds
on the backend's contract (`PLAN.md`, chapter "Clients API") and follows Rune in
words, rules and what it can do (`RUNE.md`); crypto and data transfer are decided in
`PLAN.md` (chapters "Crypto" and "Streaming").

For the review Tolk is two commits, after the backend's and Rune's (`PLAN.md`,
"Handed over for review"): this file, and then what was built from it: `eds-cli`,
the Maven profile `cli` and the section of `RUNNING.md` about the command line. It
needs Rune's commits, as it keeps copies of files Rune owns.

Since 2026-11-15 three more plans cross all three parts: `E2E.md` (rooms and
chambers: end-to-end encryption which the stone takes no part in), `FEATURES.md` (the
chat, a calendar, location and plans as features by declaration) and `STATUS.md` (the
status of a stone, for those who look after several). They hold the design, the
backend and the convention of the clients; their order of work is in `FEATURES.md`.
What Tolk does in each, and the phases in which it was built on 2026-11-15, are in
this file since 2026-11-17: "Rooms and chambers", "Features" and "The status of
stones", with the runs of Rune and Tolk together (E6, T6). Each phase says what was
built, what differs and what wasn't checked.

The definition of done in `PLAN.md` ("Rules", "Definition of done") applies here
as well. The tests of Tolk: `go vet` and `go test ./...`, and the runs with Rune.

## Goals

- A cross-platform Go CLI, usable in scripts (non-interactive) and at a terminal
  (the TUI).
- Independent of any one server: several stones in one call, and transfers between
  them.
- Pipes work: data on standard output, everything else on standard error.
- Keys stay server-side: Tolk needs no crypto for EDS's data. Its own crypto is the
  container for the stones' credentials.
- Go's standard library wherever it can do the job.

## Module

Directory `eds-cli`, module `github.com/HaugrNet/eds/eds-cli`, `go 1.26`; built by
Maven with the profile `cli`, or by `./eds.sh build tolk`.

- Go module. Rule (2026-11-10, the user): every third-party library has a cost in
  CVEs; where Go's standard library can do the job with extra work, it does.
- Client: thin and hand-written on `net/http` and `encoding/json`, no generator and
  no runtime package. A Go test reads `../eds-api/src/main/resources/openapi.json`
  and fails when a path, method or field the client uses isn't in the contract.
- Config: JSON (`config.json`), read and written with `encoding/json`; no TOML.
- Commands: the standard library's `flag` with hand-written subcommands; no `cobra`.
- Chat: a hand-written WebSocket client (RFC 6455: handshake, masked frames,
  fragments, ping/pong, close) on `net`, `crypto/tls`, `net/http`, `crypto/sha1` and
  `encoding/base64`. The standard library has no WebSocket package, but everything
  needed to write one; tested against the running server and with frame-level unit
  tests. No `coder/websocket`.
- For everything but the TUI, one library: `golang.org/x/term` (the Go team's; passphrase prompts without
  echo on Linux, macOS and Windows). Kept (2026-11-10, the user): doing that on the
  standard library would mean per-system `syscall` code.
- TUI (decided 2026-11-11, the user): `bubbletea` and `lipgloss`, as the TUI is
  there for the eye, and they suit that better. A later goal is to replace them
  with code on the standard library. They bring 14 further modules with them
  (listed under L6d), all vendored.
- Layout:
  ```
  eds-cli/
  ├── go.mod
  ├── build.sh
  ├── cmd/tolk/main.go
  └── internal/
      ├── client/      hand-written, checked against the contract by a test
      ├── service/     shared logic used by both commands and TUI
      ├── commands/    subcommands on `flag` (non-interactive), help and manual page
      ├── tui/         interactive UI (Bubble Tea, Lip Gloss)
      ├── socket/      the general socket: requests, events, presence, signals
      ├── chat/        the chat, on entries over socket/
      ├── features/    declarations, entries, calendar, plans, location, calls
      ├── e2e/         the chamber convention (E2E.md)
      ├── status/      the status of stones and its view
      ├── ws/          the WebSocket client (RFC 6455)
      ├── agent/       the agent and its socket
      ├── config/      config.json: settings and the encrypted container
      ├── address/     stone:room/path, groups
      ├── i18n/        Tolk's catalog; i18n/rune/ holds the copies of Rune's files
      ├── messages/    EDS's messages as Rune's texts
      ├── format/      numbers, sizes, dates
      ├── theme/       Rune's palettes for the terminal
      ├── qr/          the QR encoder, ported from Rune
      ├── exit/        exit codes
      └── fakestone/   a stone in memory, for the tests
  ```
- Build: `build.sh <version>` cross-compiles with `CGO_ENABLED=0`, `-trimpath`,
  `-ldflags "-s -w -X main.version=<version>"` for linux/amd64, linux/arm64,
  darwin/amd64, darwin/arm64, windows/amd64 into `target/dist/`.
- Maven wiring (`exec-maven-plugin`): `go generate ./...` in `generate-sources`,
  `go vet ./...` and `go test ./...` in `test` (honour `skipTests`), `sh build.sh ${project.version}`
  in `package`.
- What Tolk needs from the rest of the repository:
  - the contract `eds-api/src/main/resources/openapi.json`, read by the client's
    test, which fails without it;
  - Rune's shared files, which `go generate` copies into `internal/i18n/rune/`
    (see `copy.go`): the German catalog, the table of messages, the E2E vectors,
    the plans' examples and the five feature declarations. The Maven build runs `go generate` and so fails without
    Rune's sources; plain `go build`, `go test` and `build.sh` work on the checked-in
    copies, and the comparing tests skip when Rune's sources aren't there (see
    "Open decisions").
- `./eds.sh build tolk [--test]` builds only Tolk; `./eds.sh status` lists the
  binaries. Tolk isn't part of the container image.

## Behaviour

Several stones (requirement, 2026-11-10, the user): the CLI is independent of any one
server. In scripting mode one call can address several stones, to send data or chat
messages to rooms on all of them at once and to transfer data between instances; the
config file is built for that. The TUI connects to one stone at a time.

Name and credentials (2026-11-10, the user): the CLI
is called **Tolk** (binary `tolk`). The model is SSH: each stone has its own entry,
including the passphrase of the account there, kept in an encrypted container in
Tolk's config. Tolk itself creates and maintains it: registering a stone asks for
its information, updating one changes it. `tolk login` opens the container and keeps
the information in memory. A server instance is called a **stone** in Tolk's language (decided 2026-11-10, the
user: Haugr is the hill for signal fires, Rune the GUI, hence rune stones; "stone"
makes for natural commands that match the components). `server` and `haugr` mean the
same and are accepted as synonyms; Tolk's own texts and help say "stone", and the
config lists `stones`.

Decided 2026-11-10 (the user):
- The encrypted container holds the whole entry of each stone: name, URL, account and
  passphrase. The file doesn't reveal which stones are used; listing them needs the
  container unlocked. The groups are inside it too, as their targets name stones and
  rooms; only the `ui` settings stay outside.
- Addresses are `stone:room/path`. `stone:` may be left out for the default stone;
  room and file IDs are accepted in place of names; a `:` or `/` inside a name is
  written with a backslash, or the ID is used.
- Several targets in one call: Tolk continues with the rest when one fails, reports
  each target, and exits 0 only if all succeeded.
- There is no "every room on every stone". A named group in the config lists
  targets (`@announce`); `'work:*'` means every room on one stone.
- `cp` transfers between stones, streaming through Tolk's memory, never its disk.

Keeping the container unlocked (decided 2026-11-10, the user): an **agent**, like
`ssh-agent`. `tolk login` asks for the container's passphrase, starts the agent and
hands it the unlocked container; the agent keeps it in memory, logs in to a stone
when a command first needs it, keeps the session tokens, and logs in again by itself
when a session expires. `tolk logout` ends the sessions on the stones and stops the
agent. Nothing decrypted is written to disk. The agent listens on a socket in a
directory only the user can open; built on the standard library (`net`), on Windows
too.

Unattended scripts (decided with it): as an explicit opt-in, the container's
passphrase may come from a protected file named in `TOLK_PASSPHRASE_FILE`, like an
SSH key without a passphrase. Tolk looks for the unlocked container in this order: a
running agent; the file, if the variable names one; a prompt, if a person is at the
terminal; otherwise it fails with exit code 3.

Maintaining the container: `tolk init` creates it (asks for its passphrase twice);
`tolk register stone <name>` asks for URL, account and passphrase and logs in once to
check them; `tolk update stone <name>`, `tolk remove stone <name>`;
`tolk list stones` never shows passphrases. The container is encrypted with
AES-256-GCM under a key derived from its passphrase with PBKDF2, both from Go's
standard library.

Interactive vs. non-interactive is decided by `stdin` and `stdout` both being TTYs,
`CI` unset, and no `--no-input` flag.

Authentication: through the agent, which holds a session per stone (see above), so a
command doesn't pay a login each time. With `TOLK_PASSPHRASE_FILE` and no agent, a
call logs in to the stones it uses and logs out at the end. Session tokens are never
written to disk.

Non-interactive (default for every subcommand):
- `tolk send <file|-> <address> … [--name <name>] [--replace] [-p]`,
  `tolk get <address> [-o <file|folder|->]`, `tolk ls <address> …`, etc. (full list
  below; `tolk help` has them all).
- `-` means stdin/stdout; data on stdout, progress and messages on stderr.
- Pipes work from the first version (decided 2026-11-10, the user). Tolk sends the
  length whenever it can know it: a local file, `tolk send - < file` (stdin is a
  regular file), or a source on another stone (from its metadata). Only a true pipe
  (`pg_dump | tolk send - …`) has none; the stone then accepts the upload without
  `Content-Length` and spools ciphertext only (a stone change, see `PLAN.md`, Streaming).
- `tolk get -o <file>` resumes an interrupted download with `Range` and `If-Range`.
- `--output json` for machine-readable output: stable English keys, times as ISO-8601
  UTC, sizes in bytes, whatever language and clock are chosen. Errors carry
  `returnCode`, the EDS `returnMessage` and the translated `message`.
- Never prompts. Stones and their credentials come from the container in
  `~/.config/tolk/config.json`, through the agent or `TOLK_PASSPHRASE_FILE` (see
  above); without either it fails immediately. Passphrases are never taken from a
  flag or an environment variable.
- Targets are addresses, `stone:room/path`; several may be given, and `@group` names
  a list from the container (`tolk group set|add|remove`, `tolk groups`). Commands
  about a room's members take the room as `--room <address>` (alias `--circle`),
  which may be repeated; file commands take addresses as arguments. A command with
  several targets prints `{"results": [{"target", "ok", "result" | "error"}]}` with
  `--output json`, in the order given, and its exit code is the failures' common
  code, or 1 if they differ.
- An address that ends in `/`, or names a room or an existing folder, is the folder
  a file goes into under its own name; otherwise its last part is the new name.
- Exit codes: 0 ok, 2 usage, 3 authentication, 4 not found, 5 network (also: EDS
  unreachable), 6 not permitted (EDS `AUTHORIZATION_WARNING`), 1 other (also: EDS
  locked or not ready).

Interactive:
- `tolk chat` opens the TUI (see "The TUI" below). `tolk` without arguments shows
  the help (changed 2026-11-11; it opened the TUI before).
- Other commands prompt for missing passphrases (twice where a new one is set), show
  progress bars, and confirm overwrites and deletions as Rune does.
- `tolk chat` without a TTY fails with a clear message instead of hanging.

Scriptable chat:
- `tolk chat --plain --room <room> … [-m <text>] [--listen] [--history <n>]`: stdin
  lines are sent as messages, incoming messages written to stdout as JSON lines
  (with replies, reactions, edits and deletions as their own events). Same
  WebSocket code as the TUI uses.

Commands and TUI both call `internal/service`; they differ only in presentation.

## Tolk and Rune

Review of 2026-11-10, which replaced that of 2026-11-08. Rune got a rooms-and-users pane, read-only rooms,
languages, a clock setting, its own pane layout and themes since then. The rule for
the CLI: **it follows Rune in words, rules and what it can do**, and differs only where a
terminal can't do it (images, drag and drop, browser notifications) or where scripts
need something else (JSON, exit codes, no prompts). EDS enforces every permission;
like Rune, the CLI only hides or refuses early what EDS would refuse anyway, with
Rune's text for it.

**Words and messages**
- "Room" for Circle, "user" for an account, "member" for someone in a room, the access
  levels "Can read", "Can read and write", "Administrator", as in Rune. Flags use
  `--room` (with `--circle` as an alias).
- EDS's messages are turned into Rune's texts with the same table as Rune's
  `messages.ts`. To keep the two from drifting, the table moves to a shared file
  (patterns, English and German text), read by Rune and embedded in the CLI.
- Login notices as in Rune: locked, not ready, unreachable; administrators are
  pointed to RUNNING.md.

**Account**

| Rune | TUI | Commands |
|---|---|---|
| Log in, log out. The login page has Rune in runes and a footer: "Your Rune is currently running on Haugr EDS <version>", plus "Test system · The data will not survive a restart of the stone." on a test system | start screen with ᚱᚢᚾ in the accent colour and the same footer lines (no knot in a terminal) | through the agent; without it, every call logs in and out |
| The account menu (the own avatar in the header: avatar, passphrase, language and clock, theme, panes, notifications, about Rune, log out); which panes show is chosen under panes, a pane has no close button | the same entries under one key (e.g. F2) | `tolk settings`, `tolk passphrase [--stone <s>]`, `tolk avatar [<id>\|none] [--stone <s>]` |
| The stone settings, only for the System Administrator (account menu → Stone settings): a window that floats over the panes, framed by Rune's knot; the settings of EDS in groups, changed in place and saved together (all or none); the algorithms are chosen from those EDS supports (`allowedValues`); internal settings aren't shown, the MasterKey source only shown | a window over the panes, with the same groups | `tolk stone settings [<stone>]` (list), `tolk stone set <key> <value> [--stone <s>]`; a value outside `allowedValues` is refused before it is sent |
| About Rune (account menu): that everything in a room is stored encrypted and how (keys from the passphrase, the room's secret, a key per message and file, the MasterKey), and what isn't: EDS encrypts, not the browser, and names stay readable; whether this is a test system (in-memory database, data lost when the server stops) or a production system; whether the connection uses HTTPS | the same text on a page | `tolk about [--stone <s>]`: Rune's texts, with Tolk's own about the container and the agent |
| While EDS doesn't answer (no answer, or 502–504 from a proxy, twice in a row on the version operation): one page instead of the login or the panes, saying so in plain words; it says the rooms and files are safe only if EDS last reported a persistent database, and warns that the data won't survive a restart on a test system. Rune asks every 5 s and continues by itself, the session kept | a full-screen notice, retrying the same way | the error `cannot reach <url>: <cause>` (English only), exit code 5 |
| The status line at the bottom: version of EDS, "Test system" if its data doesn't survive a restart, the live connection, who is logged in; for the System Administrator also the encryption in use and a warning while EDS isn't ready | the same, as the last line | `tolk stone info`: version, the URL the stone was registered with, `database` and test or production (`persistent`) from the version operation, needs no login; the same keys with `--output json` |
| Change passphrase | yes | `tolk passphrase` (asks for the new one twice, or `--passphrase-stdin`; the current one is in the container, which gets the new one) |
| Accept an invitation | first start with a link | `tolk accept <link> [--stone <name>]` (asks twice, or `--passphrase-stdin`; registers the link's stone in the container and logs in once to check) |

**Rooms and users** (Rune's rooms pane: rooms over users)

| Rune | TUI | Commands |
|---|---|---|
| Own rooms (trustee records, not `GET /circles`); the System Administrator sees every room, marked "not in it" | yes | `tolk rooms [--stone <s>]` |
| Create a room (the creator administers it) | yes | `tolk room new <[stone:]name>`; also `tolk room rename <room> <new name>` |
| A room shows its members and their access | expand the room | `tolk members --room <room>` |
| A user shows their rooms | expand the user | `tolk user <name> [--stone <s>]` |
| All users | yes | `tolk users [--stone <s>]` |
| Avatars (2026-11-10): users choose a creature of the sagas, room administrators a place or thing for the room; EDS stores only the ID (`avatar`, `circleAvatar`) | the picture's name beside the name (no drawings in a terminal); chosen from a list | `tolk avatar [<id>\|none]`, `tolk room avatar <id>\|none --room <room>`; `--output json` has the IDs |
| Add an existing user to a room one administers | from the room or the user | `tolk add <name> --room <room> [--access read\|write\|admin]` (default `write`) |
| Remove a member; change access. Nobody changes their own access, and the only administrator of a room keeps theirs (2026-11-17); the question about the last administrator is asked before they are removed | yes: changing the own access or the only administrator's is refused with Rune's two texts | `tolk remove <name> --room <room>` (without `--room`, `tolk remove stone <name>` is meant), `tolk access <name> <level> --room <room>`; the command doesn't know the rule of 2026-11-17 and makes such a change |
| Leave a room (with Rune's warnings) | yes | `tolk leave --room <room>` (asks with Rune's warning at a terminal; `--yes` or a script doesn't) |
| Invite, with QR code | yes | `tolk invite <name> --room <room> [--access …] [--qr\|--no-qr]`: the link on standard output, and on standard error the QR code in Unicode half blocks from a port of Rune's `qr.ts` (no library), with the same test of decoding all 40 versions |
| System Administrator: create a user with a passphrase, delete a user | yes | `tolk user new <name>` (asks twice, or `--passphrase-stdin`), `tolk user rm <name>` (confirms at a terminal; rooms and files stay) |
| Several administrators (2026-11-18): the first System Administrator makes, unmakes and deletes them; invitations make users | `N` asks "User" or "Administrator" for the first; `r` on a user changes the role (not on the first); the row marks "first admin" and "admin" | `tolk user new <name> --admin`, `tolk user role <name> admin\|user`; `tolk users` and `tolk user <name>` name the first, `--output json` has `firstAdministrator` |

The rules are EDS's, as Rune shows them: adding needs an administrator of the room
(EDS wraps the room key for the user); the System Administrator may change access or
remove where not a member, but not add; nobody deletes the System Administrator or
themselves. The rule about the own access and the only administrator is the
clients' alone: the stone accepts such a change.

**Chambers** (`E2E.md`; here only what changed on 2026-11-17)

- What a new chamber asks of its members is printed with Rune's texts, the same
  paragraphs as in Rune's "New room" dialog: a key pair each, the invitation link,
  that a member added from an existing account makes their keys first (`tolk key
  create`), which apps read chambers, and that the choice is final.
- Why a chamber can't be read yet is said with Rune's texts too: without keys, make
  them; without the chamber's key, or after it changed, it comes from an
  administrator, whose app gives it "as soon as they have the chamber open in Rune,
  or list its members in Tolk".
- Tolk gives the key of a chamber to those who wait when an administrator lists its
  members (`tolk members --room <room>`). Rune does it by itself while the chamber is
  open; Tolk doesn't during `tolk chat` or in the TUI otherwise. See `RUNNING.md`,
  "Chambers: Who Gets the Key, and How".

**Read-only rooms** (members who can only read)

- TUI: no input line, but "You can only read in this room."; no reply, react or edit
  keys; deleting one's own messages stays (EDS allows it). In the files pane no
  upload, new folder, rename, move, delete or share in the chat.
- Commands: `send`, `mkdir`, `mv`, `rename`, `rm` and chat sending check the access
  first and stop with Rune's text and exit code 6 (EDS refuses anyway).
- The access is checked again when a room is opened and when a membership changes.

**Files**

| Rune | TUI | Commands |
|---|---|---|
| Folders, new folder, rename, move (also to another room), delete | yes | `tolk mkdir <address> [-p]`, `tolk rename <address> <name>`, `tolk mv <address> <address>` (another folder or room of the same stone, also under a new name), `tolk rm <address> … [--yes]` |
| Upload into a chosen folder, share in the chat | yes; `/send <path>`, a path dropped onto the terminal is offered for sending | `tolk send <file\|-> <address> … [--name] [--type] [--replace] [-p] [--share [-m <text>]]`; `tolk cp <address> <address> …` copies between rooms and stones |
| Live refresh (`CHANGED`) | yes | no |
| Downloads with progress, pause and resume, across restarts | yes | `tolk get <address> [-o …] [--force]`: `<file>.eds-part` with the ETag in `<file>.eds-part.etag`; running it again continues with `Range` and `If-Range`, and starts over if the content changed |
| Thumbnails | not shown | none: CLI uploads get a generic icon in Rune |
| Image and video preview | `o` opens a downloaded file with the system viewer, on request only (it writes decrypted content to disk) | no |

Names starting with a dot are refused, as hidden; folders can't be moved.

**Chat**

| Rune | TUI | Commands |
|---|---|---|
| Messages, earlier messages | yes | `--plain` |
| Replies, reactions (the six), edit and delete own | keys on the selected message; reactions as emoji, or their names where the terminal lacks emoji | `--plain` reads them |
| Receipts: checkmarks on the own messages (one sent, two received by everyone, two blue read by everyone; who in the tooltip) | send `RECEIVED` after a `MESSAGE` and after `JOINED` with unread messages; mark read while the newest messages are in view; ✓, ✓✓ and coloured ✓✓ | no |
| Typing indicator | send while typing (at most every 3 s), show "… is typing" | no |
| Unread counts, notifications | counts in the rooms pane and the terminal title; the terminal bell, off by default | no |

**Settings** (as in Rune's account menu. Rune stores language, clock, theme and pane
layout with the account, as the JSON object `Member.settings`, which only the Member
gets; the CLI keeps its own in `config.json` under `ui`, per machine, and may read
the language and clock of the account as defaults)

| Setting | Values | Default |
|---|---|---|
| `language` | `auto`, `en`, `de` | `auto`: `LC_ALL`, `LC_MESSAGES`, then `LANG`; English if none matches |
| `clock` | `24`, `12`, `auto` | `24`; `auto`: 12 hours for the locales that use them (`en_US`, …), else 24 |
| `appearance` | `auto`, `light`, `dark` | `auto`: the terminal's background, detected by the TUI |
| `theme` | `rune`, `fjord`, `moss`, `contrast` | `rune` |
| `layout` | a preset: `three-columns`, `chat-on-top`, `rooms-beside`, `chat-and-files`, `chat-only`. The TUI also keeps a changed layout as Rune's tree, in `layoutTree` | `three-columns` |
| `bell` | `true`, `false` | `false` |
| `present` | `true`, `false` | `true`: the interactive mode shows the person as online |

- `--lang` overrides the language for one call; there is a `tolk settings` command
  to show and set them.
- Language: gettext style as in Rune, English texts as keys. The CLI shares Rune's
  catalog for the texts both have (moved to a JSON file Rune imports and the CLI
  embeds), and has its own for CLI-only texts (help, flags, prompts); the build check
  of `scripts/check-i18n.mjs` gets a Go counterpart.
- Dates and sizes: Go has no locale formatting, so the CLI formats them itself for its
  two languages (English: by region, e.g. 11/10/2026 or 10/11/2026; German:
  10.11.2026; decimal comma in German).
- Themes: the same hex values as Rune's palettes in true colour, with a
  fallback to 256 or 16 colours; a Go test reads `rune-theme.css` and `themes.css` so
  the colours can't drift. `NO_COLOR` turns colour off; "High contrast" uses bold
  and underline as well as colour.

## The TUI

- Panes as in Rune: **Rooms and users** (rooms over users), **Chat** and **Files**;
  the stone settings of the System Administrator are a window over them.
  Enter on a room opens it and shows its members; → only shows them; Enter on a user
  shows their rooms.
- Layout: Rune's presets (three columns, chat on top, rooms beside chat over files,
  chat and files, chat only), panes hidden and shown with keys, the focused divider
  moved with Ctrl+arrows, and the focused pane swapped with a neighbour. The tree is
  the same as Rune's (`layout.ts`), so a layout can be copied between them; free
  dragging isn't offered in a terminal.
- With the rooms pane hidden, Ctrl+R (or `/room <name>`) chooses the room, as Rune's
  header picker does. Without rooms it says Rune's text, which names Rune's "New
  room" button since 2026-11-17 (before, a "+" neither has); in the TUI a room is
  made with `n` in the rooms pane.
- Not taken over from Rune (2026-11-17): the shaded rooms pane and the tint and mark
  of a chamber's content, the rooms pane minimized to a strip (the TUI hides and
  shows panes with Alt+1, 2, 3), and the lists of rooms and users following the
  stone by themselves: the TUI reads them again after its own actions and when the
  stone announces a change in a room or that a room was left, so a room one was
  added to shows only then.
- Narrow terminals (under about 100 columns) show one pane at a time with tabs, as
  Rune does on phones. `Tab` moves between panes.
- A test system is marked as in Rune: "Test system" in the last line and on the start
  screen, emphasised with the accent colour and bold, never as a blocking notice.
- Keys that work everywhere: Tab and Shift+Tab (next and previous pane), F1 (the
  keys; also `?` in the rooms and files panes), F2 (account menu), F3 (next preset),
  F4 (swap the pane with the next), Alt+1, 2, 3 (hide or show a pane), Ctrl+arrows
  (move the divider), Ctrl+R (choose the room), Ctrl+C or Ctrl+Q (quit). The panes
  have single-letter keys for their actions and `h`, `j`, `k`, `l` beside the
  arrows; the input line has the readline keys (Ctrl+A, E, U, K, W). The window of
  F1 is the reference for them, in the language chosen.

## Not in Tolk

Only in the browser: download tokens (Tolk sends the Authorization header), pasting
images and dropping files on the chat (covered by `/send` and dropped paths),
browser notifications, the plaited knot of the login and maintenance pages, and
dragging panes.

## Reference

What exists beyond the sections above; `tolk help` is the full list.

**Help** (2026-11-11): `tolk` alone shows the help. `tolk help <topic>` for the topics
`start`, `stones`, `addresses`, `rooms`, `chambers`, `features`, `status`, `files`,
`chat`, `settings` and `scripts`;
`tolk help <command>` and `tolk <command> --help`; `tolk help all`. `tolk man` prints
the manual page (roff), and opens it with `man` at a terminal; `build.sh` writes
`tolk.1` and `tolk.de.1` beside the binaries, and they are in `SHA256SUMS`.

**Completion** (2026-11-21): `tolk completion bash` writes a bash function which asks
Tolk for the words: the hidden `tolk __complete <words>` writes one candidate per line,
read from the command lines of the help (commands, subcommands, flags, the values of
`a|b|c`, feature names, settings and their values, help topics), file names where a
file is wanted, and the stone names only when the agent runs; it never unlocks the
container or asks a stone. `eds.sh install` puts it, with Tolk, in place.

**Environment**

| Variable | Use |
|---|---|
| `TOLK_PASSPHRASE_FILE` | a file with the container's passphrase, for unattended scripts; `tolk init` takes a new container's passphrase from it too, without asking |
| `TOLK_CONFIG_DIR` | where `config.json` is, instead of the system's config directory (`~/.config/tolk` on Linux; `os.UserConfigDir()` elsewhere) |
| `TOLK_AGENT_DIR` | the directory of the agent's socket |
| `TOLK_KDF_ITERATIONS` | the PBKDF2 count for a new container; for the tests, see "Open decisions" |
| `CI`, `NO_COLOR`, `TERM`, `COLORTERM` | no prompts; no colour; the colours a terminal has |
| `LC_ALL`, `LC_MESSAGES`, `LANG`, `LC_TIME` | language; the region for dates |
| `TZ` | the time zone of the calendar |
| `HTTPS_PROXY`, `HTTP_PROXY`, `NO_PROXY` | the proxy, also for the chat (`CONNECT`) |

**`config.json`** (mode 0600): `{"ui": {…}, "container": {"version": 1, "kdf":
"pbkdf2-hmac-sha256", "iterations", "salt" (16 bytes), "nonce" (12 bytes), "data"}}`,
sealed with AES-256-GCM and the AAD `tolk-container-v1`. `ui` has `language`,
`clock`, `appearance`, `theme`, `bell`, `present` (true unless set otherwise: the
interactive mode shows the person as online), `layout`, `layoutTree` (a changed
layout, as Rune's tree) and `layoutAway` (the panes hidden). Inside the container:
`{"default", "stones": {<name>: {"url", "account", "passphrase", "accountKey",
"statusKey"}}, "groups"}`. Beside `config.json`, files `device-<hex>.bin` hold the
remembered chambers and the keys seen (see Phase E5).

**Stones**: a name has letters, digits, dot, dash and underscore. The first stone
registered is the default. `register stone` takes `--url`, `--account`, `--default`,
`--no-check`, `--passphrase-stdin`, `--status-key` and `--status-key-stdin`;
`update stone` takes `--url`, `--account`, `--default`, `--no-check`,
`--passphrase`, `--rename` and `--no-status-key`. `status` takes `--count`. `accept` takes
`--default`. Also accepted: `tolk list rooms|users|groups`, `tolk stones`,
`tolk stone list`.

**Agent**: the socket is `agent.sock` in its directory (on Windows below the user's
cache directory); a path over 100 bytes is refused. `tolk __agent` is the hidden
command that runs it. `login --lifetime` defaults to 0, until logout; when a
lifetime passes, the agent logs out of the stones and stops. When the container is
loaded again, the sessions of changed stones are dropped. 90 s per connection, 2 s
to reach it.

**Files**: `cp` takes `--replace` and `-p`; the long forms `--parents`, `--message`,
`--out`, and `-y` for `--yes`, exist too.

**JSON**: an error is `{"error": {"code", "message", "returnCode",
"returnMessage"}}`, where `code` is the exit code. Events of `chat --plain` also
carry `roomId`, `fromId`, `edited` and `history`. `tolk version --output json` prints
`{"tolk": …}`.

**Exit codes**, in detail: EDS's return code 401 (`AUTHORIZATION_WARNING`) gives 6,
403 (`AUTHENTICATION_WARNING`) gives 3. An answer that isn't EDS's with HTTP 502, 503
or 504 gives 5, with 404 gives 4.

## Phase L6

Done 2026-11-11. Paused twice by the user for work on Rune (2026-11-08, 2026-11-10).
Decided 2026-11-10: the non-interactive CLI first (L6a, L6b, L6c, L6e), the TUI (L6d)
afterwards. All decisions for the non-interactive CLI are made (2026-11-10). L6a,
L6b, L6c and L6e are done (2026-11-11); L6d (the TUI) is done too (2026-11-11). Built one step at a time, as the other phases; the steps follow "Tolk
and Rune" above.

- **L6a Foundation:** hand-written Go client with its contract test,
  `internal/service`, the encrypted container with `init`, `register`/`update`/
  `remove stone` and `list stones`, the agent with `login`/`logout`, addresses and
  groups, JSON config with `ui` settings, session handling, exit codes, `--output json`, and
  `tolk stone info` from the version operation (version, `database`, `persistent`). The shared
  files: EDS message table and Rune's catalog as JSON (Rune switched to them, its
  build check kept), Go i18n with the check, date/size formatting, the palettes.
  First part done 2026-11-10 (`eds-cli/`, module `github.com/HaugrNet/eds/eds-cli`):
  - `internal/client`: `Version`, `Login`, `Logout` on `net/http`; a test checks each
    operation, its schemas and every field of the Go types against `openapi.json`.
  - `internal/config`: `config.json` (mode 0600) with the `ui` settings in the clear
    and the container sealed with AES-256-GCM under a PBKDF2-HMAC-SHA256 key
    (600,000 iterations, `crypto/pbkdf2` of Go 1.24+). The groups are inside the
    container too, as their targets name stones and rooms.
  - `internal/agent`: the agent on a Unix socket (`$XDG_RUNTIME_DIR/tolk`, else a
    `tolk-<uid>` directory in the temp dir, mode 0700, owner checked; also on
    Windows). One JSON request and answer per connection: `ping`, `stones`,
    `session` (with `renew`), `load`, `logout`. `tolk login` starts it detached and
    passes the unlocked container on its standard input.
  - `internal/address`: `stone:room/path` with backslash escapes, `@group`, `stone:*`.
  - `internal/service`: finds the unlocked container in the decided order (agent,
    `TOLK_PASSPHRASE_FILE`, prompt, exit code 3); without an agent a call logs in to
    the stones it uses and out at the end.
  - Commands: `init`, `login [--lifetime]`, `logout`, `register`/`update`/`remove
    stone`, `list stones`, `stone info`, `settings`, `version`, `help`; `server` and
    `haugr` are accepted for `stone`. `--output json`, `--no-input`, `--lang`, the
    exit codes, and errors as JSON on standard error.
  - A stone's passphrase is asked without echo, or read from standard input with
    `--passphrase-stdin`; registering logs in once to check the entry. A URL without
    a path gets `/api/v1`.
  - English texts with a German catalog (`internal/i18n/de.json`); a test fails on a
    missing or unused translation.
  - Verified against a running EDS (H2): registering, a refused passphrase (exit
    code 3), `stone info`, the agent's login, list and logout, and exit code 5 when
    the stone is down. Cross-compiles for Windows and macOS.
  - Dependencies: `golang.org/x/term` v0.48.0, which brings `golang.org/x/sys`
    v0.50.0 (the Go team's too) and sets `go 1.26` in `go.mod`.
  Second part done 2026-11-11, which completes L6a:
  - Shared with Rune: its German catalog is now `src/i18n/de.json` and the table of
    EDS's messages `src/messages.json` (patterns, English text, values); Rune imports
    both, and `check-i18n.mjs` reads them. Go only embeds files of its own module, so
    Tolk has copies in `internal/i18n/rune/`, renewed by `go generate ./...`; a test
    fails when a copy differs from Rune's file. `i18n.Rune("…")` takes a text from
    Rune's catalog (a test checks Rune has it); `internal/messages` is the Go
    counterpart of `messages.ts`, and errors from a stone are shown through it
    (`returnMessage` keeps EDS's text).
  - `internal/format`: numbers, sizes (as Rune: steps of 1024, one decimal below
    10), dates and clocks for English by region and German; ISO-8601 UTC for JSON.
  - `internal/theme`: Rune's four palettes, light and dark, in true colour with the
    nearest of 256 or 16 colours as fallback, `NO_COLOR`, bold and underline for
    "High contrast"; a test reads `rune-theme.css` and `themes.css` and compares
    every colour.
  - An operation with a session is repeated once after a fresh login when the stone
    answers 403 for it (expired, or the stone restarted); `Stone.Do` since L6b.
- **L6b Commands:** first the stone change for uploads without a length (done
  2026-11-11, see `PLAN.md`, Streaming, "Upload"); then rooms, users, members and access, invitations with the terminal
  QR code (port of `qr.ts` and its 40-version test), accept, passphrase, files
  (`ls`, `mkdir`, `mv`, `rename`, `rm`), `send` and `get` with resume, `settings`.
  Read-only checks with Rune's texts.
  Done 2026-11-11:
  - `internal/client`: 27 operations (rooms, members, users, invitations, files,
    upload, download, settings), all on the ID-based API with the Bearer token where
    the contract has it; the contract test now also checks the nested types.
    `Upload` sends without `Content-Length` for a length of -1; `Download` takes a
    start and an ETag.
  - `internal/service`: `Service` (stones of the container, `@group` expansion),
    `Stone` (a session, repeated once after a 403; rooms by name or ID, users,
    members) and the paths (`Find`, `Folder`, `File`, `MakeFolders`), with the early
    checks Rune makes: names starting with a dot, and writing where one can only
    read (exit code 6, Rune's text).
  - `internal/qr`: the port of `qr.ts`, with `Text()` in half blocks, black on
    white. Tests: the same codes as Rune's encoder, module for module, for all 40
    versions (hashes generated from `qr.ts`, in `testdata/rune.json`), and a decoder
    that reads the shortest and the longest text of every version back.
  - Commands: `rooms`, `room new|rename|avatar`, `members`, `users`, `user`,
    `user new|rm`, `add`, `access`, `remove`, `leave`, `invite`, `accept`,
    `passphrase`, `avatar`, `groups`, `group set|add|remove`, `ls`, `mkdir`, `mv`,
    `rename`, `rm`, `send`, `get`, `cp`, `stone settings`, `stone set`, `about`.
  - `send`: a file is sent to each room in turn, with its length; a pipe is read
    once and sent to all rooms at the same time through memory, without a length.
    `tolk send - < file` has a length. A file registered for content that then
    fails is removed again. `--replace` replaces the content of an existing file.
  - `cp` always streams through Tolk, also within a stone (the stone's own copy
    operation isn't used).
  - Progress on standard error when it is a terminal; data only on standard output.
  - `internal/fakestone`: a stone in memory for the Go tests (rooms, members, files,
    content with ranges, sessions that can be expired); the command tests run
    against it, also under the race detector.
  - Shared message table: "The Member is already a trustee …" added
    (`messages.json`, "This user is already in the room.").
  - Verified against a running EDS (H2): rooms, users and access, invitation with
    QR code, accepting it, passphrase change, avatars, groups, stone settings,
    upload of 3 MB with and without a length, one pipe to two rooms, listing,
    download to a file and to standard output, a continued and a restarted
    download, move, rename, delete, copy between two stone entries, and the
    read-only refusals; in English and German.
  - Found on the way and fixed: Rune built invitation links below the API root
    since the move to `/` (fixed in Rune).
  - Not done: the stone's own copy for `cp` within a stone; reading language and
    clock of the account as defaults. (`--share` came with L6c.)
- **L6c Chat:** the hand-written WebSocket client with its tests, `tolk chat --plain` with all frame types.
  Done 2026-11-11:
  - `internal/ws`: WebSocket (RFC 6455) on `net`, `crypto/tls`, `net/http` (for the
    handshake's request and answer), `crypto/sha1` and `encoding/base64`: the
    opening handshake with the accept key checked, masked frames with a fresh key
    each, the three length encodings, fragmented messages, ping answered with pong,
    the closing handshake, a size limit (1 MiB), and a proxy from the environment
    with `CONNECT`. No extensions, no subprotocols. `Accept` is the server side, for
    the tests and the fake stone. Tests: the RFC's examples (accept key, "Hello"
    unmasked, masked and fragmented with a ping in between), all length encodings in
    both directions, fresh masking keys, close with code and reason, ten protocol
    violations that must be refused, and `Dial` against a real HTTP server.
  - `internal/chat`: the chat client; a request has a reference and one answer,
    everything else is an event. It pings every 30 s (in `internal/socket` since
    T5). A close with 1008 is "the
    session has ended".
    It reads frames of up to 16 MiB (since 2026-11-13): a page of the history
    with long messages is far over the WebSocket client's general 1 MiB, and such
    a room couldn't be read.
  - `tolk chat --plain --room <room> … [-m <text>] [--listen] [--history <n>]`:
    each line of standard input, or the text of `-m`, is sent to every room given
    (several rooms, several stones, `@group`); standard output gets JSON lines:
    `sent`, `message`, `edited`, `reaction`, `deleted`, `left`, with `stone`, `room`,
    `id`, `from`, `time` (ISO-8601 UTC), `text`, `replyTo`, `fileId`, `reactions`.
    It ends when the input ends; with `--listen` it goes on until interrupted or
    the connection ends. `--history <n>` first writes the last n messages. A room
    where the account can only read is listened to, but sending is refused with
    Rune's text and exit code 6. Receipts and typing aren't sent or shown.
  - `tolk send … --share [-m <text>]` posts the uploaded file in the room's chat.
  - `tolk chat` without `--plain` said that the interactive chat wasn't built yet;
    since L6d it opens the TUI.
  - Verified against a running EDS: sending lines, a listener in two rooms
    receiving another account's message and shared file, history, the read-only
    refusal, and `--share`.
  - Found: a stone keeps **one session per account**. A login with the same account
    elsewhere replaces it, and a logout ends it. See "Open decisions".
- **L6d TUI (after L6e):** the three panes, rooms over users with expanding, layout presets,
  hiding and resizing, narrow mode, read-only rooms, unread counts and bell, receipts,
  typing, replies, reactions, edit and delete, themes and appearance; the start screen
  and the maintenance notice with the test-system lines.
  Done 2026-11-11 (`internal/tui`, on Bubble Tea 2.1.0 and Lip Gloss 2.0.6):
  - `tolk chat [--stone <name>]` (not `tolk` alone, which shows the help) opens
    one stone. Without a terminal it says so and exits with 2.
  - Start screen: ᚱ ᚢ ᚾ in the accent colour, and Rune's footer ("Your Rune is
    currently running on Haugr EDS <version>", "Test system · The data will not
    survive a restart of the stone."). The same screen carries the notice for a
    stone that doesn't answer ("The stone is asleep", asked again every 5 s, "Your
    rooms and files are safe." only on a production system) and for a refused login.
  - Panes: rooms over users (Enter opens a room and shows its members, → only
    shows them, Enter on a user shows their rooms; unread counts; "not in it");
    chat; files. The status line is Rune's: version, "Test system", the live
    connection, who is logged in; notes and the progress of a transfer at its right.
  - Layout: Rune's tree and its five presets (F3), panes hidden and shown (Alt+1,
    2, 3, or the menu), the focused pane's divider moved with Ctrl+arrows, the pane
    swapped with the next (F4). Kept in `config.json` as `layout` (a preset) or
    `layoutTree` (Rune's JSON). Under 100 columns: one pane with tabs. Ctrl+R
    chooses the room.
  - Chat: history and earlier pages (PgUp), sending, replies, the six reactions
    (1–6), edit and delete of own messages with Rune's texts, shared files (`s`
    saves one), checkmarks ✓, ✓✓ and ✓✓ in the accent colour by Rune's rule (by
    place, else by time), "… is typing" for 5 s and TYPING sent at most every 3 s,
    RECEIVED after a message, READ when the open room is at its newest message,
    unread counts in the pane and the terminal's title, the bell if switched on.
    `/send <file>` uploads and shares; a file dropped on the terminal is offered
    for that. `/room <name>` changes the room.
  - Files: folders, upload (`u`), download (Enter or `s`, into the directory Tolk
    was started in, under a free name), new folder, rename, move (folders here, the
    folder above, other rooms), delete, share in the chat; refreshed on `CHANGED`.
  - Read-only rooms: Rune's line instead of the input; no reply, react or edit;
    no upload, new folder, rename, move, delete or share.
  - Members: add a user, invite (link and QR code in a window), change access,
    remove, leave, each with Rune's question; new room, rename, the room's picture.
    The System Administrator makes and deletes users.
  - F2 is the account menu: avatar, language and clock, theme (appearance and the
    four palettes; "As the terminal" asks the terminal for its background), panes,
    notifications (the bell), stone settings for the System Administrator (a
    window; Enter changes one, with the choices where EDS has them), about, keys,
    log out. F1 lists the keys.
  - When another login ends the session, Tolk says so and waits for `r`; it
    doesn't take the session back by itself, which would end the other's in turn.
  - Tests drive the model as the program would (keys in, screen out) against the
    fake stone, which got edit, delete, react, receipts and typing: start screen
    and panes, every line exactly as wide as the terminal, wrong passphrase, stone
    down, chat with checkmarks, typing, reply, reaction, edit and delete, a
    read-only room, files, members with invitation, layouts and narrow mode, menus
    and German, an ended session. Also run in a pseudo-terminal against a running
    EDS: start, open a room, send, help, leave with exit code 0.
  - Differs from the plan: the passphrase can't be changed inside the TUI (the
    container has to be opened for it; the menu points to `tolk passphrase`);
    downloads in the TUI don't pause or resume (`tolk get` does); no `o` to open a
    file with the system's viewer; settings are changed one at a time, not saved
    together; reactions are always shown as emoji; no mouse.
  - Libraries: `charm.land/bubbletea/v2` 2.1.0 and `charm.land/lipgloss/v2` 2.0.6
    (both more than two weeks old when added), with 14 further modules they need
    (`charmbracelet/ultraviolet` (an untagged version), `colorprofile`, `x/ansi`, `x/term`, `x/termios`,
    `x/windows`, `clipperhouse/displaywidth` and `uax29`, `lucasb-eyer/go-colorful`,
    `mattn/go-runewidth`, `muesli/cancelreader`, `rivo/uniseg`, `xo/terminfo`,
    `golang.org/x/sync`). All vendored (15 MB). Only `internal/tui` imports them
    (`x/ansi` directly too); the later goal is to replace them with code on the
    standard library.
- **L6e Build:** `build.sh`, Maven wiring, a short CLI section in the manual's notes.
  Done 2026-11-11:
  - `eds-cli/build.sh <version>`: the five binaries in `target/dist/`
    (`tolk-<version>-<os>-<arch>`, `.exe` on Windows) with `CGO_ENABLED=0`,
    `-trimpath` and `-ldflags "-s -w -X main.version=<version>"`, plus `SHA256SUMS`.
  - `eds-cli/pom.xml` (packaging `pom`, `exec-maven-plugin` 3.5.1): `go generate
    ./...` in `generate-sources`, `go vet ./...` and `go test ./...` in `test` (both
    skipped with `-DskipTests`), `sh build.sh ${project.version}` in `package`.
  - The root POM's profile `cli` adds the module; without it the build doesn't need
    Go. `mvn -Pcli install -pl eds-cli` verified offline: tests pass, five binaries,
    `tolk version` prints `2.0-SNAPSHOT`.
  - The notes for the manual are `RUNNING.md`, which gets "Tolk, the Command Line":
    build, first steps, addresses, container, agent, unattended scripts, exit codes,
    the one session per account, and what a reverse proxy must pass. `README.md`
    points to it.
  - Vendored (decided 2026-11-11, the user): the sources of `golang.org/x/term` and
    `golang.org/x/sys` are in `eds-cli/vendor/` (9 MB, 383 files then; with the
    TUI's libraries 18 modules, 15 MB), so the build
    needs neither the module cache nor a module proxy; verified with `GOPROXY=off`
    and an empty cache. The binaries are the same as before: the same code is
    compiled in, only its source now comes from the repository.

Done when: `mvn -Pcli install` produces all five binaries; `go test ./...` covers the
service layer, the message table, i18n completeness, the QR encoder and the palettes;
a script can pipe data through `tolk send -` / `tolk get -` without prompts and reads
`--output json`; the TUI works against a running server in English and German, light
and dark, and as a read-only member.

## Rooms and chambers

Tolk's part of end-to-end encryption (`E2E.md`): what Tolk does there, and the phases in which it
was built. The design, the backend and the convention both clients follow are in
`E2E.md`.

### What Tolk does

- The same convention in Go on the standard library, tested against the copy of
  the vectors.
- The container keeps the account key, the remembered chambers and the keys seen.
- `tolk room create` with a flag for a chamber, and the same warning; `tolk invite` adds the invite secret to
  the link by itself; accepting sets up the keys.
- Commands for the recovery key, for linking a further device (showing the link
  and QR code, and taking a key), and for fingerprints.
- `send`, `get`, `cp`, `chat` and the TUI work in chambers without new flags,
  streaming both ways. `cp` already streams through Tolk, so it re-encrypts between
  rooms and stones.
- Removing a member changes the key; repair on sight.
- The fake stone in the tests gets the new fields.

### Phase E5: Tolk

Everything of "What Tolk does" above.

Done when: Tolk's tests reproduce the vectors and pass against the fake stone, and
the flow of E3 and E4 runs from the command line, including a file larger than
memory.

Done 2026-11-15:
- `eds-cli/internal/e2e` is the convention in Go on the standard library
  (`crypto/ecdh`, `crypto/hkdf`, AES-GCM), with content as a stream both ways
  (`Encrypter`, `Decrypter`). Its tests reproduce the vectors, which `go generate`
  copies from Rune's sources with the other shared files; a test fails when a
  copy differs. Since Rune's Name Keys (2026-11-22) the copy of `vectors.json` is
  behind Rune's, and that test fails (`FINDINGS.md` #30).
- `internal/service/chamber.go` and `device.go` are what Rune's `identity.ts`
  and `rooms.ts` are: the account's keys, the state of a room, making a chamber,
  inviting, adopting an invitation, repair, the change of key, and the keys seen.
- The container keeps the account key with the stone's entry (so Tolk can show
  the recovery key again). Differs from the plan: the remembered chambers and
  the keys seen aren't in the container but in a file of their own beside it,
  sealed under a key made from the account key. They change often, and the
  container can only be written with its passphrase; so the agent hands out the
  account key (as it hands out sessions), and the file needs no passphrase.
- Commands: `tolk room new --chamber` with the warning; `tolk invite` puts the
  invite secret into the link; `tolk accept` makes the keys and takes the room
  key over; `tolk key` (show, create, recovery, link with QR code, unlock,
  accept); `tolk members` shows fingerprints, repairs on sight, and marks a
  changed key; `tolk add` gives the room key; `tolk remove` changes it.
- `send`, `get`, `cp`, `chat` and the interactive mode work in chambers without
  new flags. Uploads are encrypted as they are read and downloads decrypted as
  they arrive, so a file is never in memory; `get` to a file keeps what arrived
  as the stone has it, so it can be continued, and decrypts when it is whole.
  `cp` goes through Tolk and re-encrypts between rooms, chambers and stones.
  Content of a chamber which isn't protected is told as such, and not copied.
- The fake stone has the public key and the vault of an account, the secrets and
  the shared value of a room.
- Verified by `internal/commands/chamber_test.go` against the fake stone: a
  chamber, the chat, a file of several chunks, a pipe and an empty file, copies
  in and out, an invitation by link, someone added without keys and repaired, a
  member removed, a further device by recovery key and by link, a stripped
  chamber, and unprotected content. And by hand against the uber-jar.
- Not checked: a file of several GiB (the path is the stream the tests use), the
  interactive mode in a chamber on a real screen, and Windows and macOS.

### Phase E6: together

Rune and Tolk against one stone: each creates a room the other joins, reads the
other's files and chat, and survives the other's change of key. Then the texts:
"About Rune", `tolk about`, `RUNNING.md`.

Done when: that run is repeatable from the repository, and the three plans say
what was built and what stayed open.

Done 2026-11-15:
- `eds-quarkus/src/main/webui/scripts/flow/together.ts` is that run: Rune's own
  modules as one person and the Tolk binary as another, against one stone (34
  checks). Rune makes a chamber and Tolk joins by its link; Tolk makes one and
  Rune joins by its link; each reads the other's chat and files, by their
  hashes; each removes someone and the other reads on under the new key, old
  content included; both show the same fingerprints; and the stone holds only
  content of the convention. How to run it is at the top of the file.
- "About Rune" and `tolk about` have a part on chambers; `RUNNING.md` has
  "Encrypting in the Client", with the walk-through by OpenSSL.
- What stayed open: see "Not checked" of each phase, here and in `RUNE.md`. In short: phones, browsers
  other than Chromium, Tolk on a real screen and on other systems than Linux,
  very large files, and the warning for a changed key as a person meets it.

## Features

Tolk's part of the features of a room (`FEATURES.md`): what Tolk does there, and the phases in which it
was built. The design, the backend and the convention both clients follow are in
`FEATURES.md`.

### What Tolk does

- The same on the general socket; `chat` and the TUI on entries.
- `tolk stone features`, and features per room, with Rune's words.
- `tolk calendar` (list, add, change, remove, `.ics` in and out),
  `tolk location` (show, and report a position given on the command line) and
  `tolk plan` (plans, items, expenses, balances, payments), for scripts as for
  people.
- A general command for the entries of any feature, so a script can use a feature Tolk
  doesn't know.
- The fake stone in the tests gets features.

### Phase T5: Tolk

Everything of "What Tolk does" above.

Done when: Tolk's tests pass against the fake stone, and Tolk and Rune use the
same chat, calendar, plans and positions on one stone.

Done 2026-11-15:
- `eds-cli/internal/socket` is the general socket, and `internal/chat` the chat
  on entries behind the interface the commands and the interactive mode always
  used, as in Rune. `internal/features` has the declarations (copies of Rune's), the
  entries, and the plans, the calendar and the location as Rune's modules have
  them; its tests reproduce the examples of the plans and the cases of Rune's
  `test-features.ts`, in three time zones.
- Commands: `tolk stone features` (list, offer, withdraw); `tolk features` (list, on,
  off, readonly, remove) with Rune's words; `tolk calendar` (list, add, change,
  remove, import, export); `tolk location` (show, report, remove; `--track`,
  `--passing`); `tolk plan` (list, new, show, item, done, expense, correct, pay,
  confirm, close, open, remove); and `tolk entry` for the entries of any feature,
  as JSON. `tolk room new --features` names the features of a new room.
- A feature Tolk knows which the stone doesn't offer is offered when its System
  Administrator first switches it on, as Rune offers the chat.
- The fake stone has features: declarations, entries with their rules (who adds,
  changes, removes and closes, one per Member, about another entry, closed), the
  socket with its announcements and signals.
- Differs from the plan: the interactive mode shows the chat, as before, and no
  panes for the calendar, the plans and the location; those are commands. A
  deleted message is told to a listener as a change of its reactions and then
  as deleted, since the stone removes what is about an entry first.
- Verified by `internal/chat/chat_test.go`, `internal/commands/features_test.go`
  and `chat_test.go` against the fake stone, with the race detector, and by hand
  against the uber-jar.

### Phase T6: together

Rune and Tolk against one stone, in rooms and chambers, repeatable from the
repository. `PLAN.md`, `RUNE.md`, `TOLK.md` and `RUNNING.md` say what was built.

Done when: nothing in the stone knows what a chat is, and the build and all
suites pass.

Done 2026-11-15:
- `scripts/flow/together.ts` (see E6 above) is the run of both against one
  stone: in a chamber Tolk lists the event Rune added and Rune parses the one
  Tolk added; both work out the same payments from the expenses of both; Rune
  shows the position Tolk reported; and in a room which is no chamber Tolk
  copies a file out of a chamber.
- The stone has no chat: `ChatManager`, `ChatHub`, the chat frames and the
  `.chat` Data Types went in T4; what is left is features, entries and the socket.
- `PLAN.md`, `RUNE.md`, `TOLK.md` and `RUNNING.md` point to what was built.

## Presence and calls

Tolk's part of `FEATURES.md`, "Presence and calls" (the user, 2026-11-17). Tolk
makes no calls: a terminal has no WebRTC without a third-party library. It shows
what it can know.

### What Tolk does

- **The light**: the interactive mode marks who is online among the members of a
  room, and is itself shown as online. `tolk members` gets a column for it.
- **Commands aren't shown as online**: a command which uses the socket joins
  unseen, so a script doesn't make its person flicker. `ui.present` in the
  config switches the interactive mode to unseen too.
- **Calls**: the interactive mode says in its notes when someone calls, in a
  room or directly, and that calls are made with Rune. In a chamber it reads the
  signal with the room key.
- **The calendar** knows events which are calls: `tolk calendar list` marks
  them, and `tolk calendar add` and `change` take `--call audio|video|none`.
- **The feature `calls`** is known by name and declaration (a copy of Rune's), so
  `tolk stone features` and `tolk features` offer and switch it.
- **The fake stone** tells who is present, joins unseen, passes a signal on to
  one Member, and takes a declaration whose signals have no least time.

### Phase T9: Tolk

Everything of "What Tolk does" above.

Done when: Tolk's tests pass against the fake stone, with the race detector; and
against one stone the interactive mode shows Rune's person as online and the
other way round, and an event Rune set as a call is listed as one.

Done 2026-11-17:
- `eds-cli/internal/client` has the frames `PRESENT` and `ABSENT` and the fields
  `unseen`, `present` and `to`. `internal/socket` joins shown or unseen
  (`Join(ctx, room, unseen)` answers with the features and who is present) and
  sends a signal to one Member (`SignalTo`). `internal/chat` joins unseen unless
  told to show (`Show`), keeps who is online in each joined room (`Online`,
  `OnlineAnywhere`) and tells the interactive mode who came, who went, and of a
  `ring` of the feature `calls`, which it reads with the room key in a chamber.
- The interactive mode (`internal/tui`) is shown as online, and unseen with the
  new setting `present` set to false (`tolk settings present false`,
  `ui.present` in `config.json`, on by default, also for a config from before).
  A dot marks who is online: before the members of a room in the pane "Rooms
  and users", and before a user who is online in any room the account is in.
  It follows `PRESENT` and `ABSENT`, and nobody is marked while the stone isn't
  connected. A call is told in the notes, in Rune's words with "Calls are made
  with Rune." after them, and with the bell if it is on.
- Every command joins unseen: `chat --plain`, `send --share`, `location report
  --passing` and `members`. `tolk members` has the column "online" and the
  field `online` in JSON: it joins unseen, reads who is present and leaves.
- `internal/features/calls.go` has the messages of a call as Rune's
  `features/calls.ts` reads and writes them, and the signal which carries one
  (Base64, the context `calls|signal`). The declaration `calls.json` is a copy
  of Rune's like the other four, and `tolk stone features` and `tolk features`
  know the feature as "Calls".
- The calendar (`internal/features/calendar.go`) reads and writes the line
  `X-EDS-CALL:audio|video` as Rune does: after `RRULE`, before the lines it
  doesn't know, and a kind it doesn't know stays among those. `tolk calendar
  list` marks such an event with "[Voice call]" or "[Video call]" and has
  `call` in JSON; `add` and `change` take `--call audio|video|none`; `import`
  and `export` keep the line.
- The fake stone lists who is present in its answer to joining, joins unseen,
  tells `PRESENT` and `ABSENT` with the first and the last shown connection of a
  Member (leaving, a closed connection, access taken away), passes a signal on
  to one Member, and tells a declaration with `"signals": 0` from one without
  signals. `OfferAll` offers the five features.
- Differs from the plan: the dot is also before the users, not only before the
  members of a room. The setting `present` is read when the interactive mode
  connects; it has no entry in the menu, and a change counts from the next
  start. `tolk members` leaves the column and the field out for a room the
  account isn't in, as only those who may join a room learn who is present.
  Tolk reads the `ring` of a call and nothing else of it: it doesn't tell that a
  call goes on which began before it joined, nor that one ended.
- Verified by `go vet ./...` and `go test -race ./...` in `eds-cli`: 102 tests
  pass, 11 of them new, against the fake stone: `internal/socket/presence_test.go`
  (the list, who came and went, unseen, a second connection of a Member, a
  signal to one Member, signals without a least time),
  `internal/features/features_test.go` (the line of a call there and back, the
  messages of a call with the cases of Rune's `test-features.ts`),
  `internal/chat/chat_test.go` (who is online, a call in a room and in a
  chamber), `internal/commands/presence_test.go` (the column of `members`,
  commands aren't shown, the calendar's calls) and
  `internal/tui/presence_test.go` (the dot, the note of a call in both
  languages, unseen).
- Run against one stone with Rune (`scripts/flow/together.ts`, 37 checks, 3 more):
  `tolk members` shows Rune's person as online and its own, a command, not;
  Tolk lists the call Rune set in the calendar, and Rune reads the one Tolk set.
- Not checked: the interactive mode against a real stone, so neither that Rune
  shows its person as online, nor its dot for Rune's person, nor its note of a
  call begun in Rune; those are tested against the fake stone only, which follows
  the contract and the hub's code as read.

## The status of stones

Tolk's part of the status of a stone (`STATUS.md`): what Tolk does there, and the phases in which it
was built. The design, the backend and the convention both clients follow are in
`STATUS.md`.

### What Tolk does

- Status keys per stone in the container.
- `tolk stone key` to create, list and withdraw keys on a stone, and
  `tolk stone name`.
- `tolk status [stone …]`: a table of the stones, or JSON for scripts.
- `tolk status --watch`, and a screen in the TUI: the live view, with bars and
  curves in the terminal.
- The fake stone in the tests answers the status.

### Phase M3: Tolk

Everything of "What Tolk does" above.

Done when: Tolk's tests pass against the fake stone, and `tolk status --watch`
shows two real stones on one screen.

Done 2026-11-15:
- A stone's entry in the container can keep a status key
  (`tolk update stone --status-key`, or `tolk stone key create --keep`), and a
  stone can be kept with a status key only, without an account
  (`tolk register stone <name> --url … --status-key`).
- `tolk stone key` (list, create, withdraw) and `tolk stone name`.
- `tolk status [stone …]`: a table, the busiest first, or the latest numbers as
  JSON; exit code 5 if a stone doesn't answer or refuses. `tolk status --watch
  [--interval n]` is the live view: bars for the storage and small curves
  (`▁▂▃▄▅▆▇█`) for people, requests, processors, the host's load, memory and the
  time a request takes, drawn anew each round. In the interactive mode "Stones…"
  in the menu shows the same, asked every five seconds while it is open.
- `internal/status` is what Rune's `status.ts` is, with its cases as tests; the
  view (`view.go`) is shared by the command and the interactive mode.
- The fake stone answers the status, by key and by session, and can be told to
  be busy, asleep or gone.
- Verified by `internal/commands/status_test.go` and the test of the screen in
  `internal/tui`; and by hand against the uber-jar: one stone under two names,
  by session and by key, in the table and in the live view. Not checked: two
  different real stones on one screen, and the live view on a real terminal.

## After L6

- 2026-11-15: chambers, features and the status (`E2E.md`, `FEATURES.md`,
  `STATUS.md`; each says there what Tolk got).
- 2026-11-17, "tool" became "feature" where a feature of a room is meant; the
  everyday word stays for programs (the tool chain of Tolk, tools reading the
  specification).
- 2026-11-17, the socket: closing a socket no longer races with arriving events. It
  closed its channel of events while the reader could still send on it, which
  panicked now and then ("send on closed channel"); the reader is the only sender
  and alone closes the channel when it ends, and the chat on top guards the end of
  its channel the same way. Tests for both (`socket_test.go`, `chat_test.go`).
- 2026-11-17, reactions, with Rune: the stone replaces a member's reactions to a
  message with a new entry and announces the end of the old one. Tolk remembers the
  entry the reactions came from: only its end ends them, and an earlier entry doesn't
  replace a later one. `TestChatOnEntries` failed of that now and then.
- 2026-11-17, chambers: the notice of a new chamber and the refusals of a chamber
  use Rune's new texts (`internal/commands/chamber.go`, `internal/service/chamber.go`).
- 2026-11-17, the TUI: the own access and the only administrator's can't be changed;
  the text without rooms is Rune's new one.
- 2026-11-17, the layout, after Rune's `layout.ts` (`internal/tui/layout.go`): the
  rooms pane keeps its size when panes come and go, and the panes with content
  share the rest; a pane hidden with Alt+1, 2, 3 comes back where it was and as
  large as it was, also after a restart (the layout from before is kept in
  `config.json`, `ui.layoutAway`), and as before when the panes were arranged anew
  meanwhile: rooms at the left with 22 % of the width, files at the right, the chat
  between. Tested in `layout_test.go`; not run in a terminal.
- Tests, 2026-11-17: `go vet` and `go test ./...` pass, with the copy of Rune's
  catalog renewed by `go generate`. The changes of the TUI weren't run in a
  terminal.

## Open decisions

- `tolk send -` from a true pipe (unknown length): decided 2026-11-10 (the user, "a
  big fan of pipes"): the stone accepts uploads without `Content-Length` and spools
  ciphertext only; no plaintext on the client's disk. Done as a stone change before
  the `send` command (L6b).
- CLI, decided 2026-11-10 (the user):
  - General rule: what can be done on Go's standard library without a third-party
    library is done that way, even at the cost of extra work, as every third-party
    library has a cost in CVEs.
  - So: no `cobra` (standard `flag`), no generator and no TOML (a hand-written client
    and a JSON config), no `coder/websocket` (a hand-written WebSocket client).
    `x/term` is the only library of the non-interactive CLI, and stays.
  - Order: the non-interactive CLI first, the TUI later. For the TUI, `bubbletea`
    and `lipgloss` are approved (2026-11-11), with the later goal of replacing them
    by code on the standard library.
  - Exit code 6 for "not permitted", so scripts can tell it from a failed login (3).
  - "Room" in the command names and flags too, with `--circle` as an alias.
  - No thumbnails from the CLI; its uploads get a generic icon in Rune (as in L5).
  - The terminal bell for new messages is off by default.
- CLI, decided 2026-11-10 (the user): the agent keeps the container unlocked, and
  `TOLK_PASSPHRASE_FILE` is the opt-in for unattended scripts (see "Behaviour").
  `tolk login` and `tolk logout` are the session handling.
- **One session per account (found 2026-11-11):** solved 2026-11-15 in EDS
  (`PLAN.md`, "Several sessions"): a session per login, so Tolk and Rune with one
  account no longer end each other's session. A call with `TOLK_PASSPHRASE_FILE`
  still logs in and out by itself, which now ends only its own session.
- Practical: Go modules need a reachable module proxy or vendored dependencies
  behind the proxy, as Quinoa needed a seeded Node cache. All of Tolk's are vendored
  (L6e, L6d); run `go mod vendor` after a change of `go.mod`.
- `TOLK_PASSPHRASE_FILE` is meant as a protected file, like an SSH key, but Tolk
  doesn't check its mode or owner. To decide: refuse a file others can read, as
  `ssh` does (it would refuse files that work today).
- `go generate` in the Maven build fails without Rune's sources, while the tests
  that compare the copies skip then. To decide which it should be.
- `cp` within a stone streams through Tolk and doesn't use the stone's copy: since
  E5 that is the design, as content moved between rooms and chambers is
  re-encrypted by Tolk.
- Of 2026-11-17, to decide: whether the stone should refuse a change of one's own
  access, or of the only administrator's, for every client (see `RUNE.md`, "Open").
- Later, a goal: replacing Bubble Tea and Lip Gloss with code on the standard
  library.
- Bugs and improvements of Tolk are tickets in `FINDINGS.md`.
