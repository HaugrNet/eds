/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.core.services;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * <p>REST interface for the Signature functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_SIGNATURES_BASE)
@Tag(name = "Signatures", description = "Operations for signing documents and verifying signatures.")
public class SignatureService {

    private final ShareBean shareBean;
    private final Settings settings;

    public SignatureService() {
        this(null);
    }

    @Inject
    public SignatureService(final ShareBean shareBean) {
        this.shareBean = shareBean;
        this.settings = Settings.getInstance();
    }

    /**
     * The REST Sign Document Endpoint.
     *
     * @param signDocumentRequest Sign Document Request
     * @return Sign Document Response
     */
    @Operation(
            summary = "Sign document",
            description = "Signs a Document using the requesting Account's Private Key. The Signature is returned in the Response Object. " +
                    "The Signature itself is not stored, but a cryptographic fingerprint is saved to enable later verification.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_SIGNATURES_SIGN)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response sign(@NotNull final SignRequest signDocumentRequest) {
        return CommonService.runRequest(settings, () -> shareBean.sign(signDocumentRequest), Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_SIGN);
    }

    /**
     * The REST Verify Signature Endpoint.
     *
     * @param verifySignatureRequest Verify Signature Request
     * @return Verify Signature Response
     */
    @Operation(
            summary = "Verify signature",
            description = "Verifies a Document with a given Signature. If the Signature is correct, the Response Object will contain a True value; otherwise False.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_SIGNATURES_VERIFY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response verify(@NotNull final VerifyRequest verifySignatureRequest) {
        return CommonService.runRequest(settings, () -> shareBean.verify(verifySignatureRequest), Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_VERIFY);
    }

    /**
     * The REST Fetch Signatures Endpoint.
     *
     * @param fetchSignaturesRequest Fetch Signatures Request
     * @return Fetch Signatures Response
     */
    @Operation(
            summary = "Fetch signatures",
            description = "Retrieves a list of Signatures from the requesting Member Account. The list contains information about the number of successful verifications made with each Signature.")
    @APIResponse(responseCode = "200", description = "Successful operation")
    @POST
    @Path(Constants.REST_SIGNATURES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchSignatureRequest fetchSignaturesRequest) {
        return CommonService.runRequest(settings, () -> shareBean.fetchSignatures(fetchSignaturesRequest), Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_FETCH);
    }
}
