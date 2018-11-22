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
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class DataTypeServiceTest extends DatabaseSetup {

    @Test
    public void testProcess() {
        final DataTypeService service = prepareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.process(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedProcess() {
        final DataTypeService service = prepareFlawedService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.process(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testDelete() {
        final DataTypeService service = prepareService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.delete(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedDelete() {
        final DataTypeService service = prepareFlawedService();
        final ProcessDataTypeRequest request = new ProcessDataTypeRequest();

        final Response response = service.delete(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    @Test
    public void testFetch() {
        final DataTypeService service = prepareService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.VERIFICATION_WARNING.getHttpCode()));
    }

    @Test
    public void testFlawedFetch() {
        final DataTypeService service = prepareFlawedService();
        final FetchDataTypeRequest request = new FetchDataTypeRequest();

        final Response response = service.fetch(request);
        assertThat(response.getStatus(), is(ReturnCode.ERROR.getHttpCode()));
    }

    // =========================================================================
    // Internal Test Setup Methods
    // =========================================================================

    private static DataTypeService prepareFlawedService() {
        try {

            final DataTypeService service = DataTypeService.class.getConstructor().newInstance();
            setField(service, "bean", null);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private DataTypeService prepareService() {
        try {
            final ShareBean bean = ShareBean.class.getConstructor().newInstance();
            setField(bean, "entityManager", entityManager);

            final DataTypeService service = DataTypeService.class.getConstructor().newInstance();
            setField(service, "bean", bean);

            return service;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }
}
