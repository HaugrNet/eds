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
package net.haugr.eds.core.socket;

import java.io.IOException;

/**
 * <p>A connection of the socket (FEATURES.md), as the {@link SocketHub} sees
 * it: something to send text to, and to close. The WebSocket session is one;
 * the tests have their own.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public interface SocketConnection {

    /** Close code: the connection broke a rule, e.g. it isn't authenticated. */
    int POLICY_VIOLATION = 1008;

    /** Close code: too much waits for the connection. */
    int TRY_AGAIN_LATER = 1013;

    /**
     * The ID of the connection.
     *
     * @return Unique ID
     */
    String id();

    /**
     * Sends a text frame; it may block while the peer doesn't read.
     *
     * @param text The frame
     * @throws IOException if it can't be sent
     */
    void send(String text) throws IOException;

    /**
     * Closes the connection.
     *
     * @param code   Close code
     * @param reason Reason, for the peer
     */
    void close(int code, String reason);
}
