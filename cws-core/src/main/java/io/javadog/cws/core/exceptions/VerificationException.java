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
package io.javadog.cws.core.exceptions;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;

/**
 * <p>Verification Exception, thrown if the Request Object provided was either
 * null or invalid, meaning that the request cannot be correctly processed.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class VerificationException extends CWSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    public VerificationException(final String message) {
        super(ReturnCode.VERIFICATION_WARNING, message);
    }
}
