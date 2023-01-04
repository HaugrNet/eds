/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.api.requests;

import net.haugr.cws.api.common.Action;

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
