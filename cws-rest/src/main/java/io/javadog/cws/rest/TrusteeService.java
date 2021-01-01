/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * <p>REST interface for the Trustee functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_TRUSTEES_BASE)
public class TrusteeService {

    private static final Logger LOG = Logger.getLogger(TrusteeService.class.getName());

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    @POST
    @Path(Constants.REST_TRUSTEES_ADD)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response add(@NotNull final ProcessTrusteeRequest addTrusteeRequest) {
        return processTrustee(addTrusteeRequest, Action.ADD, Constants.REST_TRUSTEES_ADD);
    }

    @POST
    @Path(Constants.REST_TRUSTEES_ALTER)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response alter(@NotNull final ProcessTrusteeRequest alterTrusteeRequest) {
        return processTrustee(alterTrusteeRequest, Action.ALTER, Constants.REST_TRUSTEES_ALTER);
    }

    @POST
    @Path(Constants.REST_TRUSTEES_REMOVE)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response remove(@NotNull final ProcessTrusteeRequest removeTrusteeRequest) {
        return processTrustee(removeTrusteeRequest, Action.REMOVE, Constants.REST_TRUSTEES_REMOVE);
    }

    @POST
    @Path(Constants.REST_TRUSTEES_FETCH)
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response fetch(@NotNull final FetchTrusteeRequest fetchTrusteeRequest) {
        final String restAction = Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_FETCH;
        final long startTime = System.nanoTime();
        FetchTrusteeResponse response;

        try {
            response = bean.fetchTrustees(fetchTrusteeRequest);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new FetchTrusteeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }

    private Response processTrustee(final ProcessTrusteeRequest request, final Action action, final String logAction) {
        final String restAction = Constants.REST_TRUSTEES_BASE + logAction;
        final long startTime = System.nanoTime();
        ProcessTrusteeResponse response;

        try {
            request.setAction(action);
            response = bean.processTrustee(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), restAction, startTime, e));
            response = new ProcessTrusteeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
