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
package io.javadog.cws.core.stubs;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This test Stub was extracted from the CommonDaoTest.
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FakeQuery implements Query {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<?> getResultList() {
        return Collections.emptyList();
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
        return this;
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
        return this;
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
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getHints() {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Query setParameter(final Parameter<T> param, final T value) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final Parameter<Calendar> param, final Calendar value, final TemporalType temporalType) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final Parameter<Date> param, final Date value, final TemporalType temporalType) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final String name, final Object value) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final String name, final Calendar value, final TemporalType temporalType) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final String name, final Date value, final TemporalType temporalType) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final int position, final Object value) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final int position, final Calendar value, final TemporalType temporalType) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query setParameter(final int position, final Date value, final TemporalType temporalType) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Parameter<?>> getParameters() {
        return Collections.emptySet();
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
        return this;
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
        return this;
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