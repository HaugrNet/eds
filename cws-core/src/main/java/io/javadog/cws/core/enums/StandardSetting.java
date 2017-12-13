/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.enums;

import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public enum StandardSetting {

    SYMMETRIC_ALGORITHM("cws.crypto.symmetric.algorithm", "AES128"),
    ASYMMETRIC_ALGORITHM("cws.crypto.asymmetric.algorithm", "RSA2048"),
    SIGNATURE_ALGORITHM("cws.crypto.signature.algorithm", "SHA512"),
    PBE_ALGORITHM("cws.crypto.pbe.algorithm", "PBE128"),
    HASH_ALGORITHM("cws.crypto.hash.algorithm", "SHA512"),
    CWS_SALT("cws.system.salt", "Default salt, also used as kill switch. Must be set in DB."),
    CWS_LOCALE("cws.system.locale", "EN"),
    CWS_CHARSET("cws.system.charset", "UTF-8"),
    EXPOSE_ADMIN("cws.expose.admin", "false"),
    SHOW_TRUSTEES("cws.show.trustees", "true"),
    SANITY_STARTUP("cws.sanity.check.startup", "true"),
    SANITY_INTERVAL("cws.sanity.check.interval", "180");

    // =========================================================================
    // Internal Functionality
    // =========================================================================

    private final String key;
    private final String value;

    StandardSetting(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static boolean contains(final String key) {
        StandardSetting found = null;

        for (final StandardSetting setting : StandardSetting.values()) {
            if (Objects.equals(setting.key, key)) {
                found = setting;
            }
        }

        return found != null;
    }
}
