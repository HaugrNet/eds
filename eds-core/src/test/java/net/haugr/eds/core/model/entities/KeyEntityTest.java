/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.core.model.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.model.Settings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class KeyEntityTest extends DatabaseSetup {

    @Test
    void testEntity() {
        final Settings mySettings = newSettings();
        final KeyEntity key = new KeyEntity();
        key.setCircle(find(CircleEntity.class, 1L));
        key.setAlgorithm(mySettings.getSymmetricAlgorithm());
        key.setStatus(Status.ACTIVE);
        persistAndDetach(key);
        assertNotNull(key.getId());

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNotNull(found);
        Assertions.assertEquals(key.getAlgorithm(), found.getAlgorithm());
        Assertions.assertEquals(key.getStatus(), found.getStatus());
        assertEquals(1L, found.getCircle().getId());

        found.setStatus(Status.RETIRING);
        save(found);

        final KeyEntity updated = find(KeyEntity.class, key.getId());
        Assertions.assertNotEquals(key.getStatus(), updated.getStatus());
    }

    @Test
    void testCircleCannotChange() {
        final KeyEntity key = prepareKey();

        // The Circle of a secret is set once: a changed Circle isn't saved.
        key.setCircle(find(CircleEntity.class, 2L));
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertEquals(1L, found.getCircle().getId());
    }
}
