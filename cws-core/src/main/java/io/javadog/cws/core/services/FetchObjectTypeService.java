/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.FetchDataTypeRequest;
import io.javadog.cws.api.responses.FetchDataTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.DataTypeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Object Types is the basis for all shared information, as it either provides
 * the MIMEType of files or Object Information for Objects shared.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchObjectTypeService extends Serviceable<FetchDataTypeResponse, FetchDataTypeRequest> {

    public FetchObjectTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataTypeResponse perform(final FetchDataTypeRequest request) {
        verifyRequest(request, Permission.FETCH_OBJECT_TYPE);

        final List<DataTypeEntity> types = dao.findAllTypes();
        final List<DataType> objectTypes = new ArrayList<>(types.size());
        for (final DataTypeEntity type : types) {
            final DataType objectType = new DataType();
            objectType.setName(type.getName());
            objectType.setType(type.getType());
            objectTypes.add(objectType);
        }

        final FetchDataTypeResponse response = new FetchDataTypeResponse();
        response.setTypes(objectTypes);

        return response;
    }
}
