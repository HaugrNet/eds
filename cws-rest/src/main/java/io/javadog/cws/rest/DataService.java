/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
@Path("/data")
@Consumes(MediaType.APPLICATION_JSON)
public class DataService {

    private static final Logger log = Logger.getLogger(DataService.class.getName());

    @Inject private SettingBean settings;
    @Inject private ShareBean bean;

    @POST
    @Path("/addData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        ProcessDataResponse addDataResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            addDataRequest.setAction(Action.ADD);
            addDataResponse = bean.processData(addDataRequest);
            returnCode = addDataResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "addData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(addDataResponse).build();
    }

    @POST
    @Path("/updateData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        ProcessDataResponse updateDataResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            updateDataRequest.setAction(Action.UPDATE);
            updateDataResponse = bean.processData(updateDataRequest);
            returnCode = updateDataResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(updateDataResponse).build();
    }

    @POST
    @DELETE
    @Path("/deleteData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        ProcessDataResponse deleteDataResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            deleteDataRequest.setAction(Action.DELETE);
            deleteDataResponse = bean.processData(deleteDataRequest);
            returnCode = deleteDataResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(deleteDataResponse).build();
    }

    @POST
    @Path("/fetchData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        FetchDataResponse fetchDataResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            fetchDataResponse = bean.fetchData(fetchDataRequest);
            returnCode = fetchDataResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(fetchDataResponse).build();
    }
}
