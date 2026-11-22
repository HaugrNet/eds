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

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.time.LocalDateTime;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>A download token for the content of one Data Object (PLAN.md, L5),
 * for clients that can't send an Authorization header, like the media
 * elements of a browser.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({ Constants.FIELD_TOKEN, Constants.FIELD_EXPIRES })
@Schema(description = "A download token for the content of one Data Object.")
public final class DownloadTokenResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    @Schema(description = "The token, URL-safe; append it as ?token= to GET or HEAD /data/{dataId}/content of this Data Object. "
            + "It reads only this content, and ends at its expiry, when the Session ends, or when the MasterKey changes.",
            examples = { "q3V0b2dlbmVyYXRlZC10b2tlbg" })
    @JsonbProperty(Constants.FIELD_TOKEN)
    private String token = null;

    @Schema(description = "When the token expires: after the configured lifetime, at most when the Session expires. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-07T15:30:00" })
    @JsonbProperty(Constants.FIELD_EXPIRES)
    @JsonbDateFormat
    // A false positive: LocalDateTime is written by value (a serialization proxy), so its
    // identity is never relied on; transient would drop the field from JSON-B and OpenAPI.
    @SuppressWarnings("java:S3437")
    private LocalDateTime expires = null;

    /**
     * Empty Constructor, required for WebServices.
     */
    public DownloadTokenResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public DownloadTokenResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setExpires(final LocalDateTime expires) {
        this.expires = expires;
    }

    public LocalDateTime getExpires() {
        return expires;
    }
}
