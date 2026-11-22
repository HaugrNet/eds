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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.RollbackException;
import java.util.concurrent.atomic.AtomicBoolean;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.setup.fakes.FakeUserTransaction;
import org.junit.jupiter.api.Test;

/**
 * The work of a streaming request in a JTA transaction of its own, also
 * where the transaction itself fails.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StreamTransactionTest {

    private static final int TIMEOUT = 600;

    @Test
    void testWithoutTransactionTheWorkRunsInTheCallers() {
        assertEquals("done", StreamTransaction.run(null, TIMEOUT, () -> "done"));
    }

    @Test
    void testTheWorkIsCommittedWithItsOwnTimeout() {
        final FakeUserTransaction transaction = new FakeUserTransaction();
        final int[] timeoutDuringWork = new int[1];

        final String result = StreamTransaction.run(transaction, TIMEOUT, () -> {
            timeoutDuringWork[0] = transaction.timeout();
            return "done";
        });

        assertEquals("done", result);
        assertEquals(TIMEOUT, timeoutDuringWork[0]);
        assertEquals(1, transaction.commits());
        assertEquals(0, transaction.rollbacks());
        assertEquals(0, transaction.timeout());
    }

    @Test
    void testWorkMarkedRollbackOnlyIsRolledBack() {
        final FakeUserTransaction transaction = new FakeUserTransaction();

        final String result = StreamTransaction.run(transaction, TIMEOUT, () -> {
            transaction.setRollbackOnly();
            return "undone";
        });

        assertEquals("undone", result);
        assertEquals(0, transaction.commits());
        assertEquals(1, transaction.rollbacks());
    }

    @Test
    void testFailedWorkIsRolledBackAndItsExceptionKept() {
        final FakeUserTransaction transaction = new FakeUserTransaction();
        final EDSException failure = new EDSException(ReturnCode.VERIFICATION_WARNING, "The work failed.");

        final EDSException thrown = assertThrows(EDSException.class, () -> StreamTransaction.run(transaction, TIMEOUT, () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(1, transaction.rollbacks());
        assertEquals(0, transaction.timeout());
    }

    @Test
    void testAFailedRollbackDoesNotHideTheFailureOfTheWork() {
        final FakeUserTransaction transaction = new FakeUserTransaction().failingRollback();
        final IllegalStateException failure = new IllegalStateException("The work failed.");

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> StreamTransaction.run(transaction, TIMEOUT, () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(1, transaction.rollbacks());
    }

    @Test
    void testATransactionWhichCannotStartRunsNoWork() {
        final FakeUserTransaction transaction = new FakeUserTransaction().failingBegin();
        final AtomicBoolean ran = new AtomicBoolean(false);

        final EDSException thrown = assertThrows(EDSException.class, () -> StreamTransaction.run(transaction, TIMEOUT, () -> ran.getAndSet(true)));

        assertEquals(ReturnCode.DATABASE_ERROR, thrown.getReturnCode());
        assertEquals("Cannot start the transaction: Nested transactions are not supported.", thrown.getMessage());
        assertFalse(ran.get());
    }

    @Test
    void testACommitRolledBackByTheTransactionManagerFails() {
        final FakeUserTransaction transaction = new FakeUserTransaction().failingCommit(new RollbackException("Timed out."));

        final EDSException thrown = assertThrows(EDSException.class, () -> StreamTransaction.run(transaction, TIMEOUT, () -> "done"));

        assertEquals(ReturnCode.DATABASE_ERROR, thrown.getReturnCode());
        assertEquals("The transaction failed: Timed out.", thrown.getMessage());
        // The transaction is over already, so there is nothing to roll back.
        assertEquals(0, transaction.rollbacks());
    }

    @Test
    void testACommitWhichLeavesTheTransactionIsRolledBack() {
        final FakeUserTransaction transaction = new FakeUserTransaction().failingCommit(new HeuristicMixedException("Partly committed."));

        final EDSException thrown = assertThrows(EDSException.class, () -> StreamTransaction.run(transaction, TIMEOUT, () -> "done"));

        assertEquals(ReturnCode.DATABASE_ERROR, thrown.getReturnCode());
        assertEquals("The transaction failed: Partly committed.", thrown.getMessage());
        assertEquals(1, transaction.rollbacks());
    }

    @Test
    void testAFailedTimeoutResetIsIgnored() {
        final FakeUserTransaction transaction = new FakeUserTransaction().failingTimeoutReset();

        assertEquals("done", StreamTransaction.run(transaction, TIMEOUT, () -> "done"));
        assertEquals(1, transaction.commits());
    }
}
