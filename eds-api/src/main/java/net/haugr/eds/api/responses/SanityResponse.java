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
package net.haugr.eds.api.responses;

import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Sanity;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Returns a list of Data Objects, which have failed their sanity checks.
 * I.e., the bytes stored which may somehow have been corrupted over time, so
 * they no longer can be decrypted.</p>
 *
 * <p>If the CircleId is given in the request, then the resulting list is
 * limited to the records for the given Circle, the same applies to the timestamp,
 * since, which means that only newer record information is being retrieved.</p>
 *
 * <p>For any system, having <i>any</i> results from a request is considered a
 * disaster, as the underlying data storage cannot be trusted.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder(Constants.FIELD_SANITIES)
public final class SanityResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The List of failed Sanity Records. */
    @JsonbProperty(Constants.FIELD_SANITIES)
    private final List<Sanity> sanities = new ArrayList<>(0);

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public SanityResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public SanityResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the List of failed Sanity Records.
     *
     * @param sanities List of failed Sanity Records
     */
    public void setSanities(final List<Sanity> sanities) {
        this.sanities.addAll(sanities);
    }

    /**
     * Retrieves the List of failed Sanity Records.
     *
     * @return List of failed Sanity Records
     */
    public List<Sanity> getSanities() {
        return Collections.unmodifiableList(sanities);
    }
}
