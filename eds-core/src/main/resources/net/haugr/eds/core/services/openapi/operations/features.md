## GET /features
permission: FETCH_CIRCLE
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.

Returns the features which the stone offers, each with its declaration. Every Member
may read them, so a client knows what it can show.

A feature is something a Circle can have beside its files: a chat, a calendar. EDS
doesn't know what a feature is for. A feature is a **declaration**: its name, the kinds
of its entries, and for each kind rules from a fixed set. EDS stores the entries
and enforces the rules; what an entry holds is known to the clients only, and a
client may encrypt it before EDS gets it.

A declaration is a JSON object:

```json
{
  "feature": "calendar",
  "version": 1,
  "signals": 2000,
  "kinds": {
    "event": {
      "add": "WRITE",
      "change": ["adders"],
      "remove": ["author", "administrators"],
      "close": [],
      "keep": "all",
      "about": null,
      "size": 65536
    }
  }
}
```

| Field | Required | Notes |
|---|---|---|
| `feature` | yes | The name: a lower case letter, then lower case letters, digits or hyphens, at most 32 characters. The folder of the feature in a Circle is a dot and this name, e.g. `.calendar`. |
| `version` | yes | A number from 1, for the clients. |
| `signals` | no | The least milliseconds between two signals of one connection. Without it, the feature has no signals. |
| `kinds` | yes | 1 to 32 kinds of entries, each named like a feature, with its rules. |

The rules of a kind:

| Rule | Required | Notes |
|---|---|---|
| `add` | yes | The trust level needed to add an entry: `READ`, `WRITE` or `ADMIN`. |
| `change` | no | Who may change an entry: any of `author`, `adders` (everyone who may add) and `administrators` (of the Circle). Empty or absent: nobody. |
| `remove` | no | Who may remove an entry; as `change`. |
| `close` | no | Who may close an entry and open it again; as `change`. |
| `keep` | no | What is kept: `all` (the default); `member`, one entry per Member, where a new one replaces the old; `member-about`, one per Member about each entry; `age:<seconds>`, entries up to an age; `count:<number>`, the newest up to a number. |
| `about` | no | The kind of entry which an entry of this kind is about. Such an entry names the other when it is added, and goes when the other is removed. |
| `size` | yes | The largest content of an entry in bytes, at most `eds.data.max.inline.size`. Larger things are files, which an entry can name. |

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /features/{feature}
permission: FETCH_CIRCLE
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.
- VERIFICATION_WARNING: The declaration is missing, isn't a valid declaration, or names another feature than the path.

System Administrator only. Adds a feature to those the stone offers, or replaces its
declaration; the body has the `declaration`, a JSON object as text, see
`GET /features`. A replaced declaration counts for what is written afterwards: entries
of a kind which is no longer declared can still be read, but not changed.

From then on the name of the feature's folder is reserved in the root folder of every
Circle.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /features/{feature}
permission: FETCH_CIRCLE
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.
- IDENTIFICATION_WARNING: The stone doesn't offer the feature.
- ILLEGAL_ACTION: A Circle has the feature, switched on or off.

System Administrator only. Removes a feature from those the stone offers. A feature which
a Circle has can't be removed; it is removed from the Circles first.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /circles/{circleId}/features
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle.

Returns the features of the Circle with their state: `ON`, `READONLY` or `OFF`.
Requires the trust level `READ` in the Circle.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}/features/{feature}
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't an administrator of the Circle.
- IDENTIFICATION_WARNING: The stone doesn't offer the feature.
- VERIFICATION_WARNING: The state is missing.
- ILLEGAL_ACTION: The Circle has something with the name of the feature's folder already.
- INTEGRITY_ERROR: The Circle has no root folder (damaged data structure).

Requires the trust level `ADMIN` in the Circle. Gives the feature the `state` of the
body in the Circle:

| State | Meaning |
|---|---|
| `ON` | The feature is in use. The first time, its folder is made: a hidden folder of the Circle's root folder. |
| `READONLY` | The entries can be read, nothing is written: an archive. |
| `OFF` | The entries are kept and hidden: nothing is read, written or announced, also not by the data requests. |

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /circles/{circleId}/features/{feature}
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't an administrator of the Circle.
- IDENTIFICATION_WARNING: The Circle doesn't have the feature.

Requires the trust level `ADMIN` in the Circle. Removes the feature from the Circle,
with all its entries and its folder. This is the explicit step which switching off
isn't.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /circles/{circleId}/features/{feature}/entries
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle, or `about` isn't an entry of it.
- VERIFICATION_WARNING: The limit isn't from 1 to 200.

Returns a page of the entries of the feature in the Circle, the newest first.
Requires the trust level `READ` in the Circle.

| Query | Notes |
|---|---|
| `kind` | Only entries of this kind. |
| `about` | Only entries about the entry with this ID. |
| `name` | Only entries whose name starts with this text, taken as it is. |
| `after`, `before` | Only entries after, or before, this position. |
| `oldestFirst` | `true` for the oldest first. |
| `limit` | The most entries to return, 1 to 200; 50 without it. |
| `content` | `false` leaves the content of the entries out. |

An entry carries what EDS notes about it: its ID, kind, name, the entry it is about,
its author, when it was added and last changed, whether it is closed, and its
position, its place in the order of the feature in the Circle. The author isn't
readable in the database: it is sealed with a key derived from the Circle's key.
The `content` is Base64, and what it means is the clients' business.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /circles/{circleId}/features/{feature}/entries
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle, or hasn't the trust level which the kind asks for.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle, `about` isn't an entry of it, or the name is used already.
- VERIFICATION_WARNING: The kind isn't declared, the content is missing or not Base64, the name is too long, or `about` doesn't fit the kind.
- CONTENT_TOO_LARGE: The content is larger than the kind allows.
- ILLEGAL_ACTION: The feature is read-only in the Circle, or the entry would be about a closed one.
- INSUFFICIENT_STORAGE: The content would exceed `eds.data.max.total.size`, the ceiling for all stored content.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `kind` | yes | The kind of the entry. |
| `content` | yes | The content, Base64-encoded; it may be empty. |
| `about` | by kind | ID of the entry which the new one is about, for the kinds which are about another. |
| `name` | no | A name, up to 75 characters, unique in the feature of the Circle. Without one, EDS makes one. |

Adds an entry and returns it. For a kind which keeps one entry per Member, or one
per Member about each entry, the new entry replaces the Member's old one: it keeps
its ID and gets a new position. For a kind which keeps entries up to an age or a
number, the ones beyond it are removed.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /circles/{circleId}/features/{feature}/count
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle.

Returns the number of entries in `count`, of a `kind` and `after` a position if
given. This is how a client finds what a Member hasn't seen yet. Requires the trust
level `READ` in the Circle.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## GET /circles/{circleId}/features/{feature}/entries/{entryId}
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle, or the entry isn't one of it.

Returns one entry with its content. Requires the trust level `READ` in the Circle.
An entry is a Data Object too, so `GET /data/{entryId}/content` reads its content
as well.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}/features/{feature}/entries/{entryId}
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle, or isn't among those who may change an entry of the kind.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle, the entry isn't one of it, or the name is used already.
- VERIFICATION_WARNING: Neither content nor name is given, the content isn't Base64, the name is too long, or the kind is no longer declared.
- CONTENT_TOO_LARGE: The content is larger than the kind allows.
- ILLEGAL_ACTION: Nobody may change an entry of the kind, the feature is read-only in the Circle, or the entry, or one it is about, is closed.

Changes the `content` of an entry, its `name`, or both; what isn't given stays. Who
may is a rule of the kind. EDS notes the time of the change, and the entry keeps its
author and its position.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /circles/{circleId}/features/{feature}/entries/{entryId}
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle, or isn't among those who may remove an entry of the kind.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle, or the entry isn't one of it.
- ILLEGAL_ACTION: Nobody may remove an entry of the kind, the feature is read-only in the Circle, or the entry, or one it is about, is closed.

Removes an entry, and with it the entries which are about it. Returns what was
removed, without content, the entry itself last. Who may is a rule of the kind.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /circles/{circleId}/features/{feature}/entries/{entryId}/closed
permission: FETCH_DATA
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't a Trustee of the Circle, or isn't among those who may close an entry of the kind.
- IDENTIFICATION_WARNING: The feature isn't switched on in the Circle, or the entry isn't one of it.
- VERIFICATION_WARNING: `closed` is missing.
- ILLEGAL_ACTION: Nobody may close an entry of the kind, the feature is read-only in the Circle, or the entry is about a closed one.

Closes an entry with `closed` true, or opens it again with false. A closed entry
can't be changed or removed, nothing new can be about it, and what is about it can't
be changed or removed either: a plan which is settled stays as it is. Who may is a
rule of the kind.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.
