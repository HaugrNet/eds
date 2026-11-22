## POST /data/addData
permission: PROCESS_DATA
- IDENTIFICATION_WARNING: The name is already used in the folder, or `folderId` isn't a folder.
- CONTENT_TOO_LARGE: `data` is larger than `eds.data.max.inline.size`; use the content upload.
- INTEGRITY_WARNING: The Data Type doesn't exist.
- INSUFFICIENT_STORAGE: The content would exceed `eds.data.max.total.size`, the ceiling for all stored content.
- INTEGRITY_ERROR: The Circle has no root folder (damaged data structure).

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | yes | The Circle to add to. |
| `dataName` | yes | Up to 75 characters, unique within the folder. |
| `folderId` | no | Folder to add to; default is the Circle's root folder. |
| `typeName` | no | Data Type; default `data`. Use `folder` to create a folder. |
| `mediaType` | no | Media type of the content (`type/subtype`), used when serving it from the content download. |
| `data` | no | The content, Base64-encoded, up to `eds.data.max.inline.size` (default 10 MiB). A Data Object may have no content; larger content is sent with `PUT /data/{dataId}/content`. |

Requires the trust level `WRITE` in the Circle. The content is encrypted with the
Circle's key. Returns the new ID in `dataId`.

## POST /data/updateData
permission: PROCESS_DATA
- IDENTIFICATION_WARNING: The Data Object doesn't exist, or the new name is already used in the target folder.
- CONTENT_TOO_LARGE: `data` is larger than `eds.data.max.inline.size`; use the content upload.
- INTEGRITY_WARNING: The target folder doesn't exist.
- ILLEGAL_ACTION: The target folder is in another Circle, or a folder is moved.
- INSUFFICIENT_STORAGE: The new content would exceed `eds.data.max.total.size`; the old content doesn't count.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `dataId` | yes | The Data Object to update. |
| `dataName` | no | New name, up to 75 characters. |
| `folderId` | no | Folder to move it to, within the same Circle. Folders can't be moved. |
| `mediaType` | no | New media type of the content (`type/subtype`). |
| `data` | no | New content, Base64-encoded, up to `eds.data.max.inline.size`; replaces the old content. |

Requires the trust level `WRITE` in the Circle.

## POST /data/copyData
permission: PROCESS_DATA
- IDENTIFICATION_WARNING: The Data Object doesn't exist, or the requester isn't a Trustee of the target Circle.
- AUTHORIZATION_WARNING: The requester lacks `WRITE` in the target Circle.
- ILLEGAL_ACTION: The Data Object is a folder.
- INSUFFICIENT_STORAGE: The copy would exceed `eds.data.max.total.size`.
- INTEGRITY_ERROR: The stored content of the Data Object is damaged.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `dataId` | yes | The Data Object to copy. |
| `targetCircleId` | yes | The Circle to copy it to. |
| `targetFolderId` | no | Folder in the target Circle; default is its root folder. |

Requires `WRITE` in both Circles. The copy is encrypted with the target Circle's
key, re-encrypted chunk by chunk while it is streamed from the source, so large
content is never held in memory. Returns the copy's ID in `dataId`.

## POST /data/moveData
permission: PROCESS_DATA
- IDENTIFICATION_WARNING: The Data Object doesn't exist, or the requester isn't a Trustee of the target Circle.
- AUTHORIZATION_WARNING: The requester lacks `WRITE` in the target Circle.
- ILLEGAL_ACTION: The Data Object is a folder.
- INTEGRITY_ERROR: The stored content of the Data Object is damaged.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `dataId` | yes | The Data Object to move. |
| `targetCircleId` | yes | The Circle to move it to. |
| `targetFolderId` | no | Folder in the target Circle; default is its root folder. |

Requires `WRITE` in both Circles. Works like a copy followed by a delete, so the
moved Data Object gets a **new ID**, returned in `dataId`. To move within a Circle,
use `updateData` with `folderId`. A move doesn't count against
`eds.data.max.total.size`, as it adds nothing.

## POST /data/deleteData
permission: PROCESS_DATA
- IDENTIFICATION_WARNING: The Data Object doesn't exist.
- INTEGRITY_WARNING: The folder isn't empty.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `dataId` | yes | The Data Object or folder to delete. |

Requires the trust level `WRITE` in the Circle. Folders must be empty first.

## POST /data/fetchData
permission: FETCH_DATA
- IDENTIFICATION_WARNING: Nothing was found for the given ID or name.
- INTEGRITY_ERROR: The stored content failed its integrity check; the Data Object is marked as failed.
- CONTENT_TOO_LARGE: The content is larger than `eds.data.max.inline.size`; the response has the metadata, but no `data`. Download it with `GET /data/{dataId}/content`.

**Fields** (one of `circleId`, `dataId` or `dataName` is required):

| Field | Notes |
|---|---|
| `circleId` | List the Circle's root folder. |
| `dataId` | Fetch this Data Object (or list this folder). |
| `dataName` | Fetch the Data Object with this name, if no `dataId` is given. |
| `pageNumber` | Page of a folder listing; default 1. |
| `pageSize` | 1 to 100, default 100. |

Requires the trust level `READ` in the Circle. For a folder, returns a page of the
metadata of its content, newest first, with `records` set to the total count. For
any other Data Object, returns its metadata and the decrypted content in `data`.

## PUT /data/{dataId}/content
permission: PROCESS_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- IDENTIFICATION_WARNING: The Data Object doesn't exist or isn't visible to the Member.
- ILLEGAL_ACTION: The Data Object is a folder, an entry of a feature or a thumbnail.
- PRECONDITION_FAILED: `If-Match` doesn't match the ETag of the stored content.
- CONTENT_TOO_LARGE: The content is larger than `eds.stream.max.object.size`. With `Content-Length`, refused before the body is read; without, when the body passes the limit.
- INSUFFICIENT_STORAGE: The content would exceed `eds.data.max.total.size`; the content it replaces doesn't count. With `Content-Length`, checked before the body is read; without, while and after it is read.

**Headers:**

| Header | Required | Notes |
|---|---|---|
| `Authorization` | yes | `Bearer <session token>`, the token from `login`, Base64-encoded. Credentials can't be used, the body is the raw content. |
| `Content-Length` | no | Length of the content. Give it whenever it is known: the limits are then checked before the body is read, and the content goes to the database as it arrives. Without it (chunked transfer, e.g. from a pipe) the body is read to its end. |
| `Content-Type` | yes | `application/octet-stream`. The media type for serving is part of the Data Object, not taken from here. |
| `If-Match` | no | Only replace the content if its current ETag matches; `*` for any existing content. |

Uploads the content of a Data Object as a raw stream, replacing any existing
content. Register the Data Object first with `addData` (without `data`). The content
is encrypted chunk by chunk while it arrives (PLAN.md, Crypto §4), so the server doesn't
hold it in memory. Content without `Content-Length` is encrypted the same way, but
PostgreSQL needs the length of a value up front, so its driver first spools the
encrypted content to a temporary file on the server, up to the size limit; the
plaintext is never written to disk. The upload runs in one transaction with its own timeout
(`eds.stream.transaction.timeout.seconds`): a failed or interrupted upload is rolled
back, and the previous content stays. Requires the trust level `WRITE` in the
Circle. New content removes the thumbnail of the Data Object, as it no longer
matches.

Unlike the JSON operations, this endpoint answers with the HTTP status of the
`returnCode` (e.g. 404, 412, 413), and the JSON body as usual. On success, the
`ETag` header and the `etag` field hold the ETag of the new content.

## GET /data/{dataId}/content
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header and the download token are missing, the session is unknown or has expired, or the download token is invalid, for another Data Object, or has expired.
- IDENTIFICATION_WARNING: The Data Object doesn't exist, isn't visible to the Member, or has no content.
- ILLEGAL_ACTION: The Data Object is a folder.
- RANGE_NOT_SATISFIABLE: The `Range` is outside the content; `Content-Range: bytes */<size>` gives the size.
- INTEGRITY_ERROR: The stored content is damaged.

**Headers:**

| Header | Required | Notes |
|---|---|---|
| `Authorization` | yes, or `token` | `Bearer <session token>`, as for the upload. Without it, the query parameter `token` must hold a download token for this Data Object (`POST /data/{dataId}/token`); the header wins if both are given. |
| `Range` | no | One byte range: `bytes=first-last`, `bytes=first-` or `bytes=-suffix`. Multiple ranges, or an invalid one, give the full content. |
| `If-Range` | no | An ETag: serve the `Range` only if the content still has it, otherwise the full content. Stops a player from mixing two uploads. |

Downloads the content of a Data Object as a raw stream: `200` with the full
content, or `206 Partial Content` with `Content-Range` for a range. Only the chunks
covering the range are read and decrypted (PLAN.md, Crypto §4), and a chunk is never sent
before its authentication tag is verified, so seeking in large media is cheap.
Requires the trust level `READ` in the Circle. Every response has `Accept-Ranges:
bytes`, `Content-Length`, `ETag`, `Cache-Control: private, no-store`,
`X-Content-Type-Options: nosniff`, `Content-Security-Policy: default-src 'none'; ...;
sandbox` (opened directly, the content runs no scripts and has no origin),
`Cross-Origin-Resource-Policy: same-origin` and `Referrer-Policy: no-referrer`.

The content is untrusted. Only the media types `image/jpeg`, `image/png`,
`image/webp`, `video/mp4` and `video/webm` (from the Data Object's `mediaType`, set
with `addData`/`updateData`) are served with their type and `Content-Disposition:
inline`; everything else, SVG and HTML included, is served as
`application/octet-stream` with `Content-Disposition: attachment`. The type is never
taken from the content.

The content is read in one transaction (`REPEATABLE READ` on PostgreSQL), so an
upload which commits during a download doesn't mix two contents. Errors found before
the response starts are answered with their HTTP status and a JSON body. If a chunk
fails its check during the transfer, the connection is aborted and the Data Object is
marked as failed (`sanityStatus`).

## HEAD /data/{dataId}/content
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header and the download token are missing, the session is unknown or has expired, or the download token is invalid or has expired.
- IDENTIFICATION_WARNING: The Data Object doesn't exist, isn't visible to the Member, or has no content.
- ILLEGAL_ACTION: The Data Object is a folder.
- RANGE_NOT_SATISFIABLE: The `Range` is outside the content.
- INTEGRITY_ERROR: The stored content is damaged.

**Headers:** as for `GET /data/{dataId}/content`.

The same headers as the download, without the body, e.g. for a player to learn the
size and the ETag.

## POST /data/{dataId}/token
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- IDENTIFICATION_WARNING: The Data Object doesn't exist or isn't visible to the Member.
- ILLEGAL_ACTION: The Data Object is a folder.

**Headers:**

| Header | Required | Notes |
|---|---|---|
| `Authorization` | yes | `Bearer <session token>`. |

Creates a download token for the content of one Data Object, for clients which can't
send the `Authorization` header: the `<img>` and `<video>` elements of a browser.
Append it to the content URL, `GET /data/{dataId}/content?token=<token>`; `Range`
requests work as usual, so a player can seek.

The token is the session token and an expiry, sealed with the MasterKey wrap key
(AES-256-GCM, PLAN.md, Crypto §2a) with the ID of the Data Object in the AAD. Nothing is
stored, and only the server can open it. It reads only this content, and ends at
`expires` (`eds.download.token.lifetime.minutes`, default 60, at most the end of the
session), at logout, or when the MasterKey changes. It is part of a URL, so it can
end up in a browser history or a proxy log; the content responses send
`Referrer-Policy: no-referrer`. Requires the trust level `READ` in the Circle.

## PUT /data/{dataId}/thumbnail
permission: PROCESS_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- IDENTIFICATION_WARNING: The Data Object doesn't exist or isn't visible to the Member.
- ILLEGAL_ACTION: The Data Object is a folder, an entry of a feature or a thumbnail.
- VERIFICATION_WARNING: The image is empty, or its type isn't JPEG, PNG or WebP.
- CONTENT_TOO_LARGE: The image is larger than 262144 bytes.
- INSUFFICIENT_STORAGE: The thumbnail would exceed `eds.data.max.total.size`.

**Headers:**

| Header | Required | Notes |
|---|---|---|
| `Authorization` | yes | `Bearer <session token>`. |
| `Content-Type` | yes | `image/jpeg`, `image/png` or `image/webp`; served with this type. |

Stores a thumbnail of the content. The client makes it (the PWA draws an image, or a
frame of a video, on a canvas); the server stores it encrypted with the Circle's key
like any content and never reads it. An existing thumbnail is replaced. Returns the
thumbnail's ID in `dataId`; the Metadata of the Data Object has it as `thumbnailId`,
and the thumbnail is downloaded like any content (`GET /data/{thumbnailId}/content`,
with a download token in a browser).

A thumbnail is a Data Object of the type `thumbnail`, whose parent is the Data Object
it shows, so listings never show it. Only this endpoint writes it: the data
operations refuse to add, update, move, copy or upload into one, but may delete it.
It goes away with its Data Object, when new content is stored, and when the Data
Object is moved to another Circle; copies don't keep it. Requires the trust level
`WRITE` in the Circle.

## GET /data
same-as: POST /data/fetchData

Lists the root folder of the Circle given by the query parameter `circleId`, a page at a time (`pageNumber`, `pageSize`). Each entry has the `size` of its content.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /data/{dataId}
permission: FETCH_DATA
- IDENTIFICATION_WARNING: The Data Object doesn't exist, or isn't visible to the Member.

Returns the metadata of the Data Object `dataId`, with the `size` of its content, but never the content itself: download it with `GET /data/{dataId}/content`. The content isn't read, so this is cheap for any size. For a folder, returns a page of its content, as `fetchData`. Requires being a Trustee of the Circle.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /data
same-as: POST /data/addData

Adds a Data Object or a folder. The body has the fields of `addData`; large content is uploaded afterwards with `PUT /data/{dataId}/content`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /data/{dataId}
same-as: POST /data/updateData

Updates the Data Object `dataId`: name, folder, media type, or (small) content.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /data/{dataId}
same-as: POST /data/deleteData

Deletes the Data Object `dataId`. No body.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /data/{dataId}/copy
same-as: POST /data/copyData

Copies the Data Object `dataId`; the body has `targetCircleId` and optionally `targetFolderId`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /data/{dataId}/move
same-as: POST /data/moveData

Moves the Data Object `dataId`; the body has `targetCircleId` and optionally `targetFolderId`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.
