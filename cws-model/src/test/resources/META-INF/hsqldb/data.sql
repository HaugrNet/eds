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
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.symmetric.algorithm', 'AES128', false);

-- Asymmetric Encryption (Public & Private Key), is used for sharing the
-- Symmetric Keys, not for encrypting any data. For more information about
-- these, please see the references given above.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.asymmetric.algorithm', 'RSA2018', false);

-- When new Members are added, the System Administrator can issue a signature,
-- which can be used by the Member when creating their new Account. The
-- signature is made with this Algorithm.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.signature.algorithm', 'SHA512WithRSA', false);

-- If a Member is using something else than a Key to unlock their Account, the
-- CWS will use the following Password Based Encryption, PBE, algorithm to do
-- the trick. The provided information is extended with an instance specific
-- Salt, and a Member Account specific Salt to ensure that enough entropy is
-- available to create a strong enough Key to unlock the Private Key for the
-- Account.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.password.algorithm', 'PBE128', false);

-- This is the System specific Salt, which will be applied whenever PBE is used
-- to unlock the Private Key of a Member Account. This Salt should be set during
-- installation, and never changed, as it will render *all* PBE based accounts
-- useless.
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.system.salt', 'System Specific Salt, should be generated to a unique value per Setup.', false);

-- For correctly dealing with Strings, it is important that the Locale is set to
-- ensure that it is done properly. By default the Locale is English (EN), but
-- if preferred, any other can be chosen. As long as they follow the IETF BCP 47
-- allowed values. See: https://en.wikipedia.org/wiki/IETF_language_tag
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.system.locale', 'EN', true);

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
INSERT INTO members (external_id, name, salt, algorithm, public_key, private_key) VALUES
    ('07b59d91-023f-4ecf-9166-b3db4c930c7b', 'admin',   '28dcb6e5-42ea-487f-9f13-d5611e1f67e6', 'RSA2048', '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'),
    ('b5239395-bd72-4df4-9473-a5d665ba0df1', 'member1', 'cd9fa19a-0796-45ba-bfa5-5fa66d4c816b', 'RSA2048', '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'),
    ('d81658a5-196d-4081-b38b-f7362156312e', 'member2', '8e9369be-c497-400f-b4ae-c14cf1b1b5c2', 'RSA2048', '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'),
    ('0f92648a-a922-449f-a287-8d0f266547d9', 'member3', '5ef280c3-0429-48cc-90d0-35cf9df10353', 'RSA2048', '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'),
    ('6da18969-a1f4-498a-9e43-53d8e0ea59f7', 'member4', '8defcc27-50dd-4dad-8f90-151a83f40a37', 'RSA2048', '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'),
    ('279e700f-42b7-4024-aede-315445ddfde8', 'member5', '0a8d797c-31ff-4c29-bd2f-d1578a4629dd', 'RSA2048', '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');

-- Default, we have 3 Circles as part of the test setup, using the very
-- imaginative names, 'circle1' to 'circle3'.
INSERT INTO circles (external_id, name) VALUES
    ('b8743050-a8cb-4e68-9c12-adfdc97629c9', 'circle1'),
    ('2e129aa9-a790-4bdc-8438-538ab5b7d335', 'circle2'),
    ('310c694a-71c2-4a04-838a-741442a18fbb', 'circle3');

-- For each Circle, we need to have a unique Key, but with the same settings.
INSERT INTO keys (algorithm, salt, status) VALUES
    ('AES128', '1ddaf6f5-1060-420b-90a3-cb6c95c7937d', 'ACTIVE'),
    ('AES128', 'a1d5a9e0-2e08-4cde-9f03-aa8e138107e4', 'ACTIVE'),
    ('AES128', '2d5ae533-0b62-42d2-8b90-9bf4139efa3e', 'ACTIVE');

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
