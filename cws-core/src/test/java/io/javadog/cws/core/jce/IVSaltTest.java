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

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public class IVSaltTest {

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
}
