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

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import net.haugr.eds.api.common.ByteArrayAdapter;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.Utilities;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import java.io.Serial;

/**
 * <p>If the request was invoked with the CREATE action, then the response will
 * contain the newly generated Member Id. If it was invoked with the INVITE
 * action, then it will respond with the invitation token - which the to-be
 * Member can invoke the update request with, having the credential type set
 * to INVITATION.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({ Constants.FIELD_MEMBER_ID, Constants.FIELD_INVITATION, Constants.FIELD_SESSION })
@Schema(description = "Result of processing a Member.")
public final class ProcessMemberResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The MemberId. */
    @Schema(description = "ID of the Member; for a new Member, its new ID.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(Constants.FIELD_MEMBER_ID)
    private String memberId = null;

    /** The Invitation token. */
    @Schema(description = "The invitation token, Base64-encoded, when inviting a Member. It is only returned here; "
            + "EDS stores just a keyed hash of it. The invited Member sends it as credential with credential type INVITATION.",
            type = SchemaType.STRING, format = "byte")
    @JsonbProperty(Constants.FIELD_INVITATION)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] invitation = null;

    /** The Session token. */
    @Schema(description = "The session token, Base64-encoded, after a login. It is only returned here; "
            + "EDS stores just a keyed hash of it. Send it as credential with credential type SESSION.",
            type = SchemaType.STRING, format = "byte")
    @JsonbProperty(Constants.FIELD_SESSION)
    @JsonbTypeAdapter(ByteArrayAdapter.class)
    private byte[] session = null;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public ProcessMemberResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for more detailed responses.
     *
     * @param returnMessage The EDS Return Message
     */
    public ProcessMemberResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public ProcessMemberResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

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

    /**
     * Set the Invitation token.
     *
     * @param invitation Invitation token
     */
    public void setInvitation(final byte[] invitation) {
        this.invitation = Utilities.copy(invitation);
    }

    /**
     * Retrieves the Invitation token.
     *
     * @return Invitation token
     */
    public byte[] getInvitation() {
        return Utilities.copy(invitation);
    }

    /**
     * Set the Session token.
     *
     * @param session Session token
     */
    public void setSession(final byte[] session) {
        this.session = Utilities.copy(session);
    }

    /**
     * Retrieves the Session token.
     *
     * @return Session token
     */
    public byte[] getSession() {
        return Utilities.copy(session);
    }
}
