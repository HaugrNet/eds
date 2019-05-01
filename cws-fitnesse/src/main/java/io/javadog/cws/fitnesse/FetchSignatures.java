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

import io.javadog.cws.api.dtos.Signature;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.fitnesse.callers.CallShare;
import io.javadog.cws.fitnesse.utils.Converter;

import java.util.List;
import java.util.Map;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchSignatures extends CwsRequest<FetchSignatureResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String signatures() {
        final StringBuilder builder = new StringBuilder("[");
        if (response != null) {
            final List<Signature> signature = response.getSignatures();
            addSignatureInfo(builder, signature);
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
        final FetchSignatureRequest request = prepareRequest(FetchSignatureRequest.class);

        response = CallShare.fetchSignatures(requestType, requestUrl, request);
    }

    private static void addSignatureInfo(final StringBuilder builder, final List<Signature> signatures) {
        final Map<String, String> checksums = checksums();
        for (int i = 0; i < signatures.size(); i++) {
            final Signature signature = signatures.get(i);
            if (i >= 1) {
                builder.append(", ");
            }
            builder.append("Signature{signature='")
                    .append(checksums.get(signature.getChecksum()))
                    .append("', expires='")
                    .append(Converter.convertDate(signature.getExpires()))
                    .append("', verifications='")
                    .append(signature.getVerifications())
                    .append("'}");
        }
    }
}
