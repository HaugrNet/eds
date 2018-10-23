/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2018, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.model;

import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.MetadataEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.EnumSet;
import java.util.List;

/**
 * <p>Data Access Object functionality used explicitly for the fetching &amp;
 * processing of data.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.1
 */
public final class DataDao extends CommonDao {

    public DataDao(final EntityManager entityManager) {
        super(entityManager);
    }

    public DataEntity findDataByMetadata(final MetadataEntity metadata) {
        final Query query = entityManager.createNamedQuery("data.findByMetadata");
        query.setParameter("metadata", metadata);

        return findSingleRecord(query);
    }

    public DataEntity findDataByMemberAndExternalId(final MemberEntity member,
                                                    final String externalId) {
        final Query query = entityManager.createNamedQuery("data.findByMemberAndExternalId");
        query.setParameter(MEMBER, member);
        query.setParameter(EXTERNAL_ID, externalId);
        query.setParameter("trustLevels", EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE, TrustLevel.READ));

        return findSingleRecord(query);
    }

    public MetadataEntity findMetaDataByMemberAndExternalId(final Long memberId,
                                                            final String externalId) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndExternalId");
        query.setParameter("mid", memberId);
        query.setParameter("eid", externalId);

        return findSingleRecord(query);
    }

    public MetadataEntity findRootByMemberCircle(final Long memberId,
                                                 final String circleId) {
        final Query query = entityManager.createNamedQuery("metadata.findRootByMemberAndCircle");
        query.setParameter("mid", memberId);
        query.setParameter("cid", circleId);

        return findSingleRecord(query);
    }

    public List<MetadataEntity> findMetadataByMemberAndFolder(final MemberEntity member,
                                                              final Long parentId,
                                                              final int pageNumber,
                                                              final int pageSize) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndFolder");
        query.setParameter(MEMBER, member);
        query.setParameter(PARENT_ID, parentId);
        query.setMaxResults(pageSize);
        query.setFirstResult((pageNumber - 1) * pageSize);

        return findList(query);
    }

    public long countFolderContent(final Long parentId) {
        final Query query = entityManager.createNamedQuery("metadata.countFolderContent");
        query.setParameter(PARENT_ID, parentId);

        return (long) query.getSingleResult();
    }

    public boolean checkIfNameIsUsed(final Long metadataId,
                                     final String name,
                                     final Long parentId) {
        final Query query = entityManager.createNamedQuery("metadata.findByNameAndFolder");
        query.setParameter("id", metadataId);
        query.setParameter(NAME, name);
        query.setParameter(PARENT_ID, parentId);

        return findSingleRecord(query) != null;
    }

    public MetadataEntity findInFolder(final MemberEntity member,
                                       final Long parentId,
                                       final String name) {
        final Query query = entityManager.createNamedQuery("metadata.findInFolder");
        query.setParameter(MEMBER, member);
        query.setParameter(PARENT_ID, parentId);
        query.setParameter(NAME, name);

        return findSingleRecord(query);
    }
}
