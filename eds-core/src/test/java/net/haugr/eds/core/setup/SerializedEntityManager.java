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
package net.haugr.eds.core.setup;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Set;

/**
 * <p>An Entity Manager which several threads can share: each call, also on
 * the queries and the transaction it gives, holds the same lock. What
 * {@code unwrap()} gives, e.g. for JDBC work, is not serialized.</p>
 *
 * <p>For tests where code on threads of its own, such as the socket hub,
 * must see the uncommitted changes of the test's transaction, and so must
 * use its Entity Manager. In the stone, each such thread has an Entity
 * Manager of its own.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class SerializedEntityManager {

    // What the Entity Manager gives, which keeps using it. Other results,
    // such as the criteria of a query or what unwrap() gives, are the
    // provider's own objects, which it casts to its own classes.
    private static final Set<Class<?>> SERIALIZED = Set.of(
            Query.class, TypedQuery.class, StoredProcedureQuery.class, EntityTransaction.class);

    private SerializedEntityManager() {
        // Only the factory method.
    }

    public static EntityManager of(final EntityManager entityManager) {
        return serialized(EntityManager.class, entityManager, new Object());
    }

    private static <T> T serialized(final Class<T> type, final T target, final Object lock) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, arguments) -> {
            final Object result;
            synchronized (lock) {
                try {
                    result = method.invoke(target, arguments);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }

            // A query returns itself to chain calls; it stays serialized.
            // Only the identity tells that it returned itself.
            final boolean itself = result == target;
            if (itself && (target instanceof Query)) {
                return proxy;
            }
            final Class<?> returned = method.getReturnType();
            if ((result != null) && SERIALIZED.contains(returned)) {
                return serializedAs(returned, result, lock);
            }

            return result;
        }));
    }

    private static <T> T serializedAs(final Class<T> type, final Object target, final Object lock) {
        return serialized(type, type.cast(target), lock);
    }
}
