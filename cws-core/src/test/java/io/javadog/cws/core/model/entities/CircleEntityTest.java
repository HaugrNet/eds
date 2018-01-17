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

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CircleEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final CircleEntity entity = prepareCircle(UUID.randomUUID().toString(), "Circle 1");
        entityManager.flush();
        entityManager.clear();

        final CircleEntity found = find(CircleEntity.class, entity.getId());
        assertThat(found, is(not(nullValue())));
        assertThat(found.getName(), is(entity.getName()));

        found.setName("Circle 2");
        persist(found);
        entityManager.flush();
        entityManager.clear();

        final CircleEntity updated = find(CircleEntity.class, entity.getId());
        assertThat(updated, is(not(nullValue())));
        assertThat(updated.getName(), is(not(entity.getName())));
    }
}
