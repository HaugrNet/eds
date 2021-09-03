/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.haugr.cws.core.setup.DatabaseSetup;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class CircleEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final CircleEntity entity = prepareCircle(UUID.randomUUID().toString(), "Circle 1");
        entityManager.flush();
        entityManager.clear();

        final CircleEntity found = find(CircleEntity.class, entity.getId());
        assertNotNull(found);
        assertEquals(entity.getName(), found.getName());

        found.setName("Circle 2");
        persist(found);
        entityManager.flush();
        entityManager.clear();

        final CircleEntity updated = find(CircleEntity.class, entity.getId());
        assertNotNull(updated);
        assertNotEquals(entity.getName(), updated.getName());
    }
}
