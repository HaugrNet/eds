/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

-- Create the users needed for EDS. Please note, that the
-- default AppServer Configuration file is referring to these users & passwords.
-- So, if you use a different name and password, please also update this.

DO $I$
DECLARE
    user_exists BOOLEAN;
    -- Following is commented out (see below)
    -- db_exists BOOLEAN;
BEGIN
    SELECT exists (SELECT FROM pg_user WHERE usename = 'eds_user') INTO user_exists;
    IF user_exists = false THEN
        CREATE ROLE eds_user WITH LOGIN PASSWORD 'eds';
    END IF;

    -- It is not possible to run the following inside a function, hence it is
    -- commented out. It is left as an exercise for later to see if a solution
    -- to prevent an error when creating the database can be found.
    -- SELECT exists (SELECT FROM pg_database WHERE datname = 'eds') INTO db_exists;
    -- IF db_exists = false THEN
    --     -- Create the EDS database for the EDS User
    --     CREATE DATABASE eds WITH OWNER = eds_user;
    -- END IF;

    -- Save changes
    COMMIT;
END $I$;

-- Create the EDS database for the EDS User
CREATE DATABASE eds WITH OWNER = eds_user;
