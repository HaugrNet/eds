/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.DataTypeEntity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataTypeService extends Serviceable<ProcessDataTypeResponse, ProcessDataTypeRequest> {

    public ProcessDataTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataTypeResponse perform(final ProcessDataTypeRequest request) {
        verifyRequest(request, Permission.PROCESS_OBJECT_TYPE);
        final ProcessDataTypeResponse response;

        switch (request.getAction()) {
            case PROCESS:
                response = doProcess(request);
                break;
            case DELETE:
                response = doDelete(request);
                break;
            default:
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The Action " + request.getAction() + " is not supported for this request.");
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
    private ProcessDataTypeResponse doProcess(final ProcessDataTypeRequest request) {
        final String name = request.getObjectType().getName().trim();
        final String type = request.getObjectType().getType().trim();
        final DataTypeEntity entity;

        final List<DataTypeEntity> entities = dao.findMatchingObjectTypes(name);
        if (entities.isEmpty()) {
            entity = new DataTypeEntity();
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
            throw new CWSException(ReturnCode.IDENTIFICATION_ERROR, "Could not uniquely identify the Object Type '" + name + "' as " + entities.size() + " were found with conflicting names.");
        }

        final DataType objectType = new DataType();
        objectType.setName(name);
        objectType.setType(type);

        final ProcessDataTypeResponse response = new ProcessDataTypeResponse();
        response.setObjectType(objectType);

        return response;
    }

    private ProcessDataTypeResponse doDelete(final ProcessDataTypeRequest request) {
        final String name = request.getObjectType().getName().trim();

        final List<DataTypeEntity> entities = dao.findMatchingObjectTypes(name);
        if (entities.size() == 1) {
            // We need to check that the Object Type is not being used. If so,
            // then it is not allowed to remove it.
            final int records = dao.countObjectTypeUsage(entities.get(0).getId());
            if (records > 0) {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The Object Type '" + name + "' cannot be deleted, as it is being actively used.");
            } else {
                dao.delete(entities.get(0));
            }
        } else {
            throw new CWSException(ReturnCode.IDENTIFICATION_ERROR, "Could not uniquely identify the Object Type '" + name + "'.");
        }

        return new ProcessDataTypeResponse();
    }
}
