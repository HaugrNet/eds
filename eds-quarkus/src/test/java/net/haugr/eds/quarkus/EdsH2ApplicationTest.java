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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

/**
 * Starts the complete EDS application without a database URL, so the
 * in-memory H2 database is selected, and verifies that the selection, the
 * EntityManager producer and the schema bootstrap work together, by using
 * the REST API to write and read data.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsH2ApplicationTest {

    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String MEMBER_CREDENTIAL = encode("Member-Passphrase-For-Testing-Only");

    @Inject
    EntityManager entityManager;

    @Inject
    DatabaseStartup databaseStartup;

    @ConfigProperty(name = "quarkus.datasource.h2.active")
    boolean h2Active;

    @ConfigProperty(name = "quarkus.datasource.pg.active")
    boolean postgresqlActive;

    @Test
    void testH2IsSelected() {
        assertTrue(h2Active);
        assertTrue(!postgresqlActive);
        assertEquals(DatabaseSelection.Database.H2, DatabaseSelection.selected());
    }

    @Test
    void testStartedInOneAttempt() {
        // H2 is always reachable, so no retries are needed
        assertEquals(DatabaseStartup.State.STARTED, databaseStartup.getState());
        assertEquals(1, databaseStartup.getAttempts());
    }

    @Test
    void testEntityManagerUsesH2WithSchema() {
        // H2VERSION() only exists in H2, so this also proves that the default
        // EntityManager belongs to the H2 persistence unit.
        final Object version = entityManager.createNativeQuery("SELECT H2VERSION()").getSingleResult();
        assertTrue(String.valueOf(version).startsWith("2."));

        // The schema was created by the SchemaBootstrap, with the 2.0 version
        final Object schemaVersion = entityManager.createNativeQuery("SELECT schema_version FROM eds_versions").getSingleResult();
        assertEquals(4, ((Number) schemaVersion).intValue());
    }

    @Test
    void testVersionEndpoint() {
        given().contentType(ContentType.JSON).body("{}")
                .when().post("/version")
                .then().statusCode(200)
                .body("returnCode", equalTo(200))
                .body("version", notNullValue());
    }

    @Test
    void testCreateMemberAndCircle() {
        final String memberId = given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "newAccountName": "h2-member", "newCredential": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, MEMBER_CREDENTIAL))
                .when().post("/members/createMember")
                .then().statusCode(200)
                .body("returnCode", equalTo(200))
                .extract().path("memberId");

        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "circleName": "H2 Circle", "memberId": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, memberId))
                .when().post("/circles/createCircle")
                .then().statusCode(200)
                .body("returnCode", equalTo(200));

        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "h2-member", "credential": "%s"}
                        """.formatted(MEMBER_CREDENTIAL))
                .when().post("/circles/fetchCircles")
                .then().statusCode(200)
                .body("returnCode", equalTo(200))
                .body("circles.circleName", hasItem("H2 Circle"));
    }

    private static String encode(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
