/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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

/**
 * <p>EDS Specific Exception for {@link ReturnCode#ILLEGAL_ACTION}.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public final class IllegalActionException extends EDSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * Business Case Constructor, for simple examples where the logic is not
     * permitting a given action.
     *
     * @param message Error description
     */
    public IllegalActionException(final String message) {
        super(ReturnCode.ILLEGAL_ACTION, message);
    }
}
