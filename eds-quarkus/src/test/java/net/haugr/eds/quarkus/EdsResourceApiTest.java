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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import net.haugr.eds.api.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import net.haugr.eds.api.common.ReturnCode;
import org.junit.jupiter.api.Test;

/**
 * <p>The ID-based resource API over HTTP (PLAN.md, L1): session
 * authentication with the Authorization header, IDs in the path, and the
 * HTTP status of the Return Code.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsResourceApiTest {

    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String MEMBER_CREDENTIAL = encode("resource member pjvgr.yxupm-37i23.8kq4h_9s");
    private static final String UNKNOWN_ID = "4c5bc2c6-0fa5-4c3a-9d1e-1a2b3c4d5e6f";

    @BeforeEach
    void apiRoot() {
        // The REST API is below Constants.REST_API; Quarkus only knows the root
        // of the application.
        RestAssured.basePath = Constants.REST_API;
    }

    @Test
    void testResources() {
        final String admin = login("admin", ADMIN_CREDENTIAL);

        // Members: create (POST), read (GET), role (PUT).
        final String ownerId = as(admin).body("""
                        {"newAccountName": "resource-owner", "newCredential": "%s"}
                        """.formatted(MEMBER_CREDENTIAL))
                .when().post("/members")
                .then().statusCode(200).body("returnCode", equalTo(200))
                .extract().path("memberId");
        final String otherId = as(admin).body("""
                        {"newAccountName": "resource-other", "newCredential": "%s"}
                        """.formatted(MEMBER_CREDENTIAL))
                .when().post("/members")
                .then().statusCode(200).extract().path("memberId");
        membersReadAndRole(admin, ownerId, otherId);

        // Circles: create, read, update; an unknown Circle is 404.
        final String owner = login("resource-owner", MEMBER_CREDENTIAL);
        final String circleId = as(owner).body("""
                        {"circleName": "Resource Circle"}
                        """).when().post("/circles")
                .then().statusCode(200).extract().path("circleId");
        final String targetId = as(owner).body("""
                        {"circleName": "Resource Target"}
                        """).when().post("/circles")
                .then().statusCode(200).extract().path("circleId");
        circles(owner, circleId);
        trustees(owner, circleId, otherId);
        data(owner, circleId, targetId);
        dataTypes(owner);
        refused(owner);

        // Clean up: the Circles and the Members.
        as(owner).when().delete("/circles/{circleId}", circleId).then().statusCode(200);
        as(owner).when().delete("/circles/{circleId}", targetId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", otherId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", ownerId).then().statusCode(200);
    }

    /** Members: read (GET), and the role (PUT). */
    private static void membersReadAndRole(final String admin, final String ownerId, final String otherId) {
        as(admin).when().get("/members/{memberId}", ownerId)
                .then().statusCode(200).body("members[0].accountName", equalTo("resource-owner"));
        as(admin).when().get("/members").then().statusCode(200).body("members.memberId", hasItem(otherId));
        as(admin).body("""
                        {"memberRole": "ADMIN"}
                        """).when().put("/members/{memberId}", otherId)
                .then().statusCode(200);
    }

    /** Circles: read and update; an unknown Circle is 404. */
    private static void circles(final String owner, final String circleId) {
        as(owner).when().get("/circles").then().statusCode(200).body("circles.circleId", hasItem(circleId));
        as(owner).body("""
                        {"circleName": "Renamed Circle", "circleId": "%s"}
                        """.formatted(UNKNOWN_ID)).when().put("/circles/{circleId}", circleId)
                .then().statusCode(200);
        as(owner).when().get("/circles/{circleId}", circleId)
                .then().statusCode(200).body("circles[0].circleName", equalTo("Renamed Circle"));
        as(owner).when().get("/circles/{circleId}", UNKNOWN_ID)
                .then().statusCode(404).body("returnCode", equalTo(404));
    }

    private static void trustees(final String owner, final String circleId, final String otherId) {
        // Trustees of the Circle.
        as(owner).body("""
                        {"memberId": "%s", "trustLevel": "READ"}
                        """.formatted(otherId)).when().post("/circles/{circleId}/trustees", circleId)
                .then().statusCode(200);
        as(owner).when().get("/circles/{circleId}/trustees", circleId)
                .then().statusCode(200).body("trustees.memberId", hasItem(otherId));
        as(owner).body("""
                        {"trustLevel": "WRITE"}
                        """).when().put("/circles/{circleId}/trustees/{memberId}", circleId, otherId)
                .then().statusCode(200);
        as(owner).when().delete("/circles/{circleId}/trustees/{memberId}", circleId, otherId)
                .then().statusCode(200);
        // Secrets need either secrets or the Circle Key.
        as(owner).body("""
                        {"secrets": []}
                        """).when().put("/circles/{circleId}/secrets", circleId)
                .then().statusCode(ReturnCode.VERIFICATION_WARNING.getHttpCode());
    }

    private static void data(final String owner, final String circleId, final String targetId) {
        // Data: add, list with size, metadata without content, update, copy,
        // move and delete.
        final String dataId = as(owner).body("""
                        {"circleId": "%s", "dataName": "note", "data": "%s"}
                        """.formatted(circleId, encode("twelve bytes"))).when().post("/data")
                .then().statusCode(200).extract().path("dataId");
        as(owner).queryParam("circleId", circleId).when().get("/data")
                .then().statusCode(200).body("metadata.find { it.dataId == '%s' }.size".formatted(dataId), equalTo(12));
        // A page of the listing, and of a single Data Object.
        as(owner).queryParam("circleId", circleId).queryParam("pageNumber", 1).queryParam("pageSize", 1).when().get("/data")
                .then().statusCode(200).body("metadata.size()", equalTo(1));
        as(owner).queryParam("pageNumber", 1).queryParam("pageSize", 10).when().get("/data/{dataId}", dataId)
                .then().statusCode(200).body("metadata[0].dataName", equalTo("note"));
        as(owner).when().get("/data/{dataId}", dataId)
                .then().statusCode(200)
                .body("metadata[0].dataName", equalTo("note"))
                .body("metadata[0].size", equalTo(12))
                .body("data", nullValue());
        as(owner).body("""
                        {"dataName": "renamed note"}
                        """).when().put("/data/{dataId}", dataId)
                .then().statusCode(200);
        final String copyId = as(owner).body("""
                        {"targetCircleId": "%s"}
                        """.formatted(targetId)).when().post("/data/{dataId}/copy", dataId)
                .then().statusCode(200).extract().path("dataId");
        as(owner).when().get("/data/{dataId}", copyId)
                .then().statusCode(200).body("metadata[0].circleId", equalTo(targetId));
        as(owner).body("""
                        {"targetCircleId": "%s", "dataName": "moved note"}
                        """.formatted(targetId)).when().post("/data/{dataId}/move", dataId)
                .then().statusCode(200);
        as(owner).when().get("/data/{dataId}", dataId).then().statusCode(404);
        as(owner).when().delete("/data/{dataId}", copyId).then().statusCode(200);
        as(owner).when().get("/data/{dataId}", copyId).then().statusCode(404);
    }

    private static void dataTypes(final String owner) {
        // Data Types.
        as(owner).body("""
                        {"type": "Resource test type"}
                        """).when().put("/dataTypes/{typeName}", "resource-type")
                .then().statusCode(200).body("dataType.typeName", equalTo("resource-type"));
        as(owner).when().get("/dataTypes").then().statusCode(200).body("dataTypes.typeName", hasItem("resource-type"));
        as(owner).when().delete("/dataTypes/{typeName}", "resource-type").then().statusCode(200);
    }

    private static void refused(final String owner) {
        // Invalid input, and missing or wrong authentication, with their
        // HTTP status; credentials in the body don't replace the session.
        as(owner).body("{}").when().post("/circles")
                .then().statusCode(406).body("returnCode", equalTo(406));
        given().when().get("/circles").then().statusCode(403).body("returnCode", equalTo(403));
        given().header("Authorization", "Bearer " + encode("no such session")).when().get("/circles")
                .then().statusCode(403);
        given().contentType(ContentType.JSON).body("""
                        {"accountName": "admin", "credential": "%s"}
                        """.formatted(ADMIN_CREDENTIAL)).when().get("/circles")
                .then().statusCode(403);
    }

    /**
     * The served specification is the checked-in contract, byte for byte
     * (PLAN.md, L2), without authentication.
     */
    @Test
    void testSpecificationIsTheContract() throws IOException {
        final byte[] served = given().when().get("/openapi.json")
                .then().statusCode(200).contentType(ContentType.JSON)
                .extract().asByteArray();
        assertArrayEquals(Files.readAllBytes(Path.of("..", "eds-api", "src", "main", "resources", "openapi.json")), served);
    }

    private static RequestSpecification as(final String session) {
        return given().header("Authorization", "Bearer " + session).contentType(ContentType.JSON);
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
}
