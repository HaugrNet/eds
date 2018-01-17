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
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataTypeEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final DataTypeEntity entity = new DataTypeEntity();
        entity.setName("Name");
        entity.setType("Type");
        persist(entity);

        entityManager.flush();
        entityManager.clear();

        final DataTypeEntity found = find(DataTypeEntity.class, entity.getId());
        assertThat(found, is(not(nullValue())));
    }

    @Test
    public void testUpdateCircle() {
        final DataTypeEntity entity = new DataTypeEntity();
        entity.setName("Name 1");
        entity.setType("Type 1");
        persist(entity);

        final DataTypeEntity found = find(DataTypeEntity.class, entity.getId());
        assertThat(found, is(not(nullValue())));
        found.setName("Name 2");
        found.setType("Type 2");
        persist(found);
        assertThat(entity.getId(), is(found.getId()));
        assertThat(entity.getName(), is(found.getName()));
        assertThat(entity.getType(), is(found.getType()));
    }
}
