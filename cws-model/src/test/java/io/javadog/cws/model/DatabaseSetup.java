package io.javadog.cws.model;

import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.Status;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.Externable;
import io.javadog.cws.model.entities.KeyEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class DatabaseSetup {

    private static final String persistenceName = "io.javadog.cws.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(persistenceName);
    protected EntityManager entityManager = FACTORY.createEntityManager();
    private final Settings settings = new Settings();
    private final Crypto crypto = new Crypto(settings);

    /**
     * If the test is expecting an Exception, then we'll use this as the rule.
     * The method {@link #prepareCause(Class, int, String)} will provide the
     * simplest way to setup the cause to be expected.
     */
    @Rule public ExpectedException thrown = ExpectedException.none();

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
     * @param account Account name, also used as password
     * @return Newly created Entity
     */
    protected MemberEntity createMember(final String account) {
        final String salt = UUID.randomUUID().toString();
        final Key key = crypto.convertPasswordToKey(account.toCharArray(), salt);

        final KeyPair pair = crypto.generateAsymmetricKey();
        final IvParameterSpec iv = crypto.generateInitialVector(salt);
        final byte[] encryptedPrivateKey = crypto.encrypt(key, iv, pair.getPrivate().getEncoded());
        final String base64EncryptedPrivateKey = Base64.getEncoder().encodeToString(encryptedPrivateKey);
        final String armoredPublicKey = Crypto.armorPublicKey(pair.getPublic());

        final MemberEntity entity = new MemberEntity();
        entity.setName(account);
        entity.setSalt(salt);
        entity.setPrivateKey(base64EncryptedPrivateKey);
        entity.setPublicKey(armoredPublicKey);
        entity.setKeyPair(pair);
        persist(entity);

        return entity;
    }

    protected void addKeyAndTrusteesToCircle(final CircleEntity circle, final MemberEntity... accounts) {
        if (accounts != null) {
            final SecretKey key = crypto.generateSymmetricKey();
            final KeyEntity keyEntity = new KeyEntity();
            keyEntity.setAlgorithm(key.getAlgorithm());
            keyEntity.setCipherMode(settings.getSymmetricCipherMode());
            keyEntity.setPadding(settings.getSymmetricPadding());
            keyEntity.setStatus(Status.ACTIVE);
            persist(keyEntity);

            for (final MemberEntity account : accounts) {
                final TrusteeEntity entity = new TrusteeEntity();
                entity.setCircleKey(crypto.encryptAndArmorCircleKey(account.getKeyPair().getPublic(), key));
                entity.setMember(account);
                entity.setCircle(circle);
                entity.setKey(keyEntity);
                entity.setTrustLevel(TrustLevel.ADMIN);

                persist(entity);
            }
        }
    }

    /**
     * Setting the cause of an error to expect. This consists of the CWS
     * Exception, Return Code &amp; Message.
     *
     * @param cause         CWS Exception to except
     * @param returnCode    The Return Code
     * @param returnMessage The Return Message
     * @param <E>           The Exception, must extend a CWS Exception
     */
    protected <E extends CWSException> void prepareCause(final Class<E> cause, final int returnCode, final String returnMessage) {
        final String propertyName = "returnCode";
        thrown.expect(cause);
        thrown.expectMessage(returnMessage);
        thrown.expect(hasProperty(propertyName));
        thrown.expect(hasProperty(propertyName, is(returnCode)));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    protected KeyEntity prepareKey() {
        final KeyEntity entity = new KeyEntity();
        entity.setAlgorithm(settings.getSymmetricAlgorithm());
        entity.setCipherMode(settings.getSymmetricCipherMode());
        entity.setPadding(settings.getSymmetricPadding());
        entity.setStatus(Status.ACTIVE);

        return persist(entity);
    }

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
