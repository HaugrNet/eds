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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.util.HashMap;
import java.util.Map;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.responses.AuthenticateResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the Beans mark the transaction as rollback-only when a
 * request fails with an EDS Exception, so partial writes are not committed.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class TransactionRollbackTest extends DatabaseSetup {

    @Test
    void testFailedManagementRequestMarksRollback() {
        final FakeRegistry registry = new FakeRegistry(Status.STATUS_ACTIVE);
        final ManagementBean bean = new ManagementBean(entityManager, settings, registry);

        // Null is passed on purpose, to check how it is refused.
        @SuppressWarnings("findbugs:NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
        final AuthenticateResponse response = bean.authenticated(null);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertTrue(registry.rollbackOnly);
    }

    @Test
    void testSuccessfulManagementRequestDoesNotMarkRollback() {
        final FakeRegistry registry = new FakeRegistry(Status.STATUS_ACTIVE);
        final ManagementBean bean = new ManagementBean(entityManager, settings, registry);

        final AuthenticateResponse response = bean.authenticated(prepareRequest(Authentication.class, MEMBER_1));

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertFalse(registry.rollbackOnly);
    }

    @Test
    void testFailedShareRequestMarksRollback() {
        final FakeRegistry registry = new FakeRegistry(Status.STATUS_ACTIVE);
        final ShareBean bean = new ShareBean(entityManager, settings, registry);

        final FetchDataTypeResponse response = bean.fetchDataTypes(new FetchDataTypeRequest());

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertTrue(registry.rollbackOnly);
    }

    @Test
    void testNoActiveTransactionIsIgnored() {
        final FakeRegistry registry = new FakeRegistry(Status.STATUS_NO_TRANSACTION);

        Transactions.markRollbackOnly(registry);

        assertFalse(registry.rollbackOnly);
    }

    @Test
    void testMissingRegistryIsIgnored() {
        assertDoesNotThrow(() -> Transactions.markRollbackOnly(null));
    }

    /**
     * Minimal registry, only recording whether rollback-only was requested.
     */
    private static final class FakeRegistry implements TransactionSynchronizationRegistry {

        private final int status;
        private final Map<Object, Object> resources = new HashMap<>();
        private boolean rollbackOnly = false;

        private FakeRegistry(final int status) {
            this.status = status;
        }

        @Override
        public Object getTransactionKey() {
            return this;
        }

        @Override
        public void putResource(final Object key, final Object value) {
            resources.put(key, value);
        }

        @Override
        public Object getResource(final Object key) {
            return resources.get(key);
        }

        @Override
        public void registerInterposedSynchronization(final Synchronization sync) {
            // Not needed for the tests.
        }

        @Override
        public int getTransactionStatus() {
            return rollbackOnly ? Status.STATUS_MARKED_ROLLBACK : status;
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean getRollbackOnly() {
            return rollbackOnly;
        }
    }
}
