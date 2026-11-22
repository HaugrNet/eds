## POST /masterKey
permission: MASTER_KEY
- AUTHENTICATION_WARNING: The account isn't `admin`, the System Administrator's credentials are wrong, or EDS is locked and the secret doesn't match the stored key check value.
- VERIFICATION_WARNING: Neither or both of `secret` and `url` are given, the URL is invalid, or a new secret (first setup or change) is too weak.
- NETWORK_ERROR: The secret can't be read from the URL.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `secret` | one of the two | The MasterKey secret, Base64-encoded. |
| `url` | one of the two | URL to read the secret from. |

Only the account `admin` may call this. The MasterKey is derived from the secret with
HKDF-SHA512; EDS stores only its version and a key check value (KCV), never the key
or the secret. The request has these outcomes:

- **First setup**: EDS is locked and no MasterKey was set up yet. The secret must
  meet the entropy rules; it becomes MasterKey version 1, and the System
  Administrator account is created if missing ("MasterKey unlocked").
- **Unlock**: EDS is locked (no source configured, or the source didn't match) and
  the secret matches the stored KCV. With valid System Administrator credentials,
  EDS is unlocked ("MasterKey unlocked"). A wrong secret is rejected by the KCV,
  without decrypting anything.
- **Same key**: EDS is unlocked and the secret gives the current MasterKey; only
  the stored URL is updated ("MasterKey unlocked").
- **Rotation**: EDS is unlocked and the secret gives a different MasterKey. The
  secret must meet the entropy rules. In one transaction, every value the MasterKey
  protects is re-wrapped with the new MasterKey, which gets the next version
  ("MasterKey updated"): the Members' salts and private keys and the Trustees'
  Circle Secrets. Sessions end, and pending invitations are deleted. Data Objects
  aren't touched, so a rotation is fast even with a lot of data.

When `url` is given, it is stored as `eds.masterkey.url` and used at the next start;
with `secret`, the stored URL is cleared. At startup, EDS reads the source from the
environment variable `EDS_MASTERKEY_URL`, the system property `eds.masterkey.url`,
or the setting, in that order. There is no default MasterKey: without a source,
EDS stays locked, and every other request fails with CRYPTO_ERROR until it is
unlocked.
