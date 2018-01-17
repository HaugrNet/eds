/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
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
    public void testClassflow() {
        final String id = UUID.randomUUID().toString();
        final String name = "Circle Name";
        final Date date = new Date();

        final Circle circle = new Circle();
        circle.setCircleId(id);
        circle.setCircleName(name);
        circle.setAdded(date);

        assertThat(circle.getCircleId(), is(id));
        assertThat(circle.getCircleName(), is(name));
        assertThat(circle.getAdded(), is(date));
    }

    @Test
    public void testStandardMethods() {
        final Circle circle = prepareCircle(UUID.randomUUID().toString(), "The Circle Nane", new Date());
        final Circle sameCircle = new Circle();
        final Circle emptyCircle = new Circle();

        sameCircle.setCircleId(circle.getCircleId());
        sameCircle.setCircleName(circle.getCircleName());
        sameCircle.setAdded(circle.getAdded());

        assertThat(circle.equals(null), is(false));
        assertThat(circle.equals(circle), is(true));
        assertThat(circle.equals(sameCircle), is(true));
        assertThat(circle.equals(emptyCircle), is(false));

        assertThat(circle.hashCode(), is(sameCircle.hashCode()));
        assertThat(circle.hashCode(), is(not(emptyCircle.hashCode())));

        assertThat(circle.toString(), is(sameCircle.toString()));
        assertThat(circle.toString(), is(not(emptyCircle.toString())));
    }

    @Test
    public void testEquality() {
        final String circleId1 = UUID.randomUUID().toString();
        final String circleId2 = UUID.randomUUID().toString();
        final String circleName1 = "First Circle Name";
        final String circleName2 = "Second Circle Name";
        final Date added1 = new Date(1212121212L);
        final Date added2 = new Date(2121212121L);
        final Circle circle1 = prepareCircle(circleId1, circleName1, added1);
        final Circle circle2 = prepareCircle(circleId2, circleName1, added1);
        final Circle circle3 = prepareCircle(circleId1, circleName2, added1);
        final Circle circle4 = prepareCircle(circleId1, circleName1, added2);

        assertThat(circle1.equals(circle2), is(false));
        assertThat(circle2.equals(circle1), is(false));
        assertThat(circle1.equals(circle3), is(false));
        assertThat(circle3.equals(circle1), is(false));
        assertThat(circle1.equals(circle4), is(false));
        assertThat(circle4.equals(circle1), is(false));
    }

    // =========================================================================
    // Internal Helper Method
    // =========================================================================

    private static Circle prepareCircle(final String circleId, final String circleName, final Date added) {
        final Circle circle = new Circle();
        circle.setCircleId(circleId);
        circle.setCircleName(circleName);
        circle.setAdded(added);

        return circle;
    }
}
