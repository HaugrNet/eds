# Rune plan: the PWA of EDS

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: done as far as described here (Phase L5, 2026-11-08, extended until
2026-11-17). Since 2026-11-17 changes to Rune's pages are tried in a real browser
before they count as done. Since 2026-11-20 the whole of Rune is clicked through by
a test, in real browsers by several people at once (`scripts/click`, see "The click
test"); what it doesn't reach is under "Open".

One of the three plans of EDS 2.0: `PLAN.md` (the backend), `RUNE.md` (this file),
`TOLK.md` (Tolk, the CLI). Rune is Phase L5 of the work on the clients; the server
side of that phase (download tokens, thumbnails, the chat, what Rune needed from EDS
afterwards) is in `PLAN.md`, chapter "Clients API". Crypto, data transfer and the
safe serving of content are decided there too (chapters "Crypto" and "Streaming");
this file refers to them and doesn't redefine them.

For the review Rune is two commits, after those of the backend (`PLAN.md`, "Handed
over for review"): this file, and then what was built from it:
`eds-quarkus/src/main/webui`, its build with Quinoa and its headers in
`eds-quarkus`, `EdsPwaTest` and the brand in `accessories/brand`.

Since 2026-11-15 three more plans cross all three parts: `E2E.md` (rooms and
chambers: end-to-end encryption which the stone takes no part in), `FEATURES.md` (the
chat, a calendar, location and plans as features by declaration) and `STATUS.md` (the
status of a stone, for those who look after several). They hold the design, the
backend and the convention of the clients; their order of work is in `FEATURES.md`.
What Rune does in each, and the phases in which it was built on 2026-11-15, are in
this file since 2026-11-17: "Rooms and chambers", "Features", "Presence and calls"
and "The status of stones". Each phase says what was built, what differs and what wasn't checked.

The definition of done in `PLAN.md` ("Rules", "Definition of done") applies here
as well. The tests of Rune: `npm test` of the web app, and the click test of its pages.

## Goals

- A PWA served by EDS itself (`eds-rune.jar`), named Rune: rooms and users, chat and files
  in panes each person arranges, media previews, English and German, themes.
- Its settings follow the account, and it says whether EDS is a test or a production
  system.
- In a room, keys stay server-side and EDS encrypts; no key material of a room
  reaches the browser. In a chamber, Rune encrypts on the device with WebCrypto
  (`E2E.md`).
- As few dependencies as possible ("fewer dependencies, fewer CVEs"): no UI
  framework, no QR library, self-hosted fonts.

## Words

- In Rune's texts a Circle is a **room** ("invited into the room"), an account a
  user, someone in a room a member. Only the texts change; the code and the API keep
  the EDS terms.
- A server instance is a **stone**, as in Tolk (decided 2026-11-10, the user; German
  "Stein"). Code, API and the EDS settings keep "server"; messages that come from EDS
  itself aren't reworded.

## Module and build

- Source in `eds-quarkus/src/main/webui`: TypeScript and Web Components, built by
  Vite. The npm package is still named `eds-pwa`.
- Built into `eds-rune.jar` by the Quinoa extension (`quarkus-quinoa`), in the
  Maven profile `rune`, active unless `-Dstone`. The stone alone (`-Dstone`,
  `eds-stone.jar`, `./eds.sh build stone`) has neither Rune nor Quinoa:
  - Quinoa installs the pinned Node version itself.
  - Served at `/`. No SPA routing: Rune is one page without routes, so nothing
    falls back over the REST, `/q` and WebSocket paths (Quinoa ignores `/api`
    and `/q`).
  - Dev mode proxies to the frontend dev server (hot reload on both sides).
- TypeScript client: `openapi-generator-maven-plugin` 7.26.0 (`typescript-fetch`) in
  `generate-sources`, from the contract `openapi.json` unpacked from the `eds-api`
  artifact, output `src/main/webui/src/api/` (git-ignored), before Quinoa runs. Never
  hand-edited: change the API, update the contract, regenerate.
- Versions: Quinoa 2.9.3, Node 24.22.0, Vite 8.3.4, TypeScript 6.0.3;
  `package-lock.json` is checked in. Runtime packages: `@fontsource/inter` and
  `@fontsource/space-grotesk` 5.3.0 (OFL), so Rune makes no requests to font
  services. Development packages: `typescript` and `vite`, with what they bring (43
  entries in the lock file). There is no test framework for the web UI. Its checks
  are plain Node scripts.
- The build runs, in order:
  - `tsc --noEmit`;
  - `scripts/check-i18n.mjs`, which fails on a missing or unused translation;
  - the unit checks `scripts/test-e2e.ts` and `scripts/test-features.ts`, also
    run by `npm test`;
  - `vite build`.
- Outside `webui`: the Quinoa and generator wiring in `eds-quarkus/pom.xml`
  (`version.quinoa` in the root POM); `quarkus.quinoa.*` in
  `src/main/rune/META-INF/microprofile-config.properties` (only in the build with
  Rune); the response headers of the app in `application.properties`; `EdsPwaTest`; the entries in `.gitignore`, and
  `./eds.sh build rune`.
- Brand: the masters are in `accessories/brand/rune/` (with mobile icons for later);
  favicons, icons, manifest and theme tokens live in `webui/public` and `webui/src`.
- Rune works below a path prefix too: relative asset paths (Vite `base: './'`), the
  API root derived from its own address, a relative manifest and service worker.
- Verify current versions and property names (Quarkus, Quinoa, openapi-generator)
  against their documentation; pin every version; prefer releases at least two weeks
  old.

## Rune as it is

As of 2026-11-17.

- **Login and maintenance page**: Rune written in runes (ᚱᚢᚾ), the fields and one
  button, inside a plaited knot; one 540 px stage, 280 px form, the knot hidden below
  560 px width. Both pages are dark in every theme and palette (fixed colours
  `--rune-bg` and `--rune-accent`; the browser's bar follows). The buttons "Sign in"
  and "Try now" are the same size (140 × 46). The maintenance page shows while EDS
  doesn't answer, asks again every 5 s and with "Try now", and returns to where the
  person was. Its text depends on what EDS last reported: "safe" only for a
  persistent database, a warning that the data won't survive a restart on a test
  system, and no claim when unknown.
- On the maintenance page the two snoring z's float up and away while fading (4 s,
  one after the other) and the four stars twinkle, each at its own rhythm (2.1 to
  3.9 s), flaring in colour and size; still under `prefers-reduced-motion` (asked for
  2026-11-10 by the user, made livelier 2026-11-11).
- **Footer of the login**: "Your Rune is currently running on Haugr EDS <version>",
  and on a test system a second line "Test system · The data will not
  survive a restart of the stone." Small and muted; "Test system" is emphasised by
  weight and the accent colour, not by a warning block (decided 2026-11-10, the user:
  a block above the fields pushed the button onto the knot).
- Rune's texts say **stone** for a server instance, as Tolk does (decided 2026-11-10,
  the user): "The stone is asleep", "Stone settings", "a restart of the stone".
  German: "Stein". The about dialog introduces the word ("kept by a stone: a Haugr
  EDS instance"). Code, API and the EDS settings keep "server"; messages that come
  from EDS itself aren't reworded.
- No server address is shown anywhere in Rune (2026-11-10, the user: behind a reverse
  proxy the browser's address needn't be the server's).
- **Desktop**: the panes rooms and users (rooms over users), chat and files, arranged
  by each person (presets, dragging, docking at the window's edges, hiding,
  resizing). Shared files appear as events in the chat timeline; the files pane
  shows a media thumbnail grid or a list, with transfer progress.
- **Two levels** (2026-11-17, the user: rooms and users are another level of
  information than chat, files and features): the rooms pane is where one chooses,
  and is shaded (7 % of the text colour on the background, its bar 11 %); the other
  panes hold the content of the room chosen and keep the plain background.
- **Room or chamber, at a glance** (2026-11-17): with a chamber open, the content
  panes get a tint of the accent (6 %), their bars a stronger one (16 %) with a line
  of the accent on top and "E2E" as a pill, so it isn't told by colour alone; the
  room's name in the header and in the list of rooms has the pill "Chamber". A room
  stays neutral, and the rooms pane looks the same for both. The strengths are one
  block in `styles.css`. (The panes said "Chamber" at first; the user: "E2E", with a
  hint.)
- **What the hints of a chamber say** (2026-11-17, the user): who can read it, not
  that the stone cannot, which sounded as if someone could look at the data. "E2E"
  on a pane: "Chat: end-to-end encrypted in this chamber. Only its members can read
  the content, with the key of the chamber on their devices."; the pill "Chamber"
  and the text in "Keys for chambers" say the same of a chamber. No text of Rune's
  pages says "the stone cannot read" any more. The same held for the room in the
  New room dialog (see below).
- **What a room has, as tabs** (2026-11-17, the user: the chat and the files are
  features too, in Rune's look): in the header, after the buttons "Members" and
  "Features" and a line, a tab for the chat, the files and each further feature the
  room shown has; without a room there are none. They are plain words, those whose
  pane shows with a line of the accent below. On a desktop a click shows the pane or
  puts it away, and it comes back where it was and as large as it was
  (`togglePane` in `layout.ts`); the hint says which. The pane of something the room
  lacks, as the chat of a room without one, keeps its place in the arrangement and
  isn't shown; without a room all panes show. When panes come and go, the rooms pane
  keeps its size and the panes with content share the rest. Files are in every room:
  on the stone they stay the base and aren't a feature (`FEATURES.md`), and nothing
  about them can be switched.
- **Minimizing the rooms pane** (2026-11-17, the user): a « at the right end of the
  pane's bar puts it away; away, it is a strip of 34 px at the left edge with a » and
  its name upright, and a click on the strip brings the pane back where it was and as
  wide as it was, also after a reload (the arrangement from before is kept on the
  device, `rune.layout.rooms`). When the other panes were arranged anew meanwhile, it
  comes back at the left edge. The strip is always at the left edge, wherever the
  pane was; a rooms pane taken out of the layout in the settings or by a preset shows
  as the strip too; the only pane shown can't be minimized. Only the rooms pane has
  this. (A first version with a button in the header was rejected the same day: the
  control belongs on the pane, and the pane away must be easy to find.)
- **Mobile**: one pane at a time, chosen with tabs in the header.
- **Header**: the room (a picker when the rooms pane is minimized) and the own avatar,
  which opens the account menu: change avatar, change passphrase, language and
  clock, theme, panes, notifications, keys for chambers, online status, stone
  settings and stones (System Administrator only), about Rune, log out. There is no separate settings button or bell.
- **Storage ceiling**: Rune tells a person when an upload would pass the stone's
  ceiling for stored content (`storageCeiling` of the version operation; the ceiling
  itself is EDS's, see `PLAN.md`, Clients API).
- **Status line** at the bottom: version of EDS, "Test system" if the
  data doesn't survive a restart, the live connection, who is logged in; for the
  System Administrator also the encryption in use and a warning while EDS isn't
  ready.
- **Stone settings** (System Administrator): a window over the panes, on phones too;
  the settings of EDS in groups, changed in place and saved together; algorithms
  chosen from `allowedValues`; the MasterKey source only shown.
- **About Rune**: how a room's content is encrypted and what isn't, whether this is a
  test or a production system, whether the connection uses HTTPS.
- **Settings**: language (the browser's, English or German), clock (24 or 12 hours),
  appearance (system, light, dark), theme (Rune, Fjord, Moss, High contrast) and the
  arrangement of the panes follow the account (`Member.settings`), read at login and
  written when one changes; the browser keeps a copy for the login page.
  Notifications stay with the device.
- **Rooms and users**: a room shows its members and their access, a user their rooms.
  Room administrators rename the room, change access, add existing users, remove
  members and invite (link and QR code). The System Administrator sees every room, creates users with a
  passphrase and deletes users. Users and rooms have avatars; the rooms listed under
  a user show the room's avatar too (2026-11-17, they showed the initial).
- **Several administrators** (2026-11-18, the user): a stone may have more than one
  System Administrator; the first, which claimed it, alone makes, unmakes and
  deletes them (`RUNNING.md`, "The System Administrator"). Its "New user" dialog has
  the choice "User" or "Administrator"; for other administrators it makes users. A
  user's details have "Make administrator" or "Make user" for the first, never for
  itself; deleting stays for users only, so an administrator is made a user first.
  The list marks "first admin" and "admin", from `Member.firstAdministrator`.
  Invitations are unchanged: they never carried a role, and the stone now refuses
  one that does. The flow test checks the rules as a second administrator.
- **Access** (2026-11-17, the user): nobody changes their own access, and the only
  administrator of a room keeps theirs: these rows show the access as text, with the
  reason as a hint. With two administrators each can change the other's. The
  question about the last administrator is asked when the System Administrator
  removes them. The rule is the clients'; the stone still accepts such a change, and
  the only administrator can still leave, after the warning.
- **The lists follow the stone** (2026-11-17): the stone doesn't tell a member that
  they were added to a room, or that a room or a user was made. The rooms pane looks
  every 20 seconds while Rune is visible, when one comes back to it, when the
  connection is back and when a room was left, and shows the lists anew only when
  they changed, so what is open or being typed stays. A new room or user shows after
  20 seconds at most; being removed at once.
- **Without rooms** the pane says: Create one with "New room" (it named a "+" which
  isn't there).
- **Room or chamber, when making one** (2026-11-17, the user's wording): a room is
  encrypted in transit and in storage, the stone encrypts and decrypts for its
  members, and only they can read the content; a chamber is a room with end-to-end
  encryption on top. The dialog also says what a chamber asks of its members: a key
  pair each, what an invitation link does, that a member added from an existing
  account makes their keys first, which apps read chambers, and that the choice is
  final. Tolk prints the same texts.
- **The key of a chamber reaches its members by itself** (2026-11-17; `E2E.md`,
  "Repair", and `RUNNING.md`, "Chambers: Who Gets the Key, and How"). Nobody reads,
  types or pastes a key, and there is no field for one. A member without keys makes
  them with "Make my keys" (in the chamber's chat pane, and under "Keys for chambers"
  in the account menu). Rune tends the open chamber: an administrator's Rune gives
  the key to those who wait for it, every 5 seconds while anyone waits (every 30
  after two minutes), and the Rune of a member who waits opens the chamber when the
  key has come. Before, the hand-over only ran when the administrator opened the
  members or the chamber anew, so an administrator already in the chamber saw "no
  keys yet" and nothing happened.
- **Chat**: replies, reactions (six), editing and deleting one's own messages,
  checkmarks for sent, received and read, typing indicator, unread counts,
  notifications, pasting and dropping files. Reactions are kept with the entry they
  came from (2026-11-17, `src/reactions.ts`): only the end of that entry ends them,
  and an earlier entry doesn't replace a later one; before, an end announced late
  took a member's reactions away.
- **Files**: folders, new folder, rename, move (also to another room), delete,
  upload with progress, share in the chat, live refresh; downloads with progress,
  pause and resume, also across a reload.
- Members who can only read a room get no chat input and no file changes.
- Images and videos render inline in chat via thumbnails / `<video>` with range
  requests, using short-lived download tokens (media elements can't send an
  `Authorization` header).
- A service worker caches only the app shell (network first), never API calls or
  content: requests below `api/` and `q/` pass it untouched. (Between the move to `/`
  on 2026-11-10 and the fix of 2026-11-13 it did cache them, as its scope then
  included the API; the fix renamed the cache, so the old one is removed.) The page
  and the service worker themselves are asked for again on every load, see
  "Serving".
- **Downloads** are written to the browser's private file system (OPFS) by a worker
  while they run, with their state in IndexedDB, so they survive a reload; on
  Chromium they are saved through the file picker. This is decrypted content on the
  person's device. It is removed when the download is saved or dropped, and all of
  it at logout; not when a session merely expires or the tab is closed (see "Open").
- **Uploads** aren't resumable (non-goal, the user, 2026-11-08: for gigabytes use
  other means and share the keys through EDS).
- **Session**: the session token is kept in `sessionStorage`, so per tab, and
  survives a reload.
- **Notifications**: opt-in, per device, in four levels (off, without details, with
  the room, with sender and text). Only while Rune is open; no Web Push. A click
  opens the room.
- **Media**: inline only JPEG, PNG, WebP, MP4 and WebM, as EDS serves them.
  Thumbnails are JPEG with a 320 px edge, at most 256 KiB, made on a canvas at
  upload. Download tokens are cached and renewed a minute before they end.
- **Chat uploads**: a folder is chosen for them (with "new folder"); a file dragged
  from the files pane is shared as it is; pasted images get a name from the time.
- **Avatars**: twelve creatures of the sagas for users (raven, wolf, bear, eagle,
  serpent, horse, boar, stag, goat, cat, squirrel, salmon) and twelve places or
  things for rooms (tree, longship, longhouse, mountain, fjord, bridge, hearth,
  shield, horn, helmet, sun, moon), as Rune's own line drawings; an initial where
  none is chosen. No runes and no symbols used by extremists.
- **Background pictures** behind the panes (web of wyrd, triquetra, Yggdrasil, an eight-lobed knot behind the plans), as
  masks in the theme's colour. The calendar and the location have none (2026-11-17,
  the user: a month is busy enough, and the location is still under consideration).
- **Drop-down lists** take the theme's colours for their options (2026-11-17): where
  the field is transparent, as the access in the rooms pane, the browser painted the
  list light in the dark theme.
- **In a narrow rooms pane** the buttons "New room" and "New user" go below their
  titles (2026-11-17); they ran over the pane's edge.
- **Tick boxes and choices** (2026-11-17) are as wide as themselves and stand at the
  first line of their words, in the accent when ticked. A rule for all fields made
  them as wide as their line: in the New room dialog the features' boxes stood in
  the middle, their names at the right border.
- **The calendar's month** (2026-11-17, the user) fills its pane, in width and in
  height: the weeks share the height, down to two lines of text (the day's number
  and one event, 46 px), below which the pane scrolls. A day shows the events it has
  room for, and "+n" beside its number for the rest, which opens the list. Before, a
  week with events grew, the more the narrower the pane; a fixed height of 84 px,
  tried first, was worse. The days of a week still grow with their events. The
  heading (the month, or the days) keeps the width of its words, so in a narrow pane
  the view's drop-down goes to the next line, not over it.
- **Stone settings**, in detail: a question before a "careful" setting (the
  algorithms) is changed and before the window closes with unsaved changes; settings
  Rune doesn't know are shown at the end.
- **About Rune** names the algorithms EDS uses by default (ML-KEM-1024, P-384,
  PBKDF2-HMAC-SHA512, HKDF-SHA512, AES-256-GCM) as fixed text; see "Open".

## Serving

Defined in `PLAN.md`, Streaming ("Serving user content safely"). Rune adds:

- Strict CSP for the app: `'self'` only for scripts, styles, workers and the
  manifest, no inline code; `connect-src` also allows `https:` and localhost, for the
  Stones screen (see "The status of stones"); `data:` for images, `blob:`
  for media (local video frames), workers from `'self'`. Also
  `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: no-referrer` and a `Permissions-Policy`. They apply to every path
  but the API and `/q` (`quarkus.http.filter.pwa.*`).
- A **preview** size limit (100 MB, set at build time with `VITE_PREVIEW_MAX_MB`):
  larger files are shown as download-only entries. Not to be confused with
  `eds.data.max-inline-size`, which limits data sent inside JSON requests.
- No server address is shown anywhere, and the invitation link is built from Rune's
  own address.
- **Caching** (2026-11-17): the page, `sw.js`, the manifest and the icons are served
  with `Cache-Control: no-cache`, so a new build is seen at once; only the files in
  `assets/`, which have a hash in their name, are kept (`public, immutable,
  max-age=31536000`). Quarkus serves static files with `public, immutable,
  max-age=86400` by default, which kept the page of an earlier build in the browser
  for a day: changes were not seen after a rebuild (`quarkus.http.filter.fresh.*` and
  `.kept.*`; `EdsPwaTest` checks both). A browser which still has a page from before
  needs one hard reload, or its site data cleared.

## Shared with Tolk

Tolk follows Rune in words, rules and what it can do (`TOLK.md`). Rune owns the files both
use; Tolk keeps copies, and its tests fail when they differ:

| File in `webui/src` | What | Tolk |
|---|---|---|
| `i18n/de.json` | the German catalog; English texts are the keys | copy in `eds-cli/internal/i18n/rune/`, renewed by `go generate` |
| `messages.json` | the table that turns EDS's messages into Rune's texts | copy, the same way |
| `rune-theme.css`, `themes.css` | the palettes | read by a Go test |
| `qr.ts`, `layout.ts`, `avatars.ts` | QR encoder, layout tree, avatar IDs | ported by hand |

So a text or message added in Rune needs `go generate` in `eds-cli`, and
`check-i18n.mjs` refuses texts Rune doesn't use itself: Tolk-only texts live in
Tolk's own catalog. A text of Rune's which Tolk uses must stay in use in Rune, or
Tolk's test fails: the question about the last administrator is one (2026-11-17).
What a new chamber asks of its members, and why a chamber can't be read yet, are
Rune's texts in both.

Settings with the account (`Member.settings`, a JSON object): `language`, `clock`,
`appearance`, `palette` (Tolk calls it `theme`, in its own config file), `present`
(shown as online) and `layout` (the layout tree). Tolk keeps its settings per machine and doesn't read these yet.

Only in the browser, not in Tolk: download tokens, pasting images and dropping files
on the chat, browser notifications, the plaited knot of the login and maintenance
pages, and dragging panes.

## Phase L5

Tasks:
- Quinoa setup, generated TS client, three-pane responsive layout.
- Thumbnails (stored encrypted), inline image/video preview, preview size limit, CSP.

Done when: the uber-jar serves the PWA; chat, file sharing, and video seeking work in
a browser; CSP and content headers verified.

Decisions (2026-11-07, the user): Quinoa, Vite and TypeScript, without a UI framework
(plain TypeScript and Web Components; the only runtime code besides the app is the
generated `typescript-fetch` client). Media elements use a per-object download token
(`PLAN.md`, Crypto §2a), no cookies. Thumbnails are made in the browser and stored
encrypted; the server stays content-agnostic.

Done 2026-11-08 (the server side, download token, thumbnails and content headers:
`PLAN.md`, Clients API, L5):
- PWA in `eds-quarkus/src/main/webui`, served at `/eds/app/` then (at `/` since
  2026-11-10; Quinoa 2.9.3, Node
  24.22.0, Vite 8.3.4, TypeScript 6.0.3; `package-lock.json` checked in). The client
  is generated from the contract in `generate-sources` (git-ignored). Strict CSP for
  the app (`'self'` only, no inline code; `blob:` for local video frames),
  `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`. One page without routes, so
  no SPA fallback over the REST paths.
- Layout: three panes on a desktop (Circles | chat | files), one at a time on a
  phone with tabs in the header; light and dark. Chat over the L4 WebSocket, with
  history paging and reconnect; shared files are cards with their thumbnail. Files:
  folders, grid or list, uploads with progress (drag and drop too), share to the
  chat. Preview: images and videos inline through download tokens (video seeking by
  range requests), everything else and media above the preview limit (100 MB,
  build-time `VITE_PREVIEW_MAX_MB`) as a download. A service worker caches only the
  app shell, never API calls or content.
- Live data changes: the files pane reloads when a file is shared in the chat; other
  changes (upload without sharing, by another Member) showed on the next reload
  until `CHANGED` came later the same day (below).
- Tests: `EdsPwaTest` (the app and its headers from Quinoa); the server's tests are
  in `PLAN.md`. The PWA's API flow (login,
  upload, thumbnail, share in the chat, receive, token, seeking ranges, history) was
  run with Node against the uber-jar. It wasn't clicked through in a browser here.
- Name and brand (2026-11-08, the user): the app is called Rune. Its brand set is in
  `accessories/brand/rune/` (masters, mobile icons for later); the web part (icons,
  favicons, manifest, theme tokens) is in the PWA. Its fonts, Space Grotesk and Inter
  (OFL), are self-hosted from `@fontsource/*` 5.3.0, so the app makes no requests to
  font services. In Rune, a Circle is called a room ("invited into the room"); only
  the texts change, the code and the API keep the EDS terms.
- Rooms and invitations (2026-11-08, the user): any member creates a room ("+"),
  and its administrators invite people into it. For a new name Rune shows a QR code
  and the link (`<Rune's address>#invite=…`, the token in the fragment, so it never reaches a
  server log); the invited person chooses a passphrase and is in the room at once
  (`inviteMember` with `circleId`, see `PLAN.md`, Crypto Invitations). An existing name is
  added at once. The System Administrator manages the system, room administrators
  their rooms. The QR encoder is Rune's own (`qr.ts`, byte mode, level M, versions
  1–40), checked by decoding every version with an independent decoder (done by
  hand at the time; the check isn't in the repository, see "Open"); no QR library.
  The account name in the header opened "change passphrase" (now the account menu).
- Later on 2026-11-08, each with what it needed from EDS (`PLAN.md`, "After L5"):
  - Rooms: members and their access, removing members, leaving a room (with
    warnings, also for the last administrator), only one's own rooms listed.
  - Files: live refresh on every change (`CHANGED`), delete files and empty
    folders, rename and move (one action menu per entry), choosing the folder for
    chat uploads, drag and drop onto folders, downloads with progress, pause and
    resume, also across a reload.
  - Chat: delete and edit one's own messages, replies and reactions, read receipts
    and typing indicators, unread counts and notifications, sharing images by
    pasting and files by dropping them on the chat.
  - Error messages speak of rooms and files; the login notices for a locked or
    unavailable EDS only point administrators to `RUNNING.md`.
- Not done: very large files in the PWA.

Changed after 2026-11-08:
- 2026-11-10: rooms and users pane (rooms over users, members and access,
  adding and removing, users created and deleted by the System Administrator),
  read-only rooms, a divider between chat and files.
- 2026-11-10: German, clock setting, panes arranged by each
  person (dragging, docking, presets), four themes; lighter accent in the dark theme
  for contrast.
- 2026-11-10: avatars for users and rooms; the account menu behind the
  own avatar replaces the settings button and the bell.
- 2026-11-10: roomier member lists and toolbars; checkmarks
  for sent, received and read (`RECEIVED` frame).
- 2026-11-10: no system salt, PBE iterations per
  Member, `Member.settings`; server settings for the System Administrator, the
  status line, settings follow the account, "Invite someone" in the room's details.
- 2026-11-10: login and maintenance page after the design (plaited knot,
  dark in every theme, own tab titles and browser bar colour); the server settings
  become a window over the panes; about dialog.
- 2026-11-10: the version operation reports `database` and `persistent`;
  Rune shows "Test system" in the status line, the about dialog, the login and the
  maintenance page, which only says the data is safe when EDS last reported a
  persistent database.
- 2026-11-10: "Sign in" and "Try now" are the same size (140 × 46); the
  test-system notice of the login moved from a block above the fields to the page
  footer.
- Tests: the full build ran with 796 tests and no failures after that; the two
  later changes were checked with the type check, the i18n check, the Vite build and
  screenshots.
- 2026-11-10, later: a default storage ceiling by kind of system (2 GiB test, 250 GiB
  production). No server address shown anywhere; Rune's texts say "stone" for
  a server instance, in English and German ("Stein").
- 2026-11-10, later: Rune moved from `/eds/app/` to `/`, and the REST API and the
  chat from `/eds` to `/api/v1`. The "Try now" button no longer flickers during the
  automatic retries.
- 2026-11-11: the German catalog and the message table became JSON files, shared
  with Tolk (`i18n/de.json`, `messages.json`); invitation links point to Rune's own
  address again (they were built below the API root since the move to `/`).
- 2026-11-13, review: the service worker never touches the API (it cached its
  answers since the move to `/`); the hints "Drop to share" and "Drop to upload"
  come from the catalog (they were CSS texts, so always English).
- 2026-11-13, second review: the service worker removes the hashed assets (`assets/`)
  which the page no longer uses, directly or through a script or style in use, when a
  navigation brings a page which differs from the kept one, and when the worker itself
  is new; only a navigation falls back to the kept page when the network fails. An upload whose content doesn't arrive removes its registration; earlier
  messages which arrive after the room was left are dropped; the open room is read
  again after a reconnect.
- 2026-11-17: reactions are kept with their entry (with Tolk). `eds.sh build` works
  behind an HTTP proxy and with `--podman` again: it puts Node's archive in Quinoa's
  cache with curl when `HTTPS_PROXY` is set, and Rune's build names its two test
  scripts itself, as the npm which Quinoa installs needs bash, which the build
  container lacks.
- 2026-11-17, chambers: "Make my keys"; the texts of the New room dialog; the
  refusals of a chamber say whose turn it is; Rune tends the open chamber, on both
  sides (`tendChamber` in `account.ts`, `watchKeys` in `components/app.ts`).
  `RUNNING.md` got "Chambers: Who Gets the Key, and How", and `E2E.md` says under
  "Repair" that a chamber is unusable for a member without it.
- 2026-11-17, the look: an eight-lobed knot behind the plans; the room's avatar in a
  user's rooms; drop-down lists follow the theme; the rooms pane is shaded, and the
  content of a chamber tinted and marked; the rooms pane is minimized to a strip.
- 2026-11-17, rules and lists: nobody changes their own access, the only
  administrator keeps theirs; the lists of rooms and users follow the stone; the
  hint without rooms names the button.
- 2026-11-17, serving: the page and the service worker are no longer kept by the
  browser over a new build.
- 2026-11-17, later: the tabs of what a room has (first for the further features,
  then for the chat and the files too), with `togglePane` and the rooms pane keeping
  its size; "E2E" on a chamber's panes and the hints which speak of the members; the
  calendar's month following its pane, and its heading; tick boxes beside their
  words.
- Tests, 2026-11-17: the type check, the i18n check (717 texts at the end), 96 and 65 unit
  checks, the flow test (100 checks) and `EdsPwaTest` (3). From this day on, changes
  to the pages are run in headless Chromium against a test stone beside the user's,
  with current sources through the Vite dev server: the hand-over of a chamber's key
  with two browsers (the administrator touched nothing, the member's chamber opened
  about ten seconds after "Make my keys", and both could chat), the avatar in a
  user's rooms, the access rule as an administrator, a second administrator and the
  System Administrator, the shading and the chamber's look in both themes, the lists
  following the stone (added after 14 s, removed after 1 s), and the minimized pane
  with real pointer clicks; later the tabs in a room with and without the chat
  (toggling, the order of the panes afterwards, dragging the rooms divider with the
  chat's pane left out, and at phone width), the "E2E" pill and its hint, the month
  measured at five sizes of its pane and below the chat (a day of 46 to 219 px, no
  event cut off), and the New room dialog. Before, such changes were checked by type check, unit
  tests and a static mock only, which missed that the hand-over didn't run (the
  user: "None of the changes work").

## Rooms and chambers

Rune's part of end-to-end encryption (`E2E.md`): what Rune does there, and the phases in which it
was built. The design, the backend and the convention both clients follow are in
`E2E.md`.

### What Rune does

- A module for the convention: keys, wrapping, vault, secrets, the content format
  and entries of features, on WebCrypto, tested against the vectors.
- Creating offers the choice between a room and a chamber, with the warning that
  everyone in a chamber needs a client that supports chambers, and that the choice
  stays. A chamber shows that it is one.
- The invitation link carries the invite secret; accepting one sets up the keys.
- The recovery key, linking a further device, and the page a device shows when it
  has no keys yet.
- The chat and the other features in chambers (`FEATURES.md`).
- Files: encrypted before upload, decrypted when shown or downloaded (the staged
  downloads decrypt while they stage). Pictures and thumbnails are decrypted in the
  page. Download tokens can't be used, since the browser's player would get
  ciphertext; so video and audio are staged first, decrypted, on the device's
  private file storage, and played from there. No new size limit, but playing
  starts when the file has arrived, not at once.
- Copy and move between rooms go through Rune when either room is a chamber.
- Repair on sight, fingerprints with the members, the warning for a changed key.
- Texts in both languages, and "About Rune" tells what E2E does and doesn't.

### Phase E3: Rune, keys and rooms

The module, the Member's keys, recovery key and further devices, creating E2E
rooms, invitations, removal with a change of key, repair, fingerprints, and the
chat as a feature (with T4 of `FEATURES.md`).

Done when: Rune's tests reproduce the vectors; and against the uber-jar, a room is
created, a person invited by link joins it without the inviter online, both chat,
one is removed and can't read what follows, and a second browser profile gets
access with the recovery key. The database holds no readable text of it.

Done 2026-11-15:
- `src/e2e/convention.ts` is the convention (WebCrypto only), and
  `scripts/test-e2e.ts` reproduces the vectors: 96 checks, run by `npm run test`
  and so by every build.
- `src/e2e/identity.ts` and `device.ts` keep the Member's keys: the account key in
  the browser's IndexedDB as a key which can't be exported, the key pair in the
  vault on the stone. `src/e2e/rooms.ts` is the rooms: the state of a chamber
  (ready, behind, waiting, locked, stripped), making one, inviting, adopting an
  invitation, repair and the change of key.
- The pages: "New room" asks room or chamber, with the warning; the recovery key
  is shown once, and the dialog closes only when the Member says it is written
  down; "Keys for chambers" in the account menu shows the fingerprint, or takes
  the recovery key on a further device, as does a link in the chat of a locked
  chamber; chambers are marked in the list of rooms; the members of a chamber show
  their fingerprints, and to administrators who waits for the key; an
  administrator who looks at the members repairs.
- Verified by `scripts/flow` (71 checks, users as worker threads with Rune's own
  modules, against the uber-jar): the chat in a room and a chamber, the stone
  holding what it can't read, an invitation by link with nobody online, repair,
  removal with a new key, a further device with the recovery key. And by hand in a
  headless Chromium against the uber-jar: creating a chamber, the recovery key,
  the invitation link, joining as the invited person, unlocking on another origin
  with the recovery key, and both reading each other.
- Differs from the plan: Rune can't show its account key again, so a further
  device gets the keys from the recovery key as written down, or from the device
  link (`#device=`) which Tolk shows (`tolk key link`); Rune takes either in the
  same field, and has no page which shows or scans a link. Not checked: a phone,
  and browsers other than Chromium and Node's WebCrypto.

### Phase E4: Rune, files

Upload, download, pictures, thumbnails, video and audio, copy and move.

Done when: files of the sizes Rune handles today go in and out of a chamber
unchanged, pictures and thumbnails show, a video plays, and what the stone stores
is ciphertext under its own layer.

Done 2026-11-15:
- `src/e2e/files.ts` encrypts and decrypts a file chunk by chunk
  (`contentWriter` and `contentReader` of the convention), into a staged file on
  the device's private file storage from 4 MiB on, otherwise in memory. Upload,
  thumbnails, the preview of pictures and videos, downloads and the files shared
  in the chat go through it; in a room nothing changes.
- A download of a chamber stages what arrives, as before, so pause and resume work
  with ranges, and decrypts when it is complete, into a second staged file. This
  differs from "decrypt while they stage": resuming needs the bytes as the stone
  has them.
- A move between rooms goes through Rune when either is a chamber: read, decrypt,
  encrypt for the other room, store, delete. Folders aren't moved that way. Rune
  has no copy between rooms, so there was nothing to do for it.
- Content of a chamber without the convention's first bytes is not shown as part
  of it: no thumbnail, no preview, a question before it is saved, and no move.
- The size shown for a file of a chamber is that of its content, not of what the
  stone stores.
- With "Checking keys" of `E2E.md`: a device remembers the public keys it has seen; a member
  with another key is marked with the members, and gets no room key from a repair
  or a change of key until the person has compared the fingerprint and accepted.
- "About Rune" has a part on chambers: what they do, and what they don't.
- Verified in a headless Chromium: a picture, a random file of 9 MiB and a recorded
  video went into a chamber; the stone held content starting with `EDSE` for each
  and for the thumbnails; thumbnail and preview showed, the video's frames
  decoded, both downloads had the hash of what went in, and a file moved to a room
  became readable to the stone and back in the chamber was not. The flow
  (`scripts/flow`, now 80 checks) covers files of a chamber by their hash, an
  empty file, and a removed member.
- Not done: audio, which Rune doesn't play in rooms either. Not checked: files of
  several GiB, Firefox and Safari, and a browser without the private file storage
  (the path in memory is the one the flow uses).

Done 2026-11-21 — **hidden file and folder names** (`E2E.md`, "Names"), after a
security assessment showed the names were the last cleartext a database dump or the
operator could read of a chamber:
- `src/e2e/names.ts`: `protectName` and `revealName`, the name encrypted like a
  small content under the context `name`, Base64url in the place of the name.
  `revealNames` decrypts a listing in place, so the tiles, the preview, downloads,
  the actions and a share to the chat work with the real names and nothing else has
  to know. A no-op in a room; a name of a client that doesn't know chambers is shown
  as it is.
- Encrypted where a name is sent (upload, new folder, rename, and the move between
  rooms, which re-encrypts for the other room); revealed where a listing is fetched
  (the files pane, the move dialog's folders, a file shared in the chat).
- The stone's `eds_metadata.name` was widened to `MAX_DATA_NAME_LENGTH` (512) to
  hold the ciphertext; the 75-character limit a person sees is the client's. A test
  vector (`a file name`) is to keep Rune and Tolk in step.
- Verified in a headless Chromium (the click test): a file uploaded to a chamber
  shows its real name to both members, each with its own key, while a raw read of
  the stone's own API returns only Base64url ciphertext.

## Features

Rune's part of the features of a room (`FEATURES.md`): what Rune does there, and the phases in which it
was built. The design, the backend and the convention both clients follow are in
`FEATURES.md`.

### What Rune does

- The general socket in place of the chat's; the chat on entries, with everything
  it does today.
- Stone settings: the features the stone offers, from the declarations Rune brings.
  On a new stone the chat is offered at first use.
- Room and chamber: choosing the features at creation (the chat is ticked), and
  switching them on and off in the details.
- A pane per feature, in the layout as the chat's is. A feature Rune doesn't know is
  named, with a line that this app can't show it.
  Since 2026-11-17 each has a tab in the header, as the chat and the files have,
  which shows its pane or puts it away; the pane of a feature the room lacks isn't
  shown (see "Rune as it is").
- The calendar: month, week and list, events with place and time, `.ics` in and
  out. Since 2026-11-17 the month fills its pane, and a day shows the events it has
  room for (see "Rune as it is").
- Plans: the plans of a room, each with its items, its expenses, what everyone
  owes or is owed, and settling with payments and confirmations.
- Location: a list of where people were last seen, with the place as numbers, the
  distance from this device, and a link that opens the device's own map app; and
  the switch for this device. No map in Rune: it loads nothing from elsewhere
  (decided 2026-11-15).
- Texts in both languages.

### Phase T4: Rune

Everything of "What Rune does" above, and the old chat leaves the stone
(`FEATURES.md`, "What goes from the stone"). In this order: the socket and the chat, the features in the
settings and rooms, the calendar, plans, location.

Done when: the chat does in a browser what it does today, in a room and in a
chamber; a calendar is shared by two people; three people share the costs of a
plan, settle it and close it; a position reported in one browser
shows in another; and switching a feature off hides it for everyone.

Done 2026-11-15:
- `src/socket.ts` is the general socket, `src/features/features.ts` the features and their
  entries (encoded as JSON, encrypted in a chamber), and `src/chat.ts` the chat on
  entries behind the interface the chat pane always used: messages, replies,
  files, reactions, receipts by position, typing as a signal, editing and
  deleting. The old chat left the stone in the same step.
- Stone settings list the four features Rune brings, to offer, update or withdraw;
  the chat is offered when the System Administrator first logs in. A room's features
  are chosen when it is made (the chat ticked) and switched under "Features" beside
  "Members": on, read-only, off, or removed with what they have. A feature Rune
  doesn't know is named there, with a line that this app can't show it.
- A pane per feature (`calendar-pane.ts`, `plans-pane.ts`, `location-pane.ts`, on
  `feature-pane.ts`), in the layout like the chat's. They aren't in the layouts Rune
  starts with: a tab in the header shows for each feature the room has, and brings
  its pane up; on a phone it is one more tab.
- Calendar (`features/calendar.ts`): month, week and list; events with place, note
  and time zone; repeating daily, weekly, monthly or yearly, each n-th time, until
  a day; `.ics` in (an event which is there already, by its UID, is replaced) and
  out. What Rune doesn't know of an event is kept; a rule of repeating it doesn't
  know is kept and the event shown once.
- Plans (`features/plans.ts`): the plans of a room, each with what is to do,
  expenses with shares, what everyone owes or is owed, the payments which settle
  it with "I paid this" and "I got this", guests by name, closing and opening
  again, and a plan's days into the room's calendar.
- Location (`features/location.ts`, `reporter.ts`): the list of where people were
  last seen, the place as numbers with a `geo:` link, the distance from this
  device, the track of the last day, and the switch for what this device reports
  to this room: nothing, the latest, a track too, or positions passed on as
  signals and not kept. In a room it says that a chamber is the better place.
- Verified by `scripts/test-features.ts` (50 checks, part of the build; the seven
  examples of plans, iCalendar there and back, repeating over a change of clocks,
  months without the day and leap years, in three time zones), by `scripts/flow`
  (99 checks: in a chamber, two share a calendar, three share the costs of a
  plan with a correction, payments, a confirmation and closing, a position of one
  shows for the other, and a feature switched off is refused and comes back whole;
  the stone holds only content starting with `EDSE`), and in a headless Chromium:
  offering and switching the features, an event repeating weekly in the three views,
  a plan with an expense, a payment for a guest, closing, and a position of the
  browser (emulated) with its track.
- Differs from the plan: a room made by something other than Rune's dialog has no
  chat until an administrator switches it on; Rune no longer adds it by itself.
  An `.ics` file goes out without `VTIMEZONE`: the times carry the names of the
  common time zones. A rule of repeating with `BYDAY` and the like isn't expanded.
- Not checked: a real position (a phone, a browser asking for permission), other
  browsers than Chromium, reminders (none are built), and files of another
  calendar program beyond the examples of the test.

## Presence and calls

Rune's part of `FEATURES.md`, "Presence and calls" (the user, 2026-11-17): the
light beside a person who is online, voice and video calls, and calls set for a
time. The design, the stone's part and the messages of a call are there.

### What Rune does

- **The light**: a small green dot at the avatar of a person who is online,
  wherever Rune lists people: the members of a room, the users, the members
  dialog. None when they aren't, and none for people Rune shares no room with,
  as the stone doesn't tell about them. Rune joins all rooms of the person at
  login, as it does for the unread counts, so the light needs nothing more.
- **"Show me as online"** in the settings, on by default, kept with the account's
  settings. Off, Rune joins every room unseen.
- **The feature "Calls"** among the features of a stone and of a room, with the
  declaration Rune brings. It has no pane and no tab.
- **A room call**: in the head of the chat, "Voice call" and "Video call" where
  the room has calls. While a call goes on, a line there says who is in it, with
  "Join". The others in the room get a note, and a notification if they chose
  notifications.
- **A direct call**: a telephone beside a person in the lists, where they are
  online and share a room with calls. It rings at the other's Rune, with "Accept"
  and "Decline", for 45 seconds.
- **The call itself** is a panel over the panes: a tile per person with their
  picture or their avatar and name, and buttons for the microphone, the camera
  and hanging up. Four people at most; a fifth is told that the call is full. On
  a phone it fills the screen.
- **Before the first call** Rune says in two sentences what the other devices
  and the stone learn (`FEATURES.md`, "What protects a call"), and the browser
  asks for the microphone, and for the camera when it is first switched on.
- **The relay**: under "Features" an administrator of the room names STUN and
  TURN servers, one per line.
- **A call set for a time**: in the calendar an event can be a voice or a video
  call. Such an event shows a telephone, and "Join the call" in its dialog. Rune
  looks at the calendars of all rooms and tells ten minutes before a call and
  when it starts, with "Join"; a notification too, if chosen.
- **Serving**: the header `Permissions-Policy` allows the microphone and the
  camera for Rune itself; they were switched off.
- Texts in both languages.

### Phase T8: Rune

Everything of "What Rune does" above.

Done when: in two browsers the light of the other comes on and goes off without
a reload, and stays off for one who chose not to be shown; two people talk and
see each other in a room and in a chamber, a third and a fourth join and a fifth
is refused; a direct call rings, is taken, and is declined; a call set in the
calendar is announced in the other browser and joined from there.

Done 2026-11-17:
- `src/presence.ts` keeps who is present in each joined room, as `src/socket.ts`
  is told by the stone, and makes the light: a green dot at the avatar
  (`withLight`), in the members of a room and in the users. The setting is
  "Online status…" in the menu under the person's name, kept with the account
  (`present`); changing it joins the rooms anew, so the others see it at once.
- `src/features/calls.ts` has the messages of a call, the rule of who offers, the
  relay, and who is in the call of a room; `src/calls.ts` makes the calls with
  WebRTC, as `FEATURES.md` has them: one connection to each other device, voice
  and video both ways from the start, signals to the room for a room call and to
  the other Member for a direct one, encrypted in a chamber.
- `components/call-panel.ts` is the call over the panes: tiles, "Mute", "Camera
  on" and "Hang up", the calls ringing here with "Accept" and "Decline" and a
  quiet tone, and what a call tells the others, said once per device.
  `components/notices.ts` shows notes in a corner: a call began, a call is due, why
  a call ended, a call which was missed.
- The chat has a bar at its top (`chat-pane.ts`): "Voice call" and "Video call",
  or who is in the call and "Join". A telephone beside a person
  (`call-button.ts`) calls them directly, with the voice; it shows while they can
  be called.
- "Calls" is the fifth feature Rune brings (`declarations/calls.json`), offered
  and switched like the others, without a pane. Under "Features" an administrator
  names the relay ("Relay…").
- The calendar marks an event as a voice or a video call (`X-EDS-CALL` in its
  VEVENT), shows a telephone before it, and has "Join the call" in its dialog.
  `src/reminders.ts` looks at the calendars of all rooms, and tells ten minutes
  before a call and when it starts, with "Join" and a notification if chosen.
- Serving: `Permissions-Policy` is `camera=(self), microphone=(self),
  geolocation=(self)`. The position was switched off for Rune itself before,
  which kept a browser from reporting one for the location feature.
- 59 new texts, in both languages.
- Verified by `scripts/test-features.ts` (93 checks, 28 more: the messages, who
  offers, the relay as written and as an entry, who is in a room's call, the line
  of a call in an event, which calls are due), by `scripts/flow/flow.ts` against
  the uber-jar (122 checks, 23 more: who is online for whom, hidden on one device
  and on both, the direct signal reaching one Member's devices only, the room
  call known to the others, a chamber's signal and relay unreadable for the
  stone, the relay named by administrators only), and in headless Chromium with a
  made-up microphone and camera, one browser per person, against the uber-jar
  with its real headers (71 checks): the light on, off and on again without a
  reload, and off for one who chose so; a video call in a room with sound and
  picture arriving both ways, the camera switched on later, muting, a third and
  a fourth joining, a fifth told that the call is full, one hanging up and one
  browser killed; a direct call declined, taken, hung up and given up, unseen by
  a third in the room; an event set as a call told to the other "in 3 minutes"
  and "starts now", joined from the note and from the event.
- Differs from the plan:
  - The buttons of a room call are in the chat, so a room without the chat has
    direct calls and calls joined from the calendar, and none begun by a button.
  - A direct call begins with the voice; the camera is switched on in the call.
  - Someone who chose not to be shown can't be called directly, as Rune offers
    the telephone for people who are present.
  - The call ends for a device whose connection to the stone goes, though sound
    and picture don't pass the stone: the others are told that it went.
- Not checked: a real microphone and camera, and the browser's question for
  them; two devices in different networks, a STUN or TURN server (the relay was
  stored and read, never used); other browsers than Chromium; a phone, where the
  call fills the screen; a call in a chamber in a browser (the flow test covers
  its signals); the ringing tone; notifications of the browser for calls; two
  devices of one person in one call.

## The status of stones

Rune's part of the status of a stone (`STATUS.md`): what Rune does there, and the phases in which it
was built. The design, the backend and the convention both clients follow are in
`STATUS.md`.

### What Rune does

- Stone settings: the stone's name, the two intervals, and status keys (create,
  shown once, list, withdraw).
- A screen "Stones" for System Administrators: the own stone by its session, and
  further stones added by address and status key. The keys are kept in the
  browser of that device.
- Per stone: name, awake or asleep, version, storage against the ceiling with the
  estimate of when it is full, people active, requests, the stone's share of the
  processors and the host's load, memory, the time requests take, each with a
  small curve of the recent load; and the day on request.
- Sorting by load, and colours for what is near a limit. The limits are the
  client's.
- Curves are drawn by Rune itself, without a library.
- A stone that doesn't answer is shown as such, with the time it last did.

### Phase M2: Rune

Everything of "What Rune does" above.

Done when: one Rune shows its own stone and a second one by key, live at one
second and over a day; a withdrawn key and a stopped stone show as such.

Done 2026-11-15:
- Stone settings have a group "The stone" (its name, the sessions per account,
  the two intervals) and a section "Status keys": create with a label, shown
  once, list, withdraw. The setting for allowed origins says that several are
  separated by commas.
- "Stones…" in the account menu of a System Administrator opens the screen
  (`components/stones-window.ts`, on `stones.ts` and `status.ts`): the own stone
  by the session, further ones by address and status key, kept in the browser's
  storage for that account. Each stone is a card: name, awake or asleep, version,
  database, since when it runs; storage against the ceiling, when it was counted
  and when it is full if it goes on as over the kept inventories; people active,
  requests per second, the stone's share of the processors, the host's load,
  memory and the time a request takes, each with a curve; refused or failed
  requests, the answer of the database, connections, and the counts of the
  inventory. A link switches the curves to the last 24 hours.
- The stones are asked every second, every five or every thirty; a client names
  the last sample it has, so an answer is small. The busiest stone comes first,
  one which doesn't answer or refuses before all. Colours mark what is near a
  limit; the limits are in `stones-window.ts`.
- The curves are SVG lines drawn by Rune (`curve` in `status.ts`).
- Rune's Content Security Policy now lets the page ask other addresses:
  `connect-src 'self' https: http://localhost:* http://127.0.0.1:*`. Before, it
  was `'self'` only, which is tighter; scripts still come from the stone alone.
  A stone without TLS on another host can't be added. This is the one place
  where the status screen costs Rune something.
- Verified by `scripts/test-features.ts` (addresses, merging samples, a stone which
  started anew, the estimate, levels, curves), and in a headless Chromium with
  two stones: the own one and a second by key at one second and over the day, a
  key which was withdrawn ("refuses the key") and the stone stopped ("does not
  answer", with the time it last did).
- Not checked: a stone which is asleep (locked, waiting for its MasterKey), and
  a second stone over TLS.

## The click test

`eds-quarkus/src/main/webui/scripts/click` (its README tells how to run it) drives
headless Chromium over the DevTools protocol, with nothing installed: a System
Administrator claims an empty stone, sets it up (features, status keys, allowed
origins, users, rooms and a chamber) and watches it under "Stones"; two users chat,
share, preview and download files, keep a calendar and plans, tell where they are,
call each other in the room and directly, and work in the chamber; a guest comes in
by an invitation link; a member opens the chamber on a second device with the
recovery key; the accounts change avatar, passphrase, theme, language and panes, and
Rune is used at the size of a phone. A step which doesn't work fails it, and so does
anything a page showed the developer (an exception, a console error, a request the
stone refused unexpectedly, a dialog nobody expected) and an ERROR in the stone's log.

It found six bugs on 2026-11-20, all fixed the same day: among them a room
closed by selecting text (the event is `room-select` now), thumbnails of others
never loaded (`loading: 'eager'`), and an invitation link in an open tab
(`hashchange`).

## Open

Design questions for the user. Bugs, improvements and what isn't tested in a
browser yet are tickets in `FINDINGS.md`.

- **The access rule** (own access, the only administrator) is in the clients only.
  The stone accepts such a change, `tolk access` makes it, and the only
  administrator can leave
  (`ProcessTrusteeManager`). To decide:
  - whether the stone should refuse it;
  - whether removing the only administrator should be refused rather than asked
    about.
- **Staged downloads** stay on the device when a session expires or the tab is
  closed; only a logout removes them. To decide whether an ended session should too.
- **A room one was added to** shows after up to 20 seconds. Showing it at once would
  need the stone to tell the member, which it doesn't. Accepted for now.
- **Decided, for the record:**
  - very large files are deferred (Tolk covers them);
  - one session per account was solved on 2026-11-15 (`PLAN.md`, "Several
    sessions");
  - server-side video posters aren't needed, as the browser draws the frame.
