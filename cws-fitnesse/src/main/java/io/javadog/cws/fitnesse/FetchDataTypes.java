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

import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import java.util.List;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchDataTypes extends CwsRequest<FetchDataTypeResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String dataTypes() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            final List<DataType> dataTypes = response.getDataTypes();
            for (int i = 0; i < dataTypes.size(); i++) {
                final DataType dataType = dataTypes.get(i);
                if (i >= 1) {
                    builder.append(", ");
                }
                builder.append("DataType{typeName='")
                        .append(dataType.getTypeName())
                        .append("', type='")
                        .append(dataType.getType())
                        .append("'}");
            }
        }
        builder.append(']');

        return builder.toString();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchDataTypeRequest request = prepareRequest(FetchDataTypeRequest.class);

        response = CallShare.fetchDataTypes(requestType, requestUrl, request);
    }
}
