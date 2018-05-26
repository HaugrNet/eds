/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import static io.javadog.cws.api.common.Constants.REST_SIGNATURES_BASE;
import static io.javadog.cws.api.common.Constants.REST_SIGNATURES_FETCH;
import static io.javadog.cws.api.common.Constants.REST_SIGNATURES_SIGN;
import static io.javadog.cws.api.common.Constants.REST_SIGNATURES_VERIFY;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path(REST_SIGNATURES_BASE)
public class SignatureService {

    private static final Logger log = Logger.getLogger(SignatureService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ShareBean bean;

    @POST
    @Path(REST_SIGNATURES_SIGN)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response sign(@NotNull final SignRequest signDocumentRequest) {
        final String restAction = REST_SIGNATURES_BASE + REST_SIGNATURES_SIGN;
        final Long startTime = System.nanoTime();
        SignResponse response;

        try {
            response = bean.sign(signDocumentRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new SignResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    @POST
    @Path(REST_SIGNATURES_VERIFY)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response verify(@NotNull final VerifyRequest verifySignatureRequest) {
        final String restAction = REST_SIGNATURES_BASE + REST_SIGNATURES_VERIFY;
        final Long startTime = System.nanoTime();
        VerifyResponse response;

        try {
            response = bean.verify(verifySignatureRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new VerifyResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    @POST
    @Path(REST_SIGNATURES_FETCH)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response fetch(@NotNull final FetchSignatureRequest fetchSignaturesRequest) {
        final String restAction = REST_SIGNATURES_BASE + REST_SIGNATURES_FETCH;
        final Long startTime = System.nanoTime();
        FetchSignatureResponse response;

        try {
            response = bean.fetchSignatures(fetchSignaturesRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new FetchSignatureResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
