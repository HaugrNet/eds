package io.javadog.cws.model.jpa;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.model.ProcessMemberDao;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.common.exceptions.ModelException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessMemberJpaDao implements ProcessMemberDao {

    private final EntityManager entityManager;

    public ProcessMemberJpaDao(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public MemberEntity findMemberByName(final String name) {
        //final Query query = entityManager.createNamedQuery("findByName");
        final Query query = entityManager.createQuery("select m from MemberEntity m where name = :name");
        query.setParameter("name", name);

        return findUniqueRecord(query, "member", name);
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
