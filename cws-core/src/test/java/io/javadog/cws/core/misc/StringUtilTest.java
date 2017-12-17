/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class StringUtilTest {

    @Test
    public void testStringUtilConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<StringUtil> constructor = StringUtil.class.getDeclaredConstructor();
        assertThat(constructor.isAccessible(), is(false));
        constructor.setAccessible(true);
        final StringUtil constants = constructor.newInstance();
        assertThat(constants, is(not(nullValue())));
    }

    @Test
    public void testRequestDuration() {
        final long startTime = System.nanoTime();
        final String request = "theRequest";
        final String requestDuration = StringUtil.requestDuration(Locale.ENGLISH, request, startTime);
        assertThat(requestDuration, containsString(request + " completed in "));
    }
}
