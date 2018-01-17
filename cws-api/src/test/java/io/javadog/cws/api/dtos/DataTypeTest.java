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
    public void testStandardMethods() {
        final DataType dataType = prepareDataType("DataType Name", "DataType Type");
        final DataType sameDataType = new DataType();
        final DataType emptyDataType = new DataType();

        sameDataType.setTypeName(dataType.getTypeName());
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

    @Test
    public void testEquality() {
        final String typeName1 = "First Type Name";
        final String typeName2 = "Second Type Name";
        final String type1 = "The First Type";
        final String type2 = "The Second Type";
        final DataType dataType1 = prepareDataType(typeName1, type1);
        final DataType dataType2 = prepareDataType(typeName2, type1);
        final DataType dataType3 = prepareDataType(typeName1, type2);

        assertThat(dataType1.equals(dataType2), is(false));
        assertThat(dataType2.equals(dataType1), is(false));
        assertThat(dataType1.equals(dataType3), is(false));
        assertThat(dataType3.equals(dataType1), is(false));
    }

    // =========================================================================
    // Internal Helper Method
    // =========================================================================

    private static DataType prepareDataType(final String typeName, final String type) {
        final DataType dataType = new DataType();
        dataType.setTypeName(typeName);
        dataType.setType(type);

        return dataType;
    }
}
