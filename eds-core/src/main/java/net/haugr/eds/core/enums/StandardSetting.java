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
    ASYMMETRIC_ALGORITHM("eds.crypto.asymmetric.algorithm", "MLKEM1024_P384"),
    PBE_ALGORITHM("eds.crypto.pbe.algorithm", "PBE_GCM_256"),
    // OWASP Password Storage Cheat Sheet recommendation for PBKDF2-HMAC-SHA512
    PBE_ITERATIONS("eds.crypto.pbe.iterations", "210000"),
    HASH_ALGORITHM("eds.crypto.hash.algorithm", "SHA_512"),
    EDS_LOCALE("eds.system.locale", "EN"),
    EDS_CHARSET("eds.system.charset", "UTF-8"),
    // Members only see the Circles they are in, unless set to true.
    SHOW_CIRCLES("eds.show.all.circles", "false"),
    SHOW_TRUSTEES("eds.show.trustees", "true"),
    SANITY_STARTUP("eds.sanity.check.startup", "true"),
    SANITY_INTERVAL("eds.sanity.check.interval.days", "180"),
    SESSION_TIMEOUT("eds.session.timeout.minutes", "480"),
    // Sessions a Member may have at a time (PLAN.md, "Several sessions"); a
    // further login ends the one that expires first.
    SESSION_MAX_PER_MEMBER("eds.session.max.per.member", "10"),
    // Lifetime of a download token for one Data Object (PLAN.md, L5), at
    // most the rest of the Session.
    DOWNLOAD_TOKEN_LIFETIME("eds.download.token.lifetime.minutes", "60"),
    INVITATION_TIMEOUT("eds.invitation.timeout.hours", "168"),
    // Rotating a Circle Secret (PLAN.md, Crypto C6): the days until the next
    // rotation of a Circle, 0 for none but those a removal asks for, and the
    // objects re-encrypted at a time by the job of a Circle.
    CIRCLE_SECRET_ROTATION_DAYS("eds.circle.secret.rotation.days", "365"),
    CIRCLE_SECRET_ROTATION_THREADS("eds.circle.secret.rotation.threads", "2"),
    // Streaming of Data Object content, see PLAN.md, Streaming. The maximum is
    // 1000 MiB, below PostgreSQL's 1 GB field limit including the format
    // overhead; quarkus.http.limits.max-body-size must allow it.
    STREAM_MAX_OBJECT_SIZE("eds.stream.max.object.size", "1048576000"),
    STREAM_TRANSACTION_TIMEOUT("eds.stream.transaction.timeout.seconds", "3600"),
    STREAM_READ_BATCH_SIZE("eds.stream.read.batch.size", "4194304"),
    // Largest content for the JSON "data" field; larger content must use the
    // content endpoints (PLAN.md, S4).
    DATA_MAX_INLINE_SIZE("eds.data.max.inline.size", "10485760"),
    // Ceiling for all stored (encrypted) content together, in bytes. 0 means
    // the default for the kind of system (2 GiB test, 250 GiB production).
    // Concurrent writes may exceed it by what they add together.
    DATA_MAX_TOTAL_SIZE("eds.data.max.total.size", "0"),
    // Most Data Objects checked by one (hourly) sanity run; 0 for no limit.
    SANITY_MAX_OBJECTS("eds.sanity.check.max.objects", "1000"),
    MASTERKEY_URL("eds.masterkey.url", ""),
    // The name of the stone, which may be a technical one, for those who
    // look after several stones (STATUS.md). Told with the status only.
    STONE_NAME("eds.stone.name", ""),
    // Seconds between two samples of the load (STATUS.md); 0 switches the
    // gathering off.
    STATUS_LOAD_INTERVAL("eds.status.load.interval.seconds", "5"),
    // Minutes between two samples of the inventory, which are queries.
    STATUS_INVENTORY_INTERVAL("eds.status.inventory.interval.minutes", "60"),
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
