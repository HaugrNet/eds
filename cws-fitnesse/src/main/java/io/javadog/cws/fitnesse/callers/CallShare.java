/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
package io.javadog.cws.fitnesse.callers;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.client.rest.ShareRestClient;

/**
 * <p>CWS Share invocation class.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class CallShare {

    private static Share share = null;

    private CallShare() {
        // Private Constructor, this is a utility Class.
    }

    private static void prepareCWS(final String url) {
        if (share == null) {
            share = new ShareRestClient(url);
        }
    }

    // =========================================================================
    // Share Interface Functionality
    // =========================================================================

    public static ProcessDataTypeResponse processDataType(final String url, final ProcessDataTypeRequest request) {
        prepareCWS(url);
        return share.processDataType(request);
    }

    public static FetchDataTypeResponse fetchDataTypes(final String url, final FetchDataTypeRequest request) {
        prepareCWS(url);
        return share.fetchDataTypes(request);
    }

    public static ProcessDataResponse processData(final String url, final ProcessDataRequest request) {
        prepareCWS(url);
        return share.processData(request);
    }

    public static FetchDataResponse fetchData(final String url, final FetchDataRequest request) {
        prepareCWS(url);
        return share.fetchData(request);
    }

    public static SignResponse sign(final String url, final SignRequest request) {
        prepareCWS(url);
        return share.sign(request);
    }

    public static VerifyResponse verify(final String url, final VerifyRequest request) {
        prepareCWS(url);
        return share.verify(request);
    }

    public static FetchSignatureResponse fetchSignatures(final String url, final FetchSignatureRequest request) {
        prepareCWS(url);
        return share.fetchSignatures(request);
    }
}
