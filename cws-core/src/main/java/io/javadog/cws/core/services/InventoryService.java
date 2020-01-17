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

import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.InventoryRequest;
import io.javadog.cws.api.responses.InventoryResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.DataDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MetadataEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        final int pageNumber = request.getPageNumber();
        final int pageSize = request.getPageSize();
        final List<MetadataEntity> records = dao.readInventoryRecords(pageNumber, pageSize);
        final List<Metadata> inventory = new ArrayList<>(records.size());
        for (final MetadataEntity metadata : records) {
            final Metadata data = DataDao.convert(metadata, "-");
            inventory.add(data);
        }

        final InventoryResponse response = new InventoryResponse();
        response.setRecords(dao.countInventoryRecords());
        response.setInventory(inventory);

        return response;
    }
}
