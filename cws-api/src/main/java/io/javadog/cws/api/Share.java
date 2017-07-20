/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api;

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

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface Share {

    /**
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessDataTypeResponse processDataType(ProcessDataTypeRequest request);

    /**
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    FetchDataTypeResponse fetchDataTypes(FetchDataTypeRequest request);

    /**
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessDataResponse processData(ProcessDataRequest request);

    /**
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    FetchDataResponse fetchData(FetchDataRequest request);

    /**
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SignResponse sign(SignRequest request);

    /**
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    VerifyResponse verify(VerifyRequest request);

    FetchSignatureResponse fetchSignatures(FetchSignatureRequest request);
}
