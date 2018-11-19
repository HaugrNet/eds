/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.model.CommonDao;
import org.junit.Test;

import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
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
        assertThat(found.get(0).getInstalled(), is(not(nullValue())));
        assertThat(found.get(1).getId(), is(1L));
        assertThat(found.get(1).getCwsVersion(), is("1.0.0"));
        assertThat(found.get(1).getDbVendor(), is("H2"));
        assertThat(found.get(1).getSchemaVersion(), is(1));
        assertThat(found.get(1).getInstalled(), is(not(nullValue())));

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
