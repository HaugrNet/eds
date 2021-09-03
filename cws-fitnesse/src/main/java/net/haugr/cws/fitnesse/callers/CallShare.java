/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
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
package net.haugr.cws.fitnesse.callers;

import net.haugr.cws.api.Share;
import net.haugr.cws.api.requests.FetchDataRequest;
import net.haugr.cws.api.requests.FetchDataTypeRequest;
import net.haugr.cws.api.requests.FetchSignatureRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.requests.ProcessDataTypeRequest;
import net.haugr.cws.api.requests.SignRequest;
import net.haugr.cws.api.requests.VerifyRequest;
import net.haugr.cws.api.responses.FetchDataResponse;
import net.haugr.cws.api.responses.FetchDataTypeResponse;
import net.haugr.cws.api.responses.FetchSignatureResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.api.responses.ProcessDataTypeResponse;
import net.haugr.cws.api.responses.SignResponse;
import net.haugr.cws.api.responses.VerifyResponse;
import net.haugr.cws.client.rest.ShareRestClient;

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
