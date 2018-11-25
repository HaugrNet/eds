-- =============================================================================
-- PostgreSQL Setup Script for CWS
-- -----------------------------------------------------------------------------
-- This script contain the following tables:
--  * System Specific tables
--     o version    -  Simple Version information, to correlate CWS & DB version
--     o settings   -  Settings for this instance of CWS
--  * Accounts & Relations for access control
--     o members    -  Members who may access CWS
--     o circles    -  Circles of Trust that members may use for sharing data
--     o keys       -  Symmetric Keys, used by Circles for sharing data
--     o trustees   -  Relation information for each Member & Circle
--  * Folders & Shared (encrypted) data
--     o types      -  The type of Objects (Data) to be stored
--     o metadata   -  Objects to be stored, related to datatype & Circles
--     o data       -  Storage of the actual encrypted data for each Object
--     o signatures -  Cryptographic Signature References information
-- -----------------------------------------------------------------------------
-- Note; this is Script is the default to work with, however JPA create scripts
--       referenced in the persistence.xml file must have each create statement
--       on a single line.
--         So although it is tedious to have several versions of a Script, it is
--       needed in this case, since we otherwise get JDBC Exceptions!
-- =============================================================================

-- =============================================================================
-- The Version table is used to correlate the information about the current
-- database version with the CWS version. If not matching, then CWS will not
-- work correctly.
--   The table also serves a simple purpose of acting as a break for attempts at
-- accidentally trying to upgrade the CWS to an invalid version.
--   The API request for "version", will also return the latest information from
-- this table.
-- =============================================================================
CREATE TABLE cws_versions (
  id               SERIAL,
  schema_version   INTEGER,
  cws_version      VARCHAR(10),
  db_vendor        VARCHAR(25),
  installed        TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT version_pk                     PRIMARY KEY (id),

   /* Unique Constraints */
  CONSTRAINT version_unique_version         UNIQUE (schema_version, cws_version),

  /* Not Null Constraints */
  CONSTRAINT version_notnull_id             CHECK (id IS NOT NULL),
  CONSTRAINT version_notnull_db_version     CHECK (schema_version IS NOT NULL),
  CONSTRAINT version_notnull_cws_version    CHECK (cws_version IS NOT NULL),
  CONSTRAINT version_notnull_db_vendor      CHECK (db_vendor IS NOT NULL),
  CONSTRAINT version_notnull_installed      CHECK (installed IS NOT NULL)
);
-- Initial Database Version is 1, initial Production CWS release is 1.0.0
INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (1, '1.0.0', 'PostgreSQL');
-- First feature release, CWS 1.1.x results requires an update of the DB
INSERT INTO cws_versions(schema_version, cws_version, db_vendor) VALUES (2, '1.1.0', 'PostgreSQL');

-- =============================================================================
-- The CWS is configured via a set of property values, which are all stored in
-- the database. This is done so the same cluster of stateless CWS instances
-- will share the same settings.
--   The Settings consists of a series of key-value pairs, though to avoid any
-- potential conflicts with the word "key" that is also used throughout the
-- system to mean "Cryptography Key" - the are instead referred to as
-- name-setting pairs, though the end result is the same.
--   The Settings are managed via a System Administrator API request, which will
-- only be used as part of the setup. Certain values cannot be altered after the
-- initial setup, however which values and why is described in the installation
-- documentation.
-- =============================================================================
CREATE TABLE cws_settings (
  id               SERIAL,
  name             VARCHAR(256),
  setting          VARCHAR(256),
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT setting_pk                     PRIMARY KEY (id),

  /* Unique Constraints */
  CONSTRAINT setting_unique_name            UNIQUE (name),

  /* Not Null Constraints */
  CONSTRAINT setting_notnull_id             CHECK (id IS NOT NULL),
  CONSTRAINT setting_notnull_name           CHECK (name IS NOT NULL),
  CONSTRAINT setting_notnull_altered        CHECK (altered IS NOT NULL),
  CONSTRAINT setting_notnull_added          CHECK (added IS NOT NULL)
);

-- =============================================================================
-- This is the Account table for all Members. By default it is empty, and the
-- first record to be added must be the System Administrator, called "admin".
--   The Account have a credential, which must be a unique (lower case)
-- identifier for the Account Holder or Member. The Account also holds a System
-- generated Public/Private Key pair, where the Public Key is stored directly
-- but the Private Key is stored encrypted using a temporary Symmetric Key that
-- is derived from the request.
--   To Authenticate the Member, the temporary Symmetric Key is used to decrypt
-- the Private Key, and if it can successfully unlock a secret encrypted with
-- the Public Key, then it matches. If not, then the System will Warn the
-- Member, that the Account was used incorrectly.
--   The System Administrator, "admin", must be the first Member Account added,
-- and upon creating it, the provided secret (Passphrase or Private Key), is
-- then used to derive a Symmetric Key to encrypt the Private Key. This way,
-- there is no default secret for the System Administrator to be altered.
--   Note, that the first version of the CWS is not holding any mechanisms to
-- prevent Authentication Attacks. This is a deliberate design decision as the
-- CWS is only suppose to be a Data Storage Facility, and this be further down
-- the entire Application Layer.
--   The Member Role is giving a pointer towards what kinds of permissions that
-- a member has, it can be either admin, standard, session or guest.
--   Sessions are important for websites, where a user is logging in, and then
-- just works with a session onwards. To better integrate CWS into websites, it
-- is important CWS also supports Sessions. This is done by having 3 fields in
-- this table, the session_checksum, which is generated using the MasterKey, and
-- used to find the member account. Secondly, the member's private key encrypted
-- using both a PBKDF2 symmetric key and the MasterKey. And finally, the expire
-- flag, which is pre-calculated with the "login time" and the maximum life time
-- of a session. So a Timed service can stop any existing sessions which have
-- not been logged out.
-- =============================================================================
CREATE TABLE cws_members (
  id               SERIAL,
  external_id      VARCHAR(36),
  name             VARCHAR(75),    -- Member Authentication information
  salt             VARCHAR(256),
  pbe_algorithm    VARCHAR(10) DEFAULT 'PBE_256',
  rsa_algorithm    VARCHAR(10) DEFAULT 'RSA_2048',
  external_key     TEXT,           -- External Public Key, with unknown length
  public_key       VARCHAR(3072),  -- Public Key, stored armored
  private_key      VARCHAR(16384), -- Private Key, stored encrypted & armored
  member_role      VARCHAR(10) DEFAULT 'STANDARD',
  session_checksum VARCHAR(256),   -- MasterKey Checksum of the given Session
  session_crypto   VARCHAR(16384), -- Private Key, stored encrypted & armored
  session_expire   TIMESTAMP,      -- Time, when the Session expires
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT member_pk                      PRIMARY KEY (id),

  /* Unique Constraints */
  CONSTRAINT member_unique_external_id      UNIQUE (external_id),
  CONSTRAINT member_unique_name             UNIQUE (name),
  CONSTRAINT member_unique_salt             UNIQUE (salt),
  CONSTRAINT member_unique_session_checksum UNIQUE (session_checksum),

  /* Not Null Constraints */
  CONSTRAINT member_notnull_id              CHECK (id IS NOT NULL),
  CONSTRAINT member_notnull_external_id     CHECK (external_id IS NOT NULL),
  CONSTRAINT member_notnull_name            CHECK (name IS NOT NULL),
  CONSTRAINT member_notnull_salt            CHECK (salt IS NOT NULL),
  CONSTRAINT member_notnull_pbe_algorithm   CHECK (pbe_algorithm IS NOT NULL),
  CONSTRAINT member_notnull_rsa_algorithm   CHECK (rsa_algorithm IS NOT NULL),
  CONSTRAINT member_notnull_public_key      CHECK (public_key IS NOT NULL),
  CONSTRAINT member_notnull_private_key     CHECK (private_key IS NOT NULL),
  CONSTRAINT member_notnull_role            CHECK (member_role IS NOT NULL),
  CONSTRAINT member_notnull_altered         CHECK (altered IS NOT NULL),
  CONSTRAINT member_notnull_added           CHECK (added IS NOT NULL)
);
CREATE INDEX cws_members_name_index ON cws_members (lower(name));

-- =============================================================================
-- Circles act as groupings for a collection of Members sharing data. A Circle
-- is thus having an identifying name and nothing else really.
--   Circles can be created by either the System Administrator or a Member with
-- Circle Administrative privileges. When creating a new Circle the initial new
-- Circle Administrator must be added, which cannot be the System Administrator.
-- =============================================================================
CREATE TABLE cws_circles (
  id               SERIAL,
  external_id      VARCHAR(36),
  name             VARCHAR(75),
  external_key     BYTEA,          -- External Circle Key, with unknown length
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT circle_pk                      PRIMARY KEY (id),

  /* Unique Constraints */
  CONSTRAINT circle_unique_external_id      UNIQUE (external_id),
  CONSTRAINT circle_unique_name             UNIQUE (name),

  /* Not Null Constraints */
  CONSTRAINT circle_notnull_id              CHECK (id IS NOT NULL),
  CONSTRAINT circle_notnull_external_id     CHECK (external_id IS NOT NULL),
  CONSTRAINT circle_notnull_name            CHECK (name IS NOT NULL),
  CONSTRAINT circle_notnull_altered         CHECK (altered IS NOT NULL),
  CONSTRAINT circle_notnull_added           CHECK (added IS NOT NULL)
);
CREATE INDEX cws_circles_name_index ON cws_circles (lower(name));

-- =============================================================================
-- A central part of the CWS is encryption. All data is encrypted using a series
-- of Symmetric Encryption Keys. All saved Symmetric Keys are stored encrypted,
-- however, it is still important to have certain meta-data available.
--   To generate a new Key, it is important to know what algorithm and the Key
-- length to use. There is some restrictions in the standard Java distribution
-- from Oracle and other Vendors, which means that they by default only provide
-- a limited strength encryption. The default value for the CWS, is only using
-- the limited Key lengths, if the Unlimited Key Strength library has been
-- applied, then this can be altered. But be aware, that changing the Key length
-- will both increase security, but also increase computation time. It should
-- be noted, that if Keys used has been generated using the Unlimited Strength,
-- then the underlying Java must keep using this, otherwise it will produce
-- errors. The Key size is a System Setting, and is only used when generating a
-- new key, it is therefore not stored here.
--   Symmetric Key's uses block encryption, which means that the size of the
-- data to both encrypt and decrypt must have a size that fits the block size
-- exactly. Since this is seldom the case, it is normal to apply a padding,
-- which tells how it should deal with data where the size differs from the
-- block size.
--   When using Block based encryption, the blocks can be encrypted in different
-- ways, they can be encrypted directly or with some additional information to
-- improve the strength of the encryption. By default, CWS uses continued Block
-- based encryption, CBC as Cipher Algorithm Mode. Other modes are supported by
-- Java, but unless you really know what you're doing, this should be fine.
--   When applying CBC (and other continues Block Modes), the result from one
-- block is used as the initial data to encrypt the next block. However, the
-- first block is not having any existing data to use, hence to ensure that the
-- result from an encryption is always consist - an Initial Vector is applied.
--   The Symmetric Keys have a life within CWS, and although encryption can be
-- broken, even the smallest AES keys will theoretically take years to break.
-- However, trust may not always be applicable (otherwise, why use CWS), so if
-- desired, keys can also expire. If this is desired, then new Keys must be
-- generated and data must be re-encrypted. which can be a length process.
-- During this process, data will be stored twice, one with the old key, where
-- this has been marked 'Deprecated', and of course also with the new Key, which
-- is now the default 'Active' Key. The Status tells which it is.
--   If Key's can expire, then they must also be given an expiration date. This
-- date is set when the Key is first generated. And once expired, it should no
-- longer be used. But, to ensure that the data is also re-encrypted with the
-- new Key, the CWS will allow that this can be done within a period of days
-- known as a "Grace Period". Any Circle Member with the required Trust Level
-- who accesses the CWS within the Grace Period will initiate a re-encryption
-- process. If nobody have logged in within the Grace Period, then the CWS will
-- simply refuse to process the data.
-- =============================================================================
CREATE TABLE cws_keys (
  id               SERIAL,
  algorithm        VARCHAR(256) DEFAULT 'AES_CBC_256',
  status           VARCHAR(256) DEFAULT 'ACTIVE',
  expires          TIMESTAMP,
  grace_period     INTEGER,
  altered          TIMESTAMP   DEFAULT now(),
  added            TIMESTAMP   DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT key_pk                         PRIMARY KEY (id),

  /* Not Null Constraints */
  CONSTRAINT key_notnull_id                 CHECK (id IS NOT NULL),
  CONSTRAINT key_notnull_algorithm          CHECK (algorithm IS NOT NULL),
  CONSTRAINT key_notnull_status             CHECK (status IS NOT NULL),
  CONSTRAINT key_notnull_altered            CHECK (altered IS NOT NULL),
  CONSTRAINT key_notnull_added              CHECK (added IS NOT NULL)
);

-- =============================================================================
-- The Trustee is the relation between Member Accounts and Circles. The Key
-- which belongs to a Circle is stored with this relation encrypted with the
-- Member's Public Key.
--   A Member is also having a Trust Level, which determines what the Member may
-- or may not do within a Circle:
--     * Guest
--         This is the lowest level, a Guest may view the meta information from a
--       Circle, which includes Members, Folders & Object Meta information.
--       However, Guests do not have a copy of the Circle Key available, meaning
--       that they cannot read the data belonging to a Circle.
--     * Read
--         With this level of access, the Member may do what the Guest can do,
--       but contrary to a Guest, they do have access to the Circle Key, and may
--       therefore also read all the Objects belonging to a Circle
--     * Write
--         With this level of access, the Member may do all that Members with
--       Read rights may do, and additionally they may also create, update and
--       delete Folders and Objects belonging to the Circle.
--         Members with Write access, may also initiate a Key migration process,
--       meaning that if a Key has expired, they may issue a new Key and start
--       re-encrypting all content.
--     * Administrator
--         Administrators have the highest trust level, meaning that they may do
--       all with the Circle that Members with lower level of access may. They
--       may also manage the Circle itself, meaning adding or revoking Members
--       access to the Circle and change the Trust Level of a Member.
--   A Member can only belong to a Circle with Access to the Key. the actual Key
-- is stored with the relation and not within the Key table. For Guests, the Key
-- stored is simply "Not Applicable".
-- =============================================================================
CREATE TABLE cws_trustees (
  id               SERIAL,
  member_id        INTEGER,
  circle_id        INTEGER,
  key_id           INTEGER,
  trust_level      VARCHAR(10),
  circle_key       VARCHAR(8192) DEFAULT 'Not Applicable',
  altered          TIMESTAMP     DEFAULT now(),
  added            TIMESTAMP     DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT trustee_pk                     PRIMARY KEY (id),
  CONSTRAINT trustee_member_fk              FOREIGN KEY (member_id) REFERENCES cws_members (id) ON DELETE CASCADE,
  CONSTRAINT trustee_circle_fk              FOREIGN KEY (circle_id) REFERENCES cws_circles (id) ON DELETE CASCADE,
  CONSTRAINT trustee_key_fk                 FOREIGN KEY (key_id) REFERENCES cws_keys (id),

  /* Unique Constraints */
  CONSTRAINT trustee_unique_fks             UNIQUE (member_id, circle_id, key_id),

  /* Not Null Constraints */
  CONSTRAINT trustee_notnull_id             CHECK (id IS NOT NULL),
  CONSTRAINT trustee_notnull_member_id      CHECK (member_id IS NOT NULL),
  CONSTRAINT trustee_notnull_circle_id      CHECK (circle_id IS NOT NULL),
  CONSTRAINT trustee_notnull_key_id         CHECK (key_id IS NOT NULL),
  CONSTRAINT trustee_notnull_trust_level    CHECK (trust_level IS NOT NULL),
  CONSTRAINT trustee_notnull_circle_key     CHECK (circle_key IS NOT NULL),
  CONSTRAINT trustee_notnull_altered        CHECK (altered IS NOT NULL),
  CONSTRAINT trustee_notnull_added          CHECK (added IS NOT NULL)
);

-- =============================================================================
-- Data stored is completely unknown to the CWS, since multiple Clients may
-- access the system, it cannot be guaranteed that they all know what the
-- Objects may be. To avoid this problem, and to ensure that there is a simple
-- way to map Objects over, they must be stored with a type. All known types are
-- stored in this table.
--   The types is stored as a map. Where the keys (names) must be unique, and
-- the values can be anything. By default, one record exists, which is a folder,
-- the folder type is used primarily internally to help structurize the Objects
-- better.
-- =============================================================================
CREATE TABLE cws_datatypes (
  id               SERIAL,
  datatype_name    VARCHAR(75),
  datatype_value   VARCHAR(256),
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT datatype_pk                    PRIMARY KEY (id),

  /* Unique Constraints */
  CONSTRAINT datatype_unique_name           UNIQUE (datatype_name),

  /* Not Null Constraints */
  CONSTRAINT datatype_notnull_id            CHECK (id IS NOT NULL),
  CONSTRAINT datatype_notnull_type_name     CHECK (datatype_name IS NOT NULL),
  CONSTRAINT datatype_notnull_type_value    CHECK (datatype_value IS NOT NULL),
  CONSTRAINT datatype_notnull_altered       CHECK (altered IS NOT NULL),
  CONSTRAINT datatype_notnull_added         CHECK (added IS NOT NULL)
);

-- Default, we have 1 Object Type, which is the folder. The rest is left to
-- the initial setup to create
INSERT INTO cws_datatypes (datatype_name, datatype_value) VALUES
    ('folder', 'Folder'),
    ('data', 'Data Object');

-- =============================================================================
-- The main objective of CWS, is to store data securely. This table, contain the
-- primary metadata for each Object stored, but not the data for the Object.
-- This data is stored in the data table. It was done so, for two reasons, first
-- as not all Objects may have data associated, and secondly to avoid references
-- to Null data, as it usually indicates flaws in the model.
--   Objects are stored with an external Id, which is exposed via the API, the
-- internal Id is not exposed, but is kept for internal referencing.
-- All Objects are also stored with a type. CWS has two default types, the most
-- important is "Folder", which allows Members to structure their data, and the
-- second is simply "data", if the system using CWS has enough information to
-- control the Data Objects.
--   Folders are also Objects referencing the default 'Folder' type. It is
-- possible to create sub-folders and add data to any Folder belonging to the
-- Circle in question. As it is possible to create Sub-folders, a restriction
-- has been added on the table as any Object may reference a parent, which is
-- that the parent Id cannot be larger than the current Id. This way, it is
-- possible to have a recursive structure, but not to create loops.
--   Unfortunately, having the parent Id as an Integer and not a String will
-- require additional lookup's, but it is a small price to pay to have a
-- guarantee that no loops can occur in the model.
--   The structure is created as a recursive data structure, where the parentId
-- is referencing a parent Folder, however - there is a check added, so it is
-- not possible to create looping structures, since the Id must always be
-- smaller than the current Id.
-- =============================================================================
CREATE TABLE cws_metadata (
  id               SERIAL,
  external_id      VARCHAR(36),
  parent_id        INTEGER,
  circle_id        INTEGER,
  datatype_id      INTEGER,
  name             VARCHAR(75),
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT metadata_pk                    PRIMARY KEY (id),
  CONSTRAINT metadata_circle_fk             FOREIGN KEY (circle_id) REFERENCES cws_circles (id) ON DELETE CASCADE,
  CONSTRAINT metadata_datatype_fk           FOREIGN KEY (datatype_id) REFERENCES cws_datatypes (id),

  /* Unique Constraints */
  CONSTRAINT metadata_unique_external_id    UNIQUE (external_id),

  /* Other Constraints */
  CONSTRAINT metadata_notnull_id            CHECK (id IS NOT NULL),
  CONSTRAINT metadata_notnull_external_id   CHECK (external_id IS NOT NULL),
  CONSTRAINT metadata_notafter_parent_id    CHECK (parent_id < id),
  CONSTRAINT metadata_notnull_circle_id     CHECK (circle_id IS NOT NULL),
  CONSTRAINT metadata_notnull_type_id       CHECK (datatype_id IS NOT NULL),
  CONSTRAINT metadata_notnull_altered       CHECK (altered IS NOT NULL),
  CONSTRAINT metadata_notnull_added         CHECK (added IS NOT NULL)
);

-- =============================================================================
-- Metadata with data associated have the data & key information stored in this
-- table, with a reference to the Metadata record. Objects which do not have any
-- data associated will also not have a record present here.
--   All data stored here, is stored encrypted. If the Key has expired, then the
-- Data is considered deprecated and it is pending deletion and can no longer be
-- used.
-- =============================================================================
CREATE TABLE cws_data (
  id               SERIAL,
  metadata_id      INTEGER,
  key_id           INTEGER,
  encrypted_data   BYTEA,
  initial_vector   VARCHAR(256), -- Storing it armored
  checksum         VARCHAR(256),
  sanity_status    VARCHAR(256) DEFAULT 'Ok',
  sanity_checked   TIMESTAMP DEFAULT now(),
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT data_pk                        PRIMARY KEY (id),
  CONSTRAINT data_metadata_fk               FOREIGN KEY (metadata_id) REFERENCES cws_metadata (id) ON DELETE CASCADE,
  CONSTRAINT data_key_fk                    FOREIGN KEY (key_id) REFERENCES cws_keys (id) ON DELETE CASCADE,

  /* Not Null Constraints */
  CONSTRAINT data_notnull_id                CHECK (id IS NOT NULL),
  CONSTRAINT data_notnull_metadata_id       CHECK (metadata_id IS NOT NULL),
  CONSTRAINT data_notnull_key_id            CHECK (key_id IS NOT NULL),
  CONSTRAINT data_notnull_data              CHECK (encrypted_data IS NOT NULL),
  CONSTRAINT data_notnull_initial_vector    CHECK (initial_vector IS NOT NULL),
  CONSTRAINT data_notnull_checksum          CHECK (checksum IS NOT NULL),
  CONSTRAINT data_notnull_sanity_status     CHECK (sanity_status IS NOT NULL),
  CONSTRAINT data_notnull_sanity_checked    CHECK (sanity_checked IS NOT NULL),
  CONSTRAINT data_notnull_altered           CHECK (altered IS NOT NULL),
  CONSTRAINT data_notnull_added             CHECK (added IS NOT NULL)
);

-- =============================================================================
-- CWS also supports signatures, and part of the information for a Signature, is
-- stored in this table. Complete with number of verifications and expiration of
-- the signature.
-- =============================================================================
CREATE TABLE cws_signatures (
  id               SERIAL,
  public_key       VARCHAR(3072), -- Public Key, stored armored
  checksum         VARCHAR(256),
  member_id        INTEGER,
  expires          TIMESTAMP,
  verifications    INTEGER DEFAULT 0,
  altered          TIMESTAMP DEFAULT now(),
  added            TIMESTAMP DEFAULT now(),

  /* Primary & Foreign Keys */
  CONSTRAINT signature_pk                   PRIMARY KEY (id),
  CONSTRAINT signature_member_fk            FOREIGN KEY (member_id) REFERENCES cws_members (id) ON DELETE CASCADE,

  /* Unique Constraints */
  CONSTRAINT signarure_unique_checksum      UNIQUE (checksum),

  /* Not Null Constraints */
  CONSTRAINT signarure_notnull_id           CHECK (id IS NOT NULL),
  CONSTRAINT signature_notnull_public_key   CHECK (public_key IS NOT NULL),
  CONSTRAINT signature_notnull_checksum     CHECK (checksum IS NOT NULL),
  CONSTRAINT signarure_notnull_member_id    CHECK (member_id IS NOT NULL),
  CONSTRAINT signarure_notnull_altered      CHECK (altered IS NOT NULL),
  CONSTRAINT signarure_notnull_added        CHECK (added IS NOT NULL)
);
