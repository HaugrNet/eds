Operations of OpenApiFilterTest; lines before the first section are left out.

## POST /test/original
permission: FETCH_DATA
- IDENTIFICATION_WARNING: The object is unknown.

The original operation.

## GET /test/copy
same-as: POST /test/original

## POST /test/plain
permission: SETTING

## POST /test/stricter
same-as: POST /test/original
permission: PROCESS_DATA

A copy which asks for more.
