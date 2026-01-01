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
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.fitnesse.callers.CallShare;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS ProcessData feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessData extends EDSRequest<ProcessDataResponse> {

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

    /**
     * Set the Action.
     *
     * @param action Action
     */
    public void setAction(final String action) {
        this.action = Converter.findAction(action);
    }

    /**
     * Set the Human-Readable DataId.
     *
     * @param dataId Human-Readable DataId
     */
    public void setDataId(final String dataId) {
        this.dataId = getId(Converter.preCheck(dataId));
    }

    /**
     * Retrieve the Human-Readable DataId.
     *
     * @return Human-Readable DataId
     */
    public String dataId() {
        return (response != null) ? response.getDataId() : null;
    }

    /**
     * Set the Human-Readable CircleId.
     *
     * @param circleId Human-Readable CircleId
     */
    public void setCircleId(final String circleId) {
        this.circleId = getId(Converter.preCheck(circleId));
    }

    /**
     * Set the Human-Readable Target CircleId.
     *
     * @param targetCircleId Human-Readable Target CircleId
     */
    public void setTargetCircleId(final String targetCircleId) {
        this.targetCircleId = getId(Converter.preCheck(targetCircleId));
    }

    /**
     * Set the Data Name.
     *
     * @param dataName Data Name
     */
    public void setDataName(final String dataName) {
        this.dataName = Converter.preCheck(dataName);
    }

    /**
     * Set the Human-Readable FolderId.
     *
     * @param folderId Human-Readable FolderId
     */
    public void setFolderId(final String folderId) {
        this.folderId = getId(Converter.preCheck(folderId));
    }

    /**
     * Set the Target FolderId.
     *
     * @param targetFolderId Target FolderId
     */
    public void setTargetFolderId(final String targetFolderId) {
        this.targetFolderId = getId(Converter.preCheck(targetFolderId));
    }

    /**
     * Set the TypeName.
     *
     * @param typeName TypeName
     */
    public void setTypeName(final String typeName) {
        this.typeName = Converter.preCheck(typeName);
    }

    /**
     * Set the Data.
     *
     * @param data Data
     */
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

        response = CallShare.processData(requestUrl, request);

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
