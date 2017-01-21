package io.javadog.cws.model.jpa;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.exceptions.ModelException;
import io.javadog.cws.model.CommonDao;
import io.javadog.cws.model.entities.CWSEntity;
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
    public MemberEntity findMemberByNameCredential(final String credential) {
        final Query query = entityManager.createNamedQuery("member.findByCredential");
        query.setParameter("credential", credential);

        return findUniqueRecord(query, "member", credential);
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
        final Query query = entityManager.createQuery("settings.readAll");
        return findList(query);
    }

    private <E> List<E> findList(final Query query) {
        List<E> list = query.getResultList();

        if (list == null) {
            list = new ArrayList<>(0);
        }

        return list;
    }

    private static <E> E findUniqueRecord(final Query query, final String entityName, final String keyName) {
        final List<E> found;
        try {
            found = query.getResultList();
        } catch (IllegalStateException | PersistenceException e) {
            throw new ModelException(Constants.DATABASE_ERROR, e.getMessage(), e);
        }

        if ((found == null) || found.isEmpty()) {
            throw new ModelException(Constants.IDENTIFICATION_WARNING, "No " + entityName + " found with '" + keyName + "'.");
        }

        if (found.size() > 1) {
            throw new ModelException(Constants.CONSTRAINT_ERROR, "Could not uniquely identify a " + entityName + " with '" + keyName + "'.");
        }

        return found.get(0);
    }
}
