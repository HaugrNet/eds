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

import java.util.Date;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CircleEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final CircleEntity entity = prepareCircle("Circle 1");
        final Long id = entity.getId();
        entityManager.flush();
        entityManager.clear();

        final CircleEntity found = entityManager.find(CircleEntity.class, id);
        assertThat(found, is(not(nullValue())));
    }

    @Test
    public void testUpdateCircle() {
        final CircleEntity entity = prepareCircle("Circle 2");
        final Date lastModified = entity.getModified();

        persist(entity);
        assertThat(entity.getModified().after(lastModified), is(true));
    }
}
