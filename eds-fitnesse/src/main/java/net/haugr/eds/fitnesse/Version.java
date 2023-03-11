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
package net.haugr.eds.fitnesse;

import net.haugr.eds.api.responses.VersionResponse;
import net.haugr.eds.fitnesse.callers.CallManagement;

/**
 * <p>FitNesse Fixture for the EDS Version request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Version extends EDSRequest<VersionResponse> {

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public String edsVersion() {
        return (response != null) ? response.getVersion() : null;
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        response = CallManagement.version(requestUrl);
    }
}
