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
package net.haugr.eds.core.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.stream.Stream;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.core.exceptions.VerificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The declaration of a feature (FEATURES.md): what the stone accepts, and what
 * it reads from it.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class FeatureDeclarationTest {

    private static final int MAX = 1_000_000;

    @Test
    void testAllRulesAreRead() {
        final FeatureDeclaration declaration = FeatureDeclaration.parse("""
                {"feature": "plans", "version": 3, "signals": 2000, "kinds": {
                  "plan": {"add": "WRITE", "change": ["adders"], "remove": ["author"], "close": ["author", "administrators"], "size": 4096},
                  "expense": {"add": "WRITE", "change": ["author"], "remove": ["author"], "about": "plan", "keep": "all", "size": 2048},
                  "vote": {"add": "READ", "about": "plan", "keep": "member-about", "size": 16},
                  "position": {"add": "READ", "change": ["author"], "keep": "member", "size": 256},
                  "track": {"add": "READ", "keep": "age:86400", "size": 256},
                  "log": {"add": "ADMIN", "keep": "count:100", "size": 0}
                }}""", MAX);

        assertEquals("plans", declaration.name());
        assertEquals(3, declaration.version());
        assertTrue(declaration.hasSignals());
        assertEquals(2000, declaration.signals());
        assertEquals(6, declaration.kinds().size());

        final FeatureDeclaration.Kind plan = declaration.kind("plan");
        assertEquals(TrustLevel.WRITE, plan.add());
        assertEquals(EnumSet.of(FeatureDeclaration.Who.ADDERS), plan.change());
        assertEquals(EnumSet.of(FeatureDeclaration.Who.AUTHOR), plan.remove());
        assertEquals(EnumSet.of(FeatureDeclaration.Who.AUTHOR, FeatureDeclaration.Who.ADMINISTRATORS), plan.close());
        assertEquals(FeatureDeclaration.Keep.ALL, plan.keep());
        assertNull(plan.about());
        assertEquals(4096, plan.size());

        assertEquals("plan", declaration.kind("expense").about());
        assertTrue(declaration.kind("expense").close().isEmpty());
        assertEquals(FeatureDeclaration.Keep.MEMBER_ABOUT, declaration.kind("vote").keep());
        assertTrue(declaration.kind("vote").change().isEmpty());
        assertEquals(FeatureDeclaration.Keep.MEMBER, declaration.kind("position").keep());
        assertEquals(FeatureDeclaration.Keep.AGE, declaration.kind("track").keep());
        assertEquals(86400, declaration.kind("track").keepValue());
        assertEquals(FeatureDeclaration.Keep.COUNT, declaration.kind("log").keep());
        assertEquals(100, declaration.kind("log").keepValue());
        assertEquals(TrustLevel.ADMIN, declaration.kind("log").add());
        assertNull(declaration.kind("unknown"));
        assertNull(declaration.kind(null));
    }

    @Test
    void testWithoutSignals() {
        final FeatureDeclaration declaration = FeatureDeclaration.parse(minimal("calendar"), MAX);

        assertFalse(declaration.hasSignals());
        assertEquals(-1, declaration.signals());
        assertEquals(".calendar", FeatureDeclaration.folder(declaration.name()));
    }

    @Test
    void testANullFieldIsNotThere() {
        final FeatureDeclaration declaration = FeatureDeclaration.parse(
                "{\"feature\":\"a\",\"version\":1,\"signals\":null,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":null,\"about\":null}}}", MAX);

        assertFalse(declaration.hasSignals());
        assertEquals(FeatureDeclaration.Keep.ALL, declaration.kind("a").keep());
        assertNull(declaration.kind("a").about());
    }

    @Test
    void testNames() {
        assertTrue(FeatureDeclaration.isName("chat"));
        assertTrue(FeatureDeclaration.isName("to-do2"));
        assertTrue(FeatureDeclaration.isName("a".repeat(32)));
        assertFalse(FeatureDeclaration.isName("a".repeat(33)));
        assertFalse(FeatureDeclaration.isName("Chat"));
        assertFalse(FeatureDeclaration.isName("2do"));
        assertFalse(FeatureDeclaration.isName(".chat"));
        assertFalse(FeatureDeclaration.isName("a/b"));
        assertFalse(FeatureDeclaration.isName(""));
        assertFalse(FeatureDeclaration.isName(null));
    }

    /** Declarations which are refused as a whole, with what the refusal names. */
    static Stream<Arguments> refusedDeclarations() {
        return Stream.of(
                Arguments.of(null, "missing"),
                Arguments.of("  ", "missing"),
                Arguments.of("[]", "JSON object"),
                Arguments.of("{not json", "JSON object"),
                Arguments.of("{\"feature\":\"Bad Name\",\"version\":1,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "name of a feature"),
                Arguments.of("{\"feature\":\"a\",\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'version'"),
                Arguments.of("{\"feature\":\"a\",\"version\":0,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'version'"),
                Arguments.of("{\"feature\":\"a\",\"version\":1.5,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'version'"),
                Arguments.of("{\"feature\":\"a\",\"version\":1}", "at least one kind"),
                Arguments.of("{\"feature\":\"a\",\"version\":1,\"kinds\":{}}", "at least one kind"),
                Arguments.of("{\"feature\":\"a\",\"version\":1,\"kinds\":[]}", "at least one kind"),
                Arguments.of("{\"feature\":5,\"version\":1,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'feature' as text"),
                Arguments.of("{\"feature\":\"a\",\"version\":\"1\",\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'version' as a whole number"),
                Arguments.of("{\"feature\":\"a\",\"version\":123456789012345678901234567890,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'version' from 1 to"),
                Arguments.of("{\"feature\":\"a\",\"version\":1,\"extra\":true,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "unknown field 'extra'"),
                Arguments.of("{\"feature\":\"a\",\"version\":1,\"signals\":-1,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}", "'signals'"));
    }

    @ParameterizedTest
    @MethodSource("refusedDeclarations")
    void testWhatIsRefused(final String declaration, final String because) {
        refused(declaration, because);
    }

    /** Kinds which are refused, with what the refusal names. */
    static Stream<Arguments> refusedKinds() {
        return Stream.of(
                Arguments.of("\"Bad\":{\"add\":\"READ\",\"size\":1}", "no valid name"),
                Arguments.of("\"a\":\"text\"", "must be a JSON object"),
                Arguments.of("\"a\":{\"size\":1}", "'add'"),
                Arguments.of("\"a\":{\"add\":\"SYSOP\",\"size\":1}", "READ, WRITE or ADMIN"),
                Arguments.of("\"a\":{\"add\":\"read\",\"size\":1}", "READ, WRITE or ADMIN"),
                Arguments.of("\"a\":{\"add\":\"READ\"}", "'size'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":" + (MAX + 1) + "}", "'size'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"owner\":true}", "unknown field 'owner'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"change\":\"author\"}", "'change'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"remove\":[\"everyone\"]}", "'remove'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"close\":[1]}", "'close'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":\"some\"}", "'keep'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":\"age\"}", "'keep'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":\"age:0\"}", "number from 1"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":\"count:many\"}", "number from 1"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":\"member:3\"}", "'keep'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"about\":\"b\"}", "doesn't declare"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"about\":\"B\"}", "names no valid kind in 'about'"),
                Arguments.of("\"a\":{\"add\":\"READ\",\"size\":1,\"keep\":\"member-about\"}", "what it is about"));
    }

    @ParameterizedTest
    @MethodSource("refusedKinds")
    void testWhatKindIsRefused(final String kinds, final String because) {
        refused(kind(kinds), because);
    }

    @Test
    void testTooLongAndTooManyKinds() {
        refused("{\"feature\":\"a\",\"version\":1,\"kinds\":{\"a\":{\"add\":\"READ\",\"size\":1}}}" + " ".repeat(20_000), "at most");

        final StringBuilder kinds = new StringBuilder();
        for (int i = 0; i <= FeatureDeclaration.MAX_KINDS; i++) {
            kinds.append((i > 0) ? "," : "").append("\"k").append(i).append("\":{\"add\":\"READ\",\"size\":1}");
        }
        refused(kind(kinds.toString()), "at most " + FeatureDeclaration.MAX_KINDS + " kinds");
    }

    private static String minimal(final String feature) {
        return "{\"feature\":\"" + feature + "\",\"version\":1,\"kinds\":{\"event\":{\"add\":\"WRITE\",\"size\":1024}}}";
    }

    private static String kind(final String kinds) {
        return "{\"feature\":\"a\",\"version\":1,\"kinds\":{" + kinds + "}}";
    }

    private static void refused(final String declaration, final String because) {
        final VerificationException e = assertThrows(VerificationException.class, () -> FeatureDeclaration.parse(declaration, MAX), declaration);
        assertTrue(e.getMessage().contains(because), e.getMessage());
    }
}
