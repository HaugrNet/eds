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
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.algorithm', 'AES', true);
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.cipher.mode', 'CBC', false);
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.padding', 'PKCS5Padding', true);

-- The Key length is set to 128 bits by default, as this is the maximum allowed
-- by Java, unless the unlimited strength patch have been applied. If it has
-- been applied, it is also possible to use 192 and 256 bit keys.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.keylength', '128', true);

-- Asymmetric Encryption (Public & Private Key), is used for sharing the
-- Symmetric Keys, not for encrypting any data. For more information about
-- these, please see the references given above.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.asymmetric.algorithm', 'RSA', true);
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
