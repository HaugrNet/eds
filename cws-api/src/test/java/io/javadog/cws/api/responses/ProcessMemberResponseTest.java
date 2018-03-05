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

import io.javadog.cws.api.TestUtilities;
import io.javadog.cws.api.common.ReturnCode;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberResponseTest {

    @Test
    public void testClassflow() {
        final String memberId = UUID.randomUUID().toString();
        final String signature = "Invitation Signature";

        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setMemberId(memberId);
        response.setSignature(TestUtilities.convert(signature));

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getMemberId(), is(memberId));
        assertThat(TestUtilities.convert(response.getSignature()), is(signature));
    }

    @Test
    public void testError() {
        final String msg = "ProcessMember Request failed due to Verification Problems.";
        final ProcessMemberResponse response = new ProcessMemberResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getMemberId(), is(nullValue()));
        assertThat(response.getSignature(), is(nullValue()));
    }
}
