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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import net.haugr.eds.api.common.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>The rotation of a Circle Secret over HTTP (PLAN.md, Crypto C6), with the
 * runner of the stone: a Trustee is removed, a Trustee's download hands
 * the secret over, and the rotation runs in the background until every
 * Data Object is on the new secret; then a Circle administrator asks for
 * another one.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsRotationTest {

    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String MEMBER_CREDENTIAL = encode("rotation member pjvgr.yxupm-37i23.8kq4h_9s");
    private static final String CONTENT = "/data/{dataId}/content";
    private static final long WAIT_MILLIS = 60_000;

    @BeforeEach
    void apiRoot() {
        RestAssured.basePath = Constants.REST_API;
    }

    @Test
    void testRotationAfterARemoval() throws InterruptedException {
        final String admin = login("admin", ADMIN_CREDENTIAL);
        final String ownerId = member(admin, "rotation-owner");
        final String otherId = member(admin, "rotation-other");
        final String owner = login("rotation-owner", MEMBER_CREDENTIAL);
        final String circleId = circle(owner, "Rotation Circle");
        as(owner).body("""
                        {"memberId": "%s", "trustLevel": "READ"}
                        """.formatted(otherId)).when().post("/circles/{circleId}/trustees", circleId)
                .then().statusCode(200);

        // Content of several chunks, and a small object.
        final byte[] large = bytes(300_000);
        final String largeId = object(owner, circleId, "large");
        given().header("Authorization", "Bearer " + owner).contentType("application/octet-stream").body(large)
                .when().put(CONTENT, largeId).then().statusCode(200);
        final String smallId = as(owner).body("""
                        {"circleId": "%s", "dataName": "small", "data": "%s"}
                        """.formatted(circleId, encode("twelve bytes"))).when().post("/data")
                .then().statusCode(200).extract().path("dataId");
        final String madeBefore = read(owner, circleId).getString("circles[0].secretRotated");

        // The other Trustee is removed; the rotation is due, and the owner's
        // next download hands the secret over.
        as(owner).when().delete("/circles/{circleId}/trustees/{memberId}", circleId, otherId).then().statusCode(200);
        assertArrayEquals(large, download(owner, largeId));
        waitForRotation(owner, circleId, madeBefore);
        assertArrayEquals(large, download(owner, largeId));
        assertArrayEquals("twelve bytes".getBytes(StandardCharsets.UTF_8), download(owner, smallId));

        as(owner).when().delete("/circles/{circleId}", circleId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", otherId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", ownerId).then().statusCode(200);
    }

    @Test
    void testRotationAskedFor() throws InterruptedException {
        final String admin = login("admin", ADMIN_CREDENTIAL);
        final String ownerId = member(admin, "asking-owner");
        final String strangerId = member(admin, "asking-stranger");
        final String owner = login("asking-owner", MEMBER_CREDENTIAL);
        final String circleId = circle(owner, "Asked Rotation Circle");
        final String dataId = as(owner).body("""
                        {"circleId": "%s", "dataName": "note", "data": "%s"}
                        """.formatted(circleId, encode("a note"))).when().post("/data")
                .then().statusCode(200).extract().path("dataId");
        final String madeBefore = read(owner, circleId).getString("circles[0].secretRotated");

        // A Circle administrator asks; the rotation starts with their secret.
        as(owner).when().put("/circles/{circleId}/rotation", circleId)
                .then().statusCode(200).body("returnCode", equalTo(200));
        waitForRotation(owner, circleId, madeBefore);
        assertArrayEquals("a note".getBytes(StandardCharsets.UTF_8), download(owner, dataId));

        // The JSON twin; a Member who isn't a Trustee doesn't see the Circle.
        final String stranger = login("asking-stranger", MEMBER_CREDENTIAL);
        given().contentType(ContentType.JSON).body("""
                        {"credentialType": "SESSION", "credential": "%s", "circleId": "%s"}
                        """.formatted(stranger, circleId)).when().post("/circles/rotateCircle")
                .then().statusCode(200).body("returnCode", equalTo(404));

        as(owner).when().delete("/circles/{circleId}", circleId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", strangerId).then().statusCode(200);
        as(admin).when().delete("/members/{memberId}", ownerId).then().statusCode(200);
    }

    /**
     * Waits until the Circle has a new secret and no rotation under way.
     */
    // The rotation runs in the background of the stone, so there is nothing to wait on but the time:
    // the Circle is read every tenth of a second until the rotation has ended.
    @SuppressWarnings("java:S2925")
    private static void waitForRotation(final String session, final String circleId, final String madeBefore) throws InterruptedException {
        final long end = System.currentTimeMillis() + WAIT_MILLIS;
        JsonPath circle = read(session, circleId);
        while (((circle.get("circles[0].rotation") != null) || madeBefore.equals(circle.getString("circles[0].secretRotated")))
                && (System.currentTimeMillis() < end)) {
            Thread.sleep(100);
            circle = read(session, circleId);
        }
        assertNull(circle.get("circles[0].rotation"), "The rotation of the Circle Secret didn't end in time.");
        assertNotEquals(madeBefore, circle.getString("circles[0].secretRotated"));
    }

    private static String circle(final String session, final String name) {
        return as(session).body("""
                        {"circleName": "%s"}
                        """.formatted(name)).when().post("/circles")
                .then().statusCode(200).extract().path("circleId");
    }

    private static JsonPath read(final String session, final String circleId) {
        return as(session).when().get("/circles/{circleId}", circleId)
                .then().statusCode(200).body("circles[0].rotationDue", notNullValue())
                .extract().jsonPath();
    }

    private static String member(final String admin, final String name) {
        return as(admin).body("""
                        {"newAccountName": "%s", "newCredential": "%s"}
                        """.formatted(name, MEMBER_CREDENTIAL))
                .when().post("/members")
                .then().statusCode(200).extract().path("memberId");
    }

    private static String object(final String session, final String circleId, final String name) {
        return as(session).body("""
                        {"circleId": "%s", "dataName": "%s"}
                        """.formatted(circleId, name)).when().post("/data")
                .then().statusCode(200).extract().path("dataId");
    }

    private static byte[] download(final String session, final String dataId) {
        return given().header("Authorization", "Bearer " + session).when().get(CONTENT, dataId)
                .then().statusCode(200).extract().asByteArray();
    }

    // Test content: a seeded generator gives the same bytes in every run.
    @SuppressWarnings("findsecbugs:PREDICTABLE_RANDOM")
    private static byte[] bytes(final int length) {
        final byte[] bytes = new byte[length];
        new Random(42).nextBytes(bytes);
        return bytes;
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
