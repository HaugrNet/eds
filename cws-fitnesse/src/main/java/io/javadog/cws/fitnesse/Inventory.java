/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.InventoryRequest;
import io.javadog.cws.api.requests.SanityRequest;
import io.javadog.cws.api.responses.InventoryResponse;
import io.javadog.cws.api.responses.SanityResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.utils.Converter;
import java.util.Date;

/**
 * <p>FitNesse Fixture for the CWS Inventory feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.2
 */
public final class Inventory extends CwsRequest<InventoryResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String failures() {
        return response.getInventory().toString();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final InventoryRequest request = prepareRequest(InventoryRequest.class);

        response = CallManagement.inventory(requestType, requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
    }
}
