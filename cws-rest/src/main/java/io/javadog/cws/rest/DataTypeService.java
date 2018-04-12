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
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
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
@Path("/dataTypes")
public class DataTypeService {

    private static final Logger log = Logger.getLogger(DataTypeService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ShareBean bean;

    @POST
    @Path("/processDataType")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response process(@NotNull final ProcessDataTypeRequest processDataTypeRequest) {
        return processDataType(processDataTypeRequest, Action.PROCESS, "processDataType");
    }

    @POST
    @DELETE
    @Path("/deleteDataType")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response delete(@NotNull final ProcessDataTypeRequest deleteDataTypeRequest) {
        return processDataType(deleteDataTypeRequest, Action.DELETE, "deleteDataType");
    }

    @POST
    @Path("/fetchDataTypes")
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response fetch(@NotNull final FetchDataTypeRequest fetchDataTypesRequest) {
        final Long startTime = System.nanoTime();
        FetchDataTypeResponse response;

        try {
            response = bean.fetchDataTypes(fetchDataTypesRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchDataTypes", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchDataTypes", startTime, e));
            response = new FetchDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processDataType(final ProcessDataTypeRequest request, final Action action, final String logAction) {
        final Long startTime = System.nanoTime();
        ProcessDataTypeResponse response;

        try {
            request.setAction(action);
            response = bean.processDataType(request);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), logAction, startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), logAction, startTime, e));
            response = new ProcessDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
