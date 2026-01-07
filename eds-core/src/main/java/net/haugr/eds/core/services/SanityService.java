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
import net.haugr.eds.api.requests.SanityRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the Sanity functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_SANITIZED)
@Tag(name = "Sanity", description = "Operations for performing sanity checks on data integrity.")
public class SanityService {

    private final ManagementBean managementBean;
    private final Settings settings;

    public SanityService() {
        this(null);
    }

    @Inject
    public SanityService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Sanitized Endpoint.
     *
     * @param sanitizedRequest Sanitized Request
     * @return Sanitized Response
     */
    @Operation(
            summary = "Run sanity check",
            description = "Performs a sanity check on data integrity and returns any records that have been marked as corrupted or failed verification.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response sanitized(@NotNull final SanityRequest sanitizedRequest) {
        return CommonService.runRequest(settings, () -> managementBean.sanity(sanitizedRequest), Constants.REST_SANITIZED);
    }
}
