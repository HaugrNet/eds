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
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.dtos.Member;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>The result will depend on the request parameters, if no information was
 * set in the request, then the list of Circles will be filled with the Circles,
 * the requesting member is allowed to see. If a MemberId was given, then the
 * list of Circles which both the requesting member and the requested member
 * have in common will be returned.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_MEMBERS, Constants.FIELD_CIRCLES })
public final class FetchMemberResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @JsonbProperty(Constants.FIELD_MEMBERS)
    private final List<Member> members = new ArrayList<>(0);

    @JsonbProperty(Constants.FIELD_CIRCLES)
    private final List<Circle> circles = new ArrayList<>(0);

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public FetchMemberResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public FetchMemberResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Setters & Getters
    // =========================================================================

    public void setMembers(final List<Member> members) {
        this.members.addAll(members);
    }

    public List<Member> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setCircles(final List<Circle> circles) {
        this.circles.addAll(circles);
    }

    public List<Circle> getCircles() {
        return Collections.unmodifiableList(circles);
    }
}
