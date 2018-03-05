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
import io.javadog.cws.api.dtos.Trustee;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchTrusteeResponseTest {

    @Test
    public void testClassflow() {
        final List<Trustee> trustees = new ArrayList();

        final FetchTrusteeResponse response = new FetchTrusteeResponse();
        response.setTrustees(trustees);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getTrustees(), is(trustees));
    }

    @Test
    public void testError() {
        final String msg = "FetchCircle Request failed due to Verification Problems.";
        final FetchTrusteeResponse response = new FetchTrusteeResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getTrustees().isEmpty(), is(true));
    }
}
