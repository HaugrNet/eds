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
package net.haugr.eds.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Small content is read as a whole, with its parts in the same statement
 * as the Data Object. Read by a statement of their own, the parts could come
 * from after a change or delete which committed meanwhile: an entry then
 * had no content ("too short for the header"), and a small Data Object the
 * checksum of another version, so a sound object was taken for damaged.</p>
 *
 * <p>The delete meanwhile is made here in the same transaction, which a
 * later statement sees just as it would see another transaction's commit.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class DataDaoWholeContentTest extends DatabaseSetup {

    @Test
    void testFindByMetadataReadsThePartsWithTheData() {
        assertContentIsWhole(dataDao -> dataDao.findDataByMetadata(metadataOf(storeContent())));
    }

    @Test
    void testFindByMemberAndExternalIdReadsThePartsWithTheData() {
        assertContentIsWhole(dataDao -> {
            final MemberEntity member = entityManager
                    .createQuery("select m from MemberEntity m where m.externalId = :id", MemberEntity.class)
                    .setParameter("id", MEMBER_1_ID)
                    .getSingleResult();
            return dataDao.findDataByMemberAndExternalId(member, storeContent());
        });
    }

    private void assertContentIsWhole(final Function<DataDao, DataEntity> finder) {
        final DataDao dataDao = new DataDao(entityManager);
        final DataEntity found = finder.apply(dataDao);
        assertNotNull(found);
        final long id = found.getId();
        final byte[] expected = storedBytes(id);

        // Another transaction removes the content and commits
        entityManager.createNativeQuery("delete from eds_data_parts where data_id = :id")
                .setParameter("id", id)
                .executeUpdate();

        assertArrayEquals(expected, found.getData(), "the content must be the one read with the Data Object");
    }

    private String storeContent() {
        final ShareBean bean = new ShareBean(entityManager, settings);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "whole", 1_000);
        final ProcessDataResponse response = bean.processData(request);
        assertTrue(response.isOk(), response.getReturnMessage());
        // A new persistence context, so nothing is read from the cache
        entityManager.flush();
        entityManager.clear();

        return response.getDataId();
    }

    private byte[] storedBytes(final long id) {
        final DataDao dataDao = new DataDao(entityManager);
        return dataDao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0,
                Math.toIntExact(EncryptedDataStore.length(connection, id))));
    }

    private MetadataEntity metadataOf(final String dataId) {
        return entityManager
                .createQuery("select m from MetadataEntity m where m.externalId = :id", MetadataEntity.class)
                .setParameter("id", dataId)
                .getSingleResult();
    }
}
