/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse;

import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.fitnesse.exceptions.StopTestException;
import io.javadog.cws.fitnesse.utils.Converter;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class CwsRequest<R extends CwsResponse> {

    protected String accountName = null;
    protected byte[] credential = null;
    protected R response = null;

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public void setCredential(final String credential) {
        this.credential = Converter.convertBytes(credential);
    }

    public String returnCode() {
        return response.getReturnCode().name();
    }

    public String returnMessage() {
        return response.getReturnMessage();
    }

    protected <T extends Authentication> T prepareRequest(final Class<T> clazz) {
        try {
            final T request = clazz.getConstructor().newInstance();
            request.setCredentialType(CredentialType.PASSPHRASE);
            request.setAccountName(accountName);
            request.setCredential(credential);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new StopTestException("Cannot instantiate Request Object", e);
        }
    }

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the execute
     * method, which runs the request. It builds the Request Object and saves
     * the Response Object.
     */
    public abstract void execute();
}
