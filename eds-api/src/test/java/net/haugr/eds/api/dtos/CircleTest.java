/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.api.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.haugr.eds.api.common.Utilities;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class CircleTest {

    @Test
    void testClassFlow() {
        final String id = UUID.randomUUID().toString();
        final String name = "Circle Name";
        final String key = UUID.randomUUID().toString();
        final LocalDateTime date = Utilities.newDate();

        final Circle circle = new Circle();
        circle.setCircleId(id);
        circle.setCircleName(name);
        circle.setCircleKey(key);
        circle.setAdded(date);

        assertEquals(id, circle.getCircleId());
        assertEquals(name, circle.getCircleName());
        assertEquals(key, circle.getCircleKey());
        assertEquals(date, circle.getAdded());
    }

    @Test
    void testToString() {
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

        assertEquals(sameCircle.toString(), circle.toString());
        assertNotEquals(emptyCircle.toString(), circle.toString());
    }
}
