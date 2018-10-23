/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.SettingEntity;

import javax.persistence.EntityManager;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * <p>The Setting Service, allows for checking and updating existing Settings
 * and add new Settings, needed for Clients. Certain of the existing Settings
 * have a flag that will not allow them to be updated. Most settings may or can
 * be updated, but if the Flag is present, it is for a good reason - as it
 * prevents a change that can affect the system.</p>
 *
 * <p>Version 1.0 of CWS is only having one entry which may not be altered, the
 * System Salt. Reason is that if this is being changed, it will prevent anyone
 * from accessing the system, as the Member Keys cannot be unlocked.</p>
 *
 * <p>For the same reason, the request can <b>only</b> be invoked by the System
 * Administrator. Since changing things must be strictly limited.</p>
 *
 * <p>The Setting service is trying to be as error tolerant as possible, meaning
 * that null Keys and null Values will be converted into empty Strings. If an
 * attempt at making updates to a Key, which is not permitted to be updated, an
 * error will occur.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SettingService extends Serviceable<CommonDao, SettingResponse, SettingRequest> {

    private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");

    public SettingService(final Settings settings, final EntityManager entityManager) {
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
        // updated it may be impossible for the System Administrator to know if
        // the system behaves as expected. This means that the pre-checks must
        // cover all problems, so the actual update will not result in any other
        // issues.
        //   Before making the updates, the keys and values from the request
        // must therefore be inspected. The following method will do this, and
        // return a new Map of keys & values for those that needs updating.
        final Map<String, String> changedEntries = findChangedEntries(request);

        // All issues should've been covered, meaning that we can now safely
        // update the settings in the DB & the Settings Object.
        processCheckedSettings(request, changedEntries);

        // All corrections have been made, now we can destroy the credentials
        Arrays.fill(request.getCredential(), (byte) 0);

        final SettingResponse response = new SettingResponse();
        response.setSettings(convert(settings));

        return response;
    }

    private Map<String, String> findChangedEntries(final SettingRequest request) {
        final Map<String, String> map = new ConcurrentHashMap<>();

        for (final Map.Entry<String, String> entry : request.getSettings().entrySet()) {
            final String key = trim(entry.getKey());
            if (isEmpty(key)) {
                throw new CWSException(ReturnCode.SETTING_WARNING, "Setting Keys may neither be null nor empty.");
            }

            final String value = trim(entry.getValue());
            if (!Objects.equals(settings.get(key), value)) {
                checkStandardSetting(entry);
                map.put(key, value);
            }
        }

        return map;
    }

    private void processCheckedSettings(final SettingRequest request, final Map<String, String> changedEntries) {
        final Map<String, SettingEntity> existing = convertSettings(dao.findAllAscending(SettingEntity.class, "id"));
        for (final Map.Entry<String, String> entry : changedEntries.entrySet()) {
            final String key = trim(entry.getKey());
            final SettingEntity existingSetting = existing.get(key);
            final String value = trim(entry.getValue());

            if (existingSetting != null) {
                if (isEmpty(value)) {
                    deleteSetting(existingSetting);
                } else {
                    persistAndUpdateSetting(request, existingSetting, key, value);
                }
            } else {
                final SettingEntity entity = new SettingEntity();
                persistAndUpdateSetting(request, entity, key, value);
            }
        }
    }

    private void checkStandardSetting(final Map.Entry<String, String> entry) {
        final StandardSetting standardSetting = StandardSetting.find(entry.getKey());
        if (standardSetting != null) {
            if (isEmpty(entry.getValue())) {
                throw new CWSException(ReturnCode.SETTING_WARNING, "The value for the key '" + entry.getKey() + "' is undefined.");
            } else {
                precheckAllowedValues(standardSetting, entry.getValue());
            }
        }
    }

    private void precheckAllowedValues(final StandardSetting setting, final String value) {
        switch (setting) {
            case SYMMETRIC_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.SYMMETRIC, setting, value);
                break;
            case ASYMMETRIC_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.ASYMMETRIC, setting, value);
                break;
            case SIGNATURE_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.SIGNATURE, setting, value);
                break;
            case PBE_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.PASSWORD, setting, value);
                break;
            case PBE_ITERATIONS:
                // Changing the Iterations may only be done if no other accounts
                // exist, as it will render all accounts useless otherwise
                checkIfMembersExist(setting);
                checkNumber(setting, value);
                break;
            case HASH_ALGORITHM:
                checkAlgorithm(KeyAlgorithm.Type.SIGNATURE, setting, value);
                break;
            case CWS_CHARSET:
                checkCharset(setting, value);
                break;
            case SANITY_INTERVAL:
                checkNumber(setting, value);
                break;
            case CWS_SALT:
                checkIfMembersExist(setting);
                break;
            default:
                break;
        }
    }

    private static void checkAlgorithm(final KeyAlgorithm.Type type, final StandardSetting setting, final String value) {
        final Set<KeyAlgorithm> algorithms = findEntriesForType(type);

        boolean match = false;
        for (final KeyAlgorithm algorithm : algorithms) {
            if (Objects.equals(algorithm.name(), value)) {
                match = true;
            }
        }

        if (!match) {
            throw new CWSException(ReturnCode.SETTING_WARNING, "Unsupported Crypto Algorithm for '" + setting.getKey() + "'.");
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

    private static void checkCharset(final StandardSetting setting, final String value) {
        try {
            Charset.forName(value);
        } catch (IllegalArgumentException e) {
            throw new CWSException(ReturnCode.SETTING_WARNING, "Invalid Character set value for '" + setting.getKey() + "'.", e);
        }
    }

    private static void checkNumber(final StandardSetting setting, final String value) {
        if (!PATTERN_NUMBER.matcher(value).matches()) {
            throw new CWSException(ReturnCode.SETTING_WARNING, "Invalid Integer value for '" + setting + "'.");
        }
    }

    private void checkIfMembersExist(final StandardSetting setting) {
        // If there is no change between the existing Salt and the one from the
        // request, then we will simply ignore it.
        if (dao.countMembers() > 1) {
            // It is only allowed to update the System Salt provided that no
            // other account exists than the System Administrator. If that is
            // the case, then an exception is thrown.
            throw new CWSException(ReturnCode.SETTING_WARNING, "The setting " + setting.getKey() + " may not be overwritten.");
        }
    }

    private void deleteSetting(final SettingEntity entity) {
        settings.remove(entity.getName());
        dao.delete(entity);
    }

    private void persistAndUpdateSetting(final SettingRequest request, final SettingEntity entity, final String key, final String value) {
        // First, just update the setting.
        entity.setName(key);
        entity.setSetting(value);
        dao.persist(entity);
        settings.set(key, value);

        // Now the tricky part - if we have a critical setting (Salt or PBE
        // Iterations), which can only be updated when no members exist, we
        // also have to update the System Administrator account, otherwise
        // the new change will not work correctly.
        if (Objects.equals(key, StandardSetting.CWS_SALT.getKey()) ||
            Objects.equals(key, StandardSetting.PBE_ITERATIONS.getKey())) {
            // As the System Administrator is not having any Circles, the
            // newly generated Asymmetric Key for the updated account can be
            // ignored.
            updateMemberPassword(member, request.getCredential());
        }
    }

    private static Map<String, SettingEntity> convertSettings(final List<SettingEntity> list) {
        final Map<String, SettingEntity> map = new ConcurrentHashMap<>(16);

        for (final SettingEntity setting : list) {
            map.put(setting.getName(), setting);
        }

        return map;
    }

    public static Map<String, String> convert(final Settings settings) {
        final Map<String, String> map = new ConcurrentHashMap<>(16);
        for (final String key : settings.keys()) {
            map.put(key, settings.get(key));
        }

        return map;
    }
}
