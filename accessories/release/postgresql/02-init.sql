/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */

-- Create the role and database needed for EDS. The role name, password and
-- database name must be supplied by the caller via the psql variables
-- :eds_user, :eds_password and :eds_db (set with `psql -v eds_user=...
-- -v eds_password=... -v eds_db=...`). This avoids shipping defaults in
-- source control.
--
-- Note: the statements below deliberately avoid a DO block. psql does not
-- perform variable interpolation inside dollar-quoted strings, so :'eds_user'
-- and friends would be sent verbatim to the server and fail with a syntax
-- error. Instead, the statements are generated client side with format() and
-- executed with \gexec, which also lets us create the database (which cannot
-- run inside a function) conditionally.

-- Ensure all three variables were supplied. Unset variables must be caught
-- first, because psql leaves :'var' untouched when var is undefined, which the
-- server then rejects with an opaque syntax error. Each failure raises a
-- server side exception so that psql exits non-zero under ON_ERROR_STOP=1, and
-- then quits so that nothing is created from incomplete input.
\if :{?eds_user}
\else
DO $I$ BEGIN RAISE EXCEPTION 'eds_user psql variable is required (pass via -v eds_user=...)'; END $I$;
\quit
\endif
\if :{?eds_password}
\else
DO $I$ BEGIN RAISE EXCEPTION 'eds_password psql variable is required (pass via -v eds_password=...)'; END $I$;
\quit
\endif
\if :{?eds_db}
\else
DO $I$ BEGIN RAISE EXCEPTION 'eds_db psql variable is required (pass via -v eds_db=...)'; END $I$;
\quit
\endif

-- The variables exist, so they can now be interpolated and checked for empty
-- values, which format('%I') would otherwise turn into an invalid identifier.
SELECT length(:'eds_user') = 0 AS eds_user_empty,
       length(:'eds_password') = 0 AS eds_password_empty,
       length(:'eds_db') = 0 AS eds_db_empty
\gset

\if :eds_user_empty
DO $I$ BEGIN RAISE EXCEPTION 'eds_user psql variable must not be empty'; END $I$;
\quit
\endif
\if :eds_password_empty
DO $I$ BEGIN RAISE EXCEPTION 'eds_password psql variable must not be empty'; END $I$;
\quit
\endif
\if :eds_db_empty
DO $I$ BEGIN RAISE EXCEPTION 'eds_db psql variable must not be empty'; END $I$;
\quit
\endif

-- Create the EDS role, unless it already exists.
SELECT format('CREATE ROLE %I WITH LOGIN PASSWORD %L', :'eds_user', :'eds_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = :'eds_user')
\gexec

-- Create the EDS database for the EDS role, unless it already exists.
SELECT format('CREATE DATABASE %I WITH OWNER = %I', :'eds_db', :'eds_user')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'eds_db')
\gexec
