/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.core.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.api.requests.InventoryRequest;
import net.haugr.eds.api.responses.InventoryResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MetadataEntity;

/**
 * <p>Business Logic implementation for the EDS Inventory request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.2
 */
public class InventoryManager extends AbstractManager<DataDao, InventoryResponse, InventoryRequest> {

    public InventoryManager(final Settings settings, final EntityManager entityManager) {
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
