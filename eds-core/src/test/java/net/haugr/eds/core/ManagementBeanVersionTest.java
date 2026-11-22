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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.responses.VersionResponse;
import net.haugr.eds.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>This Test Class is testing the Authenticated Service Class.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ManagementBeanVersionTest extends DatabaseSetup {

    @Test
    void testVersion() {
        // As at the startup of EDS, which makes the database known.
        schemaManager.prepare();
        final ManagementBean bean = prepareManagementBean();
        final VersionResponse response = bean.version();

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertEquals(Constants.EDS_VERSION, response.getVersion());
        // The tests run on in-memory H2, which makes this a test system.
        assertEquals("H2", response.getDatabase());
        assertSame(Boolean.FALSE, response.getPersistent());
        assertNotNull(response.getStorageCeiling());
    }

    @Test
    void testVersionWithoutTheSchemaManager() {
        final VersionResponse response = new ManagementBean(entityManager, settings).version();

        assertEquals(Constants.EDS_VERSION, response.getVersion());
        assertNull(response.getDatabase());
        assertNull(response.getPersistent());
        assertNull(response.getStorageCeiling());
    }

    @Test
    void testTheStorageCeilingIsLeftOutWhenTheDatabaseFails() {
        schemaManager.prepare();
        final VersionResponse response = new ManagementBean(closedEntityManager(), schemaManager, settings, null, null).version();

        assertTrue(response.isOk(), response.getReturnMessage());
        assertEquals("H2", response.getDatabase());
        assertNull(response.getStorageCeiling());
    }
}
