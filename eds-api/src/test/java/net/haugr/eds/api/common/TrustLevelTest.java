/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.common;

import static net.haugr.eds.api.common.TrustLevel.ADMIN;
import static net.haugr.eds.api.common.TrustLevel.ALL;
import static net.haugr.eds.api.common.TrustLevel.READ;
import static net.haugr.eds.api.common.TrustLevel.SYSOP;
import static net.haugr.eds.api.common.TrustLevel.WRITE;
import static net.haugr.eds.api.common.TrustLevel.isAllowed;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class TrustLevelTest {

    @Test
    void testIsAllowed() {
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
