/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.fitnesse;

import net.haugr.cws.api.common.Action;
import net.haugr.cws.api.requests.ProcessCircleRequest;
import net.haugr.cws.api.responses.ProcessCircleResponse;
import net.haugr.cws.fitnesse.callers.CallManagement;
import net.haugr.cws.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the CWS ProcessCircle feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessCircle extends CwsRequest<ProcessCircleResponse> {

    private Action action = null;
    private String circleId = null;
    private String circleName = null;
    private String memberId = null;
    private String circleKey = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setAction(final String action) {
        this.action = Converter.findAction(action);
    }

    public void setCircleId(final String circleId) {
        this.circleId = getId(Converter.preCheck(circleId));
    }

    public String circleId() {
        return (response != null) ? getKey(response.getCircleId()) : null;
    }

    public void setCircleName(final String circleName) {
        this.circleName = Converter.preCheck(circleName);
    }

    public void setMemberId(final String memberId) {
        this.memberId = getId(Converter.preCheck(memberId));
    }

    public void setCircleKey(final String circleKey) {
        this.circleKey = Converter.preCheck(circleKey);
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final ProcessCircleRequest request = prepareRequest(ProcessCircleRequest.class);
        request.setAction(action);
        request.setCircleId(circleId);
        request.setCircleName(circleName);
        request.setMemberId(memberId);
        request.setCircleKey(circleKey);

        response = CallManagement.processCircle(requestUrl, request);

        // Ensuring that the internal mapping of Ids with accounts being
        // used is synchronized.
        processId(action, circleId, circleName, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        // Reset internal values
        action = null;
        circleId = null;
        circleName = null;
        memberId = null;
        circleKey = null;
    }
}
