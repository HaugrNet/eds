/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
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
import io.javadog.cws.core.DatabaseSetup;
import org.junit.Test;

import java.util.Date;

/**
 * <p>This Test Class, is testing the following Service Classes in one, as they
 * are all fairly small but also connected.</p>
 *
 * <ul>
 *   <li>SignService</li>
 *   <li>VerifyService</li>
 *   <li>FetchSignatureService</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignatureServiceTest extends DatabaseSetup {

    @Test
    public void testSignVerifyFetch() {
        final byte[] data = generateData(1048576);
        final FetchSignatureService fetchService = new FetchSignatureService(settings, entityManager);
        final VerifyService verifyService = new VerifyService(settings, entityManager);
        final SignService signService = new SignService(settings, entityManager);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        final SignResponse signResponse = signService.perform(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = verifyService.perform(verifyRequest);
        assertThat(verifyResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(verifyResponse.isVerified(), is(true));

        final FetchSignatureRequest fetchRequest = prepareRequest(FetchSignatureRequest.class, MEMBER_1);
        final FetchSignatureResponse fetchResponse = fetchService.perform(fetchRequest);
        assertThat(fetchResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(fetchResponse.getSignatures().size(), is(1));
        assertThat(fetchResponse.getSignatures().get(0).getVerifications(), is(1L));
    }

    @Test
    public void testSignatureExpiringIn5Minutes() {
        final byte[] data = generateData(1048576);
        final VerifyService verifyService = new VerifyService(settings, entityManager);
        final SignService signService = new SignService(settings, entityManager);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        // Let our new Signature expire in 5 minutes
        signRequest.setExpires(new Date(new Date().getTime() + 300000L));
        final SignResponse signResponse = signService.perform(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = verifyService.perform(verifyRequest);
        assertThat(verifyResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(verifyResponse.getReturnMessage(), is("Ok"));
        assertThat(verifyResponse.isVerified(), is(true));
    }

    @Test
    public void testDuplicateSigning() {
        final byte[] data = generateData(1048576);
        final SignService signService = new SignService(settings, entityManager);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        // Let our new Signature expire in 5 minutes
        signRequest.setExpires(new Date(new Date().getTime() + 300000L));
        final SignResponse signResponse = signService.perform(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        signRequest.setCredential(crypto.stringToBytes(MEMBER_1));
        final SignResponse duplicateResponse = signService.perform(signRequest);
        assertThat(duplicateResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(duplicateResponse.getReturnMessage(), is("This document has already been signed."));
    }

    @Test
    public void testExpiredSignature() {
        final byte[] data = generateData(1048576);
        final VerifyService verifyService = new VerifyService(settings, entityManager);
        final SignService signService = new SignService(settings, entityManager);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        signRequest.setExpires(new Date());
        final SignResponse signResponse = signService.perform(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        verifyRequest.setData(data);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = verifyService.perform(verifyRequest);
        assertThat(verifyResponse.getReturnCode(), is(ReturnCode.SIGNATURE_WARNING.getCode()));
        assertThat(verifyResponse.getReturnMessage(), is("The Signature has expired."));
        assertThat(verifyResponse.isVerified(), is(false));
    }

    @Test
    public void testCorrectSignatureWrongData() {
        final byte[] data = generateData(1048576);
        final VerifyService verifyService = new VerifyService(settings, entityManager);
        final SignService signService = new SignService(settings, entityManager);

        final SignRequest signRequest = prepareRequest(SignRequest.class, MEMBER_1);
        signRequest.setData(data);
        final SignResponse signResponse = signService.perform(signRequest);
        assertThat(signResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));

        final VerifyRequest verifyRequest = prepareRequest(VerifyRequest.class, MEMBER_2);
        final byte[] wrongData = generateData(524288);
        verifyRequest.setData(wrongData);
        verifyRequest.setSignature(signResponse.getSignature());
        final VerifyResponse verifyResponse = verifyService.perform(verifyRequest);
        assertThat(verifyResponse.getReturnCode(), is(ReturnCode.SUCCESS.getCode()));
        assertThat(verifyResponse.isVerified(), is(false));
    }

    @Test
    public void testInvalidSignature() {
        final byte[] data = generateData(524288);
        final String signature = "VYU8uIyr54AZGeM4aUilgEItfI39/b4YpFcry8ByJYVuJoI0gxNLiw9CMCaocOfXyGkmQJuI4KvL1lhNN6jnsY51OYxsxcJKUBgMnGMRdp9mr+ryiduotTYeD9Z+IyWXdUlQ9W3N/TX1uqLwVCh9qrngXtnOXx5rnZrWybQPsoLEnVOXvkL94Et0EcIUe6spRbaR+8I4oCtGToLcMCZdD32z6suhIfqz9UFiU10W01T2ebNV4SuTf56RSZ1vyWix6C8GJhwLqWE697femoqBWh1UYgKsi5x6d1SYC7ZWSxVj61PpPJ3MfzAxVc5rqJDk1og3zfciDWPMJmF4aJ60Sg==";
        final VerifyService verifyService = new VerifyService(settings, entityManager);
        final VerifyRequest request = prepareRequest(VerifyRequest.class, MEMBER_1);
        request.setData(data);
        request.setSignature(signature);

        final VerifyResponse response = verifyService.perform(request);
        assertThat(response.getReturnCode(), is(ReturnCode.IDENTIFICATION_WARNING.getCode()));
        assertThat(response.getReturnMessage(), is("It was not possible to find the Signature."));
        assertThat(response.isVerified(), is(false));
    }
}
