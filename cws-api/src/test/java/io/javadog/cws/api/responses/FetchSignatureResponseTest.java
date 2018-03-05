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
import io.javadog.cws.api.dtos.Signature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchSignatureResponseTest {

    @Test
    public void testClassflow() {
        final List<Signature> signatures = new ArrayList<>(1);
        signatures.add(new Signature());

        final FetchSignatureResponse response = new FetchSignatureResponse();
        response.setSignatures(signatures);

        assertThat(response.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(response.getReturnMessage(), is("Ok"));
        assertThat(response.isOk(), is(true));
        assertThat(response.getSignatures(), is(signatures));
    }

    @Test
    public void testError() {
        final String msg = "FetchSignature Request failed due to Verification Problems.";
        final FetchSignatureResponse response = new FetchSignatureResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertThat(response.getReturnCode(), is(ReturnCode.VERIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is(msg));
        assertThat(response.isOk(), is(false));
        assertThat(response.getSignatures().isEmpty(), is(true));
    }
}
