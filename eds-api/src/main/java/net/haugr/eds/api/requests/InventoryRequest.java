/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import net.haugr.eds.api.Management;
import net.haugr.eds.api.common.Constants;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.Map;

/**
 * <p>Object is used to retrieve a list of Metadata from the EDS database.</p>
 *
 * <p>For more details, please see the 'inventory' request in the Management
 * interface: {@link Management#inventory(InventoryRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.2
 */
@JsonbPropertyOrder({ Constants.FIELD_PAGE_NUMBER, Constants.FIELD_PAGE_SIZE })
public final class InventoryRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The Page Number. */
    @JsonbProperty(value = Constants.FIELD_PAGE_NUMBER)
    @JsonbNillable
    private Integer pageNumber = 1;

    /** The Page Size. */
    @JsonbProperty(value = Constants.FIELD_PAGE_SIZE)
    @JsonbNillable
    private Integer pageSize = Constants.MAX_PAGE_SIZE;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public InventoryRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Set the Page Number.
     *
     * @param pageNumber Page Number
     */
    public void setPageNumber(final int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * Retrieves the Page Number.
     *
     * @return Page Number
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Set the Page Size.
     *
     * @param pageSize Page Size
     */
    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Retrieves the Page Size.
     *
     * @return Page Size
     */
    public Integer getPageSize() {
        return pageSize;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = super.validate();

        checkIntegerWithMax(errors, Constants.FIELD_PAGE_NUMBER, pageNumber, Integer.MAX_VALUE, "The Page Number must be a positive number, starting with 1.");
        checkIntegerWithMax(errors, Constants.FIELD_PAGE_SIZE, pageSize, Constants.MAX_PAGE_SIZE, "The Page Size must be a positive number, starting with 1.");

        return errors;
    }
}
