/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * <p>REST interface for the DataType functionality.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path(Constants.REST_DATATYPES_BASE)
public class DataTypeService {

    private static final Logger LOG = Logger.getLogger(DataTypeService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ShareBean bean;

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATATYPES_PROCESS)
    public Response process(@NotNull final ProcessDataTypeRequest processDataTypeRequest) {
        return processDataType(processDataTypeRequest, Action.PROCESS, Constants.REST_DATATYPES_PROCESS);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATATYPES_DELETE)
    public Response delete(@NotNull final ProcessDataTypeRequest deleteDataTypeRequest) {
        return processDataType(deleteDataTypeRequest, Action.DELETE, Constants.REST_DATATYPES_DELETE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATATYPES_FETCH)
    public Response fetch(@NotNull final FetchDataTypeRequest fetchDataTypesRequest) {
        final String restAction = Constants.REST_DATATYPES_BASE + Constants.REST_DATATYPES_FETCH;
        final long startTime = System.nanoTime();
        FetchDataTypeResponse response;

        try {
            response = bean.fetchDataTypes(fetchDataTypesRequest);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new FetchDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processDataType(final ProcessDataTypeRequest request, final Action action, final String logAction) {
        final String restAction = Constants.REST_DATATYPES_BASE + logAction;
        final long startTime = System.nanoTime();
        ProcessDataTypeResponse response;

        try {
            request.setAction(action);
            response = bean.processDataType(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new ProcessDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
