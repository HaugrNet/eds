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
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
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
public final class TrusteeServiceTest extends DatabaseSetup {

    @Test
    public void testAdd() {
        final TrusteeService service = prepareService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.add(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedAdd() {
        final TrusteeService service = prepareFlawedService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.add(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testAlter() {
        final TrusteeService service = prepareService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.alter(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedAlter() {
        final TrusteeService service = prepareFlawedService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.alter(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testRemove() {
        final TrusteeService service = prepareService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.remove(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedRemove() {
        final TrusteeService service = prepareFlawedService();
        final ProcessTrusteeRequest request = new ProcessTrusteeRequest();

        final Response response = service.remove(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testFetch() {
        final TrusteeService service = prepareService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedFetch() {
        final TrusteeService service = prepareFlawedService();
        final FetchTrusteeRequest request = new FetchTrusteeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static TrusteeService prepareFlawedService() {
        try {

            final TrusteeService service = TrusteeService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private TrusteeService prepareService() {
        try {
            final ManagementBean bean = ManagementBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final TrusteeService service = TrusteeService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }
}
