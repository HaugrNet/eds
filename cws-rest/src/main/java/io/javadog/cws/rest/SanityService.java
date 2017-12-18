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
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.SystemBean;
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
@Path("/sanity")
public class SanityService {

    private static final Logger log = Logger.getLogger(SanityService.class.getName());

    @Inject private SettingBean settings;
    @Inject private SystemBean bean;

    @POST
    @Path("/sanitized")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response sanitized(@NotNull final SanityRequest sanitizedRequest) {
        final Long startTime = System.nanoTime();
        SanityResponse response;

        try {
            response = bean.sanity(sanitizedRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "sanitized", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "sanitized", startTime, e));
            response = new SanityResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }
}
