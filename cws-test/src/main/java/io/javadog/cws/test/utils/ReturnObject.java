/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.test.utils;

import io.javadog.cws.api.responses.CwsResponse;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public abstract class ReturnObject<R extends CwsResponse> {

    protected String accountName = null;
    protected byte[] credential = null;
    protected R response = null;

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public void setCredential(final String credential) {
        this.credential = Converter.convertBytes(credential);
    }

    public String getReturnCode() {
        return response.getReturnCode().name();
    }

    public String getReturnMessage() {
        return response.getReturnMessage();
    }

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the execute
     * method, which runs the request. It builds the Request Object and saves
     * the Response Object.
     */
    public abstract void execute();
}
