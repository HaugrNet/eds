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

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.core.exceptions.CWSException;
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
import java.util.List;
import java.util.Map;

/**
 * This test Stub was extracted from the SanitizerBeanTest.
 *
 * @author Kim Jensen
 * @since CWS 1.2
 */
public final class FakeEntityManager implements EntityManager {

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
        return new FakeCriteriaBuilder();
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
