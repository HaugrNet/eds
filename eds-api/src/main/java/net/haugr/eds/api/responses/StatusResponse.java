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
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbPropertyOrder;
import java.io.Serial;
import java.time.LocalDateTime;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.StatusSeries;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The status of a stone (STATUS.md): what it tells about itself to its
 * System Administrator and to the holders of a status key. Only general
 * numbers: no names of Members, Circles or files.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@JsonbPropertyOrder({
        Constants.FIELD_NAME,
        Constants.FIELD_VERSION,
        Constants.FIELD_AWAKE,
        Constants.FIELD_TIME,
        Constants.FIELD_STARTED,
        Constants.FIELD_DATABASE,
        Constants.FIELD_PERSISTENT,
        Constants.FIELD_DATABASE_MARK,
        Constants.FIELD_LOAD,
        Constants.FIELD_DAY,
        Constants.FIELD_INVENTORY })
@Schema(description = "The status of the stone: its load, how busy it is, and its inventory, what it holds. Only general numbers.")
public final class StatusResponse extends EDSResponse {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    @Serial
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    /** The name of the stone. */
    @Schema(description = "The name of the stone, as its System Administrator set it; null without one.", maxLength = Constants.MAX_STONE_NAME_LENGTH, examples = { "north" })
    @JsonbProperty(value = Constants.FIELD_NAME)
    @JsonbNillable
    private String name = null;

    /** The version of EDS. */
    @Schema(description = "The version of EDS.", examples = { "2.0" })
    @JsonbProperty(value = Constants.FIELD_VERSION)
    @JsonbNillable
    private String version = null;

    /** Whether the MasterKey is loaded. */
    @Schema(description = "True if the MasterKey is loaded; false if the stone is locked and waits for it.")
    @JsonbProperty(value = Constants.FIELD_AWAKE)
    @JsonbNillable
    private Boolean awake = null;

    /** The time of the stone. */
    @Schema(description = "The time of the stone. In UTC, as ISO-8601 without a time-zone suffix.", type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_TIME)
    @JsonbDateFormat
    @JsonbNillable
    // A false positive: LocalDateTime is written by value (a serialization proxy), so its
    // identity is never relied on; transient would drop the field from JSON-B and OpenAPI.
    @SuppressWarnings("java:S3437")
    private LocalDateTime time = null;

    /** The time the stone started. */
    @Schema(description = "When the stone started. In UTC, as ISO-8601 without a time-zone suffix.", type = SchemaType.STRING, format = "local-date-time", examples = { "2026-11-01T21:50:41.123456" })
    @JsonbProperty(value = Constants.FIELD_STARTED)
    @JsonbDateFormat
    @JsonbNillable
    // A false positive: LocalDateTime is written by value (a serialization proxy), so its
    // identity is never relied on; transient would drop the field from JSON-B and OpenAPI.
    @SuppressWarnings("java:S3437")
    private LocalDateTime started = null;

    /** The kind of database. */
    @Schema(description = "The kind of database, e.g. PostgreSQL or H2; null if it wasn't reached yet.", examples = { "PostgreSQL" })
    @JsonbProperty(value = Constants.FIELD_DATABASE)
    @JsonbNillable
    private String database = null;

    /** Whether the database keeps its data. */
    @Schema(description = "False for a test system, whose data is lost when the stone stops.")
    @JsonbProperty(value = Constants.FIELD_PERSISTENT)
    @JsonbNillable
    private Boolean persistent = null;

    /** The mark of the database server. */
    @Schema(description = "A mark for the database server: stones on the same server have the same mark, "
            + "and the address itself isn't given. Null for a database in memory.",
            examples = { "5f2a9c0e1b7d4a63" })
    @JsonbProperty(value = Constants.FIELD_DATABASE_MARK)
    @JsonbNillable
    private String databaseMark = null;

    /** The recent load. */
    @Schema(description = "The recent load, at the stone's load interval.")
    @JsonbProperty(value = Constants.FIELD_LOAD)
    @JsonbNillable
    private StatusSeries load = null;

    /** The load of the last day. */
    @Schema(description = "The load of the last 24 hours in steps of five minutes; only when asked for.")
    @JsonbProperty(value = Constants.FIELD_DAY)
    @JsonbNillable
    private StatusSeries day = null;

    /** The inventory. */
    @Schema(description = "The inventory: its latest sample, or all kept ones when asked for.")
    @JsonbProperty(value = Constants.FIELD_INVENTORY)
    @JsonbNillable
    private StatusSeries inventory = null;

    /**
     * Empty Constructor, required for WebServices.
     */
    public StatusResponse() {
        // Empty Constructor, required for WebServices
    }

    /**
     * Constructor for a successful request, with a message.
     *
     * @param returnMessage The EDS Return Message
     */
    public StatusResponse(final String returnMessage) {
        super(returnMessage);
    }

    /**
     * Error Constructor.
     *
     * @param returnCode    The EDS Return Code
     * @param returnMessage The EDS Return Message
     */
    public StatusResponse(final ReturnCode returnCode, final String returnMessage) {
        super(returnCode, returnMessage);
    }

    /**
     * Sets the name of the stone.
     *
     * @param name The name of the stone
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the stone.
     *
     * @return The name of the stone
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the version of EDS.
     *
     * @param version The version of EDS
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Retrieves the version of EDS.
     *
     * @return The version of EDS
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets whether the MasterKey is loaded.
     *
     * @param awake Whether the MasterKey is loaded
     */
    public void setAwake(final Boolean awake) {
        this.awake = awake;
    }

    /**
     * Retrieves whether the MasterKey is loaded.
     *
     * @return Whether the MasterKey is loaded
     */
    public Boolean getAwake() {
        return awake;
    }

    /**
     * Sets the time of the stone.
     *
     * @param time The time of the stone
     */
    public void setTime(final LocalDateTime time) {
        this.time = time;
    }

    /**
     * Retrieves the time of the stone.
     *
     * @return The time of the stone
     */
    public LocalDateTime getTime() {
        return time;
    }

    /**
     * Sets the time the stone started.
     *
     * @param started The time the stone started
     */
    public void setStarted(final LocalDateTime started) {
        this.started = started;
    }

    /**
     * Retrieves the time the stone started.
     *
     * @return The time the stone started
     */
    public LocalDateTime getStarted() {
        return started;
    }

    /**
     * Sets the kind of database.
     *
     * @param database The kind of database
     */
    public void setDatabase(final String database) {
        this.database = database;
    }

    /**
     * Retrieves the kind of database.
     *
     * @return The kind of database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets whether the database keeps its data.
     *
     * @param persistent Whether the database keeps its data
     */
    public void setPersistent(final Boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Retrieves whether the database keeps its data.
     *
     * @return Whether the database keeps its data
     */
    public Boolean getPersistent() {
        return persistent;
    }

    /**
     * Sets the mark of the database server.
     *
     * @param databaseMark The mark of the database server
     */
    public void setDatabaseMark(final String databaseMark) {
        this.databaseMark = databaseMark;
    }

    /**
     * Retrieves the mark of the database server.
     *
     * @return The mark of the database server
     */
    public String getDatabaseMark() {
        return databaseMark;
    }

    /**
     * Sets the recent load.
     *
     * @param load The recent load
     */
    public void setLoad(final StatusSeries load) {
        this.load = load;
    }

    /**
     * Retrieves the recent load.
     *
     * @return The recent load
     */
    public StatusSeries getLoad() {
        return load;
    }

    /**
     * Sets the load of the last day.
     *
     * @param day The load of the last day
     */
    public void setDay(final StatusSeries day) {
        this.day = day;
    }

    /**
     * Retrieves the load of the last day.
     *
     * @return The load of the last day
     */
    public StatusSeries getDay() {
        return day;
    }

    /**
     * Sets the inventory.
     *
     * @param inventory The inventory
     */
    public void setInventory(final StatusSeries inventory) {
        this.inventory = inventory;
    }

    /**
     * Retrieves the inventory.
     *
     * @return The inventory
     */
    public StatusSeries getInventory() {
        return inventory;
    }
}
