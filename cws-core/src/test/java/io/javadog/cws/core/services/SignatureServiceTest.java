/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.model.DatabaseSetup;
import org.junit.Test;

/**
 * This Test Class, is testing the following Service Classes in one, as they are
 * all fairly small but also connected.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignatureServiceTest extends DatabaseSetup {

    @Test
    public void testSignVerifyFetch() {
        final byte[] data = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890".getBytes(settings.getCharset());
        final FetchSignatureService fetchService = new FetchSignatureService(settings, entityManager);
        final VerifyService verifyService = new VerifyService(settings, entityManager);
        final SignService signService = new SignService(settings, entityManager);

        final SignRequest signRequest = prepareRequest(SignRequest.class, "member1");
        signRequest.setData(data);
        final SignResponse signResponse = signService.perform(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, "member2");
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        verifyRequest.setSignatureId(signResponse.getSignatureId());
        final VerifyResponse verifyResponse = verifyService.perform(verifyRequest);
        assertThat(verifyResponse.getReturnCode(), is(ReturnCode.SUCCESS));

        final FetchSignatureRequest fetchRequest = prepareRequest(FetchSignatureRequest.class, "member1");
        final FetchSignatureResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS));
        assertThat(fetchResponse.getSignatures().size(), is(1));
        assertThat(fetchResponse.getSignatures().get(0).getVerifications(), is(1L));
    }
}
