-- =============================================================================
-- This script will setup the CWS Database for PostgreSQL. It will wipe any
-- existing database and roles and create everything from scratch.
--   The script must be run by a user with the permissions to create a new
-- database, and grant permissions. To run the script, simply use this command:
-- $ psql -f 01-setup.sql postgres
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Part ONE: Creating Database & Roles, ignore errors :-/
-- -----------------------------------------------------------------------------
\ir 02-init.sql
-- -----------------------------------------------------------------------------


-- -----------------------------------------------------------------------------
-- Part TWO: Creating Tables
-- -----------------------------------------------------------------------------
-- Now, we're ready to create/update the database. First connect to it.
\connect cws cws_user

-- If it is a fresh Database, we can run the initial setup.
\ir 03-create.sql

-- Else, if DB exists, we must update it, each update checks if it is allowed.
-- Update 1: CWS 1.0 -> 1.1 (First Feature Release)
\ir 04-update-1.sql
-- Update 2: CWS 1.1 -> 1.2 (Second Feature Release)
\ir 04-update-2.sql
-- Update 3: CWS 1.2 -> 2.0 (Major Upgrade, migrating from Java 8 to Java 11)
\ir 04-update-3.sql

-- =============================================================================
-- Done, database is now created :-D
-- =============================================================================
