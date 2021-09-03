/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2021, haugr.net
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
package net.haugr.cws.core.services;

import net.haugr.cws.api.dtos.DataType;
import net.haugr.cws.api.requests.FetchDataTypeRequest;
import net.haugr.cws.api.responses.FetchDataTypeResponse;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.model.CommonDao;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.DataTypeEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * Data Types is the basis for all shared information, as it either provides
 * the MIMEType of Files or Data Information for Objects shared.
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchDataTypeService extends Serviceable<CommonDao, FetchDataTypeResponse, FetchDataTypeRequest> {

    public FetchDataTypeService(final Settings settings, final EntityManager entityManager) {
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
            final var objectType = new DataType();
            objectType.setTypeName(type.getName());
            objectType.setType(type.getType());
            objectTypes.add(objectType);
        }

        final var response = new FetchDataTypeResponse();
        response.setDataTypes(objectTypes);

        return response;
    }
}
