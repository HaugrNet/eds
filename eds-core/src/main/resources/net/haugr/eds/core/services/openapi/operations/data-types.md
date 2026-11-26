## POST /dataTypes/processDataType
permission: PROCESS_DATA_TYPE
- AUTHORIZATION_WARNING: The Member isn't an administrator, or the type is one of the defaults (`data`, `folder`, `thumbnail`, `entry`), which can't be changed.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `typeName` | yes | Up to 75 characters. An existing name updates that type, a new one creates it. |
| `type` | yes | The definition, up to 256 characters. |

Only administrators may change Data Types, as they are shared by all Circles.
Returns the stored type in `dataType`.

## POST /dataTypes/deleteDataType
permission: PROCESS_DATA_TYPE
- AUTHORIZATION_WARNING: The Member isn't an administrator, or the type is one of the defaults (`data`, `folder`, `thumbnail`, `entry`), which can't be deleted.
- IDENTIFICATION_WARNING: No Data Type with the name exists.
- ILLEGAL_ACTION: The type is still used by Data Objects.

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `typeName` | yes | The Data Type to delete. |

Only administrators may delete Data Types. Only unused types can be deleted, and
never the defaults.

## POST /dataTypes/fetchDataTypes
permission: FETCH_DATA_TYPE

**Fields:** only the authentication fields. Requires the trust level `READ` in at
least one Circle.

## GET /dataTypes
same-as: POST /dataTypes/fetchDataTypes

Lists the Data Types.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## PUT /dataTypes/{typeName}
same-as: POST /dataTypes/processDataType

Creates or updates the Data Type `typeName`; the body has `type`.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.

## DELETE /dataTypes/{typeName}
same-as: POST /dataTypes/deleteDataType

Deletes the Data Type `typeName`. No body.

Part of the ID-based resource API: authenticated with the `Authorization: Bearer` header, answered with the HTTP status of the `returnCode`.
