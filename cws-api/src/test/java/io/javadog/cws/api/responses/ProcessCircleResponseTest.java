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
 * @since CWS 1.0
 */
public class ProcessCircleResponseTest {

    @Test
    public void testClassflow() {
        final String circleId = UUID.randomUUID().toString();

        final ProcessCircleResponse response = new ProcessCircleResponse();
        response.setCircleId(circleId);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getCircleId(), is(circleId));
    }

    @Test
    public void testError() {
        final String msg = "ProcessCircle Request failed due to Verification Problems.";
        final ProcessCircleResponse response = new ProcessCircleResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getCircleId(), is(nullValue()));
    }
}
