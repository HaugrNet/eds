/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
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
