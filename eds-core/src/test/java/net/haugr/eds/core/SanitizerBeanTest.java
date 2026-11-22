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
package net.haugr.eds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.core.enums.SanityStatus;
import net.haugr.eds.core.enums.StandardSetting;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.managers.SanityManager;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.DataEntity;
import net.haugr.eds.core.managers.ProcessDataManager;
import net.haugr.eds.core.setup.DatabaseSetup;
import net.haugr.eds.core.setup.fakes.FakeEntityManager;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import java.io.ByteArrayInputStream;
import net.haugr.eds.core.model.DataDao;
import net.haugr.eds.core.model.EncryptedDataStore;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class SanitizerBeanTest extends DatabaseSetup {

    @Test
    void testEmptyConstructor() {
        final SanitizerBean bean = new SanitizerBean();

        assertNotNull(bean);
    }

    @Test
    void testEntityManagerConstructor() {
        final SanitizerBean bean = new SanitizerBean(entityManager, settings);

        assertNotNull(bean);
    }

    @Test
    void testStartupBeanWithSanitizeCheck() {
        final String runSanitizeAtStartup = "true";
        prepareInvalidData();
        prepareStartupBean(runSanitizeAtStartup);

        assertTrue(prepareSanityManager().findNextBatch(100).isEmpty());
    }

    @Test
    void testStartupBeanWithoutSanitizeCheck() {
        final String runSanitizeAtStartup = "false";
        prepareInvalidData();
        prepareStartupBean(runSanitizeAtStartup);

        assertEquals(6, prepareSanityManager().findNextBatch(100).size());
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
                .createNativeQuery("insert into eds_versions (schema_version, eds_version) values (9999, '99.9.9')")
                .executeUpdate();
        assertEquals(1, inserted);

        final StartupBean bean = prepareStartupBean("true");
        Assertions.assertFalse(getBeanSettings(bean).isReady());
    }

    @Test
    void testStartupBeanSanitizing() {
        prepareInvalidData();
        final SanityManager manager = prepareSanityManager();
        final List<Long> idsBefore = manager.findNextBatch(100);

        final StartupBean bean = prepareStartupBean("false");
        bean.runSanitizing();

        final List<Long> idsAfter = manager.findNextBatch(100);
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
        final SanityManager manager = prepareSanityManager();
        final SanitizerBean bean = new SanitizerBean(manager);
        prepareInvalidData();

        // Check that there is nothing to scan/check at first
        final List<Long> idsBefore = manager.findNextBatch(100);
        assertEquals(6, idsBefore.size());

        // Run the actual sanitizing
        bean.sanitize();

        // Finally, verify that all records have been sanitized.
        final List<Long> idsAfter = manager.findNextBatch(100);
        assertTrue(idsAfter.isEmpty());
    }

    @Test
    void testSanitizeBeanWithDatabaseProblem() {
        final SanityManager manager = prepareSanityManager();
        final SanityStatus status = manager.processEntity(123L);

        assertEquals(SanityStatus.BLOCKED, status);
    }

    /**
     * The sanity check reads each blob in batches and compares the SHA-512 of
     * the ciphertext; a damaged blob is marked as failed and reported.
     */
    @Test
    void testSanitizeFindsDamagedContent() {
        final ShareBean shareBean = prepareShareBean();
        final ProcessDataResponse added = shareBean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "scrubbed", 300_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        final DataDao dao = new DataDao(entityManager);
        final long id = (Long) dao.findContentInfo(dao.findMetadataByMemberAndExternalId(dao.findMemberByName(MEMBER_1).getId(), added.getDataId()))[0];

        // Damage one byte of the stored data, keeping the stored checksum,
        // and make the object due for a check.
        final byte[] stored = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, 400_000));
        stored[stored.length / 2] ^= 1;
        final String checksum = (String) entityManager.createQuery("select d.checksum from DataEntity d where d.id = :id").setParameter("id", id).getSingleResult();
        dao.withConnection(connection -> EncryptedDataStore.write(connection, id, new ByteArrayInputStream(stored), stored.length, "SHA-512"));
        entityManager.createQuery("update DataEntity d set d.checksum = :checksum, d.sanityChecked = :checked where d.id = :id")
                .setParameter("checksum", checksum)
                .setParameter("checked", Utilities.newDate().minusYears(10))
                .setParameter("id", id)
                .executeUpdate();

        new SanitizerBean(entityManager, settings).sanitize();

        entityManager.clear();
        assertEquals(SanityStatus.FAILED, entityManager.find(DataEntity.class, id).getSanityStatus());
        final SanityReport.Snapshot report = SanityReport.current();
        assertTrue(report.enabled());
        assertTrue(report.flawed() >= 1);
        assertTrue(report.failedTotal() >= 1);
        assertNotNull(report.lastRun());
    }

    /**
     * Content is checked with the algorithm its checksum was stored with, so
     * a change of the hash algorithm setting doesn't fail the content stored
     * before it.
     */
    @Test
    void testSanitizeKeepsContentOfAnotherHashAlgorithm() throws Exception {
        final ShareBean shareBean = prepareShareBean();
        final ProcessDataResponse added = shareBean.processData(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "older hash", 100_000));
        assertTrue(added.isOk(), added.getReturnMessage());
        final DataDao dao = new DataDao(entityManager);
        final long id = (Long) dao.findContentInfo(dao.findMetadataByMemberAndExternalId(dao.findMemberByName(MEMBER_1).getId(), added.getDataId()))[0];

        // The checksum as the other algorithm than the current one stores it.
        final String other = "SHA-512".equals(settings.getHashAlgorithm().getAlgorithm()) ? "SHA-384" : "SHA-512";
        final byte[] stored = dao.withConnection(connection -> EncryptedDataStore.read(connection, id, 0, 200_000));
        final String checksum = java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance(other).digest(stored));
        entityManager.createQuery("update DataEntity d set d.checksum = :checksum where d.id = :id")
                .setParameter("checksum", checksum)
                .setParameter("id", id)
                .executeUpdate();

        assertEquals(SanityStatus.OK, new SanityManager(settings, entityManager).processEntity(id));
    }

    @Test
    void testSanitizeCanBeSwitchedOffAndLimited() {
        final SanityManager manager = prepareSanityManager();
        prepareInvalidData();
        final int due = manager.findNextBatch(100).size();
        assertTrue(due > 2);

        final Settings off = newSettings();
        off.set(StandardSetting.SANITY_INTERVAL.getKey(), "0");
        new SanitizerBean(entityManager, off).sanitize();
        assertEquals(due, manager.findNextBatch(100).size());
        assertFalse(SanityReport.current().enabled());

        final Settings limited = newSettings();
        limited.set(StandardSetting.SANITY_MAX_OBJECTS.getKey(), "2");
        new SanitizerBean(entityManager, limited).sanitize();
        assertEquals(2, SanityReport.current().checked());
        assertEquals(due - 2, manager.findNextBatch(100).size());
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private StartupBean prepareStartupBean(final String sanityAtStartup) {
        final Settings newSettings = newSettings();
        newSettings.set(StandardSetting.SANITY_STARTUP.getKey(), sanityAtStartup);

        final StartupBean bean = new StartupBean(entityManager, prepareSanitizeBean(), newSettings);

        // The Bean is updating the Settings via the DB, so we need to alter
        // the content of the DB to reflect this. As the content has not yet
        // been read out - it is also not cached. Hence, a simple update will
        // suffice.
        final int updated = entityManager
                .createQuery("update SettingEntity set setting = :setting where name = :name")
                .setParameter("name", StandardSetting.SANITY_STARTUP.getKey())
                .setParameter("setting", sanityAtStartup)
                .executeUpdate();
        assertEquals(1, updated);

        // Run the startup, as the runtime does
        bean.startup();

        return bean;
    }

    private StartupBean prepareFlawedStartupBean(final Settings settings) {
        settings.set(StandardSetting.SANITY_STARTUP.getKey(), "true");

        return new StartupBean(new FakeEntityManager(), prepareSanitizeBean(), settings);
    }

    private SanityManager prepareSanityManager() {
        return new SanityManager(settings, entityManager);
    }

    private SanitizerBean prepareSanitizeBean() {
        return new SanitizerBean(prepareSanityManager());
    }

    // Reflection reaches what the test checks; it is not the contract.
    @SuppressWarnings("fb-contrib:RFI_SET_ACCESSIBLE")
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
        // the API. Hence, we have to modify it directly in the database!
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
