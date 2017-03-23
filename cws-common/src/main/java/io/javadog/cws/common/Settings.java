/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-common)
 * =============================================================================
 */
package io.javadog.cws.common;

import io.javadog.cws.common.exceptions.SettingException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * <p>This Class holds the general settings for the CWS. All settings used by
 * the CWS is set with default values, which may or may nor be overwritten,
 * depending on the underlying database. It is possible to extend the settings
 * with more values, if Client Systems needs certain information shared.</p>
 *
 * <p>By starting the CWS, the Settings are loaded from the Database, and
 * invoking the Settings Request will allow the System Administrator to change
 * the Settings. It is not possible for Circle Administrators to make any
 * Updates, since this may have impacts on the System.</p>
 *
 * <p>Once a CWS system is being actively used, i.e. there exist Member Accounts
 * other than the System Administrator, then the rules regarding the
 * non-updateable values will be enforced. Updateable fields may be changed, by
 * the CWS will only use them after a restart.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Settings {

    public static final String SYMMETRIC_ALGORITHM_NAME = "cws.crypto.symmetric.algorithm";
    public static final String SYMMETRIC_ALGORITHM_MODE = "cws.crypto.symmetric.cipher.mode";
    public static final String SYMMETRIC_ALGORITHM_PADDING = "cws.crypto.symmetric.padding";
    public static final String SYMMETRIC_ALGORITHM_KEYLENGTH = "cws.crypto.symmetric.keylength";
    public static final String ASYMMETRIC_ALGORITHM = "cws.crypto.asymmetric.algorithm";
    public static final String ASYMMETRIC_ALGORITHM_KEYLENGTH = "cws.crypto.asymmetric.keylength";
    public static final String PBE_ALGORITHM = "cws.crypto.pbe.algorithm";
    public static final String CWS_SALT = "cws.system.salt";
    public static final String CWS_CHARSET = "cws.system.charset";
    public static final String EXPOSE_ADMIN = "cws.expose.admin";
    public static final String SHOW_OTHER_MEMBER_INFORMATION = "cws.show.other.member.information";

    private static final String DEFAULT_SYMMETRIC_ALGORITHM_NAME = "AES";
    private static final String DEFAULT_SYMMETRIC_CIPHER_MODE = "CBC";
    private static final String DEFAULT_SYMMETRIC_PADDING = "PKCS5Padding";
    private static final String DEFAULT_ASYMMETRIC_ALGORITHM = "RSA";
    private static final String DEFAULT_ASYMMETRIC_KEYLENGTH = "2048";
    private static final String DEFAULT_SYMMETRIC_KEYLENGTH = "128";
    private static final String DEFAULT_PBE_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String DEFAULT_SALT = "Default Salt, please make sure it is set in the DB instead.";
    private static final String DEFAULT_CHARSETNAME = "UTF-8";
    private static final String DEFAULT_EXPOSE_ADMIN = "false";
    private static final String DEFAULT_SHOW_OTHER_MEMBER_INFORMATION = "true";

    private final Properties properties = new Properties();

    public Settings() {
        properties.setProperty(SYMMETRIC_ALGORITHM_NAME, DEFAULT_SYMMETRIC_ALGORITHM_NAME);
        properties.setProperty(SYMMETRIC_ALGORITHM_MODE, DEFAULT_SYMMETRIC_CIPHER_MODE);
        properties.setProperty(SYMMETRIC_ALGORITHM_PADDING, DEFAULT_SYMMETRIC_PADDING);
        properties.setProperty(SYMMETRIC_ALGORITHM_KEYLENGTH, DEFAULT_SYMMETRIC_KEYLENGTH);
        properties.setProperty(ASYMMETRIC_ALGORITHM, DEFAULT_ASYMMETRIC_ALGORITHM);
        properties.setProperty(ASYMMETRIC_ALGORITHM_KEYLENGTH, DEFAULT_ASYMMETRIC_KEYLENGTH);
        properties.setProperty(PBE_ALGORITHM, DEFAULT_PBE_ALGORITHM);
        properties.setProperty(CWS_SALT, DEFAULT_SALT);
        properties.setProperty(CWS_CHARSET, DEFAULT_CHARSETNAME);
        properties.setProperty(EXPOSE_ADMIN, DEFAULT_EXPOSE_ADMIN);
        properties.setProperty(SHOW_OTHER_MEMBER_INFORMATION, DEFAULT_SHOW_OTHER_MEMBER_INFORMATION);
    }

    public void set(final String key, final String value) {
        properties.setProperty(key, value);
    }

    public Map<String, String> get() {
        final Map<String, String> copy = new HashMap<>(16);

        for (final String key : properties.stringPropertyNames()) {
            copy.put(key, properties.getProperty(key));
        }

        return copy;
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

    public String getSymmetricCipherMode() {
        return properties.getProperty(SYMMETRIC_ALGORITHM_MODE);
    }

    public String getSymmetricPadding() {
        return properties.getProperty(SYMMETRIC_ALGORITHM_PADDING);
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

    public Boolean getExposeAdmin() {
        return toBoolean(properties.getProperty(EXPOSE_ADMIN), DEFAULT_EXPOSE_ADMIN);
    }

    public Boolean getShowOtherMemberInformation() {
        return toBoolean(properties.getProperty(SHOW_OTHER_MEMBER_INFORMATION), DEFAULT_SHOW_OTHER_MEMBER_INFORMATION);
    }

    private static int toInt(final String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new SettingException(e);
        }
    }

    private static boolean toBoolean(final String value, final String defaultValue) {
        final Boolean result;

        if (value != null) {
            final String str = value.trim().toLowerCase(Locale.ENGLISH);
            result = Boolean.valueOf(str);
        } else {
            result = Boolean.valueOf(defaultValue);
        }

        return result;
    }
}
