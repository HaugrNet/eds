/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.enums;

import java.util.Objects;

/**
 * <p>The EDS Standard Settings.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public enum StandardSetting {

    SYMMETRIC_ALGORITHM("eds.crypto.symmetric.algorithm", "AES_GCM_256"),
    ASYMMETRIC_ALGORITHM("eds.crypto.asymmetric.algorithm", "RSA_2048"),
    SIGNATURE_ALGORITHM("eds.crypto.signature.algorithm", "SHA_512"),
    PBE_ALGORITHM("eds.crypto.pbe.algorithm", "PBE_GCM_256"),
    PBE_ITERATIONS("eds.crypto.pbe.iterations", "1024"),
    HASH_ALGORITHM("eds.crypto.hash.algorithm", "SHA_512"),
    EDS_SALT("eds.system.salt", "Default salt, also used as kill switch. Must be set in DB."),
    EDS_LOCALE("eds.system.locale", "EN"),
    EDS_CHARSET("eds.system.charset", "UTF-8"),
    SHOW_CIRCLES("eds.show.all.circles", "true"),
    SHOW_TRUSTEES("eds.show.trustees", "true"),
    SANITY_STARTUP("eds.sanity.check.startup", "true"),
    SANITY_INTERVAL("eds.sanity.check.interval.days", "180"),
    SESSION_TIMEOUT("eds.session.timeout.minutes", "480"),
    MASTERKEY_URL("eds.masterkey.url", ""),
    CORS("eds.cors.value", "http://localhost"),
    // The isReady setting is set by the StartUp bean, and thus not persisted.
    IS_READY("eds.is.ready", "true");

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
