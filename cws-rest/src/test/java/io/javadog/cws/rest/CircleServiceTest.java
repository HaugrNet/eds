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
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
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
public final class CircleServiceTest extends DatabaseSetup {

    @Test
    public void testCreate() {
        final CircleService service = prepareService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.create(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedCreate() {
        final CircleService service = prepareFlawedService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.create(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testUpdatw() {
        final CircleService service = prepareService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.update(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedUpdate() {
        final CircleService service = prepareFlawedService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.update(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testDelete() {
        final CircleService service = prepareService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.delete(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedDelete() {
        final CircleService service = prepareFlawedService();
        final ProcessCircleRequest request = new ProcessCircleRequest();

        final Response response = service.delete(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testFetch() {
        final CircleService service = prepareService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedFetch() {
        final CircleService service = prepareFlawedService();
        final FetchCircleRequest request = new FetchCircleRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static CircleService prepareFlawedService() {
        try {

            final CircleService service = CircleService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private CircleService prepareService() {
        try {
            final ManagementBean bean = ManagementBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final CircleService service = CircleService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }
}
