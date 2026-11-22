## POST /sanitized
permission: SANITY

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `circleId` | no | Only failures in this Circle; the Member must be its administrator. |
| `since` | no | Only failures after this time (UTC). |

Without `circleId`, the System Administrator gets the failures in all Circles, and
a Circle administrator gets those in the Circles they administer.
