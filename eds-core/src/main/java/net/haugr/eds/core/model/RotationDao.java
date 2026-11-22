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

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.EntryEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>The queries of the rotation of a Circle Secret (PLAN.md, Crypto C6): the
 * secrets of a Circle, its Trustees and entries, and its Data Objects by the
 * secret they are encrypted with.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class RotationDao extends DataDao {

    private static final String CIRCLE = "circle";
    private static final String KEY = "key";

    public RotationDao(final EntityManager entityManager) {
        super(entityManager);
    }

    /**
     * The Circle Secret of a Circle with the status, the newest first.
     *
     * @param circle The Circle
     * @param status ACTIVE or RETIRING
     * @return The key row, or null
     */
    public KeyEntity findKey(final CircleEntity circle, final Status status) {
        final List<KeyEntity> keys = entityManager.createNamedQuery("key.findByCircleAndStatus", KeyEntity.class)
                .setParameter(CIRCLE, circle)
                .setParameter("status", status)
                .setMaxResults(1)
                .getResultList();
        return keys.isEmpty() ? null : keys.getFirst();
    }

    /**
     * The Circle Secrets of the Circles with the status.
     *
     * @param circles The Circles
     * @param status  ACTIVE or RETIRING
     * @return Their key rows
     */
    public List<KeyEntity> findKeys(final Collection<CircleEntity> circles, final Status status) {
        return circles.isEmpty()
                ? List.of()
                : entityManager.createNamedQuery("key.findForCircles", KeyEntity.class)
                        .setParameter("circles", circles)
                        .setParameter("status", status)
                        .getResultList();
    }

    public List<TrusteeEntity> findTrustees(final CircleEntity circle) {
        return entityManager.createNamedQuery("trustee.findByCircle", TrusteeEntity.class)
                .setParameter(CIRCLE, circle)
                .getResultList();
    }

    /**
     * The next entries of features of the Circle, by their ID.
     *
     * @param circle  The Circle
     * @param afterId The last ID of the batch before
     * @param limit   How many at most
     * @return The entries
     */
    public List<EntryEntity> findEntries(final CircleEntity circle, final long afterId, final int limit) {
        return entityManager.createQuery("select e from EntryEntity e where e.circleFeature.circle = :circle and e.id > :after order by e.id asc", EntryEntity.class)
                .setParameter(CIRCLE, circle)
                .setParameter("after", afterId)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Pending invitations into the Circle, sealed with its Circle Secret, are
     * deleted when the secret is rotated: they can't be sealed anew without
     * the invitation's token.
     *
     * @param circle The Circle
     * @return How many were deleted
     */
    public int deleteInvitations(final CircleEntity circle) {
        return entityManager.createQuery("delete from InvitationEntity i where i.circle = :circle")
                .setParameter(CIRCLE, circle)
                .executeUpdate();
    }

    /**
     * The next Data Objects on a Circle Secret, by their content row.
     *
     * @param key   The key row of the secret
     * @param limit How many at most
     * @return Internal IDs of the content rows
     */
    public List<Long> findDataOnKey(final KeyEntity key, final int limit) {
        // Content which failed its check can't be re-encrypted, so it isn't tried again.
        return entityManager.createQuery("select d.id from DataEntity d where d.key = :key and d.sanityStatus <> :failed order by d.id asc", Long.class)
                .setParameter(KEY, key)
                .setParameter("failed", SanityStatus.FAILED)
                .setMaxResults(limit)
                .getResultList();
    }

    public long countDataOnKey(final KeyEntity key) {
        return entityManager.createQuery("select count(d.id) from DataEntity d where d.key = :key", Long.class)
                .setParameter(KEY, key)
                .getSingleResult();
    }

    public long countData(final CircleEntity circle) {
        return entityManager.createQuery("select count(d.id) from DataEntity d where d.metadata.circle = :circle", Long.class)
                .setParameter(CIRCLE, circle)
                .getSingleResult();
    }

    /**
     * Deletes the row of a retired Circle Secret; with it go pending
     * invitations which were sealed with it (by the database). It fails if
     * content was written with it meanwhile.
     *
     * @param keyId The key row
     */
    public void deleteKey(final long keyId) {
        entityManager.createQuery("delete from KeyEntity k where k.id = :id")
                .setParameter("id", keyId)
                .executeUpdate();
    }

    /**
     * Locks the content row until the end of the transaction, and tells the
     * secret it is on. Only the row of {@code eds_data} is locked: a locking
     * read of the entity would join its Data Object, key and Circle, and
     * some databases (H2) lock the rows of all of them.
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
     * The external ID of the Data Object of a content row.
     *
     * @param id Internal ID of the content row
     * @return The Data Object's external ID
     */
    public String findObjectId(final long id) {
        return entityManager.createQuery("select d.metadata.externalId from DataEntity d where d.id = :id", String.class)
                .setParameter("id", id)
                .getSingleResult();
    }
}
