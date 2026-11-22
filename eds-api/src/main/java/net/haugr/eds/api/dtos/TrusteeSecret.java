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
package net.haugr.eds.api.dtos;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The secret of one Trustee, as part of a request which sets the secrets
 * of several Trustees of a Circle (E2E.md). EDS stores the secret without
 * looking inside.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_SECRET })
@Schema(description = "The secret of one Trustee of a Circle, for a request which sets several at once.")
public final class TrusteeSecret implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** MemberId. */
    @Schema(description = "ID of the Member, who must be a Trustee of the Circle.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** Secret. */
    @Schema(description = "The Trustee's secret: text which EDS stores without looking inside, e.g. "
            + "a key of the Circle wrapped for this Member by a client. An empty value removes it.",
            maxLength = Constants.MAX_SECRET_LENGTH)
    @JsonbProperty(value = Constants.FIELD_SECRET)
    @JsonbNillable
    private String secret = null;

    /**
     * Default Constructor.
     */
    public TrusteeSecret() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Constructor with both values.
     *
     * @param memberId ID of the Member
     * @param secret   The Trustee's secret
     */
    public TrusteeSecret(final String memberId, final String secret) {
        this.memberId = memberId;
        this.secret = secret;
    }

    /**
     * Sets the ID of the Member.
     *
     * @param memberId The ID of the Member
     */
    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    /**
     * Retrieves the ID of the Member.
     *
     * @return The ID of the Member
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * Sets the secret of the Trustee.
     *
     * @param secret The secret of the Trustee
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * Retrieves the secret of the Trustee.
     *
     * @return The secret of the Trustee
     */
    public String getSecret() {
        return secret;
    }
}
