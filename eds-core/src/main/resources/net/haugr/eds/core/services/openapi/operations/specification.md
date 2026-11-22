## GET /openapi.json
permission: NONE

Needs no authentication. The specification is the API contract, checked in as
`eds-api/src/main/resources/openapi.json`; it is served unchanged, and the build
ensures that it describes the running code. Clients can be generated from it.
