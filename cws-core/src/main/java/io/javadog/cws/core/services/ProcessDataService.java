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
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.common.keys.CWSKey;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.DataTypeEntity;
import io.javadog.cws.model.entities.MetadataEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

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
        final String circleId = (request != null) ? request.getCircleId() : null;
        verifyRequest(request, Permission.PROCESS_DATA, circleId);
        // Since the verification above is of a more general nature, it is
        // important that the processing is being double checked against the
        // actual Circle.
        final ProcessDataResponse response;

        switch (request.getAction()) {
            case ADD:
                response = processNewData(request);
                break;
            case UPDATE:
                response = processExistingData(request);
                break;
            case DELETE:
                response = delete(request);
                break;
            default:
                // Unreachable Code by design.
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
        }

        return response;
    }

    private ProcessDataResponse processExistingData(final ProcessDataRequest request) {
        final DataEntity entity = dao.findDataByMemberAndExternalId(member, request.getId());
        final ProcessDataResponse response;

        if (entity != null) {
            final MetadataEntity folder = checkFolder(entity, request.getFolderId());
            checkName(entity, request.getName());
            checkData(entity, request.getBytes());
            dao.persist(entity.getMetadata());

            response = new ProcessDataResponse();
            response.setId(entity.getMetadata().getExternalId());
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Data Object could not be located.");
        }

        return response;
    }

    private ProcessDataResponse processNewData(final ProcessDataRequest request) {
        final DataTypeEntity type = dao.findDataTypeByName(request.getTypeName());
        final TrusteeEntity trustee = findTrustee(request.getCircleId());
        final byte[] bytes = request.getBytes();
        final ProcessDataResponse response;

        if (Objects.equals(Constants.FOLDER_TYPENAME, type.getName())) {
            response = createFolder(trustee, request);
        } else if (bytes != null) {
            // 2. Extract Circle Key, and create new Key & Data Entities
            final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
            final Long parentId = null;

            final MetadataEntity metadataEntity = new MetadataEntity();
            metadataEntity.setCircle(trustee.getCircle());
            metadataEntity.setName(request.getName());
            metadataEntity.setParentId(parentId);
            metadataEntity.setType(type);
            dao.persist(metadataEntity);

            final CWSKey circleKey = crypto.extractCircleKey(algorithm, keyPair, trustee.getCircleKey());
            final String salt = UUID.randomUUID().toString();
            circleKey.setSalt(salt);
            final byte[] encrypted = crypto.encrypt(circleKey, bytes);
            final String checksum = crypto.generateChecksum(encrypted);
            final DataEntity dataEntity = new DataEntity();
            dataEntity.setMetadata(metadataEntity);
            dataEntity.setData(encrypted);
            dataEntity.setInitialVector(salt);
            dataEntity.setChecksum(checksum);
            dataEntity.setKey(trustee.getKey());

            dao.persist(dataEntity);
            response = new ProcessDataResponse();
            response.setId(metadataEntity.getExternalId());
        } else {
            // Okay, weird case - storing a Data Object without Data. So,
            // basically we're just storing the MetaData.
            final MetadataEntity parent = findParent(request);
            final Long parentId = parent.getId();

            final MetadataEntity metaData = new MetadataEntity();
            metaData.setCircle(trustee.getCircle());
            metaData.setName(request.getName());
            metaData.setParentId(parentId);
            metaData.setType(type);

            response = new ProcessDataResponse();
            response.setId(metaData.getExternalId());
        }

        return response;
    }

    private MetadataEntity findParent(final ProcessDataRequest request) {
        final MetadataEntity entity;

        if (request.getFolderId() != null) {
            entity = dao.findMetaDataByMemberAndExternalId(member, request.getFolderId());
            if (!Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
                throw new ModelException(ReturnCode.IDENTIFICATION_WARNING, "Provided FolderId is not a folder.");
            }
        } else {
            entity = dao.findRootByMemberCircle(member, request.getCircleId());
        }

        return entity;
    }

    private ProcessDataResponse createFolder(final TrusteeEntity trustee, final ProcessDataRequest request) {
        final MetadataEntity parent = findParent(request);
        final DataTypeEntity folderType = dao.findDataTypeByName(Constants.FOLDER_TYPENAME);
        final MetadataEntity folder = createMetadata(trustee, request.getName(), parent.getId(), folderType);

        final ProcessDataResponse response = new ProcessDataResponse();
        response.setId(folder.getExternalId());

        return response;
    }

    private MetadataEntity createMetadata(final TrusteeEntity trustee, final String name, final Long parentId, final DataTypeEntity dataType) {
        final MetadataEntity entity = new MetadataEntity();
        entity.setCircle(trustee.getCircle());
        entity.setName(name);
        entity.setParentId(parentId);
        entity.setType(dataType);
        dao.persist(entity);

        return entity;
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
    private MetadataEntity checkFolder(final DataEntity entity, final String folderId) {
        MetadataEntity folder = null;

        if (folderId != null) {
            final MetadataEntity metadata = entity.getMetadata();

            folder = dao.findMetaDataByMemberAndExternalId(member, folderId);
            if (folder == null) {
                metadata.setParentId(null);
            } else {
                final Long currentCircleId = metadata.getCircle().getId();
                final Long foundCircleId = folder.getCircle().getId();

                if (Objects.equals(currentCircleId, foundCircleId)) {
                    if (Objects.equals(Constants.FOLDER_TYPENAME, metadata.getType().getName())) {
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
            final TrusteeEntity trustee = findTrustee(entity.getMetadata().getCircle().getExternalId());
            final CWSKey circleKey = crypto.extractCircleKey(entity.getKey().getAlgorithm(), keyPair, trustee.getCircleKey());
            final String salt = UUID.randomUUID().toString();
            circleKey.setSalt(salt);
            final byte[] encrypted = crypto.encrypt(circleKey, bytes);
            final String checksum = crypto.generateChecksum(encrypted);

            entity.setInitialVector(salt);
            entity.setData(encrypted);
            entity.setChecksum(checksum);
            dao.persist(entity);
        }
    }

    private ProcessDataResponse delete(final ProcessDataRequest request) {
        final MetadataEntity entity = dao.findMetaDataByMemberAndExternalId(member, request.getId());
        final ProcessDataResponse response;

        if (entity != null) {
            dao.delete(entity);
            response = new ProcessDataResponse();
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Data Object could not be located.");
        }

        return response;
    }

    private TrusteeEntity findTrustee(final String externalCircleId) {
        TrusteeEntity found = null;

        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(trustee.getCircle().getExternalId(), externalCircleId)) {
                found = trustee;
                break;
            }
        }

        if (found != null) {
            if (TrustLevel.isAllowed(found.getTrustLevel(), TrustLevel.WRITE)) {
                return found;
            } else {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The current Account is not allowed to perform the given action.");
            }
        } else {
            throw new CWSException(ReturnCode.ILLEGAL_ACTION, "The current Account does not have any relation with the requested Circle.");
        }
    }
}
