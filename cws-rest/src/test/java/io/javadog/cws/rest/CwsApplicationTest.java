/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CwsApplicationTest {

    @Test
    public void testApplication() {
        final CwsApplication application = new CwsApplication();
        assertThat(application.getClasses().size(), is(10));
    }
}
