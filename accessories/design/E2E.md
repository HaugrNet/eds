# EDS plan: end-to-end encryption

Project: EDS (Encrypted Data Share), https://github.com/HaugrNet/eds

Status: planned 2026-11-15, nothing built. Discussed and decided with the user the
same day; what is still to decide is listed under "Open decisions".

This plan crosses the three parts of EDS. `PLAN.md` (the backend), `RUNE.md` and
`TOLK.md` stay the plans of their parts; for end-to-end encryption they refer to
this file, and code comments cite it as `E2E.md`. Phases are named E1 to E6.

`FEATURES.md` builds on this plan and changes the same parts of Rune and Tolk, so the
two are built together: see "Order of work" there.

## The idea

Today EDS encrypts on the stone: a client sends content through TLS, and the stone
encrypts it with keys that come from the Member's passphrase. The stone sees the
content while it handles it. That was a choice of ten years ago, when a person using
a browser, a phone and a tablet could not be asked to carry a key between them, and
it stays the default: a shop built on EDS can't ask its customers for keys.

End-to-end encryption (E2E) adds a layer in the clients, below the one of the
stone: the client encrypts, the stone encrypts what it gets as it always did, and
never sees the content. It is optional per Circle.

Three things were decided about how:

- **EDS doesn't take part in it.** The stone offers a few places to store values it
  can't read, and rules for who writes and reads them. Algorithms, key formats and
  the exchange of keys are the clients' business. So other clients than Rune and
  Tolk can use an EDS stone with their own features, such as OpenSSL, or as part of
  their own PKI.
- **The login isn't touched.** The passphrase over TLS keeps authenticating the
  Member and opening the stone's layer. E2E has keys of its own.
- **The backend's part must be complete with this plan.** A change in the backend
  has dire consequences; the clients are released on their own and can improve
  step by step. So the aim is a backend design that needs no further change for
  E2E, not perfect clients from the start.

## Words

In Rune and Tolk (the user, 2026-11-15):

- A **room** is a Circle without E2E, as today: the stone is trusted, and it is
  the default.
- A **chamber** is a Circle with E2E. German: "Kammer". The word also fits the
  name of the project: a haugr is a burial mound, and the chamber is its closed
  inner room.

The variant is chosen when the Circle is created, by whoever creates it, and stays.
The clients warn at that point that everyone in a chamber needs a client that
supports chambers.

In the backend there are no such words, as EDS doesn't know the difference.

## Goals

- A Circle whose content the stone can't read, without the people in it knowing
  what a key is.
- No moment where two people must be online together: inviting, joining, adding a
  device and removing a Member all work with one client at a time.
- The backend stays neutral: it stores and guards, it doesn't interpret.
- Rune and Tolk follow one written convention with shared test vectors, so each
  reads what the other wrote, and a third client can follow it.
- Circles without E2E work exactly as today.

## Non-goals

- Changing the login, the sessions or the stone's own key hierarchy
  (`PLAN.md`, Crypto).
- Converting a room to a chamber, or back. It would mean encrypting everything
  again from a client, and substantial changes; the variant is a decision of the
  setup (the user, 2026-11-15).
- Hiding sizes, times, who talks to whom, and the names of rooms from the stone
  (see "What the stone still sees"). The names of files and folders in a chamber
  are hidden too, since 2026-11-21 (see "Names").
- The exchange between people who meet (QR code shown and scanned, NFC): that
  belongs to the apps. This plan only makes sure the link carries what is needed.
- A phone app.
- The plugin layer (see "Follow-up").

## What exists today

| Field | Stored in | Behaviour |
|---|---|---|
| `Member.publicKey` | `eds_members.external_key` (TEXT) | Stored as given, set by the Member with their account, returned with Members and Trustees. EDS never uses it. |
| `Circle.circleKey` | `eds_circles.external_key` (BYTEA) | One value per Circle, set by a Circle Administrator, sealed with a subkey of the Circle Secret, returned to every Trustee. |

The first is what a public key directory needs. The second is the same value for
everyone in the Circle, so it can only hold a shared key, and the stone can open it:
that is key sharing, not E2E. Both stay as they are.

On names: in `eds_members`, the column `external_key` holds the API's `publicKey`,
while `public_key` and `private_key` are the stone's own keys of the Member. New
names must not add to that.

## Backend: what the stone offers

### The values

| Value | API field | Stored in | Written by | Read by | Ends with |
|---|---|---|---|---|---|
| Public key of a Member | `Member.publicKey` (exists) | `eds_members.external_key` | the Member | Members who share a Circle with them, as today | the account |
| Vault of a Member | `Member.vault` (new) | `eds_members.external_vault` (new) | the Member | the Member only | the account |
| Shared value of a Circle | `Circle.circleKey` (exists) | `eds_circles.external_key` | a Circle Administrator | every Trustee | the Circle |
| Secret of a Member in a Circle | `Trustee.secret` (new) | `eds_trustees.external_key` (new) | the Member themselves, or a Circle Administrator | the Member themselves and the Circle's Administrators | the membership |
| The same secret while the invitation is pending | `secret` of `inviteMember` (new) | `eds_invitations.external_key` (new) | the inviter | nobody; carried over to the Trustee on acceptance | the invitation |

All of them are optional text, and EDS never looks inside.

- **Why Administrators read every secret of their Circle:** they may write them
  anyway, and a client can only repair a missing or outdated secret if it sees
  which ones are. A careful client stores a secret wrapped for its Member, so
  reading it gives an Administrator nothing.
- **At rest:** the vault and the public key are stored as given, like the settings.
  A Trustee's secret is sealed like the Circle's shared value, with a subkey of the
  Circle Secret (`eds/trustee/external-key|circleId|memberId`), so a client that
  stores a plain key there is still protected in the database. A MasterKey rotation
  and a change of passphrase leave it alone; a rotation of the Circle Secret
  (PLAN.md, Crypto C6) seals it anew, and the client reads the same text. The pending secret uses the same label
  (the invited account exists from the invitation on), so acceptance copies it.
- **Sizes:** `secret` and `vault` up to 65536 characters each, and `publicKey` and
  `circleKey` at least as much. That is far more than suite 1 needs, on purpose: a
  hybrid post-quantum key and its wrap take a few thousand characters, and a
  client may want to keep several device keys in one field.

### Why the backend needs no later change

What the clients may want later, and where it would live:

| Later wish | Needs from the stone |
|---|---|
| A post-quantum or other suite | nothing: the values are text, and the sizes leave room |
| A key per device instead of per Member | nothing: `publicKey` and `secret` can hold several |
| A passphrase for the vault | nothing: the vault is the client's |
| Signed lists of Members | nothing: they fit in the Circle's shared value |
| Hidden file and folder names | done 2026-11-21 (see "Names"): the client encrypts the name in its place; the stone's name column was widened to hold the ciphertext, and still only stores it. Since 2026-11-22 the stone keeps names unique by a Name Key the client sends (see "Names") |
| Content bound to its Data Object | nothing: a matter of the content format |
| Playing video while it is fetched | nothing: ranges of ciphertext are served today |

The plugin layer will change the backend again, but for its own reasons.

### The rules

- A Member sets their own secret in a Circle whatever their trust level, READ
  included.
- A Circle Administrator sets the secret of any Trustee of that Circle.
- A secret goes with its Trustee; a pending one with its invitation (expiry, use,
  MasterKey rotation, rotation of the Circle Secret, deletion of the Circle).
- One request sets the shared value of a Circle and the secrets of several of its
  Trustees, all or nothing, so a change of key can't end half done.
- EDS does not check that a Circle with secrets is used sensibly. A copy between
  Circles made by the stone, for one, copies content the target can't read. The
  clients have to know (decided 2026-11-15).

### API changes

| Operation | Change |
|---|---|
| `createCircle` | optional `secret`: the creator's own secret, set with the Circle |
| `addTrustee` | optional `secret` for the new Trustee |
| `inviteMember` | optional `secret`, kept with the invitation and carried over on acceptance. A name that exists already is added at once and gets it as a Trustee. |
| `alterTrustee` | optional `secret`; with one's own `memberId` any Trustee may call it to set their secret, and only that |
| `fetchTrustees` | returns `secret` for the requester's own records, and for all records of a Circle the requester administers |
| `updateMember`, and accepting an invitation | optional `vault`, like `settings`: an empty value removes it |
| `fetchMembers` | returns `vault` with the requester's own account only, as for `settings` |
| new: set secrets | `POST /trustees/setSecrets` and `PUT /circles/{circleId}/secrets`: an optional `circleKey` and a list of `memberId` and `secret`; Circle Administrator; one transaction; a `memberId` that isn't a Trustee fails the whole request |

The ID-based resource API gets the same fields as its POST twins.

## The convention of Rune and Tolk

Version 1. Everything in this chapter is done by the clients; the stone stores
text. A client of someone else may follow it, or use the same fields in another way
in its own Circles.

### Suite 1

| Use | Algorithm |
|---|---|
| Key pair of a Member | ECDH on P-384 |
| Wrapping for a Member | an ephemeral P-384 key, ECDH, HKDF-SHA-512, AES-256-GCM |
| Wrapping under a secret | HKDF-SHA-512, AES-256-GCM |
| Content | the chunked format of `PLAN.md`, Crypto §4, with keys and labels of its own |

All of it is in the browser's WebCrypto and in Go's standard library, so neither
client needs a library. It is not post-quantum; every stored value names its
suite, so a later suite can live beside this one, without the stone noticing.

### The Member's keys

- **Key pair:** made by the client the first time the Member needs one. The public
  key goes to `Member.publicKey`, in a form that names the convention and suite.
- **Account key:** 256 random bits, made at the same time. It wraps the private
  key, and the result is the `vault`. The account key never goes to the stone.
- **On a device:** Tolk keeps the account key in its container. Rune keeps it in
  the browser's key store as a key that can't be exported.
- **A further device** needs the account key once: the device that has it shows it
  as a link and QR code (`<Rune's address>#device=…`, in the fragment), or the
  person types it. Then it fetches the vault and has the private key.
- **Recovery:** the account key, written down, is the recovery key. Rune and Tolk
  show it when it is made and remind until the person says it is kept.
- **Lost:** without a device and without the recovery key the private key is gone.
  The person sets up new keys, and an Administrator's client gives them the room
  keys again (see "Repair"). A room where they were alone is lost.

### The room key

- 256 random bits per Circle, with a version that starts at 1, made by the client
  that creates the Circle.
- **The Circle's shared value** says that the Circle follows this convention, the
  suite, the current version, and holds the earlier room keys, each sealed under
  the current one. Its presence is what makes a room a chamber.
- **A Member's secret** holds the current room key, wrapped for that Member, either
  to their public key or, until they have one, under the invite secret. The
  binding (Circle, Member, version) is authenticated.
- **A device remembers** which rooms are chambers. If the stone later presents
  such a room without its shared value, the client refuses to write to it instead
  of sending plain content.

### Inviting

The stone makes the invitation's token and sees it again on acceptance, so the
token can't protect anything from the stone. The inviting client therefore makes an
**invite secret** of its own, 256 random bits:

1. The inviter's client wraps the room key under the invite secret, bound to the
   Circle and the invited name, and sends it as `secret` with `inviteMember`.
2. The link and QR code carry the name and the token as today, and the invite
   secret, all in the fragment. The link grows by about 45 characters.
3. The invitee's client accepts as today, makes the Member's keys, reads its
   secret, opens it with the invite secret, wraps the room key to the new public
   key and stores that as its secret. The invite secret is then forgotten.

Nobody else has to be online. Because the link never passed the stone, the stone
can't replace the key on its way.

- **A name that exists already** has a public key in the directory: the inviter's
  client wraps the room key to it. If the account has no keys yet, it is added
  without a secret and repaired later.
- **The System Administrator creating a user** without a room: nothing to wrap.

### Removing a Member, and changing the key

The client that removes a Member makes a new room key with the next version, wraps
it for every remaining Member's public key, seals the earlier keys under it, and
sends all of it in one `setSecrets` request. Old content stays readable through the
earlier keys; new content uses the new key.

### Repair

An Administrator's client that sees a Trustee with a public key and a missing or
outdated secret wraps the current room key for them and stores it. That covers an
account added before it had keys, a person who lost their keys, and someone who
accepted an invitation made before a change of key.

**Without the repair a chamber is unusable for such a member**: they are in it
and can read nothing, and there is no other way to them, since the stone never
has the room key. So a client must not leave the repair to chance, nor to a
place the Administrator has to find:

- Rune tends the chamber which is open: an Administrator's Rune looks for
  members who wait, every few seconds as long as there are any, and gives them
  the key; the Rune of a member who waits looks whether the key came, and opens
  the chamber then. Neither clicks anything (since 2026-11-17; before, the
  repair ran only when the Administrator opened the list of members).
- Tolk repairs when an Administrator lists the members of the chamber.
- A member without keys has to make them first, which only they can do. Rune
  offers it where the chamber refuses them, and under "Keys for chambers"; Tolk
  has `tolk key create`.
- Both tell each side whose turn it is: the member, that their keys are missing
  or that the key of the chamber is on its way from an Administrator; the
  Administrator, which members have no keys yet and which wait.

`RUNNING.md`, "Chambers: Who Gets the Key, and How", is the same from the side
of the people.

### Content

- A file is encrypted by the client and uploaded as it is. Its first bytes name the
  convention, the suite and the version of the room key; then follows the chunked
  format (64 KiB chunks, the nonce layout of Crypto §4), with an object key derived
  from the room key and a salt.
- Content is bound to the Circle and the key version, not to the Data Object, so a
  copy or move by the stone within one room keeps working.
- Thumbnails are encrypted the same way before they are stored.
- **Chat and other features:** the chat is rebuilt as a feature before the clients get
  chambers (`FEATURES.md`). The content of an entry is then the client's, and in a
  chamber it is encrypted as a whole, bound to the Circle and the key version. The
  stone still notes author and time itself.

### Clients that don't know chambers

The stone can't keep them out. Such a client shows a chamber as a room with
unreadable files and chat, and what it writes there is not protected. A client
that follows the convention recognises content without the convention's first
bytes, shows it as not protected, and doesn't pass it off as part of the chamber.
Hence the warning when a chamber is created.

### Checking keys

The stone hands out the public keys, so it could hand out one of its own before an
Administrator wraps a room key to it. Invitations by link aren't affected. For the
rest, clients show a fingerprint of each Member's public key, which two people can
compare, and remember the keys they have seen and warn when one changes.

### Test vectors

One file of vectors for wrapping, the vault, the secrets, content and entries of features,
computed by an independent implementation, as for Crypto §4. Rune owns it, Tolk
keeps a copy, and the tests of both must reproduce it (`RUNE.md`, "Shared with
Tolk").

### The formats, version 1

Fixed in E2 (2026-11-15). Text is UTF-8. `b64u` is Base64, URL-safe, without
padding. `HKDF` is HKDF-SHA-512 to 32 bytes; without a salt it uses its default.
`seal(key, aad, data)` is AES-256-GCM and gives `nonce (12) | ciphertext | tag
(16)`, the nonce random. Labels start with `eds-e2e/v1/`. JSON is written without
blanks; readers take any order of fields and pass over fields they don't know.
The independent implementation is `eds-quarkus/src/main/webui/vectors/e2e-v1.py`,
its vectors are `src/e2e/vectors.json` beside it.

**The Member's keys**

| Value | Form |
|---|---|
| Private key | the P-384 scalar, 48 bytes, big-endian |
| Public point | uncompressed: `0x04 | X (48) | Y (48)` |
| `Member.publicKey` | `eds-e2e:1:1:` and `b64u(public point)`: the convention, its version, the suite |
| Fingerprint | SHA-256 of the public point, the first 16 bytes, as eight groups of four hex digits |
| Account key | 32 random bytes |
| Recovery key | the account key as Base32 without padding, in groups of four with hyphens (13 groups) |
| Device link | `<Rune's address>#device=` and `b64u(account key)` |
| `Member.vault` | `{"e2e":1,"suite":1,"salt":b64u(32 random bytes),"key":b64u(seal(k, label, private key))}` with `label = eds-e2e/v1/vault|<memberId>` and `k = HKDF(account key, salt, label)` |

**The room key**

| Value | Form |
|---|---|
| Room key | 32 random bytes, with a version from 1 |
| `Trustee.secret`, to a public key | `{"e2e":1,"suite":1,"by":"key","version":v,"epk":b64u(ephemeral public point),"key":b64u(seal(k, label, room key))}` with `label = eds-e2e/v1/secret|<circleId>|<memberId>|<v>`, `shared` the X coordinate of ECDH between the ephemeral private key and the Member's public point, and `k = HKDF(shared, epk | member's point, label)` |
| `Trustee.secret`, of an invitation | `{"e2e":1,"suite":1,"by":"invite","version":v,"key":b64u(seal(k, label, room key))}` with `label = eds-e2e/v1/invite|<circleId>|<invited name>|<v>` and `k = HKDF(invite secret, no salt, label)`. The name is the one in the link, as it is. |
| `Circle.circleKey` | `{"e2e":1,"suite":1,"version":v,"earlier":{"1":b64u(seal(k, base|1, room key 1)), …}}` with `base = eds-e2e/v1/earlier|<circleId>|<v>` and `k = HKDF(room key v, no salt, base)`. A new chamber has `"earlier":{}`. |
| Invitation link | `<Rune's address>#invite=` and `b64u` of `{"n":name,"t":token,"k":b64u(invite secret)}`; `k` only for a chamber |

**Content**: a file, a thumbnail, an entry of a feature.

```
header = "EDSE" | 0x01 (format) | 0x01 (suite) | room key version (4, big-endian)
       | 0x01 | chunk size (4, big-endian) | salt (32) | nonce prefix (7)        = 54 bytes
key    = HKDF(room key of that version, salt, eds-e2e/v1/content|<circleId>|<version>)
aad    = header | "|" circleId "|" context
chunk  = AES-256-GCM(key, nonce = prefix | chunk index (4, big-endian) | last (1 or 0), aad)
```

The chunks follow the header, each of the chunk size but the last; an empty content
is one empty chunk, and a content which fills its chunks exactly ends with a full
one marked as the last. This is the layout of `PLAN.md`, Crypto §4. Writers use 64
KiB; readers take 16 bytes to 16 MiB. The `context` says what the content is,
which the reader knows from where it reads: empty for a file, `thumbnail` for a
thumbnail, `<feature>|<kind>` for an entry, `name` for the name of a file or folder.
So a thumbnail isn't taken for its file, nor an expense for a payment, nor a name for
a file's content. An entry of a feature in a chamber is its form of
`FEATURES.md`, encrypted this way; it grows by 70 bytes.

### Names

The name of a file or folder in a chamber is encrypted like a small content, under
the context `name`, and stored in the place of the name (the API's `dataName`). The
stone, a database dump and a curious operator see only Base64url ciphertext, never
"tax return.pdf". A client encrypts the name when it creates, uploads, renames,
copies or moves into the chamber, and decrypts it to show it; a name written by a
client that doesn't know chambers is shown as it is, as its unreadable content would
be. The root folder keeps its `/`.

The stored name is longer than the name a person types (the ciphertext of a
75-character name, Base64url encoded, is about 494 characters), so the stone's name
column holds up to `MAX_DATA_NAME_LENGTH` (512); the 75-character limit a person
sees is the clients'. Room names, media types, sizes and times stay as they are
(see "What the stone still sees").

**Name Key** (backend since 2026-11-22). The stone keeps a name unique within its
folder, but each encryption differs, so it can't compare two encrypted names. A
client of a chamber therefore sends a Name Key next to the encrypted name: a keyed
hash of the name, `nameKey` in the API, Base64url without padding, at most
`MAX_NAME_KEY_LENGTH` (128) characters. The stone stores it in
`eds_metadata.name_key`, gives it back in `Metadata.nameKey`, and refuses a second
Data Object with the same Name Key in the same folder ("Another record with the same
name already exists."), with a unique constraint on `(parent_id, name_key)` behind
the check, so two uploads at the same moment can't both succeed. The stone compares
it exactly and never learns the name from it. How the stone treats it:

- `ADD` stores the Name Key given, or none.
- `UPDATE` with a new `dataName` replaces the Name Key with the one given, or drops
  it if none is given, so a key never outlives the name it was made from. A Name Key
  alone is set on the present name, e.g. for objects stored before, or for a move
  to another folder when the key depends on the folder.
- `COPY` and `MOVE` to another Circle store only the Name Key the request gives for
  the target Circle; the old one is made with the old Circle's key.
- Without a Name Key, nothing changes: a room, or a client that doesn't know
  chambers, is checked by the name as before.

What the clients must agree on (the Rune and Tolk stories): the key, the input and
the normalisation, with a test vector in `vectors.json`. The stone compares names
case-insensitively, so the input should be the name normalised (NFC) and lowercased.
The key must stay the same across room key versions, or the Name Keys of older
objects no longer match after a rotation (or the clients re-key them with an
`UPDATE`). A Name Key shows the stone which objects share a name; with the folder's
ID in the input, only within one folder, where the stone refuses the second one
anyway, at the cost of a new Name Key on a move between folders.

**What a reader refuses**: content of a chamber without `EDSE` at its start is
shown as not protected; an unknown format or suite, a room key version it has no
key for, and anything which doesn't authenticate, is refused with a message.

## What the stone still sees

- The media types, sizes and times of files, and the names of rooms. The names of
  files and folders in a chamber are hidden (since 2026-11-21, see "Names"); in a
  room they are as given. Which objects in a chamber share a name, by their Name
  Keys (see "Names").
- Who is in a room, who wrote when, reactions, receipts and typing.
- Who is connected, and to which rooms; and of a call who calls whom and when,
  never what is said (`FEATURES.md`, "What protects a call").
- That a room is a chamber.

And two limits of what is protected:

- **Rune is served by the stone.** A stone that turns against its users can serve
  a changed Rune. In a browser, E2E protects against a copy of the database and a
  curious operator, not against an operator who changes the app. Tolk gives the
  full protection. Rune's "About" says so.
- **Content isn't bound to its Data Object.** The stone could present one file of
  a room in the place of another of the same room. It can't read or change either.

## Rune

In `RUNE.md`, "Rooms and chambers".

## Tolk

In `TOLK.md`, "Rooms and chambers".

## Phases

The phases of this plan and of `FEATURES.md` were built in one order, one at a time,
the two backends first and each client once; the table is in `FEATURES.md`, "Order
of work". For the review the work is handed over by layer, not by phase (`PLAN.md`,
"Handed over for review"): E1 and E2 are here, with the backend; E3 and E4 are in
`RUNE.md`; E5, and E6 which needs both clients, in `TOLK.md`.

### Phase E1: the backend

The values, rules and API changes of "Backend", with the OpenAPI documentation in
the same phase, and pointers to this file in `PLAN.md`,
`RUNE.md` and `TOLK.md`.

This is the phase that has to be right, see "Why the backend needs no later
change".

To check first: that storing a thumbnail doesn't look at its bytes, that an Administrator can read the
public key of an account before adding it to a Circle, and the limits of
`publicKey` and `circleKey` today.

Done when: `mvn clean install` passes with the completeness test; unit tests cover
each rule (own secret with READ, another's secret refused without ADMIN, the vault
unreadable for others, secrets gone with Trustee and invitation, the pending secret
carried over, `setSecrets` all or nothing, secrets readable after a MasterKey
rotation and a change of passphrase); FitNesse covers the round trip; and a
walk-through with OpenSSL in `RUNNING.md` shows a Circle used with keys EDS never
saw.

Done 2026-11-15, after N1 (several sessions, `PLAN.md`):
- `Trustee.secret` (`eds_trustees.external_key`), the same on a pending invitation
  (`eds_invitations.external_key`), and `Member.vault`
  (`eds_members.external_vault`).
- The secret is sealed with an HKDF subkey of the Circle Secret
  (`eds/trustee/external-key|circleId|memberId`), like the Circle's shared value;
  the vault is stored as given. Limits: 65536 characters each. `publicKey` and
  `circleKey` had no limit and keep none.
- `createCircle`, `addTrustee` and `inviteMember` take a `secret`; a name which
  exists already gets it at once. Accepting an invitation copies the waiting secret
  to the new Trustee and takes a `vault`.
- `alterTrustee` takes `trustLevel`, `secret` or both. With only a `secret`, every
  Trustee sets their own (READ is enough) and an administrator of the Circle that
  of anyone in it. An empty secret removes it.
- New: `POST /trustees/setSecrets` and `PUT /circles/{circleId}/secrets` (action
  `SECRETS`): a list of `memberId` and `secret`, and optionally `circleKey`.
  Everything is checked before anything is changed, and the Circle is locked
  meanwhile.
- `fetchTrustees` returns a secret with the requester's own records and with all
  records of a Circle they administer; `fetchMembers` returns the vault with the
  requester's own record only.
- The System Administrator neither reads secrets nor sets them without being a
  Trustee of the Circle: sealing takes the Circle Secret.
- Checked first: storing a thumbnail looks at the declared media type, not the
  bytes; every Member can already fetch any account with its public key, so an
  administrator can wrap for a name before adding it; `publicKey` and `circleKey`
  are unlimited text.
- The walk-through in `RUNNING.md` ("Encrypting in the Client") was run against the
  uber-jar: two accounts, RSA keys from OpenSSL, a Circle key wrapped for each, a
  file encrypted by one and read by the other.
- Tests: `SecretsTest` (21 tests: own secret with READ, another's refused without
  ADMIN, readers, sealed in the database, removal, with the Trustee, `setSecrets`
  all or nothing, invitations, a new passphrase, a new MasterKey, the vault, the
  limits). FitNesse `EDS.API` has the secrets in its Trustees section; the suite
  passed (744 0 0 0 as right, wrong, ignored, exceptions on the page).
- Differs from the plan: the old chat is left alone (it becomes a feature, `FEATURES.md`),
  so "the chat passes a text through unchanged" wasn't checked. The pointers to this
  file are in `PLAN.md`, `RUNE.md` and `TOLK.md`.
- Left for the clients: Rune and Tolk don't use the new fields yet.

### Phase E2: the convention

The exact formats of this file's convention chapter, written out byte by byte, and
the test vectors from an independent implementation.

Done when: the formats are in this file, the vectors are in the repository with the
script that made them, and they cover wrapping to a key and under a secret, the
vault, the Circle's shared value with earlier keys, content (empty, partial, exact
multiple, several chunks) and an entry of a feature.

Done 2026-11-15:
- The formats are in "The formats, version 1" above.
- `eds-quarkus/src/main/webui/vectors/e2e-v1.py` is the independent
  implementation (Python with the package `cryptography`, nothing of EDS), and
  `src/e2e/vectors.json` what it made: the keys with public key text, fingerprint,
  recovery key and device link; the vault; a secret to a public key and one of an
  invitation, with the link; the shared value with two earlier keys and that of a
  new chamber; content (empty, partial, an exact multiple, several chunks, a
  thumbnail, an entry of a feature, an earlier key), and a file of two full chunks of
  64 KiB and a rest, by its hash.
- Decided here: a content says what it is (`context`), so the stone can't present
  a thumbnail as its file or one kind of entry as another. The chat's text is an
  entry of the chat feature, so it has no form of its own.
- Rune keeps the account key as a key which can't be exported, so Rune can't show
  it again: the recovery key is shown when the keys are made, and a further device
  is linked with it, or from Tolk, which can show it.
- Not here: the clients which reproduce the vectors; that is E3 and E5.

### Phase E3: Rune, keys and rooms

In `RUNE.md`, "Rooms and chambers".

### Phase E4: Rune, files

In `RUNE.md`, "Rooms and chambers".

### Phase E5: Tolk

In `TOLK.md`, "Rooms and chambers".

### Phase E6: together

In `TOLK.md`, "Rooms and chambers".

## Decisions

All 2026-11-15, the user:

- E2E becomes a first-class, optional part of EDS; the plugin layer follows and
  builds on it.
- The login and the stone's key hierarchy aren't used for it.
- EDS offers neutral values; the decision how to use them is the clients'. The
  existing `publicKey` and shared Circle value stay, for other tools and PKIs.
- The secret per Member in a Circle is the one addition that matters, and it lives
  with the Trustee.
- Nobody needs to be online for another; the invitation carries what is needed.
- The exchange itself (QR code, NFC, people passing each other) is the apps'.
- One plan across backend, Rune and Tolk, in this file.
- The backend's part is to be final; the clients may start simple. So suite 1 is
  classical, and a post-quantum suite is added in the clients when browsers offer
  ML-KEM dependably (Chrome has been trying it since mid 2026; nothing confirmed
  was found for Firefox and Safari, checked 2026-11-15). Go has it in its standard
  library.
- A Circle without E2E is a room, one with E2E a chamber ("Kammer"). "Varde" was
  considered first.
- Room or chamber is chosen at creation and stays, with a warning that all clients
  of its people must support chambers.
- The words "recovery key" ("Wiederherstellungsschlüssel") and "link a device"
  ("Gerät verbinden").
- Several sessions per account come first (`PLAN.md`, "Several sessions"), so a
  further device doesn't end the session of the first.
- One review at the end of all the work, not after each plan: every phase leaves
  a summary of what was built and what differs from the plan, in its plan file.

## Open decisions

- **Playing while fetching** in a chamber, through the service worker, so a video
  starts at once: later, in Rune only.
- **Names.** Hiding file and folder names, with a list kept by the clients; not in
  version 1.
- **A passphrase for the vault** instead of the recovery key, for people who would
  rather remember than keep something: weaker against a copy of the database, so
  not offered in version 1.
- **Signed lists of Members,** so a client needn't trust the stone's directory at
  all: later, if fingerprints prove too little.
- **Certificate pinning** for a stone on an unusual host: the link could carry the
  certificate's fingerprint for Tolk and a phone app. A topic of its own.
- **Whether the stone should refuse** its own copy between Circles when one has a
  shared value: decided no for now, as EDS doesn't interpret.

## Follow-up: features

Planned in `FEATURES.md` since 2026-11-15, where "plugin" became "tool" and then, on 2026-11-17, "feature". What follows
is the note as it was written before that plan, kept for what it says about this
one.

The idea (the user, 2026-11-15): what comes beside the chat becomes plugins that can
be switched on and off, each with a reserved folder in the Circle: the chat as the
first (`.chat` exists), then a shared calendar (`.calendar`) and a family location
service (`.location`). A plugin is a convention on general facilities of the
stone, not code loaded into it, since the stone doesn't interpret content.

What the stone would have to offer, as far as discussed: reserved folders, notice
of changes over the socket, a list of what is switched on, and rules per plugin,
such as how long things are kept and who may write what.

What this plan gives it:

- Content the stone can't read, which the location service needs most.
- The first case of "a place in a Circle that only its owner writes": the secret
  of a Member. The location service needs the same rule for positions, so the
  plugin layer should make it general.
- Names stay visible to the stone, but a plugin chooses the names in its folder
  and can make them meaningless.

Open there: whether the chat can be rebuilt on those facilities, where plugins are
switched on (the stone, the Circle, or both), and a client for phones, which the
location service needs.
