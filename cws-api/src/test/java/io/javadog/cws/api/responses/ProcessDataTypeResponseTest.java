/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataTypeResponseTest {

    @Test
    public void testClassflow() {
        final DataType dataType = new DataType();
        dataType.setTypeName("The TypeName");
        dataType.setType("The Type");

        final ProcessDataTypeResponse response = new ProcessDataTypeResponse();
        response.setDataType(dataType);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getDataType(), is(dataType));
    }

    @Test
    public void testError() {
        final String msg = "ProcessDataType Request failed due to Verification Problems.";
        final ProcessDataTypeResponse response = new ProcessDataTypeResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getDataType(), is(nullValue()));
    }
}
