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

/**
 * <p>CDI event, fired when a change of Members, Circles, Trustees or the
 * MasterKey may end the access of open connections of the socket. The
 * {@link SocketHub} checks them again, after the change is committed: the
 * connections which joined the Circle named, or all of them (ticket 18).</p>
 *
 * @param circleId The Circle whose access changed, or null for a change
 *                 which may touch every connection (a Member, a Session,
 *                 the MasterKey)
 * @author Kim Jensen
 * @since EDS 2.0
 */
public record AccessChange(String circleId) {

    /** A change which may touch every connection. */
    public static final AccessChange EVERYONE = new AccessChange(null);

    /**
     * A change of the access to one Circle.
     *
     * @param circleId The Circle, by its ID; null means every connection
     * @return The event
     */
    public static AccessChange of(final String circleId) {
        return (circleId == null) ? EVERYONE : new AccessChange(circleId);
    }

    /**
     * Whether the change may touch every connection.
     *
     * @return True for every connection
     */
    public boolean touchesEveryone() {
        return circleId == null;
    }
}
