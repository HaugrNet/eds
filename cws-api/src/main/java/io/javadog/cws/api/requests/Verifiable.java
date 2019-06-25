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
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

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
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "verifiable")
public abstract class Verifiable implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * Simple Validation method, which checks if the required values are usable
     * or not.
     *
     * @return Map with Fields and error information
     */
    public abstract Map<String, String> validate();

    static void checkNotNull(final Map<String, String> errors, final String field, final Object value, final String message) {
        if (value == null) {
            errors.put(field, message);
        }
    }

    static void checkNotNullOrEmpty(final Map<String, String> errors, final String field, final byte[] value, final String message) {
        if ((value == null) || (value.length == 0)) {
            errors.put(field, message);
        }
    }

    static void checkNotNullOrEmpty(final Map<String, String> errors, final String field, final String value, final String message) {
        if ((value == null) || isEmpty(value)) {
            errors.put(field, message);
        }
    }

    static void checkNotNullEmptyOrTooLong(final Map<String, String> errors, final String field, final String value, final int maxLength, final String message) {
        checkNotNullOrEmpty(errors, field, value, message);
        checkNotTooLong(errors, field, value, maxLength, message);
    }

    static void checkNotTooLong(final Map<String, String> errors, final String field, final String value, final int maxLength, final String message) {
        if ((value != null) && (value.trim().length() > maxLength)) {
            errors.put(field, message);
        }
    }

    static void checkValidId(final Map<String, String> errors, final String field, final String value, final String message) {
        if ((value != null) && !Constants.ID_PATTERN.matcher(value).matches()) {
            errors.put(field, message);
        }
    }

    static void checkNotNullAndValidId(final Map<String, String> errors, final String field, final String value, final String message) {
        checkNotNull(errors, field, value, message);
        checkValidId(errors, field, value, message);
    }

    static void checkIntegerWithMax(final Map<String, String> errors, final String field, final int value, final int max, final String message) {
        if ((value < 1) || (value > max)) {
            errors.put(field, message);
        }
    }

    static void checkUrl(final Map<String, String> errors, final String value) {
        try {
            final URL url = new URL(value);
            url.toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            // The error information from the Exception is added to the
            // error Object, which again is returned. Logging it here
            // would be pointless and thus the Sonar warning is ignored
            // at this place.
            errors.put(Constants.FIELD_URL, "The URL field is invalid - " + e.getMessage());
        }
    }

    /**
     * Method added as per PMD rule InefficientEmptyStringCheck. It checks the
     * given value to see if it contains any non-whitespace characters. If so,
     * then it returns false - if the value has zero length or only whitespace
     * characters, then it returns true.
     *
     * @param value Nullable value to check if is empty
     * @return True if the string is empty, meaning no non-whitespace chars exist
     */
    private static boolean isEmpty(final String value) {
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
