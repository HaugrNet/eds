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

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import net.haugr.eds.api.responses.EDSResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the DataType functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_DATATYPES_BASE)
@Tag(name = "Data Types", description = "Operations for managing data types used by data objects.")
public class DataTypeService {

    private final ShareBean shareBean;
    private final Settings settings;

    public DataTypeService() {
        this(null);
    }

    @Inject
    public DataTypeService(final ShareBean shareBean) {
        this.shareBean = shareBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Process DataType Endpoint.
     *
     * @param processDataTypeRequest Process DataType Request
     * @return Process DataType Response
     */
    @Operation(
            summary = "Process data type",
            description = "Creates a Data Type, or updates it if the name exists. `data`, `folder`, `thumbnail` and `entry` can't be changed.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataTypeResponse.class)))
    @POST
    @Path(Constants.REST_DATATYPES_PROCESS)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response process(@NotNull final ProcessDataTypeRequest processDataTypeRequest) {
        processDataTypeRequest.setAction(Action.PROCESS);
        return CommonService.runRequest(settings, () -> shareBean.processDataType(processDataTypeRequest), Constants.REST_DATATYPES_BASE + Constants.REST_DATATYPES_PROCESS);
    }

    /**
     * The REST Delete DataType Endpoint.
     *
     * @param deleteDataTypeRequest Delete DataType Request
     * @return Delete DataType Response
     */
    @Operation(
            summary = "Delete data type",
            description = "Deletes an unused Data Type.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ProcessDataTypeResponse.class)))
    @POST
    @Path(Constants.REST_DATATYPES_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessDataTypeRequest deleteDataTypeRequest) {
        deleteDataTypeRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, () -> shareBean.processDataType(deleteDataTypeRequest), Constants.REST_DATATYPES_BASE + Constants.REST_DATATYPES_DELETE);
    }

    /**
     * The REST Fetch DataTypes Endpoint.
     *
     * @param fetchDataTypesRequest Fetch DataTypes Request
     * @return Fetch DataTypes Response
     */
    @Operation(
            summary = "Fetch data types",
            description = "Returns all Data Types.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FetchDataTypeResponse.class)))
    @POST
    @Path(Constants.REST_DATATYPES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchDataTypeRequest fetchDataTypesRequest) {
        return CommonService.runRequest(settings, () -> shareBean.fetchDataTypes(fetchDataTypesRequest), Constants.REST_DATATYPES_BASE + Constants.REST_DATATYPES_FETCH);
    }

    // =========================================================================
    // ID-based resource API (PLAN.md, L1)
    // =========================================================================

    /**
     * List data types: {@code GET /dataTypes} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @return FetchDataTypeResponse
     */
    @Operation(operationId = "getDataTypes", summary = "List data types", description = "Returns all Data Types; as fetchDataTypes.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchDataTypeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getDataTypes(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization) {
        final FetchDataTypeRequest request = new FetchDataTypeRequest();
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::fetchDataTypes, "GET /dataTypes");
    }

    /**
     * Save data type: {@code PUT /dataTypes/{typeName}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param typeName Name of the Data Type.
     * @param body          The request body
     * @return ProcessDataTypeResponse
     */
    @Operation(operationId = "putDataTypeByName", summary = "Save data type", description = "Creates or updates a Data Type; as processDataType.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataTypeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{typeName}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putDataTypeByName(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "Name of the Data Type.", required = true) @PathParam("typeName") final String typeName,
            final ProcessDataTypeRequest body) {
        final ProcessDataTypeRequest request = (body != null) ? body : new ProcessDataTypeRequest();
        request.setAction(Action.PROCESS);
        request.setTypeName(typeName);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processDataType, "PUT /dataTypes/{typeName}");
    }

    /**
     * Delete data type: {@code DELETE /dataTypes/{typeName}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param typeName Name of the Data Type.
     * @return ProcessDataTypeResponse
     */
    @Operation(operationId = "deleteDataTypeByName", summary = "Delete data type", description = "Deletes an unused Data Type; as deleteDataType.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessDataTypeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{typeName}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteDataTypeByName(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "Name of the Data Type.", required = true) @PathParam("typeName") final String typeName) {
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();
        request.setAction(Action.DELETE);
        request.setTypeName(typeName);
        return CommonService.runResourceRequest(settings, authorization, request, shareBean::processDataType, "DELETE /dataTypes/{typeName}");
    }
}
