/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.fitnesse.callers;

import net.haugr.eds.api.Share;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.api.responses.FetchSignatureResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.api.responses.SignResponse;
import net.haugr.eds.api.responses.VerifyResponse;
import net.haugr.eds.client.rest.ShareRestClient;

/**
 * <p>EDS Share invocation class.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class CallShare {

    private static Share share = null;

    private CallShare() {
        // Private Constructor, this is a utility Class.
    }

    private static void prepareEDS(final String url) {
        if (share == null) {
            share = new ShareRestClient(url);
        }
    }

    // =========================================================================
    // Share Interface Functionality
    // =========================================================================

    public static ProcessDataTypeResponse processDataType(final String url, final ProcessDataTypeRequest request) {
        prepareEDS(url);
        return share.processDataType(request);
    }

    public static FetchDataTypeResponse fetchDataTypes(final String url, final FetchDataTypeRequest request) {
        prepareEDS(url);
        return share.fetchDataTypes(request);
    }

    public static ProcessDataResponse processData(final String url, final ProcessDataRequest request) {
        prepareEDS(url);
        return share.processData(request);
    }

    public static FetchDataResponse fetchData(final String url, final FetchDataRequest request) {
        prepareEDS(url);
        return share.fetchData(request);
    }

    public static SignResponse sign(final String url, final SignRequest request) {
        prepareEDS(url);
        return share.sign(request);
    }

    public static VerifyResponse verify(final String url, final VerifyRequest request) {
        prepareEDS(url);
        return share.verify(request);
    }

    public static FetchSignatureResponse fetchSignatures(final String url, final FetchSignatureRequest request) {
        prepareEDS(url);
        return share.fetchSignatures(request);
    }
}
