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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

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
 * @since CWS 1.0
 */
final class PrivateConstructorTest {

    @Test
    void testConstantsConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        final Constants constants = constructor.newInstance();
        assertNotNull(constants);
    }

    @Test
    void testUtilitiesConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor<Utilities> constructor = Utilities.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        final Utilities utilities = constructor.newInstance();
        assertNotNull(utilities);
    }
}
