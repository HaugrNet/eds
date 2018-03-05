/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
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
    public void testClassflow() {
        final List<DataType> dataTypes = new ArrayList<>(3);
        dataTypes.add(new DataType());
        dataTypes.add(new DataType());
        dataTypes.add(new DataType());

        final FetchDataTypeResponse response = new FetchDataTypeResponse();
        response.setDataTypes(dataTypes);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getDataTypes(), is(dataTypes));
    }

    @Test
    public void testError() {
        final String msg = "FetchDataType Request failed due to Verification Problems.";
        final FetchDataTypeResponse response = new FetchDataTypeResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getDataTypes().isEmpty(), is(true));
    }
}
