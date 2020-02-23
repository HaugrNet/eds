/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.2
 */
final class UtilitiesTest {

    @Test
    void testNewDate() {
        final Date date1 = new Date();
        final Date date2 = Utilities.newDate();
        final Date copy1 = Utilities.newDate(date1.getTime());
        final Date copy2 = Utilities.newDate(date2.getTime());

        assertEquals(date1, copy1);
        assertNotSame(date1, copy1);

        assertEquals(date2, copy2);
        assertNotSame(date2, copy2);
    }

    @Test
    void testCopyDate() {
        final Date date1 = new Date();
        final Date date2 = Utilities.newDate();
        final Date date3 = null;
        final Date copy1 = Utilities.copy(date1);
        final Date copy2 = Utilities.copy(date2);
        final Date copy3 = Utilities.copy(date3);

        assertEquals(date1, copy1);
        assertNotSame(date1, copy1);

        assertEquals(date2, copy2);
        assertNotSame(date2, copy2);

        assertEquals(date3, copy3);
    }

    @Test
    void testCopyBytes() {
        final byte[] bytes1 = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10 };
        final byte[] bytes2 = { (byte) 10, (byte) 9, (byte) 8, (byte) 7, (byte) 6, (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1 };
        final byte[] copy1 = Utilities.copy(bytes1);
        final byte[] copy2 = Utilities.copy(bytes2);

        assertEquals(bytes1, copy1);
        assertSame(bytes1, copy1);
        assertEquals(bytes2, copy2);
        assertSame(bytes2, copy2);
    }
}
