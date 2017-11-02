/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchDataTypeResponseTest {

    @Test
    public void testFetchingAllCircles() {
        final List<DataType> types = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            types.add(new DataType());
        }

        final FetchDataTypeResponse response = new FetchDataTypeResponse();
        response.setDataTypes(types);

        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getDataTypes().size(), is(3));
    }

    @Test
    public void testFetchingWithError() {
        final FetchDataTypeResponse response = new FetchDataTypeResponse(ReturnCode.CONSTRAINT_ERROR, "Bollocks.");

        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.CONSTRAINT_ERROR));
        assertThat(response.getReturnMessage(), is("Bollocks."));
        assertThat(response.getDataTypes().isEmpty(), is(true));
    }
}
