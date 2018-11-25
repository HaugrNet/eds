/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.jce;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.model.Settings;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
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

    // See Bug report #46: https://github.com/JavaDogs/cws/issues/46
    // To get around the problem of altering the MasterKey algorithm, there are
    // two choices:
    //   a) Save the chosen algorithm as a property, which is persisted and must
    //      also be updated and verified. Still renders the problem open to the
    //      case where someone changes the property accidentally.
    //   b) Hard code it to the current default.
    // The second option was chosen, as it is unlikely that anyone wishes to
    // downgrade security and it also prevents the problems with adding checks
    // on this setting.
    //   Note, that the same philosophy has been applied to the other Settings,
    // which is being used, except for the System Salt - where other checks
    // exists to protect it.
    private static final Integer ITERATIONS = Integer.valueOf(StandardSetting.PBE_ITERATIONS.getValue());
    private static final Charset CHARSET = Charset.forName(StandardSetting.CWS_CHARSET.getValue());
    private static final KeyAlgorithm ALGORITHM = KeyAlgorithm.PBE_256;
    private static final Object LOCK = new Object();
    private static MasterKey instance = null;

    private final Settings settings;
    private SecretCWSKey key;

    private MasterKey(final Settings settings) {
        this.settings = settings;

        final byte[] secret = Constants.ADMIN_ACCOUNT.getBytes(CHARSET);
        key = generateMasterKey(secret);
    }

    public static MasterKey getInstance(final Settings settings) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new MasterKey(settings);
            }

            return instance;
        }
    }

    public SecretCWSKey generateMasterKey(final byte[] secret) {
        try {
            final char[] secretChars = Crypto.generateSecretChars(secret);

            final String salt = settings.getSalt();
            final byte[] secretSalt = salt.getBytes(CHARSET);
            final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM.getTransformationValue());
            final KeySpec keySpec = new PBEKeySpec(secretChars, secretSalt, ITERATIONS, ALGORITHM.getLength());
            final SecretKey tmp = keyFactory.generateSecret(keySpec);
            final SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), ALGORITHM.getName());

            final SecretCWSKey newKey = new SecretCWSKey(ALGORITHM.getDerived(), secretKey);
            newKey.setSalt(new IVSalt(salt));

            return newKey;
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public void setKey(final SecretCWSKey key) {
        this.key = key;
    }

    public SecretCWSKey getKey() {
        return key;
    }
}
