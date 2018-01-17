/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.misc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class LoggingUtilTest {

    @Test
    public void testLoggingUtilConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<LoggingUtil> constructor = LoggingUtil.class.getDeclaredConstructor();
        assertThat(constructor.isAccessible(), is(false));
        constructor.setAccessible(true);
        final LoggingUtil constants = constructor.newInstance();
        assertThat(constants, is(not(nullValue())));
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
