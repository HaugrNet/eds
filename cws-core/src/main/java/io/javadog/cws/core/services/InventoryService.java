/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2019 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.InventoryRequest;
import io.javadog.cws.api.responses.InventoryResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.DataDao;
import io.javadog.cws.core.model.Settings;
import java.util.Arrays;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS Inventory request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.2
 */
public class InventoryService extends Serviceable<DataDao, InventoryResponse, InventoryRequest> {

    public InventoryService(final Settings settings, final EntityManager entityManager) {
        super(settings, new DataDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InventoryResponse perform(final InventoryRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.INVENTORY);
        Arrays.fill(request.getCredential(), (byte) 0);

        // And done... Authentication checks all completed
        return new InventoryResponse(ReturnCode.ILLEGAL_ACTION, "Method not implemented.");
    }
}
