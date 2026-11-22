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
package net.haugr.eds.api.common;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The state of a feature in a Circle (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Schema(description = "The state of a feature in a Circle. Values: ON (in use), READONLY (its entries can be read, nothing is written: "
        + "an archive), OFF (its entries are kept and hidden: nothing is read, written or announced).")
public enum FeatureState {

    /** The feature is in use. */
    ON,

    /** The entries of the feature can be read, nothing is written. */
    READONLY,

    /** The entries of the feature are kept and hidden. */
    OFF
}
