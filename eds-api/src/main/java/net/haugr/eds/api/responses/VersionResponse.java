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

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;

/**
 * <p>Returns the current version of the EDS instance running.</p>
 *
 * <p>Please see {@link EDSResponse} for information about the result of the
 * processing.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
@JsonbPropertyOrder({Constants.FIELD_VERSION, Constants.FIELD_DATABASE, Constants.FIELD_PERSISTENT, Constants.FIELD_STORAGE_CEILING})
@Schema(description = "The EDS version, and what kind of system this is.")
public final class VersionResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The EDS Version. */
    @Schema(description = "The version of the running EDS.", examples = { "2.0-SNAPSHOT" })
    @JsonbProperty(Constants.FIELD_VERSION)
    private String version = null;

    /** The product name of the database behind the EDS instance. */
    @Schema(description = "The product name of the database EDS is running on, as reported by the JDBC driver. "
            + "Absent if the database has not been reached since EDS started.",
            examples = { "PostgreSQL" })
    @JsonbProperty(Constants.FIELD_DATABASE)
    private String database = null;

    /** Whether the data survives a restart of EDS. */
    @Schema(description = "False if this is a test system on an in-memory database (H2), where all data is lost when EDS stops; "
            + "true if the database is persistent, which makes this a production-grade system. Absent if the database has not been reached since EDS started.",
            examples = { "true" })
    @JsonbProperty(Constants.FIELD_PERSISTENT)
    private Boolean persistent = null;

    /** The storage ceiling. */
    @Schema(description = "The ceiling for all stored content together, in bytes of encrypted content: eds.data.max.total.size if it is positive, "
            + "otherwise 2 GiB on a test system and 250 GiB on a production system. Absent if the database has not been reached since EDS started.",
            examples = { "268435456000" })
    @JsonbProperty(Constants.FIELD_STORAGE_CEILING)
    private Long storageCeiling = null;

    // =========================================================================
    // Object Constructors
    // =========================================================================

    /**
     * Empty Constructor, to use if the setters are invoked. This is required
     * for WebServices to work properly.
     */
    public VersionResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Error Constructor, used if an error occurred, and the request could not
     * complete successfully.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public VersionResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    // =========================================================================
    // Standard Setters & Getters
    // =========================================================================

    /**
     * Set the EDS Version.
     *
     * @param version EDS Version
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Retrieves the EDS Version.
     *
     * @return EDS Version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the product name of the database.
     *
     * @param database Database product name
     */
    public void setDatabase(final String database) {
        this.database = database;
    }

    /**
     * Retrieves the product name of the database, e.g. "H2" or "PostgreSQL".
     *
     * @return Database product name, or null if not known yet
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Set whether the data survives a restart of EDS.
     *
     * @param persistent True for a persistent database
     */
    public void setPersistent(final Boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Retrieves whether the data survives a restart of EDS. False means a
     * test system, running on an in-memory database.
     *
     * @return True if persistent, false if not, null if not known yet
     */
    public Boolean getPersistent() {
        return persistent;
    }

    /**
     * Set the storage ceiling.
     *
     * @param storageCeiling Ceiling in bytes of encrypted content
     */
    public void setStorageCeiling(final Long storageCeiling) {
        this.storageCeiling = storageCeiling;
    }

    /**
     * Retrieves the ceiling for all stored content together, in bytes of
     * encrypted content: {@code eds.data.max.total.size} if it is positive,
     * otherwise the default for the kind of system.
     *
     * @return Ceiling in bytes, or null if the database has not been reached
     *         since EDS started
     */
    public Long getStorageCeiling() {
        return storageCeiling;
    }
}
