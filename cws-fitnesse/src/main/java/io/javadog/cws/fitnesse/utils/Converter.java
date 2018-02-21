/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse.utils;

import io.javadog.cws.fitnesse.exceptions.StopTestException;

import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Converter {

    private static final Locale LOCALE = Locale.ENGLISH;
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Charset CHARSET = Charset.forName("UTF-8");

    public static String convertDate(final Date date) {
        final Format formatter = new SimpleDateFormat(DATE_FORMAT, LOCALE);
        return formatter.format(date);
    }

    public static Date convertDate(final String date) {
        try {
            final DateFormat formatter = new SimpleDateFormat(DATE_FORMAT, LOCALE);
            return formatter.parse(date);
        } catch (ParseException e) {
            throw new StopTestException(e);
        }
    }

    public static String convertBytes(final byte[] bytes) {
        return new String(bytes, CHARSET);
    }

    public static byte[] convertBytes(final String bytes) {
        return bytes.getBytes(CHARSET);
    }
}
