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
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the Data functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_DATA_BASE)
public class DataService {

    private static final String PROCESS_METHOD = "processData";
    private static final String FETCH_METHOD = "fetchData";

    @Inject
    private ShareBean bean;
    private final Settings settings = Settings.getInstance();

    /**
     * Default Constructor.
     */
    public DataService() {
        // Empty Constructor
    }

    /**
     * The REST Add Data Endpoint.
     *
     * @param addDataRequest Add Data Request
     * @return Add Data Response
     */
    @POST
    @Path(Constants.REST_DATA_ADD)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        addDataRequest.setAction(Action.ADD);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, addDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_ADD);
    }

    /**
     * The REST Copy Data Endpoint.
     *
     * @param copyDataRequest Copy Data Request
     * @return Copy Data Response
     */
    @POST
    @Path(Constants.REST_DATA_COPY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response copy(@NotNull final ProcessDataRequest copyDataRequest) {
        copyDataRequest.setAction(Action.COPY);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, copyDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_COPY);
    }

    /**
     * The REST Move Data Endpoint.
     *
     * @param moveDataRequest Move Data Request
     * @return Move Data Response
     */
    @POST
    @Path(Constants.REST_DATA_MOVE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response move(@NotNull final ProcessDataRequest moveDataRequest) {
        moveDataRequest.setAction(Action.MOVE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, moveDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_MOVE);
    }

    /**
     * The REST Update Data Endpoint.
     *
     * @param updateDataRequest Update Data Request
     * @return Update Data Response
     */
    @POST
    @Path(Constants.REST_DATA_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        updateDataRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_UPDATE);
    }

    /**
     * The REST Delete Data Endpoint.
     *
     * @param deleteDataRequest Delete Data Request
     * @return Delete Data Response
     */
    @POST
    @Path(Constants.REST_DATA_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        deleteDataRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, deleteDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_DELETE);
    }

    /**
     * The REST Fetch Data Endpoint.
     *
     * @param fetchDataRequest Fetch Data Request
     * @return Fetch Data Response
     */
    @POST
    @Path(Constants.REST_DATA_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_FETCH);
    }
}
