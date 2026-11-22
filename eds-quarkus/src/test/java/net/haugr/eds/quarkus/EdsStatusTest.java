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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The status of the stone (STATUS.md) in the running stone: the
 * gathering is driven by the scheduler, the requests are counted by the
 * filter, a status key reads the status and nothing else, and a page from
 * an allowed origin may call the stone.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsStatusTest {

    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String MEMBER_CREDENTIAL = encode("status member pjvgr.yxupm-37i23.8kq4h_9s");

    @BeforeEach
    void apiRoot() {
        // The REST API is below Constants.REST_API; Quarkus only knows the root
        // of the application.
        RestAssured.basePath = Constants.REST_API;
    }

    @Test
    void testStatusWithAKey() throws Exception {
        final String admin = login("admin", ADMIN_CREDENTIAL);
        final String memberId = as(admin).body("""
                        {"newAccountName": "status-member", "newCredential": "%s"}
                        """.formatted(MEMBER_CREDENTIAL)).when().post("/members")
                .then().statusCode(200).extract().path("memberId");
        final String member = login("status-member", MEMBER_CREDENTIAL);

        // The stone gets a name, and gathers its load every second.
        settings("""
                {"eds.stone.name": "north", "eds.status.load.interval.seconds": "1"}
                """);
        final String key = statusKey(admin, member);

        // The requests below must fall into samples of the new interval: a
        // sample of the old one is dropped when the interval changes.
        until(() -> as(key).when().get("/status").then().statusCode(200).extract().jsonPath(),
                ticking -> (ticking.getInt("load.interval") == 1) && !ticking.getList("load.samples").isEmpty());

        // Some requests to count, by an account which is then active.
        for (int i = 0; i < 5; i++) {
            as(member).when().get("/circles").then().statusCode(200);
        }
        as(member).when().get("/circles/{circleId}", "4c5bc2c6-0fa5-4c3a-9d1e-1a2b3c4d5e6f").then().statusCode(404);

        // The key reads the status. The scheduler gathers a sample every
        // second; a few are waited for. The new interval counts from the next
        // tick on: a sample of before, with the requests in it, isn't enough.
        // The other requests of the test may make six before the sample with
        // the refused one is in, so that is waited for too.
        final JsonPath status = until(() -> as(key).queryParam("history", true).when().get("/status").then().statusCode(200).extract().jsonPath(),
                read -> (read.getInt("load.interval") == 1) && (Load.of(read).requests() >= 6) && (Load.of(read).failed() >= 1)
                        && !read.getList("inventory.samples").isEmpty());
        assertNotNull(status);
        assertStatus(status);

        // Only what is new, for a client which asks often.
        final int last = status.getInt("load.samples[-1].sequence");
        as(key).queryParam("since", last + 1000).when().get("/status").then().statusCode(200)
                .body("load.samples.size()", equalTo(0)).body("day", nullValue());

        whoReadsTheStatus(admin, member, key);

        settings("""
                {"eds.stone.name": "", "eds.status.load.interval.seconds": "5"}
                """);
        as(admin).when().get("/status").then().statusCode(200).body("name", nullValue());
        as(admin).when().delete("/members/{memberId}", memberId).then().statusCode(200);
    }

    /** Only the System Administrator has status keys; the key of the test. */
    private static String statusKey(final String admin, final String member) {
        as(member).body("""
                        {"label": "not mine"}
                        """).when().post("/status/keys")
                .then().statusCode(ReturnCode.AUTHORIZATION_WARNING.getHttpCode());
        final String key = as(admin).body("""
                        {"label": "overview"}
                        """).when().post("/status/keys")
                .then().statusCode(200).body("keys", hasItem("overview")).extract().path("key");
        assertNotNull(key);
        as(admin).when().get("/status/keys").then().statusCode(200).body("keys", hasItem("overview")).body("key", nullValue());
        return key;
    }

    /** What the status read with the key tells of the stone, its load and its inventory. */
    private static void assertStatus(final JsonPath status) {
        final Load load = Load.of(status);
        assertEquals("north", status.getString("name"));
        assertEquals(Constants.EDS_VERSION, status.getString("version"));
        assertTrue(status.getBoolean("awake"));
        assertEquals("H2", status.getString("database"));
        assertFalse(status.getBoolean("persistent"));
        assertEquals(1, status.getInt("load.interval"));
        assertTrue(load.requests() >= 6, "requests counted: " + load.requests());
        assertTrue(load.failed() >= 1, "failed requests counted: " + load.failed());
        assertTrue(load.accounts() >= 1, "active accounts: " + load.accounts());
        assertFalse(status.getList("day.samples").isEmpty());
        final Map<String, Number> inventory = status.getMap("inventory.samples[0].values");
        // The inventory was gathered when the stone started, and is again in
        // an hour: it needn't know the accounts of this test.
        assertTrue(inventory.get("accounts").doubleValue() >= 0);
        assertTrue(inventory.containsKey("circles"));
        assertTrue(inventory.get("storageCeiling").doubleValue() > 0);
        assertTrue(inventory.containsKey("sessions"));
        final Map<String, Number> latest = status.getMap("load.samples[-1].values");
        assertTrue(latest.get("memoryUsed").doubleValue() > 0);
        assertTrue(latest.containsKey("connections"));
        assertTrue(latest.containsKey("threads"));
    }

    /** Who may read the status, and that the key reads nothing else; withdrawn, it is refused at once. */
    private static void whoReadsTheStatus(final String admin, final String member, final String key) {
        // The System Administrator reads it with the Session; a Member doesn't,
        // and without anything nobody does.
        as(admin).when().get("/status").then().statusCode(200).body("name", equalTo("north"));
        as(member).when().get("/status").then().statusCode(ReturnCode.AUTHORIZATION_WARNING.getHttpCode());
        given().when().get("/status").then().statusCode(ReturnCode.AUTHENTICATION_WARNING.getHttpCode());

        // The key reads nothing else, and the stone doesn't tell its name to
        // everyone who asks.
        as(key).when().get("/circles").then().statusCode(ReturnCode.AUTHENTICATION_WARNING.getHttpCode());
        as(key).when().get("/status/keys").then().statusCode(ReturnCode.AUTHENTICATION_WARNING.getHttpCode());
        given().contentType(ContentType.JSON).body("{}").when().post("/version")
                .then().statusCode(200).body("name", nullValue()).body("version", notNullValue());

        // Withdrawn, the key is refused at once.
        as(admin).when().delete("/status/keys/{label}", "overview").then().statusCode(200).body("keys", not(hasItem("overview")));
        as(key).when().get("/status").then().statusCode(ReturnCode.AUTHENTICATION_WARNING.getHttpCode());
    }

    /** The load of all samples of a status: the requests and failures summed, the most active accounts. */
    private record Load(double requests, double failed, double accounts) {

        private static Load of(final JsonPath status) {
            double requests = 0;
            double failed = 0;
            double accounts = 0;
            final List<Map<String, Object>> samples = status.getList("load.samples");
            for (final Map<String, Object> sample : samples) {
                @SuppressWarnings("unchecked")
                final Map<String, Number> values = (Map<String, Number>) sample.get("values");
                requests += values.get("requests").doubleValue();
                failed += values.get("failed").doubleValue();
                accounts = Math.max(accounts, values.get("accounts").doubleValue());
            }
            return new Load(requests, failed, accounts);
        }
    }

    /**
     * Reads until the condition holds, at most 80 times, a quarter of a
     * second apart; the last read.
     */
    // The stone's scheduler gathers by the clock, so there is nothing to wait on but the time: the
    // reads are a quarter of a second apart, and end as soon as the condition holds (no Awaitility).
    @SuppressWarnings("java:S2925")
    private static JsonPath until(final Supplier<JsonPath> read, final Predicate<JsonPath> done) throws InterruptedException {
        JsonPath last = null;
        for (int i = 0; (i < 80) && ((last == null) || !done.test(last)); i++) {
            Thread.sleep(250);
            last = read.get();
        }
        return last;
    }

    @Test
    void testAPageFromAnAllowedOriginMayCall() {
        // By default only a page from http://localhost is allowed.
        given().header("Origin", "http://localhost").contentType(ContentType.JSON).body("{}").when().post("/version")
                .then().statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost")
                .header("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization"))
                .header("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("DELETE"))
                .header("Vary", org.hamcrest.Matchers.containsString("Origin"));
        given().header("Origin", "https://elsewhere.example").contentType(ContentType.JSON).body("{}").when().post("/version")
                .then().statusCode(200).header("Access-Control-Allow-Origin", nullValue());
        given().contentType(ContentType.JSON).body("{}").when().post("/version")
                .then().statusCode(200).header("Access-Control-Allow-Origin", nullValue());

        // The question a browser asks before a request with an Authorization header.
        given().header("Origin", "http://localhost").header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .when().options("/status")
                .then().statusCode(204)
                .header("Access-Control-Allow-Origin", "http://localhost")
                .header("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Authorization"));
        given().header("Origin", "https://elsewhere.example").header("Access-Control-Request-Method", "GET")
                .when().options("/status")
                .then().header("Access-Control-Allow-Origin", nullValue());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void settings(final String values) {
        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "settings": %s}
                        """.formatted(ADMIN_CREDENTIAL, values))
                .when().post("/settings")
                .then().statusCode(200).body("returnCode", equalTo(200));
    }

    private static RequestSpecification as(final String token) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON);
    }

    private static String login(final String account, final String credential) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "%s", "credential": "%s"}
                        """.formatted(account, credential))
                .when().post("/members/login")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("session");
    }

    private static String encode(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The readiness check of EDS is served on the management port, and is up
     * once the database of EDS is ready.
     */
    @Test
    void testReadinessReportsEds() throws Exception {
        final int port = ConfigProvider.getConfig().getOptionalValue("quarkus.management.test-port", Integer.class).orElse(9001);
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/q/health/ready")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), response.body());
        final JsonPath health = JsonPath.from(response.body());
        assertEquals("UP", health.getString("checks.find { it.name == 'EDS' }.status"), response.body());
        assertEquals("EDS is ready", health.getString("checks.find { it.name == 'EDS' }.data.status"), response.body());
    }
}
