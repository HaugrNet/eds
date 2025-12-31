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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the Version functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_VERSION)
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Version", description = "Service for retrieving the server/version information.")
public class VersionService {

    private static final String METHOD = "version";

    private final ManagementBean managementBean;
    private final Settings settings;

    public VersionService() {
        this(null);
    }

    @Inject
    public VersionService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Version Endpoint.
     *
     * @return Version Response
     */
    @org.eclipse.microprofile.openapi.annotations.Operation(
            summary = "Get version",
            description = "Returns version information for the EDS server.")
    @org.eclipse.microprofile.openapi.annotations.responses.APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Produces(CommonService.PRODUCES)
    public Response version() {
        return CommonService.runRequest(settings, managementBean, METHOD, null, Constants.REST_VERSION);
    }
}
