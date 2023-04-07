/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>Rest interface for the Authenticated functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
@Path(Constants.REST_AUTHENTICATED)
public class AuthenticatedService {

    private static final String METHOD = "authenticated";

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    /**
     * The REST Authentication Endpoint.
     *
     * @param request Authentication Request
     * @return Authentication Response
     */
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response authenticated(@NotNull final Authentication request) {
        return CommonService.runRequest(settings, bean, METHOD, request, Constants.REST_AUTHENTICATED);
    }
}
