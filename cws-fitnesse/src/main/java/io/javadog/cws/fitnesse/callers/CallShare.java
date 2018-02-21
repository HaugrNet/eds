/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-test)
 * =============================================================================
 */
package io.javadog.cws.fitnesse.callers;

import io.javadog.cws.api.Share;
import io.javadog.cws.api.requests.SignRequest;
import io.javadog.cws.api.requests.VerifyRequest;
import io.javadog.cws.api.responses.SignResponse;
import io.javadog.cws.api.responses.VerifyResponse;
import io.javadog.cws.client.ShareSoapClient;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CallShare {

    private static final Share SHARE = new ShareSoapClient("http://localhost:8080/cws");

    public static SignResponse sign(final SignRequest request) {
        return SHARE.sign(request);
    }

    public static VerifyResponse verify(final VerifyRequest request) {
        return SHARE.verify(request);
    }
}
