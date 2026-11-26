# The EDS plans

This folder holds the **design documents** of EDS — the plans we wrote before and
while building it. They explain *what* each part should do, *why* it was built that
way, and *what has been done*. The code itself refers back to these documents by
name (for example, a comment might say `PLAN.md, Crypto §4`), so if you ever see one
of those names in the code, this is where the document lives.

You don't need to read these to *use* EDS — for that, see `README.md` and
`RUNNING.md` in the project root. Read these to understand how EDS is put together.

## What EDS is, in one breath

EDS (Encrypted Data Share) is a small server that stores data **encrypted** and lets
a group of people share it. People form a **Circle** (think: a shared, locked room);
anything put in a Circle is encrypted so that only the Circle's members can read it.
The server stores the locked boxes but can't open them. On top of that, a group can
turn a room into a **chamber**, where the members' own devices hold the keys and the
server takes no part in the encryption at all.

## The documents, in plain words

Each file is a plan for one part. They were written in this order, and most of what
they describe is already built.

- **`PLAN.md` — the server (the backend).** The heart of it: how data is encrypted
  and stored, how accounts and keys work, how large files are streamed, and the API
  other programs talk to. The biggest document, because it is the foundation
  everything else stands on. *Built.* Its newest part lets a Circle change its key
  over time ("rotating the Circle Secret"), so a member who leaves can't use a key
  they might have kept.

- **`E2E.md` — chambers (end-to-end encryption).** How a room can be upgraded so the
  server never sees the keys at all — only the members' devices do. Covers how a new
  member safely receives the key, and what is and isn't hidden from the server.
  *Built.* Its newest part hides **file and folder names** in a chamber, so the
  server stores only gibberish where the name would be.

- **`FEATURES.md` — what a room can hold besides files.** A chat, a calendar, shared
  locations, plans. The clever bit: the server guards and stores these without
  knowing what they are, so new kinds can be added without changing the server.
  *Built.*

- **`STATUS.md` — watching over stones.** A way for someone who runs several EDS
  servers ("stones") to see, on one screen, how full and how busy each is — without
  being able to read anyone's data. *Built.*

- **`RUNE.md` — Rune, the web app.** The friendly web interface: rooms, chat, files,
  calls, everything a person actually clicks. Runs in a browser, served by the
  server itself. *Built.*

- **`TOLK.md` — Tolk, the command-line tool.** The same power as Rune, but for a
  terminal and for scripts — for people who prefer typing, and for automation.
  *Built.*

- **`SECURITY.md` — can anyone break in?** The adversary's view: we tried to read a
  Circle's data as an outsider, as a thief with a copy of the whole database, and as
  the server's own operator — and couldn't. The first assessment (2026-11-21) found
  one weak spot (names were readable), which is fixed; the second (2026-11-22, white
  and black hat) found more, most of them fixed and recorded there.

- **`FINDINGS.md` — the open tickets.** Bugs, improvements and checks still to do,
  with the decisions taken on them. Temporary: an entry leaves when its work is
  done, and its decision goes into the plan it belongs to.

## How to read the status language

Each plan starts with a **Status** line and is divided into numbered **phases**
(like `C1`…`C6` or `E1`…`E6`). A phase is a step of the work; "done" means it is
built and tested. The phases are a record of how the part was built, not a to-do
list you need to follow.

## Where the rest lives

- **`README.md`** (project root) — what EDS is and how to build it.
- **`RUNNING.md`** (project root) — how to run and operate a stone.
- The code is in the `eds-*` folders; the web app is in `eds-quarkus/src/main/webui`.
