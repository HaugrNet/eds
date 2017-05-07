/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.ObjectType;
import io.javadog.cws.api.requests.ProcessObjectTypeRequest;
import io.javadog.cws.api.responses.ProcessObjectTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.TypeEntity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessObjectTypeService extends Serviceable<ProcessObjectTypeResponse, ProcessObjectTypeRequest> {

    public ProcessObjectTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessObjectTypeResponse perform(final ProcessObjectTypeRequest request) {
        verifyRequest(request, Permission.PROCESS_OBJECT_TYPE);
        final ProcessObjectTypeResponse response;

        switch (request.getAction()) {
            case PROCESS:
                response = doProcess(request);
                break;
            case DELETE:
                response = doDelete(request);
                break;
            default:
                throw new CWSException(Constants.ILLEGAL_ACTION, "The Action " + request.getAction() + " is not supported for this request.");
        }

        return response;
    }

    /**
     * Processing means either create or update. To check which it is, we're
     * searching for the value using a trimmed lowercase version of the name.
     * If a match is found, then it is updated, otherwise it is created.
     *
     * @param request Process ObjectType Request Object
     * @return Response with the newly processed ObjectType
     */
    private ProcessObjectTypeResponse doProcess(final ProcessObjectTypeRequest request) {
        final String name = request.getObjectType().getName().trim();
        final String type = request.getObjectType().getType().trim();
        final TypeEntity entity;

        final List<TypeEntity> entities = dao.findMatchingObjectTypes(name);
        if (entities.isEmpty()) {
            entity = new TypeEntity();
            entity.setName(name);
            entity.setType(type);
            dao.persist(entity);
        } else if (entities.size() == 1) {
            entity = entities.get(0);
            if (!Objects.equals(type, entity.getType())) {
                entity.setType(type);
                dao.persist(entity);
            }
        } else {
            throw new CWSException(Constants.IDENTIFICATION_ERROR, "Could not uniquely identify the Object Type '" + name + "' as " + entities.size() + " were found with conflicting names.");
        }

        final ObjectType objectType = new ObjectType();
        objectType.setName(name);
        objectType.setType(type);

        final ProcessObjectTypeResponse response = new ProcessObjectTypeResponse();
        response.setObjectType(objectType);

        return response;
    }

    private ProcessObjectTypeResponse doDelete(final ProcessObjectTypeRequest request) {
        final String name = request.getObjectType().getName().trim();

        final List<TypeEntity> entities = dao.findMatchingObjectTypes(name);
        if (entities.size() == 1) {
            // We need to check that the Object Type is not being used. If so,
            // then it is not allowed to remove it.
            final int records = dao.countObjectTypeUsage(entities.get(0).getId());
            if (records > 0) {
                throw new CWSException(Constants.ILLEGAL_ACTION, "The Object Type '" + name + "' cannot be deleted, as it is being actively used.");
            } else {
                dao.delete(entities.get(0));
            }
        } else {
            throw new CWSException(Constants.IDENTIFICATION_ERROR, "Could not uniquely identify the Object Type '" + name + "'.");
        }

        return new ProcessObjectTypeResponse();
    }
}
