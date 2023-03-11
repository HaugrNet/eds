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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.responses.VersionResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class, is testing the Authenticated Service Class.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ManagementBeanVersionTest extends DatabaseSetup {

    @Test
    void testVersion() {
        final ManagementBean bean = prepareManagementBean();
        final VersionResponse response = bean.version();

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(Constants.EDS_VERSION, response.getVersion());
    }
}
