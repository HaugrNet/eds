/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.ProcessDataTypeRequest;
import io.javadog.cws.api.responses.ProcessDataTypeResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.IllegalActionException;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import java.util.Arrays;
import java.util.Objects;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS ProcessDataType request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessDataTypeService extends Serviceable<CommonDao, ProcessDataTypeResponse, ProcessDataTypeRequest> {

    public ProcessDataTypeService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataTypeResponse perform(final ProcessDataTypeRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_DATA_TYPE);
        Arrays.fill(request.getCredential(), (byte) 0);
        final ProcessDataTypeResponse response;

        switch (request.getAction()) {
            case PROCESS:
                response = doProcess(request);
                break;
            case DELETE:
                response = doDelete(request);
                break;
            default:
                // Unreachable Code by design.
                throw new IllegalActionException("Unsupported Action.");
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
        final String name = request.getTypeName().trim();
        final String type = request.getType().trim();
        final DataTypeEntity found = dao.findDataTypeByName(name);
        final DataTypeEntity entity;

        if (found == null) {
            entity = new DataTypeEntity();
            entity.setName(name);
            entity.setType(type);
            dao.persist(entity);
        } else {
            entity = found;
            if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getName()) || Objects.equals(Constants.DATA_TYPENAME, entity.getName())) {
                throw new AuthorizationException("It is not permitted to update the Data Type '" + entity.getName() + "'.");
            }

            if (!Objects.equals(type, entity.getType())) {
                entity.setType(type);
                dao.persist(entity);
            }
        }

        final DataType objectType = new DataType();
        objectType.setTypeName(name);
        objectType.setType(type);

        final ProcessDataTypeResponse response = new ProcessDataTypeResponse(theDataType(entity) + " was successfully processed.");
        response.setDataType(objectType);

        return response;
    }

    private ProcessDataTypeResponse doDelete(final ProcessDataTypeRequest request) {
        final ProcessDataTypeResponse response;
        final String name = request.getTypeName().trim();
        final DataTypeEntity entity = dao.findDataTypeByName(name);

        if (entity != null) {
            // We need to check that the Data Type is not being used. If so,
            // then it is not allowed to remove it.
            final long records = dao.countDataTypeUsage(entity);
            if (records > 0) {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, theDataType(entity) + " cannot be deleted, as it is being actively used.");
            } else {
                dao.delete(entity);
                response = new ProcessDataTypeResponse(theDataType(entity) + " was successfully deleted.");
            }
        } else {
            response = new ProcessDataTypeResponse(ReturnCode.IDENTIFICATION_WARNING, "No records were found with the name '" + name + "'.");
        }

        return response;
    }

    /**
     * <p>Wrapper method to ensure that the data type is always presented the
     * same way. The method simply returns the Data Type + type name.</p>
     *
     * @param dataType DataType Entity to read the name from
     * @return String starting with 'the Data Type' and then the type name quoted
     */
    private static String theDataType(final DataTypeEntity dataType) {
        return  "The Data Type '" + dataType.getName() + "'";
    }
}
