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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.core.exceptions.CryptoException;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.entities.MetadataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Sets the size of the content in Metadata (PLAN.md, L1). The size is
 * the plaintext size, computed from the stored length and the chunk size in
 * the header (PLAN.md, Crypto §4), so the content itself is never read; one query
 * covers a whole listing.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ContentSizes {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentSizes.class);

    private ContentSizes() {
        // Utility class.
    }

    /**
     * Sets the size of each Metadata from its entity; both lists are in the
     * same order. Folders, Data Objects without content and content with an
     * unreadable header get no size.
     *
     * @param dao      Data DAO, for the connection
     * @param entities The entities
     * @param metadata The converted Metadata, same order
     */
    static void fill(final DataDao dao, final List<MetadataEntity> entities, final List<Metadata> metadata) {
        final List<Long> ids = new ArrayList<>(entities.size());
        for (final MetadataEntity entity : entities) {
            ids.add(entity.getId());
        }
        final Map<Long, EncryptedDataStore.StoredHead> heads = dao.withConnection(connection -> EncryptedDataStore.heads(connection, ids, DataFormat.HEADER_BYTES));

        for (int i = 0; i < entities.size(); i++) {
            final EncryptedDataStore.StoredHead head = heads.get(entities.get(i).getId());
            if (head != null) {
                metadata.get(i).setSize(size(head));
            }
        }
    }

    /**
     * The plaintext size of stored content.
     *
     * @param head Stored length and at least the header
     * @return Size, or null if the header can't be read
     */
    static Long size(final EncryptedDataStore.StoredHead head) {
        try {
            final long size = DataFormat.plaintextLength(head.storedLength(), DataFormat.Header.read(head.head()).chunkSize());
            return (size >= 0) ? size : null;
        } catch (CryptoException e) {
            // Content whose header can't be read is listed without a size.
            LOGGER.debug("The size of stored content is unknown, as its header can't be read.", e);
            return null;
        }
    }
}
