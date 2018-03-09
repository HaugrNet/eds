/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
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
