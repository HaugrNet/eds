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
package net.haugr.eds.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.setup.fakes.FakeEntityManager;
import net.haugr.eds.core.setup.fakes.FakeQuery;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class CommonDaoTest extends DatabaseSetup {

    @Test
    void testInvalidDataRead() {
        // Creating a Query, which will attempt to read data from a non-existing record
        final Query query = entityManager.createNativeQuery("select * from versions");

        final EDSException cause = assertThrows(EDSException.class, () -> CommonDao.findList(query));
        assertEquals(ReturnCode.DATABASE_ERROR, cause.getReturnCode());
        assertEquals("could not prepare statement [Table \"VERSIONS\" not found; SQL statement:\n" +
                "select * from versions [42102-224]] [select * from versions]", cause.getMessage());
    }

    @Test
    void testNullResultList() {
        final EntityManager manager = new FakeEntityManager();
        final String jql = "select v from VersionEntity v order by v.id desc";
        final Query query = manager.createQuery(jql);
        query.setHint(FakeQuery.NULLABLE, Boolean.TRUE);
        final List<?> found = CommonDao.findList(query);

        assertNotNull(found);
        assertTrue(found.isEmpty());
    }
}
