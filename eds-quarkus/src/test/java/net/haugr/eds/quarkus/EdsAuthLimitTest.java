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
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <p>End-to-end test of the authentication attempt limits (#68), over the real
 * REST API and with the real (JTA) transactions. This is the test that proves
 * the count survives the rollback of a failed request: a failed passphrase
 * rolls its request back, yet the three failures still add up and lock the
 * account, because {@code AuthLimitBean} commits the count in its own
 * transaction. A unit test cannot show this, as it has no JTA container.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@QuarkusTest
final class EdsAuthLimitTest {

    private static final int AUTH_WARNING = ReturnCode.AUTHENTICATION_WARNING.getCode();
    private static final String ADMIN_CREDENTIAL = encode("Admin-Passphrase-For-Testing-Only");
    private static final String VICTIM = "auth-limit-victim";
    private static final String VICTIM_CREDENTIAL = encode("Auth-Limit-Victim-Passphrase-For-Testing-Only");
    private static final String WRONG_CREDENTIAL = encode("a-wrong-passphrase");
    private static final String CONCURRENT_VICTIM = "auth-limit-concurrent";
    private static final int CONCURRENT_ATTEMPTS = 20;
    private static final String SAVING_VICTIM = "auth-limit-saving";
    private static final String DATA_VICTIM = "auth-limit-data";
    // A locked account gets the same answer as a wrong passphrase.
    private static final String REFUSED_MESSAGE = "Cannot authenticate the Account from the given Credentials.";

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void apiRoot() {
        RestAssured.basePath = Constants.REST_API;
    }

    private void setMaxFailedAttempts(final String value) {
        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "settings": {"eds.auth.max.failed.attempts": "%s", "eds.auth.failed.window.minutes": "15", "eds.auth.lockout.minutes": "15"}}
                        """.formatted(ADMIN_CREDENTIAL, value))
                .when().post("/settings")
                .then().statusCode(200)
                .body("returnCode", equalTo(ReturnCode.SUCCESS.getCode()));
    }

    @AfterEach
    void disableFeature() {
        // Restore the default, so the shared application's global settings do
        // not affect any other test.
        setMaxFailedAttempts("0");
    }

    private void createVictim(final String name) {
        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "admin", "credential": "%s", "newAccountName": "%s", "newCredential": "%s"}
                        """.formatted(ADMIN_CREDENTIAL, name, VICTIM_CREDENTIAL))
                .when().post("/members/createMember")
                .then().statusCode(200)
                .body("returnCode", equalTo(ReturnCode.SUCCESS.getCode()));
    }

    private int authenticate(final String credential) {
        return authenticate(VICTIM, credential);
    }

    private static int authenticate(final String account, final String credential) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "%s", "credential": "%s"}
                        """.formatted(account, credential))
                .when().post("/authenticated")
                .then().statusCode(200)
                .extract().path("returnCode");
    }

    @Test
    void testAccountLocksAndCountSurvivesRollback() {
        // The first authenticated call claims the Administrator; then a victim
        // account is created, whose passphrase we will get wrong on purpose.
        createVictim(VICTIM);

        setMaxFailedAttempts("3");

        // Three failed attempts. Each one fails and rolls its request back, so
        // the count only adds up if it is committed in its own transaction.
        for (int i = 0; i < 3; i++) {
            assertEquals(AUTH_WARNING, authenticate(WRONG_CREDENTIAL), "Attempt " + (i + 1) + " should be an authentication warning");
        }

        // The account is now locked: even the correct passphrase is refused,
        // with the answer to a wrong one. This only holds if the three
        // rolled-back failures were nevertheless counted.
        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "%s", "credential": "%s"}
                        """.formatted(VICTIM, VICTIM_CREDENTIAL))
                .when().post("/authenticated")
                .then().statusCode(200)
                .body("returnCode", equalTo(AUTH_WARNING))
                .body("returnMessage", equalTo(REFUSED_MESSAGE));
    }

    @Test
    void testFailuresAtTheSameMomentAllCount() throws InterruptedException, ExecutionException {
        // Many wrong passphrases at once, as many as the limit: each is
        // counted on top of the others, in a transaction of its own, so a
        // single count lost to another would leave the account unlocked.
        createVictim(CONCURRENT_VICTIM);
        setMaxFailedAttempts(Integer.toString(CONCURRENT_ATTEMPTS));

        final List<Future<Integer>> results = new ArrayList<>(CONCURRENT_ATTEMPTS);
        try (ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS)) {
            final CountDownLatch start = new CountDownLatch(1);
            for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return authenticate(CONCURRENT_VICTIM, WRONG_CREDENTIAL);
                }));
            }
            start.countDown();
            for (final Future<Integer> result : results) {
                assertEquals(AUTH_WARNING, result.get());
            }
        }

        assertEquals(CONCURRENT_ATTEMPTS, failedLogins(CONCURRENT_VICTIM));
        // Locked: the right passphrase is refused.
        assertEquals(AUTH_WARNING, authenticate(CONCURRENT_VICTIM, VICTIM_CREDENTIAL));
    }

    @Test
    void testRequestWhichSavesItsMemberKeepsTheCount() {
        // A request with the right passphrase which saves its Member (here,
        // an avatar) clears the count; it must not write an older count back
        // when it commits, so two more failures don't lock the account.
        createVictim(SAVING_VICTIM);
        setMaxFailedAttempts("3");
        authenticate(SAVING_VICTIM, WRONG_CREDENTIAL);
        authenticate(SAVING_VICTIM, WRONG_CREDENTIAL);
        given().contentType(ContentType.JSON)
                .body("""
                        {"accountName": "%s", "credential": "%s", "avatar": "raven"}
                        """.formatted(SAVING_VICTIM, VICTIM_CREDENTIAL))
                .when().post("/members/updateMember")
                .then().statusCode(200)
                .body("returnCode", equalTo(ReturnCode.SUCCESS.getCode()));
        assertEquals(0, failedLogins(SAVING_VICTIM));

        authenticate(SAVING_VICTIM, WRONG_CREDENTIAL);
        authenticate(SAVING_VICTIM, WRONG_CREDENTIAL);
        assertEquals(2, failedLogins(SAVING_VICTIM));
        assertEquals(ReturnCode.SUCCESS.getCode(), authenticate(SAVING_VICTIM, VICTIM_CREDENTIAL));
    }

    @Test
    void testFailedDataRequestCounts() {
        // A request for data runs in a transaction of its own (with the
        // stream timeout); its wrong passphrases count all the same.
        createVictim(DATA_VICTIM);
        setMaxFailedAttempts("3");
        for (int i = 0; i < 3; i++) {
            given().contentType(ContentType.JSON)
                    .body("""
                            {"accountName": "%s", "credential": "%s", "dataId": "%s"}
                            """.formatted(DATA_VICTIM, WRONG_CREDENTIAL, UUID.randomUUID()))
                    .when().post("/data/deleteData")
                    .then().statusCode(200)
                    .body("returnCode", equalTo(AUTH_WARNING));
        }

        assertEquals(3, failedLogins(DATA_VICTIM));
        assertEquals(AUTH_WARNING, authenticate(DATA_VICTIM, VICTIM_CREDENTIAL));
    }

    private int failedLogins(final String account) {
        final Object count = QuarkusTransaction.requiringNew().call(() -> entityManager
                .createNativeQuery("select failed_logins from eds_members where name = :name")
                .setParameter("name", account)
                .getSingleResult());
        return ((Number) count).intValue();
    }

    private static String encode(final String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
