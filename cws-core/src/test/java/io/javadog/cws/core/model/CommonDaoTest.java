/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.stubs.FakeEntityManager;
import io.javadog.cws.core.stubs.FakeQuery;
import org.junit.jupiter.api.Test;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class CommonDaoTest extends DatabaseSetup {

    @Test
    void testInvalidDataRead() {
        // Creating a Query, which will attempt to read data from a record
        // which doesn't exist.
        final Query query = entityManager.createNativeQuery("select * from versions");

        final CWSException cause = assertThrows(CWSException.class, () -> CommonDao.findList(query));
        assertEquals(ReturnCode.DATABASE_ERROR, cause.getReturnCode());
        assertEquals("org.hibernate.exception.SQLGrammarException: could not prepare statement", cause.getMessage());
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
