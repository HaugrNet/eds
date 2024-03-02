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
package net.haugr.eds.rest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.requests.SignRequest;
import net.haugr.eds.api.requests.VerifyRequest;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.model.Settings;

/**
 * <p>REST interface for the Signature functionality.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@Path(Constants.REST_SIGNATURES_BASE)
public class SignatureService {

    private static final String FETCH_METHOD = "fetchSignatures";
    private static final String VERIFY_METHOD = "verify";
    private static final String SIGN_METHOD = "sign";

    @Inject
    private ShareBean bean;
    private final Settings settings = Settings.getInstance();

    /**
     * Default Constructor.
     */
    public SignatureService() {
    }

    /**
     * The REST Sign Document Endpoint.
     *
     * @param signDocumentRequest Sign Document Request
     * @return Sign Document Response
     */
    @POST
    @Path(Constants.REST_SIGNATURES_SIGN)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response sign(@NotNull final SignRequest signDocumentRequest) {
        return CommonService.runRequest(settings, bean, SIGN_METHOD, signDocumentRequest, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_SIGN);
    }

    /**
     * The REST Verify Signature Endpoint.
     *
     * @param verifySignatureRequest Verify Signature Request
     * @return Verify Signature Response
     */
    @POST
    @Path(Constants.REST_SIGNATURES_VERIFY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response verify(@NotNull final VerifyRequest verifySignatureRequest) {
        return CommonService.runRequest(settings, bean, VERIFY_METHOD, verifySignatureRequest, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_VERIFY);
    }

    /**
     * The REST Fetch Signatures Endpoint.
     *
     * @param fetchSignaturesRequest Fetch Signatures Request
     * @return Fetch Signatures Response
     */
    @POST
    @Path(Constants.REST_SIGNATURES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchSignatureRequest fetchSignaturesRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchSignaturesRequest, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_FETCH);
    }
}
