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
package net.haugr.eds.core.setup.fakes;

import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.core.exceptions.EDSException;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.Map;

/**
 * This test Stub was extracted from the SanitizerBeanTest.
 *
 * @author Kim Jensen
 * @since EDS 1.2
 */
@SuppressWarnings("SqlSourceToSinkFlow")
public final class FakeEntityManager implements EntityManager {

    private static final String PERSISTENCE_NAME = "net.haugr.jpa";
    private static final EntityManagerFactory FACTORY = Persistence.createEntityManagerFactory(PERSISTENCE_NAME);
    private final EntityManager entityManager = FACTORY.createEntityManager();

    /**
     * {@inheritDoc}
     */
    @Override
    public void persist(final Object entity) {
        entityManager.persist(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T merge(final T entity) {
        return entityManager.merge(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final Object entity) {
        entityManager.remove(entity);
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
        return entityManager.getReference(entityClass, primaryKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() {
        entityManager.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFlushMode(final FlushModeType flushMode) {
        entityManager.setFlushMode(flushMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FlushModeType getFlushMode() {
        return entityManager.getFlushMode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lock(final Object entity, final LockModeType lockMode) {
        entityManager.lock(entity, lockMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lock(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
        entityManager.lock(entity, lockMode, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final Object entity) {
        entityManager.refresh(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final Object entity, final Map<String, Object> properties) {
        entityManager.refresh(entity, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final Object entity, final LockModeType lockMode) {
        entityManager.refresh(entity, lockMode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final Object entity, final LockModeType lockMode, final Map<String, Object> properties) {
        entityManager.refresh(entity, lockMode, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        entityManager.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void detach(final Object entity) {
        entityManager.detach(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final Object entity) {
        return entityManager.contains(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LockModeType getLockMode(final Object entity) {
        return entityManager.getLockMode(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProperty(final String propertyName, final Object value) {
        entityManager.setProperty(propertyName, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getProperties() {
        return entityManager.getProperties();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createQuery(final String qlString) {
        return new FakeQuery(entityManager.createQuery(qlString));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> TypedQuery<T> createQuery(final CriteriaQuery<T> criteriaQuery) {
        return entityManager.createQuery(criteriaQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createQuery(final CriteriaUpdate updateQuery) {
        return new FakeQuery(entityManager.createQuery(updateQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createQuery(final CriteriaDelete deleteQuery) {
        return new FakeQuery(entityManager.createQuery(deleteQuery));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> TypedQuery<T> createQuery(final String qlString, final Class<T> resultClass) {
        return entityManager.createQuery(qlString, resultClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createNamedQuery(final String name) {
        throw new EDSException(ReturnCode.DATABASE_ERROR, "Error in the database.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> TypedQuery<T> createNamedQuery(final String name, final Class<T> resultClass) {
        return entityManager.createNamedQuery(name, resultClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createNativeQuery(final String sqlString) {
        return entityManager.createNamedQuery(sqlString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createNativeQuery(final String sqlString, final Class resultClass) {
        return new FakeQuery(entityManager.createNativeQuery(sqlString, resultClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query createNativeQuery(final String sqlString, final String resultSetMapping) {
        return new FakeQuery(entityManager.createNativeQuery(sqlString, resultSetMapping));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(final String name) {
        return entityManager.createNamedStoredProcedureQuery(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(final String procedureName) {
        return entityManager.createStoredProcedureQuery(procedureName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(final String procedureName, final Class... resultClasses) {
        return entityManager.createStoredProcedureQuery(procedureName, resultClasses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredProcedureQuery createStoredProcedureQuery(final String procedureName, final String... resultSetMappings) {
        return entityManager.createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void joinTransaction() {
        entityManager.joinTransaction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isJoinedToTransaction() {
        return entityManager.isJoinedToTransaction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> cls) {
        return entityManager.unwrap(cls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDelegate() {
        return entityManager.getDelegate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        entityManager.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return entityManager.isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityTransaction getTransaction() {
        return entityManager.getTransaction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManager.getEntityManagerFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Metamodel getMetamodel() {
        return entityManager.getMetamodel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> EntityGraph<T> createEntityGraph(final Class<T> rootType) {
        return entityManager.createEntityGraph(rootType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityGraph<?> createEntityGraph(final String graphName) {
        return entityManager.createEntityGraph(graphName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityGraph<?> getEntityGraph(final String graphName) {
        return entityManager.getEntityGraph(graphName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(final Class<T> entityClass) {
        return entityManager.getEntityGraphs(entityClass);
    }
}
