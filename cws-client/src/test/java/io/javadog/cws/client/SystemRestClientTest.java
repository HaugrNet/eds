/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SystemRestClientTest extends BaseSystemTest {

    @BeforeClass
    public static void before() {
        system = new SystemRestClient(BASE_URL);
    }

    /** All tests are in the abstract class. */
    @Test
    public void testDummy() {
        assertThat(Boolean.TRUE, is(true));
    }
}
