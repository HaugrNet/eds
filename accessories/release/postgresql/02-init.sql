-- Create the users needed for CWS. Please note, that the
-- default AppServer Configuration file is referring to these users & passwords.
-- So, if you use a different name and password, please also update this.

DO $I$
DECLARE
    user_exists BOOLEAN;
    -- Following is commented out (see below)
    -- db_exists BOOLEAN;
BEGIN
    SELECT exists (SELECT FROM pg_user WHERE usename = 'cws_user') INTO user_exists;
    IF user_exists = false THEN
        CREATE ROLE cws_user WITH LOGIN PASSWORD 'cws';
    END IF;

    -- It is not possible to run the following inside a function, hence it is
    -- commented out. It is left as an exercise for later to see if a solution
    -- to prevent an error when creating the database can be found.
    -- SELECT exists (SELECT FROM pg_database WHERE datname = 'cws') INTO db_exists;
    -- IF db_exists = false THEN
    --     -- Create the CWS database for the CWS User
    --     CREATE DATABASE cws WITH OWNER = cws_user;
    -- END IF;

    -- Save changes
    COMMIT;
END $I$;

-- Create the CWS database for the CWS User
CREATE DATABASE cws WITH OWNER = cws_user;
