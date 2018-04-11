/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataTypeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data Types is the basis for all shared information, as it either provides
 * the MIMEType of Files or Data Information for Objects shared.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchDataTypeService extends Serviceable<FetchDataTypeResponse, FetchDataTypeRequest> {

    public FetchDataTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
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
