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

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import net.haugr.eds.api.common.Constants;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>An entry of a feature in a Circle (FEATURES.md). EDS notes who wrote it and
 * when, and where it stands in the order of its feature; the content is the
 * clients' alone.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ENTRY_ID,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_FEATURE,
        Constants.FIELD_KIND,
        Constants.FIELD_POSITION,
        Constants.FIELD_ABOUT,
        Constants.FIELD_NAME,
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_ACCOUNT_NAME,
        Constants.FIELD_ADDED,
        Constants.FIELD_CHANGED,
        Constants.FIELD_CLOSED,
        Constants.FIELD_CONTENT })
@Schema(description = "An entry of a feature in a Circle. EDS notes its author, times and position; the content is the clients' alone.")
public final class Entry implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The ID of the entry. */
    @Schema(description = "ID of the entry. It is a Data Object too, so its content can be read with the content download.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_ENTRY_ID)
    @JsonbNillable
    private String entryId = null;

    /** The ID of the Circle. */
    @Schema(description = "ID of the Circle.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_CIRCLE_ID)
    @JsonbNillable
    private String circleId = null;

    /** The name of the feature. */
    @Schema(description = "The name of the feature.", maxLength = Constants.MAX_FEATURE_NAME_LENGTH, examples = { "calendar" })
    @JsonbProperty(value = Constants.FIELD_FEATURE)
    @JsonbNillable
    private String feature = null;

    /** The kind of the entry. */
    @Schema(description = "The kind of the entry, one of those the feature declares.", maxLength = Constants.MAX_FEATURE_NAME_LENGTH, examples = { "event" })
    @JsonbProperty(value = Constants.FIELD_KIND)
    @JsonbNillable
    private String kind = null;

    /** The position of the entry. */
    @Schema(description = "The place of the entry in the order of its feature in the Circle; "
            + "it grows with every entry added, and an entry which replaces an older one of its Member gets a new one.",
            examples = { "42" })
    @JsonbProperty(value = Constants.FIELD_POSITION)
    @JsonbNillable
    private Long position = null;

    /** The ID of the entry which this one is about. */
    @Schema(description = "ID of the entry which this one is about, for kinds which are about another; otherwise null.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_ABOUT)
    @JsonbNillable
    private String about = null;

    /** The name of the entry. */
    @Schema(description = "The name of the entry, given by the client or made by EDS; unique in the feature of the Circle.",
            maxLength = Constants.MAX_NAME_LENGTH, examples = { "2026-11/dinner-3f9a" })
    @JsonbProperty(value = Constants.FIELD_NAME)
    @JsonbNillable
    private String name = null;

    /** The ID of the author. */
    @Schema(description = "ID of the author, noted by EDS.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** The account name of the author. */
    @Schema(description = "The author's account name; null if the account is gone.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "alice" })
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

    /** The time the entry was added. */
    @Schema(description = "When the entry was added. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_ADDED)
    @JsonbDateFormat
    @JsonbNillable
    // A false positive: LocalDateTime is written by value (a serialization proxy), so its
    // identity is never relied on; transient would drop the field from JSON-B and OpenAPI.
    @SuppressWarnings("java:S3437")
    private LocalDateTime added = null;

    /** The time the entry was last changed. */
    @Schema(description = "When the entry was last changed; null if it never was. In UTC, as ISO-8601 without a time-zone suffix.",
            type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_CHANGED)
    @JsonbDateFormat
    @JsonbNillable
    // A false positive: LocalDateTime is written by value (a serialization proxy), so its
    // identity is never relied on; transient would drop the field from JSON-B and OpenAPI.
    @SuppressWarnings("java:S3437")
    private LocalDateTime changed = null;

    /** Whether the entry is closed. */
    @Schema(description = "True if the entry is closed: neither it nor what is about it can be changed or removed, and nothing new can be about it.")
    @JsonbProperty(value = Constants.FIELD_CLOSED)
    @JsonbNillable
    private Boolean closed = null;

    /** The content of the entry, Base64-encoded. */
    @Schema(description = "The content, Base64-encoded. In a listing it is left out when asked for.")
    @JsonbProperty(value = Constants.FIELD_CONTENT)
    @JsonbNillable
    private String content = null;

    /**
     * Default Constructor.
     */
    public Entry() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Sets the ID of the entry.
     *
     * @param entryId The ID of the entry
     */
    public void setEntryId(final String entryId) {
        this.entryId = entryId;
    }

    /**
     * Retrieves the ID of the entry.
     *
     * @return The ID of the entry
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Sets the ID of the Circle.
     *
     * @param circleId The ID of the Circle
     */
    public void setCircleId(final String circleId) {
        this.circleId = circleId;
    }

    /**
     * Retrieves the ID of the Circle.
     *
     * @return The ID of the Circle
     */
    public String getCircleId() {
        return circleId;
    }

    /**
     * Sets the name of the feature.
     *
     * @param feature The name of the feature
     */
    public void setFeature(final String feature) {
        this.feature = feature;
    }

    /**
     * Retrieves the name of the feature.
     *
     * @return The name of the feature
     */
    public String getFeature() {
        return feature;
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
     * Sets the position of the entry.
     *
     * @param position The position of the entry
     */
    public void setPosition(final Long position) {
        this.position = position;
    }

    /**
     * Retrieves the position of the entry.
     *
     * @return The position of the entry
     */
    public Long getPosition() {
        return position;
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
     * Sets the ID of the author.
     *
     * @param memberId The ID of the author
     */
    public void setMemberId(final String memberId) {
        this.memberId = memberId;
    }

    /**
     * Retrieves the ID of the author.
     *
     * @return The ID of the author
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * Sets the account name of the author.
     *
     * @param accountName The account name of the author
     */
    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    /**
     * Retrieves the account name of the author.
     *
     * @return The account name of the author
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Sets the time the entry was added.
     *
     * @param added The time the entry was added
     */
    public void setAdded(final LocalDateTime added) {
        this.added = added;
    }

    /**
     * Retrieves the time the entry was added.
     *
     * @return The time the entry was added
     */
    public LocalDateTime getAdded() {
        return added;
    }

    /**
     * Sets the time the entry was last changed.
     *
     * @param changed The time the entry was last changed
     */
    public void setChanged(final LocalDateTime changed) {
        this.changed = changed;
    }

    /**
     * Retrieves the time the entry was last changed.
     *
     * @return The time the entry was last changed
     */
    public LocalDateTime getChanged() {
        return changed;
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
}
