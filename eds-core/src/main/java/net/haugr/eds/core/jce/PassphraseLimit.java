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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>How many passphrases are checked at once (ticket 20). A check derives
 * the passphrase's key with PBKDF2, about a fifth of a CPU-second, and it is
 * made whenever the named account exists, whether the passphrase is right or
 * not, and also for a locked account. So whoever knows one account's name
 * could keep every processor busy with requests which cost them nothing.
 * Only so many are checked at once ({@code eds.auth.concurrent.passphrases},
 * half the processors by default); a request waits for a place at most
 * {@code eds.auth.passphrase.wait.millis}, and is then answered as busy
 * ({@link ReturnCode#SERVICE_BUSY}, 503), so the rest of the stone keeps its
 * share of the processors.</p>
 *
 * <p>A request takes its place before it looks the account up, and keeps it
 * until its passphrase is checked ({@link #take(int, long)}), so it waits for
 * a place before it holds a connection of the database, and before an attempt
 * is counted for the lock of the account (#68): a busy answer is no attempt.
 * The derivation within takes no second place.</p>
 *
 * <p>The busy answers aren't logged one by one, as the sender decides how
 * many there are: the first is logged as WARN, and then at most one WARN a
 * minute, with how many were answered so since the one before.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class PassphraseLimit {

    private static final Logger LOGGER = LoggerFactory.getLogger(PassphraseLimit.class);
    static final String BUSY = "The stone is busy checking other passphrases, try again shortly.";
    static final long SUMMARY_MILLIS = 60_000;

    /** The limit of the stone, which every derivation of a passphrase takes. */
    static final PassphraseLimit SHARED = new PassphraseLimit();

    /**
     * A place held by a thread; closing it gives it back once the outermost
     * holder closes it.
     */
    public interface Place extends AutoCloseable {
        @Override
        void close();
    }

    // The limit a thread holds a place of, and how many takes hold it: the
    // outermost take acquires it, the takes within count only.
    private static final ThreadLocal<PassphraseLimit> HELD_BY = new ThreadLocal<>();
    private static final ThreadLocal<int[]> HELD_TAKES = new ThreadLocal<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition freed = lock.newCondition();
    private int running = 0;
    private long refused = 0;
    private long lastSummary = 0;

    PassphraseLimit() {
        // The stone has one (SHARED); the tests make their own.
    }

    /**
     * Takes a place to check a passphrase, waiting for one if all are taken.
     *
     * @param places     How many passphrases are checked at once
     * @param waitMillis How long to wait for a place at most
     * @throws EDSException with SERVICE_BUSY, if no place came free in time
     */
    void acquire(final int places, final long waitMillis) {
        lock.lock();
        try {
            long left = TimeUnit.MILLISECONDS.toNanos(waitMillis);
            while (running >= places) {
                if (left <= 0) {
                    throw busy(places);
                }
                left = freed.awaitNanos(left);
            }
            running++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EDSException(ReturnCode.SERVICE_BUSY, BUSY, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Takes a place for the calling thread, unless it holds one already, as
     * {@link #acquire(int, long)} does.
     *
     * @param places     How many passphrases are checked at once
     * @param waitMillis How long to wait for a place at most
     * @return The place, to close when the passphrase is checked
     * @throws EDSException with SERVICE_BUSY, if no place came free in time
     */
    public Place take(final int places, final long waitMillis) {
        final PassphraseLimit holding = HELD_BY.get();
        if (holding == this) {
            HELD_TAKES.get()[0]++;
            return this::leave;
        }
        acquire(places, waitMillis);
        if (holding != null) {
            // A place of another limit (only in the tests): this one is given
            // back on its own.
            return this::release;
        }
        HELD_BY.set(this);
        HELD_TAKES.set(new int[] { 1 });

        return this::leave;
    }

    private void leave() {
        final int[] takes = HELD_TAKES.get();
        takes[0]--;
        if (takes[0] == 0) {
            HELD_BY.remove();
            HELD_TAKES.remove();
            release();
        }
    }

    /**
     * Gives a place back, for the next request which waits.
     */
    void release() {
        lock.lock();
        try {
            running--;
            freed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * How many passphrases are being checked now.
     *
     * @return Places taken
     */
    int running() {
        lock.lock();
        try {
            return running;
        } finally {
            lock.unlock();
        }
    }

    // Called with the lock held.
    private EDSException busy(final int places) {
        refused++;
        final long now = System.currentTimeMillis();
        if ((now - lastSummary) >= SUMMARY_MILLIS) {
            LOGGER.warn("{} request(s) with a passphrase were answered as busy since the last such message; {} passphrase(s) are checked at once "
                    + "(eds.auth.concurrent.passphrases).", refused, places);
            refused = 0;
            lastSummary = now;
        }

        return new EDSException(ReturnCode.SERVICE_BUSY, BUSY);
    }
}
