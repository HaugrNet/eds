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
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Utilities;
import java.util.Date;
import java.util.UUID;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class CircleTest {

    @Test
    public void testClassflow() {
        final String id = UUID.randomUUID().toString();
        final String name = "Circle Name";
        final String key = UUID.randomUUID().toString();
        final Date date = new Date();

        final Circle circle = new Circle();
        circle.setCircleId(id);
        circle.setCircleName(name);
        circle.setCircleKey(key);
        circle.setAdded(date);

        assertThat(circle.getCircleId(), is(id));
        assertThat(circle.getCircleName(), is(name));
        assertThat(circle.getCircleKey(), is(key));
        assertThat(circle.getAdded(), is(date));
    }

    @Test
    public void testToString() {
        final Circle circle = new Circle();
        final Circle sameCircle = new Circle();
        final Circle emptyCircle = new Circle();

        circle.setCircleId(UUID.randomUUID().toString());
        circle.setCircleName(UUID.randomUUID().toString());
        circle.setCircleKey(UUID.randomUUID().toString());
        circle.setAdded(Utilities.newDate());
        sameCircle.setCircleId(circle.getCircleId());
        sameCircle.setCircleName(circle.getCircleName());
        sameCircle.setCircleKey(circle.getCircleKey());
        sameCircle.setAdded(circle.getAdded());

        assertThat(circle.toString(), is(sameCircle.toString()));
        assertThat(circle.toString(), is(not(emptyCircle.toString())));
    }
}
