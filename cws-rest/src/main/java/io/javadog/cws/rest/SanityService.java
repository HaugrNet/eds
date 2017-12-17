/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.core.SystemBean;
import io.javadog.cws.core.misc.StringUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
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
@Path("/sanity")
@Consumes(MediaType.APPLICATION_JSON)
public class SanityService {

    private static final Logger log = Logger.getLogger(SanityService.class.getName());

    @Inject private SystemBean bean;

    @POST
    @Path("/sanitized")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sanitized(final SanityRequest sanitizedRequest) {
        SanityResponse sanitizedResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            sanitizedResponse = bean.sanity(sanitizedRequest);
            returnCode = sanitizedResponse.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("sanitized", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(sanitizedResponse).build();
    }
}
