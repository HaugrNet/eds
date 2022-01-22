-- =============================================================================
-- PostgreSQL Upgrade Script to upgrade CWS from 1.1 to 1.2
-- -----------------------------------------------------------------------------

DO $U2$
DECLARE
    current_schema_version INTEGER;
BEGIN
    SELECT max(schema_version)
    FROM cws_versions
    INTO current_schema_version;

    IF current_schema_version < 3 THEN
        -- Second feature release, CWS 1.2.x results requires an update of the DB
        INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (3, '1.2.0', 'PostgreSQL');

        -- Save all changes
        COMMIT;
    END IF;
END $U2$;
