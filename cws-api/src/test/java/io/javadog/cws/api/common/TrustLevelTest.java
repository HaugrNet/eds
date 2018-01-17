/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import static io.javadog.cws.api.common.TrustLevel.ADMIN;
import static io.javadog.cws.api.common.TrustLevel.ALL;
import static io.javadog.cws.api.common.TrustLevel.READ;
import static io.javadog.cws.api.common.TrustLevel.SYSOP;
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
        assertThat(isAllowed(ALL, ALL), is(true));
        assertThat(isAllowed(ALL, READ), is(false));
        assertThat(isAllowed(ALL, WRITE), is(false));
        assertThat(isAllowed(ALL, ADMIN), is(false));
        assertThat(isAllowed(ALL, SYSOP), is(false));

        assertThat(isAllowed(READ, ALL), is(true));
        assertThat(isAllowed(READ, READ), is(true));
        assertThat(isAllowed(READ, WRITE), is(false));
        assertThat(isAllowed(READ, ADMIN), is(false));
        assertThat(isAllowed(READ, ADMIN), is(false));

        assertThat(isAllowed(WRITE, ALL), is(true));
        assertThat(isAllowed(WRITE, READ), is(true));
        assertThat(isAllowed(WRITE, WRITE), is(true));
        assertThat(isAllowed(WRITE, ADMIN), is(false));
        assertThat(isAllowed(WRITE, SYSOP), is(false));

        assertThat(isAllowed(ADMIN, ALL), is(true));
        assertThat(isAllowed(ADMIN, READ), is(true));
        assertThat(isAllowed(ADMIN, WRITE), is(true));
        assertThat(isAllowed(ADMIN, ADMIN), is(true));
        assertThat(isAllowed(ADMIN, SYSOP), is(false));

        assertThat(isAllowed(SYSOP, ALL), is(true));
        assertThat(isAllowed(SYSOP, READ), is(true));
        assertThat(isAllowed(SYSOP, WRITE), is(true));
        assertThat(isAllowed(SYSOP, ADMIN), is(true));
        assertThat(isAllowed(SYSOP, SYSOP), is(true));
    }
}
