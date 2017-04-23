/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.jpa;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.Externable;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.SettingEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CommonJpaDao implements CommonDao {

    private final EntityManager entityManager;

    public CommonJpaDao(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends CWSEntity> E persist(final E entity) {
        if ((entity instanceof Externable) && (((Externable) entity).getExternalId() == null)) {
            ((Externable) entity).setExternalId(UUID.randomUUID().toString());
        }

        if (entity.getCreated() == null) {
            entity.setCreated(new Date());
        }

        entity.setModified(new Date());
        entityManager.persist(entity);

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends CWSEntity> E find(final Class<E> cwsEntity, final Long id) {
        return entityManager.find(cwsEntity, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemberEntity findMemberByName(final String name) {
        final Query query = entityManager.createNamedQuery("member.findByName");
        query.setParameter("name", name);

        return findUniqueRecord(query, "member", name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TrusteeEntity> findTrustByMember(final MemberEntity member) {
        final Query query = entityManager.createNamedQuery("trust.findByMemberId");
        query.setParameter("id", member.getId());

        return query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SettingEntity> readSettings() {
        final Query query = entityManager.createNamedQuery("setting.readAll");
        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TrusteeEntity> findTrusteesByCircle(final CircleEntity circle) {
        final Query query = entityManager.createNamedQuery("trustee.findByCircleId");
        query.setParameter("circleId", circle.getId());

        return findExistingList(query, "trustees");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircleEntity findCircleByExternalId(String externalId) {
        final Query query = entityManager.createNamedQuery("circle.findByExternalId");
        query.setParameter("eid", externalId);

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CircleEntity> findAllCircles() {
        final Query query = entityManager.createNamedQuery("circle.findAll");

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MemberEntity> findAllMembers() {
        final Query query = entityManager.createNamedQuery("member.findAll");
        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemberEntity findMemberByExternalId(final String externalId) {
        final Query query = entityManager.createNamedQuery("member.findByExternalId");
        query.setParameter("externalId", externalId);

        final List<MemberEntity> found = findList(query);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CircleEntity> findCirclesForMember(final MemberEntity requested) {
        final Query query = entityManager.createNamedQuery("trustee.findCirclesByMember");
        query.setParameter("memberId", requested.getId());

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CircleEntity> findCirclesBothBelongTo(final MemberEntity member, final MemberEntity requested) {
        final Query query = entityManager.createNamedQuery("trustee.findSharedCircles");
        query.setParameter("member", member.getId());
        query.setParameter("requested", requested.getId());

        return findList(query);
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup's to simplify error handling
    // =========================================================================

    private static <E> List<E> findExistingList(final Query query, final String listName) {
        List<E> list = findList(query);

        if (list.isEmpty()) {
            throw new ModelException(Constants.IDENTIFICATION_WARNING, "No " + listName + " found.");
        }

        return list;
    }

    private static <E> E findSingleRecord(final Query query) {
        final List<E> found = query.getResultList();

        return found.size() >= 1 ? found.get(0) : null;
    }

    private static <E> E findUniqueRecord(final Query query, final String entityName, final String keyName) {
        final List<E> found = query.getResultList();

        if (found.isEmpty()) {
            throw new ModelException(Constants.IDENTIFICATION_WARNING, "No " + entityName + " found with '" + keyName + "'.");
        }

        if (found.size() > 1) {
            throw new ModelException(Constants.CONSTRAINT_ERROR, "Could not uniquely identify a " + entityName + " with '" + keyName + "'.");
        }

        return found.get(0);
    }

    private static <E> List<E> findList(final Query query) {
        try {
            List<E> list = query.getResultList();

            if (list == null) {
                list = new ArrayList<>(0);
            }

            return list;
        } catch (IllegalStateException | PersistenceException e) {
            throw new ModelException(Constants.DATABASE_ERROR, e.getMessage(), e);
        }
    }
}
