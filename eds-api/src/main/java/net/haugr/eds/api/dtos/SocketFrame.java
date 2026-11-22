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
import java.util.ArrayList;
import java.util.List;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.SocketFrameType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>A frame of the WebSocket of the stone (FEATURES.md), in both directions.
 * The type decides which fields are used.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_TYPE,
        Constants.FIELD_REF,
        Constants.FIELD_TOKEN,
        Constants.FIELD_MEMBER_ID,
        Constants.FIELD_ACCOUNT_NAME,
        Constants.FIELD_CIRCLE_ID,
        Constants.FIELD_FEATURE,
        Constants.FIELD_KIND,
        Constants.FIELD_ENTRY_ID,
        Constants.FIELD_ABOUT,
        Constants.FIELD_NAME,
        Constants.FIELD_CONTENT,
        Constants.FIELD_CLOSED,
        Constants.FIELD_AFTER,
        Constants.FIELD_BEFORE,
        Constants.FIELD_OLDEST_FIRST,
        Constants.FIELD_LIMIT,
        Constants.FIELD_BARE,
        Constants.FIELD_SIGNAL,
        Constants.FIELD_TO,
        Constants.FIELD_UNSEEN,
        Constants.FIELD_PRESENT,
        Constants.FIELD_ENTRY,
        Constants.FIELD_ENTRIES,
        Constants.FIELD_COUNT,
        Constants.FIELD_FEATURES,
        Constants.FIELD_DATA_ID,
        Constants.FIELD_RETURN_CODE,
        Constants.FIELD_RETURN_MESSAGE })
@Schema(description = "A frame of the WebSocket (`/socket`), JSON in a text frame, in both directions. The `type` decides which fields are used; the others are omitted. The first frame must be AUTH.")
public final class SocketFrame implements Serializable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The type of the frame. */
    @Schema(description = "The type of the frame.")
    @JsonbProperty(value = Constants.FIELD_TYPE)
    @JsonbNillable
    private SocketFrameType type = null;

    /** The reference of the frame. */
    @Schema(description = "Chosen by the client, and returned in the answer to the frame.", maxLength = Constants.MAX_STRING_LENGTH, examples = { "17" })
    @JsonbProperty(value = Constants.FIELD_REF)
    @JsonbNillable
    private String ref = null;

    /** The Session token. */
    @Schema(description = "AUTH: the session token, Base64-encoded.")
    @JsonbProperty(value = Constants.FIELD_TOKEN)
    @JsonbNillable
    private String token = null;

    /** The ID of the Member. */
    @Schema(description = "READY: ID of the Member. SIGNAL from EDS: ID of the Member who sent it. PRESENT and ABSENT: ID of the Member who came or went.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_MEMBER_ID)
    @JsonbNillable
    private String memberId = null;

    /** The account name of the Member. */
    @Schema(description = "READY, SIGNAL from EDS, PRESENT and ABSENT: the account name of the Member.", maxLength = Constants.MAX_NAME_LENGTH, examples = { "alice" })
    @JsonbProperty(value = Constants.FIELD_ACCOUNT_NAME)
    @JsonbNillable
    private String accountName = null;

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

    /** The kind of entry. */
    @Schema(description = "ADD: the kind of the entry. LIST and COUNT: only entries of this kind.", maxLength = Constants.MAX_FEATURE_NAME_LENGTH, examples = { "event" })
    @JsonbProperty(value = Constants.FIELD_KIND)
    @JsonbNillable
    private String kind = null;

    /** The ID of the entry. */
    @Schema(description = "CHANGE, REMOVE, CLOSE and GET: ID of the entry.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_ENTRY_ID)
    @JsonbNillable
    private String entryId = null;

    /** The ID of the entry which another is about. */
    @Schema(description = "ADD: ID of the entry which the new one is about. LIST: only entries about this entry.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_ABOUT)
    @JsonbNillable
    private String about = null;

    /** The name of the entry. */
    @Schema(description = "ADD and CHANGE: the name of the entry. LIST: only entries whose name starts with this.", maxLength = Constants.MAX_NAME_LENGTH)
    @JsonbProperty(value = Constants.FIELD_NAME)
    @JsonbNillable
    private String name = null;

    /** The content of the entry, Base64-encoded. */
    @Schema(description = "ADD and CHANGE: the content of the entry, Base64-encoded.")
    @JsonbProperty(value = Constants.FIELD_CONTENT)
    @JsonbNillable
    private String content = null;

    /** Whether the entry is closed. */
    @Schema(description = "CLOSE: true closes the entry, false opens it again.")
    @JsonbProperty(value = Constants.FIELD_CLOSED)
    @JsonbNillable
    private Boolean closed = null;

    /** The position after which entries are wanted. */
    @Schema(description = "LIST and COUNT: only entries after this position.", examples = { "42" })
    @JsonbProperty(value = Constants.FIELD_AFTER)
    @JsonbNillable
    private Long after = null;

    /** The position before which entries are wanted. */
    @Schema(description = "LIST: only entries before this position.", examples = { "42" })
    @JsonbProperty(value = Constants.FIELD_BEFORE)
    @JsonbNillable
    private Long before = null;

    /** Whether the oldest entries come first. */
    @Schema(description = "LIST: true for the oldest first; the newest come first otherwise.")
    @JsonbProperty(value = Constants.FIELD_OLDEST_FIRST)
    @JsonbNillable
    private Boolean oldestFirst = null;

    /** The most entries to return. */
    @Schema(description = "LIST: the most entries to return, 1 to 200; 50 without it.", examples = { "50" })
    @JsonbProperty(value = Constants.FIELD_LIMIT)
    @JsonbNillable
    private Integer limit = null;

    /** Whether the content of the entries is left out. */
    @Schema(description = "LIST: true leaves the content of the entries out.")
    @JsonbProperty(value = Constants.FIELD_BARE)
    @JsonbNillable
    private Boolean bare = null;

    /** The signal. */
    @Schema(description = "SIGNAL: a short text for the others in the Circle, which EDS passes on and doesn't store.", maxLength = Constants.MAX_SIGNAL_LENGTH)
    @JsonbProperty(value = Constants.FIELD_SIGNAL)
    @JsonbNillable
    private String signal = null;

    /** The Member a signal is for. */
    @Schema(description = "SIGNAL from a client: ID of the one Member the signal is for; it is passed on to their connections which "
            + "joined the Circle, and to nobody else. It may be the sender, to reach their other connections. "
            + "Without it the signal is for everyone else who joined. SIGNAL from EDS: the same ID, when the signal was for one Member.",
            pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_TO)
    @JsonbNillable
    private String to = null;

    /** Whether the connection joins without being shown as present. */
    @Schema(description = "JOIN: true joins the Circle without the Member being shown as present by this connection. The connection is told who is present all the same.")
    @JsonbProperty(value = Constants.FIELD_UNSEEN)
    @JsonbNillable
    private Boolean unseen = null;

    /** The Members who are present in the Circle. */
    @Schema(description = "JOINED: the IDs of the Members who are present in the Circle: those with at least one connection which joined it and is shown.")
    @JsonbProperty(value = Constants.FIELD_PRESENT)
    @JsonbNillable
    private List<String> present = null;

    /** The entry. */
    @Schema(description = "ADDED, CHANGED and REMOVED: the entry. Its content is left out when it is long, and always for a removed entry.")
    @JsonbProperty(value = Constants.FIELD_ENTRY)
    @JsonbNillable
    private Entry entry = null;

    /** The entries. */
    @Schema(description = "DONE and ENTRIES: the entries.")
    @JsonbProperty(value = Constants.FIELD_ENTRIES)
    @JsonbNillable
    private List<Entry> entries = null;

    /** The number of entries. */
    @Schema(description = "COUNTED: the number of entries.", examples = { "3" })
    @JsonbProperty(value = Constants.FIELD_COUNT)
    @JsonbNillable
    private Long count = null;

    /** The features of the Circle. */
    @Schema(description = "JOINED: the features of the Circle with their state.")
    @JsonbProperty(value = Constants.FIELD_FEATURES)
    @JsonbNillable
    private List<CircleFeature> features = null;

    /** The ID of the Data Object. */
    @Schema(description = "FILES: ID of the Data Object which changed.", pattern = Constants.ID_PATTERN_REGEX, examples = { "5d2a0c7e-8c4f-4d7b-9a2e-3f1b6c0d9e8a" })
    @JsonbProperty(value = Constants.FIELD_DATA_ID)
    @JsonbNillable
    private String dataId = null;

    /** The return code. */
    @Schema(description = "ERROR and LEFT: the return code.", examples = { "404" })
    @JsonbProperty(value = Constants.FIELD_RETURN_CODE)
    @JsonbNillable
    private Integer returnCode = null;

    /** The return message. */
    @Schema(description = "ERROR and LEFT: the return message.")
    @JsonbProperty(value = Constants.FIELD_RETURN_MESSAGE)
    @JsonbNillable
    private String returnMessage = null;

    /**
     * Default Constructor.
     */
    public SocketFrame() {
        // Generating JavaDoc requires an explicit Constructor, SonarQube
        // requires explicit comment in empty methods, hence this comment
        // for the default, empty, constructor.
    }

    /**
     * Constructor with the type.
     *
     * @param type The type of the frame
     */
    public SocketFrame(final SocketFrameType type) {
        this.type = type;
    }

    /**
     * Sets the type of the frame.
     *
     * @param type The type of the frame
     */
    public void setType(final SocketFrameType type) {
        this.type = type;
    }

    /**
     * Retrieves the type of the frame.
     *
     * @return The type of the frame
     */
    public SocketFrameType getType() {
        return type;
    }

    /**
     * Sets the reference of the frame.
     *
     * @param ref The reference of the frame
     */
    public void setRef(final String ref) {
        this.ref = ref;
    }

    /**
     * Retrieves the reference of the frame.
     *
     * @return The reference of the frame
     */
    public String getRef() {
        return ref;
    }

    /**
     * Sets the Session token.
     *
     * @param token The Session token
     */
    public void setToken(final String token) {
        this.token = token;
    }

    /**
     * Retrieves the Session token.
     *
     * @return The Session token
     */
    public String getToken() {
        return token;
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
     * Sets the account name of the Member.
     *
     * @param accountName The account name of the Member
     */
    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    /**
     * Retrieves the account name of the Member.
     *
     * @return The account name of the Member
     */
    public String getAccountName() {
        return accountName;
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
     * Sets the kind of entry.
     *
     * @param kind The kind of entry
     */
    public void setKind(final String kind) {
        this.kind = kind;
    }

    /**
     * Retrieves the kind of entry.
     *
     * @return The kind of entry
     */
    public String getKind() {
        return kind;
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
     * Sets the ID of the entry which another is about.
     *
     * @param about The ID of the entry which another is about
     */
    public void setAbout(final String about) {
        this.about = about;
    }

    /**
     * Retrieves the ID of the entry which another is about.
     *
     * @return The ID of the entry which another is about
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

    /**
     * Sets the position after which entries are wanted.
     *
     * @param after The position after which entries are wanted
     */
    public void setAfter(final Long after) {
        this.after = after;
    }

    /**
     * Retrieves the position after which entries are wanted.
     *
     * @return The position after which entries are wanted
     */
    public Long getAfter() {
        return after;
    }

    /**
     * Sets the position before which entries are wanted.
     *
     * @param before The position before which entries are wanted
     */
    public void setBefore(final Long before) {
        this.before = before;
    }

    /**
     * Retrieves the position before which entries are wanted.
     *
     * @return The position before which entries are wanted
     */
    public Long getBefore() {
        return before;
    }

    /**
     * Sets whether the oldest entries come first.
     *
     * @param oldestFirst Whether the oldest entries come first
     */
    public void setOldestFirst(final Boolean oldestFirst) {
        this.oldestFirst = oldestFirst;
    }

    /**
     * Retrieves whether the oldest entries come first.
     *
     * @return Whether the oldest entries come first
     */
    public Boolean getOldestFirst() {
        return oldestFirst;
    }

    /**
     * Sets the most entries to return.
     *
     * @param limit The most entries to return
     */
    public void setLimit(final Integer limit) {
        this.limit = limit;
    }

    /**
     * Retrieves the most entries to return.
     *
     * @return The most entries to return
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * Sets whether the content of the entries is left out.
     *
     * @param bare Whether the content of the entries is left out
     */
    public void setBare(final Boolean bare) {
        this.bare = bare;
    }

    /**
     * Retrieves whether the content of the entries is left out.
     *
     * @return Whether the content of the entries is left out
     */
    public Boolean getBare() {
        return bare;
    }

    /**
     * Sets the signal.
     *
     * @param signal The signal
     */
    public void setSignal(final String signal) {
        this.signal = signal;
    }

    /**
     * Retrieves the signal.
     *
     * @return The signal
     */
    public String getSignal() {
        return signal;
    }

    /**
     * Sets the Member a signal is for.
     *
     * @param to The ID of the Member a signal is for
     */
    public void setTo(final String to) {
        this.to = to;
    }

    /**
     * Retrieves the Member a signal is for.
     *
     * @return The ID of the Member a signal is for
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets whether the connection joins without being shown as present.
     *
     * @param unseen Whether the connection joins without being shown
     */
    public void setUnseen(final Boolean unseen) {
        this.unseen = unseen;
    }

    /**
     * Retrieves whether the connection joins without being shown as present.
     *
     * @return Whether the connection joins without being shown
     */
    public Boolean getUnseen() {
        return unseen;
    }

    /**
     * Sets the Members who are present in the Circle.
     *
     * @param present The IDs of the Members who are present
     */
    public void setPresent(final List<String> present) {
        this.present = (present == null) ? null : new ArrayList<>(present);
    }

    /**
     * Retrieves the Members who are present in the Circle.
     *
     * @return The IDs of the Members who are present
     */
    public List<String> getPresent() {
        return (present == null) ? null : new ArrayList<>(present);
    }

    /**
     * Sets the entry.
     *
     * @param entry The entry
     */
    public void setEntry(final Entry entry) {
        this.entry = entry;
    }

    /**
     * Retrieves the entry.
     *
     * @return The entry
     */
    public Entry getEntry() {
        return entry;
    }

    /**
     * Sets the entries.
     *
     * @param entries The entries
     */
    public void setEntries(final List<Entry> entries) {
        this.entries = (entries == null) ? null : new ArrayList<>(entries);
    }

    /**
     * Retrieves the entries.
     *
     * @return The entries
     */
    public List<Entry> getEntries() {
        return (entries == null) ? null : new ArrayList<>(entries);
    }

    /**
     * Sets the number of entries.
     *
     * @param count The number of entries
     */
    public void setCount(final Long count) {
        this.count = count;
    }

    /**
     * Retrieves the number of entries.
     *
     * @return The number of entries
     */
    public Long getCount() {
        return count;
    }

    /**
     * Sets the features of the Circle.
     *
     * @param features The features of the Circle
     */
    public void setFeatures(final List<CircleFeature> features) {
        this.features = (features == null) ? null : new ArrayList<>(features);
    }

    /**
     * Retrieves the features of the Circle.
     *
     * @return The features of the Circle
     */
    public List<CircleFeature> getFeatures() {
        return (features == null) ? null : new ArrayList<>(features);
    }

    /**
     * Sets the ID of the Data Object.
     *
     * @param dataId The ID of the Data Object
     */
    public void setDataId(final String dataId) {
        this.dataId = dataId;
    }

    /**
     * Retrieves the ID of the Data Object.
     *
     * @return The ID of the Data Object
     */
    public String getDataId() {
        return dataId;
    }

    /**
     * Sets the return code.
     *
     * @param returnCode The return code
     */
    public void setReturnCode(final Integer returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * Retrieves the return code.
     *
     * @return The return code
     */
    public Integer getReturnCode() {
        return returnCode;
    }

    /**
     * Sets the return message.
     *
     * @param returnMessage The return message
     */
    public void setReturnMessage(final String returnMessage) {
        this.returnMessage = returnMessage;
    }

    /**
     * Retrieves the return message.
     *
     * @return The return message
     */
    public String getReturnMessage() {
        return returnMessage;
    }
}
