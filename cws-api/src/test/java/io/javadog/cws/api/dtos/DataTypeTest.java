/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataTypeTest {

    @Test
    public void testClass() {
        final String name = "name";
        final String type = "type";

        final DataType dataType = new DataType();
        dataType.setName(name);
        dataType.setType(type);

        assertThat(dataType.getName(), is(name));
        assertThat(dataType.getType(), is(type));
    }

    @Test
    public void testStandardMethods() {
        final DataType dataType = new DataType();
        final DataType sameDataType = new DataType();
        final DataType emptyDataType = new DataType();

        dataType.setName("DataType Name");
        dataType.setType("DataType Type");
        sameDataType.setName(dataType.getName());
        sameDataType.setType(dataType.getType());

        assertThat(dataType.equals(null), is(false));
        assertThat(dataType.equals(dataType), is(true));
        assertThat(dataType.equals(sameDataType), is(true));
        assertThat(dataType.equals(emptyDataType), is(false));

        assertThat(dataType.hashCode(), is(sameDataType.hashCode()));
        assertThat(dataType.hashCode(), is(not(emptyDataType.hashCode())));

        assertThat(dataType.toString(), is(sameDataType.toString()));
        assertThat(dataType.toString(), is(not(emptyDataType.toString())));
    }
}
