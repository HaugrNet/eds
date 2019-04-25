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
package io.javadog.cws.fitnesse.utils;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.fitnesse.exceptions.StopTestException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class Converter {

    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Private Constructor, this is a utility class.
     */
    private Converter() {
    }

    public static Action findAction(final String action) {
        final String tmp = toUpper(action);
        return (tmp != null) ? Action.valueOf(tmp) : null;
    }

    public static CredentialType findCredentialType(final String credentialType) {
        final String tmp = toUpper(credentialType);
        return (tmp != null) ? CredentialType.valueOf(tmp) : null;
    }

    public static MemberRole findMemberRole(final String memberRole) {
        final String tmp = toUpper(memberRole);
        return (tmp != null) ? MemberRole.valueOf(tmp) : null;
    }

    private static String toUpper(final String source) {
        final String tmp = preCheck(source);
        return (tmp != null) ? tmp.trim().toUpperCase(LOCALE) : null;
    }

    public static int parseInt(final String value, final int defaultValue) {
        final String checked = preCheck(value);
        int result = defaultValue;

        if ((checked != null) && !checked.trim().isEmpty()) {
            result = Integer.parseInt(value.trim());
        }

        return result;
    }

    public static String preCheck(final String source) {
        String checked = source;

        if ((source == null) || "null".equalsIgnoreCase(source.trim())) {
            checked = null;
        }

        return checked;
    }

    public static String convertDate(final Date date) {
        String converted = null;

        if (date != null) {
            final Format formatter = new SimpleDateFormat(DATE_FORMAT, LOCALE);
            converted = formatter.format(date);
        }

        return converted;
    }

    public static Date convertDate(final String date) {
        final String checked = preCheck(date);
        Date converted = null;

        if (checked != null) {
            try {
                final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT, LOCALE);
                converted = formatter.parse(checked);
            } catch (ParseException e) {
                throw new StopTestException(e);
            }
        }

        return converted;
    }

    public static String convertBytes(final byte[] bytes) {
        return new String(bytes, CHARSET);
    }

    public static byte[] convertBytes(final String str) {
        final String tmp = preCheck(str);

        return (tmp != null) ? str.getBytes(CHARSET) : null;
    }
}
