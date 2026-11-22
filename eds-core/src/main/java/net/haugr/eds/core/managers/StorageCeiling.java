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
package net.haugr.eds.core.managers;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.SchemaManager;
import net.haugr.eds.core.model.Settings;

/**
 * <p>The ceiling for all stored content together
 * ({@code eds.data.max.total.size}, see {@link SchemaManager#ceiling}),
 * checked before content is written. It
 * counts the stored, encrypted bytes, as that is what the database holds.</p>
 *
 * <p>With the setting at 0, the ceiling follows the kind of system: 2 GiB on
 * a test system (in-memory H2, whose data lives in the heap and is lost when
 * the server stops), 250 GiB on a production system. A positive value sets
 * the ceiling explicitly, as the opt-in to allow more.</p>
 *
 * <p>The check is made without a lock, so writes running at the same time
 * can each pass it and together exceed the ceiling, by at most what they add.
 * A lock would serialise all uploads, which may take long. If the storage
 * itself is full, the write fails and is rolled back.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class StorageCeiling {

    static final String REACHED = "The content cannot be stored, the storage limit of EDS is reached.";

    private StorageCeiling() {
        // Utility class.
    }

    /**
     * Refuses the write if it would exceed the ceiling.
     *
     * @param settings   EDS Settings
     * @param dao        Data DAO, for the current transaction
     * @param replacedId Internal Id of the content which is replaced, or -1
     * @param adding     Stored length of the new content
     * @throws EDSException with INSUFFICIENT_STORAGE, if it would exceed the ceiling
     */
    static void check(final Settings settings, final DataDao dao, final long replacedId, final long adding) {
        dao.withConnection(connection -> {
            final long ceiling = SchemaManager.ceiling(settings, connection);
            if (ceiling > 0) {
                final long used = EncryptedDataStore.totalLength(connection, replacedId);
                if (used + adding > ceiling) {
                    // The usage of the whole EDS isn't given to the Member.
                    throw new EDSException(ReturnCode.INSUFFICIENT_STORAGE, REACHED);
                }
            }
            return null;
        });
    }

    /**
     * The stored bytes which may still be written, for content whose length
     * isn't known before it is read.
     *
     * @param settings   EDS Settings
     * @param dao        Data DAO, for the current transaction
     * @param replacedId Internal Id of the content which is replaced, or -1
     * @return Stored bytes below the ceiling, never negative
     */
    static long remaining(final Settings settings, final DataDao dao, final long replacedId) {
        return dao.withConnection(connection -> {
            final long ceiling = SchemaManager.ceiling(settings, connection);
            if (ceiling <= 0) {
                return Long.MAX_VALUE;
            }

            return Math.max(0, ceiling - EncryptedDataStore.totalLength(connection, replacedId));
        });
    }
}
