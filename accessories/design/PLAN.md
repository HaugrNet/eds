# EDS 2.0 plan: the backend

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds
Base package: `net.haugr.eds`

Status: done. OpenAPI 2026-11-01, Crypto and Streaming 2026-11-02, Clients API
2026-11-04 to 2026-11-08, the server changes made while the clients were built until
2026-11-11, and the fixes of the review of 2026-11-13. What is still open is listed
at the end of each chapter. *Crypto* C6, rotating a Circle Secret, was added and
built on 2026-11-21; the corrections after the load and security tests followed
2026-11-22 to 2026-11-24 (*Streaming*, "The storage ceiling: a total for each
Circle"; `SECURITY.md`). The findings still open are in `FINDINGS.md`.

EDS 2.0 has three plans, one per part:

| File      | Part                                                                      |
|-----------|---------------------------------------------------------------------------|
| `PLAN.md` | The backend: `eds-api`, `eds-core`, `eds-quarkus` (the server), `eds-testing` |
| `RUNE.md` | Rune, the PWA in `eds-quarkus/src/main/webui`                              |
| `TOLK.md` | Tolk, the CLI in `eds-cli`                                                 |

The backend comes first; Rune builds on its contract, and Tolk on the contract and
on files it shares with Rune.

Since 2026-11-15 three more plans cross all three parts: `E2E.md` (rooms and
chambers: end-to-end encryption which the stone takes no part in), `FEATURES.md` (the
chat, a calendar, location and plans as features by declaration) and `STATUS.md` (the
status of a stone, for those who look after several). Their order of work is in
`FEATURES.md`. All three were built on 2026-11-15, in the backend, in Rune and in
Tolk; each phase says what was built, what differs and what wasn't checked. They
hold the design, the backend and the convention of the clients; what Rune and Tolk
do, and their phases, are in `RUNE.md` and `TOLK.md` since 2026-11-17.

This file sets the order of the backend's work and holds its four chapters, done in
this order:

| Order | Chapter       | Phases | Covers                                                         |
|-------|---------------|--------|----------------------------------------------------------------|
| 1     | *OpenAPI*     | O1–O5  | Complete generated OpenAPI spec: docs, schemas, constraints, return codes, errors, permissions; completeness test |
| 2     | *Crypto*      | C1–C6  | MasterKey, key hierarchy, algorithms, sessions, data format, removal of RSA and signatures; rotating a Circle Secret (C6) |
| 3     | *Streaming*   | S1–S5  | Streaming upload/download, ranges, storage access, transactions, safe serving |
| 4     | *Clients API* | L1–L5  | OpenAPI contract, ID-based resource API, acceptance tests on a generated client, chat, download tokens and thumbnails, and what the clients needed afterwards |

Until 2026-11-13 the chapters were files of their own (`OPENAPI.md`, `CRYPTO.md`,
`STREAMING.md`, and the server part of `CLIENTS.md`). The names of the phases and
sections are unchanged, so "Crypto §4" or "L4" mean what they always did. The
phases L5 (Rune) and L6 (Tolk) continue in `RUNE.md` and `TOLK.md`.

## Handed over for review

The work was built phase by phase, as the chapters tell it, over 160 commits in which
backend, Rune and Tolk often changed together. For the review it is handed over
anew, as fourteen commits on the branch `feature/refactor`, on top of `master`
(rearranged on 2026-11-26, at the user's request): eight slices of the backend by
layer, one commit of everything that changed the backend after them, and then Rune
and Tolk, each as its plan and what was built from it, and what operates all three:

| # | Commit | Holds |
|---|--------|-------|
| 1 | Plans | `PLAN.md`, and `E2E.md`, `FEATURES.md`, `STATUS.md` without the parts of Rune and Tolk |
| 2 | Crypto | `eds-core`: the package `jce` and the enums of the algorithms |
| 3 | Database | `eds-core`: the schema, the entities and DAOs, the storage of encrypted content in parts (`model`) |
| 4 | Configuration and start | the settings, `StartupBean`, `eds-quarkus` (the choice of the database, health, the schedule, the profile `stone`), the POMs, the container, `RUNNING.md` |
| 5 | API: the contract | `eds-api` (requests, responses, DTOs, `openapi.json`) and the description of the API in `eds-core` (`services/openapi`) |
| 6 | API: REST | `eds-core`: the services, the managers behind them, streaming (`stream`), the status, the declarations of features |
| 7 | API: the socket | `eds-core`: the package `socket`, and its frames in `eds-api` |
| 8 | Tests | the tests of `eds-api`, `eds-core` and `eds-quarkus`, and `eds-fitnesse`; `eds-client`, the Java client FitNesse used, goes |
| 9 | The backend after the slices | #68 (the lock of an account after too many failed passphrase attempts); the corrections after the load and security tests of 2026-11-22/23 (tickets 5–11, 14, 20, 21, 23–25); `eds-fitnesse` renamed `eds-testing`, with the load test's PostgreSQL observer and its stone in a container (ticket 29); the throughput under load (tickets 28, 35–37); the load test as the clients ask (38); the socket's limits before AUTH and its checks of access (18, 22), Data Types by administrators only (15), a new MasterKey installed after its commit (13), and one lock order for the whole stone (1–3); the stone alone in operation (`eds.sh`, `accessories/podman/stone`, CI); the documents checked against the code, and `FINDINGS.md` |
| 10 | The plan of Rune | `RUNE.md`, with Rune's parts of the three crossing plans |
| 11 | Rune | `eds-quarkus/src/main/webui`, its build (Quinoa) and headers, its test, the brand; and `EdsStoneTest`, which checks that the stone alone (`-Dstone`) leaves it out |
| 12 | The plan of Tolk | `TOLK.md`, with Tolk's parts of the three crossing plans and the runs of both together |
| 13 | Tolk | `eds-cli`, the Maven profile `cli` |
| 14 | Operating Rune and Tolk | what `eds.sh`, `README.md`, `RUNNING.md`, `accessories/podman/stone` and CI need for Rune and Tolk: building and installing them, EDS with Rune beside the stone alone, the clients against a stone |

- **The commits are slices, not steps.** A file is in one commit, whole, by the
  layer it belongs to; the state after a commit never existed as such. They are
  meant to be read one layer at a time, not to be run. These states build and pass
  their tests: the backend by itself after commit 8 and again after commit 9, and
  everything after the last.
- **A change of the database is in commit 3 whichever chapter asked for it**: the
  columns for the keys (*Crypto*), the stored content (*Streaming*), the entries of
  features, the invitations, the status. The chapters say why; the schema version
  and what each version added are in *Clients API*, "After L5". The exceptions are
  in commit 9: the columns of #68, and the table `eds_circle_storage` and three
  indexes (*Streaming*, "The storage ceiling: a total for each Circle").
- **Rune and Tolk follow the backend**, each as its plan and then what was built
  from it, so each can be taken to a branch of its own with both.
  Rune needs commits 1 to 9; Tolk needs those and Rune's, as it keeps copies of
  files Rune owns (`RUNE.md`, "Shared with Tolk").
- **Some files have parts of several commits** and grow with them: the root
  `pom.xml` (Quinoa's version with Rune, the profile `cli` with Tolk),
  `eds-quarkus/pom.xml` and `application.properties` (Quinoa, the TypeScript
  client and the headers of the web app with Rune), `RUNNING.md` (the web app with
  Rune, the command line with Tolk, the clients with commit 14), and `eds.sh`,
  `README.md`, `accessories/podman/stone` and the CI build (the stone alone with
  commit 9, Rune and Tolk with commit 14). Commit 9 changes files of commits 1 to 8
  where its changes need it.
- The names of the phases (O1 to O5, C1 to C6, S1 to S5, L1 to L6, N1, E1 to E6, T1
  to T6, M1 to M3) are those of the plans and of comments in the code. They say when
  and why something was built, not which commit holds it.

## Rules

- Finish one phase before starting the next; each phase has its own "done when"
  criteria.
- After each phase: `mvn clean install` passes, and a short summary of what changed
  and any deviations from the plan.
- When chapters overlap, the earlier chapter decides: *OpenAPI* for how the API is
  documented, *Crypto* for anything crypto, *Streaming* for data transfer and
  serving content. Later chapters, and the plans of the clients, refer to them instead of redefining.
- From *OpenAPI* O5 on, every phase that changes the API updates the OpenAPI
  documentation in the same phase; the completeness test keeps the build honest.
- EDS 2.0 is a clean break: no in-place upgrade from CWS or EDS 1.x
  (*Crypto* Non-goals).
- Plain Java/JCE on the server; no new third-party libraries without agreement.

### Definition of done (2026-11-22)

The user's rules for every change, of the backend, Rune or Tolk, whoever makes
it and on whichever computer. A change which doesn't meet all of them isn't done,
isn't committed as done, and isn't described as done.

1. **The build is green.** `mvn clean install` of every module passes from a clean
   tree (`./eds.sh build --test` for all three parts). Nothing is left for later
   with a failing step. Tolk's tests are part of it (`./eds.sh build tolk --test`).
2. **All tests pass.** No failure and no error. A skipped test is skipped for a
   stated reason only: the tests of PostgreSQL without a PostgreSQL database, and
   those which read Rune's sources in a tree without Rune.
3. **Coverage is at least 95 %**, overall and of the new code, as JaCoCo measures
   it for all modules together (`eds-quarkus/target/jacoco-aggregate`, which
   SonarQube reads). Testing is where bugs are caught; a change brings its tests.
4. **SonarQube finds nothing new.** Every build is analysed by the user's own
   SonarQube (`https://dawn.dk/sonar`, which checks more than SonarQube Cloud), and
   its quality gate passes. Known issues in code a change touches are fixed with
   it. An issue which seems a false positive is put to the user, not marked so.
5. **The review is thorough.** Before a commit, a review of the consequences, not
   only of the diff: a change in one place can break another. It follows every
   caller and every other way into the same logic (all endpoints which check a
   passphrase, not only the one changed), the transactions and what runs at the
   same moment, the data model, the contract, and what Rune and Tolk rely on. It
   is made by a reviewer which didn't write the change. Each finding is fixed, or
   written down as open with the reason (`SECURITY.md` for security, the plan of
   the part otherwise).
6. **A bug gets a test which fails without its fix**, and a security rule gets
   tests of its abuse: another account, the same moment, values out of range.
7. **What is claimed was checked.** A commit message or a plan says what was
   verified, with the numbers (tests run, coverage, the quality gate); anything
   assumed is called an assumption.
8. **Decisions are written into the plans** in the same commit as the change, as
   work goes on from more than one computer; a note kept elsewhere is lost there.
9. **The branches are the user's.** History is rewritten or branches made, moved
   or pushed only when the user asks, and only as asked.

## Why this order

- **OpenAPI documentation first**: it changes no behaviour, so it's low-risk, and it
  gives the later chapters a complete spec plus a test that keeps it complete while the
  API changes. It also gives *Clients API* L1 a complete starting point.
- **Crypto next**: it changes the storage format, the key hierarchy and the
  authentication model (sessions). Everything else builds on that, and with a clean
  break there's no reason to build features on the old format first.
- **Streaming third**: it needs the per-object keys (C4), the chunked format (§4)
  and sessions (C3), and it adds the last server API changes (content endpoints).
- **Clients API last**: the contract (L1) is written once, against the final API, instead
  of being rewritten after the crypto and streaming changes.

## Sequence

### Step 0: Baseline

Before O1:
- Run the existing unit tests and FitNesse suites (`eds-fitnesse`, now `eds-testing`, on `eds-client`)
  and record the result. They're the regression safety net during the crypto and
  streaming work. (The first version of the plan for the clients put acceptance tests before
  the crypto upgrade for this reason; the existing suites fill that role now.)
- Fix the open transaction issue: `ManagementBean`/`ShareBean` catch `EDSException`
  without `setRollbackOnly()`, so partial writes commit. Both the crypto re-wrapping
  (C4) and streaming uploads (S2) depend on rollback working.

Done 2026-11-01. Recorded baseline:

| Suite                         | Result                                              |
|-------------------------------|-----------------------------------------------------|
| `eds-api` unit tests          | 171 passed                                          |
| `eds-core` unit tests         | 421 passed (426 after the rollback fix, +5 new tests) |
| `eds-quarkus` tests           | 14 passed                                           |
| `eds-client` tests            | 16, of which 13 skipped (need a running server)     |
| `eds-fitnesse` unit tests     | 4, of which 3 skipped                               |
| FitNesse `EDS` suite (H2)     | 342 right, 15 wrong; same before and after the fix  |

The 15 FitNesse failures already existed before this work:
- 3 in the MasterKey table: the page sets a weak MasterKey secret, which the
  `EntropyVerifier` rejects since commit 696d247d. *Crypto* C2 rewrites this
  table anyway.
- 12 in the settings table: `fetchSettings` reports the crypto/system keys as
  "undefined", because the schema no longer seeds `eds_settings` (commit 40604cc0).
  To be decided: should `fetchSettings` return the code defaults, or should the page
  stop expecting them? (Several of these settings, e.g. the RSA and signature
  algorithms, disappear in *Crypto* anyway.)
  Resolved 2026-11-02: the page now only shows the values (FitNesse `show` instead
  of `check`). The actual cause was that the table authenticated with the weak
  credential `admin` instead of `${ADMIN_PASSPHRASE}`, so the request failed;
  `fetchSettings` does return the defaults. With both fixed, the suite has no
  failures (335 right, 0 wrong).

The rollback fix marks the JTA transaction rollback-only in every `EDSException`
catch block (`Transactions.markRollbackOnly`). One side effect was checked: an
expired session is no longer removed when it's used, because that removal is now
rolled back with the failed request. The hourly sanitizer already clears expired
sessions (`SanityManager.clearExpireSessions`), so nothing is lost.

### Step 1: OpenAPI

| Phase | Content                                               | Notes |
|-------|-------------------------------------------------------|-------|
| O1    | Groundwork: annotations in `eds-api`, `OASFilter`, completeness test (report mode), OpenAPI 3.0.3 output | Signature operations only marked deprecated |
| O2    | Global model: overview, authentication, HTTP-200 response model, generated return-code table, framework errors | |
| O3    | Schemas: descriptions, constraints, examples; response types referenced | |
| O4    | Operations: fields used, permissions, return codes, examples | Small code/doc inconsistencies fixed here |
| O5    | Completeness test fails the build; release spec validated | From here on, API changes must keep the spec complete |

No behaviour changes in this step.

### Step 2: Crypto

| Phase | Content                                               | Notes |
|-------|-------------------------------------------------------|-------|
| C1    | Remove RSA and signatures; HMAC invitation tokens     | Signature suites and fixtures are deleted, not ported. Done 2026-11-02; RSA member keys stay until C3 |
| C2    | Crypto API cleanup (explicit nonce/AAD); MasterKey: HKDF, version + KCV, random IVs, no fallback | Done 2026-11-02; without a source EDS stays locked |
| C3    | Member keys (ML-KEM-1024 + P-384), sessions           | Sessions are required by streaming. Done 2026-11-02; hybrid KEM moved here from C4 |
| C4    | Circle Secret, trustees, HKDF object keys, §4 data format (chunk size, header, test vectors) | Data is written in the final format from here on. Done 2026-11-02; chunk size 64 KiB |
| C5    | Streaming crypto API: encrypting stream, range decryptor, re-encryption helper | Crypto side only; storage and endpoints are in *Streaming*. Done 2026-11-02 |
| C6    | Rotating a Circle Secret: on removal and at intervals, a background job; a big-data test on PostgreSQL | Done 2026-11-21 |

The FitNesse suites change along the way (signature fixtures removed, sessions
added), but stay on `eds-client` until L3.

### Step 3: Streaming

| Phase | Content                                               |
|-------|-------------------------------------------------------|
| S1    | Storage access: streamed writes, batched range reads, schema changes. Done 2026-11-02 |
| S2    | Upload endpoint. Done 2026-11-02                      |
| S3    | Download, ranges, safe-serving headers. Done 2026-11-02 |
| S4    | JSON inline limit, copy/move, sanity job. Done 2026-11-02 |
| S5    | `eds-client` methods and FitNesse suites for streaming. Done 2026-11-02 |

Endpoints are added code-first (annotations), like the rest of the current API; L1
turns them into contract.

### Step 4: Clients API

| Phase | Content                                               |
|-------|-------------------------------------------------------|
| L1    | OpenAPI contract for the final API, ID-based resource API. Done 2026-11-04 |
| L2    | Serve the contract at runtime; path layout (reduced: the server stays code-first). Done 2026-11-05 |
| L3    | FitNesse on the generated Java client; retire `eds-client`. Done 2026-11-05 |
| L4    | Live chat over WebSockets (encrypted storage). Done 2026-11-06 |
| L5    | Server side of the PWA: download tokens and thumbnails. Done 2026-11-08. Rune itself: `RUNE.md` |
| after | What Rune and Tolk needed from EDS, 2026-11-08 to 2026-11-11, and the review fixes of 2026-11-13: see "After L5" in *Clients API* |
| L6    | Tolk: `TOLK.md` |

## Decisions that cross chapters

Points that more than one chapter or plan depends on. Each is decided in the
chapter named first.

- **OpenAPI version**: decided, 3.0.x (3.0.3) for now; a move to 3.1 is a later
  step. Configured in *OpenAPI* O1; the *Clients API* contract and generators
  follow it.
- **HTTP status codes vs. the HTTP-200 model**: documented as is in *OpenAPI*;
  decided in *Clients API* L1 (2026-11-04): the JSON API keeps HTTP 200. The
  streaming endpoints and the ID-based resource API use real status codes.

- **Chunk size** of the data format: decided in *Crypto* C4, 64 KiB; affects
  *Streaming* range reads.
- **Test or production system**: *Clients API* (2026-11-10). The version operation
  reports `database` and `persistent`; the PWA and the CLI both show a test system
  (in-memory H2) as such.
- **Download token or session cookie** for media elements: decided in *Clients API*
  L5, a download token per object.
- **Server-side spool for uploads without `Content-Length`**: *Streaming*; decided
  2026-11-10, done 2026-11-11 for the CLI (`TOLK.md`, L6b) so true pipes work
  with `tolk send -`; `LENGTH_REQUIRED` (411) is gone from the contract.
- **Maximum object size** (`eds.stream.max.object.size`, within the HTTP body limit;
  content is stored in parts, so no field limit applies): *Streaming*; affects PWA
  and CLI limits.
- **Chat and thumbnail storage**: *Clients API*, but must use the *Crypto* §4
  format. Chat decided in L4, rebuilt as a feature since 2026-11-15 (`FEATURES.md`):
  an entry per message (`eds_entries`, with a Data Object of Data Type `entry`) in the
  hidden `.chat` folder of the Circle. Thumbnails decided in L5: made by the browser, stored
  encrypted as a child of their Data Object.
- **Path layout**: *Clients API* (2026-11-10). Rune at `/`, the REST API and the socket
  (`/api/v1/socket`, the chat and the other features) below `/api/v1`, health at `/q/health` on the management port.
- **The System Administrator has no set name** (2026-11-18, asked for by the
  user: a default name is left as it is as a default passphrase is). The account
  `admin` was made by whoever first used that name, and was known by the name: it
  could not be renamed, the name was reserved, and only it could give the
  MasterKey. Now a stone without accounts makes whoever comes first its System
  Administrator, under the name given (`AbstractManager.claimStone`; those who
  come at the same moment are let in one at a time). It is known by being the
  first account, the one with the lowest Id of its role
  (`systemAdministrator()`): that one can't be deleted or lose its role, and only
  it gives the MasterKey, as before; it may change its name, and no name is
  reserved. Once a stone has an account, an unknown name is refused, `admin` as
  any other. `Constants.ADMIN_ACCOUNT` is only what the tests and examples use.
  Nothing changed in the API or the schema, and Rune and Tolk know the System
  Administrator by its role, not its name. `eds.sh` and `stone` ask for the name
  for `masterkey load`.
- **Several administrators** (2026-11-18, decided by the user). A stone may have
  more than one System Administrator; the first (above) has a place no other has
  ("God-like", in the user's word): besides the MasterKey, it alone creates an
  administrator, changes a role and deletes an administrator
  (`ProcessMemberManager`, `ONLY_FIRST_ADMINISTRATOR`). Other administrators create
  and delete users and run rooms and settings. **An invitation always makes a
  user**: one which asks for `ADMIN` is refused, as a stolen link must not give
  more; an account with a passphrase goes through channels the administrator
  controls, so that may be an administrator. The member data has
  `firstAdministrator`, so Rune and Tolk mark the first and offer the choice of role
  only to it (`RUNE.md`, `TOLK.md`).
- **Schema version**: 4, the version of EDS 2.0 on `master`, and it stays 4 until
  2.0 is released, however the tables change while it is made (the user decided
  2026-11-20; it had been raised with each change, to 16). EDS 2.0 has no upgrade
  in place from 1.x: a database of another schema version is refused at startup. A
  database of an earlier 2.0 build has the same version and isn't recognised; it
  needs to be created anew. Each chapter says what it changed in the tables.
- **An index for each foreign key** (2026-11-20), as PostgreSQL makes none by
  itself: 11 were missing. The 5-hour load test on PostgreSQL found it: deleting a
  room with 19,441 Objects took 205 s instead of 2 s and ran into the 60 s timeout
  of the transaction. `SchemaIndexTest` keeps the rule.

## OpenAPI

Status: done (O1–O5, 2026-11-01); see the "Done" notes under each phase.
Scope: make the generated OpenAPI specification a complete description of the EDS
API: operations, request and response models, field rules, return codes, errors,
authentication and permissions. Everything a client developer needs should be in the
spec, without reading the Java sources.

Since L1 the spec is the checked-in contract `eds-api/src/main/resources/openapi.json`
(it was `accessories/release/openapi.json` while this chapter was done, and the text
below says so where it describes that time). EDS serves it at `/api/v1/openapi.json`.

### Why first

- The later chapters change the API (signatures removed, sessions, streaming endpoints).
  Once the spec is complete and a test enforces it, each of those changes has to keep
  it complete, instead of documentation being caught up at the end.
- *Clients API* L1 turns the spec into the hand-maintained contract. Starting from a
  complete generated spec makes L1 mostly a move, not a rewrite.

### State before O1 (2026-11-01)

The spec is generated code-first at build time: `smallrye-open-api-maven-plugin`
(4.2.0) in `eds-core` scans the MicroProfile OpenAPI annotations on the REST services
and writes `target/generated/openapi.json`, which is copied to
`accessories/release/openapi.json`.

Measured on the current `accessories/release/openapi.json`:

| Part                  | State                                                        |
|-----------------------|--------------------------------------------------------------|
| Operations            | 35, all with summary, description and tag                    |
| Responses             | only `200 "Successful operation"`, with an **empty schema** (`{}`): the services return `jakarta.ws.rs.core.Response`, so no response type is known |
| Response models       | missing from `components/schemas` entirely                   |
| Schemas               | 22 (requests and DTOs), **none** with a description          |
| Properties            | 100, **none** with a description, no constraints             |
| Return codes          | not documented; `returnCode` is a bare integer               |
| Authentication        | not described (it's in the request body, see below)          |
| Spec version          | OpenAPI 3.1.0 (target: 3.0.x, see "Spec version")            |
| Runtime               | not served by the running server (`eds-quarkus` has no OpenAPI extension) |

Where the missing information lives today:

- `eds-api`:
  - Javadoc on requests, responses and DTOs (mostly short, e.g. "Account Name.").
  - Operation-level Javadoc on the `Share` and `Management` interfaces, often more
    detailed than the `@Operation` texts in the services.
  - `ReturnCode`: name, numeric code, classification (info/warning/error) and
    description for every code; `ReturnCode.Classification` explains the three
    classes.
  - Enums with value docs: `Action`, `CredentialType` (`PASSPHRASE`, `SESSION`),
    `MemberRole`, `TrustLevel`.
  - Field rules: each request's `validate()` method (required fields per action,
    maximum lengths, ID format) and `Constants` (`MAX_NAME_LENGTH` = 75,
    `MAX_STRING_LENGTH` = 256, `MAX_PAGE_SIZE` = 100, `ID_PATTERN_REGEX`).
  - Encoding: `ByteArrayAdapter` serialises `byte[]` fields as Base64 strings.
- `eds-core`:
  - The REST services (`@Operation`, `@Tag` texts) and `CommonService.runRequest`,
    which defines the response model: **every request returns HTTP 200**, and the
    outcome is in `returnCode`/`returnMessage` ("EDS is not an HTTP service").
  - Exceptions and the return codes they map to (`AuthenticationException` →
    `AUTHENTICATION_WARNING`, `CryptoException` → `CRYPTO_ERROR`, …); any other
    runtime exception becomes `ERROR`.
  - `Permission`: the trust level each action requires; actions with
    `TrustLevel.SYSOP` are restricted to the System Administrator.
  - `StandardSetting`: the setting keys and defaults handled by the settings
    endpoint (no descriptions in the code yet).

### Goals

1. Every operation documents: purpose, request schema, the fields it uses and which
   are required, the response schema, required role/trust level, and the return codes
   it can produce.
2. Every schema, property and enum value has a description; properties carry their
   constraints (max length, pattern, format, nullability) and an example.
3. The return-code model is explained once, centrally, with a complete table generated
   from `ReturnCode`, so it can't drift from the code.
4. Authentication, the HTTP-200 response model and framework-level HTTP errors are
   described in the spec.
5. A test fails the build when the spec is incomplete, so later chapters keep it
   complete.

### Non-goals

- Changing API behaviour. This plan documents the API as it is (bugs found along the
  way are listed and fixed separately, see Phase O4). Moving to real HTTP status
  codes, per-operation request schemas or the ID-based API belongs to *Clients API*.
- Switching to contract-first (*Clients API* L1/L2).
- Documenting the signature operations in depth: they're removed in *Crypto* C1.
  They're marked `deprecated: true` with a note, nothing more.

### Approach

#### Where documentation lives

- **DTOs and enums (`eds-api`)**: `@Schema` annotations on classes, fields and enum
  values. This needs `org.eclipse.microprofile.openapi:microprofile-openapi-api` in
  `eds-api`, scope `provided`: it's annotations only, already managed in the parent
  POM and used by `eds-core`, so it adds no new library. Clients using `eds-api` don't
  need it at runtime (missing annotation classes are ignored by the JVM).
- **Operations (`eds-core` services)**: `@Operation`, `@APIResponse` with
  `content = @Content(schema = @Schema(implementation = XxxResponse.class))`,
  `@RequestBody` descriptions, and `@Extension`s for EDS-specific metadata (below).
- **Generated parts**: an `OASFilter` in `eds-core` (configured on the SmallRye plugin)
  adds what is derived from code, so it can't drift:
  - the return-code table from `ReturnCode`,
  - the trust level per operation from `Permission`,
  - the settings table from `StandardSetting`.
- **Static base document** (optional): a `META-INF/openapi.yaml` in `eds-core` that
  SmallRye merges with the annotations, for long texts that don't belong on a class
  (overview, concepts). Use it only if `info.description` becomes unwieldy as an
  annotation.
- **Javadoc vs. spec text**: the `@Schema`/`@Operation` text is the client-facing
  documentation; Javadoc stays for developers and may be shorter. Where the
  `Share`/`Management` Javadoc says more than the current `@Operation` text, the
  content moves into the annotation.

#### Global documentation

- `info.description`: what EDS is (members, circles, trustees, data, data types), the
  encryption model in short (keys stay server-side; reference to *Crypto* once
  implemented), authentication, the response model, and how to read return codes.
- **Authentication**: credentials are sent in the request body of every
  authenticated request (`accountName`, `credential` as Base64, `credentialType`
  `PASSPHRASE` or `SESSION`). There's no header-based scheme, so no
  `securitySchemes` yet; that changes with sessions in *Crypto* C3.
- **Response model**: every EDS operation answers HTTP 200 with an `EDSResponse`
  subtype; success is `returnCode` 200, warnings are 4xx-style codes, errors are
  5xx-style codes. Document `EDSResponse` as the base schema and every response
  schema as extending it (`allOf`).
- **Return-code table** (generated): name, numeric `returnCode`, classification,
  description, and typical causes. Exposed both in the `returnCode` property
  description and as an `x-eds-return-codes` extension on `EDSResponse` for tools.
- **Framework-level HTTP errors**, which don't use the EDS response body: malformed
  JSON (400), missing body on `@NotNull` parameters (400), unknown path (404),
  wrong media type (415), unexpected server errors (500). Documented as shared
  responses in `components/responses`.

#### Per-operation documentation

For each operation:

- Description reconciled with the `Share`/`Management` Javadoc.
- **Fields used**: requests like `ProcessMemberRequest` are shared by several
  endpoints and the endpoint sets `action`. The description lists which fields this
  endpoint reads, which are required (from `validate()`), and that `action` is set by
  the endpoint and ignored in the body.
- **Permissions**: required role (System Administrator only, for `SYSOP` actions) and
  trust level in the circle (from `Permission`), as text and as `x-eds-trust-level`.
- **Return codes**: the codes this operation can return, with the condition for each,
  as text and as `x-eds-return-codes`. Found by reading the manager behind the
  operation (`throwConditional…`, exception types, validation).
- Response schema `$ref` to the concrete response type.
- Examples for request and response of the main flows (create member, login, create
  circle, add trustee, add/fetch data).

#### Schemas

For every request, response, DTO and enum:

- Class description; property descriptions that say what the field means, not just
  its name.
- Constraints from `validate()` and `Constants`: `maxLength`, `pattern` for IDs
  (`ID_PATTERN_REGEX`), `format: byte` for Base64 `byte[]` fields, `maximum` for page
  sizes, nullability.
- Enum values with a description each (Action, CredentialType, MemberRole,
  TrustLevel, SanityStatus where exposed).
- Property names as serialised (JSON-B `@JsonbProperty`); verify that SmallRye picks
  them up, and fix any mismatch.
- `example` on properties; full examples on the main request types.

#### Spec version

Decided (2026-11-01): the spec is **OpenAPI 3.0.x** (3.0.3) for now, for the widest
generator support (Java, TypeScript, `oapi-codegen`). Moving to 3.1 is a later,
separate step.

- Configure SmallRye to emit 3.0.3 (SmallRye 4.x defaults to 3.1.0). Verify that the
  output is real 3.0 and not just a relabelled 3.1 document: `nullable: true` instead
  of type arrays, `example` instead of `examples` on schemas, no `$ref` siblings, no
  `const`, no JSON Schema 2020-12 keywords.
- If SmallRye 4.x can't produce clean 3.0 output, fall back to the last SmallRye
  plugin line that targets 3.0 (MicroProfile OpenAPI 3.x), and note it in this file.
- The completeness test also checks the version (`openapi` starts with `3.0.`) and
  rejects 3.1-only constructs.
- Annotations are written so the later move to 3.1 needs no rework: no hand-written
  3.0-specific JSON in descriptions or extensions.

### Completeness test

A JUnit test in `eds-core` reads the generated `target/generated/openapi.json` (with
JSON-P from the Jakarta EE API already on the classpath) and fails when:

- an operation lacks summary, description, tag, a response schema `$ref`,
  `x-eds-return-codes` or `x-eds-trust-level`;
- any response content schema is empty (`{}`);
- a schema, property or enum value lacks a description;
- a string property with a known limit lacks `maxLength`, or an ID property lacks
  the pattern;
- the return-code table doesn't match `ReturnCode.values()`;
- the document isn't OpenAPI 3.0.x, or uses 3.1-only constructs;
- an endpoint in the code has no matching operation in the spec, or the reverse.

Deprecated operations (signatures) are exempt until they're removed. The test runs in
the normal build, so `mvn clean install` fails on an incomplete spec.

### Phases

Each phase ends with `mvn clean install` green and a short summary of deviations.

#### Phase O1: Groundwork

- `microprofile-openapi-api` (provided) in `eds-api`.
- `OASFilter` skeleton, configured on the SmallRye plugin.
- Completeness test in report mode (prints all gaps, doesn't fail yet).
- Configure the spec version to 3.0.3 and verify the output (see "Spec version").
- Mark the signature operations and schemas `deprecated`.

Done when: the build produces an OpenAPI 3.0.3 spec with the filter active, and the
test prints a gap report.

Done 2026-11-01. SmallRye 4.2.0 with `openApiVersion` 3.0.3 produces clean 3.0
output, so no fallback was needed; the 3.0 rules are enforced from now on
(`OpenApiCompletenessTest.testSpecificationIsOpenApi30`). The code and spec endpoints
match. Starting gap report (`eds-core/target/openapi-gaps.txt`): 281 gaps, of which
96 are on operations (32 empty response schemas, 64 missing `x-eds-*` extensions),
184 on schemas (106 missing descriptions, 21 names without `maxLength`, 16 IDs
without a pattern, 18 Base64 fields labelled `format: binary`, undescribed enum
values), and 1 for the missing return-code table.

#### Phase O2: Global model

- `info.description`, tags reviewed, `externalDocs` to the project.
- `EDSResponse` as base schema; return-code table generated by the filter.
- Authentication and response-model description.
- Shared framework-level error responses.

Done when: the gap report shows no global gaps; the return-code table matches
`ReturnCode`.

Done 2026-11-01 (280 gaps left, all on operations and schemas):
- The overview is a Markdown resource next to the filter
  (`eds-core/src/main/resources/net/haugr/eds/core/services/openapi/overview.md`),
  set as `info.description` by `EDSOpenApiFilter`. Title, version, contact and
  license stay in the plugin configuration.
- `EDSResponse` carries the generated return-code table (in the `returnCode`
  description and as `x-eds-return-codes`); the "when" texts are in
  `ReturnCodeCauses`, and a test requires one for every `ReturnCode`.
- Shared responses `BadRequest`, `MethodNotAllowed`, `UnsupportedMediaType` (plus the
  `ConstraintViolationReport` schema) are referenced from every operation; 400 and 415
  only from operations with a request body.
- Checked against the running server: malformed JSON, invalid enum values and
  invalid Base64 give 400 with an empty body; a missing body gives 400 with a
  constraint-violation report; wrong method 405; wrong content type 415; unknown
  path 404 (HTML). Unknown request fields are ignored.
- Findings for O3: the server serialises with **Jackson** (`quarkus-rest-jackson`),
  not JSON-B, so the `@Jsonb*` annotations on the API objects aren't what defines
  the wire format. Responses include an `ok` field (from `isOk()`) and null fields.
  The schemas must describe the Jackson output.
- Findings for O4: `ReturnCode` uses 401 for `AUTHORIZATION_WARNING` and 403 for
  `AUTHENTICATION_WARNING`, the reverse of the HTTP meaning of those numbers (the
  Javadoc describes the HTTP meaning); `SIGNATURE_WARNING`'s description has a typo
  ("There Signature"). `WARNING` and `ILLEGAL_SERVICE` are never raised.

#### Phase O3: Schemas

- `@Schema` on every request, response, DTO and enum in `eds-api`, with descriptions,
  constraints and examples.
- Response types referenced from every `@APIResponse`, so they appear in
  `components/schemas`.

Done when: the gap report shows no schema or property gaps, and no empty response
schemas.

Done 2026-11-01 (64 gaps left, all `x-eds-*` extensions on operations, for O4):
- Every operation's 200 response references its concrete response type (derived
  from the bean method the service calls).
- Inheritance is rendered as `allOf` (SmallRye `auto-inheritance=BOTH`): every
  request extends `Authentication`, every response extends `EDSResponse`, so the
  common fields are documented once. The filter moves class descriptions to the
  schema itself, and removes the reference to `AbstractRequest`, which is hidden
  (`@Schema(hidden = true)`) because it has no fields.
- `@Schema` on all 49 API classes and their fields: descriptions, `maxLength` from
  `Constants`, the ID pattern, `format: byte` for Base64 fields, paging limits,
  examples, enum values described in the enum schema.
- Timestamps are `LocalDateTime` in **UTC** (`Utilities.newDate()`), serialised by
  Jackson as e.g. `2026-11-01T21:12:28.510566`. They're documented as
  `type: string, format: local-date-time` rather than `date-time`: `date-time`
  requires a time-zone offset, and generators would map it to an offset type that
  fails to parse these values.
- Verified against the running server (Jackson): the fields of 7 live responses
  match their schemas exactly, including `ok`.
- Correction made along the way: a login doesn't return a session key; the client
  chooses it and sends it as `newCredential`. The overview says so.

#### Phase O4: Operations

- Per operation: reconciled description, fields used, permissions, return codes,
  examples.
- Trust levels generated from `Permission` by the filter.
- List inconsistencies found between code and documentation, e.g.
  `ProcessMemberRequest.validate()` reports "may not exceed 256 characters"
  (`MAX_STRING_LENGTH`) while checking `MAX_NAME_LENGTH` (75). Fix the small ones in
  this phase; anything that changes behaviour goes to the relevant later plan.

Done when: the gap report is empty except for deprecated operations.

Done 2026-11-01 (0 gaps):
- Deviation from "Approach": the per-operation text (fields used, rules, specific
  return codes) is long Markdown, so it lives in one file per tag
  (`eds-core/src/main/resources/net/haugr/eds/core/services/openapi/operations/`)
  instead of in annotation strings. Each section names its `Permission` and its
  specific return codes. `OperationDocumenter` (called by the filter) appends the
  text, derives `x-eds-trust-level` from `Permission`, adds the common return codes
  and renders both as extensions and tables. Unknown permissions, unknown return
  codes and documentation for operations that don't exist stop the build.
- The `@Operation` descriptions are now short and checked against the managers;
  several were wrong (e.g. `createMember` claimed Circle administrators may create
  Members, `login` claimed to return a session token, `inviteMember` named a field
  `armoredKey`, `processDataType` claimed only administrators may manage types,
  `fetchMembers` claimed paging).
- The settings table is generated from `StandardSetting`, with the meanings in
  `SettingDescriptions` (a test requires one per setting).
- Small fixes: `SIGNATURE_WARNING` description typo; `ProcessMemberRequest` said
  "may not exceed 256 characters" for a 75-character limit; `ReturnCodeCauses`
  corrected to match where the codes are actually raised; paging defaults
  documented.

Findings that change behaviour, left for later (most touch code that
*Crypto* rewrites anyway). Two are fixed since, as marked, and 10 is gone; the open
bugs among them are tickets in `FINDINGS.md`, and 7 and 11 are decisions recorded
here:

| # | Finding | Where |
|---|---|---|
| 1 | `alterMember` with an unknown `memberId` throws a `NullPointerException`, returned as `ERROR` instead of `IDENTIFICATION_WARNING`. **Fixed 2026-11-13.** | `ProcessMemberManager.alterMember` |
| 2 | An administrator who isn't a Trustee of the Circle and sets a `circleKey` calls `trustees.getFirst()` on an empty list, returned as `ERROR`. (A Member without a Trustee record is refused before.) | `ProcessCircleManager.updateExternalCircleKey` |
| 3 | `updateCircle` removes the stored external key whenever `circleKey` is omitted. Documented, but surprising; renaming a Circle shouldn't drop its key. **Fixed 2026-11-10**: an omitted `circleKey` keeps the stored key. | `ProcessCircleManager.updateCircle` |
| 4 | The default Data Types `data` and `folder` could be deleted when unused, although they can't be updated. **Fixed 2026-11-26** with ticket 15 (`SECURITY.md`, "Data Types, and the rotation of the MasterKey"). | `ProcessDataTypeManager.doDelete` |
| 5 | Inconsistent codes for duplicates: duplicate Circle, Member or Data Object names give `IDENTIFICATION_WARNING`; duplicate names on invite or rename give `CONSTRAINT_ERROR`. | several managers |
| 6 | Changing a default Data Type gives `AUTHORIZATION_WARNING` (should be `ILLEGAL_ACTION`); a non-admin calling the MasterKey operation gets `AUTHENTICATION_WARNING` (should be `AUTHORIZATION_WARNING`); inviting as a non-admin without a Circle gives `ILLEGAL_ACTION`, into a Circle not administered `AUTHORIZATION_WARNING`, while creating gives `AUTHORIZATION_WARNING`. | `ProcessDataTypeManager`, `MasterKeyManager`, `ProcessMemberManager` |
| 7 | `ReturnCode` uses 401 for `AUTHORIZATION_WARNING` and 403 for `AUTHENTICATION_WARNING`, the reverse of the HTTP meaning of those numbers. Changing it changes the wire values; decide with the HTTP-status question in *Clients API* L1. | `ReturnCode` |
| 8 | `WARNING` and `ILLEGAL_SERVICE` are never raised. | `ReturnCode` |
| 9 | `invalidate` acts on the requester and ignores `memberId`; the endpoint name suggests otherwise. | `ProcessMemberManager.invalidate` |
| 10 | The inventory returned `folderId` `-` for every entry. **Gone**: the operation was removed (`STATUS.md`). | `InventoryManager` |
| 11 | `EntropyVerifier` rates word-based passphrases per character, so long passphrases like "correct horse battery staple…" are rated by their characters, not their words (known gap, accepted with *Crypto* §5 and PBKDF2). | `EntropyVerifier` |

#### Phase O5: Enforce and publish

- Switch the completeness test to failing mode.
- `accessories/release/openapi.json` is the published artifact; check it renders in
  a standard viewer and passes a validator.
- Decide whether the running server serves the spec (Open decisions).

Done when: the build fails on any newly introduced gap (verified by removing one
description in a test branch), and the release spec validates.

Done 2026-11-01:
- `OpenApiCompletenessTest` fails the build on any gap. Verified by removing the
  description of `Circle.circleId`: the build failed with exactly that gap.
- The spec passes `openapi-generator validate --recommend` (v7.10.0, run through
  Podman, not part of the build) with no issues.
- Added: every operation gets an `operationId` from the last path segment
  (`createCircle`, `addData`, …), set by the filter; duplicates stop the build, and
  the test requires one. Without them, generated clients had names like
  `circlesCreateCirclePost`.
- Generated Java, Go and TypeScript (`typescript-fetch`) clients from the spec
  without warnings; timestamps map to plain strings, as intended.
- FitNesse unchanged from the Step 0 baseline (342 right, the same 15 known
  failures).
- Still open (see Open decisions): whether the running server serves the spec.
  Nothing was added; `accessories/release/openapi.json` is the published spec.
  (Decided since: L1 made the spec the contract in `eds-api`, and L2 serves it.)

### Rule for the later chapters

Every phase in *Crypto*, *Streaming* and *Clients API* that changes the API
updates the annotations in the same phase, and the completeness test must stay green.
In particular:

- *Crypto* C1 removes the signature operations, schemas and `SIGNATURE_WARNING`
  from the spec, and documents the new invitation token.
- *Crypto* C3 documents sessions (and adds a `securitySchemes` entry if the
  session token moves to a header).
- *Streaming* documents the content endpoints, which use real HTTP status codes
  (`206`, `412`, `413`, `416`; `411` was dropped on 2026-11-11) next to the HTTP-200 model; the difference is explained
  in the spec.

### Decisions that were open

All decided since:

- **Serving the spec at runtime**: L2. `SpecificationService` serves the contract
  from the `eds-api` jar; `quarkus-smallrye-openapi` isn't used.
- **Per-operation request schemas**: L1. The shared request types stay; the spec
  documents the fields per operation.
- **HTTP status codes**: L1. The JSON API keeps the HTTP-200 model; the content
  endpoints and the ID-based resource API answer with real status codes.
- **Language and tone**: English, with the terms Member, Circle, Trustee, Data
  Object and Folder, as agreed in O2.

### Open

- Finding 7 (401 and 403 swapped in `ReturnCode`): to decide with the HTTP-status
  question of *Clients API* L1, as it changes the wire values. The other open
  findings (2, 5, 6, 8, 9) are tickets in `FINDINGS.md`.

## Crypto

Status: done (C1–C5, 2026-11-02, C6 2026-11-21); see the "Done" notes under each phase. Changed
since: invitations into a Circle (2026-11-08), download tokens (L5), no system salt
and the PBE iterations per Member (2026-11-10); each is described where it belongs
below, and listed under "After L5" in *Clients API*.
Scope: MasterKey, key hierarchy, algorithm changes, removal of signatures, invitations.
API changes made here keep the OpenAPI spec complete (*OpenAPI*, "Rule for the later
chapters"). This chapter decides everything crypto, including the chunked storage
format for data objects (§4) and sessions (§2a); the later chapters and the plans of
the clients refer to it.

### Goals

1. **The core secret stays inside the system.** A stolen database alone must not be
   decryptable (kill-switch property). Weak user credentials only unlock the user's
   asymmetric keys, never the data directly.
2. **One compliance baseline for BSI (TR-02102), ANSSI and NSA CNSA 2.0** at the same
   time: AES-256, SHA-384/512, P-384, ML-KEM-1024.
3. **Post-quantum confidentiality** for stored data via hybrid key encapsulation.
4. **Plain JDK/JCE only** (Java 25: `javax.crypto.KDF` / HKDF from JEP 510, ML-KEM from
   JEP 496). No BouncyCastle, Tink or other third-party crypto at runtime.
5. **Smaller attack surface**: drop algorithms and features that aren't needed.

### Non-goals

- In-place migration from CWS or EDS 1.x. EDS 2.0 is a **clean break** and needs a
  fresh setup. A migration script can be prepared on request later, building on the
  planned CLI.
- Signatures (RSA or ML-DSA). See "Removed features".
- Fully protecting users who insist on weak passwords. EDS enforces the entropy rules,
  but can't do more than that without locale-bound word lists.

### Threat model summary

- EDS may run on untrusted hosts; the database may be dumped or stolen.
- The MasterKey is never persisted. It's loaded at startup from an external
  high-entropy source (`eds.masterkey.url`) and kept in memory only. Removing that
  source makes every stored secret unreadable (kill-switch).
- DB-only attacker: must learn nothing usable, and must not be able to run offline
  password guessing.
- Attacker with the MasterKey but no user credentials: must not be able to read circle
  data either (double wrapping).

### Algorithm baseline

| Purpose                         | Algorithm                                         |
|---------------------------------|---------------------------------------------------|
| Symmetric encryption (keys)     | AES-256-GCM, random 96-bit IV per encryption, AAD |
| Data object encryption          | Chunked AES-256-GCM (STREAM construction), AAD    |
| KDF for high-entropy input      | HKDF-SHA512 (`javax.crypto.KDF`)                  |
| KDF for user passwords          | PBKDF2-HMAC-SHA512 (iterations configurable)      |
| Key encapsulation               | Hybrid ML-KEM-1024 + ECDH P-384                   |
| MAC / lookup tokens             | HMAC-SHA512 (key derived from the MasterKey)      |
| Hashes                          | SHA-384 / SHA-512 only                            |

Decisions and rejected options:

- **PBKDF2 stays for passwords.** A zxcvbn-style word list for rating passphrases would
  be locale-bound, which conflicts with the goal of one container that runs anywhere
  with minimal changes. The MasterKey outer layer (below) already blocks offline
  guessing from a DB dump.
- **HKDF replaces PBKDF2 for all high-entropy inputs** (MasterKey, Circle Secret).
- **P-521 rejected**: not part of CNSA 2.0, no real gain over P-384.
- **Pure ML-KEM-1024** may become an opt-in later; hybrid is the default.
- **X25519/X448 dropped**: not in the CNSA/BSI baseline (and the current enum entries
  are placeholders that aren't wired up).

### Key hierarchy

```
External source (eds.masterkey.url)
        │ HKDF-SHA512
        ▼
   MasterKey (memory only, versioned, key check value)
        │ HKDF subkeys: "wrap", "hmac", ...
        ├──────────────────────────────┐
        ▼                              ▼
 outer wrap of member private keys   outer wrap of trustee rows,
 and trustee rows (AES-256-GCM)      HMAC for invitations/session lookup

 Member password ──PBKDF2──► password key ──► inner wrap of member private keys
                                              (ML-KEM-1024 + P-384 key pairs)

 Circle Secret (random 256 bit)
   └─ per trustee: inner = hybrid KEM to member public keys
                   outer = AES-256-GCM with MasterKey wrap key
        │ HKDF(CircleSecret, salt = random per object,
        │      info = label|circleId|objectId|circleSecretVersion)
        ▼
   Object key ──► chunked AES-256-GCM (nonce prefix + counter, AAD) ──► data object

 Session token (random) ──HKDF──► session key ──► inner wrap of a session copy of
                                                  the member private keys
                                                  (MasterKey outer layer)
```

#### 1. MasterKey (kill-switch)

- Input comes from the external source only. **No `ADMIN_ACCOUNT` / `"admin"`
  fallback**: if the source is missing or empty, EDS refuses to start (or stays locked).
- Input must pass `EntropyVerifier` (same rules as passwords, see below).
- Derived with HKDF-SHA512; separate subkeys per purpose via `info` labels
  (`eds/mk/wrap`, `eds/mk/hmac`, …). The raw input is never used directly.
- Stored in the DB: only a **version** and a **key check value** (KCV, e.g. HMAC of a
  fixed label with a dedicated subkey), so a wrong MasterKey is detected at startup
  without decrypting anything.
- **Fixes the fixed-IV nonce reuse** (finding 2026-10-22): every MasterKey encryption
  uses a random IV. Values that today are encrypted deterministically for lookup
  (e.g. `session_checksum`) become HMAC-SHA512 tokens with the `hmac` subkey instead.
- MasterKey rotation: new version, re-wrap all outer layers (trustee rows, member
  private keys) in one transaction; sessions are ended, not re-wrapped (C4), and
  pending invitations are deleted. AAD includes `mkVersion`, so mixing
  versions fails. Data objects are **not** touched by a rotation.
- **The MasterKey stays out of the data path.** The kill-switch still covers data,
  because the object key comes from the Circle Secret, and every stored copy of the
  Circle Secret has a MasterKey outer layer. Mixing MasterKey material into object keys
  would force re-encrypting all data, including large streamed objects, on every
  rotation.

#### 2. Member keys

- Each member has two key pairs: ML-KEM-1024 and ECDH P-384.
- Private keys are **double wrapped**:
  - inner: AES-256-GCM with the password key (PBKDF2-HMAC-SHA512, random salt),
  - outer: AES-256-GCM with the MasterKey wrap key.
- Because of the outer layer, a DB dump without the MasterKey gives an attacker nothing
  to run password guesses against.
- AAD binds each wrap to `memberId | keyType | mkVersion | algId`.

#### 2a. Sessions

Logging in with credentials runs PBKDF2 (hundreds of thousands of iterations, roughly
0.3–1 s) on every request. That's acceptable for occasional calls, but not for
streaming: a media player sends many small `Range` requests while seeking. Sessions
are therefore the normal way to access data, and they must not need PBKDF2 per
request.

- Login (credentials, PBKDF2 once) creates a random session token (≥ 256 bits), which
  is returned to the client once.
- Lookup: the DB stores `HMAC-SHA512(MasterKey hmac subkey, token)` in
  `session_checksum`, never the token itself.
- Session copy of the private keys (`session_crypto`): double wrapped like the
  permanent copy:
  - inner: AES-256-GCM with a session key = HKDF-SHA512(token, info =
    `eds/session|memberId|expiry`),
  - outer: AES-256-GCM with the MasterKey wrap key.
- AAD binds the session copy to `memberId | sessionExpiry | mkVersion | algId`.
- Logout, expiry or a MasterKey change makes the session unusable: the row is deleted,
  or its outer layer no longer opens.
- Download token (done in L5, for media elements, which can't send headers): the
  Session token and an expiry, sealed with the MasterKey wrap key (AES-256-GCM, AAD
  `eds/download|<dataId>`). Nothing is stored; the token reads only that object's
  content, as `?token=` on the content URL. It ends after
  `eds.download.token.lifetime.minutes` (default 60, at most the end of the
  Session), at logout, or when the MasterKey changes (`MediaManager`).

#### 3. Circle Secret and trustees

- Circle Secret: random 256 bits from `SecureRandom`, generated when the circle is
  created.
- Each trustee row holds the Circle Secret **double wrapped**:
  - inner: hybrid KEM (ML-KEM-1024 + ECDH P-384) to the member's public keys,
  - outer: AES-256-GCM with the MasterKey wrap key, random IV,
    AAD = `circleId | memberId | mkVersion | algId`.
- Neither the MasterKey alone nor a user's key alone can recover the Circle Secret.
- Hybrid combiner (to be fixed in the implementation and documented with test vectors):
  HKDF-SHA512 over both shared secrets, with the KEM ciphertext, the ECDH ephemeral
  public key and the recipient public keys in `info`, so neither component can be
  swapped or stripped independently.

#### 4. Data objects

All data objects, small or large, use **one** chunked format, so there's only one
storage format to maintain.

- Object key = HKDF-SHA512(CircleSecret, salt = random 256-bit per object,
  info = `eds/object | circleId | objectId | circleSecretVersion`).
  - `circleSecretVersion` is the Circle Secret version the object was encrypted
    under (today `eds_data.key_id`).
  - Every write (create, update, copy into another circle) generates a new salt, so
    an object key is never reused for different content.
- Encryption: STREAM construction with AES-256-GCM.
  - The plaintext is split into fixed-size chunks; each chunk is sealed with its own
    GCM tag (128 bit).
  - Nonce (96 bit) = random nonce prefix from the header | 32-bit chunk counter |
    last-chunk flag. Because the key is unique per object, nonces only need to be
    unique within one object.
  - AAD per chunk = header | `circleId | objectId | circleSecretVersion`. This
    detects reordering, truncation, dropped chunks and swapping between objects.
- Header, stored at the start of `encrypted_data` (no extra columns; the DB stays
  unaware of the content structure): format version, chunk size, HKDF salt, nonce
  prefix. The header isn't secret, but it's authenticated through the AAD.
- The format version lives in the header, not in the HKDF `info`. A new format is
  introduced by a new header version.
- The plaintext size follows from the blob length, chunk size and tag size, so no size
  column is needed.
- Range reads decrypt only the chunks that cover the requested bytes.
- Integrity: GCM tags protect the content against tampering, but only someone with
  the object key can check them. The keyless sanity job keeps a SHA-512 hash of the
  ciphertext (`eds_data.checksum`), computed while streaming the upload. It's checked
  by the sanity job, not on every read (a range read can't check a hash over the
  whole blob).
- Sanity job (background scrub): kept on purpose.
  - Purpose: find silent corruption in data nobody reads (firmware bugs, lost or
    misdirected writes, controller/RAM faults) while a good backup still exists.
    EDS can only detect corruption, not repair it.
  - Why in EDS: the storage below may scrub on its own (ZFS/Btrfs scrubs, Ceph,
    RAID patrol reads, managed databases), but EDS can't assume that, given the
    "runs anywhere" container goal and H2 support. PostgreSQL 18 page checksums are
    on by default, but they're only checked when a page is read, with no background
    scrub.
  - Not a security control: someone with DB write access can recompute the hash.
    Tampering is covered by the GCM tags.
  - Make failures visible: report scrub status and failed objects through a health
    group of its own (`/q/health/group/sanity`, DOWN while an object is failed; not
    readiness, and no metric, see *Streaming* S4), so monitoring raises an alarm. Marking
    a row as failed isn't enough on its own.
  - Configurable interval, including "off" for installations whose storage already
    scrubs; large installations can spread the reads over time.
  - Store the hash rather than rely on the read alone: reading a blob verifies it on
    PostgreSQL with page checksums, but detects nothing on H2 or on PostgreSQL
    without checksums.
- Moving or copying an object to another circle means re-encrypting it chunk by chunk
  (the Circle Secret and `circleId` differ).
- Rotating a Circle Secret (issue #43) means re-encrypting every object in that
  circle. That has to run as a background job, chunk by chunk, with progress
  tracking, not as one transaction: done in C6.

#### 5. Passwords and EntropyVerifier

- `EntropyVerifier` (`eds-core` `jce`) applies the same rules to passwords and MasterKey
  input: at least 15 symbols, not on the blocklist, estimated ≥ 128 bits (zxcvbn-style
  dynamic programming).
- Enable it on every new secret: account creation, password change, invitation accept,
  MasterKey setup.
- Known gap: word-based passphrases are rated per character. Accepted for now (see
  PBKDF2 decision above).

### Removed features

- **RSA**: all RSA key pairs, `RSA/ECB/OAEP…` usage, and `RSA_2048/4096/8192` enum
  entries.
- **Signatures**: sign, verify and fetch-signature endpoints. ML-DSA isn't needed as a
  replacement. Affected code:
  - `eds-api`: `SignRequest`, `VerifyRequest`, `FetchSignatureRequest`, `SignResponse`,
    `VerifyResponse`, `FetchSignatureResponse`, `dtos/Signature`, related constants.
  - `eds-core`: `SignatureService`, `SignManager`, `VerifyManager`,
    `FetchSignatureManager`, `SignatureDao`, `SignatureEntity`, signature handling in
    `ShareBean`, `Crypto`, `PublicEDSKey`, `Permission`, settings.
  - Schema: `eds_signatures` table.
  - `eds-testing`: `Sign`, `Verify`, `FetchSignatures` fixtures and suites.
- **AES-CBC and PBE-CBC** variants, **AES-GCM-128/192**, **SHA-256** transformations.
- **X25519 / X448** placeholder entries.

### Invitations

- Today an invitation is a signature issued by the System Administrator. This goes away
  with signatures.
- New design: a random token (≥ 256 bits) is returned once to the inviter; the DB stores
  only `HMAC-SHA512(MasterKey hmac subkey, token)` plus expiry.
- Accepting an invitation, as built (C1): the invited account exists from the
  invitation on, without keys. It is found by its account name, the HMAC of the
  token is compared and the expiry checked; then the Member gets their keys from
  their own password (EntropyVerifier applies). The Member's row is kept.
- Side effect: invitations are void after a MasterKey change, which is consistent with
  the kill-switch.
- Invitations into a Circle (2026-11-08, for Rune): an administrator of a Circle may
  invite into it. The Circle Secret is sealed for the invitation (`eds_invitations`):
  inside with AES-256-GCM under `HKDF-SHA512(token, salt "eds/invitation/grant",
  info "eds/invitation/grant|<memberId>|<circleId>")`, outside with the MasterKey
  wrap key, both with the binding in the AAD. Neither a copy of the database nor the
  MasterKey alone opens it; only the token, which EDS doesn't keep. On acceptance the
  Secret is wrapped to the new Member's keys (a normal Trustee) and the row deleted;
  it goes with the invited Member (expiry, MasterKey rotation) or the Circle.
  `inviteMember` takes the access as `trustLevel` (READ, WRITE as the default, or
  ADMIN). An inviter who isn't an administrator must name a Circle they administer
  (`ILLEGAL_ACTION` without a Circle, `AUTHORIZATION_WARNING` for a Circle they don't
  administer), and the invited Member always gets the
  role STANDARD. A name that exists already is added to the Circle at once, without
  an invitation.

### Implementation phases

Each phase ends with `mvn clean install` green and a short summary of deviations.

#### Phase C1: Remove RSA and signatures

- Delete the signature API, services, managers, DAO, entity, table and fixtures listed
  above.
- Replace invitations with HMAC tokens (can temporarily use the current MasterKey until
  C2 lands, but must not reuse its fixed IV).
- Trim `KeyAlgorithm` to the algorithms that remain.

Done when: no RSA or signature code remains (`grep -ri "rsa\|signature"` only hits
docs/history), invitation suites pass.

Done 2026-11-02. Deviations:
- **RSA stays for the Member key pairs until C3.** RSA is the only asymmetric
  algorithm the member keys and trustee rows have today; removing it in C1 would
  need an interim replacement that C3 throws away. C1 removes everything else RSA
  was used for (signatures, `SHA256WithRSA`/`SHA512WithRSA`), and C3 removes the
  remaining `RSA_*` entries together with the `rsa_algorithm` column.
- `KeyAlgorithm` is trimmed to `AES_GCM_256`, `PBE_GCM_256` and the RSA entries;
  `HashAlgorithm` loses `SHA_256`. The hash setting is now checked against
  `HashAlgorithm` (it was checked against the signature algorithms before).
- Invitations: the token (256 bits) is returned once in `ProcessMemberResponse.invitation`
  and accepted with credential type `INVITATION` (replaces `SIGNATURE`). The member
  row stores `HMAC-SHA512(k, token)` in `salt`, with
  `k = HKDF-SHA512(MasterKey key bytes, info = "eds/mk/hmac")` as an interim key until
  C2 derives the subkeys from the external source, plus the new column
  `invitation_expire` (setting `eds.invitation.timeout.hours`, default 168). The
  sanity job deletes expired invitations. Accepting keeps the row (and so the member
  ID returned at invite time) instead of deleting and recreating it.
- `ReturnCode.SIGNATURE_WARNING` (491) is removed.
- Results: `eds-api` 154, `eds-core` 453, `eds-quarkus` 14 tests passed; FitNesse
  `EDS` suite (H2) 318 right, 14 wrong. The 14 are the known failures from Step 0
  (3 MasterKey, 11 settings; one settings failure went away with the signature
  setting). The signature tables accounted for the drop from 342 right.

#### Phase C2: MasterKey

- Crypto API cleanup (`eds-core` `jce`), used by everything after this:
  - `seal`/`open` with an explicit nonce and AAD, instead of the IV being carried as
    mutable state on the key (`SecretEDSKey.setSalt`, `prepareCipher`).
  - Replace `IVSalt` (16 bytes, Base64, UUID fallback) with a raw 12-byte nonce type.
  - Rename `Constants.GCM_IV_LENGTH` (128, actually the tag length in bits) to
    `GCM_TAG_BITS`, and add `GCM_NONCE_BYTES = 12`.
- HKDF-SHA512 derivation with purpose subkeys; version + KCV in the DB.
- Remove the `"admin"` fallback; refuse to start without a valid external source.
- Random IV for every MasterKey encryption; HMAC tokens for lookups
  (`session_checksum`, invitations).
- EntropyVerifier on MasterKey input.

Done when: tests prove two encryptions of the same value differ, a wrong MasterKey is
rejected via KCV, and startup without a source fails.

Done 2026-11-02. Deviations and details:
- **Startup without a source leaves EDS locked** (the "or stays locked" option of
  §1), instead of failing the start. Every request except the MasterKey request then
  fails with CRYPTO_ERROR. The MasterKey request unlocks EDS (KCV match plus the
  System Administrator's credentials), or sets up version 1 on a fresh database.
  Source order: `EDS_MASTERKEY_URL`, system property `eds.masterkey.url`, setting.
  Quarkus loads it after the database is ready (`ManagementBean.loadMasterKey`, so
  the first setup is written in a transaction).
- MasterKey: HKDF-SHA512 extract (salt `eds/masterkey`) and expand into
  `eds/mk/wrap` (AES-256), `eds/mk/hmac` (64 bytes) and `eds/mk/kcv` (32 bytes,
  the KCV itself). New table `eds_masterkey (version, kcv)`.
  MasterKey-wrapped values are `nonce | ciphertext`, with the AAD extended by
  `|mk<version>|AES_GCM_256`.
- Changing the MasterKey is still only allowed while the System Administrator is
  the only Member; full rotation (re-wrapping) stays in C4.
- Sessions: `session_checksum` = HMAC(`eds/session/lookup`, token); the session copy
  of the private key is protected by a PBKDF2 key from HMAC(`eds/session/secret`,
  token). The PBKDF2 step per session request is removed in C3 (§2a).
- Data objects: random 96-bit nonce per write, kept MasterKey-wrapped in
  `initial_vector`, content AAD `eds/data`. C4 replaces this with the §4 format
  (ids in the AAD, no `initial_vector`).
- The member PBKDF2 salt is 16 random bytes, MasterKey-wrapped with AAD
  `eds/member/salt|<memberId>`; private keys use AAD
  `eds/member/key|<memberId>|<algorithm>` (`eds/member/session|...` for the session
  copy). The PBKDF2 iterations are kept per Member too (`pbe_iterations`): the setting
  counts for new passphrases, each account logs in with its own. `eds.system.salt` is gone: it was a constant appended to the passphrase before
  PBKDF2, the same for every installation unless set by hand; the random salt of each
  Member, sealed with the MasterKey, does that work.
- Crypto API: `Crypto.seal/open` with an explicit `Nonce` and AAD (or a random nonce
  prepended), `sealWithMasterKey/openWithMasterKey`, `hmacWithMasterKey(label, value)`;
  `IVSalt`, `SecretEDSKey.setSalt` and the symmetric `encrypt/decrypt` are removed.
  `Constants.GCM_TAG_BITS` = 128, `GCM_NONCE_BYTES` = 12.
- Results: `eds-api` 154, `eds-core` 466, `eds-quarkus` 14 tests passed; FitNesse
  `EDS` suite (H2, started without a MasterKey source) 323 right, 12 wrong: only the
  known settings failures from Step 0. The MasterKey failures are fixed (the Setup
  page unlocks or sets up the MasterKey, and the MasterKey table reverts it).

#### Phase C3: Member keys

- ML-KEM-1024 and P-384 key pairs per member.
- Double-wrapped private keys (PBKDF2 inner, MasterKey outer) with AAD.
- PBKDF2-HMAC-SHA512; iteration count setting written at setup (no longer written
  since 2026-11-10, when the count moved to the Member; see the notes below).
- Sessions as in §2a: random token, HMAC lookup, double-wrapped session copy of the
  private keys, no PBKDF2 per request.

Done when: login/password change round trips pass, a DB dump without the MasterKey
can't be used to test a password guess (unit test against the stored format), and a
request made with a session token runs no PBKDF2 (test or benchmark).

Done 2026-11-02. Deviations and details:
- **The hybrid KEM came forward from C4.** RSA was the only way to wrap Circle keys
  to Members, so replacing the Member keys needs the hybrid KEM now. C3 has
  `HybridKem` (format version 1, combiner below) and wraps the trustee rows with it,
  inner layer only, AAD `eds/trustee`. C4 adds the MasterKey outer layer, binds the
  AAD to `circleId | memberId | mkVersion`, and publishes the test vectors.
- Combiner as implemented: `kek = HKDF-SHA512(salt = "eds/hybrid-kem/v1",
  ikm = ss_kem | ss_ecdh, info = "eds/hybrid-kem/v1/kek" | SHA-512(ct_kem | epk |
  pk_kem | pk_ec), L = 32)`; the secret is sealed with AES-256-GCM under `kek`.
  Wire format: `0x01 | len | ct_kem | len | epk | nonce | ciphertext+tag`, lengths
  as 4-byte big-endian. Encodings: X.509 public keys, PKCS#8 private keys (the JDK's
  ML-KEM private key is the 64-byte seed form).
- Member keys: `KeyAlgorithm.MLKEM1024_P384` replaces `RSA_*`; column
  `rsa_algorithm` is now `key_algorithm`. Private keys are stored
  as `MasterKey(AAD eds/member/key-outer|id|alg) ∘ passwordKey(AAD eds/member/key|id|alg)`.
- PBKDF2-HMAC-SHA512 with a default of 210000 iterations (OWASP for SHA-512; this
  closes the open point on the default). Each account keeps the iterations of its
  passphrase (`pbe_iterations`), so a later default or a changed setting can't lock
  existing Members out.
- **Sessions: the API changed.** LOGIN no longer takes `newCredential`; EDS
  generates a 256-bit token and returns it once in `ProcessMemberResponse.session`.
  Session key = HKDF-SHA512(token, salt `eds/session`, info
  `eds/session|memberId|expiry`), expiry in epoch seconds (stored truncated to
  seconds). Changing the passphrase ends the session.
- Tests: `MemberKeysTest` (double wrap, no password test without the MasterKey,
  hybrid KEM round trip, tampering of each part), session tests including a PBKDF2
  counter (`Crypto.passwordKeyDerivations`) that stays unchanged across session
  requests. FitNesse keeps the returned tokens under the names in the "new
  credential" column of the login.
- Results: `eds-api` 154, `eds-core` 470, `eds-quarkus` 14 tests passed; FitNesse
  `EDS` suite (H2) 317 right, 12 wrong (only the known settings failures).

#### Phase C4: Circle Secret, trustees, data objects

- Hybrid KEM with a documented combiner and test vectors.
- Double-wrapped trustee rows; HKDF object keys with per-object salt; AAD everywhere.
- MasterKey rotation re-wraps outer layers (and leaves data objects alone).
- Data format (§4): fix the chunk size and header layout, and publish test vectors
  (including an empty object, an exact multiple of the chunk size, and a final partial
  chunk). Data is written in the full chunked format from here on, still from and to
  `byte[]`; C5 adds the streaming API on top without changing the format.
- Data rows: drop `eds_data.initial_vector` (the header lives in `encrypted_data`).

Done when: FitNesse suites cover circle create/add trustee/remove trustee, data
round trips, and rotation; MasterKey-only and user-key-only recovery attempts fail in
tests.

Done 2026-11-02. Decisions and details:
- **Chunk size: 64 KiB** (65536). Tag overhead is 0.02 %, and a range read decrypts at
  most one extra chunk at each end. Readers accept 16 B to 16 MiB from the header.
- **Header (44 bytes)**: `version (0x01) | chunk size (4, BE) | HKDF salt (32) |
  nonce prefix (7)`. Chunk nonce = `prefix (7) | chunk index (4, BE) | last flag (1)`
  (the STREAM layout). Chunk AAD = `header | "|" circleId "|" objectId "|"
  circleSecretVersion`. Object key = HKDF-SHA512(Circle Secret, salt, info
  `eds/object|circleId|objectId|circleSecretVersion`). An empty object is one empty
  final chunk; an exact multiple ends with a full final chunk. Implemented in
  `jce/DataFormat` (byte[] API; C5 adds streaming).
- **Test vectors**: `eds-core/src/test/resources/test-vectors/`. The data format
  vectors (empty, partial, exact multiple, several chunks) are computed by an
  independent Python implementation of this section (`data-format-v1.py`). The hybrid
  KEM vector comes from EDS; its ECDH, combiner and AES-GCM seal are checked
  independently (`hybrid-kem-v1-check.py`). `DataFormatTest` requires EDS to
  reproduce both.
- Trustee rows: `MasterKey(AAD eds/trustee-outer|circleId|memberId|mk<version>) ∘
  HybridKem(AAD eds/trustee|circleId|memberId)`. The external Circle key uses an HKDF
  subkey of the Circle Secret (`eds/circle/external-key|circleId`) instead of the
  Circle Secret itself.
- **MasterKey rotation** is allowed with Members: one transaction re-wraps the
  Member salts, private keys and trustee rows, ends sessions and deletes pending
  invitations. Data Objects are not touched. The old restriction (admin only) and its
  ILLEGAL_ACTION are gone.
- `eds_data.initial_vector` is dropped. The Circle Secret is the
  existing per-circle key (`eds_keys`, its id is the `circleSecretVersion`).
- Tests: `DataFormatTest` (vectors, round trips around the chunk size, binding to
  the object, reordered, dropped, truncated, extended and modified chunks),
  `KeyHierarchyTest` (MasterKey-only and Member-key-only recovery fail, rows bound to
  circle and member), rotation tests with data. FitNesse rotates the MasterKey with
  Members, Circles and Data in place and reads the data afterwards.
- Results: `eds-api` 154, `eds-core` 491, `eds-quarkus` 14 tests passed; FitNesse
  `EDS` suite (H2) 335 right, 12 wrong (only the known settings failures).

#### Phase C5: Streaming crypto API

The crypto side of streaming; storage access, endpoints, transactions and the sanity
job are in *Streaming*, which follows this chapter.

Tasks (`eds-core` `jce`):

- Encrypting stream: wraps a plaintext `InputStream` of known length, produces the §4
  format chunk by chunk, and reports the exact ciphertext length up front (needed for
  the streamed database write).
- Chunk decryptor: given the header and a run of ciphertext chunks, verifies and
  decrypts them, and trims the first and last chunk to a requested byte range.
  Maps a plaintext byte range to chunk indexes and blob offsets.
- Chunk-by-chunk re-encryption helper (source object key → target object key), used
  for copy/move between circles and later for Circle Secret rotation.

Done when: unit tests cover streamed round trips with bounded memory, range
decryption at and around chunk boundaries, and rejection of tampered, reordered,
truncated or extended chunk sequences; output matches the C4 test vectors.

Done 2026-11-02. Details:
- `jce/DataStreams`: `encrypt(...)` returns an `EncryptingStream` (an InputStream of
  the §4 format) with `storedLength()` known up front; it reads one chunk of
  plaintext at a time and fails if the source is shorter or longer than declared.
  `plan(header, storedLength, from, to)` maps an inclusive plaintext range (as in
  HTTP `Range`) to chunk indexes, the stored offset and length, and the bytes to
  skip. `decrypt(...)` verifies each chunk before returning any of its bytes and
  returns exactly the requested range; it also decrypts whole objects from a stream.
  `reencrypt(...)` chains the two, chunk by chunk.
- The format is unchanged; the stream and byte[] paths are tested against each other
  and against the C4 vectors.
- Tests (`DataStreamsTest`): a 64 MiB object encrypted and decrypted as a chain of
  streams with the read-ahead checked to stay within two chunks; every range between
  the points around the chunk boundaries of a 100-byte object (chunk size 32), and a
  mid-object range with the default chunk size reading a single chunk; reordered,
  truncated (at and inside a chunk), extended and modified streams; wrong declared
  lengths; re-encryption of empty, small and multi-chunk objects.
- No runtime path uses the streams yet (*Streaming* S1–S3 does), so the FitNesse
  result is unchanged from C4. Results: `eds-api` 154, `eds-core` 516, `eds-quarkus`
  14 tests passed.

#### Phase C6: Rotating a Circle Secret

Status: done 2026-11-21 (see "Done" at the end of this phase for what differs from
the plan below). Asked for by the user (issue #43, which
called it "re-keying"; the plan says *rotate*, as for the MasterKey). The user
confirmed the same day: the rotation asked for by a Circle administrator, the API
fields, the schema and the version of the secret in the trustee row's AAD.

**Why.** A Member who leaves a Circle loses their trustee row, and with it EDS's
copy of the Circle Secret for them. What they may have kept is another matter: a
client which held the Circle Secret in memory, or a database dump together with the
MasterKey. Rotating gives the Circle a new Circle Secret which they never had, and
re-encrypts the Circle's data under it. Rotating at intervals also limits how much
data one Circle Secret protects over the years.

**When a rotation is due.** One timestamp per Circle tells it,
`eds_circles.rotate_circle_key_due` (2026-11-21, the user): a rotation is due once
that time has passed.
- A new Circle sets it to now + `eds.circle.secret.rotation.days` (default 365).
- A Trustee who leaves or is removed sets it to now, whoever removes them (also the
  System Administrator, who needn't be a Trustee).
- The end of a rotation sets it to now + the period again, as the last step.
- With the period 0 the periodic rotation is off: the timestamp is null except after
  a removal. A changed period applies to a Circle from its next rotation on; the
  times already set stay.
- A Circle administrator asks for one
  (`PUT /circles/{circleId}/rotation`, JSON twin `rotateCircle`), which sets it to
  now.

**When it runs.** The time passing makes a rotation due; it doesn't start it. The
stone alone can't read a Circle Secret (§3); only a Trustee's request unwraps it. So
a due rotation **starts in the background at the next request of a Trustee which
unwraps the Circle Secret** (a read, a write, a listing with content). A Circle
nobody uses stays due until someone does: its data then waits under the old secret,
which nobody can read either. The request itself isn't held up; it only hands the
unwrapped secret to the job.

**How.**
1. **Start, in one short transaction:** a new Circle Secret (random 256 bits, a new
   `eds_keys` row, status `ACTIVE`; the former one becomes `RETIRING`). Every
   remaining Trustee's row gets the new secret, wrapped to their public keys (§3: no
   session of theirs is needed), and keeps the retiring one beside it. The external Circle key and
   the Trustees' secrets (E2E.md), which are sealed with subkeys of the Circle
   Secret, are sealed anew under the new one. From here on, everything written to
   the Circle (uploads, new content, copies and moves into it, entries of features)
   uses the new secret.
2. **The job** re-encrypts every object whose `eds_data.key_id` isn't the current
   secret, chunk by chunk with `DataStreams.reencrypt` (C5): new salt, new nonce
   prefix, new checksum, new parts. One transaction per object: the row is locked,
   checked to be still on an old secret (it may have been deleted or rewritten
   meanwhile), written anew and switched over. Several objects at a time, by
   `eds.circle.secret.rotation.threads` (default 2), and one job per Circle.
   Memory stays bounded by a part per thread, as for any upload.
3. **The end, in one transaction**, when no object is left on an old secret: the
   trustee rows of the old secrets and their `eds_keys` rows are deleted, and
   `rotate_circle_key_due` is set to now + the period (null with the period 0).
   Unless a removal set it after this rotation started: then it stays as the removal
   set it, and the next rotation follows (see "Removed during a rotation").

**Reading and writing during a rotation.** Each object names its secret
(`eds_data.key_id`), and a Trustee's row holds each secret in use, so a read picks
the one of the object. A move or copy out of the Circle reads with the old
or the new secret, whichever the object is on. A download that runs while its object
is switched over finishes on its snapshot (PostgreSQL, `REPEATABLE READ`); the ETag
comes from the header, so it changes, and a client which resumes with `If-Range`
gets the whole content again. That is correct, as the stored bytes did change.

**Interruptions.** Nothing about the job is held only in memory except the
unwrapped secrets. If the stone stops, the state is in the database: the `ACTIVE`
and `RETIRING` keys, and the `key_id` of each object. The job continues at the next
unwrapping request of a Trustee, with both secrets from that Trustee's rows.

**Removed during a rotation.** A rotation that becomes due again (another Trustee
removed) follows when this one ends: the job has the new secret, and starts the next
rotation with it at once. A Circle has at most two secrets at a time. A removed
Member's row goes with both.

**The MasterKey** (§1). A MasterKey rotation re-wraps every trustee row, so also
those of a `RETIRING` secret. The two rotations don't wait for each other.

**Binding.** Today a trustee row's AAD is `circleId|memberId`. With two secrets per
Member and Circle, it gets the secret's version: `circleId|memberId|keyId`, so the
wrapped secrets can't be swapped. The schema version stays 4 until the release, so
this changes the format in place.

**Schema** (layer 3):
- `eds_keys`: `circle_id` (FK to the Circle, cascading), and `status` `ACTIVE` or
  `RETIRING`. The unused `expires` and `grace_period` go.
- `eds_circles`: `rotate_circle_key_due` (timestamp: when the next rotation is due;
  null with the periodic rotation off and no removal). Indexed, as the Status
  counts the Circles that are due.
- `eds_trustees`: `previous_key_id` and `previous_circle_key`, the retiring secret
  while a rotation runs.

**API**, decided in this phase so it needn't change later:
- Circle: `secretRotated` (when the current secret was made), `rotationDue` (the
  timestamp above), and `rotation`, null unless running: `started`, `objects` and
  `objectsDone`.
- Status (STATUS.md), in the inventory: Circles with a rotation due and running, and
  objects still to re-encrypt.
- The socket tells the Circle's members when a rotation starts and ends, so a client
  can show it.

**Tests in the normal build** (H2, small data, in `eds-core`): due after a removal
and after the period; started by a Trustee's request and not by the System
Administrator's; reads, writes, copies and moves into and out of the Circle during a
rotation; an object rewritten or deleted while the job reaches it; the job
interrupted and continued; a second removal during a rotation; external key and
Trustee secrets readable afterwards; the removed Member's rows and the old secret
gone at the end; a MasterKey rotation during a Circle rotation; swapped trustee rows
refused (the new AAD).

**The big-data test** (2026-11-21, the user: "a big-data test using PG, and set the
data amount as a parameter"). `CircleRotationIT` in `eds-testing`, under its own
Maven profile `rotation`, like the load test: `./mvnw -Protation verify -pl
eds-testing`. It needs PostgreSQL: it refuses to start without `EDS_DB_URL`
(with `EDS_DB_USERNAME`, `EDS_DB_PASSWORD`), an empty database, and starts the stone
alone on it.
- It fills a Circle with content of the given amount through the API (streamed
  uploads), in objects of random sizes up to a largest, generated from a seed so
  that nothing is kept on the client: each object can be generated again to compare.
- It removes a Trustee, and with another Trustee's session starts the rotation, then
  waits for its end by the Circle's `rotation`.
- Meanwhile (optional, on by default) a user keeps reading ranges, uploading and
  deleting in the Circle, as during a real rotation.
- Afterwards: every object downloaded and compared with what was generated; in the
  database, no object and no trustee row left on an old secret and one `eds_keys`
  row for the Circle; the removed Member can't read; the stone alive, with no ERROR
  in its log.
- The report (`target/rotation-report.txt`): time and MB/s of the upload and of the
  rotation, objects per second, the stone's heap and CPU from its status samples.

| Property                     | Meaning                                              | Default |
|------------------------------|------------------------------------------------------|---------|
| `eds.rotation.size`          | Amount of content, e.g. `512MiB`, `25GiB`             | `2GiB`  |
| `eds.rotation.object.max`    | Largest object, at most the stone's maximum (1000 MiB)| `256MiB`|
| `eds.rotation.object.min`    | Smallest object                                       | `1KiB`  |
| `eds.rotation.threads`       | `eds.circle.secret.rotation.threads` of the stone     | `2`     |
| `eds.rotation.uploads`       | Uploads at a time while the Circle is filled          | `4`     |
| `eds.rotation.concurrent`    | Users at work during the rotation                     | `true`  |
| `eds.rotation.seed`          | Repeat a run's sizes and content                      | random  |
| `eds.rotation.jvm`           | Options for the stone's JVM, e.g. `-Xmx512m`          |         |

The heap of the stone may be set far below the amount of content, which is the
point: a 25 GiB run with `-Xmx512m` shows that memory stays bounded by the parts.

Done when: the tests of the normal build pass; a big-data run on PostgreSQL passes,
and its size and figures are recorded here; FitNesse shows a rotation after a
removal (the Trustees and the data before and after); `eds.circle.secret.rotation.*`
in the settings, the API description and `RUNNING.md`.

Done 2026-11-21. What differs from the plan above, and details:
- **The retiring secret is kept in the Trustee's own row** (`previous_key_id`,
  `previous_circle_key`), not in a second row. Many queries join the Trustee rows
  (listings, permissions, the last Trustee, shared Circles); a second row per Member
  would have given each of them duplicates. With one row, only the reads by an
  object's secret know of the rotation (`AbstractManager.unwrapCircleKey(trustee,
  keyId)`). It also means at most two secrets per Circle: a removal during a rotation
  starts the next one when this one ends, not a third secret beside it.
- **The hand-off.** `AbstractManager.unwrapCircleKey` offers the unwrapped secrets to
  `rotation.CircleRotations` when the Circle's rotation is due or under way, and one
  job per Circle runs at a time on a stone. eds-quarkus (`RotationRunner`) starts
  the job (`RotationBean`) on a virtual thread **when the request's transaction has
  ended**: the first FitNesse run showed why, as the request which handed the secret
  over wrote with the old one, and the job had already deleted it. The job keeps the
  retiring secret at least 5 seconds after the start, and re-encrypts what was
  written with it meanwhile, before it ends.
- **The start is short**, as it holds the lock of the Circle: the key rows, the
  Trustee rows, the external key and the Trustees' secrets, and the pending
  invitations into the Circle, which are deleted, as they can't be sealed anew
  without their token (a grant whose secret is no longer the Circle's active one is
  dropped when the invitation is accepted). A first version also sealed anew every
  entry of a feature there; the load test showed that a room with a thousand chat
  messages then blocked its chat for seconds.
- **Each Data Object is re-encrypted in its own content row**: the parts are replaced
  and the row switched to the new secret in one transaction, and only that row is
  locked (`SELECT key_id FROM eds_data … FOR UPDATE`; a locking read of the entity
  would join its Data Object, key and Circle, and H2 locks the rows of all of them).
  A first version wrote a new row and deleted the old; with it, the job needed the
  Data Object's row while holding the content's, and requests take the two in either
  order (a delete takes the content first, an upload deletes thumbnails, so their
  Data Objects, first; see also the delete below): the load test found deadlocks both ways. A large object's old
  parts are read on a connection of their own, which sees them as committed.
- **A delete takes the content row before its parts** (2026-11-22): the 2-hour load
  test on PostgreSQL 18 (128 users, 32 rooms, no pauses, 15.5 million operations
  without a failure) ended with a deadlock when a room was deleted while its rotation
  ran. `DataEntity.parts` is an element collection, so Hibernate's bulk delete of the
  entity deletes the rows of `eds_data_parts` first, without the content row, which
  the job holds while it replaces those parts. The content is now deleted with a
  native `DELETE FROM eds_data`, and the database takes the parts with each row; a
  Circle's rows are locked first, in the order of their IDs. Deleting a Circle locks
  the Circle first, as the end of a rotation does before it changes the Trustees; and
  a step of the rotation whose Circle went while it waited for its lock leaves it
  alone (`CommonDao.findAndLockCircle`) instead of failing with an ERROR.
  A copy or move (to another Circle) now holds the source's content row while it reads
  it, and reads its secret from that row: the second load run (30 minutes) had a move
  fail with "Tag mismatch", as the rotation replaced the content and its secret
  between the two reads. **One order for the rows of a Data Object**: the content row
  and the thumbnails' rows are taken together, in the order of their IDs, as the
  delete of the Circle takes them, before anything else changes (`DataDao.
  lockContentWithThumbnails`), by the move, the update with data, the upload and the
  delete. Before, the update took the content first and the others the thumbnails
  first, and a move which held its rows for the whole copy made that a deadlock with
  the delete of the Circle (found by the review). `RotationLockOrderTest` holds the
  rows as the job and the delete of the Circle do, with the thumbnail's row before
  and after the content's, and fails without each part (the move with the same "Tag
  mismatch").
  The review found lock orders which were there before, and consequences of holding
  the source row (tickets 1, 2 and 3; decided 2026-11-22: legit, but not worked on
  then). Done 2026-11-26, see "One lock order for the whole stone" below; what is
  left of ticket 3 is in `FINDINGS.md`. Seen in the load
  runs: the stone logged nothing for a request it answered with 500 (the failed
  move), and on a new database a query met `eds_data_parts` before the schema was
  there (one WARN at the start). Decided and done 2026-11-22: a request answered with
  an error Return Code (5xx; on the endpoints without status codes, the same codes
  in a 200) is logged as WARN with the Return Code and message (`CommonService.
  runRequest`, the download in `DataService` with the Data Object's ID, and a socket
  frame in `SocketHub`, there only after AUTH: a locked stone fails every AUTH, and
  an unknown client could fill the log). A warning (4xx) is not. `CONSTRAINT_ERROR`
  (a name which is taken) answers 500 and is logged so, though it is the request's
  fault (ticket 4 in `FINDINGS.md`). The inventory of the stone's status (`StatusBean.tick(boolean)`)
  waits until `DatabaseStartup` has started, and logs the wait once as INFO; it was
  the inventory which queried the tables while the startup created them. The load is
  sampled meanwhile. `DatabaseStartup.getState` no longer waits for an attempt in
  progress. The tests of eds-core can now see what is logged (`LogCapture`):
  logback, still off by `logback-test.xml`, is the provider of SLF4J, named by the
  tests' `META-INF/services`, as its jar names none for the class path, and
  `slf4j-nop` goes. Reviewed by a reviewer which didn't write it.
- **One lock order for the whole stone** (2026-11-26, tickets 1–3, the user asked
  for the five worst findings to be fixed). Members before Circles, each in the
  order of their IDs, and Circles before the rows in them:
  - **The deletion of a Member** (`ProcessMemberManager.rotateAfterLeaving`) locks
    the Member, then its Circles, then reads each Circle again and marks its
    rotation due, before anything changes. It changed the Circles unlocked, in the
    order of its Trustees, and then deleted the Member: the opposite order of the
    rotation of the MasterKey, which locks all Members, then all Circles (ticket 13),
    so the two could deadlock. Under the locks it checks again that the Member isn't
    the last Trustee of a Circle: before, another Trustee could leave meanwhile, or
    two deletions run at once, and leave a Circle nobody could read again. A Member
    deleted meanwhile is answered as gone, and a Circle deleted meanwhile is passed
    over, instead of an error.
  - **The rotation of the MasterKey** locks the Circles before it deletes pending
    invitees, whose invitations go with them. The old code didn't hit it, as
    Hibernate deferred the delete until after the lock; the order is now explicit.
  - **A second delete of a Circle** which waited for the first one is answered as a
    Circle which isn't there (`IDENTIFICATION_WARNING`), not with an error
    (`CommonDao.lockCircleIfThere`). A rotation put off for a deleted Circle is
    forgotten with it (`CircleRotations.forget`).
  - **A copy whose source was replaced** by an update while it waited for the source
    row copies the new content, instead of failing with "deleted meanwhile", and
    reads its Trustee again once it holds the row, so the secret of a rotation which
    began meanwhile is there.
  - **A put-off rotation of a deleted Circle** is forgotten with it; a job which
    runs isn't touched, as it finds the Circle gone itself.
  - Tested in `RotationLockOrderTest`, `MasterKeyRotationLockTest` and
    `ManagementBeanCircleTest`; each fails without its part, but for the order of
    the invitees, which the old code didn't hit, and the Trustee read again by a
    copy, for which no test can make the rotation win the row. Reviewed by a
    reviewer which didn't write it; its findings are fixed, or listed below.
  - What stays:
    - of ticket 3 (`FINDINGS.md`): copies of one object run one after the other (H2
      has no `FOR SHARE`); a delete of the Circle waits for a large copy;
    - a Trustee added to a Circle while the deletion of its Member waits for another
      Circle goes with the Member, and its Circle isn't marked for a rotation;
    - the MasterKey's part: ticket 53.
- **The ETag stays**: a rotation keeps the header of the stored content (chunk size,
  salt, nonce prefix; `DataStreams.rotate`), as the content doesn't change. The key
  still does, as it comes from the new secret and its version. Without it, a client's
  `If-Match` failed with 412 after a rotation (found by the load test), and a resumed
  download started anew.
- **A download prepared on the retiring secret** carries the new one too: it reads
  the content's secret again in the transaction which streams it, and takes the
  matching one (`ContentDownload.reading`). Without it, content re-encrypted between
  the two transactions of a download failed its check, and was marked damaged.
- **Entries of features** (`EntrySeals`, taken out of `FeatureManager`): their
  author and owner tag are subkeys of the Circle Secret. The job seals them anew in
  batches of 100 under a short lock of the Circle, after the content; meanwhile, a
  request reads an author, finds a Member's own entries and checks who may change
  one with either secret, and reads an entry's content with the secret it is on.
  Features read their Trustee row anew before they unwrap the secret (after the lock,
  for a write).
- **Every writer of Trustee rows locks the Circle and reads the rows anew first**,
  or it and the start of a rotation could undo each other: adding or inviting a
  Trustee, setting a secret, the external key, a trust level, the MasterKey rotation
  (all Circles, in the order of their IDs), and a new passphrase. The load test found
  the last: a passphrase changed while a rotation started left the Member with the new
  secret wrapped to their old keys, and every request of theirs in the room failed.
  The passphrase change takes the locks only after it derived and generated the new
  keys, which takes seconds on a busy host. The end step gives the new secret to a
  Trustee who joined with the retiring one only.
- **An object which fails its check** while it is re-encrypted is marked for the
  sanity check (`FAILED`), and skipped; the rotation then can't end, and keeps the
  retiring secret until the object is deleted or replaced; the Circle's next try waits
  an hour, instead of every request starting a job which fails again. A failure of the
  database (a deadlock, say) only makes the object wait for the next round.
- **Schema**: `eds_data.key_id` no longer cascades (deleting a retiring secret must
  never take content which was written with it meanwhile; the delete then fails and
  the job re-encrypts that content first); `eds_invitations.key_id` cascades;
  `eds_keys.circle_id` cascades with the Circle, and deleting a Circle deletes its
  content before it. Three indexes (`key_circle_idx`, `trustee_previous_key_idx`,
  `circle_rotate_due_idx`).
- **API**: `POST /circles/rotateCircle` and `PUT /circles/{circleId}/rotation` (the
  action `ROTATE`; a Circle administrator or the System Administrator, who sets it due
  only); the Circle's `secretRotated`, `rotationDue` and `rotation` (`CircleRotation`:
  `started`, `objects`, `objectsDone`); the socket frames `ROTATING` and `ROTATED`; the
  inventory's `rotationsDue`, `rotationsRunning` and `rotationObjects`; the settings
  `eds.circle.secret.rotation.days` (0 to 36500) and `.threads` (1 to 64). Rune and
  Tolk don't show a rotation yet; they ignore the two frames (a ticket in
  `FINDINGS.md`).
- **Tests**: `CircleRotationTest` (29, eds-core, H2, each in a room of its own and with
  settings of its own, as the other tests of the module share the test data and the
  settings): due after creation, removal and
  the period, and when the account is deleted; started by a Trustee's read and not by
  the System Administrator's; asked for by a Circle administrator; reads, writes, a
  move out and a new Trustee during the rotation; entries read and changed by their
  author during it; interrupted and continued; a second removal during it; a
  MasterKey rotation and a new passphrase during it; pending invitations deleted;
  swapped wrapped secrets refused; an object which fails its check; the ETag kept;
  external key, Trustee secrets and entries afterwards; a Circle deleted during a
  rotation. `DataStreamsTest` for `rotate`, `SocketHubTest` for the frames,
  `ManagementBeanSettingsTest` for the settings. `EdsRotationTest` (eds-quarkus) over
  HTTP with the runner of the stone: a removal, then a request of a Circle
  administrator, each rotation in the background until its end. FitNesse
  (`EDS.API`): the rotation after the removals of the Trustees section, and one asked
  for, with the data read afterwards.
- **The load test rotates too**: its administrator asks for the rotation of a shared
  room now and then (1 in 20 of its actions), and the users' requests start it; what
  they keep is compared byte for byte at the end, as always. It found the five
  problems above; with them fixed, runs of 5 minutes (16 users, H2) had 5, 7 and 8
  rotations under load, no failed request, no WARN or ERROR. A user who logs out and
  in again now says first that it ends its Session, as for a new passphrase, so an
  error the stone sends the old socket meanwhile counts as expected (a run had one).
- **SonarQube**: the quality gate passes (no new violations, coverage of new code
  93.3 %).
- **The big-data test** passed on PostgreSQL 18, on the dev VM, with the defaults and
  the final code: 2 GiB in 42 objects of up to 256 MiB, rotated in 164 s (13.1 MB/s,
  heap of the stone up to 561 MiB, CPU up to 30 %), while a user read 509 ranges and
  made 51 uploads and 20 deletes; all 73 objects compared byte for byte afterwards,
  nothing left on the old secret, no WARN or ERROR in the stone's log (the upload of
  the 2 GiB took 142 s, 15.1 MB/s). A run of tens of GiB is for the final test system.

### Open points

- ~~Exact hybrid combiner labels and wire format~~: fixed in C3, vectors in C4.
- ~~Default PBKDF2 iteration count for SHA-512~~: decided in C3, 210000 (OWASP).
- Whether to offer pure ML-KEM-1024 as an opt-in, and how it is selected per circle.
- ~~Chunk size for the §4 format~~: decided in C4, 64 KiB.
- Maximum object size: see *Streaming*, "Open points". Since content is stored in
  parts (*Streaming*, "Content in parts", 2026-11-18), no field holds more than a
  part and `substring` is no longer used, so PostgreSQL's 1 GB field limit and H2's
  handling of large values are no longer the bound.
- ~~Per-object download tokens (§2a)~~: done in L5.
- ~~`ManagementBean`/`ShareBean` should call `setRollbackOnly()` when catching
  `EDSException`~~: done in Step 0.
- ~~Rotating a Circle Secret (when a Trustee is removed, and at intervals, §4)~~: done
  in C6 (2026-11-21).
- The checksum over the stored ciphertext uses `eds.crypto.hash.algorithm`: SHA-512
  by default, which is what this plan names, or SHA-384. The sanity job checks
  content with the algorithm its checksum was stored with, told by the checksum's
  length (2026-11-13), so the setting can change while content exists.
- Bugs and improvements found along the way are tickets in `FINDINGS.md`.
## Streaming

Status: done (S1–S5, 2026-11-02); see the "Done" notes under each phase. Changed
since: a streamed copy and the storage ceiling (2026-11-04), download tokens (L5),
uploads without `Content-Length` (2026-11-11), content in parts (2026-11-18), the
storage ceiling per Circle (2026-11-23), and connections and locks held for less
(2026-11-25).
Scope: streaming upload and download of Data Objects, including byte ranges.
The endpoints are documented to the *OpenAPI* standard in the phase they're added.
Related:
- *Crypto* §4 defines the chunked storage format (keys, nonces, header, AAD), §2a
  defines sessions, and Phase C5 the streaming crypto API. This file doesn't repeat
  them; it covers the API, storage access, transactions, safe serving and the server
  code.
- *Clients API* later turns the endpoints defined here into the OpenAPI contract;
  Rune and Tolk are built on them (`RUNE.md`, `TOLK.md`).
- A parked prototype (a branch since deleted) streamed uploads with bounded memory,
  but downloads still loaded the whole value (pgjdbc reads a `bytea` value fully).

### Goals

1. Upload and download Data Objects of up to about 1 GB with bounded server memory.
2. Serve byte ranges (`Range`), so media can be played and seeked without
   downloading the whole object.
3. Keep the database unaware of the content structure. (Until 2026-11-18 this
   also meant one value per object, `eds_data.encrypted_data`, and no table of
   chunks. Since then the data is stored in parts, which are ranges of bytes and
   know nothing of the content either: "Content in parts" below.)
4. Plain Java/JCE and the existing stack only; no new third-party libraries.
5. One storage format for all objects, whether they arrive by streaming or through
   the existing JSON `byte[] data` field.

### Non-goals

- Objects above PostgreSQL's 1 GB field limit (would need large objects; see Open
  points).
- Resumable or multi-part uploads. An interrupted upload is rolled back and must be
  sent again.
- Server-side transcoding, thumbnails or media handling (*Clients API*).

### Dependencies

| Needs                                         | From            |
|-----------------------------------------------|-----------------|
| Chunked AES-256-GCM format and header         | *Crypto* §4  |
| HKDF object key from the Circle Secret        | *Crypto* C4  |
| Sessions without PBKDF2 per request           | *Crypto* C3  |
| Explicit nonce/AAD API, nonce type            | *Crypto* C2  |
| Chunk size, header layout, test vectors       | *Crypto* C4  |
| Encrypting stream, range decryptor, re-encryption helper | *Crypto* C5 |

Streaming can be built on the old key hierarchy for experiments, but it should not be
released before C3 and C4: without sessions, every `Range` request runs PBKDF2.

### Flow

The two-step flow from the prototype stays: register first, then send the content in
a separate call.

1. **Register**: `addData` without `data` creates the metadata and returns the
   `dataId`. (Small objects can still send `data` inline, see "JSON path".)
2. **Upload**: `PUT` the raw bytes to the object's content resource. Any existing
   content is replaced.
3. **Download**: `GET` the content resource, optionally with `Range`.

### API

Endpoint names below are proposals. They're added code-first (MicroProfile OpenAPI
annotations), like the rest of the current API; *Clients API* L1 then moves them into
the OpenAPI contract unchanged.

#### Authentication

- Streaming requests carry the session token in a header
  (`Authorization: Bearer <token>`), because the body is raw data and can't hold the
  JSON authentication fields.
- The prototype's credential headers (`X-EDS-AccountName`, `X-EDS-Credential`, …) are
  dropped: they put the password in a header on every request and run PBKDF2 each
  time.
- Media elements in the PWA (`<video src>`, `<img src>`) can't send custom headers.
  This needs either a short-lived download token for one object (*Crypto* §2a) or
  a same-origin session cookie. Decided in L5: a download token per object.

#### Upload: `PUT /data/{dataId}/content`

- `Content-Type: application/octet-stream`; the media type for serving is part of the
  metadata, not taken from this header.
- `Content-Length` is the normal case, and given whenever the client knows it. With
  it, content above the configured maximum is refused with `413 Content Too Large`
  before the body is read, and the storage ceiling is checked up front.
- Without `Content-Length` (changed 2026-11-11; `411 Length Required` and the return
  code `LENGTH_REQUIRED` are gone): the body is read to its end, for true pipes
  (`… | tolk send -`). The same format is written; which chunk is the last is found
  by reading one byte ahead. (In S1, pgjdbc spooled such a stream to a temp file.
  Since "Content in parts" each part is read into a buffer, from 64 KiB up to the
  part size, and inserted; there is no temp file.) The limits are enforced while reading: the plaintext is cut off
  where it passes the maximum object size (`413`) or what is left below the storage
  ceiling (`507`), and the stored length is checked against the ceiling again after
  the write; either way the transaction is rolled back and the previous content
  stays.
- Optional `If-Match: <etag>` to avoid overwriting a newer upload (see ETag below).
- Response: JSON, as for the other data operations, with the new ETag.

#### Download: `GET /data/{dataId}/content`

- Without `Range`: `200`, full content.
- With `Range: bytes=a-b` (single range only): `206 Partial Content` with
  `Content-Range`. Unsatisfiable → `416` with `Content-Range: bytes */<size>`.
  Multiple ranges → serve the full content with `200` (allowed by RFC 9110).
- Always: `Accept-Ranges: bytes`, `Content-Length`, `ETag`, `Cache-Control: private,
  no-store`, `X-Content-Type-Options: nosniff`, and `Content-Type` /
  `Content-Disposition` as defined in "Serving user content safely" below.
- `If-Range: <etag>`: serve the range only if the object hasn't changed, otherwise the
  full content. This stops a player from mixing ranges from two different uploads.
- `HEAD` returns the same headers without a body, so players can get the size.
- ETag: derived from the header's per-write HKDF salt, e.g. the first 16 bytes of
  `SHA-512(salt)`, Base64url. The salt changes on every write, so the ETag changes with
  the content, and computing it only needs the header.
- Errors found before the response starts (auth, not found, bad range) return the
  usual JSON error. Errors after the response has started (a chunk fails its GCM tag,
  DB error) can't be reported in-band: the server aborts the connection, logs the
  error and marks the object as failed (`sanity_status`). A chunk is never sent before
  its tag has been verified.

#### Serving user content safely

Content is untrusted. These rules apply to every content response; the PWA's CSP
and preview limits are in `RUNE.md`.

- `X-Content-Type-Options: nosniff` on all content responses.
- Since L5 also `Content-Security-Policy: default-src 'none'; …; sandbox`,
  `Cross-Origin-Resource-Policy: same-origin` and `Referrer-Policy: no-referrer`
  (`ContentPolicy`); the last because a download token may be part of the URL
  (`GET`/`HEAD …/content?token=…`, *Crypto* §2a).
- Explicit `Content-Type` from an allowlist for inline display (JPEG, PNG, WebP, MP4,
  WebM); everything else is served as `application/octet-stream` with
  `Content-Disposition: attachment`.
- SVG is never served for inline display (only as an attachment); the PWA may render
  it through `<img>` only.
- The media type comes from the object's metadata, set when the object is registered
  (new nullable column on `eds_metadata`, e.g. `media_type`), never from sniffing the
  content or from the upload's `Content-Type` header.

#### JSON path

- `addData`/`updateData` with `byte[] data` and `fetchData` returning `data` stay for
  small objects. They use the same chunked format internally.
- A configurable limit (e.g. 10 MB) applies to inline data. Larger objects must use
  the streaming endpoints; `fetchData` on a larger object returns the metadata and a
  return code that says so, instead of loading it into memory.

### Storage access (`DataDao`)

This is how it was built in S1, with the data of an object as one value. Since
2026-11-18 the value is stored in parts, and what is said here about `substring`,
`octet_length`, `STORAGE EXTERNAL` and H2 no longer holds: see "Content in parts".

- **Write**: one `UPDATE eds_data SET encrypted_data = ? …` (or `INSERT`) with
  `setBinaryStream(stream, exactLength)`, where the stream encrypts chunk by chunk as
  pgjdbc reads it, and a SHA-512 digest runs over the ciphertext (for the sanity job,
  *Crypto* §4). JPA is only used for the non-blob columns, as in the prototype
  (`saveAndDetach` + native write).
- **Read**: `substring(encrypted_data from ? for ?)`, one query per batch of chunks
  (e.g. 1–4 MB per query). Memory per request is one batch.
- **Size**: `octet_length(encrypted_data)` gives the blob size without reading the
  value; the plaintext size follows from the format.
- **Schema**: `ALTER TABLE eds_data ALTER COLUMN encrypted_data SET STORAGE EXTERNAL`
  on PostgreSQL, so `substring` and `octet_length` only fetch what they need (ciphertext
  doesn't compress anyway). (`initial_vector` is already dropped in *Crypto* C4.)
  Add the media type column to `eds_metadata` (see "Serving user content safely").
- **H2**: must work functionally. Bounded memory is only guaranteed on PostgreSQL
  unless verification shows H2 handles `substring` on its `BYTEA` mapping without
  loading the whole value (Open points).

### Transactions

- **Upload**: one transaction covering delete/insert of the row and the stream write,
  with its own configurable timeout (prototype: `eds.stream.transaction-timeout`,
  default 3600 s). A failure or dropped connection rolls back, so readers keep seeing
  the previous content (PostgreSQL MVCC).
- **Download**: one read-only transaction at `REPEATABLE READ` for the whole response.
  The batches are separate queries; without a snapshot, an upload that commits
  mid-download could make later batches come from different content. The same
  stream timeout applies.
- Long transactions hold back VACUUM on PostgreSQL. That's acceptable at the expected
  object sizes, but keep the timeout configurable and document it.
- Fixed in Step 0: `ShareBean`/`ManagementBean` caught `EDSException` without
  `setRollbackOnly()`. For streaming, a half-written
  object must never commit.

### Server code

- **Quarkus REST**: streaming endpoints run on worker threads (`@Blocking` or the
  default for `InputStream`/`StreamingOutput` signatures; verify for the Quarkus
  version). The download writes through `StreamingOutput`, and the transaction must
  span the write, as in the prototype's `prepareDownload` + `streamData` split.
- **Limits**: `quarkus.http.limits.max-body-size` must allow the configured maximum
  object size; the EDS limit is checked against `Content-Length` first.
- **Managers** (reuse and adapt from the prototype):
  - Upload: `UploadDataManager` (checks: object exists, not a folder, member has write
    access), now with the chunked format, an optional length and the ETag.
  - Download: `DownloadDataManager` + `DataStream`, now with range calculation
    (requested bytes → chunk indexes → blob offsets), batch reads, per-chunk
    decryption and trimming of the first and last chunk.
- **Copy/move**:
  - Move within the same circle: no re-encryption (AAD and key don't include the
    folder).
  - Copy, or move to another circle: re-encrypt chunk by chunk, reading the source
    in batches and writing the target as a stream with its known length.
- **Sanity job**: reads each blob in batches and computes the SHA-512 over the
  ciphertext; no key needed. Interval and reporting as in *Crypto* §4.

### Configuration

| Setting | Default | Purpose |
|---|---|---|
| `eds.stream.max.object.size` | `1048576000` (1000 MiB) | Upload limit |
| `eds.stream.transaction.timeout.seconds` | `3600` | Upload/download transaction timeout |
| `eds.stream.read.batch.size` | `4194304` (4 MiB, 64 KiB–16 MiB) | The size of a stored part |
| `eds.data.max.inline.size` | `10485760` (10 MiB) | Limit for the JSON `byte[] data` path |
| `eds.data.max.total.size` | `0` (none) | The storage ceiling |
| `quarkus.http.limits.max-body-size` | `1100M`, ≥ max object size | HTTP layer limit |

The chunk size is part of the format and stored in each object's header; it isn't a
runtime setting (see *Crypto* Open points).

### Clients

- **Java test client** (`eds-client`): upload and download methods with
  `InputStream`/`OutputStream`, as in the prototype's `GsonRestClient`, switched to
  session auth.
- **FitNesse**: fixtures for upload, full download, range download and the error
  cases.
- **Tolk and Rune**: built afterwards (*Clients API*): Rune on a generated client,
  Tolk on a hand-written one checked against the contract; the
  existing `eds-client` gets the streaming methods now and is retired in *Clients API*
  L3.

### Phases

Each phase ends with `mvn clean install` green and a short summary of deviations.

#### Phase S1: Storage access

- `DataDao`: streamed write with exact length, batched `substring` reads,
  `octet_length`.
- Schema: `STORAGE EXTERNAL` on `encrypted_data`, media type column on
  `eds_metadata`.
- Verify pgjdbc (unknown-length buffering) and H2 (`substring` memory behaviour), and
  record the results in this file.

Done when: DAO tests write and read back a large blob in batches on PostgreSQL and H2,
and a heap-limited test (e.g. `-Xmx128m` with a 512 MB blob) passes on PostgreSQL.

Done 2026-11-02. Results and deviations:
- `model/EncryptedDataStore` (plain JDBC on a `Connection`): `write` (stream of exact
  length, SHA-512 checksum over the bytes written, stored in the same transaction),
  `length` (`octet_length`), `read` (`substring`), `open` (an InputStream that reads
  in batches, one query each). `DataDao.withConnection` runs it in the
  EntityManager's transaction via Hibernate's `Session.doReturningWork`; so
  `hibernate-core` is now `provided` in `eds-core`, as in the prototype.
- `eds_metadata.media_type VARCHAR(255)`; on PostgreSQL the
  `SchemaManager` also sets `encrypted_data` to `STORAGE EXTERNAL` (gone since
  "Content in parts").
- Tests: `EncryptedDataStoreTest` writes 20 MB and reads it back whole, in ranges and
  across batch boundaries, and checks short streams, reads past the end and unknown
  rows. It always runs on H2. It runs on PostgreSQL when `EDS_TEST_POSTGRESQL_URL` (or
  `-Deds.test.postgresql.url`) is set, together with the heap-limited check: a child
  JVM with `-Xmx128m` writes a 512 MB blob and reads it back in 4 MB batches. Verified
  against a local PostgreSQL 18.6: all pass. The PostgreSQL driver (42.7.13, the
  version in the Quarkus BOM) is a test dependency of `eds-core`.
- **Verified: pgjdbc 42.7.13** streams `setBinaryStream(stream, length)` to the
  server (512 MB with a 128 MB heap: about 7 s write, 3 s read back). Without a length it
  does **not** buffer in memory: it spools the stream to a local temp file
  (`postgres-pgjdbc-stream*.tmp`, deleted afterwards), then sends it. As EDS passes
  the encrypting stream, that file only holds ciphertext. `substring(bytea)` takes
  `int` positions, so offsets are bound as `int` (objects stay below 1 GB).
- **Verified: H2 2.4** maps `BYTEA` to `BINARY VARYING` with a limit of
  1,000,000,000 bytes, and holds the value in memory: a 64 MB object already fails
  with a 128 MB heap. H2 is "functional, not memory-bounded", for tests and small
  installations.

#### Phase S2: Upload

- `PUT` endpoint, session auth, required `Content-Length` (no longer required since
  2026-11-11, see "Upload"), size limit, `If-Match`.
- Encrypt chunk by chunk while streaming; SHA-512 digest on the fly; ETag.
- One transaction with rollback on any failure.

Done when: uploads of empty, one-chunk, exact-multiple and partial-last-chunk objects
round-trip; an aborted upload leaves the previous content intact.

Done 2026-11-02. Results and deviations:
- `PUT /data/{dataId}/content` (`DataService.uploadContent`): `Authorization: Bearer
  <session token>` (Base64 as returned by `login`), `Content-Length` required (then), size
  checked before the body is read, optional `If-Match` (an ETag, or `*`). The
  response is the JSON `ProcessDataResponse` with the new `etag` field, plus the
  `ETag` header. **Real HTTP status codes** (see "Decisions that cross chapters"): the
  status is the HTTP code of the `returnCode`. New return codes for this:
  `LENGTH_REQUIRED` (411, removed again 2026-11-11, when uploads without a length
  were accepted), `PRECONDITION_FAILED` (412), `CONTENT_TOO_LARGE` (413).
- `UploadDataManager`: session auth, the Data Object must exist and not be a folder,
  trust level `WRITE`; the content is encrypted with `DataStreams.encrypt` (§4,
  current Circle Secret) while `EncryptedDataStore.write` streams it to the database,
  with the SHA-512 of the ciphertext as checksum. The entity is detached before the
  JDBC write, so a later flush can't overwrite it.
- Transactions: `ShareBean.uploadContent` suspends the container transaction
  (`NOT_SUPPORTED`) and runs its own JTA transaction through `UserTransaction` with
  the stream timeout (`StreamTransaction`), so `eds-core` stays independent of
  Quarkus' `@TransactionConfiguration`. Any failure, including a body that ends
  early, marks it rollback-only.
- Settings: as EDS settings (adjustable via the settings request, like the others),
  with dotted names: `eds.stream.max.object.size` (1048576000 bytes = 1000 MiB),
  `eds.stream.transaction.timeout.seconds` (3600), `eds.stream.read.batch.size`
  (4194304, used from S3). `quarkus.http.limits.max-body-size=1100M`.
- ETag: `"` + Base64url(first 16 bytes of SHA-512(header salt)) + `"`
  (`DataFormat.etag`).
- Tests: `ShareBeanUploadTest` (round trips for 0, 1, 1000 bytes, one and two chunks
  exactly and 200000 bytes; replace and If-Match; a stream failing half way and a
  body shorter than its length keep the previous content; missing or unknown
  session, missing length, unknown object, READ-only member, too large, folder).
  `EdsStreamingTest` (eds-quarkus, over HTTP): Bearer upload, ETag header, If-Match
  412, 403/404, a real chunked request (411 then, accepted since 2026-11-11), and a client that aborts after 100000
  of 300000 bytes, after which the previous content is still there.

#### Phase S3: Download and ranges

- `GET`/`HEAD` endpoint with `Range`, `If-Range`, `206`/`416`, the safe-serving
  headers.
- `REPEATABLE READ` read-only transaction; batch reads; per-chunk decrypt and trim.
- Abort and mark the object as failed on a tag failure mid-stream.

Done when: range reads at and around chunk boundaries (first byte, last byte, one
byte, spanning two chunks, suffix range `bytes=-n`) return the right bytes; a
tampered chunk aborts the response; a download during a concurrent upload returns
consistent content; the heap-limited test passes for downloads.

Done 2026-11-02. Results and deviations:
- `GET` and `HEAD /data/{dataId}/content` (Bearer session, trust level `READ`): `200`
  or `206` with `Content-Range`; `416` with `Content-Range: bytes */size`
  (`RANGE_NOT_SATISFIABLE`); `If-Range`; one range only (multiple or invalid ranges
  give the full content, `stream/ByteRange`). Headers: `Accept-Ranges`,
  `Content-Length`, `ETag`, `Cache-Control: private, no-store`,
  `X-Content-Type-Options: nosniff`, `Content-Type` and `Content-Disposition` from
  `stream/ContentPolicy`: inline only for JPEG, PNG, WebP, MP4 and WebM; everything
  else (SVG and HTML included) as `application/octet-stream`, attachment; file name
  per RFC 6266. Errors before the response starts are JSON with their HTTP status.
- `mediaType` is a new optional field of `addData`/`updateData` (validated as
  `type/subtype`), stored in `eds_metadata.media_type`, copied by copy/move, and
  returned in the Metadata.
- Two steps: `DownloadDataManager.prepare` (short transaction: auth, access, header
  and `octet_length`, Range) gives a `ContentDownload`; `ShareBean.streamContent` then
  streams it in its own read-only transaction with the stream timeout
  (`ContentStreamer`). On PostgreSQL that transaction starts with `SET TRANSACTION
  ISOLATION LEVEL REPEATABLE READ READ ONLY`, and the header and length are read again
  in the snapshot: if an upload committed between the two steps, the download is
  aborted instead of mixing contents. On H2 there is no snapshot; a change mid-stream
  makes a chunk fail its tag and the download is aborted, so content is never mixed
  silently.
- A chunk is only written after its tag is verified. A failed check aborts the
  connection (IOException from the StreamingOutput) and sets `sanity_status` to
  `FAILED` in a separate transaction; a client that goes away doesn't.
- Found and fixed on the way: the upload (S2) loaded the old `DataEntity`, i.e. the
  whole old blob, before replacing it. Upload and download now only read the row id
  and key (`data.findContentInfoByMetadata`) and update via JPQL.
- Tests: `ShareBeanDownloadTest` (full; first and last byte, one byte, a whole chunk,
  across a boundary, open-ended, suffix; If-Range; 416; empty content; serving
  headers; errors incl. READ access; a tampered third chunk aborts after at most two
  verified chunks and marks the object failed; a change after prepare aborts without
  marking it), `ByteRangeAndPolicyTest`, `EdsStreamingTest.testDownloadOverHttp` (the
  same over HTTP, incl. HEAD). On PostgreSQL (`EncryptedDataStoreTest`): the
  REPEATABLE READ snapshot keeps the first content while an upload commits mid-read
  (and without it the later batches change), and the heap-limited child JVM
  (`-Xmx128m`) encrypts, stores and decrypts 512 MB and reads a 1 MB range in the
  middle. Verified against PostgreSQL 18.6.
- Results: `eds-api` 154, `eds-core` 548 (2 skipped without a PostgreSQL test
  database), `eds-quarkus` 16 tests passed; FitNesse `EDS` suite 335 right, 0 wrong.

#### Phase S4: JSON path, copy/move, sanity job

- Inline limit for `byte[] data`; `fetchData` refuses large objects.
- Chunk-by-chunk re-encryption for copy and cross-circle move.
- Sanity job with batched reads, a configurable interval (including off), and status
  reported through the health endpoint and a metric (*Crypto* §4).

Done when: existing data suites pass unchanged for small objects; copy/move of a
large object works with bounded heap; a corrupted blob shows up in the health/metric
output.

Done 2026-11-02. Results and deviations:
- Inline limit `eds.data.max.inline.size` (10485760): `addData`/`updateData` with
  larger `data` → `CONTENT_TOO_LARGE`; `fetchData` checks the size from
  `octet_length` and the header first, and for larger content returns the metadata
  without `data` and `CONTENT_TOO_LARGE`, pointing to the content endpoint.
- Copy and cross-circle move re-encrypt chunk by chunk (`stream/ContentCopier`, on
  `DataStreams.reencrypt`). One connection can't run the source's batch queries while
  it receives the target's streamed statement (the reason before "Content in
  parts"), so above the inline limit the source
  is read on a second connection (`ConnectionSource`, a reader thread in a
  `REPEATABLE READ` read-only transaction) and handed over in 64 KiB chunks through a
  bounded queue: source → decrypt → encrypt → target, nothing buffered. Up to the
  inline limit, the copy is made in memory on the request's connection. (First
  implemented with a temp-file buffer; replaced after review, 2026-11-04.) Copy and
  move run in a transaction with the stream timeout. Delete, update and move no longer load
  the old content either (JPQL bulk delete; a managed instance is detached by
  reference first, so its cascade can't re-persist the metadata).
- Sanity job: the SHA-512 over the ciphertext is computed while reading in batches;
  every Data Object is checked in its own transaction with the stream timeout
  (`SanitizerBean` no longer runs a whole pass in one transaction). New setting
  `eds.sanity.check.max.objects` (1000 per hourly run, `0` = no limit), and
  `eds.sanity.check.interval.days = 0` switches the checks off. A pass never loops on
  objects it can't check.
- Reporting: `SanityReport` (last run, checked, failed in the run, failed in total;
  also updated when a download finds a damaged object) and the health group
  `/q/health/group/sanity` (eds-quarkus, SmallRye Health), DOWN while any object is
  marked as failed. It is not part of readiness, so damaged data doesn't take EDS out
  of service. **Deviation: no metric.** The stack has no metrics library, and adding
  one (e.g. `quarkus-micrometer`) is a new dependency, which needs agreement. The
  health group gives monitoring the same signal.
- Storage ceiling (added after review, 2026-11-04; default by kind of system
  2026-11-10): `eds.data.max.total.size` limits all stored, encrypted content
  together, checked before uploads, the JSON `data` and copies. `0` (default) means
  2 GiB on a test system (in-memory H2) and 250 GiB on a production system; a
  positive value sets it explicitly (`507 INSUFFICIENT_STORAGE`); replaced content doesn't
  count, a move adds nothing. No lock, so concurrent writes may exceed it by what
  they add. Found on the way: a runtime exception from a stream given to pgjdbc (e.g.
  a damaged chunk's `CryptoException`) left the connection mid-message;
  `EncryptedDataStore.write` now passes every stream failure on as an IOException,
  which pgjdbc handles cleanly. `SchemaManager` now only looks for its tables in the
  connection's current schema.
- Tests: `ShareBeanLargeDataTest` (inline limits; fetch of large content; copy and
  move of 300 KB streamed content through the reader connection; delete; the
  storage ceiling), `ContentCopierTest` (streamed, in memory, empty, damaged source;
  H2 and PostgreSQL),
  `SanitizerBeanTest` (a damaged blob is found by the batched digest, marked failed
  and reported; off and limit), `EdsStreamingTest.testSanityHealthGroup`. On
  PostgreSQL, the heap-limited child JVM (`-Xmx128m`) also copies a 512 MB object to
  another "circle" via `ContentCopier` and decrypts the copy. The existing data suites
  (unit and FitNesse) pass unchanged.
- Results: `eds-api` 154, `eds-core` 556 (2 skipped without a PostgreSQL test
  database), `eds-quarkus` 17 tests passed; FitNesse `EDS` suite 335 right, 0 wrong.

#### Phase S5: Clients and acceptance tests

- Streaming methods in `eds-client`, FitNesse fixtures and suites.

Done when: FitNesse suites for streaming pass against the runner JAR.

Done 2026-11-02. Results:
- `eds-client`: `ShareRestClient.uploadContent`, `downloadContent` (to an
  `OutputStream`, with `Range`/`If-Range`) and `contentHeaders` (HEAD), with the
  Session as Bearer token; `ContentResponse` holds the status and headers, or the
  JSON error. They are extra methods, not part of the `Share` interface (which is the
  JSON API). `RestClientTest.testStreamingContent` covers them against a running
  instance (like the other client tests, only with `EDS_LOCAL_INSTANCE=true`).
  The variable was renamed from `local.instance.running`, which the `mvn` shell
  script dropped because of the dots.
- FitNesse: fixtures `UploadContent` and `DownloadContent` (sessions by the name
  given at login, as in C3; deterministic generated content for large objects), a
  `media type` column for `ProcessData`, and the section "Content Streaming" on the
  API page: text upload, full download, ranges (`bytes=4-8`, suffix, stale If-Range),
  HEAD, the inline/attachment rule, a 200000-byte object (full, across the first
  chunk boundary, last byte, 416), the 403/404/412 errors, and cleanup.
- Result against the runner JAR (H2, started locked): FitNesse `EDS` suite 416 right,
  0 wrong, twice in a row on the same instance. `mvn clean install` green.

### Content in parts (2026-11-18)

Status: done. Asked for by the user when other databases than PostgreSQL came up
(a stone in AWS, and then MySQL, Oracle, SQL Server): the entities and the queries
are plain JPA and go with any of them, but the stored content did not.

- **What was in the way.** The data of an object was one value of up to 1000 MiB,
  read in ranges with `substring` and measured with `octet_length`. PostgreSQL does
  that well (`STORAGE EXTERNAL`); the functions are spelled differently elsewhere,
  some databases read the whole value to give a range of it, and several limit what
  one statement or one row may carry to about 1 GiB or less. H2 held the value in
  memory as a whole.
- **Parts.** The data is a row of `eds_data_parts` for each part of it:
  `(data_id, part_offset, part_length, part)`, with `eds_data.encrypted_data` gone.
  A part is a range of the bytes and nothing more. Where one ends says nothing about
  the content, and the parts are not the chunks of the format (*Crypto* §4), so the
  database is as unaware of the content as before; this takes back the "no chunk
  table" of the goals in its letter, not in what it was for.
- **Each part tells where it starts and how long it is.** A range is found by these
  two, the length of the data is the sum of the lengths, and so the size of the
  parts can change without anything stored being written anew. The first part is
  short (4096 bytes), so the header of the data, which is read for every listing
  and before every download, comes without the rest. The others have the size of
  `eds.stream.read.batch.size` when they were written (4 MiB), kept within 64 KiB
  and 16 MiB.
- **Only SQL every database knows**: equality and comparison on the two numbers,
  `SUM`, and a binary value of at most a part. No `substring`, no `octet_length`,
  and nothing in the schema for PostgreSQL alone any more.
- **Writing.** `EncryptedDataStore.write` and `writeToTheEnd` read the stream part
  by part and insert each as it has arrived. Content without a length (a pipe) is
  no longer spooled to a temporary file by the driver. A write is several
  statements now, so it is undone to a savepoint when it fails, and what was
  stored before stays as it did when the write was one statement; where a
  connection has no savepoints, the rollback of the caller does it.
- **Reading.** One query for each part, by the offset. `open(…, batchSize)` keeps
  its signature; a query reads a part, of the size it was written with.
- **Small data through JPA.** `DataEntity.getData()`/`setData()` stay, for data
  small enough for memory (the JSON path, thumbnails, entries of features): the
  parts are an element collection (`DataPart`), read only when the data is asked
  for, and replaced as a whole when it is set.
- **Memory** is bounded by a part on H2 as well, not only on PostgreSQL.
- **The schema**, with three changes which cost nothing and remove
  differences between databases: `NOT NULL` on the columns instead of 107 `CHECK`
  constraints, `DEFAULT CURRENT_TIMESTAMP` instead of `now()`, and
  `eds_entries.closed` as `INTEGER` (0 or 1), as not every database has `BOOLEAN`.
- **What still differs between databases**, and needs a script for each: the names
  of the types `BYTEA` and `TEXT`, and the identity columns (not in MySQL, MariaDB
  and SQL Server in this spelling). In the code: the lock which lets one instance
  create the schema (`pg_advisory_xact_lock`), and the snapshot of a download
  (`REPEATABLE READ`), both only set on PostgreSQL. No other database is supported
  by this; it is what stands in the way of one that got smaller.
- **Tests.** `EncryptedDataStoreTest` checks the layout of the parts, another part
  size, a stream to its end, a row without content and a failed write without a
  rollback, on H2 and PostgreSQL (2026-11-18, PostgreSQL 18: also the snapshot,
  and 512 MiB written, read and copied with a heap of 128 MiB).

### The storage ceiling: a total for each Circle (2026-11-23)

Status: done 2026-11-24 (ticket 23, which has left `FINDINGS.md`).

- **Why.** Before every write, also of every chat message, the ceiling check summed
  `part_length` over all of `eds_data_parts` (`EncryptedDataStore.totalLength`). The
  cost of a write grew with everything the stone holds: in the 5-hour load run on
  PostgreSQL the requests went from 736 to 309 per 5 s while the CPU went idle, and
  the table was read in full 1.7 million times.
- **The total, in a table of its own** (decided by the user, 2026-11-24):
  `eds_circle_storage` (`circle_id`, the key and a foreign key to the Circle with
  `ON DELETE CASCADE`; `stored_bytes BIGINT DEFAULT 0 NOT NULL`), a row for each
  Circle: the sum of `part_length` of the parts of its objects. It changes in the
  same transaction as the parts, so it is exact, and every stone on the same
  database sees the same number. Not a column of `eds_circles`, as first planned: a
  write would then lock the Circle's row while it holds content rows, the reverse
  of the order in which deleting a Circle takes them (Circle first,
  `ProcessCircleManager.deleteCircle`), and the two could deadlock. A write never
  waits for the Circle's row. The total isn't part of `CircleEntity`, so a save of
  a Circle can't write an old value back.
- **One statement, never read and write.**
  `UPDATE eds_circle_storage SET stored_bytes = stored_bytes + ? WHERE circle_id = ?`
  (`CommonDao.changeStoredBytes`). Reading, adding and saving through JPA would lose
  updates without notice, as the entities have no `@Version`. The statement locks
  the row until the commit, so writes to one Circle wait for each other and writes
  to other Circles don't; it comes after the parts are written, to keep that wait
  short.
- **Where the total changes**, all in `CommonDao` and `DataDao`:
  - `CommonDao.save` of a new `CircleEntity` makes its row.
  - `CommonDao.save` of a new `DataEntity` adds its content (content given with
    `setData` is only ever given to a new entity): the inline data of
    `ProcessDataManager` (add, update) and `AbstractManager.encryptAndSave`
    (entries, thumbnails). The empty placeholders of an upload and a copy add 0.
  - `DataDao.writeContent` measures the stored length of the row before and after
    the write and adds the difference: an upload with and without a length
    (`UploadDataManager`), a copy (`ProcessDataManager`, to the target Circle), and
    the rotation (`RotationManager.reencrypt`). The rotation keeps the length (the
    header keeps its chunk size), so it changes nothing and doesn't take the row.
  - `DataDao.deleteContent` locks the content row, takes its parts from the total,
    then deletes it. Through it go the delete of an object and its thumbnails
    (`deleteThumbnails`), replaced inline content, a move's source, entries
    changed, replaced, removed or pruned, and those of a feature removed from a
    Circle.
  - `CommonDao.delete` of a `MetadataEntity` or `DataEntity` takes what is still
    stored, for parts `ON DELETE CASCADE` would remove unseen (no caller leaves
    any today; it costs one indexed query).
  - The delete of a Circle changes nothing: its row goes with it. A folder has no
    content; its children are deleted one by one (a feature's folder), or it must
    be empty.

  A change is written at once, unless held (`holdStoredBytes`/`writeStoredBytes`):
  a move holds the changes of both Circles and writes them at its end, in the order
  of the Circles' IDs, so two moves can't deadlock. An upload deletes the old
  thumbnails after the content is written, not before, so the row isn't held while
  it streams (the ceiling check then still counts the old thumbnail, a few KB).
- **The check** (`StorageCeiling`): `CommonDao.storedBytes()`, i.e.
  `SUM(stored_bytes)` (a row for each Circle, not each part) plus the changes held,
  less `EncryptedDataStore.length` of the content being replaced.
  `EncryptedDataStore.totalLength` is gone. As before there is no lock on the
  whole: writes in different Circles can together pass the ceiling by what they add
  ("Storage ceiling", S4).
- **Kept honest.** The inventory reads `storedBytes()` as `storageUsed`, and
  `StatusManager.checkStoredBytes` compares in one statement each Circle's total
  with the sum of its parts, logging a WARN with the internal id and both numbers
  (-1 for a Circle without its row). It corrects nothing, as that would race the
  writers; a difference is a path above that was missed. Its cost: ticket 27.
- **Schema.** The version stays 4 (see "Schema version"); a database of an earlier
  2.0 build is created anew. Found by the same load runs and changed with it:
  indexes `data_key_idx` on `eds_data (key_id, id)` (the rotation takes the next
  content of a key in ID order from the index), `entry_about_idx` on
  `eds_entries (about_id)` and `entry_kind_idx` on
  `eds_entries (circle_feature_id, kind, position)`; and
  `DataDao.lockContentWithThumbnails` finds the rows first and locks them one by
  one in ID order, as the `OR` of one statement kept the index out.
- **Not chosen.** A total in memory, loaded at startup: faster, but with two stones
  on one database each sees only its own writes, and a rollback or a cascade
  missed on one path leaves it wrong until a restart. A total refreshed at most
  every 30 seconds, exact near the ceiling: no schema change, but every refresh
  still reads every part.
- **Done when**, and met: a test shows that the check reads no part of another
  object, a test of each path compares the totals with the sum of the parts, a test
  of concurrent writes to one Circle shows that no update is lost (all in
  `StoredBytesTest`), and a long load run on PostgreSQL keeps its throughput
  (below). `RotationLockOrderTest` shows that a write finishes while another
  transaction holds the Circle's row, as its delete does.
- **Measured (2026-11-24).** The same 5-hour load on PostgreSQL as before (160
  users in 40 rooms, `auto_explain` at 250 ms): 34.1 million operations instead of
  26.6, none failed, no WARN or ERROR in the stone's log, no busy answers. Requests
  a second, hour by hour: 2,435, 2,120, 1,742, 1,693, 1,677 (before: 2,302, 1,750,
  1,296, 1,088, 991), so flat from the third hour on. `eds_data_parts` and
  `eds_entries` are no longer read by sequential scans (before: 1.6 million and
  104,000 full scans). Statements over 250 ms: 22,442 instead of 628,531, of which
  22,222 wait for the lock of a Circle's row (each a few hundred ms) and 105 for
  the row of totals. The inventory's comparison took 0.5 s in the first hour and
  2.1 s in the fifth (ticket 27). Why the first hours are slower: see "Connections
  and locks held for less" below.

### Connections and locks held for less (2026-11-25)

Status: done 2026-11-26 (tickets 28, steps 1, 2 and 4; 35; 36; 37), commit
"Throughput under load". What is left of 28, and ticket 38, are in
`FINDINGS.md`.

- **Why.** In the 4-hour load run of 2026-11-25 (160 users in 40 rooms, a pool of
  60), the requests a second fell from 2,458 to 1,682 in four hours. All 60
  connections were open and 54 of them were idle in transaction: the stone held
  them while it worked in Java. That work was:
  - Hibernate checking every entity for changes before every query of a page of
    entries, with up to three queries more for each entry;
  - the unwrap of the Circle key (ECDH on P-384 and ML-KEM) under the lock of the
    Circle's row;
  - PBKDF2 at login, after the account was read.
- **Decided by the user (2026-11-25), in this order, each measured by a load run:**
  - **Step 1, reads without flushes.** `CommonDao.reading` runs a read with
    Hibernate's flush mode at COMMIT, after one flush, and sets the mode back.
    `FeatureManager.entries` reads a page that way, with its authors, entries
    and content each in one query (`findMemberNames`, `findEntryExternalIds`,
    `findDataByMetadata`). The content of a read is loaded read-only and
    detached after it.
  - **Step 2, the Circle key before the lock.** Add, change, remove and close
    unwrap the Circle key before `lockCircle`. Under the lock the Trustee is
    refreshed, and the key is unwrapped again only if its key ID changed. An
    offer to the rotation from the early unwrap is checked again under the
    Circle's lock by `RotationManager.begin`.
  - **Step 4, the passphrase before the request's connection.** The account is
    read in a transaction of its own (`AuthAttempts.findAccount`, `REQUIRES_NEW`
    in `AuthLimitBean`) and the passphrase checked before the request's
    transaction reads anything. Then the account is read again in it, and the
    passphrase checked again if its keys changed meanwhile (`sameKeys`).
    - An unknown name still costs the same work and gets the same answer as a
      wrong passphrase.
    - A stone's first account is still claimed in the request's transaction.
  - **Option 3, a cache of Circle keys:** the last option. The user isn't fond
    of caching secrets.
- **The JSON provider (ticket 35, option 2).** `JsonFactories` holds one
  `JsonBuilderFactory` and one `JsonReaderFactory`. Before, each
  `Json.create*` call ran a `ServiceLoader` lookup that read the stone's JAR.
- **The buffer of a stored content (ticket 36).**
  - The decision: stream the parts when the length is known, and use a buffer
    from 64 KiB that grows when it isn't.
  - Done otherwise for a known length, accepted by the user (2026-11-26). If the
    client's stream fails partway through a part, pgjdbc has already sent part
    of it, can't recover and drops the connection, and the error would be the
    driver's. So the stream is still read into a buffer first, and the buffer is
    the size of the content's largest part, no longer 4 MiB for every write.
  - Without a length, the buffer starts at 64 KiB and doubles up to the part
    size (`PartBuffer`).
- **The load test's Flight Recording (ticket 37):** `maxsize=8g` and no
  `maxage`, so it keeps the whole run (`TestStone`). `maxage=0` kept only the
  last chunk.
- **Tests, each failing without its part:**
  - `FeaturesTest`: the statements of a page and its checks for changes, 5
    against 18 entries; nothing read for the page kept.
  - `RotationLockOrderTest`: the Circle key unwrapped while a write waits for
    the Circle's lock.
  - `PassphraseBeforeReadTest`: no account in the request's session while
    PBKDF2 runs; an account changed meanwhile checked again.
  - `SocketCodecTest`: one JSON provider made.
  - `EncryptedDataStoreTest`: under 1 MiB allocated for a write of 100 bytes.
  - `TestStoneTest`: the recording option.
- **Measured (2026-11-25):** the same load as before, each step for 30 minutes,
  then all of them for 4 hours. Runs vary by about 7 %.

  | Run (30 min) | requests/s | effect |
  |---|---|---|
  | baseline | 2,665 | `chat.history.rest` p50 786 ms |
  | + step 1 | 3,289 | `chat.history.rest` p50 254 ms; a page of entries from 28.5 % to 4.6 % of the CPU |
  | + step 2 | 3,078 | lock waits over 200 ms down by a third |
  | + step 4 | 3,287 | idle in transaction over 100 ms after a member select: 3.8 → 0.75 |

  The 4-hour run, requests a second hour by hour:

  | | hour 1 | hour 2 | hour 3 | hour 4 |
  |---|---|---|---|---|
  | before | 2,458 | 2,081 | 1,869 | 1,682 |
  | after | 4,481 | 4,034 | 3,667 | 3,396 |

  - **The run:** 55.9 million operations, none failed.
  - **Idle in transaction:** 18 ms at the median and 82–99 ms at the 90th
    percentile, before 48–69 and 419–566 ms.
  - **GC:** about 285 s an hour, before about 690.
  - **Still a fall of 24 %:** that is the database's own work, which grows with
    the history. Two counts are ticket 38, and the buffer cache is ticket 32.
    ECDH is now the largest CPU cost (23 %). The runs' files are in
    `/home/kim/projects/eds-runs/results/`.

### Open points

- ~~pgjdbc buffering~~: verified in S1. Since "Content in parts" there is no temp
  file: a part at a time in a buffer. `Content-Length` stays the normal case (it
  allows `413` before reading the body).
- ~~H2 limits~~: verified in S1; since "Content in parts" bounded by a part on H2 as
  well.
- Objects above 1000 MiB: not planned unless a use case appears. Since "Content in
  parts" (2026-11-18) no field holds more than a part, so PostgreSQL's 1 GB field
  limit is no longer the bound, and large objects (`lo_*`) aren't needed. The
  limits are `eds.stream.max.object.size` (1000 MiB; the settings request checks
  no upper bound, ticket 34) and `quarkus.http.limits.max-body-size` (1100M).
- PWA media access: decided in *Clients API* L5, a download token per object
  (`POST /data/{dataId}/token`, then `?token=` on the content URL); no cookie.
- ~~Server-side spool for uploads without `Content-Length`~~: decided 2026-11-10
  (the user), done 2026-11-11 for the CLI (`TOLK.md`, L6b); see "Upload" above.
  `DataStreams.encrypt` without a length, `EncryptedDataStore.writeToTheEnd`
  (parts read to the end of the stream), and `UploadDataManager.writeToTheEnd` with
  the limits. H2 accepts a stream without a length as well. Tests:
  `DataStreamsTest` (the same bytes as with a length, for every boundary case),
  `ShareBeanUploadTest` (round trips, too large, a failing source),
  `ShareBeanLargeDataTest` (the ceiling, while reading and after), and
  `EdsStreamingTest` with a real chunked request over HTTP.
- ~~Chunk size~~: 64 KiB, decided in *Crypto* C4.
- ~~Defaults for the inline limit, read batch size and maximum object size~~: 10 MiB,
  4 MiB and 1000 MiB (S2, S4).

## Clients API

Status: done (L1–L4 2026-11-04 to 2026-11-06, the server side of L5 2026-11-08, and
the changes listed under "After L5" until 2026-11-13).
Scope: what EDS offers its clients: the OpenAPI contract, the ID-based resource API,
acceptance tests on a generated client, the live chat, download tokens and
thumbnails, and what Rune and Tolk needed from EDS while they were built. The
clients themselves have their own plans, `RUNE.md` and `TOLK.md`.

Precedence: crypto decisions are made in *Crypto*, and data transfer, the content
endpoint and safe serving of content in *Streaming*. Where this chapter touches those
topics, it refers to them and doesn't redefine them.

### The contract

- From Phase L1 on, the OpenAPI contract `eds-api/src/main/resources/openapi.json` is
  the source of truth for the clients. It is generated from the code and checked in;
  the build fails if the code and the contract differ (decided 2026-11-04, see L1).
  Never hand-edit generated client code; change the API, update the contract with
  `-Deds.contract.update=true`, and regenerate. The CLI's client is hand-written
  (see `eds-cli`); a test keeps it in line with the contract.
- Verify current versions and property names (Quarkus, openapi-generator) against
  their documentation. Pin every version explicitly; prefer releases that are at least
  two weeks old.

### Goals

1. One Maven build that produces the server, the Java test client, and (with their
   own plans) Rune and Tolk, from one OpenAPI contract.
2. Contract-first REST API: existing data API kept, plus a parallel ID-based API.
3. Live chat over WebSockets. Data transfer itself uses the HTTP content endpoints
   from *Streaming*, not WebSockets.
4. FitNesse acceptance tests against the running server, using a generated client.

### Starting point (2026-11-01)

Where the plan started; parts of the original restructuring were already done, in
some cases differently from the first version of this plan:

- Modules: `eds-api`, `eds-core`, `eds-quarkus`, `eds-client`, `eds-fitnesse` (now `eds-testing`).
  The pg/h2 Quarkus variants and WildFly are gone.
- Java 25 (`pom.xml`), Quarkus 3.x; `eds-quarkus` is the only runnable module and
  builds an uber-jar (`quarkus.package.jar.type=uber-jar`).
- No EJBs left: `ManagementBean`, `ShareBean`, `SanitizerBean`, `StartupBean` are CDI
  beans in `eds-core`; scheduling is in `eds-quarkus` (`SanitizerSchedule`).
- Database: PostgreSQL when `EDS_DB_URL` is set, otherwise in-memory H2; schema created
  at startup from `eds-core` `META-INF/eds-schema.sql`.
- Health on the management port 9000 (`/q/health/ready`).
- REST under `quarkus.http.root-path=/eds`.
- The API is **code-first**: MicroProfile OpenAPI annotations in `eds-core` services;
  `accessories/release/openapi.json` is generated from them during the build.
- `eds-client` is a hand-written Gson REST client; `eds-fitnesse` uses it.

### Layout

```
eds/
├── pom.xml                  parent (packaging=pom): versions, dependency and plugin management
├── eds-api/                 openapi.json (the contract), hand-written request and response classes
├── eds-core/                business logic, crypto, persistence, REST and WebSocket implementations
├── eds-quarkus/             Quarkus application → runnable uber-jar
│   └── src/main/webui/      Rune (RUNE.md)
├── eds-testing/             generated Java client, FitNesse fixtures and suites, load and rotation tests
└── eds-cli/                 Tolk (TOLK.md; profile: cli)
```

`eds-client` was retired in Phase L3; the generated client in `eds-testing` covers
what it did.

Reactor order: `eds-api` → `eds-core` → `eds-quarkus` → `eds-testing`.
`eds-testing` is always a module of the build; its profiles `fitnesse`, `load` and
`rotation` only add the runs against a stone (`eds-testing/README.txt`).

Baseline: Java 25, Quarkus 3.x (current LTS).

### Module specifications

#### eds-api

- Contract at `src/main/resources/openapi.json`, OpenAPI 3.0.3 (decided in
  *OpenAPI*), so all client generators (TypeScript, Java client, Go) are on
  well-supported ground. A move to 3.1 is a later step.
  It is packaged inside the JAR so other modules consume it as a versioned artifact.
- The contract is generated from the annotated code in `eds-core` (as since
  *OpenAPI*) and checked in; `OpenApiContractTest` in `eds-core` fails the build
  when they differ. The hand-written request and response classes, with their
  validation rules, stay. (The first version of this plan generated JAX-RS
  interfaces and models with `openapi-generator` `jaxrs-spec`; dropped in L1, as the
  generated models would lose the validation in `eds-api` and need a new build
  plugin.)
- No client code in this module; each client module generates its own client.

#### eds-core

- Everything server-side: crypto layer, persistence (Jakarta Persistence / Hibernate ORM),
  business services, the REST resources (code-first, annotated; they use the
  hand-written `eds-api` types), and the Jakarta WebSocket endpoint of the socket
  (`SocketEndpoint`; the runner brings `quarkus-websockets`).
- Written against Jakarta APIs (CDI, REST, Persistence, Transactions); no Quarkus
  bootstrapping here, so it stays unit-testable without a container.
- CDI beans and resources of `eds-core` are indexed by the runner
  (`quarkus.index-dependency.eds-core.*` in `application.properties`); there is no
  Jandex plugin.
- In the tests, logback is the provider of SLF4J (off by `logback-test.xml`, named
  by the tests' `META-INF/services`), so a test can see what is logged
  (`LogCapture`); `slf4j-nop` went on 2026-11-22.

#### eds-quarkus

- Thin module: depends on `eds-core`; contains `application.properties`, the database
  selection/startup classes and the PWA.
- REST and PWA on the same origin (no CORS). Path layout (2026-11-10): Rune at `/`,
  the REST API and the socket (`/api/v1/socket`) below `/api/v1`. The API root is defined once
  in the code (`Constants.REST_API`, the `@ApplicationPath` of `EDSApplication`);
  there is no `quarkus.http.root-path`.
- Rune is built into the same JAR by Quinoa; that wiring is described in `RUNE.md`.
- Published spec: serve the contract itself at runtime (L2), not a version
  re-derived at runtime.

#### eds-testing

- Test module, not production code: third-party libraries are allowed here (decided
  2026-11-05).
- Unpacks `openapi.json` from `eds-api`, generates a Java client with generator `java`,
  `library=native` (java.net.http + Jackson), package `net.haugr.eds.testing.client`.
  No Quarkus or MicroProfile runtime in this module, and no other EDS code.
- FitNesse fixtures wrap the generated client; wiki pages live in the module.
- Lifecycle (profile `fitnesse`): `FitNesseSuiteIT` (Failsafe) starts the
  `eds-quarkus` runner JAR on its own ports, waits for `/q/health/ready`, runs the
  `EDS` suite with FitNesse (a test dependency) in a separate process, and stops the
  runner. H2 by default; PostgreSQL when `EDS_DB_URL` is set in the environment.
- Packages under `net.haugr.eds.testing`: `.fitnesse` (the fixtures), `.client`
  (the generated client), `.load` (in `src/test/java`: the load test `StoneLoadIT`,
  profile `load`; the rotation test `CircleRotationIT`, profile `rotation`; the
  PostgreSQL observer `PostgresObserver`). The module was `eds-fitnesse` until
  2026-11-25, renamed as it holds more than FitNesse.
- The load and rotation tests start their stone with `TestStone`. With Podman on
  the PATH, it runs in a rootless container, `eds-<name>-stone` (chosen by the
  user, 2026-11-25), built from `eds-stone.jar` with
  `accessories/podman/Containerfile`. The container uses the host's network, the
  user's ID and `target/` as a mount, and gets the set `EDS_*` and `QUARKUS_*`
  variables by name. A container left by a killed run is removed by name at the
  next start, and a shutdown hook stops it when the JVM is stopped. Without
  Podman, or with `-Deds.stone.container=false`, it runs `java -jar`. This was
  ticket 29 in `FINDINGS.md`: a killed test left its stone holding the ports.
- `TestStone` records the stone with JFR (`-Deds.load.jfr`) with `maxsize=8g` and
  no `maxage`, so the recording keeps a whole run (ticket 37, 2026-11-25).

### Cross-cutting requirements

#### API contract

- Existing data API kept (minus the signature operations removed in *Crypto* C1);
  parallel ID-based resource API added (L1).
- Content endpoints as defined in *Streaming* (upload, download, `Range`/`206`,
  `HEAD`, ETag), described in the contract.
- Thumbnail endpoint for images (and video posters later); added in L5 with the
  thumbnails themselves.
- Data metadata includes media type and size (the media type is stored with the
  metadata, see *Streaming*; the size is computed from the stored object).
- WebSocket frame types defined as JSON schemas in `components/schemas`, so all
  clients get generated frame models; only socket handling is hand-written. Added in
  L4 with the chat; `SocketFrame` and `SocketFrameType` since `FEATURES.md`.
- Consistent error model across all endpoints: every response body is the JSON
  response with `returnCode` and `returnMessage`. The JSON API answers HTTP 200
  (decided 2026-11-04: kept); the content endpoints and the resource API answer with
  the HTTP status of the return code.
- Authentication: session tokens as defined in *Crypto* §2a; credentials only for
  login.
- The version operation (`POST /version`, no login) returns the version of EDS, the
  product name of its database (`database`) and whether the data survives a restart
  (`persistent`: false for in-memory H2). Both are absent until the database has been
  reached. Every client shows a test system as such (2026-11-10). It also returns
  `storageCeiling`, the ceiling for stored content that is in effect, so a client can
  tell a person before an upload is refused.
- `Member.settings` is the one place for client settings that follow the account;
  EDS doesn't interpret it; the keys are Rune's (`RUNE.md`).

#### Crypto and storage

Defined in *Crypto*; nothing here overrides it. Consequences for this plan:

- Keys stay server-side; clients never handle key material.
- Everything EDS stores for a circle is encrypted with the circle's keys in the
  *Crypto* §4 format. That includes **thumbnails** and **chat messages** (see Open
  decisions): neither may be stored in plaintext.
#### Serving user content

Defined in *Streaming* ("Serving user content safely"). Rune adds a strict CSP for
the app and a preview size limit (`RUNE.md`).

### Phases

Phases are prefixed L (cLients) to keep them apart from *Crypto* (C) and
*Streaming* (S). L5 is split: its server side is here, Rune itself in `RUNE.md`. L6
is Tolk, in `TOLK.md`.

#### Phase L1: Contract

Tasks:
- Write `eds-api/src/main/resources/openapi.json`, starting from the generated
  `accessories/release/openapi.json`, which is complete after *OpenAPI* (docs,
  schemas, return codes, permissions). Keep that level of documentation, and add the
  parallel ID-based API, thumbnails, media metadata and WebSocket frame schemas.
- Decide whether the contract keeps the HTTP-200 response model or moves to real
  HTTP status codes, and whether shared request types are split per operation
  (*OpenAPI* Open decisions).
- Port the completeness test from *OpenAPI* to the contract file.
- Generate server interfaces and models in `eds-api`; Jandex.

Done when: generation succeeds; the contract validates with openapi-generator's
validator; every REST operation of the current server is represented.

Decisions (2026-11-04, by the user):
- **Contract**: the generated spec, checked in as the contract, with a build check;
  no generated server code (see `eds-api` above).
- **HTTP-200 model**: kept for the JSON API.
- **Request types**: shared types kept; the spec documents the fields per operation.
- **ID-based API**: added in L1.

Done 2026-11-04:
- Contract `eds-api/src/main/resources/openapi.json` (moved from
  `accessories/release/`, which the build no longer writes). `OpenApiContractTest`
  compares it with the generated spec, as JSON, and lists the differing JSON
  Pointers; `mvn install -Deds.contract.update=true` writes the generated spec into
  the contract. The completeness test runs on the generated spec, which equals the
  contract.
- ID-based resource API, 24 operations, next to the JSON API, in the existing service
  classes and tags:
  `/members` (`GET`, `POST`), `/members/{memberId}` (`GET`, `PUT` role, `DELETE`),
  `/circles` (`GET`, `POST`), `/circles/{circleId}` (`GET`, `PUT`, `DELETE`),
  `/circles/{circleId}/trustees` (`GET`, `POST`),
  `/circles/{circleId}/trustees/{memberId}` (`PUT`, `DELETE`),
  `/data?circleId=` (`GET`), `/data` (`POST`), `/data/{dataId}` (`GET`, `PUT`,
  `DELETE`), `/data/{dataId}/copy` and `/move` (`POST`), `/dataTypes` (`GET`),
  `/dataTypes/{typeName}` (`PUT`, `DELETE`).
  Each runs the business logic of its JSON twin, with the same request object as body;
  path values replace body values. Authentication only with
  `Authorization: Bearer <session>`, now declared as the security scheme `session`
  (also for the content endpoints, whose header parameter OpenAPI ignored).
  Responses carry the HTTP status of the return code, like the content endpoints
  (my choice, as no client depends on it yet; the existing mapping gives e.g. 403 for
  `AUTHENTICATION_WARNING` and 406 for `VERIFICATION_WARNING`).
  `GET /data/{dataId}` returns only the metadata, without reading the content.
  Operation IDs: `get<Things>`, `post<Things>`, `get|put|delete<Thing>ById`.
  Their docs use a new `same-as:` line, which takes permission and return codes from
  the JSON twin.
- Media metadata: `Metadata.size`, the plaintext size, computed from the stored
  length and the header (one query per listing, the content isn't read). The media
  type was already there (S2).
- Moved: thumbnails to L5, WebSocket frame schemas to L4. The contract is generated
  from the code, so they're added when they're implemented.
- Tests: `EdsResourceApiTest` (Quarkus, over HTTP), size test in
  `ShareBeanLargeDataTest`, `OpenApiContractTest`.
#### Phase L2: Serving the contract

Reduced by the L1 decision: the server stays code-first, with the contract checked
against it, so there are no generated interfaces to implement.

Tasks:
- Serve the contract at runtime, unchanged (e.g. from the `eds-api` jar).
- Decide and apply the path layout (Open decisions).

Done when: all existing unit tests and FitNesse suites (still on `eds-client`) pass;
the served spec is the contract unchanged.

Done 2026-11-05 (the user didn't choose; the proposed defaults were taken). The
paths below are those of that day; since 2026-11-10 the API root is `/api/v1`, so the
contract is served at `/api/v1/openapi.json` (see "After L5"):
- Path layout: unchanged, everything under `quarkus.http.root-path=/eds` (REST at
  `/eds/...`; the PWA will be at `/eds/` in L5).
- `GET /eds/openapi.json` (`SpecificationService`, tag `Specification`) serves the
  contract from the `eds-api` jar byte for byte, without authentication. No new
  dependency (`quarkus-smallrye-openapi` isn't used, so there's no `/q/openapi`).
  The completeness test exempts this operation from the rule that JSON responses
  reference an EDS schema.
- `EdsResourceApiTest.testSpecificationIsTheContract` compares the served bytes
  with the contract file.

#### Phase L3: Acceptance tests on the generated client

Tasks:
- Generate the Java client in `eds-testing`; port the fixtures from `eds-client`.
- Suites for the ID-based API and anything not yet covered.
- Integration-test lifecycle (start DB + server, run suites, stop).
- Retire `eds-client`.

Done when: `mvn -Pfitnesse verify` runs the suites green against the runner JAR and
no module depends on `eds-client`.

Decision (2026-11-05, by the user): `eds-testing` is a test module, so the rule
against third-party libraries doesn't apply to it.

Done 2026-11-05:
- `eds-testing` generates the client from the contract, unpacked from the `eds-api`
  jar (`openapi-generator-maven-plugin` 7.26.0, `java`/`native`, Jackson 2.22.1).
  The normalizer rule `REF_AS_PARENT_IN_ALLOF` keeps the inheritance of eds-api
  (requests extend `Authentication`, responses `EDSResponse`), so the fixtures stayed
  close to what they were. They no longer depend on `eds-api` or `eds-client`.
- Contract defects found by the generated client, fixed in the code (so the contract):
  - `Content-Length` was a declared header parameter of the upload; the generated
    client set it itself, which `java.net.http` refuses. Now hidden; the operation's
    header table documents it.
  - `EDSResponse.ok` was `readOnly`; the Java generator then produces code that
    doesn't compile, once the responses extend `EDSResponse`. `readOnly` meant nothing
    on a response-only schema, so it's gone.
  - The `EDSResponse` description still said every operation answers HTTP 200.
- Generator limitation, worked around in the fixtures: the `native` library sends a
  binary body as JSON (the file name as a JSON string), also in the current
  upstream template. The upload fixture replaces only the body publisher, with a
  request interceptor on a client for that call.
- New suite section "Resource API" on `EDS.API`: all 24 operations of the ID-based
  API (fixture `Resources`, one row per operationId), with their HTTP statuses,
  including 401, 403, 404 and 406. It removes what it creates, so the inventory
  after it is unchanged.
- `mvn -Pfitnesse verify` (or `install` from the root) runs the suite against the
  runner JAR; ports 18080/19000, so a local EDS on the default ports doesn't clash.
  `EDS_URL` in the environment replaces the URL of the wiki setup.
- `eds-client` (hand-written Gson client) removed, with Gson.
- Result: `EDS.API` 548 right, 0 wrong (was 416; the 132 new are the resource API).
#### Phase L4: Live chat

> Replaced since `FEATURES.md` T4 (2026-11-15): the chat is a feature on the
> socket `/api/v1/socket` (`SocketFrame`, `SocketFrameType`). The Data Types `chat`
> and `chat-receipt`, the chat frames and the `chat` fixture are gone. Kept as the
> record of how it was built.

Tasks:
- Chat storage design (Open decisions), encrypted per *Crypto* §4.
- WebSocket endpoints for chat and timeline events (e.g. "file shared"), session
  authentication.

Done when: existing suites still pass; new suites cover WebSocket message exchange,
authorisation (non-members can't join a circle's channel), and that stored messages
are encrypted.

Decision (2026-11-06, the user): each message is a small Data Object in a hidden
folder `.chat` of the Circle. Following the Unix convention, every name starting with
a dot is hidden: listings leave it out unless asked for. No deletion or retention
rules yet; they come later with an extended Circle configuration, which will also
control the chat.

Done 2026-11-06:
- Storage: a message is a Data Object of the new standard type `chat` (media type
  `application/json`) in `/.chat`, created with the first message; its content
  (sender ID and name, time, text, shared Data Object) is encrypted like any other
  content (*Crypto* §4), so the database learns nothing new. The Circle row is
  locked while the folder is created. Message names are the time plus random hex.
- Hidden names: folder listings (`fetchData`, `GET /data`, `GET /data/{dataId}`) skip
  names starting with `.`, unless `includeHidden` is set (new request field and query
  parameter). Reading by ID or name is unchanged.
- Rules: `.chat` is reserved in the root folder (any case); the data requests may read
  the folder and the messages, but not add, rename, move, copy, delete or upload to
  them (`ILLEGAL_ACTION`), so the history can't be forged. The type `chat` can be
  neither changed nor deleted. The inventory leaves messages out; sanity checks and
  the storage ceiling include them.
- WebSocket `/eds/chat` (`/api/v1/chat` since 2026-11-10; Jakarta WebSocket in `eds-core`, `quarkus-websockets` in the
  runner). JSON text frames, schemas `ChatFrame`, `ChatFrameType` and `ChatMessage`
  in the contract, the protocol in its overview. The first frame is `AUTH` with the
  Session token (browsers can't set headers on a WebSocket), within 30 s. `JOIN`
  needs READ, `SEND` needs WRITE and a joined Circle, `HISTORY` pages backwards
  (`before`, `limit` up to 100). Every frame is authenticated like a request.
- "File shared" is a message with a `dataId` of the same Circle, so it is part of the
  history. Live events for other data changes came with `CHANGED` (see "After L5").
- Access ends promptly: after a committed change of Members, Circles, Trustees or the
  MasterKey (CDI event, after success), open connections are checked again, and
  get `LEFT` for a Circle they can no longer read, or `ERROR` and close (1008) when
  the Session ended; expired Sessions are closed by a timer.
- Since 2026-11-13 what waits for a connection is limited: 4096 frames each way, 4 M
  characters to handle, and three of the largest history pages to send. A connection
  over a limit (its client doesn't read, or floods) is closed with 1013 and its
  backlog thrown away. Frames are handled and sent by two threads per connection, so
  a blocked send holds nothing else up; a dropped connection which doesn't read is
  closed after 10 s all the same.
- Not done: rate limits per connection (since 2026-11-26 the socket's budgets,
  `SECURITY.md`), and delivery across several EDS instances (messages reach the
  connections of the same instance); see `FEATURES.md`. Editing and deleting
  messages came later, see "After L5".
- Tests: `ChatTest` and `ChatCodecTest` (core, including the check that the stored
  bytes contain neither text nor sender), `EdsChatTest` (Quarkus, real WebSockets),
  and the FitNesse section "Chat" in `EDS.API` with the `chat` fixture (JDK WebSocket
  client, generated frame models). `EDS.API`: 705 right, 0 wrong (was 548).

#### Phase L5: the server side of the PWA

Decisions (2026-11-07, the user): media elements use a per-object download token
(*Crypto* §2a), no cookies. Thumbnails are made in the browser and stored encrypted;
the server stays content-agnostic.

Done 2026-11-08:
- Download token: `POST /data/{dataId}/token` (Bearer, READ) returns a URL-safe token
  and its expiry; `GET`/`HEAD /data/{dataId}/content?token=…` accept it in place of
  the header (the header wins). The token is the Session token and an expiry, sealed
  with the MasterKey wrap key, with the object's ID in the AAD: nothing is stored,
  and it reads only that content. It ends at `eds.download.token.lifetime.minutes`
  (new setting, default 60, at most the end of the Session), at logout, or when the
  MasterKey changes.
- Thumbnails: `PUT /data/{dataId}/thumbnail` (Bearer, WRITE; JPEG, PNG or WebP, at
  most 256 KiB) stores a Data Object of the new standard type `thumbnail`, whose
  parent is the object it shows, so no listing shows it; `Metadata.thumbnailId`
  points to it. Only the endpoint writes it; it goes away with its object, when new
  content is stored, and on a move to another Circle; copies don't keep it. The type
  can be neither changed nor deleted, and the inventory leaves thumbnails out.
- Content responses also send `Content-Security-Policy: default-src 'none'; …;
  sandbox`, `Cross-Origin-Resource-Policy: same-origin` and `Referrer-Policy:
  no-referrer` (the token is part of the URL).
- Tests: `MediaTest` (core: tokens bound to object, session and expiry; thumbnail
  rules), `EdsStreamingTest` (token and thumbnail over HTTP, content headers).
  FitNesse `EDS.API`: 705 right, 0 wrong (the type lists now include `thumbnail`);
  tables for the two operations were added on 2026-11-10.
- Not done: video posters on the server (not needed, the browser makes them).

#### After L5: what the clients needed from EDS

Changes to EDS made while Rune and Tolk were built. All are in the contract. Several
came in small steps; this is what they add up to.

**Members, Circles and access (2026-11-08)**
- Invitations into a Circle by its administrators, with the access included
  (*Crypto*, "Invitations").
- `login` returns the `memberId` of the account.
- Every Trustee may leave a Circle: `removeTrustee` with the own `memberId` needs
  only `FETCH_TRUSTEE`. Also the last administrator may leave; the clients warn.
  The last Trustee of a Circle can't leave or be removed (since 2026-11-13): the
  Circle Secret would be lost with them, so the Circle is deleted instead. For the
  same reason an account which is the last Trustee of a Circle can't be deleted.
  Removing others stays with the Circle's administrators and the System
  Administrator.
- `eds.show.all.circles` defaults to `false` (was `true`): a Member's Circle list
  holds their own Circles, sorted by name. The System Administrator sees all.
- Used by the clients, but not new: the System Administrator creating a Member with
  a passphrase and deleting Members, and changing or removing Trustees where not a
  Trustee; adding a Trustee needs an administrator of the Circle, as the Circle
  Secret has to be wrapped for the new one. READ-only Trustees are refused what
  needs WRITE.
- The message for a secret that is too weak advises "at least 20 random characters";
  the rule is unchanged (*Crypto* §5). The clients' message table matches on EDS's
  texts, so a change of such a text needs their table to follow
  (`messages.json`, `RUNE.md`).

**Files (2026-11-08)**
- A file can move into a folder created after it: the check
  `metadata_not_before_parent_id` is dropped. Folders can't be moved, so loops still
  can't occur.

**Chat (2026-11-08 to 2026-11-10)**, replaced since `FEATURES.md` T4 (see L4), on the
L4 WebSocket; the frames and schemas were
in the contract (`ChatFrameType`, `ChatFrame`, `ChatMessage`, `ChatReceipt`), the
protocol in its overview:
- `CHANGED`: the files of a joined Circle changed (added, uploaded, updated, moved,
  copied or deleted), with `circleId` and `dataId`, so a client reloads its listing.
  It is the general event channel L5 left open.
- `DELETE` → `DELETED` and `EDIT` → `EDITED`: only the author of a message deletes
  or edits it, not an administrator; an edited message carries `edited`. No
  retention or automatic deletion (see "Open").
- Replies: `SEND` with `replyTo`, a message of the same Circle.
- `REACT` → `REACTED`: gives a reaction or takes it back; six fixed reactions; needs
  WRITE, as `EDIT` does. A message carries its `reactions`.
- `RECEIVED` and `READ` → `RECEIPT`: a Member's client received, or the Member read,
  up to a message; told to the others who joined. The marker per Member is a Data
  Object of the new standard type `chat-receipt` in `.chat`, encrypted like a
  message. Since 2026-11-13 it also holds the positions of its messages in the chat
  (not sent to clients), so a marker stays where it is when its message is deleted.
- `TYPING`: passed on to the others who joined, with `memberId` and `accountName`;
  not stored, and accepted at most once in 2 s per connection.
- `JOINED` carries `unread`, the number of messages after the Member's read marker.

**Avatars (2026-11-10)**
- `Member.avatar`, `Circle.avatar`, and `Trustee.avatar`/`circleAvatar`: the ID of a
  picture the clients know (at most 32 characters); EDS stores only the ID, in
  `eds_members.avatar` and `eds_circles.avatar`.
- Updating a Circle without `circleKey` keeps the stored external key (it was
  removed before; *OpenAPI* O4, finding 3).

**Passphrases and settings (2026-11-10)**
- No system salt any more. The PBE iterations are kept per Member
  (`eds_members.pbe_iterations`), so `eds.crypto.pbe.iterations` only counts for new
  passphrases. It may change while Members exist, within 1000 to 999999999, and is
  no longer written at MasterKey setup; changing it doesn't re-key the System
  Administrator.
- `Member.settings` (`eds_members.settings`): a JSON object as text of at most 4096
  characters that EDS stores as it is, for the clients' settings; only the Member
  gets it.
- Settings responses carry `allowedValues` for the settings that have a fixed set.

**Test or production, and the storage ceiling (2026-11-10)**
- The version operation reports `database`, `persistent` and `storageCeiling` (see
  "API contract").
- The storage ceiling has a default by kind of system when `eds.data.max.total.size`
  is `0`: 2 GiB on a test system, 250 GiB on a production system; a positive value
  sets it explicitly (decided by the user, after uploading gigabytes into a test
  instance ran it out of memory). Described in *Streaming*, S4.

**Paths (2026-11-10)**
- Rune at `/`, the REST API and the chat WebSocket below `/api/v1`, health at
  `/q/health` on the management port (decided by the user, to follow conventions).
  This replaces the L2 layout (everything under `/eds`). The API root is defined once
  (`Constants.REST_API`, the `@ApplicationPath` of `EDSApplication`); there is no
  `quarkus.http.root-path`, and `applicationPathDisable` in `eds-core/pom.xml` keeps
  the paths of the contract relative to the root.

**Uploads without a length (2026-11-11)**
- For pipes (`… | tolk send -`): see *Streaming*, "Upload". `LENGTH_REQUIRED` (411)
  is gone from the contract.

**Schema**
- The schema changed with these steps. Since S1:
  - the standard Data Types `chat`, `thumbnail` and `chat-receipt` (since
    `FEATURES.md`: `entry` instead of `chat` and `chat-receipt`);
  - the table `eds_invitations`;
  - `eds_members.avatar`, `eds_circles.avatar`;
  - `eds_members.pbe_iterations` (not null) and `eds_members.settings`;
  - the check `metadata_not_before_parent_id` dropped.
- There is no upgrade in place: `StartupBean` compares the version, logs a
  mismatch, and EDS doesn't become ready. A fresh database is needed (`RUNNING.md`).

**Review fixes (2026-11-13)**
- The message in the log when the schema version doesn't match (above).
- The sanity job checks content with the algorithm its checksum was stored with
  (*Crypto*, "Open points").
- `alterMember` with an unknown `memberId` answers `IDENTIFICATION_WARNING`
  (*OpenAPI* O4, finding 1).
- Contract: the text for `circleKey` on a Circle update, and `avatar` and `settings`
  in the field tables.
- The FitNesse setup page uses `/api/v1`.

**Tests**
- The full build ran with 796 tests and no failures on 2026-11-10, and with 806 on
  2026-11-13 (154 in `eds-api`, 626 in `eds-core`, 24 in `eds-quarkus`, 2 in
  `eds-testing`).
- The FitNesse suite runs in every verification since; see the latest commit.

### Tools

- `eds.sh` in the project root builds and runs the parts (`RUNNING.md` describes its
  use): `build [eds|rune|stone|tolk|all] [--test] [--no-sonar] [--sbom] [--podman]`,
  `start [--masterkey]`, `stop [--remove]`, `restart`,
  `status`, `logs` for the one Podman container; `masterkey generate [--force]` and
  `masterkey load` for a MasterKey file in `accessories/podman/` (git-ignored);
  `fitnesse start|stop`, `completion`, `install`. The parts for Rune and Tolk come
  with their plans.
- `.github/workflows/build.yml` builds EDS with Rune and the stone, with the tests
  skipped; Tolk (`-Pcli`) isn't built in CI (a ticket in `FINDINGS.md`).

### Open

- **One session per account**: solved 2026-11-15, see "Several sessions" below.
- The socket and the features (retention, delivery across several EDS instances):
  see `FEATURES.md`, "Open decisions".
- Objects above about 1 GB (*Streaming*, "Open points").

## Several sessions

Added 2026-11-15, as the first step of the work planned in `E2E.md`, `FEATURES.md` and
`STATUS.md` (the user: "let's add multiple session support as the first step").

### Why

A stone kept one session per account, in the Member's row (*Crypto* §2a; "Open"
above). A login replaced it and a logout ended it everywhere, so Rune and Tolk with
one account ended each other's session. With chambers and features that breaks more:
linking a further device would log the first one out, and a phone couldn't report
positions while its owner watches on another device.

### Design

- A session is a row of its own (`eds_sessions`): the Member, the keyed hash of the
  token, the session copy of the private keys, the expiry. The three session
  columns of `eds_members` go.
- What a session is doesn't change (*Crypto* §2a): the same token, the same lookup
  by `HMAC-SHA512`, the same double wrap of the private keys, with the session's
  own expiry in the AADs.
- A login adds a session and leaves the others alone. A logout ends the session it
  is called with.
- All sessions of a Member end when the passphrase changes, when the account is
  invalidated or deleted, and all sessions of the stone when the MasterKey changes,
  as before.
- A Member has at most `eds.session.max.per.member` sessions (new setting, default
  10); a further login ends the one that expires first.
- Sessions that expired are removed by the job that cleared them before.
- A download token belongs to the session it was made with, and the socket's
  connection to the session it authenticated with.

### Phase N1

Done when: `mvn clean install` passes; tests cover two sessions of one account
side by side, a logout that ends one and leaves the other, the ends listed above,
the limit, and expiry; and Rune and Tolk with one account no longer end each
other's session.

Done 2026-11-15:
- `eds_sessions` with `SessionEntity`; the session columns of `eds_members` are
  gone.
- The Member's keys and AADs of a session are as before; the expiry in them is the
  session's own. `MemberEntity.sessionExpire` stays as a field that isn't stored:
  the expiry of the session a request came with, which download tokens and the
  socket read as before.
- Login adds a session and applies the limit (`eds.session.max.per.member`,
  default 10, at least 1); logout ends the session it came with; a new passphrase,
  invalidating and a MasterKey change end all, and deleting an account takes its
  sessions along (foreign key). The sanity job deletes expired sessions.
- Rune and Tolk needed no change: a session token is what it was.
- Tests: `SessionsTest` (two sessions side by side, logout of one, other Members
  untouched, new passphrase, invalidate, the limit, expiry). The contract changed
  only in the texts of login, logout and the new setting.
- Not done: a list of a Member's sessions and "log out everywhere" as requests of
  their own (a ticket in `FINDINGS.md`).
