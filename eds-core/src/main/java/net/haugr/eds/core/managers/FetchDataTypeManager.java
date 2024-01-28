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
import javax.persistence.EntityManager;
import net.haugr.eds.api.dtos.DataType;
import net.haugr.eds.api.requests.FetchDataTypeRequest;
import net.haugr.eds.api.responses.FetchDataTypeResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataTypeEntity;

/**
 * Data Types is the basis for all shared information, as it either provides
 * the MIMEType of Files or Data Information for Objects shared.
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchDataTypeManager extends AbstractManager<CommonDao, FetchDataTypeResponse, FetchDataTypeRequest> {

    public FetchDataTypeManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse perform(final FetchDataTypeRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_DATA_TYPE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final List<DataTypeEntity> types = dao.findAllTypes();
        final List<DataType> objectTypes = new ArrayList<>(types.size());
        for (final DataTypeEntity type : types) {
            final DataType objectType = new DataType();
            objectType.setTypeName(type.getName());
            objectType.setType(type.getType());
            objectTypes.add(objectType);
        }

        final FetchDataTypeResponse response = new FetchDataTypeResponse();
        response.setDataTypes(objectTypes);

        return response;
    }
}
