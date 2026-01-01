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

-- =============================================================================
-- PostgreSQL Upgrade Script to upgrade EDS from 1.2 to 2.0
-- -----------------------------------------------------------------------------

DO $U3$
DECLARE
    current_schema_version INTEGER;
BEGIN
    SELECT max(schema_version)
    FROM eds_versions
    INTO current_schema_version;

    IF current_schema_version < 4 THEN
        -- Java Version upgrade (8->11), breaks backwards compatibility, EDS 2.x
        INSERT INTO eds_versions(schema_version, eds_version, db_vendor) VALUES (4, '2.0.0', 'PostgreSQL');

        -- As the names of the Algorithm's have been extended, we also have to
        -- update these and set the new default as well.
        ALTER TABLE eds_members ALTER COLUMN pbe_algorithm TYPE VARCHAR(25);
        ALTER TABLE eds_members ALTER COLUMN pbe_algorithm SET DEFAULT 'PBE_GCM_256';
        ALTER TABLE eds_members ALTER COLUMN rsa_algorithm TYPE VARCHAR(25);

        -- Rename misleading constraint
        ALTER TABLE eds_metadata RENAME CONSTRAINT metadata_notafter_parent_id TO metadata_not_before_parent_id;

        -- Now the tables have been updated, the values must also be corrected.
        UPDATE eds_members  SET pbe_algorithm = 'PBE_CBC_128'  WHERE pbe_algorithm = 'PBE_128';
        UPDATE eds_members  SET pbe_algorithm = 'PBE_CBC_192'  WHERE pbe_algorithm = 'PBE_192';
        UPDATE eds_members  SET pbe_algorithm = 'PBE_CBC_256'  WHERE pbe_algorithm = 'PBE_256';
        UPDATE eds_settings SET setting = 'PBE_CBC_128'        WHERE setting = 'PBE_128';
        UPDATE eds_settings SET setting = 'PBE_CBC_192'        WHERE setting = 'PBE_192';
        UPDATE eds_settings SET setting = 'PBE_CBC_256'        WHERE setting = 'PBE_256';

        -- Save changes for EDS 2.0
        COMMIT;
    END IF;
END $U3$;
