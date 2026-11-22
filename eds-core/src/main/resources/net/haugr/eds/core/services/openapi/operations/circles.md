## POST /circles/createCircle
permission: PROCESS_CIRCLE
- IDENTIFICATION_WARNING: A Circle with the name already exists, or the given `memberId` doesn't exist.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleName` | yes | Up to 75 characters, unique. |
| `memberId` | no | System Administrator only: the Member who becomes the Circle's first administrator. Ignored for other Members. |
| `circleKey` | no | External key to store with the Circle. |
| `secret` | no | The secret of the Circle's first administrator in it, see `alterTrustee`. |

Any Member may create a Circle. The creator becomes its first administrator
(trust level `ADMIN`), unless the System Administrator names another Member in
`memberId`. Returns the new Circle's ID in `circleId`.

## POST /circles/updateCircle
permission: PROCESS_CIRCLE
- AUTHORIZATION_WARNING: The requesting Member is neither the Circle's administrator nor the System Administrator.
- IDENTIFICATION_WARNING: The Circle doesn't exist, or the new name is already used by another Circle.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle to update. |
| `circleName` | no | New name, up to 75 characters, unique. |
| `circleKey` | no | The external key to store. Omitting it keeps the stored key. |
| `avatar` | no | The Circle's avatar, the ID of a picture the clients know. An empty value removes it, omitting it keeps it. |

## POST /circles/deleteCircle
permission: PROCESS_CIRCLE
- AUTHORIZATION_WARNING: The requesting Member is neither the Circle's administrator nor the System Administrator.
- IDENTIFICATION_WARNING: The Circle doesn't exist.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle to delete. |

Deletes the Circle with its keys, Trustees and all its Data Objects. This can't be
undone.

## POST /circles/rotateCircle
permission: PROCESS_CIRCLE
- AUTHORIZATION_WARNING: The requesting Member is neither the Circle's administrator nor the System Administrator.
- IDENTIFICATION_WARNING: The Circle doesn't exist.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle whose Circle Secret is rotated. |

Has the Circle Secret rotated now: the rotation is set due
(`rotationDue` of the Circle), and starts in the background. The Circle gets a new
secret, wrapped for every Trustee, and its Data Objects are re-encrypted with it, one
by one; meanwhile everything can be read and written as usual, and the Circle's
`rotation` tells the progress. The stone can't read a Circle Secret by itself: with the
request of a Circle administrator the rotation starts at once, with that of the System
Administrator, who needn't be a Trustee, at the next request of a Trustee which reads
the Circle's secret. Pending invitations into the Circle are deleted, as they are
sealed with the old secret. The rotation is also due by itself: when a Trustee leaves
or is removed, and `eds.circle.secret.rotation.days` after the last one.

## POST /circles/fetchCircles
permission: FETCH_CIRCLE

**Fields:** only the authentication fields.

Returns all Circles to the System Administrator. Members get only the Circles they
are a Trustee of, unless `eds.show.all.circles` is true (default false), which shows
every Circle's name to every Member. The external `circleKey` is only filled in for Circles the
requester is a Trustee of. Each Circle tells when its Circle Secret was made
(`secretRotated`), when it is to be rotated next (`rotationDue`), and the rotation under
way (`rotation`), see `rotateCircle`.

## GET /circles
same-as: POST /circles/fetchCircles

Lists the Circles.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /circles/{circleId}
same-as: POST /circles/fetchCircles
- IDENTIFICATION_WARNING: The Circle doesn't exist, or the requesting Member may not see it.

Returns the Circle `circleId`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /circles
same-as: POST /circles/createCircle

Creates a Circle. The body has the fields of `createCircle`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}
same-as: POST /circles/updateCircle

Updates the Circle `circleId`; the body has `circleName` and/or `circleKey`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}/rotation
same-as: POST /circles/rotateCircle

Has the Circle Secret of the Circle `circleId` rotated now. No body.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /circles/{circleId}
same-as: POST /circles/deleteCircle

Deletes the Circle `circleId`. No body.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /circles/{circleId}/trustees
same-as: POST /trustees/fetchTrustees

Lists the Trustees of the Circle `circleId`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /circles/{circleId}/trustees
same-as: POST /trustees/addTrustee

Adds a Trustee to the Circle `circleId`; the body has `memberId`, `trustLevel` and optionally `secret`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}/trustees/{memberId}
same-as: POST /trustees/alterTrustee

Changes the trust level of the Trustee `memberId`, their secret, or both; the body has `trustLevel` and `secret`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}/secrets
same-as: POST /trustees/setSecrets

Sets the secrets of several Trustees of the Circle `circleId`, all or none; the body has `secrets` and `circleKey`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /circles/{circleId}/trustees/{memberId}
same-as: POST /trustees/removeTrustee

Removes the Trustee `memberId` from the Circle `circleId`. No body.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.
