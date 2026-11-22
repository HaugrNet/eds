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
package net.haugr.eds.core.services.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Reads the per-operation documentation: one Markdown file per tag in the
 * {@code operations} folder next to this class (tag "Data Types" is
 * {@code data-types.md}). Each operation is a section:</p>
 *
 * <pre>
 * ## POST /members/createMember
 * permission: PROCESS_MEMBER
 * - IDENTIFICATION_WARNING: When this code is returned.
 *
 * Markdown text: fields used, rules, ...
 * </pre>
 *
 * <p>The permission is a {@code Permission} name, or {@code NONE} for
 * operations without authentication. The listed return codes are the ones
 * specific to the operation; the common ones are added by the filter.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class OperationDocs {

    /**
     * The documentation of one operation.
     *
     * @param permission  Permission name, or NONE; null if taken from sameAs
     * @param returnCodes Operation-specific return codes, name to condition
     * @param body        Markdown text
     * @param sameAs      "METHOD /path" of the operation whose rules this
     *                    one shares (the ID-based twin of a POST operation),
     *                    or null
     */
    record Section(String permission, Map<String, String> returnCodes, String body, String sameAs) {
    }

    private static final String FOLDER = "operations/";
    private static final Pattern HEADER = Pattern.compile("^## ([A-Z]+) (\\S+)\\s*$");
    private static final Pattern PERMISSION = Pattern.compile("^permission:\\s*([A-Z_]+)\\s*$");
    // The condition is stripped where it is used, so no leading \s* here,
    // which would backtrack against the (.+) behind it.
    private static final Pattern RETURN_CODE = Pattern.compile("^- ([A-Z_]+):(.+)$");
    private static final Pattern SAME_AS = Pattern.compile("^same-as:\\s*([A-Z]+ \\S+)\\s*$");

    private OperationDocs() {
        // Utility class, no instances.
    }

    /**
     * File name for a tag: lower case, spaces replaced by dashes.
     *
     * @param tag The OpenAPI tag
     * @return The Markdown file name
     */
    static String fileName(final String tag) {
        return tag.toLowerCase(Locale.ROOT).replace(' ', '-') + ".md";
    }

    /**
     * Reads the sections for the given tag, keyed by "METHOD /path".
     *
     * @param tag The OpenAPI tag
     * @return The sections, empty if the tag has no documentation file
     */
    static Map<String, Section> load(final String tag) {
        final String text = read(FOLDER + fileName(tag));
        return (text == null) ? Collections.emptyMap() : parse(fileName(tag), text);
    }

    static Map<String, Section> parse(final String source, final String text) {
        final Map<String, Section> sections = new LinkedHashMap<>();
        String key = null;
        String permission = null;
        String sameAs = null;
        Map<String, String> returnCodes = new LinkedHashMap<>();
        StringBuilder body = new StringBuilder();
        boolean inHeader = false;

        for (final String line : text.split("\n", -1)) {
            final Matcher header = HEADER.matcher(line);
            if (header.matches()) {
                store(source, sections, key, permission, sameAs, returnCodes, body);
                key = header.group(1) + ' ' + header.group(2);
                permission = null;
                sameAs = null;
                returnCodes = new LinkedHashMap<>();
                body = new StringBuilder();
                inHeader = true;
            } else if (key != null) {
                // Lines before the first section are left out.
                final Matcher permissionLine = PERMISSION.matcher(line);
                final Matcher returnCodeLine = RETURN_CODE.matcher(line);
                final Matcher sameAsLine = SAME_AS.matcher(line);
                if (inHeader && permissionLine.matches()) {
                    permission = permissionLine.group(1);
                } else if (inHeader && sameAsLine.matches()) {
                    sameAs = sameAsLine.group(1);
                } else if (inHeader && returnCodeLine.matches()) {
                    returnCodes.put(returnCodeLine.group(1), returnCodeLine.group(2).strip());
                } else {
                    inHeader = false;
                    body.append(line).append('\n');
                }
            }
        }
        store(source, sections, key, permission, sameAs, returnCodes, body);
        return sections;
    }

    private static void store(final String source, final Map<String, Section> sections, final String key, final String permission,
                              final String sameAs, final Map<String, String> returnCodes, final StringBuilder body) {
        if (key != null) {
            if ((permission == null) && (sameAs == null)) {
                throw new IllegalStateException(source + ": the section '" + key + "' has no 'permission:' or 'same-as:' line.");
            }
            if (sections.put(key, new Section(permission, returnCodes, body.toString().strip(), sameAs)) != null) {
                throw new IllegalStateException(source + ": the section '" + key + "' is documented twice.");
            }
        }
    }

    private static String read(final String resource) {
        try (InputStream stream = OperationDocs.class.getResourceAsStream(resource)) {
            return (stream == null) ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
