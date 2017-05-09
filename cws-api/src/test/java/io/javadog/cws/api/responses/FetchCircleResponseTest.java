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
import io.javadog.cws.api.dtos.Trustee;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCircleResponseTest {

    @Test
    public void testFetchingAllCircles() {
        final FetchCircleResponse response = new FetchCircleResponse();
        response.setCircles(prepareCircles(3));

        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(3));
        assertThat(response.getTrustees().isEmpty(), is(true));
    }

    @Test
    public void testFetchingSpecificCircle() {
        final FetchCircleResponse response = new FetchCircleResponse();
        response.setCircles(prepareCircles(1));
        response.setTrustees(prepareTrustees());

        assertThat(response.isOk(), is(true));
        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.getCircles().size(), is(1));
        assertThat(response.getTrustees().size(), is(3));
    }

    @Test
    public void testFetchingWithError() {
        final FetchCircleResponse response = new FetchCircleResponse(ReturnCode.CONSTRAINT_ERROR, "Bollocks.");

        assertThat(response.isOk(), is(false));
        assertThat(response.getReturnCode(), is(ReturnCode.CONSTRAINT_ERROR));
        assertThat(response.getReturnMessage(), is("Bollocks."));
        assertThat(response.getCircles().isEmpty(), is(true));
        assertThat(response.getTrustees().isEmpty(), is(true));
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private static List<Circle> prepareCircles(final int amount) {
        final List<Circle> circles = new ArrayList<>(amount);

        for (int i = 0; i < amount; i++) {
            circles.add(new Circle());
        }

        return circles;
    }

    private static List<Trustee> prepareTrustees() {
        final List<Trustee> trustees = new ArrayList<>(3);

        for (int i = 0; i < 3; i++) {
            trustees.add(new Trustee());
        }

        return trustees;
    }
}
