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
 * <p>Crypto Exception, thrown if an error occurred from invoking the JCE logic
 * or in the Crypto components.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class CryptoException extends EDSException {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /**
     * Business Case Constructor, for simple examples where the logic is not
     * permitting a given action.
     *
     * @param message Error description
     */
    public CryptoException(final String message) {
        super(ReturnCode.CRYPTO_ERROR, message);
    }

    /**
     * To prevent that too much information about the underlying system is
     * exposed via the error text, the Cause must be separate from the message.
     * Hence, even for trivial problems, we're not allowing the Cause to stand
     * alone.
     *
     * @param message Error description
     * @param cause   Cause of the error
     */
    public CryptoException(final String message, final Throwable cause) {
        super(ReturnCode.CRYPTO_ERROR, message, cause);
    }
}
