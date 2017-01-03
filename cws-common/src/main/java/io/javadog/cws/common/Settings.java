package io.javadog.cws.common;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.common.exceptions.CWSException;

import java.util.Properties;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Settings {

    private static final String SYMMETRIC_ALGORITHM_NAME = "cws.crypto.symmetric.algorithm";
    private static final String SYMMETRIC_ALGORITHM_MODE = "cws.crypto.symmetric.cipher.mode";
    private static final String SYMMETRIC_ALGORITHM_PADDING = "cws.crypto.symmetric.padding";
    private static final String SYMMETRIC_ALGORITHM_KEYLENGTH = "cws.crypto.symmetric.keylength";
    private static final String ASYMMETRIC_ALGORITHM = "cws.crypto.asymmetric.algorithm";
    private static final String ASYMMETRIC_ALGORITHM_KEYLENGTH = "cws.crypto.asymmetric.keylength";
    private static final String PBE_ALGORITHM = "cws.crypto.pbe.algorithm";
    private static final String CWS_SALT = "cws.system.salt";
    private static final String CWS_CHARSET = "cws.system.charset";

    private static final String DEFAULT_SYMMETRIC_ALGORITHM_NAME = "AES";
    private static final String DEFAULT_SYMMETRIC_CIPHER_MODE = "CBC";
    private static final String DEFAULT_SYMMETRIC_PADDING = "PKCS5Padding";
    private static final String DEFAULT_ASYMMETRIC_ALGORITHM = "RSA";
    private static final String DEFAULT_ASYMMETRIC_KEYLENGTH = "2048";
    private static final String DEFAULT_SYMMETRIC_KEYLENGTH = "128";
    private static final String DEFAULT_PBE_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String DEFAULT_SALT = "Default Salt, please make sure it is set in the DB instead.";
    private static final String DEFAULT_CHARSETNAME = "UTF-8";

    private static final Object LOCK = new Object();
    private static Settings instance = null;
    private final Properties properties = new Properties();

    private Settings() {
        properties.setProperty(SYMMETRIC_ALGORITHM_NAME, DEFAULT_SYMMETRIC_ALGORITHM_NAME);
        properties.setProperty(SYMMETRIC_ALGORITHM_MODE, DEFAULT_SYMMETRIC_CIPHER_MODE);
        properties.setProperty(SYMMETRIC_ALGORITHM_PADDING, DEFAULT_SYMMETRIC_PADDING);
        properties.setProperty(SYMMETRIC_ALGORITHM_KEYLENGTH, DEFAULT_SYMMETRIC_KEYLENGTH);
        properties.setProperty(ASYMMETRIC_ALGORITHM, DEFAULT_ASYMMETRIC_ALGORITHM);
        properties.setProperty(ASYMMETRIC_ALGORITHM_KEYLENGTH, DEFAULT_ASYMMETRIC_KEYLENGTH);
        properties.setProperty(PBE_ALGORITHM, DEFAULT_PBE_ALGORITHM);
        properties.setProperty(CWS_SALT, DEFAULT_SALT);
        properties.setProperty(CWS_CHARSET, DEFAULT_CHARSETNAME);
    }

    public static Settings getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new Settings();
            }

            return instance;
        }
    }

    public void set(final String key, final String value) {
        properties.setProperty(key, value);
    }

    public String getSymmetricAlgorithm() {
        final String algorithm = properties.getProperty(SYMMETRIC_ALGORITHM_NAME);
        final String mode = properties.getProperty(SYMMETRIC_ALGORITHM_MODE);
        final String padding = properties.getProperty(SYMMETRIC_ALGORITHM_PADDING);

        return algorithm + '/' + mode + '/' + padding;
    }

    public String getSymmetricAlgorithmName() {
        return properties.getProperty(SYMMETRIC_ALGORITHM_NAME);
    }

    public int getSymmetricKeylength() {
        return toInt(properties.getProperty(SYMMETRIC_ALGORITHM_KEYLENGTH));
    }

    public String getAsymmetricAlgorithmName() {
        return properties.getProperty(ASYMMETRIC_ALGORITHM);
    }

    public int getAsymmetricKeylength() {
        return toInt(properties.getProperty(ASYMMETRIC_ALGORITHM_KEYLENGTH));
    }

    public String getPBEAlgorithm() {
        return properties.getProperty(PBE_ALGORITHM);
    }

    public String getSalt() {
        return properties.getProperty(CWS_SALT);
    }

    public String getCharset() {
        return properties.getProperty(CWS_CHARSET);
    }

    private static int toInt(final String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new CWSException(Constants.PROPERTY_ERROR, e);
        }
    }
}
