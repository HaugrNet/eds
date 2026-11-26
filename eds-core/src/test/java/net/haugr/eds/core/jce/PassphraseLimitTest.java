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
package net.haugr.eds.core.jce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.LogCapture;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * Only so many passphrases are checked at once (ticket 20); beyond them a
 * request waits for a place, and is answered as busy if none comes free.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class PassphraseLimitTest extends DatabaseSetup {

    @Test
    void testBeyondThePlacesTheRequestIsBusy() {
        final PassphraseLimit limit = new PassphraseLimit();
        limit.acquire(2, 0);
        limit.acquire(2, 0);
        assertEquals(2, limit.running());

        final EDSException busy = assertThrows(EDSException.class, () -> limit.acquire(2, 0));
        assertEquals(ReturnCode.SERVICE_BUSY, busy.getReturnCode());
        assertEquals(503, busy.getReturnCode().getHttpCode());
        assertEquals(2, limit.running());

        limit.release();
        limit.acquire(2, 0);
        assertEquals(2, limit.running());
    }

    @Test
    void testAWaitingRequestGetsAPlaceWhichComesFree() throws Exception {
        final PassphraseLimit limit = new PassphraseLimit();
        limit.acquire(1, 0);
        final CountDownLatch waiting = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            final Future<?> second = executor.submit(() -> {
                waiting.countDown();
                limit.acquire(1, 30_000);
            });
            assertTrue(waiting.await(10, TimeUnit.SECONDS));
            assertFalse(second.isDone());
            limit.release();
            second.get(10, TimeUnit.SECONDS);
        }
        assertEquals(1, limit.running());
    }

    @Test
    void testEveryDerivationOfAPassphraseTakesAPlace() {
        final Settings settings = newSettings();
        settings.set(StandardSetting.AUTH_CONCURRENT_PASSPHRASES.getKey(), "1");
        settings.set(StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS.getKey(), "0");
        final PassphraseLimit limit = new PassphraseLimit();
        final Crypto crypto = new Crypto(settings, limit);
        final byte[] salt = new byte[16];
        final byte[] passphrase = crypto.stringToBytes("a passphrase");

        crypto.generatePasswordKey(settings.getPasswordAlgorithm(), passphrase, salt, 1000);
        assertEquals(0, limit.running(), "The place is given back.");

        limit.acquire(1, 0);
        final KeyAlgorithm algorithm = settings.getPasswordAlgorithm();
        final EDSException busy = assertThrows(EDSException.class,
                () -> crypto.generatePasswordKey(algorithm, passphrase, salt, 1000));
        assertEquals(ReturnCode.SERVICE_BUSY, busy.getReturnCode());
        assertEquals(1, limit.running());
    }

    @Test
    void testABusyAnswerIsNoAttemptOfTheLock() throws Exception {
        // The review of ticket 20: with the lock of #68 on, a request which
        // is answered as busy mustn't count as an attempt, or a user whose
        // client retries in a busy moment is locked out.
        final Settings settings = newSettings();
        settings.set(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS.getKey(), "1");
        settings.set(StandardSetting.AUTH_CONCURRENT_PASSPHRASES.getKey(), "1");
        settings.set(StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS.getKey(), "0");
        final ManagementBean bean = prepareManagementBean(settings);
        final CountDownLatch held = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            // Another request holds the only place.
            final Future<?> other = executor.submit(() -> {
                try (var _ = PassphraseLimit.SHARED.take(1, 30_000)) {
                    held.countDown();
                    assertTrue(done.await(30, TimeUnit.SECONDS));
                }
                return null;
            });
            assertTrue(held.await(30, TimeUnit.SECONDS));
            final AuthenticateResponse busy = bean.authenticated(prepareRequest(Authentication.class, MEMBER_1));
            assertEquals(ReturnCode.SERVICE_BUSY.getCode(), busy.getReturnCode(), busy.getReturnMessage());
            done.countDown();
            other.get(30, TimeUnit.SECONDS);
        }

        assertEquals(0, ((Number) entityManager.createNativeQuery("select failed_logins from eds_members where name = :name")
                .setParameter("name", MEMBER_1).getSingleResult()).intValue());
        final AuthenticateResponse accepted = bean.authenticated(prepareRequest(Authentication.class, MEMBER_1));
        assertEquals(ReturnCode.SUCCESS.getCode(), accepted.getReturnCode(), accepted.getReturnMessage());
    }

    @Test
    void testAPlaceIsTakenOncePerThread() {
        final PassphraseLimit limit = new PassphraseLimit();
        try (var _ = limit.take(1, 0)) {
            try (var _ = limit.take(1, 0)) {
                assertEquals(1, limit.running());
            }
            assertEquals(1, limit.running(), "The outer place is still held.");
        }
        assertEquals(0, limit.running());
    }

    @Test
    void testTheWaitEndsInTime() {
        final PassphraseLimit limit = new PassphraseLimit();
        limit.acquire(1, 0);
        final long started = System.nanoTime();

        assertThrows(EDSException.class, () -> limit.acquire(1, 200));
        final long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        assertTrue(waited >= 150, String.valueOf(waited));
        assertTrue(waited < 10_000, String.valueOf(waited));
    }

    @Test
    void testTheBusyAnswersAreSummedUpInTheLog() {
        final PassphraseLimit limit = new PassphraseLimit();
        limit.acquire(1, 0);
        try (LogCapture log = LogCapture.of(PassphraseLimit.class)) {
            for (int i = 0; i < 5; i++) {
                assertThrows(EDSException.class, () -> limit.acquire(1, 0));
            }
            // The first at once; the rest within the minute aren't logged one by one.
            assertEquals(1, log.warnings().size(), String.valueOf(log.warnings()));
            assertTrue(log.warnings().getFirst().startsWith("1 request(s) with a passphrase were answered as busy"), log.warnings().getFirst());
        }
    }

    @Test
    void testTheSettings() {
        final Settings settings = newSettings();
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() / 2), settings.getAuthConcurrentPassphrases());
        assertEquals(2000, settings.getAuthPassphraseWaitMillis());

        settings.set(StandardSetting.AUTH_CONCURRENT_PASSPHRASES.getKey(), "3");
        settings.set(StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS.getKey(), "0");
        assertEquals(3, settings.getAuthConcurrentPassphrases());
        assertEquals(0, settings.getAuthPassphraseWaitMillis());

        // A stored value which isn't a number, or out of range, can't stop the logins.
        settings.set(StandardSetting.AUTH_CONCURRENT_PASSPHRASES.getKey(), "many");
        settings.set(StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS.getKey(), "999999999");
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() / 2), settings.getAuthConcurrentPassphrases());
        assertEquals(Settings.MAX_PASSPHRASE_WAIT_MILLIS, settings.getAuthPassphraseWaitMillis());
    }
}
