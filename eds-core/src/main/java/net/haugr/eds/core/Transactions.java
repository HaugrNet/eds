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

import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * <p>Helper for the transactional Beans. The Beans catch all EDS Exceptions
 * and convert them into a Response, so the Exception never reaches the
 * transaction interceptor, and the transaction would otherwise be committed
 * with whatever was written before the failure. Marking the transaction as
 * rollback-only prevents that.</p>
 *
 * <p>Outside a JTA container (e.g. in the unit tests, which manage their own
 * transactions), there is no registry, and nothing is done.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class Transactions {

    private Transactions() {
        // Utility class, no instances.
    }

    /**
     * Marks the current transaction as rollback-only, if there is an active
     * transaction.
     *
     * @param registry The JTA Transaction Synchronization Registry, may be null
     */
    static void markRollbackOnly(final TransactionSynchronizationRegistry registry) {
        if ((registry != null) && (registry.getTransactionStatus() == Status.STATUS_ACTIVE)) {
            registry.setRollbackOnly();
        }
    }
}
