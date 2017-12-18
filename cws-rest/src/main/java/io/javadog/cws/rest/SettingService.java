/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.SystemBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
@Path("/settings")
@Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
public class SettingService {

    private static final Logger log = Logger.getLogger(SettingService.class.getName());

    @Inject private SettingBean settings;
    @Inject private SystemBean bean;

    @GET
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response settings(@NotNull final SettingRequest settingRequest) {
        SettingResponse settingResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            settingResponse = bean.settings(settingRequest);
            returnCode = settingResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "settings", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(settingResponse).build();
    }
}
