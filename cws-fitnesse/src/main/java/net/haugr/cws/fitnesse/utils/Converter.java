/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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
package net.haugr.cws.fitnesse.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.CredentialType;
import net.haugr.cws.api.common.MemberRole;
import net.haugr.cws.api.requests.AbstractRequest;
import net.haugr.cws.fitnesse.exceptions.StopTestException;

/**
 * <p>Common Utilities for converting data between the FitNesse WIKI pages and
 * CWS.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class Converter {

    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern(DATE_FORMAT)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

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

        if ((checked != null) && !AbstractRequest.isEmpty(checked)) {
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

    public static String convertDate(final LocalDateTime date) {
        return date != null
                ? FORMATTER.format(date)
                : null;
    }

    public static LocalDateTime convertDate(final String date) {
        final String checked = preCheck(date);
        LocalDateTime converted = null;

        if (checked != null) {
            try {
                converted = LocalDateTime.from(FORMATTER.parse(checked));
            } catch (DateTimeParseException e) {
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
