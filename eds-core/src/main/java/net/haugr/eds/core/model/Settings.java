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
package net.haugr.eds.core.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.core.enums.HashAlgorithm;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.StandardSetting;

/**
 * <p>This Class holds the general settings for the EDS. All settings used by
 * the EDS are set with default values, which may or may not be overwritten,
 * depending on the underlying database. It is possible to extend the settings
 * with more values if Client Systems need certain information shared.</p>
 *
 * <p>By starting the EDS, the Settings are loaded from the Database, and
 * invoking the Settings Request will allow the System Administrator to change
 * the Settings. It is not possible for Circle Administrators to make any
 * Updates, since this may have impacts on the System.</p>
 *
 * <p>Once an EDS system is being actively used, i.e., there exist Member Accounts
 * other than the System Administrator, then the rules regarding the
 * non-updatable values will be enforced. Updatable fields may be changed, but
 * the EDS will only use them after a restart.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Settings {

    /** The most failed passphrase attempts which may be allowed before a lock (#68). */
    public static final int MAX_AUTH_FAILED_ATTEMPTS = 1000;

    /** The longest window and lock of the authentication attempt limits (#68), a day. */
    public static final int MAX_AUTH_MINUTES = 1440;
    /** The most passphrases checked at once which may be set (ticket 20). */
    public static final int MAX_CONCURRENT_PASSPHRASES = 1024;
    /** The longest wait for a place to check a passphrase which may be set. */
    public static final int MAX_PASSPHRASE_WAIT_MILLIS = 60_000;

    private static Settings instance = null;
    private static final Object LOCK = new Object();

    private final Properties properties = new Properties();

    /**
     * Private Constructor, this is a Singleton.
     */
    private Settings() {
        for (final StandardSetting setting : StandardSetting.values()) {
            set(setting, setting.getValue());
        }
    }

    public static Settings getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new Settings();
            }

            return instance;
        }
    }

    // =========================================================================
    // Generic Settings Methods
    // =========================================================================

    public void set(final StandardSetting key, final String value) {
        set(key.getKey(), value);
    }

    public void set(final String key, final String value) {
        properties.setProperty(key, value);
    }

    public String get(final String key) {
        return properties.getProperty(key);
    }

    public Map<String, String> get() {
        final Map<String, String> copy = new ConcurrentHashMap<>(16);

        for (final String key : keys()) {
            copy.put(key, get(key));
        }

        return copy;
    }

    public Set<String> keys() {
        return properties.stringPropertyNames();
    }

    public void remove(final String key) {
        properties.remove(key);
    }

    // =========================================================================
    // Specific Setting Getter Methods
    // =========================================================================

    public KeyAlgorithm getSymmetricAlgorithm() {
        return KeyAlgorithm.valueOf(get(StandardSetting.SYMMETRIC_ALGORITHM.getKey()));
    }

    public KeyAlgorithm getAsymmetricAlgorithm() {
        return KeyAlgorithm.valueOf(get(StandardSetting.ASYMMETRIC_ALGORITHM.getKey()));
    }

    public KeyAlgorithm getPasswordAlgorithm() {
        return KeyAlgorithm.valueOf(get(StandardSetting.PBE_ALGORITHM.getKey()));
    }

    public Integer getPasswordIterations() {
        return Integer.valueOf(get(StandardSetting.PBE_ITERATIONS.getKey()).trim());
    }

    public HashAlgorithm getHashAlgorithm() {
        return HashAlgorithm.valueOf(get(StandardSetting.HASH_ALGORITHM.getKey()));
    }

    public Locale getLocale() {
        return Locale.forLanguageTag(get(StandardSetting.EDS_LOCALE.getKey()));
    }

    /**
     * Text becomes bytes as UTF-8, always: passphrases, and the associated
     * data of every key and object. It was a setting, but another character
     * set made every stored key unreadable, so nobody could log in to set it
     * back (ticket 14).
     *
     * @return UTF-8
     */
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    public boolean hasShowAllCircles() {
        return Boolean.parseBoolean(get(StandardSetting.SHOW_CIRCLES.getKey()).trim());
    }

    public boolean hasShareTrustees() {
        return Boolean.parseBoolean(get(StandardSetting.SHOW_TRUSTEES.getKey()).trim());
    }

    public boolean hasSanityStartup() {
        return Boolean.parseBoolean(get(StandardSetting.SANITY_STARTUP.getKey()).trim());
    }

    public Integer getSanityInterval() {
        return Integer.valueOf(get(StandardSetting.SANITY_INTERVAL.getKey()).trim());
    }

    /**
     * Days until the next rotation of a Circle Secret (PLAN.md, Crypto C6);
     * 0 for none but those asked for.
     *
     * @return Days
     */
    public int getCircleSecretRotationDays() {
        return Integer.parseInt(get(StandardSetting.CIRCLE_SECRET_ROTATION_DAYS.getKey()).trim());
    }

    /**
     * When the next rotation of a Circle Secret is due, counted from now.
     *
     * @return The time, or null if the period is 0
     */
    public LocalDateTime nextCircleSecretRotation() {
        final int days = getCircleSecretRotationDays();
        return (days > 0) ? Utilities.newDate().plusDays(days) : null;
    }

    public int getCircleSecretRotationThreads() {
        return Math.max(1, Integer.parseInt(get(StandardSetting.CIRCLE_SECRET_ROTATION_THREADS.getKey()).trim()));
    }

    public Integer getInvitationTimeout() {
        return Integer.valueOf(get(StandardSetting.INVITATION_TIMEOUT.getKey()).trim());
    }

    public long getStreamMaxObjectSize() {
        return Long.parseLong(get(StandardSetting.STREAM_MAX_OBJECT_SIZE.getKey()).trim());
    }

    public int getStreamTransactionTimeout() {
        return Integer.parseInt(get(StandardSetting.STREAM_TRANSACTION_TIMEOUT.getKey()).trim());
    }

    public long getDataMaxInlineSize() {
        return Long.parseLong(get(StandardSetting.DATA_MAX_INLINE_SIZE.getKey()).trim());
    }

    public long getDataMaxTotalSize() {
        return Long.parseLong(get(StandardSetting.DATA_MAX_TOTAL_SIZE.getKey()).trim());
    }

    public int getSanityMaxObjects() {
        return Integer.parseInt(get(StandardSetting.SANITY_MAX_OBJECTS.getKey()).trim());
    }

    public int getStreamReadBatchSize() {
        return Integer.parseInt(get(StandardSetting.STREAM_READ_BATCH_SIZE.getKey()).trim());
    }

    public Integer getSessionTimeout() {
        return Integer.valueOf(get(StandardSetting.SESSION_TIMEOUT.getKey()).trim());
    }

    public int getSessionMaxPerMember() {
        return Math.max(1, Integer.parseInt(get(StandardSetting.SESSION_MAX_PER_MEMBER.getKey()).trim()));
    }

    /**
     * The number of failed passphrase attempts allowed within the window
     * before an account is locked (#68), 0 to {@link #MAX_AUTH_FAILED_ATTEMPTS};
     * 0 turns the lock off.
     *
     * @return Maximum failed attempts, or 0 when the lock is off
     */
    public int getAuthMaxFailedAttempts() {
        return authSetting(StandardSetting.AUTH_MAX_FAILED_ATTEMPTS, 0, MAX_AUTH_FAILED_ATTEMPTS);
    }

    /**
     * The window, in minutes, in which the failed attempts are counted (#68),
     * 1 to {@link #MAX_AUTH_MINUTES}.
     *
     * @return Failed-attempt window in minutes
     */
    public int getAuthFailedWindowMinutes() {
        return authSetting(StandardSetting.AUTH_FAILED_WINDOW_MINUTES, 1, MAX_AUTH_MINUTES);
    }

    /**
     * How long, in minutes, an account stays locked after too many failed
     * attempts (#68), 1 to {@link #MAX_AUTH_MINUTES}; the lock heals itself
     * when the time passes.
     *
     * @return Lockout time in minutes
     */
    public int getAuthLockoutMinutes() {
        return authSetting(StandardSetting.AUTH_LOCKOUT_MINUTES, 1, MAX_AUTH_MINUTES);
    }

    /**
     * Reads a setting of the lock (#68), which every check of a passphrase
     * reads: a stored value which isn't a number gives the default, and one
     * out of range the nearest limit, so no stored value can stop the
     * logins with a passphrase, or lock an account for good. A new value is
     * checked when it is set (SettingManager).
     */
    private int authSetting(final StandardSetting setting, final int min, final int max) {
        final String value = get(setting.getKey());
        int number;
        try {
            number = Integer.parseInt((value != null) ? value.trim() : setting.getValue());
        } catch (NumberFormatException _) {
            number = Integer.parseInt(setting.getValue());
        }

        return Math.clamp(number, min, max);
    }

    /**
     * How many passphrases are checked at once (ticket 20), 1 to
     * {@link #MAX_CONCURRENT_PASSPHRASES}; 0, the default, gives half the
     * processors, at least one.
     *
     * @return Passphrases checked at once
     */
    public int getAuthConcurrentPassphrases() {
        final int configured = authSetting(StandardSetting.AUTH_CONCURRENT_PASSPHRASES, 0, MAX_CONCURRENT_PASSPHRASES);
        return (configured > 0) ? configured : Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    }

    /**
     * How long, in milliseconds, a request waits for a place to check its
     * passphrase, before it is answered as busy (ticket 20), 0 to
     * {@link #MAX_PASSPHRASE_WAIT_MILLIS}.
     *
     * @return Milliseconds
     */
    public int getAuthPassphraseWaitMillis() {
        return authSetting(StandardSetting.AUTH_PASSPHRASE_WAIT_MILLIS, 0, MAX_PASSPHRASE_WAIT_MILLIS);
    }

    public String getStoneName() {
        final String name = get(StandardSetting.STONE_NAME.getKey());
        return ((name == null) || name.isBlank()) ? null : name.trim();
    }

    public long getStatusLoadInterval() {
        return Long.parseLong(get(StandardSetting.STATUS_LOAD_INTERVAL.getKey()).trim());
    }

    public long getStatusInventoryInterval() {
        return Math.max(1, Long.parseLong(get(StandardSetting.STATUS_INVENTORY_INTERVAL.getKey()).trim()));
    }

    public long getDownloadTokenLifetime() {
        return Long.parseLong(get(StandardSetting.DOWNLOAD_TOKEN_LIFETIME.getKey()).trim());
    }

    public String getMasterKeyURL() {
        return get(StandardSetting.MASTERKEY_URL.getKey()).trim();
    }

    public String getCORS() {
        return get(StandardSetting.CORS.getKey()).trim();
    }

    public boolean isReady() {
        return Boolean.parseBoolean(get(StandardSetting.IS_READY.getKey()).trim());
    }
}
