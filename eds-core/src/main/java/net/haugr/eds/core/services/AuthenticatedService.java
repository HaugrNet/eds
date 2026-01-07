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
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>Rest interface for the Authenticated functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
@Path(Constants.REST_AUTHENTICATED)
@Tag(name = "Authentication", description = "Operations for verifying authentication status.")
public class AuthenticatedService {

    private final ManagementBean managementBean;
    private final Settings settings;

    public AuthenticatedService() {
        this(null);
    }

    @Inject
    public AuthenticatedService(final ManagementBean managementBean) {
        this(managementBean, Settings.getInstance());
    }

    public AuthenticatedService(final ManagementBean managementBean, final Settings settings) {
        this.managementBean = managementBean;
        this.settings = settings;
    }

    /**
     * The REST Authentication Endpoint.
     *
     * @param request Authentication Request
     * @return Authentication Response
     */
    @Operation(
            summary = "Check authentication",
            description = "Verifies if the provided credentials are valid and the member is authenticated.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response authenticated(@NotNull final Authentication request) {
        return CommonService.runRequest(settings, () -> managementBean.authenticated(request), Constants.REST_AUTHENTICATED);
    }
}
