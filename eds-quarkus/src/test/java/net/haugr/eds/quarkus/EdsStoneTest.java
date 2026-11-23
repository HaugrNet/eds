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
package net.haugr.eds.quarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import net.haugr.eds.api.common.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * <p>The stone alone (built with -Dstone) is the REST API and the chat,
 * without a web app: nothing of the PWA (RUNE.md) is served, and neither its
 * files nor Quinoa, which serves them, are part of the build. What is not
 * there can't be switched on in a running stone.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "eds.stone", matches = "true")
final class EdsStoneTest {

    @Test
    void testNoAppIsServed() {
        for (final String path : new String[] { "/", "/index.html", "/sw.js", "/manifest.webmanifest", "/assets/" }) {
            given().when().get(path).then().statusCode(404);
        }
    }

    @Test
    void testNeitherTheAppNorQuinoaIsPresent() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.quarkiverse.quinoa.QuinoaRecorder", false, loader));
        assertNull(loader.getResource("io/quarkiverse/quinoa"));
        assertNull(loader.getResource("META-INF/resources/index.html"));
        assertNull(loader.getResource("META-INF/resources/sw.js"));
    }

    @Test
    void testTheApiIsServed() {
        given().contentType("application/json").body("{}")
                .when().post(Constants.REST_API + "/version")
                .then().statusCode(200).body("version", notNullValue());
    }
}
