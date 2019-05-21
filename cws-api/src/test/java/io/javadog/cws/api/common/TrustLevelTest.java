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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class TrustLevelTest {

    @Test
    public void testIsAllowed() {
        assertTrue(isAllowed(ALL, ALL));
        assertFalse(isAllowed(ALL, READ));
        assertFalse(isAllowed(ALL, WRITE));
        assertFalse(isAllowed(ALL, ADMIN));
        assertFalse(isAllowed(ALL, SYSOP));

        assertTrue(isAllowed(READ, ALL));
        assertTrue(isAllowed(READ, READ));
        assertFalse(isAllowed(READ, WRITE));
        assertFalse(isAllowed(READ, ADMIN));
        assertFalse(isAllowed(READ, ADMIN));

        assertTrue(isAllowed(WRITE, ALL));
        assertTrue(isAllowed(WRITE, READ));
        assertTrue(isAllowed(WRITE, WRITE));
        assertFalse(isAllowed(WRITE, ADMIN));
        assertFalse(isAllowed(WRITE, SYSOP));

        assertTrue(isAllowed(ADMIN, ALL));
        assertTrue(isAllowed(ADMIN, READ));
        assertTrue(isAllowed(ADMIN, WRITE));
        assertTrue(isAllowed(ADMIN, ADMIN));
        assertFalse(isAllowed(ADMIN, SYSOP));

        assertTrue(isAllowed(SYSOP, ALL));
        assertTrue(isAllowed(SYSOP, READ));
        assertTrue(isAllowed(SYSOP, WRITE));
        assertTrue(isAllowed(SYSOP, ADMIN));
        assertTrue(isAllowed(SYSOP, SYSOP));
    }
}
