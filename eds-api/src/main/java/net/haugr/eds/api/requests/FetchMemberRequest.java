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
 * <p>The request will return a list with all the Member's from the system, or
 * simply details about a single Member, if the MemberId is set.</p>
 *
 * <p>For more details, please see the 'fetchMembers' request in the Management
 * interface: {@link Management#fetchMembers(FetchMemberRequest)}</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder(Constants.FIELD_MEMBER_ID)
public final class FetchMemberRequest extends Authentication {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** MemberId. */
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    // =========================================================================
    // Standard Methods (Constructor, Setters & Getters)
    // =========================================================================

    /**
     * Default Constructor.
     */
    public FetchMemberRequest() {
        // Empty Constructor
    }

    /**
     * Set the MemberId.
     *
     * @param memberId MemberId
     */
    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    /**
     * Retrieves the MemberId.
     *
     * @return MemberId
     */
    public String getMemberId() {
        return memberId;
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

        checkValidId(errors, Constants.FIELD_MEMBER_ID, memberId, "The Member Id is invalid.");

        return errors;
    }
}
