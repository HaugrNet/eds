/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.rest;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the Trustee functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_TRUSTEES_BASE)
public class TrusteeService {

    private static final String PROCESS_METHOD = "processTrustee";
    private static final String FETCH_METHOD = "fetchTrustees";

    private final ManagementBean managementBean;
    private final Settings settings;

    public TrusteeService() {
        this(null);
    }

    @Inject
    public TrusteeService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Add Trustee Endpoint.
     *
     * @param addTrusteeRequest Add Trustee Request
     * @return Add Trustee Response
     */
    @POST
    @Path(Constants.REST_TRUSTEES_ADD)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response add(@NotNull final ProcessTrusteeRequest addTrusteeRequest) {
        addTrusteeRequest.setAction(Action.ADD);
        return CommonService.runRequest(settings, managementBean, PROCESS_METHOD, addTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_ADD);
    }

    /**
     * The REST Alter Trustee Endpoint.
     *
     * @param alterTrusteeRequest Alter Trustee Request
     * @return Alter Trustee Response
     */
    @POST
    @Path(Constants.REST_TRUSTEES_ALTER)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response alter(@NotNull final ProcessTrusteeRequest alterTrusteeRequest) {
        alterTrusteeRequest.setAction(Action.ALTER);
        return CommonService.runRequest(settings, managementBean, PROCESS_METHOD, alterTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_ALTER);
    }

    /**
     * The REST Remove Trustee Endpoint.
     *
     * @param removeTrusteeRequest Remove Trustee Request
     * @return Remove Trustee Response
     */
    @POST
    @Path(Constants.REST_TRUSTEES_REMOVE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response remove(@NotNull final ProcessTrusteeRequest removeTrusteeRequest) {
        removeTrusteeRequest.setAction(Action.REMOVE);
        return CommonService.runRequest(settings, managementBean, PROCESS_METHOD, removeTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_REMOVE);
    }

    /**
     * The REST Fetch Trustees Endpoint.
     *
     * @param fetchTrusteeRequest Fetch Trustees Request
     * @return Fetch Trustees Response
     */
    @POST
    @Path(Constants.REST_TRUSTEES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchTrusteeRequest fetchTrusteeRequest) {
        return CommonService.runRequest(settings, managementBean, FETCH_METHOD, fetchTrusteeRequest, Constants.REST_TRUSTEES_BASE + Constants.REST_TRUSTEES_FETCH);
    }
}
