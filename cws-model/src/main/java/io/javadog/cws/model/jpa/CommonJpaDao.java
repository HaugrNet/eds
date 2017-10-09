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
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.DataEntity;
import io.javadog.cws.model.entities.DataTypeEntity;
import io.javadog.cws.model.entities.Externable;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.MetadataEntity;
import io.javadog.cws.model.entities.SettingEntity;
import io.javadog.cws.model.entities.SignatureEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Externable> E find(final Class<E> cwsEntity, final String externalId) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(cwsEntity);
        final Root<E> entity = query.from(cwsEntity);
        query.select(entity).where(builder.equal(entity.get("externalId"), externalId));

        return findSingleRecord(entityManager.createQuery(query));
    }

    /**
     * {@inheritDoc}
     */
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

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemberEntity findMemberByNameAndCircleId(final String name, final String externalCircleId) {
        final Query query = entityManager.createNamedQuery("member.findByNameAndCircle");
        query.setParameter("name", name);
        query.setParameter("externalCircleId", externalCircleId);

        final List<MemberEntity> found = findList(query);

        if (found.isEmpty()) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "No Trustee information found for member '" + name + "' and circle '" + externalCircleId + "'.");
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

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TrusteeEntity> findTrustByMemberAndCircle(final MemberEntity member, final String externalCircleId) {
        final Query query = entityManager.createNamedQuery("trust.findByMemberIdAndExternalCircleId");
        query.setParameter("id", member.getId());
        query.setParameter("externalCircleId", externalCircleId);

        return findList(query);
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
    public CircleEntity findCircleByName(final String name) {
        final Query query = entityManager.createNamedQuery("circle.findByName");
        query.setParameter("name", name);

        return findSingleRecord(query);
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
    public DataTypeEntity findDataTypeByName(final String name) {
        final Query query = entityManager.createNamedQuery("type.findMatching");
        query.setParameter("name", name);

        return findUniqueRecord(query);
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
    public MetadataEntity findMetaDataByMemberAndExternalId(final MemberEntity member, final String externalId) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndExternalId");
        query.setParameter("mid", member.getId());
        query.setParameter("eid", externalId);

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetadataEntity> findMetadataByMemberFolderAndType(final MemberEntity member, final MetadataEntity entity, final DataType dataType) {
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
    public MetadataEntity findRootByMemberCircle(final MemberEntity member, final String circleId) {
        final Query query = entityManager.createNamedQuery("metadata.findRootByMemberAndCircle");
        query.setParameter("mid", member.getId());
        query.setParameter("cid", circleId);

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MetadataEntity> findMetadataByMemberAndFolder(final MemberEntity member, final MetadataEntity folder) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndFolder");
        query.setParameter("mid", member.getId());
        query.setParameter("parentId", folder.getId());

        return findList(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SignatureEntity findByChecksum(final String checksum) {
        final Query query = entityManager.createNamedQuery("signature.findByChecksum");
        query.setParameter("checksum", checksum);

        return findSingleRecord(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SignatureEntity> findAllSignatures(final Long id) {
        final Query query = entityManager.createNamedQuery("signature.findByMember");
        query.setParameter("mid", id);

        return findList(query);
    }

    @Override
    public TrusteeEntity findTrusteeByCircleAndMember(final String externalCircleId, final String externalMemberId) {
        final Query query = entityManager.createNamedQuery("trustee.findByCircleAndMember");
        query.setParameter("externalCircleId", externalCircleId);
        query.setParameter("externalMemberId", externalMemberId);

        return findSingleRecord(query);
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup's to simplify error handling
    // =========================================================================

    private static <E> E findUniqueRecord(final Query query) {
        final List<E> found = findList(query);

        if (found.size() != 1) {
            throw new CWSException(ReturnCode.DATABASE_ERROR, "Could not uniquely identify a record with the given criteria's.");
        }

        return found.get(0);
    }

    private static <E> E findSingleRecord(final Query query) {
        final List<E> found = findList(query);

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
            // This should be and will hopefully remain unreachable code.
            throw new CWSException(ReturnCode.DATABASE_ERROR, e.getMessage(), e);
        }
    }
}
