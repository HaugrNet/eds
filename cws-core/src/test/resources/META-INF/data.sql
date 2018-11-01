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

-- For the testing, we're persisting the Settings also, it is not really needed,
-- but it helps with ensuring that all corners of CWS is being checked.
INSERT INTO cws_settings (name, setting) VALUES
    ('cws.crypto.symmetric.algorithm', 'AES_CBC_256'),
    ('cws.crypto.asymmetric.algorithm', 'RSA_2048'),
    ('cws.crypto.signature.algorithm', 'SHA_512'),
    ('cws.crypto.pbe.algorithm', 'PBE_256'),
    ('cws.crypto.pbe.iterations', '1024'),
    ('cws.crypto.hash.algorithm', 'SHA_512'),
    ('cws.system.salt', 'Default salt, also used as kill switch. Must be set in DB.'),
    ('cws.system.locale', 'EN'),
    ('cws.system.charset', 'UTF-8'),
    ('cws.expose.admin', 'false'),
    ('cws.show.all.circles', 'true'),
    ('cws.show.trustees', 'true'),
    ('cws.sanity.check.startup', 'true'),
    ('cws.sanity.check.interval.days', '180'),
    ('cws.is.ready', 'true');

-- Default Administrator User, it is set at the first request to the System, and
-- is thus needed for loads of tests. Remaining Accounts is for "member1" to
-- "member5", which is all used as part of the tests.
INSERT INTO cws_members (external_id, name, salt, pbe_algorithm, rsa_algorithm, public_key, private_key, member_role) VALUES
    ('d95a14e6-e1d1-424b-8834-16a79498f4d1', 'admin', '5fiN2G9SE5Wvos3YfqnpDeEq/PqgeTZFWCZtaRY6TYbV3azHPprj33PbxUXydThF', 'PBE_256', 'RSA_2048', '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', 'ADMIN'),
    ('073dcc8f-ffa6-4cda-8d61-09ba9441e78e', 'member1', 'ATH3bPxoT8Cu95f3vr0PF2SNtMoXNKFwbv8iOUtiputclUlTuqiNr0LHw45iultW', 'PBE_256', 'RSA_2048', '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', 'STANDARD'),
    ('d842fa67-5387-44e6-96e3-4e8a7ead4c8d', 'member2', 'cD+Co0lsfOs8uNEZVYGkly7R3z0C5cx2IV/vKNvm6RY6DI3UhV8zD1Qm6u9BGzfs', 'PBE_256', 'RSA_2048', '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', 'STANDARD'),
    ('f32c9422-b3e4-4b52-8d39-82c45f6e80a9', 'member3', 'WvihEwh3ozBv5FKljLGUmkGLtoRxLy+0ViwWquRw8TKiY2z1Fo23f6OwsnvOeLDl', 'PBE_256', 'RSA_2048', 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjW/OxuoRZa5gU8+7VYtm59f2EF3gbvTeCBy6hRLjoNtqUs8GeS9uxidtLArOywiN4OG7WiJmuyFTM/r/LFvrptbVfqvnXrcmDrzXQ118xGdvrRckKAmZ1sq93RkNGZYBPZEPZhqBteQ3piw8CnSKKZLMVJACnrCnRtDs72Cw+B0cgoHIvwIqbINGBb8jsp0KvfhNxTH7v8POAphMYDnECZI2bHVR3rBfVtZT4kwCdWoFUPT1Q78kAaQQ8QOglW9dhDlGKNlcJZw2h5caAcF8o5iG/Y4caTrqVGThHuTB7vWQsY+5gDjpozz1Ni0GHHT8R9qE04fkI/4x9ZYn4ui6/wIDAQAB', 'aPrQQYJIJG0s1vs5L5+RHA0Nth9ONUSfT7QUNvQrNYL4fRNz4BPVSrSRJpY76Q/XL/T/ozq1ZIuhrTKjibJEfxvqL6UtMr/o4RTo3UetEKkIcbQikcgPJqAzI1cRWtoydteYc1Wn8FF3bdBot2diB/Hd7qm7aXNm61WM5r3BJ1MPwWLJqB0BIPlZ6XYHcA26lLCp6SmJ9m4bEl+WEje33B+3zc17czY+3WERfZalPTjStyIU649nPQgTwBNVKE4+WoBDPRsEEDrc1UC/uLZDgkKF+zBGK/anbayzxvvlPy2U56Sp4kEOuMKifF06JEjhzb11s8ptHgfS0cAW4wwNz3NCP0xKDYcfy3ZrTdJdBZVvbFsy1OdzCxr2ValrsSpeR9m89aGKlzVIzb5tyy75UDoZgRPqg0FGzwtPYjw4CGCjHRsU1jhSkCQyM1uwi7rbxuHYOrcEYE9THkomDYfXNE6HK11wHS8ZvZ/kT4ib5VOXeJ0fcAmjO7sG+6b7FMAbbu6qPuOULaJGL7BWKUMW0CnQcy2iHDYGp2xe3RzM9w6dP65NyczJ84KwZ+JckastDvt/sTe6sSm8eVx/QKLSW7pK+Ij06pgr33ufv8SiUE+sJ8B1z4ofJ3ZxVvAlEL5fUP72HTV+c6LQUtQt/G2mNB4qUyln/dqNv0IzykYI1I62IwlfGPs0ZlOr4XTJ/bUENHjrmuPD+tIcVEkhIIXXQ9kR01UWSt7XnWmWxhYWdpCzSXW17NQfesyKioyk1IUZE0Mg8icb3f6GlMBE6oqV5uUkGNjvNczqONUMrZYp6bxDxsFqtCr6Eql+2fL8ymb1mW7IIA6om3uQxfLaDoQ1OOmxsiyMfXqNJi7Xi8Oc2uHRGe7BTCOvKopo9x+iLRe6tGat5D2No8EYSoPyeTl7BKVIA1/XP0jUaEgf+PMJB1fnnYYPMzURMdKzRR4jwIfQfQduvqlgqWAX4Wf3jgvF2cmY7OZFIBIhiBqgaCzPINIpYzd2mg+OjtS3U1+nt+EL7zlVUgNJGdASgLld9+Nuk93uDFW9s+/FdUfBTCtxglIc+oUcHJ1epwdBMHZMu/rGmc5eHgXVaCz5P+g75pkZg+rPTaIm7ob7uSMEMiTicSRr9d4tkGpNGw0IgxPb2E7n66OlsWKufilsg7qikMIf+a8bWgQMNWRqj48GeZ0Tfuc2mN3M0danq/69Z2x/hTGGTldo2+t5bUACoanRKAgwUeYnXEhwqO3gm4WF+SUuMFbhQpCD3tZYbyYE+IgZ5xcnnZFaRinfcPjWsvnE2UZulemQ4LbGaUBt3UhwGe1M9Y55jAh6Mt5YlWcapF8xChwEtqTzgvkGuAvmZia22Ji2EtNPTdbUvKuJcL23hkXLhc+p6S4QxEcpYurcuaNDzj/nDPvgq4MS/70PS1rSm/Tv3w+1u8wQ/OS4AA7qoXxkugU5YY1EgBa9rlzZYjuWS4vzGxVH1NRSLg21oP6eFwd/gtfKGvgCCtBMtNY/DIfTNTQhhgPwICtf02co5VQwXOtYjVvl8RsMkcfMH94JVZQ7IgiRNwWnfYgFCyfY6/jiGr2ftbLZFSiGXjhSbBgvBDGI7fA3+Jlw0NS4MriwxisxZd6u1PXJy2nVUyeHiKqZrfM=', 'STANDARD'),
    ('b629f009-4da2-46ed-91b8-aa9dec54814d', 'member4', 'CzlQi4ndB0Vm7oZe3TxMCHmCwfhVlJ9whVBpUT6finDWNLSJOv2l+E1NXNWR0DEt', 'PBE_256', 'RSA_2048', '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', 'STANDARD'),
    ('63cb90cc-c1fb-4c6a-b881-bec278b4e232', 'member5', 'n6JGLxyY2RNTL3A8ix+h4L+dX3wKoi7mt7hNMIK+b2Bt4aMoZHfsmkNAmCDS+wlo', 'PBE_256', 'RSA_2048', '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', 'STANDARD');

-- Default, we have 3 Circles as part of the test setup, using the very
-- imaginative names, 'circle1' to 'circle3'.

INSERT INTO cws_circles (external_id, name) VALUES
    ('d8838d7d-71e7-433d-8790-af7c080e9de9', 'circle1'),
    ('8ba34e12-8830-4a1f-9681-b689cad52009', 'circle2'),
    ('a2797176-a5b9-4dc9-867b-8c5c1bb3a9f9', 'circle3');

-- For each Circle, we need to have a unique Key, but with the same settings.
INSERT INTO cws_keys (algorithm, status) VALUES
    ('AES_CBC_256', 'ACTIVE'),
    ('AES_CBC_256', 'ACTIVE'),
    ('AES_CBC_256', 'ACTIVE');

-- For each Circle, we need to have root folder for all data.
INSERT INTO cws_metadata (external_id, parent_id, circle_id, datatype_id, name) VALUES
    ('9a1e60a8-078d-4b16-a337-36bbcef80519', 0, 1, 1, '/'),
    ('5ace76bb-a6b9-42d9-ac76-3f2f88227493', 0, 2, 1, '/'),
    ('450696d6-987e-4f7b-9d80-7b6b4890ded9', 0, 3, 1, '/');

-- With the Members created, and the Circles and Keys added, it is possible to
-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,
-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.
-- The Trust Level is different for each Member.
INSERT INTO cws_trustees (member_id, circle_id, key_id, trust_level, circle_key) VALUES
    (2, 1, 1, 'ADMIN', 'ABcpJBKMV7qwz+EbBXo49v89AbCMx8pr1tYf9wFwIhPHtfkJvjCCnvmn9wv5dLN9vSnON2n+HV/Gx7/bJOxHxUT7qVdFYsiUYYrHdUmlvSUUF5Fr4UAF5ZJM9bsK7oYgWygggnaHgBFPAhurKETt5t2DK+MBvdnrckquRUucAmTmzZDL39YJN8m/mc/eSW8R8UpBBTnYbju/ndWhzWFzWeMpLsadzm0D5ZmZnqf+p7AUj4K3mskBc5yhTk3CZd3vTxqx05ioYpaZkFwTh3L2tO3Skiw8GFDeIHjGY4eq1/+4ZMRSmMPJ6YhxSCWLwrrBmOSjpRlR10bJC3mfRzQAng=='),
    (3, 1, 1, 'WRITE', 'QdWEXezyoBG9I61jwHylCy5sxHP+5ab466AxnZ188I0lmU7DMDlGlxM7NNS8adFCFMsNkU7iFunppPG0dicfNAaks4KadH/AAqfq7MPCuQJ1vFNJK6+Kh9EnKy9ceq1RDOlvGVP7NrTk4xGR1RO9dFDzR+cevfQbBNZGcHfl3RPr+aaFDZlX9MwP08ekpymPkkLN6YIWkm75thVT640nUORDoV1rRmbTw6fKvx3Q7Skd97kU+YhQIHEQIFB4VvvsaPGl0vf0WySY5e8kQTV9jerpBP9NXNqezg3ypvF9W67MR0bzwWs0Xo3SHAmBgdcuDxfnfQa/FYuAXbddJwHv0g=='),
    (4, 1, 1, 'READ', 'cuP/+9NSCsnBdKPMhyUjD1hfhCeYo+HxAFf8TwBf64tbOb6fMxfcM+2S8bwM7LeRuqOHLuhJkZ5iSAWyULO3thRryeDYSN4Uauy9pN9lKAJPgw7aH5L2HqDrxnXQDW4o9vkkR1eMlraOH6RTsuevX1jL5E4xsylPtBetVsfFO7cFBG+ZNGehXnj2/awMHyXsm9TIBmT+9maqHPJ2sRHD3SoZVnVnuJ8EEJ4RNw3uglL//3b33xiETstFNTfB6RcIZ/VfSQiDQXhRXjFeN01eRvDUw5s74BJeNxKkPpz5/ZQCe8WShJy3M3Qj8E+CHf3l+snSAn5O3VjEskJMtjjFwQ=='),
    (2, 2, 2, 'ADMIN', 'C4qrUHVqDY9Rnfuz+RXN7dE5fkVQLfxnyD+ld7Af1wjb+Ak9P4akunv9B6KE8ZBSz4TETFJFX+ZuzKc8smrjAqi0jdxZMInOiCtVxa4zS0lDdQoyZzP0l0SBOd6n4KhfQqd6jM9dHKqHAa8vK+QMUHE3AUbN6xdB17h3CGvTr77tazYoQAPMxcpE4jBz9GYVp2dWvpk5cPFNKC/pKj5B07OmX5Th0TxGKMPr06/nG/paOg/gOXFhmR3pgcuBW5+kTlfCPt/vpoW5TwJHMP9jM0ozY1BZrA/YKi7MgmGUS9Qf+LFw6iQr3m0HK+JldVqGT5vPQ2pCpnOsxVXopVq6Fg=='),
    (3, 2, 2, 'WRITE', 'HCNDRu6+2sho/hQcC8uhKUwNLkKewP6/dxWHQbotXpAGQQkgWdJjel2LnIP9sG4YiYxMzNzjjvIDfvnPFfh524U04Ngc3YeC/O5i+H9Rmu5jf2BdKREMXAKmL/KJsb2SBVvzlRjGO8S5LYFO2uAjpmoQsjoh4D1uQRrHTe+FPqK6PNTee51Lx/o0CaK993SQSpNCECiI/D0laaRgs+FruWbvgC+UdONM61leHXWlvAjYX53+4Jt8DAAg8OlvP/f6JMe3MQqBVXWpHiwwJjtXZ1dzwWIG4pqkHu8NpTsiwX0PrfbxeItfJ+F5TADVEB5fMBdt0+W2RvI02UvqHYcp/Q=='),
    (4, 2, 2, 'READ', 'CfPPo4GuhueBlhgI/WHN2i9xIFhbsJm+v1nDOELMqX9x9vZ+ha7qbuQtKPz/1N1ABmpgpyawxAPeBFfzTLkLbwQ3JrIZ7DRCuoBR9CNiv9mgElPQZVJRYAZDpSh+/DW9jf3aS/zGIQ3mzJbHKXNEPhIn5K/jM6bwhMspHcBd5V3UKuO1lQYstc7Qcm7HWHVrh4YK/tDcIffeNahEP3RqPOM/dRilYKBDqPDbciNESnAo8pNJ4Hbnkpk9hgkuuEGnPFPrEvn1BG/eAdlowtFR0tLxTwmgK71Y7DCEzQh7gbwHVEIg4iIeTSS6PruCcfUcAx8JpKpF5ZM/nG2Er/E53g=='),
    (5, 2, 2, 'ADMIN', 'H3Q7e4eZ1xbXvwFWDG0VW1GCB7wKfd5nQVKTB3ecnnj8RopY0dKg7VJ41tnMaTzRzhs7MQcyQUN+pxX0tOj8RP4KRvl3qOYQowFY1p4o6xzgNWRLyuaPIWtS0DF0xjX7A9b6RA611imfQuvc5A4vGTrqhbbw27MpE+p0WVLdpTcS1LNxPXP6us/gBFj218DzLw4B4C52OSlL+0dtIGwqlbY4dkesZvBH81uHtDjr3HswS3GJNJYoU+n9txUwYlaEjtndygbW7rc3fc2z7MSID/0NM4vbqtJl+r2htVC8Xsecr6srziK6zpvROa+ec0hORSCwzT61WC5FgMHubLO1SA=='),
    (3, 3, 3, 'WRITE', 'hSOR/kO7lkihQBvT3juvOVwYvWBO5XNEAZpvBmcDl6Fo2ZF7hx9HF8CKnGK8E9oCPaKK27s05wV8+tgG6w94plp3xQ2d/+S9Xr3V8SFuaAgjSWwOcuhu+AeLIcM6jQmztdOz8v+ZeKu/rSoWkj03BtKvsLFnNq+D/9kPQBNQxNoDJePLCiMD/vyEBwMBRLtNyRLiTfsDEWxY1xGVjTFXDj6sXZdWrEQuhVvAvPL7/vnAexLx2pVL5k1y/Jn67epoARDEplHTmdtqq8IGHVifOd1nh0rMmZ64ssn33yBMtVIjPLD7BAvW49w16ZE7YgcbCXMJyGy3Jjs0oXNtyxgfVQ=='),
    (4, 3, 3, 'WRITE', 'MqjV93f3dKe0WZZ3sm/S7zVHs4E2ne5ZDiGb9dOpeR50k31HPLtIWkYXrH7W4ANsVQAxR7V6siZCbdt3Me3wTSdlQvtZvMu+ICwffKYJ+sZh6t3MY6TlX+Rlsa9DlLLOTQFrAYunPbaiFmSY/QS/d4moU5ygCFF8kJogC1voeBDG+X33Obii7QCPfAZB6TctSZk9ETUauO9Ve2zS6q2axsbUff+YX+ncydNplQH9HTd/4hd8Y/8ZJkRf5gjKt9BWlIa0/NA6LGeufP8XZw0UY2EzxedGz6nT6YsLHsPmhSFMA3mzXfrYuPiaTK0vBmgc0+n58UgHMJXjzM6m85PYEw=='),
    (5, 3, 3, 'ADMIN', 'E7uoYvy6OqbSUMco8r0fMgcMXwCjTxYiXsgy82TLgOMZHNszZx7fpfJj1lBWCjOrsZDbDNdXYKYcy30fJEeRrI9FHVLnjiVc4olKf8j0nnIfb7MY3IHaXykFwJItIWt26HKkxc80V0TJStpfaCPLpzHAV1zF18MaVJXX5DNjMIdhJlcbe9GNAUM3i2BDFPhQpP1EY1yUm9IzV516ECb2cgKP9d2mzEG6aSBUwFMPMkvNO8BEYNLpVa2+6LH2B8RcE2ZRId1YlI9Zl9EqViOp4SHZjLrTt+qDoX/WRFfQDwLEAc4VOE/MwQ3aEt82nqtaMPmnnpi/FfOox8noYi2Olg=='),
    (6, 3, 3, 'READ', 'dseDJ4Ktm3Uva2xG+MhJrigl7fRODjSwV2Rhy7/Psnw0n5rJW4Q/9HD0ZogfLi6dLqezWCWTFxgR+/QuQv8h978hpAlsxLFO0ThCJz2LBjKZwRj8yvnPVIWy9ELa2c01amcUbmUTqrAr+is+mE7rOoD5Ug2/r9TPfinWlc/y+UwhA0m+YrdwmmeKp7C3msDqj65pnGMDV6xmkU4n+ficLWxHZrfODebtplAtjkat/pesJ7oQrgg8cvrld9OTpm+oiJSISkDQl/SjTQlgaynrGQUU5pIFijLsqrtGvwu0s1OQ+JTMHt0xduP1QAsv7HTmdjpRIky2Aq98MZsP/jMEjw==');
