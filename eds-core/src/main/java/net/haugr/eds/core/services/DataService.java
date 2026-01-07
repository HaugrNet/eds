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
package net.haugr.eds.core.services;

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
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Data", description = "Operations for managing data objects within circles.")
public class DataService {

    private final ShareBean shareBean;
    private final Settings settings;

    public DataService() {
        this(null);
    }

    @Inject
    public DataService(final ShareBean shareBean) {
        this.shareBean = shareBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Add Data Endpoint.
     *
     * @param addDataRequest Add Data Request
     * @return Add Data Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Add data",
            description = "Adds a new data object to a circle or folder. The action is set to ADD internally.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATA_ADD)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        addDataRequest.setAction(Action.ADD);
        return CommonService.runRequest(settings, () -> shareBean.processData(addDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_ADD);
    }

    /**
     * The REST Copy Data Endpoint.
     *
     * @param copyDataRequest Copy Data Request
     * @return Copy Data Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Copy data",
            description = "Copies an existing data object to a target circle or folder. The action is set to COPY internally.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATA_COPY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response copy(@NotNull final ProcessDataRequest copyDataRequest) {
        copyDataRequest.setAction(Action.COPY);
        return CommonService.runRequest(settings, () -> shareBean.processData(copyDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_COPY);
    }

    /**
     * The REST Move Data Endpoint.
     *
     * @param moveDataRequest Move Data Request
     * @return Move Data Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Move data",
            description = "Moves an existing data object to a target circle or folder. The action is set to MOVE internally.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATA_MOVE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response move(@NotNull final ProcessDataRequest moveDataRequest) {
        moveDataRequest.setAction(Action.MOVE);
        return CommonService.runRequest(settings, () -> shareBean.processData(moveDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_MOVE);
    }

    /**
     * The REST Update Data Endpoint.
     *
     * @param updateDataRequest Update Data Request
     * @return Update Data Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Update data",
            description = "Updates an existing data object, including optional rename and/or moving within the structure. The action is set to UPDATE internally.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATA_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        updateDataRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, () -> shareBean.processData(updateDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_UPDATE);
    }

    /**
     * The REST Delete Data Endpoint.
     *
     * @param deleteDataRequest Delete Data Request
     * @return Delete Data Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Delete data",
            description = "Deletes an existing data object. The action is set to DELETE internally.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATA_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        deleteDataRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, () -> shareBean.processData(deleteDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_DELETE);
    }

    /**
     * The REST Fetch Data Endpoint.
     *
     * @param fetchDataRequest Fetch Data Request
     * @return Fetch Data Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Fetch data",
            description = "Fetches data objects by circle, data id, or name with paging support.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATA_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        return CommonService.runRequest(settings, () -> shareBean.fetchData(fetchDataRequest), Constants.REST_DATA_BASE + Constants.REST_DATA_FETCH);
    }
}
