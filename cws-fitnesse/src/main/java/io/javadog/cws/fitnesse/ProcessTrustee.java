/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
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
        this.circleId = getCircleId(circle);
    }

    public void setMember(final String member) {
        this.memberId = getMemberId(member);
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
