/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.common.enums.HashAlgorithm;
import io.javadog.cws.common.enums.KeyAlgorithm;
import org.junit.Test;

import java.util.Locale;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingsTest {

    @Test
    public void testSettings() {
        final Settings settings = new Settings();

        final Map<String, String> existing = settings.get();
        assertThat(existing.size(), is(10));
        settings.set("my.new.key", "the awesome value");

        final Map<String, String> updated = settings.get();
        assertThat(updated.size(), is(existing.size() + 1));
    }

    @Test
    public void testReadingDefaultSettings() {
        final Settings settings = new Settings();

        assertThat(settings.getSymmetricAlgorithm(), is(KeyAlgorithm.AES128));
        assertThat(settings.getAsymmetricAlgorithm(), is(KeyAlgorithm.RSA2048));
        assertThat(settings.getSignatureAlgorithm(), is(KeyAlgorithm.SHA512));
        assertThat(settings.getPasswordAlgorithm(), is(KeyAlgorithm.PBE128));
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

        settings.set(Settings.SIGNATURE_ALGORITHM, KeyAlgorithm.SHA256.name());
        assertThat(settings.getSignatureAlgorithm(), is(KeyAlgorithm.SHA256));

        settings.set(Settings.HASH_ALGORITHM, HashAlgorithm.SHA256.name());
        assertThat(settings.getHashAlgorithm(), is(HashAlgorithm.SHA256));

        settings.set(Settings.PBE_ALGORITHM, KeyAlgorithm.PBE192.name());
        assertThat(settings.getPasswordAlgorithm(), is(KeyAlgorithm.PBE192));

        settings.set(Settings.CWS_SALT, "UUID value");
        assertThat(settings.getSalt(), is("UUID value"));

        settings.set(Settings.CWS_LOCALE, Locale.GERMAN.toString());
        assertThat(settings.getLocale(), is(Locale.GERMAN));

        settings.set(Settings.CWS_CHARSET, "ISO-8859-15");
        assertThat(settings.getCharset().name(), is("ISO-8859-15"));
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
}
