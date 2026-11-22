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
package net.haugr.eds.api.requests;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The body of a request which adds or changes an entry of a feature, or
 * closes it (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_KIND,
        Constants.FIELD_ABOUT,
        Constants.FIELD_NAME,
        Constants.FIELD_CONTENT,
        Constants.FIELD_CLOSED })
@Schema(description = "An entry to add or change. Which fields are used depends on the endpoint.")
public final class EntryRequest implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The kind of the entry. */
    @Schema(description = "When adding: the kind of the entry.", maxLength = Constants.MAX_FEATURE_NAME_LENGTH, examples = { "event" })
    @JsonbProperty(value = Constants.FIELD_KIND)
    @JsonbNillable
    private String kind = null;

    /** The ID of the entry which this one is about. */
    @Schema(description = "When adding: ID of the entry which the new one is about, for kinds which are about another.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_ABOUT)
    @JsonbNillable
    private String about = null;

    /** The name of the entry. */
    @Schema(description = "The name of the entry, unique in the feature of the Circle; without one, EDS makes one. When changing, null keeps the name.",
            maxLength = Constants.MAX_NAME_LENGTH, examples = { "2026-11/dinner-3f9a" })
    @JsonbProperty(value = Constants.FIELD_NAME)
    @JsonbNillable
    private String name = null;

    /** The content of the entry, Base64-encoded. */
    @Schema(description = "The content, Base64-encoded, up to the size the kind allows. When changing, null keeps the content.")
    @JsonbProperty(value = Constants.FIELD_CONTENT)
    @JsonbNillable
    private String content = null;

    /** Whether the entry is closed. */
    @Schema(description = "When closing: true closes the entry, false opens it again.")
    @JsonbProperty(value = Constants.FIELD_CLOSED)
    @JsonbNillable
    private Boolean closed = null;

    /**
     * Default Constructor.
     */
    public EntryRequest() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the kind of the entry.
     *
     * @param kind The kind of the entry
     */
    public void setKind(final String kind) {
        this.kind = kind;
    }

    /**
     * Retrieves the kind of the entry.
     *
     * @return The kind of the entry
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the ID of the entry which this one is about.
     *
     * @param about The ID of the entry which this one is about
     */
    public void setAbout(final String about) {
        this.about = about;
    }

    /**
     * Retrieves the ID of the entry which this one is about.
     *
     * @return The ID of the entry which this one is about
     */
    public String getAbout() {
        return about;
    }

    /**
     * Sets the name of the entry.
     *
     * @param name The name of the entry
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the entry.
     *
     * @return The name of the entry
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the content of the entry, Base64-encoded.
     *
     * @param content The content of the entry, Base64-encoded
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * Retrieves the content of the entry, Base64-encoded.
     *
     * @return The content of the entry, Base64-encoded
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets whether the entry is closed.
     *
     * @param closed Whether the entry is closed
     */
    public void setClosed(final Boolean closed) {
        this.closed = closed;
    }

    /**
     * Retrieves whether the entry is closed.
     *
     * @return Whether the entry is closed
     */
    public Boolean getClosed() {
        return closed;
    }
}
