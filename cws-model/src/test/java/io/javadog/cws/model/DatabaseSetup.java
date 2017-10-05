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

import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.common.Crypto;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.enums.Status;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.keys.CWSKey;
import io.javadog.cws.model.entities.CWSEntity;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.KeyEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.jpa.CommonJpaDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class DatabaseSetup {

    protected static final String MEMBER_1 = "member1";
    protected static final String MEMBER_2 = "member2";
    protected static final String MEMBER_3 = "member3";
    protected static final String MEMBER_4 = "member4";
    protected static final String MEMBER_5 = "member5";
    protected static final String CIRCLE_1 = "circle1";
    protected static final String CIRCLE_2 = "circle2";
    protected static final String CIRCLE_3 = "circle3";
    protected static final String CIRCLE_1_ID = "d8838d7d-71e7-433d-8790-af7c080e9de9";
    protected static final String CIRCLE_2_ID = "8ba34e12-8830-4a1f-9681-b689cad52009";
    protected static final String CIRCLE_3_ID = "a2797176-a5b9-4dc9-867b-8c5c1bb3a9f9";
    protected static final String ADMIN_ID = "d95a14e6-e1d1-424b-8834-16a79498f4d1";
    protected static final String MEMBER_1_ID = "073dcc8f-ffa6-4cda-8d61-09ba9441e78e";
    protected static final String MEMBER_2_ID = "d842fa67-5387-44e6-96e3-4e8a7ead4c8d";
    protected static final String MEMBER_3_ID = "f32c9422-b3e4-4b52-8d39-82c45f6e80a9";
    protected static final String MEMBER_4_ID = "b629f009-4da2-46ed-91b8-aa9dec54814d";
    protected static final String MEMBER_5_ID = "63cb90cc-c1fb-4c6a-b881-bec278b4e232";

    private static final String TIMESTAMP = "yyyyMMddHHmmssSSS";
    private static final String persistenceName = "io.javadog.cws.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(persistenceName);
    protected EntityManager entityManager = FACTORY.createEntityManager();
    protected CommonDao dao = new CommonJpaDao(entityManager);
    protected final Settings settings = new Settings();
    protected final Crypto crypto = new Crypto(settings);

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

    protected static <T extends Authentication> T prepareRequest(final Class<T> clazz, final String account) {
        try {
            final T request = clazz.getConstructor().newInstance();

            request.setAccount(account);
            request.setCredential(account);
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
        }
    }

    protected MemberEntity prepareMember(final String externalId, final String accountName, final String secret, final CWSKey keyPair) {
        final String salt = UUID.randomUUID().toString();
        final CWSKey secretKey = crypto.generatePasswordKey(settings.getSymmetricAlgorithm(), secret, salt);
        secretKey.setSalt(salt);

        final MemberEntity entity = new MemberEntity();
        entity.setExternalId(externalId);
        entity.setName(accountName);
        entity.setSalt(salt);
        entity.setAlgorithm(settings.getAsymmetricAlgorithm());
        entity.setPublicKey(crypto.armoringPublicKey(keyPair.getPublic()));
        entity.setPrivateKey(crypto.armoringPrivateKey(secretKey, keyPair.getPrivate()));
        entity.setModified(new Date());
        entity.setCreated(new Date());

        return entity;
    }

    protected static byte[] generateData(final int bytes) {
        final byte[] data = new byte[bytes];
        final SecureRandom random = new SecureRandom();
        random.nextBytes(data);

        return data;
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
        entity.setStatus(Status.ACTIVE);
        persist(entity);

        return entity;
    }

    protected MemberEntity prepareMember(final String externalId, final String credential, final KeyAlgorithm algorithm, final String publicKey, final String privateKey) {
        final MemberEntity entity = new MemberEntity();
        entity.setName(credential);
        entity.setAlgorithm(algorithm);
        entity.setSalt(externalId);
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);
        persist(entity);

        return entity;
    }

    protected CircleEntity prepareCircle(final String externalId, final String name) {
        final CircleEntity entity = new CircleEntity();
        entity.setExternalId(externalId);
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
