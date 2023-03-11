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
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import java.util.Locale;
import net.haugr.eds.fitnesse.callers.CallManagement;

/**
 * <p>FitNesse Fixture for the EDS ProcessTrustee feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessTrustee extends EDSRequest<ProcessTrusteeResponse> {

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

    public void setCircleId(final String circleId) {
        this.circleId = getId(circleId);
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

        response = CallManagement.processTrustee(requestUrl, request);
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
