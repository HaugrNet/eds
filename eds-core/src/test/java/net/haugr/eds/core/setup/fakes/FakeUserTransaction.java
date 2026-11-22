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
package net.haugr.eds.core.setup.fakes;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * A JTA transaction, as a transaction manager gives it, which can be told to
 * fail where a real one may: at the start, at the commit, at the rollback or
 * when its timeout is reset. It counts what it was asked to do.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class FakeUserTransaction implements UserTransaction {

    private boolean failBegin = false;
    private Exception commitFailure = null;
    private boolean failRollback = false;
    private boolean failTimeoutReset = false;

    private int status = Status.STATUS_NO_TRANSACTION;
    private int timeout = 0;
    private int commits = 0;
    private int rollbacks = 0;

    /** The next begin() fails, as for a nested transaction. */
    public FakeUserTransaction failingBegin() {
        failBegin = true;
        return this;
    }

    /**
     * The next commit() throws the given exception. A RollbackException
     * ends the transaction, as the transaction manager rolled it back; any
     * other leaves it active.
     */
    public FakeUserTransaction failingCommit(final Exception failure) {
        commitFailure = failure;
        return this;
    }

    /** A rollback() fails. */
    public FakeUserTransaction failingRollback() {
        failRollback = true;
        return this;
    }

    /** Resetting the timeout to the default fails. */
    public FakeUserTransaction failingTimeoutReset() {
        failTimeoutReset = true;
        return this;
    }

    public int commits() {
        return commits;
    }

    public int rollbacks() {
        return rollbacks;
    }

    public int timeout() {
        return timeout;
    }

    @Override
    public void begin() throws NotSupportedException {
        if (failBegin) {
            throw new NotSupportedException("Nested transactions are not supported.");
        }
        status = Status.STATUS_ACTIVE;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
        if (commitFailure instanceof RollbackException failure) {
            status = Status.STATUS_NO_TRANSACTION;
            throw failure;
        }
        if (commitFailure instanceof HeuristicMixedException failure) {
            throw failure;
        }
        if (commitFailure instanceof HeuristicRollbackException failure) {
            throw failure;
        }
        if (commitFailure instanceof SystemException failure) {
            throw failure;
        }
        commits++;
        status = Status.STATUS_NO_TRANSACTION;
    }

    @Override
    public void rollback() throws SystemException {
        rollbacks++;
        if (failRollback) {
            throw new SystemException("The rollback failed.");
        }
        status = Status.STATUS_NO_TRANSACTION;
    }

    @Override
    public void setRollbackOnly() {
        status = Status.STATUS_MARKED_ROLLBACK;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setTransactionTimeout(final int seconds) throws SystemException {
        if (failTimeoutReset && (seconds == 0)) {
            throw new SystemException("The timeout can't be reset.");
        }
        timeout = seconds;
    }
}
