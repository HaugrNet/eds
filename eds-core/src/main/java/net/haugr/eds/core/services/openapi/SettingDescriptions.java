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
package net.haugr.eds.core.services.openapi;

import java.util.EnumMap;
import java.util.Map;
import net.haugr.eds.core.enums.StandardSetting;

/**
 * <p>What each standard setting means, for the settings table in the OpenAPI
 * specification. The keys and defaults come from {@link StandardSetting};
 * every value must have an entry here, which {@code SettingDescriptionsTest}
 * enforces.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class SettingDescriptions {

    private static final Map<StandardSetting, String> DESCRIPTIONS = new EnumMap<>(StandardSetting.class);

    static {
        DESCRIPTIONS.put(StandardSetting.SYMMETRIC_ALGORITHM, "Algorithm for the Circle keys and data encryption. Must be a symmetric algorithm known to EDS.");
        DESCRIPTIONS.put(StandardSetting.ASYMMETRIC_ALGORITHM, "Algorithm for the Members' key pairs. Must be an asymmetric algorithm known to EDS.");
        DESCRIPTIONS.put(StandardSetting.PBE_ALGORITHM, "Algorithm for deriving keys from passphrases.");
        DESCRIPTIONS.put(StandardSetting.PBE_ITERATIONS, "Iterations for deriving keys from new passphrases, at least 1000. Each account keeps the iterations of its passphrase.");
        DESCRIPTIONS.put(StandardSetting.HASH_ALGORITHM, "Algorithm for checksums.");
        DESCRIPTIONS.put(StandardSetting.EDS_LOCALE, "Locale for formatting.");
        DESCRIPTIONS.put(StandardSetting.SHOW_CIRCLES, "If true, every Member can list all Circles, not only their own.");
        DESCRIPTIONS.put(StandardSetting.SHOW_TRUSTEES, "If true, Members can see all Circles of another Member, not only the shared ones. "
                + "As any Member can list all Members, every account then sees every Circle of the stone and who is in it, "
                + "though not its data; for small groups where everybody knows everybody.");
        DESCRIPTIONS.put(StandardSetting.SANITY_STARTUP, "If true, the sanity check runs when EDS starts.");
        DESCRIPTIONS.put(StandardSetting.SANITY_INTERVAL, "Days between sanity checks of a Data Object; 0 switches the checks off. A number.");
        DESCRIPTIONS.put(StandardSetting.SESSION_TIMEOUT, "Lifetime of a session, in minutes.");
        DESCRIPTIONS.put(StandardSetting.SESSION_MAX_PER_MEMBER, "Sessions a Member may have at a time, one per login; a further login ends the one that expires first.");
        DESCRIPTIONS.put(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS, "Failed passphrase attempts allowed within the window before an account is locked, 0 to 1000; 0 disables the limit.");
        DESCRIPTIONS.put(StandardSetting.AUTH_FAILED_WINDOW_MINUTES, "The window, in minutes, in which the failed passphrase attempts are counted, 1 to 1440.");
        DESCRIPTIONS.put(StandardSetting.AUTH_CONCURRENT_PASSPHRASES, "How many passphrases are checked at once, 1 to 1024; 0 (the default) for half the processors. "
                + "A passphrase check (PBKDF2) costs about a fifth of a CPU-second, so this keeps CPU for everything else when many arrive at once.");
        DESCRIPTIONS.put(StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS, "How long, in milliseconds, a request waits for a place to check its passphrase "
                + "before it is answered as busy (503), 0 to 60000.");
        DESCRIPTIONS.put(StandardSetting.AUTH_LOCKOUT_MINUTES, "How long, in minutes, an account stays locked after too many failed attempts, 1 to 1440; the lock then heals itself.");
        DESCRIPTIONS.put(StandardSetting.DOWNLOAD_TOKEN_LIFETIME, "Lifetime of a download token for the content of one Data Object, in minutes; at most the rest of the Session. A number.");
        DESCRIPTIONS.put(StandardSetting.INVITATION_TIMEOUT, "Lifetime of an invitation, in hours. A number.");
        DESCRIPTIONS.put(StandardSetting.CIRCLE_SECRET_ROTATION_DAYS, "Days from a new Circle, or the end of a rotation, until the Circle Secret is rotated again, 0 to 36500; "
                + "0 rotates only after a Trustee left or was removed, or when a Circle administrator asks. A changed value applies from a Circle's next rotation on.");
        DESCRIPTIONS.put(StandardSetting.CIRCLE_SECRET_ROTATION_THREADS, "Data Objects re-encrypted at a time by the rotation of a Circle Secret, 1 to 64.");
        DESCRIPTIONS.put(StandardSetting.STREAM_MAX_OBJECT_SIZE, "Largest content upload, in bytes (default 1000 MiB). A number; the HTTP layer's body limit must allow it.");
        DESCRIPTIONS.put(StandardSetting.STREAM_TRANSACTION_TIMEOUT, "Transaction timeout for content uploads and downloads, in seconds. A number.");
        DESCRIPTIONS.put(StandardSetting.DATA_MAX_INLINE_SIZE, "Largest content in the JSON data field, in bytes (default 10 MiB); larger content uses the content endpoints. A number.");
        DESCRIPTIONS.put(StandardSetting.DATA_MAX_TOTAL_SIZE, "Ceiling for all stored content together, in bytes of encrypted content. "
                + "0 (default) means 2 GiB on a test system (in-memory H2) and 250 GiB on a production system; "
                + "a positive value sets it explicitly. Writes beyond it fail with 507. A number.");
        DESCRIPTIONS.put(StandardSetting.SANITY_MAX_OBJECTS, "Most Data Objects checked by one hourly sanity run; 0 for no limit. A number.");
        DESCRIPTIONS.put(StandardSetting.STREAM_READ_BATCH_SIZE, "Bytes of a part which content is stored in, and so written or read per query "
                + "when streaming content; kept within 64 KiB and 16 MiB, and used for content written after a change. A number.");
        DESCRIPTIONS.put(StandardSetting.MASTERKEY_URL, "URL the MasterKey secret was read from. Can't be changed with this request; use the MasterKey operation.");
        DESCRIPTIONS.put(StandardSetting.STONE_NAME, "The name of the stone, at most 64 characters, for those who look after several stones; it is told with the status only. Empty for none.");
        DESCRIPTIONS.put(StandardSetting.STATUS_LOAD_INTERVAL, "Seconds between two samples of the load for the status, 0 to 3600; 0 switches the gathering off.");
        DESCRIPTIONS.put(StandardSetting.STATUS_INVENTORY_INTERVAL, "Minutes between two samples of the inventory for the status, 1 to 10080.");
        DESCRIPTIONS.put(StandardSetting.CORS, "Allowed origin for CORS (cross-origin requests).");
        DESCRIPTIONS.put(StandardSetting.IS_READY, "Internal: false while EDS isn't ready, e.g. while the database is unavailable. Set by EDS only; can't be changed with this request.");
    }

    private SettingDescriptions() {
        // Utility class, no instances.
    }

    /**
     * Returns the description of the given setting.
     *
     * @param setting The standard setting
     * @return The description, or an empty string if none is defined
     */
    static String of(final StandardSetting setting) {
        return DESCRIPTIONS.getOrDefault(setting, "");
    }
}
