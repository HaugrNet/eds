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
INSERT INTO settings (name, setting, modifiable) VALUES ('cws.crypto.signature.algorithm', 'SHA512', false);

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
-- useless (kill-switch).
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
    ('d95a14e6-e1d1-424b-8834-16a79498f4d1', 'admin',   '2457709a-9325-455a-bc55-2511525c563f', 'RSA2048', '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'),
    ('7d18a5c6-1720-477f-8a40-7a4fe4ab6951', 'member1', '810d5251-cb59-45ee-9880-a95126f0424c', 'RSA2048', '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'),
    ('d0211634-2324-4746-baff-1f789ac460db', 'member2', '1fd45485-12f1-401b-b210-c16c5b229cc4', 'RSA2048', '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'),
    ('43301e25-8286-4b9a-bae0-46d2c658bc19', 'member3', '02a51b2f-f802-420a-b4b7-a1bdb8966286', 'RSA2048', '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'),
    ('f33782fe-3db5-470a-ba2a-e0bce0a0973e', 'member4', '28d02d7a-660b-44cd-b604-2d36ed06ca61', 'RSA2048', 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq38jw3erxfVdxtxKJ7sUPVgNrMecw1mW1GCJy/Zz/PPJlxqvTrt7ea4xDsjoP4kRreUwC6MjX9GGJIXy7W7ZzGdU24ztcc4dBVWply1kY7tT61KlQhnh+KbYTIBQ+ZVaTh/nwxH7EkRTHp08fVwWbTz43alJbJ75kkM+tRQvMUkSMp/urGGd9esA99nRDPJzqBbthdMYZE46gBXegjUNR8/ivoavKcmJEswXQ7U3Tdq4tPDNk3GeY8ohlyf1TrMJf2YPn3aci+q5sLrmLOd/ECT26FUOqYEYFlV2BYyeeoN7M1BuMC8ybvGsMQHrNHwEfVe8RpASaBTShVj+HQ0qIQIDAQAB', 'em1uUVPNR0ywQc/ondm1rZpKgVKjuEVb0V0O47gW1pCsj0UrJPkPPh5kHDk+NkWeX/CjQXCdWOFWOKBtwjnJC1lA1CfEaK1Unn/OlH1v2O3eDUd5kiNeTx6xadQwLANLuIp0lY2w1jhdYJN216pr5xZkTOZXnsh/XK3+ExYWh/X5MuSHugBiuwkYIrOjLOs4i1iOxP386lW5pAQZzJ2L9ljCkyrxQ8YnAxJmW2B/Msc/gFFaB/S/YMW5ljcepIUjzPzw+Gcxu1ijup7/pFdDeYcuvXyWu1ZVto+ATlMvIBk63A62Nwim4E/LMX7DSJkV/5vKiXJHUrT1E/W0+DRNR1NE1BXdO/7fKJ7xX2o5sTZUOYL2rGnZgjUa7+0B/dRWK9VUnFDdXi3rUfzhDUxCipTGFKgqaV+CeRZXKU7p+0EoAlj4IVtpI1ZZNpTI05u1wXmMK4rdcHK9/J238BBw6Q6YWcUaoKVlBHLc3XKVNE92XsBmjHnVDYDQi/93mAk2YkjCi3S6cXMe3WTYxkLT0xUzwl1QvOYe1Dur34lXdNMXhXLIczmTOah4KRarwcUx8m4IsaxNfNOvMt6Zur+EPYG1++CGpK/47VM3DwUNRtojbyEM3yECH3TnUiRixd6y6x0Nc4lhSHM7zgTS7gqKjm4xEfQ9yQ2rqmRcw4a1fuPZxyAA0kb2Xb7CYX3RZxm25ZSrzKpYRWEooIm8QkWcKCRLJ6my5CIV3qkFaBcvukp7Y4BH86uIMuIw88rnPKkZvhAS5+Y3JNXU4mPU1UgfjkQC2LSzVb/hfEH5Df4QPd1xJSJOLDLw37Zj17o2xHYlzHGmjlhCrikHNA+QuQCDQOl1p1bFo+2vxyi5ipWsZ6uJzIWvhCvEqu+g90Jc3z93Pi4Zq04sQ4x3rusQehqhnEiWqvfvIqUDsoUrFT6LhTgsk2q+4ZlJk7mKXqgIwzE56xm862j2XM676wi1CSJcskybWvFxXz3V4zYzjn8mXf+93GPWGClJvoM3YyM/HWWlSMeKbu2yYgjVgJzWaHoguyIOAVuoH5HaI6o0BZ8Ih6CrkniefKePb7nxYHQyG66zh5mStLitAqCVPQzhg/OcWJv/kPd0X0E1a3KqiHoTAHY1QE3tubHGXDhoTUn1/nFj6odMrpmdZpncTYdjgoraZpvmihXAclz5kqZCrMVn0LjZSjxbF678DkfIfgqAchLK1gDFbT6/KF4xQwppBseI2GDC/wNw8SwPhZcZSvB1lMLMNpI/wDRFGSesx+HpPs/TEzire2bms504JR4HSLTxPl0rtbUg7L3hedk3EtUH+ACLTzmt7OmygD8q1iMMu3Qd/whIPJTGoarRgWGOx58i0j9OAmbwXF7x938ltaGaZV6LQ0lHu6K39/GVliRptp31UwfpRMDSY+dsgogpVxyULimS5840IK0RfJZmgSWRYcVvMeAdsMV2pzxTrNXGcy5PsfjKKu3TK83dH2FIbpTyEGF5EITNG7CLvfHNltOKbaBH3/2wTMrJ0Y9ogTTiCZB+J8dO6da34x1yhJGw6ByeyYWVMj4PHXy7XoErDzphhCv7FBIZbSw9RXVVOmarKoIZ2vHl6RtcscBFbAdOjeoKl7xDSIJU6KUu9edNBjAkGmY='),
    ('f17e30c4-50a9-484e-8563-346d91d9dff0', 'member5', 'd8c70e52-8361-4ec0-a0d4-4aee7c1388df', 'RSA2048', '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');

-- Default, we have 3 Circles as part of the test setup, using the very
-- imaginative names, 'circle1' to 'circle3'.

INSERT INTO circles (external_id, name) VALUES
    ('d8838d7d-71e7-433d-8790-af7c080e9de9', 'circle1'),
    ('09842344-4eff-4514-a290-e52efa67788f', 'circle2'),
    ('0a866d7e-5f87-4a2b-a1aa-55c77586aee3', 'circle3');

-- For each Circle, we need to have a unique Key, but with the same settings.
INSERT INTO keys (algorithm, status) VALUES
    ('AES128', 'ACTIVE'),
    ('AES128', 'ACTIVE'),
    ('AES128', 'ACTIVE');

-- With the Members created, and the Circles and Keys added, it is possible to
-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,
-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.
-- The Trust Level is different for each Member.
INSERT INTO trustees (external_id, member_id, circle_id, key_id, trust_level, circle_key) VALUES
    ('f51967c3-6c6b-4ed2-a71c-b10a6e1cde1c', 2, 1, 1, 'ADMIN', '17EDQ+esdPIvXHwfB277h2j7Xn9xj1Q+m1h7xnG3HM1evFrG+Y4j4izGsBnyVhVSgSgDy98XSuP/5vY9WmybmKVqBD596rG9L3qBTK810vkwn+zHq7gibK8sbvwWECDZ2nj3eb/FYz1bao5sPtvHeN9FuS8zgTWIKnkYBebLvka8IVKada0VaRDOwgszdes90BuXURibFe0Yy9O+rp4fq8n+ObQmJcPQ00UGNRHX/Rjj2lSBU9nvK+AIU+W/nnIeFSvTSQJ7YbXt95rgeVgFEYe/YJMD7ScRhm7yhS3wQBviBkmujdK+0lM/+w8CLTgGGuTAsTo8rEzVCjmdKz+8Og=='),
    ('8780e0ae-27d7-4454-b574-9f2a4bf53e26', 3, 1, 1, 'WRITE', 'n2t0GUsny9AbZPi9NNPufb+EyaS6Eas7Cfi+pmUZ5QgO7FATefBOTKGJbjNGWGYrR70Jw1Vv91M+i/m7kvra2sbXCi8DW2wY6A78AcNDKj7thwlxHKkbPXnp6IQsCUwTKcskl+GSE97+L7ykQY676j7FKX/Dv+glJJe7+6Yhf6GCzeW38OYNVRYlpKnOPhqYaDEYuK9fiU5AfQDhFThk0sJDZn4BcKW4PKxsHuFiWXalLU5yrHCaJlkWu3aOxoThS03MpFCjnI6/ya5bp6Jk9TamMlI2pyg/3qXnAdXBLbm6tSaGSJsLo7pfru6e8db7q9vo2WBs3D7aQ9BXORUxsQ=='),
    ('e8c432ed-b7f6-4d5c-9976-94629e4dc0ed', 4, 1, 1, 'READ',  'biGcZxtx6ZfhtsLAJQOYFp4mrAxLxa+8kncm+2I4jlkTDFIpBYM2Q6wPm51bm4q95B/UY+WN1L/Rof+iTpsD55esEo5D81u2qfaM6K8EaBrZazg/zCDCKIbHZ47y0/94VtenfQxT468e88aOswL/QReq/EW70RR95X+ZMi8bFh0orEM4DunUKLQXxZgTdNSD51DMTgTsfqY2hwlCr1LsoXDagtKRHrc3M4WnGQr5shqgZ4s3abILW6EMWWcUG5hF0ai/z1FOavpFQpWwr+FKGp6HqujsdhD+AB4kGevHzYg6vM40NlenqT1UlVYpFGi3/qn1YLQhl+MEDVev5S3wsQ=='),
    ('48e80a1e-7c0d-4f69-8a95-be9152d5562a', 2, 2, 2, 'ADMIN', '4tTWgue7y0oMpU+T6LUy93l5DZ7xeFAlat0J1cgkjrqmzetqSsyEfEWquwgofuNX/7N4Nh0m/x2CfWkV9LJMwaEfnrPFASyk5Go1Rc9BvruVeK6+gYrn3g25eXmWxCUbklMOeDv/92zz4ZzR07pjyoyAYhtmNctpopsv1Tw9oGUF2IZ+9WY/Rt90mR7jFOwr+iq7sOhwGJLu1RzxQ8Cl4WXoPPLkrUPXN61Yd3kdnVHHDSdecMz+3ZWCAiMVRd0OBZ+13FXhNheRN3ZmLkoeqMgS9sx9JNXb3ZQMq4YFsfM49sigcwpTO5yW6avnIqvBN9YA0ehntYPDweh+imLXsg=='),
    ('7a9506a7-d961-483c-95bf-5e9e420fc5bf', 3, 2, 2, 'WRITE', 'N6+qzenVYHdNE81YXtMQxSHF73f5Rlw6QZhaQ9YVuToLPWL9fVzafseVGmdHn8TYQWGcKV3vYUPzoTa/UHhJ3yBIbrhjgv+06Wxw+DB6gKLaUtDZ5TLDeTyAXhRaq8o05LmuwSiiI+7YBlOlNKEoKaZiy550tVlcjALdJAWwzw931s6weq+iRSTw2H+nbv3qU7hL2lpKIIvXlHtCKON121AQocKydnr0600PO1QKsUyYHYEP5IB0vt4inDqaZ+nN/sifGrGrdjs/JGGNa6j2XhZ7eegpX0XCtFeGH4jRwkHkDyrkM/WWX4tKPYvwB+gwXkZIFj/aMPDZIXlbHRwZ1w=='),
    ('710d06a4-205c-48e2-a761-96cf73fe3f80', 4, 2, 2, 'READ',  'KlISfxIoQwB3BzhUBhM+GXCvFWX0pJj1WksdJ/tmVBYEZNbsMRIA0kAneov9gbFOffe2YCZ1JRWjdKJ4oA8BK5dYG1uTBl6SlKInniG0bARR7xAFhSOAzk5JbhcB6RlmK6vOGqiE48fBGZigb8krMeuDega6Fw9q1GikGn9xjQLvm3YMaxhbIJXpq9kWLgDqfnkApHpzFp6NMGgRsfAFp9RfhqHHtIsf6EH/r9V4SK+1yNT6zN8NNzZ+w4ewaiIBkaoZgoxYTDRht5Jbmji8kBRw+rMnQ3zjRcHfY04V1Q94ZlQdGea3T4HzRXblpCEMv6C0ayspbVbQD+91XbfiTg=='),
    ('f4492126-5719-463f-a0a9-4123937529e5', 5, 2, 2, 'ADMIN', 'dVhvwLz9zWXLGZ0YKEU4KWuBHEOMwTg+5RHR59PqY+MrdpGl5usLvXNIJOhPv3NjfnMSNS15rXKtNQc0Qr/eos6+3Hzy/5ZCIIDq4oENWRGe9yIL3Olxhrq+3BmbG80xO5EmRw/xQWG99l/QGw26BBSV4qTs1v//2jtP5us2pJ//C+Awe+YvKx9FmJkqqUhPLBYKk3gQWMPYWZB1DzTLTEYmzaYq31InkZ+Mk496Bj36ZEIoFuC5vrJUd0WMONcSpVJhw2VZrpSxfY/02TSd1GFWARqIYrWxtZwyNSkuuRZIvtyGdvXnRtRTqgpv8l6ysBnpLYApkvMiD6InszpGMw=='),
    ('546ae5e5-ae0f-40a7-9ec5-fed825f303d2', 3, 3, 3, 'WRITE', 'PaLTpMWPr8VdPqTStpOBueZv6qMTQLWnJabmKuHdPj80Ncksh8yThq+ZlpeCj1reMt9Wx4DxVf0tpZ92PQ3UN73kDR6bLt2zsCv0RN93kqkUOT8WnCy4U00Vpkd5YPAOJwi5VHDFt0Kd+v/AO6EGgzdmJIgHzaZORBYm0K3WX+tifv0j7ctBnEv80qpu9rrx6ktAtls23qLj8YU7xKzpm0LpMyPTrdvAlccydmlab1PfAtu49Z61/jd39R+tu+FfBYkEywSsYvN7vHkPFED5S4z4FEd/entab3gtT0PjkP7XhY2O+VaMRL9Ci4XDgYFEBPyw96cEmANXgT+eG5ZJ9g=='),
    ('9510ac00-a60f-4fe4-a9ed-c76ce3ba5d82', 4, 3, 3, 'READ',  'BAbSPQbTC3fL4vTUjtyFb7RBtBhH9ZfpTZ/Vls6qIWDVHFKxme/l2oIjROqo6UxH0j2Bz8k6lS/Zgao5Sw+GRwI8blnD4nOZx1aj1RcW+a2aAdRGS7VdtNBXOTyFpECwtEiQnqdO3wk2BFKvH4rwvIxvEj/NDMAS0BqpWLzyomTlBLHu/LtzOriUNfQrIBKVzQqItS+y/LwFx2UZYKt8kkMMSUrAaURMC8/DUadzSDJcQ/1OEtbVbkceFwfLzQsC3N9KQi+xAYezDTfncwnEWdSmXi7G7Pa/xSek4y66u6xbA2p9eczBIa/N35WJD3ReiGJjog304ODVldzm6Y9z6g=='),
    ('5c0f78e5-5a7b-4b95-8de6-e10d6e3766bc', 5, 3, 3, 'ADMIN', 'QHoXxNnzAI+oX6N8ZkJrbBCpbhqlCDbc2SYwWgozgcSvlh8952ELYuTQFPV+6fZHMRNa258dR3nNrOGuuYebCgBIhodJ4zFYh4+nScyXPsNchrhaKw0TGbAyMO539PL/LsxWo6dcQlIIBhw8DMRrZHVhkjQoPhTD8p9zX/KNY46o3qH0GuujeH1s0AVx1e0iAjnXnEtlZCxVUKU5QiR6jdlvVl7arLiV5pkY0gfL/I9wGpQ7AKXGvt+6BNxJ3xUtBvlVte9zTWJM2PSJxhmoneHRE6GcyfNirBR1JfFVoaztcLkgRBq9dgnv4dqTcBx6hWesN4b4GPpnynNs9fLc8w=='),
    ('5a39c271-ca07-4ff4-a955-609a4a622b64', 6, 3, 3, 'GUEST', 'XDINCLsrcoTcwJf7SPbGGnu68+PM5CM69O31lkJJThSXMdpCwOdIFMhBzulsX5Ec4+9QT0a81H2L6mguK1F+Uq4N813C5bxCJNxNeMamFBzA4oUaHitv2FFn2z9nbZt1K2OLFe55AeT2x8aeMGyOzXakHc93Fgd203qZm7RngI7xVTVuh2oDky/DDqbav9Bpenb3+JO2TFw46cWSGq9livZtK3H4AYSo27+IpJK5LPpxzngzjCA6PWLrINJb7iUiCOEtZvJeBRuVKfk5JbWK/uC15IMJszuh8LW4vVd0JS5dAGEacY9n174lu6U8jbqwOs62gxGpSl8ZnRwQ0XVIkQ==');
