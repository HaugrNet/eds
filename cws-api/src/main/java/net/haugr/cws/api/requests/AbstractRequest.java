/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.api.requests;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import net.haugr.cws.api.common.Constants;

/**
 * <p>This Class contains checks for different fields that is used as part of
 * the input and output Objects. Reason for having this, is because it is
 * important that all data is 100% reliable when it is coming in so it can be
 * processed correctly without any errors occurring.</p>
 *
 * <p>Although all Classes and Fields are annotated with the necessary checks,
 * this is not a guarantee that the data will also correctly be passed through,
 * since different frameworks may choose to discard the Annotated requirements
 * or have flaws. Hence, this simple PoJo approach will act as a last line of
 * defense before data is being processed.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public abstract class AbstractRequest implements Serializable {

    /**
     * {@link Constants#SERIAL_VERSION_UID}.
     */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * Simple Validation method, which checks if the required values are usable
     * or not.
     *
     * @return Map with Fields and error information
     */
    public abstract Map<String, String> validate();

    /**
     * <p>Checks that the given Object is not null, if so - then the given
     * message will be added to the error map, using the field as key.</p>
     *
     * @param errors  Error Map
     * @param field   Key for the Error Map
     * @param value   The value Object to check
     * @param message Message to add to Error Map, if validation failed
     */
    protected static void checkNotNull(final Map<String, String> errors, final String field, final Object value, final String message) {
        if (value == null) {
            errors.put(field, message);
        }
    }

    /**
     * <p>Checks a byte array, to ensure that that it is neither null nor empty,
     * if the validation failed, then the given message will be added to the
     * error map with the field as key.</p>
     *
     * @param errors  Error Map
     * @param field   Key for the Error Map
     * @param value   The byte array to check
     * @param message Message to add to Error Map, if validation failed
     */
    protected static void checkNotNullOrEmpty(final Map<String, String> errors, final String field, final byte[] value, final String message) {
        if ((value == null) || (value.length == 0)) {
            errors.put(field, message);
        }
    }

    /**
     * <p>Checks if the given String is neither null nor empty, if the check
     * failed, then the given message will be added to the error map using the
     * field as key.</p>
     *
     * @param errors  Error Map
     * @param field   Key for the Error Map
     * @param value   The String to check that it is neither null nor empty
     * @param message Message to add to Error Map, if validation failed
     * @see #isEmpty(String)
     */
    protected static void checkNotNullOrEmpty(final Map<String, String> errors, final String field, final String value, final String message) {
        if ((value == null) || isEmpty(value)) {
            errors.put(field, message);
        }
    }

    /**
     * <p>Checks that the given String is neither null, empty, nor too long. The
     * method is a shorthand method invoking the
     * {@link #checkNotNullOrEmpty(Map, String, String, String)} &amp;
     * {@link #checkNotTooLong(Map, String, String, int, String)} methods.</p>
     *
     * @param errors    Error Map
     * @param field     Key for the Error Map
     * @param value     The value Object to check
     * @param maxLength The maximum length the given String may have
     * @param message   Message to add to Error Map, if validation failed
     * @see #checkNotNullOrEmpty(Map, String, String, String)
     * @see #checkNotTooLong(Map, String, String, int, String)
     */
    protected static void checkNotNullEmptyOrTooLong(final Map<String, String> errors, final String field, final String value, final int maxLength, final String message) {
        checkNotNullOrEmpty(errors, field, value, message);
        checkNotTooLong(errors, field, value, maxLength, message);
    }

    /**
     * <p>Checks that the given String is neither null, nor that the trimmed
     * version of the String exceeds the given maximum length. If the validation
     * failed, then the message will be added to the error map, using the field
     * as key.</p>
     *
     * @param errors    Error Map
     * @param field     Key for the Error Map
     * @param value     The String to check
     * @param maxLength The max length for the given String value
     * @param message   Message to add to Error Map, if validation failed
     */
    protected static void checkNotTooLong(final Map<String, String> errors, final String field, final String value, final int maxLength, final String message) {
        if ((value != null) && (value.trim().length() > maxLength)) {
            errors.put(field, message);
        }
    }

    /**
     * <p>Checks if the given Id is valid, i.e. that it matches the standard
     * UUID regular expression. The method takes an error map which it will add
     * the given field to as a key with the message provided, if the id (value)
     * it invalid.</p>
     *
     * @param errors  Map to store the error information in, if id is invalid
     * @param field   Name of the field from the request/dto holding the value
     * @param value   The value to check if is a valid ID (UUID)
     * @param message The error message to add to the error map
     * @see Constants#ID_PATTERN_REGEX
     */
    protected static void checkValidId(final Map<String, String> errors, final String field, final String value, final String message) {
        if ((value != null) && !Constants.ID_PATTERN.matcher(value).matches()) {
            errors.put(field, message);
        }
    }

    /**
     * <p>Checks that the given value is neither null, nor an invalid Id. The
     * method is a shorthand method invoking the
     * {@link #checkNotNull(Map, String, Object, String)} &amp;
     * {@link #checkValidId(Map, String, String, String)} methods.</p>
     *
     * @param errors  Error Map
     * @param field   Key for the Error Map
     * @param value   The value Object to check
     * @param message Message to add to Error Map, if validation failed
     * @see #checkNotNull(Map, String, Object, String)
     * @see #checkValidId(Map, String, String, String)
     */
    protected static void checkNotNullAndValidId(final Map<String, String> errors, final String field, final String value, final String message) {
        checkNotNull(errors, field, value, message);
        checkValidId(errors, field, value, message);
    }

    /**
     * <p>Checks the given Integer value, to ensure that it is valid, i.e.
     * within the values 0 (zero) and the given max. If not, then the message
     * is added to the error map with the given field as key.</p>
     *
     * @param errors  Error Map
     * @param field   Key for the Error Map
     * @param value   The Integer to check
     * @param max     The maximum allowed value for the checked Integer
     * @param message Message to add to Error Map, if validation failed
     */
    protected static void checkIntegerWithMax(final Map<String, String> errors, final String field, final int value, final int max, final String message) {
        if ((value < 1) || (value > max)) {
            errors.put(field, message);
        }
    }

    /**
     * <p>Checks if a URL is valid or not, it uses the URL methods to see if it
     * is valid, and if not - the given error map is extended with the error
     * information from the Exception thrown. The method is thus not logging ot
     * re-throwing the exception, as the method is only intended as a
     * pre-processing check.</p>
     *
     * <p>The key for the error map, is the {@link Constants#FIELD_URL}, which
     * is the only CWS field holding a URL, and thus it is not a parameter to
     * this method, although it is for the other methods.</p>
     *
     * @param errors Map to store the error information in, if URL is invalid
     * @param value  the URL to check
     * @see Constants#FIELD_URL
     */
    protected static void checkUrl(final Map<String, String> errors, final String value) {
        try {
            final URL url = new URL(value);
            url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            // SonarQube Warning java:S1166 - Ignored here
            // The error information from the Exception is added to the
            // error Object, which again is returned. Logging it here
            // would be pointless and thus the Sonar warning is ignored
            // at this place.
            errors.put(Constants.FIELD_URL, "The URL field is invalid - " + e.getMessage());
        }
    }

    /**
     * <p>Method added as per PMD rule InefficientEmptyStringCheck. It checks
     * the given value to see if it contains any non-whitespace characters. If
     * so, then it returns false - if the value has zero length or only
     * whitespace characters, then it returns true.</p>
     *
     * @param value Nullable value to check if is empty
     * @return True if the string is empty, meaning no non-whitespace chars exist
     */
    public static boolean isEmpty(final String value) {
        boolean whitespace = true;

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                whitespace = false;
                break;
            }
        }

        return whitespace;
    }
}
