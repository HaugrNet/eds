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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.requests.FetchTrusteeRequest;
import net.haugr.cws.api.requests.ProcessTrusteeRequest;
import net.haugr.cws.core.ManagementBean;
import net.haugr.cws.core.model.Settings;

/**
 * <p>REST interface for the Trustee functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_TRUSTEES_BASE)
public class TrusteeService {

    private static final String PROCESS_METHOD = "processTrustee";
    private static final String FETCH_METHOD = "fetchTrustees";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    @POST
    @Path(Constants.REST_TRUSTEES_ADD)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response add(@NotNull final ProcessTrusteeRequest addTrusteeRequest) {
        addTrusteeRequest.setAction(Action.ADD);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, addTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_ADD);
    }

    @POST
    @Path(Constants.REST_TRUSTEES_ALTER)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response alter(@NotNull final ProcessTrusteeRequest alterTrusteeRequest) {
        alterTrusteeRequest.setAction(Action.ALTER);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, alterTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_ALTER);
    }

    @POST
    @Path(Constants.REST_TRUSTEES_REMOVE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response remove(@NotNull final ProcessTrusteeRequest removeTrusteeRequest) {
        removeTrusteeRequest.setAction(Action.REMOVE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, removeTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_REMOVE);
    }

    @POST
    @Path(Constants.REST_TRUSTEES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchTrusteeRequest fetchTrusteeRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_FETCH);
    }
}
