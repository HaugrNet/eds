package io.javadog.cws.api.common;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ConstantsTest {

    /**
     * <p>Private methods should never be tested, as they are part of an
     * internal workflow. Classes should always be tested via their contract,
     * i.e. public methods.</p>
     *
     * <p>However, for Utility Classes, with a Private Constructor, the contract
     * disallows instantiation, so the constructor is thus not testable via
     * normal means. This little Test method will verify that the contract is
     * kept, and that the Constructor is not made public.</p>
     */
    @Test
    public void testPrivateConstructor() {
        try {
            final Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
            assertThat(constructor.isAccessible(), is(false));
            constructor.setAccessible(true);
            final Constants mapper = constructor.newInstance();
            assertThat(mapper, is(not(nullValue())));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
            fail("Could not invoke Private Constructor: " + e.getMessage());
        }
    }
}
