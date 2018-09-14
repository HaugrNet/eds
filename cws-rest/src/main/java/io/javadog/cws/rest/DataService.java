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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * <p>REST interface for the Data functionality.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path(Constants.REST_DATA_BASE)
public class DataService {

    private static final Logger LOG = Logger.getLogger(DataService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ShareBean bean;

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATA_ADD)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        return processData(addDataRequest, Action.ADD, Constants.REST_DATA_ADD);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATA_UPDATE)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        return processData(updateDataRequest, Action.UPDATE, Constants.REST_DATA_UPDATE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATA_DELETE)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        return processData(deleteDataRequest, Action.DELETE, Constants.REST_DATA_DELETE);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATA_FETCH)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        final String restAction = Constants.REST_DATA_BASE + Constants.REST_DATA_FETCH;
        final long startTime = System.nanoTime();
        FetchDataResponse response;

        try {
            response = bean.fetchData(fetchDataRequest);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new FetchDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processData(final ProcessDataRequest request, final Action action, final String logAction) {
        final String restAction = Constants.REST_DATA_BASE + logAction;
        final long startTime = System.nanoTime();
        ProcessDataResponse response;

        try {
            request.setAction(action);
            response = bean.processData(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new ProcessDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
