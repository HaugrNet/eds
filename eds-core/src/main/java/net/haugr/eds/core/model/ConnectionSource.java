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
package net.haugr.eds.core.model;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>Opens a JDBC Connection of its own, outside the transaction of the
 * request, e.g. a DataSource's {@code getConnection}. A copy of large content
 * reads the source with such a Connection while it writes the target with
 * the request's (PLAN.md, S4), as one Connection can't do both at the
 * same time. The caller closes the Connection.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@FunctionalInterface
public interface ConnectionSource {

    /**
     * Opens a new Connection, which isn't part of a JTA transaction when it
     * is opened on a thread without one.
     *
     * @return The Connection
     * @throws SQLException if no Connection can be opened
     */
    Connection open() throws SQLException;
}
