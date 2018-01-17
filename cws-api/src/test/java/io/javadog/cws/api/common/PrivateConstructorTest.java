/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * <p>Private methods should never be tested, as they are part of an
 * internal workflow. Classes should always be tested via their contract,
 * i.e. public methods.</p>
 *
 * <p>However, for Utility Classes, with a Private Constructor, the contract
 * disallows instantiation, so the constructor is thus not testable via
 * normal means. This little Test method will verify that the contract is
 * kept, and that the Constructor is not made public.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class PrivateConstructorTest {

    @Test
    public void testConstantsConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        assertThat(constructor.isAccessible(), is(false));
        constructor.setAccessible(true);
        final Constants constants = constructor.newInstance();
        assertThat(constants, is(not(nullValue())));
    }

    @Test
    public void testUtilitiesConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<Utilities> constructor = Utilities.class.getDeclaredConstructor();
        assertThat(constructor.isAccessible(), is(false));
        constructor.setAccessible(true);
        final Utilities utilities = constructor.newInstance();
        assertThat(utilities, is(not(nullValue())));
    }
}
