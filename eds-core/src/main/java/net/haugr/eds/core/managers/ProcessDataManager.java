/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.ConnectionSource;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.stream.ContentCopier;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.DataTypeEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessData request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessDataManager extends AbstractManager<DataDao, ProcessDataResponse, ProcessDataRequest> {

    /** How often a copy looks again for content replaced while it waited for it. */
    private static final int MAX_REPLACED = 5;

    private final ConnectionSource readers;

    public ProcessDataManager(final Settings settings, final EntityManager entityManager) {
        this(settings, entityManager, null);
    }

    /**
     * @param settings      EDS Settings
     * @param entityManager EntityManager
     * @param readers       Opens the Connection which reads the source of a
     *                      large copy, or null if large content can't be copied
     */
    public ProcessDataManager(final Settings settings, final EntityManager entityManager, final ConnectionSource readers) {
        super(settings, new DataDao(entityManager));
        this.readers = readers;
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
        return switch (request.getAction()) {
            case ADD -> processAddData(request);
            case UPDATE -> processUpdateData(request);
            case COPY -> processCopyData(request);
            case MOVE -> processMoveData(request);
            case DELETE -> processDeleteData(request);
            // Unreachable Code by design.
            default -> throw new IllegalActionException("Unsupported Action.");
        };
    }

    private ProcessDataResponse processAddData(final ProcessDataRequest request) {
        final MetadataEntity parent = findParent(request.getCircleId(), request.getFolderId());
        FeatureFolder.refuseIf(FeatureFolder.isFeatureFolder(dao, parent) || FeatureFolder.isReservedName(dao, parent, request.getDataName()));
        final MetadataEntity existingName = dao.findInFolder(member, parent.getId(), request.getDataName());

        if (existingName != null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "Another record with the same name already exists.");
        }
        checkNameKey(null, request.getNameKey(), parent.getId());

        final TrusteeEntity trustee = findTrustee(request.getCircleId());
        final DataTypeEntity type = findDataType(request.getTypeName());
        FeatureFolder.refuseIf(Objects.equals(Constants.ENTRY_TYPENAME, type.getName()));
        Thumbnails.refuseIf(Objects.equals(Constants.THUMBNAIL_TYPENAME, type.getName()));
        final byte[] bytes = request.getData();
        checkInlineSize(bytes);
        final ProcessDataResponse response;

        if (Objects.equals(Constants.FOLDER_TYPENAME, type.getName())) {
            response = createFolder(trustee, request);
        } else {
            final MetadataEntity metadataEntity = createMetadata(trustee, request.getDataName(), request.getNameKey(), parent.getId(), type, request.getMediaType());
            encryptAndSaveData(trustee, metadataEntity, bytes);
            response = buildProcessDataResponse(metadataEntity.getExternalId(), theDataObject(metadataEntity) + " was successfully added to the Circle '" + trustee.getCircle().getName() + "'.");
        }

        return response;
    }

    private ProcessDataResponse processUpdateData(final ProcessDataRequest request) {
        final MetadataEntity entity = findMetadataAndTrustee(request);
        FeatureFolder.refuseIf(FeatureFolder.isFeature(dao, entity));
        Thumbnails.refuseIf(Thumbnails.isThumbnail(entity));
        if (request.getData() != null) {
            // The content and thumbnails are replaced; their rows are held
            // before the Data Object's own row changes, as a move or delete
            // takes them.
            dao.lockContentWithThumbnails(entity);
        }

        // First, let's identify the folder if we're not updating it yet, only
        // after the name has also been checked.
        Long folderId = entity.getParentId();
        MetadataEntity folder = null;
        if (request.getFolderId() != null) {
            folder = checkFolder(entity, request.getFolderId());
            FeatureFolder.refuseIf(FeatureFolder.isFeatureFolder(dao, folder));
            folderId = folder.getId();
        }

        if (folder == null) {
            folder = dao.find(MetadataEntity.class, folderId);
        }
        FeatureFolder.refuseIf(FeatureFolder.isReservedName(dao, folder, (request.getDataName() != null) ? request.getDataName() : entity.getName()));
        entity.setName(checkName(entity, request.getDataName(), folderId));
        // A new name comes with its own Name Key, or with none, so the old
        // one never outlives the name it was made from. A Name Key alone is
        // added to the present name, e.g. for an object stored before it.
        // Checked before it is set, as the query flushes the managed entity.
        final String nameKey = ((request.getDataName() != null) || (request.getNameKey() != null)) ? request.getNameKey() : entity.getNameKey();
        checkNameKey(entity.getId(), nameKey, folderId);
        entity.setNameKey(nameKey);
        if (request.getMediaType() != null) {
            entity.setMediaType(request.getMediaType());
        }
        checkInlineSize(request.getData());
        checkData(entity, request.getData());
        entity.setParentId(folderId);
        dao.save(entity);

        return buildProcessDataResponse(entity.getExternalId(), theDataObject(entity) + " was successfully updated.");
    }

    private ProcessDataResponse processCopyData(final ProcessDataRequest request) {
        final TrusteeEntity targetTrustee = findTargetTrustee(request.getTargetCircleId());
        final MetadataEntity metadataEntity = findSourceEntity(request.getDataId());
        Thumbnails.refuseIf(Thumbnails.isThumbnail(metadataEntity));
        final String externalDataId = copyDataToTargetCircle(targetTrustee, metadataEntity, request, true);

        return buildProcessDataResponse(externalDataId, theDataObject(metadataEntity) + " was successfully copied from '"
                + metadataEntity.getCircle().getName() + "' to '" + targetTrustee.getCircle().getName() + "'.");
    }

    private ProcessDataResponse processMoveData(final ProcessDataRequest request) {
        final TrusteeEntity targetTrustee = findTargetTrustee(request.getTargetCircleId());
        final MetadataEntity metadataEntity = findSourceEntity(request.getDataId());
        Thumbnails.refuseIf(Thumbnails.isThumbnail(metadataEntity));
        // The move deletes the content and thumbnails at its end; their rows
        // are held from the start, in the order every such request takes them.
        dao.lockContentWithThumbnails(metadataEntity);
        // The stored bytes of both Circles change at the end, in the order
        // of their IDs, so two moves between them can't wait for each other.
        dao.holdStoredBytes();
        final String externalDataId = copyDataToTargetCircle(targetTrustee, metadataEntity, request, false);
        deleteMetaDataAndDataEntities(metadataEntity);
        dao.writeStoredBytes();

        return buildProcessDataResponse(externalDataId, theDataObject(metadataEntity) + " was successfully moved from '"
                + metadataEntity.getCircle().getName() + "' to '" + targetTrustee.getCircle().getName() + "'.");
    }

    private static ProcessDataResponse buildProcessDataResponse(final String externalDataId, final String returnMessage) {
        final ProcessDataResponse response = new ProcessDataResponse(returnMessage);
        response.setDataId(externalDataId);

        return response;
    }

    private MetadataEntity findMetadataAndTrustee(final ProcessDataRequest request) {
        final MetadataEntity entity = dao.findMetadataByMemberAndExternalId(member.getId(), request.getDataId());

        if (entity == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The Data Object could not be found.");
        }

        // Now, check if the member account is allowed to perform the requested
        // action. If not allowed, then an Exception is thrown.
        findTrustee(entity.getCircle().getExternalId());

        return entity;
    }

    private ProcessDataResponse processDeleteData(final ProcessDataRequest request) {
        final MetadataEntity entity = findMetadataAndTrustee(request);
        FeatureFolder.refuseIf(FeatureFolder.isFeature(dao, entity));

        if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
            // If the Entity is a Folder, then we must check if it
            // currently has content, if so - then we cannot delete it.
            final long count = dao.countFolderContent(entity.getId());
            if (count > 0) {
                throw new EDSException(ReturnCode.INTEGRITY_WARNING, "The Folder cannot be removed as it is not empty.");
            }
        }

        deleteMetaDataAndDataEntities(entity);
        return new ProcessDataResponse(theDataObject(entity) + " has been removed from the Circle '" + entity.getCircle().getName() + "'.");
    }

    private TrusteeEntity findTargetTrustee(final String externalCircleId) {
        final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(externalCircleId, member.getExternalId());

        if (trustee == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The member has no trustee relationship with the target Circle '" + externalCircleId + "'.");
        }

        final Set<TrustLevel> trustLevels = EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE);
        if (!trustLevels.contains(trustee.getTrustLevel())) {
            throw new EDSException(ReturnCode.AUTHORIZATION_WARNING, "Member is not permitted to perform this action for the target Circle.");
        }

        return trustee;
    }

    /**
     * The Data Object to copy or move. A Member may see it with READ, but
     * copying or moving it needs WRITE or ADMIN in its Circle, checked here
     * before anything else, whether it has content or not; before, a move of
     * an object without content deleted it from a Circle the Member could
     * only read (ticket 8).
     */
    private MetadataEntity findSourceEntity(final String externalDataId) {
        final MetadataEntity entity = dao.findMetadataByMemberAndExternalId(member.getId(), externalDataId);
        if (entity == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "No data could be found for the given Data Id '" + externalDataId + "'.");
        }
        findTrustee(entity.getCircle().getExternalId());
        if (Objects.equals(Constants.FOLDER_TYPENAME, entity.getType().getName())) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "It is not permitted to copy or move folders.");
        }
        FeatureFolder.refuseIf(FeatureFolder.isEntry(entity));

        return entity;
    }

    /**
     * Copies the content to a new Data Object in the target Circle,
     * re-encrypted chunk by chunk (PLAN.md, S4), so large content is
     * never held in memory as a whole. A move removes the source afterwards,
     * so only a copy counts against the storage ceiling.
     */
    private String copyDataToTargetCircle(final TrusteeEntity trustee, final MetadataEntity oldMetadataEntity, final ProcessDataRequest request, final boolean addsContent) {
        final MetadataEntity folder = findParent(request.getTargetCircleId(), request.getTargetFolderId());
        FeatureFolder.refuseIf(FeatureFolder.isFeatureFolder(dao, folder) || FeatureFolder.isReservedName(dao, folder, oldMetadataEntity.getName()));
        // The Name Key is made with a key of the Circle, so the old one is
        // meaningless in the target Circle; only the request can give one.
        checkNameKey(null, request.getNameKey(), folder.getId());
        final MetadataEntity metadataEntity = createMetadata(trustee, oldMetadataEntity.getName(), request.getNameKey(), folder.getId(), oldMetadataEntity.getType(), oldMetadataEntity.getMediaType());
        Object[] info = dao.findContentInfo(oldMetadataEntity);
        if (info != null) {
            long sourceId = (Long) info[0];
            // The content row is held while it is read: the rotation of the
            // Circle Secret replaces the content and its secret together, and
            // without it, could do so between reading the secret and the content.
            Long sourceKeyId = dao.lockData(sourceId);
            // Replaced by an update while this waited: the new content of the
            // same Data Object is copied (ticket 3).
            for (int tries = 0; (sourceKeyId == null) && (info != null) && (tries < MAX_REPLACED); tries++) {
                info = dao.findContentInfo(oldMetadataEntity);
                if (info != null) {
                    sourceId = (Long) info[0];
                    sourceKeyId = dao.lockData(sourceId);
                }
            }
            throwConditionalException(sourceKeyId == null, ReturnCode.IDENTIFICATION_WARNING,
                    "The content of " + theDataObject(oldMetadataEntity) + " was deleted meanwhile.");
            final TrusteeEntity sourceTrustee = findTrustee(oldMetadataEntity.getCircle().getExternalId());
            // Read again once the content is held: a rotation which began
            // while this waited gave the Trustee the secret the content may
            // now be on (ticket 3).
            dao.refresh(sourceTrustee);
            final DataFormat.Context sourceContext = new DataFormat.Context(oldMetadataEntity.getCircle().getExternalId(), oldMetadataEntity.getExternalId(), sourceKeyId);
            final long lockedId = sourceId;
            final long sourceLength = dao.withConnection(connection -> EncryptedDataStore.length(connection, lockedId));
            if (addsContent) {
                StorageCeiling.check(settings, dao, -1, sourceLength);
            }
            final ContentCopier.Source source = new ContentCopier.Source(lockedId, sourceLength, unwrapCircleKey(sourceTrustee, sourceKeyId), sourceContext);

            final DataEntity target = new DataEntity();
            target.setMetadata(metadataEntity);
            target.setKey(trustee.getKey());
            target.setData(new byte[0]);
            target.setChecksum("pending");
            target.setSanityStatus(SanityStatus.OK);
            target.setSanityChecked(Utilities.newDate());
            dao.save(target);
            final long targetId = target.getId();
            final DataFormat.Context targetContext = dataContext(target);
            final SecretEDSKey targetSecret = unwrapCircleKey(trustee);
            final String algorithm = settings.getHashAlgorithm().getAlgorithm();
            dao.writeContent(trustee.getCircle().getId(), targetId, connection -> {
                dao.detach(target);
                return ContentCopier.copy(connection, readers, source, new ContentCopier.Target(targetId, targetSecret, targetContext),
                        settings.getStreamReadBatchSize(), algorithm, settings.getDataMaxInlineSize());
            });
        }

        return metadataEntity.getExternalId();
    }

    private void checkData(final MetadataEntity metadata, final byte[] bytes) {
        if (bytes != null) {
            final TrusteeEntity trustee = findTrustee(metadata.getCircle().getExternalId());
            // The old content is replaced, without loading it; its thumbnail
            // no longer matches.
            dao.deleteThumbnails(metadata);
            dao.deleteContent(metadata);
            encryptAndSaveData(trustee, metadata, bytes);
        }
    }

    private void checkInlineSize(final byte[] bytes) {
        if ((bytes != null) && (bytes.length > settings.getDataMaxInlineSize())) {
            throw new EDSException(ReturnCode.CONTENT_TOO_LARGE, "The data is larger than the " + settings.getDataMaxInlineSize()
                    + " bytes allowed in the request; upload it with the content endpoint (PUT " + Constants.REST_DATA_BASE + Constants.REST_DATA_CONTENT + ").");
        }
    }

    private void encryptAndSaveData(final TrusteeEntity trustee, final MetadataEntity metadataEntity, final byte[] bytes) {
        if (bytes != null) {
            final KeyEntity keyEntity = trustee.getKey();
            final SecretEDSKey circleSecret = unwrapCircleKey(trustee);

            // A new object key (random HKDF salt) on every write, see
            // DataFormat and PLAN.md, Crypto §4.
            try {
                final DataEntity toSave = new DataEntity();
                toSave.setMetadata(metadataEntity);
                toSave.setKey(keyEntity);
                toSave.setData(DataFormat.encrypt(circleSecret, dataContext(toSave), bytes));
                // An update has removed the old content already.
                StorageCeiling.check(settings, dao, -1, toSave.getData().length);
                toSave.setChecksum(crypto.generateChecksum(toSave.getData()));
                toSave.setSanityStatus(SanityStatus.OK);
                toSave.setSanityChecked(Utilities.newDate());
                dao.save(toSave);
            } finally {
                // Actively overwrite the raw Object bytes, so it no longer
                // can be read unencrypted.
                Arrays.fill(bytes, (byte) 0);
            }
        }
    }

    private DataTypeEntity findDataType(final String typeName) {
        DataTypeEntity entity = null;

        if (typeName != null) {
            entity = dao.findDataTypeByName(typeName);
            if (entity == null) {
                throw new EDSException(ReturnCode.INTEGRITY_WARNING, "Cannot find a matching DataType for the Object.");
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
                throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "Provided FolderId '" + folderId + "' is not a folder.");
            }
            // A folder of another Circle is never a parent, as UPDATE has it
            // (checkFolder); before, ADD, COPY and MOVE took one (ticket 9).
            throwConditionalException(!Objects.equals(entity.getCircle().getExternalId(), circleId),
                    ReturnCode.ILLEGAL_ACTION, "The folder belongs to another Circle.");
        } else {
            entity = dao.findRootByMemberCircle(member.getId(), circleId);
            // BugReport #57 states that an NPE was thrown. However, it
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
        final MetadataEntity folder = createMetadata(trustee, request.getDataName(), request.getNameKey(), parent.getId(), folderType, null);

        final ProcessDataResponse response = new ProcessDataResponse("The Folder '" + request.getDataName() + "' was successfully added to the Circle '" + trustee.getCircle().getName() + "'.");
        response.setDataId(folder.getExternalId());

        return response;
    }

    private MetadataEntity createMetadata(final TrusteeEntity trustee, final String name, final String nameKey, final Long parentId, final DataTypeEntity dataType, final String mediaType) {
        final MetadataEntity entity = new MetadataEntity();
        entity.setMediaType(mediaType);
        entity.setCircle(trustee.getCircle());
        entity.setName(name);
        entity.setNameKey(nameKey);
        entity.setParentId(parentId);
        entity.setType(dataType);
        dao.save(entity);

        return entity;
    }

    private void deleteMetaDataAndDataEntities(final MetadataEntity entity) {
        // The content may be large; it is deleted without loading it. A
        // thumbnail goes with its Data Object (a moved one doesn't keep it).
        dao.lockContentWithThumbnails(entity);
        dao.deleteThumbnails(entity);
        dao.deleteContent(entity);
        dao.delete(entity);
    }

    /**
     * <p>It is possible to move Data from one Folder to another. However,
     * it is not permitted to move a Folder, as this is fairly problematic
     * due to the restraints in the Data Model, which has been added to
     * prevent looping models.</p>
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
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The name '" + theName + "' provided is already being used in the given folder.");
        }

        return theName;
    }

    /**
     * In a chamber (E2E.md) the name is ciphertext which differs each time, so
     * {@link #checkName(MetadataEntity, String, Long)} can't find a repeated
     * name; the client sends a Name Key, a keyed hash of the name, and that
     * is kept unique within the folder instead.
     *
     * @param metadataId The Data Object which may keep its own Name Key, or
     *                   null for a new one
     * @param nameKey    The Name Key, or null if none was given
     * @param folderId   The folder the Data Object is or goes in
     */
    private void checkNameKey(final Long metadataId, final String nameKey, final Long folderId) {
        if ((nameKey != null) && dao.checkIfNameKeyIsUsed(metadataId, nameKey, folderId)) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "Another record with the same name already exists.");
        }
    }

    /**
     * <p>Wrapper method to ensure that the data object is always presented the
     * same way. The method simply returns the Data Object plus the data name.</p>
     *
     * @param metadata Metadata Entity to read the name from
     * @return String starting with 'the Data Object' and then the data name quoted
     */
    private static String theDataObject(final MetadataEntity metadata) {
        return "The Data Object '" + metadata.getName() + "'";
    }
}
