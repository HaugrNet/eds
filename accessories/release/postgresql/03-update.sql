-- =============================================================================
-- PostgreSQL Upgrade Script to upgrade CWS from 1.2 to 2.0
-- -----------------------------------------------------------------------------

-- Start the changes;
BEGIN;

-- Java Version upgrade (8->11), breaks backwards compatibility, CWS 2.x
INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (4, '2.0.0', 'PostgreSQL');

-- Save all changes
COMMIT;
