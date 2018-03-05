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
import io.javadog.cws.api.dtos.Metadata;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchDataResponseTest {

    @Test
    public void testClassflow() {
        final byte[] data = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
        final List<Metadata> metadata = new ArrayList<>(3);
        metadata.add(new Metadata());
        metadata.add(new Metadata());
        metadata.add(new Metadata());

        final FetchDataResponse response = new FetchDataResponse();
        response.setMetadata(metadata);
        response.setRecords(3L);
        response.setData(data);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getMetadata(), is(metadata));
        assertThat(response.getRecords(), is(3L));
        assertThat(response.getData(), is(data));
    }

    @Test
    public void testError() {
        final String msg = "FetchData Request failed due to Verification Problems.";
        final FetchDataResponse response = new FetchDataResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getMetadata().isEmpty(), is(true));
        assertThat(response.getData(), is(nullValue()));
    }
}
