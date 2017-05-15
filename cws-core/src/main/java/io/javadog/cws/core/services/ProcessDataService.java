/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.Data;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.MetaDataEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.persistence.EntityManager;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataService extends Serviceable<ProcessDataResponse, ProcessDataRequest> {

    public ProcessDataService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse perform(final ProcessDataRequest request) {
        // TODO Check if the Member is allowed to perform writing Operations against the specific Circle, as the verify only makes a general check.
        verifyRequest(request, Permission.PROCESS_DATA);
        final ProcessDataResponse response;

        switch (request.getAction()) {
            case PROCESS:
                response = process(request);
                break;
            case DELETE:
                response = delete(request);
                break;
            default:
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The Action " + request.getAction() + " is not supported for this request.");
        }

        return response;
    }

    private ProcessDataResponse process(final ProcessDataRequest request) {
        final Data data = request.getData();
        final ProcessDataResponse response;

        if (data.getId() != null) {
            response = processExistingData(data);
        } else {
            response = processNewData(data);
        }

        return response;
    }

    private ProcessDataResponse processExistingData(final Data data) {
        final DataEntity entity = dao.findDataByMemberAndExternalId(member, data.getId());
        final ProcessDataResponse response;

        if (entity != null) {
            final MetaDataEntity folder = checkFolder(entity, data.getFolderId());
            checkName(entity, data.getName());
            checkData(entity, data.getBytes());
            dao.persist(entity.getMetadata());

            response = new ProcessDataResponse();
            response.setData(buildDataObject(entity, folder));
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Data Object could not be located.");
        }

        return response;
    }

    /**
     * <p>It is possible to move Data from one Folder to another, but it is not
     * permitted to move a Folder, as this is fairly problematic due to the
     * restraints in the Data Model, which has been added to prevent looping
     * models.</p>
     *
     * <p>For the same reason, the FolderId must internally remain a number, so
     * it can be checked for consistency - although the externally exposed Id
     * is the UUID or ExternalId value.</p>
     *
     * @param entity   Data Entity to check the Folder of
     * @param folderId The External Id of the Folder to check
     * @return Metadata Entity of the Folder
     */
    private MetaDataEntity checkFolder(final DataEntity entity, final String folderId) {
        MetaDataEntity folder = null;

        if (folderId != null) {
            final MetaDataEntity metadata = entity.getMetadata();

            folder = dao.findMetaDataByMemberAndExternalId(member, folderId);
            if (folder == null) {
                metadata.setParentId(null);
            } else {
                final Long currentCircleId = metadata.getCircle().getId();
                final Long foundCircleId = folder.getCircle().getId();

                if (Objects.equals(currentCircleId, foundCircleId)) {
                    if (Objects.equals(metadata.getType().getName(), "folder")) {
                        throw new ModelException(ReturnCode.ILLEGAL_ACTION, "It is not permitted to move Folders.");
                    } else {
                        metadata.setParentId(folder.getId());
                    }
                } else {
                    throw new ModelException(ReturnCode.ILLEGAL_ACTION, "Moving Data from one Circle to another is not permitted.");
                }
            }
        }

        return folder;
    }

    private static void checkName(final DataEntity entity, final String name) {
        if (name != null) {
            final String currentName = entity.getMetadata().getName();
            final String givenName = name.trim();
            if (!Objects.equals(currentName, givenName)) {
                entity.getMetadata().setName(givenName);
            }
        }
    }

    private void checkData(final DataEntity entity, final byte[] bytes) {
        if (bytes != null) {
            final TrusteeEntity trustee = findTrustee(entity.getMetadata().getCircle());
            final SecretKey circleKey = crypto.extractCircleKey(keyPair.getPrivate(), trustee.getCircleKey(), entity.getKey().getAlgorithm());
            final String salt = UUID.randomUUID().toString();
            final IvParameterSpec iv = crypto.generateInitialVector(salt);
            final byte[] encrypted = crypto.encrypt(circleKey, iv, bytes);

            entity.setInitialVector(salt);
            entity.setData(encrypted);
            dao.persist(entity);
        }
    }

    private static Data buildDataObject(final DataEntity entity, final MetaDataEntity folder) {
        final MetaDataEntity metaDataEntity = entity.getMetadata();

        final DataType type = new DataType();
        type.setName(metaDataEntity.getType().getName());
        type.setType(metaDataEntity.getType().getType());

        final Data data = new Data();
        data.setId(metaDataEntity.getExternalId());
        data.setCircleId(metaDataEntity.getCircle().getExternalId());
        data.setName(metaDataEntity.getName());
        data.setTypeName(type.getName());
        data.setAdded(metaDataEntity.getCreated());

        if (folder != null) {
            data.setFolderId(folder.getExternalId());
        }

        return data;
    }

    private ProcessDataResponse processNewData(final Data data) {

        return null;
    }

    private ProcessDataResponse delete(final ProcessDataRequest request) {
        final MetaDataEntity entity = dao.findMetaDataByMemberAndExternalId(member, request.getDataId());
        final ProcessDataResponse response;

        if (entity != null) {
            dao.delete(entity);
            response = new ProcessDataResponse();
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Data Object could not be located.");
        }

        return response;
    }

    private TrusteeEntity findTrustee(final CircleEntity circle) {
        TrusteeEntity found = null;

        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(trustee.getCircle().getId(), circle.getId())) {
                found = trustee;
                break;
            }
        }

        if (found != null) {
            if (TrustLevel.isAllowed(found.getTrustLevel(), TrustLevel.WRITE)) {
                return found;
            } else {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The current Account allowed to perform the given action.");
            }
        } else {
            throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The current Account does not have any relation with the requested Circle.");
        }
    }
}
