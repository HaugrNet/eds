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

import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.Member;
import net.haugr.eds.api.requests.FetchMemberRequest;
import net.haugr.eds.api.responses.FetchMemberResponse;
import java.util.List;
import net.haugr.eds.fitnesse.callers.CallManagement;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS FetchMembers feature.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchMembers extends EDSRequest<FetchMemberResponse> {

    private String memberId = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setMemberId(final String memberId) {
        this.memberId = getId(Converter.preCheck(memberId));
    }

    public String members() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            final List<Member> members = response.getMembers();
            for (int i = 0; i < members.size(); i++) {
                final Member member = members.get(i);
                if (i >= 1) {
                    builder.append(", ");
                }
                builder.append("Member{memberId='")
                        .append(getKey(member.getMemberId()))
                        .append("', accountName='")
                        .append(member.getAccountName())
                        .append("', memberRole='")
                        .append(member.getMemberRole())
                        .append("', publicKey='")
                        .append(member.getPublicKey())
                        .append("'}");
            }
        }
        builder.append(']');

        return builder.toString();
    }

    public String circles() {
        //noinspection DuplicatedCode
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            final List<Circle> circles = response.getCircles();
            addCircleInfo(builder, circles);
        }
        builder.append(']');

        return builder.toString();
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

        response = CallManagement.fetchMembers(requestUrl, request);
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
