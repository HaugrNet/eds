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
package net.haugr.cws.core.model;

import java.util.EnumSet;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import net.haugr.cws.api.common.TrustLevel;
import net.haugr.cws.api.dtos.Metadata;
import net.haugr.cws.core.model.entities.DataEntity;
import net.haugr.cws.core.model.entities.MemberEntity;
import net.haugr.cws.core.model.entities.MetadataEntity;

/**
 * <p>Data Access Object functionality used explicitly for the fetching &amp;
 * processing of data.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.1
 */
public final class DataDao extends CommonDao {

    public DataDao(final EntityManager entityManager) {
        super(entityManager);
    }

    public DataEntity findDataByMetadata(final MetadataEntity metadata) {
        final Query query = entityManager
                .createNamedQuery("data.findByMetadata")
                .setParameter("metadata", metadata);

        return findSingleRecord(query);
    }

    public DataEntity findDataByMemberAndExternalId(final MemberEntity member, final String externalId) {
        final Query query = entityManager
                .createNamedQuery("data.findByMemberAndExternalId")
                .setParameter(MEMBER, member)
                .setParameter(EXTERNAL_ID, externalId)
                .setParameter("trustLevels", EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE, TrustLevel.READ));

        return findSingleRecord(query);
    }

    public MetadataEntity findMetadataByMemberAndExternalId(final Long memberId, final String externalId) {
        final Query query = entityManager
                .createNamedQuery("metadata.findByMemberAndExternalId")
                .setParameter("mid", memberId)
                .setParameter("eid", externalId);

        return findSingleRecord(query);
    }

    public MetadataEntity findMetadataByMemberAndName(final Long memberId, final String name) {
        final Query query = entityManager
                .createNamedQuery("metadata.findByMemberAndName")
                .setParameter("mid", memberId)
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public MetadataEntity findRootByMemberCircle(final Long memberId, final String circleId) {
        final Query query = entityManager
                .createNamedQuery("metadata.findRootByMemberAndCircle")
                .setParameter("mid", memberId)
                .setParameter("cid", circleId);

        return findSingleRecord(query);
    }

    public List<MetadataEntity> findMetadataByMemberAndFolder(final MemberEntity member, final Long parentId, final int pageNumber, final int pageSize) {
        final Query query = entityManager
                .createNamedQuery("metadata.findByMemberAndFolder")
                .setParameter(MEMBER, member)
                .setParameter(PARENT_ID, parentId)
                .setMaxResults(pageSize)
                .setFirstResult((pageNumber - 1) * pageSize);

        return findList(query);
    }

    public long countFolderContent(final Long parentId) {
        final Query query = entityManager
                .createNamedQuery("metadata.countFolderContent")
                .setParameter(PARENT_ID, parentId);

        return (long) query.getSingleResult();
    }

    public boolean checkIfNameIsUsed(final Long metadataId, final String name, final Long parentId) {
        final Query query = entityManager
                .createNamedQuery("metadata.findByNameAndFolder")
                .setParameter("id", metadataId)
                .setParameter(NAME, name)
                .setParameter(PARENT_ID, parentId);

        return findSingleRecord(query) != null;
    }

    public MetadataEntity findInFolder(final MemberEntity member, final Long parentId, final String name) {
        final Query query = entityManager
                .createNamedQuery("metadata.findInFolder")
                .setParameter(MEMBER, member)
                .setParameter(PARENT_ID, parentId)
                .setParameter(NAME, name);

        return findSingleRecord(query);
    }

    public long countInventoryRecords() {
        final Query query = entityManager
                .createNamedQuery("metadata.countInventoryRecords");

        return (long) query.getSingleResult();
    }

    public List<MetadataEntity> readInventoryRecords(final int pageNumber, final int pageSize) {
        final Query query = entityManager
                .createNamedQuery("metadata.readInventoryRecords")
                .setMaxResults(pageSize)
                .setFirstResult((pageNumber - 1) * pageSize);

        return findList(query);
    }

    /**
     * <p>Converts a Metadata Entity to a DTO. As the relation between folders
     * and their content is made with a foreign key and not with the external
     * Id, the external folder Id is added as second parameter.</p>
     *
     * @param entity   The Metadata Entity to convert to a DTO
     * @param folderId The external folder ID
     * @return Converted Metadata DTO
     */
    public static Metadata convert(final MetadataEntity entity, final String folderId) {
        final Metadata metaData = new Metadata();

        metaData.setDataId(entity.getExternalId());
        metaData.setCircleId(entity.getCircle().getExternalId());
        metaData.setDataName(entity.getName());
        metaData.setTypeName(entity.getType().getName());
        metaData.setAdded(entity.getAdded());
        metaData.setFolderId(folderId);

        return metaData;
    }
}
