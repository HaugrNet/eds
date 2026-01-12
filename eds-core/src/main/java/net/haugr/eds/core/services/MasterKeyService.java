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
import net.haugr.eds.api.requests.MasterKeyRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the MasterKey functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_MASTERKEY)
@Tag(name = "Master Key", description = "Operations for managing the system master key.")
public class MasterKeyService {

    private final ManagementBean managementBean;
    private final Settings settings;

    public MasterKeyService() {
        this(null);
    }

    @Inject
    public MasterKeyService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST MasterKey Endpoint.
     *
     * @param masterKeyRequest MasterKey Request
     * @return MasterKey Response
     */
    @Operation(
            summary = "Manage master key",
            description = "The Master Key is a special symmetric key used to encrypt and decrypt all Initial Vectors and Member Salt values. " +
                    "The key is only kept in memory and is never persisted, meaning it must be set when the EDS instance is started. " +
                    "There is a default Master Key based on known values in the system, but for enhanced security, it should be set by the System Administrator. " +
                    "Important: The MasterKey must be set AFTER the system Salt has been updated, as the system Salt is required by the Master Key.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response masterKey(@NotNull final MasterKeyRequest masterKeyRequest) {
        return CommonService.runRequest(settings, () -> managementBean.masterKey(masterKeyRequest), Constants.REST_MASTERKEY);
    }
}
