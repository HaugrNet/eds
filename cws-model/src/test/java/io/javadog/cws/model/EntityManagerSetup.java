package io.javadog.cws.model;

import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import org.junit.After;
import org.junit.Before;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class EntityManagerSetup {

    private static final String persistenceName = "io.javadog.cws.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(persistenceName);
    protected EntityManager entityManager = FACTORY.createEntityManager();

    @Before
    public void setup() {
        entityManager.getTransaction().begin();
    }

    @After
    public void tearDown() {
        entityManager.getTransaction().rollback();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    protected MemberEntity prepareMember(final String credential, final String publicKey, final String privateKey) {
        final MemberEntity entity = new MemberEntity();
        entity.setCredential(credential);
        entity.setName(credential);
        entity.setArmoredPublicKey(publicKey);
        entity.setArmoredEncryptedPrivateKey(privateKey);

        return persist(entity);
    }

    protected CircleEntity prepareCircle(final String name) {
        final CircleEntity entity = new CircleEntity();
        entity.setName(name);
        return persist(entity);
    }

    protected <E extends CWSEntity> E persist(final E entity) {
        if (entity.getExternalId() == null) {
            entity.setExternalId(UUID.randomUUID().toString());
        }
        if (entity.getCreated() == null) {
            entity.setCreated(new Date());
        }

        entity.setModified(new Date());
        entityManager.persist(entity);

        return entity;
    }
}
