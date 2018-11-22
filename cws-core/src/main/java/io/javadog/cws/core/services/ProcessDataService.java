/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.DataDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import io.javadog.cws.core.model.entities.KeyEntity;
import io.javadog.cws.core.model.entities.MetadataEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * <p>Business Logic implementation for the CWS ProcessData request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessDataService extends Serviceable<DataDao, ProcessDataResponse, ProcessDataRequest> {

    public ProcessDataService(final Settings settings, final EntityManager entityManager) {
        super(settings, new DataDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessDataResponse perform(final ProcessDataRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_DATA);
        Arrays.fill(request.getCredential(), (byte) 0);

        // Since the verification above is of a more general nature, it is
        // important that the processing is being double checked against the
        // actual Circle.
        final ProcessDataResponse response;

        switch (request.getAction()) {
            case ADD:
                response = processAddData(request);
                break;
            case UPDATE:
                response = processUpdateData(request);
                break;
            case COPY:
                response = processCopyData(request);
                break;
            case MOVE:
                response = processMoveData(request);
                break;
            case DELETE:
                response = processDeleteData(request);
                break;
            default:
                // Unreachable Code by design.
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
        }

        return response;
    }

    private ProcessDataResponse processAddData(final ProcessDataRequest request) {
        final DataTypeEntity type = findDataType(request.getTypeName());
        final MetadataEntity parent = findParent(request.getCircleId(), request.getFolderId());
        final MetadataEntity existingName = dao.findInFolder(member, parent.getId(), request.getDataName());
        final ProcessDataResponse response;

        if (existingName == null) {
            final TrusteeEntity trustee = findTrustee(request.getCircleId());
            final byte[] bytes = request.getData();

            if (Objects.equals(Constants.FOLDER_TYPENAME, type.getName())) {
                response = createFolder(trustee, request);
            } else {
                final MetadataEntity metadataEntity = createMetadata(trustee, request.getDataName(), parent.getId(), type);
                if (bytes != null) {
                    createDataEntity(trustee, metadataEntity, bytes);
                }

                response = buildProcessDataResponse(metadataEntity.getExternalId());
            }
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "Another record with the same name already exists.");
        }

        return response;
    }

    private ProcessDataResponse processUpdateData(final ProcessDataRequest request) {
        final MetadataEntity entity = dao.findMetaDataByMemberAndExternalId(member.getId(), request.getDataId());
        final ProcessDataResponse response;

        if (entity != null) {
            // Now, check if the member account is allowed to perform the requested
            // action. If not allowed, then an Exception is thrown.
            findTrustee(entity.getCircle().getExternalId());

            // First, let's identify the folder, we're not updating it yet, only
            // after the name has also been checked.
            Long folderId = entity.getParentId();
            if (request.getFolderId() != null) {
                final MetadataEntity folder = checkFolder(entity, request.getFolderId());
                folderId = folder.getId();
            }

            entity.setName(checkName(entity, request.getDataName(), folderId));
            checkData(entity, request.getData());
            entity.setParentId(folderId);
            dao.persist(entity);

            response = buildProcessDataResponse(entity.getExternalId());
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Data Object could not be found.");
        }

        return response;
    }

    private ProcessDataResponse processCopyData(final ProcessDataRequest request) {
        final TrusteeEntity targetTrustee = findTargetTrustee(request.getTargetCircleId());
        final MetadataEntity metadataEntity = findMetadataEntity(request.getDataId());
        final String externalDataId = copyDataToTargetCircle(targetTrustee, metadataEntity, request);

        return buildProcessDataResponse(externalDataId);
    }

    private ProcessDataResponse processMoveData(final ProcessDataRequest request) {
        final TrusteeEntity targetTrustee = findTargetTrustee(request.getTargetCircleId());
        final MetadataEntity metadataEntity = findMetadataEntity(request.getDataId());
        final String externalDataId = copyDataToTargetCircle(targetTrustee, metadataEntity, request);
        dao.delete(metadataEntity);

        return buildProcessDataResponse(externalDataId);
    }

    private static ProcessDataResponse buildProcessDataResponse(final String externalDataId) {
        final ProcessDataResponse response = new ProcessDataResponse();
        response.setDataId(externalDataId);

        return response;
    }

    private ProcessDataResponse processDeleteData(final ProcessDataRequest request) {
        final MetadataEntity entity = dao.findMetaDataByMemberAndExternalId(member.getId(), request.getDataId());
        final ProcessDataResponse response;

        if (entity != null) {
            // Now, check if the member account is allowed to perform the requested
            // action. If not allowed, then an Exception is thrown.
            findTrustee(entity.getCircle().getExternalId());

            if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
                // If the Entity is a Folder, then we must check if it
                // currently has content, if so - then we cannot delete it.
                final long count = dao.countFolderContent(entity.getId());
                if (count > 0) {
                    response = new ProcessDataResponse(ReturnCode.INTEGRITY_WARNING, "The requested Folder cannot be removed as it is not empty.");
                } else {
                    dao.delete(entity);
                    response = new ProcessDataResponse();
                }
            } else {
                dao.delete(entity);
                response = new ProcessDataResponse();
            }
        } else {
            response = new ProcessDataResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Data Object could not be found.");
        }

        return response;
    }

    private TrusteeEntity findTargetTrustee(final String externalCircleId) {
        final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(externalCircleId, member.getExternalId());
        if (trustee == null) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "The member has no trustee relationship with the target Circle '" + externalCircleId + "'.");
        }

        final Set<TrustLevel> trustLevels = EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE);
        if (!trustLevels.contains(trustee.getTrustLevel())) {
            throw new CWSException(ReturnCode.AUTHORIZATION_WARNING, "Member is not permitted to perform this action for the target Circle.");
        }

        return trustee;
    }

    private MetadataEntity findMetadataEntity(final String externalDataId) {
        final MetadataEntity entity = dao.findMetaDataByMemberAndExternalId(member.getId(), externalDataId);
        if (entity == null) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "No data could be found for the given Data Id '" + externalDataId + "'.");
        }
        if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
            throw new CWSException(ReturnCode.ILLEGAL_ACTION, "It is not permitted to copy or move folders.");
        }

        return entity;
    }

    private String copyDataToTargetCircle(final TrusteeEntity trustee, final MetadataEntity oldMetadataEntity, final ProcessDataRequest request) {
        final MetadataEntity folder = findParent(request.getTargetCircleId(), request.getTargetFolderId());
        final MetadataEntity metadataEntity = createMetadata(trustee, oldMetadataEntity.getName(), folder.getId(), oldMetadataEntity.getType());
        final DataEntity dataEntity = dao.findDataByMemberAndExternalId(member, oldMetadataEntity.getExternalId());
        if (dataEntity != null) {
            final byte[] bytes = decryptData(dataEntity);
            createDataEntity(trustee, metadataEntity, bytes);
        }

        return metadataEntity.getExternalId();
    }

    private void createDataEntity(final TrusteeEntity trustee, final MetadataEntity metadataEntity, final byte[] bytes) {
        final KeyEntity keyEntity = trustee.getKey();
        final KeyAlgorithm algorithm = keyEntity.getAlgorithm();
        final SecretCWSKey key = crypto.extractCircleKey(algorithm, keyPair.getPrivate(), trustee.getCircleKey());
        key.setSalt(new IVSalt());

        final String armored = key.getSalt().getArmored();
        final DataEntity dataEntity = new DataEntity();
        dataEntity.setMetadata(metadataEntity);
        dataEntity.setKey(keyEntity);
        dataEntity.setData(crypto.encrypt(key, bytes));
        dataEntity.setInitialVector(crypto.encryptWithMasterKey(armored));
        dataEntity.setChecksum(crypto.generateChecksum(dataEntity.getData()));
        dataEntity.setSanityStatus(SanityStatus.OK);
        dataEntity.setSanityChecked(Utilities.newDate());
        dao.persist(dataEntity);

        // Actively overwrite the raw Object bytes, so it no longer
        // can be read unencrypted.
        Arrays.fill(bytes, (byte) 0);
    }

    private DataTypeEntity findDataType(final String typeName) {
        DataTypeEntity entity = null;

        if (typeName != null) {
            entity = dao.findDataTypeByName(typeName);
            if (entity == null) {
                throw new CWSException(ReturnCode.INTEGRITY_WARNING, "Cannot find a matching DataType for the Object.");
            }
        }

        if (entity == null) {
            entity = dao.findDataTypeByName(Constants.DATA_TYPENAME);
        }

        return entity;
    }

    private MetadataEntity findParent(final String circleId, final String folderId) {
        final MetadataEntity entity;

        if (folderId != null) {
            entity = dao.findMetaDataByMemberAndExternalId(member.getId(), folderId);
            if ((entity == null) || !Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
                throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "Provided FolderId '" + folderId + "' is not a folder.");
            }
        } else {
            entity = dao.findRootByMemberCircle(member.getId(), circleId);
        }

        return entity;
    }

    private ProcessDataResponse createFolder(final TrusteeEntity trustee, final ProcessDataRequest request) {
        final MetadataEntity parent = findParent(request.getCircleId(), request.getFolderId());
        final DataTypeEntity folderType = dao.findDataTypeByName(Constants.FOLDER_TYPENAME);
        final MetadataEntity folder = createMetadata(trustee, request.getDataName(), parent.getId(), folderType);

        final ProcessDataResponse response = new ProcessDataResponse();
        response.setDataId(folder.getExternalId());

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
    private MetadataEntity checkFolder(final MetadataEntity entity, final String folderId) {
        final MetadataEntity folder;

        folder = dao.findMetaDataByMemberAndExternalId(member.getId(), folderId);
        if (folder != null) {
            final Long currentCircleId = entity.getCircle().getId();
            final Long foundCircleId = folder.getCircle().getId();

            if (Objects.equals(currentCircleId, foundCircleId)) {
                if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
                    throw new CWSException(ReturnCode.ILLEGAL_ACTION, "It is not permitted to move Folders.");
                }
            } else {
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Moving Data from one Circle to another is not permitted.");
            }
        } else {
            throw new CWSException(ReturnCode.INTEGRITY_WARNING, "No existing Folder could be found.");
        }

        return folder;
    }

    private String checkName(final MetadataEntity entity, final String name, final Long folderId) {
        final String theName = (name != null) ? name.trim() : entity.getName();

        if (dao.checkIfNameIsUsed(entity.getId(), theName, folderId)) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "The name provided is already being used in the given folder.");
        }

        return theName;
    }

    private void checkData(final MetadataEntity metadata, final byte[] bytes) {
        if (bytes != null) {
            final TrusteeEntity trustee = findTrustee(metadata.getCircle().getExternalId());

            DataEntity entity = dao.findDataByMetadata(metadata);
            if (entity == null) {
                entity = new DataEntity();
                entity.setMetadata(metadata);
                entity.setKey(trustee.getKey());
            }

            final SecretCWSKey circleKey = extractCircleKey(entity);
            final String salt = UUID.randomUUID().toString();
            circleKey.setSalt(new IVSalt(salt));
            final byte[] encrypted = crypto.encrypt(circleKey, bytes);
            final String checksum = crypto.generateChecksum(encrypted);

            entity.setData(encrypted);
            entity.setInitialVector(crypto.encryptWithMasterKey(salt));
            entity.setChecksum(checksum);
            entity.setSanityStatus(SanityStatus.OK);
            entity.setSanityChecked(Utilities.newDate());
            dao.persist(entity);
        }
    }
}
