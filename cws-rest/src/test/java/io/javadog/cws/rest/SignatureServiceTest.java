/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SignatureServiceTest extends DatabaseSetup {

    @Test
    public void testSign() {
        final SignatureService service = prepareService();
        final SignRequest request = new SignRequest();

        final Response response = service.sign(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedSign() {
        final SignatureService service = prepareFlawedService();
        final SignRequest request = new SignRequest();

        final Response response = service.sign(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testVerify() {
        final SignatureService service = prepareService();
        final VerifyRequest request = new VerifyRequest();

        final Response response = service.verify(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedVerify() {
        final SignatureService service = prepareFlawedService();
        final VerifyRequest request = new VerifyRequest();

        final Response response = service.verify(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testFetchSignatures() {
        final SignatureService service = prepareService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedFetchSignatures() {
        final SignatureService service = prepareFlawedService();
        final FetchSignatureRequest request = new FetchSignatureRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static SignatureService prepareFlawedService() {
        try {

            final SignatureService service = SignatureService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SignatureService prepareService() {
        try {
            final ShareBean bean = ShareBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final SignatureService service = SignatureService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }
}
