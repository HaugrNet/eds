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

import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.parsson.JsonProviderImpl;

/**
 * The JSON provider of the tests, as Parsson's, which counts how often it is
 * made: {@code JsonProvider.provider()} makes one at each call, when the
 * system property {@code jakarta.json.provider} names it (ticket 35).
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class CountingJsonProvider extends JsonProviderImpl {

    static final AtomicInteger MADE = new AtomicInteger();

    /** Made by {@code JsonProvider.provider()}. */
    public CountingJsonProvider() {
        MADE.incrementAndGet();
    }
}
