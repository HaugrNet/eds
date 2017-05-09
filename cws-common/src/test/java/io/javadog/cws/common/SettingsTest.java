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

import io.javadog.cws.common.exceptions.SettingException;
import org.junit.Test;

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
        assertThat(existing.size(), is(13));
        settings.set("my.new.key", "the awesome value");

        final Map<String, String> updated = settings.get();
        assertThat(updated.size(), is(existing.size() + 1));
    }

    @Test
    public void testReadingDefaultSettings() {
        final Settings settings = new Settings();

        assertThat(settings.getSymmetricAlgorithm(), is("AES/CBC/PKCS5Padding"));
        assertThat(settings.getSymmetricAlgorithmName(), is("AES"));
        assertThat(settings.getSymmetricCipherMode(), is("CBC"));
        assertThat(settings.getSymmetricKeylength(), is(128));
        assertThat(settings.getAsymmetricAlgorithmName(), is("RSA"));
        assertThat(settings.getAsymmetricKeylength(), is(2048));
        assertThat(settings.getSignatureAlgorithm(), is("SHA512WithRSA"));
        assertThat(settings.getPBEAlgorithm(), is("PBKDF2WithHmacSHA256"));
        assertThat(settings.getSalt(), is("Default Salt, please make sure it is set in the DB instead."));
        assertThat(settings.getCharset().name(), is("UTF-8"));
    }

    @Test
    public void testUpdateDefaultSettings() {
        final Settings settings = new Settings();

        settings.set(Settings.SYMMETRIC_ALGORITHM_NAME, "DES");
        settings.set(Settings.SYMMETRIC_ALGORITHM_MODE, "ECB");
        settings.set(Settings.SYMMETRIC_ALGORITHM_PADDING, "NoPadding");
        assertThat(settings.getSymmetricAlgorithm(), is("DES/ECB/NoPadding"));

        settings.set(Settings.SYMMETRIC_ALGORITHM_KEYLENGTH, "256");
        assertThat(settings.getSymmetricKeylength(), is(256));

        settings.set(Settings.ASYMMETRIC_ALGORITHM, "DSA");
        assertThat(settings.getAsymmetricAlgorithmName(), is("DSA"));

        settings.set(Settings.ASYMMETRIC_ALGORITHM_KEYLENGTH, "4096");
        assertThat(settings.getAsymmetricKeylength(), is(4096));

        settings.set(Settings.PBE_ALGORITHM, "PBEWithMD5AndTripleDES");
        assertThat(settings.getPBEAlgorithm(), is("PBEWithMD5AndTripleDES"));

        settings.set(Settings.CWS_SALT, "UUID value");
        assertThat(settings.getSalt(), is("UUID value"));

        settings.set(Settings.CWS_CHARSET, "ISO-8859-15");
        assertThat(settings.getCharset().name(), is("ISO-8859-15"));
    }

    @Test(expected = SettingException.class)
    public void testSetInvalidKeylength() {
        final Settings settings = new Settings();
        settings.set(Settings.ASYMMETRIC_ALGORITHM_KEYLENGTH, "Long");
        settings.getAsymmetricKeylength();
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
