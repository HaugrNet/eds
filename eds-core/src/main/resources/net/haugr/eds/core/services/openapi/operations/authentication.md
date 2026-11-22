## POST /authenticated
permission: PROCESS_MEMBER

Checks the credentials in the request and returns the Member's ID. Useful for a
client to verify credentials (passphrase or session) before doing anything else.

**Fields:** only the authentication fields (`accountName`, `credential`,
`credentialType`).
