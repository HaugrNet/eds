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
-- PostgreSQL Upgrade Script to upgrade EDS from 2.0 to 2.1
-- Rate-limiting feature for authentication (GitHub Issue #68)
-- -----------------------------------------------------------------------------

DO $U4$
DECLARE
    current_schema_version INTEGER;
BEGIN
    SELECT max(schema_version)
    FROM eds_versions
    INTO current_schema_version;

    IF current_schema_version < 5 THEN
        -- Rate limiting feature, EDS 2.1.0
        INSERT INTO eds_versions(schema_version, eds_version, db_vendor) VALUES (5, '2.1.0', 'PostgreSQL');

        -- Login attempts tracking table for rate-limiting
        CREATE TABLE eds_login_attempts (
          id               SERIAL,
          account_name     VARCHAR(75) NOT NULL,
          success          BOOLEAN NOT NULL DEFAULT false,
          ip_address       VARCHAR(45),
          altered          TIMESTAMP DEFAULT now(),
          added            TIMESTAMP DEFAULT now(),

          CONSTRAINT login_attempt_pk              PRIMARY KEY (id),
          CONSTRAINT login_attempt_notnull_id      CHECK (id IS NOT NULL),
          CONSTRAINT login_attempt_notnull_account CHECK (account_name IS NOT NULL),
          CONSTRAINT login_attempt_notnull_success CHECK (success IS NOT NULL),
          CONSTRAINT login_attempt_notnull_altered CHECK (altered IS NOT NULL),
          CONSTRAINT login_attempt_notnull_added   CHECK (added IS NOT NULL)
        );

        -- Index for efficient lookups by account name and time
        CREATE INDEX eds_login_attempts_account_idx ON eds_login_attempts (lower(account_name), added);

        -- Save changes for EDS 2.1
        COMMIT;
    END IF;
END $U4$;
