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
import org.junit.Test;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataResponseTest {

    @Test
    public void testClassflow() {
        final String dataId = UUID.randomUUID().toString();

        final ProcessDataResponse response = new ProcessDataResponse();
        response.setDataId(dataId);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getDataId(), is(dataId));
    }

    @Test
    public void testError() {
        final String msg = "ProcessData Request failed due to Verification Problems.";
        final ProcessDataResponse response = new ProcessDataResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getDataId(), is(nullValue()));
    }
}
