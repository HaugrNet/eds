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
import org.junit.Test;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CwsResponseTest {

    @Test
    public void testClassflow() {
        final CwsResponse response = new CwsResponse();
        response.setReturnCode(ReturnCode.ERROR);
        response.setReturnMessage(ReturnCode.ERROR.getDescription());

        assertThat(response.getReturnCode(), is(ReturnCode.ERROR.getCode()));
        assertThat(response.getReturnMessage(), is(ReturnCode.ERROR.getDescription()));
        assertThat(response.isOk(), is(false));
    }

    @Test
    public void testError() {
        final String msg = "FetchCircle Request failed due to Verification Problems.";
        final CwsResponse response = new CwsResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
    }
}
