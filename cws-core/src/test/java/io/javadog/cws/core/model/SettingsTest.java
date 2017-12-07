/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.enums.HashAlgorithm;
import io.javadog.cws.core.enums.KeyAlgorithm;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingsTest {

    @Test
    public void testSettings() {
        final Settings settings = new Settings();

        final Map<String, String> existing = settings.get();
        assertThat(existing.size(), is(12));
        settings.set("my.new.key", "the awesome value");

        final Map<String, String> updated = settings.get();
        assertThat(updated.size(), is(existing.size() + 1));
    }

    @Test
    public void testReadingDefaultSettings() {
        final Settings settings = new Settings();

        final Set<KeyAlgorithm> aes = prepareSetOf(KeyAlgorithm.Type.SYMMETRIC);
        final Set<KeyAlgorithm> rsa = prepareSetOf(KeyAlgorithm.Type.ASYMMETRIC);
        final Set<KeyAlgorithm> sha = prepareSetOf(KeyAlgorithm.Type.SIGNATURE);
        final Set<KeyAlgorithm> pbe = prepareSetOf(KeyAlgorithm.Type.PASSWORD);

        assertThat(aes.contains(settings.getSymmetricAlgorithm()), is(true));
        assertThat(rsa.contains(settings.getAsymmetricAlgorithm()), is(true));
        assertThat(sha.contains(settings.getSignatureAlgorithm()), is(true));
        assertThat(pbe.contains(settings.getPasswordAlgorithm()), is(true));
        assertThat(settings.getSalt(), is("Default salt, also used as kill switch. Must be set in DB."));
        assertThat(settings.getCharset().name(), is("UTF-8"));
    }

    @Test
    public void testUpdateDefaultSettings() {
        final Settings settings = new Settings();

        settings.set(Settings.SYMMETRIC_ALGORITHM, KeyAlgorithm.AES192.name());
        assertThat(settings.getSymmetricAlgorithm(), is(KeyAlgorithm.AES192));

        settings.set(Settings.ASYMMETRIC_ALGORITHM, KeyAlgorithm.RSA4096.name());
        assertThat(settings.getAsymmetricAlgorithm(), is(KeyAlgorithm.RSA4096));

        settings.set(Settings.ASYMMETRIC_ALGORITHM, KeyAlgorithm.RSA8192.name());
        assertThat(settings.getAsymmetricAlgorithm(), is(KeyAlgorithm.RSA8192));

        settings.set(Settings.SIGNATURE_ALGORITHM, KeyAlgorithm.SHA256.name());
        assertThat(settings.getSignatureAlgorithm(), is(KeyAlgorithm.SHA256));

        settings.set(Settings.HASH_ALGORITHM, HashAlgorithm.SHA256.name());
        assertThat(settings.getHashAlgorithm(), is(HashAlgorithm.SHA256));

        settings.set(Settings.PBE_ALGORITHM, KeyAlgorithm.PBE192.name());
        assertThat(settings.getPasswordAlgorithm(), is(KeyAlgorithm.PBE192));

        settings.set(Settings.PBE_ALGORITHM, KeyAlgorithm.PBE256.name());
        assertThat(settings.getPasswordAlgorithm(), is(KeyAlgorithm.PBE256));

        settings.set(Settings.CWS_SALT, "UUID value");
        assertThat(settings.getSalt(), is("UUID value"));

        settings.set(Settings.CWS_LOCALE, Locale.GERMAN.toString());
        assertThat(settings.getLocale(), is(Locale.GERMAN));

        settings.set(Settings.CWS_CHARSET, "ISO-8859-15");
        assertThat(settings.getCharset().name(), is("ISO-8859-15"));

        settings.set(Settings.SANITY_STARTUP, "false");
        assertThat(settings.getSanityStartup(), is(false));

        settings.set(Settings.SANITY_INTERVAL, "120");
        assertThat(settings.getSanityInterval(), is(120));
    }

    @Test
    public void testExposeAdmin() {
        final Settings settings = new Settings();
        assertThat(settings.getExposeAdmin(), is(false));
        settings.set(Settings.EXPOSE_ADMIN, "true");
        assertThat(settings.getExposeAdmin(), is(true));
        settings.set(Settings.EXPOSE_ADMIN, "false");
        assertThat(settings.getExposeAdmin(), is(false));
        settings.set(Settings.EXPOSE_ADMIN, " true ");
        assertThat(settings.getExposeAdmin(), is(true));
        settings.set(Settings.EXPOSE_ADMIN, "");
        assertThat(settings.getExposeAdmin(), is(false));
        settings.set(Settings.EXPOSE_ADMIN, " what ");
        assertThat(settings.getExposeAdmin(), is(false));
    }

    @Test
    public void testShowOtherMemberInformation() {
        final Settings settings = new Settings();
        assertThat(settings.getShareTrustees(), is(true));
        settings.set(Settings.SHOW_TRUSTEES, "false");
        assertThat(settings.getShareTrustees(), is(false));
        settings.set(Settings.SHOW_TRUSTEES, "true");
        assertThat(settings.getShareTrustees(), is(true));
        settings.set(Settings.SHOW_TRUSTEES, " true ");
        assertThat(settings.getShareTrustees(), is(true));
        settings.set(Settings.SHOW_TRUSTEES, "");
        assertThat(settings.getShareTrustees(), is(false));
        settings.set(Settings.SHOW_TRUSTEES, " what ");
        assertThat(settings.getShareTrustees(), is(false));
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
