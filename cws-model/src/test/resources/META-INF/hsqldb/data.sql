-- =============================================================================
-- Initialization Script for the CWS Database
-- =============================================================================

-- Initial Database Version is 1, initial Production CWS release is 1.0.0
INSERT INTO versions(schema_version, cws_version) VALUES (1, '1.0.0');

-- Default, we have 1 Object Type, which is the folder. The rest is left to
-- the initial setup to create
INSERT INTO datatypes (datatype_name, datatype_value) VALUES ('folder', 'Folder');


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

-- When new Members are added, the System Administrator can issue a signature,
-- which can be used by the Member when creating their new Account. The
-- signature is made with this Algorithm.
INSERT INTO settings (name, setting, modifiable) VALUES ('ccws.crypto.signature.algorithm', 'SHA512WithRSA', false);

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

-- The Administrator Account is a special Account in the CWS, it is not
-- permitted to be a member of any Circles, nor can it be used for anything else
-- than some system administrative tasks. Which is also why it should not appear
-- in the list of Members to be fetched or assigned to Circles. However, rather
-- than completely opting out on this, it may be a good idea to expose it. Hence
-- this new setting value. Default false, meaning that the Administrator Account
-- is not visible unless explicitly changed to true.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.expose.admin', 'false', true);

-- Privacy is important, however - there may be reasons to reduce the privacy
-- level, and allow that a Member can view information about other Members even
-- if there is no direct relation between the two. If two members share a
-- Circle, then they will automatically be able to view each other, but  if not,
-- then this setting apply. By default, it is set to True - as CWS should be
-- used by organizations or companies where all members already share
-- information.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.show.trustees', 'true', true);


-- =============================================================================
-- Following is TEST data, and should not be added in a PRODUCTION environment
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
-- is thus needed for loads of tests. Remaining Accounts is for "member1" to
-- "member5", which is all used as part of the tests.
INSERT INTO members (external_id, name, salt, public_key, private_key) VALUES
    ('483833a4-2af7-4d9d-953d-b1e86cac8035', 'admin',   '16dc08da-84f3-4ebb-af1d-c51d2c7c377d', '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'),
    ('801bf00d-f483-464a-950c-6a585195c1bc', 'member1', '40b897b0-4b42-4f11-a0c7-07c4aee087de', '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'),
    ('1b7f50d8-a7b9-45dc-b22f-dae097ba1fd6', 'member2', 'af15fac4-b4cc-4fb5-9b8f-cbf7820a2fd2', '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'),
    ('1e9f0677-903d-40b6-a2af-e60382e5241b', 'member3', '31c33605-1df8-4618-8c76-59634efbf48c', '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'),
    ('70b1c008-5573-4ff0-8da5-340d5f73d804', 'member4', '746891f2-5708-4896-b250-51db475f9ee4', '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'),
    ('02c04ab5-0193-424c-9772-8431521aba72', 'member5', '8781a6d1-563c-4e0c-95d7-1860342d1853', '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');

-- Default, we have 3 Circles as part of the test setup, using the very
-- imaginative names, 'circle1' to 'circle3'.
INSERT INTO circles (external_id, name) VALUES
    ('b8743050-a8cb-4e68-9c12-adfdc97629c9', 'circle1'),
    ('2e129aa9-a790-4bdc-8438-538ab5b7d335', 'circle2'),
    ('310c694a-71c2-4a04-838a-741442a18fbb', 'circle3');

-- For each Circle, we need to have a unique Key, but with the same settings.
INSERT INTO keys (algorithm, cipher_mode, padding, status) VALUES
    ('AES', 'CBC', 'PKCS5Padding', 'ACTIVE'),
    ('AES', 'CBC', 'PKCS5Padding', 'ACTIVE'),
    ('AES', 'CBC', 'PKCS5Padding', 'ACTIVE');

-- With the Members created, and the Circles and Keys added, it is possible to
-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,
-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.
-- The Trust Level is different for each Member.
INSERT INTO trustees (external_id, member_id, circle_id, key_id, trust_level) VALUES
    ('c01e9a37-9ba0-4572-94ff-3d5bf357fc4a', 2, 1, 1, 'ADMIN'),
    ('d8158a61-a06a-40b6-bcda-39a8710e2339', 3, 1, 1, 'WRITE'),
    ('b3437863-9143-4a15-aeac-8372e9c44775', 4, 1, 1, 'READ'),
    ('41eb49f2-b1e5-43dc-8e4c-4bdcd19dd059', 2, 2, 2, 'ADMIN'),
    ('3ce6c2b8-c16e-46cd-987a-46e69edeab16', 3, 2, 2, 'WRITE'),
    ('9737762b-2f87-4caa-9013-d59851839a6a', 4, 2, 2, 'READ'),
    ('70e96aa5-2731-4f2a-bebe-7f5fecd5c688', 5, 2, 2, 'ADMIN'),
    ('9968d55a-4eee-414a-bbb6-4f273456f08b', 3, 3, 3, 'WRITE'),
    ('d1cae751-a160-430c-aba7-431407342dfa', 4, 3, 3, 'READ'),
    ('1f92b9ad-f68f-45fe-85d5-9756bbd2c600', 5, 3, 3, 'ADMIN'),
    ('b35a0d99-397c-4e77-8f84-843758700d60', 6, 3, 3, 'GUEST');
