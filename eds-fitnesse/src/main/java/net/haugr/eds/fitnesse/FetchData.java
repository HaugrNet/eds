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
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import java.util.ArrayList;
import java.util.List;
import net.haugr.eds.fitnesse.callers.CallShare;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS FetchData feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchData extends EDSRequest<FetchDataResponse> {

    private String circleId = null;
    private String dataId = null;
    private String dataName = null;
    private int pageNumber = 1;
    private int pageSize = 1;
    private final List<Metadata> metadata = new ArrayList<>();

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setCircleId(final String circleId) {
        this.circleId = getId(Converter.preCheck(circleId));
    }

    public void setDataId(final String dataId) {
        this.dataId = getId(Converter.preCheck(dataId));
    }

    public void setDataName(final String dataName) {
        this.dataName = Converter.preCheck(dataName);
    }

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
        return metadata.isEmpty() ? null : metadata.get(0).getCircleId();
    }

    public String dataId() {
        final String id;

        if ((dataId == null) && (dataName == null)) {
            final List<String> ids = new ArrayList<>(metadata.size());
            for (final Metadata current : metadata) {
                ids.add(getKey(current.getDataId()));
            }
            id = ids.toString();
        } else {
            id = metadata.isEmpty() ? null : getKey(metadata.get(0).getDataId());
        }

        return id;
    }

    public String folderId() {
        final String tmpId = metadata.isEmpty() ? null : metadata.get(0).getFolderId();
        String folderId = null;

        if (tmpId != null) {
            final String tmpKey = getKey(tmpId);
            if (tmpKey == null) {
                final String circleKey = getKey(metadata.get(0).getCircleId());
                final String newKey = circleKey.substring(0, circleKey.indexOf(EXTENSION_ID)) + "_root";
                processId(Action.ADD, null, newKey, tmpId);
                folderId = newKey + EXTENSION_ID;
            } else {
                folderId = tmpKey;
            }
        }

        return folderId;
    }

    public String dataName() {
        final String name;

        if ((dataId == null) && (dataName == null)) {
            final List<String> names = new ArrayList<>(metadata.size());
            for (final Metadata current : metadata) {
                names.add(current.getDataName());
            }
            name = names.toString();
        } else {
            name = metadata.isEmpty() ? null : metadata.get(0).getDataName();
        }

        return name;
    }

    public String typeName() {
        final String type;

        if ((dataId == null) && (dataName == null)) {
            final List<String> types = new ArrayList<>(metadata.size());
            for (final Metadata current : metadata) {
                types.add(current.getTypeName());
            }
            type = types.toString();
        } else {
            type = metadata.isEmpty() ? null : metadata.get(0).getTypeName();
        }

        return type;
    }

    public String added() {
        final String added;

        if (dataId == null) {
            final List<String> dates = new ArrayList<>(metadata.size());
            for (final Metadata current : metadata) {
                dates.add(Converter.convertDate(current.getAdded()));
            }
            added = dates.toString();
        } else {
            added = metadata.isEmpty() ? null : Converter.convertDate(metadata.get(0).getAdded());
        }

        return added;
    }

    public String data() {
        return (response.getData() != null) ? Converter.convertBytes(response.getData()) : null;
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchDataRequest request = prepareRequest(FetchDataRequest.class);
        request.setCircleId(circleId);
        request.setDataId(dataId);
        request.setDataName(dataName);
        request.setPageNumber(pageNumber);
        request.setPageSize(pageSize);

        response = CallShare.fetchData(requestUrl, request);
        if (response != null) {
            metadata.addAll(response.getMetadata());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.circleId = null;
        this.dataId = null;
        this.pageNumber = 1;
        this.pageSize = 1;
        metadata.clear();
    }
}
