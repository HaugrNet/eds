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
import io.javadog.cws.client.ShareSoapClient;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CallShare {

    private static final Share SHARE = new ShareSoapClient("http://localhost:8080/cws");

    private CallShare() {
        // Private Constructor, this is a utility Class.
    }

    // =========================================================================
    // Share Interface Functionality
    // =========================================================================

    public static ProcessDataTypeResponse processDataType(final ProcessDataTypeRequest request) {
        return SHARE.processDataType(request);
    }

    public static FetchDataTypeResponse fetchDataTypes(final FetchDataTypeRequest request) {
        return SHARE.fetchDataTypes(request);
    }

    public static ProcessDataResponse processData(final ProcessDataRequest request) {
        return SHARE.processData(request);
    }

    public static FetchDataResponse fetchData(final FetchDataRequest request) {
        return SHARE.fetchData(request);
    }

    public static SignResponse sign(final SignRequest request) {
        return SHARE.sign(request);
    }

    public static VerifyResponse verify(final VerifyRequest request) {
        return SHARE.verify(request);
    }

    public static FetchSignatureResponse fetchSignatures(final FetchSignatureRequest request) {
        return SHARE.fetchSignatures(request);
    }
}
