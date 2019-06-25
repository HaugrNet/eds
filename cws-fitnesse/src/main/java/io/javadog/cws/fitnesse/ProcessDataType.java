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

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the CWS ProcessDataType feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessDataType extends CwsRequest<ProcessDataTypeResponse> {

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

        response = CallShare.processDataType(requestType, requestUrl, request);
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
