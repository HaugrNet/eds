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

-- Create the users and database needed for EDS. The role name, password and
-- database name must be supplied by the caller via the psql variables
-- :eds_user, :eds_password and :eds_db (set with `psql -v eds_user=...
-- -v eds_password=... -v eds_db=...`). This avoids shipping defaults in
-- source control.

DO $I$
DECLARE
    user_exists BOOLEAN;
    -- Following is commented out (see below)
    -- db_exists BOOLEAN;
BEGIN
    IF length(:'eds_user') = 0 THEN
        RAISE EXCEPTION 'eds_user psql variable is required (pass via -v eds_user=...)';
    END IF;
    IF length(:'eds_password') = 0 THEN
        RAISE EXCEPTION 'eds_password psql variable is required (pass via -v eds_password=...)';
    END IF;
    IF length(:'eds_db') = 0 THEN
        RAISE EXCEPTION 'eds_db psql variable is required (pass via -v eds_db=...)';
    END IF;

    SELECT exists (SELECT FROM pg_user WHERE usename = :'eds_user') INTO user_exists;
    IF user_exists = false THEN
        EXECUTE format('CREATE ROLE %I WITH LOGIN PASSWORD %L', :'eds_user', :'eds_password');
    END IF;

    -- It is not possible to run the following inside a function, hence it is
    -- commented out. It is left as an exercise for later to see if a solution
    -- to prevent an error when creating the database can be found.
    -- SELECT exists (SELECT FROM pg_database WHERE datname = :'eds_db') INTO db_exists;
    -- IF db_exists = false THEN
    --     -- Create the EDS database for the EDS User
    --     CREATE DATABASE :"eds_db" WITH OWNER = :"eds_user";
    -- END IF;

    -- Save changes
    COMMIT;
END $I$;

-- Create the EDS database for the EDS User
CREATE DATABASE :"eds_db" WITH OWNER = :"eds_user";
