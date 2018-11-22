/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.services.ProcessDataService;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.persistence.Query;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class SanitizerBeanTest extends DatabaseSetup {

    @Test
    public void testStartupBeanWithSanitizeCheck() {
        prepareInvalidData();
        prepareStartupBean("true");

        assertThat(prepareSanitizeBean().findNextBatch(100).isEmpty(), is(true));
    }

    @Test
    public void testStartupBeanWithEmptyVersionTable() {
        final Query query = entityManager.createNativeQuery("delete from cws_versions");
        query.executeUpdate();

        final StartupBean bean = prepareStartupBean("true");
        assertThat(getBeanSettings(bean).isReady(), is(false));
    }

    @Test
    public void testStartupBeanWithDifferentVersion() {
        final Query query = entityManager.createNativeQuery("insert into cws_versions (schema_version, cws_version, db_vendor) values (9999, '99.9.9', 'H2')");
        query.executeUpdate();

        final StartupBean bean = prepareStartupBean("true");
        assertThat(getBeanSettings(bean).isReady(), is(false));
    }

    @Test
    public void testStartupBeanWithoutSanitizeCheck() {
        prepareInvalidData();
        prepareStartupBean("false");

        assertThat(prepareSanitizeBean().findNextBatch(100).isEmpty(), is(false));
    }

    @Test
    public void testStartupBeanTimerService() {
        prepareInvalidData();
        final SanitizerBean sanitizerBean = prepareSanitizeBean();
        final List<Long> idsBefore = sanitizerBean.findNextBatch(100);

        final StartupBean bean = prepareStartupBean("false");
        bean.runSanitizing(new TestTimer());

        final List<Long> idsAfter = sanitizerBean.findNextBatch(100);
        assertThat(idsBefore.isEmpty(), is(false));
        assertThat(idsAfter.isEmpty(), is(true));
    }

    @Test
    public void testSantizeBean() {
        final SanitizerBean bean = prepareSanitizeBean();
        prepareInvalidData();

        // Check that there is nothing to scan/check at first
        final List<Long> idsBefore = bean.findNextBatch(100);
        assertThat(idsBefore.size(), is(6));

        // Run the actual sanitizing
        bean.sanitize();

        // Finally, verify that all records have been sanitized.
        final List<Long> idsAfter = bean.findNextBatch(100);
        assertThat(idsAfter.isEmpty(), is(true));
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
            setField(bean, "entityManager", entityManager);
            setField(bean, "sanitizerBean", prepareSanitizeBean());
            setField(bean, "timerService", new TestTimerService());
            setField(bean, "settings", newSettings);

            // The Bean is updating the Settings via the DB, so we need to alter
            // the content of the DB to reflect this. As the content has not yet
            // been read out - it is also not cached, hence a simply update will
            // suffice.
            final Query query = entityManager.createQuery("update SettingEntity set setting = :setting where name = :name");
            query.setParameter("name", StandardSetting.SANITY_STARTUP.getKey());
            query.setParameter("setting", sanityAtStartup);
            query.executeUpdate();

            // Invoke PostConstructor
            bean.startup();

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SanitizerBean prepareSanitizeBean() {
        try {
            final SanitizerBean bean = SanitizerBean.class.getConstructor().newInstance();

            // Inject Dependencies
            setField(bean, "entityManager", entityManager);

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private static Settings getBeanSettings(final StartupBean bean) {
        try {
            final Class<?> clazz = bean.getClass();
            final Field field = clazz.getDeclaredField("settings");
            final boolean isAccessible = field.isAccessible();

            field.setAccessible(true);
            final Settings beanSettings = (Settings) field.get(bean);
            field.setAccessible(isAccessible);

            return beanSettings;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private static class TestTimer implements Timer {

        @Override
        public void cancel() throws IllegalStateException, EJBException {

        }

        @Override
        public long getTimeRemaining() throws IllegalStateException, EJBException {
            return 0;
        }

        @Override
        public Date getNextTimeout() throws IllegalStateException, EJBException {
            return null;
        }

        @Override
        public ScheduleExpression getSchedule() throws IllegalStateException, EJBException {
            return null;
        }

        @Override
        public boolean isPersistent() throws IllegalStateException, EJBException {
            return false;
        }

        @Override
        public boolean isCalendarTimer() throws IllegalStateException, EJBException {
            return false;
        }

        @Override
        public Serializable getInfo() throws IllegalStateException, EJBException {
            return null;
        }

        @Override
        public TimerHandle getHandle() throws IllegalStateException, EJBException {
            return null;
        }
    }

    private static class TestTimerService implements TimerService {

        @Override
        public Timer createTimer(final long duration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createSingleActionTimer(final long duration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createTimer(final long initialDuration, final long intervalDuration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createIntervalTimer(final long initialDuration, final long intervalDuration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createTimer(final Date expiration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createSingleActionTimer(final Date expiration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createTimer(final Date initialExpiration, final long intervalDuration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createIntervalTimer(final Date initialExpiration, final long intervalDuration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createCalendarTimer(final ScheduleExpression schedule) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Timer createCalendarTimer(final ScheduleExpression schedule, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
            return null;
        }

        @Override
        public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
            return null;
        }
    }

    private void prepareInvalidData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Valid Data1", 1048576)), new Date(1L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data1", 1048576)), new Date(2L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data2", 524288)), new Date(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Valid Data2", 1048576)), new Date(3L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data3", 1048576)), new Date(4L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data4", 524288)), new Date(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Valid Data3", 1048576)), new Date(5L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data5", 1048576)), new Date(6L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data6", 524288)), new Date(), SanityStatus.OK);
    }

    private void timeWarpChecksum(final ProcessDataResponse response, final Date sanityCheck, final SanityStatus status) {
        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by CWS, it cannot be altered (rightfully) via
        // the API, hence we have to modify it directly in the database!
        final String jql = "select d from DataEntity d where d.metadata.externalId = :eid";
        final Query query = entityManager.createQuery(jql);
        query.setParameter("eid", response.getDataId());
        final DataEntity entity = (DataEntity) query.getSingleResult();
        entity.setSanityStatus(status);
        entity.setSanityChecked(sanityCheck);
        entityManager.persist(entity);
    }
}
