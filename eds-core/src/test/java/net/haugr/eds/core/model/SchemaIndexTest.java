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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * <p>Every foreign key of the schema has an index which starts with its
 * columns. PostgreSQL doesn't create one for a foreign key, H2 does, so a
 * missing index only shows on PostgreSQL, and only when a table is large: a
 * delete which cascades reads the whole child table for each row it removes.
 * The load test of 2026-11-20 found it, when a Circle with 19,441 Objects
 * took 205 seconds to delete (2 seconds with the index).</p>
 *
 * <p>The test reads the script itself, so it needs no PostgreSQL.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SchemaIndexTest {

    private static final Pattern CREATE_TABLE = Pattern.compile("^CREATE TABLE (\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_INDEX = Pattern.compile("^CREATE (?:UNIQUE )?INDEX \\w+ ON (\\w+) \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_KEY = Pattern.compile("CONSTRAINT (\\w+)\\s+FOREIGN KEY \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INDEXED = Pattern.compile("(?:PRIMARY KEY|UNIQUE) \\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void testEveryForeignKeyHasAnIndex() throws IOException {
        final Map<String, List<List<String>>> indexes = new HashMap<>();
        final Map<String, List<String>> foreignKeys = new HashMap<>();
        final Map<String, String> foreignKeyTables = new HashMap<>();

        for (final String statement : SqlScript.split(readSchema())) {
            final String flat = statement.replaceAll("\\s+", " ").trim();
            final Matcher table = CREATE_TABLE.matcher(flat);
            final Matcher index = CREATE_INDEX.matcher(flat);
            if (table.find()) {
                final String name = table.group(1).toLowerCase(Locale.ROOT);
                final Matcher indexed = INDEXED.matcher(flat);
                while (indexed.find()) {
                    indexes.computeIfAbsent(name, k -> new ArrayList<>()).add(columns(indexed.group(1)));
                }
                final Matcher foreignKey = FOREIGN_KEY.matcher(flat);
                while (foreignKey.find()) {
                    foreignKeys.put(foreignKey.group(1), columns(foreignKey.group(2)));
                    foreignKeyTables.put(foreignKey.group(1), name);
                }
            } else if (index.find()) {
                indexes.computeIfAbsent(index.group(1).toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(columns(index.group(2)));
            }
        }

        assertTrue(foreignKeys.size() >= 16, "The script should have its foreign keys, found " + foreignKeys.size());
        final List<String> missing = new ArrayList<>();
        foreignKeys.forEach((constraint, columns) -> {
            final String table = foreignKeyTables.get(constraint);
            final boolean covered = indexes.getOrDefault(table, List.of()).stream()
                    .anyMatch(index -> index.size() >= columns.size() && index.subList(0, columns.size()).equals(columns));
            if (!covered) {
                missing.add(constraint + " on " + table + columns);
            }
        });
        assertEquals(List.of(), missing, "Foreign keys without an index which starts with their columns");
    }

    private static List<String> columns(final String list) {
        final List<String> columns = new ArrayList<>();
        for (final String column : list.split(",")) {
            columns.add(column.trim().toLowerCase(Locale.ROOT));
        }
        return columns;
    }

    private static String readSchema() throws IOException {
        try (InputStream stream = SchemaIndexTest.class.getClassLoader().getResourceAsStream("META-INF/eds-schema.sql")) {
            assertNotNull(stream, "The schema script must be on the class path");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
