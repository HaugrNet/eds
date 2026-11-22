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
 * {@link SocketHub} checks them again, after the change is committed.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class AccessChange {

    /** The event; it carries no information. */
    public static final AccessChange INSTANCE = new AccessChange();

    private AccessChange() {
        // Single instance.
    }
}
