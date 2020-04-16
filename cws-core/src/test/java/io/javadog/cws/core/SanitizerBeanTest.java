/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package io.javadog.cws.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.responses.ProcessDataResponse;
import io.javadog.cws.core.enums.SanityStatus;
import io.javadog.cws.core.enums.StandardSetting;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.DataEntity;
import io.javadog.cws.core.services.ProcessDataService;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerHandle;
import javax.ejb.TimerService;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;

/**
 * @author Kim Jensen
 * @since CWS 1.0
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
        final Query query = entityManager.createNativeQuery("delete from cws_versions where id > 0");
        query.executeUpdate();

        final StartupBean bean = prepareStartupBean("true");
        assertFalse(getBeanSettings(bean).isReady());
    }

    @Test
    void testStartupBeanWithDifferentVersion() {
        final Query query = entityManager.createNativeQuery("insert into cws_versions (schema_version, cws_version, db_vendor) values (9999, '99.9.9', 'H2')");
        query.executeUpdate();

        final StartupBean bean = prepareStartupBean("true");
        assertFalse(getBeanSettings(bean).isReady());
    }

    @Test
    void testStartupBeanTimerService() {
        prepareInvalidData();
        final SanitizerBean sanitizerBean = prepareSanitizeBean();
        final List<Long> idsBefore = sanitizerBean.findNextBatch(100);

        final StartupBean bean = prepareStartupBean("false");
        bean.runSanitizing(new TestTimer());

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
            inject(bean, new TestTimerService());
            inject(bean, newSettings);

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

    private StartupBean prepareFlawedStartupBean(final Settings settings) {
        try {
            final StartupBean bean = StartupBean.class.getConstructor().newInstance();

            settings.set(StandardSetting.SANITY_STARTUP.getKey(), "true");

            // Inject Dependencies
            inject(bean, new TestEntityManager());
            inject(bean, prepareSanitizeBean());
            inject(bean, new TestTimerService());
            inject(bean, settings);

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SanitizerBean prepareSanitizeBean() {
        try {
            final SanitizerBean bean = SanitizerBean.class.getConstructor().newInstance();
            inject(bean, entityManager);

            return bean;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new CWSException(ReturnCode.ERROR, "Cannot instantiate Service Object", e);
        }
    }

    private SanitizerBean prepareFlawedSanitizeBean() {
        try {
            final SanitizerBean bean = SanitizerBean.class.getConstructor().newInstance();
            inject(bean, new TestEntityManager());

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

    /**
     * @author Kim Jensen
     * @since CWS 1.0
     */
    private static class TestTimer implements Timer {

        /**
         * {@inheritDoc}
         */
        @Override
        public void cancel() throws IllegalStateException, EJBException {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getTimeRemaining() throws IllegalStateException, EJBException {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Date getNextTimeout() throws IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ScheduleExpression getSchedule() throws IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isPersistent() throws IllegalStateException, EJBException {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCalendarTimer() throws IllegalStateException, EJBException {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Serializable getInfo() throws IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TimerHandle getHandle() throws IllegalStateException, EJBException {
            return null;
        }
    }

    /**
     * @author Kim Jensen
     * @since CWS 1.0
     */
    private static class TestTimerService implements TimerService {

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createTimer(final long duration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createSingleActionTimer(final long duration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createTimer(final long initialDuration, final long intervalDuration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createIntervalTimer(final long initialDuration, final long intervalDuration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createTimer(final Date expiration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createSingleActionTimer(final Date expiration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createTimer(final Date initialExpiration, final long intervalDuration, final Serializable info) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createIntervalTimer(final Date initialExpiration, final long intervalDuration, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createCalendarTimer(final ScheduleExpression schedule) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Timer createCalendarTimer(final ScheduleExpression schedule, final TimerConfig timerConfig) throws IllegalArgumentException, IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<Timer> getTimers() throws IllegalStateException, EJBException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
            return null;
        }
    }

    /**
     * @author Kim Jensen
     * @since CWS 1.2
     */
    private static class TestEntityManager implements EntityManager {

        /**
         * {@inheritDoc}
         */
        @Override
        public void persist(final Object entity) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T merge(final T entity) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove(final Object entity) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T find(final Class<T> entityClass, final Object primaryKey) {
            throw new PersistenceException("Problem with the database.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T find(final Class<T> entityClass, final Object primaryKey, final Map<String, Object> properties) {
            throw new PersistenceException("Problem with the database.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockMode) {
            throw new PersistenceException("Problem with the database.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T find(final Class<T> entityClass, final Object primaryKey, final LockModeType lockMode, final Map<String, Object> properties) {
            throw new PersistenceException("Problem with the database.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T getReference(final Class<T> entityClass, final Object primaryKey) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFlushMode(final FlushModeType flushMode) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FlushModeType getFlushMode() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void lock(final Object entity, final LockModeType lockMode) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void lock(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void refresh(final Object entity) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void refresh(final Object entity, final Map<String, Object> properties) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void refresh(final Object entity, final LockModeType lockMode) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void refresh(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void clear() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void detach(final Object entity) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(final Object entity) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LockModeType getLockMode(final Object entity) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setProperty(final String propertyName, final Object value) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, Object> getProperties() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createQuery(final String qlString) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> TypedQuery<T> createQuery(final CriteriaQuery<T> criteriaQuery) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createQuery(final CriteriaUpdate updateQuery) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createQuery(final CriteriaDelete deleteQuery) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> TypedQuery<T> createQuery(final String qlString, final Class<T> resultClass) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createNamedQuery(final String name) {
            throw new CWSException(ReturnCode.DATABASE_ERROR, "Error in the database.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> TypedQuery<T> createNamedQuery(final String name, final Class<T> resultClass) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createNativeQuery(final String sqlString) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createNativeQuery(final String sqlString, final Class resultClass) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query createNativeQuery(final String sqlString, final String resultSetMapping) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StoredProcedureQuery createNamedStoredProcedureQuery(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StoredProcedureQuery createStoredProcedureQuery(final String procedureName) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StoredProcedureQuery createStoredProcedureQuery(final String procedureName, final Class... resultClasses) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StoredProcedureQuery createStoredProcedureQuery(final String procedureName, final String... resultSetMappings) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void joinTransaction() {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isJoinedToTransaction() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T unwrap(final Class<T> cls) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getDelegate() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOpen() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EntityTransaction getTransaction() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EntityManagerFactory getEntityManagerFactory() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CriteriaBuilder getCriteriaBuilder() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Metamodel getMetamodel() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> EntityGraph<T> createEntityGraph(final Class<T> rootType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EntityGraph<?> createEntityGraph(final String graphName) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public EntityGraph<?> getEntityGraph(final String graphName) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> List<EntityGraph<? super T>> getEntityGraphs(final Class<T> entityClass) {
            return null;
        }
    }

    private void prepareInvalidData() {
        final ProcessDataService service = new ProcessDataService(settings, entityManager);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Valid Data1", 1048576)), new Date(1L));
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data1", 1048576)), new Date(2L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_1_ID, "Invalidated Data2", 524288)), new Date(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Valid Data2", 1048576)), new Date(3L));
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data3", 1048576)), new Date(4L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_1, CIRCLE_2_ID, "Invalidated Data4", 524288)), new Date(), SanityStatus.OK);
        timeWarpChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Valid Data3", 1048576)), new Date(5L));
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data5", 1048576)), new Date(6L), SanityStatus.OK);
        falsifyChecksum(service.perform(prepareAddDataRequest(MEMBER_4, CIRCLE_3_ID, "Invalidated Data6", 524288)), new Date(), SanityStatus.OK);
    }

    private void timeWarpChecksum(final ProcessDataResponse response, final Date sanityCheck) {
        // Now to the tricky part. We wish to test that the checksum is invalid,
        // and thus resulting in a correct error message. As the checksum is
        // controlled internally by CWS, it cannot be altered (rightfully) via
        // the API, hence we have to modify it directly in the database!
        final String jql = "select d from DataEntity d where d.metadata.externalId = :eid";
        final Query query = entityManager.createQuery(jql);
        query.setParameter("eid", response.getDataId());
        final DataEntity entity = (DataEntity) query.getSingleResult();
        entity.setSanityStatus(SanityStatus.OK);
        entity.setSanityChecked(sanityCheck);
        entityManager.persist(entity);
    }
}
