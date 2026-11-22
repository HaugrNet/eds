## POST /trustees/addTrustee
permission: PROCESS_TRUSTEE
- ILLEGAL_ACTION: The requesting Member isn't a Trustee of the Circle (e.g. the System Administrator without membership).
- IDENTIFICATION_WARNING: The Member doesn't exist, or is already a Trustee of the Circle.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle. |
| `memberId` | yes | The Member to add. |
| `trustLevel` | yes | `READ`, `WRITE` or `ADMIN`. |
| `secret` | no | The new Trustee's secret in the Circle, see `alterTrustee`. |

Requires the trust level `ADMIN` in the Circle. Gives the Member access to the
Circle's key, so they can read (and, with `WRITE` or `ADMIN`, write) its data.

## POST /trustees/alterTrustee
permission: PROCESS_TRUSTEE
- IDENTIFICATION_WARNING: The Member isn't a Trustee of the Circle.
- AUTHORIZATION_WARNING: Only a secret is given, and the requesting Member is neither the Trustee nor an administrator of the Circle, or isn't a Trustee of it at all.
- ILLEGAL_ACTION: A secret is given with a trust level, and the requesting Member isn't a Trustee of the Circle (e.g. the System Administrator without membership).

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle. |
| `memberId` | yes | The Trustee's Member ID. |
| `trustLevel` | without `secret` | The new trust level: `READ`, `WRITE` or `ADMIN`. |
| `secret` | without `trustLevel` | The Trustee's secret in the Circle, up to 65536 characters. An empty value removes it. |

Changing the trust level requires the trust level `ADMIN` in the Circle.

The secret is text of a client which EDS stores without looking inside, for example
a key of the Circle which a client wrapped for this Member, so that EDS itself can't
read what the clients encrypt with it. With only a `secret`, every Trustee may set
their own, whatever their trust level, and an administrator of the Circle that of
any Trustee. EDS seals it with a key derived from the Circle's key, bound to the
Circle and the Member; it goes with the Trustee. See `fetchTrustees` for who reads it.

## POST /trustees/setSecrets
permission: PROCESS_TRUSTEE
- ILLEGAL_ACTION: The requesting Member isn't a Trustee of the Circle (e.g. the System Administrator without membership).
- IDENTIFICATION_WARNING: A Member named in `secrets` isn't a Trustee of the Circle; nothing was changed.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle. |
| `secrets` | without `circleKey` | A list of `memberId` and `secret`, see `alterTrustee`. An empty `secret` removes it. |
| `circleKey` | without `secrets` | The external key to store with the Circle, see `updateCircle`. |

Requires the trust level `ADMIN` in the Circle. Sets the secrets of several Trustees,
and the Circle's external key if given, in one step: either all of it is stored or
nothing. Clients which keep a key of their own in the secrets change it this way,
e.g. after a Trustee was removed, so the Circle is never left with some Trustees on
the old key and some on the new. Trustees not named keep their secret.

## POST /trustees/removeTrustee
permission: FETCH_TRUSTEE
- IDENTIFICATION_WARNING: The Member isn't a Trustee of the Circle.
- AUTHORIZATION_WARNING: Another Member is removed, and the requesting Member isn't an administrator of the Circle.
- ILLEGAL_ACTION: The Member is the last Trustee of the Circle.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle. |
| `memberId` | yes | The Trustee's Member ID. |

Removes another Member from the Circle, which requires the trust level `ADMIN` in
the Circle (or the System Administrator), or removes the requesting Member, i.e.
leaves the Circle, which every Trustee may. The Member loses access to the Circle's
data at once; their open connections of the socket get `LEFT`. Even the last administrator may
leave; the System Administrator can then make another Trustee an administrator.
Afterwards the Circle Secret is rotated, so what the Member may have kept of it is of
no use (see `rotateCircle`).

The last Trustee of a Circle can neither leave nor be removed, not even by the System
Administrator: nobody could unlock the Circle's data afterwards. The Circle is deleted
instead.

## POST /trustees/fetchTrustees
permission: FETCH_TRUSTEE
- IDENTIFICATION_WARNING: No Trustees match: the Circle has none or doesn't exist, the Member isn't a Trustee of the Circle, or a non-administrator asks about another Member.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | no | List the Trustees of this Circle (requires `READ` in it). |
| `memberId` | no | List this Member's Trustee records. Other Members than the requester: System Administrator only. |

With both fields, returns the one Trustee record of that Member in that Circle.
With neither, returns the requester's own Trustee records.

A Trustee's `secret` (see `alterTrustee`) is returned with the requester's own
records, and with all records of a Circle the requester administers. Nobody else
gets it, also not the System Administrator.
