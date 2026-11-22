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

import net.haugr.eds.core.features.EntryQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.CircleFeatureEntity;
import net.haugr.eds.core.model.entities.EntryEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.FeatureEntity;

/**
 * <p>Data access for the features (FEATURES.md): the features a stone offers, the
 * features of a Circle, and their entries.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class FeatureDao extends DataDao {

    // Every query of entries reads their Metadata and feature in the same
    // statement: read one by one afterwards, an entry which was removed
    // meanwhile would have lost them (EntityNotFoundException).
    private static final String ENTRIES = "select e from EntryEntity e join fetch e.metadata join fetch e.circleFeature";
    private static final String ENTRIES_OF_FEATURE = ENTRIES + " where e.circleFeature = :feature";
    private static final String FIND_OWN_ENTRY = ENTRIES_OF_FEATURE + " and e.kind = :kind and e.ownerTag in :owners order by e.position desc";
    private static final String FIND_OWN_ENTRY_ABOUT = ENTRIES_OF_FEATURE + " and e.kind = :kind and e.ownerTag in :owners and e.aboutId = :about order by e.position desc";
    private static final String FEATURE = "feature";
    private static final String ABOUT = "about";

    public FeatureDao(final EntityManager entityManager) {
        super(entityManager);
    }

    // =========================================================================
    // The features of the stone
    // =========================================================================

    public List<FeatureEntity> findFeatures() {
        return entityManager.createQuery("select t from FeatureEntity t order by t.name asc", FeatureEntity.class).getResultList();
    }

    public FeatureEntity findFeature(final String name) {
        return first(entityManager.createQuery("select t from FeatureEntity t where t.name = :name", FeatureEntity.class)
                .setParameter("name", name));
    }

    public long countCirclesUsing(final FeatureEntity feature) {
        return entityManager.createQuery("select count(c.id) from CircleFeatureEntity c where c.feature = :feature", Long.class)
                .setParameter(FEATURE, feature)
                .getSingleResult();
    }

    // =========================================================================
    // The features of a Circle
    // =========================================================================

    public List<CircleFeatureEntity> findCircleFeatures(final CircleEntity circle) {
        return entityManager.createQuery("select c from CircleFeatureEntity c where c.circle = :circle order by c.feature.name asc", CircleFeatureEntity.class)
                .setParameter("circle", circle)
                .getResultList();
    }

    public CircleFeatureEntity findCircleFeature(final CircleEntity circle, final String feature) {
        return first(entityManager.createQuery("select c from CircleFeatureEntity c where c.circle = :circle and c.feature.name = :feature", CircleFeatureEntity.class)
                .setParameter("circle", circle)
                .setParameter(FEATURE, feature));
    }

    /**
     * The feature whose entries are kept in the given folder.
     *
     * @param folderId Internal ID of a folder
     * @return The feature of the Circle, or null if it isn't a feature's folder
     */
    public CircleFeatureEntity findCircleFeatureByFolder(final Long folderId) {
        return first(entityManager.createQuery("select c from CircleFeatureEntity c where c.folderId = :folder", CircleFeatureEntity.class)
                .setParameter("folder", folderId));
    }

    // =========================================================================
    // Entries
    // =========================================================================

    public EntryEntity findEntry(final CircleFeatureEntity circleFeature, final String externalId) {
        return first(entityManager.createQuery(ENTRIES_OF_FEATURE + " and e.metadata.externalId = :id", EntryEntity.class)
                .setParameter(FEATURE, circleFeature)
                .setParameter("id", externalId));
    }

    public EntryEntity findEntryById(final Long id) {
        return (id != null) ? entityManager.find(EntryEntity.class, id) : null;
    }

    public EntryEntity findEntryByMetadata(final MetadataEntity metadata) {
        return first(entityManager.createQuery(ENTRIES + " where e.metadata = :metadata", EntryEntity.class)
                .setParameter("metadata", metadata));
    }

    /**
     * The entry of a kind which a Member wrote, for the kinds which keep
     * one per Member, or one per Member about each entry.
     *
     * @param circleFeature The feature of the Circle
     * @param kind       The kind
     * @param ownerTags  The owner tags of the Member: one, and while the Circle
     *                   Secret is rotated also the one of the retiring secret
     * @param aboutId    Internal ID of the entry it is about, or null for any
     * @return The entry, or null if the Member has none
     */
    public EntryEntity findOwnEntry(final CircleFeatureEntity circleFeature, final String kind, final java.util.Collection<String> ownerTags, final Long aboutId) {
        final TypedQuery<EntryEntity> query = entityManager.createQuery((aboutId != null) ? FIND_OWN_ENTRY_ABOUT : FIND_OWN_ENTRY, EntryEntity.class)
                .setParameter(FEATURE, circleFeature)
                .setParameter("kind", kind)
                .setParameter("owners", ownerTags);
        if (aboutId != null) {
            query.setParameter(ABOUT, aboutId);
        }

        return first(query);
    }

    /**
     * A page of the entries of a feature in a Circle.
     *
     * @param circleFeature The feature of the Circle
     * @param filter     The kind, name, positions and order asked for
     * @param aboutId    Only entries about this entry (internal ID), or null
     * @param limit      The most entries to return
     * @return The entries
     */
    public List<EntryEntity> findEntries(final CircleFeatureEntity circleFeature, final EntryQuery filter, final Long aboutId, final int limit) {
        final String kind = filter.kind();
        final String namePrefix = filter.namePrefix();
        final Long after = filter.after();
        final Long before = filter.before();
        final StringBuilder jpql = new StringBuilder(ENTRIES_OF_FEATURE);
        if (kind != null) {
            jpql.append(" and e.kind = :kind");
        }
        if (aboutId != null) {
            jpql.append(" and e.aboutId = :about");
        }
        if (namePrefix != null) {
            jpql.append(" and e.metadata.name like :name escape '!'");
        }
        if (after != null) {
            jpql.append(" and e.position > :after");
        }
        if (before != null) {
            jpql.append(" and e.position < :before");
        }
        jpql.append(" order by e.position ").append(filter.oldestFirst() ? "asc" : "desc");

        final TypedQuery<EntryEntity> query = entityManager.createQuery(jpql.toString(), EntryEntity.class).setParameter(FEATURE, circleFeature);
        if (kind != null) {
            query.setParameter("kind", kind);
        }
        if (aboutId != null) {
            query.setParameter(ABOUT, aboutId);
        }
        if (namePrefix != null) {
            query.setParameter("name", namePrefix.replace("!", "!!").replace("%", "!%").replace("_", "!_") + '%');
        }
        if (after != null) {
            query.setParameter("after", after);
        }
        if (before != null) {
            query.setParameter("before", before);
        }

        return query.setMaxResults(limit).getResultList();
    }

    public long countEntries(final CircleFeatureEntity circleFeature, final String kind, final Long after) {
        final StringBuilder jpql = new StringBuilder("select count(e.id) from EntryEntity e where e.circleFeature = :feature");
        if (kind != null) {
            jpql.append(" and e.kind = :kind");
        }
        if (after != null) {
            jpql.append(" and e.position > :after");
        }
        final TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class).setParameter(FEATURE, circleFeature);
        if (kind != null) {
            query.setParameter("kind", kind);
        }
        if (after != null) {
            query.setParameter("after", after);
        }

        return query.getSingleResult();
    }

    public List<EntryEntity> findEntriesAbout(final Long entryId) {
        return entityManager.createQuery(ENTRIES + " where e.aboutId = :about", EntryEntity.class)
                .setParameter(ABOUT, entryId)
                .getResultList();
    }

    public List<EntryEntity> findEntriesOlderThan(final CircleFeatureEntity circleFeature, final String kind, final LocalDateTime before) {
        return entityManager.createQuery(ENTRIES_OF_FEATURE + " and e.kind = :kind and e.added < :before", EntryEntity.class)
                .setParameter(FEATURE, circleFeature)
                .setParameter("kind", kind)
                .setParameter("before", before)
                .getResultList();
    }

    /**
     * The entries of a kind beyond the newest ones.
     *
     * @param circleFeature The feature of the Circle
     * @param kind       The kind
     * @param keep       The number of the newest entries to pass over
     * @return The older entries
     */
    public List<EntryEntity> findEntriesBeyond(final CircleFeatureEntity circleFeature, final String kind, final int keep) {
        return entityManager.createQuery(ENTRIES_OF_FEATURE + " and e.kind = :kind order by e.position desc", EntryEntity.class)
                .setParameter(FEATURE, circleFeature)
                .setParameter("kind", kind)
                .setFirstResult(keep)
                .getResultList();
    }

    public List<EntryEntity> findAllEntries(final CircleFeatureEntity circleFeature) {
        return entityManager.createQuery(ENTRIES_OF_FEATURE, EntryEntity.class)
                .setParameter(FEATURE, circleFeature)
                .getResultList();
    }

    private static <T> T first(final TypedQuery<T> query) {
        final List<T> found = query.setMaxResults(1).getResultList();
        return found.isEmpty() ? null : found.getFirst();
    }
}
