/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMembers extends CwsRequest<FetchMemberResponse> {

    private String memberId = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    public String circles() {
        return getCircleNames();
    }

    public String members() {
        return getMemberNames();
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final FetchMemberRequest request = prepareRequest(FetchMemberRequest.class);
        request.setMemberId(memberId);

        response = CallManagement.fetchMembers(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.memberId = null;
    }
}
