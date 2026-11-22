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

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import java.util.function.Supplier;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Runs a streaming request (PLAN.md, Streaming, "Transactions") in its own JTA
 * transaction, with its own timeout, as streaming may take much longer than
 * the default transaction timeout. The work is committed, unless it marked
 * the transaction rollback-only or threw an exception.</p>
 *
 * <p>Without a UserTransaction (the unit tests, with a resource-local
 * transaction), the work simply runs in the caller's transaction.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StreamTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTransaction.class);

    private StreamTransaction() {
        // Utility class.
    }

    static <T> T run(final UserTransaction transaction, final int timeoutSeconds, final Supplier<T> work) {
        if (transaction == null) {
            return work.get();
        }

        try {
            transaction.setTransactionTimeout(timeoutSeconds);
            transaction.begin();
        } catch (NotSupportedException | SystemException e) {
            throw new EDSException(ReturnCode.DATABASE_ERROR, "Cannot start the transaction: " + e.getMessage(), e);
        }

        try {
            final T result = work.get();
            if (transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                transaction.rollback();
            } else {
                transaction.commit();
            }
            return result;
        } catch (RuntimeException e) {
            rollbackQuietly(transaction);
            throw e;
        } catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            rollbackQuietly(transaction);
            throw new EDSException(ReturnCode.DATABASE_ERROR, "The transaction failed: " + e.getMessage(), e);
        } finally {
            try {
                transaction.setTransactionTimeout(0);
            } catch (SystemException e) {
                // Resetting to the default timeout is best effort.
                LOGGER.debug("The transaction timeout could not be reset.", e);
            }
        }
    }

    private static void rollbackQuietly(final UserTransaction transaction) {
        try {
            if (transaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
                transaction.rollback();
            }
        } catch (SystemException | IllegalStateException | SecurityException e) {
            // The original exception is the relevant one, and is thrown on.
            LOGGER.debug("The failed transaction could not be rolled back.", e);
        }
    }
}
