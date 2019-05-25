/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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
package io.javadog.cws.core.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.model.CommonDao;
import java.util.Date;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class VersionEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final Query query = entityManager.createNamedQuery("version.findAll");
        final List<VersionEntity> found = CommonDao.findList(query);
        assertThat(found.size(), is(2));
        assertThat(found.get(0).getId(), is(2L));
        assertThat(found.get(0).getCwsVersion(), is("1.1.0"));
        assertThat(found.get(0).getDbVendor(), is("H2"));
        assertThat(found.get(0).getSchemaVersion(), is(2));
        assertNotNull(found.get(0).getInstalled());
        assertThat(found.get(1).getId(), is(1L));
        assertThat(found.get(1).getCwsVersion(), is("1.0.0"));
        assertThat(found.get(1).getDbVendor(), is("H2"));
        assertThat(found.get(1).getSchemaVersion(), is(1));
        assertNotNull(found.get(1).getInstalled());

        // Now adding a new Entity, this must fail.
        final VersionEntity entity = new VersionEntity();
        entity.setId(9999L);
        entity.setCwsVersion("99.99.99");
        entity.setDbVendor("SuperDB");
        entity.setSchemaVersion(9999);
        entity.setInstalled(new Date());

        try {
            entityManager.persist(entity);
        } catch (PersistenceException e) {
            assertThat(e.getMessage(), is("ids for this class must be manually assigned before calling save(): io.javadog.cws.core.model.entities.VersionEntity"));
        }
    }
}
