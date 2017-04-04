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
public final class CircleEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final CircleEntity entity = prepareCircle("Circle 1");
        entityManager.flush();
        entityManager.clear();

        final CircleEntity found = entityManager.find(CircleEntity.class, entity.getId());
        assertThat(found, is(not(nullValue())));
        assertThat(found.getName(), is(entity.getName()));

        wait(2500);
        found.setName("Circle 2");
        persist(found);
        entityManager.flush();
        entityManager.clear();

        final CircleEntity updated = entityManager.find(CircleEntity.class, entity.getId());
        assertThat(updated, is(not(nullValue())));
        assertThat(updated.getName(), is(not(entity.getName())));
        assertThat(updated.getModified().after(entity.getModified()), is(true));
    }
}
