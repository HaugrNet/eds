/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.MasterKeyRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MasterKeyServiceTest extends DatabaseSetup {

    @Test
    public void testMasterKey() {
        final MasterKeyService service = prepareService();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(Constants.ADMIN_ACCOUNT.getBytes(settings.getCharset()));

        final Response response = service.masterKey(request);
        assertThat(response.getStatus(), is(ReturnCode.SUCCESS.getHttpCode()));
    }

    @Test
    public void testFlawedMasterKey() {
        final MasterKeyService service = prepareFlawedService();
        final MasterKeyRequest request = prepareRequest(MasterKeyRequest.class, Constants.ADMIN_ACCOUNT);
        request.setSecret(Constants.ADMIN_ACCOUNT.getBytes(settings.getCharset()));

        final Response response = service.masterKey(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static MasterKeyService prepareFlawedService() {
        try {
            final MasterKeyService service = MasterKeyService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private MasterKeyService prepareService() {
        try {
            final ManagementBean bean = ManagementBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final MasterKeyService service = MasterKeyService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }
}
