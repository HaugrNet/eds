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

import java.util.Arrays;
import java.util.Objects;
import javax.persistence.EntityManager;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.DataType;
import net.haugr.eds.api.requests.ProcessDataTypeRequest;
import net.haugr.eds.api.responses.ProcessDataTypeResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataTypeEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessDataType request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessDataTypeManager extends AbstractManager<CommonDao, ProcessDataTypeResponse, ProcessDataTypeRequest> {

    public ProcessDataTypeManager(final Settings settings, final EntityManager entityManager) {
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

        return switch (request.getAction()) {
            case PROCESS -> doProcess(request);
            case DELETE -> doDelete(request);
            // Unreachable Code by design.
            default -> throw new IllegalActionException("Unsupported Action.");
        };
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
            dao.save(entity);
        } else {
            entity = found;
            throwConditionalException(Objects.equals(Constants.FOLDER_TYPENAME, entity.getName()) || Objects.equals(Constants.DATA_TYPENAME, entity.getName()),
                    ReturnCode.AUTHORIZATION_WARNING, "It is not permitted to update the Data Type '" + entity.getName() + "'.");

            if (!Objects.equals(type, entity.getType())) {
                entity.setType(type);
                dao.save(entity);
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
        final String name = request.getTypeName().trim();
        final DataTypeEntity entity = dao.findDataTypeByName(name);

        throwConditionalNullException(entity,
                ReturnCode.IDENTIFICATION_WARNING, "No records were found with the name '" + name + "'.");

        // We need to check that the Data Type is not being used. If so,
        // then it is not allowed to remove it.
        final long records = dao.countDataTypeUsage(entity);
        throwConditionalException(records > 0,
                ReturnCode.ILLEGAL_ACTION, theDataType(entity) + " cannot be deleted, as it is being actively used.");
        dao.delete(entity);

        return new ProcessDataTypeResponse(theDataType(entity) + " was successfully deleted.");
    }

    /**
     * <p>Wrapper method to ensure that the data type is always presented the
     * same way. The method simply returns the Data Type + type name.</p>
     *
     * @param dataType DataType Entity to read the name from
     * @return String starting with 'the Data Type' and then the type name quoted
     */
    private static String theDataType(final DataTypeEntity dataType) {
        return "The Data Type '" + dataType.getName() + "'";
    }
}
