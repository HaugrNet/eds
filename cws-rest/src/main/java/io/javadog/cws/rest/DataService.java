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
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.misc.StringUtil;
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

    @Inject private ShareBean bean;

    @POST
    @Path("/addData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(@NotNull final ProcessDataRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessDataResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.ADD);
            response = bean.processData(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("addData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/updateData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@NotNull final ProcessDataRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessDataResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.UPDATE);
            response = bean.processData(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("updateData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @DELETE
    @Path("/deleteData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@NotNull final ProcessDataRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        ProcessDataResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            request.setAction(Action.DELETE);
            response = bean.processData(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("deleteData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }

    @POST
    @Path("/fetchData")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(@NotNull final FetchDataRequest request) {
        ReturnCode returnCode = ReturnCode.ERROR;
        FetchDataResponse response = null;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchData(request);
            returnCode = response.getReturnCode();
            log.log(Settings.INFO, () -> StringUtil.durationSince("fetchData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(response).build();
    }
}
