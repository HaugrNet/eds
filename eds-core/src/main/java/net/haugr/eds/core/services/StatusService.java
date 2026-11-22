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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.StatusKeyRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.StatusKeyResponse;
import net.haugr.eds.api.responses.StatusResponse;
import net.haugr.eds.core.StatusBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the status of the stone (STATUS.md): what it tells
 * about itself to those who look after it, and the status keys. The
 * {@code Authorization: Bearer} header carries a status key or the Session
 * of the System Administrator; the answer has the HTTP status of the
 * Return Code.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Path(Constants.REST_STATUS_BASE)
@Tag(name = "Status", description = "The status of the stone, for those who look after it: its load and its inventory, in general numbers.")
public class StatusService {

    private final StatusBean statusBean;
    private final Settings settings;

    public StatusService() {
        this(null);
    }

    @Inject
    public StatusService(final StatusBean statusBean) {
        this.statusBean = statusBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The status: {@code GET /status} (STATUS.md).
     *
     * @param authorization Authorization header with a status key or the Session token
     * @param since         Only samples of the load after this sequence
     * @param history       True to add the day and the kept inventory
     * @return StatusResponse
     */
    @Operation(operationId = "getStatus", summary = "Status of the stone", description = "Returns the load and the inventory of the stone.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatusResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getStatus(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "Only samples of the load after this sequence, i.e. those a client doesn't have yet.") @QueryParam("since") final Long since,
            @Parameter(description = "True to add the load of the last day in steps, and all kept samples of the inventory.") @QueryParam("history") final Boolean history) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> statusBean.status(r.getCredential(), since, history), "GET " + Constants.REST_STATUS_BASE);
    }

    /**
     * List status keys: {@code GET /status/keys} (STATUS.md).
     *
     * @param authorization Authorization header with the Session token
     * @return StatusKeyResponse
     */
    @Operation(operationId = "getStatusKeys", summary = "List status keys", description = "Returns the labels of the status keys of the stone.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatusKeyResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path(Constants.REST_STATUS_KEYS)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getStatusKeys(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> statusBean.keys(r.getCredential()), "GET " + Constants.REST_STATUS_BASE + Constants.REST_STATUS_KEYS);
    }

    /**
     * Create a status key: {@code POST /status/keys} (STATUS.md).
     *
     * @param authorization Authorization header with the Session token
     * @param body          The label of the key
     * @return StatusKeyResponse
     */
    @Operation(operationId = "postStatusKey", summary = "Create a status key", description = "Creates a status key, which is returned once.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatusKeyResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Path(Constants.REST_STATUS_KEYS)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postStatusKey(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            final StatusKeyRequest body) {
        final String label = (body != null) ? body.getLabel() : null;
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> statusBean.createKey(r.getCredential(), label), "POST " + Constants.REST_STATUS_BASE + Constants.REST_STATUS_KEYS);
    }

    /**
     * Withdraw a status key: {@code DELETE /status/keys/{label}} (STATUS.md).
     *
     * @param authorization Authorization header with the Session token
     * @param label         The label of the key
     * @return StatusKeyResponse
     */
    @Operation(operationId = "deleteStatusKey", summary = "Withdraw a status key", description = "Withdraws a status key.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = StatusKeyResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path(Constants.REST_STATUS_KEYS + "/{label}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteStatusKey(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "The label of the key.", required = true) @PathParam("label") final String label) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> statusBean.deleteKey(r.getCredential(), label), "DELETE " + Constants.REST_STATUS_BASE + Constants.REST_STATUS_KEYS + "/{label}");
    }
}
