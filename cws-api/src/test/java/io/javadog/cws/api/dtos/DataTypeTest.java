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

import java.util.UUID;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataTypeTest {

    @Test
    public void testClassflow() {
        final String name = "name";
        final String type = "type";

        final DataType dataType = new DataType();
        dataType.setTypeName(name);
        dataType.setType(type);

        assertThat(dataType.getTypeName(), is(name));
        assertThat(dataType.getType(), is(type));
    }

    @Test
    public void testToString() {
        final DataType dataType = new DataType();
        final DataType sameDataType = new DataType();
        final DataType emptyDataType = new DataType();

        dataType.setTypeName(UUID.randomUUID().toString());
        dataType.setType(UUID.randomUUID().toString());
        sameDataType.setTypeName(dataType.getTypeName());
        sameDataType.setType(dataType.getType());

        assertThat(dataType.toString(), is(sameDataType.toString()));
        assertThat(dataType.toString(), is(not(emptyDataType.toString())));
    }
}
