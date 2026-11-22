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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>The bridge from JPA to the streamed access (PLAN.md, S1): the
 * JDBC work runs in the EntityManager's transaction and sees its changes.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class DataDaoStreamTest extends DatabaseSetup {

    @Test
    void testStreamedAccessToStoredData() {
        final ShareBean bean = new ShareBean(entityManager, settings);
        final ProcessDataRequest request = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "streamed", 100_000);
        final ProcessDataResponse response = bean.processData(request);
        assertTrue(response.isOk(), response.getReturnMessage());

        final DataDao dataDao = new DataDao(entityManager);
        final DataEntity entity = entityManager.createQuery("select d from DataEntity d where d.metadata.externalId = :id", DataEntity.class)
                .setParameter("id", response.getDataId())
                .getSingleResult();
        final byte[] stored = entity.getData();
        final long id = entity.getId();

        final long length = dataDao.withConnection(connection -> EncryptedDataStore.length(connection, id));
        assertEquals(stored.length, length);
        assertArrayEquals(Arrays.copyOfRange(stored, 1000, 1100), dataDao.withConnection(connection -> EncryptedDataStore.read(connection, id, 1000, 100)));
    }
}
