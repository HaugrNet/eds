# EDS plan: remediation

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: planned 2026-11-27, nothing built. It comes from the review of the whole
branch `feature/refactor` on 2026-11-27 (design documents and code, eleven readers,
one per area), and holds what that review found and `FINDINGS.md` did not have.

Like `E2E.md`, `FEATURES.md` and `STATUS.md`, this plan crosses the backend, Rune and
Tolk. Code comments cite it as `REMEDIATION.md`. Phases are named R0 to R8, and an
item is cited as `R1.3`.

## The idea

The review found about a hundred issues which no document had. Some lose data or
lock a stone for good, some break what `SECURITY.md` and `E2E.md` claim, many are
smaller. This plan orders them by how much harm they do, so the worst are closed
first, and says for each how the fix is shown to work.

It does not replace `FINDINGS.md`: the tickets there stay. Where an item here makes
a ticket worse, or is closed by the same work, the ticket is named.

## Words

- **Critical:** loses data, locks a stone or an account for good, or breaks the
  confidentiality claim without anyone noticing.
- **High:** a security rule which can be passed, or a stone which can be stopped,
  by someone who should not be able to.
- **Medium:** wrong answers, failures under load, races which repair themselves on
  a retry.
- **Low:** operation, hardening and documents.
- **Load test:** the item is only done when a run of the load test
  (`eds-testing`, `-Pload`, on PostgreSQL) shows the fix. The column says which
  kind:
  - **LT-race**: the fix closes a race; the run must provoke the two requests at
    the same moment and end with the final check green.
  - **LT-before/after**: the fix changes throughput or what is held; a run before
    and one after, with the numbers in this plan.
  - **LT-abuse**: the fix bounds what one account can take; the run has a user who
    tries, and the others must not notice.
- **Check first:** found by reading only, and it depends on how a library or the
  database behaves. A test which shows the fault comes before the fix; if it does
  not show, the item is closed with that result.

## How the review was verified

- The code behind each critical and high item was read again after the reader
  reported it, unless the item says "check first".
- No race was reproduced against a running stone. Three SQL behaviours were tried
  in isolation (the time zone of `current_timestamp` on H2 and PostgreSQL, the
  missing unique constraint, the foreign key at a Circle delete).
- The backend was built twice (`mvn -Dstone verify`): 1058 of 1059 tests of
  `eds-core` passed both times, and one failed both times (R0.1). Tolk's
  `go test ./...` fails one test (ticket 30). Rune's build, FitNesse, the load test
  and everything on PostgreSQL were not run.

## Rules

- The Definition of done of `PLAN.md` holds for every item: a test which fails
  without the fix, the build green, the review by a reader who did not write it.
- R0 comes first, whatever else is urgent: without it "green" says too little.
- Within a phase the order of the table is the order of work.
- An item which needs a decision is marked **decide**; the decisions are gathered
  under "Decisions needed". Work on such an item starts with the decision.
- When an item is done, its line gets the date and what was verified, and the
  design it changed is written into the plan it belongs to (`PLAN.md`,
  `SECURITY.md`, `E2E.md`, ...).

## Order of work

| Phase | Covers | Severity | Items | Load test |
|---|---|---|---|---|
| R0 | The verification: a green build that says something | first | 8 | builds the means |
| R1 | The stone: data lost, stones and accounts locked | critical | 12 | 4 |
| R2 | Chambers: the key reaches the stone, keys lost | critical | 14 | — |
| R3 | The stone: security rules which can be passed | high | 17 | 4 |
| R4 | The stone: wrong answers and races | medium | 25 | 10 |
| R5 | Operating a stone | medium, low | 14 | — |
| R6 | Rune and Tolk, apart from chambers | medium, low | 12 | 1 |
| R7 | The API contract | low | 8 | — |
| R8 | The documents | low | 9 | — |

R1 and R2 are both critical and touch different code (the stone; the clients), so
they can be worked on side by side. R2 starts with its decision.

---

## Phase R0: the verification

Until this is done, a fix of R1 to R4 cannot be shown on the database it is for.

| # | Issue | Where | To do |
|---|---|---|---|
| R0.1 | `testASessionCannotMakeAnotherSession` fails in the whole suite (2 sessions, expects 1) and passes alone: another test leaves a session of `member1` | `ManagementBeanProcessMemberTest:710` | Find the test which leaves it; each test cleans up, or counts only its own |
| R0.2 | `testFailuresAtTheSameMomentAllCount` answered 503 for 403 once, on a busy machine | `EdsAuthLimitTest:166` | **Check first**: repeat under load; if it is the place of `PassphraseLimit`, the test waits for one |
| R0.3 | The Maven build of Tolk runs `go generate` before the tests, so `TestCopiesMatchRune` can never fail there, and a build changes tracked files | `eds-cli/pom.xml:34-47` | Take `go generate` out of the build, or fail on a changed tree; then close ticket 30 |
| R0.4 | Nothing of PostgreSQL runs in a normal build: the snapshot of a download, the advisory lock of the schema, `FOR UPDATE` in the order of the IDs; the lock tests read H2's own tables | `ContentStreamer:180`, `SchemaManager:63`, `DataDao:164-199`, `MasterKeyRotationLockTest:233` | A PostgreSQL in a container for the verification; the gated tests, FitNesse and a short load run against it; the lock tests rewritten to run on both |
| R0.5 | The load test cannot fail on a drift of the stored bytes: the comparison only logs a WARN, the test only collects ERROR, and the comparison runs hourly | `StatusManager:296`, `TestStone:337`, `LoadRun:241-262` | The final check asks for the comparison and expects none; unexpected WARN lines fail the run (ticket 27, decision D) |
| R0.6 | The load test has no user who misbehaves, and no two requests aimed at the same moment | `eds-testing/.../load` | The means for LT-race and LT-abuse: a pair of users on one object, one Circle, one account; a slow reader and writer; many connections of one account |
| R0.7 | With `EDS_DB_URL` exported, a plain build runs the tests of `eds-quarkus` against that database, with the test MasterKey | `DatabaseSelection:90-92`, `eds-quarkus/pom.xml:162-170` | **Check first**; the tests force H2 unless asked otherwise |
| R0.8 | `README.md` says FitNesse is part of every verification; `eds.sh build --test` does not run it. The 95 % rule measures Java only, and CI skips all tests | `README.md:229`, `eds.sh:444-483`, `PLAN.md:129` | **Decide** (D7): FitNesse in `build --test`; what the coverage rule covers; tests in CI (ticket 31) |

Done when: the whole build is green twice in a row, on H2 and on PostgreSQL; a
deliberate drift of the stored bytes fails the load test; and the load test can run
the three kinds named under "Words".

---

## Phase R1: the stone, critical

| # | Issue | Where | Direction | Load test |
|---|---|---|---|---|
| R1.1 | Sessions and invitations are deleted by the database's `current_timestamp`, while their expiry is written in UTC. `eds.sh` starts the container in `Europe/Berlin`: they go 1 to 2 hours early. East of UTC+8 every Session goes at the next hourly run | `SessionEntity:54`, `MemberEntity:57`, `eds.sh:806` | The time as a parameter (`Utilities.newDate()`), as the lock of #68 does it; a test in three time zones | — |
| R1.2 | A Trustee with WRITE renames or deletes the root folder of a Circle. After a rename the Circle lists nothing and takes nothing, and the name `/` can't be given back | `ProcessDataManager:129-171, 222-237` | Refuse both for the root; refuse content for a folder | — |
| R1.3 | A setting can stop the stone. Ten numbers are checked as digits only; `eds.session.timeout.minutes` isn't checked at all. `eds.data.max.inline.size` too long breaks every JSON request, the one that would repair it too; others break every login, the hourly job, or the next start | `SettingManager:186-222, 268-272`, `Settings:173-234`, `BodyCounter:78-93`, `StartupBean:138` | A range for every number; getters which fall back to the default; the start doesn't die of a setting. Closes the rest of ticket 14 and of ticket 34 | — |
| R1.4 | No entity has a version, and a save writes every column. A small change of an account (avatar, settings) which overlaps its passphrase change writes the old salt and keys back: the account opens with neither passphrase and loses its Circles. The same undoes a change of role | `CommonDao:143`, `ProcessMemberManager:453, 498`, `AbstractManager:357-364, 515-551` | Lock and read the Member again before any write, or `@Version` and a retry. Widens ticket 53 | LT-race |
| R1.5 | The same for a Circle: an update of its name or avatar which overlaps the removal of a Trustee writes the old due date back, and the rotation after the removal never starts | `ProcessCircleManager:216-232`, `ProcessTrusteeManager:255-264` | `updateCircle` locks and reads again; every save after `lockCircle` reads again, or the due date is set by one statement | LT-race |
| R1.6 | A Data Object can get two content rows. `eds_data.metadata_id` isn't unique, and the first upload has nothing to lock: a repeated `PUT …/content` leaves two, and readers get either. A delete during the first upload leaves the stored bytes too high for good; a thumbnail can outlive its object | `eds-schema.sql:497-512`, `UploadDataManager:123-150`, `DataDao:89-95`, `MediaManager:149-171` | `UNIQUE (metadata_id)`; the `eds_metadata` row locked before content is made, changed or deleted | LT-race |
| R1.7 | The sanity job marks sound content FAILED when a replacement or a rotation commits between its two reads. FAILED content is never checked again, and a rotation passes over it and so never ends: the retiring secret stays | `SanityManager:146-158`, `RotationDao.findDataOnKey` | Hold the content row, or read the checksum again before FAILED is written; a way back from FAILED | LT-race |
| R1.8 | A rotation calls a read failure of the database "damage" for large content, with the same end as R1.7 | `ContentCopier:179-184` | Follow the whole chain of causes, as `ContentStreamer` does | — |
| R1.9 | A changed `eds.crypto.hash.algorithm` makes every inline read of older content answer "corrupted" and mark it FAILED | `FetchDataManager:215-229` | Choose the digest by the length of the stored checksum, as `SanityManager` does | — |
| R1.10 | The first MasterKey of a stone: two requests at the same moment leave the stone running on one secret while the stored check value is of the other. After a restart nothing opens | `MasterKeyManager:179-204, 333-337` | Take the lock first; seal with the request's own key; install after the commit, as the rotation does | — |
| R1.11 | A MasterKey request on an unlocked stone rotates to whatever secret it carries, if that differs: no word of intent, no proof of the old one. A source which answers wrongly once, or a file with a newline, becomes the MasterKey; `masterkey load` in both scripts does it too | `MasterKeyManager:213-225`, `stone:1401-1447`, `eds.sh:997-1040` | A rotation of its own, which needs the present secret; unlock and "set the source" refuse another secret; the scripts look whether the stone is locked. **Decide** (D5) | — |
| R1.12 | A database of `master`, or of an earlier build of this branch, passes the check at start: both say schema version 4. The stone reports ready and fails by request | `StartupBean:59, 165`, `eds-schema.sql:94` | **Decide** (D4): raise the version at the merge, or check a mark of the schema at start and refuse | — |

Done when: each has its test; the LT-race items have a run in which the pair of
requests met at least a thousand times and the final check, with R0.5, is green; and
a stone started in `Asia/Singapore` keeps a Session for its whole time.

---

## Phase R2: chambers, critical

`SECURITY.md` says a chamber protects "against a dump and a curious operator", and
that "Tolk gives the full protection". R2.1 to R2.3 show a stone which answers
falsely gets the room key from an unchanged Rune or Tolk. Either the clients close
it, or the claim is narrowed; that is decision D1, and it comes before the work.

| # | Issue | Where | Direction |
|---|---|---|---|
| R2.1 | An administrator's client gives the room key to any public key it sees for the first time, by itself, every few seconds. The stone lists a Trustee with a key of its own and receives the room key, and with it every earlier one. The fingerprint is shown after the key has gone | Rune `e2e/rooms.ts:272-303`, `e2e/device.ts:78-85`, `account.ts:200-220`; Tolk `service/device.go:139-153`, `service/chamber.go:522-570` | **Decide** (D1): no key to a key nobody accepted, or lists of Members signed by an administrator's key |
| R2.2 | Adding a Member, and inviting an account which exists, wrap the room key without the check of R2.1, a changed key too | Rune `account.ts:51-54, 340-348`; Tolk `service/chamber.go:460-474` | Through the same check as the repair |
| R2.3 | Nothing remembers a room key. The stone can hand a Member a key of its choosing, or an older one after a Member was removed, and the client writes under it | Rune `e2e/rooms.ts:85-112`; Tolk `service/chamber.go:303-358` | The device remembers, per chamber, the highest version and a hash of its key, and refuses another; later, key changes signed |
| R2.4 | Rune opens an entry under the Circle ID the stone puts into it, and a file under the one in its metadata. The stone names another Circle, without a key, and its plain text shows in the chamber as a Member's | Rune `features/features.ts:94-123`, `chat.ts:176-194`, `downloads.ts:244`, `media.ts:75` | The Circle of the room being read goes in; another one is refused. **Check** Tolk the same way |
| R2.5 | The state of a room is kept for the life of the tab or process. A second administrator's stale client changes the key from an old version: the newer key is gone and what was written under it can't be read; or its repair gives everyone the old key again | Rune `e2e/rooms.ts:60-78, 288-330`; Tolk `service/chamber.go:288-301` | Read again before a repair and a key change, and on a failure to open; never lower a version; the stone takes `setSecrets` only for the version it expects |
| R2.6 | A Member is removed and the key stays, without a word: in Tolk's TUI always; in both when the remover's device is locked or behind, when a Member leaves, when an account is deleted, and when the key change fails after the removal | Tolk `tui/rooms.go:395-407`, `commands/rooms.go:655`; Rune `account.ts:160-171` | Removal and key change as one step in `service`; the chamber marked "key change due", which any ready administrator's client completes; a key change by hand |
| R2.7 | Rune makes new keys from what it knew when the page was loaded. Keys made on a phone since are replaced: everything wrapped to them is lost | Rune `e2e/rooms.ts:189-197`, `e2e/identity.ts:101-128` | Ask the stone just before; the stone refuses to replace a public key and vault without being told to |
| R2.8 | The recovery key is shown after the room is made. If that fails (a name taken, the network), it is never shown, and can't be shown later | Rune `account.ts:17-30, 108-111` | Show it right after the keys are made; remember that it was not confirmed (`E2E.md` promises the reminder) |
| R2.9 | Tolk writes the keys to the stone before the account key is safe on disk, and in JSON mode says nothing of the recovery key on a failure. There is no way to make new keys afterwards | Tolk `commands/chamber.go:52-86`, `commands/stones.go:111-136` | The account key first; the recovery key in JSON too; `tolk key reset` with the passphrase |
| R2.10 | The repair passes over two states for good: a Member whose secret is still that of the invitation, and one whose keys were replaced | Rune `e2e/rooms.ts:296-299`; Tolk `service/chamber.go:557` | The secret says which key it was wrapped to; an invitation secret of a Member with a public key is repaired |
| R2.11 | `tolk key accept` accepts the key the stone serves at that moment, and gives it the room key in the same call | Tolk `commands/chamber.go:252-300` | The fingerprint as an argument, or shown and confirmed first |
| R2.12 | `tolk mv` and the TUI's move use the stone's own move between rooms: a chamber's content lands unreadable in another, or plain in a chamber, and is reported as moved | Tolk `commands/files.go:375-380`, `tui/filespane.go:250` | Copy through Tolk and delete, as `cp` does, or refuse |
| R2.13 | Rune leaves an ordinary room behind when turning it into a chamber fails | Rune `account.ts:19-24` | Delete it, or make room and chamber in one request |
| R2.14 | An entry isn't bound to its author, its time or what it is about: the stone can show one Member's text as another's, or again | Rune `e2e/convention.ts:472-484` | **Decide** (D1): the author inside the encrypted body, or written down as a limit in `E2E.md` |

Done when: a fake stone which lies (a Trustee of its own, another Circle ID, an old
key, a changed key at `accept`) gets nothing from Rune's modules and from Tolk, each
as a test; two administrators changing a key at once lose nothing; and `SECURITY.md`
and `E2E.md` say what D1 decided. Tickets 43 and 45 are done with it or stay named.

---

## Phase R3: the stone, security rules which can be passed

| # | Issue | Where | Direction | Load test |
|---|---|---|---|---|
| R3.1 | A fragmented socket message is gathered without a limit, before AUTH too: the 256 KiB is set through two calls which the container doesn't read | `SocketEndpoint:55-65` | **Check first**, with a real socket. Then count in the endpoint and close at the limit | LT-abuse |
| R3.2 | A download token is the Session token, sealed with the MasterKey. A logged URL, the MasterKey and one dump give the Member's keys, not "one object for 60 minutes" | `MediaManager:93-97` | **Decide** (D3): seal what opens the one object, or keep a row for the token. Corrects ticket 19 | — |
| R3.3 | `credentialType=INVITATION` tells without any credential whether a name is free, an account, or invited | `ProcessMemberManager:92-98, 676-697` | One answer, after the work of a wrong passphrase. Undoes less of ticket 21 | — |
| R3.4 | A locked stone is unlocked for everyone while the System Administrator's passphrase is still being checked; a failing request can lock a stone another just opened | `MasterKeyManager:186-206` | Check against the candidate key without installing it | — |
| R3.5 | A control character, such as the newline at the end of a file, makes the entropy check treat a secret as binary: keyboard patterns pass. The stored check value then lets a thief of the database try MasterKeys fast | `EntropyVerifier:493-507`, `MasterKey:161-175` | Judge it as text without the control characters; ask for 32 random bytes or stretch the secret | — |
| R3.6 | `publicKey` and `circleKey` have no limit, are returned in lists without pages, and loaded at every request of the account; they pass the storage ceiling | `ProcessMemberRequest:368-374`, `FetchMemberManager:138-150` | A limit as for `secret`; out of the list form. `E2E.md` decided "no limit": **decide** (D6) | LT-abuse |
| R3.7 | Lengths are checked on the trimmed value and the value is stored untrimmed: the limits of `vault` and `secret` are passed, `settings` and `dataName` answer 500 | `AbstractRequest:146-150` | Trim once, and check what is stored | — |
| R3.8 | Any administrator sets `eds.crypto.pbe.iterations` to 999999999: a login with an unknown name then holds a place for minutes, and no one logs in | `SettingManager:277-283`, `AbstractManager:429-432` | A range which can be lived with; a cap on the work for an unknown name | — |
| R3.9 | One account holds the whole pool: uploads and downloads keep a transaction, a connection and row locks for the whole body, up to an hour, and nothing limits how many | `ShareBean:249-314`, `UploadDataManager:123` | A limit per account and for the stone; a body which stands still ends; locks after the first part | LT-abuse |
| R3.10 | After AUTH nothing limits the connections of an account or the bytes of a page; an empty signal costs nothing of its budget | `SocketHub:113-117, 243-259, 679`, `FeatureManager:603-631` | Limits per Member; a page in bytes; a cost per signal | LT-abuse (with R3.1) |
| R3.11 | The source of the MasterKey, its query included, is returned to every administrator with the settings, and lies in the database | `SettingManager:101, 341-348` | Out of the answer: "set" or "not set" | — |
| R3.12 | `INVALIDATE` doesn't ask whether the account is the last Trustee of a Circle, as delete and leave do: the Circle can never be read again | `ProcessMemberManager:559-580` | The same check | — |
| R3.13 | A pending invitation can be made an administrator by `ALTER`; the link then gives an administrator | `ProcessMemberManager:433-460` | Refuse it for a pending account | — |
| R3.14 | An invitation into a Circle can't be seen or withdrawn by the Circle's administrators, and outlives the inviter's own demotion | `ProcessMemberManager:202-214, 714-731` | Listed with the Trustees; withdrawn by them; gone with the inviter's level | — |
| R3.15 | A Trustee added while the Member changes the passphrase is wrapped to the key just thrown away; two accepts of one invitation leave the same | `ProcessTrusteeManager:98-124`, `ProcessMemberManager:516-540, 676-740` | Who wraps to a Member's key holds the Member's row; with R1.4 | — |
| R3.16 | The sanity report without a Circle lists failed objects of Circles where the asker only reads | `SanityManager:243`, `DataEntity:88-94` | The level in the query | — |
| R3.17 | Small ones: a withdrawn status key is remembered again if asked for during the withdrawal; the mark of the database is a plain hash of host and port; the active accounts are counted before the passphrase; the answers of the API carry no `Cache-Control` | `StatusBean:255-261`, `StatusManager:329-357`, `AbstractManager:347`, `CommonService:181` | Each small: after the commit; a keyed hash; after the check; `no-store` | — |

Done when: each has a test of its abuse; the LT-abuse runs show the other users'
throughput and errors unchanged while one account tries.

---

## Phase R4: the stone, wrong answers and races

| # | Issue | Where | Direction | Load test |
|---|---|---|---|---|
| R4.1 | The end of a rotation waits for an upload which began just before it, with the Circle locked: chat and Trustee changes of the room stand still for the length of the upload | `RotationManager:272-300` | Try the key before the Circle lock, or without waiting | LT-race |
| R4.2 | Listing entries prunes old ones without the Circle lock; a list beside an add fails with 500, two lists can deadlock | `FeatureManager:614-620` | Prune under the lock only, or by one statement | LT-race |
| R4.3 | Deleting a Circle fails on a foreign key when content is added meanwhile (seen on PostgreSQL 18) | `ProcessCircleManager:305-326`, `CommonDao:346-370` | Delete the metadata after waiting for the writers, or try again | LT-race |
| R4.4 | `parent_id` has no foreign key and nothing holds a folder: an add beside the delete of its folder orphans the object; `updateData` takes any object as the folder | `ProcessDataManager:226-235, 475-489` | The folder's row held; the type checked; a missing parent borne by the fetch | LT-race |
| R4.5 | A name is not unique in its folder: copy and move don't check, two adds at once both pass, add and rename compare differently; account and Circle names differ by case only | `ProcessDataManager:103, 281-287, 494`, `eds-schema.sql:204, 261` | One check under the folder's lock; a normalised name with a constraint. **Decide** (D8) | LT-race |
| R4.6 | Every JSON read of a small object writes its content row: a lock, a dead row, and readers wait behind an upload | `FetchDataManager:237` | Write only when the status changes | LT-before/after |
| R4.7 | The rotation's batch of entries has no index and reads the stone's whole history for its last batch, with the Circle locked | `RotationDao:92-98` | An index on `(circle_feature_id, id)` | LT-before/after |
| R4.8 | Lists of Trustees load each row's Member, Circle and keys by statements of their own, at authentication too | `TrusteeEntity` | `join fetch` | LT-before/after |
| R4.9 | A streamed copy needs a second connection while it holds the first: as many copies as the pool has connections all fail together | `ContentCopier:209-219` | **Check first**. A limit on streamed copies, or a pool for readers | LT-abuse |
| R4.10 | The hourly sanity run may start while the last still runs, and both take the same objects | `SanitizerSchedule:65` | Skip while one runs; claim the rows | — |
| R4.11 | A failed upload answers the container's 500 without the contract's JSON (a cast of the wrong response); `content()` likewise | `DataService:157-160, 247-249` | Ask for the type; catch in the bean | — |
| R4.12 | `fetchData` by name passes over the Circle given and takes the newest of that name anywhere | `FetchDataManager:135-141` | The Circle in the query | — |
| R4.13 | `updateData` renames to an empty name; `"pageSize": null` throws before authentication, with a stack trace per request | `ProcessDataRequest:379`, `FetchDataRequest:227` | Checks in `validate()` | — |
| R4.14 | After a restart, a status view which asks with `since` gets nothing until the new stone has run as long as the old | `StatusCollector:332-338`, Rune `status.ts:53-58`, Tolk `status/status.go:18-30` | Everything when `since` is ahead of the stone | — |
| R4.15 | One socket frame is at most 64 KiB, not the 256 KiB the documents say; Rune's longest chat messages would close the socket | `SocketEndpoint:51-55`, `application.properties` | **Check first**. Set the frame size, with R3.1; a test over a real socket | — |
| R4.16 | The answer to an add or change comes without its content above 16384 characters | `FeatureManager:436-441`, `SocketHub:440-444` | Announce a copy | — |
| R4.17 | A feature switched to read-only or off while a write waits for the lock still takes the write | `FeatureManager:389-404` | Check again under the lock | — |
| R4.18 | A kind may be about itself; below 16 levels, entries escape removal and closing | `FeatureDeclaration:293-302` | Refuse the cycle, or the depth | — |
| R4.19 | Settings change in memory before the commit and stay when it fails | `SettingManager:293-304` | After the commit | — |
| R4.20 | On H2, a download during the rotation of its object marks it FAILED | `ContentStreamer:140-159` | Compare the key too | — |
| R4.21 | The socket has no ping and no idle time: a lost connection stays "present" | `SocketEndpoint` | **Check first**. A ping by the stone | — |
| R4.22 | A pending invitee as a Trustee or owner fails as a crypto error | `ProcessTrusteeManager`, `MemberKeyPair:79-88` | A 4xx which says so | — |
| R4.23 | The storage ceiling is checked before the Circle's row is held: writes in one Circle pass it together | `StorageCeiling.check` | Under the lock, or written down | — |
| R4.24 | Small ones: `Vary: Origin` only for allowed origins; the inventory inside the transaction of the one-second tick; a body `circleId` left over steers authorization on requests by ID | `OriginFilter:101-107`, `StatusBean:133-166`, `AbstractManager:172-174` | Each small | — |
| R4.25 | Socket calls wait behind checks of access which nothing needs. Every member request but create and invite (a login, a logout, an avatar or settings update too) marks every connection as unchecked. Since ticket 18 what is announced to a connection waits behind its check, on the queue which also handles the connection's own frames; the check is a transaction of its own (`readableCircles`) and waits for the pool. `checkAll` then checks each connection once more. The load test is a closed loop, so the faster REST answers, the more such requests a second: an estimate from the mix of the 16-user report is about 540 checks a second at 2,458 requests a second and about 980 at 4,481, with 160 connections. Found 2026-11-27 by comparing the load test and the stone of 2026-11-22 with this branch; it would explain REST becoming faster while the socket became slower, and is not confirmed by a run. Widens the open point of ticket 28 | `ManagementBean:271`, `SocketHub:336-349, 363-400, 797-803, 1024-1037` | **Check first**: in the reports, the same operation over both ways (`chat.send` and `chat.send.rest`, `chat.history` and `chat.history.rest`), and a run without the account actions; if both ways slowed alike it is the Circle's lock under more writes, not this. Then: check only the connections of the Member concerned, only for what can end an access (logout, passphrase, delete, invalidate, role, a login which ends the oldest Session); no second check when the first covered the version; keep a connection's own frames off the wait where order allows | LT-before/after |

Done when: each has its test; the LT-before/after items have their two runs with
the numbers here; the 4-hour run ticket 38 waits for is one of them.

---

## Phase R5: operating a stone

| # | Issue | Where | Direction |
|---|---|---|---|
| R5.1 | `eds.sh start` offers a new stone, which the first comer may claim, on all interfaces; 8443 always | `eds.sh:802-804` | `127.0.0.1` unless told; 8443 with a certificate only |
| R5.2 | A missing or misspelt `EDS_DB_URL` starts the stone in memory, and the scripts say "ready"; all is lost at the restart | `DatabaseSelection:89-105`, `stone:876-903` | Memory only when asked for by name; refuse a user or password without a URL |
| R5.3 | The start-up sanity run comes before the MasterKey: the stone is "ready" and locked for as long as it reads, and a failure in it stops the start | `StartupBean:119-143`, `DatabaseStartup:150-166` | The MasterKey first; the run in the background, with its own handling |
| R5.4 | The unit of `stone enable` doesn't watch the stone: a JVM which dies stays down | `stone:1140-1148, 1292-1312` | A restart policy, or the container in the foreground under systemd |
| R5.5 | With TLS by the stone itself the scripts fail: they speak HTTP only; a redirect points at the container's port | `eds.sh:847-869`, `stone:1242, 1437` | **Check first**. Scheme and port from the settings |
| R5.6 | Ready is true before the first attempt to start | `StandardSetting:95`, `EDSHealthCheck:54` | False until started |
| R5.7 | Several stones on one database: `RUNNING.md` invites it in one place and forbids it in another. Settings, the MasterKey, rotations and the limits live in one JVM's memory | `Settings:63-85`, `MasterKey:144`, `CircleRotations:48` | **Decide** (D2): one stone per database, enforced; or the shared state designed |
| R5.8 | No word anywhere on backup and restore, on the MasterKey after a rotation and a restart, or on a lost passphrase of the first administrator or a Member | `RUNNING.md` | A chapter "Backup, restore and lost keys" |
| R5.9 | An account made by an administrator keeps the passphrase the administrator knows | `ProcessMemberManager` | **Decide** (D9): a new one at the first login, or written down with the advice to invite |
| R5.10 | `stone check` prints the MasterKey URL and the database URL in full; the administrator's passphrase is taken from the environment | `stone:1017, 1025, 1424`, `eds.sh:1019` | Shortened; a prompt or a file |
| R5.11 | `mvn -Pstone` builds an `eds-stone.jar` with Rune in it; only `eds.sh` checks | `eds-quarkus/pom.xml:242-248, 328-341` | The check in Maven |
| R5.12 | The build fetches and runs Node and the Maven wrapper without a checksum; images by tag; CI's permissions commented out, actions by moving tags | `eds.sh:581`, `.mvn/wrapper`, `Containerfile:22`, `build.yml` | Checksums, digests, pinned actions |
| R5.13 | Tolk's Go modules are in no SBOM or scan; the binaries and Rune's fonts ship without the notices of what they contain; 197 TypeScript and Go files have no licence header | `eds.sh:598-646`, `eds-cli/build.sh` | Tolk in `--sbom`; a file of notices; the headers |
| R5.14 | Small ones: `stone cleanup --images` also takes the builder's image; an empty `STONE_BIND` binds all; `eds.sh restart` forgets `--masterkey`; no pool size is set though the plans quote one | `stone:89-94`, `eds.sh` | Each small |

---

## Phase R6: Rune and Tolk, apart from chambers

| # | Issue | Where | Direction | Load test |
|---|---|---|---|---|
| R6.1 | `tolk chat --plain` loses piped input without a word: a line over 100 KiB ends the reading, and lines sent faster than the stone's budget are dropped | Tolk `commands/chat.go:317-323` | Read the scanner's error; try again when busy; the fake stone gets the budget and the frame limit | LT-abuse (a pipe of 500 lines) |
| R6.2 | Tolk's container and device files: a second Tolk's change is overwritten; no sync before the rename; a damaged nonce panics; errors of the device file are dropped | Tolk `config/config.go:170-223`, `service/device.go:105` | A lock and a new read; sync; lengths checked | — |
| R6.3 | `tolk remove stone` deletes the only copy of the account key without asking | Tolk `commands/stones.go:328-366` | Ask, and show the recovery key | — |
| R6.4 | The invitation link is taken from the command line only and can be read by others on the host until it is used | Tolk `commands/account.go:170-218` | From a prompt or stdin; use it before the long prompts | — |
| R6.5 | `service.Stone` is used by several goroutines without a lock (the token, the caches) | Tolk `service/stone.go:149-274` | One lock; `go test -race` over the overlap | — |
| R6.6 | The agent holds its lock over a login to a stone which doesn't answer; `--lifetime` doesn't count the time asleep | Tolk `agent/agent.go:75, 132-178` | Log in outside the lock; compare the clock | — |
| R6.7 | Tolk's socket doesn't notice a dead peer, and its reader can stand still for 30 seconds behind its own events | Tolk `socket/socket.go:94-138` | Deadlines; answers off the reader's path | — |
| R6.8 | A stone can make Tolk read pages without end | Tolk `client/api.go:109-129`, `socket/socket.go:337-352` | A bound | — |
| R6.9 | Tolk takes an `http://` stone without a warning | Tolk `commands/stones.go:35` | Say so | — |
| R6.10 | Rune's opened copies of chamber files stay in the browser's storage when the Session ends or the tab closes | Rune `e2e/files.ts:113`, `downloads.ts:250` | Swept at start and at the end of a Session | — |
| R6.11 | Rune's calendar export leaves a lone carriage return | Rune `features/calendar.ts:127-129` | **Check first**. Replace it | — |
| R6.12 | The fake stone differs from the stone where it matters; its rules of permission were not compared | Tolk `internal/fakestone` | Compare with the managers; with R6.1 | — |

---

## Phase R7: the API contract

| # | Issue | Where | Direction |
|---|---|---|---|
| R7.1 | The contract still speaks of the built-in account `admin`, of a refused rename, and of an invitation which may be an administrator | `openapi/overview.md:14`, `operations/master-key.md`, `members.md:32, 100` | The texts as the code is |
| R7.2 | Names of Data Objects are "up to 75 characters"; the code and schema take 512 | `overview.md:182`, `data.md` | One number |
| R7.3 | The content operations declare 4xx only; they answer 500, 503 and 507 | `openapi.json` | Declared |
| R7.4 | `x-eds-trust-level` gives the first check only: many operations need more than it says | `OperationDocumenter:152` | The real level, or another name |
| R7.5 | The completeness test checks that a text is there, not that it is true; parameters aren't checked; a service missing from `EDSApplication` wouldn't be noticed | `OpenApiCompletenessTest:270-361` | Parameters as properties; every `@Path` class registered |
| R7.6 | The pattern of an ID is published without anchors | `openapi.json` | Anchored |
| R7.7 | No test keeps the default of `eds.show.trustees` (ticket 10) | `StandardSetting:40` | One test of the default |
| R7.8 | The key hierarchy of the server is tested against itself only (MasterKey subkeys, the wrap of Member keys, the HMACs) | `MasterKeyTest:61`, `MemberKeysTest` | Vectors from an independent script, as for the data format |

---

## Phase R8: the documents

| # | Issue | Where | Direction |
|---|---|---|---|
| R8.1 | The claim: "not by the person who runs the stone", "the server … can't open them", "EDS may run on untrusted hosts", beside "the stone sees the content while it handles it" | `SECURITY.md:16-19, 44-45, 58-60`, `design/README.md:17`, `README.md:180`, `PLAN.md:774`, `E2E.md:18-20` | After D1 and D3: for rooms, "at rest, and against the administrator's API"; what a chamber adds, and against whom |
| R8.2 | "Hidden names: built" and "one weak spot … fixed" hold for Rune only (ticket 43) | `design/README.md:36, 58`, `SECURITY.md:5` | Said as it is |
| R8.3 | "These states build and pass their tests … everything after the last", while Tolk's test fails | `PLAN.md:77-79`, `TOLK.md:5` | After R0 |
| R8.4 | Phase records which were reversed later and still read as the present: the ETag at a rotation; who chooses the Session token; who manages Data Types; "keys stay server-side"; "no CORS"; the 1 GB limit; spooling uploads; signal length; one Session per account | `PLAN.md:454, 624, 648, 1294-1313, 1630, 2455, 2526`, `TOLK.md:40, 184, 604`, `FEATURES.md:622` | A note at each which names the later decision |
| R8.5 | Tickets 19 and 53 say what isn't so: `GET /version` (it is POST), "one object, at most 60 min" (R3.2), Sessions "wrapped anew" (they are ended). 53 is also worse: one row left on an old MasterKey stops every later rotation | `FINDINGS.md` | Corrected |
| R8.6 | The root `README.md`: after an invalidation "it is possible to log in"; "no third-part dependencies"; a release plan of Java 8 to 11; only "a breaking update" for no migration | `README.md:25, 199-217` | Rewritten |
| R8.7 | Settings: `eds.data.max-inline-size` for `eds.data.max.inline.size`; `eds.crypto.*`, `eds.system.locale`, `eds.sanity.check.startup` in no document; the Session timeout, `eds.show.*` and others missing from `RUNNING.md`; H2 2.3.232 beside 2.4 | `RUNE.md:317`, `RUNNING.md`, `application.properties` | One table in `RUNNING.md`, checked by a test against `StandardSetting` |
| R8.8 | The cost of content in PostgreSQL with a yearly rewrite of every Circle isn't said: WAL, backup, Circles made together coming due together | `RUNNING.md:185` | Sizing; the due time spread |
| R8.9 | Comments which say otherwise than the code: who deletes a Circle, who may be its first administrator, "session key chosen by the client", the zeroing of keys, the WildFly counter, 600.000 iterations | `ProcessCircleManager:99, 296`, `MemberService:126`, `Crypto:136`, `MasterKey:207`, `SECURITY.md:417` | With ticket 34 |

---

## Decisions needed

| # | Question | Blocks |
|---|---|---|
| D1 | Chambers against a stone which lies: close it in the clients (no key without an accepted fingerprint; or signed lists of Members, already open in `E2E.md`), or narrow the claim to a stone which only reads. And whether entries are bound to their author | R2, R8.1 |
| D2 | One stone per database, enforced at start; or several, with settings, MasterKey, rotations and limits shared | R5.7 |
| D3 | What a download token seals instead of the Session token | R3.2, R8.1 |
| D4 | The schema version: it was decided on 2026-11-20 to stay 4 until 2.0 is released. A `master` database is then taken for good. Raise it at the merge, or a mark of the schema | R1.12 |
| D5 | A rotation of the MasterKey as a request of its own, which changes the contract, Rune, Tolk and both scripts | R1.11 |
| D6 | Limits for `publicKey` and `circleKey`, against the decision "no limit" in `E2E.md` | R3.6 |
| D7 | What "the build is green" runs: FitNesse, PostgreSQL, Tolk, Rune; what the 95 % covers; tests in CI | R0.8 |
| D8 | Names: unique in a folder for copies and moves too; case of account and Circle names | R4.5 |
| D9 | An account made by an administrator: a new passphrase at the first login | R5.9 |

## What was found sound

So it is not looked for again: the chunked format of content (nonce, AAD, the last
chunk, ranges); the hybrid KEM and its binding; the separation of the derived keys;
comparisons in constant time; the random source; authorization by ID on every data
path; the hidden folders of features; what a Session may not do (ticket 7); the
first administrator's place; the accounting of stored bytes on paths which don't
overlap; the lock order; `EncryptedDataStore`; the hub's ordering and its events
after the commit; the socket's codec; `ByteRange` and `ContentPolicy`; Rune's DOM,
CSP, service worker and its dependencies; Tolk's primitives beside Rune's, byte for
byte, its WebSocket and its agent's socket; the vendored modules. Against `master`,
every removal is acknowledged in a plan, and no check was dropped without another
in its place.

## Not covered by the review

- Rune's build, the click and flow tests, FitNesse and the load test were not run.
- Tolk's TUI beyond its I/O; `qr`, `format`, `theme`; Windows and macOS.
- Rune's panes for calendar, plans and location beyond how they open an entry.
- The fake stone against the managers, line by line (R6.12).
- Whether the socket negotiates compression.
- Each race, against a running stone: that is what the load tests here are for.
