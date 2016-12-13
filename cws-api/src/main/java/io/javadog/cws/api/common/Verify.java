package io.javadog.cws.api.common;

import java.io.Serializable;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class Verify implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    protected static void ensureNotNull(final String field, final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("The field '" + field + "' may not be null.");
        }
    }

    protected static void ensureLength(final String field, final String value, final int min, final int max) {
        if ((value != null) && ((value.length() < min) || (value.length() > max))) {
            throw new IllegalArgumentException("The field '" + field+ "' is outside of the allowed boundaries.");
        }
    }
}
