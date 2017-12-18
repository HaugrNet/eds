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
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response process(@NotNull final ProcessDataTypeRequest processDataTypeRequest) {
        final Long startTime = System.nanoTime();
        ProcessDataTypeResponse response;

        try {
            processDataTypeRequest.setAction(Action.PROCESS);
            response = bean.processDataType(processDataTypeRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "processDataType", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "processDataType", startTime, e));
            response = new ProcessDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @DELETE
    @Path("/deleteDataType")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response delete(@NotNull final ProcessDataTypeRequest deleteDataTypeRequest) {
        final Long startTime = System.nanoTime();
        ProcessDataTypeResponse response;

        try {
            deleteDataTypeRequest.setAction(Action.DELETE);
            response = bean.processDataType(deleteDataTypeRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteDataType", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteDataType", startTime, e));
            response = new ProcessDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/fetchDataTypes")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response fetch(@NotNull final FetchDataTypeRequest fetchDataTypesRequest) {
        final Long startTime = System.nanoTime();
        FetchDataTypeResponse response;

        try {
            response = bean.fetchDataTypes(fetchDataTypesRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchDataTypes", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchDataTypes", startTime, e));
            response = new FetchDataTypeResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }
}
