/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.requests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.Map;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleRequestTest {

    @Test
    public void testEmptyObject() {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        final Map<String, String> errors = request.validate();
        assertThat(errors.size(), is(3));
    }
}
