/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CWSEntity;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.model.entities.KeyEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class DatabaseSetup {

    private static final Logger log = Logger.getLogger(DatabaseSetup.class.getName());

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
    protected CommonDao dao = new CommonDao(entityManager);
    protected final Settings settings = Settings.getInstance();
    protected final Crypto crypto = new Crypto(settings);

    static {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader != null) {
            try (final InputStream stream = loader.getResourceAsStream("logger.properties")) {
                final LogManager manager = LogManager.getLogManager();
                manager.readConfiguration(stream);
            } catch (IOException e) {
                log.log(Settings.ERROR, e.getMessage());
            }
        }
    }

    /**
     * If the test is expecting an Exception, then we'll use this as the rule.
     * The method {@link #prepareCause(Class, ReturnCode, String)} will provide
     * the simplest way to setup the cause to be expected.
     */
    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        entityManager.getTransaction().begin();
        settings.set(StandardSetting.IS_READY.getKey(), "true");
    }

    @After
    public void tearDown() {
        entityManager.getTransaction().rollback();
    }

    protected static <T extends Authentication> T prepareRequest(final Class<T> clazz, final String account) {
        try {
            final Crypto myCrypto = new Crypto(Settings.getInstance());
            final T request = clazz.getConstructor().newInstance();

            request.setAccountName(account);
            request.setCredential(myCrypto.stringToBytes(account));
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
        }
    }

    protected static <T extends Authentication> T prepareSessionRequest(final Class<T> clazz, final String session) {
        try {
            final Crypto myCrypto = new Crypto(Settings.getInstance());
            final T request = clazz.getConstructor().newInstance();

            request.setCredential(myCrypto.stringToBytes(session));
            request.setCredentialType(CredentialType.SESSION);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
        }
    }

    protected MemberEntity prepareMember(final String externalId, final String accountName, final String secret, final CWSKeyPair keyPair, final MemberRole role) {
        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final IVSalt salt = new IVSalt();
        final SecretCWSKey secretKey = crypto.generatePasswordKey(pbeAlgorithm, crypto.stringToBytes(secret), salt.getArmored());
        secretKey.setSalt(salt);

        final MemberEntity entity = new MemberEntity();
        entity.setExternalId(externalId);
        entity.setName(accountName);
        entity.setSalt(salt.getArmored());
        entity.setPbeAlgorithm(pbeAlgorithm);
        entity.setRsaAlgorithm(settings.getAsymmetricAlgorithm());
        entity.setPublicKey(crypto.armoringPublicKey(keyPair.getPublic().getKey()));
        entity.setPrivateKey(crypto.armoringPrivateKey(secretKey, keyPair.getPrivate().getKey()));
        entity.setMemberRole(role);
        entity.setAltered(new Date());
        entity.setAdded(new Date());

        return entity;
    }

    protected static ProcessDataRequest prepareAddDataRequest(final String account, final String circleId, final String dataName, final int bytes) {
        final ProcessDataRequest request = prepareRequest(ProcessDataRequest.class, account);
        request.setAction(Action.ADD);
        request.setCircleId(circleId);
        request.setDataName(dataName);
        request.setTypeName(Constants.DATA_TYPENAME);
        request.setData(generateData(bytes));

        return request;
    }

    protected static byte[] generateData(final int bytes) {
        byte[] data = null;

        if (bytes > 0) {
            data = new byte[bytes];
            final SecureRandom random = new SecureRandom();
            random.nextBytes(data);
        }

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

    /**
     * Some of our tests require that we change the Settings, but as changing
     * the settings may affect other tests, hence for those - a new Settings
     * instance is enforced, which has to be done via Reflection.
     *
     * @return New Settings instance
     */
    protected Settings newSettings() {
        try {
            final Constructor<Settings> constructor = Settings.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, e.getMessage(), e);
        }
    }

    protected KeyEntity prepareKey() {
        final KeyEntity entity = new KeyEntity();
        entity.setAlgorithm(settings.getSymmetricAlgorithm());
        entity.setStatus(Status.ACTIVE);
        persist(entity);

        return entity;
    }

    protected MemberEntity prepareMember(final String externalId, final String credential, final KeyAlgorithm algorithm, final String publicKey, final String privateKey, final MemberRole role) {
        final MemberEntity entity = new MemberEntity();
        entity.setName(credential);
        entity.setPbeAlgorithm(settings.getPasswordAlgorithm());
        entity.setRsaAlgorithm(algorithm);
        entity.setSalt(crypto.encryptWithMasterKey(externalId));
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);
        entity.setMemberRole(role);
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

    /**
     * To properly test the cases where the SanityCheck is suppose to fail, i.e.
     * the Data Checksum is not matching the data anymore - a backdoor is needed
     * into the database whereby it is achieved. This method will do just that.
     *
     * @param response    Process Data Response Object
     * @param sanityCheck The timestamp for the last sanity check
     * @param status      The Sanity Status to use
     */
    protected void falsifyChecksum(final ProcessDataResponse response, final Date sanityCheck, final SanityStatus status) {
        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by CWS, it cannot be altered (rightfully) via
        // the API, hence we have to modify it directly in the database!
        final String jql = "select d from DataEntity d where d.metadata.externalId = :eid";
        final Query query = entityManager.createQuery(jql);
        query.setParameter("eid", response.getDataId());
        final DataEntity entity = (DataEntity) query.getSingleResult();
        entity.setChecksum(UUID.randomUUID().toString());
        entity.setSanityStatus(status);
        entity.setSanityChecked(sanityCheck);
        entityManager.persist(entity);
    }

    protected static void setField(final Object instance, final String fieldName, final Object value) {
        try {
            final Class<?> clazz = instance.getClass();
            final Field field;

            field = clazz.getDeclaredField(fieldName);
            final boolean accessible = field.isAccessible();

            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessible);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot set Field", e);
        }
    }
}
