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
package net.haugr.eds.spring;

import java.util.Locale;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * @author Kim Jensen
 * @since EDS 2.0
 */
public interface Logged {

    default <T> T logRequest(final Logger logger, final String endpoint, final Supplier<T> action) {
        final long start = System.nanoTime();

        try {
            final T result = action.get();
            final String duration = duration(start);
            logger.info("{} completed in {} ms", endpoint, duration);

            return result;
        } catch (RuntimeException e) {
            final String duration = duration(start);
            logger.error("{} failed in {} ms: {}", endpoint, duration, e.getMessage());

            throw e;
        }
    }

    default String duration(final long start) {
        return String.format(Locale.getDefault(), "%.2f", (System.nanoTime() - start) / 1_000_000.0);
    }
}
