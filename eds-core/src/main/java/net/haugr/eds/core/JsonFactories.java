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
package net.haugr.eds.core;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonReaderFactory;
import jakarta.json.spi.JsonProvider;
import java.util.Map;

/**
 * <p>The factories of JSON objects, arrays and readers, made once (ticket
 * 35). {@code Json.createObjectBuilder()} and the like look the provider up
 * again at each call: a {@code ServiceLoader}, which reads the stone's JAR,
 * for every socket frame and every declaration of a feature. The factories
 * may be used by many threads at once (Jakarta JSON Processing).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class JsonFactories {

    private static final JsonProvider PROVIDER = JsonProvider.provider();

    /** Makes the builders of JSON objects and arrays. */
    public static final JsonBuilderFactory BUILDERS = PROVIDER.createBuilderFactory(Map.of());

    /** Makes the readers of JSON text. */
    public static final JsonReaderFactory READERS = PROVIDER.createReaderFactory(Map.of());

    private JsonFactories() {
        // Only the factories.
    }
}
