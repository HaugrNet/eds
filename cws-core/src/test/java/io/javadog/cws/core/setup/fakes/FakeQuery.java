/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2021, JavaDog.io
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
package io.javadog.cws.core.setup.fakes;

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
 * <p>This wrapper is used by the FakeEntityManager to alter expected Query
 * behavior to mimic expectations that otherwise cannot be tested unless
 * mocking is used - but mocking have other problems.</p>
 *
 * <p>By using the setHint feature, certain hits are used as part of running
 * the query, so it is possible to change behavior.</p>
 *
 * <p>Following hints have been added:</p>
 * <ul>
 *   <li><b>returnNull</b> - methods return null, instead of expected
 *   result.</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FakeQuery implements Query {

    public static final String NULLABLE = "returnNull";
    private final Query query;
    private boolean returnNull = false;

    public FakeQuery(final Query query) {
        this.query = query;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<?> getResultList() {
        return returnNull ? null : query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getSingleResult() {
        return returnNull ? null : query.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int executeUpdate() {
        return query.executeUpdate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setMaxResults(final int maxResult) {
        return query.setMaxResults(maxResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxResults() {
        return query.getMaxResults();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setFirstResult(final int startPosition) {
        return query.setFirstResult(startPosition);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFirstResult() {
        return query.getFirstResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setHint(final String hintName, final Object value) {
        if (NULLABLE.equals(hintName)) {
            returnNull = true;
        }
        return query.setHint(hintName, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getHints() {
        return query.getHints();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Query setParameter(final Parameter<T> param, final T value) {
        return query.setParameter(param, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final Parameter<Calendar> param, final Calendar value, final TemporalType temporalType) {
        return query.setParameter(param, value, temporalType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final Parameter<Date> param, final Date value, final TemporalType temporalType) {
        return query.setParameter(param, value, temporalType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final String name, final Object value) {
        return query.setParameter(name, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final String name, final Calendar value, final TemporalType temporalType) {
        return query.setParameter(name, value, temporalType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final String name, final Date value, final TemporalType temporalType) {
        return query.setParameter(name, value, temporalType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final int position, final Object value) {
        return query.setParameter(position, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final int position, final Calendar value, final TemporalType temporalType) {
        return query.setParameter(position, value, temporalType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final int position, final Date value, final TemporalType temporalType) {
        return query.setParameter(position, value, temporalType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Parameter<?>> getParameters() {
        return query.getParameters();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter<?> getParameter(final String name) {
        return query.getParameter(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Parameter<T> getParameter(final String name, final Class<T> type) {
        return query.getParameter(name, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter<?> getParameter(final int position) {
        return query.getParameter(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Parameter<T> getParameter(final int position, final Class<T> type) {
        return query.getParameter(position, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBound(final Parameter<?> param) {
        return query.isBound(param);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getParameterValue(final Parameter<T> param) {
        return query.getParameterValue(param);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParameterValue(final String name) {
        return query.getParameterValue(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParameterValue(final int position) {
        return query.getParameterValue(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setFlushMode(final FlushModeType flushMode) {
        return query.setFlushMode(flushMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlushModeType getFlushMode() {
        return query.getFlushMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setLockMode(final LockModeType lockMode) {
        return query.setLockMode(lockMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LockModeType getLockMode() {
        return query.getLockMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> cls) {
        return query.unwrap(cls);
    }
}
