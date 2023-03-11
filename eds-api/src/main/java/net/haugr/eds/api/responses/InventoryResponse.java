/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2023, haugr.net
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
import net.haugr.eds.api.dtos.Metadata;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Response contains a list of the Metadata from the EDS database.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.2
 */
@JsonbPropertyOrder({ Constants.FIELD_INVENTORY, Constants.FIELD_RECORDS })
public class InventoryResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(Constants.FIELD_INVENTORY)
    private final List<Metadata> inventory = new ArrayList<>(0);

    @JsonbProperty(Constants.FIELD_RECORDS)
    private long records = 0;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public InventoryResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public InventoryResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setInventory(final List<Metadata> inventory) {
        this.inventory.addAll(inventory);
    }

    public List<Metadata> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public void setRecords(final long records) {
        this.records = records;
    }

    public long getRecords() {
        return records;
    }
}
