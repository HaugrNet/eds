/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.jce.MemberKeyPair;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>The double wrapping of the Circle Secret (PLAN.md, Crypto §3): neither the
 * MasterKey alone nor a Member's keys alone can recover it. The tests use
 * the stored test data (data.sql) for {@code member1} in {@code circle1}.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class KeyHierarchyTest extends DatabaseSetup {

    @Test
    void testBothLayersRecoverTheCircleSecret() {
        final TrusteeEntity trustee = trustee();
        final MemberKeyPair memberKeys = memberKeys(trustee.getMember(), credentialOf(MEMBER_1));

        final SecretEDSKey circleSecret = unwrap(trustee, memberKeys);
        assertEquals(32, circleSecret.getEncoded().length);
    }

    /**
     * An attacker with the database and the MasterKey, but without the
     * Member's passphrase: the outer layer opens, but the inner layer needs
     * the Member's private keys, which are protected by the passphrase.
     */
    @Test
    void testMasterKeyAloneCannotRecoverTheCircleSecret() {
        final TrusteeEntity trustee = trustee();
        final MemberEntity member = trustee.getMember();
        final String binding = binding(trustee);

        // The outer layer opens with the MasterKey ...
        final String inner = crypto.bytesToString(crypto.openWithMasterKey("eds/trustee-outer|" + binding, trustee.getCircleKey()));

        // ... but the inner layer only opens with the Member's keys: other
        // keys fail, and the Member's keys can't be opened with a guess.
        final MemberKeyPair attackerKeys = MemberKeyPair.generate(KeyAlgorithm.MLKEM1024_P384);
        final byte[] trusteeAad = crypto.stringToBytes("eds/trustee|" + binding);
        assertThrows(EDSException.class, () -> Crypto.unwrapCircleKey(KeyAlgorithm.AES_GCM_256, attackerKeys, trusteeAad, inner));
        final byte[] guess = credentialOf("a wrong guess");
        assertThrows(EDSException.class, () -> memberKeys(member, guess));
    }

    /**
     * An attacker with the database and a Member's private keys (e.g. taken
     * from a client), but without the MasterKey: the outer layer can't be
     * opened.
     */
    @Test
    void testMemberKeysAloneCannotRecoverTheCircleSecret() {
        final TrusteeEntity trustee = trustee();
        final MemberKeyPair memberKeys = memberKeys(trustee.getMember(), credentialOf(MEMBER_1));
        final byte[] trusteeAad = crypto.stringToBytes("eds/trustee|" + binding(trustee));

        // The stored value is the MasterKey layer, not the hybrid KEM.
        final String stored = trustee.getCircleKey();
        assertThrows(EDSException.class, () -> Crypto.unwrapCircleKey(KeyAlgorithm.AES_GCM_256, memberKeys, trusteeAad, stored));

        // And without the MasterKey, the outer layer doesn't open.
        MasterKey.getInstance().lock();
        assertThrows(EDSException.class, () -> unwrap(trustee, memberKeys));
    }

    @Test
    void testTrusteeRowsAreBoundToCircleAndMember() {
        final TrusteeEntity trustee = trustee();
        final MemberKeyPair memberKeys = memberKeys(trustee.getMember(), credentialOf(MEMBER_1));
        final SecretEDSKey circleSecret = unwrap(trustee, memberKeys);

        // A row copied to another Member or Circle doesn't open there.
        final String row = trustee.getCircleKey();
        assertThrows(EDSException.class, () -> crypto.openWithMasterKey("eds/trustee-outer|" + CIRCLE_2_ID + '|' + MEMBER_1_ID, row));
        assertThrows(EDSException.class, () -> crypto.openWithMasterKey("eds/trustee-outer|" + CIRCLE_1_ID + '|' + MEMBER_2_ID, row));
        assertArrayEquals(circleSecret.getEncoded(), unwrap(trustee, memberKeys).getEncoded());
    }

    // =========================================================================
    // Internal helper methods, mirroring AbstractManager
    // =========================================================================

    private TrusteeEntity trustee() {
        return entityManager.createQuery("select t from TrusteeEntity t where t.member.externalId = :member and t.circle.externalId = :circle", TrusteeEntity.class)
                .setParameter("member", MEMBER_1_ID)
                .setParameter("circle", CIRCLE_1_ID)
                .getSingleResult();
    }

    private static String binding(final TrusteeEntity trustee) {
        return trustee.getCircle().getExternalId() + '|' + trustee.getMember().getExternalId() + '|' + trustee.getKey().getId();
    }

    private SecretEDSKey unwrap(final TrusteeEntity trustee, final MemberKeyPair memberKeys) {
        final String binding = binding(trustee);
        final String inner = crypto.bytesToString(crypto.openWithMasterKey("eds/trustee-outer|" + binding, trustee.getCircleKey()));
        return Crypto.unwrapCircleKey(KeyAlgorithm.AES_GCM_256, memberKeys, crypto.stringToBytes("eds/trustee|" + binding), inner);
    }

    private MemberKeyPair memberKeys(final MemberEntity member, final byte[] passphrase) {
        final String id = member.getExternalId();
        final KeyAlgorithm algorithm = member.getKeyAlgorithm();
        final byte[] salt = crypto.openWithMasterKey("eds/member/salt|" + id, member.getSalt());
        final SecretEDSKey key = crypto.generatePasswordKey(member.getPbeAlgorithm(), passphrase, salt, member.getPbeIterations());
        final MemberKeyPair.Public publicKeys = MemberKeyPair.Public.fromArmored(algorithm, member.getPublicKey());

        return crypto.openPrivateKeys(key, crypto.stringToBytes("eds/member/key|" + id + '|' + algorithm), "eds/member/key-outer|" + id + '|' + algorithm, publicKeys, member.getPrivateKey());
    }
}
