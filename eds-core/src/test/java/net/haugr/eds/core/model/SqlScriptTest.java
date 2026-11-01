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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SqlScript} statement splitter.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SqlScriptTest {

    @Test
    void testEmptyScript() {
        assertTrue(SqlScript.split("").isEmpty());
        assertTrue(SqlScript.split("  \n\t ; ;  ").isEmpty());
    }

    @Test
    void testSplitsOnSemicolons() {
        final List<String> statements = SqlScript.split("CREATE TABLE a (id INTEGER);\nINSERT INTO a VALUES (1);");

        assertEquals(List.of("CREATE TABLE a (id INTEGER)", "INSERT INTO a VALUES (1)"), statements);
    }

    @Test
    void testLastStatementWithoutSemicolon() {
        assertEquals(List.of("SELECT 1", "SELECT 2"), SqlScript.split("SELECT 1; SELECT 2"));
    }

    @Test
    void testLineCommentsAreRemoved() {
        final String script = """
                -- A comment; with a semicolon
                SELECT 1; -- Trailing comment; also with one
                -- Only a comment
                SELECT 2;
                """;

        assertEquals(List.of("SELECT 1", "SELECT 2"), SqlScript.split(script));
    }

    @Test
    void testBlockCommentsAreRemoved() {
        final String script = """
                /*
                 * Header; with a semicolon
                 */
                CREATE TABLE a (
                  id INTEGER, /* inline; comment */
                  name VARCHAR(10)
                );
                """;
        final List<String> statements = SqlScript.split(script);

        assertEquals(1, statements.size());
        assertFalse(statements.getFirst().contains("comment"));
        assertTrue(statements.getFirst().startsWith("CREATE TABLE a ("));
    }

    @Test
    void testSemicolonsInLiteralsAreKept() {
        final List<String> statements = SqlScript.split("INSERT INTO a VALUES ('x;y', 'it''s; fine'); SELECT \"odd;name\" FROM a;");

        assertEquals(List.of("INSERT INTO a VALUES ('x;y', 'it''s; fine')", "SELECT \"odd;name\" FROM a"), statements);
    }

    @Test
    void testCommentMarkersInLiteralsAreKept() {
        final List<String> statements = SqlScript.split("INSERT INTO a VALUES ('--not a comment', '/* nor this */');");

        assertEquals(List.of("INSERT INTO a VALUES ('--not a comment', '/* nor this */')"), statements);
    }

    @Test
    void testUnterminatedConstructsDoNotFail() {
        assertEquals(List.of("SELECT 1"), SqlScript.split("SELECT 1; /* never closed"));
        assertEquals(List.of("SELECT 'never closed"), SqlScript.split("SELECT 'never closed"));
    }

    @Test
    void testEdsSchemaScript() throws IOException {
        final List<String> statements = SqlScript.split(readResource("META-INF/eds-schema.sql"));

        // 10 tables, the version row and the default data types
        assertEquals(12, statements.size());
        assertEquals(10, statements.stream().filter(s -> s.startsWith("CREATE TABLE ")).count());
        assertEquals(2, statements.stream().filter(s -> s.startsWith("INSERT INTO ")).count());
        assertTrue(statements.stream().noneMatch(s -> s.contains("--") || s.contains("/*")));
    }

    private static String readResource(final String name) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            assertNotNull(stream, "Missing resource " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
