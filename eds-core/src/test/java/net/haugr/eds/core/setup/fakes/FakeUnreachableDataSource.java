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
package net.haugr.eds.core.setup.fakes;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * A datasource which always fails to connect, as a database which is not
 * (yet) reachable.
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class FakeUnreachableDataSource implements DataSource {

    /** The message of the connection failure. */
    public static final String MESSAGE = "Connection refused";

    @Override
    public Connection getConnection() throws SQLException {
        throw new SQLException(MESSAGE, "08001");
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(final PrintWriter out) {
        // Not used
    }

    @Override
    public void setLoginTimeout(final int seconds) {
        // Not used
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        throw new SQLException("Not a wrapper");
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) {
        return false;
    }
}
