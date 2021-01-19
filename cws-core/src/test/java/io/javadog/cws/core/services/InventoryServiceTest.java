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
package io.javadog.cws.core.services;

import static org.junit.jupiter.api.Assertions.*;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.InventoryRequest;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.InventoryResponse;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.setup.DatabaseSetup;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.jupiter.api.Test;

/**
 * <p>Common test class for the Process & Fetch Data Services.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.2
 */
final class InventoryServiceTest extends DatabaseSetup  {

    @Test
    void testEmptyRequest() {
        final InventoryService service = new InventoryService(settings, entityManager);
        final InventoryRequest request = new InventoryRequest();

        final CWSException cause = assertThrows(CWSException.class, () -> service.perform(request));
        assertEquals(ReturnCode.VERIFICATION_WARNING, cause.getReturnCode());
        assertEquals("Request Object contained errors:" +
                "\nKey: credential, Error: The Session (Credential) is missing.", cause.getMessage());
    }

    @Test
    void testReadingEmptyInventory() {
        final InventoryService service = new InventoryService(settings, entityManager);

        final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
        final InventoryResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(0, response.getInventory().size());
    }

    @Test
    void testReadingInventory() {
        final ProcessDataService dataService = new ProcessDataService(settings, entityManager);
        final InventoryService service = new InventoryService(settings, entityManager);

        // Prepare 100 Objects, so we can read them out.
        for (int i = 0; i < 100; i++) {
            final ProcessDataRequest dataRequest = prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "DataObject" + i, 1024);
            final ProcessDataResponse saveResponse = dataService.perform(dataRequest);
            assertEquals(ReturnCode.SUCCESS.getCode(), saveResponse.getReturnCode());
        }

        final int pageSize = 25;
        for (int i = 1; i < 4; i++) {
            final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
            request.setPageSize(pageSize);
            request.setPageNumber(1);

            final InventoryResponse response = service.perform(request);
            assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
            assertEquals(25, response.getInventory().size());
            assertEquals(100, response.getRecords());
        }

        final InventoryRequest request = prepareRequest(InventoryRequest.class, Constants.ADMIN_ACCOUNT);
        request.setPageSize(pageSize);
        request.setPageNumber(5);

        final InventoryResponse response = service.perform(request);
        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals(0, response.getInventory().size());
    }
}
