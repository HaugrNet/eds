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
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberResponseTest {

    @Test
    public void testClassflow() {
        final List<Circle> circles = new ArrayList(1);
        circles.add(new Circle());
        final List<Member> members = new ArrayList<>(1);
        members.add(new Member());

        final FetchMemberResponse response = new FetchMemberResponse();
        response.setCircles(circles);
        response.setMembers(members);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getMembers(), is(members));
        assertThat(response.getCircles(), is(circles));
    }

    @Test
    public void testError() {
        final String msg = "FetchMember Request failed due to Verification Problems.";
        final FetchMemberResponse response = new FetchMemberResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getMembers().isEmpty(), is(true));
        assertThat(response.getCircles().isEmpty(), is(true));
    }
}
