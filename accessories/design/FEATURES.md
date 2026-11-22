# EDS plan: features

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: planned 2026-11-15, nothing built. Discussed and decided with the user the
same day; what is still to decide is listed under "Open decisions".

Like `E2E.md`, this plan crosses the backend, Rune and Tolk. It builds on `E2E.md`
and shares its order of work (see "Order of work"). Code comments cite it as
`FEATURES.md`. Phases are named T1 to T6.

## The idea

EDS holds what a group needs to exchange things safely. The chat was built on that
by hand, as a part of its own. This plan turns it into the first of several
**features**: a feature is something a room or chamber can have beside its files,
and can be switched on and off. Planned beside the chat:

- **Calendar:** shared among the people of a room. Not meant as a private
  calendar, but a room with one person makes it one.
- **Location:** where the people of a room are, for families. Existing services
  for this are worrying because nobody knows who else sees the positions. In a
  chamber, the stone doesn't.
- **Plans:** a few people plan a trip or an event, note what it costs and who
  paid, and settle it afterwards.
- **Calls** (added 2026-11-17): voice and video between the people of a room,
  or between two of them, and calls set for a time in the calendar. See "Presence
  and calls".

Each feature keeps what it stores in a folder of its own in the Circle: `.chat`,
`.calendar`, `.location`, `.plans`.

A view of the stones themselves for System Administrators (storage, load, people
active) was discussed as a feature too and is kept apart: it belongs to a stone, not
to a room, and needs no declaration. It gets a plan of its own.

Decided about how:

- **A feature is a declaration, not code in the stone.** The stone knows a fixed set
  of rules; a declaration names a feature, its folder, and picks rules from that set.
  A new feature is then a matter of the clients, and the stone doesn't change for it.
- **The stone handles entries, never their content.** It stores an entry, notes
  who wrote it and when, enforces the declaration's rules, and tells the
  subscribers. What an entry means is known to the clients only. That holds in
  rooms and chambers alike.
- **The backend's part must be complete with this plan**, as in `E2E.md`: the set
  of rules has to be right the first time.

## Words

- **Feature**, plural features (German "Funktion", "Funktionen"): the word in Rune,
  Tolk, the backend and its API (the user, 2026-11-17). It was "tool" ("Werkzeug")
  from 2026-11-15 until then; "tool" and "plugin" aren't used. The word means this
  one thing in the plans: what EDS or a client can do otherwise isn't called a
  feature. The phases keep their names T1 to T6, which the plans cite.
- Where a list of names does the job, no noun is used at all: "In this room: Chat,
  Calendar".
- An **entry** is one thing a feature stores; a **kind** is a sort of entry within a
  feature, with rules of its own; a **signal** is a short message passed on to the
  others and not stored. These three are words of the plans and the API, not of
  Rune's texts.

## Goals

- Features can be offered by a stone and switched on per room and chamber.
- The chat is rebuilt as a feature, with everything it does today.
- A calendar, a location feature and plans exist as declarations and in the clients.
- The stone doesn't need a change for a further feature.
- A feature works in rooms and in chambers; in a chamber its entries are encrypted by
  the clients (`E2E.md`).

## Non-goals

- Code loaded into the stone.
- Files as a feature: they stay the base of every room and chamber. To look at later.
- A phone app. The location feature is of limited use without one; see "Location".
- Features that work across rooms, or across stones.
- Moving a stone's existing chat history: EDS 2.0 has no upgrade in place
  (`PLAN.md`, Rules).

## The chat today

It already has the parts of a feature, built by hand (`PLAN.md`, Clients API, L4 and
"After L5"):

| Part | Today |
|---|---|
| Reserved folder | `.chat` in the root of the Circle; hidden, and the data requests may read but not change it |
| Kinds of entries | messages (Data Type `chat`) and a marker per Member for received and read (`chat-receipt`) |
| Rules | sending needs WRITE; only the author edits or deletes a message, not an Administrator; reactions need WRITE; a marker belongs to its Member |
| Author and time | set by the stone, kept inside the encrypted content, so the database doesn't show who wrote what |
| Socket | `/api/v1/chat`: history in pages, new and changed messages, receipts, and `CHANGED` for the files of the Circle |
| Not stored | `TYPING`, at most once in 2 s per connection |

What the stone knows about a chat beyond that is what moves to the clients: the
form of a message, replies, the six reactions, the shared file, and how unread
messages are counted.

## Backend: what the stone offers

### Declarations

A declaration is a text the System Administrator gives the stone. It holds:

- the feature's name (lower case, a word) and a version;
- its folder: `.` and the name, in the root of the Circle;
- one or more kinds, each with rules from the table below;
- whether signals are allowed, and how often per connection.

The rules of a kind:

| Rule | Choices |
|---|---|
| Who may add | everyone in the Circle (READ), those who may write (WRITE), Administrators |
| Who may change an entry | nobody, or any of: its author, everyone who may add, Administrators |
| Who may remove an entry | the same choices |
| Who may close an entry, and open it again | the same choices |
| What is kept | everything; one per Member (a new one replaces the old); one per Member about each entry; entries up to an age; the newest up to a number |
| About another entry | not at all, or an entry of a named kind of the same feature; an entry about another goes when that one is removed |
| Size | the largest content of an entry |

- **Any of** means a combination: "its author and Administrators" is one rule.
- **A closed entry** can't be changed or removed, nothing new can be about it, and
  what is about it can't be changed or removed either. That it is closed is noted
  by the stone, like author and time.
- **The System Administrator** adds, replaces and removes declarations, and so
  decides what the stone offers. A replaced declaration counts for what is written
  afterwards. One that rooms use can't be removed.
- **An Administrator of a Circle** switches an offered feature on and off there.
  Switching on creates the folder. Switching off keeps the entries and hides them:
  nothing is read, written or announced. Removing a feature's entries is a separate
  request (decided 2026-11-15). Between on and off there is read-only: everything
  can be read, nothing written, which makes an archive of a chat or a calendar.
- **Everyone** can read what the stone offers and what a Circle of theirs uses, so
  a client knows what to show.

### Entries

- An entry is a Data Object in the feature's folder, so storage, the stone's
  encryption, the storage ceiling, sanity checks and streaming are what they are
  for files.
- The stone notes author, time, the time of the last change and the entry it is
  about. These are kept inside the stone's encryption, as the chat does today: a
  copy of the database doesn't show who wrote what.
- Every entry has a position, its place in the order of its feature in that Circle.
  Listing goes by kind, or by the entry they are about, in pages backwards or
  forwards from a position, and the stone counts the entries of a kind after a position (which is how a client
  finds what is unread).
- A client may give an entry a name; names starting with the same text can be
  listed together (a calendar's month, for one).
- The data requests may read a feature's folder and entries, but not add, change,
  move, copy or remove them. All writing goes through the feature, so its rules hold.
  While a feature is switched off, they can't read them either.
- The name of an offered feature's folder is reserved in the root of every Circle.

### The socket

One socket for everything, in place of the chat's:

- The first frame authenticates, as today. A client then joins Circles.
- Requests: add, change and remove an entry, list, count, and send a signal.
- Announcements to the others who joined: entry added, changed, removed (with the
  feature, kind, entry, author and time; and the content when it is small), a signal,
  a feature switched on or off, and the files of the Circle changed (`CHANGED`, as
  today).
- Access ends promptly when it is taken away, and the limits on what may wait for
  a slow connection stay (`PLAN.md`, L4).

### Presence, and signals to one Member

Added 2026-11-17 for the online light and for calls ("Presence and calls"), and
general like the rest: the stone tells who is there and passes on what it is
given, and knows nothing of a call.

- **Present** in a Circle is a Member with at least one connection which joined
  it. The answer to joining lists the Members present, and the others who joined
  are told when a Member's first connection comes and when the last one goes:
  by leaving, by closing, by a connection lost, or by access taken away.
- **Only those who joined a Circle learn who is present in it**, and joining needs
  READ there. So presence is seen by people who share a room, and nobody gets a
  list of who is on the stone.
- **A connection may join without being shown.** It still learns who is present.
  This is for a person who doesn't want to be seen, and for a script.
- **A signal may name one Member.** It is then passed on to the connections of
  that Member which joined the Circle, and to nobody else; the sender may name
  themselves, to reach their other devices. Whether the Member is shown as present
  doesn't matter.
- **A signal has up to 32768 characters** (4096 before), and a declaration may
  set the least time between two signals to 0.
- Nothing of this is stored, and the status of a stone (`STATUS.md`) is unchanged.

### The API

- Features of the stone: list, add or replace, remove.
- Features of a Circle: list, switch on, switch off, remove the entries.
- Entries: add, change, remove, read, list and count, for scripts and for content
  too large for a frame. Reading and writing large content streams
  (`PLAN.md`, Streaming).

The exact operations, frames and the form of a declaration are fixed in T1, with
the OpenAPI documentation.

### What goes from the stone

The chat's own operations and frames, `ChatManager` and its helpers, the form of a
stored message, and the Data Types `chat` and `chat-receipt`.

The old chat and the chat as a feature both want the folder `.chat`, so a stone can't
serve both. Until Rune has moved (T4) the stone serves the old chat, and the tests
of T1 and T2 use a declaration of another name. With T4 the old chat leaves the
stone. Tolk follows in T5; between the two, Tolk's chat works against its fake
stone only.

### Why the backend needs no later change

| Later wish | Needs from the stone |
|---|---|
| A new feature | a declaration |
| A new kind of entry in a feature, or other rules | a replaced declaration |
| Polls, pinned messages, threads in the chat | nothing, or a new kind |
| Entries in a chamber | nothing: content is the clients' |
| A phone app that reports positions | nothing |
| Who is online, shown in another way | nothing: presence is told per Circle |
| Calls with more people through a media server, screen sharing, another way to set a call up | nothing: signals carry whatever the clients agree on, also to one Member |
| A relay for calls, per room or per stone | nothing: an entry, or the clients' own settings |

What would need a change is a rule that isn't in the table. So T1 checks the table
against the first four features before anything else is built, and against two more that
aren't planned (a shared list of tasks, a notice board), to see whether they fit.
The plans already added three things the first three features hadn't asked for:
combined permissions, closing an entry, and listing by the entry something is
about.

The stone did change once more, on 2026-11-17, for "Presence, and signals to one
Member". The table hadn't such a wish, as it isn't about entries: who is
connected is known to the stone only. The three last rows are what that change
is meant to cover.

## The features

Their forms are conventions of the clients, as in `E2E.md`: Rune owns the files,
Tolk keeps copies, and both are tested against shared examples. The declarations
are files in the repository which Rune and Tolk bring along.

### Chat

| Kind | Add | Change | Remove | Kept | About |
|---|---|---|---|---|---|
| message | WRITE | author | author | everything | |
| reaction | WRITE | author | author | one per Member about each entry | message |
| receipt | READ | author | nobody | one per Member | |

Signals: typing, at most once in 2 s.

- A message holds text, the message it replies to, and a shared file. The stone no
  longer checks that the file is of the same Circle; reading it needs access to it
  anyway.
- A Member's reactions to a message are one entry, so the stone's "one per Member
  about each entry" is enough for the six reactions.
- A receipt holds the positions up to which the Member's client received and the
  Member read. Unread is the count of messages after it.
- Retention, which `PLAN.md` left open, becomes a choice of "what is kept" in the
  declaration.

### Calendar

| Kind | Add | Change | Remove | Kept |
|---|---|---|---|---|
| event | WRITE | everyone who may add | everyone who may add | everything |

- One entry per event, named by its month, so a client fetches the months it
  shows. Repeating events are named apart and always fetched.
- The form of an event follows iCalendar, so events can be brought in and taken
  out as `.ics`.
- Two people changing the same event: the later change wins, and the earlier
  author's client is told, as for every change.
- First version (decided 2026-11-15): single events and events repeating daily,
  weekly, monthly or yearly; the time zone is kept with the event; no invitations
  to single events.
- Reminders are the client's.

### Location

| Kind | Add | Change | Remove | Kept |
|---|---|---|---|---|
| position | READ | author | author | one per Member |
| track | READ | nobody | author | entries up to an age |

Signals: a position that isn't to be kept at all.

- Each Member decides on each device whether it reports, and whether only the
  latest position or a track. Nobody reports for someone else: only the author
  writes.
- The age up to which a track is kept is in the declaration; a room that wants
  another gets a declaration of another name.
- In a room, the stone can read the positions while it handles them. A client
  recommends a chamber when the location feature is switched on, and says why.
- Without a phone app, a position comes from a browser while Rune is open, or from
  a script through Tolk. That is enough to build and prove the feature, not to use it
  in a family. The feature is built with these two as reporters (decided
  2026-11-15); a phone app is not part of this plan.

### Plans

| Kind | Add | Change | Remove | Close | About |
|---|---|---|---|---|---|
| plan | WRITE | everyone who may add | author | author and Administrators | |
| item | WRITE | everyone who may add | everyone who may add | | plan |
| expense | WRITE | author | author | | plan |
| payment | WRITE | author | author | | plan |
| confirmation | WRITE | nobody | author | | payment |

- A plan is a trip or an event, with a name, dates and a place. Several can live
  in one room; everything else is about one of them, and goes with it.
- An item is something to do, to book or to bring, and who takes it.
- An expense says who paid, how much, in which currency, and among whom it is
  shared and how. Its author corrects it, or removes it (the user, 2026-11-15);
  the stone notes the time of the change and tells the others, so no share shifts
  unseen.
- A payment says that one person gave another money to settle; the receiver
  confirms it. A payment changed after its confirmation shows as not confirmed.
- Who owes whom is worked out by the clients from expenses and payments, with as
  few payments as possible. The stone never sees an amount as such; in a chamber
  it can't read one.
- A plan that is settled is closed, and its records stay as they are.
- Someone on the trip without an account on the stone is a name in the entries.
- A plan's dates can be put into the calendar of the room by the client.

### Calls

Added 2026-11-17; the design is in "Presence and calls".

| Kind | Add | Change | Remove | Kept |
|---|---|---|---|---|
| relay | Administrators | Administrators | Administrators | the newest one |

Signals: everything that sets a call up, without a least time between two.

### The forms of the entries

Fixed in T3 (2026-11-15). The content of an entry is a JSON object in UTF-8 with
`"v":1`; readers pass over fields they don't know. In a chamber it is encrypted as
content of `E2E.md`, with the context `<feature>|<kind>`. The declarations are
`eds-quarkus/src/main/webui/src/features/declarations/*.json`; Rune owns them and
Tolk keeps copies. A person is `m:<memberId>`, or `g:<name>` for a guest without
an account. Times are ISO-8601 in UTC with `Z`; days are `YYYY-MM-DD`.

| Feature, kind | Content | Name of the entry |
|---|---|---|
| chat, message | `{"v":1,"text":…,"reply":entryId?,"file":dataId?}` | the stone's |
| chat, reaction | `{"v":1,"reactions":[…]}`: all reactions of the Member to the message; the six of Rune | the stone's |
| chat, receipt | `{"v":1,"received":position,"read":position}` | the stone's |
| calendar, event | `{"v":1,"ics":"BEGIN:VEVENT…END:VEVENT"}`: one event of iCalendar | `YYYY-MM/<id>` by the month it starts in; `repeat/<id>` for a repeating one |
| location, position | `{"v":1,"lat":…,"lon":…,"acc":metres?,"at":time,"note":…?}` | the stone's |
| location, track | as a position | the stone's |
| plans, plan | `{"v":1,"title":…,"from":day?,"to":day?,"place":…?,"currency":"EUR","people":[person…]}` | the stone's |
| plans, item | `{"v":1,"text":…,"who":person?,"done":bool}` | the stone's |
| plans, expense | `{"v":1,"text":…,"amount":"12.50","currency":"EUR","paidBy":person,"split":[{"who":person,"share":1}…],"at":day?}` | the stone's |
| plans, payment | `{"v":1,"from":person,"to":person,"amount":"10.00","currency":"EUR","at":day?}` | the stone's |
| plans, confirmation | `{"v":1}`, by the receiver of the payment | the stone's |
| calls, relay | `{"v":1,"servers":[{"urls":["turn:host:3478"],"username":…?,"credential":…?}…]}`: the STUN and TURN servers of the room, as WebRTC takes them | the stone's |
| calendar, event which is a call | the event, with the line `X-EDS-CALL:audio` or `X-EDS-CALL:video` in its VEVENT | as an event |

Signals: the chat sends `typing`; location sends a position as its JSON, for one
which isn't to be kept; calls send the messages of "Presence and calls", which
unlike the two are encrypted in a chamber.

**Who owes whom** (plans). An amount is a decimal text with at most two decimals,
counted in hundredths. Of an expense, each of the split bears `amount × share ÷ sum
of the shares`, rounded down; the hundredths which rounding leaves go, one each, to
the first of the split. The one who paid is owed the amount. A payment moves its
amount from the one who paid it to the one who got it; it counts when it is
entered, and the confirmation of the receiver is shown beside it. Currencies are
kept apart. To settle a currency, the one who owes most pays the one who is owed
most, as much as the smaller of the two, until nothing is left; of equals the
first by the person's text. The independent implementation is
`vectors/plans-v1.py`, its examples are `src/features/plans-examples.json`.

A confirmation counts if its author is the `to` of the payment, and if it was added
after the payment was last changed; the stone notes both times.

## Presence and calls

Added 2026-11-17 (the user): a light beside a person who is online; voice and
video calls in a room and between two people; and calls set for a time, of which
people are told. The stone's part is "Presence, and signals to one Member" above.
Phases T7 to T9.

### Words

- **Online**: a person has Rune, or Tolk's interactive mode, open and connected.
  It doesn't say that they sit in front of it. German "online".
- **Call** (German "Anruf"): voice, or voice and video. A **room call** is open to
  everyone in a room; a **direct call** is between two people.
- **Relay**: a TURN server, which passes a call on when two devices can't reach
  each other; with it a STUN server, which tells a device its address as others
  see it.

### What is decided

- **EDS is no replacement for other systems** (the user, 2026-11-17). A call has
  at most **four** people. The devices connect to each other, everyone to
  everyone, which carries about that many with video; more would need a media
  server, which isn't planned.
- **The browser makes the call** (WebRTC, built in, no library). Sound and
  picture go from device to device, encrypted between them (DTLS-SRTP), and never
  through the stone. The stone passes on the setup, as signals of the feature
  `calls`.
- **A call needs the feature `calls` in a room.** A room call is the room's. A
  direct call is set up in a room the two share: the first with `calls` in which
  the other is present, a chamber before a room. Its signals name the other
  Member, so nobody else in that room learns of the call. Two people who share no
  room can't call each other; so not everyone on a stone can ring everyone.
- **Only someone who is online can be rung.** Nothing is pushed to a closed app,
  as for chat messages (`RUNE.md`, notifications). A direct call rings for 45
  seconds.
- **Who is shown as online** is each person's choice, on by default, kept with
  their settings. The light is beside a person wherever Rune lists people, for
  those one shares a room with.
- **A call set for a time is an event of the room's calendar**, marked as a call.
  It needs the calendar in the room, and nothing new from the stone. Everyone
  whose Rune is open is told ten minutes before and when it starts, with a button
  to join; who opens Rune later sees it in the calendar, and joins while the call
  goes on.
- **No relay is built in, and none is asked for elsewhere.** Without one a call
  connects where the devices reach each other directly: in one network, or with
  public addresses. Otherwise an administrator of the room names a STUN or TURN
  server, kept as an entry of the room, in a chamber unreadable for the stone.
  Rune names no public server by itself, as that would tell a third party who
  calls. `RUNNING.md` says how to run one (coturn).
- **Tolk makes no calls**: a terminal has no WebRTC without a third-party library
  (`TOLK.md`, the rule of the standard library). It shows who is online, that
  someone calls, and which events are calls.

### What protects a call

- The media is encrypted between the devices, always. The stone never has it; a
  relay passes on what it can't read.
- The setup holds the fingerprints of the keys the devices use. In a **chamber**
  it is encrypted with the room key (`E2E.md`, content, context `calls|signal`):
  the stone can neither read nor change it, so the call is end-to-end between
  members of the chamber. In a **room** the stone passes the setup on readable,
  and one which turns against its users could put itself between the callers:
  the same trust as for the room's other content.
- The stone learns who calls whom, when, and for how long the setup lasts; with
  a room's readable setup also the network addresses of the devices. The devices
  learn each other's addresses in any case, as they connect to each other. Rune
  says so before the first call.

### The messages of a call, version 1

A signal of the feature `calls` is the Base64 of a JSON object in UTF-8, in a
chamber encrypted as content with the context `calls|signal`. Readers pass over
fields and types they don't know.

`{"v":1,"t":type,"call":id,"peer":id,…}`: `call` is `room` for the room call (a
room has one at a time) or a UUID for a direct call; `peer` is a UUID a device
makes for itself when it enters a call, as a Member may have several devices.

| Type | From | To | Holds | Means |
|---|---|---|---|---|
| `ring` | who starts | the room, or the Member called | `media`: `audio` or `video` | a room call began, or a direct call is asked for |
| `join` | a device entering | the room, or the other Member | `media` | it is in the call now |
| `here` | every device in the call | the Member who joined | `target`: the peer who joined, `media` | it is in the call too |
| `full` | a device in the call | the Member who joined | `target` | the call has four already |
| `offer`, `answer` | a device | the Member of the other | `target`, `sdp` | the session description of WebRTC |
| `ice` | a device | the Member of the other | `target`, `candidate` | a candidate of ICE, as WebRTC gives it |
| `state` | a device in the call | the room, or the other Member | `mic`, `camera`: true or false | what it sends now, told when it meets a device and when its person switches one |
| `leave` | a device leaving | the room, or the other Member | | it left the call; a direct call ends |
| `decline` | the Member called | the caller | | a direct call isn't taken |
| `taken` | the device which took a direct call | its own Member | | the other devices stop ringing |
| `probe` | a device which joined the room on the socket | the room | | is a room call going on? |
| `ongoing` | every device in the room call | the Member who asked | `media`, `peers`: how many | yes |

- **Who offers**: of two devices in a call, the one whose `peer` is the smaller
  text sends the `offer`. Each learns of the other by `join` or `here`.
- **Entering**: a device sends `join` and is in the call. The devices in it answer
  with `here`, or one with `full` when they are four; then it leaves again.
- **A room call** exists while a device is in it. The first to enter sends `ring`
  after its `join`, so the others are told once. Who comes later asks with
  `probe`. A device whose Member is no longer present in the room (the stone
  tells) is taken to have left.
- **A direct call**: `ring` to the Member, who answers on one device with `join`
  (and `taken` to their others) or `decline`; without either the caller gives up
  after 45 seconds and sends `leave`.
- **Media**: every connection is set up for voice and video both ways. A device
  which sends no picture sends none until its person switches the camera on,
  without a new setup. `media` says what the person asked for at the start, and
  `state` what a device sends from then on.
- **A call set for a time** has no message of its own: who joins it enters the
  call of the room, as at any other time.
- Sizes: a signal has up to 32768 characters, which a session description with
  video stays far below.

## Rune

In `RUNE.md`, "Features" and "Presence and calls".

## Tolk

In `TOLK.md`, "Features" and "Presence and calls".

## Order of work

`E2E.md` and this plan change the same parts of Rune and Tolk, the chat most of
all. So the two backends come first, and each client is changed once (decided
2026-11-15):

| Step | Phases | What |
|---|---|---|
| 0 | N1 | Several sessions per account (`PLAN.md`, "Several sessions") |
| 1 | E1 | Backend for E2E |
| 2 | T1, T2, M1 | Backend for features, and the status (`STATUS.md`); the old chat still served |
| 3 | E2, T3 | The conventions: E2E, and the four features |
| 4 | E3, E4, T4, M2 | Rune: chambers, features (the old chat leaves the stone), the status |
| 5 | E5, T5, M3 | Tolk: chambers, features, the status |
| 6 | E6, T6 | Together |

Built one phase at a time and without pauses: the user reviews once, at the end
(decided 2026-11-15). So every phase leaves a summary in its plan file: what was
built, and what differs from the plan. The review gets the work by layer, not by
phase (`PLAN.md`, "Handed over for review").

## Phases

### Phase T1: declarations and entries

The form of a declaration, the rules, offering and switching, entries with
positions and names, the API, and the OpenAPI documentation. First the check of
the rules against five features (see "Why the backend needs no later change").

Done when: `mvn clean install` passes with the completeness test; unit tests
cover every choice of every rule, switching off and on again, removing entries,
the reserved folders, and that the database shows neither author nor content;
FitNesse covers a feature from declaration to entries.

Done 2026-11-15:
- **The check of the rules first.** The four features fit, as the tables of "The
  features" have them. Two features which aren't planned fit as well: a list of tasks
  (a task which everyone who may add changes and closes, comments about a task) and
  a notice board (notices by administrators, kept up to a number, and one
  acknowledgement per Member about each notice). What the fixed set can't say, and
  what is therefore the clients' to check: that only a Member named in an entry
  may add something about it (the receiver confirming a payment), and entries which
  only some of a Circle may read. Everyone in a Circle reads all its entries.
- **Declarations**: a JSON object, see `FeatureDeclaration` and the Features operations
  of the API documentation. `add` is READ, WRITE or ADMIN; `change`, `remove` and
  `close` are lists of `author`, `adders` and `administrators`; `keep` is `all`,
  `member`, `member-about`, `age:<seconds>` or `count:<number>`; `about` names a
  kind; `size` is in bytes; `signals` is the least milliseconds between two
  signals of a connection. Unknown fields are refused, so a mistyped rule isn't
  silently no rule.
- **Tables**: `eds_features`, `eds_circle_features`, `eds_entries`; the Data Type
  `entry`.
- **An entry** is a Data Object of the type `entry` in the feature's folder, with a
  row of its own for what the stone notes: kind, position, the entry it is about,
  closed, the time of the last change. The author is sealed with a subkey of the
  Circle Secret (`eds/entry/author|circleId`), and the Member's own entries are
  found by a keyed value of it (`eds/entry/owner|circleId|memberId`). The
  database shows kinds, positions, what is about what, times and sizes; neither
  author nor content.
- **Positions** count per feature and Circle, from 1. An entry which replaces its
  Member's old one keeps its ID and gets a new position.
- **API**: resource API only, with the Session in the `Authorization` header:
  `/features`, `/circles/{circleId}/features` and below it the entries, `count` and
  `closed`. In the contract, with documentation for every operation.
- **Rules**: every choice of every rule is enforced in `FeatureManager`; writes lock
  the Circle. Switching off hides the entries from the feature's own requests and
  from the data requests; read-only refuses every write; removing a feature from a
  Circle removes its entries and folder.
- **Reserved**: the folder name of every feature the stone offers, in the root of
  every Circle. The data requests can't write a feature's folder or entries.
- **What happened** is collected per call (`FeatureEvent`) and fired after the
  transaction succeeded, for the socket of T2.
- Tests: `FeatureDeclarationTest` (5), `FeaturesTest` (20: who declares and switches,
  every rule, every way of keeping, closing, listing and counting, the states,
  the data requests, what the database shows, the events). FitNesse `EDS.API` has
  a section "Features" with its own fixture; the suite passed with 853 right, 0
  wrong.
- Differs from the plan:
  - No POST twins: the features are in the resource API only.
  - An entry is at most `eds.data.max.inline.size` large and is written whole;
    its content is read with the content download too. Larger things are files,
    which an entry names.
  - Entries past their age are removed when the kind is added to or listed, not
    by a timer; until then they stay in the database, unread.
  - The folder of a feature which is switched off is still named in a listing which
    asks for hidden names; nothing in it is read.
  - The name `chat` is refused for a feature until the old chat has left the stone
    (T4).
  - No limit on how often entries are added.

### Phase T2: the socket

The general socket with requests, announcements and signals, beside the chat's,
which stays.

Done when: tests with real WebSockets cover two clients in one Circle for every
announcement, signals and their limit, access taken away, and a slow connection;
and the chat's declaration, used through the general socket, does what `ChatTest`
asks of the chat today.

Done 2026-11-15:
- **The socket** is `/api/v1/socket` (`SocketEndpoint`, `SocketHub`), beside the
  chat's, which stays until T4. Frames are the schemas `SocketFrame` and
  `SocketFrameType` of the contract, described in its overview ("Features and the
  socket"); a frame has at most 256 KiB.
- **Requests**: `AUTH`, `JOIN`, `LEAVE`; `ADD`, `CHANGE`, `REMOVE`, `CLOSE` and `GET`
  (answered with `DONE`), `LIST` (`ENTRIES`), `COUNT` (`COUNTED`), `SIGNAL` (not
  answered). A Circle is joined before its features are used. Every request is
  authenticated and checked by `FeatureBean`, like a request of the resource API; the
  hub knows nothing of what a feature is for.
- **Announcements**: `ADDED`, `CHANGED`, `REMOVED` with the entry, `FEATURE`, and
  `FILES` (the chat's `CHANGED` for the files of a Circle). They are fired by the
  bean and reach the hub after the transaction succeeded, so they are the same for
  a change through the socket and through the resource API, and a request which
  fails tells nothing.
- **Signals**: every Trustee of a Circle may send one in a feature which declares
  them, up to 4096 characters; one which comes sooner than the declared interval
  after the connection's last is dropped. Passed on to the others, not stored.
- **Access ends promptly**, and the limits on what may wait for a connection are
  those of the chat's hub.
- Tests: `SocketHubTest` (12, connections in memory, the real hub and features on the
  test database) and `EdsSocketTest` (2, real WebSockets in the running stone:
  the resource API and two connections, every announcement, a signal, access
  taken away, a logout, the first frame).
- Differs from the plan:
  - The connection which asked for a change is told of it like the others, and
    before its answer, as the announcement is sent when the change is committed.
    Clients tell the two apart by the entry.
  - A content of more than 16384 characters as Base64 is left out of the
    announcement and fetched.
  - An entry too large for a frame is written with the resource API.
  - The chat's declaration through the socket is tested with T3, where the
    declaration is written. Here the general parts it needs are tested: one per
    Member, one per Member about each entry, change and removal by the author,
    counting after a position, and signals.
- Left open: delivery across several instances of EDS, and limits on how often a
  connection may ask.

### Phase T3: the conventions of the features

The forms of the entries of chat, calendar, location and plans, in a room and in
a chamber, written out in this file, the four declarations, and shared examples,
among them expenses with the balances that follow from them.

Done when: the forms and declarations are in the repository and the examples
cover every kind.

Done 2026-11-15:
- The four declarations are in `eds-quarkus/src/main/webui/src/features/declarations/`,
  and the forms in "The forms of the entries" above.
- `FeatureConventionsTest` (core; skipped without Rune's sources): the stone accepts
  all four, and the chat's declaration does what the chat of the stone did:
  sending with WRITE, the history in pages, editing and deleting by the author
  only, reactions with one entry per Member about each message, a read marker per
  Member also with READ, unread messages by counting after it, a deleted message
  taking its reactions along, and typing as a signal. This is the test T2 left for
  here. The feature is called `talk` in it, as the old chat still has the name.
- `vectors/plans-v1.py` and `src/features/plans-examples.json`: seven examples of
  expenses, payments, balances and what settles them (even and uneven shares, a
  hundredth left over, a guest, a payment made, two currencies, settled, the
  payer not among those who share).
- Decided here: a track of the location feature is kept for 24 hours; the sizes of
  the kinds (a chat message up to 70000 bytes, which is the old limit of 16384
  characters with room for a chamber); amounts have two decimals.
- Not covered by examples: the forms of calendar and location entries, which are
  one object each; their examples come with the clients.

### Phase T4: Rune

In `RUNE.md`, "Features". The old chat left the stone in this phase ("What goes from the
stone").

### Phase T5: Tolk

In `TOLK.md`, "Features".

### Phase T6: together

In `TOLK.md`, "Features".

### Phase T7: presence, and signals to one Member

The stone's part of "Presence and calls": who is present in a Circle, joining
without being shown, a signal to one Member, and the longer signal. In the
contract with its documentation.

Done when: `mvn clean install` passes; tests cover the list on joining, the first
connection of a Member coming and the last one going by each of the four ways,
a second connection of the same Member changing nothing, joining unseen, and a
signal to one Member reaching all their connections and nobody else's.

Done 2026-11-17:
- **Frames**: `JOIN` takes `unseen`; `JOINED` carries `present`, the IDs of the
  Members present, in order; `PRESENT` and `ABSENT` with the Circle, the Member
  and the account name; `SIGNAL` takes `to`, and carries it on. In the contract
  (`SocketFrame`, `SocketFrameType`) and its overview, "Features and the socket".
- **The hub** (`SocketHub`) keeps for each connection the Circles it is shown in.
  Coming and going are told under one lock, so the list a connection gets on
  joining and what it is told afterwards are in order. A connection which goes,
  by closing, by a Session which ended, or because too much waited for it, takes
  its Member along where it was their last; so does access taken away. Joining
  again decides anew whether the connection is shown.
- **A signal** has up to 32768 characters; `"signals": 0` in a declaration was
  valid before and is now tested.
- Tests: `SocketHubTest` has 4 more (16): the list and who came and went with a
  second connection of a Member, going by each of the four ways, joining unseen,
  a signal to one Member and to oneself. `EdsSocketTest` covers the list, a
  Member coming, a signal to one Member and going by access taken away, with real
  WebSockets. The build passes: 150 tests in `eds-api`, 697 in `eds-core`, 29 in
  `eds-quarkus`, and FitNesse.
- Differs from the plan: nothing.
- Found on the way: the header `Permissions-Policy` of Rune switched the
  position off for Rune itself (`geolocation=()`), so a browser could not report
  one for the location feature. It now allows it, with the microphone and the
  camera (`RUNE.md`, "Presence and calls").
- Left open: who is present isn't told across several instances of EDS, like
  everything else of the socket ("Open decisions").

### Phase T8: Rune

In `RUNE.md`, "Presence and calls".

### Phase T9: Tolk

In `TOLK.md`, "Presence and calls".

## Decisions

All 2026-11-15, the user:

- A layer for features, with the chat as the first, then a calendar and a location
  service, each with a folder of its own.
- A feature is a declaration with rules from a fixed set, not code in the stone and
  not a fixed list.
- Features are offered per stone and switched on per room or chamber.
- Only the System Administrator adds declarations.
- Switching a feature off keeps its entries and hides them.
- The chat is rebuilt on the general parts.
- Features work in rooms and chambers alike; the content doesn't matter to the stone.
- The word is "features", in the clients and the backend.
- Files stay the base and aren't a feature.
  (2026-11-17, the user: Rune shows the chat and the files like the further
  features, each with a tab and a pane; on the stone nothing changes, the files
  aren't declared and can't be switched off. See `RUNE.md`.)
- The backends first, then each client once.
- Plans as a fourth feature; the author of an expense corrects and removes it.
- The view of the stones for System Administrators is not a feature and is planned
  apart.
- The feature for trips is called "Plans" ("Pläne"); payments are corrected by their
  author like expenses; a settled plan can be closed.
- Location is built with the browser and Tolk as reporters, without a phone app
  and without a map in Rune.
- The first calendar has single and simply repeating events.
- One review at the end; several sessions per account come first.

2026-11-17, the user:

- A light beside a person who is online. The stone tells who is present, per
  Circle, in place of the clients telling each other.
- Voice and video calls in the chat of a room, and between two people from the
  list of people.
- Calls can be set for a time, and people are told.
- A call has at most four people: "EDS was never intended to be used as a
  replacement for other systems."

## Open decisions

- **Delivery across several instances** of EDS and rate limits per connection,
  open for the chat today (`PLAN.md`, Clients API, "Open"), now for the socket.
- **Whether a declaration may be changed** in ways that make stored entries break
  a rule (a smaller size, a shorter age). Planned: the new rule counts for what is
  written afterwards, and ages are applied to everything.
- **A phone app,** which the location feature needs to be of real use, and a map.
- **Files as a feature**, later.
- **A relay for a whole stone.** Today an administrator names the servers per
  room. A stone-wide default would be a setting everyone may read, or a
  credential the stone hands out for a short time; neither is built.
- **Ringing someone whose app is closed** needs Web Push through the browser
  makers' services, which Rune doesn't use.
