/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import io.javadog.cws.core.DatabaseSetup;
import io.javadog.cws.core.exceptions.CWSException;
import org.junit.Test;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class CommonDaoTest extends DatabaseSetup {

    @Test
    public void testInvalidDataRead() {
        thrown.expect(CWSException.class);
        thrown.expectMessage("org.hibernate.exception.SQLGrammarException: could not prepare statement");

        // Creating a Query, which will attempt to read data from a record
        // which doesn't exist.
        final Query query = entityManager.createNativeQuery("select * from versions");
        CommonDao.findList(query);
    }

    @Test
    public void testNullResultList() {
        final Query query = new FakeQuery();
        final List<Object> found = CommonDao.findList(query);

        assertThat(found, is(not(nullValue())));
        assertThat(found.isEmpty(), is(true));
    }

    private static class FakeQuery implements Query {

        /**
         * {@inheritDoc}
         */
        @Override
        public List getResultList() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getSingleResult() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int executeUpdate() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setMaxResults(final int maxResult) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getMaxResults() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setFirstResult(final int startPosition) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getFirstResult() {
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setHint(final String hintName, final Object value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, Object> getHints() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Query setParameter(final Parameter<T> param, final T value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final Parameter<Calendar> param, final Calendar value, final TemporalType temporalType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final Parameter<Date> param, final Date value, final TemporalType temporalType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final String name, final Object value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final String name, final Calendar value, final TemporalType temporalType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final String name, final Date value, final TemporalType temporalType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final int position, final Object value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final int position, final Calendar value, final TemporalType temporalType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setParameter(final int position, final Date value, final TemporalType temporalType) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Parameter<?>> getParameters() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Parameter<?> getParameter(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Parameter<T> getParameter(final String name, final Class<T> type) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Parameter<?> getParameter(final int position) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Parameter<T> getParameter(final int position, final Class<T> type) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBound(final Parameter<?> param) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T getParameterValue(final Parameter<T> param) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getParameterValue(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getParameterValue(final int position) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Query setFlushMode(final FlushModeType flushMode) {
            return null;
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
        public Query setLockMode(final LockModeType lockMode) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LockModeType getLockMode() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T unwrap(final Class<T> cls) {
            return null;
        }
    }
}
