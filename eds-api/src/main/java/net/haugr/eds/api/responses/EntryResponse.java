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

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Entry;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>Entries of a feature in a Circle, or their number (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_ENTRIES,
        Constants.FIELD_COUNT })
@Schema(description = "Entries of a feature in a Circle: the entry added, changed or asked for, a page of a listing, or a count.")
public final class EntryResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The entries. */
    @Schema(description = "The entries.")
    @JsonbProperty(value = Constants.FIELD_ENTRIES)
    @JsonbNillable
    private List<Entry> entries = new ArrayList<>(0);

    /** The number of entries. */
    @Schema(description = "When counting: the number of entries; otherwise null.", examples = { "3" })
    @JsonbProperty(value = Constants.FIELD_COUNT)
    @JsonbNillable
    private Long count = null;

    /**
     * Empty Constructor, required for WebServices.
     */
    public EntryResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for a successful request, with a message.
     *
     * @param returnMessage The EDS Return Message
     */
    public EntryResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public EntryResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
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
        return (entries == null) ? null : Collections.unmodifiableList(entries);
    }

    /**
     * Adds an entry.
     *
     * @param entry The entry to add
     */
    public void addEntry(final Entry entry) {
        entries.add(entry);
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
}
