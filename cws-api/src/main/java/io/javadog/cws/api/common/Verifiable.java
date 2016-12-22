package io.javadog.cws.api.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class Verifiable implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
    private static final String PRE_VALUE = "The value for '";

    /**
     * Simple Validation method, which checks if the required values are usable
     * or not.
     *
     * @return Map with Fields and error information
     */
    public abstract Map<String, String> validate();

    protected static void ensureNotNull(final String field, final Object value) {
        if (value == null) {
            throw new IllegalArgumentException(PRE_VALUE + field + "' may not be null.");
        }
    }

    protected void ensurePattern(final String field, final String value, final String regex) {
        if (value != null) {
            final Pattern pattern = Pattern.compile(regex);
            if (!pattern.matcher(value).matches()) {
                throw new IllegalArgumentException(PRE_VALUE + field + "' is not matching the required pattern '" + regex + "'.");
            }
        }
    }

    protected static <E extends Enum<?>> void ensureValidEntry(final String field, final E value, final Collection<E> acceptable) {
        if (!acceptable.contains(value)) {
            throw new IllegalArgumentException(PRE_VALUE + field + "' is not allowed.");
        }
    }

    protected void ensureMaxLength(final String field, final String value, final int max) {
        if ((value != null) && (value.length() > max)) {
            throw new IllegalArgumentException(PRE_VALUE + field + "' is too long.");
        }
    }

    protected static void ensureLength(final String field, final String value, final int min, final int max) {
        if ((value != null) && ((value.length() < min) || (value.length() > max))) {
            throw new IllegalArgumentException(PRE_VALUE + field + "' is outside of the allowed boundaries.");
        }
    }
}
