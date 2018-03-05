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
public final class ProcessTrusteeResponseTest {

    @Test
    public void testClassflow() {
        final ProcessTrusteeResponse response = new ProcessTrusteeResponse();

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
    }

    @Test
    public void testError() {
        final String msg = "ProcessTrustee Request failed due to Verification Problems.";
        final ProcessTrusteeResponse response = new ProcessTrusteeResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
    }
}
