/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.model.Settings;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * <p>This Singleton holds the CWS Master Key, which is set upon instantiating
 * the Class. The Master Key is used by the Crypto Library. The default Master
 * Key is based on the default settings, so having the default alone will not
 * do anything to increase security. But, if set by the System Administrator,
 * to a different value, will suddenly increase security. The Master Key is not
 * persisted, and will remain in memory as long as the CWS instance is
 * running.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class MasterKey {

    private static final Object LOCK = new Object();
    private static MasterKey instance = null;

    private final Settings settings;
    private SecretCWSKey key = null;

    private MasterKey(final Settings settings) {
        this.settings = settings;

        final byte[] secret = Constants.ADMIN_ACCOUNT.getBytes(settings.getCharset());
        updateKey(secret, settings.getSalt());
    }

    public static MasterKey getInstance(final Settings settings) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new MasterKey(settings);
            }

            return instance;
        }
    }

    private void updateKey(final byte[] secret, final String salt) {
        synchronized (LOCK) {
            try {
                final char[] chars = new char[secret.length];
                for (int i = 0; i < secret.length; i++) {
                    chars[i] = (char) secret[i];
                }

                final byte[] secretSalt = salt.getBytes(settings.getCharset());

                final KeyAlgorithm keyAlgorithm = settings.getPasswordAlgorithm();
                final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyAlgorithm.getTransformation());
                final KeySpec keySpec = new PBEKeySpec(chars, secretSalt, settings.getPasswordIterations(), keyAlgorithm.getLength());
                final SecretKey tmp = keyFactory.generateSecret(keySpec);
                final SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), keyAlgorithm.getName());

                key = new SecretCWSKey(keyAlgorithm.getDerived(), secretKey);
                key.setSalt(salt);
            } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new CryptoException(e.getMessage(), e);
            }
        }
    }

    public SecretCWSKey getKey() {
        synchronized (LOCK) {
            return key;
        }
    }
}
