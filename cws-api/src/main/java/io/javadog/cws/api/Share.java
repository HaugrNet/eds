/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
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
 * This interface contain the functionality to deal with data, dataTypes as
 * well as signing and verifying data.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public interface Share {

    /**
     * <p>All stored data must have a data type, i.e. a way for an external
     * client or system to identify and apply rules for how to work with the
     * data. By default, two data types exist; &quot;<b>data</b>&quot; &amp;
     * &quot;<b>folder</b>&quot;, which will allow any system to operate
     * without adding additional types.</p>
     *
     * <p>If more data types are needed, i.e. MIME Type data or Object Creation
     * information - then these must be added before they can be used, which is
     * done using this request.</p>
     *
     * <p>The request takes both the name of a Data Type (typeName) and the
     * type (type) itself, but to properly identify what action should be taken
     * (added/updated or deleted), the proper
     * {@link io.javadog.cws.api.common.Action} must also be set.</p>
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
     * <p>As the data types are generally available, it is only allowed for
     * Circle Administrators or the System Administrator to create new, update
     * or delete existing.</p>
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
     * <p>Allow adding new Folders or Data records as well as update or remove
     * existing records. The data provided will be encrypted for the specified
     * Circle and stored encrypted in the underlying database.</p>
     *
     * <p>The Request supports the following Actions:</p>
     * <ul>
     *   <li><b>{@link io.javadog.cws.api.common.Action#ADD}</b> a new, not
     *   existing record to the system with a given name. The name must be
     *   unique in the Folder where the data is stored. If no type is specified,
     *   then by default the &quot;data&quot; type is used, which is a general
     *   purpose type, indicating that the requesting system must have details
     *   about the actual data type.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#UPDATE}</b> an existing
     *   record. It is possible to replace the encrypted data, rename the Data
     *   Object and move Data Objects between different Folders belonging to the
     *   same Circle. It is not possible to move Folders, as this will break the
     *   underlying data model since it can lead to recursive data structures,
     *   folders can thus only be renamed.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#COPY}</b> an existing
     *   record from one Circle to a second Circle, requires that the requesting
     *   member have write access in both Circles.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#MOVE}</b> an existing
     *   record from one Circle to a second Circle, requires that the requesting
     *   member have write access in both Circles.</li>
     *   <li><b>{@link io.javadog.cws.api.common.Action#DELETE}</b> an existing
     *   Data Object or an existing, empty, folder. It is not possible to delete
     *   folders with Data.</li>
     * </ul>
     *
     * @param request Request Object
     * @return Response Object with ReturnCode and Message
     */
    ProcessDataResponse processData(ProcessDataRequest request);

    /**
     * <p>Allow retrieving data for a specific Circle from the System. Unless a
     * specific Data Object is requested, the list of returned information will
     * only be the Metadata for the Data Object. If a specific Data Object is
     * requested, then the listing will contain a single entry with the Metadata
     * for the Object, and the data is set in the Response Object as well.</p>
     *
     * <p>By default, the content retrieved is a list of data for the root
     * folder for the given Circle. If a specific Folder is given, then the list
     * will be the content of the provided folder. The sorting of the content
     * is always with the most recent data first. It is possible to specify how
     * many Metadata Objects should be returned, and also which page number to
     * read from.</p>
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
