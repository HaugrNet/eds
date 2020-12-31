-- =============================================================================
-- PostgreSQL Upgrade Script to upgrade CWS from 1.1 to 1.2
-- -----------------------------------------------------------------------------

-- Start the changes;
BEGIN;

-- Second feature release, CWS 1.2.x results requires an update of the DB
INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (3, '1.2.0', 'PostgreSQL');

-- Save all changes
COMMIT;
