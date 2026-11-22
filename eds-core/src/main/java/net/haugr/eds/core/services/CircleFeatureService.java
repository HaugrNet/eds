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

import net.haugr.eds.core.features.EntryQuery;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.FeatureState;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.CircleFeatureRequest;
import net.haugr.eds.api.requests.EntryRequest;
import net.haugr.eds.api.responses.CircleFeatureResponse;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.EntryResponse;
import net.haugr.eds.core.FeatureBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the features of a Circle and their entries (FEATURES.md).
 * Part of the ID-based resource API: authenticated with the Session token
 * in the {@code Authorization: Bearer} header, answered with the HTTP
 * status of the Return Code.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Path(Constants.REST_CIRCLES_BASE + "/{circleId}" + Constants.REST_FEATURES_BASE)
@Tag(name = "Features")
public class CircleFeatureService {

    private final FeatureBean featureBean;
    private final Settings settings;

    public CircleFeatureService() {
        this(null);
    }

    @Inject
    public CircleFeatureService(final FeatureBean featureBean) {
        this.featureBean = featureBean;
        this.settings = Settings.getInstance();
    }

    /**
     * List the features of a circle: {@code GET /circles/{circleId}/features} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle

     * @return CircleFeatureResponse
     */
    @Operation(operationId = "getCircleFeatures", summary = "List the features of a circle", description = "Returns the features of a Circle with their state.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CircleFeatureResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getCircleFeatures(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.circleFeatures(r.getCredential(), circleId), "GET /circles/{circleId}/features");
    }

    /**
     * Switch a feature: {@code PUT /circles/{circleId}/features/{feature}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param body The state
     * @return CircleFeatureResponse
     */
    @Operation(operationId = "putCircleFeature", summary = "Switch a feature", description = "Switches a feature on in a Circle, off, or to read-only.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CircleFeatureResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{feature}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putCircleFeature(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            final CircleFeatureRequest body) {
        final FeatureState state = (body != null) ? body.getState() : null;
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.setCircleFeature(r.getCredential(), circleId, feature, state), "PUT /circles/{circleId}/features/{feature}");
    }

    /**
     * Remove a feature from a circle: {@code DELETE /circles/{circleId}/features/{feature}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @return CircleFeatureResponse
     */
    @Operation(operationId = "deleteCircleFeature", summary = "Remove a feature from a circle", description = "Removes a feature from a Circle, with all its entries.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CircleFeatureResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{feature}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteCircleFeature(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.removeCircleFeature(r.getCredential(), circleId, feature), "DELETE /circles/{circleId}/features/{feature}");
    }

    /**
     * List entries: {@code GET /circles/{circleId}/features/{feature}/entries} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param kind Only this kind
     * @param about Only entries about this entry
     * @param name Only names starting with this
     * @param after Only after this position
     * @param before Only before this position
     * @param oldestFirst True for the oldest first
     * @param limit The most entries
     * @param content False to leave the content out
     * @return EntryResponse
     */
    @Operation(operationId = "getEntries", summary = "List entries", description = "Returns a page of the entries of a feature in a Circle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{feature}/entries")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getEntries(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            @Parameter(description = "Only entries of this kind.") @QueryParam(Constants.FIELD_KIND) final String kind,
            @Parameter(description = "Only entries about the entry with this ID.") @QueryParam(Constants.FIELD_ABOUT) final String about,
            @Parameter(description = "Only entries whose name starts with this.") @QueryParam(Constants.FIELD_NAME) final String name,
            @Parameter(description = "Only entries after this position.") @QueryParam("after") final Long after,
            @Parameter(description = "Only entries before this position.") @QueryParam("before") final Long before,
            @Parameter(description = "True for the oldest first; the newest come first otherwise.") @QueryParam("oldestFirst") final Boolean oldestFirst,
            @Parameter(description = "The most entries to return, 1 to 200; 50 without it.") @QueryParam("limit") final Integer limit,
            @Parameter(description = "False to leave the content of the entries out.") @QueryParam(Constants.FIELD_CONTENT) final Boolean content) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.entries(r.getCredential(), circleId, feature, EntryQuery.all().kind(kind).about(about).namePrefix(name)
                        .after(after).before(before).oldestFirst(oldestFirst).limit(limit).content(content)),
                "GET /circles/{circleId}/features/{feature}/entries");
    }

    /**
     * Add an entry: {@code POST /circles/{circleId}/features/{feature}/entries} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param body The entry
     * @return EntryResponse
     */
    @Operation(operationId = "postEntry", summary = "Add an entry", description = "Adds an entry to a feature in a Circle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @POST
    @Path("/{feature}/entries")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response postEntry(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            final EntryRequest body) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.addEntry(r.getCredential(), circleId, feature, body), "POST /circles/{circleId}/features/{feature}/entries");
    }

    /**
     * Count entries: {@code GET /circles/{circleId}/features/{feature}/count} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param kind Only this kind
     * @param after Only after this position
     * @return EntryResponse
     */
    @Operation(operationId = "getEntryCount", summary = "Count entries", description = "Counts the entries of a feature in a Circle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{feature}/count")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getEntryCount(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            @Parameter(description = "Only entries of this kind.") @QueryParam(Constants.FIELD_KIND) final String kind,
            @Parameter(description = "Only entries after this position.") @QueryParam("after") final Long after) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.count(r.getCredential(), circleId, feature, kind, after), "GET /circles/{circleId}/features/{feature}/count");
    }

    /**
     * Get an entry: {@code GET /circles/{circleId}/features/{feature}/entries/{entryId}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param entryId ID of the entry
     * @return EntryResponse
     */
    @Operation(operationId = "getEntry", summary = "Get an entry", description = "Returns one entry of a feature in a Circle.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Path("/{feature}/entries/{entryId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getEntry(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            @Parameter(description = "ID of the entry.", required = true) @PathParam("entryId") final String entryId) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.entry(r.getCredential(), circleId, feature, entryId), "GET /circles/{circleId}/features/{feature}/entries/{entryId}");
    }

    /**
     * Change an entry: {@code PUT /circles/{circleId}/features/{feature}/entries/{entryId}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param entryId ID of the entry
     * @param body The new content or name
     * @return EntryResponse
     */
    @Operation(operationId = "putEntry", summary = "Change an entry", description = "Changes the content of an entry, its name, or both.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{feature}/entries/{entryId}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putEntry(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            @Parameter(description = "ID of the entry.", required = true) @PathParam("entryId") final String entryId,
            final EntryRequest body) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.changeEntry(r.getCredential(), circleId, feature, entryId, body), "PUT /circles/{circleId}/features/{feature}/entries/{entryId}");
    }

    /**
     * Remove an entry: {@code DELETE /circles/{circleId}/features/{feature}/entries/{entryId}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param entryId ID of the entry
     * @return EntryResponse
     */
    @Operation(operationId = "deleteEntry", summary = "Remove an entry", description = "Removes an entry, and the entries which are about it.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{feature}/entries/{entryId}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteEntry(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            @Parameter(description = "ID of the entry.", required = true) @PathParam("entryId") final String entryId) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.removeEntry(r.getCredential(), circleId, feature, entryId), "DELETE /circles/{circleId}/features/{feature}/entries/{entryId}");
    }

    /**
     * Close an entry: {@code PUT /circles/{circleId}/features/{feature}/entries/{entryId}/closed} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param circleId ID of the Circle
     * @param feature Name of the feature
     * @param entryId ID of the entry
     * @param body Whether to close or to open again
     * @return EntryResponse
     */
    @Operation(operationId = "putEntryClosed", summary = "Close an entry", description = "Closes an entry, or opens it again.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EntryResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{feature}/entries/{entryId}/closed")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putEntryClosed(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "ID of the Circle.", required = true) @PathParam("circleId") final String circleId,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            @Parameter(description = "ID of the entry.", required = true) @PathParam("entryId") final String entryId,
            final EntryRequest body) {
        final Boolean closed = (body != null) ? body.getClosed() : null;
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.closeEntry(r.getCredential(), circleId, feature, entryId, closed), "PUT /circles/{circleId}/features/{feature}/entries/{entryId}/closed");
    }
}
