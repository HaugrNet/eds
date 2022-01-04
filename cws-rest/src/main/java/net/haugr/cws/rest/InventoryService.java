/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.rest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.InventoryRequest;
import net.haugr.cws.api.responses.InventoryResponse;
import net.haugr.cws.core.ManagementBean;
import net.haugr.cws.core.misc.LoggingUtil;
import net.haugr.cws.core.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>REST interface for the MasterKey functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_INVENTORY)
public class InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryService.class);

    @Inject
    private ManagementBean bean;
    private final Settings settings = Settings.getInstance();

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response inventory(@NotNull final InventoryRequest request) {
        final long startTime = System.nanoTime();
        InventoryResponse response;

        try {
            response = bean.inventory(request);
            LOGGER.info(LoggingUtil.requestDuration(settings.getLocale(), Constants.REST_INVENTORY, startTime));
        } catch (RuntimeException e) {
            LOGGER.error(LoggingUtil.requestDuration(settings.getLocale(), Constants.REST_INVENTORY, startTime, e), e);
            response = new InventoryResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
