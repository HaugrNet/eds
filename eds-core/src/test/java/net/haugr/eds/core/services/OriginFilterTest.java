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
package net.haugr.eds.core.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * Which origins may call the stone from a browser (STATUS.md); the headers
 * themselves are tested in the running stone.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class OriginFilterTest extends DatabaseSetup {

    @Test
    void testTheDefaultAllowsOnlyLocalhost() {
        final OriginFilter filter = new OriginFilter(newSettings());

        assertTrue(filter.allowed("http://localhost"));
        assertFalse(filter.allowed("http://localhost:5173"));
        assertFalse(filter.allowed("https://rune.example.com"));
        assertFalse(filter.allowed(null));
        assertFalse(filter.allowed("  "));
    }

    @Test
    void testSeveralOrigins() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.CORS.getKey(), "https://rune.example.com, https://watch.example.org  http://localhost:5173");
        final OriginFilter filter = new OriginFilter(mySettings);

        assertTrue(filter.allowed("https://rune.example.com"));
        assertTrue(filter.allowed("https://WATCH.example.org"));
        assertTrue(filter.allowed("http://localhost:5173"));
        // An origin is taken whole: no part of it, and nothing beyond it.
        assertFalse(filter.allowed("https://rune.example.com.evil.example"));
        assertFalse(filter.allowed("https://example.com"));
        assertFalse(filter.allowed("http://rune.example.com"));
    }

    @Test
    void testEveryOrigin() {
        final Settings mySettings = newSettings();
        mySettings.set(StandardSetting.CORS.getKey(), "*");

        assertTrue(new OriginFilter(mySettings).allowed("https://anywhere.example"));
    }
}
