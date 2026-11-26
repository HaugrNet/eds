## POST /members/createMember
permission: PROCESS_MEMBER
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.
- IDENTIFICATION_WARNING: An account with the name already exists.
- VERIFICATION_WARNING: The name or credential is missing, the name is too long, or the credential is too weak.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `newAccountName` | yes | Up to 75 characters, unique (case-insensitive). |
| `newCredential` | yes | The new Member's passphrase, Base64-encoded; must meet the entropy rules. |
| `memberRole` | no | `STANDARD` (default) or `ADMIN`. |

System Administrator only. Returns the new Member's ID in `memberId`.

## POST /members/inviteMember
permission: PROCESS_MEMBER
- ILLEGAL_ACTION: No `circleId` is given, and the requesting Member isn't the System Administrator.
- AUTHORIZATION_WARNING: The requesting Member isn't an administrator (trust level `ADMIN`) of the Circle.
- VERIFICATION_WARNING: `trustLevel` isn't `READ`, `WRITE` or `ADMIN`.
- CONSTRAINT_ERROR: An account with the name already exists (without a Circle), or has an invitation pending.
- IDENTIFICATION_WARNING: The existing account is already a Trustee of the Circle.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `newAccountName` | yes | Up to 75 characters, unique. |
| `circleId` | for Members | The Circle the invited Member joins. Required unless the System Administrator invites. |
| `trustLevel` | no | `READ`, `WRITE` (default) or `ADMIN`, in the Circle. |
| `memberRole` | no | System Administrator only: `STANDARD` (default) or `ADMIN`. |
| `secret` | no | With a `circleId`: the invited Member's secret in the Circle, see `alterTrustee`. |

The System Administrator may invite anyone; other Members invite into a Circle they
administer. Returns the new account's ID in `memberId` and a one-time invitation
token in `invitation`. EDS stores only a keyed hash of the token, so it can't be
fetched again. The invitation expires after `eds.invitation.timeout.hours` (default
168), and becomes void if the MasterKey changes. While it is pending, the name can't
be invited again; once it has expired, a new invitation for the name replaces it.

With a `circleId`, the invited Member joins the Circle when accepting: the Circle's
key is sealed with a key derived from the token (HKDF-SHA512) and with the MasterKey,
so only the holder of the token can unlock it, and no one has to add the Member after
the acceptance. If the name already belongs to an account, it is added to the Circle
at once with `trustLevel`, and no invitation is returned.

A `secret` is kept with the invitation and becomes the Trustee's secret when the
invitation is accepted, or at once for an account which exists already. A client can
so hand the invited person a key of its own, wrapped with something only the two
share, without EDS being able to read it and without anyone having to be online when
the invitation is accepted.

To accept, the invited person sends a request to `updateMember` with
`credentialType` `INVITATION`, `accountName` set to the invited name,
`credential` set to the token, and their chosen passphrase in `newCredential`
(optionally a `publicKey` and a `vault`).

## POST /members/login
permission: PROCESS_MEMBER
- VERIFICATION_WARNING: The request is invalid (required fields are missing, values are too long or IDs are invalid), or it comes with a session; a login needs the passphrase.

Starts a session for the Member. **Fields:** only the authentication fields. A
login needs the passphrase: a session can't make another session, so one that is
stolen ends when it expires.

EDS generates a random 256-bit session token and returns it once in `session`;
it stores only a keyed hash of it (HMAC with a MasterKey-derived key) and a copy of
the Member's private keys protected by a key derived from the token (HKDF-SHA512)
and the MasterKey. Until the session expires (`eds.session.timeout.minutes`,
default 480) or the Member logs out, requests can authenticate with
`credentialType` `SESSION` and the token as `credential`, without `accountName`.
Requests with a session don't run PBKDF2, so they are cheap. A Member may have
several sessions, one per login, so several devices can be used side by side; with
more than `eds.session.max.per.member` (default 10), a new login ends the session
that expires first. Changing the passphrase or the MasterKey ends all of them.

## POST /members/logout
permission: PROCESS_MEMBER

Ends the session the request comes with; the Member's other sessions stay.
**Fields:** only the authentication fields.

## POST /members/alterMember
permission: PROCESS_MEMBER
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.
- ILLEGAL_ACTION: The Member to alter is the requester or the System Administrator account.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `memberId` | yes | The Member whose role changes. |
| `memberRole` | yes | The new role. |

System Administrator only.

## POST /members/updateMember
permission: PROCESS_MEMBER
- ILLEGAL_ACTION: The System Administrator account is renamed, or the new name is `admin`.
- CONSTRAINT_ERROR: The new account name already exists.
- VERIFICATION_WARNING: The new name is too long, or the new credential is too weak. With a session: the credential or the account name is changed, or a public key or vault is replaced or removed, which need the passphrase. When accepting an invitation: the account has no invitation pending, or `newCredential` is missing.
- IDENTIFICATION_WARNING: When accepting an invitation: the account doesn't exist.
- AUTHENTICATION_WARNING: When accepting an invitation: the token is wrong, or the invitation has expired.

**Fields** (all optional; only the given ones change):

| Field | Notes |
|---|---|
| `newAccountName` | New account name, up to 75 characters. Only allowed when authenticated with the passphrase. |
| `newCredential` | New passphrase, Base64-encoded; must meet the entropy rules. Only allowed when authenticated with the passphrase. |
| `publicKey` | New external public key. With a session, only while the account has no `vault`; after that, it needs the passphrase. |
| `avatar` | New avatar, the ID of a picture the clients know. An empty value removes it. |
| `settings` | The Member's settings for clients, a JSON object as text of at most 4096 characters; EDS stores it as it is. An empty value removes them. |
| `vault` | The Member's vault: text of at most 65536 characters, which EDS stores as it is and returns to the Member only, e.g. their own private key, wrapped by a client, so their other devices can fetch it. An empty value removes it. With a session, only the first one; replacing or removing it needs the passphrase. |

Updates the requesting Member's own account. Changing the passphrase also
re-protects the Member's access to all their Circles. This endpoint is also used to
accept an invitation, see `inviteMember`.

## POST /members/invalidate
permission: PROCESS_MEMBER
- ILLEGAL_ACTION: The requesting Member is the System Administrator.
- VERIFICATION_WARNING: The request is invalid (required fields are missing, values are too long or IDs are invalid), or it comes with a session; invalidating needs the passphrase.

Invalidates the **requesting** Member's own credentials: the passphrase is replaced
by a random one and the session ends, so the account can no longer be used.
`memberId` is not used. The account keeps existing; access to Circles is lost,
since the Circle keys can no longer be unlocked.

## POST /members/deleteMember
permission: PROCESS_MEMBER
- ILLEGAL_ACTION: A Member other than the System Administrator deletes another account, the System Administrator deletes their own account, the System Administrator account is to be deleted, or the account is the last Trustee of a Circle.
- IDENTIFICATION_WARNING: No Member with the given `memberId` exists.
- VERIFICATION_WARNING: The request is invalid (required fields are missing, values are too long or IDs are invalid), or the requester's own account is deleted with a session, which needs the passphrase.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `memberId` | no | The account to delete (System Administrator only). Omit to delete the requester's own account, which needs the passphrase. |

Deletes the account and its Trustee records. Data stored in Circles stays with the
Circles. This can't be undone.

An account which is the last Trustee of a Circle can't be deleted: nobody could unlock
that Circle's data afterwards. Such Circles are deleted, or get another Trustee, first.

## POST /members/fetchMembers
permission: FETCH_MEMBER
- IDENTIFICATION_WARNING: No Member with the given `memberId` exists.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `memberId` | no | Fetch only this Member. |

Without `memberId`, returns all Members sorted by name (no paging). With
`memberId`, returns that Member and Circles: for the requester themselves, their
own Circles; for the System Administrator, all Circles of the Member; for others,
all Circles of the Member if `eds.show.trustees` is true, otherwise (the default)
only the Circles both share. With it true, any Member can learn every Circle of the
stone and who is in it, though not its data. The requester's own record carries their `settings` and their
`vault`; nobody else gets them.

## GET /members
same-as: POST /members/fetchMembers

Lists the Members.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /members/{memberId}
same-as: POST /members/fetchMembers

Returns the Member `memberId` and the Circles it shares with the requesting Member.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /members
same-as: POST /members/createMember

Creates a Member. The body has the fields of `createMember`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /members/{memberId}
same-as: POST /members/alterMember

Changes the role of the Member `memberId`; the body has `memberRole`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /members/{memberId}
same-as: POST /members/deleteMember

Deletes the Member `memberId`. No body.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.
