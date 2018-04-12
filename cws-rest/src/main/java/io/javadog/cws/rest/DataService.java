/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
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
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path("/data")
public class DataService {

    private static final Logger log = Logger.getLogger(DataService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ShareBean bean;

    @POST
    @Path("/addData")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        return processData(addDataRequest, Action.ADD, "addData");
    }

    @POST
    @Path("/updateData")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        return processData(updateDataRequest, Action.UPDATE, "updateData");
    }

    @POST
    @DELETE
    @Path("/deleteData")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        return processData(deleteDataRequest, Action.DELETE, "deleteData");
    }

    @POST
    @Path("/fetchData")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        final Long startTime = System.nanoTime();
        FetchDataResponse response;

        try {
            response = bean.fetchData(fetchDataRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchData", startTime, e));
            response = new FetchDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processData(final ProcessDataRequest request, final Action action, final String logAction) {
        final Long startTime = System.nanoTime();
        ProcessDataResponse response;

        try {
            request.setAction(action);
            response = bean.processData(request);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), logAction, startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), logAction, startTime, e));
            response = new ProcessDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
