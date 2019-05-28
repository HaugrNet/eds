/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.HashAlgorithm;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.StandardSetting;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class SettingsTest extends DatabaseSetup {

    @Test
    public void testSettings() {
        final Settings mySettings = newSettings();

        final Map<String, String> existing = mySettings.get();
        assertEquals(StandardSetting.values().length, existing.size());
        mySettings.set("my.new.key", "the awesome value");

        final Map<String, String> updated = mySettings.get();
        assertEquals(existing.size() + 1, updated.size());
    }

    @Test
    public void testReadingDefaultSettings() {
        final Settings mySettings = newSettings();

        final Set<KeyAlgorithm> aes = prepareSetOf(KeyAlgorithm.Type.SYMMETRIC);
        final Set<KeyAlgorithm> rsa = prepareSetOf(KeyAlgorithm.Type.ASYMMETRIC);
        final Set<KeyAlgorithm> sha = prepareSetOf(KeyAlgorithm.Type.SIGNATURE);
        final Set<KeyAlgorithm> pbe = prepareSetOf(KeyAlgorithm.Type.PASSWORD);

        assertTrue(aes.contains(mySettings.getSymmetricAlgorithm()));
        assertTrue(rsa.contains(mySettings.getAsymmetricAlgorithm()));
        assertTrue(sha.contains(mySettings.getSignatureAlgorithm()));
        assertTrue(pbe.contains(mySettings.getPasswordAlgorithm()));
        assertEquals("Default salt, also used as kill switch. Must be set in DB.", mySettings.getSalt());
        assertEquals("UTF-8", mySettings.getCharset().name());
    }

    @Test
    public void testUpdateDefaultSettings() {
        final Settings mySettings = newSettings();

        mySettings.set(StandardSetting.SYMMETRIC_ALGORITHM, KeyAlgorithm.AES_CBC_192.name());
        assertEquals(KeyAlgorithm.AES_CBC_192, mySettings.getSymmetricAlgorithm());

        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM, KeyAlgorithm.RSA_4096.name());
        assertEquals(KeyAlgorithm.RSA_4096, mySettings.getAsymmetricAlgorithm());

        mySettings.set(StandardSetting.ASYMMETRIC_ALGORITHM, KeyAlgorithm.RSA_8192.name());
        assertEquals(KeyAlgorithm.RSA_8192, mySettings.getAsymmetricAlgorithm());

        mySettings.set(StandardSetting.SIGNATURE_ALGORITHM, KeyAlgorithm.SHA_256.name());
        assertEquals(KeyAlgorithm.SHA_256, mySettings.getSignatureAlgorithm());

        mySettings.set(StandardSetting.HASH_ALGORITHM, HashAlgorithm.SHA_256.name());
        assertEquals(HashAlgorithm.SHA_256, mySettings.getHashAlgorithm());

        mySettings.set(StandardSetting.PBE_ALGORITHM, KeyAlgorithm.PBE_192.name());
        assertEquals(KeyAlgorithm.PBE_192, mySettings.getPasswordAlgorithm());

        mySettings.set(StandardSetting.PBE_ALGORITHM, KeyAlgorithm.PBE_256.name());
        assertEquals(KeyAlgorithm.PBE_256, mySettings.getPasswordAlgorithm());

        mySettings.set(StandardSetting.CWS_SALT, "UUID value");
        assertEquals("UUID value", mySettings.getSalt());

        mySettings.set(StandardSetting.CWS_LOCALE, Locale.GERMAN.toString());
        assertEquals(Locale.GERMAN, mySettings.getLocale());

        mySettings.set(StandardSetting.CWS_CHARSET, "ISO-8859-15");
        assertEquals("ISO-8859-15", mySettings.getCharset().name());

        mySettings.set(StandardSetting.SANITY_STARTUP, "false");
        assertFalse(mySettings.getSanityStartup());

        mySettings.set(StandardSetting.SANITY_INTERVAL, "120");
        assertEquals(Integer.valueOf(120), mySettings.getSanityInterval());

        mySettings.set(StandardSetting.SESSION_TIMEOUT, "240");
        assertEquals(Integer.valueOf(240), mySettings.getSessionTimeout());
    }

    @Test
    public void testShowOtherMemberInformation() {
        final Settings mySettings = newSettings();
        assertTrue(mySettings.getShareTrustees());
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "false");
        assertFalse(mySettings.getShareTrustees());
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "true");
        assertTrue(mySettings.getShareTrustees());
        mySettings.set(StandardSetting.SHOW_TRUSTEES, " true ");
        assertTrue(mySettings.getShareTrustees());
        mySettings.set(StandardSetting.SHOW_TRUSTEES, "");
        assertFalse(mySettings.getShareTrustees());
        mySettings.set(StandardSetting.SHOW_TRUSTEES, " what ");
        assertFalse(mySettings.getShareTrustees());
    }

    // =========================================================================
    // Internal methods
    // =========================================================================

    private static Set<KeyAlgorithm> prepareSetOf(final KeyAlgorithm.Type type) {
        final Set<KeyAlgorithm> set = EnumSet.noneOf(KeyAlgorithm.class);

        for (final KeyAlgorithm algorithm : KeyAlgorithm.values()) {
            if (algorithm.getType() == type) {
                set.add(algorithm);
            }
        }

        return set;
    }
}
