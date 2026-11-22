EDS, Encrypted Data Share, stores data encrypted and shares it between Members who
belong to the same Circle. All encryption happens in EDS: clients send and receive
plain data over the API, and EDS stores it encrypted, so a copy of the database alone
is not readable.

All paths in this specification are relative to the API root, `/api/v1` (e.g.
`http://localhost:8080/api/v1/version`).

## Concepts

| Term | Meaning |
|------|---------|
| **Member** | An account in EDS, identified by an account name. Every Member has their own key pair, protected by their credentials. |
| **System Administrator** | The built-in Member `admin` (role `ADMIN`). Manages Members, Circles, settings and the MasterKey, but can't read Circle data without being a Trustee. |
| **Circle** | A group of Members sharing data. Each Circle has its own key; data stored in a Circle is encrypted with it. |
| **Trustee** | A Member's membership of a Circle, with a trust level: `READ` (read data), `WRITE` (also add, change and delete data) or `ADMIN` (also manage the Circle and its Trustees). |
| **Data Object** | A named piece of data stored in a Circle. It has metadata (name, type, folder) and, optionally, encrypted content. |
| **Folder** | A Data Object of type `folder`, used to structure the data within a Circle. Every Circle has a root folder. Entries whose names start with a dot are hidden: folder listings skip them, unless `includeHidden` is set. |
| **Data Type** | The type of a Data Object (`data`, `folder`, `thumbnail` and `entry` by default), so clients know how to interpret it. |
| **Feature** | Something a Circle can have beside its files, such as a chat or a calendar: a declaration, whose entries EDS stores and guards without knowing what they are (see Features and the socket). |
| **Session** | A time-limited login with a session token from EDS (see Authentication), so clients don't have to keep the passphrase. |
| **MasterKey** | A system-wide key, loaded from an external source when EDS starts, protecting the stored keys. Removing the source makes the stored data unreadable (kill-switch). |

## Authentication

The operations of the JSON API (`POST /<area>/<operation>`) carry the credentials in
their JSON body:

- `accountName`: the Member's account name.
- `credential`: the credential as Base64 of its bytes (the passphrase in UTF-8, or the
  session key).
- `credentialType`: `PASSPHRASE` (default) or `SESSION`. With `SESSION`, the
  `accountName` is omitted and the `credential` holds the session key.

To start a session, the client calls the login operation with the passphrase; EDS
answers with a random session token in `session` (Base64), and stores only a keyed
hash of it. Until the session expires or the client logs out, the token can be used
as the credential.

The content endpoints (`/data/{dataId}/content`) and the ID-based resource API (see
below) take only the session, as the HTTP header `Authorization: Bearer <session>`
(security scheme `session`); credentials in their body are replaced by it.

Credentials are checked on every request. A failed check returns the return code
`AUTHENTICATION_WARNING` (403); a Member without the required role or trust level gets
`AUTHORIZATION_WARNING` (401).

## Responses and return codes

EDS treats every request as an application call, not as an HTTP resource operation:
every operation of the JSON API that reaches EDS answers **HTTP 200**, and the outcome
is in the response body. (The content endpoints and the resource API answer with the
HTTP status of the return code instead, see below.) All response objects share two fields:

- `returnCode`: the outcome. `200` is success; `4xx` values are warnings, i.e.
  problems the caller can correct (invalid input, missing permission, wrong
  credentials); `5xx` values are errors inside EDS that need an administrator.
- `returnMessage`: a human-readable explanation, e.g. which request fields are
  invalid.

The response also has an `ok` flag, which is `true` when `returnCode` is `200`. The
complete list of return codes is in the `EDSResponse` schema (`x-eds-return-codes`),
and each operation lists the codes it can return (`x-eds-return-codes` on the
operation) and the trust level it requires (`x-eds-trust-level`).

Only requests that don't reach EDS get another HTTP status: malformed JSON or invalid
field values (400), a missing request body (400, with a constraint-violation body),
an unknown path (404), the wrong HTTP method (405) or the wrong content type (415).

## ID-based resource API

Next to the JSON API, the main objects are also available as resources, addressed by
their ID in the path:

| Resource | Operations |
|---|---|
| `/members`, `/members/{memberId}` | `GET`, `POST`; `GET`, `PUT` (role), `DELETE` |
| `/circles`, `/circles/{circleId}` | `GET`, `POST`; `GET`, `PUT`, `DELETE` |
| `/circles/{circleId}/trustees`, `.../trustees/{memberId}` | `GET`, `POST`; `PUT` (trust level), `DELETE` |
| `/data?circleId=…`, `/data/{dataId}` | `GET` (root folder), `POST`; `GET` (metadata or folder), `PUT`, `DELETE` |
| `/data/{dataId}/copy`, `/data/{dataId}/move` | `POST` |
| `/dataTypes`, `/dataTypes/{typeName}` | `GET`; `PUT`, `DELETE` |

Each resource operation runs the same business logic as its JSON counterpart (named
in its description) and takes the same request object as body, without credentials;
values from the path replace those of the body. Unlike the JSON API, they answer with
the HTTP status of the `returnCode` (as the content endpoints do), with the same JSON
response as body. `GET /data/{dataId}` never returns the content; it's downloaded
with `GET /data/{dataId}/content`.

## Features and the socket

A feature is something a Circle can have beside its files, such as a chat or a
calendar. EDS doesn't know what a feature is for: a feature is a declaration, with the
kinds of its entries and rules for each kind, and EDS stores the entries and
enforces the rules (see the Features operations). What an entry holds is the clients'
business.

Next to those operations, the Members of a Circle work with the entries over the
WebSocket `/socket` (e.g. `ws://localhost:8080/api/v1/socket`, `wss://` with TLS),
and are told there what changes. It isn't an operation of this specification, as
OpenAPI doesn't describe WebSockets; its frames are the schema `SocketFrame` (with
the `type` from `SocketFrameType`), with entries as the schema `Entry`. Each frame is
a JSON object in a text frame of at most 256 KiB; fields which don't apply are
omitted, and `ref`, chosen by the client, is returned in the answer to the frame.

1. The first frame must be `AUTH`, with the session as `token` (Base64), within 30
   seconds; EDS answers `READY` with the Member. Browsers can't send an
   `Authorization` header on a WebSocket, hence the frame.
2. `JOIN` with a `circleId` subscribes to the Circle (trust level `READ`); `JOINED`
   brings the `features` of the Circle with their state, and in `present` the IDs of
   the Members who are present in it. `LEAVE` ends it. A client may join all its
   Circles at once.
3. With the Circle joined, a client works with the entries of a `feature`, by the same
   rules as the Features operations: `ADD` (with `kind`, `content`, and optionally
   `name` and `about`), `CHANGE` (with `entryId`, and `content`, `name` or both),
   `REMOVE` (with `entryId`), `CLOSE` (with `entryId` and `closed`) and `GET` (with
   `entryId`) are answered with `DONE` and the `entries`; `LIST` (optionally with
   `kind`, `about`, `name`, `after`, `before`, `oldestFirst`, `limit` and `bare`) with
   `ENTRIES`; `COUNT` (optionally with `kind` and `after`) with `COUNTED`. A request
   which fails is answered with `ERROR`, the `returnCode` and the `returnMessage`.
   An entry whose content doesn't fit a frame is written with the Features operations.
4. What changes in a joined Circle is told to every connection which joined it, once
   it is stored: `ADDED`, `CHANGED` and `REMOVED` with the `entry`, whichever
   connection or operation changed it. The connection which asked for the change gets
   this too, and before its `DONE`. A long content, of more than 16384 characters as
   Base64, is left out of the announcement and read with `GET`; a removed entry comes
   without content. `FEATURE` with the `feature` tells that it was switched on, off or to
   read-only in the Circle, or removed from it.
5. `SIGNAL` with a `feature` and a `signal`, a text of at most 32768 characters, is passed
   on to the others which joined the Circle, with the `memberId` and `accountName`;
   "is typing" is one. With `to`, the ID of a Member, it is passed on only to the
   connections of that Member which joined the Circle; the sender may name themselves,
   to reach their other connections. It is neither stored nor answered. A feature must
   declare signals, with the least time between two of a connection (which may be 0);
   one which comes sooner is dropped.
6. `FILES` with a `circleId` and a `dataId` is sent to the connections which joined
   the Circle, after its files changed: a Data Object was added, uploaded, updated,
   moved, copied or deleted, or got a thumbnail. Clients reload the files through the
   API, so they don't need to poll.
7. A Member is present in a Circle while at least one of their connections has
   joined it. The others which joined are told with `PRESENT` when the first such
   connection comes, and with `ABSENT` when the last one goes: by `LEAVE`, by closing,
   or because the access ended; both carry the `circleId`, `memberId` and
   `accountName`. A connection which joins with `unseen` set to `true` doesn't count
   for this, and is told who is present all the same. Only who joined a Circle learns
   who is present in it, and nothing of it is stored.
8. Access ends promptly: when a Member leaves a Circle or is removed, their
   connections get `LEFT` for it with a return code; when a session ends, its
   connections get `ERROR` and are closed (1008). A connection whose client doesn't
   read, or floods, is closed (1013).

## Media in a browser

The `<img>` and `<video>` elements of a browser can't send the `Authorization`
header. For them, `POST /data/{dataId}/token` creates a short-lived download token for
the content of one Data Object, used as `GET /data/{dataId}/content?token=…`; `Range`
requests work as usual, so a player can seek. The token reads only that content and
ends at its expiry, at logout, or when the MasterKey changes.

Thumbnails are made by the client (the web app draws an image, or a frame of a video,
on a canvas) and stored with `PUT /data/{dataId}/thumbnail`, encrypted like any
content; EDS never reads them. The Metadata of the Data Object has the thumbnail's ID
as `thumbnailId`. Listings don't show thumbnails, and a thumbnail goes away with its
Data Object or when new content is stored.

The web app itself is served at the root of the stone (e.g.
`http://localhost:8080/`), beside the API.

## Conventions

- The JSON API takes and returns `application/json`, using `POST`; the resource API
  and the content endpoints use the HTTP methods above.
- IDs are UUIDs in their textual form, e.g. `5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a`.
- Names (accounts, Circles, Data Objects, Data Types) are at most 75 characters.
- Binary values (credentials, data, keys) are Base64-encoded strings.
- Timestamps are in UTC, as ISO-8601 without a time-zone suffix, e.g.
  `2026-11-01T21:12:28.510566` (format `local-date-time` in the schemas).
- Fields that don't apply are `null`; unknown fields in a request are ignored.
