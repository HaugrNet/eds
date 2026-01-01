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
-- PostgreSQL Upgrade Script to upgrade EDS from 1.1 to 1.2
-- -----------------------------------------------------------------------------

DO $U2$
DECLARE
    current_schema_version INTEGER;
BEGIN
    SELECT max(schema_version)
    FROM eds_versions
    INTO current_schema_version;

    IF current_schema_version < 3 THEN
        -- Second feature release, EDS 1.2.x results requires an update of the DB
        INSERT INTO eds_versions(schema_version, eds_version, db_vendor) VALUES (3, '1.2.0', 'PostgreSQL');

        -- Save all changes
        COMMIT;
    END IF;
END $U2$;
