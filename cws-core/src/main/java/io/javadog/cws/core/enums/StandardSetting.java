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
package io.javadog.cws.core.enums;

import java.util.Objects;

/**
 * <p>The CWS Standard Settings.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public enum StandardSetting {

    SYMMETRIC_ALGORITHM("cws.crypto.symmetric.algorithm", "AES_CBC_256"),
    ASYMMETRIC_ALGORITHM("cws.crypto.asymmetric.algorithm", "RSA_2048"),
    SIGNATURE_ALGORITHM("cws.crypto.signature.algorithm", "SHA_512"),
    PBE_ALGORITHM("cws.crypto.pbe.algorithm", "PBE_256"),
    PBE_ITERATIONS("cws.crypto.pbe.iterations", "1024"),
    HASH_ALGORITHM("cws.crypto.hash.algorithm", "SHA_512"),
    CWS_SALT("cws.system.salt", "Default salt, also used as kill switch. Must be set in DB."),
    CWS_LOCALE("cws.system.locale", "EN"),
    CWS_CHARSET("cws.system.charset", "UTF-8"),
    SHOW_CIRCLES("cws.show.all.circles", "true"),
    SHOW_TRUSTEES("cws.show.trustees", "true"),
    SANITY_STARTUP("cws.sanity.check.startup", "true"),
    SANITY_INTERVAL("cws.sanity.check.interval.days", "180"),
    SESSION_TIMEOUT("cws.session.timeout.minutes", "480"),
    MASTERKEY_URL("cws.masterkey.url", ""),
    CORS("cws.cors.value", "*"),
    // The isReady setting is set by the StartUp bean, and thus not persisted.
    IS_READY("cws.is.ready", "true");

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

    public static StandardSetting find(final String key) {
        StandardSetting found = null;

        for (final StandardSetting setting : StandardSetting.values()) {
            if (Objects.equals(setting.key, key)) {
                found = setting;
            }
        }

        return found;
    }
}
