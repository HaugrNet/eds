-- =============================================================================
-- Initialization Script for the CWS Database
-- =============================================================================

-- Default, we have 1 Object Type, which is the folder. The rest is left to
-- the initial setup to create
INSERT INTO cws_datatypes (datatype_name, datatype_value) VALUES
    ('folder', 'Folder'),
    ('data', 'Data Object');

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
INSERT INTO cws_settings (name, setting) VALUES ('cws.crypto.symmetric.algorithm', 'AES128');

-- Asymmetric Encryption (Public & Private Key), is used for sharing the
-- Symmetric Keys, not for encrypting any data. For more information about
-- these, please see the references given above.
INSERT INTO cws_settings (name, setting) VALUES ('cws.crypto.asymmetric.algorithm', 'RSA2048');

-- When new Members are added, the System Administrator can issue a signature,
-- which can be used by the Member when creating their new Account. The
-- signature is made with this Algorithm.
INSERT INTO cws_settings (name, setting) VALUES ('cws.crypto.signature.algorithm', 'SHA512');

-- If a Member is using something else than a Key to unlock their Account, the
-- CWS will use the following Password Based Encryption, PBE, algorithm to do
-- the trick. The provided information is extended with an instance specific
-- Salt, and a Member Account specific Salt to ensure that enough entropy is
-- available to create a strong enough Key to unlock the Private Key for the
-- Account.
INSERT INTO cws_settings (name, setting) VALUES ('cws.crypto.pbe.algorithm', 'PBE128');

-- For the CheckSums or Fingerprints we're generating - we just need a way
-- to ensure that the value is both identifiable. For Signatures, it is used
-- as part of the lookup to find a Signature in the Database and for stored
-- Data Objects, it is a simple mechanism to ensure the integrity of the
-- stored data.
INSERT INTO cws_settings (name, setting) VALUES ('cws.crypto.hash.algorithm', 'SHA512');

-- This is the System specific Salt, which will be applied whenever PBE is used
-- to unlock the Private Key of a Member Account. This Salt should be set during
-- installation, and never changed, as it will render *all* PBE based accounts
-- useless (kill-switch).
--   Note, that the value can be modified by the System Administrator via the
-- settings request as long as no other accounts exist. Once other accounts,
-- exists it cannot be altered anymore.
INSERT INTO cws_settings (name, setting) VALUES ('cws.system.salt', 'Default salt, also used as kill switch. Must be set in DB.');

-- For correctly dealing with Strings, it is important that the Locale is set to
-- ensure that it is done properly. By default the Locale is English (EN), but
-- if preferred, any other can be chosen. As long as they follow the IETF BCP 47
-- allowed values. See: https://en.wikipedia.org/wiki/IETF_language_tag
INSERT INTO cws_settings (name, setting) VALUES ('cws.system.locale', 'EN');

-- When applying armoring to the raw keys, it means using a Base64 encoding and
-- decoding. However, they have to be saved using a character set. Any character
-- set can be used, but if keys have been stored using one, changing it will
-- cause problems as they may not be read out safely again. So, please only
-- change this if you are really sure.
INSERT INTO cws_settings (name, setting) VALUES ('cws.system.charset', 'UTF-8');

-- The Administrator Account is a special Account in the CWS, it is not
-- permitted to be a member of any Circles, nor can it be used for anything else
-- than some system administrative tasks. Which is also why it should not appear
-- in the list of Members to be fetched or assigned to Circles. However, rather
-- than completely opting out on this, it may be a good idea to expose it. Hence
-- this new setting value. Default false, meaning that the Administrator Account
-- is not visible unless explicitly changed to true.
INSERT INTO cws_settings (name, setting) VALUES ('cws.expose.admin', 'false');

-- Exposing all Circles, means that it is possible for a member, other than the
-- System Administrator, to be able to view Circles who they are not having a
-- Trustee relationship with - If the value is set to true.
--   If the value is set to false, then it is only possible to extract a list of
-- Circles with whom the Member is having a Trustee relationship with.
INSERT INTO cws_settings (name, setting) VALUES ('cws.show.all.circles', 'true');

-- Privacy is important, however - there may be reasons to reduce the privacy
-- level, and allow that a Member can view information about other Members even
-- if there is no direct relation between the two. If two members share a
-- Circle, then they will automatically be able to view each other, but  if not,
-- then this setting apply. By default, it is set to True - as CWS should be
-- used by organizations or companies where all members already share
-- information.
INSERT INTO cws_settings (name, setting) VALUES ('cws.show.trustees', 'true');

-- Overtime, it can happen that the data is deteriorating. Meaning that some of
-- the bits can change and thus result in data which cannot be recovered as the
-- decryption will give a completely false Object back. When data is stored, it
-- is having a checksum of the encrypted bytes, which is also read out when the
-- data is requested. If the checksum fails, then it is not possible to recover
-- the original data anymore.
--   However, as most systems also use backups, it is possible to recover the
-- encrypted data from a backup, but the question is how far back the backup
-- has to go. To ensure that a backup is correct and that there is no problems
-- in the database, the sanity checks can be enabled at startup, meaning that
-- when CWS is started up, all encrypted data is checked and verified. If a
-- check fails - then the field is marked with a failed Sanity check, and the
-- date of the check.
INSERT INTO cws_settings (name, setting) VALUES ('cws.sanity.check.startup', 'true');

-- Please see the comment for the 'cws.sanity.check.startup', for the motivation
-- and reason for the sanity check. This setting sets the interval, at which the
-- sanity checks should be made. By default, it is set to 180 days but it can be
-- altered if needed.
INSERT INTO cws_settings (name, setting) VALUES ('cws.sanity.check.interval.days', '180');
