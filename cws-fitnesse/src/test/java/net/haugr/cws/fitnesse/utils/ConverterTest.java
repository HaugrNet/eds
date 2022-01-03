/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.fitnesse.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.haugr.cws.api.common.Utilities;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 2.0
 */
final class ConverterTest {

    @Test
    void testConvertingLocalDateTime() {
        final var date = Utilities.newDate();

        // Convert the date to String, check that it is not null (Coverity finding)
        final var str = Converter.convertDate(date);
        assertNotNull(str);

        // Convert the String to date, check that it is not null (Coverity finding)
        final var converted = Converter.convertDate(str);
        assertNotNull(converted);

        // Not comparing the date - as the rest is not provided
        assertEquals(converted.getYear(), date.getYear());
        assertEquals(converted.getMonth(), date.getMonth());
        assertEquals(converted.getDayOfMonth(), date.getDayOfMonth());
    }
}
