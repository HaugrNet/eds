/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-soap)
 * =============================================================================
 */
package io.javadoc.cws.soap;

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
@WebService(name = "share", targetNamespace = "http://ws.cws.javadog.io/", serviceName = "share", portName = "share")
public class ShareService implements Share {

    private static final Logger log = Logger.getLogger(ShareService.class.getName());

    private static final String GENERAL_RETURN_MESSAGE = "An unknown error occurred. Please consult the CWS System Log.";

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
            response = bean.processDataType(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
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
            response = bean.fetchDataTypes(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
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
            response = bean.processData(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
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
            response = bean.fetchData(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
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
            response = bean.sign(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
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
            response = bean.verify(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
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
            response = bean.fetchSignatures(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.log(Settings.ERROR, e.getMessage(), e);
            response = new FetchSignatureResponse(ReturnCode.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }
}
