/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2019 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.requests;

import io.javadog.cws.api.common.Action;

/**
 * <p>Processing Requests, which can perform one or more Actions must implement
 * this Interface.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public interface ActionRequest {

    /**
     * <p>Defines the action which should be performed by the request.</p>
     *
     * @param action The Action to be performed
     */
    void setAction(Action action);

    /**
     * <p>Retrieves the Action for the current request.</p>
     *
     * @return The Action the request should perform
     */
    Action getAction();
}
