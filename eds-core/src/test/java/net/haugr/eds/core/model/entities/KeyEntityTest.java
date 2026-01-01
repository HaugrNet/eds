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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZoneOffset;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.model.Settings;
import java.time.LocalDateTime;

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
        key.setAlgorithm(mySettings.getSymmetricAlgorithm());
        key.setStatus(Status.ACTIVE);
        key.setExpires(Utilities.newDate());
        key.setGracePeriod(3);
        persistAndDetach(key);
        assertNotNull(key.getId());

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNotNull(found);
        Assertions.assertEquals(key.getAlgorithm(), found.getAlgorithm());
        Assertions.assertEquals(key.getStatus(), found.getStatus());
        assertEquals(key.getExpires().toEpochSecond(ZoneOffset.UTC), found.getExpires().toEpochSecond(ZoneOffset.UTC));
        assertEquals(key.getGracePeriod(), found.getGracePeriod());

        found.setStatus(Status.DEPRECATED);
        save(found);

        final KeyEntity updated = find(KeyEntity.class, key.getId());
        Assertions.assertNotEquals(key.getStatus(), updated.getStatus());
    }

    @Test
    void testUpdateExpires() {
        final KeyEntity key = prepareKey();

        // Now to the actual test, change the Expires, persist, detach
        // and find the Entity again. Expected is no errors, but the
        // value is the same.
        final LocalDateTime expires = Utilities.newDate();
        key.setExpires(expires);
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNull(toString(found.getExpires()));
    }

    @Test
    void testUpdateGracePeriod() {
        final KeyEntity key = prepareKey();

        // Now to the actual test, change the GracePeriod, persist, detach
        // and find the Entity again. Expected is no errors, but the value
        // is the same.
        key.setGracePeriod(3);
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNull(found.getGracePeriod());
    }
}
