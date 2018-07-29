/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public class SaltTest {

    @Test
    public void testSaltAsUUID() {
        final String uuid = UUID.randomUUID().toString();
        final Salt salt = new Salt(uuid);

        assertThat(salt.getArmored(), is(uuid));
        assertThat(salt.getBytes(), is(uuid.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testDefaultSalt() {
        final Salt salt = new Salt();

        assertThat(salt.getArmored().length(), is(24));
        assertThat(salt.getBytes().length, is(16));
    }
}
