/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.ProcessDataRequest;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.CWSKeyPair;
import io.javadog.cws.core.jce.Crypto;
import io.javadog.cws.core.jce.IVSalt;
import io.javadog.cws.core.jce.MasterKey;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.TimerService;
import javax.persistence.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Kim Jensen
 * @since CWS 1.0
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

    @BeforeEach
    public void setup() {
        final EntityTransaction transaction = entityManager.getTransaction();
        if (transaction != null) {
            transaction.begin();
        }
        settings.set(StandardSetting.IS_READY.getKey(), "true");
    }

    @AfterEach
    public void tearDown() {
        final EntityTransaction transaction = entityManager.getTransaction();
        if (transaction != null) {
            transaction.rollback();
        }
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
        entity.setPublicKey(Crypto.armoringPublicKey(keyPair.getPublic().getKey()));
        entity.setPrivateKey(Crypto.armoringPrivateKey(secretKey, keyPair.getPrivate().getKey()));
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
     * Some of our tests require that we change the Settings, but as changing
     * the settings may affect other tests, hence for those - a new Settings
     * instance is enforced, which has to be done via Reflection.
     *
     * @return New Settings instance
     */
    protected Settings newSettings() {
        try {
            final Constructor<Settings> constructor = Settings.class.getDeclaredConstructor();
            final boolean accessible = constructor.isAccessible();
            constructor.setAccessible(true);
            final Settings instance = constructor.newInstance();
            constructor.setAccessible(accessible);

            return instance;
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }


    /**
     * Testing, requiring a customized MasterKey, should use this method to get
     * a new instance of the Singleton Class MasterKey. It requires an instance
     * of the Settings to work, so it is possible to alter the content and thus
     * behaviour without affecting other tests requiring the MasterKey and
     * Settings.
     *
     * @param settings Customized Settings instance for the MasterKey instance
     * @return New MasterKey instance
     */
    protected static MasterKey newMasterKey(final Settings settings) {
        try {
            final Constructor<MasterKey> constructor = MasterKey.class.getDeclaredConstructor(Settings.class);
            final boolean accessible = constructor.isAccessible();
            constructor.setAccessible(true);
            final MasterKey instance = constructor.newInstance(settings);
            constructor.setAccessible(accessible);

            return instance;
        } catch (InvocationTargetException e) {
            final Throwable target = e.getTargetException();
            if (target instanceof CWSException) {
                throw new CWSException(((CWSException) target).getReturnCode(), target.getMessage(), e);
            }
            throw new CWSException(ReturnCode.ERROR, target.getMessage(), e);
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
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
        final Query query = entityManager
                .createQuery(jql)
                .setParameter("eid", response.getDataId());
        final DataEntity entity = (DataEntity) query.getSingleResult();
        entity.setChecksum(UUID.randomUUID().toString());
        entity.setSanityStatus(status);
        entity.setSanityChecked(sanityCheck);
        entityManager.persist(entity);
    }

    /**
     * <p>Simplified CDI Injection mechanism. Based on the Bean and the
     * given Object, it tries to find the best place to inject the given Object
     * into the Bean. Initially by simply looking at the types, but if not
     * possible to find a match, then it also looks at the annotations.</p>
     *
     * <p>For the standard @Inject annotation, the types must be an exact
     * match, or at least a sub-class that inherits the expected type. For
     * other annotation such as @PersistenceContext or @Resource, the rules
     * differ - here it is more lax in the type expectations, provided the
     * given Object inherits from a basic Object that is normally used, such
     * as {@link EntityManager}, {@link TimerService} or
     * {@link ThreadFactory}.</p>
     *
     * @param instance Bean Instance with CDI Injection places
     * @param value Object to inject
     * @throws CWSException if an error occurred
     */
    protected static void inject(final Object instance, final Object value) {
        for (final Field field : instance.getClass().getDeclaredFields()) {
            if (field.getType().equals(value.getClass()) || field.getType().equals(value.getClass().getSuperclass())) {
                setField(instance, field, value);
            } else {
                for (final Annotation annotation : field.getAnnotations()) {
                    if ((annotation instanceof PersistenceContext) && ((value instanceof EntityManager))) {
                        setField(instance, field, value);
                    } else if (annotation instanceof Resource) {
                        if ((value instanceof  TimerService) || (value instanceof ThreadFactory)) {
                            setField(instance, field, value);
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>Uses Reflection to update an instantiated Object by altering the
     * given field with a new value.</p>
     *
     * @param instance The Object to change a Field in
     * @param field    The Field to change
     * @param value    The new value to set the Field too
     * @throws CWSException if unable to update the field
     */
    private static void setField(final Object instance, final Field field, final Object value) {
        try {
            final boolean accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessible);
        } catch (IllegalAccessException e) {
            throw new CWSException(ReturnCode.ERROR, e);
        }
    }
}
