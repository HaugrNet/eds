/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZoneId;
import java.time.ZoneOffset;
import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.core.setup.DatabaseSetup;
import net.haugr.cws.core.enums.Status;
import net.haugr.cws.core.model.Settings;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
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
        assertEquals(key.getAlgorithm(), found.getAlgorithm());
        assertEquals(key.getStatus(), found.getStatus());
        assertEquals(key.getExpires().toEpochSecond(ZoneOffset.UTC), found.getExpires().toEpochSecond(ZoneOffset.UTC));
        assertEquals(key.getGracePeriod(), found.getGracePeriod());

        found.setStatus(Status.DEPRECATED);
        save(found);

        final KeyEntity updated = find(KeyEntity.class, key.getId());
        assertNotEquals(key.getStatus(), updated.getStatus());
    }

    @Test
    void testUpdateExpires() {
        final KeyEntity key = prepareKey();

        // Now to the actual test, change the Expires, persist, detach and
        // find the Entity again. Expected is no errors but the value is same.
        final LocalDateTime expires = Utilities.newDate();
        key.setExpires(expires);
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNull(toString(found.getExpires()));
    }

    @Test
    void testUpdateGracePeriod() {
        final KeyEntity key = prepareKey();

        // Now to the actual test, change the GracePeriod, persist, detach and
        // find the Entity again. Expected is no errors but the value is same.
        key.setGracePeriod(3);
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNull(found.getGracePeriod());
    }
}
