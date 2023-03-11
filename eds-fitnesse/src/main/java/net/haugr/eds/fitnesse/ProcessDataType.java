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
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.fitnesse.callers.CallShare;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS ProcessDataType feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessDataType extends EDSRequest<ProcessDataTypeResponse> {

    private Action action = null;
    private String name = null;
    private String type = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setAction(final String action) {
        this.action = Converter.findAction(action);
    }

    public void setName(final String name) {
        this.name = Converter.preCheck(name);
    }

    public void setType(final String type) {
        this.type = Converter.preCheck(type);
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final ProcessDataTypeRequest request = prepareRequest(ProcessDataTypeRequest.class);
        request.setAction(action);
        request.setTypeName(name);
        request.setType(type);

        response = CallShare.processDataType(requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        // Reset internal values
        action = null;
        name = null;
        type = null;
    }
}
