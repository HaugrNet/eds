/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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
package io.javadog.cws.core.jce;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.exceptions.CryptoException;
import io.javadog.cws.core.model.Settings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
 * @since CWS 1.0
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
    private static final Integer ITERATIONS = 1024;
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final KeyAlgorithm ALGORITHM = KeyAlgorithm.PBE_256;
    private static final Object LOCK = new Object();
    private static final int BUFFER_SIZE = 512;
    private static MasterKey instance = null;

    private final Settings settings;
    private SecretCWSKey key;

    private MasterKey(final Settings settings) {
        this.settings = settings;

        final String url = settings.getMasterKeyURL();
        final byte[] secret;
        if (url.isEmpty()) {
            secret = Constants.ADMIN_ACCOUNT.getBytes(CHARSET);
        } else {
            secret = readMasterKeySecretFromUrl(url);
        }
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

    public static byte[] readMasterKeySecretFromUrl(final String masterKeyUrl) {
        try {
            final URL url = new URL(masterKeyUrl);
            // Note, that the following line is raised as a security issue by
            // SonarQube. Normally, the rule is correct, but only if the
            // information read is also returned. In this case, the content
            // is simply used to read a series of controlled bytes, which
            // again is used to generate the Symmetric Key later referred
            // to as the MasterKey. Thus the rule is ignored at this place.
            // See: http://localhost:9000/coding_rules?open=findsecbugs%3AURLCONNECTION_SSRF_FD&rule_key=findsecbugs%3AURLCONNECTION_SSRF_FD
            final URLConnection connection = url.openConnection();
            return readContent(connection);
        } catch (IOException e) {
            throw new CWSException(ReturnCode.NETWORK_ERROR, e.getMessage(), e);
        }
    }

    private static byte[] readContent(final URLConnection connection) throws IOException {
        final int length = connection.getContentLength();

        try (final InputStream inputStream = connection.getInputStream();
             final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length)) {
            final byte[] buffer = new byte[BUFFER_SIZE];

            int read = inputStream.read(buffer);
            while (read != -1) {
                outputStream.write(buffer, 0, read);
                read = inputStream.read(buffer);
            }

            return outputStream.toByteArray();
        }
    }

    public static char[] generateSecretChars(final byte[] secret) {
        final char[] chars = new char[secret.length];
        for (int i = 0; i < secret.length; i++) {
            chars[i] = (char) secret[i];
        }

        return chars;
    }

    public SecretCWSKey generateMasterKey(final byte[] secret) {
        try {
            final char[] secretChars = generateSecretChars(secret);

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
