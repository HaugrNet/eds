## POST /settings
permission: SETTING
- SETTING_WARNING: A key is empty, a standard setting has an empty or invalid value, a locked setting is changed after Members exist, or `eds.masterkey.url` is changed (use the MasterKey operation).

**Fields:**

| Field | Required | Notes |
|---|---|---|
| `settings` | yes | Key-value pairs to add or change; `{}` only reads the settings. |

Values are trimmed. An empty value deletes a custom setting; standard settings
can't be deleted. Unknown keys are stored as custom settings. The response always
contains all settings, also when a `SETTING_WARNING` is returned.
