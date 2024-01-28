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
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.dtos.DataType;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import java.util.List;
import net.haugr.eds.fitnesse.callers.CallShare;

/**
 * <p>FitNesse Fixture for the EDS FetchDataTypes feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchDataTypes extends EDSRequest<FetchDataTypeResponse> {

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

        response = CallShare.fetchDataTypes(requestUrl, request);
    }
}
