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
import net.haugr.eds.api.requests.InventoryRequest;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the Inventory functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_INVENTORY)
@Tag(name = "Inventory", description = "Operations for retrieving system inventory information.")
public class InventoryService {

    private final ManagementBean managementBean;
    private final Settings settings;

    public InventoryService() {
        this(null);
    }

    @Inject
    public InventoryService(final ManagementBean managementBean) {
        this.managementBean = managementBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Inventory Endpoint.
     *
     * @param inventoryRequest Inventory Request
     * @return Inventory Response
     */
    @Operation(
            summary = "Get inventory",
            description = "Retrieves a complete inventory of all circles and data objects accessible to the authenticated member.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response inventory(@NotNull final InventoryRequest inventoryRequest) {
        return CommonService.runRequest(settings, () -> managementBean.inventory(inventoryRequest), Constants.REST_INVENTORY);
    }
}
