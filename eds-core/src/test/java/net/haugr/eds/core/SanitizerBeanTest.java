/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.managers.ProcessDataManager;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.setup.fakes.FakeEntityManager;
import net.haugr.eds.core.setup.fakes.FakeTimer;
import net.haugr.eds.core.setup.fakes.FakeTimerService;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class SanitizerBeanTest extends DatabaseSetup {

    @Test
    void testStartupBeanWithSanitizeCheck() {
        final String runSanitizeAtStartup = "true";
        prepareInvalidData();
        prepareStartupBean(runSanitizeAtStartup);

        assertTrue(prepareSanitizeBean().findNextBatch(100).isEmpty());
    }

    @Test
    void testStartupBeanWithoutSanitizeCheck() {
        final String runSanitizeAtStartup = "false";
        prepareInvalidData();
        prepareStartupBean(runSanitizeAtStartup);

        assertEquals(6, prepareSanitizeBean().findNextBatch(100).size());
    }

    @Test
    void testStartupBeanWithEmptyVersionTable() {
        final int deleted = entityManager
                .createNativeQuery("delete from eds_versions where id > 0")
                .executeUpdate();
        assertTrue(deleted >= 1);

        final StartupBean bean = prepareStartupBean("true");
        Assertions.assertFalse(getBeanSettings(bean).isReady());
    }

    @Test
    void testStartupBeanWithDifferentVersion() {
        final int inserted = entityManager
                .createNativeQuery("insert into eds_versions (schema_version, eds_version, db_vendor) values (9999, '99.9.9', 'H2')")
                .executeUpdate();
        assertEquals(1, inserted);

        final StartupBean bean = prepareStartupBean("true");
        Assertions.assertFalse(getBeanSettings(bean).isReady());
    }

    @Test
    void testStartupBeanTimerService() {
        prepareInvalidData();
        final SanitizerBean sanitizerBean = prepareSanitizeBean();
        final List<Long> idsBefore = sanitizerBean.findNextBatch(100);

        final StartupBean bean = prepareStartupBean("false");
        bean.runSanitizing(new FakeTimer());

        final List<Long> idsAfter = sanitizerBean.findNextBatch(100);
        assertFalse(idsBefore.isEmpty());
        assertTrue(idsAfter.isEmpty());
    }

    @Test
    void testStartupBeanWithDatabaseProblem() {
        final Settings settings = newSettings();
        final StartupBean bean = prepareFlawedStartupBean(settings);

        // The isReady flag is by default set to true in our system. But, as
        // we're running the flawed Startup Bean, which has DB problems - the
        // flag will be set to false.
        assertTrue(settings.isReady());
        bean.startup();
        assertFalse(settings.isReady());
    }

    @Test
    void testSanitizeBean() {
        final SanitizerBean bean = prepareSanitizeBean();
        prepareInvalidData();

        // Check that there is nothing to scan/check at first
        final List<Long> idsBefore = bean.findNextBatch(100);
        assertEquals(6, idsBefore.size());

        // Run the actual sanitizing
        bean.sanitize();

        // Finally, verify that all records have been sanitized.
        final List<Long> idsAfter = bean.findNextBatch(100);
        assertTrue(idsAfter.isEmpty());
    }

    @Test
    void testSanitizeBeanWithDatabaseProblem() {
        final SanitizerBean bean = prepareFlawedSanitizeBean();
        final SanityStatus status = bean.processEntity(123L);

        assertEquals(SanityStatus.BLOCKED, status);
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private StartupBean prepareStartupBean(final String sanityAtStartup) {
        try {
            final StartupBean bean = StartupBean.class.getConstructor().newInstance();

            final Settings newSettings = newSettings();
            newSettings.set(StandardSetting.SANITY_STARTUP.getKey(), sanityAtStartup);

            // Inject Dependencies
            inject(bean, entityManager);
            inject(bean, prepareSanitizeBean());
            inject(bean, new FakeTimerService());
            inject(bean, newSettings);

            // The Bean is updating the Settings via the DB, so we need to alter
            // the content of the DB to reflect this. As the content has not yet
            // been read out - it is also not cached, hence a simple update will
            // suffice.
            final int updated = entityManager
                    .createQuery("update SettingEntity set setting = :setting where name = :name")
                    .setParameter("name", StandardSetting.SANITY_STARTUP.getKey())
                    .setParameter("setting", sanityAtStartup)
                    .executeUpdate();
            assertEquals(1, updated);

            // Invoke PostConstructor
            bean.startup();

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private StartupBean prepareFlawedStartupBean(final Settings settings) {
        try {
            final StartupBean bean = StartupBean.class.getConstructor().newInstance();

            settings.set(StandardSetting.SANITY_STARTUP.getKey(), "true");

            // Inject Dependencies
            inject(bean, new FakeEntityManager());
            inject(bean, prepareSanitizeBean());
            inject(bean, new FakeTimerService());
            inject(bean, settings);

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SanitizerBean prepareSanitizeBean() {
        try {
            final SanitizerBean bean = SanitizerBean.class.getConstructor().newInstance();
            inject(bean, entityManager);

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SanitizerBean prepareFlawedSanitizeBean() {
        try {
            final SanitizerBean bean = SanitizerBean.class.getConstructor().newInstance();
            inject(bean, new FakeEntityManager());

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private static Settings getBeanSettings(final StartupBean bean) {
        try {
            final Class<?> clazz = bean.getClass();
            final Field field = clazz.getDeclaredField("settings");
            final boolean isAccessible = field.canAccess(bean);

            field.setAccessible(true);
            final Settings beanSettings = (Settings) field.get(bean);
            field.setAccessible(isAccessible);

            return beanSettings;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new EDSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private void prepareInvalidData() {
        final ProcessDataManager service = new ProcessDataManager(settings, entityManager);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Valid Data1", LARGE_SIZE_BYTES)), Utilities.newDate(1L));
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data1", LARGE_SIZE_BYTES)), Utilities.newDate(2L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data2", MEDIUM_SIZE_BYTES)), Utilities.newDate(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Valid Data2", LARGE_SIZE_BYTES)), Utilities.newDate(3L));
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data3", LARGE_SIZE_BYTES)), Utilities.newDate(4L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data4", MEDIUM_SIZE_BYTES)), Utilities.newDate(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Valid Data3", LARGE_SIZE_BYTES)), Utilities.newDate(5L));
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data5", LARGE_SIZE_BYTES)), Utilities.newDate(6L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data6", MEDIUM_SIZE_BYTES)), Utilities.newDate(), SanityStatus.OK);
    }

    private void timeWarpChecksum(final ProcessDataResponse response, final LocalDateTime sanityCheck) {
        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by EDS, it cannot be altered (rightfully) via
        // the API, hence we have to modify it directly in the database!
        final String jql = "select d from DataEntity d where d.metadata.externalId = :eid";
        final DataEntity entity = (DataEntity) entityManager
                .createQuery(jql)
                .setParameter("eid", response.getDataId())
                .getSingleResult();
        entity.setSanityStatus(SanityStatus.OK);
        entity.setSanityChecked(sanityCheck);
        entityManager.persist(entity);
    }
}
