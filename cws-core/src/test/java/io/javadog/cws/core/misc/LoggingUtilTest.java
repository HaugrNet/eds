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
package io.javadog.cws.core.misc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.exceptions.CWSException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class LoggingUtilTest {

    @Test
    public void testLoggingUtilConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<LoggingUtil> constructor = LoggingUtil.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        final LoggingUtil constants = constructor.newInstance();
        assertNotNull(constants);
    }

    @Test
    public void testRequestDuration() {
        final long startTime = System.nanoTime();
        final String request = "theRequest";
        final String requestDuration = LoggingUtil.requestDuration(Locale.ENGLISH, request, startTime);
        assertThat(requestDuration, containsString(request + " completed in "));
    }

    @Test
    public void testRequestDurationWithException() {
        final long startTime = System.nanoTime();
        final String request = "theRequest";
        final CWSException exception = new CWSException(ReturnCode.ERROR, "An error occurred");
        final String requestDuration = LoggingUtil.requestDuration(Locale.ENGLISH, request, startTime, exception);
        assertThat(requestDuration, containsString(request + " completed in "));
    }
}
