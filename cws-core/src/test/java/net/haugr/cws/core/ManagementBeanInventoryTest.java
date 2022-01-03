/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
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
package net.haugr.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.requests.InventoryRequest;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.responses.InventoryResponse;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.core.setup.DatabaseSetup;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.2
 */
final class ManagementBeanInventoryTest extends DatabaseSetup {

    @Test
    void testEmptyRequest() {
        final ManagementBean bean = prepareManagementBean();
        final InventoryRequest request = new InventoryRequest();

        final InventoryResponse response = bean.inventory(request);
        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", response.getReturnMessage());
    }

    @Test
    void testReadingEmptyInventory() {
        final ManagementBean bean = prepareManagementBean();

        final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
        final InventoryResponse response = bean.inventory(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(0, response.getInventory().size());
    }

    @Test
    void testReadingInventory() {
        final ManagementBean bean = prepareManagementBean();
        final ShareBean shareBean = prepareShareBean();

        // Prepare 100 Objects, so we can read them out.
        for (int i = 0; i < 100; i++) {
            final ProcessDataRequest dataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "DataObject" + i, 1024);
            final ProcessDataResponse saveResponse = shareBean.processData(dataRequest);
            assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        }

        final int pageSize = 25;
        for (int i = 1; i < 4; i++) {
            final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
            request.setPageSize(pageSize);
            request.setPageNumber(1);

            final InventoryResponse response = bean.inventory(request);
            assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
            assertEquals(25, response.getInventory().size());
            assertEquals(100, response.getRecords());
        }

        final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
        request.setPageSize(pageSize);
        request.setPageNumber(5);

        final InventoryResponse response = bean.inventory(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(0, response.getInventory().size());
    }
}
