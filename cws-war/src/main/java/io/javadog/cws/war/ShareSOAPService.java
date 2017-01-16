package io.javadog.cws.war;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.requests.FetchFolderRequest;
import io.javadog.cws.api.requests.FetchObjectRequest;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.requests.ProcessFolderRequest;
import io.javadog.cws.api.requests.ProcessObjectRequest;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.FetchFolderResponse;
import io.javadog.cws.api.responses.FetchObjectResponse;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.api.responses.ProcessFolderResponse;
import io.javadog.cws.api.responses.ProcessObjectResponse;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.BindingType;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING)
@WebService(name = "shareWS", serviceName = "shareWSService", portName = "shareWS", targetNamespace = "http://ws.cws.javadog.io/")
public final class ShareSOAPService implements Share {

    private static final Logger log = LoggerFactory.getLogger(ShareSOAPService.class);

    private static final String GENERAL_RETURN_MESSAGE = "An unknown error occurred. Please consult the CWS System Log.";

    @Inject private ShareBean bean;

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    public ProcessFolderResponse processFolder(final ProcessFolderRequest request) {
        ProcessFolderResponse response;

        try {
            response = bean.processFolder(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.error(e.getMessage(), e);
            response = new ProcessFolderResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    public FetchFolderResponse fetchFolder(final FetchFolderRequest request) {
        FetchFolderResponse response;

        try {
            response = bean.fetchFolder(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.error(e.getMessage(), e);
            response = new FetchFolderResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    public ProcessObjectTypeResponse processObjectType(final ProcessObjectTypeRequest request) {
        ProcessObjectTypeResponse response;

        try {
            response = bean.processObjectType(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.error(e.getMessage(), e);
            response = new ProcessObjectTypeResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    public FetchObjectTypeResponse fetchObjectTypes(final FetchObjectTypeRequest request) {
        FetchObjectTypeResponse response;

        try {
            response = bean.fetchObjectTypes(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.error(e.getMessage(), e);
            response = new FetchObjectTypeResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    public ProcessObjectResponse processObject(final ProcessObjectRequest request) {
        ProcessObjectResponse response;

        try {
            response = bean.processObject(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.error(e.getMessage(), e);
            response = new ProcessObjectResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WebMethod
    public FetchObjectResponse fetchObject(final FetchObjectRequest request) {
        FetchObjectResponse response;

        try {
            response = bean.fetchObject(request);
        } catch (RuntimeException e) {
            // If an error occurs that has so far not been resolved, this is the
            // final level where it can be handled. Errors can be Persistence
            // problems or other things that will affect the reliability and/or
            // performance of the system.
            log.error(e.getMessage(), e);
            response = new FetchObjectResponse(Constants.ERROR, GENERAL_RETURN_MESSAGE);
        }

        return response;
    }
}
