/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.misc.StringUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path("/signatures")
@Consumes(MediaType.APPLICATION_JSON)
public class SignatureService {

    private static final Logger log = Logger.getLogger(SignatureService.class.getName());

    @Inject private ShareBean bean;

    @POST
    @Path("/signDocument")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sign(@NotNull final SignRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        SignResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            response = bean.sign(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("signDocument", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/verifySignature")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verify(@NotNull final VerifyRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        VerifyResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            response = bean.verify(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("verifySignature", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/fetchSignatures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@NotNull final FetchSignatureRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        FetchSignatureResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchSignatures(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("fetchSignatures", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }
}
