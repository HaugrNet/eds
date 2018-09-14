/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.api.responses.MasterKeyResponse;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * <p>REST interface for the MasterKey functionality.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path(Constants.REST_MASTERKEY)
public class MasterKeyService {

    private static final Logger LOG = Logger.getLogger(MasterKeyService.class.getName());

    private final Settings settings = Settings.getInstance();
    @Inject private ManagementBean bean;

    @POST
    @Consumes(RestUtils.CONSUMES)
    @Produces(RestUtils.PRODUCES)
    public Response masterKey(@NotNull final MasterKeyRequest masterKeyRequest) {
        final long startTime = System.nanoTime();
        MasterKeyResponse response;

        try {
            response = bean.masterKey(masterKeyRequest);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), Constants.REST_MASTERKEY, startTime));
        } catch (RuntimeException e) {
            LOG.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getLocale(), Constants.REST_MASTERKEY, startTime, e));
            response = new MasterKeyResponse(ReturnCode.ERROR, e.getMessage());
        }

        return RestUtils.buildResponse(response);
    }
}
