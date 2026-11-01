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
package net.haugr.eds.quarkus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * <p>Selects the database for EDS at startup. The rule is deliberately based
 * on the configuration only, never on whether a database is reachable:</p>
 *
 * <ul>
 *   <li>If a database URL is configured, via the system property
 *       {@value #URL_PROPERTY} or the environment variable
 *       {@value #URL_VARIABLE}, PostgreSQL is used. If it cannot be reached,
 *       EDS stays not ready, it does not fall back to H2, as that would
 *       silently replace the real data with an empty database.</li>
 *   <li>Otherwise, the in-memory H2 database is used, and all data is lost
 *       when EDS stops.</li>
 * </ul>
 *
 * <p>The selection is applied by providing the "active" flags for the
 * datasources and persistence units, together with the PostgreSQL URL. The
 * ordinal is above application.properties, but below environment variables,
 * so the flags can still be overridden explicitly.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class DatabaseSelection implements ConfigSource {

    /** System property with the PostgreSQL JDBC URL. */
    public static final String URL_PROPERTY = "eds.db.url";
    /** Environment variable with the PostgreSQL JDBC URL. */
    public static final String URL_VARIABLE = "EDS_DB_URL";

    private static final int ORDINAL = 275;

    /**
     * The supported databases, with the name of their datasource and
     * persistence unit.
     */
    public enum Database {

        /** PostgreSQL, used when a database URL is configured. */
        POSTGRESQL("pg"),

        /** H2 in-memory, used when no database URL is configured. */
        H2("h2");

        private final String unit;

        Database(final String unit) {
            this.unit = unit;
        }

        /**
         * Returns the name of the datasource and persistence unit.
         *
         * @return Datasource and persistence unit name
         */
        public String getUnit() {
            return unit;
        }
    }

    /**
     * Returns the configured PostgreSQL JDBC URL, if any.
     *
     * @return The URL, or empty if none is configured
     */
    public static Optional<String> configuredUrl() {
        String url = System.getProperty(URL_PROPERTY);
        if (url == null || url.isBlank()) {
            url = System.getenv(URL_VARIABLE);
        }

        return (url == null || url.isBlank()) ? Optional.empty() : Optional.of(url.trim());
    }

    /**
     * Returns the database selected by the configuration.
     *
     * @return The selected database
     */
    public static Database selected() {
        return configuredUrl().isPresent() ? Database.POSTGRESQL : Database.H2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getProperties() {
        final Database database = selected();
        final Map<String, String> properties = new HashMap<>();

        for (final Database candidate : Database.values()) {
            final String active = String.valueOf(candidate == database);
            properties.put("quarkus.datasource." + candidate.getUnit() + ".active", active);
            properties.put("quarkus.hibernate-orm." + candidate.getUnit() + ".active", active);
        }
        configuredUrl().ifPresent(url -> properties.put("quarkus.datasource.pg.jdbc.url", url));

        return properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue(final String propertyName) {
        return getProperties().get(propertyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "EDS Database Selection";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrdinal() {
        return ORDINAL;
    }
}
