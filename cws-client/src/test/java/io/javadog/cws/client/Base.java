/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-client)
 * =============================================================================
 */
package io.javadog.cws.client;

import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.Authentication;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Base {

    public static final String URL = "http://localhost:8080/cws";

    private Base() {
        // Private Constructor, this is a Utility Class.
    }

    public static <A extends Authentication> A prepareRequest(final Class<A> clazz, final String account) {
        try {
            final A request = clazz.getConstructor().newInstance();

            request.setAccountName(account);
            request.setCredential(account.getBytes(Charset.forName("UTF-8")));
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSClientException("Cannot instantiate Request Object", e);
        }
    }
}
