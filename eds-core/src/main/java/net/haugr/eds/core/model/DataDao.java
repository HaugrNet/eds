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
package net.haugr.eds.core.model;

import java.util.EnumSet;
import java.util.List;
import jakarta.persistence.EntityManager;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.api.common.Utilities;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.Session;
import jakarta.persistence.Query;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.dtos.Metadata;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;

/**
 * <p>Data Access Object functionality used explicitly for the fetching &amp;
 * processing of data.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.1
 */
public class DataDao extends CommonDao {

    private static final String METADATA = "metadata";

    public DataDao(final EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * Work with the JDBC Connection of the current transaction.
     *
     * @param <T> Result type
     */
    @FunctionalInterface
    public interface ConnectionWork<T> {

        T execute(Connection connection) throws SQLException;
    }

    /**
     * <p>Runs the given work with the JDBC Connection of the EntityManager's
     * current transaction, e.g. to stream the encrypted data with
     * {@link EncryptedDataStore}. Pending JPA changes are flushed first, so
     * the work sees them.</p>
     *
     * @param work The work
     * @param <T>  Result type
     * @return Result of the work
     */
    public <T> T withConnection(final ConnectionWork<T> work) {
        entityManager.flush();
        return entityManager.unwrap(Session.class).doReturningWork(work::execute);
    }

    /**
     * <p>The internal Id and the Key (Circle Secret version) of the content of
     * a Data Object, without reading the content itself, which may be large
     * (PLAN.md, Streaming).</p>
     *
     * @param metadata The Data Object
     * @return {@code [Long dataId, Long keyId]}, or null if it has no content
     */
    public Object[] findContentInfo(final MetadataEntity metadata) {
        final Query query = entityManager
                .createNamedQuery("data.findContentInfoByMetadata")
                .setParameter(METADATA, metadata);

        return findSingleRecord(query);
    }

    /**
     * Sets the Key of the content, before its content is replaced by a
     * streamed write, without loading the old content.
     *
     * @param dataId Internal Id of the content
     * @param key    The Key (current Circle Secret)
     */
    public void updateContentKey(final long dataId, final KeyEntity key) {
        entityManager.createNamedQuery("data.updateContentKey")
                .setParameter("id", dataId)
                .setParameter("key", key)
                .setParameter("status", SanityStatus.OK)
                .setParameter("checked", Utilities.newDate())
                .executeUpdate();
    }

    /**
     * Sets the Sanity status of the content, e.g. when a chunk fails its
     * check while it is streamed.
     *
     * @param dataId Internal Id of the content
     * @param status New status
     */
    public void updateSanityStatus(final long dataId, final SanityStatus status) {
        entityManager.createNamedQuery("data.updateSanityStatus")
                .setParameter("id", dataId)
                .setParameter("status", status)
                .setParameter("checked", Utilities.newDate())
                .executeUpdate();
    }

    /**
     * Deletes the content of a Data Object, without loading it.
     *
     * @param metadata The Data Object
     */
    public void deleteContent(final MetadataEntity metadata) {
        // A managed instance of the content (e.g. written earlier in the same
        // persistence context) would re-persist the Metadata on flush, via
        // its cascade, after the bulk delete. It is detached first;
        // getReference doesn't load the content.
        final Object[] info = findContentInfo(metadata);
        if (info != null) {
            entityManager.detach(entityManager.getReference(DataEntity.class, info[0]));
        }
        entityManager.createNamedQuery("data.deleteByMetadata")
                .setParameter(METADATA, metadata)
                .executeUpdate();
    }

    public DataEntity findDataByMetadata(final MetadataEntity metadata) {
        final Query query = entityManager
                .createNamedQuery("data.findByMetadata")
                .setParameter(METADATA, metadata);

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

    /**
     * Lists a page of a folder, without the hidden entries (names starting
     * with a dot).
     *
     * @param member     The requesting Member
     * @param parentId   The folder
     * @param pageNumber Page number, from 1
     * @param pageSize   Page size
     * @return The page
     */
    public List<MetadataEntity> findVisibleMetadataByMemberAndFolder(final MemberEntity member, final Long parentId, final int pageNumber, final int pageSize) {
        final Query query = entityManager
                .createNamedQuery("metadata.findVisibleByMemberAndFolder")
                .setParameter(MEMBER, member)
                .setParameter(PARENT_ID, parentId)
                .setMaxResults(pageSize)
                .setFirstResult((pageNumber - 1) * pageSize);

        return findList(query);
    }

    /**
     * Counts the entries of a folder, without the hidden ones.
     *
     * @param parentId The folder
     * @return Number of visible entries
     */
    public long countVisibleFolderContent(final Long parentId) {
        final Query query = entityManager
                .createNamedQuery("metadata.countVisibleFolderContent")
                .setParameter(PARENT_ID, parentId);

        return (long) query.getSingleResult();
    }

    /**
     * Reads the thumbnails of the given Data Objects (PLAN.md, L5); the
     * parent of a thumbnail is the Data Object it shows.
     *
     * @param parentIds Internal IDs of the Data Objects
     * @return The thumbnails
     */
    public List<MetadataEntity> findThumbnails(final java.util.Collection<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        final Query query = entityManager
                .createNamedQuery("metadata.findThumbnails")
                .setParameter("parentIds", parentIds);

        return findList(query);
    }

    /**
     * Removes the thumbnails of a Data Object, with their content.
     *
     * @param metadata The Data Object
     */
    public void deleteThumbnails(final MetadataEntity metadata) {
        for (final MetadataEntity thumbnail : findThumbnails(List.of(metadata.getId()))) {
            deleteContent(thumbnail);
            delete(thumbnail);
        }
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

    /**
     * Checks if another Data Object in the folder has the Name Key, the keyed
     * hash of a name a client of a chamber sends (E2E.md).
     *
     * @param metadataId The Data Object which may keep its own Name Key, or
     *                   null for a new one
     * @param nameKey    The Name Key
     * @param parentId   The folder
     * @return True if another Data Object in the folder has the Name Key
     */
    public boolean checkIfNameKeyIsUsed(final Long metadataId, final String nameKey, final Long parentId) {
        final Query query = entityManager
                .createNamedQuery("metadata.findByNameKeyInFolder")
                .setParameter("id", (metadataId != null) ? metadataId : -1L)
                .setParameter("nameKey", nameKey)
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

    /**
     * <p>Converts a Metadata Entity to a DTO. As the relation between folders
     * and their content is made with a foreign key and not with the external
     * Id, the external folder Id is added as the second parameter.</p>
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
        metaData.setNameKey(entity.getNameKey());
        metaData.setTypeName(entity.getType().getName());
        metaData.setMediaType(entity.getMediaType());
        metaData.setAdded(entity.getAdded());
        metaData.setFolderId(folderId);

        return metaData;
    }

    /**
     * Reads the ID of the Circle of a Data Object, without a check of the
     * access; for the change events of the chat hub.
     *
     * @param externalDataId The Data Object
     * @return The Circle's ID, or null if the Data Object doesn't exist
     */
    public String findCircleIdOfData(final String externalDataId) {
        final List<String> found = entityManager
                .createQuery("select m.circle.externalId from MetadataEntity m where m.externalId = :eid", String.class)
                .setParameter("eid", externalDataId)
                .getResultList();
        return found.isEmpty() ? null : found.getFirst();
    }
}
