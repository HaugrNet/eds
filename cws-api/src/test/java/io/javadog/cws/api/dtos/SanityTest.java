/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.javadog.cws.api.common.Utilities;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
final class SanityTest {

    @Test
    void testClassflow() {
        final String dataId = UUID.randomUUID().toString();
        final Date changed = new Date();

        final Sanity sanity = new Sanity();
        sanity.setDataId(dataId);
        sanity.setChanged(changed);

        assertEquals(dataId, sanity.getDataId());
        assertEquals(changed, sanity.getChanged());
    }

    @Test
    void testStandardMethods() {
        final Sanity sanity = new Sanity();
        final Sanity sameSanity = new Sanity();
        final Sanity emptySanity = new Sanity();

        sanity.setDataId(UUID.randomUUID().toString());
        sanity.setChanged(Utilities.newDate());
        sameSanity.setDataId(sanity.getDataId());
        sameSanity.setChanged(sanity.getChanged());

        assertEquals(sameSanity.toString(), sanity.toString());
        assertNotEquals(emptySanity.toString(), sanity.toString());
    }
}
