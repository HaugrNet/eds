# EDS: findings to fix

Status: temporary (since 2026-11-22), reorganised on 2026-11-26. It holds the open
tickets: bugs, improvements and checks still to do, found by the load tests, the
reviews and the security assessments, each with the user's decision on it.

The rule (the user, 2026-11-22): plans hold the design and its decisions; bugs and
improvements are tickets, here. When the work on a ticket is done, it leaves this
file, and its decision goes into the plan it belongs to (PLAN.md, SECURITY.md,
RUNE.md, TOLK.md, ...). When nothing is left, this file goes.

Every fix follows the Definition of done in PLAN.md, with a test which fails without
it. Numbers aren't reused; gaps are tickets which are done.

## Overview

**The backend: bugs**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 3 | A copy or move holds the source row for the whole copy | bug (concurrency, performance) | partly done (2026-11-26); the rest open |
| 4 | CONSTRAINT_ERROR answers 500 for a name that is taken | bug (API) | discuss |
| 39 | Behaviour left from the review of the OpenAPI (O4) | low | — |

**The backend: security**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 16 | Internal exception messages are returned to clients | low | kept for the record |
| 17 | Pending accounts without a cap (name squatting) | low | analyse further |
| 19 | HTTP and client hardening | info | put off (2026-11-26); proxy speaks plain HTTP to the stone |
| 26 | A request which names a Circle tells whether an account name exists | low | — |
| 33 | The MasterKey request tells the System Administrator's name before the passphrase | low | — |
| 52 | Studies of integrity and availability | info | — |
| 53 | A request which sealed with the old MasterKey can write after its rotation | low | — |

**The backend: performance**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 27 | The inventory sums every part of the stone | info | D and B; next in line |
| 28 | What is left of the throughput under load | info | steps 1, 2, 4 done; a key cache last; the rest open |
| 38 | Counting a room's entries, and a rotating Circle's content, reads its whole history | info (low priority) | load test done; the rest open |

**The backend: improvements**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 40 | A Member can't list their sessions or log out everywhere | info | — |

**Tests, operations and process**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 31 | Tolk's tests aren't part of the verification here | info | — |
| 32 | PostgreSQL checkpoints too often under load | info | — |
| 34 | Stale comments in the code; the largest object has no upper bound | info | — |
| 41 | Tests set private constructors accessible | info | don't set the flag |
| 42 | The rotation of tens of GiB isn't tested | info | — |

**Rune and Tolk together**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 43 | Chambers: Tolk shows encrypted names, and no client sends the Name Key | low | — |
| 44 | Rune and Tolk don't show a rotation of the Circle Secret | info | — |

**Rune**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 45 | Rune: bugs | low | — |
| 46 | Rune: improvements | info | — |
| 47 | Rune: what isn't tried in a browser | info | — |

**Tolk**

| # | Ticket | Severity | Decision |
|---|---|---|---|
| 12 | Tolk prints remote text without stripping terminal escape sequences | medium/low (suspected) | Tolk's, when it gets its focus |
| 30 | Tolk's copy of Rune's `vectors.json` lacks the vector of names | bug (test) | — |
| 48 | Tolk: bugs | low | — |
| 49 | Tolk: local hardening | low | — |
| 50 | Tolk: improvements | info | — |
| 51 | Tolk: what isn't tried on a real screen | info | — |

Design questions which wait for the user are in the plans, not here:
- `SECURITY.md`, "Open" (who is in which Circle, with 26; ending another account's
  lock; hiding room names), and the "What stays" of the MasterKey source (5 and 6)
  and of 20 and 25;
- `RUNE.md`, "Open"; `TOLK.md`, "Open decisions"; `FEATURES.md`, `E2E.md` and
  `STATUS.md`, "Open decisions";
- `PLAN.md`, OpenAPI, "Open" (401 and 403 in `ReturnCode`).

---

# The backend: bugs

# Ticket 3 — A copy or move holds the source row for the whole copy

**Labels:** bug, backend, concurrency, performance · **Severity:** bug (concurrency, performance)

**Decision:** (2026-11-22) flagged for follow-up discussion. (2026-11-26) Partly done
with tickets 1 and 2 (PLAN.md, Crypto C6, "One lock order for the whole stone"): a
copy whose source was replaced meanwhile copies the new content. The rest: none yet.

## Summary
A copy or move to another Circle locks the source's content row
(`DataDao.lockData`, `SELECT … FOR UPDATE`) for the whole copy, and reads the key ID
from it, so the rotation can't replace the content and its secret between the two
reads. What is left of its consequences:
1. **Copies of the same object run one at a time.** `FOR SHARE` would let them run
   side by side, but H2 doesn't have it.
2. **A Circle delete waits for a large copy.** It locks all content rows of the
   Circle, so it can wait for a streamed copy of a large object, and fails if the
   copy runs past the delete's transaction timeout.
3. ~~A copy which waited for a rotation could fail to unwrap the new secret~~: done
   2026-11-26, the Trustee is read again once the row is held. No test can make the
   database hand the row to the rotation before the copy, so it is tested only on
   the common path.

## Where
`ProcessDataManager.copyDataToTargetCircle` and `processMoveData`;
`CommonDao.deleteContentOfCircle` for (2).

## Expected
A decision on 1 and 2.

---

# Ticket 4 — CONSTRAINT_ERROR answers 500 for a name that is taken

**Labels:** bug, backend, api · **Severity:** bug (API) · **Source:** the review of the log changes

**Decision:** (2026-11-22) flagged for follow-up discussion.

## Summary
`ReturnCode.CONSTRAINT_ERROR` is `CLASS_ERROR` with code 95. `getHttpCode` maps any
code above 90 (`ERROR_CODE_NUMBER`) to the classification's code, so the resource
endpoints (`POST`/`PUT /members…`, through `CommonService.runResourceRequest`)
answer **500**; the others answer 200 with code 95. It is the requester's fault,
not the stone's:
- a member renames themselves to a name that is taken (`ProcessMemberManager` ~507);
- an invitation is sent for a name that is pending or already exists (~181, ~184).

`CommonService.runRequest` logs every `CLASS_ERROR` answer except `SERVICE_BUSY` as
WARN (`isError`), so these show up in the log as stone errors.

## Expected
A 4xx warning, e.g. a new `CONFLICT_WARNING` (409). This changes the contract: the
OpenAPI, Rune, Tolk and the FitNesse tests (eds-testing) check codes. Then the WARN
only fires for real stone errors.

---

# Ticket 39 — Behaviour left from the review of the OpenAPI (O4)

**Labels:** bug, backend, api · **Severity:** low · **Source:** PLAN.md, OpenAPI, Phase O4; moved here 2026-11-26

**Decision:** none yet. Finding 7 (401 and 403 swapped) is a design question in PLAN.md, OpenAPI, "Open".

## Summary
The findings of O4 that change behaviour and are still open (PLAN.md has the table):
- **2:** an administrator who isn't a Trustee of a Circle and sets a `circleKey`
  calls `trustees.getFirst()` on an empty list, answered as `ERROR`
  (`ProcessCircleManager.updateExternalCircleKey`).
- **5:** duplicate names answer differently: duplicate Circle, Member or Data Object
  names give `IDENTIFICATION_WARNING`, a taken name on invite or rename
  `CONSTRAINT_ERROR` (with ticket 4).
- **6:** codes: changing a default Data Type gives `AUTHORIZATION_WARNING` (should be
  `ILLEGAL_ACTION`); a non-admin at the MasterKey operation gets
  `AUTHENTICATION_WARNING` (should be `AUTHORIZATION_WARNING`).
- **8:** `WARNING` and `ILLEGAL_SERVICE` are never raised, but by `perform` stubs
  which can't be reached.
- **9:** `invalidate` acts on the requester and ignores `memberId`
  (`ProcessMemberManager.invalidate`); the endpoint's name suggests otherwise.

## Expected
Each answered as the contract describes, with a test; 5 and 6 decided with 4, as
they change answers clients see.

---


# The backend: security

# Ticket 16 — Internal exception messages are returned to clients

**Labels:** security, backend · **Severity:** low · **Source:** WH #11

**Decision:** (2026-11-22) not a finding as such: the messages help those who use the API directly; Rune and Tolk adapt what they show. Kept for the record.

## Summary
- `CommonService.runRequest` returns `e.getMessage()` for any `RuntimeException`
  (Hibernate or JDBC text, `NoSuchElementException`, …).
- `SocketHub.handle` does the same in ERROR frames.
- `UploadDataManager` (~232, `rootMessage`) returns the driver's root message of a
  failed upload.

## Expected
A generic text with a correlation ID for the client; the details only in the log.

---

# Ticket 17 — Pending accounts without a cap (name squatting)

**Labels:** security, backend · **Severity:** low · **Source:** WH #12

**Decision:** (2026-11-22) flagged for further analysis.

## Summary
Any account can create a Circle and so become its admin. `inviteMember` then creates
a pending account for any free name. It is held 168 h (`eds.invitation.timeout.hours`),
can be renewed after expiry,
and there is no cap. This allows reserving future users' names or look-alikes
("Admin", homoglyphs), which then show in member lists and block `createMember`.

## Expected
A per-inviter cap and rate limit, or only administrators create new pending
accounts.

---

# Ticket 19 — HTTP and client hardening

**Labels:** security, ops · **Severity:** info · **Source:** WH #14

**Decision:** (2026-11-22) flagged for discussion: to be strongly considered; the options aren't weighed yet. (2026-11-26) Put off by the user: behind a reverse proxy, plain HTTP between the proxy and the stone is the intended setup, so the defaults for HTTP and HSTS stay as they are for now. The other points (the management port, Tolk's redirects, Unicode passphrases, `/version`, the download tokens in proxy logs) wait with it.

## Summary
- `quarkus.http.insecure-requests=${EDS_INSECURE_REQUESTS:enabled}` keeps plain
  HTTP on even with TLS unless the operator sets it; default to `redirect` when a
  certificate is set.
- No HSTS header.
- The management port (9000) has no `quarkus.management.host`, so it binds all
  interfaces.
- The PWA CSP `connect-src 'self' https: http://localhost:* http://127.0.0.1:*` is
  broad (it lets the status screen ask other stones, STATUS.md).
- `GET /version` is unauthenticated and shows the version, database product,
  persistence and storage ceiling.
- Tolk: Go's default client follows 307/308 redirects and resends the JSON body
  (with the passphrase) across hosts and from https to http. Add a `CheckRedirect`
  that refuses scheme or host changes.
- Passphrases are not Unicode-normalized (NFC), so the same passphrase typed on two
  systems can differ.
- Download tokens in URLs can show up in reverse-proxy logs (one object, at most
  60 min, `eds.download.token.lifetime.minutes`): add a line to RUNNING.md, which
  names the setting only.

---

# Ticket 26 — A request which names a Circle tells whether an account name exists

**Labels:** security, backend · **Severity:** low · **Source:** the review of tickets 20 and 21, 2026-11-24 (by reading)

**Decision:** none yet. To decide with SECURITY.md, "Open" (who is in which Circle) and 33.

## Summary
Ticket 21 answers an unknown name as a wrong passphrase, after the same work. But
`AbstractManager.verifyRequest` checks, for a request with a `circleId`, whether the
account is a Trustee of that Circle (`checkMemberAccount`, through
`CommonDao.findMemberByNameAndCircleId`) before the passphrase is checked. A known
name which isn't a Trustee of the Circle (any random UUID will do) gets
`IDENTIFICATION_WARNING` "No Trustee information found …" at once, without PBKDF2;
an unknown name gets the answer of a wrong passphrase after about 200 ms. So both
the answer and the time tell whether a name exists, to anyone, without a passphrase.
It is the open item of SECURITY.md "Who is in which Circle can be found out without a
passphrase" (2026-11-22, to decide), seen from the other side. The MasterKey request
tells the System Administrator's name the same way (`checkPermitted` before the
passphrase), which is known anyway.

Since ticket 28 (2026-11-26), the check is in `AuthAttempts.findAccount`, in a
transaction of its own before the passphrase. The answer is the same, so this is
unchanged.

## Expected
To decide with the open item: check the passphrase before the Trustee, or answer
"no such Trustee" for a name which isn't one only after the passphrase.

---

# Ticket 33 — The MasterKey request tells the System Administrator's name before the passphrase

**Labels:** security, backend · **Severity:** low · **Source:** the review of SECURITY.md against the code, 2026-11-25

**Decision:** none yet; with 26.

## Summary
`MasterKeyManager.checkPermitted` refuses any name but the System Administrator's at
once, "Given Account is not permitted to perform this request.", before PBKDF2; the
System Administrator's own name gets the work and "wrong passphrase". So answer and
time tell anyone the name. SECURITY.md, "The MasterKey source", "What stays", reasons
about an attacker who knows the name, as if it were a hurdle; ticket 26 called it
"known anyway". The two should agree. Also: the MasterKey request looks the System
Administrator up (`findAdmin`, by role) before it takes its place of
`PassphraseLimit`, unlike `AbstractManager`; minor, as no name is looked up.

## Expected
To decide with 26: answer a wrong name as a wrong passphrase after the same work (as
ticket 21 does elsewhere), or write in SECURITY.md that the name is public.

---

# Ticket 52 — Studies of integrity and availability

**Labels:** security, backend · **Severity:** info · **Source:** SECURITY.md, "Open" (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- An active-operator study: an operator who tampers with the database between
  requests, to confuse a client.
- A concurrency study of the rotation of a Circle Secret.

They test integrity and availability, not the confidentiality claim, which stands.

## Expected
To decide whether and when; each as an assessment with its findings as tickets.

---


# Ticket 53 — A request which sealed with the old MasterKey can write after its rotation

**Labels:** bug, security, backend · **Severity:** low · **Source:** the review of the fix of ticket 13, 2026-11-26

**Decision:** none yet.

## Summary
The rotation of the MasterKey wraps every Member's keys, every Trustee's Circle
Secret and the Sessions anew, and installs the new key after its commit (ticket 13,
SECURITY.md, "Data Types, and the rotation of the MasterKey"). A request which
sealed something with the MasterKey before the rotation began, and writes it after
the rotation committed, still writes it sealed with the old key:
- **A new passphrase** (`AbstractManager.updateMemberPassword`,
  `ProcessMemberManager.updateOwnCredential`): it seals the new salt and private key
  (seconds, with PBKDF2), then its update waits for the rotation's lock of the
  Member, then overwrites the rotated row. The account then opens with neither
  passphrase, and its Circles are lost to it.
- **A new account and an invitation** insert a Member the rotation never saw.
- **A Session** made by a login in that moment fails once.
- **An invitation of an expired name** locks its Circle, then changes the expired
  invitee's Member: the opposite order of the rotation, so one of the two fails as a
  deadlock (the review of tickets 1–3).

It was so before ticket 13. A MasterKey rotation is rare, and done by an
administrator.

## Options
- Every writer which seals with the MasterKey takes a shared lock of one row of the
  whole stone (e.g. the current `eds_masterkey` row) before it seals, and checks
  that the version in memory is the stored one; the rotation takes the row
  exclusively first. A writer which finds another version fails with a retryable
  answer. This also covers the moment between the commit and the installation.
- Or: the rotation puts the stone in maintenance (refuses such requests) while it
  runs.

## Expected
A decision, then a test of each kind of writer racing a rotation, which fails
without the fix.

---

# The backend: performance

# Ticket 27 — The inventory sums every part of the stone

**Labels:** performance, backend · **Severity:** info · **Source:** the review of ticket 23, 2026-11-24

**Decision:** (2026-11-25) D and B: the full comparison in the tests (the load test's final check and the integration tests), and in the stone a check of a few Circles at a time, in turn. To do after 28, 35 and 36 (done), as it needs heavy testing.

## Summary
To keep the totals of the Circles honest, `StatusManager.checkStoredBytes`
(`DIFFERING_TOTALS`) joins `eds_circles`, `eds_circle_storage`, `eds_metadata`,
`eds_data` and `eds_data_parts` over the whole stone at each
inventory: at the start, and then every `eds.status.inventory.interval.minutes` (60
by default). It is the scan ticket 23 took out of every write, now once an hour. To
watch in the load runs; if it costs too much on a large stone, check one Circle at a
time, or less often.

Measured in the 5-hour run of 2026-11-24 (40 rooms, about 260 objects each at the
end, content up to 1 MiB): 0.50 s at the first hour, 0.69, 1.03, 1.22, and 2.10 s at
the fifth, growing with what the stone holds. Nothing waits for it, as it takes no
lock, but it is a full read of three tables an hour.

---

# Ticket 28 — What is left of the throughput under load

**Labels:** performance, backend · **Severity:** info · **Source:** the load runs of 2026-11-24/25 (the observer, JFR)

**Decision:** (2026-11-25) steps 1, 2 and 4 done (PLAN.md, *Streaming*, "Connections and locks held for less"). Option 3, a cache of Circle keys, is the last option: the user isn't fond of caching secrets. The points below: none yet.

## Summary
Most of it is done: PLAN.md, *Streaming*, "Connections and locks held for less".
The 4-hour run with the fixes doubled the throughput in each hour. It still falls
by 24 % from the first hour to the fourth. That is mostly ticket 38 and the
buffer cache (ticket 32). Open:
- **ECDH is now the largest cost:** 23 % of the CPU samples, nearly all of it
  the unwrap of Circle keys. Option 3, a cache of Circle keys, is the user's last
  option. Fewer unwraps per request would be another way.
- **One access change checked every socket again.** Done with ticket 18
  (2026-11-26, SECURITY.md, "The socket before and after AUTH"): a change of
  Trustees or of a Circle checks only the connections which joined it. A change of
  a Member, a Session (a login, a logout) or the MasterKey still checks every
  connection.
- **The pool is still full:** 42–50 of 60 connections idle in transaction, now
  for 18 ms at the median, and about 140 threads waiting for one. With the work
  per request down, a larger pool is worth a run.
- **From the review of the fixes (2026-11-26):**
  - A passphrase changed during a login: that login counts two attempts, or
    refuses the new passphrase once. It's rare.
  - `ShareBean.processData` reads before `verifyRequest`. So its request holds a
    connection while the account is read in a second one, as `attempts.begin`
    already did.

## Expected
A decision on each point. A load run before and after each fix.

---

# Ticket 38 — Counting a room's entries, and a rotating Circle's content, reads its whole history

**Labels:** performance, backend · **Severity:** info (low priority) · **Source:** the 4-hour run of the fixes of 28, 2026-11-25 (the observer)

**Decision:** (2026-11-26) the load test first, done. The rest: none yet.

## Summary
Two counts read every row of a history, so their cost grows with the stone's
age. In the fourth hour of the run they were 2.07 and 2.47 of the 14.8 statements
running at a time. Both mostly came from how the load test behaved, not from the
stone or its clients (the user, 2026-11-26):
- **The entry count** (`FeatureManager.count` → `FeatureDao.countEntries`).
  - Rune (`chat.ts`) and Tolk (`chat.go`) count with `after`, the position
    their last receipt read up to.
  - The load test counted without `after`, every message of the room, about
    10 times a second.
  - Pagination is not involved: listing entries counts nothing.
- **The rotation progress** (`FetchCircleManager.addRotations` →
  `RotationDao.countDataOnKey`). It counts all content on the active key and the
  retiring key, at each fetch of the Circles, while a Circle is being rotated.
  - A stone rotates each Circle once a year by default
    (`eds.circle.secret.rotation.days` 365).
  - The load test had one rotated about once a minute: 230 in 4 hours.

**Done (2026-11-26, the user: the load test realistic first).** The load test now
asks as the clients do:
- **The count:** a user counts the messages after the position its last receipt
  read up to. The receipt holds that position, as Rune's does.
- **Rotations:** the administrator has one rotated at once, then one each
  `eds.load.rotate.every` (PT20M by default).
- **Tests:** `RealisticLoadTest`. A 2-minute run passed.

## What stays in the stone (low priority, not decided)
- **The count:**
  - A client which counts without `after` still makes the stone count a whole
    history.
  - A cap (for example 1,000, shown as "999+") would bound it.
- **The rotation progress:**
  - Its cost grows with a Circle's content while a rotation runs.
  - The total could be kept from the start of the rotation, so only the
    retiring key is counted.

## Expected
A 4-hour run with the realistic load shows what is left of the fall. Then a
decision on the two points above.

---


# The backend: improvements

# Ticket 40 — A Member can't list their sessions or log out everywhere

**Labels:** improvement, backend · **Severity:** info · **Source:** PLAN.md, "Several sessions" (2026-11-15)

**Decision:** none yet.

## Summary
Since several sessions per account (`eds.session.max.per.member`, 10), a Member has
no request to list their sessions, nor to end all of them but by a new passphrase.

## Expected
To decide whether it is wanted; then two requests and their place in Rune and Tolk.

---


# Tests, operations and process

# Ticket 31 — Tolk's tests aren't part of the verification here

**Labels:** process, Tolk · **Severity:** info · **Source:** the review of the findings, 2026-11-25

**Decision:** none yet.

## Summary
"Build and tests green" in the commits since 2026-11-22 covers the Java modules,
Rune and FitNesse; `eds.sh build tolk --test` wasn't run. Some commit messages gave
"no Go" as the reason, which was wrong: this computer has Go 1.26.8 (`/usr/bin/go`;
`eds-cli/go.mod` asks for 1.26.0). 30 is what it hid. Those messages went with the
rework of the history on 2026-11-26.

CI (`.github/workflows/build.yml`) builds EDS with Rune and the stone, with the tests
skipped; Tolk (`-Pcli`) isn't built there at all.

## Expected
Tolk's tests in every verification, as part of the Definition of done's "build and
tests 100 % green". Whether CI runs the tests and builds Tolk, to decide.

---

# Ticket 32 — PostgreSQL checkpoints too often under load

**Labels:** operations, PostgreSQL · **Severity:** info · **Source:** the 5-hour load runs of 2026-11-23/24 (PostgreSQL 14, defaults)

**Decision:** none yet.

## Summary
At the end of both runs, while the test deleted its users and rooms, PostgreSQL
logged "checkpoints are occurring too frequently (4 seconds apart)" with the hint to
raise `max_wal_size` (1 GB by default). It was written in ticket 23, and went with it.

The 4-hour run of 2026-11-25 (PostgreSQL 18 in Podman, defaults) logged it
throughout the run, not only at the end: in each of the four hours. The same run's
buffer hit rate fell from 96 % to 77 % with the default `shared_buffers` of 128 MB.
In the run with the fixes of 28 (twice the requests), it fell from 94 % to 54 %,
and reads from disk were part of the fall that is left (PLAN.md, "Connections and
locks held for less").

## Expected
A sentence in RUNNING.md on sizing `max_wal_size` (and `log_checkpoints`) and
`shared_buffers` for a stone which stores and deletes much content; nothing in the
stone.

---

# Ticket 34 — Stale comments in the code; the largest object has no upper bound

**Labels:** backend, documentation · **Severity:** info · **Source:** the review of the documents against the code, 2026-11-25

**Decision:** none yet.

## Summary
Comments which no longer match the code:
- `FetchMemberManager.fetchSomeoneElse`: "these Settings are set to True"; the default
  of `eds.show.trustees` is false since ticket 10.
- `CommonDao.holdStoredBytes`: "while content is streamed after its thumbnails were
  deleted"; an upload deletes the thumbnails after the content now (ticket 23).
- `StandardSetting.STREAM_MAX_OBJECT_SIZE`: 1000 MiB "below PostgreSQL's 1 GB field
  limit"; content is stored in parts of 4 MiB, so that limit no longer applies.
- `ProcessMemberManager` (~558): "Invitations can only be issued by the System
  Administrator"; administrators of a Circle invite into it too.

And `SettingManager` checks `eds.stream.max.object.size` for a positive number only.
A value over `quarkus.http.limits.max-body-size` (1100M) is accepted, and uploads of
that size then fail in the HTTP layer instead of with the stone's own answer.

## Expected
The comments corrected. Decide the upper bound of the setting (the HTTP limit, or a
check at startup that the two agree) and the reason for the default.

---

# Ticket 41 — Tests set private constructors accessible

**Labels:** test, backend · **Severity:** info · **Source:** SonarQube `RFI_SET_ACCESSIBLE`, 2026-11-22 (PLAN.md, Crypto, open points)

**Decision:** (2026-11-22) not a false positive, but a warning of the coming changes to Java's integrity by default. The way forward: don't set the flag; try the action and handle its failure. Not changed yet.

## Summary
`DatabaseSetup.newSettings` calls `setAccessible` on the private constructor of
`Settings` (`eds-core/src/test/.../setup/DatabaseSetup.java` ~353); so do
`SanitizerBeanTest` (~287) and `SchemaManagerTest` (~165).

## Expected
The tests reach what they need without `setAccessible`.

---

# Ticket 42 — The rotation of tens of GiB isn't tested

**Labels:** test, backend · **Severity:** info · **Source:** PLAN.md, Crypto C6

**Decision:** none yet.

## Summary
The big-data test of the rotation (`CircleRotationIT`) passed with 2 GiB on
PostgreSQL 18. A run of tens of GiB was left for the final test system.

## Expected
A run of tens of GiB on the final test system, with its numbers in PLAN.md.

---


# Rune and Tolk together

# Ticket 43 — Chambers: Tolk shows encrypted names, and no client sends the Name Key

**Labels:** bug, Rune, Tolk, security · **Severity:** low · **Source:** SECURITY.md, RUNE.md, E2E.md (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- **Tolk** shows a chamber's file and folder names as ciphertext: it has the content
  encryption of chambers, but not that of names (`eds-cli/internal/e2e`). Rune hides
  names since 2026-11-21 (`E2E.md`, "Names").
- **Neither Rune nor Tolk sends the Name Key**, which the stone takes since
  2026-11-22 to keep a chamber's names unique within a folder (`nameKey` in the
  contract). So two files of one name can be stored in a chamber's folder.
- With ticket 30, Tolk's encryption of names can be checked against the shared
  vector.

## Expected
Tolk hides names as Rune does; both send the Name Key; tests against the vectors.

---

# Ticket 44 — Rune and Tolk don't show a rotation of the Circle Secret

**Labels:** improvement, Rune, Tolk · **Severity:** info · **Source:** PLAN.md, Crypto C6

**Decision:** none yet.

## Summary
The stone tells a rotation (the socket frames `ROTATING` and `ROTATED`; the Circle's
`secretRotated`, `rotationDue` and `rotation` with its progress). Rune and Tolk
ignore them.

## Expected
To decide what a person should see (that the room's key is being changed, and how
far), then in both clients.

---


# Rune

# Ticket 45 — Rune: bugs

**Labels:** bug, Rune · **Severity:** low · **Source:** RUNE.md, "Open", and E2E.md (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- **The chat's tab can hide the chat:** in a room which has the chat, its tab puts
  the pane away; unread counts then show only in the list of rooms.
- **A chamber's key is given only while that chamber is open:** Rune tends the open
  chamber (`watchKeys`), so a member added without keys waits until an
  administrator opens that chamber; a changed key isn't given by itself.
- **About Rune is out of date:** it names fixed algorithms, while the System
  Administrator may choose others; and it says a chamber doesn't hide the names of
  its files, which it does since 2026-11-21 (`components/about-dialog.ts`, its
  texts).
- **The login** says "Username" and "Password" (`components/login.ts:30-31`), the
  rest of Rune "passphrase".
- **On windows lower than 700 px** the login's footer sits below the knot and needs
  scrolling (not confirmed by reading).

## Expected
Each fixed, and checked in the click test where it can be.

---

# Ticket 46 — Rune: improvements

**Labels:** improvement, Rune · **Severity:** info · **Source:** RUNE.md, SECURITY.md, E2E.md (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- Only the rooms pane can be minimized, and there is no key for it.
- No form to replace an account's chamber keys with the passphrase. It matters when
  a stolen Session set the first keys of an account without a vault (SECURITY.md,
  "Sessions, copies, folders, visibility and settings", "What stays").
- Playing a video of a chamber while it is fetched, through the service worker, so
  it starts at once (E2E.md).
- The check of the QR encoder against an independent decoder isn't in the
  repository, nor the generator of Tolk's `internal/qr/testdata/rune.json`.
- `accessories/brand/rune/README.md` describes web files which live in the PWA now.

## Expected
To decide which are wanted, and their order.

---

# Ticket 47 — Rune: what isn't tried in a browser

**Labels:** test, Rune · **Severity:** info · **Source:** RUNE.md, "Open" (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- **The "done when" of L5:** seeking in a video isn't in the click test, and the
  user's own test round in a browser is still to be completed.
- **The service worker's fixes of 2026-11-13** (API never cached; pruning and the
  fallback) weren't tried in a browser; the pruning ran against a mock of the Cache
  API in Node.
- **Not in the click test:** joining a chamber by an invitation link (the flow test
  does it with Rune's modules); the save dialog of the File System Access API;
  drag and drop; the page while the stone is asleep.
- **Of 2026-11-17, not tried in a browser:** the opened list of a drop-down in the
  dark theme; the shading and the chamber's look on a phone; the minimized pane in
  another arrangement than three columns; the final wording of the hints of "E2E",
  "Chamber" and "Keys for chambers"; tick boxes other than those of the New room
  dialog; the month on a phone.
- **The knot behind the plans** wasn't seen by the user when it was made (most
  likely the page kept by the browser); to look at again after a rebuild.

## Expected
The user's round in a browser, and the click test extended where a headless browser
can reach.

---


# Tolk

# Ticket 12 — Tolk prints remote text without stripping terminal escape sequences

**Labels:** security, cli · **Severity:** medium/low (suspected) · **Source:** WH #7

**Decision:** (2026-11-22) not the backend's: a matter of Tolk, for when Tolk gets its focus.

## Summary
Names, file names and chat text from other members are printed through `fmt` and
lipgloss/bubbletea without filtering control characters. `ansi.Strip` is only used
for restyling (`eds-cli/internal/tui/overlay.go:44,162`, `rooms.go:147`). The stone
can't check chat content, since it is client-encrypted.

## Attack
A member's name or message contains:
- `ESC]52;c;…BEL`, which writes the victim's clipboard;
- `ESC]8;;https://evil BEL`, which disguises hyperlinks;
- cursor or clear-screen sequences, which forge output.

## Expected
Replace C0/C1 controls and bidi controls (U+202A–202E, U+2066–2069) in every remote
string before display. Rune is not affected (`textContent`).

---

# Ticket 30 — Tolk's copy of Rune's `vectors.json` lacks the vector of names

**Labels:** test, Tolk · **Severity:** bug (test) · **Source:** the review of the findings, 2026-11-25 (by comparing the files)

**Decision:** none yet.

## Summary
Tolk keeps copies of files of Rune in `eds-cli/internal/i18n/rune/` (`copy.go`,
`go generate`), and `TestCopiesMatchRune` fails when a copy differs from its source.
Rune's `eds-quarkus/src/main/webui/src/e2e/vectors.json` gained a test vector for
names (context `name`, "a file name") with the Name Keys of 2026-11-22; Tolk's copy
hasn't it (checked 2026-11-25: the only difference in content is that vector).
So Tolk's tests fail on it, and Tolk doesn't check its encryption of names against
the shared vector. It went unseen, as Tolk's tests weren't run (31).

Confirmed on 2026-11-25. `go test ./...` in `eds-cli` (Go 1.26.8, `go vet`
clean): every package passes except `internal/i18n/rune`, where
`TestCopiesMatchRune` fails with "vectors.json differs from
…/webui/src/e2e/vectors.json; run 'go generate ./...'". `go generate` wasn't run.

## Expected
`go generate` in `eds-cli/internal/i18n/rune`, then Tolk's tests; and Tolk's
encryption of names checked against the vector, with the Name Key when Tolk sends
it (SECURITY.md, "Open").

---

# Ticket 48 — Tolk: bugs

**Labels:** bug, Tolk · **Severity:** low · **Source:** TOLK.md, "Open decisions" (moved here 2026-11-26)

**Decision:** none yet. Whether the stone should refuse such access changes for every client is a design question in RUNE.md and TOLK.md.

## Summary
- **`tolk access` changes one's own access and the only administrator's**, which
  Rune and the TUI refuse (only `tui/rooms.go` checks).
- **A chamber's key is given only by `tolk members`**, not while `tolk chat` or the
  TUI is open (`Repair` is called only in `commands/chamber.go`).
- **The TUI's text without rooms** (Ctrl+R) names Rune's "New room" button; the TUI's
  key is `n` (`tui/keys.go` ~242).
- **The help says "Tolk keeps one file, config.json"** (`commands/help.go` ~429);
  since E5 the files `device-<hex>.bin` are beside it.
- **"cannot reach …"** is English only (`client/client.go`, `content.go`,
  `socket/socket.go`).

## Expected
Each fixed, with a test against the fake stone.

---

# Ticket 49 — Tolk: local hardening

**Labels:** security, Tolk · **Severity:** low · **Source:** TOLK.md, "Open decisions" (moved here 2026-11-26)

**Decision:** none yet; the first point needs one (it would refuse files that work today).

## Summary
- **`TOLK_PASSPHRASE_FILE`** is meant as a protected file, like an SSH key, but Tolk
  doesn't check its mode or owner (`service/access.go` ~97). To decide: refuse a
  file others can read, as `ssh` does.
- **The agent's directory** is checked for mode 0700 and its owner on Unix; on
  Windows nothing is checked (`agent/owner_windows.go`).
- **`TOLK_KDF_ITERATIONS`** is there for the tests, but works for anyone and can
  weaken a new container (`commands/container.go` ~17); it isn't in the help.

## Expected
A decision on the first; the second and third closed (e.g. only in test builds, or
a floor).

---

# Ticket 50 — Tolk: improvements

**Labels:** improvement, Tolk · **Severity:** info · **Source:** TOLK.md (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- `tolk settings layout` takes a preset only; a layout tree copied from Rune can't
  be set with it. Tolk doesn't read the account's settings (`Member.settings`), so
  language and clock aren't taken as defaults.
- Avatar IDs, the six reactions and the layout presets are copied from Rune by hand,
  without a test that compares them.
- The TUI's differences from the plan (L6d): no passphrase change, no pause or
  resume of downloads, no `o` to open a file, settings saved one at a time,
  reactions always as emoji, no mouse.
- Receipts and typing in `chat --plain`.
- The TUI has no tabs for the chat and the files, and shows the chat pane also for a
  room without a chat.
- The TUI doesn't look for rooms one was added to, or for new rooms and users, by
  itself (TOLK.md records it as not taken over from Rune).
- `present` has no entry in the menu and counts from the next start; a call which
  began before Tolk joined, or ended, isn't told.
- Certificate pinning for a stone on an unusual host: the link could carry the
  certificate's fingerprint (E2E.md).

## Expected
To decide which are wanted, and their order.

---

# Ticket 51 — Tolk: what isn't tried on a real screen

**Labels:** test, Tolk · **Severity:** info · **Source:** TOLK.md (moved here 2026-11-26)

**Decision:** none yet.

## Summary
- The user's review of the TUI on a real screen.
- The phases' "Not checked": the interactive mode in a chamber and against a real
  stone (presence, calls), the layout changes of 2026-11-17 in a terminal, a file
  of several GiB, and Windows and macOS.

## Expected
The user's round, and runs on macOS and Windows.

---
