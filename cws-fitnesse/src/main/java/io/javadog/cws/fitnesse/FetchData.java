/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.utils.Converter;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchData extends CwsRequest<FetchDataResponse> {

    private String circleId = null;
    private String dataId = null;
    private int pageNumber = 1;
    private int pageSize = 1;
    private Metadata metadata = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    public void setPageNumber(final String pageNumber) {
        this.pageNumber = Integer.parseInt(pageNumber);
    }

    public void setPageSize(final String pageSize) {
        this.pageSize = Integer.parseInt(pageSize);
    }

    public void setIndex(final int index) {
        if ((response != null) && (index < response.getMetadata().size())) {
            metadata = response.getMetadata().get(index);
        }
    }

    public String records() {
        return Long.toString(response.getRecords());
    }

    public String circleId() {
        return (metadata != null) ? metadata.getCircleId() : UNDEFINED;
    }

    public String dataId() {
        return (metadata != null) ? metadata.getDataId() : UNDEFINED;
    }

    public String folderId() {
        return (metadata != null) ? metadata.getFolderId() : UNDEFINED;
    }

    public String dataName() {
        return (metadata != null) ? metadata.getDataName() : UNDEFINED;
    }

    public String typeName() {
        return (metadata != null) ? metadata.getTypeName() : UNDEFINED;
    }

    public String added() {
        return (metadata != null) ? Converter.convertDate(metadata.getAdded()) : UNDEFINED;
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
        request.setPageNumber(pageNumber);
        request.setPageSize(pageSize);

        response = CallShare.fetchData(request);
        setIndex(0);
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
        this.metadata = null;
    }
}
