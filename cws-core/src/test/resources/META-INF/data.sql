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


-- Default Administrator User, it is set at the first request to the System, and
-- is thus needed for loads of tests. Remaining Accounts is for "member1" to
-- "member5", which is all used as part of the tests.
INSERT INTO cws_members (external_id, name, salt, pbe_algorithm, rsa_algorithm, public_key, private_key) VALUES
    ('d95a14e6-e1d1-424b-8834-16a79498f4d1', 'admin',   'd3cecd21-b1a1-48bf-8936-780157158a19', 'PBE128', 'RSA2048', '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'),
    ('073dcc8f-ffa6-4cda-8d61-09ba9441e78e', 'member1', '9cbed0bc-22c9-417f-8c6c-44fbaa0c0844', 'PBE128', 'RSA2048', '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'),
    ('d842fa67-5387-44e6-96e3-4e8a7ead4c8d', 'member2', '3157bb59-abe6-46a0-9d80-146afea7fd60', 'PBE128', 'RSA2048', 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkkW4UI6QDCdJRte3NhUWdSwBXXFb2OCdVIyjINUcpUtIjCBY5UhVEMoZFNhBVivklpX3tOevLmeNU8r6F99wrSs9g4JdGzHWY7SFyIIbM2UArD6JmJh6EwhK0rf3s8BAMcPLqQeZycNWg4sVzpU9CtzDCFk4n1vh5JvpVWN5uCo+XMMOXHWYrdWeYyRCg1tlJmGx3v5tDmAHkkmUYPU6LFeJXS8qlYlWOGGx5AUXyKX2uzo0RRnqIaa0wWpjxtJG500y9U5DeHmNT1EXC/54Oln7YiS5LGxhs9O9baAE24y5E2QtRPuA0hePXRjZE0SZuNUDlPvHdr4QuVqqr1HrIQIDAQAB', 'mhb4ttFnR827KlmrrztljiKesn6u6yQPpfExGC8Aw4KHblH4ncXZyZrAfnQmyC8pjEcB2xTbWN3V74LTXVHWyN/P0xTJ0G4I0yHkqCXYQ8EjHH2Vuaho6tjGgDWnYAYydmhz0fkcsAk5QGHvFc9plql0X+Cp1LdP8+Iz7eH0xzKcm3ldouLzZGpZTCRhHAwl1yhtvV8XDwnJKv43U2LLU7/ld6s0XsW72Ju5zkP5+HiGDcQamVC2NmOtqgNHdaF6J+h6h4ZAlHpRa7HtmpHKxp7m6HzgCgXilFNAK7yQ9DmmyVbRtG2/2gmPCZPMW9H7wZdeKzeOBwUZrJvNw6AVgZCaoAgF8M0kSUEFKsqRUwJkINcT83txy8bYKZlA1tE5TcQGTsp/fB6fT1PtL/W+3GSHDhqh65HCXuqbLLeFT2WuP9IwflHNe1GwtPVCryoP+3OM3PA9ntgF7R+o+cI7IYzGqGwKQjYFLxjf/yUwQhohfQBiefRLO3FTKfSx+4zbl5JIKF1LVYyqMxE/BBnmcaRs6xC6oblzsjdryF0CGacUV8fAKyaKj//b9Wcg5fuhwXTqw4D1AtW0xSP2D6HaxmMEtkGkWGiH1C+RufLLyl1hQoKEZ+6f2M+iSXhwpqSK1Pb8KWxMmQY3GOIWTXUtwV7Kh7WdBvR694y548S8msyPBv0Bob8qPcYg0/rqfsZR/EHJI+1UHKvEY/AQK4mktC7iaEsjjde1pqUjCLLQvmqOCoLxqII7g1tlwv1DEdCKlMIdd6joaO8LhuUZUyZ2Kge4+/9e/Odlm1hlv6H93gYMDIScCPUxVraDUQIX1sLola57JBBnmIh7YYmY2OeZUdvKLtvmeDGEhyll9DCx31wgyWfm3d72mD0Q/aB0VMIaYSiIsp9Isy9Up5ZJaoFzNkG6GL0sVlaIXvBgclyWQynQC+4b7qzrRObSlSLd/sjng6MgTRpdP4xeYFDZvvlJOR6bK7uAouobQNaUhgHai/fYlf3lBwKbLFxsW3474vwc5FuWqmHOVOy7y29Z1J4wt8+XKAH8d4NYsLvIO5vge4xwTCV3OAFtcknMjCVohHv1+3ZqTtJVLC0HfK1xtNGW/FIZdb9mEx6HrOvIntli8QuIyKRZVUZi3dGJ56NKdLgmMKn00FL4Vpq4dLWA8KsPxo+J6CQu5GxaMg82keanp3CtqgIW6Rx+VRANkedJmIqIXlZgYDpn9OpIaKQnX3FEa8xQR9mjCLv3DBtk/okmI0rCr5qddPQTDP2wvwsdy4EK3F1rlASY7Zo0j6XxJ6ZNHeIQu+V+8hzae19bErnkVBP8zL6gFiWOu2GrgcVtNWELQXdWtBPp2+vjbzVmmNttWptJ5uY9Ww1hk4L1SCQNTPxHj4P2nljUvkcQO2bfsG5oEdY4ddau9Mm1Vc30itL1+0IhAWT3cE+2+t4DUWfUJUOyYVK6S5yIAsCjda2fiZJuBHDBNo/u8zTZlzISNz/l+yNxzTMSOQAgOTfWHBcdy0TvswHCmOrlcAyETQ43glr7B5FNDhlw1e5LlSjoJe1Ym/e1cRywpAJTjZUd/9EYYnUoyYTChabvoZ+tAw7yr0EAFffD2654dNsLqChLRNMQ4vOhLL3PIRXcDfy5qgX+vTc='),
    ('f32c9422-b3e4-4b52-8d39-82c45f6e80a9', 'member3', '9bafe6ff-ecb1-463e-b248-eccde3a34984', 'PBE128', 'RSA2048', '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'),
    ('b629f009-4da2-46ed-91b8-aa9dec54814d', 'member4', '29d440b3-d861-46d9-a0b2-9a8f76f382b6', 'PBE128', 'RSA2048', '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'),
    ('63cb90cc-c1fb-4c6a-b881-bec278b4e232', 'member5', '63a5932f-8c1a-45ef-a9b1-a05ace706056', 'PBE128', 'RSA2048', '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');

-- Default, we have 3 Circles as part of the test setup, using the very
-- imaginative names, 'circle1' to 'circle3'.

INSERT INTO cws_circles (external_id, name) VALUES
    ('d8838d7d-71e7-433d-8790-af7c080e9de9', 'circle1'),
    ('8ba34e12-8830-4a1f-9681-b689cad52009', 'circle2'),
    ('a2797176-a5b9-4dc9-867b-8c5c1bb3a9f9', 'circle3');

-- For each Circle, we need to have a unique Key, but with the same settings.
INSERT INTO cws_keys (algorithm, status) VALUES
    ('AES128', 'ACTIVE'),
    ('AES128', 'ACTIVE'),
    ('AES128', 'ACTIVE');

-- For each Circle, we need to have root folder for all data.
INSERT INTO cws_metadata (external_id, parent_id, circle_id, datatype_id, name) VALUES
    ('3568d3cf-d7ae-4aba-bf21-ef50fe6020e8', 0, 1, 1, '/'),
    ('e72ee11d-6268-44d8-a6cf-3165f4531dbf', 0, 2, 1, '/'),
    ('32cda78d-0016-47ea-a0a6-da05f87c26f3', 0, 3, 1, '/');

-- With the Members created, and the Circles and Keys added, it is possible to
-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,
-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.
-- The Trust Level is different for each Member.
INSERT INTO cws_trustees (member_id, circle_id, key_id, trust_level, circle_key) VALUES
    (2, 1, 1, 'ADMIN', 'Ov9nn3NWCQDYrFzvuD8wOtG7LH90sr8D24PRafKEY/jPRNQwYSE7m+vEq4NvIjmjAdq0bTG48fir+eaxD+Dxm8cQSxkv+nmsNWks6N0g42sv94v+/blNB9V6ocau1FrZn42C65Pe+AR0REFa6OKxAYpovz7cKpIsqNF4+5OAFybDC/HitVdIWtdYnl3Xb5UiUKR2TtgxkpNn/bw72ogDVxnscO49YAFq6GtGACYMzWG4+0pU/A/TQ0MQNGPk4ueTK5as5WT1uURhQ8UIt1IXwzHhUgpTAEM0kcTxCFCnivhdQcoEPHoDXv0MmmmrN94I7A/L+WEGhjF+ltA2bPLmbA=='),
    (3, 1, 1, 'WRITE', 'cQmT0OUntmlLNHzcInyV2uLKIUf72HtF7s+ts5bJ9UgoIM7gyC1gTaQ+Gw/cfOH1fLpMAiezlNuK1+fwuonIBZtF8njEdITA6WpesR8NQTFSM2ys85Dmue2faD8nUyp55y4D2PZQAAnzWhmwvo43OXwIUrbZbJglSJUqryt+mxdacVO42pn6rdolTbJg+9gTBc9yRoZFdxlWRjMdka5VA28mhopyVFCc3X5tD7bTx+5IhQJibIyjnu2MvA88Vv2csLkJUpVa0jNQQg0l5BUd/ug71r2vvo9IJw3Z3+jZGYB4vAxbtngsBKtaO9EbSHhUE1ZXZSqUmR4GiMac5te7IQ=='),
    (4, 1, 1, 'READ',  'wegsUSXTt7YMrbL6Oj6AEt75ftAICK4bKqVYZDNap5SMkgVm4fARVYDTLg2ucuOeyqk+WFY4KP860xk0QXSrDvcmkwkH889h2Ye8+VI5uqBnt4ihqFCfHAYmzAJ0r45EDCtNl2PivoKGx+XlK1cVtPLSwrefIWoF+rwhZtPjEfeit753iRnDQrcbZtIpOW8I7uNyw600e5sbZmSgFB10sdxxRM6RqhM3rxmLe3KwgSn/HpBuoVChgbSLXRlwIvd3nW3R8GExI7nv7arEevAmLuVJfp+KIK4/RH/V3DKBvxrjPGx1Fgmvf41LqMbpqsObAAc0H6QXGfXf20MII2CojQ=='),
    (2, 2, 2, 'ADMIN', 'VBzlRPln4T4XyH7yAcY9dteMYDDe9q+8iYaFFMC/MgILMwXi1t+M82Akgnz/nCw2rYZapHCkCC/07RC5YnIzQpQerSeX2rYlwgAju2NvjbeZkTeYMM9KgSK4dtzG1IYB63/2qETcnvxcT/bKNzNqIDZCt10zmAkDloZTfFHmV/FV3XORJ3nJKe5iCrxsa6of/yOzDj3YmyfwqGkY0QFH67dZJR4MIhrMJfCno0j7VnDZ0y3I6KIinqzJUOEQUeK0FEa3lAR1Kcj8PcdqYffEg2EZ4NzraAwDYZDtEOVRyGxLlUcEOS68Unypw0boKgXWl0T/OhA7HGW6L17ZqNLKRg=='),
    (3, 2, 2, 'WRITE', 'XHy52WvdsBgCd0UswNNQbzeGplZz0eB9IwR6Zw+U4shEdXAWfECoBGm7gb4JFqFBQebzUo1p7TWjHiWLl8yNoQGrUe8klZ7oRF2kMhW4YvcfGynan2Qtu48MAEXjQtGqDPou3m29XQ7hNon73snTpAr/Zsr3DpCVVIxrZ/dmOkWPGOSBvvftxKym+DiAiiK2zGf1OzMHWVrXD4kJQTZjzTMSFIVxZo6XwyZJWjI2dXETW1gUF83HjlFq5FZEQlFgAk9pLDPJ57PX91vM2qlSgXjzbQKn3C/IGqiBhzFSnHe2zhzGrNRvXzzr6OMoxHEyStXFtxPFxsotUt69h3GcbA=='),
    (4, 2, 2, 'READ',  'i8LFRPZblcVlkx6jDV7I0x0wUqeojaiGNC1bM8169dC1rrEqfw2rWQeD2J0xNlvsfW6e+R2AHAXu+QNZ3vz6JknsYwGuVOloXq+WUluPrtyR4WZ9gsBSlmSYtbAbgQrm/rJXOZj+JnP69MUQjtLgKakUm2jOY8k/m4gTGLAu1M3f3JnDN2Bvg1yBmwaVw/vWfWW7XIYNzXcp7VftVLEzQ2f60x4PjEuXx6aoLwxSw9tMu31nS5+VTP8dATH4VXtjFh2q4WwhQ/LsFoq8aPbFSRBUo1s7fDtoyJOp2fSN9zsiWjbQW2gBsxnL0hM9rONPlfw9ruRZz9OHG3zH1O00MA=='),
    (5, 2, 2, 'ADMIN', 'O1gqLrhQkQomPaIZdyRIIe5Y3UFYnitG0D4/rMaVJsKFO/WBVVZARMzpJhFx3ZDIAOzxYyc0an6pgADmE+7aIA4X+uWg/8YuHXIv7bF8RQbQuQeCVZc5WWt98lBPEQyXbzhJjsBswZyByhszICHE1AQruZUF5XhYXEYJTM+xfxlbktvkvuYpr3aDnVVGYx8fhn7J2gfOPAZ0O0VtB7tBmVP8G62zcAUixZ/lzCB5befSD9Hujr9g7MGjie7ftYaQNVTNgUVSDxdt/Y1SPy0E7ll85ui5TnEWuis8YUXNkTzm0QaXol4tKe2b/Guv0h5H06ymbmdXVm2+O3UaVKBiiQ=='),
    (3, 3, 3, 'WRITE', 'JWdfqJuAJAnN04hguhInnumq1bQ/kHNOpCuDpyZvGBmiNFsxT/wYQwdg2/AYql+ZaNiaIQm852vlAC2qpElRR3ZzvWf9CW7ax0aZNvlZqnD1Q9P5gEEB8TmJav6vaOmw30bIXGjY5WyC74QwkMmbL1Ca5eLiYJrJSW3USfvfzcxU0Eow/Fug61ntn++2T52aH5sSFMIqxUWtYtD9U8222un0cxYYwtAFPjEFrirt3RrOw6YNxyz/QRxA0kctbGLueFZMp1xfYuvrVDWc3rXPYzoMahL8p+w93OYmcJcbVQpgAp9mw8s4K9uNS1OXXbRf8Y87tpm+ocuXQ2N3MO7ozw=='),
    (4, 3, 3, 'WRITE', 'D6ZoFwk2xOiWYeVY3R8jHSvrNMVI3bzt/2055T/hKn1hRPPeYLLhUIAsv1MpeQAY5S7EHouqjDDTJBpDpCLEz5OBsvUzYaPmF+o53rgxKHYLTv0pINX5nY/HOGKfHHmbL7X2e8/TXLQfAAg5YWm0d/EBu1+sioZdOZGnfos2p5Tkj4OGclqfG4gGpMAGgvRx2ptCr1Oo1ht52ioj06n7rIATm/8TaaJobk+CmAiaU3PIelYt6XvnfjjZ1YGaJ9Omhpn+50W60XXY4fokD+pk8ZOK08fcXadDiRj1kzDKTKoe8mFXWJP7ndBUmfkz5Bak+9CvJxDwoGVOsLEhpxSgxg=='),
    (5, 3, 3, 'ADMIN', 'LrsTIOcs2X5jdMkQ5boOyXGhQ7qVZ3qC0U7nQ06XIdQWeXulw6+/dj/paWzep30Ncw13/8N2ZgfeI1oQQYfOQ54InXRMQ/SeTS/PhDlkKpEtpmUjFBBg/DgdQsRoyt9n6trzHsdEbxEkrjqmrjzCFGoz/s1g427t4ilv/ZcMfKRw09WnRRVscx5jI8yGNJ8wGCzKrGQTi2lFaTOGNoVhhRIlO1CejmQDOyxCMbjGH5maTYOKkEQ/iYRLGWx0fs7lWYQ89McD9hvdhWqwgh6V/I1Z10BuUpf7OtZ1hKOL0sb4orWyLbACI5y9SSYoF42u/oNapjhF7pc0KMhAubtDBg=='),
    (6, 3, 3, 'READ',  's3vPgkw6PmVT2k2Meys+mXg81ddqy6GiFTejhoCBmqMNpcmoDQJ3UJgRw0++9PztnJFUVEsihdFdm7fmBrM/s68arWb369lbmo9rDlVKPi6eTrUQpxWOF4nW/d8Id9JRUF19S+p3KN7mVzv14CJYuikTfJSLgqlLNkWYYli6OivZT2g0sjnf4tlZrut0LMr8yH9wJyLdCJzwYtQUaVvtqTl0zYZAjbKbMR4ro1FHrilSv063d1cBkKX9xC06w5MWDjJCOeysgTmyqhkZYHDOlgESkWPHN95wlzUNKmLJ5TLWr1xArW2Qbsr8rc+ebpOsWfZu44KsCERmnxsZdWJGyw==');
