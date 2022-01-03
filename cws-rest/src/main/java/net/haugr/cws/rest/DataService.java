/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.rest;

import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.FetchDataRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.responses.FetchDataResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.core.ShareBean;
import net.haugr.cws.core.misc.LoggingUtil;
import net.haugr.cws.core.model.Settings;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * <p>REST interface for the Data functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_DATA_BASE)
public class DataService {

    private static final Logger LOG = Logger.getLogger(DataService.class.getName());

    @Inject
    private ShareBean bean;
    private final Settings settings = Settings.getInstance();

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
    @Path(Constants.REST_DATA_COPY)
    public Response copy(@NotNull final ProcessDataRequest updateDataRequest) {
        return processData(updateDataRequest, Action.COPY, Constants.REST_DATA_COPY);
    }

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    @Path(Constants.REST_DATA_MOVE)
    public Response move(@NotNull final ProcessDataRequest updateDataRequest) {
        return processData(updateDataRequest, Action.MOVE, Constants.REST_DATA_MOVE);
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
