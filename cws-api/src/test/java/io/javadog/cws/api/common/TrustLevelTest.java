/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import static io.javadog.cws.api.common.TrustLevel.ADMIN;
import static io.javadog.cws.api.common.TrustLevel.READ;
import static io.javadog.cws.api.common.TrustLevel.WRITE;
import static io.javadog.cws.api.common.TrustLevel.isAllowed;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrustLevelTest {

    @Test
    public void testIsAllowed() {
        assertThat(isAllowed(READ, READ), is(true));
        assertThat(isAllowed(READ, WRITE), is(false));
        assertThat(isAllowed(READ, ADMIN), is(false));

        assertThat(isAllowed(WRITE, READ), is(true));
        assertThat(isAllowed(WRITE, WRITE), is(true));
        assertThat(isAllowed(WRITE, ADMIN), is(false));

        assertThat(isAllowed(ADMIN, READ), is(true));
        assertThat(isAllowed(ADMIN, WRITE), is(true));
        assertThat(isAllowed(ADMIN, ADMIN), is(true));
    }
}
