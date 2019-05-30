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
package io.javadog.cws.core.model.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.model.Settings;
import java.util.Date;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class KeyEntityTest extends DatabaseSetup {

    @Test
    public void testEntity() {
        final Settings mySettings = newSettings();
        final KeyEntity key = new KeyEntity();
        key.setAlgorithm(mySettings.getSymmetricAlgorithm());
        key.setStatus(Status.ACTIVE);
        key.setExpires(new Date());
        key.setGracePeriod(3);
        persistAndDetach(key);
        assertNotNull(key.getId());

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNotNull(found);
        assertEquals(key.getAlgorithm(), found.getAlgorithm());
        assertEquals(key.getStatus(), found.getStatus());
        assertEquals(toString(key.getExpires()), toString(found.getExpires()));
        assertEquals(key.getGracePeriod(), found.getGracePeriod());

        found.setStatus(Status.DEPRECATED);
        persist(found);

        final KeyEntity updated = find(KeyEntity.class, key.getId());
        assertNotEquals(key.getStatus(), updated.getStatus());
    }

    @Test
    public void testUpdateExpires() {
        final KeyEntity key = prepareKey();

        // Now to the actual test, change the Expires, persist, detach and
        // find the Entity again. Expected is no errors but the value is same.
        final Date expires = new Date();
        key.setExpires(expires);
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNull(toString(found.getExpires()));
    }

    @Test
    public void testUpdateGracePeriod() {
        final KeyEntity key = prepareKey();

        // Now to the actual test, change the GracePeriod, persist, detach and
        // find the Entity again. Expected is no errors but the value is same.
        key.setGracePeriod(3);
        persistAndDetach(key);

        final KeyEntity found = find(KeyEntity.class, key.getId());
        assertNull(found.getGracePeriod());
    }
}
