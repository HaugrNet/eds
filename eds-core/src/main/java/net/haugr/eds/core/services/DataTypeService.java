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
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the DataType functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_DATATYPES_BASE)
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Data Types", description = "Operations for managing data types used by data objects.")
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
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Process data type",
            description = "Creates or updates a Data Type. All stored data must have a data type for external clients to identify and apply rules. " +
                    "By default, two data types exist: 'data' and 'folder'. Additional types (like MIME Types) can be added. " +
                    "Note: The two default types cannot be updated. Only Circle Administrators or System Administrators can manage Data Types.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
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
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Delete data type",
            description = "Removes an unused Data Type from the system. If a Type is still being used by data objects, it cannot be removed. " +
                    "The two default types ('data' and 'folder') also cannot be removed.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
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
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Fetch data types",
            description = "Retrieves a list of all currently available Data Types. These can be used to add/update data or to identify how existing data should be processed.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_DATATYPES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchDataTypeRequest fetchDataTypesRequest) {
        return CommonService.runRequest(settings, () -> shareBean.fetchDataTypes(fetchDataTypesRequest), Constants.REST_DATATYPES_BASE + Constants.REST_DATATYPES_FETCH);
    }
}
