/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
package io.javadog.cws.core.misc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.exceptions.CWSException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class LoggingUtilTest {

    @Test
    void testLoggingUtilConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<LoggingUtil> constructor = LoggingUtil.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));
        constructor.setAccessible(true);
        final LoggingUtil constants = constructor.newInstance();
        assertNotNull(constants);
    }

    @Test
    void testRequestDuration() {
        final long startTime = System.nanoTime();
        final String request = "theRequest";
        final String requestDuration = LoggingUtil.requestDuration(Locale.ENGLISH, request, startTime);
        assertTrue(requestDuration.contains(request + " completed in "));
    }

    @Test
    void testRequestDurationWithException() {
        final long startTime = System.nanoTime();
        final String request = "theRequest";
        final CWSException exception = new CWSException(ReturnCode.ERROR, "An error occurred");
        final String requestDuration = LoggingUtil.requestDuration(Locale.ENGLISH, request, startTime, exception);
        assertTrue(requestDuration.contains(request + " completed in "));
    }
}
