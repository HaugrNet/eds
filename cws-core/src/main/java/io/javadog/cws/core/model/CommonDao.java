/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.entities.CWSEntity;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import io.javadog.cws.core.model.entities.Externable;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.MetadataEntity;
import io.javadog.cws.core.model.entities.SettingEntity;
import io.javadog.cws.core.model.entities.SignatureEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

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
import java.util.Set;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CommonDao {

    private final EntityManager entityManager;

    public CommonDao(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void persist(final CWSEntity entity) {
        if ((entity instanceof Externable) && (((Externable) entity).getExternalId() == null)) {
            ((Externable) entity).setExternalId(UUID.randomUUID().toString());
        }

        if (entity.getAdded() == null) {
            entity.setAdded(new Date());
        }

        entity.setAltered(new Date());
        entityManager.persist(entity);
    }

    public <E extends CWSEntity> E find(final Class<E> cwsEntity, final Long id) {
        return entityManager.find(cwsEntity, id);
    }

    public <E extends Externable> E find(final Class<E> cwsEntity, final String externalId) {
        final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<E> query = builder.createQuery(cwsEntity);
        final Root<E> entity = query.from(cwsEntity);
        query.select(entity).where(builder.equal(entity.get("externalId"), externalId));

        return findSingleRecord(entityManager.createQuery(query));
    }

    public <E extends CWSEntity> E getReference(final Class<E> cwsEntity, final Long id) {
        return entityManager.getReference(cwsEntity, id);
    }

    public <E extends CWSEntity> void delete(final E entity) {
        entityManager.remove(entity);
    }

    public MemberEntity findMemberByName(final String name) {
        final Query query = entityManager.createNamedQuery("member.findByName");
        query.setParameter("name", name);

        return findSingleRecord(query);
    }

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

    public List<TrusteeEntity> findTrustByMember(final MemberEntity member, final Set<TrustLevel> permissions) {
        final Query query = entityManager.createNamedQuery("trust.findByMemberId");
        query.setParameter("id", member.getId());
        query.setParameter("permissions", permissions);

        return findList(query);
    }

    public List<TrusteeEntity> findTrustByMemberAndCircle(final MemberEntity member, final String externalCircleId, final Set<TrustLevel> permissions) {
        final Query query = entityManager.createNamedQuery("trust.findByMemberIdAndExternalCircleId");
        query.setParameter("id", member.getId());
        query.setParameter("externalCircleId", externalCircleId);
        query.setParameter("permissions", permissions);

        return findList(query);
    }

    public List<SettingEntity> readSettings() {
        final Query query = entityManager.createNamedQuery("setting.readAll");
        return findList(query);
    }

    public List<TrusteeEntity> findTrusteesByCircle(final CircleEntity circle) {
        final Query query = entityManager.createNamedQuery("trustee.findByCircleId");
        query.setParameter("circleId", circle.getId());

        return findList(query);
    }

    public List<CircleEntity> findAllCircles() {
        final Query query = entityManager.createNamedQuery("circle.findAll");

        return findList(query);
    }

    public List<MemberEntity> findAllMembers() {
        final Query query = entityManager.createNamedQuery("member.findAll");
        return findList(query);
    }

    public CircleEntity findCircleByName(final String name) {
        final Query query = entityManager.createNamedQuery("circle.findByName");
        query.setParameter("name", name);

        return findSingleRecord(query);
    }

    public List<CircleEntity> findCirclesForMember(final MemberEntity requested) {
        final Query query = entityManager.createNamedQuery("trustee.findCirclesByMember");
        query.setParameter("memberId", requested.getId());

        return findList(query);
    }

    public List<CircleEntity> findCirclesBothBelongTo(final MemberEntity member, final MemberEntity requested) {
        final Query query = entityManager.createNamedQuery("trustee.findSharedCircles");
        query.setParameter("member", member.getId());
        query.setParameter("requested", requested.getId());

        return findList(query);
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

    public long countObjectTypeUsage(final Long id) {
        final Query query = entityManager.createNamedQuery("type.countUsage");
        query.setParameter("id", id);

        return (long) query.getSingleResult();
    }

    public DataEntity findDataByMetadata(final MetadataEntity metadata) {
        final Query query = entityManager.createNamedQuery("data.findByMetadata");
        query.setParameter("metadataId", metadata.getId());

        return findSingleRecord(query);
    }

    public DataEntity findDataByMemberAndExternalId(final MemberEntity member, final String externalId) {
        final Query query = entityManager.createNamedQuery("data.findByMemberAndExternalId");
        query.setParameter("mid", member.getId());
        query.setParameter("eid", externalId);
        query.setParameter("trustLevels", EnumSet.of(TrustLevel.ADMIN, TrustLevel.WRITE, TrustLevel.READ));

        return findSingleRecord(query);
    }

    public MetadataEntity findMetaDataByMemberAndExternalId(final MemberEntity member, final String externalId) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndExternalId");
        query.setParameter("mid", member.getId());
        query.setParameter("eid", externalId);

        return findSingleRecord(query);
    }

    public MetadataEntity findRootByMemberCircle(final MemberEntity member, final String circleId) {
        final Query query = entityManager.createNamedQuery("metadata.findRootByMemberAndCircle");
        query.setParameter("mid", member.getId());
        query.setParameter("cid", circleId);

        return findSingleRecord(query);
    }

    public List<MetadataEntity> findMetadataByMemberAndFolder(final MemberEntity member, final MetadataEntity folder, final int pageNumber, final int pageSize) {
        final Query query = entityManager.createNamedQuery("metadata.findByMemberAndFolder");
        query.setParameter("mid", member.getId());
        query.setParameter("parentId", folder.getId());
        query.setMaxResults(pageSize);
        query.setFirstResult((pageNumber - 1) * pageSize);

        return findList(query);
    }

    public SignatureEntity findByChecksum(final String checksum) {
        final Query query = entityManager.createNamedQuery("signature.findByChecksum");
        query.setParameter("checksum", checksum);

        return findSingleRecord(query);
    }

    public List<SignatureEntity> findAllSignatures(final Long id) {
        final Query query = entityManager.createNamedQuery("signature.findByMember");
        query.setParameter("mid", id);

        return findList(query);
    }

    public TrusteeEntity findTrusteeByCircleAndMember(final String externalCircleId, final String externalMemberId) {
        final Query query = entityManager.createNamedQuery("trustee.findByCircleAndMember");
        query.setParameter("ecid", externalCircleId);
        query.setParameter("emid", externalMemberId);

        return findSingleRecord(query);
    }

    public long countFolderContent(final MetadataEntity entity) {
        final Query query = entityManager.createNamedQuery("metadata.countFolderContent");
        query.setParameter("pid", entity.getId());

        return (long) query.getSingleResult();
    }

    public MetadataEntity findInFolder(final MemberEntity member, final MetadataEntity folder, final String name) {
        final Query query = entityManager.createNamedQuery("metadata.findInFolder");
        query.setParameter("mid", member.getId());
        query.setParameter("pid", folder.getId());
        query.setParameter("name", name);

        return findSingleRecord(query);
    }

    public boolean checkIfNameIsUsed(final MetadataEntity entity, final String name, final Long folderId) {
        final Query query = entityManager.createNamedQuery("metadata.findByNameAndFolder");
        query.setParameter("id", entity.getId());
        query.setParameter("name", name);
        query.setParameter("parentId", folderId);

        return findSingleRecord(query) != null;
    }

    public List<DataEntity> findFailedRecords() {
        final Query query = entityManager.createNamedQuery("data.findAllWithState");
        query.setParameter("status", SanityStatus.FAILED);

        return findList(query);
    }

    // =========================================================================
    // Internal Methods, handling the actual lookup's to simplify error handling
    // =========================================================================

    private static <E> E findSingleRecord(final Query query) {
        final List<E> found = findList(query);

        return found.isEmpty() ? null : found.get(0);
    }

    private static <E> List<E> findList(final Query query) {
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
}
