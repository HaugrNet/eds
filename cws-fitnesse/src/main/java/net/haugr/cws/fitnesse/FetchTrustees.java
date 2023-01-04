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

import net.haugr.cws.api.dtos.Trustee;
import net.haugr.cws.api.requests.FetchTrusteeRequest;
import net.haugr.cws.api.responses.FetchTrusteeResponse;
import java.util.List;
import net.haugr.cws.fitnesse.callers.CallManagement;
import net.haugr.cws.fitnesse.utils.Converter;

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
                        .append("accountName='")
                        .append(trustee.getAccountName())
                        .append("', circleId='")
                        .append(getKey(trustee.getCircleId()))
                        .append("', circleName='")
                        .append(getKey(trustee.getCircleName()))
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

        response = CallManagement.fetchTrustees(requestUrl, request);
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
