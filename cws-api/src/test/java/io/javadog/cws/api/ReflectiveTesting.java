/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api;

import java.lang.reflect.Field;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ReflectiveTesting {

    private ReflectiveTesting() {
        // Empty Constructor, this is a Utility Class.
    }

    public static void reflectiveCorrection(final Object instance, final String fieldName, final Object newValue) {
        try {
            final Field field = instance.getClass().getDeclaredField(fieldName);
            final boolean isAccessible = field.isAccessible();
            field.setAccessible(true);
            field.set(instance, newValue);
            field.setAccessible(isAccessible);
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
