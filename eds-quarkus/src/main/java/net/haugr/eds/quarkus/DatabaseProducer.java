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

import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import net.haugr.eds.core.model.SchemaManager;

/**
 * <p>Produces the database related Beans for the selected database, so the
 * EDS Beans do not need to know which database is used:</p>
 *
 * <ul>
 *   <li>The default (unqualified) EntityManager, which is the one of the
 *       active persistence unit. It is the Quarkus managed proxy, which binds
 *       to the current transaction, so it is safe to inject into application
 *       scoped Beans.</li>
 *   <li>The {@link SchemaManager}, with the active datasource, which the
 *       StartupBean uses to create a missing schema.</li>
 * </ul>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@ApplicationScoped
public class DatabaseProducer {

    private final Instance<EntityManager> postgresql;
    private final Instance<EntityManager> h2;
    private final Instance<DataSource> postgresqlDataSource;
    private final Instance<DataSource> h2DataSource;

    /**
     * CDI Constructor. The persistence units and datasources are injected
     * lazily, as only the active ones may be resolved.
     *
     * @param postgresql           EntityManager of the PostgreSQL persistence unit
     * @param h2                   EntityManager of the H2 persistence unit
     * @param postgresqlDataSource The PostgreSQL datasource
     * @param h2DataSource         The H2 datasource
     */
    @Inject
    public DatabaseProducer(@PersistenceUnit("pg") final Instance<EntityManager> postgresql,
                            @PersistenceUnit("h2") final Instance<EntityManager> h2,
                            @io.quarkus.agroal.DataSource("pg") final Instance<DataSource> postgresqlDataSource,
                            @io.quarkus.agroal.DataSource("h2") final Instance<DataSource> h2DataSource) {
        this.postgresql = postgresql;
        this.h2 = h2;
        this.postgresqlDataSource = postgresqlDataSource;
        this.h2DataSource = h2DataSource;
    }

    /**
     * Returns the EntityManager of the active persistence unit.
     *
     * @return EntityManager for the selected database
     */
    @Produces
    @Default
    public EntityManager entityManager() {
        return switch (DatabaseSelection.selected()) {
            case POSTGRESQL -> postgresql.get();
            case H2 -> h2.get();
        };
    }

    /**
     * Returns the SchemaManager for the active datasource.
     *
     * @return SchemaManager for the selected database
     */
    @Produces
    @Singleton
    public SchemaManager schemaManager() {
        return new SchemaManager(switch (DatabaseSelection.selected()) {
            case POSTGRESQL -> postgresqlDataSource.get();
            case H2 -> h2DataSource.get();
        });
    }
}
