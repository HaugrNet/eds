/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.soap;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.core.ShareBean;
import io.javadog.cws.core.misc.LoggingUtil;
import io.javadog.cws.core.model.Settings;

import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.MTOM;
import java.util.logging.Logger;

/**
 * <p>This is the CWS SOAP based WebServices class for the Share interface.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@SOAPBinding
@MTOM(threshold = 4096)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_MTOM_BINDING)
@WebService(name = "share", targetNamespace = "http://ws.cws.javadog.io/", serviceName = "share", portName = "share")
public class ShareService implements Share {

    private static final Logger LOG = Logger.getLogger(ShareService.class.getName());

    private static final String GENERAL_RETURN_MESSAGE = "An unknown error occurred. Please consult the CWS System Log.";

    private final Settings settings = Settings.getInstance();
    @Inject private ShareBean bean;

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public ProcessDataTypeResponse processDataType(@WebParam(name = "request") final ProcessDataTypeRequest request) {
        ProcessDataTypeResponse response;

        try {
            final long startTime = System.nanoTime();
            response = bean.processDataType(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "processDataType", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new ProcessDataTypeResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public FetchDataTypeResponse fetchDataTypes(@WebParam(name = "request") final FetchDataTypeRequest request) {
        FetchDataTypeResponse response;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchDataTypes(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchDataTypes", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new FetchDataTypeResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public ProcessDataResponse processData(@WebParam(name = "request") final ProcessDataRequest request) {
        ProcessDataResponse response;

        try {
            final Long startTime = System.nanoTime();
            response = bean.processData(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "processData", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new ProcessDataResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public FetchDataResponse fetchData(@WebParam(name = "request") final FetchDataRequest request) {
        FetchDataResponse response;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchData(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchData", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new FetchDataResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public SignResponse sign(@WebParam(name = "request") final SignRequest request) {
        SignResponse response;

        try {
            final Long startTime = System.nanoTime();
            response = bean.sign(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "signDocument", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new SignResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public VerifyResponse verify(@WebParam(name = "request") final VerifyRequest request) {
        VerifyResponse response;

        try {
            final Long startTime = System.nanoTime();
            response = bean.verify(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "verifySignatures", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new VerifyResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    @WebResult(name = "response")
    public FetchSignatureResponse fetchSignatures(@WebParam(name = "request") final FetchSignatureRequest request) {
        FetchSignatureResponse response;

        try {
            final Long startTime = System.nanoTime();
            response = bean.fetchSignatures(request);
            LOG.log(Settings.INFO, () -> LoggingUtil.requestDuration(settings.getLocale(), "fetchSignatures", startTime));
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            LOG.log(Settings.ERROR, e.getMessage(), e);
            response = new FetchSignatureResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }
}
