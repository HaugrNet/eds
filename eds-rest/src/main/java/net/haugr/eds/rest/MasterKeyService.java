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

/**
 * <p>REST interface for the MasterKey functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_MASTERKEY)
public class MasterKeyService {

    private static final String METHOD = "masterKey";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    /**
     * Default Constructor.
     */
    public MasterKeyService() {
        // Empty Constructor
    }

    /**
     * The REST MasterKey Endpoint.
     *
     * @param masterKeyRequest MasterKey Request
     * @return MasterKey Response
     */
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response masterKey(@NotNull final MasterKeyRequest masterKeyRequest) {
        return CommonService.runRequest(settings, bean, METHOD, masterKeyRequest, Constants.REST_MASTERKEY);
    }
}
