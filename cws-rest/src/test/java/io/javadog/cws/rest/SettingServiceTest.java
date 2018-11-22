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

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
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
public final class SettingServiceTest extends DatabaseSetup {

    @Test
    public void testSettings() {
        final SettingService service = prepareService();
        final SettingRequest request = new SettingRequest();

        final Response response = service.settings(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedSettings() {
        final SettingService service = prepareFlawedService();
        final SettingRequest request = new SettingRequest();

        final Response response = service.settings(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static SettingService prepareFlawedService() {
        try {

            final SettingService service = SettingService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SettingService prepareService() {
        try {
            final ManagementBean bean = ManagementBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final SettingService service = SettingService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }
}
