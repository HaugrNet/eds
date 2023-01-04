/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.cws.api.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.dtos.Metadata;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.2
 */
final class InventoryResponseTest {

    @Test
    void testClassFlow() {
        final List<Metadata> inventory = new ArrayList<>(3);
        inventory.add(new Metadata());
        inventory.add(new Metadata());
        inventory.add(new Metadata());

        final InventoryResponse response = new InventoryResponse();
        response.setInventory(inventory);
        response.setRecords(3L);

        assertEquals(ReturnCode.SUCCESS.getCode(), response.getReturnCode());
        assertEquals("Ok", response.getReturnMessage());
        assertTrue(response.isOk());
        assertEquals(inventory, response.getInventory());
        assertEquals(3L, response.getRecords());
    }

    @Test
    void testError() {
        final String msg = "Inventory Request failed due to Verification Problems.";
        final InventoryResponse response = new InventoryResponse(ReturnCode.VERIFICATION_WARNING, msg);

        assertEquals(ReturnCode.VERIFICATION_WARNING.getCode(), response.getReturnCode());
        assertEquals(msg, response.getReturnMessage());
        assertFalse(response.isOk());
        assertTrue(response.getInventory().isEmpty());
    }
}
