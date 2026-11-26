# EDS plan: the status of a stone

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: planned and built 2026-11-15 (M1 here, M2 in `RUNE.md`, M3 in `TOLK.md`).
Discussed and decided with the user the same day; what is still to decide is
listed under "Open decisions".

Like `E2E.md` and `FEATURES.md`, this plan crosses the backend, Rune and Tolk. It
doesn't depend on either. Code comments cite it as `STATUS.md`. Phases are named
M1 to M3.

## The idea

Someone who looks after a number of stones wants to see them on one screen: how
full each is, how busy, how many people use it right now. The aim is to catch a
stone under load early, so it can be moved to another server, or the stones of one
database be spread out.

This was first thought of as a feature. It isn't one (`FEATURES.md`): it belongs to a
stone, not to a room, it is for System Administrators only, and it needs no
declaration, folder or entries. A stone answers one request about itself, its
**status**, and Rune and Tolk show the statuses of several stones together.

## Words

- **Status:** what a stone tells about itself. It has two parts (the user,
  2026-11-15):
  - **Load:** how busy the stone is. Cheap to gather, so it can be asked for
    often, like `btop`.
  - **Inventory:** what the stone holds. Each number is a query in the database,
    so it is gathered seldom.
- **Status key:** a key that reads the status of one stone, and nothing else.
- German: "Status", "Last", "Bestand", "Statusschlüssel".

## Goals

- One request gives the status of a stone, to a System Administrator or to the
  holder of a status key.
- Asking is cheap however often it is done and however many ask.
- A live view at steps of seconds, and a look back over a day, without anything
  stored in the database.
- Rune and Tolk show several stones on one screen.
- Only general numbers leave the stone: no names of people, rooms or files, and
  nothing about a single one of them. EDS should stay a little anonymous (the
  user, 2026-11-15).
- Plain Java in the stone: no library, and nothing specific to a database.

## Non-goals

- The load of the database itself. Plain Jakarta EE can't read it. The stone
  times its own work instead, which rises when the database is busy.
- Telling someone when no client is open. A scheduled Tolk can do that later.
- An interface for other monitoring systems. A script can read the status with a
  status key, which covers the simple cases.
- Several copies of one stone serving the same data. Each stone has its own data
  here; two stones may run on one host, or use one database server.
- Moving a stone, or balancing between stones.

## What existed before M1 (2026-11-15)

- The version request, without authentication: version, kind of database, whether
  it is persistent, and the storage ceiling.
- Health for the container, on the management port.
- The storage used, summed up by a query whenever content is written and a ceiling
  is set. It isn't given to Members.
- A request named `inventory`, which lists the metadata of all Data Objects for an
  administrator. It was a customer's wish and was never used; it goes (see "The old
  inventory request").
- A setting for one foreign address that may call the stone from a browser. (Found
  with M1: nothing applied it. The filter for it was a servlet filter from before
  Quarkus, which the stone never registered.)
- Tolk names each stone, but only in its own configuration. The stone has no name
  of its own.
- Nothing reads the JDK's management interface.

## Backend: what the stone offers

### The old inventory request

`POST /inventory` and what belongs to it (its request and response, the manager,
the permission `INVENTORY`, its tests and its FitNesse pages) are removed in M1
(the user, 2026-11-15). Neither Rune nor Tolk uses it. A list of every stored
object says more about a stone's people than an administrator needs; the status
gives general numbers instead, and the word "inventory" is free for its part.

### The stone's name

A new setting, set by the System Administrator: a name for the stone, which may be
a technical one. The status reports it, so every overview shows the same name for
the same stone. The version request, which needs no authentication, doesn't: a
stone shouldn't tell its name to everyone who asks. The address stays what a client connects
to; two stones may share a host and differ in the port.

### Who may ask

- **A System Administrator** of the stone, with their session, for their own stone.
- **The holder of a status key.** The System Administrator creates keys, each with
  a label, sees a key once, lists the labels and withdraws a key. A key is 256
  random bits; the stone keeps only its SHA-512 hash.

Why a key and not a login: a login would take one of the account's sessions
(`eds.session.max.per.member`) and need the passphrase in the overview. (When this
was planned, a stone kept one session per account, so a login would have ended the
administrator's own.) A key that leaks gives away numbers, nothing else.

Why a plain hash and not the MasterKey's, as for invitations: the status should
answer while the stone is asleep, and say so, and a key should survive a change of
the MasterKey.

### Load

Gathered at an interval the System Administrator sets, in seconds; zero switches
it off. Everything is read from memory or from the JDK, so a step of one second is
affordable.

| Number | From |
|---|---|
| Requests, and those that failed | counted by the stone |
| Data in and out | counted by the stone |
| Time a request takes: average and longest | measured by the stone |
| Time a trivial query takes | measured by the stone, at most every 10 seconds |
| Accounts with a request in the step | counted by the stone, without keeping who |
| Open socket connections | the socket's own list |
| The stone's share of the processors | the processor time of its own process |
| Load of the host, and its processors | the JDK |
| Memory used and available to the stone, threads | the JDK |
| Time since start; awake or asleep | the stone |

A stone reports the host's load and its own share side by side, since two stones
on one host would report the same host load.

### Inventory

Gathered at an interval of its own, in minutes, with an hour as the default (the
user: a day would be too slow where the limits are reached quickly). The status
says how old the inventory is.

| Number | From |
|---|---|
| Storage used, and the ceiling | the sum of the Circles' totals (`eds_circle_storage`) |
| Accounts, rooms, files, entries of features | counts |
| Sessions that haven't ended | a count |
| Circles whose Circle Secret is due to be rotated, those being rotated, and the objects they still have to re-encrypt (PLAN.md, Crypto C6; added 2026-11-21) | counts |
| The database: its kind, and a mark for its server | the connection |

Each inventory also compares the Circles' totals with their stored parts, and logs a
WARN for each Circle that differs (`PLAN.md`, "The storage ceiling"; its cost is
ticket 27 in `FINDINGS.md`).

The mark is a hash of the database server's address: two stones on one database
server show the same mark, and the address itself isn't given away.

### History

All in memory, lost at a restart, nothing in the database:

| History | Step | Length |
|---|---|---|
| Recent load | the load's interval | a fixed number of samples |
| A day of load | five minutes, with average and peak | 24 hours |
| Inventory | the inventory's interval | a fixed number of samples |

The inventory's history is what lets a client estimate when a stone reaches its
ceiling.

### The request

- A request never makes the stone gather anything: it answers from what it holds,
  so ten watchers cost what one costs.
- A client names the last sample it has and gets what is newer, so a view that
  refreshes every second moves a few lines each time.
- The status is a list of named numbers. A further number later is an addition
  that older clients pass over, not a change of the contract.
- From a browser on another address the request needs the stone's setting for the
  allowed address; one address is enough for one Rune that watches all stones.
  The filter for it wasn't in use; since M1 it is, and allows the header of a
  status key and all methods of the API.

The exact request, names and units are fixed in M1, with the OpenAPI
documentation.

## Rune

In `RUNE.md`, "The status of stones".

## Tolk

In `TOLK.md`, "The status of stones".

## Order of work

Independent of chambers and features, and small. Decided 2026-11-15: M1 with the
other backend work, M2 and M3 when Rune and Tolk are changed for chambers and
features; the table is in `FEATURES.md`, "Order of work".

## Phases

### Phase M1: the backend

The old inventory request goes. Then the stone's name, status keys, gathering
load and inventory, the histories, the request, the settings, and the OpenAPI
documentation.

To check first: what gathering costs at a step of one second on a busy stone; that
the processor time of the own process is given on the platforms EDS runs on; and
that the filter for foreign addresses lets the status key's header through.

Done when: `mvn clean install` passes with the completeness test; tests cover a
status key (read, withdrawn, wrong, and that it reads nothing else), the status
while asleep, both histories, "newer than", gathering switched off, and that no
name appears in a status; and a run with many requests shows the cost of gathering
at one second to be negligible.

Done 2026-11-15:
- **The old inventory request is gone**: `POST /inventory`, its request, response,
  manager, service, the permission `INVENTORY`, its tests, its FitNesse fixture and
  page section, and its two queries.
- **Settings**: `eds.stone.name` (up to 64 characters, may be empty),
  `eds.status.load.interval.seconds` (0 to 3600, default 5, 0 for off) and
  `eds.status.inventory.interval.minutes` (1 to 10080, default 60).
- **Status keys**: `eds_status_keys` with a label and the SHA-512 hash of the key;
  `GET` and `POST /status/keys`, `DELETE /status/keys/{label}`, System
  Administrator only. A key is 256 random bits, returned once as Base64
  (URL-safe).
- **The status**: `GET /status`, with a status key or the Session of the System
  Administrator in the `Authorization: Bearer` header; `since` gives only the
  samples of the load after a sequence, `history` adds the day and all kept
  samples of the inventory. Each series is a list of samples with named numbers;
  the names and meanings are in the API documentation (Status).
- **Gathering** (`StatusCollector`): requests are counted by a filter of the REST
  API, active accounts where a request is authenticated (counted and forgotten
  with the step), and a sample of the load reads those counters, the JDK's
  management interface and the processor time of the own process. The scheduler
  calls once a second; `StatusBean.tick()` decides what is due. The trivial query
  to the database runs at most every 10 seconds.
- **Measured** (`StatusCollectorTest`, on the development machine): about 0.1
  microseconds per counted request, and 75 microseconds per sample of the load. At
  a step of one second that is below a hundredth of a percent of one processor.
- **The histories**: 720 samples of the recent load; 288 steps of five minutes for
  the day, with sums, averages and peaks; 720 samples of the inventory. A changed
  load interval starts the recent load anew, as its samples were of another step.
- **A valid status key is remembered for a minute**, so a watcher which asks every
  second costs no query each time; a withdrawn key is refused at once.
- **Asleep**: a status key reads the status of a locked stone, with `awake` false.
  The Session of the System Administrator can't be read without the MasterKey.
- **Calls from another address** (CORS): the filter is a filter of the REST API
  now and registered. `eds.cors.value` holds the allowed origins, several
  separated by commas, `*` for all; the `Authorization` header and all methods of
  the API are allowed, and a browser's preflight is answered. So the open decision
  "several allowed addresses" is settled: yes.
- **The name** is told with the status only; the version request, which needs no
  authentication, doesn't tell it.
- Tests, as of 2026-11-15: `StatusCollectorTest` (8), `StatusTest` (11: the keys, who reads, a
  withdrawn key, the sleeping stone, a key reads nothing else, load and inventory,
  switched off, only numbers, the database mark), `OriginFilterTest` (3), and
  `EdsStatusTest` (2, in the running stone: the scheduler gathers, the requests are
  counted, the key over HTTP, the headers for an allowed origin and a preflight).
  FitNesse `EDS.API` passed without the inventory section: 839 right, 0 wrong.
- Differs from the plan: the day and the inventory's history come only when asked
  for (`history`), and the inventory is gathered when the stone starts and then at
  its interval, so its first sample may be older than a change.
- For M2, done there: Rune's Content Security Policy opened for the stones it
  watches, and Rune's stone settings name the new settings.

### Phase M2: Rune

In `RUNE.md`, "The status of stones".

### Phase M3: Tolk

In `TOLK.md`, "The status of stones".

## Decisions

All 2026-11-15, the user:

- The view of the stones is not a feature: it is per stone and for System
  Administrators only, and planned apart from `FEATURES.md`.
- A System Administrator sets up Rune or Tolk to show several stones.
- A status key instead of the administrator's login.
- History in the stone's memory, with a configurable interval, so the view can be
  used like `btop`, provided gathering is cheap.
- The two parts are called load and inventory. Load is asked for often; inventory
  is gathered seldom, with an hour as the default.
- Each stone has a name of its own, which may be technical, beside its address;
  stones can share a host.
- The database's own load is left out.
- The old `inventory` request is dropped, and the status keeps to general
  information.

## Open decisions

Built as planned, open for revision:
- **Where Rune keeps status keys:** in the browser of the device, so a second device
  needs them again. With chambers, the Member's vault (`E2E.md`) could carry them.
- **Limits for the colours:** fixed in the clients (`stones-window.ts`).
- **The lengths of the histories** (720, 288, 720 samples) and the ranges of the
  two intervals (0–3600 s and 1–10080 minutes).
