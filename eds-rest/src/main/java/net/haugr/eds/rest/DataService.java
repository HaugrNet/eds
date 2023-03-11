/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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

    @POST
    @Path(Constants.REST_DATA_ADD)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        addDataRequest.setAction(Action.ADD);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, addDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_ADD);
    }

    @POST
    @Path(Constants.REST_DATA_COPY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response copy(@NotNull final ProcessDataRequest updateDataRequest) {
        updateDataRequest.setAction(Action.COPY);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_COPY);
    }

    @POST
    @Path(Constants.REST_DATA_MOVE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response move(@NotNull final ProcessDataRequest updateDataRequest) {
        updateDataRequest.setAction(Action.MOVE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_MOVE);
    }

    @POST
    @Path(Constants.REST_DATA_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        updateDataRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, updateDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_UPDATE);
    }

    @POST
    @Path(Constants.REST_DATA_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        deleteDataRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, bean, PROCESS_METHOD, deleteDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_DELETE);
    }

    @POST
    @Path(Constants.REST_DATA_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchDataRequest, Constants.REST_DATA_BASE + Constants.REST_DATA_FETCH);
    }
}
