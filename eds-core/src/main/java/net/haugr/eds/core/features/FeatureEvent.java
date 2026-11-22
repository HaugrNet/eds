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
package net.haugr.eds.core.features;

import net.haugr.eds.api.dtos.Entry;

/**
 * <p>Something that happened in a feature of a Circle, which the others in the
 * Circle are told about (FEATURES.md, the socket).</p>
 *
 * @param change   What happened
 * @param circleId ID of the Circle
 * @param feature     Name of the feature
 * @param entry    The entry it happened to; null for a change of the feature itself
 * @author Kim Jensen
 * @since EDS 2.0
 */
public record FeatureEvent(Change change, String circleId, String feature, Entry entry) {

    /** What happened. */
    public enum Change {
        /** An entry was added. */
        ADDED,
        /** An entry was changed, closed or opened again. */
        CHANGED,
        /** An entry was removed. */
        REMOVED,
        /** The feature was switched on or off, set read-only, or removed from the Circle. */
        FEATURE
    }
}
