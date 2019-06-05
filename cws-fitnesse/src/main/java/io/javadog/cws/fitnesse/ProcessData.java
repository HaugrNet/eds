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
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.utils.Converter;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessData extends CwsRequest<ProcessDataResponse> {

    private Action action = null;
    private String dataId = null;
    private String circleId = null;
    private String targetCircleId = null;
    private String dataName = null;
    private String folderId = null;
    private String targetFolderId = null;
    private String typeName = null;
    private byte[] data = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setAction(final String action) {
        this.action = Converter.findAction(action);
    }

    public void setDataId(final String dataId) {
        this.dataId = getId(Converter.preCheck(dataId));
    }

    public String dataId() {
        return (response != null) ? response.getDataId() : null;
    }

    public void setCircleId(final String circleId) {
        this.circleId = getId(Converter.preCheck(circleId));
    }

    public void setTargetCircleId(final String targetCircleId) {
        this.targetCircleId = getId(Converter.preCheck(targetCircleId));
    }

    public void setDataName(final String dataName) {
        this.dataName = Converter.preCheck(dataName);
    }

    public void setFolderId(final String folderId) {
        this.folderId = getId(Converter.preCheck(folderId));
    }

    public void setTargetFolderId(final String targetFolderId) {
        this.targetFolderId = getId(Converter.preCheck(targetFolderId));
    }

    public void setTypeName(final String typeName) {
        this.typeName = Converter.preCheck(typeName);
    }

    public void setData(final String data) {
        this.data = Converter.convertBytes(data);
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class);
        request.setAction(action);
        request.setDataId(dataId);
        request.setCircleId(circleId);
        request.setTargetCircleId(targetCircleId);
        request.setDataName(dataName);
        request.setFolderId(folderId);
        request.setTargetFolderId(targetFolderId);
        request.setTypeName(typeName);
        request.setData(data);

        response = CallShare.processData(requestType, requestUrl, request);

        // Ensuring that the internal mapping of Ids with accounts being
        // used is synchronized.
        processId(action, circleId, dataName, response);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        action = null;
        dataId = null;
        circleId = null;
        targetCircleId = null;
        dataName = null;
        folderId = null;
        targetFolderId = null;
        typeName = null;
        data = null;
    }
}
