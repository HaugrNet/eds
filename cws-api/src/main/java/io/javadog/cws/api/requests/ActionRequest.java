/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
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
