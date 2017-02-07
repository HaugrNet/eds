package io.javadog.cws.model;

import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.Status;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.Externable;
import io.javadog.cws.model.entities.KeyEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;
import org.junit.After;
import org.junit.Before;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.security.Key;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class DatabaseSetup {

    private static final String persistenceName = "io.javadog.cws.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(persistenceName);
    protected EntityManager entityManager = FACTORY.createEntityManager();
    private final Crypto crypto = new Crypto(new Settings());

    @Before
    public void setup() {
        entityManager.getTransaction().begin();
    }

    @After
    public void tearDown() {
        entityManager.getTransaction().rollback();
    }

    /**
     * Creates a new Member Entity, with a Password which is the same as the
     * name.
     *
     * @param name Account name, also used as password
     * @return Newly created Account
     */
    public MemberEntity createMember(final String name) {
        final String salt = UUID.randomUUID().toString();
        final Key key = crypto.convertPasswordToKey(name.toCharArray(), salt);

        final KeyPair pair = crypto.generateAsymmetricKey();
        final IvParameterSpec iv = crypto.generateInitialVector(salt);
        final byte[] encryptedPrivateKey = crypto.encrypt(key, iv, pair.getPrivate().getEncoded());
        final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
        final String armoredPublicKey = Crypto.armorPublicKey(pair.getPublic());

        final MemberEntity account = new MemberEntity();
        account.setName(name);
        account.setSalt(salt);
        account.setPrivateKey(base64EncryptedPrivateKey);
        account.setPublicKey(armoredPublicKey);
        entityManager.persist(account);

        return account;
    }

    public void addKeyAndTrusteesToCircle(final CircleEntity circle, final MemberEntity... accounts) {
        if (accounts != null) {
            final SecretKey key = crypto.generateSymmetricKey();
            final KeyEntity keyEntity = new KeyEntity();
            keyEntity.setAlgorithm(key.getAlgorithm());
            keyEntity.setStatus(Status.ACTIVE);
            entityManager.persist(keyEntity);

            for (final MemberEntity account : accounts) {
                final TrusteeEntity entity = new TrusteeEntity();
                entity.setCircleKey(crypto.encryptAndArmorCircleKey(account.getKeyPair().getPublic(), key));
                entity.setCircle(circle);
                entity.setKey(keyEntity);
                entity.setTrustLevel(TrustLevel.ADMIN);

                entityManager.persist(entity);
            }
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    protected MemberEntity prepareMember(final String credential, final String publicKey, final String privateKey) {
        final MemberEntity entity = new MemberEntity();
        entity.setName(credential);
        entity.setSalt(UUID.randomUUID().toString());
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);

        return persist(entity);
    }

    protected CircleEntity prepareCircle(final String name) {
        final CircleEntity entity = new CircleEntity();
        entity.setName(name);

        return persist(entity);
    }

    protected <E extends CWSEntity> E persist(final E entity) {
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
}
