/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * <p>REST interface for the Circle functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_CIRCLES_BASE)
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Circles", description = "Operations for managing circles (create, update, delete, fetch).")
public class CircleService {

    private static final String PROCESS_METHOD = "processCircle";
    private static final String FETCH_METHOD = "fetchCircles";

    private final ManagementBean managementBean;
    private final Settings settings;

    public CircleService() {
        this(null);
    }

    @Inject
    public CircleService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Create Circle Endpoint.
     *
     * @param createCircleRequest Create Circle Request
     * @return Create Circle Response
     */
    @Operation(
            summary = "Create circle",
            description = "Creates a new circle. The action is set to CREATE internally.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_CIRCLES_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        createCircleRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, managementBean, PROCESS_METHOD, createCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_CREATE);
    }

    /**
     * The REST Update circle Endpoint.
     *
     * @param updateCircleRequest Update Circle Request
     * @return Update Circle Response
     */
    @Operation(
            summary = "Update circle",
            description = "Updates an existing circle. The action is set to UPDATE internally.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_CIRCLES_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        updateCircleRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, managementBean, PROCESS_METHOD, updateCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_UPDATE);
    }

    /**
     * The REST Delete Circle Endpoint.
     *
     * @param deleteCircleRequest Delete Circle Request
     * @return Delete Circle Response
     */
    @Operation(
            summary = "Delete circle",
            description = "Deletes an existing circle. The action is set to DELETE internally.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_CIRCLES_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        deleteCircleRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, managementBean, PROCESS_METHOD, deleteCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_DELETE);
    }

    /**
     * The REST Fetch Circles Endpoint.
     *
     * @param fetchCirclesRequest Fetch Circles Request
     * @return Fetch Circles Response
     */
    @Operation(
            summary = "Fetch circles",
            description = "Fetches circles, optionally filtered and paginated.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_CIRCLES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        return CommonService.runRequest(settings, managementBean, FETCH_METHOD, fetchCirclesRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_FETCH);
    }
}
