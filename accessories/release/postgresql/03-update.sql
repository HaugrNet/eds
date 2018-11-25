-- =============================================================================
-- PostgreSQL Upgrade Script to upgrade CWS from 1.0 to 1.1
-- -----------------------------------------------------------------------------

-- Start the changes;
BEGIN;

-- First feature release, CWS 1.1.x results requires an update of the DB
INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (2, '1.1.0', 'PostgreSQL');

-- Support for Sessions, is the most important add-on for CWS 1.1. It requires
-- that the Members table is extended with 3 new fields:
ALTER TABLE cws_members ADD COLUMN session_checksum VARCHAR(256);
ALTER TABLE cws_members ADD COLUMN session_crypto   VARCHAR(16384);
ALTER TABLE cws_members ADD COLUMN session_expire   TIMESTAMP;
ALTER TABLE cws_members ADD CONSTRAINT member_unique_session_checksum UNIQUE (session_checksum);

-- To prevent full-table scans on users and circles, a case-insensitive indexing
-- is needed, however that won't work directly. Hence, a lowercase index is
-- added to the names, which is used for searches.
CREATE INDEX cws_members_name_index ON cws_members (lower(name));
CREATE INDEX cws_circles_name_index ON cws_circles (lower(name));

-- The design flaw in CWS 1.0, where upon roles were hardcoded to specific Id's
-- and name, has to be corrected to allow for more transparency. This means that
-- we need a new field to store the role in.
ALTER TABLE cws_members ADD COLUMN member_role VARCHAR(10) DEFAULT 'STANDARD';

-- Update existing accounts to reflect this new standard.
UPDATE cws_members SET member_role = 'ADMIN' WHERE id = 1;
UPDATE cws_members SET member_role = 'STANDARD' WHERE id > 1;
-- Now, apply the not-null constraint to this column
ALTER TABLE cws_members ADD CONSTRAINT member_notnull_role CHECK (member_role IS NOT NULL),

-- The constants have been altered, to allow for more clarity and additional
-- values, this means that they default values must be corrected as well.
ALTER TABLE cws_members ALTER COLUMN pbe_algorithm SET DEFAULT 'PBE_256';
ALTER TABLE cws_members ALTER COLUMN rsa_algorithm SET DEFAULT 'RSA_2048';
ALTER TABLE cws_keys    ALTER COLUMN algorithm     SET DEFAULT 'AES_CBC_256';

-- Now the tables have been updated, the values must also be corrected.
UPDATE cws_members  SET pbe_algorithm = 'PBE_128'  WHERE pbe_algorithm = 'PBE128';
UPDATE cws_members  SET pbe_algorithm = 'PBE_192'  WHERE pbe_algorithm = 'PBE192';
UPDATE cws_members  SET pbe_algorithm = 'PBE_256'  WHERE pbe_algorithm = 'PBE256';
UPDATE cws_members  SET rsa_algorithm = 'RSA_2048' WHERE rsa_algorithm = 'RSA2048';
UPDATE cws_members  SET rsa_algorithm = 'RSA_4096' WHERE rsa_algorithm = 'RSA4096';
UPDATE cws_members  SET rsa_algorithm = 'RSA_8192' WHERE rsa_algorithm = 'RSA8192';
UPDATE cws_keys     SET algorithm = 'AES_CBC_128'  WHERE algorithm = 'AES128';
UPDATE cws_keys     SET algorithm = 'AES_CBC_192'  WHERE algorithm = 'AES192';
UPDATE cws_keys     SET algorithm = 'AES_CBC_256'  WHERE algorithm = 'AES256';
UPDATE cws_settings SET setting = 'SHA_256'        WHERE setting = 'SHA256';
UPDATE cws_settings SET setting = 'SHA_512'        WHERE setting = 'SHA512';
UPDATE cws_settings SET setting = 'AES_CBC_128'    WHERE setting = 'AES128';
UPDATE cws_settings SET setting = 'AES_CBC_192'    WHERE setting = 'AES192';
UPDATE cws_settings SET setting = 'AES_CBC_256'    WHERE setting = 'AES256';
UPDATE cws_settings SET setting = 'PBE_128'        WHERE setting = 'PBE128';
UPDATE cws_settings SET setting = 'PBE_192'        WHERE setting = 'PBE192';
UPDATE cws_settings SET setting = 'PBE_256'        WHERE setting = 'PBE256';
UPDATE cws_settings SET setting = 'RSA_2048'       WHERE setting = 'RSA2048';
UPDATE cws_settings SET setting = 'RSA_4096'       WHERE setting = 'RSA4096';
UPDATE cws_settings SET setting = 'RSA_8192'       WHERE setting = 'RSA8192';

-- Save all changes
COMMIT;
