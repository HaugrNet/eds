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

import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.fitnesse.callers.CallManagement;
import io.javadog.cws.fitnesse.utils.Converter;
import java.util.List;

/**
 * <p>FitNesse Fixture for the CWS FetchTrustees feature.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchTrustees extends CwsRequest<FetchTrusteeResponse> {

    private String circleId = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setCircleId(final String circleId) {
        this.circleId = getId(Converter.preCheck(circleId));
    }

    public String trustees() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            final List<Trustee> trustees = response.getTrustees();
            for (int i = 0; i < trustees.size(); i++) {
                final Trustee trustee = trustees.get(i);
                if (i >= 1) {
                    builder.append(", ");
                }

                builder.append("Trustee{memberId='")
                        .append(getKey(trustee.getMemberId()))
                        .append("', circleId='")
                        .append(getKey(trustee.getMemberId()))
                        .append("', trustLevel='")
                        .append(trustee.getTrustLevel())
                        .append("', publicKey='")
                        .append(trustee.getPublicKey())
                        .append("'}");
            }
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
        final FetchTrusteeRequest request = prepareRequest(FetchTrusteeRequest.class);
        request.setCircleId(circleId);

        response = CallManagement.fetchTrustees(requestType, requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.circleId = null;
    }
}
