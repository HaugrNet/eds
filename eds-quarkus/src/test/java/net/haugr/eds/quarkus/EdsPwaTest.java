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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import net.haugr.eds.api.common.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * <p>The PWA (RUNE.md) is served by Quinoa at the root (/), with its
 * security headers, while the REST paths (below /api/v1) don't get them.
 * Not for the stone alone (-Dstone), which has no PWA; see EdsStoneTest.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
@DisabledIfSystemProperty(named = "eds.stone", matches = "true")
final class EdsPwaTest {

    private static final String CSP = "Content-Security-Policy";

    @Test
    void testAppIsServedWithItsHeaders() {
        given().when().get("/")
                .then().statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("<eds-root>"))
                // No inline scripts or styles: the CSP allows only 'self'.
                .body(not(containsString("<script>")))
                .header(CSP, containsString("script-src 'self'"))
                .header(CSP, containsString("frame-ancestors 'none'"))
                .header(CSP, not(containsString("unsafe-inline")))
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Frame-Options", "DENY");
        given().when().get("/manifest.webmanifest")
                .then().statusCode(200).body(containsString("\"start_url\""))
                .header(CSP, containsString("default-src 'self'"));
        given().when().get("/sw.js")
                .then().statusCode(200).body(containsString("rune-shell"));
    }

    /**
     * The page, the service worker and the manifest must not be kept by the
     * browser, or a new build isn't seen; the files in assets/ have a hash
     * in their name and may be kept.
     */
    @Test
    void testOnlyHashedAssetsAreCached() {
        final String page = given().when().get("/")
                .then().statusCode(200).header("Cache-Control", "no-cache")
                .extract().asString();
        given().when().get("/sw.js").then().statusCode(200).header("Cache-Control", "no-cache");
        given().when().get("/manifest.webmanifest").then().statusCode(200).header("Cache-Control", "no-cache");

        final java.util.regex.Matcher script = java.util.regex.Pattern.compile("assets/[^\"]+\\.js").matcher(page);
        org.junit.jupiter.api.Assertions.assertTrue(script.find(), "The page names its script.");
        given().when().get("/" + script.group())
                .then().statusCode(200).header("Cache-Control", "public, immutable, max-age=31536000");
    }

    @Test
    void testRestPathsAreNotTheApp() {
        // The REST API is below /api/v1 and doesn't get the app's headers.
        given().when().get(Constants.REST_API + "/openapi.json")
                .then().statusCode(200)
                .contentType(containsString("json"))
                .header(CSP, org.hamcrest.Matchers.nullValue());
        given().when().get("/unknown-path").then().statusCode(404);
    }
}
