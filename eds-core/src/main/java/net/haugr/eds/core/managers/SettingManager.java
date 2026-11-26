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
package net.haugr.eds.core.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.requests.SettingRequest;
import net.haugr.eds.api.responses.SettingResponse;
import net.haugr.eds.core.enums.HashAlgorithm;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.SettingEntity;

/**
 * <p>The Setting Service allows for checking and updating existing Settings
 * and add new Settings, needed for Clients. Certain of the existing Settings
 * have a flag that will not allow them to be updated. Most settings may or can
 * be updated, but if the Flag is present, it is for a good reason - as it
 * prevents a change that can affect the system.</p>
 *
 * <p>The only entry which may not be altered here is the MasterKey URL, which
 * is changed with the MasterKey request. The PBE iterations count for new
 * passphrases; each account keeps those of its own.</p>
 *
 * <p>For the same reason, the request can <b>only</b> be invoked by the System
 * Administrator. Since changing things must be strictly limited.</p>
 *
 * <p>The Setting service is trying to be as error-tolerant as possible, meaning
 * that null Keys and null Values will be converted into empty Strings. If an
 * attempt at making updates to a Key, which is not permitted to be updated, an
 * error will occur.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class SettingManager extends AbstractManager<CommonDao, SettingResponse, SettingRequest> {

    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");
    private static final int MIN_PBE_ITERATIONS = 1000;

    public SettingManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse perform(final SettingRequest request) {
        // Pre-checks, postponing destruction of credentials
        verifyRequest(request, Permission.SETTING);

        // Updating the Settings should be all or nothing. If only partially
        // updated, it may be impossible for the System Administrator to know if
        // the system behaves as expected. This means that the pre-checks must
        // cover all problems, so the actual update will not result in any other
        // issues.
        //   Before making the updates, the keys and values from the request
        // must therefore be inspected. The following method will do this and
        // return a new Map of keys & values for those that need updating.
        final Map<String, String> changedEntries = findChangedEntries(request);

        // All issues should've been covered, meaning that we can now safely
        // update the settings in the DB & the Settings Object.
        processCheckedSettings(changedEntries);

        // All corrections have been made, now we can destroy the credentials
        Arrays.fill(request.getCredential(), (byte) 0);

        final SettingResponse response = new SettingResponse();
        response.setSettings(convert(settings));
        response.setAllowedValues(allowedValues());

        return response;
    }

    private Map<String, String> findChangedEntries(final SettingRequest request) {
        final Map<String, String> map = new ConcurrentHashMap<>();

        for (final Map.Entry<String, String> entry : request.getSettings().entrySet()) {
            final String key = trim(entry.getKey());
            if (isEmpty(key)) {
                throw new EDSException(ReturnCode.SETTING_WARNING, "Setting Keys may neither be null nor empty.");
            }

            final String value = trim(entry.getValue());
            if (!Objects.equals(settings.get(key), value)) {
                // The key and value as they are stored; before, a blank around
                // the key passed every check of a standard setting.
                checkStandardSetting(key, value);
                map.put(key, value);
            }
        }

        return map;
    }

    private void processCheckedSettings(final Map<String, String> changedEntries) {
        final Map<String, SettingEntity> existing = convertSettings(dao.findAllAscending(SettingEntity.class, "id"));
        for (final Map.Entry<String, String> entry : changedEntries.entrySet()) {
            processSettingRecord(existing, entry);
        }
    }

    private void processSettingRecord(final Map<String, SettingEntity> existing, final Map.Entry<String, String> entry) {
        final String key = trim(entry.getKey());
        final SettingEntity existingSetting = existing.get(key);
        final String value = trim(entry.getValue());

        if (existingSetting != null) {
            if (isEmpty(value)) {
                deleteSetting(existingSetting);
            } else {
                persistAndUpdateSetting(existingSetting, key, value);
            }
        } else {
            final SettingEntity entity = new SettingEntity();
            persistAndUpdateSetting(entity, key, value);
        }
    }

    private static void checkStandardSetting(final String key, final String value) {
        final StandardSetting standardSetting = StandardSetting.find(key);
        if (standardSetting != null) {
            if (isEmpty(value)) {
                // A stone needn't have a name.
                if (standardSetting == StandardSetting.STONE_NAME) {
                    return;
                }
                throw new EDSException(ReturnCode.SETTING_WARNING, "The value for the key '" + key + "' is undefined.");
            } else {
                checkAllowedValues(standardSetting, value);
            }
        }
    }

    private static void checkAllowedValues(final StandardSetting setting, final String value) {
        switch (setting) {
            case SYMMETRIC_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.SYMMETRIC, setting, value);
                break;
            case ASYMMETRIC_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.ASYMMETRIC, setting, value);
                break;
            case HASH_ALGORITHM:
                checkHashAlgorithm(setting, value);
                break;
            case PBE_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.PASSWORD, setting, value);
                break;
            case PBE_ITERATIONS:
                // Each account keeps the iterations of its passphrase, so
                // the setting only counts for new passphrases.
                checkIterations(setting, value);
                break;
            case SANITY_INTERVAL, INVITATION_TIMEOUT, SESSION_MAX_PER_MEMBER, DOWNLOAD_TOKEN_LIFETIME, STREAM_MAX_OBJECT_SIZE,
                 STREAM_TRANSACTION_TIMEOUT, STREAM_READ_BATCH_SIZE, DATA_MAX_INLINE_SIZE, DATA_MAX_TOTAL_SIZE, SANITY_MAX_OBJECTS:
                checkNumber(setting, value);
                break;
            case STONE_NAME:
                if (value.trim().length() > Constants.MAX_STONE_NAME_LENGTH) {
                    throw new EDSException(ReturnCode.SETTING_WARNING, "The value for '" + setting.getKey() + "' has at most " + Constants.MAX_STONE_NAME_LENGTH + " characters.");
                }
                break;
            case STATUS_LOAD_INTERVAL:
                checkRange(setting, value, 0, 3600);
                break;
            case STATUS_INVENTORY_INTERVAL:
                checkRange(setting, value, 1, 10080);
                break;
            case CIRCLE_SECRET_ROTATION_DAYS:
                checkRange(setting, value, 0, 36500);
                break;
            case CIRCLE_SECRET_ROTATION_THREADS:
                checkRange(setting, value, 1, 64);
                break;
            case AUTH_MAX_FAILED_ATTEMPTS:
                checkRange(setting, value, 0, Settings.MAX_AUTH_FAILED_ATTEMPTS);
                break;
            case AUTH_FAILED_WINDOW_MINUTES, AUTH_LOCKOUT_MINUTES:
                checkRange(setting, value, 1, Settings.MAX_AUTH_MINUTES);
                break;
            case AUTH_CONCURRENT_PASSPHRASES:
                checkRange(setting, value, 0, Settings.MAX_CONCURRENT_PASSPHRASES);
                break;
            case AUTH_PASSPHRASE_WAIT_MILLIS:
                checkRange(setting, value, 0, Settings.MAX_PASSPHRASE_WAIT_MILLIS);
                break;
            default:
                checkOtherSettings(setting);
                break;
        }
    }

    private static void checkRange(final StandardSetting setting, final String value, final int min, final int max) {
        checkNumber(setting, value);
        if ((value.length() > 9) || (Integer.parseInt(value) < min) || (Integer.parseInt(value) > max)) {
            throw new EDSException(ReturnCode.SETTING_WARNING, "The value for '" + setting.getKey() + "' must be from " + min + " to " + max + '.');
        }
    }

    private static void checkHashAlgorithm(final StandardSetting setting, final String value) {
        final boolean match = Arrays.stream(HashAlgorithm.values())
                .anyMatch(algorithm -> Objects.equals(algorithm.name(), value));

        if (!match) {
            throw new EDSException(ReturnCode.SETTING_WARNING, "Unsupported Crypto Algorithm for '" + setting.getKey() + "'.");
        }
    }

    private static void checkAlgorithm(final KeyAlgorithm.Type type, final StandardSetting setting, final String value) {
        final Set<KeyAlgorithm> algorithms = findEntriesForType(type);

        boolean match = false;
        for (final KeyAlgorithm algorithm : algorithms) {
            if (Objects.equals(algorithm.name(), value)) {
                match = true;
                break;
            }
        }

        if (!match) {
            throw new EDSException(ReturnCode.SETTING_WARNING, "Unsupported Crypto Algorithm for '" + setting.getKey() + "'.");
        }
    }

    private static Set<KeyAlgorithm> findEntriesForType(final KeyAlgorithm.Type type) {
        final Set<KeyAlgorithm> set = EnumSet.noneOf(KeyAlgorithm.class);

        for (final KeyAlgorithm algorithm : KeyAlgorithm.values()) {
            if (algorithm.getType() == type) {
                set.add(algorithm);
            }
        }

        return set;
    }

    private static void checkNumber(final StandardSetting setting, final String value) {
        if (!PATTERN_NUMBER.matcher(value).matches()) {
            throw new EDSException(ReturnCode.SETTING_WARNING, "Invalid Integer value for '" + setting + "'.");
        }
    }

    private static void checkIterations(final StandardSetting setting, final String value) {
        checkNumber(setting, value);
        // Fewer make the passphrases easy to guess; more than an int can't be used.
        if ((value.length() > 9) || (Integer.parseInt(value) < MIN_PBE_ITERATIONS)) {
            throw new EDSException(ReturnCode.SETTING_WARNING, "The value for '" + setting.getKey() + "' must be from " + MIN_PBE_ITERATIONS + " to 999999999.");
        }
    }

    private static void checkOtherSettings(final StandardSetting setting) {
        // Two settings may not be changed with this request: the MasterKey
        // URL, which only the MasterKey request changes, and whether EDS is
        // ready, which EDS sets itself; set to false, every request was
        // refused until a restart (ticket 14).
        if ((setting == StandardSetting.MASTERKEY_URL) || (setting == StandardSetting.IS_READY)) {
            throw new EDSException(ReturnCode.SETTING_WARNING, "The setting " + setting.getKey() + " may not be changed with this request.");
        }
    }

    private void deleteSetting(final SettingEntity entity) {
        settings.remove(entity.getName());
        dao.delete(entity);
    }

    private void persistAndUpdateSetting(final SettingEntity entity, final String key, final String value) {
        // First, just update the setting.
        entity.setName(key);
        entity.setSetting(value);
        dao.save(entity);
        settings.set(key, value);
    }

    private static Map<String, SettingEntity> convertSettings(final Collection<SettingEntity> list) {
        final Map<String, SettingEntity> map = new ConcurrentHashMap<>(16);

        for (final SettingEntity setting : list) {
            map.put(setting.getName(), setting);
        }

        return map;
    }

    /**
     * The values EDS accepts for the settings with a fixed choice: the
     * algorithms it supports.
     *
     * @return Setting keys with their allowed values
     */
    public static Map<String, List<String>> allowedValues() {
        final Map<String, List<String>> map = new HashMap<>();
        map.put(StandardSetting.SYMMETRIC_ALGORITHM.getKey(), names(findEntriesForType(KeyAlgorithm.Type.SYMMETRIC)));
        map.put(StandardSetting.ASYMMETRIC_ALGORITHM.getKey(), names(findEntriesForType(KeyAlgorithm.Type.ASYMMETRIC)));
        map.put(StandardSetting.PBE_ALGORITHM.getKey(), names(findEntriesForType(KeyAlgorithm.Type.PASSWORD)));
        map.put(StandardSetting.HASH_ALGORITHM.getKey(), names(EnumSet.allOf(HashAlgorithm.class)));

        return map;
    }

    private static List<String> names(final Collection<? extends Enum<?>> values) {
        final List<String> names = new ArrayList<>(values.size());
        for (final Enum<?> value : values) {
            names.add(value.name());
        }

        return names;
    }

    public static Map<String, String> convert(final Settings settings) {
        final Map<String, String> map = new ConcurrentHashMap<>(16);
        for (final String key : settings.keys()) {
            map.put(key, settings.get(key));
        }

        return map;
    }
}
