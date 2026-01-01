/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.exceptions;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;

import java.io.Serial;

/**
 * <p>Authentication Exception, thrown if a Member is not permitted to perform
 * a requested Action.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class AuthorizationException extends EDSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public AuthorizationException(final String message) {
        super(ReturnCode.AUTHORIZATION_WARNING, message);
    }
}
