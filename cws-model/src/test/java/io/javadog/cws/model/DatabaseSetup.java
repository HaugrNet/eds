/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-model)
 * =============================================================================
 */
package io.javadog.cws.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.Status;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.KeyEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;
import io.javadog.cws.model.jpa.CommonJpaDao;
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
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class DatabaseSetup {

    private static final String TIMESTAMP = "yyyyMMddHHmmssSSS";
    private static final String persistenceName = "io.javadog.cws.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(persistenceName);
    protected EntityManager entityManager = FACTORY.createEntityManager();
    protected CommonDao dao = new CommonJpaDao(entityManager);
    protected final Settings settings = new Settings();
    private final Crypto crypto = new Crypto(settings);

    /**
     * If the test is expecting an Exception, then we'll use this as the rule.
     * The method {@link #prepareCause(Class, ReturnCode, String)} will provide
     * the simplest way to setup the cause to be expected.
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
     * Exception &amp; Message.
     *
     * @param cause         CWS Exception to except
     * @param returnMessage The Return Message
     * @param <E>           The Exception, must extend a CWS Exception
     */
    protected <E extends CWSException> void prepareCause(final Class<E> cause, final String returnMessage) {
        final String propertyName = "returnCode";
        thrown.expect(cause);
        thrown.expectMessage(returnMessage);
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
    protected <E extends CWSException> void prepareCause(final Class<E> cause, final ReturnCode returnCode, final String returnMessage) {
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
        persist(entity);

        return entity;
    }

    protected MemberEntity prepareMember(final String credential, final String publicKey, final String privateKey) {
        final MemberEntity entity = new MemberEntity();
        entity.setName(credential);
        entity.setSalt(UUID.randomUUID().toString());
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);
        persist(entity);

        return entity;
    }

    protected CircleEntity prepareCircle(final String name) {
        final CircleEntity entity = new CircleEntity();
        entity.setName(name);
        persist(entity);

        return entity;
    }

    protected <E extends CWSEntity> void persist(final E entity) {
        dao.persist(entity);
    }

    protected <E extends CWSEntity> void persistAndDetach(final E entity) {
        persist(entity);
        entityManager.flush();
        entityManager.clear();
    }

    protected <E extends CWSEntity> E find(final Class<E> cwsEntity, final Long id) {
        return dao.find(cwsEntity, id);
    }

    protected String toString(final Date date) {
        String str = null;

        if (date != null) {
            final SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP, Locale.ENGLISH);
            str = sdf.format(date);
        }

        return str;
    }
}
