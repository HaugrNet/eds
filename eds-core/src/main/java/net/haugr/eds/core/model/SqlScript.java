/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Splits a plain SQL script into its statements, so it can be run via
 * JDBC, which only accepts one statement at a time. Statements are separated
 * by semicolons; semicolons inside line comments (double dash), block comments
 * (slash-star to star-slash), string literals and quoted identifiers are
 * ignored.
 * Comments are removed from the statements.</p>
 *
 * <p>Vendor specific constructs, such as PostgreSQL dollar quoting, are not
 * supported, as the EDS schema must only use SQL which all supported
 * databases understand.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class SqlScript {

    private SqlScript() {
        // Utility class
    }

    /**
     * Splits the given script into statements.
     *
     * @param script The SQL script
     * @return The statements, without comments and trailing semicolons
     */
    public static List<String> split(final String script) {
        final List<String> statements = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        int i = 0;
        while (i < script.length()) {
            i = step(script, i, current, statements);
        }
        addStatement(statements, current);

        return statements;
    }

    /**
     * Takes what starts at the given position: a comment, a quoted part, the
     * end of a statement, or a single character.
     *
     * @return The position after it
     */
    private static int step(final String script, final int i, final StringBuilder current, final List<String> statements) {
        final int length = script.length();
        final char c = script.charAt(i);
        final char next = (i + 1 < length) ? script.charAt(i + 1) : '\0';

        if (c == '-' && next == '-') {
            // Line comment, skip to the end of the line
            final int end = script.indexOf('\n', i);
            return (end < 0) ? length : end;
        }
        if (c == '/' && next == '*') {
            // Block comment, skip to the closing marker
            final int end = script.indexOf("*/", i + 2);
            current.append(' ');
            return (end < 0) ? length : (end + 2);
        }
        if (c == '\'' || c == '"') {
            // String literal or quoted identifier; a doubled quote is an
            // escaped quote, which the loop handles as two literals
            final int end = script.indexOf(c, i + 1);
            final int stop = (end < 0) ? length : (end + 1);
            current.append(script, i, stop);
            return stop;
        }
        if (c == ';') {
            addStatement(statements, current);
        } else {
            current.append(c);
        }

        return i + 1;
    }

    private static void addStatement(final List<String> statements, final StringBuilder current) {
        final String statement = current.toString().strip();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }
}
