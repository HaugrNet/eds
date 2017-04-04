/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.entities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TypeEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final TypeEntity entity = new TypeEntity();
        entity.setName("Name");
        entity.setType("Type");
        persist(entity);

        entityManager.flush();
        entityManager.clear();

        final TypeEntity found = entityManager.find(TypeEntity.class, entity.getId());
        assertThat(found, is(not(nullValue())));
    }

    @Test
    public void testUpdateCircle() {
        final TypeEntity entity = new TypeEntity();
        entity.setName("Name 1");
        entity.setType("Type 1");
        persist(entity);

        final TypeEntity found = entityManager.find(TypeEntity.class, entity.getId());
        assertThat(found, is(not(nullValue())));
        found.setName("Name 2");
        found.setType("Type 2");
        persist(found);
        assertThat(entity.getId(), is(found.getId()));
        assertThat(entity.getName(), is(found.getName()));
        assertThat(entity.getType(), is(found.getType()));
    }
}
