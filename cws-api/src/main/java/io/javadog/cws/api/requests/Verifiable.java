/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Constants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Map;

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
 * @since  CWS 1.0
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

    protected static void checkNotNull(final Map<String, String> errors, final String field, final Object value, final String message) {
        if (value == null) {
            errors.put(field, message);
        }
    }

    protected static void checkNotNullOrEmpty(final Map<String, String> errors, final String field, final byte[] value, final String message) {
        if ((value == null) || (value.length == 0)) {
            errors.put(field, message);
        }
    }

    protected static void checkNotNullOrEmpty(final Map<String, String> errors, final String field, final String value, final String message) {
        if ((value == null) || value.trim().isEmpty()) {
            errors.put(field, message);
        }
    }

    protected static void checkNotNullEmptyOrTooLong(final Map<String, String> errors, final String field, final String value, final int maxLength, final String message) {
        checkNotNullOrEmpty(errors, field, value, message);
        checkNotTooLong(errors, field, value, maxLength, message);
    }

    protected static void checkNotTooLong(final Map<String, String> errors, final String field, final String value, final int maxLength, final String message) {
        if ((value != null) && (value.trim().length() > maxLength)) {
            errors.put(field, message);
        }
    }

    protected static void checkValidId(final Map<String, String> errors, final String field, final String value, final String message) {
        if ((value != null) && !Constants.ID_PATTERN.matcher(value).matches()) {
            errors.put(field, message);
        }
    }

    protected static void checkNotNullAndValidId(final Map<String, String> errors, final String field, final String value, final String message) {
        checkNotNull(errors, field, value, message);
        checkValidId(errors, field, value, message);
    }

    protected static void checkIntegerWithMax(final Map<String, String> errors, final String field, final int value, final int max, final String message) {
        if ((value < 1) || (value > max)) {
            errors.put(field, message);
        }
    }
}
