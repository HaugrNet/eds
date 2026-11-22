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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.FeatureRequest;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.FeatureResponse;
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
 * <p>REST interface for the features which the stone offers (FEATURES.md). Part
 * of the ID-based resource API: authenticated with the Session token in the
 * {@code Authorization: Bearer} header, answered with the HTTP status of
 * the Return Code.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Path(Constants.REST_FEATURES_BASE)
@Tag(name = "Features", description = "Features: what a Circle can have beside its files, such as a chat or a calendar. "
        + "A feature is a declaration; EDS stores and guards its entries without knowing what they are.")
public class FeatureService {

    private final FeatureBean featureBean;
    private final Settings settings;

    public FeatureService() {
        this(null);
    }

    @Inject
    public FeatureService(final FeatureBean featureBean) {
        this.featureBean = featureBean;
        this.settings = Settings.getInstance();
    }

    /**
     * List features: {@code GET /features} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @return FeatureResponse
     */
    @Operation(operationId = "getFeatures", summary = "List features", description = "Returns the features which the stone offers.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeatureResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @GET
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response getFeatures(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.features(r.getCredential()), "GET " + Constants.REST_FEATURES_BASE);
    }

    /**
     * Declare a feature: {@code PUT /features/{feature}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param feature          Name of the feature
     * @param body          The declaration
     * @return FeatureResponse
     */
    @Operation(operationId = "putFeature", summary = "Declare a feature", description = "Adds a feature to those the stone offers, or replaces its declaration.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeatureResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @PUT
    @Path("/{feature}")
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response putFeature(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature,
            final FeatureRequest body) {
        final String declaration = (body != null) ? body.getDeclaration() : null;
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.putFeature(r.getCredential(), feature, declaration), "PUT " + Constants.REST_FEATURES_BASE + "/{feature}");
    }

    /**
     * Remove a feature: {@code DELETE /features/{feature}} (FEATURES.md).
     *
     * @param authorization Authorization header with the Session token
     * @param feature          Name of the feature
     * @return FeatureResponse
     */
    @Operation(operationId = "deleteFeature", summary = "Remove a feature", description = "Removes a feature from those the stone offers.")
    @APIResponse(responseCode = "200", description = CommonService.RESOURCE_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FeatureResponse.class)))
    @APIResponse(responseCode = "4xx", description = CommonService.RESOURCE_WARNING,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @APIResponse(responseCode = "5xx", description = CommonService.RESOURCE_ERROR,
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = EDSResponse.class)))
    @DELETE
    @Path("/{feature}")
    @Produces(CommonService.PRODUCES)
    @SecurityRequirement(name = CommonService.SESSION_SCHEME)
    public Response deleteFeature(
            @Parameter(hidden = true) @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @Parameter(description = "Name of the feature.", required = true) @PathParam("feature") final String feature) {
        return CommonService.runResourceRequest(settings, authorization, new Authentication(),
                r -> featureBean.deleteFeature(r.getCredential(), feature), "DELETE " + Constants.REST_FEATURES_BASE + "/{feature}");
    }
}
