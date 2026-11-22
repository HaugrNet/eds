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
package net.haugr.eds.api.common;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * <p>The types of the frames of the WebSocket of the stone (FEATURES.md).</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Schema(description = "The type of a frame of the WebSocket (`/socket`). From clients: AUTH (the first frame, with the session token), JOIN and LEAVE (a Circle), "
        + "ADD, CHANGE, REMOVE and CLOSE (an entry of a feature of a joined Circle), GET (one entry), LIST (a page of entries), COUNT (the number of entries), "
        + "SIGNAL (a short text for the others in the Circle, or for one Member of it, which isn't stored). From EDS: READY (the answer to AUTH), "
        + "JOINED (with the features of the Circle and the Members present in it), "
        + "LEFT (also sent when the access to the Circle ends), DONE (the answer to ADD, CHANGE, REMOVE, CLOSE and GET, with the entries), "
        + "ENTRIES (the answer to LIST), COUNTED (the answer to COUNT), ADDED, CHANGED and REMOVED (an entry of a joined Circle was added, changed or removed, "
        + "by any connection or request; with the entry), FEATURE (a feature of a joined Circle was switched or removed), SIGNAL (a signal of another connection), "
        + "PRESENT and ABSENT (a Member came to a joined Circle with their first connection, or went with their last), "
        + "FILES (the files of a joined Circle changed; with the dataId) and ERROR.")
public enum SocketFrameType {

    /** The first frame, with the Session token. */
    AUTH,
    /** The answer to AUTH. */
    READY,
    /** Subscribes to a Circle. */
    JOIN,
    /** The answer to JOIN. */
    JOINED,
    /** Ends the subscription to a Circle. */
    LEAVE,
    /** The answer to LEAVE, and sent when the access to a Circle ends. */
    LEFT,
    /** Adds an entry. */
    ADD,
    /** Changes an entry. */
    CHANGE,
    /** Removes an entry. */
    REMOVE,
    /** Closes an entry, or opens it again. */
    CLOSE,
    /** Reads one entry. */
    GET,
    /** Reads a page of entries. */
    LIST,
    /** Counts entries. */
    COUNT,
    /** A short text for the others in the Circle, not stored. */
    SIGNAL,
    /** The answer to ADD, CHANGE, REMOVE, CLOSE and GET. */
    DONE,
    /** The answer to LIST. */
    ENTRIES,
    /** The answer to COUNT. */
    COUNTED,
    /** An entry was added. */
    ADDED,
    /** An entry was changed, closed or opened again. */
    CHANGED,
    /** An entry was removed. */
    REMOVED,
    /** A feature of the Circle was switched or removed. */
    FEATURE,
    /** The files of the Circle changed. */
    FILES,
    /** A Member is present in the Circle: their first connection joined it. */
    PRESENT,
    /** A Member is no longer present in the Circle: their last connection went. */
    ABSENT,
    /** The Circle Secret of the Circle is being rotated (PLAN.md, Crypto C6): its Data Objects are re-encrypted. */
    ROTATING,
    /** The rotation of the Circle Secret has ended: every Data Object of the Circle is on the new secret. */
    ROTATED,
    /** A frame failed. */
    ERROR
}
