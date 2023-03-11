/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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

-- =============================================================================
-- This script will setup the EDS Database for PostgreSQL. It will wipe any
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
\connect eds eds_user

-- If it is a fresh Database, we can run the initial setup.
\ir 03-create.sql

-- Else, if DB exists, we must update it, each update checks if it is allowed.
-- Update 1: EDS 1.0 -> 1.1 (First Feature Release)
\ir 04-update-1.sql
-- Update 2: EDS 1.1 -> 1.2 (Second Feature Release)
\ir 04-update-2.sql
-- Update 3: EDS 1.2 -> 2.0 (Major Upgrade, migrating from Java 8 to Java 11)
\ir 04-update-3.sql

-- =============================================================================
-- Done, database is now created :-D
-- =============================================================================
