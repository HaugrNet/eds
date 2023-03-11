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
-- PostgreSQL Upgrade Script to upgrade EDS from 1.0 to 1.1
-- -----------------------------------------------------------------------------

DO $U1$
DECLARE
    current_schema_version INTEGER;
BEGIN
    SELECT max(schema_version)
    FROM eds_versions
    INTO current_schema_version;

    IF current_schema_version < 2 THEN
        -- First feature release, EDS 1.1.x results requires an update of the DB
        INSERT INTO eds_versions(schema_version, eds_version, db_vendor) VALUES (2, '1.1.0', 'PostgreSQL');

        -- Support for Sessions, is the most important add-on for EDS 1.1. It requires
        -- that the Members table is extended with 3 new fields:
        ALTER TABLE eds_members ADD COLUMN session_checksum VARCHAR(256);
        ALTER TABLE eds_members ADD COLUMN session_crypto   VARCHAR(16384);
        ALTER TABLE eds_members ADD COLUMN session_expire   TIMESTAMP;
        ALTER TABLE eds_members ADD CONSTRAINT member_unique_session_checksum UNIQUE (session_checksum);

        -- To prevent full-table scans on users and circles, a case-insensitive indexing
        -- is needed, however that won't work directly. Hence, a lowercase index is
        -- added to the names, which is used for searches.
        CREATE INDEX eds_members_name_index ON eds_members (lower(name));
        CREATE INDEX eds_circles_name_index ON eds_circles (lower(name));

        -- The design flaw in EDS 1.0, where upon roles were hardcoded to specific Id's
        -- and name, has to be corrected to allow for more transparency. This means that
        -- we need a new field to store the role in.
        ALTER TABLE eds_members ADD COLUMN member_role VARCHAR(10) DEFAULT 'STANDARD';

        -- Update existing accounts to reflect this new standard.
        UPDATE eds_members SET member_role = 'ADMIN' WHERE id = 1;
        UPDATE eds_members SET member_role = 'STANDARD' WHERE id > 1;
        -- Now, apply the not-null constraint to this column
        ALTER TABLE eds_members ADD CONSTRAINT member_notnull_role CHECK (member_role IS NOT NULL);

        -- The constants have been altered, to allow for more clarity and additional
        -- values, this means that they default values must be corrected as well.
        ALTER TABLE eds_members ALTER COLUMN pbe_algorithm SET DEFAULT 'PBE_256';
        ALTER TABLE eds_members ALTER COLUMN rsa_algorithm SET DEFAULT 'RSA_2048';
        ALTER TABLE eds_keys    ALTER COLUMN algorithm     SET DEFAULT 'AES_CBC_256';

        -- Now the tables have been updated, the values must also be corrected.
        UPDATE eds_members  SET pbe_algorithm = 'PBE_128'  WHERE pbe_algorithm = 'PBE128';
        UPDATE eds_members  SET pbe_algorithm = 'PBE_192'  WHERE pbe_algorithm = 'PBE192';
        UPDATE eds_members  SET pbe_algorithm = 'PBE_256'  WHERE pbe_algorithm = 'PBE256';
        UPDATE eds_members  SET rsa_algorithm = 'RSA_2048' WHERE rsa_algorithm = 'RSA2048';
        UPDATE eds_members  SET rsa_algorithm = 'RSA_4096' WHERE rsa_algorithm = 'RSA4096';
        UPDATE eds_members  SET rsa_algorithm = 'RSA_8192' WHERE rsa_algorithm = 'RSA8192';
        UPDATE eds_keys     SET algorithm = 'AES_CBC_128'  WHERE algorithm = 'AES128';
        UPDATE eds_keys     SET algorithm = 'AES_CBC_192'  WHERE algorithm = 'AES192';
        UPDATE eds_keys     SET algorithm = 'AES_CBC_256'  WHERE algorithm = 'AES256';
        UPDATE eds_settings SET setting = 'SHA_256'        WHERE setting = 'SHA256';
        UPDATE eds_settings SET setting = 'SHA_512'        WHERE setting = 'SHA512';
        UPDATE eds_settings SET setting = 'AES_CBC_128'    WHERE setting = 'AES128';
        UPDATE eds_settings SET setting = 'AES_CBC_192'    WHERE setting = 'AES192';
        UPDATE eds_settings SET setting = 'AES_CBC_256'    WHERE setting = 'AES256';
        UPDATE eds_settings SET setting = 'PBE_128'        WHERE setting = 'PBE128';
        UPDATE eds_settings SET setting = 'PBE_192'        WHERE setting = 'PBE192';
        UPDATE eds_settings SET setting = 'PBE_256'        WHERE setting = 'PBE256';
        UPDATE eds_settings SET setting = 'RSA_2048'       WHERE setting = 'RSA2048';
        UPDATE eds_settings SET setting = 'RSA_4096'       WHERE setting = 'RSA4096';
        UPDATE eds_settings SET setting = 'RSA_8192'       WHERE setting = 'RSA8192';

        -- Save all changes
        COMMIT;
    END IF;
END $U1$;
