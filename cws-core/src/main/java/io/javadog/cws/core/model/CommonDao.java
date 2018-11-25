/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.model;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.entities.CWSEntity;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import io.javadog.cws.core.model.entities.Externable;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * <p>Common DAO functionality, used throughout CWS.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class CommonDao {

    protected static final String EXTERNAL_ID = "externalId";
    protected static final String MEMBER = "member";
    protected static final String PARENT_ID = "parentId";
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
     * <p>Persist, create or updates, an Entity. If it is derived from
     * {@link Externable}, then a new external Id (UUID) is set if it currently
     * does not have one.</p>
     *
     * <p>Regardless of an Entity is being created or updated, the method will
     * always update the alteration data and ensure that the creation date is
     * defined as well.</p>
     *
     * @param entity CWS Entity to persist (create or update)
     */
    public void persist(final CWSEntity entity) {
        if ((entity instanceof Externable) && (((Externable) entity).getExternalId() == null)) {
            ((Externable) entity).setExternalId(UUID.randomUUID().toString());
        }

        if (entity.getAdded() == null) {
            entity.setAdded(Utilities.newDate());
        }

        entity.setAltered(Utilities.newDate());
        entityManager.persist(entity);
    }

    public <E extends CWSEntity> E find(final Class<E> cwsEntity, final Long id) {
        return entityManager.find(cwsEntity, id);
    }

    public <E extends Externable> E find(final Class<E> cwsEntity, final String externalId) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(cwsEntity);
        final Root<E> entity = query.from(cwsEntity);
        query.select(entity).where(builder.equal(entity.get(EXTERNAL_ID), externalId));

        return findSingleRecord(entityManager.createQuery(query));
    }

    /**
     * <p>Uses the JPA {@link CriteriaBuilder} to create a new query to find
     * all matching records, sorted ascending according to the given
     * parameters.</p>
     *
     * @param cwsEntity The CWS Entity to find all records for
     * @param orderBy   The field to order the ascending sorting by
     * @param <E>       The CWS Entity to use in the query
     * @return List of sorted records from the database
     */
    public <E extends CWSEntity> List<E> findAllAscending(final Class<E> cwsEntity, final String orderBy) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(cwsEntity);
        final Root<E> entity = query.from(cwsEntity);
        query.orderBy(builder.asc(entity.get(orderBy)));

        return findList(entityManager.createQuery(query));
    }

    public <E extends CWSEntity> E getReference(final Class<E> cwsEntity, final Long id) {
        return entityManager.getReference(cwsEntity, id);
    }

    public <E extends CWSEntity> void delete(final E entity) {
        entityManager.remove(entity);
    }

    public void removeSession(final MemberEntity member) {
        member.setSessionChecksum(null);
        member.setSessionCrypto(null);
        member.setSessionExpire(null);

        persist(member);
    }

    public MemberEntity findMemberByName(final String name) {
        final Query query = entityManager.createNamedQuery("member.findByName");
        query.setParameter("name", toLower(name));

        return findSingleRecord(query);
    }

    public List<MemberEntity> findMemberByRole(final MemberRole role) {
        final Query query = entityManager.createNamedQuery("member.findByRole");
        query.setParameter("role", role);

        return findList(query);
    }

    /**
     * The checksums are not having any uniqueness assigned to them, meaning
     * that their may exist multiple checksum's with the same value - but as
     * all checksum's are fairly short-lived (hours), the problem is ignored
     * and only of their is an actual problem with it, will it be addressed.
     *
     * @param checksum Checksum of a Member SessionKey
     * @return MemberEntity with a matching SessionKey checksum
     */
    public MemberEntity findMemberByChecksum(final String checksum) {
        final Query query = entityManager.createNamedQuery("member.findByChecksum");
        query.setParameter("checksum", checksum);

        return findSingleRecord(query);
    }

    public MemberEntity findMemberByNameAndCircleId(final String name,
                                                    final String externalCircleId) {
        final Query query = entityManager.createNamedQuery("member.findByNameAndCircle");
        query.setParameter("name", toLower(name));
        query.setParameter("externalCircleId", externalCircleId);
        final List<MemberEntity> found = findList(query);

        if (found.isEmpty()) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING,
                    "No Trustee information found for member '" + name + "' and circle '" + externalCircleId + "'.");
        }

        return found.get(0);
    }

    public List<TrusteeEntity> findTrusteesByMember(final MemberEntity member,
                                                    final Set<TrustLevel> permissions) {
        final Query query = entityManager.createNamedQuery("trust.findByMember");
        query.setParameter(MEMBER, member);
        query.setParameter("permissions", permissions);

        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByMemberAndCircle(final MemberEntity member,
                                                             final String externalCircleId,
                                                             final Set<TrustLevel> permissions) {
        final Query query = entityManager.createNamedQuery("trust.findByMemberAndExternalCircleId");
        query.setParameter(MEMBER, member);
        query.setParameter("externalCircleId", externalCircleId);
        query.setParameter("permissions", permissions);

        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByCircle(final CircleEntity circle) {
        final Query query = entityManager.createNamedQuery("trustee.findByCircle");
        query.setParameter("circle", circle);

        return findList(query);
    }

    public CircleEntity findCircleByName(final String name) {
        final Query query = entityManager.createNamedQuery("circle.findByName");
        query.setParameter("name", toLower(name));

        return findSingleRecord(query);
    }

    public List<DataTypeEntity> findAllTypes() {
        final Query query = entityManager.createNamedQuery("type.findAll");

        return findList(query);
    }

    /**
     * Finds a unique DataType in the system. If none exist or it is not
     * possible to find a unique record, then an Exception is thrown.
     *
     * @param name Name of the DataType to find
     * @return Found DataType Entity
     * @throws CWSException if no unique value could be found
     */
    public DataTypeEntity findDataTypeByName(final String name) {
        final Query query = entityManager.createNamedQuery("type.findByName");
        query.setParameter("name", name);

        return findSingleRecord(query);
    }

    public long countDataTypeUsage(final DataTypeEntity dataType) {
        final Query query = entityManager.createNamedQuery("type.countUsage");
        query.setParameter("type", dataType);

        return (long) query.getSingleResult();
    }

    public TrusteeEntity findTrusteeByCircleAndMember(final String externalCircleId,
                                                      final String externalMemberId) {
        final Query query = entityManager.createNamedQuery("trustee.findByCircleAndMember");
        query.setParameter("ecid", externalCircleId);
        query.setParameter("emid", externalMemberId);

        return findSingleRecord(query);
    }

    public Long countMembers() {
        final Query query = entityManager.createNamedQuery("member.countMembers");
        final Object obj = findSingleRecord(query);

        return (Long) obj;
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup's to simplify error handling
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
    protected static <E> E findSingleRecord(final Query query) {
        final List<E> found = findList(query);

        return found.isEmpty() ? null : found.get(0);
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
    public static <E> List<E> findList(final Query query) {
        try {
            final List<E> list = query.getResultList();

            // JPA does not specify the exact behaviour of the getResultList()
            // call, hence if a null is returned, we're converting it into an
            // empty list. Which is always easier to deal with.
            return (list != null) ? list : new ArrayList<>(0);
        } catch (IllegalStateException | PersistenceException e) {
            // This should be and will hopefully remain unreachable code.
            throw new CWSException(ReturnCode.DATABASE_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Simpler wrapper for the {@link String#toLowerCase(Locale)}, to have a
     * standard way to invoke it which will prevent usage without a predefined
     * Locale.
     *
     * @param str String to convert to lower case with the CWS Locale
     * @return Lower case version of the String
     */
    private static String toLower(final String str) {
        return (str != null) ? str.toLowerCase(Settings.getInstance().getLocale()) : null;
    }
}
