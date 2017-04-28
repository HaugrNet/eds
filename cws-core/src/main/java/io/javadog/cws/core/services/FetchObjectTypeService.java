/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.dtos.ObjectType;
import io.javadog.cws.api.requests.FetchObjectTypeRequest;
import io.javadog.cws.api.responses.FetchObjectTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.TypeEntity;

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
public final class FetchObjectTypeService extends Serviceable<FetchObjectTypeResponse, FetchObjectTypeRequest> {

    public FetchObjectTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchObjectTypeResponse perform(final FetchObjectTypeRequest request) {
        verifyRequest(request, Permission.FETCH_OBJECT_TYPE);

        final List<TypeEntity> types = dao.findAllTypes();
        final List<ObjectType> objectTypes = new ArrayList<>(types.size());
        for (final TypeEntity type : types) {
            final ObjectType objectType = new ObjectType();
            objectType.setName(type.getName());
            objectType.setType(type.getType());
            objectTypes.add(objectType);
        }

        final FetchObjectTypeResponse response = new FetchObjectTypeResponse();
        response.setTypes(objectTypes);

        return response;
    }
}
