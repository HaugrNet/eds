/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2025, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.eds.core.setup;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import jakarta.annotation.Resource;
import jakarta.ejb.TimerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.requests.ProcessDataRequest;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.ManagementBean;
import net.haugr.eds.core.ShareBean;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.jce.EDSKeyPair;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.IVSalt;
import net.haugr.eds.core.jce.MasterKey;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.EDSEntity;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * <p>Primarily, EDS is tested using an in-memory database, which offers
 * more flexibility and greater trust than using mocking. The initial setup
 * is more cumbersome, but the gain is also more trust in the tests - and no
 * need to fiddle with mocks or other things.</p>
 *
 * <p>This class controls the setup parts of the database, and offers other
 * small helpful utilities for the tests.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public class DatabaseSetup {

    // Following an upgrade of the H2 Database from 1.4.200 to 2.0.202+, it
    // seems that the internal handling of larger LOB's have been changed,
    // so if the value exceeds 1MB - 16 bytes, then the following  error
    // is shown: 'Value too long for column "BINARY VARYING".'
    //   To resolve it, the simplest way was to ensure that no data LOBs
    // were generated using hardcoded sizes, and secondly, by not exceeding
    // the maximum allowed value of 1048560 bytes.
    protected static final int LARGE_SIZE_BYTES = 1024 * 1024 - 16;
    protected static final int MEDIUM_SIZE_BYTES = 512 * 1024;

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

    private static final String DEFAULT_ACCOUNT_NAME = "New Account Name";
    private static final MemberRole DEFAULT_ROLE = MemberRole.STANDARD;
    private static final String DEFAULT_SECRET = "My Super Secret";
    private static final String TIMESTAMP = "yyyyMMddHHmmssSSS";
    private static final String PERSISTENCE_NAME = "net.haugr.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(PERSISTENCE_NAME);
    protected final EntityManager entityManager = FACTORY.createEntityManager();
    protected final CommonDao dao = new CommonDao(entityManager);
    protected final Settings settings = Settings.getInstance();
    protected final Crypto crypto = new Crypto(settings);

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


    protected static MasterKey prepareNewMasterKeyInstance(final Settings settings) {
        final Constructor<?> constructor = MasterKey.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);

        try {
            return (MasterKey) constructor.newInstance(settings);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new EDSException(ReturnCode.ERROR, e);
        }
    }

    protected ManagementBean prepareManagementBean(final Settings... settings) {
        final ManagementBean bean = new ManagementBean();
        inject(bean, entityManager);
        inject(bean, ((settings != null) && (settings.length == 1)) ? settings[0] : this.settings);

        return bean;
    }

    protected ManagementBean prepareManagementBeanWithNewMasterKey(final Settings... settings) {
        final Settings instanceSettings = ((settings != null) && (settings.length == 1)) ? settings[0] : this.settings;
        final ManagementBean bean = new ManagementBean();
        inject(bean, prepareNewMasterKeyInstance(instanceSettings));
        inject(bean, entityManager);
        inject(bean, instanceSettings);

        return bean;
    }

    protected ShareBean prepareShareBean() {
        final ShareBean bean = new ShareBean();
        inject(bean, entityManager);
        inject(bean, settings);

        return bean;
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
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
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
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Request Object", e);
        }
    }

    protected MemberEntity prepareMember(final String externalId, final String accountName, final KeyAlgorithm algorithm, final String publicKey, final String privateKey) {
        final MemberEntity entity = new MemberEntity();
        entity.setName(accountName);
        entity.setPbeAlgorithm(settings.getPasswordAlgorithm());
        entity.setRsaAlgorithm(algorithm);
        entity.setSalt(crypto.encryptWithMasterKey(externalId));
        entity.setPublicKey(publicKey);
        entity.setPrivateKey(privateKey);
        entity.setMemberRole(DEFAULT_ROLE);
        save(entity);

        return entity;
    }

    protected MemberEntity prepareMember(final String externalId, final EDSKeyPair keyPair) {
        final KeyAlgorithm pbeAlgorithm = settings.getPasswordAlgorithm();
        final IVSalt salt = new IVSalt();
        final SecretEDSKey secretKey = crypto.generatePasswordKey(pbeAlgorithm, crypto.stringToBytes(DEFAULT_SECRET), salt.getArmored());
        secretKey.setSalt(salt);

        return prepareMember(externalId,
                DEFAULT_ACCOUNT_NAME,
                settings.getAsymmetricAlgorithm(),
                Crypto.armoringPublicKey(keyPair.getPublic().getKey()),
                Crypto.encryptAndArmorPrivateKey(secretKey, keyPair.getPrivate().getKey()));
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
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
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
            constructor.setAccessible(true);
            return constructor.newInstance(settings);
        } catch (InvocationTargetException e) {
            final Throwable target = e.getTargetException();
            if (target instanceof EDSException) {
                throw new EDSException(((EDSException) target).getReturnCode(), target.getMessage(), e);
            }
            throw new EDSException(ReturnCode.ERROR, target.getMessage(), e);
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new EDSException(ReturnCode.ERROR, e.getMessage(), e);
        }
    }

    protected KeyEntity prepareKey() {
        final KeyEntity entity = new KeyEntity();
        entity.setAlgorithm(settings.getSymmetricAlgorithm());
        entity.setStatus(Status.ACTIVE);
        save(entity);

        return entity;
    }

    protected CircleEntity prepareCircle(final String externalId, final String name) {
        final CircleEntity entity = new CircleEntity();
        entity.setExternalId(externalId);
        entity.setName(name);
        save(entity);

        return entity;
    }

    protected <E extends EDSEntity> void save(final E entity) {
        dao.save(entity);
    }

    protected <E extends EDSEntity> void persistAndDetach(final E entity) {
        save(entity);
        entityManager.flush();
        entityManager.clear();
    }

    protected <E extends EDSEntity> E find(final Class<E> edsEntity, final Long id) {
        return dao.find(edsEntity, id);
    }

    protected String toString(final LocalDateTime date) {
        String str = null;

        if (date != null) {
            final SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP, Locale.ENGLISH);
            str = sdf.format(date);
        }

        return str;
    }

    /**
     * To properly test the cases where the SanityCheck is supposed to fail, i.e.
     * the Data Checksum is not matching the data anymore - a backdoor is needed
     * into the database whereby it is achieved. This method will do just that.
     *
     * @param response    Process Data Response Object
     * @param sanityCheck The timestamp for the last sanity check
     * @param status      The Sanity Status to use
     */
    protected void falsifyChecksum(final ProcessDataResponse response, final LocalDateTime sanityCheck, final SanityStatus status) {
        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by EDS, it cannot be altered (rightfully) via
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
     * @param value    Object to inject
     * @throws EDSException if an error occurred
     */
    protected static void inject(final Object instance, final Object value) {
        for (final Field field : instance.getClass().getDeclaredFields()) {
            if (field.getType().equals(value.getClass()) || field.getType().equals(value.getClass().getSuperclass())) {
                setField(instance, field, value);
            } else {
                for (final Annotation annotation : field.getAnnotations()) {
                    if (isInjectableResource(annotation, value)) {
                        setField(instance, field, value);
                    }
                }
            }
        }
    }

    private static boolean isInjectableResource(final Annotation annotation, final Object value) {
        return ((annotation instanceof PersistenceContext) && (value instanceof EntityManager)) ||
                ((annotation instanceof Resource) && ((value instanceof TimerService) || (value instanceof ThreadFactory)));
    }

    /**
     * <p>Uses Reflection to update an instantiated Object by altering the
     * given field with a new value.</p>
     *
     * @param instance The Object to change a Field in
     * @param field    The Field to change
     * @param value    The new value to set the Field too
     * @throws EDSException if unable to update the field
     */
    private static void setField(final Object instance, final Field field, final Object value) {
        try {
            final boolean accessible = field.canAccess(instance);
            field.setAccessible(true);
            field.set(instance, value);
            field.setAccessible(accessible);
        } catch (IllegalAccessException e) {
            throw new EDSException(ReturnCode.ERROR, e);
        }
    }
}
