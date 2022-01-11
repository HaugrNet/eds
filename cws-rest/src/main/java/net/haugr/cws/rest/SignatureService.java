/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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
package net.haugr.cws.rest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.requests.FetchSignatureRequest;
import net.haugr.cws.api.requests.SignRequest;
import net.haugr.cws.api.requests.VerifyRequest;
import net.haugr.cws.core.ShareBean;
import net.haugr.cws.core.model.Settings;

/**
 * <p>REST interface for the Signature functionality.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
@Path(Constants.REST_SIGNATURES_BASE)
public class SignatureService {

    private static final String FETCH_METHOD = "fetchSignatures";
    private static final String VERIFY_METHOD = "verify";
    private static final String SIGN_METHOD = "sign";

    @Inject
    private ShareBean bean;
    private final Settings settings = Settings.getInstance();

    @POST
    @Path(Constants.REST_SIGNATURES_SIGN)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response sign(@NotNull final SignRequest signDocumentRequest) {
        return CommonService.runRequest(settings, bean, SIGN_METHOD, signDocumentRequest, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_SIGN);
    }

    @POST
    @Path(Constants.REST_SIGNATURES_VERIFY)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response verify(@NotNull final VerifyRequest verifySignatureRequest) {
        return CommonService.runRequest(settings, bean, VERIFY_METHOD, verifySignatureRequest, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_VERIFY);
    }

    @POST
    @Path(Constants.REST_SIGNATURES_FETCH)
    @Consumes(CommonService.CONSUMES)
    @Produces(CommonService.PRODUCES)
    public Response fetch(@NotNull final FetchSignatureRequest fetchSignaturesRequest) {
        return CommonService.runRequest(settings, bean, FETCH_METHOD, fetchSignaturesRequest, Constants.REST_SIGNATURES_BASE + Constants.REST_SIGNATURES_FETCH);
    }
}
