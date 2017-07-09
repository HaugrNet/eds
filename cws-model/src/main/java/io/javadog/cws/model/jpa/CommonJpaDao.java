/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model.jpa;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.dtos.DataType;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.DataTypeEntity;
import io.javadog.cws.model.entities.Externable;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.MetaDataEntity;
import io.javadog.cws.model.entities.SettingEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
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
    public void persist(final CWSEntity entity) {
        if ((entity instanceof Externable) && (((Externable) entity).getExternalId() == null)) {
            ((Externable) entity).setExternalId(UUID.randomUUID().toString());
        }

        if (entity.getCreated() == null) {
            entity.setCreated(new Date());
        }

        entity.setModified(new Date());
        entityManager.persist(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends CWSEntity> E find(final Class<E> cwsEntity, final Long id) {
        return entityManager.find(cwsEntity, id);
    }

    @Override
    public <E extends CWSEntity> void delete(final E entity) {
        entityManager.remove(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemberEntity findMemberByName(final String name) {
        final Query query = entityManager.createNamedQuery("member.findByName");
        query.setParameter("name", name);

        final List<MemberEntity> found = query.getResultList();

        if (found.size() > 1) {
            throw new ModelException(ReturnCode.CONSTRAINT_ERROR, "Could not uniquely identify a member with '" + name + "'.");
        }

        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemberEntity findMemberByNameAndCircleId(final String name, final String externalCircleId) {
        final Query query = entityManager.createNamedQuery("member.findByNameAndCircle");
        query.setParameter("name", name);
        query.setParameter("externalCircleId", externalCircleId);

        final List<MemberEntity> found = query.getResultList();

        if (found.isEmpty()) {
            throw new ModelException(ReturnCode.IDENTIFICATION_WARNING, "No member found with '" + name + "'.");
        }

        if (found.size() > 1) {
            throw new ModelException(ReturnCode.CONSTRAINT_ERROR, "Could not uniquely identify a member with '" + name + "'.");
        }

        return found.get(0);
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
    public List<TrusteeEntity> findTrustByMemberAndCircle(final MemberEntity member, final String externalCircleId) {
        final Query query = entityManager.createNamedQuery("trust.findByMemberIdAndExternalCircleId");
        query.setParameter("id", member.getId());
        query.setParameter("externalCircleId", externalCircleId);

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

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CircleEntity findCircleByExternalId(final String externalId) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataTypeEntity> findAllTypes() {
        final Query query = entityManager.createNamedQuery("type.findAll");

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataTypeEntity> findMatchingDataTypes(final String name) {
        final Query query = entityManager.createNamedQuery("type.findMatching");
        query.setParameter("name", name);

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countObjectTypeUsage(final Long id) {
        final Query query = entityManager.createNamedQuery("type.countUsage");
        query.setParameter("id", id);

        return (long) query.getSingleResult();
    }

    @Override
    public DataEntity findDataByMemberAndExternalId(final MemberEntity member, final String externalId) {
        final Query query = entityManager.createNamedQuery("data.findByMemberAndExternalId");
        query.setParameter("mid", member.getId());
        query.setParameter("eid", externalId);
        query.setParameter("trustLevels", EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE, TrustLevel.READ));

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaDataEntity findMetaDataByMemberAndExternalId(final MemberEntity member, final String externalId) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndExternalId");
        query.setParameter("mid", member.getId());
        query.setParameter("eid", externalId);

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetaDataEntity> findMetadataByMemberFolderAndType(final MemberEntity member, final MetaDataEntity entity, final DataType dataType) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndFolderAndType");
        query.setParameter("parentId", entity.getParentId());
        query.setParameter("mid", member.getId());
        query.setParameter("typename", dataType.getName());

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MetaDataEntity findRootByMemberCircle(final MemberEntity member, final String circleId) {
        final Query query = entityManager.createNamedQuery("metadata.findRootByMemberAndCircle");

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetaDataEntity> findMetadataByMemberAndFolder(final MemberEntity member, final MetaDataEntity folder) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndFolder");
        query.setParameter("mid", member.getId());
        query.setParameter("parentId", folder.getId());

        return findList(query);
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup's to simplify error handling
    // =========================================================================

    private static <E> E findSingleRecord(final Query query) {
        final List<E> found = query.getResultList();
        return found.isEmpty() ? null : found.get(0);
    }

    private static <E> List<E> findList(final Query query) {
        try {
            List<E> list = query.getResultList();

            if (list == null) {
                list = new ArrayList<>(0);
            }

            return list;
        } catch (IllegalStateException | PersistenceException e) {
            throw new ModelException(ReturnCode.DATABASE_ERROR, e.getMessage(), e);
        }
    }
}
