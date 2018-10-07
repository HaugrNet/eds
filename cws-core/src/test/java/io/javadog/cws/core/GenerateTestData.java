/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.Settings;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class GenerateTestData {

    private final Settings settings = Settings.getInstance();
    private final Crypto crypto = new Crypto(settings);

    public String prepareTestData() {
        final StringBuilder builder = new StringBuilder(20000);
        append(builder, "-- =============================================================================");
        append(builder, "-- Following is TEST data, and should not be added in a PRODUCTION environment");
        append(builder, "-- -----------------------------------------------------------------------------");
        append(builder, "-- Unfortunately, JPA only allow setting 3 scripts when creating the database,");
        append(builder, "-- the first is the actual model, which contain what is needed to setup the");
        append(builder, "-- database, including all tables, views, procedures, constraints, etc. The");
        append(builder, "-- second script is for the data (this one), but as we both need to have data");
        append(builder, "-- for production and for testing, we're adding it all here. The final script");
        append(builder, "-- is for destroying the database, which is needed of you have a real database");
        append(builder, "-- and not just an in-memory database.");
        append(builder, "-- =============================================================================");
        append(builder, "");
        append(builder, "-- For the testing, we're persisting the Settings also, it is not really needed,");
        append(builder, "-- but it helps with ensuring that all corners of CWS is being checked.");
        append(builder, "INSERT INTO cws_settings (name, setting) VALUES");
        appendSettings(builder);

        append(builder, "");
        append(builder, "-- Default Administrator User, it is set at the first request to the System, and");
        append(builder, "-- is thus needed for loads of tests. Remaining Accounts is for \"member1\" to");
        append(builder, "-- \"member5\", which is all used as part of the tests.");
        append(builder, "INSERT INTO cws_members (external_id, name, salt, pbe_algorithm, rsa_algorithm, public_key, private_key, member_role) VALUES");

        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, DatabaseSetup.ADMIN_ID, Constants.ADMIN_ACCOUNT, keyPair, MemberRole.ADMIN, ',');
        final CWSKeyPair keyPair1 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, DatabaseSetup.MEMBER_1_ID, DatabaseSetup.MEMBER_1, keyPair1, MemberRole.STANDARD, ',');
        final CWSKeyPair keyPair2 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, DatabaseSetup.MEMBER_2_ID, DatabaseSetup.MEMBER_2,keyPair2,  MemberRole.STANDARD, ',');
        final CWSKeyPair keyPair3 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, DatabaseSetup.MEMBER_3_ID, DatabaseSetup.MEMBER_3, keyPair3, MemberRole.STANDARD, ',');
        final CWSKeyPair keyPair4 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, DatabaseSetup.MEMBER_4_ID, DatabaseSetup.MEMBER_4, keyPair4, MemberRole.STANDARD, ',');
        final CWSKeyPair keyPair5 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, DatabaseSetup.MEMBER_5_ID, DatabaseSetup.MEMBER_5, keyPair5, MemberRole.STANDARD, ';');

        append(builder, "");
        append(builder, "-- Default, we have 3 Circles as part of the test setup, using the very");
        append(builder, "-- imaginative names, 'circle1' to 'circle3'.");
        append(builder, "");
        append(builder, "INSERT INTO cws_circles (external_id, name) VALUES");
        append(builder, "    ('" + DatabaseSetup.CIRCLE_1_ID + "', '" + DatabaseSetup.CIRCLE_1 + "'),");
        append(builder, "    ('" + DatabaseSetup.CIRCLE_2_ID + "', '" + DatabaseSetup.CIRCLE_2 + "'),");
        append(builder, "    ('" + DatabaseSetup.CIRCLE_3_ID + "', '" + DatabaseSetup.CIRCLE_3 + "');");
        append(builder, "");
        append(builder, "-- For each Circle, we need to have a unique Key, but with the same settings.");
        append(builder, "INSERT INTO cws_keys (algorithm, status) VALUES");
        append(builder, "    ('" + settings.getSymmetricAlgorithm() + "', '" + Status.ACTIVE + "'),");
        append(builder, "    ('" + settings.getSymmetricAlgorithm() + "', '" + Status.ACTIVE + "'),");
        append(builder, "    ('" + settings.getSymmetricAlgorithm() + "', '" + Status.ACTIVE + "');");
        append(builder, "");
        append(builder, "-- For each Circle, we need to have root folder for all data.");
        append(builder, "INSERT INTO cws_metadata (external_id, parent_id, circle_id, datatype_id, name) VALUES");
        append(builder, "    ('" + UUID.randomUUID() + "', 0, " + 1 + ", 1, '/'),");
        append(builder, "    ('" + UUID.randomUUID() + "', 0, " + 2 + ", 1, '/'),");
        append(builder, "    ('" + UUID.randomUUID() + "', 0, " + 3 + ", 1, '/');");
        append(builder, "");
        append(builder, "-- With the Members created, and the Circles and Keys added, it is possible to");
        append(builder, "-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,");
        append(builder, "-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.");
        append(builder, "-- The Trust Level is different for each Member.");
        append(builder, "INSERT INTO cws_trustees (member_id, circle_id, key_id, trust_level, circle_key) VALUES");

        final SecretCWSKey cwsKey1 = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        final SecretCWSKey cwsKey2 = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        final SecretCWSKey cwsKey3 = crypto.generateSymmetricKey(settings.getSymmetricAlgorithm());
        createAndAppendTrustee(builder, 2, keyPair1, 1, 1, cwsKey1, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, 3, keyPair2, 1, 1, cwsKey1, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, 4, keyPair3, 1, 1, cwsKey1, TrustLevel.READ,  ',');
        createAndAppendTrustee(builder, 2, keyPair1, 2, 2, cwsKey2, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, 3, keyPair2, 2, 2, cwsKey2, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, 4, keyPair3, 2, 2, cwsKey2, TrustLevel.READ,  ',');
        createAndAppendTrustee(builder, 5, keyPair4, 2, 2, cwsKey2, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, 3, keyPair2, 3, 3, cwsKey3, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, 4, keyPair3, 3, 3, cwsKey3, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, 5, keyPair4, 3, 3, cwsKey3, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, 6, keyPair5, 3, 3, cwsKey3, TrustLevel.READ,  ';');

        return builder.toString();
    }

    private static void appendSettings(final StringBuilder builder) {
        final int size = StandardSetting.values().length;
        int count = 1;
        for (final StandardSetting setting : StandardSetting.values()) {
            if (count < size) {
                append(builder, "    ('" + setting.getKey() + "', '" + setting.getValue() + "'),");
            } else {
                append(builder, "    ('" + setting.getKey() + "', '" + setting.getValue() + "');");
            }
            count++;
        }
    }

    private void createAndAppendMember(final StringBuilder builder, final String externalId, final String name, final CWSKeyPair keyPair, final MemberRole role, final char delimiter) {
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey secretKey = crypto.generatePasswordKey(settings.getPasswordAlgorithm(), crypto.stringToBytes(name), salt);
        final String publicKey = crypto.armoringPublicKey(keyPair.getPublic().getKey());
        final String privateKey = crypto.armoringPrivateKey(secretKey, keyPair.getPrivate().getKey());
        final String encryptedSalt = crypto.encryptWithMasterKey(salt);

        append(builder, "    ('" + externalId + "', '" + name + "', '" + encryptedSalt + "', '" + settings.getPasswordAlgorithm() + "', '" + settings.getAsymmetricAlgorithm() + "', '" + publicKey + "', '" + privateKey + "', '" + role + "')" + delimiter);
    }

    private void createAndAppendTrustee(final StringBuilder builder, final int memberId, final CWSKeyPair keyPair, final int circleId, final int keyId, final SecretCWSKey circleKey, final TrustLevel trustLevel, final char delimiter) {
        final String armoredKey = crypto.encryptAndArmorCircleKey(keyPair.getPublic(), circleKey);

        append(builder, "    (" + memberId + ", " + circleId + ", " + keyId + ", '" + trustLevel + "', '" + armoredKey + "')" + delimiter);
    }

    private static void append(final StringBuilder builder, final String str) {
        builder.append(str).append('\n');
    }
}
