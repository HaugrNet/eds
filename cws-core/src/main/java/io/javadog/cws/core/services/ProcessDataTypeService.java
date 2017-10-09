/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.AuthorizationException;
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
        verifyRequest(request, Permission.PROCESS_DATA_TYPE);
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
     * @param request Process DataType Request Object
     * @return Response with the newly processed DataType
     */
    private ProcessDataTypeResponse doProcess(final ProcessDataTypeRequest request) {
        final String name = request.getDataType().getName().trim();
        final String type = request.getDataType().getType().trim();
        final DataTypeEntity found = findEntity(name);
        final DataTypeEntity entity;

        if (found == null) {
            entity = new DataTypeEntity();
            entity.setName(name);
            entity.setType(type);
            dao.persist(entity);
        } else {
            entity = found;
            if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getName()) || Objects.equals(Constants.DATA_TYPENAME, entity.getName())) {
                throw new AuthorizationException("It is not permitted to update the DataType '" + entity.getName() + "'.");
            } else if (!Objects.equals(type, entity.getType())) {
                entity.setType(type);
                dao.persist(entity);
            }
        }

        final DataType objectType = new DataType();
        objectType.setName(name);
        objectType.setType(type);

        final ProcessDataTypeResponse response = new ProcessDataTypeResponse();
        response.setDataType(objectType);

        return response;
    }

    private ProcessDataTypeResponse doDelete(final ProcessDataTypeRequest request) {
        final ProcessDataTypeResponse response;
        final String name = request.getDataType().getName().trim();
        final DataTypeEntity entity = findEntity(name);

        if (entity != null) {
            // We need to check that the Data Type is not being used. If so,
            // then it is not allowed to remove it.
            final long records = dao.countObjectTypeUsage(entity.getId());
            if (records > 0) {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The Data Type '" + name + "' cannot be deleted, as it is being actively used.");
            } else {
                dao.delete(entity);
                response = new ProcessDataTypeResponse();
            }
        } else {
            response = new ProcessDataTypeResponse(ReturnCode.IDENTIFICATION_WARNING, "No records were found with the name '" + name + "'.");
        }

        return response;
    }

    private DataTypeEntity findEntity(final String name) {
        final List<DataTypeEntity> entities = dao.findMatchingDataTypes(name);
        DataTypeEntity entity = null;

        if (entities.size() == 1) {
            entity = entities.get(0);
        } else if (entities.size() > 1) {
            throw new CWSException(ReturnCode.IDENTIFICATION_ERROR, "Could not uniquely identify the Data Type '" + name + "'.");
        }

        return entity;
    }
}
