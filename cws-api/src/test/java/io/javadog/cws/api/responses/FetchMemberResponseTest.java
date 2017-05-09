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
    public void testFetchingAllMembers() {
        final FetchMemberResponse response = new FetchMemberResponse();
        response.setMembers(prepareMembers(3));

        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(3));
        assertThat(response.getCircles().isEmpty(), is(true));
    }

    @Test
    public void testFetchingSpecificMember() {
        final FetchMemberResponse response = new FetchMemberResponse();
        response.setMembers(prepareMembers(1));
        response.setCircles(prepareCircles());

        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getMembers().size(), is(1));
        assertThat(response.getCircles().size(), is(3));
    }

    @Test
    public void testFetchingWithError() {
        final FetchMemberResponse response = new FetchMemberResponse(ReturnCode.CRYPTO_ERROR, "Blimey.");

        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.CRYPTO_ERROR));
        assertThat(response.getReturnMessage(), is("Blimey."));
        assertThat(response.getMembers().isEmpty(), is(true));
        assertThat(response.getCircles().isEmpty(), is(true));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static List<Member> prepareMembers(final int amount) {
        final List<Member> members = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) {
            members.add(new Member());
        }

        return members;
    }

    private static List<Circle> prepareCircles() {
        final List<Circle> circles = new ArrayList<>(3);

        for (int i = 0; i < 3; i++) {
            circles.add(new Circle());
        }

        return circles;
    }
}
