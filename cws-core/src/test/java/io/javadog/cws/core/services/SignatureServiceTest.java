/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * This Test Class, is testing the following Service Classes in one, as they are
 * all fairly small but also connected.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignatureServiceTest extends DatabaseSetup {

    // TODO test requesting data with a false root, i.e. a root for a different circle.

    @Test
    public void testDummy() {
        assertThat(Boolean.TRUE, is(true));
    }
}
