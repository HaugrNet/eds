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
import io.javadog.cws.api.dtos.Metadata;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.keys.SecretCWSKey;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.DataTypeEntity;
import io.javadog.cws.model.entities.MetadataEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchDataService extends Serviceable<FetchDataResponse, FetchDataRequest> {

    public FetchDataService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchDataResponse perform(final FetchDataRequest request) {
        verifyRequest(request, Permission.FETCH_DATA);
        final MetadataEntity root = findRootMetadata(request);
        final FetchDataResponse response;

        if (root != null) {
            if (Objects.equals(root.getType().getName(), Constants.FOLDER_TYPENAME)) {
                final int pageNumber = request.getPageNumber();
                final int pageSize = request.getPageSize();
                final List<MetadataEntity> found = dao.findMetadataByMemberAndFolder(member, root, pageNumber, pageSize);
                response = prepareResponse(root.getExternalId(), found);
            } else {
                response = readCompleteDataObject(root);
            }
        } else {
            response = new FetchDataResponse(ReturnCode.IDENTIFICATION_WARNING, "No information could be found for the given Id.");
        }

        return response;
    }

    /**
     * <p>When getting a request, we first need to find out what data it is that
     * is being requested. As all we have to go on is Id's, the information can
     * be hard to specify. Hence, we simply start by looking up the base
     * Metadata entity for the request, which can be either the root (if only a
     * Circle is given) folder, a folder or a Data record (if DataId is
     * given).</p>
     *
     * <p>If no record is found, then a null value is returned.</p>
     *
     * @param request Request Object with Circle & Data Id's
     * @return Root Metadata Record, to base the rest of the processing on
     */
    private MetadataEntity findRootMetadata(final FetchDataRequest request) {
        final String circleId = request.getCircleId();
        final String dataId = request.getDataId();
        final MetadataEntity found;

        if (dataId != null) {
            found = dao.findMetaDataByMemberAndExternalId(member, dataId);
        } else {
            found = dao.findRootByMemberCircle(member, circleId);
        }

        return found;
    }

    private FetchDataResponse readCompleteDataObject(final MetadataEntity metadata) {
        // Following Query will read out a specific Data Record with meta data
        // information, if the person is allowed, which includes checks for
        // Circle Membership and right TrustLevel of the Member. If no Entity
        // is found, then there can be multiple reasons.
        final DataEntity entity = dao.findDataByMemberAndExternalId(member, metadata.getExternalId());

        if (entity != null) {
            final String checksum = crypto.generateChecksum(entity.getData());
            if (Objects.equals(checksum, entity.getChecksum())) {
                final SecretCWSKey key = extractCircleKey(entity);
                key.setSalt(entity.getInitialVector());
                final byte[] bytes = crypto.decrypt(key, entity.getData());

                final Metadata metaData = convert(entity.getMetadata(), readFolder(entity.getMetadata()));
                final List<Metadata> objects = new ArrayList<>(1);
                objects.add(metaData);

                final FetchDataResponse response = new FetchDataResponse();
                response.setMetadata(objects);
                response.setData(bytes);

                return response;
            } else {
                throw new CWSException(ReturnCode.INTEGRITY_ERROR, "The Encrypted Data Checksum is invalid, the data appears to have been corrupted.");
            }
        } else {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "No Data Object found, matching the provided information or member is lacking privileges to read the actual data.");
        }
    }

    private static FetchDataResponse prepareResponse(final String folderId, final List<MetadataEntity> records) {
        final List<Metadata> list = new ArrayList<>(records.size());

        for (final MetadataEntity metadata : records) {
            final Metadata data = convert(metadata, folderId);
            list.add(data);
        }

        final FetchDataResponse response = new FetchDataResponse();
        response.setMetadata(list);

        return response;
    }

    private static Metadata convert(final MetadataEntity entity, final String folderId) {
        final Metadata metaData = new Metadata();

        metaData.setDataType(convert(entity.getType()));
        metaData.setDataName(entity.getName());
        metaData.setDataId(entity.getExternalId());
        metaData.setCircleId(entity.getCircle().getExternalId());
        metaData.setAdded(entity.getCreated());
        metaData.setFolderId(folderId);

        return metaData;
    }

    private static DataType convert(final DataTypeEntity entity) {
        final DataType dataType = new DataType();

        dataType.setTypeName(entity.getName());
        dataType.setType(entity.getType());

        return dataType;
    }

    private SecretCWSKey extractCircleKey(final DataEntity entity) {
        final CircleEntity circle = entity.getMetadata().getCircle();

        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(circle.getId(), trustee.getCircle().getId())) {
                return crypto.extractCircleKey(entity.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
            }
        }

        throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "Could not extract the Key for the requested Data Object");
    }

    private String readFolder(final MetadataEntity folderEntity) {
        final Long parentId = folderEntity.getParentId();
        final MetadataEntity entity;

        if (parentId != null) {
            entity = dao.find(MetadataEntity.class, parentId);
        } else {
            entity = dao.findRootByMemberCircle(member, folderEntity.getCircle().getExternalId());
        }

        return entity.getExternalId();
    }
}
