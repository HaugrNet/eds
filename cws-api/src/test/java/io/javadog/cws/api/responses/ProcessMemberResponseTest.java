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
import org.junit.Test;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberResponseTest {

    @Test
    public void testClass() {
        final String id = UUID.randomUUID().toString();

        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setId(id);

        assertThat(response.getId(), is(id));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testError() {
        final String returnMessage = "Cannot complete processing.";
        final ProcessMemberResponse response = new ProcessMemberResponse(ReturnCode.VERIFICATION_WARNING, returnMessage);

        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is(returnMessage));
    }
}
