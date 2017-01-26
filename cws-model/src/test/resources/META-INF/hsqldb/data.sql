-- =============================================================================
-- Initialization Script for the CWS Database
-- =============================================================================

-- Initial Database Version is 1, initial Production CWS release is 1.0.0
INSERT INTO versions(schema_version, cws_version) VALUES (1, '1.0.0');

-- Default, we have 1 Object Type, which is the folder. The rest is left to
-- the initial setup to create
INSERT INTO types (type_name, type_value) VALUES ('folder', 'Folder');


-- =============================================================================
-- Default Settings values
-- =============================================================================
-- The following settings are used for the Symmetric Encryption parts. When
-- applying Symmetric encryption, 4 things are required. The Algorithm, Cipher
-- Mode, Padding and Key length. Changes to any of these may cause problems, so
-- please only change them if you know what you're doing.
--   See: http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html
-- -----------------------------------------------------------------------------

-- The Algorithm used for the Symmetric Keys in CWS. All data is stored using
-- this Algorithm. Although it can be changed, please test the CWS carefully
-- before doing so. And please be aware, that the information here is only used
-- for generating new Keys, so changing things will not affect existing.
--   According to Oracle (http://docs.oracle.com/javase/8/docs/api/javax/crypto/Cipher.html),
-- The following combined algorithm, cipher mode and padding must be supported:
--   * AES/CBC/NoPadding (128)
--   * AES/CBC/PKCS5Padding (128)
--   * AES/ECB/NoPadding (128)
--   * AES/ECB/PKCS5Padding (128)
--   * DES/CBC/NoPadding (56)
--   * DES/CBC/PKCS5Padding (56)
--   * DES/ECB/NoPadding (56)
--   * DES/ECB/PKCS5Padding (56)
--   * DESede/CBC/NoPadding (168)
--   * DESede/CBC/PKCS5Padding (168)
--   * DESede/ECB/NoPadding (168)
--   * DESede/ECB/PKCS5Padding (168)
--   * RSA/ECB/PKCS1Padding (1024, 2048)
--   * RSA/ECB/OAEPWithSHA-1AndMGF1Padding (1024, 2048)
--   * RSA/ECB/OAEPWithSHA-256AndMGF1Padding (1024, 2048)
-- The default should be sufficient for most, if increased security is wanted,
-- please consider installing and using the unlimited strength patch.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.algorithm', 'AES', false);
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.cipher.mode', 'CBC', false);
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.padding', 'PKCS5Padding', false);

-- The Key length is set to 128 bits by default, as this is the maximum allowed
-- by Java, unless the unlimited strength patch have been applied. If it has
-- been applied, it is also possible to use 192 and 256 bit keys.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.keylength', '128', true);

-- Asymmetric Encryption (Public & Private Key), is used for sharing the
-- Symmetric Keys, not for encrypting any data. For more information about
-- these, please see the references given above.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.asymmetric.algorithm', 'RSA', false);
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.asymmetric.keylength', '2048', true);

-- If a Member is using something else than a Key to unlock their Account, the
-- CWS will use the following Password Based Encryption, PBE, algorithm to do
-- the trick. The provided information is extended with an instance specific
-- Salt, and a Member Account specific Salt to ensure that enough entropy is
-- available to create a strong enough Key to unlock the Private Key for the
-- Account.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.pbe.algorithm', 'PBKDF2WithHmacSHA256', false);

-- This is the System specific Salt, which will be applied whenever PBE is used
-- to unlock the Private Key of a Member Account. This Salt should be set during
-- installation, and never changed, as it will render PBE based *all* accounts
-- useless.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.system.salt', 'System Specific Salt, should be generated to a unique value per Setup.', false);

-- When applying armoring to the raw keys, it means using a Base64 encoding and
-- decoding. However, they have to be saved using a character set. Any character
-- set can be used, but if keys have been stored using one, changing it will
-- cause problems as they may not be read out safely again. So, please only
-- change this if you are really sure.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.system.charset', 'UTF-8', true);


-- =============================================================================
--  Following is TEST data, and should not be added in a PRODUCTION environment
-- -----------------------------------------------------------------------------
-- Unfortunately, JPA only allow setting 3 scripts when creating the database,
-- the first is the actual model, which contain what is needed to setup the
-- database, including all tables, views, procedures, constraints, etc. The
-- second script is for the data (this one), but as we both need to have data
-- for production and for testing, we're adding it all here. The final script
-- is for destroying the database, which is needed of you have a real database
-- and not just an in-memory database.
-- =============================================================================

-- Default Administrator User, it is set at the first request to the System, and
-- is thus needed for loads of tests.
insert into members (external_id, name, salt, public_key, private_key) values (
    '483833a4-2af7-4d9d-953d-b1e86cac8035',
    'admin',
    '16dc08da-84f3-4ebb-af1d-c51d2c7c377d',
    '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'
);
