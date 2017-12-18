/*
 * =====================================================================================================================
 * Copyright (c) 2010-2017, secunet Security Networks AG, Germany
 * ---------------------------------------------------------------------------------------------------------------------
 * Project: DÜbEL (duebel-ws160)
 * =====================================================================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.SystemBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
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
@Path("/version")
public class VersionService {

    private static final Logger log = Logger.getLogger(VersionService.class.getName());

    @Inject private SettingBean settings;
    @Inject private SystemBean bean;

    @GET
    @POST
    @Produces(MediaType.APPLICATION_XML)
    public Response version() {
        VersionResponse versionResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            versionResponse = bean.version();
            returnCode = versionResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "version", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(versionResponse).build();
    }
}
