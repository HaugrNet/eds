/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.test;

import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.test.utils.Converter;

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

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the execute
     * method, which runs the request. It builds the Request Object and saves
     * the Response Object.
     */
    public abstract void execute();
}
