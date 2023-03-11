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
package net.haugr.eds.api.requests;

import net.haugr.eds.api.Management;
import net.haugr.eds.api.common.Constants;

/**
 * <p>When the Management Request 'fetchCircles' is invoked, it requires a
 * Request Object, with the Authentication information.</p>
 *
 * <p>For more details, please see the 'fetchCircles' request in the Management
 * interface: {@link Management#fetchCircles(FetchCircleRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchCircleRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;
}
