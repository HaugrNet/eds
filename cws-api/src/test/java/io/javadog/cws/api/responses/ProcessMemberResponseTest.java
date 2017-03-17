/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.responses;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Member;
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
        final Member member = new Member();
        final String armoredKey = UUID.randomUUID().toString();

        final ProcessMemberResponse response = new ProcessMemberResponse();
        response.setId(id);
        response.setMember(member);
        response.setArmoredKey(armoredKey);

        assertThat(response.getId(), is(id));
        assertThat(response.getMember(), is(member));
        assertThat(response.getArmoredKey(), is(armoredKey));
        assertThat(response.getReturnCode(), is(Constants.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
    }

    @Test
    public void testError() {
        final String returnMessage = "Cannot complete processing.";
        final ProcessMemberResponse response = new ProcessMemberResponse(Constants.VERIFICATION_WARNING, returnMessage);

        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(Constants.VERIFICATION_WARNING));
        assertThat(response.getReturnMessage(), is(returnMessage));
    }
}
