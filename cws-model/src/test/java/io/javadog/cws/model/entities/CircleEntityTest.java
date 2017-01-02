package io.javadog.cws.model.entities;

import io.javadog.cws.model.EntityManagerSetup;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CircleEntityTest extends EntityManagerSetup {

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
