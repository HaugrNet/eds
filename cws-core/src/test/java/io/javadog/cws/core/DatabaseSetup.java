/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CWSEntity;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import io.javadog.cws.core.model.entities.KeyEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.MetadataEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
    protected final Settings settings = new Settings();
    protected final Crypto crypto = new Crypto(settings);

    static {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader != null) {
            final String propertiesFile = "logger.properties";
            try (InputStream stream = loader.getResourceAsStream(propertiesFile)) {
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
    }

    @After
    public void tearDown() {
        entityManager.getTransaction().rollback();
    }

    protected static <T extends Authentication> T prepareRequest(final Class<T> clazz, final String account) {
        try {
            final T request = clazz.getConstructor().newInstance();

            request.setAccountName(account);
            request.setCredential(account);
            request.setCredentialType(CredentialType.PASSPHRASE);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
        }
    }

    protected MemberEntity prepareMember(final String externalId, final String accountName, final String secret, final CWSKeyPair keyPair) {
        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final String salt = UUID.randomUUID().toString();
        final SecretCWSKey secretKey = crypto.generatePasswordKey(pbeAlgorithm, secret, salt);
        secretKey.setSalt(salt);

        final MemberEntity entity = new MemberEntity();
        entity.setExternalId(externalId);
        entity.setName(accountName);
        entity.setSalt(salt);
        entity.setPbeAlgorithm(pbeAlgorithm);
        entity.setRsaAlgorithm(settings.getAsymmetricAlgorithm());
        entity.setPublicKey(crypto.armoringPublicKey(keyPair.getPublic().getKey()));
        entity.setPrivateKey(crypto.armoringPrivateKey(secretKey, keyPair.getPrivate().getKey()));
        entity.setAltered(new Date());
        entity.setAdded(new Date());

        return entity;
    }

    protected static byte[] generateData(final int bytes) {
        final byte[] data;

        if (bytes > 0) {
            data = new byte[bytes];
            final SecureRandom random = new SecureRandom();
            random.nextBytes(data);
        } else {
            data = null;
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
        entity.setPbeAlgorithm(settings.getPasswordAlgorithm());
        entity.setRsaAlgorithm(algorithm);
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

    /**
     * This method will help build the
     */
    protected String prepareTestData() {
        final StringBuilder builder = new StringBuilder(20000);
        append(builder, "-- =============================================================================");
        append(builder, "-- Following is TEST data, and should not be added in a PRODUCTION environment");
        append(builder, "-- -----------------------------------------------------------------------------");
        append(builder, "-- Unfortunately, JPA only allow setting 3 scripts when creating the database,");
        append(builder, "-- the first is the actual model, which contain what is needed to setup the");
        append(builder, "-- database, including all tables, views, procedures, constraints, etc. The");
        append(builder, "-- second script is for the data (this one), but as we both need to have data");
        append(builder, "-- for production and for testing, we're adding it all here. The final script");
        append(builder, "-- is for destroying the database, which is needed of you have a real database");
        append(builder, "-- and not just an in-memory database.");
        append(builder, "-- =============================================================================");

        append(builder, "");
        append(builder, "-- Default Administrator User, it is set at the first request to the System, and");
        append(builder, "-- is thus needed for loads of tests. Remaining Accounts is for \"member1\" to");
        append(builder, "-- \"member5\", which is all used as part of the tests.");
        append(builder, "INSERT INTO cws_members (external_id, name, salt, pbe_algorithm, rsa_algorithm, public_key, private_key) VALUES");

        final CWSKeyPair keyPair = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        createAndAppendMember(builder, ADMIN_ID, Constants.ADMIN_ACCOUNT, keyPair, ',');
        final CWSKeyPair keyPair1 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final MemberEntity member1 = createAndAppendMember(builder, MEMBER_1_ID, MEMBER_1, keyPair1, ',');
        final CWSKeyPair keyPair2 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final MemberEntity member2 = createAndAppendMember(builder, MEMBER_2_ID, MEMBER_2, keyPair2, ',');
        final CWSKeyPair keyPair3 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final MemberEntity member3 = createAndAppendMember(builder, MEMBER_3_ID, MEMBER_3, keyPair3, ',');
        final CWSKeyPair keyPair4 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final MemberEntity member4 = createAndAppendMember(builder, MEMBER_4_ID, MEMBER_4, keyPair4, ',');
        final CWSKeyPair keyPair5 = crypto.generateAsymmetricKey(settings.getAsymmetricAlgorithm());
        final MemberEntity member5 = createAndAppendMember(builder, MEMBER_5_ID, MEMBER_5, keyPair5, ';');

        append(builder, "");
        append(builder, "-- Default, we have 3 Circles as part of the test setup, using the very");
        append(builder, "-- imaginative names, 'circle1' to 'circle3'.");
        append(builder, "");
        append(builder, "INSERT INTO cws_circles (external_id, name) VALUES");
        final CircleEntity circle1 = createAndAppendCircle(builder, CIRCLE_1_ID, CIRCLE_1, ',');
        final CircleEntity circle2 = createAndAppendCircle(builder, CIRCLE_2_ID, CIRCLE_2, ',');
        final CircleEntity circle3 = createAndAppendCircle(builder, CIRCLE_3_ID, CIRCLE_3, ';');

        append(builder, "");
        append(builder, "-- For each Circle, we need to have a unique Key, but with the same settings.");
        append(builder, "INSERT INTO cws_keys (algorithm, status) VALUES");
        final KeyEntity key1 = createAndAppendKey(builder, ',');
        final KeyEntity key2 = createAndAppendKey(builder, ',');
        final KeyEntity key3 = createAndAppendKey(builder, ';');

        append(builder, "");
        append(builder, "-- For each Circle, we need to have root folder for all data.");
        append(builder, "INSERT INTO metadata (external_id, parent_id, circle_id, datatype_id, name) VALUES");
        createandAppendFolder(builder, circle1, ',');
        createandAppendFolder(builder, circle2, ',');
        createandAppendFolder(builder, circle3, ';');

        append(builder, "");
        append(builder, "-- With the Members created, and the Circles and Keys added, it is possible to");
        append(builder, "-- also create a number of Trustees, in this case we add Member 1-3 to Circle 1,");
        append(builder, "-- Member 1-4 to Circle 2 and Member 2-5 to Circle 3.");
        append(builder, "-- The Trust Level is different for each Member.");
        append(builder, "INSERT INTO cws_trustees (member_id, circle_id, key_id, trust_level, circle_key) VALUES");
        final SecretCWSKey cwsKey1 = crypto.generateSymmetricKey(key1.getAlgorithm());
        final SecretCWSKey cwsKey2 = crypto.generateSymmetricKey(key1.getAlgorithm());
        final SecretCWSKey cwsKey3 = crypto.generateSymmetricKey(key1.getAlgorithm());
        createAndAppendTrustee(builder, member1, keyPair1, circle1, key1, cwsKey1, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, member2, keyPair2, circle1, key1, cwsKey1, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, member3, keyPair3, circle1, key1, cwsKey1, TrustLevel.READ,  ',');
        createAndAppendTrustee(builder, member1, keyPair1, circle2, key2, cwsKey2, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, member2, keyPair2, circle2, key2, cwsKey2, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, member3, keyPair3, circle2, key2, cwsKey2, TrustLevel.READ,  ',');
        createAndAppendTrustee(builder, member4, keyPair4, circle2, key2, cwsKey2, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, member2, keyPair2, circle3, key3, cwsKey3, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, member3, keyPair3, circle3, key3, cwsKey3, TrustLevel.WRITE, ',');
        createAndAppendTrustee(builder, member4, keyPair4, circle3, key3, cwsKey3, TrustLevel.ADMIN, ',');
        createAndAppendTrustee(builder, member5, keyPair5, circle3, key3, cwsKey3, TrustLevel.READ,  ';');

        return builder.toString();
    }

    private MemberEntity createAndAppendMember(final StringBuilder builder, final String externalId, final String name, final CWSKeyPair keyPair, final char delimiter) {
        final MemberEntity entity = prepareMember(externalId, name, name, keyPair);
        dao.persist(entity);

        append(builder, "    ('" + entity.getExternalId() + "', '" + name + "', '" + entity.getSalt() + "', '" + entity.getPbeAlgorithm() + "', '" + entity.getRsaAlgorithm() + "', '" + entity.getPublicKey() + "', '" + entity.getPrivateKey() + "')" + delimiter);

        return entity;
    }

    private CircleEntity createAndAppendCircle(final StringBuilder builder, final String externalId, final String name, final char delimiter) {
        final CircleEntity entity = prepareCircle(externalId, name);

        append(builder, "    ('" + entity.getExternalId() + "', '" + entity.getName() + "')" + delimiter);

        return entity;
    }

    private KeyEntity createAndAppendKey(final StringBuilder builder, final char delimiter) {
        final KeyEntity entity = prepareKey();

        append(builder, "    ('" + entity.getAlgorithm() + "', '" + entity.getStatus() + "')" + delimiter);

        return entity;
    }

    private void createandAppendFolder(final StringBuilder builder, final CircleEntity circleEntity, final char delimiter) {
        final DataTypeEntity dataTypeEntity = dao.getReference(DataTypeEntity.class, 1L);
        final MetadataEntity entity = new MetadataEntity();
        entity.setCircle(circleEntity);
        entity.setName("/");
        entity.setParentId(0L);
        entity.setType(dataTypeEntity);
        dao.persist(entity);

        append(builder, "    ('" + entity.getExternalId() + "', 0, " + entity.getCircle().getId() + ", 1, '/')" + delimiter);
    }

    private void createAndAppendTrustee(final StringBuilder builder, final MemberEntity member, final CWSKeyPair keyPair, final CircleEntity circle, final KeyEntity key, final SecretCWSKey circleKey, final TrustLevel trustLevel, final char delimiter) {
        final String armoredKey = crypto.encryptAndArmorCircleKey(keyPair.getPublic(), circleKey);
        final TrusteeEntity entity = new TrusteeEntity();
        entity.setMember(member);
        entity.setCircle(circle);
        entity.setKey(key);
        entity.setTrustLevel(trustLevel);
        entity.setCircleKey(armoredKey);
        dao.persist(entity);

        append(builder, "    (" + member.getId() + ", " + circle.getId() + ", " + key.getId() + ", '" + trustLevel + "', '" + armoredKey + "')" + delimiter);
    }

    private static void append(final StringBuilder builder, final String str) {
        builder.append(str);
    }
}