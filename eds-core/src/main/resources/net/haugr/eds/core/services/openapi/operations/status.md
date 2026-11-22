## GET /status
permission: SETTING
- AUTHENTICATION_WARNING: The Authorization header is missing, or it holds neither a status key nor a session which is known and hasn't expired.
- AUTHORIZATION_WARNING: The session isn't the System Administrator's.

Returns the status of the stone: what it tells about itself to those who look after
it. The `Authorization: Bearer` header carries either a **status key** (see
`POST /status/keys`) or the session of the System Administrator. A status key reads
the status and nothing else, also while the stone is locked and waits for its
MasterKey; a session needs the stone to be awake.

Only general numbers are told: no names of Members, Circles or files, and nothing
about a single one of them.

| Query | Notes |
|---|---|
| `since` | Only samples of the load after this `sequence`: a client which asks often names the last sample it has and gets what is new. |
| `history` | `true` adds the load of the last 24 hours (`day`), and all kept samples of the inventory instead of the latest. |

The answer has the `name` of the stone (the setting `eds.stone.name`), its `version`,
whether it is `awake`, its `time` and when it `started`, the kind of `database`,
whether that is `persistent`, and a `databaseMark`: stones on the same database
server have the same mark, without the address being told. Then three series of
samples, the oldest first; each sample has a `sequence`, a `time` and `values`, numbers
by their names. A client shows the numbers it knows and passes over the others.

**`load`**: how busy the stone is. A sample is taken every `interval` seconds (the
setting `eds.status.load.interval.seconds`, default 5; 0 switches the gathering off),
and the last 720 are kept. Gathering reads counters and the Java runtime, so it is
cheap also every second; a request for the status gathers nothing.

| Value | Meaning |
|---|---|
| `requests`, `failed` | Requests handled since the last sample, and those of them which were refused or failed. |
| `bytesIn`, `bytesOut` | Bytes received and sent with them, as far as the headers tell. |
| `requestMillis`, `requestMillisMax` | How long a request took: the average, and the longest. |
| `databaseMillis` | How long a trivial query to the database took, measured at most every 10 seconds. EDS can't read the load of the database; this rises with it. |
| `accounts` | Accounts with an authenticated request since the last sample. |
| `connections` | Open WebSocket connections. |
| `processShare` | The share of all processors which the stone's own process used since the last sample, 0 to 1. Left out where the platform doesn't tell. |
| `systemLoad`, `processors` | The load average of the host, and its processors. Two stones on one host tell the same; `processShare` tells them apart. |
| `memoryUsed`, `memoryMax` | Memory of the Java runtime, in bytes. |
| `threads` | Live threads. |

**`day`**: the load of the last 24 hours in steps of five minutes, the step still being
filled as the last. Counts are summed over the step; `requestMillis`, `databaseMillis`,
`processShare` and `systemLoad` are averages, with the peak as `<name>Max`; `accounts`,
`connections`, `memoryUsed` and `threads` are the highest of the step; `samples` is the
number of samples in it.

**`inventory`**: what the stone holds. Each number is a query, so a sample is taken
every `interval` seconds only (the setting `eds.status.inventory.interval.minutes`,
default 60), and the last 720 are kept.

| Value | Meaning |
|---|---|
| `storageUsed`, `storageCeiling` | The stored, encrypted bytes, and the ceiling for them (`eds.data.max.total.size`). |
| `accounts`, `circles` | Accounts without the pending invitations, and Circles. |
| `files`, `entries` | Files, and entries of features. |
| `sessions` | Sessions which haven't expired. |
| `rotationsDue`, `rotationsRunning`, `rotationObjects` | Circles whose Circle Secret is due to be rotated and waits for a Trustee's request, those being rotated, and the Data Objects they still have to re-encrypt. |

All series are kept in memory and start anew with the stone.

A browser page from another address may call this, and the rest of the API, if the
setting `eds.cors.value` names its origin (several are separated by commas).

## GET /status/keys
permission: SETTING
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.

System Administrator only. Returns the labels of the status keys of the stone in
`keys`. The keys themselves aren't kept, only their hashes.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## POST /status/keys
permission: SETTING
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.
- VERIFICATION_WARNING: The label is missing or longer than 75 characters.
- IDENTIFICATION_WARNING: A status key with the label exists already.

System Administrator only. Creates a status key with the `label` of the body, and
returns it once in `key`: 256 random bits as Base64 (URL-safe). EDS keeps only its
SHA-512 hash. Someone who looks after several stones gives the key to a client which
shows them together; they needn't log in to each stone, and a key which gets into
the wrong hands gives away numbers and nothing else. It stays valid when the
MasterKey changes.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /status/keys/{label}
permission: SETTING
- AUTHENTICATION_WARNING: The Authorization header is missing, or the session is unknown or has expired.
- AUTHORIZATION_WARNING: The requesting Member isn't the System Administrator.
- IDENTIFICATION_WARNING: There is no status key with the label.

System Administrator only. Withdraws the status key with the label; it is refused
from then on.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.
