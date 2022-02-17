/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core.managers;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.EntityManager;
import net.haugr.cws.api.common.Constants;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.common.TrustLevel;
import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.api.requests.ProcessDataRequest;
import net.haugr.cws.api.responses.ProcessDataResponse;
import net.haugr.cws.core.enums.KeyAlgorithm;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.enums.SanityStatus;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.exceptions.IllegalActionException;
import net.haugr.cws.core.jce.Crypto;
import net.haugr.cws.core.jce.IVSalt;
import net.haugr.cws.core.jce.SecretCWSKey;
import net.haugr.cws.core.model.DataDao;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.DataEntity;
import net.haugr.cws.core.model.entities.DataTypeEntity;
import net.haugr.cws.core.model.entities.KeyEntity;
import net.haugr.cws.core.model.entities.MetadataEntity;
import net.haugr.cws.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the CWS ProcessData request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class ProcessDataManager extends AbstractManager<DataDao, ProcessDataResponse, ProcessDataRequest> {

    public ProcessDataManager(final Settings settings, final EntityManager entityManager) {
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
        // important that the processing is being double-checked against the
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
                throw new IllegalActionException("Unsupported Action.");
        }

        return response;
    }

    private ProcessDataResponse processAddData(final ProcessDataRequest request) {
        final MetadataEntity parent = findParent(request.getCircleId(), request.getFolderId());
        final MetadataEntity existingName = dao.findInFolder(member, parent.getId(), request.getDataName());

        if (existingName != null) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "Another record with the same name already exists.");
        }

        final TrusteeEntity trustee = findTrustee(request.getCircleId());
        final DataTypeEntity type = findDataType(request.getTypeName());
        final byte[] bytes = request.getData();
        final ProcessDataResponse response;

        if (Objects.equals(Constants.FOLDER_TYPENAME, type.getName())) {
            response = createFolder(trustee, request);
        } else {
            final MetadataEntity metadataEntity = createMetadata(trustee, request.getDataName(), parent.getId(), type);
            encryptAndSaveData(trustee, metadataEntity, null, bytes);
            response = buildProcessDataResponse(metadataEntity.getExternalId(), theDataObject(metadataEntity) + " was successfully added to the Circle '" + trustee.getCircle().getName() + "'.");
        }

        return response;
    }

    private ProcessDataResponse processUpdateData(final ProcessDataRequest request) {
        final MetadataEntity entity = findMetadataAndTrustee(request);

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
        dao.save(entity);

        return buildProcessDataResponse(entity.getExternalId(), theDataObject(entity) + " was successfully updated.");
    }

    private ProcessDataResponse processCopyData(final ProcessDataRequest request) {
        final TrusteeEntity targetTrustee = findTargetTrustee(request.getTargetCircleId());
        final MetadataEntity metadataEntity = findMetadataEntity(request.getDataId());
        final String externalDataId = copyDataToTargetCircle(targetTrustee, metadataEntity, request);

        return buildProcessDataResponse(externalDataId, theDataObject(metadataEntity) + " was successfully copied from '" + metadataEntity.getCircle().getName() + "' to '" + targetTrustee.getCircle().getName() + "'.");
    }

    private ProcessDataResponse processMoveData(final ProcessDataRequest request) {
        final TrusteeEntity targetTrustee = findTargetTrustee(request.getTargetCircleId());
        final MetadataEntity metadataEntity = findMetadataEntity(request.getDataId());
        final String externalDataId = copyDataToTargetCircle(targetTrustee, metadataEntity, request);
        dao.delete(metadataEntity);

        return buildProcessDataResponse(externalDataId, theDataObject(metadataEntity) + " was successfully moved from '" + metadataEntity.getCircle().getName() + "' to '" + targetTrustee.getCircle().getName() + "'.");
    }

    private static ProcessDataResponse buildProcessDataResponse(final String externalDataId, final String returnMessage) {
        final ProcessDataResponse response = new ProcessDataResponse(returnMessage);
        response.setDataId(externalDataId);

        return response;
    }

    private MetadataEntity findMetadataAndTrustee(final ProcessDataRequest request) {
        final MetadataEntity entity = dao.findMetadataByMemberAndExternalId(member.getId(), request.getDataId());

        if (entity == null) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "The Data Object could not be found.");
        }

        // Now, check if the member account is allowed to perform the requested
        // action. If not allowed, then an Exception is thrown.
        findTrustee(entity.getCircle().getExternalId());

        return entity;
    }

    private ProcessDataResponse processDeleteData(final ProcessDataRequest request) {
        final MetadataEntity entity = findMetadataAndTrustee(request);

        if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
            // If the Entity is a Folder, then we must check if it
            // currently has content, if so - then we cannot delete it.
            final long count = dao.countFolderContent(entity.getId());
            if (count > 0) {
                throw new CWSException(ReturnCode.INTEGRITY_WARNING, "The Folder cannot be removed as it is not empty.");
            }
        }

        dao.delete(entity);
        return new ProcessDataResponse(theDataObject(entity) + " has been removed from the Circle '" + entity.getCircle().getName() + "'.");
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
        final MetadataEntity entity = dao.findMetadataByMemberAndExternalId(member.getId(), externalDataId);
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
            encryptAndSaveData(trustee, metadataEntity, null, bytes);
        }

        return metadataEntity.getExternalId();
    }

    private void checkData(final MetadataEntity metadata, final byte[] bytes) {
        if (bytes != null) {
            final TrusteeEntity trustee = findTrustee(metadata.getCircle().getExternalId());
            final DataEntity dataEntity = dao.findDataByMetadata(metadata);
            encryptAndSaveData(trustee, metadata, dataEntity, bytes);
        }
    }

    private void encryptAndSaveData(final TrusteeEntity trustee, final MetadataEntity metadataEntity, final DataEntity oldDataEntity, final byte[] bytes) {
        if (bytes != null) {
            final KeyEntity keyEntity = trustee.getKey();
            final KeyAlgorithm algorithm = keyEntity.getAlgorithm();
            final SecretCWSKey key = Crypto.extractCircleKey(algorithm, keyPair.getPrivate(), trustee.getCircleKey());
            key.setSalt(new IVSalt());
            final String armored = key.getSalt().getArmored();

            final DataEntity toSave = (oldDataEntity != null) ? oldDataEntity : new DataEntity();
            toSave.setMetadata(metadataEntity);
            toSave.setKey(keyEntity);
            toSave.setData(Crypto.encrypt(key, bytes));
            toSave.setInitialVector(crypto.encryptWithMasterKey(armored));
            toSave.setChecksum(crypto.generateChecksum(toSave.getData()));
            toSave.setSanityStatus(SanityStatus.OK);
            toSave.setSanityChecked(Utilities.newDate());
            dao.save(toSave);

            // Actively overwrite the raw Object bytes, so it no longer
            // can be read unencrypted.
            Arrays.fill(bytes, (byte) 0);
        }
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
            entity = dao.findMetadataByMemberAndExternalId(member.getId(), folderId);
            if ((entity == null) || !Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
                throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "Provided FolderId '" + folderId + "' is not a folder.");
            }
        } else {
            entity = dao.findRootByMemberCircle(member.getId(), circleId);
            // BugReport #57 states that an NPE was thrown, however it
            // seems that the database might have entered into a strange
            // inconsistency, hence this Exception.
            //   The Circle must be manually corrected, which can be done
            // by simply adding a metadata record with the root folder
            throwConditionalNullException(entity,
                    ReturnCode.INTEGRITY_ERROR, "No Parent could be found for the Circle '" + circleId + "', please contact the administrators.");
        }

        return entity;
    }

    private ProcessDataResponse createFolder(final TrusteeEntity trustee, final ProcessDataRequest request) {
        final MetadataEntity parent = findParent(request.getCircleId(), request.getFolderId());
        final DataTypeEntity folderType = dao.findDataTypeByName(Constants.FOLDER_TYPENAME);
        final MetadataEntity folder = createMetadata(trustee, request.getDataName(), parent.getId(), folderType);

        final ProcessDataResponse response = new ProcessDataResponse("The Folder '" + request.getDataName() + "' was successfully added to the Circle '" + trustee.getCircle().getName() + "'.");
        response.setDataId(folder.getExternalId());

        return response;
    }

    private MetadataEntity createMetadata(final TrusteeEntity trustee, final String name, final Long parentId, final DataTypeEntity dataType) {
        final MetadataEntity entity = new MetadataEntity();
        entity.setCircle(trustee.getCircle());
        entity.setName(name);
        entity.setParentId(parentId);
        entity.setType(dataType);
        dao.save(entity);

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
     * @param folderId The External ID of the Folder to check
     * @return Metadata Entity of the Folder
     */
    private MetadataEntity checkFolder(final MetadataEntity entity, final String folderId) {
        final MetadataEntity folder = dao.findMetadataByMemberAndExternalId(member.getId(), folderId);
        throwConditionalNullException(folder,
                ReturnCode.INTEGRITY_WARNING, "No existing Folder could be found.");

        final Long currentCircleId = entity.getCircle().getId();
        final Long foundCircleId = folder.getCircle().getId();

        throwConditionalException(!Objects.equals(currentCircleId, foundCircleId),
                ReturnCode.ILLEGAL_ACTION, "Moving Data from one Circle to another is not permitted.");
        throwConditionalException(Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName()),
                ReturnCode.ILLEGAL_ACTION, "It is not permitted to move Folders.");

        return folder;
    }

    private String checkName(final MetadataEntity entity, final String name, final Long folderId) {
        final String theName = (name != null) ? name.trim() : entity.getName();

        if (dao.checkIfNameIsUsed(entity.getId(), theName, folderId)) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "The name '" + theName + "' provided is already being used in the given folder.");
        }

        return theName;
    }

    /**
     * <p>Wrapper method to ensure that the data object is always presented the
     * same way. The method simply returns the Data Object + data name.</p>
     *
     * @param metadata Metadata Entity to read the name from
     * @return String starting with 'the Data Object' and then the data name quoted
     */
    private static String theDataObject(final MetadataEntity metadata) {
        return "The Data Object '" + metadata.getName() + "'";
    }
}
