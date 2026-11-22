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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Parsing of the per-operation documentation files.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class OperationDocsTest {

    @Test
    void testParseSections() {
        final String text = """
                ## POST /a/first
                permission: PROCESS_MEMBER
                - IDENTIFICATION_WARNING: Not found.
                - ILLEGAL_ACTION: Not allowed.

                Body of the first.
                - a list item in the body

                ## POST /a/second
                permission: NONE
                Body of the second.
                """;

        final Map<String, OperationDocs.Section> sections = OperationDocs.parse("test.md", text);

        assertEquals(List.of("POST /a/first", "POST /a/second"), List.copyOf(sections.keySet()));
        final OperationDocs.Section first = sections.get("POST /a/first");
        assertEquals("PROCESS_MEMBER", first.permission());
        assertEquals(Map.of("IDENTIFICATION_WARNING", "Not found.", "ILLEGAL_ACTION", "Not allowed."), first.returnCodes());
        assertTrue(first.body().startsWith("Body of the first."));
        assertTrue(first.body().contains("- a list item in the body"));
        assertEquals("NONE", sections.get("POST /a/second").permission());
        assertEquals("Body of the second.", sections.get("POST /a/second").body());
    }

    @Test
    void testMissingPermissionFails() {
        assertThrows(IllegalStateException.class, () -> OperationDocs.parse("test.md", "## POST /a\nBody\n"));
    }

    @Test
    void testDuplicateSectionFails() {
        final String text = "## POST /a\npermission: NONE\n## POST /a\npermission: NONE\n";
        assertThrows(IllegalStateException.class, () -> OperationDocs.parse("test.md", text));
    }

    @Test
    void testFileName() {
        assertEquals("data-types.md", OperationDocs.fileName("Data Types"));
        assertEquals("members.md", OperationDocs.fileName("Members"));
    }
}
