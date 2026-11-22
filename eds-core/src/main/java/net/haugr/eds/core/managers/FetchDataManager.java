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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.api.requests.FetchDataRequest;
import net.haugr.eds.api.responses.FetchDataResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.jce.DataFormat;
import net.haugr.eds.core.model.EncryptedDataStore;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.DataTypeEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;

/**
 * <p>Business Logic implementation for the EDS FetchData request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchDataManager extends AbstractManager<DataDao, FetchDataResponse, FetchDataRequest> {

    private final boolean metadataOnly;

    public FetchDataManager(final Settings settings, final EntityManager entityManager) {
        this(settings, entityManager, false);
    }

    /**
     * Creates the manager.
     *
     * @param settings      EDS Settings
     * @param entityManager Entity Manager
     * @param metadataOnly  True to return only the metadata of a Data Object,
     *                      without reading its content (PLAN.md, L1);
     *                      folders are listed as usual
     */
    public FetchDataManager(final Settings settings, final EntityManager entityManager, final boolean metadataOnly) {
        super(settings, new DataDao(entityManager));
        this.metadataOnly = metadataOnly;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse perform(final FetchDataRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_DATA);
        Arrays.fill(request.getCredential(), (byte) 0);

        // It is not possible to directly compare a sub-object against a defined
        // value if the methods are all final - this is because it prevents the
        // Hibernate proxy, using ByteBuddy, to override the method. Hence, the
        // Object it should be compared against is first read out - since this
        // will help override the proxying.
        //   See: https://github.com/HaugrNet/eds/issues/45
        final MetadataEntity root = findRootMetadata(request);

        if (root == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "No information could be found for the given Id.");
        }

        final DataTypeEntity folder = dao.findDataTypeByName(Constants.FOLDER_TYPENAME);
        final FetchDataResponse response;

        if (Objects.equals(folder, root.getType())) {
            final int pageNumber = request.getPageNumber();
            final int pageSize = request.getPageSize();
            // Names starting with a dot are hidden, unless asked for, like
            // the folders of the features (FEATURES.md).
            final List<MetadataEntity> found;
            final long count;
            if (request.includesHidden()) {
                found = dao.findMetadataByMemberAndFolder(member, root.getId(), pageNumber, pageSize);
                count = dao.countFolderContent(root.getId());
            } else {
                found = dao.findVisibleMetadataByMemberAndFolder(member, root.getId(), pageNumber, pageSize);
                count = dao.countVisibleFolderContent(root.getId());
            }
            response = prepareResponse(root.getExternalId(), found, count);
        } else if (metadataOnly) {
            response = readMetadata(root);
        } else {
            response = readCompleteDataObject(root);
        }

        return response;
    }

    /**
     * <p>When getting a request, we first need to find out what data it is that
     * is being requested. As all we have to go on is Id's, the information can
     * be hard to specify. Hence, we simply start by looking up the base
     * Metadata entity for the request, which can be either the root (if only a
     * Circle is given) folder, a folder, or a Data record (if DataId is
     * given).</p>
     *
     * <p>If no record is found, then a null value is returned.</p>
     *
     * @param request Request Object with Circle & Data ID's
     * @return Root Metadata Record to base the rest of the processing on
     */
    private MetadataEntity findRootMetadata(final FetchDataRequest request) {
        final String circleId = request.getCircleId();
        final String dataId = request.getDataId();
        final String dataName = request.getDataName();
        final MetadataEntity found;

        if (dataId != null) {
            found = dao.findMetadataByMemberAndExternalId(member.getId(), dataId);
        } else if (dataName != null) {
            found = dao.findMetadataByMemberAndName(member.getId(), dataName);
        } else {
            found = dao.findRootByMemberCircle(member.getId(), circleId);
        }

        // What belongs to a feature which is switched off in its Circle isn't
        // read (FEATURES.md): it is as if it weren't there.
        return FeatureFolder.isSwitchedOff(dao, found) ? null : found;
    }

    private FetchDataResponse readMetadata(final MetadataEntity metadata) {
        final MetadataEntity parent = dao.find(MetadataEntity.class, metadata.getParentId());
        final List<MetadataEntity> entities = List.of(metadata);
        final List<Metadata> metadataList = new ArrayList<>(1);
        metadataList.add(DataDao.convert(metadata, parent.getExternalId()));
        ContentSizes.fill(dao, entities, metadataList);
        Thumbnails.fill(dao, entities, metadataList);

        final FetchDataResponse response = new FetchDataResponse();
        response.setMetadata(metadataList);
        response.setRecords(1L);

        return response;
    }

    private FetchDataResponse readCompleteDataObject(final MetadataEntity metadata) {
        // Large content is not loaded into the JSON response (PLAN.md, Streaming,
        // S4): the size follows from the stored length and the header.
        final Object[] info = dao.findContentInfo(metadata);
        if (info != null) {
            final long id = (Long) info[0];
            final long storedLength = dao.withConnection(connection -> EncryptedDataStore.length(connection, id));
            if (storedLength > settings.getDataMaxInlineSize()) {
                final byte[] header = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, DataFormat.HEADER_BYTES));
                final long size = DataFormat.plaintextLength(storedLength, DataFormat.Header.read(header).chunkSize());
                if (size > settings.getDataMaxInlineSize()) {
                    return largeObjectResponse(metadata, size);
                }
            }
        }

        return readSmallDataObject(metadata);
    }

    private FetchDataResponse largeObjectResponse(final MetadataEntity metadata, final long size) {
        final MetadataEntity parent = dao.find(MetadataEntity.class, metadata.getParentId());
        final List<Metadata> metadataList = new ArrayList<>(1);
        final Metadata converted = DataDao.convert(metadata, parent.getExternalId());
        converted.setSize(size);
        metadataList.add(converted);
        Thumbnails.fill(dao, List.of(metadata), metadataList);

        final FetchDataResponse response = new FetchDataResponse(ReturnCode.CONTENT_TOO_LARGE, "The content has " + size
                + " bytes, more than the " + settings.getDataMaxInlineSize() + " bytes returned here; download it with the content endpoint (GET "
                + Constants.REST_DATA_BASE + Constants.REST_DATA_CONTENT + ").");
        response.setMetadata(metadataList);
        response.setRecords(1L);

        return response;
    }

    private FetchDataResponse readSmallDataObject(final MetadataEntity metadata) {
        // The following Query will read out a specific Data Record with metadata
        // information if the person is allowed, which includes checks for
        // Circle Membership and right TrustLevel of the Member. If no Entity
        // is found, then there can be multiple reasons.
        final DataEntity entity = dao.findDataByMemberAndExternalId(member, metadata.getExternalId());
        final FetchDataResponse response = new FetchDataResponse();
        final MetadataEntity parent = dao.find(MetadataEntity.class, metadata.getParentId());
        final Metadata metaData = DataDao.convert(metadata, parent.getExternalId());
        final List<Metadata> metadataList = new ArrayList<>(1);
        metadataList.add(metaData);
        Thumbnails.fill(dao, List.of(metadata), metadataList);
        if (entity != null) {
            // The content was read with its parts in one statement, so it
            // is one version of it, whatever is changed meanwhile.
            final byte[] stored = entity.getData();
            metaData.setSize(ContentSizes.size(new EncryptedDataStore.StoredHead(stored.length, stored)));
            final String checksum = crypto.generateChecksum(stored);
            final SanityStatus status;
            if (Objects.equals(checksum, entity.getChecksum())) {
                final byte[] bytes = decryptData(entity);

                // The Object may have the Status Failed, but was corrected.
                status = SanityStatus.OK;
                response.setMetadata(metadataList);
                response.setRecords(1L);
                response.setData(bytes);
            } else {
                // Note, no Exception is thrown here, so the status is saved.
                status = SanityStatus.FAILED;
                response.setReturnCode(ReturnCode.INTEGRITY_ERROR);
                response.setReturnMessage("The Encrypted Data Checksum is invalid, the data appears to have been corrupted.");
            }

            // Regardless of what the Status is, it is saved, which also
            // prevents that the Object is checked too soon. As an update by
            // its id, it changes nothing if the Object was deleted meanwhile,
            // where saving the read Entity would fail.
            dao.updateSanityStatus(entity.getId(), status);
        } else {
            response.setMetadata(metadataList);
        }

        return response;
    }

    private FetchDataResponse prepareResponse(final String folderId, final List<MetadataEntity> records, final long count) {
        final List<Metadata> list = new ArrayList<>(records.size());

        for (final MetadataEntity metadata : records) {
            final Metadata data = DataDao.convert(metadata, folderId);
            list.add(data);
        }
        ContentSizes.fill(dao, records, list);
        Thumbnails.fill(dao, records, list);

        final FetchDataResponse response = new FetchDataResponse();
        response.setMetadata(list);
        response.setRecords(count);

        return response;
    }
}
