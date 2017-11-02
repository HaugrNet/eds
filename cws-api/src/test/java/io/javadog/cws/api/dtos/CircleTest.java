/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class CircleTest {

    @Test
    public void testClass() {
        final String id = UUID.randomUUID().toString();
        final String name = "Circle Name";
        final Date date = new Date();

        final Circle circle = new Circle();
        circle.setCircleId(id);
        circle.setCircleName(name);
        circle.setCreated(date);

        assertThat(circle.getCircleId(), is(id));
        assertThat(circle.getCircleName(), is(name));
        assertThat(circle.getCreated(), is(date));
    }

    @Test
    public void testStandardMethods() {
        final Circle circle = new Circle();
        final Circle sameCircle = new Circle();
        final Circle emptyCircle = new Circle();

        circle.setCircleId(UUID.randomUUID().toString());
        circle.setCircleName("The Circle Name");
        circle.setCreated(new Date());
        sameCircle.setCircleId(circle.getCircleId());
        sameCircle.setCircleName(circle.getCircleName());
        sameCircle.setCreated(circle.getCreated());

        assertThat(circle.equals(null), is(false));
        assertThat(circle.equals(circle), is(true));
        assertThat(circle.equals(sameCircle), is(true));
        assertThat(circle.equals(emptyCircle), is(false));

        assertThat(circle.hashCode(), is(sameCircle.hashCode()));
        assertThat(circle.hashCode(), is(not(emptyCircle.hashCode())));

        assertThat(circle.toString(), is(sameCircle.toString()));
        assertThat(circle.toString(), is(not(emptyCircle.toString())));
    }
}
