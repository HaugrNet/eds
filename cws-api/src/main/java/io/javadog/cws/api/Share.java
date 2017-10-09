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
     * Signs a Document using the requesting Accounts Private Key, and returning
     * the Signature in the Response Object. The Signature is not stored in the
     * underlying data model, rather a cryptographic fingerprint is saved to
     * make sure that it can be retrieved for someone wishing to verify the
     * document against the signature.
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    SignResponse sign(SignRequest request);

    /**
     * Verifies a Document with a given Signature. If the Signature is correct,
     * then the Response Object will contain a True value, otherwise a False
     * value.
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    VerifyResponse verify(VerifyRequest request);

    /**
     * Retrieve a list of Signatures from the requesting Member Account. The
     * list contain information about number of successful verifications which
     * has been made with each Signature.
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    FetchSignatureResponse fetchSignatures(FetchSignatureRequest request);
}
