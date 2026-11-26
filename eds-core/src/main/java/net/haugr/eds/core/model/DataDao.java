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
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.jpa.HibernateHints;
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
     * Locks the content row until the end of the transaction, and tells the
     * secret it is on. Only the row of {@code eds_data} is locked: a locking
     * read of the entity would join its Data Object, key and Circle, and
     * some databases (H2) lock the rows of all of them. The rotation of the
     * Circle Secret holds it while it replaces the content and its secret,
     * so a copy which holds it reads the content with the secret it is on.
     *
     * @param id Internal ID of the content row
     * @return Key row of its secret, or null if the row is gone
     */
    public Long lockData(final long id) {
        final List<?> found = entityManager.createNativeQuery("SELECT key_id FROM eds_data WHERE id = ? FOR UPDATE")
                .setParameter(1, id)
                .getResultList();
        return found.isEmpty() ? null : ((Number) found.getFirst()).longValue();
    }

    /**
     * Locks the content rows of a Data Object and of its thumbnails, in the
     * order of their IDs, as the delete of its Circle takes them; a request
     * which replaces, moves or deletes the Data Object does so before it
     * changes anything, so two of them, or one and the delete of the Circle,
     * never hold one of the rows each while they wait for the other.
     *
     * @param metadata The Data Object
     */
    public void lockContentWithThumbnails(final MetadataEntity metadata) {
        // The rows are found first, and then locked one by one in the order
        // of their IDs. In one statement, with "OR metadata_id IN (subquery)",
        // the index of metadata_id couldn't be used, and PostgreSQL read all
        // content rows each time (the load run of 2026-11-23); and with
        // "metadata_id IN (...) ORDER BY id FOR UPDATE", H2 locks the rows in
        // the order of that index, not of their IDs. A row added between the
        // two steps is a new thumbnail, which no other request holds yet.
        final List<Long> ids = new ArrayList<>();
        for (final Object id : entityManager.createNativeQuery("SELECT d.id FROM eds_data d WHERE d.metadata_id = ? "
                        + "UNION ALL SELECT d.id FROM eds_data d JOIN eds_metadata m ON m.id = d.metadata_id "
                        + "JOIN eds_datatypes t ON t.id = m.datatype_id WHERE m.parent_id = ? AND t.datatype_name = 'thumbnail'")
                .setParameter(1, metadata.getId())
                .setParameter(2, metadata.getId())
                .getResultList()) {
            ids.add(((Number) id).longValue());
        }
        ids.sort(null);
        for (final Long id : ids) {
            entityManager.createNativeQuery("SELECT id FROM eds_data WHERE id = ? FOR UPDATE")
                    .setParameter(1, id)
                    .getResultList();
        }
    }

    /**
     * Deletes the content of a Data Object, without loading it. The content
     * row goes before its parts, which the database deletes with it (a bulk
     * delete of the entity would take the parts first), as the rotation of
     * the Circle Secret holds the row while it replaces the parts.
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
            // The parts go with the row, so their length is taken from the
            // Circle's total before. The row is held first, so a rotation
            // can't replace the parts between the two.
            entityManager.createNativeQuery("SELECT id FROM eds_data WHERE metadata_id = ? ORDER BY id FOR UPDATE")
                    .setParameter(1, metadata.getId())
                    .getResultList();
            changeStoredBytes(metadata.getCircle().getId(), -storedLengthOfObject(metadata.getId()));
        }
        entityManager.createNativeQuery("DELETE FROM eds_data WHERE metadata_id = ?")
                .setParameter(1, metadata.getId())
                .executeUpdate();
    }

    /**
     * Writes the content of an existing row with the {@link EncryptedDataStore}
     * (an upload, a copy, a rotation), and changes the stored bytes of its
     * Circle by the difference of its stored length, measured before and
     * after: content which is replaced counts as removed.
     *
     * @param circleId Internal ID of the Circle of the content
     * @param dataId   Internal ID of the content row
     * @param write    The write, on the transaction's Connection
     * @param <T>      Result type
     * @return Result of the write
     */
    public <T> T writeContent(final long circleId, final long dataId, final ConnectionWork<T> write) {
        final long[] difference = new long[1];
        final T result = withConnection(connection -> {
            final long before = Math.max(0, EncryptedDataStore.length(connection, dataId));
            final T written = write.execute(connection);
            difference[0] = Math.max(0, EncryptedDataStore.length(connection, dataId)) - before;
            return written;
        });
        changeStoredBytes(circleId, difference[0]);

        return result;
    }

    /**
     * The content of each of the given Metadata, with its parts, in one
     * query. For a read only, what isn't in the persistence context yet is
     * loaded read-only: no copy is kept to compare at the flush.
     *
     * @param metadata The Metadata
     * @param onlyRead Whether the content is only read in this transaction
     * @return The content found, by the internal ID of its Metadata
     */
    public Map<Long, DataEntity> findDataByMetadata(final Collection<MetadataEntity> metadata, final boolean onlyRead) {
        final Map<Long, DataEntity> found = HashMap.newHashMap(metadata.size());
        if (!metadata.isEmpty()) {
            for (final DataEntity data : entityManager.createQuery(
                            "select distinct d from DataEntity d left join fetch d.parts where d.metadata in :metadata", DataEntity.class)
                    .setParameter(METADATA, metadata)
                    .setHint(HibernateHints.HINT_READ_ONLY, onlyRead)
                    .getResultList()) {
                found.put(data.getMetadata().getId(), data);
            }
        }
        return found;
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
    public List<MetadataEntity> findThumbnails(final Collection<Long> parentIds) {
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
