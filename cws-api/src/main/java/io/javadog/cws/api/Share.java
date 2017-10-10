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
     * <p>All stored data must have a data type, i.e. a way for an external
     * client or system to identify and apply rules for how to work with the
     * data. By default, two data types exist; &quot;data&quot; &amp;
     * &quot;folder&quot;, which will allow any system to operate without
     * adding additional types.</p>
     *
     * <p>However, if additional types is needed, which can be MIME Type
     * information for file sharing between multiple Clients, or Object
     * Identification information for Data Objects, then these must be added
     * before they can be used.</p>
     *
     * <p>The request takes both the name of a Data Type and the type itself,
     * but to properly identify what action should be taken (added/updated or
     * deleted), the proper {@link io.javadog.cws.api.common.Action} must also
     * be set.</p>
     *
     * <p>Allowed Actions include the following:</p>
     * <ul>
     *   <li><b>{@link io.javadog.cws.api.common.Action#PROCESS}</b> to either
     *   add or update an existing Data type, note that the two default types
     *   cannot be updated.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#DELETE}</b> to remove
     *   an unused Data Type from the system. If a Type is still being used,
     *   then it cannot be removed. The two default types also cannot be
     *   removed.</li>
     * </ul>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessDataTypeResponse processDataType(ProcessDataTypeRequest request);

    /**
     * <p>This request will retrieve a list of all currently available Data
     * Types, which can be used to either add/update data to the system or
     * identify how existing data should be processed.</p>
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
