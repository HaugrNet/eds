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

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class DataTypeTest {

    @Test
    void testClassFlow() {
        final String name = "name";
        final String type = "type";

        final DataType dataType = new DataType();
        dataType.setTypeName(name);
        dataType.setType(type);

        assertEquals(name, dataType.getTypeName());
        assertEquals(type, dataType.getType());
    }

    @Test
    void testToString() {
        final DataType dataType = new DataType();
        final DataType sameDataType = new DataType();
        final DataType emptyDataType = new DataType();

        dataType.setTypeName(UUID.randomUUID().toString());
        dataType.setType(UUID.randomUUID().toString());
        sameDataType.setTypeName(dataType.getTypeName());
        sameDataType.setType(dataType.getType());

        assertEquals(sameDataType.toString(), dataType.toString());
        assertNotEquals(emptyDataType.toString(), dataType.toString());
    }
}
