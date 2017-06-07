/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Data;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.api.requests.FetchDataRequest;
import io.javadog.cws.api.responses.FetchDataResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.MetaDataEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.crypto.spec.IvParameterSpec;
import javax.persistence.EntityManager;
import java.security.Key;
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
        final String externalDataId = request.getDataId();
        final FetchDataResponse response;

        if (externalDataId == null) {
            final DataType type = request.getDataType();
            if (type != null) {
                response = readAllMetadataForCircleAndType(request.getCircleId(), type);

            } else {
                response = readAllMetadataForCircle(request.getCircleId());
            }
        } else {
            final MetaDataEntity entity = dao.findMetaDataByMemberAndExternalId(member, externalDataId);
            if (entity != null) {
                if (Objects.equals("Folder", entity.getType().getType())) {
                    response = readFolderContent(entity, request.getDataType());
                } else {
                    response = readCompleteDataObject(entity);
                }
            } else {
                response = new FetchDataResponse(ReturnCode.IDENTIFICATION_WARNING, "No information could be found for the given Id.");
            }
        }

        return response;
    }

    private FetchDataResponse readAllMetadataForCircleAndType(final String circleId, final DataType type) {
        final MetaDataEntity root = dao.findRootByMemberCircle(member, circleId);
        final List<MetaDataEntity> data = dao.findMetadataByMemberFolderAndType(member, root, type);

        return prepareResponse(root.getExternalId(), data);
    }

    private FetchDataResponse readAllMetadataForCircle(final String circleId) {
        final MetaDataEntity root = dao.findRootByMemberCircle(member, circleId);
        final List<MetaDataEntity> data = dao.findMetadataByMemberAndFolder(member, root);

        return prepareResponse(root.getExternalId(), data);
    }

    private FetchDataResponse readFolderContent(final MetaDataEntity entity, final DataType dataType) {
        final List<MetaDataEntity> entities;

        if (dataType != null) {
            entities = dao.findMetadataByMemberFolderAndType(member, entity, dataType);
        } else {
            entities = dao.findMetadataByMemberAndFolder(member, entity);
        }

        return prepareResponse(entity.getExternalId(), entities);
    }

    private static FetchDataResponse prepareResponse(final String folderId, final List<MetaDataEntity> records) {
        final List<Data> list = new ArrayList<>(records.size());

        for (final MetaDataEntity metadata : records) {
            final Data data = new Data();
            data.setId(metadata.getExternalId());
            data.setCircleId(metadata.getCircle().getExternalId());
            data.setFolderId(folderId);
            data.setName(metadata.getName());
            data.setTypeName(metadata.getType().getName());
            data.setAdded(metadata.getCreated());

            list.add(data);
        }

        final FetchDataResponse response = new FetchDataResponse();
        response.setData(list);

        return response;
    }

    private FetchDataResponse readCompleteDataObject(final MetaDataEntity metadata) {
        // Following Query will read out a specific Data Records with meta data
        // information, if the person is allowed, which includes checks for
        // Circle Membership and right TrustLevel of the Member. If no Entity
        // is found, then there can be multiple reasons.
        final DataEntity entity = dao.findDataByMemberAndExternalId(member, metadata.getExternalId());

        if (entity != null) {
            final Key key = extractCircleKey(entity);
            final IvParameterSpec iv = crypto.generateInitialVector(entity.getInitialVector());
            final byte[] bytes = crypto.decrypt(key, iv, entity.getData());
            final FetchDataResponse response = new FetchDataResponse();
            final Data data = new Data();
            data.setTypeName(entity.getMetadata().getType().getName());
            data.setName(entity.getMetadata().getName());
            data.setId(entity.getMetadata().getExternalId());
            data.setCircleId(entity.getMetadata().getCircle().getExternalId());
            data.setAdded(entity.getMetadata().getCreated());
            data.setFolderId(readFolder(entity.getMetadata().getParentId()));
            data.setBytes(bytes);
            final List<Data> objects = new ArrayList<>(1);
            objects.add(data);
            response.setData(objects);

            return response;
        } else {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "No Data Object found, matching the provided information or member is lacking privileges to read the actual data.");
        }
    }

    private Key extractCircleKey(final DataEntity entity) {
        final CircleEntity circle = entity.getMetadata().getCircle();

        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(circle.getId(), trustee.getCircle().getId())) {
                return crypto.extractCircleKey(member.getKeyPair().getPrivate(), trustee.getCircleKey(), entity.getKey().getAlgorithm());
            }
        }

        throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "Could not extract the Key for the requested Data Object");
    }

    private String readFolder(final Long folderId) {
        final MetaDataEntity entity = dao.find(MetaDataEntity.class, folderId);

        return entity.getExternalId();
    }
}
