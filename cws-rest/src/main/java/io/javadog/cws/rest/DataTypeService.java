/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-rest)
 * =============================================================================
 */
package io.javadog.cws.rest;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.core.SettingBean;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@Path("/dataTypes")
public class DataTypeService {

    private static final Logger log = Logger.getLogger(DataTypeService.class.getName());

    @Inject private SettingBean settings;
    @Inject private ShareBean bean;

    @POST
    @Path("/processDataType")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response process(@NotNull final ProcessDataTypeRequest processDataTypeRequest) {
        ProcessDataTypeResponse processDataTypeResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            processDataTypeRequest.setAction(Action.PROCESS);
            processDataTypeResponse = bean.processDataType(processDataTypeRequest);
            returnCode = processDataTypeResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "processDataType", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(processDataTypeResponse).build();
    }

    @POST
    @DELETE
    @Path("/deleteDataType")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response delete(@NotNull final ProcessDataTypeRequest deleteDataTypeRequest) {
        ProcessDataTypeResponse deleteDataTypeResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            deleteDataTypeRequest.setAction(Action.DELETE);
            deleteDataTypeResponse = bean.processDataType(deleteDataTypeRequest);
            returnCode = deleteDataTypeResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteDataType", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(deleteDataTypeResponse).build();
    }

    @POST
    @Path("/fetchDataTypes")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response fetch(@NotNull final FetchDataTypeRequest fetchDataTypesRequest) {
        FetchDataTypeResponse fetchDataTypesResponse = null;
        ReturnCode returnCode = ReturnCode.ERROR;

        try {
            final Long startTime = System.nanoTime();
            fetchDataTypesResponse = bean.fetchDataTypes(fetchDataTypesRequest);
            returnCode = fetchDataTypesResponse.getReturnCode();
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchDataTypes", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, e.getMessage(), e);
        }

        return Response.status(returnCode.getHttpCode()).entity(fetchDataTypesResponse).build();
    }
}
