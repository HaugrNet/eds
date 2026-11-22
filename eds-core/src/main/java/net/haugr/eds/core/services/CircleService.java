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
import net.haugr.eds.api.responses.EDSResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.ws.rs.core.MediaType;
import net.haugr.eds.api.responses.FetchCircleResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
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
            description = "Creates a new Circle, with the creator (or a Member named by the System Administrator) as its first administrator.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @POST
    @Path(Constants.REST_CIRCLES_CREATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response create(@NotNull final ProcessCircleRequest createCircleRequest) {
        createCircleRequest.setAction(Action.CREATE);
        return CommonService.runRequest(settings, () -> managementBean.processCircle(createCircleRequest), Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_CREATE);
    }

    /**
     * The REST Update circle Endpoint.
     *
     * @param updateCircleRequest Update Circle Request
     * @return Update Circle Response
     */
    @Operation(
            summary = "Update circle",
            description = "Renames a Circle and/or replaces its external key.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @POST
    @Path(Constants.REST_CIRCLES_UPDATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response update(@NotNull final ProcessCircleRequest updateCircleRequest) {
        updateCircleRequest.setAction(Action.UPDATE);
        return CommonService.runRequest(settings, () -> managementBean.processCircle(updateCircleRequest), Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_UPDATE);
    }

    /**
     * The REST Delete Circle Endpoint.
     *
     * @param deleteCircleRequest Delete Circle Request
     * @return Delete Circle Response
     */
    @Operation(
            summary = "Delete circle",
            description = "Deletes a Circle with its keys, Trustees and Data Objects. This can't be undone.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @POST
    @Path(Constants.REST_CIRCLES_DELETE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response delete(@NotNull final ProcessCircleRequest deleteCircleRequest) {
        deleteCircleRequest.setAction(Action.DELETE);
        return CommonService.runRequest(settings, () -> managementBean.processCircle(deleteCircleRequest), Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_DELETE);
    }

    /**
     * The REST Rotate Circle Endpoint (PLAN.md, Crypto C6).
     *
     * @param rotateCircleRequest Rotate Circle Request
     * @return Rotate Circle Response
     */
    @Operation(
            summary = "Rotate circle secret",
            description = "Has the Circle Secret of a Circle rotated now: a new secret, with which its Data Objects are re-encrypted in the background.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @POST
    @Path(Constants.REST_CIRCLES_ROTATE)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response rotate(@NotNull final ProcessCircleRequest rotateCircleRequest) {
        rotateCircleRequest.setAction(Action.ROTATE);
        return CommonService.runRequest(settings, () -> managementBean.processCircle(rotateCircleRequest), Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_ROTATE);
    }

    /**
     * The REST Fetch Circles Endpoint.
     *
     * @param fetchCirclesRequest Fetch Circles Request
     * @return Fetch Circles Response
     */
    @Operation(
            summary = "Fetch circles",
            description = "Returns the Circles the requesting Member may see.")
    @APIResponse(responseCode = "200", description = "The outcome of the request; see returnCode.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchCircleResponse.class)))
    @POST
    @Path(Constants.REST_CIRCLES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchCircleRequest fetchCirclesRequest) {
        return CommonService.runRequest(settings, () -> managementBean.fetchCircles(fetchCirclesRequest), Constants.REST_CIRCLES_BASE + Constants.REST_CIRCLES_FETCH);
    }

    // =========================================================================
    // ID-based resource API (PLAN.md, L1)
    // =========================================================================

    /**
     * List circles: {@code GET /circles} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @return FetchCircleResponse
     */
    @Operation(operationId = "getCircles", summary = "List circles", description = "Returns the Circles the requesting Member may see; as fetchCircles.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchCircleResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getCircles(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization) {
        final FetchCircleRequest request = new FetchCircleRequest();
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::fetchCircles, "GET /circles");
    }

    /**
     * Get circle: {@code GET /circles/{circleId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @return FetchCircleResponse
     */
    @Operation(operationId = "getCircleById", summary = "Get circle", description = "Returns one Circle, if the requesting Member may see it.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchCircleResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{circleId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getCircleById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId) {
        final FetchCircleRequest request = new FetchCircleRequest();
        return CommonService.runResourceRequest(settings, authorization, request, r -> onlyCircle(managementBean.fetchCircles(r), circleId), "GET /circles/{circleId}");
    }

    /**
     * Create circle: {@code POST /circles} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param body          The request body
     * @return ProcessCircleResponse
     */
    @Operation(operationId = "postCircles", summary = "Create circle", description = "Creates a new Circle; as createCircle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postCircles(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            final ProcessCircleRequest body) {
        final ProcessCircleRequest request = (body != null) ? body : new ProcessCircleRequest();
        request.setAction(Action.CREATE);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processCircle, "POST /circles");
    }

    /**
     * Update circle: {@code PUT /circles/{circleId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @param body          The request body
     * @return ProcessCircleResponse
     */
    @Operation(operationId = "putCircleById", summary = "Update circle", description = "Renames a Circle and/or replaces its external key; as updateCircle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{circleId}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putCircleById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            final ProcessCircleRequest body) {
        final ProcessCircleRequest request = (body != null) ? body : new ProcessCircleRequest();
        request.setAction(Action.UPDATE);
        request.setCircleId(circleId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processCircle, "PUT /circles/{circleId}");
    }

    /**
     * Rotate the Circle Secret: {@code PUT /circles/{circleId}/rotation} (ID-based resource API; PLAN.md, Crypto C6).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @return ProcessCircleResponse
     */
    @Operation(operationId = "putCircleRotation", summary = "Rotate circle secret", description = "Has the Circle Secret of the Circle rotated now; as rotateCircle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{circleId}/rotation")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putCircleRotation(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId) {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAction(Action.ROTATE);
        request.setCircleId(circleId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processCircle, "PUT /circles/{circleId}/rotation");
    }

    /**
     * Delete circle: {@code DELETE /circles/{circleId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @return ProcessCircleResponse
     */
    @Operation(operationId = "deleteCircleById", summary = "Delete circle", description = "Deletes a Circle with its keys, Trustees and Data Objects; as deleteCircle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessCircleResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{circleId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteCircleById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId) {
        final ProcessCircleRequest request = new ProcessCircleRequest();
        request.setAction(Action.DELETE);
        request.setCircleId(circleId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processCircle, "DELETE /circles/{circleId}");
    }

    /**
     * List trustees: {@code GET /circles/{circleId}/trustees} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @return FetchTrusteeResponse
     */
    @Operation(operationId = "getCircleTrustees", summary = "List trustees", description = "Returns the Trustees of a Circle; as fetchTrustees with a circleId.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FetchTrusteeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{circleId}/trustees")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getCircleTrustees(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId) {
        final FetchTrusteeRequest request = new FetchTrusteeRequest();
        request.setCircleId(circleId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::fetchTrustees, "GET /circles/{circleId}/trustees");
    }

    /**
     * Add trustee: {@code POST /circles/{circleId}/trustees} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @param body          The request body
     * @return ProcessTrusteeResponse
     */
    @Operation(operationId = "postCircleTrustees", summary = "Add trustee", description = "Adds a Member as Trustee of a Circle; as addTrustee.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessTrusteeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Path("/{circleId}/trustees")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postCircleTrustees(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            final ProcessTrusteeRequest body) {
        final ProcessTrusteeRequest request = (body != null) ? body : new ProcessTrusteeRequest();
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processTrustee, "POST /circles/{circleId}/trustees");
    }

    /**
     * Alter trustee: {@code PUT /circles/{circleId}/trustees/{memberId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @param memberId ID of the Member.
     * @param body          The request body
     * @return ProcessTrusteeResponse
     */
    @Operation(operationId = "putCircleTrusteeById", summary = "Alter trustee", description = "Changes the trust level of a Trustee, their secret, or both; as alterTrustee.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessTrusteeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{circleId}/trustees/{memberId}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putCircleTrusteeById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "ID of the Member.", required = true) @PathParam("memberId") final String memberId,
            final ProcessTrusteeRequest body) {
        final ProcessTrusteeRequest request = (body != null) ? body : new ProcessTrusteeRequest();
        request.setAction(Action.ALTER);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processTrustee, "PUT /circles/{circleId}/trustees/{memberId}");
    }

    /**
     * Set secrets: {@code PUT /circles/{circleId}/secrets} (ID-based resource API, PLAN.md, L1; E2E.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @param body          The request body
     * @return ProcessTrusteeResponse
     */
    @Operation(operationId = "putCircleSecrets", summary = "Set secrets", description = "Sets the secrets of several Trustees of a Circle, all or none; as setSecrets.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessTrusteeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{circleId}/secrets")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putCircleSecrets(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            final ProcessTrusteeRequest body) {
        final ProcessTrusteeRequest request = (body != null) ? body : new ProcessTrusteeRequest();
        request.setAction(Action.SECRETS);
        request.setCircleId(circleId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processTrustee, "PUT /circles/{circleId}/secrets");
    }

    /**
     * Remove trustee: {@code DELETE /circles/{circleId}/trustees/{memberId}} (ID-based resource API, PLAN.md, L1).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle.
     * @param memberId ID of the Member.
     * @return ProcessTrusteeResponse
     */
    @Operation(operationId = "deleteCircleTrusteeById", summary = "Remove trustee", description = "Removes a Trustee from a Circle; as removeTrustee.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessTrusteeResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{circleId}/trustees/{memberId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteCircleTrusteeById(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "ID of the Member.", required = true) @PathParam("memberId") final String memberId) {
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();
        request.setAction(Action.REMOVE);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        return CommonService.runResourceRequest(settings, authorization, request, managementBean::processTrustee, "DELETE /circles/{circleId}/trustees/{memberId}");
    }

    /**
     * Keeps only the given Circle of the fetched ones; fetchCircles has no
     * Circle Id, so the Circles the Member may see are filtered.
     *
     * @param response The fetched Circles
     * @param circleId The Circle to keep
     * @return Response with that Circle, or IDENTIFICATION_WARNING
     */
    private static EDSResponse onlyCircle(final FetchCircleResponse response, final String circleId) {
        if (!response.isOk()) {
            return response;
        }
        final java.util.List<net.haugr.eds.api.dtos.Circle> found = response.getCircles().stream()
                .filter(circle -> circleId.equals(circle.getCircleId()))
                .toList();
        if (found.isEmpty()) {
            return new FetchCircleResponse(net.haugr.eds.api.common.ReturnCode.IDENTIFICATION_WARNING, "No Circle could be found with the given Id.");
        }
        final FetchCircleResponse one = new FetchCircleResponse();
        one.setCircles(found);
        return one;
    }
}
