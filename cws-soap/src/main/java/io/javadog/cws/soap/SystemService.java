/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-soap)
 * =============================================================================
 */
package io.javadog.cws.soap;

import io.javadog.cws.api.System;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.api.responses.ProcessMemberResponse;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.api.responses.VersionResponse;
import io.javadog.cws.common.Settings;

import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.BindingType;
import java.util.logging.Logger;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@SOAPBinding
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING)
@WebService(name = "system", targetNamespace = "http://ws.cws.javadog.io/", serviceName = "system", portName = "system")
public class SystemService implements System {

    private static final Logger log = Logger.getLogger(SystemService.class.getName());

    private static final String GENERAL_RETURN_MESSAGE = "An unknown error occurred. Please consult the CWS System Log.";

    @Inject private SystemBean bean;

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public VersionResponse version() {
        VersionResponse response;

        try {
            response = bean.version();
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new VersionResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public SettingResponse settings(@WebParam(name = "request") final SettingRequest request) {
        SettingResponse response;

        try {
            response = bean.settings(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new SettingResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public FetchMemberResponse fetchMembers(@WebParam(name = "request") final FetchMemberRequest request) {
        FetchMemberResponse response;

        try {
            response = bean.fetchMembers(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new FetchMemberResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public ProcessMemberResponse processMember(@WebParam(name = "request") final ProcessMemberRequest request) {
        ProcessMemberResponse response;

        try {
            response = bean.processMember(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new ProcessMemberResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public FetchCircleResponse fetchCircles(@WebParam(name = "request") final FetchCircleRequest request) {
        FetchCircleResponse response;

        try {
            response = bean.fetchCircles(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new FetchCircleResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public ProcessCircleResponse processCircle(@WebParam(name = "request") final ProcessCircleRequest request) {
        ProcessCircleResponse response;

        try {
            response = bean.processCircle(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new ProcessCircleResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }
}
