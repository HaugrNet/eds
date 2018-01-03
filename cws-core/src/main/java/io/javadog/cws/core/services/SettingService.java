/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.requests.SettingRequest;
import io.javadog.cws.api.responses.SettingResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.SettingEntity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
public final class SettingService extends Serviceable<SettingResponse, SettingRequest> {

    public SettingService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingResponse perform(final SettingRequest request) {
        verifyRequest(request, Permission.SETTING);

        final Map<String, SettingEntity> existing = convertSettings(dao.findAllAscending(SettingEntity.class, "id"));
        for (final Map.Entry<String, String> entry : request.getSettings().entrySet()) {
            final String key = trim(entry.getKey());
            if (key.isEmpty()) {
                throw new CWSException(ReturnCode.SETTING_WARNING, "Setting Keys may neither be null nor empty.");
            }

            final String value = trim(entry.getValue());
            final SettingEntity entity;

            if (existing.containsKey(key) && value.isEmpty()) {
                deleteSetting(existing.get(key));
            } else if (existing.containsKey(key)) {
                entity = existing.get(key);
                if (Objects.equals(entity.getName(), StandardSetting.CWS_SALT.getKey())) {
                    updateSaltOrThrowException(request, entity, key, value);
                } else if (!Objects.equals(entity.getSetting(), value)) {
                    persistAndUpdateSetting(entity, key, value);
                }
            } else {
                entity = new SettingEntity();
                persistAndUpdateSetting(entity, key, value);
            }
        }

        final SettingResponse response = new SettingResponse();
        response.setSettings(convert(settings));

        return response;
    }

    private void updateSaltOrThrowException(final SettingRequest request, final SettingEntity entity, final String key, final String value) {
        // If there is no change between the existing Salt and the one from the
        // request, then we will simply ignore it.
        if (!Objects.equals(entity.getSetting(), value)) {
            final Long members = dao.countMembers();

            if (members > 1) {
                // It is only allowed to update the System Salt provided that no
                // other account exists than the System Administrator. If that is
                // the case, then an exception is thrown.
                throw new CWSException(ReturnCode.SETTING_WARNING, "The setting " + key + " may not be overwritten.");
            }

            // First, save the updated Salt value
            persistAndUpdateSetting(entity, key, value);

            // Now the tricky part - we have to update the Admin Account also.
            updateMemberPassword(member, request.getCredential());
        }
    }

    private void deleteSetting(final SettingEntity entity) {
        if (StandardSetting.contains(entity.getName())) {
            throw new CWSException(ReturnCode.SETTING_WARNING, "It is not allowed to delete standard settings.");
        }

        settings.remove(entity.getName());
        dao.delete(entity);
    }

    private void persistAndUpdateSetting(final SettingEntity entity, final String key, final String value) {
        if (value.isEmpty()) {
            throw new CWSException(ReturnCode.SETTING_WARNING, "Cannot add a setting without a value.");
        }

        entity.setName(key);
        entity.setSetting(value);
        dao.persist(entity);
        settings.set(key, value);
    }

    private static Map<String, SettingEntity> convertSettings(final List<SettingEntity> list) {
        final Map<String, SettingEntity> map = new ConcurrentHashMap<>(16);

        for (final SettingEntity setting : list) {
            map.put(setting.getName(), setting);
        }

        return map;
    }

    private static Map<String, String> convert(final Settings settings) {
        final Map<String, String> map = new ConcurrentHashMap<>(16);
        for (final String key : settings.keys()) {
            map.put(key, settings.get(key));
        }

        return map;
    }

    private static String trim(final String value) {
        return (value != null) ? value.trim() : "";
    }
}
