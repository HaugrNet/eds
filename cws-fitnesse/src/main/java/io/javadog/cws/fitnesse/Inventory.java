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

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.InventoryRequest;
import io.javadog.cws.api.responses.InventoryResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.utils.Converter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>FitNesse Fixture for the CWS Inventory feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.2
 */
public final class Inventory extends CwsRequest<InventoryResponse> {

    private int pageNumber = 1;
    private int pageSize = Constants.MAX_PAGE_SIZE;
    private final List<Metadata> metadata = new ArrayList<>();

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setPageNumber(final String pageNumber) {
        this.pageNumber = Converter.parseInt(pageNumber, this.pageNumber);
    }

    public void setPageSize(final String pageSize) {
        this.pageSize = Converter.parseInt(pageSize, this.pageSize);
    }

    public String records() {
        return Long.toString(response.getRecords());
    }

    public String circleId() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            for (int i = 0; i < response.getInventory().size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(getKey(response.getInventory().get(i).getCircleId()));
            }
        }
        builder.append(']');

        return builder.toString();
    }

    public String dataId() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            for (int i = 0; i < response.getInventory().size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(getKey(response.getInventory().get(i).getDataId()));
            }
        }
        builder.append(']');

        return builder.toString();
    }

    public String folderId() {
        return metadata.isEmpty() ? null : "-";
    }

    public String dataName() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            for (int i = 0; i < response.getInventory().size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(response.getInventory().get(i).getDataName());
            }
        }
        builder.append(']');

        return builder.toString();
    }

    public String typeName() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            for (int i = 0; i < response.getInventory().size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(response.getInventory().get(i).getTypeName());
            }
        }
        builder.append(']');

        return builder.toString();
    }

    public String added() {
        return metadata.isEmpty() ? null : Converter.convertDate(metadata.get(0).getAdded());
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
        request.setPageNumber(pageNumber);
        request.setPageSize(pageSize);

        response = CallManagement.inventory(requestType, requestUrl, request);
        if (response != null) {
            metadata.addAll(response.getInventory());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.pageNumber = 1;
        this.pageSize = 1;
        metadata.clear();
    }
}
