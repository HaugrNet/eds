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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.entities.EDSEntity;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataTypeEntity;
import net.haugr.eds.core.model.entities.Externable;
import net.haugr.eds.core.model.entities.LoginAttemptEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.SettingEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import java.time.LocalDateTime;

/**
 * <p>Common DAO functionality, used throughout EDS.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public class CommonDao {

    protected static final String EXTERNAL_ID = "externalId";
    protected static final String PARENT_ID = "parentId";
    protected static final String MEMBER = "member";
    protected static final String STATUS = "status";
    protected static final String SINCE = "since";
    protected static final String NAME = "name";
    protected final EntityManager entityManager;

    /**
     * <p>Default Constructor.</p>
     *
     * @param entityManager EntityManager instance with transactional control
     */
    public CommonDao(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Prepares a new Named Query instance to be used directly.
     *
     * @param query Name of the Named Query
     * @return Prepared Query
     */
    public Query createNamedQuery(final String query) {
        return entityManager.createNamedQuery(query);
    }

    /**
     * <p>Persist, create or updates, an Entity. If it is derived from
     * {@link Externable}, then a new external Id (UUID) is set if it currently
     * does not have one.</p>
     *
     * <p>Regardless of an Entity is being created or updated, the method will
     * always update the alteration data and ensure that the creation date is
     * defined as well.</p>
     *
     * @param entity EDS Entity to persist (create or update)
     * @return Created or Updated EDS Entity
     */
    public EDSEntity save(final EDSEntity entity) {
        entity.setAltered(Utilities.newDate());
        final EDSEntity saved;

        if (entity.getId() == null) {
            if ((entity instanceof Externable externable) && (externable.getExternalId() == null)) {
                externable.setExternalId(UUID.randomUUID().toString());
            }
            entity.setAdded(Utilities.newDate());

            entityManager.persist(entity);
            saved = entity;
        } else {
            saved = entityManager.merge(entity);
        }

        return saved;
    }

    public <E extends EDSEntity> E find(final Class<E> edsEntity, final Long id) {
        return entityManager.find(edsEntity, id);
    }

    public <E extends EDSEntity> E find(final Class<E> edsEntity, final Long id, final LockModeType lockModeType) {
        return entityManager.find(edsEntity, id, lockModeType);
    }

    public <E extends Externable> E find(final Class<E> edsEntity, final String externalId) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(edsEntity);
        final Root<E> entity = query.from(edsEntity);
        query.select(entity).where(builder.equal(entity.get(EXTERNAL_ID), externalId));

        return findSingleRecord(entityManager.createQuery(query));
    }

    /**
     * <p>Uses the JPA {@link CriteriaBuilder} to create a new query to find
     * all matching records, sorted ascending according to the given
     * parameters.</p>
     *
     * @param edsEntity The EDS Entity to find all records for
     * @param orderBy   The field to order the ascending sorting by
     * @param <E>       The EDS Entity to use in the query
     * @return List of sorted records from the database
     */
    public <E extends EDSEntity> List<E> findAllAscending(final Class<E> edsEntity, final String orderBy) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(edsEntity);
        final Root<E> entity = query.from(edsEntity);
        query.orderBy(builder.asc(entity.get(orderBy)));

        return findList(entityManager.createQuery(query));
    }

    public <E extends EDSEntity> E getReference(final Class<E> edsEntity, final Long id) {
        return entityManager.getReference(edsEntity, id);
    }

    public <E extends EDSEntity> void delete(final E entity) {
        entityManager.remove(entity);
    }

    public void removeSession(final MemberEntity member) {
        member.setSessionChecksum(null);
        member.setSessionCrypto(null);
        member.setSessionExpire(null);

        save(member);
    }

    public MemberEntity findMemberByName(final String name) {
        final Query query = entityManager
                .createNamedQuery("member.findByName")
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public List<MemberEntity> findMemberByRole(final MemberRole role) {
        final Query query = entityManager
                .createNamedQuery("member.findByRole")
                .setParameter("role", role);

        return findList(query);
    }

    /**
     * The checksums are not having any uniqueness assigned to them, meaning
     * that there may exist multiple checksums with the same value - but as
     * all checksums are short-lived (hours), the problem is ignored.
     *
     * @param checksum Checksum of a Member SessionKey
     * @return MemberEntity with a matching SessionKey checksum
     */
    public MemberEntity findMemberByChecksum(final String checksum) {
        final Query query = entityManager
                .createNamedQuery("member.findByChecksum")
                .setParameter("checksum", checksum);

        return findSingleRecord(query);
    }

    public MemberEntity findMemberByNameAndCircleId(final String name, final String externalCircleId) {
        final Query query = entityManager
                .createNamedQuery("member.findByNameAndCircle")
                .setParameter("name", name)
                .setParameter("externalCircleId", externalCircleId);
        final List<MemberEntity> found = findList(query);

        if (found.isEmpty()) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING,
                    "No Trustee information found for member '" + name + "' and circle '" + externalCircleId + "'.");
        }

        return found.getFirst();
    }

    public List<TrusteeEntity> findTrusteesByMember(final MemberEntity member, final Set<TrustLevel> permissions) {
        final Query query = entityManager
                .createNamedQuery("trust.findByMember")
                .setParameter(MEMBER, member)
                .setParameter("permissions", permissions);

        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByMemberAndCircle(final MemberEntity member, final String externalCircleId, final Set<TrustLevel> permissions) {
        final Query query = entityManager
                .createNamedQuery("trust.findByMemberAndExternalCircleId")
                .setParameter(MEMBER, member)
                .setParameter("externalCircleId", externalCircleId)
                .setParameter("permissions", permissions);

        return findList(query);
    }

    public CircleEntity findCircleByName(final String name) {
        final Query query = entityManager
                .createNamedQuery("circle.findByName")
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public List<DataTypeEntity> findAllTypes() {
        final Query query = entityManager.createNamedQuery("type.findAll");

        return findList(query);
    }

    /**
     * Finds a unique DataType in the system. If none exists, or it is not
     * possible to find a unique record, then an Exception is thrown.
     *
     * @param name Name of the DataType to find
     * @return Found DataType Entity
     * @throws EDSException if no unique value could be found
     */
    public DataTypeEntity findDataTypeByName(final String name) {
        final Query query = entityManager
                .createNamedQuery("type.findByName")
                .setParameter("name", name);

        return findSingleRecord(query);
    }

    public long countDataTypeUsage(final DataTypeEntity dataType) {
        final Query query = entityManager
                .createNamedQuery("type.countUsage")
                .setParameter("type", dataType);

        return (long) query.getSingleResult();
    }

    public TrusteeEntity findTrusteeByCircleAndMember(final String externalCircleId, final String externalMemberId) {
        final Query query = entityManager
                .createNamedQuery("trustee.findByCircleAndMember")
                .setParameter("ecid", externalCircleId)
                .setParameter("emid", externalMemberId);

        return findSingleRecord(query);
    }

    public SettingEntity findSettingByKey(final StandardSetting setting) {
        final Query query = entityManager
                .createNamedQuery("setting.findByName")
                .setParameter("name", setting.getKey());

        return findSingleRecord(query);
    }

    public Long countMembers() {
        final Query query = entityManager.createNamedQuery("member.countMembers");
        final Object obj = findSingleRecord(query);

        return (Long) obj;
    }

    // =========================================================================
    // Login Attempt Methods (Rate-Limiting)
    // =========================================================================

    /**
     * Counts the number of failed login attempts for a given account
     * since the specified time.
     *
     * @param accountName Account name to check
     * @param since       Time threshold for counting attempts
     * @return Number of failed attempts
     */
    public long countRecentFailedAttempts(final String accountName, final LocalDateTime since) {
        final Query query = entityManager
                .createNamedQuery("loginAttempt.countRecentFailures")
                .setParameter("accountName", accountName)
                .setParameter(SINCE, since);

        return (Long) query.getSingleResult();
    }

    /**
     * Records a login attempt for rate-limiting tracking.
     *
     * @param accountName Account name that attempted login
     * @param success     Whether the attempt was successful
     * @param ipAddress   Optional IP address of the client
     */
    public void recordLoginAttempt(final String accountName, final boolean success, final String ipAddress) {
        final LoginAttemptEntity attempt = new LoginAttemptEntity();
        attempt.setAccountName(accountName);
        attempt.setSuccess(success);
        attempt.setIpAddress(ipAddress);
        save(attempt);
    }

    /**
     * Clears all login attempt records for a given account.
     * Called after successful authentication.
     *
     * @param accountName Account name to clear attempts for
     */
    public void clearLoginAttempts(final String accountName) {
        entityManager
                .createNamedQuery("loginAttempt.deleteByAccountName")
                .setParameter("accountName", accountName)
                .executeUpdate();
    }

    /**
     * Removes old login attempt records for cleanup purposes.
     *
     * @param cutoff Records older than this time will be deleted
     * @return Number of records deleted
     */
    public int cleanupOldLoginAttempts(final LocalDateTime cutoff) {
        return entityManager
                .createNamedQuery("loginAttempt.deleteOlderThan")
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup to simplify error handling
    // =========================================================================

    /**
     * <p>Returns the first record matching the query. If no records are found,
     * a null is returned.</p>
     *
     * <p>This method is preferred to be used over the JPA Query method
     * {@link Query#getFirstResult()}, as it throws an exception if no result
     * was found. Rather than having Exception handling to deal with a fairly
     * common case - a null is returned so it can be dealt with in a better
     * way.</p>
     *
     * <p>The Java 8 {@link java.util.Optional} class was discarded, since all
     * it does is wrap the null in an Object, that as well requires a check.
     * This does not mean that the {@link java.util.Optional} class is useless,
     * only that in this case, it is simply not used as the null has a specific
     * meaning.</p>
     *
     * @param query JPA Query to run
     * @param <E>   Entity Type to return
     * @return First Entity matching Query or null
     */
    public static <E> E findSingleRecord(final Query query) {
        final List<E> found = findList(query);

        return found.isEmpty() ? null : found.getFirst();
    }

    /**
     * <p>Wrapper for the JPA {@link Query#getResultList()} method, as it may
     * (according to the specifications) return a null rather than an empty
     * list if no records could be found.</p>
     *
     * @param query JPA Query to execute
     * @param <E>   Entity Type to return a list if
     * @return List of found Entities or an empty list if none were found
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> findList(final Query query) {
        try {
            final List<E> list = query.getResultList();

            // JPA does not specify the exact behavior of the getResultList()
            // call, hence if a null is returned, we're converting it into an
            // empty list. Which is always easier to deal with.
            return (list != null) ? list : new ArrayList<>(0);
        } catch (IllegalStateException | PersistenceException e) {
            // This should be and will hopefully remain unreachable code.
            throw new EDSException(ReturnCode.DATABASE_ERROR, e.getMessage(), e);
        }
    }
}
