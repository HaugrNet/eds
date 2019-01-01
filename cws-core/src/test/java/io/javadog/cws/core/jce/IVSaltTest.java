/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.DatabaseSetup;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public class IVSaltTest extends DatabaseSetup {

    @Test
    public void testSaltAsUUID() {
        final String uuid = UUID.randomUUID().toString();
        final IVSalt salt = new IVSalt(uuid);

        assertThat(salt.getArmored(), is(uuid));
        assertThat(salt.getBytes().length, is(16));
    }

    @Test
    public void testDefaultSalt() {
        final IVSalt salt = new IVSalt();

        assertThat(salt.getArmored().length(), is(24));
        assertThat(salt.getBytes().length, is(16));
    }

    @Test
    public void testSecureRandom16Bytes() {
        final byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        final String armored = Base64.getEncoder().encodeToString(random);

        final IVSalt salt = new IVSalt(armored);
        final byte[] bytes = salt.getBytes();

        assertThat(salt.getArmored(), is(armored));
        assertThat(bytes, is(random));
    }

    /**
     * <p>The problem with the IVSalt changes between CWS 1.0 &amp; 1.1, is
     * that in CWS 1.0, the Salt was generated using a UUID, which was not
     * stored using Base64 encoding, since it was persisted raw. Since the code
     * is checking for an exception, this code is generating a random array
     * based on SecureRandom, and &quot;String&quot; armoring encoding it.</p>
     *
     * <p>As the logic is using Base64 logic to convert it, the source and the
     * decoded target will differ - which the test is verifying.</p>
     */
    @Test
    public void testSecureRandom24Bytes() {
        final byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        final String armored = new String(random, settings.getCharset());

        final IVSalt salt = new IVSalt(armored);
        final byte[] bytes = salt.getBytes();

        assertThat(salt.getArmored(), is(armored));
        assertThat(bytes, is(not(random)));
    }
}
