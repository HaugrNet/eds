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
package net.haugr.eds.core.socket;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * What CDI gives for an injection point, in the tests: the value, or nothing
 * if it is null.
 *
 * @param <T> The type of the value
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class Given<T> implements Instance<T> {

    private final T value;

    Given(final T value) {
        this.value = value;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new UnsatisfiedResolutionException("Nothing is given.");
        }
        return value;
    }

    @Override
    public Iterator<T> iterator() {
        return (value != null) ? List.of(value).iterator() : Collections.emptyIterator();
    }

    @Override
    public Instance<T> select(final Annotation... qualifiers) {
        return this;
    }

    @Override
    public <U extends T> Instance<U> select(final Class<U> subtype, final Annotation... qualifiers) {
        throw new UnsupportedOperationException("Not needed by the tests.");
    }

    @Override
    public <U extends T> Instance<U> select(final TypeLiteral<U> subtype, final Annotation... qualifiers) {
        throw new UnsupportedOperationException("Not needed by the tests.");
    }

    @Override
    public boolean isUnsatisfied() {
        return value == null;
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public void destroy(final T instance) {
        throw new UnsupportedOperationException("Not needed by the tests.");
    }

    @Override
    public Handle<T> getHandle() {
        throw new UnsupportedOperationException("Not needed by the tests.");
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        throw new UnsupportedOperationException("Not needed by the tests.");
    }
}
