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
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
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
@Path("/data")
public class DataService {

    private static final Logger log = Logger.getLogger(DataService.class.getName());

    @Inject private SettingBean settings;
    @Inject private ShareBean bean;

    @POST
    @Path("/addData")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response add(@NotNull final ProcessDataRequest addDataRequest) {
        final Long startTime = System.nanoTime();
        ProcessDataResponse response;

        try {
            addDataRequest.setAction(Action.ADD);
            response = bean.processData(addDataRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "addData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "addData", startTime, e));
            response = new ProcessDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/updateData")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response update(@NotNull final ProcessDataRequest updateDataRequest) {
        final Long startTime = System.nanoTime();
        ProcessDataResponse response;

        try {
            updateDataRequest.setAction(Action.UPDATE);
            response = bean.processData(updateDataRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "updateData", startTime, e));
            response = new ProcessDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @DELETE
    @Path("/deleteData")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response delete(@NotNull final ProcessDataRequest deleteDataRequest) {
        final Long startTime = System.nanoTime();
        ProcessDataResponse response;

        try {
            deleteDataRequest.setAction(Action.DELETE);
            response = bean.processData(deleteDataRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "deleteData", startTime, e));
            response = new ProcessDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }

    @POST
    @Path("/fetchData")
    @Consumes(CwsApplication.CONSUMES)
    @Produces(CwsApplication.PRODUCES)
    public Response fetch(@NotNull final FetchDataRequest fetchDataRequest) {
        final Long startTime = System.nanoTime();
        FetchDataResponse response;

        try {
            response = bean.fetchData(fetchDataRequest);
            log.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchData", startTime));
        } catch (RuntimeException e) {
            log.log(Settings.ERROR, () -> LoggingUtil.requestDuration(settings.getSettings().getLocale(), "fetchData", startTime, e));
            response = new FetchDataResponse(ReturnCode.ERROR, e.getMessage());
        }

        return CwsApplication.buildResponse(response);
    }
}
