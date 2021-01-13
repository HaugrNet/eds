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

import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Kim Jensen
 * @since CWS 2.0
 */
public final class FakeCriteriaBuilder implements CriteriaBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public CriteriaQuery<Object> createQuery() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CriteriaQuery<T> createQuery(final Class<T> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CriteriaQuery<Tuple> createTupleQuery() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CriteriaUpdate<T> createCriteriaUpdate(final Class<T> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CriteriaDelete<T> createCriteriaDelete(final Class<T> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> CompoundSelection<Y> construct(final Class<Y> aClass, final Selection<?>... selections) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundSelection<Tuple> tuple(final Selection<?>... selections) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundSelection<Object[]> array(final Selection<?>... selections) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order asc(final Expression<?> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Order desc(final Expression<?> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<Double> avg(final Expression<N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> sum(final Expression<N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Long> sumAsLong(final Expression<Integer> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Double> sumAsDouble(final Expression<Float> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> max(final Expression<N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> min(final Expression<N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X extends Comparable<? super X>> Expression<X> greatest(final Expression<X> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X extends Comparable<? super X>> Expression<X> least(final Expression<X> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Long> count(final Expression<?> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Long> countDistinct(final Expression<?> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate exists(final Subquery<?> subQuery) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> all(final Subquery<Y> subQuery) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> some(final Subquery<Y> subQuery) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> any(final Subquery<Y> subQuery) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate and(final Expression<Boolean> expression, final Expression<Boolean> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate and(final Predicate... predicates) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate or(final Expression<Boolean> expression, final Expression<Boolean> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate or(final Predicate... predicates) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate not(final Expression<Boolean> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate conjunction() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate disjunction() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate isTrue(final Expression<Boolean> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate isFalse(final Expression<Boolean> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate isNull(final Expression<?> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate isNotNull(final Expression<?> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate equal(final Expression<?> expression, final Expression<?> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate equal(final Expression<?> expression, final Object o) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notEqual(final Expression<?> expression, final Expression<?> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notEqual(final Expression<?> expression, final Object o) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThan(final Expression<? extends Y> expression, final Expression<? extends Y> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThan(final Expression<? extends Y> expression, final Y y) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(final Expression<? extends Y> expression, final Expression<? extends Y> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate greaterThanOrEqualTo(final Expression<? extends Y> expression, final Y y) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThan(final Expression<? extends Y> expression, final Expression<? extends Y> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThan(final Expression<? extends Y> expression, final Y y) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(final Expression<? extends Y> expression, final Expression<? extends Y> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate lessThanOrEqualTo(final Expression<? extends Y> expression, final Y y) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate between(final Expression<? extends Y> expression, final Expression<? extends Y> expression1, final Expression<? extends Y> expression2) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y extends Comparable<? super Y>> Predicate between(final Expression<? extends Y> expression, final Y y, final Y y1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate gt(Expression<? extends Number> expression, Expression<? extends Number> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate gt(final Expression<? extends Number> expression, final Number number) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate ge(final Expression<? extends Number> expression, final Expression<? extends Number> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate ge(final Expression<? extends Number> expression, final Number number) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate lt(final Expression<? extends Number> expression, final Expression<? extends Number> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate lt(final Expression<? extends Number> expression, final Number number) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate le(final Expression<? extends Number> expression, final Expression<? extends Number> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate le(final Expression<? extends Number> expression, final Number number) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> neg(final Expression<N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> abs(final Expression<N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> sum(final Expression<? extends N> expression, final Expression<? extends N> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> sum(final Expression<? extends N> expression, final N n) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> sum(final N n, final Expression<? extends N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> prod(final Expression<? extends N> expression, final Expression<? extends N> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> prod(final Expression<? extends N> expression, final N n) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> prod(final N n, final Expression<? extends N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> diff(final Expression<? extends N> expression, final Expression<? extends N> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> diff(final Expression<? extends N> expression, final N n) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N extends Number> Expression<N> diff(final N n, final Expression<? extends N> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Number> quot(final Expression<? extends Number> expression, final Expression<? extends Number> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Number> quot(final Expression<? extends Number> expression, final Number number) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Number> quot(final Number number, final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> mod(final Expression<Integer> expression, final Expression<Integer> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> mod(final Expression<Integer> expression, final Integer integer) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> mod(final Integer integer, final Expression<Integer> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Double> sqrt(final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Long> toLong(final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> toInteger(Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Float> toFloat(final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Double> toDouble(final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<BigDecimal> toBigDecimal(final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<BigInteger> toBigInteger(final Expression<? extends Number> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> toString(final Expression<Character> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Expression<T> literal(final T t) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Expression<T> nullLiteral(final Class<T> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ParameterExpression<T> parameter(final Class<T> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ParameterExpression<T> parameter(final Class<T> aClass, final String s) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends Collection<?>> Predicate isEmpty(final Expression<C> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends Collection<?>> Predicate isNotEmpty(final Expression<C> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends Collection<?>> Expression<Integer> size(final Expression<C> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends Collection<?>> Expression<Integer> size(final C objects) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E, C extends Collection<E>> Predicate isMember(final Expression<E> expression, final Expression<C> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E, C extends Collection<E>> Predicate isMember(final E e, final Expression<C> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E, C extends Collection<E>> Predicate isNotMember(final Expression<E> expression, final Expression<C> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E, C extends Collection<E>> Predicate isNotMember(final E e, final Expression<C> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V, M extends Map<?, V>> Expression<Collection<V>> values(final M m) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <K, M extends Map<K, ?>> Expression<Set<K>> keys(final M m) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate like(final Expression<String> expression, final Expression<String> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate like(final Expression<String> expression, final String s) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate like(final Expression<String> expression, final Expression<String> expression1, final Expression<Character> expression2) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate like(final Expression<String> expression, final Expression<String> expression1, final char c) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate like(final Expression<String> expression, final String s, final Expression<Character> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate like(final Expression<String> expression, final String s, final char c) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notLike(final Expression<String> expression, final Expression<String> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notLike(final Expression<String> expression, final String s) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notLike(final Expression<String> expression, final Expression<String> expression1, final Expression<Character> expression2) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notLike(final Expression<String> expression, final Expression<String> expression1, final char c) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notLike(final Expression<String> expression, final String s, final Expression<Character> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Predicate notLike(final Expression<String> expression, final String s, final char c) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> concat(final Expression<String> expression, final Expression<String> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> concat(final Expression<String> expression, final String s) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> concat(final String s, final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> substring(final Expression<String> expression, final Expression<Integer> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> substring(final Expression<String> expression, final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> substring(final Expression<String> expression, final Expression<Integer> expression1, final Expression<Integer> expression2) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> substring(final Expression<String> expression, final int i, final int i1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> trim(final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> trim(final Trimspec trimspec, final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> trim(final Expression<Character> expression, final Expression<String> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> trim(final Trimspec trimspec, final Expression<Character> expression, final Expression<String> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> trim(final char c, final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> trim(final Trimspec trimspec, final char c, final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> lower(final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<String> upper(final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> length(final Expression<String> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> locate(final Expression<String> expression, final Expression<String> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> locate(final Expression<String> expression, final String s) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> locate(final Expression<String> expression, final Expression<String> expression1, final Expression<Integer> expression2) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Integer> locate(final Expression<String> expression, final String s, final int i) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Date> currentDate() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Timestamp> currentTimestamp() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression<Time> currentTime() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> In<T> in(final Expression<? extends T> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> coalesce(final Expression<? extends Y> expression, final Expression<? extends Y> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> coalesce(final Expression<? extends Y> expression, Y y) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> nullif(final Expression<Y> expression, final Expression<?> expression1) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <Y> Expression<Y> nullif(final Expression<Y> expression, Y y) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Coalesce<T> coalesce() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C, R> SimpleCase<C, R> selectCase(final Expression<? extends C> expression) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> Case<R> selectCase() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Expression<T> function(final String s, final Class<T> aClass, final Expression<?>... expressions) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, T, V extends T> Join<X, V> treat(final Join<X, T> join, final Class<V> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, T, E extends T> CollectionJoin<X, E> treat(final CollectionJoin<X, T> collectionJoin, final Class<E> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, T, E extends T> SetJoin<X, E> treat(final SetJoin<X, T> setJoin, final Class<E> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, T, E extends T> ListJoin<X, E> treat(final ListJoin<X, T> listJoin, final Class<E> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, K, T, V extends T> MapJoin<X, K, V> treat(final MapJoin<X, K, T> mapJoin, final Class<V> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, T extends X> Path<T> treat(final Path<X> path, final Class<T> aClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <X, T extends X> Root<T> treat(final Root<X> root, final Class<T> aClass) {
        return null;
    }
}
