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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchCircleRequest;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the Circle functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_CIRCLES_BASE)
public class CircleService {

    private static final String PROCESS_METHOD = "processCircle";
    private static final String FETCH_METHOD = "fetchCircles";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    /**
     * Default Constructor.
     */
    public CircleService() {
    }

    /**
     * The REST Create Circle Endpoint.
     *
     * @param createCircleRequest Create Circle Request
     * @return Create Circle Response
     */
    @POST
    @Path(Constants.REST_CIRCLES_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        createCircleRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, createCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_CREATE);
    }

    /**
     * The REST Update circle Endpoint.
     *
     * @param updateCircleRequest Update Circle Request
     * @return Update Circle Response
     */
    @POST
    @Path(Constants.REST_CIRCLES_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        updateCircleRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_UPDATE);
    }

    /**
     * The REST Delete Circle Endpoint.
     *
     * @param deleteCircleRequest Delete Circle Request
     * @return Delete Circle Response
     */
    @POST
    @Path(Constants.REST_CIRCLES_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        deleteCircleRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, deleteCircleRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_DELETE);
    }

    /**
     * The REST Fetch Circles Endpoint.
     *
     * @param fetchCirclesRequest Fetch Circles Request
     * @return Fetch Circles Response
     */
    @POST
    @Path(Constants.REST_CIRCLES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchCirclesRequest, Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_FETCH);
    }
}
