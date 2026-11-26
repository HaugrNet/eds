# EDS plan: security and hardening

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: the confidentiality assessment was done 2026-11-21 and its one finding was
fixed (chamber names, `E2E.md`, committed 2026-11-22). This plan records what EDS
promises about confidentiality, how it was tested, what held, and what is left.
Code comments and the other plans cite it as `SECURITY.md`.

This plan crosses the backend, Rune and Tolk, like `E2E.md`, `FEATURES.md` and
`STATUS.md`. It doesn't replace the crypto design, which is in `PLAN.md`, Crypto,
and `E2E.md`; it is the adversary's view of them.

## The idea

EDS is built so that data stored in a Circle can be read only by its Trustees —
not by other accounts, not by someone who steals the database, and not by the
person who runs the stone. The oldest open question of the project
(issue #33) is whether that holds. In a world where anyone can point an AI at the
code to look for a way in, it is worth checking up front, and worth writing down
exactly what is protected and what is not, so nobody is surprised.

Put plainly: a person should be able to store something in EDS that nobody else can
read, and the honest answer to "nobody" matters.

## Words

- **Confidentiality:** no one learns the *content* of a Data Object who isn't a
  Trustee of its Circle.
- **Metadata:** everything around the content — names, sizes, times, media types,
  and who shares with whom.
- **The three vantages** an attacker can have, tested together:
  - **An unprivileged account:** a real EDS login that is not a Trustee of the
    target Circle, which may have scraped the IDs.
  - **A database thief:** full read of the stored data (a dump, a backup, a stolen
    disk), without the MasterKey.
  - **The operator:** the System Administrator of the stone, driving the live API,
    trying to grant themselves access.
- **Marker data:** harmless unique text stored by a "victim" account, which the
  assessment tries to read from each vantage. Reading it would break the claim.

## The claim

Content stored in a Circle is unreadable to anyone who isn't a Trustee of it,
including the operator and anyone holding a database dump. The keys are layered so
that no single secret is enough:

- A Data Object's content is encrypted with its Circle's secret (`PLAN.md`,
  Crypto §4).
- A Circle Secret is **double-wrapped** for each Trustee: inside to the Member's
  keys (a hybrid of ML-KEM-1024 and P-384), outside with the MasterKey. Neither
  the MasterKey alone nor a Member's keys alone opens it (`PLAN.md`, Crypto §3).
- A Member's private keys are themselves double-wrapped: inside with a key from
  their passphrase (PBKDF2), outside with the MasterKey (`PLAN.md`, Crypto §2).
- The MasterKey is never stored; only a version and a check value are
  (`PLAN.md`, Crypto §1).

So a database dump *and* the MasterKey together still can't reach a Circle Secret
without a Member's passphrase, and the stone — which holds the MasterKey while it
runs — still can't read a Circle it isn't a Trustee of.

## The assessment, 2026-11-21

Done at the user's request, on a local stone on PostgreSQL, holding all three
vantages at once. The method was black-box against the running API, plus a
white-box read of the crypto the API can't reach. A "victim" stored marker data in
a private Circle; the goal was to read it from any vantage it wasn't meant for.

### What held

| Vantage | Tried | Result |
|---|---|---|
| Unprivileged account | fetch by data and circle id, resource GET, content stream, a download token, cross-circle copy, self-add as Trustee, list Trustees, enumerate Circles | all denied (401/404); the Circle isn't even visible (with `eds.show.trustees` off, the default since 2026-11-23; with it on, every Circle and who is in it can be seen, not its data); a real and a random id give the same 404, so no existence oracle |
| Database thief | grep the whole dump for the plaintext; inspect the stored bytes and the keys | the marker is absent; the content is the `01 00 01…` format header plus AES-GCM ciphertext; the keys are layered as above, so the dump yields nothing readable |
| Operator | read the data; add self as Trustee; invite an accomplice in; delete the victim to orphan the Circle; rotate the secret then read | all denied: add-self is refused "without membership", invite needs Circle-admin, the last Trustee can't be removed, and a rotation re-wraps only for existing Trustees |
| Download tokens | use a token across objects, after logout, malformed | bound to one object (403 across), die on logout, clean 403 on garbage — no crash |

The crypto review found the nonce and AAD discipline sound, including the part that
looks most dangerous: the Circle-Secret rotation (`PLAN.md`, Crypto C6) reuses a
content nonce prefix, but under a fresh key, so no (key, nonce) pair repeats. No
hostile request produced a server error.

### The one gap

Metadata was cleartext to the operator and the database thief. The dump plainly
showed Circle names, account names, **file and folder names**, sizes, times, and
the full Trustee graph (who shares with whom). The *content* was safe; the
*names and relationships* were not.

## What was built: hidden names

The sharpest part of the gap — file and folder names — is closed for chambers
(end-to-end-encrypted rooms, `E2E.md`). A client now encrypts the name the same way
it encrypts content, and stores the ciphertext in the name's place; the stone, a
dump and a curious operator see only ciphertext. Built in Rune and committed
2026-11-22 (`E2E.md`, "Names"; `RUNE.md`). The backend widened the name column so it
can hold the ciphertext, and since 2026-11-22 it takes a Name Key, so it can keep
names unique; it never learns the name.

- **Done:** Rune hides file and folder names in chambers, verified in a real
  browser (both members see the real name; the stone's own API returns only
  ciphertext).
- **Not done:** Tolk doesn't hide names yet (a ticket in `FINDINGS.md`).

## What the stone still sees

By design, even for a chamber, the stone (and a dump, and the operator) still see:

- The names of **rooms**, and the media types, sizes and times of files.
- Who is in a room, who wrote when, reactions, receipts and typing.
- Who is connected and to which rooms; and of a call, who calls whom and when,
  never what is said.
- That a room is a chamber.

Hiding these is a non-goal of `E2E.md`. Hiding a *room's* name is possible with the
same technique, but a room name is unique on the stone and shown in lists, so it is
a separate decision, not yet made.

Two limits of what is protected, both noted in `E2E.md`:

- **Rune is served by the stone.** A stone that turns against its users could serve
  a changed Rune. In a browser, E2E protects against a dump and a curious operator,
  not against one who changes the app. Tolk gives the full protection.
- **Content isn't bound to its Data Object.** The stone could present one file of a
  room in place of another of the same room; it can't read or change either.

## Deferred, at the user's steer (2026-11-21)

- **Argon2 for passwords** (issue #85): skipped for now. The `EntropyVerifier`
  already refuses weak passphrases (at least 15 symbols, an estimated 128 bits),
  which was judged enough for the moment. PBKDF2 stays (`PLAN.md`, Crypto §2a).
- **Authentication checks with limitations** (issue #68): built 2026-11-22, after
  all. After `eds.auth.max.failed.attempts` attempts at the passphrase of an
  account within `eds.auth.failed.window.minutes`, it is locked for
  `eds.auth.lockout.minutes`; off (0) by default. Each attempt is counted before
  the passphrase is checked, in a transaction of its own, so no more are tried than
  the limit allows, however many come at the same moment; an accepted passphrase
  clears the count, and so does a new one (as when an invitation is accepted).
  Every endpoint which checks a passphrase counts, the MasterKey request included;
  nothing else counts (Sessions, invitation tokens, a request which isn't valid).
  A locked account gets the answer of a wrong passphrase, after the same work, so
  neither the answer nor its time tells the lock. A new value of a setting is
  checked when it is set; a stored one which isn't a number gives the default, and
  one out of range the nearest limit. The findings of its two reviews (2026-11-22)
  are fixed, each with a test which fails without the fix. What the design accepts:
  - an attempt counts from its start until it is cleared, a right one too, so while
    as many attempts as the limit are in progress, a further one is refused, even
    with the right passphrase; an accepted passphrase clears the whole count, also
    wrong attempts made at the same moment;
  - the account is read, and with the lock on the attempt counted, each in a
    transaction of its own, before the request's own transaction reads anything
    (ticket 28). So the request holds no second connection meanwhile. There are two
    exceptions: a data request (`ShareBean.processData` reads its Circles first) and
    the MasterKey request (it finds the System Administrator first). Those can still
    wait for each other at as many at once as the pool has connections, and fail,
    closed, with an error. PBKDF2 makes such a load cost the CPU first;
  - as with any lock, someone who knows a name can keep its account locked; it heals
    itself after its time, and a stone which can't accept that leaves the feature
    off.

## The cost of a passphrase, and unknown names

Decided and built 2026-11-24 (tickets 20 and 21 of the assessment below).

**The cost** (ticket 20). A passphrase is checked by deriving its key with PBKDF2,
about a fifth of a CPU-second, whenever the named account exists: right or wrong,
locked or not. So one known name could keep every processor busy (measured: 500
wrong logins, 30 at once, took 185 CPU-seconds on 16 cores).

- `PassphraseLimit` (eds-core, `jce`) bounds how many keys are derived at once:
  `eds.auth.concurrent.passphrases`, 0 by default for half the processors (at
  least one), at most 1024.
- A request waits at most `eds.auth.passphrase.wait.millis` (2000 by default, at
  most 60000) for a place. Then it is answered with the new Return Code
  `SERVICE_BUSY`, HTTP 503, "The stone is busy checking other passphrases, try
  again shortly."
- Every derivation takes a place (`Crypto.generatePasswordKey`), so the limit holds
  for every endpoint with a passphrase, the MasterKey request and new passphrases
  included.
- A request with a passphrase takes its place before it looks the account up, and
  keeps it until the passphrase is checked (`Crypto.passphrasePlace`, in
  `AbstractManager`); the derivation within takes no second one. So a waiting
  request holds no database connection yet (except the MasterKey request and a data
  request, which read before they take the place), and a busy answer is no attempt for the
  lock of #68. The review of 2026-11-24 found both: waiting requests holding the 20
  connections of the pool, and a user locked out by the retries of a busy moment.
- The busy answers aren't logged one by one, as the sender decides how many there
  are: the first is logged as WARN, then at most one WARN a minute, with how many
  were answered so since the one before. `CommonService` leaves `SERVICE_BUSY` out
  of its WARN for each error.

The user chose this over a limit for each client address, which needs the proxy's
headers to be trusted and doesn't bound the CPU.

**Unknown names** (ticket 21). An unknown name was refused before PBKDF2, "Could not
uniquely identify an account", so both the answer and the time (7 ms against 212 ms)
told whether a name exists. Now it gets the answer of a wrong passphrase, "Cannot
authenticate the Account from the given Credentials.", after the same work: a key
is derived from the passphrase given, with the current algorithm and iterations and
a fixed salt, and thrown away. It takes a place of the limit as any other. Once the
stone has an account, an unknown name no longer takes the lock of the first account
(the row of the version), so unknown names aren't checked one at a time.

What stays:
- A request which names a Circle still tells a known name from an unknown one: a
  known name which isn't a Trustee of the Circle is answered "No Trustee information
  found" at once, before the passphrase; an unknown one as a wrong passphrase after
  the derivation. It is the item "Who is in which Circle" under Open (the review of
  2026-11-24; ticket 26).
- The times still differ a little: a known account derives with its own iterations,
  an unknown name with the current setting; a known one then opens its keys and,
  with the lock of #68 on, counts the attempt in a transaction of its own; then it
  reads its account again in the request's transaction (ticket 28).
- Under such a load, real users get the busy answer as well; the stone stays
  responsive for everything else, and requests with a Session (no PBKDF2) aren't
  held.
- The WARN of the busy answers is logged when one comes; a count which ends within a
  minute after the last WARN is told by the next one, whenever that comes.
- Which Circle a name is a Trustee of is still told without a passphrase (Open,
  below).

## The assessment, 2026-11-22

At the user's request, after the 2-hour load test: a white-box read of the stone,
Rune and Tolk by a reviewer which didn't write them, and a black-box attack on a
local stone (H2). Each finding became a ticket; the numbers are those of
`FINDINGS.md` (1–3 are the lock orders in `PLAN.md`, Crypto C6), which also holds
the user's decision on each.

Decided (the user, 2026-11-22): 6 is a blocker, fixed first; 7, 8, 5, 9, 14 and the
default of 10 are to be fixed; 20 and 21 go with #68; 12 is Tolk's, for when Tolk gets
its focus; the rest are flagged.

Fixed:
- 5 and 6, 2026-11-23: "The MasterKey source".
- 7, 8, 9, 10 and 14, 2026-11-23: "Sessions, copies, folders, visibility and
  settings".
- 11, 2026-11-23, and 25 (an HTTP/2 body without a length), 2026-11-24: "The size
  of a request".
- 20 and 21, 2026-11-24, as a follow-up of #68: "The cost of a passphrase, and
  unknown names".

Fixed since: 18 and 22, 2026-11-26: "The socket before and after AUTH"; 15 and 13,
2026-11-26: "Data Types, and the rotation of the MasterKey".

Open: 12 (Tolk's), and 17 and 19 (flagged); 16, kept for the
record; 26, found by the review of 20 and 21; and 33, found by the review of this
file (2026-11-25). All are in `FINDINGS.md`.

- **High:** the MasterKey request fetched the URL it was given *before* the
  passphrase was checked, by any scheme (`file:` too), without timeouts or a size
  limit, inside the transaction; only the first administrator's name was needed.
  Request forgery, probing local files, a stone out of memory or out of
  connections (6). The suppressed finding of find-sec-bugs said the URL is the
  System Administrator's; it is the request's. Fixed.
- **Medium:**
  - a Session could make new Sessions, and rename, invalidate or delete its
    account, or replace its public key, without the passphrase (7; fixed);
  - a READ Trustee could delete a Data Object without content by moving it (8;
    fixed);
  - at the then default `eds.show.trustees=true`, every account saw every Circle of
    any member, so the table above ("the Circle isn't even visible") holds only
    with it off (10; the default is now off);
  - bodies of up to 1100 MB on every endpoint, parsed before authentication (11;
    confirmed and fixed; 25, an HTTP/2 body without a length, fixed);
  - Tolk prints remote text with its escape sequences (12, not tried);
  - one known name let anybody spend 0.37 CPU-seconds of the stone per request on
    PBKDF2; 500 requests pinned 16 cores; a locked account cost the same, so #68
    didn't help (20, measured; fixed).
- **Low:**
  - a parent folder could be of another Circle (9; fixed);
  - the MasterKey rotation installs the key before its commit (13);
  - administrators could break the stone by `eds.system.charset` or `eds.is.ready`
    (14; fixed);
  - Data Types are stone-wide and any WRITE Trustee changes them (15);
  - internal exception texts reach the client (16);
  - pending accounts can be made without a limit (17);
  - socket details (18, 22);
  - the secret of a MasterKey URL could reach the log (5; fixed);
  - which names exist showed in the response time too: 6.7 ms against 212 ms (21;
    fixed).
- **Info:** HSTS, plain HTTP beside TLS, the management port on all interfaces,
  `/version` without a session, Tolk following redirects across hosts, passphrases
  not normalised (19).
- **Held** (black box): authorization by ID and by trust level, the boundaries of
  administrators, sessions and tokens, invitations, the claim of a new stone (15 at
  once, one won), the socket's checks, malformed and deep input, Range,
  `Content-Disposition`, SQL, path traversal, and how content is served. No request
  made the stone answer with an error. The #68 lock leaks by neither message nor
  time (212, 220 and 222 ms).

### The black-hat checks that held, in detail

**Unauthenticated surface**
- The management port shows only health counts.
- Rune's CSP and headers are strict.
- CORS answers only the configured origin.
- 15 parallel claims of the stone made exactly one System Administrator.

**Authorization**
- IDOR on data, circles, trustees, tokens and copy was refused, with no ID oracle.
- Trust levels and the admin vs System Administrator boundaries held.

**Sessions and tokens**
- A session ends at logout and at a passphrase change.
- Status keys, invitations (single use, HMAC, bound to the account) and download
  tokens held.

**Socket**
- A foreign JOIN is refused.
- An oversized frame closes the connection (1009).

**Input and content**
- Malformed or deep JSON, Range/If-Range, Content-Disposition injection, SQL
  injection and path traversal: none worked.
- Downloads are served as attachments with nosniff and a sandbox CSP.
- 0 ERROR lines in the stone's log.

**Not tested dynamically:** ticket 11 (large JSON bodies), since confirmed and fixed,
also for HTTP/2 (SECURITY.md, "The size of a request"). Ticket 6 (the MasterKey URL
fetch) wasn't either; it is fixed (SECURITY.md, "The MasterKey source").

## Sessions, copies, folders, visibility and settings

Decided by the user 2026-11-22 and built 2026-11-23 (tickets 7, 8, 9, 10 and 14 of
the assessment above).

**Sessions (7).** A Session lets a client act for a Member without the passphrase,
and can be stolen. So what would keep the account from its owner, or outlast the
Session, needs the passphrase (`ProcessMemberManager`):
- a login, so a Session can't make another one with a new expiry, and a stolen one
  ends when it expires;
- a new Account Name;
- invalidating or deleting one's own account;
- a new passphrase, as before;
- replacing or removing the public key or the vault (`E2E.md`), once the account
  has a vault.

While the account has no vault, a public key and a vault may be set with a Session:
Rune and Tolk make them after a login, when the account has no vault, which is what
both take for having no keys, and Rune doesn't keep the passphrase. Neither client
logs in with a Session, nor renames, invalidates or deletes its own account, so
neither changes.

What stays: a stolen Session can do everything else until it expires
(`eds.session.timeout.minutes`, 480 by default): read and write in the Member's
Circles, and an administrator's Session the administrator's work. On an account
without a vault, it can set the first keys; the owner's client then finds keys it
didn't make, and replacing them needs the passphrase (Rune has no form for it yet,
a ticket in `FINDINGS.md`).

**Copy and move (8).** Both need WRITE or ADMIN in the source Circle, checked first,
whether the Data Object has content or not. Before, the check sat with the content,
so a READ Trustee could move an object without content out of a Circle, which
deleted it there.

**Parent folders (9).** ADD, COPY and MOVE refuse a folder of another Circle as the
parent, as UPDATE did ("The folder belongs to another Circle.", `ILLEGAL_ACTION`).

**Visibility (10).** `eds.show.trustees` is off by default: of another Member, a
Member sees only the Circles both share. On, any Member sees every Circle of any
other Member, and as any Member can list all Members, every Circle of the stone and
who is in it, though not its data; the setting's description, the API documentation
and Rune say so, and Rune asks before it is changed. Administrators see all Circles
of a Member either way. EDS is made for small groups where everybody knows
everybody, so it stays a choice; a finer model is left open.

**Settings (14).** Text becomes bytes as UTF-8, always: `eds.system.charset` is no
longer a setting, as another character set made every stored key unreadable and
nobody could log in to set it back. A value stored under the old key is a custom
setting, which EDS doesn't read. `eds.is.ready` is set by EDS only, and refused
through the API, as `eds.masterkey.url` is. A setting's key is trimmed before it is
checked, as it is stored trimmed; the review of 2026-11-23 found that a blank around
the key passed all checks of the standard settings, these two included. What stays:
every administrator, not only the System Administrator, may change the other
settings; the algorithms and iterations count for new keys only.

## The size of a request

Confirmed and built 2026-11-23 (ticket 11 of the assessment above); a body without
a length, 2026-11-24 (ticket 25). The HTTP layer has one body limit for all routes,
`quarkus.http.limits.max-body-size`, which the upload of content needs to be 1100M.
So every request could carry as much, and JSON is parsed before anybody is
authenticated: on a stone with a heap of 512 MB, one anonymous body of 300 MB was
parsed and answered, and four at once took the stone down (`OutOfMemoryError`),
also while it was locked.

**The limit** (`BodyCounter.limit`): 16 MiB, or the Base64 of
`eds.data.max.inline.size` plus 1 MiB if that is more. It holds for every request
but `PUT …/content` and `PUT …/thumbnail`, which are read as a stream and keep
their own limits (`eds.stream.max.object.size`, and 256 KiB for a thumbnail).

**From the headers** (`BodyLimit`, eds-quarkus, a filter in front of all routes),
before any of the body is read:
- a `Content-Length` over the limit (or not a number) is answered with 413
  (`CONTENT_TOO_LARGE`), and the connection is closed (in HTTP/2 only the stream
  is reset);
- a body with a `Transfer-Encoding` (chunked, HTTP/1.1) is answered with 411, as it
  can't be measured first. Rune, Tolk and the generated clients send the length of
  a body;
- the uploads are told by the normalized path, the one requests are routed by; the
  review of 2026-11-23 found `PUT /data/./content` reaching the JSON
  `PUT /data/{dataId}` as an upload.

**While it is read** (`BodyCounter`, eds-core, a reader interceptor; ticket 25). In
HTTP/2, which the stone speaks on both ports, a body needs no length, and a request
without a body has none either: the generated Java client sends POST and PUT
without a body so, and FitNesse failed when they were refused. The headers can't
tell the two apart, and a body read by the filter can't be handed on to RESTEasy (it
answered 500). So `BodyLimit` lets a request without a length pass, and
`BodyCounter` counts every body RESTEasy reads into an object: past the limit, the
reading stops and the request is answered with 413. RESTEasy holds only a small
buffer of the body before it hands the rest on as a stream, so at most the limit is
held. The user chose this over switching HTTP/2 off. Measured on a stone with a heap
of 512 MB: four HTTP/2 bodies of 300 MB without a length at once took it down
before (`OutOfMemoryError`); with the counter, each got 413 and the stone stayed up.
The counter runs under WildFly too, as it is listed in `EDSApplication`.

What stays:
- After the 413 the stone still receives the rest of such a body, and throws it
  away: a client may keep sending it, costing bandwidth, not memory. Resetting the
  HTTP/2 stream after the answer didn't stop curl (tried 2026-11-24).
- Many requests just under the limit at once still cost memory, as any JSON does;
  the worker pool bounds how many are parsed at a time.
- The socket's frames have limits of their own ("The socket before and after AUTH").

## The socket before and after AUTH

Built 2026-11-26 (tickets 18 and 22 of the assessment above, decided by the user the
same day). The socket (`SocketHub`, FEATURES.md) authenticates with its first frame,
as a browser can't send an Authorization header on a WebSocket, so anyone who
reaches the stone could open connections and hold them until their AUTH.

**Before AUTH:**
- At most 256 connections wait for their AUTH at once
  (`MAX_WAITING_FOR_AUTH`). One more is closed at once (1013, try again later)
  and never kept.
- The first frame must be a valid AUTH within 10 seconds, not 30 as before.
  Anything else closes the connection: another frame, a frame which isn't one, a
  token which isn't Base64 or names no Session. Before, only a well-formed frame of
  another type did; the others were answered and the connection could try again
  (ticket 22). The answer names the frame it answers (`ref`), so Rune and Tolk
  still learn why.
- The close code tells whose failure it was: 1008 for the client's (Rune and Tolk
  take it for an ended Session), 1013 for the stone's, such as a locked stone or
  a database which can't be read, so Rune reconnects later instead of logging out.
- Frames which already wait behind a failed first frame aren't handled. Without
  that, one connection could send hundreds of AUTH frames at once, and each would
  cost a lookup.
- At most 64 KiB wait to be handled before AUTH, not 4 MiB.
- Nothing of it is logged above DEBUG, not even a connection over its limits.
- There is no cap for each address: the WebSocket API doesn't give it, and behind
  a reverse proxy every connection comes from the proxy's (the user, 2026-11-26).

**Access ends before anything more is sent.** A change of the access is committed,
and only then is the socket told (`AccessChange`, after the commit). Its connections
used to be checked off the request's thread, and until then an entry of the Circle
could still reach a Member who was removed. Now:
- Each Circle has an access version in the stone's memory, and so has "everyone"
  (a Member, a Session, the MasterKey). It is raised as soon as the socket is told,
  on the thread of the change.
- A connection remembers the versions its access to a Circle was checked against
  when it joined and at each check, read before the check.
- Before anything of a Circle is sent, a connection checked against an older
  version is checked first. What it is told meanwhile waits behind the check, in
  its order. If the check fails (the database can't be read), it isn't sent: the
  next announcement checks again.
- Only an access change keeps a version of a Circle; joining doesn't, so a client
  can't fill the stone's memory with the IDs it tries to join.
- `AccessChange` names the Circle of a change of Trustees or of a Circle, so only
  the connections which joined it are checked. Before, every change checked every
  connection (ticket 28). A new Circle, a new account and an invitation end no
  access and aren't told.
- No change to the database.

**After AUTH:**
- **Signals:** a budget for each connection, 256 KiB at once, coming back at 32 KiB
  a second; a signal over it is dropped silently, as one sooner than the feature's
  interval is, and doesn't use up the interval. One Member could fill the others' backlog with signals of 32 KiB
  before. It isn't a floor for the interval: `calls` declares none (`"signals": 0`),
  as setting up a call sends a burst of small signals, which would be dropped.
- **Changes of entries:** a budget for each connection of 100 at once, coming back
  at 20 a second; one over it is answered with `SERVICE_BUSY`, which isn't logged as
  WARN.

Tested in `SocketHubTest` and `ManagementBeanCircleTest`, each part by a test
which fails without it (checked by switching each part off):
- the cap before AUTH, and the deadline;
- a large frame before AUTH, without a WARN;
- the connection closed at once on a bad first frame, with its `ref`, and with
  1013 when the stone failed;
- the frames behind a failed AUTH not handled;
- nothing of a Circle sent before the check of a removed Member, nor when the check
  fails;
- what is held sent in order;
- a change of one Circle checking only its connections, and each change naming
  what it may end;
- no versions kept for joins;
- both budgets, and a signal over the budget leaving the interval.

Reviewed by a reviewer which didn't write it; its findings are fixed, but for what
stays below.

What stays:
- **A small gap.** An entry committed between the commit of an access change and
  the moment the socket is told can still be sent. They run back to back on one
  thread, so the gap is short. Closing it would need a version in the database,
  read at every send.
- **One instance only:** the versions live in the memory of one stone, as the
  socket does (FEATURES.md, "Open decisions").
- **The budgets hold for the socket only.** The resource API has no rate limit of
  its own.
- **The total cap is for everyone.** Many connections from one address can still
  take all 256 places for up to 10 seconds each, and so delay others' AUTH.
- **A join in flight.** A JOIN whose check passed just before an access change is
  checked at the next announcement, not at once. It gets nothing before that, but
  is shown as present until then.
- **The order of an answer.** When the connection which asked must be checked
  again, its answer (DONE) may come before the announcement of its own change.
  Rune and Tolk match answers by `ref`.
- **A change of a Member** (a login, a logout, a new passphrase, a deletion) still
  checks every connection, as does the MasterKey (ticket 28).

## Data Types, and the rotation of the MasterKey

Built 2026-11-26 (tickets 15 and 13 of the assessment above; the user asked for the
five worst findings to be fixed, and the recommended options were taken).

**Data Types** (ticket 15). They are of the whole stone, but any Member with WRITE in
a Circle could add, change or delete them, and anyone can make a Circle. So any
account could change the types other Circles' clients rely on, fill the table, or
delete `data` or `folder` while they were unused, after which adding without a type
failed for everyone.
- Only administrators may add, change or delete a Data Type
  (`Permission.PROCESS_DATA_TYPE`, `SYSOP`), as with the settings. Rune and Tolk
  don't change Data Types.
- The default types (`data`, `folder`, `thumbnail`, `entry`) can be deleted no more
  than they can be changed (also O4 finding 4).
- Tested: `ShareBeanDataTypeTest` (a Circle's administrator refused, an
  administrator not; the defaults kept) and FitNesse; each fails without its part.
- What stays: Data Types aren't per Circle.

**The rotation of the MasterKey** (ticket 13). It wraps every Member's keys and every
Trustee's Circle Secret anew, in one transaction.
- **The new MasterKey is installed when the transaction has committed**, not before
  (`MasterKeyManager.installAfterCommit`, a synchronization of the transaction).
  Before, it was installed while the transaction was still open: requests in that
  moment failed, and their logins counted toward the lock of #68. If the transaction
  rolled back, the stone held the new key over the old data until a restart. Now a
  rollback leaves the old MasterKey in place, and is logged as WARN.
- **The Members are locked, in the order of their IDs, before they are read**
  (`CommonDao.lockMembers`). Members have no version column, so a new passphrase
  which held its Member's row while the rotation ran was overwritten with the old
  keys, wrapped anew, and the account then opened with neither passphrase. The
  rotation now waits for it, and wraps the new keys anew.
- **The Trustees are read after the Circles are locked**, so one added while the
  rotation waited for its Circle is wrapped anew too.
- Tested: `ManagementBeanMasterKeyTest` (the key installed on a commit, kept on a
  rollback) and `MasterKeyRotationLockTest` (a change of a Member, committed while
  the rotation waits for it, is kept); each fails without its part.
- What stays (found by the review of this fix, 2026-11-26; ticket 53 in
  `FINDINGS.md`):
  - A request which sealed with the old MasterKey before the rotation began, and
    writes after it, still writes its rows sealed with the old key: a new
    passphrase whose update waits for the rotation's lock of its Member, a new
    account, an invitation, a Session. It was so before. Closing it needs every
    writer which seals with the MasterKey to hold one lock the rotation takes
    exclusively.
  - Between the commit and the installation, the old key is in memory for a
    moment, and a request then may fail once.
  - The order of the locks against the deletion of a Member and the invitees: done
    with ticket 1 (PLAN.md, Crypto C6, "One lock order for the whole stone").

## The MasterKey source

Decided and built 2026-11-23 (tickets 6 and 5 of the assessment above).

**The order** (`MasterKeyManager`). While EDS is unlocked, the System
Administrator's passphrase is checked before anything is read from the request's
URL: without it, nothing is fetched, and a wrong passphrase counts towards the #68
lock as any other. While EDS is locked, the passphrase can't be checked first,
because the accounts' keys are wrapped with the MasterKey; the source is read
before anyone is authenticated, so the reading itself is held tight.

**The source** (`MasterKey.readMasterKeySecretFromUrl`, also at startup):
- Only `file:` without a host, or `https:`.
  - A `file:` source must be a regular file, so not `/dev/zero`, a pipe or a
    directory. Symbolic links are followed.
  - A host in a `file:` URL is refused, as on Windows it would be a share of that
    host.
  - `https:` must answer 200 without a redirect.
  - `http:`, `jar:`, `ftp:` and the rest are refused before any connection.
- At most 16 KiB.
- 10 seconds for it all, the TLS handshake and the headers included, in a virtual
  thread of its own, which is interrupted at the deadline; within it, a connect and
  a read timeout of 5 s each.
- At most two sources are read at the same time; a third is refused at once with
  `NETWORK_ERROR` ("other sources are being read"), also while locked. So slow
  sources can't hold the stone's database connections, which a request holds while
  it waits.

**The answer and the log** (ticket 5):
- A failure gives a fixed message that names only its kind, for example "not a
  regular file", "HTTP 403" or "SocketTimeoutException". It never gives the URL.
- The caught exception is deliberately neither chained nor logged, because its
  message repeats the URL, query included. This is the user's decision, an
  exception to the usual good practice; SonarQube's `java:S1166` is suppressed for
  it with that reason.
- The validation of the request's URL answers "The URL field is invalid.", without
  the exception's text, for the same reason.
- While EDS is locked, a source that can't be read is answered as a wrong secret
  ("Invalid credentials."), so the answer tells nothing about files or hosts; the
  log has a WARN with the kind, without the URL. Only the refusal of a third source
  is passed on as it is.

**What stays** (accepted for now, to discuss):
- While locked, whoever knows the System Administrator's name can still make the
  stone try a `file:` path or an `https:` host. Nothing comes back but the time it
  took: a refused port answers at once, a silent one after 5 s, so blind probing of
  ports stays possible, at two at a time.
- While locked, each such request logs a WARN, so the log can be filled. It is only
  possible while locked, and only with the name.
- DNS lookups have no timeout of their own. The caller still stops at the deadline,
  but the lookup takes its permit with it until it ends.
- A file read can't be interrupted. A file swapped for a pipe between the check and
  the open, or a hung network mount (NFS, SMB), blocks its thread for good. The
  caller stops at the deadline, but the slot is lost until a restart. After two such
  reads, every URL source is refused until then; a secret sent inline still works.
  The swap needs local write access.
- While locked, whoever knows the name can keep both slots busy (two requests to a
  silent host every 5 s). The System Administrator's unlock by URL is then refused
  at once, with "other sources are being read" (NETWORK_ERROR). That refusal depends
  on no URL, so it is passed on, not answered as a wrong secret. A secret sent
  inline still unlocks.
- Upgrading: a configured source that is now refused (`http:`, a redirect, more than
  16 KiB, `file://host/…`) leaves the stone locked, and the log says why.

## Open

- Who is in which Circle can be found out without a passphrase: a request which
  names a Circle looks the account up as a Trustee of it before the passphrase is
  checked, and answers "No Trustee information found" for a name which isn't one.
  Older than #68, found by its tests (2026-11-22); to decide. It also tells whether
  a name exists, as fast as before ticket 21 (ticket 26).
- An administrator can't end the lock of another account (#68); it ends by itself
  after `eds.auth.lockout.minutes`. To decide whether one should.
- Decide whether to hide room names, and if so how the uniqueness check adapts.
- Work on the clients (Tolk hiding chamber names, Rune and Tolk sending the Name
  Key) and the further studies (an active operator, the rotation's concurrency) are
  tickets in `FINDINGS.md`.
