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
| Unprivileged account | fetch by data and circle id, resource GET, content stream, a download token, cross-circle copy, self-add as Trustee, list Trustees, enumerate Circles | all denied (401/404); the Circle isn't even visible; a real and a random id give the same 404, so no existence oracle |
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
2026-11-22 (`E2E.md`, "Names"; `RUNE.md`). The only backend change was to widen the
name column so it can hold the ciphertext; the stone still only stores the name.

- **Done:** Rune hides file and folder names in chambers, verified in a real
  browser (both members see the real name; the stone's own API returns only
  ciphertext).
- **Not done:** Tolk doesn't hide names yet; it shows a chamber's file names as
  ciphertext until it follows the same convention (its content encryption is built,
  names are the next step).

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
- **Authentication checks with limitations** (issue #68): not started; a question
  of the integrating system more than of the stone.

## Open

- Tolk: hide chamber names, to match Rune and the convention.
- Rune and Tolk: send the Name Key with a chamber's names, so the stone keeps them
  unique within a folder again (`E2E.md`, "Names"); the stone takes it since
  2026-11-22.
- Decide whether to hide room names, and if so how the uniqueness check adapts.
- An active-operator study (tampering between requests to confuse a client) and a
  concurrency study of the rotation: these test integrity and availability, not the
  confidentiality claim, which stands.
