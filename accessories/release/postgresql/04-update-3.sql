-- =============================================================================
-- PostgreSQL Upgrade Script to upgrade CWS from 1.2 to 2.0
-- -----------------------------------------------------------------------------

DO $U3$
DECLARE
    current_schema_version INTEGER;
BEGIN
    SELECT max(schema_version)
    FROM cws_versions
    INTO current_schema_version;

    IF current_schema_version < 4 THEN
        -- Java Version upgrade (8->11), breaks backwards compatibility, CWS 2.x
        INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (4, '2.0.0', 'PostgreSQL');

        -- As the names of the Algorithm's have been extended, we also have to
        -- update these, and set the new default as well.
        ALTER TABLE cws_members ALTER COLUMN pbe_algorithm TYPE VARCHAR(25);
        ALTER TABLE cws_members ALTER COLUMN pbe_algorithm SET DEFAULT 'PBE_GCM_256';
        ALTER TABLE cws_members ALTER COLUMN rsa_algorithm TYPE VARCHAR(25);

        -- Rename misleading constraint
        ALTER TABLE cws_metadata RENAME CONSTRAINT metadata_notafter_parent_id TO metadata_not_before_parent_id;

        -- Now the tables have been updated, the values must also be corrected.
        UPDATE cws_members  SET pbe_algorithm = 'PBE_CBC_128'  WHERE pbe_algorithm = 'PBE_128';
        UPDATE cws_members  SET pbe_algorithm = 'PBE_CBC_192'  WHERE pbe_algorithm = 'PBE_192';
        UPDATE cws_members  SET pbe_algorithm = 'PBE_CBC_256'  WHERE pbe_algorithm = 'PBE_256';
        UPDATE cws_settings SET setting = 'PBE_CBC_128'        WHERE setting = 'PBE_128';
        UPDATE cws_settings SET setting = 'PBE_CBC_192'        WHERE setting = 'PBE_192';
        UPDATE cws_settings SET setting = 'PBE_CBC_256'        WHERE setting = 'PBE_256';

        -- Save changes for CWS 2.0
        COMMIT;
    END IF;
END $U3$;
