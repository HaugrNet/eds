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
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

import java.util.Locale;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessTrustee extends CwsRequest<ProcessTrusteeResponse> {

    private Action action = null;
    private String circleId = null;
    private String memberId = null;
    private TrustLevel trustLevel = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setAction(final String action) {
        this.action = Action.valueOf(action.toUpperCase(Locale.ENGLISH));
    }

    public void setCircle(final String circle) {
        this.circleId = getId(circle);
    }

    public void setMemberId(final String memberId) {
        this.memberId = getId(memberId);
    }

    public void setTrustLevel(final String trustLevel) {
        this.trustLevel = TrustLevel.valueOf(trustLevel.toUpperCase(Locale.ENGLISH));
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class);
        request.setAction(action);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setTrustLevel(trustLevel);

        response = CallManagement.processTrustee(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.action = null;
        this.circleId = null;
        this.memberId = null;
        this.trustLevel = null;
    }
}
