/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.UUID;

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
