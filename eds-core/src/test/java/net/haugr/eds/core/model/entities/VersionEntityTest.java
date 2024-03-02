/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.core.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.setup.DatabaseSetup;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class VersionEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final Query query = entityManager.createNamedQuery("version.findAll");
        final List<VersionEntity> found = CommonDao.findList(query);
        assertEquals(4, found.size());
        assertEquals(Long.valueOf(4L), found.getFirst().getId());
        assertEquals("2.0.0", found.getFirst().getEDSVersion());
        assertEquals("H2", found.getFirst().getDBVendor());
        assertEquals(Integer.valueOf(4), found.getFirst().getSchemaVersion());
        assertNotNull(found.getFirst().getInstalled());

        // Now adding a new Entity, this must fail.
        final VersionEntity entity = new VersionEntity();
        entity.setId(9999L);
        entity.setEDSVersion("99.99.99");
        entity.setDBVendor("SuperDB");
        entity.setSchemaVersion(9999);
        entity.setInstalled(Utilities.newDate());

        try {
            entityManager.persist(entity);
        } catch (PersistenceException e) {
            assertEquals("ids for this class must be manually assigned before calling save(): VersionEntity", e.getMessage());
        }
    }
}
